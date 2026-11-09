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
            home.setText("خذ نفَساً… " + left);
            home.setAlpha(0.5f);
            left--;
            hd.postDelayed(this, 1000);
        } else { home.setText("رجوع للرئيسية"); home.setAlpha(1f); left = 0; }
    } };

    private static final String[] LINES = {
        "هذه اللحظة ستمضي، وأنت أقوى منها.",
        "كل مرة تقول فيها «لا» تصبح أقوى من سابقتها.",
        "قُل للمؤمنين يغضّوا من أبصارهم ويحفظوا فروجهم ذلك أزكى لهم.",
        "دقائق من المتعة لا تساوي أياماً من الندم.",
        "قم واشرب ماءً، وغيّر مكانك، أو اتصل بمن تحب.",
        "أنت تبني نسخة أفضل من نفسك. واصل.",
        "من ترك شيئاً لله عوّضه الله خيراً منه."
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
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

        TextView title = Ui.text(this, "درع العفّة حماك", 30, Ui.TEXT, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        String reason = getIntent().getStringExtra("reason");
        TextView r = Ui.text(this, reason == null ? "تم الحجب." : reason, 17, Ui.MUTED, false);
        r.setGravity(Gravity.CENTER);
        root.addView(r, new LinearLayout.LayoutParams(-1, -2));
        root.addView(Ui.gap(this, 28));

        LinearLayout card = Ui.card(this);
        String custom = s.customMsg();
        TextView line = Ui.text(this, custom.length() > 0 ? custom : LINES[new Random().nextInt(LINES.length)], 19, Ui.TEXT, true);
        line.setGravity(Gravity.CENTER);
        card.addView(line);
        card.addView(Ui.gap(this, 10));
        TextView streak = Ui.text(this, "لك " + s.streakDays() + " يوماً من النقاء 💪", 16, Ui.ACCENT, true);
        streak.setGravity(Gravity.CENTER);
        card.addView(streak);
        root.addView(card);

        home = Ui.button(this, "رجوع للرئيسية", Ui.ACCENT, Ui.BG);
        home.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { if (left <= 0) goHome(); } });
        root.addView(home);
        left = s.countdown();
        tick.run();

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
