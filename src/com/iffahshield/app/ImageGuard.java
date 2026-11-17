package com.iffahshield.app;

import android.accessibilityservice.AccessibilityService;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.Display;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * On-device nudity detection. Takes a screenshot through the accessibility service, cuts it into tiles and
 * runs a small classifier on each. Nothing is saved and nothing leaves the phone.
 * Classes of the model: 0 drawings, 1 hentai, 2 neutral, 3 porn, 4 sexy.
 */
final class ImageGuard {
    interface Listener { void onNudity(String pkg, String kind, float score); }

    static final int SIZE = 224;
    // Thresholds: explicit content blocks alone; "sexy" needs a higher score.
    static final float T_PORN = 0.80f, T_HENTAI = 0.85f, T_SEXY = 0.90f;
    // Thresholds stay as they are. But one frame alone can fool the model (a normal video scored 86% once),
    // so unless a hit is near-certain, the very next look at the screen (about a second later) must agree.
    static final float T_SURE = 0.97f;
    // Social feeds: also catch immodest / revealing photos (lower "sexy" bar). Such hits are only covered and
    // scrolled past, never close the app (GuardService.onNudity), so a lower bar costs no lock-outs.
    static final float T_SEXY_FEED = 0.60f;
    static final java.util.Set<String> SOCIAL = new java.util.HashSet<String>(java.util.Arrays.asList(
        "com.facebook.katana", "com.facebook.lite", "com.instagram.android", "com.instagram.barcelona",
        "com.google.android.youtube", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
        "com.twitter.android", "com.snapchat.android", "com.linkedin.android", "com.pinterest",
        "com.reddit.frontpage", "com.facebook.orca", "org.telegram.messenger"));
    private String pendingPkg = null;
    private long pendingAt = 0;
    volatile boolean pending = false;

    private final AccessibilityService svc;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final HandlerThread thread = new HandlerThread("hisn-img");
    private final Handler bg;
    private final Executor bgExec = new Executor() { @Override public void execute(Runnable r) { bg.post(r); } };
    private Interpreter tfl;
    private boolean failed = false;
    private volatile boolean busy = false;
    private long lastHash = 0;
    private final ByteBuffer in = ByteBuffer.allocateDirect(SIZE * SIZE * 3 * 4).order(ByteOrder.nativeOrder());
    private final int[] px = new int[SIZE * SIZE];
    private final float[][] out = new float[1][5];
    private final Bitmap tile = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
    private final Canvas tileCanvas = new Canvas(tile);
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    volatile String lastInfo = "-";
    /** «Ghadd al-basar» mode: in social apps, cover and skip any picture that clearly shows a woman. */
    volatile boolean ghadd = false;
    private BodyDetector body;
    private boolean bodyFailed = false;
    // NudeNet thresholds. Explicit parts are checked everywhere; the rest only in social apps (cover + skip, never close).
    static final float N_EXPLICIT = 0.45f, N_IMMODEST = 0.60f, N_WOMAN = 0.50f;
    // The 320n model can't tell men from women reliably (men's portraits scored FACE_FEMALE 84% in tests, and
    // normal Facebook videos got skipped). The woman rule stays OFF until a dedicated, tested gender model is in.
    static final boolean WOMAN_RULE_READY = false;
    static final float WOMAN_MIN_AREA = 0.012f;


    ImageGuard(AccessibilityService svc, Listener l) {
        this.svc = svc; this.listener = l;
        thread.start();
        bg = new Handler(thread.getLooper());
    }

    void stop() { thread.quitSafely(); }

    boolean available() { return android.os.Build.VERSION.SDK_INT >= 30 && !failed; }

    private boolean ensureModel() {
        if (tfl != null) return true;
        if (failed) return false;
        try {
            AssetFileDescriptor fd = svc.getAssets().openFd("model.tflite");
            FileInputStream is = new FileInputStream(fd.getFileDescriptor());
            ByteBuffer model = is.getChannel().map(FileChannel.MapMode.READ_ONLY, fd.getStartOffset(), fd.getDeclaredLength());
            Interpreter.Options o = new Interpreter.Options();
            o.setNumThreads(4);
            tfl = new Interpreter(model, o);
            return true;
        } catch (Throwable t) { failed = true; lastInfo = "model failed: " + t; return false; }
    }

    private boolean ensureBody() {
        if (body != null) return true;
        if (bodyFailed) return false;
        try { body = new BodyDetector(svc); return true; }
        catch (Throwable t) { bodyFailed = true; lastInfo = "body model failed: " + t; return false; }
    }

