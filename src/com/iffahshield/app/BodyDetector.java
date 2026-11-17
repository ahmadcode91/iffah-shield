package com.iffahshield.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

/**
 * NudeNet 320n (YOLOv8n) body-part detector, run on the phone with ONNX Runtime.
 * Unlike the whole-image classifier it says WHAT is in the picture and WHERE (box + confidence),
 * so decisions rest on concrete findings rather than a general guess.
 * Preprocessing and decoding follow nudenet.py 3.4.2: pad right/bottom to a square, resize to 320, RGB / 255,
 * output [1, 4 + 18, 2100] (cx, cy, w, h, class scores), confidence >= 0.25, NMS IoU 0.45.
 */
final class BodyDetector {
    static final String[] LABELS = {
        "FEMALE_GENITALIA_COVERED", "FACE_FEMALE", "BUTTOCKS_EXPOSED", "FEMALE_BREAST_EXPOSED",
        "FEMALE_GENITALIA_EXPOSED", "MALE_BREAST_EXPOSED", "ANUS_EXPOSED", "FEET_EXPOSED", "BELLY_COVERED",
        "FEET_COVERED", "ARMPITS_COVERED", "ARMPITS_EXPOSED", "FACE_MALE", "BELLY_EXPOSED",
        "MALE_GENITALIA_EXPOSED", "ANUS_COVERED", "FEMALE_BREAST_COVERED", "BUTTOCKS_COVERED" };
    static final int FEMALE_GENITALIA_COVERED = 0, FACE_FEMALE = 1, BUTTOCKS_EXPOSED = 2, FEMALE_BREAST_EXPOSED = 3,
        FEMALE_GENITALIA_EXPOSED = 4, ANUS_EXPOSED = 6, ARMPITS_EXPOSED = 11, BELLY_EXPOSED = 13,
        MALE_GENITALIA_EXPOSED = 14, FEMALE_BREAST_COVERED = 16, BUTTOCKS_COVERED = 17;

    static final String ASSET = "320n.onnx";
    int size() { return S; }

    static final class Det {
        final int cls; final float score; final Rect box;   // box in the source bitmap's coordinates
        Det(int c, float s, Rect b) { cls = c; score = s; box = b; }
        @Override public String toString() { return LABELS[cls] + " " + Math.round(score * 100); }
    }

    private final int S;
    private final OrtEnvironment env;
    private final OrtSession session;
    private final String inputName;
    private final FloatBuffer input;
    private final int[] px;
    private final Bitmap canvasBmp;
    private final Canvas canvas;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

    BodyDetector(Context c) throws Exception { this(c, null); }

    /** modelFile: null = bundled assets model; else a file path (adb tests). */
    BodyDetector(Context c, String modelFile) throws Exception {
        InputStream is = modelFile == null ? c.getAssets().open(ASSET) : new java.io.FileInputStream(modelFile);
        ByteArrayOutputStream bo = new ByteArrayOutputStream(16 << 20);
        byte[] buf = new byte[1 << 16]; int n;
        while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
        is.close();
        env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions o = new OrtSession.SessionOptions();
        o.setIntraOpNumThreads(4);
        session = env.createSession(bo.toByteArray(), o);
        inputName = session.getInputNames().iterator().next();
        long[] shape = ((ai.onnxruntime.TensorInfo) session.getInputInfo().get(inputName).getInfo()).getShape();
        S = shape.length == 4 && shape[2] > 0 ? (int) shape[2] : 320;
        input = ByteBuffer.allocateDirect(3 * S * S * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        px = new int[S * S];
        canvasBmp = Bitmap.createBitmap(S, S, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(canvasBmp);
    }

    /** Detect on region r of src. */
    List<Det> detect(Bitmap src, Rect r) throws Exception {
        int side = Math.max(r.width(), r.height());
        float scale = (float) S / side;
        // Black square, region drawn top-left (= nudenet's right/bottom padding).
        canvas.drawColor(0xFF000000);
        canvas.drawBitmap(src, r, new Rect(0, 0, Math.round(r.width() * scale), Math.round(r.height() * scale)), paint);
        canvasBmp.getPixels(px, 0, S, 0, 0, S, S);
        input.rewind();
        int plane = S * S;
        for (int i = 0; i < plane; i++) {
            int c = px[i];
            input.put(i, ((c >> 16) & 0xFF) / 255f);
            input.put(plane + i, ((c >> 8) & 0xFF) / 255f);
            input.put(2 * plane + i, (c & 0xFF) / 255f);
        }
        input.rewind();
        float[][] o;
        OnnxTensor t = OnnxTensor.createTensor(env, input, new long[] { 1, 3, S, S });
        try {
            OrtSession.Result res = session.run(Collections.singletonMap(inputName, t));
            try { o = ((float[][][]) res.get(0).getValue())[0]; } finally { res.close(); }
        } finally { t.close(); }

        int anchors = o[0].length, classes = o.length - 4;
        List<Det> all = new ArrayList<Det>();
        for (int a = 0; a < anchors; a++) {
            int best = -1; float bs = 0;
            for (int k = 0; k < classes; k++) if (o[4 + k][a] > bs) { bs = o[4 + k][a]; best = k; }
            if (bs < 0.25f) continue;
            float cx = o[0][a] / scale, cy = o[1][a] / scale, bw = o[2][a] / scale, bh = o[3][a] / scale;
            int x0 = r.left + Math.max(0, Math.round(cx - bw / 2)), y0 = r.top + Math.max(0, Math.round(cy - bh / 2));
            int x1 = Math.min(r.right, r.left + Math.round(cx + bw / 2)), y1 = Math.min(r.bottom, r.top + Math.round(cy + bh / 2));
            if (x1 > x0 && y1 > y0) all.add(new Det(best, bs, new Rect(x0, y0, x1, y1)));
        }
        // Non-maximum suppression per class (IoU 0.45).
        Collections.sort(all, new Comparator<Det>() { @Override public int compare(Det a, Det b) { return Float.compare(b.score, a.score); } });
        List<Det> keep = new ArrayList<Det>();
        for (Det d : all) {
            boolean dup = false;
            for (Det k : keep) if (k.cls == d.cls && iou(k.box, d.box) > 0.45f) { dup = true; break; }
            if (!dup) keep.add(d);
        }
        return keep;
    }

    private static float iou(Rect a, Rect b) {
        int ix = Math.max(0, Math.min(a.right, b.right) - Math.max(a.left, b.left));
        int iy = Math.max(0, Math.min(a.bottom, b.bottom) - Math.max(a.top, b.top));
        float inter = (float) ix * iy;
        float u = (float) a.width() * a.height() + (float) b.width() * b.height() - inter;
        return u <= 0 ? 0 : inter / u;
    }
}
