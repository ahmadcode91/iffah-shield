package com.iffahshield.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Random;

public class BlockActivity extends Activity {
    private Button home;
    private int left = 0;
    private final android.os.Handler hd = new android.os.Handler();
    private final Runnable tick = new Runnable() { @Override public void run() {
        if (left > 0) {
            home.setText(L.t("خذ نفَساً… ", "Take a breath… ") + left);
            home.setAlpha(0.5f);
            left--;
            hd.postDelayed(this, 1000);
        } else { home.setText(L.t("رجوع للرئيسية", "Back to home")); home.setAlpha(1f); left = 0; }
    } };

    private static final String[] LINES = {
        "هذه اللحظة ستمضي، وأنت أقوى منها.",
        "كل مرة تقول فيها «لا» تصبح أقوى من سابقتها.",
        "«قل للمؤمنين يغضوا من أبصارهم ويحفظوا فروجهم ذلك أزكى لهم» (النور 30)",
        "«ألم يعلم بأن الله يرى» (العلق 14)",
        "«إن السمع والبصر والفؤاد كل أولئك كان عنه مسؤولاً» (الإسراء 36)",
        "«يعلم خائنة الأعين وما تخفي الصدور» (غافر 19)",
        "«والذين جاهدوا فينا لنهدينهم سبلنا» (العنكبوت 69)",
        "«ولا تقربوا الزنى إنه كان فاحشة وساء سبيلاً» (الإسراء 32)",
        "«قل يا عبادي الذين أسرفوا على أنفسهم لا تقنطوا من رحمة الله» (الزمر 53)",
        "«إنك لن تدع شيئاً لله عز وجل إلا بدّلك الله به ما هو خير لك منه» (رواه أحمد)",
        "«احفظ الله يحفظك» (رواه الترمذي)",
        "دقائق من المتعة لا تساوي أياماً من الندم.",
        "قم واشرب ماءً، وغيّر مكانك، أو اتصل بمن تحب.",
        "أنت تبني نسخة أفضل من نفسك. واصل."
    };
    private static final String[] LINES_EN = {
        "This moment will pass, and you are stronger than it.",
        "Every time you say \"no\", you get stronger than the time before.",
        "\"Tell the believing men to lower their gaze and guard their chastity. That is purer for them.\" (24:30)",
        "\"Does he not know that Allah sees?\" (96:14)",
        "\"The hearing, the sight and the heart, all of these will be questioned.\" (17:36)",
        "\"He knows the stealthy glance of the eyes and what the hearts conceal.\" (40:19)",
        "\"Those who strive for Us, We will surely guide them to Our ways.\" (29:69)",
        "\"Do not despair of the mercy of Allah.\" (39:53)",
        "\"You will never give up something for Allah except that He will replace it with something better.\" (Ahmad)",
        "\"Be mindful of Allah, and He will protect you.\" (Tirmidhi)",
        "A few minutes of pleasure are not worth days of regret.",
        "Get up, drink water, change rooms, or call someone you love.",
        "You are building a better version of yourself. Keep going."
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        L.init(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        Store s = new Store(this);

        LinearLayout root = Ui.vbox(this);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Ui.BG);
        int p = Ui.dp(this, 28);
        root.setPadding(p, p, p, p);

        TextView icon = Ui.text(this, "🛡", 72, Ui.ACCENT, false);
        icon.setGravity(Gravity.CENTER);
        root.addView(icon, new LinearLayout.LayoutParams(-1, -2));

        TextView title = Ui.text(this, L.t("درع العفّة حماك", "Iffah Shield protected you"), 30, Ui.TEXT, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        String reason = getIntent().getStringExtra("reason");
        TextView r = Ui.text(this, reason == null ? L.t("تم الحجب.", "Blocked.") : reason, 17, Ui.MUTED, false);
        r.setGravity(Gravity.CENTER);
        root.addView(r, new LinearLayout.LayoutParams(-1, -2));
        root.addView(Ui.gap(this, 28));

        LinearLayout card = Ui.card(this);
        String custom = s.customMsg();
        TextView line = Ui.text(this, custom.length() > 0 ? custom : (L.en ? LINES_EN[new Random().nextInt(LINES_EN.length)] : LINES[new Random().nextInt(LINES.length)]), 19, Ui.TEXT, true);
        line.setGravity(Gravity.CENTER);
        card.addView(line);
        card.addView(Ui.gap(this, 10));
        TextView streak = Ui.text(this, L.t("لك ", "") + s.streakDays() + L.t(" يوماً من النقاء 💪", " clean days 💪"), 16, Ui.ACCENT, true);
        streak.setGravity(Gravity.CENTER);
        card.addView(streak);
        root.addView(card);

        home = Ui.button(this, L.t("رجوع للرئيسية", "Back to home"), Ui.ACCENT, Ui.BG);
        home.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { if (left <= 0) goHome(); } });
        root.addView(home);
        left = s.countdown();
        tick.run();

        root.setFitsSystemWindows(true);
        setContentView(root);
    }

    private void goHome() {
        startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }

    @Override public void onBackPressed() { if (left <= 0) goHome(); }
    @Override protected void onDestroy() { hd.removeCallbacks(tick); super.onDestroy(); }
}