    /** Ask for one scan of the current screen, attributed to pkg. Cheap to call; drops the request when busy. */
    void scan(final String pkg, final java.util.List<Rect> images) {
        if (busy || !available()) return;
        busy = true;
        try {
            svc.takeScreenshot(Display.DEFAULT_DISPLAY, bgExec, new AccessibilityService.TakeScreenshotCallback() {
                @Override public void onSuccess(AccessibilityService.ScreenshotResult r) {
                    try { analyse(r, pkg, images); } catch (Throwable t) { lastInfo = "err " + t; } finally { busy = false; }
                }
                @Override public void onFailure(int code) { lastInfo = "shot failed " + code; busy = false; }
            });
        } catch (Throwable t) { lastInfo = "shot err " + t; busy = false; }
    }

    private void analyse(AccessibilityService.ScreenshotResult r, final String pkg, java.util.List<Rect> images) {
        HardwareBuffer hb = r.getHardwareBuffer();
        Bitmap hw = Bitmap.wrapHardwareBuffer(hb, r.getColorSpace());
        if (hw == null) { hb.close(); return; }
        Bitmap sw = hw.copy(Bitmap.Config.ARGB_8888, false);
        hw.recycle(); hb.close();
        if (sw == null) return;
        try {
            int w = sw.getWidth(), h = sw.getHeight();
            int top = (int) (h * 0.07f), bottom = (int) (h * 0.95f);   // skip status bar / navigation bar

            // Unchanged screen: nothing to do.
            long hash = 1469598103934665603L;
            for (int y = 0; y < 24; y++) for (int x = 0; x < 16; x++) {
                int c = sw.getPixel(w * x / 16 + w / 32, top + (bottom - top) * y / 24) & 0xF8F8F8;
                hash = (hash ^ c) * 1099511628211L;
            }
            if (hash == lastHash) return;
            lastHash = hash;
            if (!ensureModel()) return;

            long t0 = System.currentTimeMillis();
            float bestP = 0, bestH = 0, bestS = 0;
            String kind = null; float score = 0;
            // 1 whole view + 3 big squares + small squares in a 2-column grid (thumbnails).
            java.util.ArrayList<Rect> tiles = new java.util.ArrayList<Rect>();
            // Real pictures on screen first (exact frames from the accessibility tree), then generic tiles.
            if (images != null) for (Rect ir : images) {
                Rect c = new Rect(Math.max(0, ir.left), Math.max(0, ir.top), Math.min(w, ir.right), Math.min(h, ir.bottom));
                if (c.width() >= 120 && c.height() >= 120) tiles.add(c);
            }
            int exact = tiles.size();
            tiles.add(new Rect(0, top, w, bottom));
            int span = bottom - top - w;
            for (int i = 0; i < 3; i++) tiles.add(new Rect(0, top + span * i / 2, w, top + span * i / 2 + w));
            int s = w / 2, rows = Math.max(1, (bottom - top) / s);
            for (int ry = 0; ry < rows; ry++) for (int cx = 0; cx < 2; cx++) {
                int y = top + (bottom - top - s) * ry / Math.max(1, rows - 1);
                tiles.add(new Rect(cx * s, y, cx * s + s, y + s));
            }
            final float tSexy = SOCIAL.contains(pkg) ? T_SEXY_FEED : T_SEXY;
            for (Rect t : tiles) {
                float[] p = classify(sw, t);
                if (p[3] > bestP) bestP = p[3];
                if (p[1] > bestH) bestH = p[1];
                if (p[4] > bestS) bestS = p[4];
                if (p[3] >= T_PORN) { kind = "porn"; score = p[3]; break; }
                if (p[1] >= T_HENTAI) { kind = "hentai"; score = p[1]; break; }
                if (p[4] >= tSexy) { kind = "sexy"; score = p[4]; break; }
            }
            String found = "";
            // Second engine: NudeNet body-part detector on the whole screen and the two biggest pictures.
            // A classifier hit in a social app that is not near-certain must be confirmed by NudeNet (concrete exposed
            // parts); otherwise it becomes a "doubt": covered and skipped, never an instant close.
            final boolean checkHit = kind != null && SOCIAL.contains(pkg) && score < T_SURE;
            boolean nnExplicit = false, nnBody = false;
            if ((kind == null || checkHit) && ensureBody()) {
                List<Rect> regs = new ArrayList<Rect>();
                regs.add(new Rect(0, top, w, bottom));
                List<Rect> pics = new ArrayList<Rect>(tiles.subList(0, exact));
                Collections.sort(pics, new java.util.Comparator<Rect>() { @Override public int compare(Rect a, Rect b) { return b.width() * b.height() - a.width() * a.height(); } });
                for (int i = 0; i < Math.min(2, pics.size()); i++) if (pics.get(i).width() >= 200 && pics.get(i).height() >= 200) regs.add(pics.get(i));
                boolean social = SOCIAL.contains(pkg);
                float screenArea = (float) w * (bottom - top);
                String soft = null; float softScore = 0;
                outer:
                for (Rect reg : regs) {
                    List<BodyDetector.Det> ds;
                    try { ds = body.detect(sw, reg); } catch (Throwable t) { lastInfo = "body err " + t; break; }
                    for (BodyDetector.Det d : ds) {
                        if (d.score >= 0.30f && d.cls != BodyDetector.FACE_FEMALE && d.cls != 12 && d.cls != 7 && d.cls != 9) nnBody = true;   // any body part (not faces/feet)
                        if (d.score >= 0.35f && found.length() < 160) found += " " + d;
                        switch (d.cls) {
                            case BodyDetector.FEMALE_BREAST_EXPOSED: case BodyDetector.FEMALE_GENITALIA_EXPOSED:
                            case BodyDetector.MALE_GENITALIA_EXPOSED: case BodyDetector.ANUS_EXPOSED: case BodyDetector.BUTTOCKS_EXPOSED:
                                if (d.score >= N_EXPLICIT) { nnExplicit = true; if (kind == null) { kind = "porn"; score = d.score; } break outer; }
                                break;
                            case BodyDetector.BELLY_EXPOSED: case BodyDetector.ARMPITS_EXPOSED: case BodyDetector.FEMALE_BREAST_COVERED:
                            case BodyDetector.BUTTOCKS_COVERED: case BodyDetector.FEMALE_GENITALIA_COVERED:
                                if (social && d.score >= N_IMMODEST && soft == null) { soft = "immodest"; softScore = d.score; }
                                break;
                            case BodyDetector.FACE_FEMALE:
                                if (WOMAN_RULE_READY && social && ghadd && d.score >= N_WOMAN && d.box.width() * d.box.height() >= WOMAN_MIN_AREA * screenArea && soft == null) { soft = "woman"; softScore = d.score; }
                                break;
                        }
                    }
                }
                if (kind == null && soft != null) { kind = soft; score = softScore; }
                // Classifier hit, NudeNet not confirming: if NudeNet sees no body at all, it's a false alarm (dismissed);
                // if it sees a body, cover and skip it as a doubt.
                if (checkHit && !nnExplicit) {
                    if (nnBody) { found += " (body seen, not explicit)"; kind = "doubt"; }
                    else { found += " (dismissed: no body seen by NudeNet)"; kind = null; }
                }
            }
            lastInfo = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date())
                + " " + pkg + " porn=" + pct(bestP) + " hentai=" + pct(bestH) + " sexy=" + pct(bestS)
                + " imgs=" + exact + " tiles=" + tiles.size() + " ms=" + (System.currentTimeMillis() - t0) + (kind != null ? " HIT " + kind : "") + (found.isEmpty() ? "" : " |" + found);
            if (kind != null) {
                lastHash = 0;   // look again next time even if the screen did not change
                long now = System.currentTimeMillis();
                boolean sure = !"sexy".equals(kind) && !"immodest".equals(kind) && !"woman".equals(kind) && score >= T_SURE;
                boolean confirmed = pkg.equals(pendingPkg) && now - pendingAt < 5000;
                if (sure || confirmed) {
                    pendingPkg = null; pending = false;
                    final String k = kind; final float sc = score;
                    main.post(new Runnable() { @Override public void run() { listener.onNudity(pkg, k, sc); } });
                } else {
                    pendingPkg = pkg; pendingAt = now; pending = true;
                    lastInfo += " (waiting for 2nd look)";
                }
            } else { pendingPkg = null; pending = false; }
        } finally { sw.recycle(); }
    }

    private static String pct(float f) { return String.valueOf(Math.round(f * 100)); }

    private float[] classify(Bitmap src, Rect r) {
        tileCanvas.drawBitmap(src, r, new Rect(0, 0, SIZE, SIZE), paint);
        tile.getPixels(px, 0, SIZE, 0, 0, SIZE, SIZE);
        in.rewind();
        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            in.putFloat(((c >> 16) & 0xFF) / 255f);
            in.putFloat(((c >> 8) & 0xFF) / 255f);
            in.putFloat((c & 0xFF) / 255f);
        }
        in.rewind();
        tfl.run(in, out);
        return out[0];
    }
}
