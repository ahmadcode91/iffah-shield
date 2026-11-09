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
    private boolean triggerLogged;

    private static final String[] TIPS = {
        "الفراغ باب كبير. قم واعمل شيئاً بيدك الآن: مشي، رياضة، ترتيب، عمل.",
        "الوحدة تقوّي الرغبة. اذهب إلى أهلك أو اتصل بصديق الآن.",
        "التعب يُضعف الإرادة. ضع الهاتف خارج الغرفة ونم.",
        "التوتر يبحث عن راحة سريعة. توضّأ، صلِّ، أو امشِ 10 دقائق.",
        "السرير مع الهاتف أخطر مكان. أخرج الهاتف من الغرفة الآن.",
        "أغلق الصورة فوراً وقل أستغفر الله. «لك الأولى وليست لك الآخرة»."
    };
    private static final String[] TIPS_EN = {
        "Idle time is a big door. Get up and do something with your hands now: walk, exercise, tidy, work.",
        "Loneliness feeds the urge. Go to your family or call a friend now.",
        "Tiredness weakens willpower. Put the phone outside the room and sleep.",
        "Stress looks for quick relief. Make wudu, pray, or walk for 10 minutes.",
        "Bed plus phone is the most dangerous place. Take the phone out of the room now.",
        "Close the image at once and say astaghfirullah. The first glance is yours, the second is not."
    };

    private static final String[] ACTIONS_EN = {
        "Make wudu and pray two rak'ahs.",
        "Get up now, go outside or to a room with people.",
        "Take a shower, a cold one if you can.",
        "Do 20 push-ups or walk for 10 minutes.",
        "Call someone you love, even if you don't tell them what's happening.",
        "Write on paper what you feel now, and why you want to stop.",
        "Put the phone in another room until the moment passes."
    };
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
        L.init(this);
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

        TextView t = Ui.text(this, L.t("ابقَ معي دقيقة 🤝", "Stay with me for a minute 🤝"), 26, Ui.TEXT, true);
        t.setGravity(Gravity.CENTER);
        root.addView(t, new LinearLayout.LayoutParams(-1, -2));
        TextView sub = Ui.text(this, L.t("ما تشعر به الآن طبيعي وسيمضي. تخف الرغبة عادة خلال 10 إلى 15 دقيقة.", "What you feel now is normal and it will pass. Urges usually fade within 10 to 15 minutes."), 15, Ui.MUTED, false);
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
        breathText = Ui.text(this, L.t("شهيق", "Breathe in"), 22, Ui.TEXT, true);
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
                if (f < 1f / 3) { scale = 0.55f + 0.45f * (f * 3); label = L.t("شهيق… ", "Breathe in… ") + (4 - (int) (f * 12)); }
                else if (f < 2f / 3) { scale = 1f; label = L.t("احبس نفسك… ", "Hold… ") + (4 - (int) ((f - 1f / 3) * 12)); }
                else { scale = 1f - 0.45f * ((f - 2f / 3) * 3); label = L.t("زفير… ", "Breathe out… ") + (4 - (int) ((f - 2f / 3) * 12)); }
                circle.setScaleX(scale); circle.setScaleY(scale);
                breathText.setText(label);
            }
        });
        anim.start();

        LinearLayout tc = Ui.card(this);
        TextView tl = Ui.text(this, L.t("صامد منذ:", "Holding on for:"), 14, Ui.MUTED, false);
        tl.setGravity(Gravity.CENTER);
        tc.addView(tl, new LinearLayout.LayoutParams(-1, -2));
        timer = Ui.text(this, "00:00", 34, Ui.ACCENT, true);
        timer.setGravity(Gravity.CENTER);
        tc.addView(timer, new LinearLayout.LayoutParams(-1, -2));
        root.addView(tc);

        LinearLayout rc = Ui.card(this);
        rc.addView(Ui.text(this, L.t("لماذا بدأت؟", "Why did you start?"), 17, Ui.TEXT, true));
        String r = s.reasons();
        rc.addView(Ui.text(this, r.length() > 0 ? r : L.t("اكتب أسبابك من الشاشة الرئيسية، وستظهر لك هنا في أصعب اللحظات.", "Write your reasons on the main screen, and they'll appear here in the hardest moments."), 15,
            r.length() > 0 ? Ui.TEXT : Ui.MUTED, false));
        root.addView(rc);

        final String[] trig = Store.triggers();
        LinearLayout tg = Ui.card(this);
        tg.addView(Ui.text(this, L.t("ما الذي حرّكك الآن؟", "What triggered it right now?"), 17, Ui.TEXT, true));
        LinearLayout row = null;
        for (int i = 0; i < trig.length; i++) {
            if (i % 2 == 0) { row = Ui.hbox(this); tg.addView(row); }
            final int idx = i;
            final Button tb = Ui.button(this, trig[i], Ui.alpha(Ui.TEXT, 25), Ui.TEXT);
            tb.setTextSize(13);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1);
            lp.setMargins(Ui.dp(this, 3), Ui.dp(this, 6), Ui.dp(this, 3), 0);
            tb.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
                if (triggerLogged) return;
                triggerLogged = true;
                s.logTrigger(idx);
                tb.setBackground(Ui.round(Ui.ACCENT_DARK, Ui.dp(PanicActivity.this, 14)));
                toast(L.en ? TIPS_EN[idx] : TIPS[idx]);
            } });
            row.addView(tb, lp);
        }
        root.addView(tg);

        LinearLayout dc = Ui.card(this);
        dc.addView(Ui.text(this, L.t("قل من قلبك:", "Say it from your heart:"), 17, Ui.TEXT, true));
        dc.addView(Ui.text(this, "«اللهم إني أعوذ بك من شر سمعي، ومن شر بصري، ومن شر لساني، ومن شر قلبي، ومن شر منيّي»\n«اللهم طهّر قلبي، وحصّن فرجي»", 16, Ui.TEXT, false));
        dc.addView(Ui.text(this, L.t("«ومن لم يستطع فعليه بالصوم فإنه له وِجاء» (متفق عليه)",
            "\"Whoever cannot, let him fast, for it is a shield for him.\" (Bukhari & Muslim)"), 13, Ui.MUTED, false));
        root.addView(dc);

        LinearLayout ac = Ui.card(this);
        ac.addView(Ui.text(this, L.t("افعل واحدة من هذه الآن:", "Do one of these now:"), 17, Ui.TEXT, true));
        for (String a : (L.en ? ACTIONS_EN : ACTIONS)) ac.addView(Ui.text(this, "• " + a, 15, Ui.MUTED, false));
        root.addView(ac);

        Button lock = Ui.button(this, L.t("🔒 أقفل كل التطبيقات لفترة", "🔒 Lock all apps for a while"), Ui.WARN, Ui.BG);
        lock.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { chooseLock(); } });
        root.addView(lock);

        final String phone = s.friendPhone();
        Button call = Ui.button(this, phone.length() > 0 ? L.t("📞 اتصل بصديقك", "📞 Call your friend") : L.t("📞 أضف رقم صديق من الشاشة الرئيسية", "📞 Add a friend's number on the main screen"), Ui.ACCENT_DARK, Ui.TEXT);
        call.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            if (phone.length() == 0) { toast(L.t("أضف رقم صديق موثوق من الشاشة الرئيسية.", "Add a trusted friend's number on the main screen.")); return; }
            try { startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone))); } catch (Exception ignored) { }
        } });
        root.addView(call);

        Button done = Ui.button(this, L.t("مضت اللحظة، أنا أقوى 💪", "The moment passed, I'm stronger 💪"), Ui.ACCENT, Ui.BG);
        done.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            s.logUrgeWon();
            toast(L.t("أحسنت! كل مرة تصبح أسهل. (انتصرت " + s.urgesWon() + " مرة)", "Well done! It gets easier every time. (" + s.urgesWon() + " urges beaten)"));
            finish();
        } });
        root.addView(done);

        sv.setFitsSystemWindows(true);
        setContentView(sv);
    }

    private void chooseLock() {
        final int[] mins = { 15, 30, 60, 120, 240 };
        String[] labels = { L.t("15 دقيقة", "15 minutes"), L.t("30 دقيقة", "30 minutes"), L.t("ساعة", "1 hour"), L.t("ساعتين", "2 hours"), L.t("4 ساعات", "4 hours") };
        new AlertDialog.Builder(this).setTitle(L.t("قفل كل التطبيقات", "Lock all apps"))
            .setMessage(null)
            .setItems(labels, new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int i) {
                s.lockdown(mins[i]);
                toast(L.t("تم. كل شيء مقفل ما عدا الاتصال والرسائل والكاميرا والساعة.", "Done. Everything is locked except calls, messages, camera and clock."));
                startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            } }).show();
    }

    @Override protected void onResume() { super.onResume(); h.post(tick); }
    @Override protected void onPause() { super.onPause(); h.removeCallbacks(tick); }
    @Override protected void onDestroy() { if (anim != null) anim.cancel(); super.onDestroy(); }
    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_LONG).show(); }
}
