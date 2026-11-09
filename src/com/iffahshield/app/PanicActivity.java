package com.iffahshield.app;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/** "I'm feeling weak right now": breathing, urge timer, reasons, call a friend, lock everything. */
public class PanicActivity extends Activity {
    private Store s;
    private final Handler h = new Handler();
    private long start;
    private TextView timer, breathText;
    private ValueAnimator anim;

    private static final String[] ACTIONS = {
        "توضّأ وصلِّ ركعتين.",
        "انهض من مكانك الآن، واخرج أو اذهب إلى غرفة فيها أناس.",
        "خذ حماماً، وبماء بارد إن استطعت.",
        "مارس 20 تمرين ضغط، أو امشِ 10 دقائق.",
        "اتصل بشخص تحبه، ولو لم تخبره بما يحدث.",
        "اكتب على ورقة ما تشعر به الآن، ولماذا تريد التوقف.",
        "ضع الهاتف في غرفة أخرى حتى تمضي اللحظة."
    };

    private final Runnable tick = new Runnable() { @Override public void run() {
        long sec = (SystemClock.elapsedRealtime() - start) / 1000;
        timer.setText(String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60));
        h.postDelayed(this, 1000);
    } };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        s = new Store(this);
        s.countPanic();
        start = SystemClock.elapsedRealtime();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Ui.BG);
        LinearLayout root = Ui.vbox(this);
        int p = Ui.dp(this, 18);
        root.setPadding(p, Ui.dp(this, 26), p, Ui.dp(this, 36));
        sv.addView(root);

        TextView t = Ui.text(this, "ابقَ معي دقيقة 🤝", 26, Ui.TEXT, true);
        t.setGravity(Gravity.CENTER);
        root.addView(t, new LinearLayout.LayoutParams(-1, -2));
        TextView sub = Ui.text(this, "ما تشعر به الآن طبيعي وسيمضي. تخف الرغبة عادة خلال 10 إلى 15 دقيقة.", 15, Ui.MUTED, false);
        sub.setGravity(Gravity.CENTER);
        root.addView(sub, new LinearLayout.LayoutParams(-1, -2));
        root.addView(Ui.gap(this, 18));

        // breathing circle
        FrameLayout fl = new FrameLayout(this);
        final View circle = new View(this);
        circle.setBackground(Ui.round(Ui.alpha(Ui.ACCENT, 90), Ui.dp(this, 110)));
        int d = Ui.dp(this, 220);
        FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(d, d, Gravity.CENTER);
        fl.addView(circle, cl);
        breathText = Ui.text(this, "شهيق", 22, Ui.TEXT, true);
        breathText.setGravity(Gravity.CENTER);
        fl.addView(breathText, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        root.addView(fl, new LinearLayout.LayoutParams(-1, Ui.dp(this, 240)));

        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(12000); // 4s in, 4s hold, 4s out
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                float f = (Float) a.getAnimatedValue();
                float scale; String label;
                if (f < 1f / 3) { scale = 0.55f + 0.45f * (f * 3); label = "شهيق… " + (4 - (int) (f * 12)); }
                else if (f < 2f / 3) { scale = 1f; label = "احبس نفسك… " + (4 - (int) ((f - 1f / 3) * 12)); }
                else { scale = 1f - 0.45f * ((f - 2f / 3) * 3); label = "زفير… " + (4 - (int) ((f - 2f / 3) * 12)); }
                circle.setScaleX(scale); circle.setScaleY(scale);
                breathText.setText(label);
            }
        });
        anim.start();

        LinearLayout tc = Ui.card(this);
        TextView tl = Ui.text(this, "صامد منذ:", 14, Ui.MUTED, false);
        tl.setGravity(Gravity.CENTER);
        tc.addView(tl, new LinearLayout.LayoutParams(-1, -2));
        timer = Ui.text(this, "00:00", 34, Ui.ACCENT, true);
        timer.setGravity(Gravity.CENTER);
        tc.addView(timer, new LinearLayout.LayoutParams(-1, -2));
        root.addView(tc);

        LinearLayout rc = Ui.card(this);
        rc.addView(Ui.text(this, "لماذا بدأت؟", 17, Ui.TEXT, true));
        String r = s.reasons();
        rc.addView(Ui.text(this, r.length() > 0 ? r : "اكتب أسبابك من الشاشة الرئيسية، وستظهر لك هنا في أصعب اللحظات.", 15,
            r.length() > 0 ? Ui.TEXT : Ui.MUTED, false));
        root.addView(rc);

        LinearLayout ac = Ui.card(this);
        ac.addView(Ui.text(this, "افعل واحدة من هذه الآن:", 17, Ui.TEXT, true));
        for (String a : ACTIONS) ac.addView(Ui.text(this, "• " + a, 15, Ui.MUTED, false));
        root.addView(ac);

        Button lock = Ui.button(this, "🔒 أقفل كل التطبيقات لفترة", Ui.WARN, Ui.BG);
        lock.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { chooseLock(); } });
        root.addView(lock);

        final String phone = s.friendPhone();
        Button call = Ui.button(this, phone.length() > 0 ? "📞 اتصل بصديقك" : "📞 أضف رقم صديق من الشاشة الرئيسية", Ui.ACCENT_DARK, Ui.TEXT);
        call.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            if (phone.length() == 0) { toast("أضف رقم صديق موثوق من الشاشة الرئيسية."); return; }
            try { startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone))); } catch (Exception ignored) { }
        } });
        root.addView(call);

        Button done = Ui.button(this, "مضت اللحظة، أنا أقوى 💪", Ui.ACCENT, Ui.BG);
        done.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            toast("أحسنت! كل مرة تصبح أسهل.");
            finish();
        } });
        root.addView(done);

        setContentView(sv);
    }

    private void chooseLock() {
        final int[] mins = { 15, 30, 60, 120, 240 };
        String[] labels = { "15 دقيقة", "30 دقيقة", "ساعة", "ساعتين", "4 ساعات" };
        new AlertDialog.Builder(this).setTitle("قفل كل التطبيقات")
            .setMessage(null)
            .setItems(labels, new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int i) {
                s.lockdown(mins[i]);
                toast("تم. كل شيء مقفل ما عدا الاتصال والرسائل والكاميرا والساعة.");
                startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            } }).show();
    }

    @Override protected void onResume() { super.onResume(); h.post(tick); }
    @Override protected void onPause() { super.onPause(); h.removeCallbacks(tick); }
    @Override protected void onDestroy() { if (anim != null) anim.cancel(); super.onDestroy(); }
    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_LONG).show(); }
}
