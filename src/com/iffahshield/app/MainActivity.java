package com.iffahshield.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private Store s;
    private LinearLayout content;
    private final Handler h = new Handler();
    private TextView countdown;

    private final Runnable refresher = new Runnable() {
        @Override public void run() {
            if (countdown != null && s.unlockRemaining() >= 0) countdown.setText(fmtRemaining());
            else if (countdown != null) render(); // strict mode just ended: refresh the screen
            h.postDelayed(this, 1000);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        s = new Store(this);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Ui.BG);
        sv.setFillViewport(true);
        sv.setFitsSystemWindows(true);
        content = Ui.vbox(this);
        int p = Ui.dp(this, 16);
        content.setPadding(p, Ui.dp(this, 28), p, Ui.dp(this, 40));
        sv.addView(content);
        setContentView(sv);
    }

    @Override protected void onResume() { super.onResume(); render(); h.post(refresher); maybeDisclosure(); }
    @Override protected void onPause() { super.onPause(); h.removeCallbacks(refresher); }

    private boolean serviceOn() {
        String v = Settings.Secure.getString(getContentResolver(), "enabled_accessibility_services");
        return v != null && v.contains("com.iffahshield.app/");
    }

    private void render() {
        content.removeAllViews();
        countdown = null;
        boolean on = serviceOn();
        boolean strict = s.isStrict();

        // ---- header ----
        LinearLayout head = Ui.hbox(this);
        TextView logo = Ui.text(this, "🛡", 34, Ui.ACCENT, false);
        head.addView(logo);
        LinearLayout titles = Ui.vbox(this);
        titles.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), 0);
        titles.addView(Ui.text(this, "درع العفّة", 28, Ui.TEXT, true));
        titles.addView(Ui.text(this, "حماية مجانية، بلا إنترنت ولا اشتراك", 13, Ui.MUTED, false));
        head.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        TextView pill = Ui.text(this, on ? (strict ? " مقفل 🔒 " : " محمي ✓ ") : " غير مفعّل ", 13,
            on ? Ui.BG : Ui.TEXT, true);
        pill.setBackground(Ui.round(on ? Ui.ACCENT : Ui.DANGER, Ui.dp(this, 20)));
        pill.setPadding(Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 12), Ui.dp(this, 6));
        head.addView(pill);
        content.addView(head);
        content.addView(Ui.gap(this, 14));

        Button panic = Ui.button(this, "🚨 أشعر بالضعف الآن", Ui.DANGER, Ui.BG);
        panic.setTextSize(18);
        panic.setLayoutParams(new LinearLayout.LayoutParams(-1, Ui.dp(this, 62)));
        panic.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            startActivity(new Intent(MainActivity.this, PanicActivity.class));
        } });
        content.addView(panic);
        if (s.inLockdown()) {
            long m = s.lockRemaining() / 60000 + 1;
            TextView lk = Ui.text(this, "🔒 وضع الطوارئ مفعّل، المتبقي " + m + " دقيقة.", 14, Ui.WARN, true);
            content.addView(lk);
        }
        content.addView(Ui.gap(this, 14));

        // ---- setup checklist (public edition) ----
        boolean dnsOn = "hostname".equals(Settings.Global.getString(getContentResolver(), "private_dns_mode"));
        android.app.admin.DevicePolicyManager dpm0 = (android.app.admin.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        boolean admin0 = dpm0 != null && dpm0.isAdminActive(AdminReceiver.component(this));
        int done = (on ? 1 : 0) + (notifOn() ? 1 : 0) + (admin0 ? 1 : 0) + (dnsOn ? 1 : 0) + (strict ? 1 : 0);
        if (done < 5) {
            LinearLayout sc = Ui.card(this);
            sc.addView(Ui.text(this, "خطوات الإعداد (" + done + " من 5)", 18, Ui.TEXT, true));
            sc.addView(step(on, "1. تشغيل الحماية (إمكانية الوصول)", new Runnable() { public void run() {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); } }));
            sc.addView(step(notifOn(), "2. تنظيف الإشعارات (الوصول للإشعارات)", new Runnable() { public void run() {
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")); } }));
            sc.addView(step(admin0, "3. الحماية من الحذف", new Runnable() { public void run() {
                startActivity(new Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    .putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, AdminReceiver.component(MainActivity.this))
                    .putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "بهذا لا يستطيع أحد حذف التطبيق في لحظة ضعف. لا يستخدم التطبيق أي صلاحية أخرى.")); } }));
            sc.addView(step(dnsOn, "4. فلتر المواقع الإباحية لكل الهاتف (DNS)", new Runnable() { public void run() { dnsGuide(); } }));
            sc.addView(step(strict, "5. الوضع الصارم (آخر خطوة)", new Runnable() { public void run() {
                if (serviceOn()) confirmStrict(); else toast("فعّل الخطوة 1 أولاً."); } }));
            content.addView(sc);
        }

        if (!on) {
            LinearLayout c = Ui.card(this);
            c.addView(Ui.text(this, "خطوة واحدة لتشغيل الحماية", 18, Ui.TEXT, true));
            c.addView(Ui.text(this, "افتح «إمكانية الوصول» ← «التطبيقات المثبتة» ← «درع العفّة» وفعّله.\nلا يملك التطبيق إذن الإنترنت، ولا يرسل أو يحفظ أي شيء من شاشتك.", 14, Ui.MUTED, false));
            Button go = Ui.button(this, "فتح إعدادات إمكانية الوصول", Ui.ACCENT, Ui.BG);
            go.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } });
            c.addView(go);
            content.addView(c);
        }

        // ---- stats ----
        LinearLayout stats = Ui.hbox(this);
        stats.addView(stat(String.valueOf(s.streakDays()), "يوماً من النقاء"), new LinearLayout.LayoutParams(0, -2, 1));
        stats.addView(Ui.gap(this, 1), new LinearLayout.LayoutParams(Ui.dp(this, 10), 1));
        stats.addView(stat(String.valueOf(s.todayCount()), "حجب اليوم"), new LinearLayout.LayoutParams(0, -2, 1));
        stats.addView(Ui.gap(this, 1), new LinearLayout.LayoutParams(Ui.dp(this, 10), 1));
        stats.addView(stat(String.valueOf(s.total()), "المجموع"), new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(stats);
        content.addView(Ui.gap(this, 12));

        // ---- protections ----
        LinearLayout prot = Ui.card(this);
        prot.addView(Ui.text(this, "الحماية", 18, Ui.TEXT, true));
        prot.addView(toggle("reels", "حجب الريلز والشورتس", "إنستغرام وفيسبوك ويوتيوب. تبقى الرسائل والمنشورات متاحة. (" + s.cat("reels") + " مرة)"));
        prot.addView(toggle("words", "حجب الكلمات الإباحية", "في أي تطبيق أو متصفح، بالعربية والإنجليزية ولغات أخرى. (" + s.cat("words") + " مرة)"));
        prot.addView(toggle("incognito", "منع التصفح المتخفي", "Chrome وSamsung Internet وغيرها. (" + s.cat("incognito") + " مرة)"));
        prot.addView(toggle("stories", "حجب ستوريات إنستغرام", "اختياري. (" + s.cat("stories") + " مرة)"));
        prot.addView(toggle("notif", "تنظيف الإشعارات", "يحذف إشعارات التطبيقات المحظورة وأي إشعار يحتوي على كلمة محظورة. (" + s.cat("notif") + " مرة)"));
        prot.addView(toggle("newApps", "حظر أي تطبيق جديد تلقائياً", "يُحظر أي تطبيق جديد تلقائياً، ويبقى المحظور محظوراً حتى لو حذفته وأعدت تثبيته."));
        if (!notifOn()) {
            Button na = Ui.button(this, "تفعيل الوصول للإشعارات (لتنظيف الإشعارات)", Ui.alpha(Ui.WARN, 70), Ui.TEXT);
            na.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            } });
            prot.addView(na);
        }
        content.addView(prot);

        // ---- lists ----
        LinearLayout lists = Ui.card(this);
        lists.addView(Ui.text(this, "القوائم", 18, Ui.TEXT, true));
        Button kw = Ui.button(this, "الكلمات المحظورة (" + (BuiltinWords.LIST.length + s.keywords().size()) + ")", Ui.ACCENT_DARK, Ui.TEXT);
        kw.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { editKeywords(); } });
        lists.addView(kw);
        Button ap = Ui.button(this, "التطبيقات المحظورة (" + s.apps().size() + ")", Ui.ACCENT_DARK, Ui.TEXT);
        ap.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { editApps(); } });
        lists.addView(ap);
        Button lim = Ui.button(this, "حدود الوقت اليومية (" + s.limits().size() + ")", Ui.ACCENT_DARK, Ui.TEXT);
        lim.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { editLimits(); } });
        lists.addView(lim);
        Button scr = Ui.button(this, "شاشة الحجب (عدّاد " + s.countdown() + " ثواني)", Ui.ACCENT_DARK, Ui.TEXT);
        scr.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { editBlockScreen(); } });
        lists.addView(scr);
        if (strict) lists.addView(Ui.text(this, "في الوضع الصارم يمكنك التشديد فقط: الإضافة، أو تقليل الوقت، أو إطالة العدّاد. لا يمكنك التخفيف.", 13, Ui.WARN, false));
        content.addView(lists);

        // ---- uninstall protection (device admin) ----
        final android.app.admin.DevicePolicyManager dpm = (android.app.admin.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        boolean adminOn = dpm != null && dpm.isAdminActive(AdminReceiver.component(this));
        LinearLayout ad = Ui.card(this);
        ad.addView(Ui.text(this, "حماية من الحذف 🛡", 18, Ui.TEXT, true));
        if (adminOn) {
            ad.addView(Ui.text(this, "مفعّلة. سيرفض أندرويد نفسه حذف التطبيق، حتى من الكمبيوتر.", 14, Ui.ACCENT, false));
        } else {
            ad.addView(Ui.text(this, "فعّلها ليصبح حذف التطبيق مستحيلاً من الهاتف أو من الكمبيوتر، إلا بعد إيقاف الوضع الصارم بالطريقة الرسمية.", 14, Ui.MUTED, false));
            Button ab = Ui.button(this, "تفعيل الحماية من الحذف", Ui.WARN, Ui.BG);
            ab.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
                Intent i = new Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    .putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, AdminReceiver.component(MainActivity.this))
                    .putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "بهذا لا يستطيع أحد حذف التطبيق، ولا حتى أنت في لحظة ضعف. لا يستخدم التطبيق أي صلاحية أخرى.");
                startActivity(i);
            } });
            ad.addView(ab);
        }
        content.addView(ad);

        // ---- strict mode ----
        LinearLayout st = Ui.card(this);
        st.addView(Ui.text(this, "الوضع الصارم 🔒", 18, Ui.TEXT, true));
        if (!strict) {
            st.addView(Ui.text(this, "يقفل كل شيء: لا يمكنك إيقاف الحماية أو حذف التطبيق، ولا تغيير DNS الخاص أو إعدادات الـVPN. لإيقافه يجب أن تطلب ذلك وتنتظر 24 ساعة والهاتف قيد التشغيل.", 14, Ui.MUTED, false));
            Button en = Ui.button(this, "تفعيل الوضع الصارم", Ui.WARN, Ui.BG);
            en.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { confirmStrict(); } });
            if (!on) { en.setEnabled(false); en.setAlpha(0.5f); }
            st.addView(en);
        } else if (s.unlockRemaining() < 0) {
            st.addView(Ui.text(this, "مفعّل. الحماية مقفلة ولا يمكن إيقافها من الإعدادات.", 14, Ui.ACCENT, false));
            if (s.hasPartner()) {
                Button pu = Ui.button(this, "إيقاف فوري بكود الشريك", Ui.alpha(Ui.WARN, 60), Ui.TEXT);
                pu.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { partnerUnlock(); } });
                st.addView(pu);
            }
            Button rq = Ui.button(this, "طلب إيقاف (بعد 24 ساعة)", Ui.alpha(Ui.DANGER, 60), Ui.TEXT);
            rq.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { confirmUnlock(); } });
            st.addView(rq);
        } else {
            st.addView(Ui.text(this, "طلبتَ الإيقاف. الوقت المتبقي:", 14, Ui.MUTED, false));
            countdown = Ui.text(this, fmtRemaining(), 30, Ui.WARN, true);
            countdown.setGravity(Gravity.CENTER);
            st.addView(countdown);
            st.addView(Ui.text(this, "يُحتسب الوقت فقط والهاتف قيد التشغيل، وتغيير الساعة لا يفيد.", 13, Ui.MUTED, false));
            Button cancel = Ui.button(this, "تراجعت، ألغِ الطلب 💪", Ui.ACCENT, Ui.BG);
            cancel.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
                s.cancelUnlock(); toast("أحسنت! الحماية مستمرة."); render();
            } });
            st.addView(cancel);
        }
        content.addView(st);

        // ---- personal: reasons + friend ----
        LinearLayout me = Ui.card(this);
        me.addView(Ui.text(this, "أسبابي وصديقي الموثوق", 18, Ui.TEXT, true));
        me.addView(Ui.text(this, s.reasons().length() > 0 ? s.reasons() : "اكتب لماذا تريد التوقف. ستظهر لك في لحظة الضعف.", 14,
            s.reasons().length() > 0 ? Ui.TEXT : Ui.MUTED, false));
        Button rs = Ui.button(this, "✍️ اكتب أسبابي", Ui.ACCENT_DARK, Ui.TEXT);
        rs.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            final EditText e = new EditText(MainActivity.this);
            e.setText(s.reasons()); e.setMinLines(3); e.setTextDirection(View.TEXT_DIRECTION_RTL);
            e.setHint("مثلاً: أريد قلباً نقياً، أريد أن أحترم نفسي، أريد أن أتزوج وأنا نقي…");
            new AlertDialog.Builder(MainActivity.this).setTitle("لماذا تريد التوقف؟").setView(e)
                .setPositiveButton("حفظ", new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                    s.setReasons(e.getText().toString()); render(); } })
                .setNegativeButton("إلغاء", null).show();
        } });
        me.addView(rs);
        Button fp = Ui.button(this, s.friendPhone().length() > 0 ? "📞 رقم صديقي: " + s.friendPhone() : "📞 أضف رقم صديق موثوق", Ui.ACCENT_DARK, Ui.TEXT);
        fp.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            final EditText e = new EditText(MainActivity.this);
            e.setInputType(InputType.TYPE_CLASS_PHONE); e.setText(s.friendPhone());
            new AlertDialog.Builder(MainActivity.this).setTitle("رقم صديق تتصل به وقت الضعف").setView(e)
                .setPositiveButton("حفظ", new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                    s.setFriendPhone(e.getText().toString()); render(); } })
                .setNegativeButton("إلغاء", null).show();
        } });
        me.addView(fp);
        content.addView(me);

        // ---- accountability partner ----
        LinearLayout pc = Ui.card(this);
        pc.addView(Ui.text(this, "شريك المحاسبة 🤝", 18, Ui.TEXT, true));
        pc.addView(Ui.text(this, s.hasPartner()
            ? "لدى شريكك رمز سري يمكنه به إيقاف الوضع الصارم فوراً بدل الـ24 ساعة، إن اقتنع هو بذلك."
            : "اطلب من صديق موثوق أن يكتب رمزاً سرياً هنا بنفسه دون أن يخبرك به. عندها لا يمكن الإيقاف الفوري إلا عن طريقه.", 14, Ui.MUTED, false));
        if (!s.hasPartner() && strict) {
            pc.addView(Ui.text(this, "لا يمكن إضافة شريك أثناء الوضع الصارم، حتى لا يُستخدم لإيقاف الحماية. الطريق الوحيد هو طلب الـ24 ساعة.", 13, Ui.WARN, false));
        } else {
            Button pb = Ui.button(this, s.hasPartner() ? "تغيير رمز الشريك (يتطلب الرمز القديم)" : "يكتب الشريك الرمز", Ui.ACCENT_DARK, Ui.TEXT);
            pb.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { partnerSetup(); } });
            pc.addView(pb);
        }
        content.addView(pc);

        // ---- streak reset ----
        LinearLayout sr = Ui.card(this);
        sr.addView(Ui.text(this, "أطول مدة: " + s.bestStreak() + " يوم", 14, Ui.MUTED, false));
        Button reset = Ui.button(this, "ابدأ العدّاد من جديد", Ui.alpha(Ui.TEXT, 25), Ui.TEXT);
        reset.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            new AlertDialog.Builder(MainActivity.this)
                .setMessage("الخطأ وارد، والمهم أن تعود. هل نبدأ العدّاد من الصفر؟")
                .setPositiveButton("نعم", new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                    s.saveBest(); s.resetStreak(); render(); } })
                .setNegativeButton("لا", null).show();
        } });
        sr.addView(reset);
        content.addView(sr);

        TextView foot = Ui.text(this, "لا يملك التطبيق إذن الإنترنت. كل شيء يبقى على هاتفك.", 12, Ui.MUTED, false);
        foot.setGravity(Gravity.CENTER);
        content.addView(foot, new LinearLayout.LayoutParams(-1, -2));
        TextView ab = Ui.text(this, "عن التطبيق والخصوصية", 13, Ui.ACCENT, true);
        ab.setGravity(Gravity.CENTER);
        ab.setPadding(0, Ui.dp(this, 10), 0, 0);
        ab.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { about(); } });
        content.addView(ab, new LinearLayout.LayoutParams(-1, -2));
    }

    private View step(boolean ok, String label, final Runnable action) {
        LinearLayout row = Ui.hbox(this);
        row.setPadding(0, Ui.dp(this, 10), 0, 0);
        TextView mark = Ui.text(this, ok ? "✅" : "⬜", 18, Ui.TEXT, false);
        row.addView(mark);
        TextView t = Ui.text(this, "  " + label, 15, ok ? Ui.MUTED : Ui.TEXT, !ok);
        row.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        if (!ok) {
            TextView go = Ui.text(this, " ابدأ ", 14, Ui.BG, true);
            go.setBackground(Ui.round(Ui.ACCENT, Ui.dp(this, 12)));
            go.setPadding(Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 12), Ui.dp(this, 6));
            go.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
                try { action.run(); } catch (Exception e) { toast("افتح الإعدادات يدوياً من فضلك."); } } });
            row.addView(go);
        }
        return row;
    }

    static final String DNS_HOST = "family-filter-dns.cleanbrowsing.org";

    private void dnsGuide() {
        new AlertDialog.Builder(this).setTitle("فلتر المواقع الإباحية (مجاني)")
            .setMessage("هذا الفلتر يحجب آلاف المواقع الإباحية في كل الهاتف، ويفرض البحث الآمن في Google وYouTube، ويعمل حتى لو توقف التطبيق.\n\n"
                + "الخطوات:\n1. اضغط «نسخ العنوان».\n2. اضغط «فتح الإعدادات».\n3. ابحث في الإعدادات عن «DNS الخاص» (Private DNS).\n"
                + "4. اختر «اسم مضيف موفر DNS الخاص» والصق العنوان:\n\n" + DNS_HOST + "\n\n5. احفظ.\n\nالخدمة من CleanBrowsing، مجانية للعائلات.")
            .setPositiveButton("نسخ العنوان", new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (cm != null) cm.setPrimaryClip(android.content.ClipData.newPlainText("dns", DNS_HOST));
                toast("تم النسخ ✓ الآن افتح الإعدادات.");
                dnsGuide();
            } })
            .setNeutralButton("فتح الإعدادات", new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                try { startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)); }
                catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            } })
            .setNegativeButton("إغلاق", null).show();
    }

    private boolean disclosureShowing = false;

    private void maybeDisclosure() {
        final android.content.SharedPreferences sp = getSharedPreferences("ui", MODE_PRIVATE);
        if (sp.getBoolean("disclosed", false) || disclosureShowing) return;
        disclosureShowing = true;
        new AlertDialog.Builder(this).setCancelable(false).setTitle("قبل أن تبدأ: خصوصيتك")
            .setMessage("يستخدم «درع العفّة» خدمة «إمكانية الوصول» في أندرويد لقراءة النصوص الظاهرة على الشاشة، فقط ليعرف متى يحجب المحتوى الإباحي والريلز ويحمي إعداداته.\n\n"
                + "• لا يملك التطبيق إذن الإنترنت إطلاقاً.\n• لا يرسل ولا يحفظ أي نص أو صورة من شاشتك.\n• يحفظ على هاتفك فقط: إعداداتك وقوائمك، وأعداد مرات الحجب، ووقت استخدام التطبيقات المحدودة، وأسبابك ورقم صديقك إن أضفتهما.\n"
                + "• الحماية من الحذف تستخدم صلاحية «مسؤول الجهاز» لمنع الحذف فقط، دون أي صلاحية أخرى.\n\nبالضغط على «موافق» فأنت توافق على ذلك.")
            .setPositiveButton("موافق", new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                sp.edit().putBoolean("disclosed", true).apply(); } })
            .setNegativeButton("خروج", new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) { finish(); } })
            .show();
    }

    private void about() {
        new AlertDialog.Builder(this).setTitle("عن درع العفّة")
            .setMessage("تطبيق مجاني ومفتوح المصدر، صدقة جارية لحماية الشباب والفتيات من الإباحية.\n\n"
                + "• بلا إنترنت، بلا إعلانات، بلا اشتراكات، بلا جمع بيانات.\n• يحجب الكلمات والمواقع والريلز والشورتس والتصفح المتخفي.\n"
                + "• وضع صارم وحماية من الحذف وزر طوارئ.\n\nإن نفعك فادعُ لمن صنعه، وانشره لغيرك فالدال على الخير كفاعله.")
            .setPositiveButton("حسناً", null).show();
    }

    private EditText codeField(String hint) {
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        e.setHint(hint);
        return e;
    }

    private void partnerSetup() {
        LinearLayout l = Ui.vbox(this);
        int p = Ui.dp(this, 20); l.setPadding(p, 0, p, 0);
        final EditText old = codeField("الرمز القديم");
        final EditText c1 = codeField("رمز جديد (6 أرقام أو أكثر)");
        final EditText c2 = codeField("أعد الكود");
        if (s.hasPartner()) l.addView(old);
        l.addView(c1); l.addView(c2);
        new AlertDialog.Builder(this).setTitle("هذه الخطوة لشريكك، لا لك").setView(l)
            .setMessage("سلّم الهاتف لشريكك ليكتب الرمز بنفسه. لا يحفظ التطبيق الرمز نفسه، بل بصمة مشفّرة منه.")
            .setPositiveButton("حفظ", new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                String a = c1.getText().toString(), b2 = c2.getText().toString();
                if (a.length() < 6 || !a.equals(b2)) { toast("يجب أن يتكون الرمز من 6 أرقام أو أكثر، وأن يتطابق الحقلان."); return; }
                if (s.setPartnerCode(old.getText().toString(), a)) toast("تم حفظ رمز الشريك ✓");
                else toast("الرمز القديم غير صحيح.");
                render();
            } })
            .setNegativeButton("إلغاء", null).show();
    }

    private void partnerUnlock() {
        final EditText c = codeField("رمز الشريك");
        new AlertDialog.Builder(this).setTitle("إيقاف الوضع الصارم فوراً").setView(c)
            .setMessage("شريكك وحده يعرف الرمز. بعد 3 محاولات خاطئة يُقفل لمدة نصف ساعة.")
            .setPositiveButton("تأكيد", new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                if (s.checkPartner(c.getText().toString())) { s.forceStrictOff(); toast("تم إيقاف الوضع الصارم."); }
                else toast("الرمز غير صحيح، أو عليك الانتظار قليلاً.");
                render();
            } })
            .setNegativeButton("إلغاء", null).show();
    }

    private boolean notifOn() {
        String v = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return v != null && v.contains("com.iffahshield.app/");
    }

    /** Launcher apps sorted by label: [label, pkg]. */
    private List<String[]> launcherApps() {
        final PackageManager pm = getPackageManager();
        List<ResolveInfo> ris = pm.queryIntentActivities(
            new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        List<String[]> apps = new ArrayList<String[]>();
        List<String> seen = new ArrayList<String>();
        for (ResolveInfo r : ris) {
            String pkg = r.activityInfo.packageName;
            if (seen.contains(pkg) || GuardService.NEVER_BLOCK.contains(pkg)) continue;
            seen.add(pkg);
            apps.add(new String[] { r.loadLabel(pm).toString(), pkg });
        }
        Collections.sort(apps, new Comparator<String[]>() { @Override public int compare(String[] a, String[] b2) { return a[0].compareToIgnoreCase(b2[0]); } });
        return apps;
    }

    private String labelOf(String pkg) {
        try { return getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg, 0)).toString(); }
        catch (Exception e) { return pkg; }
    }

    private void editLimits() {
        final List<String> pkgs = new ArrayList<String>(s.limits().keySet());
        final String[] items = new String[pkgs.size()];
        for (int i = 0; i < items.length; i++) {
            String pk = pkgs.get(i);
            items[i] = labelOf(pk) + " — " + (s.usedToday(pk) / 60000) + " من " + s.limits().get(pk) + " دقيقة اليوم";
        }
        new AlertDialog.Builder(this).setTitle("حدود الوقت اليومية")
            .setItems(items, new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int i) {
                pickMinutes(pkgs.get(i), true);
            } })
            .setPositiveButton("إضافة تطبيق", new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                final List<String[]> apps = launcherApps();
                String[] labels = new String[apps.size()];
                for (int i = 0; i < labels.length; i++) labels[i] = apps.get(i)[0];
                new AlertDialog.Builder(MainActivity.this).setTitle("اختر التطبيق")
                    .setItems(labels, new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d2, int i) {
                        pickMinutes(apps.get(i)[1], false);
                    } }).show();
            } })
            .setNegativeButton("إغلاق", null).show();
    }

    private void pickMinutes(final String pkg, boolean existing) {
        final int[] mins = { 10, 15, 30, 45, 60, 90, 120 };
        String[] labels = new String[mins.length + (existing ? 1 : 0)];
        for (int i = 0; i < mins.length; i++) labels[i] = mins[i] + " دقيقة باليوم";
        if (existing) labels[mins.length] = "إزالة الحد";
        new AlertDialog.Builder(this).setTitle(labelOf(pkg))
            .setItems(labels, new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int i) {
                int m = i < mins.length ? mins[i] : 0;
                if (!s.setLimit(pkg, m)) toast("في الوضع الصارم يمكنك تقليل الوقت فقط.");
                render();
            } }).show();
    }

    private void editBlockScreen() {
        final int[] secs = { 0, 5, 10, 20, 30, 60 };
        String[] labels = new String[secs.length + 1];
        for (int i = 0; i < secs.length; i++) labels[i] = "عدّاد " + secs[i] + " ثانية قبل إمكانية الإغلاق";
        labels[secs.length] = "رسالتي الخاصة على شاشة الحجب";
        new AlertDialog.Builder(this).setTitle("شاشة الحجب")
            .setItems(labels, new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int i) {
                if (i < secs.length) {
                    if (!s.setCountdown(secs[i])) toast("في الوضع الصارم يمكنك إطالة العدّاد فقط.");
                    render();
                    return;
                }
                final EditText e = new EditText(MainActivity.this);
                e.setText(s.customMsg());
                e.setHint("مثلاً: تذكّر لماذا بدأت");
                e.setTextDirection(View.TEXT_DIRECTION_RTL);
                new AlertDialog.Builder(MainActivity.this).setTitle("رسالتك إلى نفسك").setView(e)
                    .setMessage("تظهر على شاشة الحجب بدل العبارات الجاهزة. اتركها فارغة لاستعادة العبارات الجاهزة.")
                    .setPositiveButton("حفظ", new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d2, int w) {
                        s.setCustomMsg(e.getText().toString()); toast("تم الحفظ ✓"); } })
                    .setNegativeButton("إلغاء", null).show();
            } }).show();
    }

    private View stat(String num, String label) {
        LinearLayout c = Ui.vbox(this);
        c.setBackground(Ui.round(Ui.CARD, Ui.dp(this, 16)));
        c.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 14));
        c.setGravity(Gravity.CENTER);
        TextView n = Ui.text(this, num, 26, Ui.ACCENT, true); n.setGravity(Gravity.CENTER);
        TextView l = Ui.text(this, label, 12, Ui.MUTED, false); l.setGravity(Gravity.CENTER);
        c.addView(n, new LinearLayout.LayoutParams(-1, -2));
        c.addView(l, new LinearLayout.LayoutParams(-1, -2));
        return c;
    }

    private View toggle(final String key, String title, String sub) {
        LinearLayout row = Ui.hbox(this);
        row.setPadding(0, Ui.dp(this, 12), 0, 0);
        LinearLayout t = Ui.vbox(this);
        t.addView(Ui.text(this, title, 16, Ui.TEXT, true));
        t.addView(Ui.text(this, sub, 12, Ui.MUTED, false));
        row.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        final Switch sw = new Switch(this);
        sw.setChecked(s.flag(key));
        sw.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            if (!s.setFlag(key, sw.isChecked())) {
                sw.setChecked(true);
                toast("الوضع الصارم مفعّل، لا يمكنك إيقاف الحماية.");
            }
        } });
        row.addView(sw);
        return row;
    }

    private void editKeywords() {
        final List<String> words = s.keywords();
        final String[] items = new String[words.size()];
        for (int i = 0; i < items.length; i++) {
            String w = words.get(i);
            items[i] = w.startsWith("=") ? w.substring(1) + "  (كلمة كاملة)" : w;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(this)
            .setTitle(items.length == 0 ? "يوجد " + BuiltinWords.LIST.length + " كلمة مدمجة (مخفية لحمايتك). أضف كلماتك:"
                                        : "كلماتك (+" + BuiltinWords.LIST.length + " كلمة مدمجة مخفية)")
            .setItems(items, new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, final int i) {
                if (s.isStrict()) { toast("لا يمكن حذف الكلمات في الوضع الصارم."); return; }
                new AlertDialog.Builder(MainActivity.this).setMessage("هل تحذف «" + items[i] + "»؟")
                    .setPositiveButton("حذف", new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d2, int w) {
                        s.removeKeyword(words.get(i)); GuardService.matcherVersion++; render(); } })
                    .setNegativeButton("لا", null).show();
            } })
            .setPositiveButton("إضافة كلمة", new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) { addKeyword(); } })
            .setNegativeButton("إغلاق", null);
        b.show();
    }

    private void addKeyword() {
        final EditText e = new EditText(this);
        e.setHint("مثلاً: كلمة أو اسم موقع");
        e.setInputType(InputType.TYPE_CLASS_TEXT);
        e.setTextDirection(View.TEXT_DIRECTION_RTL);
        new AlertDialog.Builder(this).setTitle("إضافة كلمة").setView(e)
            .setMessage("ستُحجب أي صفحة أو شاشة تحتوي على هذه الكلمة.")
            .setPositiveButton("إضافة", new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                if (s.addKeyword(e.getText().toString())) { GuardService.matcherVersion++; toast("تمت الإضافة ✓"); }
                else toast("الكلمة يجب أن تكون 3 أحرف على الأقل، وغير مكررة.");
                render();
            } })
            .setNegativeButton("إلغاء", null).show();
    }

    private void editApps() {
        final List<String[]> apps = launcherApps();
        final List<String> blocked = s.apps();
        final String[] labels = new String[apps.size()];
        final boolean[] checked = new boolean[apps.size()];
        for (int i = 0; i < labels.length; i++) { labels[i] = apps.get(i)[0]; checked[i] = blocked.contains(apps.get(i)[1]); }
        new AlertDialog.Builder(this).setTitle("اختر التطبيقات التي تريد حظرها")
            .setMultiChoiceItems(labels, checked, new DialogInterface.OnMultiChoiceClickListener() {
                @Override public void onClick(DialogInterface d, int i, boolean c) {
                    String pkg = apps.get(i)[1];
                    if (c) s.addApp(pkg);
                    else if (!s.removeApp(pkg)) {
                        ((AlertDialog) d).getListView().setItemChecked(i, true); checked[i] = true;
                        toast("لا يمكن إلغاء الحظر في الوضع الصارم.");
                    }
                } })
            .setPositiveButton("تم", new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) { render(); } })
            .show();
    }

    private void confirmStrict() {
        new AlertDialog.Builder(this).setTitle("تفعيل الوضع الصارم؟")
            .setMessage("بعد التفعيل:\n• لا يمكنك إيقاف أي حماية.\n• لا يمكنك حذف التطبيق.\n• تُقفل صفحات DNS الخاص والـVPN الدائم وإعادة ضبط المصنع والمجلد الآمن.\n• للإيقاف يجب أن تطلب وتنتظر 24 ساعة.\n\nهل أنت متأكد؟")
            .setPositiveButton("فعّل، أنا قادر 💪", new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                s.enableStrict(); toast("الوضع الصارم مفعّل 🔒"); render(); } })
            .setNegativeButton("ليس الآن", null).show();
    }

    private void confirmUnlock() {
        new AlertDialog.Builder(this).setTitle("هل أنت متأكد؟")
            .setMessage("إن كانت لحظة ضعف فستمضي. اشرب ماءً، واخرج قليلاً، ثم عد.\n\nإن كنت ما زلت تريد ذلك، سيبدأ عدّاد مدته 24 ساعة، ويمكنك إلغاؤه في أي وقت.")
            .setPositiveButton("ابدأ العدّاد", new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                s.requestUnlock(); render(); } })
            .setNegativeButton("لا، أبقِه مقفلاً 💪", null).show();
    }

    private String fmtRemaining() {
        long ms = Math.max(0, s.liveRemaining());
        long hh = ms / 3600000, mm = (ms / 60000) % 60, ss = (ms / 1000) % 60;
        return String.format(java.util.Locale.US, "%02d:%02d:%02d", hh, mm, ss);
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}
