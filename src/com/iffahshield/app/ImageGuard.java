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
            for (Rect t : tiles) {
                float[] p = classify(sw, t);
                if (p[3] > bestP) bestP = p[3];
                if (p[1] > bestH) bestH = p[1];
                if (p[4] > bestS) bestS = p[4];
                if (p[3] >= T_PORN) { kind = "porn"; score = p[3]; break; }
                if (p[1] >= T_HENTAI) { kind = "hentai"; score = p[1]; break; }
                if (p[4] >= T_SEXY) { kind = "sexy"; score = p[4]; break; }
            }
            lastInfo = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date())
                + " " + pkg + " porn=" + pct(bestP) + " hentai=" + pct(bestH) + " sexy=" + pct(bestS)
                + " imgs=" + exact + " tiles=" + tiles.size() + " ms=" + (System.currentTimeMillis() - t0) + (kind != null ? " HIT " + kind : "");
            if (kind != null) {
                lastHash = 0;
                final String k = kind; final float sc = score;
                main.post(new Runnable() { @Override public void run() { listener.onNudity(pkg, k, sc); } });
            }
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
