package com.iffahshield.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Tasbih-style adhkar: tap the big circle to count, it moves on by itself.
 * When opened by the guard (forced), the phone stays locked on this screen until the last dhikr.
 */
public class AdhkarActivity extends Activity {
    private Store s;
    private String key, session;
    private String[][] list;
    private int pos, cnt;
    private boolean forced;
    private TextView title, progressText, body, source, counter;
    private ProgressBar bar;
    private Button prev;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        s = new Store(this);
        L.init(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        setup(getIntent());

        LinearLayout root = Ui.vbox(this);
        root.setBackgroundColor(Ui.BG);
        int p = Ui.dp(this, 16);
        root.setPadding(p, Ui.dp(this, 14), p, Ui.dp(this, 14));

        title = Ui.text(this, "", 22, Ui.TEXT, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));
        progressText = Ui.text(this, "", 13, Ui.MUTED, false);
        progressText.setGravity(Gravity.CENTER);
        root.addView(progressText, new LinearLayout.LayoutParams(-1, -2));
        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(Ui.ACCENT));
        root.addView(bar, new LinearLayout.LayoutParams(-1, Ui.dp(this, 10)));
        root.addView(Ui.gap(this, 8));

        ScrollView sv = new ScrollView(this);
        LinearLayout card = Ui.card(this);
        body = Ui.text(this, "", 21, Ui.TEXT, false);
        body.setGravity(Gravity.CENTER);
        body.setLineSpacing(0, 1.45f);
        card.addView(body, new LinearLayout.LayoutParams(-1, -2));
        source = Ui.text(this, "", 12, Ui.MUTED, false);
        source.setGravity(Gravity.CENTER);
        card.addView(source, new LinearLayout.LayoutParams(-1, -2));
        sv.addView(card);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));

        counter = Ui.text(this, "", 30, Ui.BG, true);
        counter.setGravity(Gravity.CENTER);
        counter.setTextDirection(View.TEXT_DIRECTION_LTR);
        int d = Ui.dp(this, 150);
        counter.setBackground(Ui.round(Ui.ACCENT, d / 2f));
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(d, d);
        cl.gravity = Gravity.CENTER_HORIZONTAL;
        cl.setMargins(0, Ui.dp(this, 10), 0, Ui.dp(this, 6));
        root.addView(counter, cl);
        counter.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { tap(); } });
        TextView hint = Ui.text(this, L.t("اضغط على الدائرة كلما قرأت الذكر مرة", "Tap the circle each time you recite it"), 12, Ui.MUTED, false);
        hint.setGravity(Gravity.CENTER);
        root.addView(hint, new LinearLayout.LayoutParams(-1, -2));

        prev = Ui.button(this, L.t("↩ الذكر السابق", "↩ Previous"), Ui.alpha(Ui.TEXT, 25), Ui.TEXT);
        prev.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            if (pos > 0) { pos--; cnt = 0; save(); show(); }
        } });
        root.addView(prev);
        Button call = Ui.button(this, L.t("📞 اتصال / طوارئ", "📞 Call / emergency"), Ui.alpha(Ui.DANGER, 60), Ui.TEXT);
        call.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            try { startActivity(new Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Exception ignored) { }
        } });
        root.addView(call);

        root.setFitsSystemWindows(true);
        setContentView(root);
        show();
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        String old = key;
        setup(i);
        if (title != null && (old == null || !old.equals(key))) show();
    }

    private void setup(Intent i) {
        String due = s.adhkarDue();
        forced = due != null;
        String want = i == null ? null : i.getStringExtra("session");
        if (due != null) key = due;                       // a due session always wins
        else if (want != null) key = want + ":free";      // reading by choice
        else key = (s.sessionNow() != null && s.sessionNow().startsWith("e") ? "e" : "m") + ":free";
        session = key.substring(0, 1);
        list = Adhkar.of(session);
        pos = forced ? Math.min(s.adhkarPos(key), list.length - 1) : 0;
        cnt = forced ? s.adhkarCount(key) : 0;
    }

    private int need() { return Integer.parseInt(list[pos][1]); }

    private void show() {
        title.setText(Adhkar.title(session));
        progressText.setText((pos + 1) + L.t(" من ", " of ") + list.length + (forced ? L.t("  •  يُفتح الهاتف عند الانتهاء 🔒", "  •  Phone unlocks when you finish 🔒") : ""));
        bar.setMax(list.length);
        bar.setProgress(pos + 1);
        body.setText(list[pos][0]);
        int n = need();
        source.setText((n > 1 ? L.t("يُقال " + n + " مرات  •  ", "Recite " + n + " times  •  ") : "") + list[pos][2]);
        counter.setText(cnt + " / " + n);
        prev.setVisibility(pos > 0 ? View.VISIBLE : View.INVISIBLE);
    }

    private void tap() {
        cnt++;
        try { Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE); if (v != null) v.vibrate(cnt >= need() ? 60 : 15); } catch (Exception ignored) { }
        if (cnt >= need()) {
            if (pos == list.length - 1) { finishAll(); return; }
            pos++; cnt = 0;
        }
        save();
        show();
    }

    private void save() { if (forced) s.saveAdhkar(key, pos, cnt); }

    private void finishAll() {
        if (forced) s.adhkarDone(key);
        Toast.makeText(this, L.t("تقبّل الله منك 🤍 أصبح الهاتف مفتوحاً.", "May Allah accept it from you 🤍 Your phone is unlocked."), Toast.LENGTH_LONG).show();
        startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }

    @Override protected void onResume() { super.onResume(); s.adhkarSeen(); }

    @Override public void onBackPressed() {
        if (forced && s.adhkarDue() != null) {
            Toast.makeText(this, L.t("أكمل الأذكار أولاً، ثم يُفتح الهاتف 🤍", "Finish the adhkar first, then the phone unlocks 🤍"), Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }
}
