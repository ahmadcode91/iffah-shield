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
        L.init(this);
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
        titles.addView(Ui.text(this, L.t("درع العفّة", "Iffah Shield"), 28, Ui.TEXT, true));
        titles.addView(Ui.text(this, L.t("حماية مجانية، بلا إنترنت ولا اشتراك", "Free protection, no internet, no subscription"), 13, Ui.MUTED, false));
        head.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        TextView pill = Ui.text(this, on ? (strict ? L.t(" مقفل 🔒 ", " Locked 🔒 ") : L.t(" محمي ✓ ", " Protected ✓ ")) : L.t(" غير مفعّل ", " Not active "), 13,
            on ? Ui.BG : Ui.TEXT, true);
        pill.setBackground(Ui.round(on ? Ui.ACCENT : Ui.DANGER, Ui.dp(this, 20)));
        pill.setPadding(Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 12), Ui.dp(this, 6));
        head.addView(pill);
        content.addView(head);
        content.addView(Ui.gap(this, 14));

        Button panic = Ui.button(this, L.t("🚨 أشعر بالضعف الآن", "🚨 I feel weak right now"), Ui.DANGER, Ui.BG);
        panic.setTextSize(18);
        panic.setLayoutParams(new LinearLayout.LayoutParams(-1, Ui.dp(this, 62)));
        panic.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            startActivity(new Intent(MainActivity.this, PanicActivity.class));
        } });
        content.addView(panic);
        if (s.inLockdown()) {
            long m = s.lockRemaining() / 60000 + 1;
            TextView lk = Ui.text(this, L.t("🔒 وضع الطوارئ مفعّل، المتبقي ", "🔒 Emergency lock is on, remaining ") + m + L.t(" دقيقة.", " min."), 14, Ui.WARN, true);
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
            sc.addView(Ui.text(this, L.t("خطوات الإعداد (", "Setup steps (") + done + L.t(" من 5)", " of 5)"), 18, Ui.TEXT, true));
            sc.addView(step(on, L.t("1. تشغيل الحماية (إمكانية الوصول)", "1. Turn on protection (Accessibility)"), new Runnable() { public void run() {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); } }));
            sc.addView(step(notifOn(), L.t("2. تنظيف الإشعارات (الوصول للإشعارات)", "2. Clean notifications (Notification access)"), new Runnable() { public void run() {
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")); } }));
            sc.addView(step(admin0, L.t("3. الحماية من الحذف", "3. Uninstall protection"), new Runnable() { public void run() {
                startActivity(new Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    .putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, AdminReceiver.component(MainActivity.this))
                    .putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        L.t("بهذا لا يستطيع أحد حذف التطبيق في لحظة ضعف. لا يستخدم التطبيق أي صلاحية أخرى.", "This way no one can uninstall the app in a weak moment. The app uses no other permission."))); } }));
            sc.addView(step(dnsOn, L.t("4. فلتر المواقع الإباحية لكل الهاتف (DNS)", "4. Phone-wide adult-site filter (DNS)"), new Runnable() { public void run() { dnsGuide(); } }));
            sc.addView(step(strict, L.t("5. الوضع الصارم (آخر خطوة)", "5. Strict mode (last step)"), new Runnable() { public void run() {
                if (serviceOn()) confirmStrict(); else toast(L.t("فعّل الخطوة 1 أولاً.", "Do step 1 first.")); } }));
            content.addView(sc);
        }

        if (!on) {
            LinearLayout c = Ui.card(this);
            c.addView(Ui.text(this, L.t("خطوة واحدة لتشغيل الحماية", "One step to turn on protection"), 18, Ui.TEXT, true));
            c.addView(Ui.text(this, L.t("افتح «إمكانية الوصول» ← «التطبيقات المثبتة» ← «درع العفّة» وفعّله.\nلا يملك التطبيق إذن الإنترنت، ولا يرسل أو يحفظ أي شيء من شاشتك.", "Open Accessibility → Installed apps → Iffah Shield and turn it on.\nThe app has no internet permission and never sends or stores anything from your screen."), 14, Ui.MUTED, false));
            Button go = Ui.button(this, L.t("فتح إعدادات إمكانية الوصول", "Open Accessibility settings"), Ui.ACCENT, Ui.BG);
            go.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } });
            c.addView(go);
            content.addView(c);
        }

        // ---- stats ----
        LinearLayout stats = Ui.hbox(this);
        stats.addView(stat(String.valueOf(s.streakDays()), L.t("يوماً من النقاء", "clean days")), new LinearLayout.LayoutParams(0, -2, 1));
        stats.addView(Ui.gap(this, 1), new LinearLayout.LayoutParams(Ui.dp(this, 10), 1));
        stats.addView(stat(String.valueOf(s.todayCount()), L.t("حجب اليوم", "blocked today")), new LinearLayout.LayoutParams(0, -2, 1));
        stats.addView(Ui.gap(this, 1), new LinearLayout.LayoutParams(Ui.dp(this, 10), 1));
        stats.addView(stat(String.valueOf(s.total()), L.t("المجموع", "total")), new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(stats);
        content.addView(Ui.gap(this, 12));
        content.addView(RecoveryActivity.medals(this, s));

        // ---- tamper log ----
        if (s.tamperLog().length() > 0) {
            LinearLayout tl = Ui.card(this);
            tl.addView(Ui.text(this, L.t("🚨 سجل العبث (", "🚨 Tamper log (") + s.tamperCount() + ")", 17, Ui.DANGER, true));
            tl.addView(Ui.text(this, L.t("كل مرة توقفت فيها الحماية والهاتف يعمل تُسجَّل هنا ولا تُمسح. شريك المحاسبة يستطيع رؤيتها.", "Every time protection stopped while the phone was in use is recorded here and cannot be cleared. Your accountability partner can see it."), 12, Ui.MUTED, false));
            tl.addView(Ui.text(this, s.tamperLog(), 13, Ui.TEXT, false));
            content.addView(tl);
        }

        // ---- adhkar + recovery ----
        LinearLayout dz = Ui.card(this);
        dz.addView(Ui.text(this, L.t("الأذكار والتعافي", "Adhkar & recovery"), 18, Ui.TEXT, true));
        dz.addView(toggle("adhkar", L.t("أذكار الصباح والمساء قبل فتح الهاتف", "Morning & evening adhkar before using the phone"),
            L.t("في الوقت المحدد يُقفل الهاتف على الأذكار حتى تكملها (الاتصال متاح دائماً). الصباح " + hm(s.morningStart()) + " والمساء " + hm(s.eveningStart()) + ". أكملتها " + s.adhkarTotal() + " مرة.",
                "At the set time the phone opens only the adhkar until you finish (calls always work). Morning " + hm(s.morningStart()) + ", evening " + hm(s.eveningStart()) + ". Completed " + s.adhkarTotal() + " times.")));
        Button times = Ui.button(this, L.t("⏰ تغيير أوقات الأذكار", "⏰ Change adhkar times"), Ui.alpha(Ui.TEXT, 25), Ui.TEXT);
        times.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { pickAdhkarTimes(); } });
        dz.addView(times);
        LinearLayout adk = Ui.hbox(this);
        Button am = Ui.button(this, L.t("☀️ أذكار الصباح", "☀️ Morning"), Ui.ACCENT_DARK, Ui.TEXT);
        Button ae = Ui.button(this, L.t("🌙 أذكار المساء", "🌙 Evening"), Ui.ACCENT_DARK, Ui.TEXT);
        am.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            startActivity(new Intent(MainActivity.this, AdhkarActivity.class).putExtra("session", "m")); } });
        ae.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            startActivity(new Intent(MainActivity.this, AdhkarActivity.class).putExtra("session", "e")); } });
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, Ui.dp(this, 50), 1);
        half.setMargins(Ui.dp(this, 3), Ui.dp(this, 8), Ui.dp(this, 3), 0);
        adk.addView(am, half);
        adk.addView(ae, new LinearLayout.LayoutParams(half));
        dz.addView(adk);
        Button plan = Ui.button(this, L.t("🌱 خطة التعافي", "🌱 Recovery plan"), Ui.ACCENT, Ui.BG);
        plan.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            startActivity(new Intent(MainActivity.this, RecoveryActivity.class)); } });
        dz.addView(plan);
        dz.addView(nightRow());
        content.addView(dz);

        // ---- protections ----
        LinearLayout prot = Ui.card(this);
        prot.addView(Ui.text(this, L.t("الحماية", "Protection"), 18, Ui.TEXT, true));
        prot.addView(toggle("reels", L.t("حجب الريلز والشورتس", "Block Reels & Shorts"), L.t("إنستغرام وفيسبوك ويوتيوب. تبقى الرسائل والمنشورات متاحة. (", "Instagram, Facebook and YouTube. Messages and posts still work. (") + s.cat("reels") + L.t(" مرة)", "×)")));
        prot.addView(toggle("words", L.t("حجب الكلمات الإباحية", "Block adult words"), L.t("في أي تطبيق أو متصفح، بالعربية والإنجليزية ولغات أخرى. (", "In any app or browser, in Arabic, English and other languages. (") + s.cat("words") + L.t(" مرة)", "×)")));
        if (android.os.Build.VERSION.SDK_INT >= 30) prot.addView(toggle("images", L.t("كشف الصور بالذكاء الاصطناعي 👁", "AI image detection 👁"), L.t("يفحص الشاشة نفسها ويحجب الصور والمقاطع العارية حتى لو لم يكن فيها أي كلمة (كالمولَّدة بالذكاء الاصطناعي). كل شيء يتم داخل الهاتف، ولا تُحفظ أي صورة ولا تُرسل. (" + s.cat("image") + " مرة)", "Looks at the screen itself and blocks nude images and videos even with no words in them (such as AI-generated ones). Everything runs on the phone; no image is saved or sent. (" + s.cat("image") + "×)")));
        prot.addView(toggle("safesearch", L.t("البحث الآمن إجباري", "Force safe search"), L.t("يفتح Google وBing وDuckDuckGo وYahoo دائماً بالوضع الآمن. (" + s.cat("safesearch") + " مرة)", "Google, Bing, DuckDuckGo and Yahoo always open in safe mode. (" + s.cat("safesearch") + "×)")));
        prot.addView(toggle("newBrowsers", L.t("حظر المتصفحات غير المعروفة", "Block unknown browsers"), L.t("يُحظر تلقائياً أي متصفح غير المتصفحات المعروفة، حتى لا يكون باباً للالتفاف.", "Any browser other than the known ones is blocked automatically, so it can't be used to get around protection.")));
        prot.addView(toggle("incognito", L.t("منع التصفح المتخفي", "Block incognito browsing"), L.t("Chrome وSamsung Internet وغيرها. (", "Chrome, Samsung Internet and others. (") + s.cat("incognito") + L.t(" مرة)", "×)")));
        prot.addView(toggle("stories", L.t("حجب ستوريات إنستغرام", "Block Instagram Stories"), L.t("اختياري. (", "Optional. (") + s.cat("stories") + L.t(" مرة)", "×)")));
        prot.addView(toggle("notif", L.t("تنظيف الإشعارات", "Clean notifications"), L.t("يحذف إشعارات التطبيقات المحظورة وأي إشعار يحتوي على كلمة محظورة. (", "Removes notifications from blocked apps and any notification containing a blocked word. (") + s.cat("notif") + L.t(" مرة)", "×)")));
        prot.addView(toggle("newApps", L.t("حظر أي تطبيق جديد تلقائياً", "Block every new app automatically"), L.t("يُحظر أي تطبيق جديد تلقائياً، ويبقى المحظور محظوراً حتى لو حذفته وأعدت تثبيته.", "Any new app is blocked automatically, and stays blocked even if you uninstall and reinstall it.")));
        if (!notifOn()) {
            Button na = Ui.button(this, L.t("تفعيل الوصول للإشعارات (لتنظيف الإشعارات)", "Allow notification access (to clean notifications)"), Ui.alpha(Ui.WARN, 70), Ui.TEXT);
            na.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            } });
            prot.addView(na);
        }
        content.addView(prot);

        // ---- lists ----
        LinearLayout lists = Ui.card(this);
        lists.addView(Ui.text(this, L.t("القوائم", "Lists"), 18, Ui.TEXT, true));
        Button kw = Ui.button(this, L.t("الكلمات المحظورة (", "Blocked words (") + ((BuiltinWords.LIST.length + BuiltinWords2.LIST.length) + s.keywords().size()) + ")", Ui.ACCENT_DARK, Ui.TEXT);
        kw.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { editKeywords(); } });
        lists.addView(kw);
        Button ap = Ui.button(this, L.t("التطبيقات المحظورة (", "Blocked apps (") + s.apps().size() + ")", Ui.ACCENT_DARK, Ui.TEXT);
        ap.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { editApps(); } });
        lists.addView(ap);
        Button lim = Ui.button(this, L.t("حدود الوقت اليومية (", "Daily time limits (") + s.limits().size() + ")", Ui.ACCENT_DARK, Ui.TEXT);
        lim.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { editLimits(); } });
        lists.addView(lim);
        Button scr = Ui.button(this, L.t("شاشة الحجب (عدّاد ", "Block screen (countdown ") + s.countdown() + L.t(" ثواني)", " s)"), Ui.ACCENT_DARK, Ui.TEXT);
        scr.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { editBlockScreen(); } });
        lists.addView(scr);
        if (strict) lists.addView(Ui.text(this, L.t("في الوضع الصارم يمكنك التشديد فقط: الإضافة، أو تقليل الوقت، أو إطالة العدّاد. لا يمكنك التخفيف.", "In strict mode you can only tighten: add, reduce time, or lengthen the countdown. You can't loosen anything."), 13, Ui.WARN, false));
        content.addView(lists);

        // ---- uninstall protection (device admin) ----
        final android.app.admin.DevicePolicyManager dpm = (android.app.admin.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        boolean adminOn = dpm != null && dpm.isAdminActive(AdminReceiver.component(this));
        LinearLayout ad = Ui.card(this);
        ad.addView(Ui.text(this, L.t("حماية من الحذف 🛡", "Uninstall protection 🛡"), 18, Ui.TEXT, true));
        if (adminOn) {
            ad.addView(Ui.text(this, L.t("مفعّلة. سيرفض أندرويد نفسه حذف التطبيق، حتى من الكمبيوتر.", "On. Android itself will refuse to uninstall the app, even from a computer."), 14, Ui.ACCENT, false));
        } else {
            ad.addView(Ui.text(this, L.t("فعّلها ليصبح حذف التطبيق مستحيلاً من الهاتف أو من الكمبيوتر، إلا بعد إيقاف الوضع الصارم بالطريقة الرسمية.", "Turn it on so the app can't be uninstalled from the phone or a computer, unless strict mode is first turned off the official way."), 14, Ui.MUTED, false));
            Button ab = Ui.button(this, L.t("تفعيل الحماية من الحذف", "Turn on uninstall protection"), Ui.WARN, Ui.BG);
            ab.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
                Intent i = new Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    .putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, AdminReceiver.component(MainActivity.this))
                    .putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        L.t("بهذا لا يستطيع أحد حذف التطبيق، ولا حتى أنت في لحظة ضعف. لا يستخدم التطبيق أي صلاحية أخرى.", "This way no one can uninstall the app, not even you in a weak moment. The app uses no other permission."));
                startActivity(i);
            } });
            ad.addView(ab);
        }
        content.addView(ad);

        // ---- strict mode ----
        LinearLayout st = Ui.card(this);
        st.addView(Ui.text(this, L.t("الوضع الصارم 🔒", "Strict mode 🔒"), 18, Ui.TEXT, true));
        if (!strict) {
            st.addView(Ui.text(this, L.t("يقفل كل شيء: لا يمكنك إيقاف الحماية أو حذف التطبيق، ولا تغيير DNS الخاص أو إعدادات الـVPN. لإيقافه يجب أن تطلب ذلك وتنتظر 24 ساعة والهاتف قيد التشغيل.", "Locks everything: you can't turn off protection, uninstall the app, or change Private DNS or VPN settings. To turn it off you must request it and wait 24 hours with the phone on."), 14, Ui.MUTED, false));
            Button en = Ui.button(this, L.t("تفعيل الوضع الصارم", "Turn on strict mode"), Ui.WARN, Ui.BG);
            en.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { confirmStrict(); } });
            if (!on) { en.setEnabled(false); en.setAlpha(0.5f); }
            st.addView(en);
        } else if (s.unlockRemaining() < 0) {
            st.addView(Ui.text(this, L.t("مفعّل. الحماية مقفلة ولا يمكن إيقافها من الإعدادات.", "On. Protection is locked and can't be turned off from settings."), 14, Ui.ACCENT, false));
            if (s.hasPartner()) {
                Button pu = Ui.button(this, L.t("إيقاف فوري بكود الشريك", "Turn off now with partner code"), Ui.alpha(Ui.WARN, 60), Ui.TEXT);
                pu.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { partnerUnlock(); } });
                st.addView(pu);
            }
            Button rq = Ui.button(this, L.t("طلب إيقاف (بعد 24 ساعة)", "Request turn-off (after 24 hours)"), Ui.alpha(Ui.DANGER, 60), Ui.TEXT);
            rq.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { confirmUnlock(); } });
            st.addView(rq);
        } else {
            st.addView(Ui.text(this, L.t("طلبتَ الإيقاف. الوقت المتبقي:", "You requested turn-off. Time remaining:"), 14, Ui.MUTED, false));
            countdown = Ui.text(this, fmtRemaining(), 30, Ui.WARN, true);
            countdown.setGravity(Gravity.CENTER);
            st.addView(countdown);
            st.addView(Ui.text(this, L.t("يُحتسب الوقت فقط والهاتف قيد التشغيل، وتغيير الساعة لا يفيد.", "Time only counts while the phone is on, and changing the clock doesn't help."), 13, Ui.MUTED, false));
            Button cancel = Ui.button(this, L.t("تراجعت، ألغِ الطلب 💪", "I changed my mind, cancel 💪"), Ui.ACCENT, Ui.BG);
            cancel.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
                s.cancelUnlock(); toast(L.t("أحسنت! الحماية مستمرة.", "Well done! Protection continues.")); render();
            } });
            st.addView(cancel);
        }
        content.addView(st);

        // ---- personal: reasons + friend ----
        LinearLayout me = Ui.card(this);
        me.addView(Ui.text(this, L.t("أسبابي وصديقي الموثوق", "My reasons & trusted friend"), 18, Ui.TEXT, true));
        me.addView(Ui.text(this, s.reasons().length() > 0 ? s.reasons() : L.t("اكتب لماذا تريد التوقف. ستظهر لك في لحظة الضعف.", "Write why you want to stop. You'll see it in a weak moment."), 14,
            s.reasons().length() > 0 ? Ui.TEXT : Ui.MUTED, false));
        Button rs = Ui.button(this, L.t("✍️ اكتب أسبابي", "✍️ Write my reasons"), Ui.ACCENT_DARK, Ui.TEXT);
        rs.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            final EditText e = new EditText(MainActivity.this);
            e.setText(s.reasons()); e.setMinLines(3); e.setTextDirection(View.TEXT_DIRECTION_RTL);
            e.setHint(L.t("مثلاً: أريد قلباً نقياً، أريد أن أحترم نفسي، أريد أن أتزوج وأنا نقي…", "e.g. I want a pure heart, I want to respect myself, I want to marry with a clean past…"));
            new AlertDialog.Builder(MainActivity.this).setTitle(L.t("لماذا تريد التوقف؟", "Why do you want to stop?")).setView(e)
                .setPositiveButton(L.t("حفظ", "Save"), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                    s.setReasons(e.getText().toString()); render(); } })
                .setNegativeButton(L.t("إلغاء", "Cancel"), null).show();
        } });
        me.addView(rs);
        Button fp = Ui.button(this, s.friendPhone().length() > 0 ? L.t("📞 رقم صديقي: ", "📞 My friend's number: ") + s.friendPhone() : L.t("📞 أضف رقم صديق موثوق", "📞 Add a trusted friend's number"), Ui.ACCENT_DARK, Ui.TEXT);
        fp.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            final EditText e = new EditText(MainActivity.this);
            e.setInputType(InputType.TYPE_CLASS_PHONE); e.setText(s.friendPhone());
            new AlertDialog.Builder(MainActivity.this).setTitle(L.t("رقم صديق تتصل به وقت الضعف", "A friend to call in a weak moment")).setView(e)
                .setPositiveButton(L.t("حفظ", "Save"), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                    s.setFriendPhone(e.getText().toString()); render(); } })
                .setNegativeButton(L.t("إلغاء", "Cancel"), null).show();
        } });
        me.addView(fp);
        content.addView(me);

        // ---- accountability partner ----
        LinearLayout pc = Ui.card(this);
        pc.addView(Ui.text(this, L.t("شريك المحاسبة 🤝", "Accountability partner 🤝"), 18, Ui.TEXT, true));
        pc.addView(Ui.text(this, s.hasPartner()
            ? L.t("لدى شريكك رمز سري يمكنه به إيقاف الوضع الصارم فوراً بدل الـ24 ساعة، إن اقتنع هو بذلك.", "Your partner has a secret code that turns off strict mode immediately instead of 24 hours, if they agree.")
            : L.t("اطلب من صديق موثوق أن يكتب رمزاً سرياً هنا بنفسه دون أن يخبرك به. عندها لا يمكن الإيقاف الفوري إلا عن طريقه.", "Ask a trusted friend to type a secret code here themselves without telling you. Then only they can turn it off immediately."), 14, Ui.MUTED, false));
        if (!s.hasPartner() && strict) {
            pc.addView(Ui.text(this, L.t("لا يمكن إضافة شريك أثناء الوضع الصارم، حتى لا يُستخدم لإيقاف الحماية. الطريق الوحيد هو طلب الـ24 ساعة.", "A partner can't be added during strict mode, so it can't be used to turn protection off. The only way is the 24-hour request."), 13, Ui.WARN, false));
        } else {
            Button pb = Ui.button(this, s.hasPartner() ? L.t("تغيير رمز الشريك (يتطلب الرمز القديم)", "Change partner code (needs the old code)") : L.t("يكتب الشريك الرمز", "Partner types the code"), Ui.ACCENT_DARK, Ui.TEXT);
            pb.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { partnerSetup(); } });
            pc.addView(pb);
        }
        content.addView(pc);

        // ---- streak reset ----
        LinearLayout sr = Ui.card(this);
        sr.addView(Ui.text(this, L.t("أطول مدة: ", "Longest streak: ") + s.bestStreak() + L.t(" يوم", " days"), 14, Ui.MUTED, false));
        Button reset = Ui.button(this, L.t("ابدأ العدّاد من جديد", "Restart the counter"), Ui.alpha(Ui.TEXT, 25), Ui.TEXT);
        reset.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            new AlertDialog.Builder(MainActivity.this)
                .setMessage(L.t("الخطأ وارد، والمهم أن تعود. هل نبدأ العدّاد من الصفر؟", "Mistakes happen; what matters is coming back. Restart the counter from zero?"))
                .setPositiveButton(L.t("نعم", "Yes"), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                    s.saveBest(); s.resetStreak(); render(); } })
                .setNegativeButton(L.t("لا", "No"), null).show();
        } });
        sr.addView(reset);
        content.addView(sr);

        // ---- kids mode ----
        LinearLayout kc = Ui.card(this);
        kc.addView(Ui.text(this, L.t("وضع الأطفال 👶", "Kids mode 👶"), 18, Ui.TEXT, true));
        kc.addView(toggle("kids", L.t("تفتح التطبيقات المسموحة فقط", "Only allowed apps open"),
            L.t("كل التطبيقات مقفلة ما عدا ما تختاره (" + s.kidApps().size() + " تطبيق). وإن سمحت بمتصفح، تفتح فيه المواقع المسموحة فقط (" + s.kidSites().size() + " موقع، وإن كانت القائمة فارغة فكل المواقع النظيفة).",
                "Everything is locked except the apps you choose (" + s.kidApps().size() + " apps). If you allow a browser, only allowed sites open in it (" + s.kidSites().size() + " sites; an empty list means any clean site).")));
        LinearLayout kb = Ui.hbox(this);
        Button ka = Ui.button(this, L.t("التطبيقات المسموحة", "Allowed apps"), Ui.ACCENT_DARK, Ui.TEXT);
        Button ks = Ui.button(this, L.t("المواقع المسموحة", "Allowed sites"), Ui.ACCENT_DARK, Ui.TEXT);
        ka.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { editKidApps(); } });
        ks.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { editKidSites(); } });
        LinearLayout.LayoutParams kh = new LinearLayout.LayoutParams(0, Ui.dp(this, 50), 1);
        kh.setMargins(Ui.dp(this, 3), Ui.dp(this, 8), Ui.dp(this, 3), 0);
        kb.addView(ka, kh); kb.addView(ks, new LinearLayout.LayoutParams(kh));
        kc.addView(kb);
        if (strict) kc.addView(Ui.text(this, L.t("في الوضع الصارم لا يمكن إضافة تطبيقات أو مواقع جديدة للقائمة المسموحة.", "In strict mode nothing new can be added to the allowed lists."), 12, Ui.WARN, false));
        else kc.addView(Ui.text(this, L.t("نصيحة: بعد اختيار القوائم فعّل الوضع الصارم، حتى لا يستطيع الطفل إيقاف هذا الوضع.", "Tip: after setting the lists, turn on strict mode so the child can't switch this off."), 12, Ui.MUTED, false));
        content.addView(kc);

        Button lang = Ui.button(this, L.t("🌐 Language: العربية", "🌐 اللغة: English"), Ui.alpha(Ui.TEXT, 25), Ui.TEXT);
        lang.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            final String[] codes = { "auto", "ar", "en" };
            new AlertDialog.Builder(MainActivity.this).setTitle("اللغة / Language")
                .setItems(new String[] { "تلقائي / Auto", "العربية", "English" }, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int i) { s.setLang(codes[i]); L.init(MainActivity.this); recreate(); } })
                .show();
        } });
        content.addView(lang);

        TextView foot = Ui.text(this, L.t("لا يملك التطبيق إذن الإنترنت. كل شيء يبقى على هاتفك.", "The app has no internet permission. Everything stays on your phone."), 12, Ui.MUTED, false);
        foot.setGravity(Gravity.CENTER);
        content.addView(foot, new LinearLayout.LayoutParams(-1, -2));
        TextView ab = Ui.text(this, L.t("عن التطبيق والخصوصية", "About & privacy"), 13, Ui.ACCENT, true);
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
            TextView go = Ui.text(this, L.t(" ابدأ ", " Start "), 14, Ui.BG, true);
            go.setBackground(Ui.round(Ui.ACCENT, Ui.dp(this, 12)));
            go.setPadding(Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 12), Ui.dp(this, 6));
            go.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
                try { action.run(); } catch (Exception e) { toast(L.t("افتح الإعدادات يدوياً من فضلك.", "Please open Settings manually.")); } } });
            row.addView(go);
        }
        return row;
    }

    static final String DNS_HOST = "family-filter-dns.cleanbrowsing.org";

    private void dnsGuide() {
        new AlertDialog.Builder(this).setTitle(L.t("فلتر المواقع الإباحية (مجاني)", "Adult-site filter (free)"))
            .setMessage(L.t("هذا الفلتر يحجب آلاف المواقع الإباحية في كل الهاتف، ويفرض البحث الآمن في Google وYouTube، ويعمل حتى لو توقف التطبيق.\n\n", "This filter blocks thousands of adult sites across the whole phone, forces safe search on Google and YouTube, and works even if the app stops.\n\n")
                + L.t("الخطوات:\n1. اضغط «نسخ العنوان».\n2. اضغط «فتح الإعدادات».\n3. ابحث في الإعدادات عن «DNS الخاص» (Private DNS).\n", "Steps:\n1. Tap \"Copy address\".\n2. Tap \"Open settings\".\n3. Search settings for \"Private DNS\".\n")
                + L.t("4. اختر «اسم مضيف موفر DNS الخاص» والصق العنوان:\n\n", "4. Choose \"Private DNS provider hostname\" and paste the address:\n\n") + DNS_HOST + L.t("\n\n5. احفظ.\n\nالخدمة من CleanBrowsing، مجانية للعائلات.", "\n\n5. Save.\n\nThe service is from CleanBrowsing, free for families."))
            .setPositiveButton(L.t("نسخ العنوان", "Copy address"), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (cm != null) cm.setPrimaryClip(android.content.ClipData.newPlainText("dns", DNS_HOST));
                toast(L.t("تم النسخ ✓ الآن افتح الإعدادات.", "Copied ✓ Now open settings."));
                dnsGuide();
            } })
            .setNeutralButton(L.t("فتح الإعدادات", "Open settings"), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                try { startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)); }
                catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            } })
            .setNegativeButton(L.t("إغلاق", "Close"), null).show();
    }

    private boolean disclosureShowing = false;

    private void maybeDisclosure() {
        final android.content.SharedPreferences sp = getSharedPreferences("ui", MODE_PRIVATE);
        if (sp.getBoolean("disclosed", false) || disclosureShowing) return;
        disclosureShowing = true;
        new AlertDialog.Builder(this).setCancelable(false).setTitle(L.t("قبل أن تبدأ: خصوصيتك", "Before you start: your privacy"))
            .setMessage(L.t("يستخدم «درع العفّة» خدمة «إمكانية الوصول» في أندرويد لقراءة النصوص الظاهرة على الشاشة، فقط ليعرف متى يحجب المحتوى الإباحي والريلز ويحمي إعداداته.\n\n", "Iffah Shield uses Android's Accessibility service to read the text shown on screen, only to know when to block adult content and Reels and to protect its own settings.\n\n")
                + L.t("• لا يملك التطبيق إذن الإنترنت إطلاقاً.\n• لا يرسل ولا يحفظ أي نص أو صورة من شاشتك.\n• يحفظ على هاتفك فقط: إعداداتك وقوائمك، وأعداد مرات الحجب، ووقت استخدام التطبيقات المحدودة، وأسبابك ورقم صديقك إن أضفتهما.\n", "• The app has no internet permission at all.\n• It never sends or stores any text or image from your screen.\n• It stores only on your phone: your settings and lists, block counts, usage time of limited apps, and your reasons and friend's number if you add them.\n")
                + L.t("• الحماية من الحذف تستخدم صلاحية «مسؤول الجهاز» لمنع الحذف فقط، دون أي صلاحية أخرى.\n\nبالضغط على «موافق» فأنت توافق على ذلك.", "• Uninstall protection uses the Device admin permission only to prevent uninstalling, nothing else.\n\nBy tapping \"Agree\" you accept this."))
            .setPositiveButton(L.t("موافق", "Agree"), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                sp.edit().putBoolean("disclosed", true).apply(); } })
            .setNegativeButton(L.t("خروج", "Exit"), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) { finish(); } })
            .show();
    }

    private void about() {
        new AlertDialog.Builder(this).setTitle(L.t("عن درع العفّة", "About Iffah Shield"))
            .setMessage(L.t("تطبيق مجاني ومفتوح المصدر، صدقة جارية لحماية الشباب والفتيات من الإباحية.\n\n", "A free, open-source app, made as ongoing charity to protect young people from pornography.\n\n")
                + L.t("• بلا إنترنت، بلا إعلانات، بلا اشتراكات، بلا جمع بيانات.\n• يحجب الكلمات والمواقع والريلز والشورتس والتصفح المتخفي.\n", "• No internet, no ads, no subscriptions, no data collection.\n• Blocks words, sites, Reels, Shorts and incognito browsing.\n")
                + L.t("• وضع صارم وحماية من الحذف وزر طوارئ.\n\nإن نفعك فادعُ لمن صنعه، وانشره لغيرك فالدال على الخير كفاعله.", "• Strict mode, uninstall protection and an emergency button.\n\nIf it helps you, make dua for whoever made it, and share it: whoever guides to good gets its reward."))
            .setPositiveButton(L.t("حسناً", "OK"), null).show();
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
        final EditText old = codeField(L.t("الرمز القديم", "Old code"));
        final EditText c1 = codeField(L.t("رمز جديد (6 أرقام أو أكثر)", "New code (6+ digits)"));
        final EditText c2 = codeField(L.t("أعد الكود", "Repeat the code"));
        if (s.hasPartner()) l.addView(old);
        l.addView(c1); l.addView(c2);
        new AlertDialog.Builder(this).setTitle(L.t("هذه الخطوة لشريكك، لا لك", "This step is for your partner, not you")).setView(l)
            .setMessage(L.t("سلّم الهاتف لشريكك ليكتب الرمز بنفسه. لا يحفظ التطبيق الرمز نفسه، بل بصمة مشفّرة منه.", "Hand the phone to your partner to type the code themselves. The app stores only an encrypted fingerprint, not the code."))
            .setPositiveButton(L.t("حفظ", "Save"), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                String a = c1.getText().toString(), b2 = c2.getText().toString();
                if (a.length() < 6 || !a.equals(b2)) { toast(L.t("يجب أن يتكون الرمز من 6 أرقام أو أكثر، وأن يتطابق الحقلان.", "The code must be 6+ digits and both fields must match.")); return; }
                if (s.setPartnerCode(old.getText().toString(), a)) toast(L.t("تم حفظ رمز الشريك ✓", "Partner code saved ✓"));
                else toast(L.t("الرمز القديم غير صحيح.", "The old code is wrong."));
                render();
            } })
            .setNegativeButton(L.t("إلغاء", "Cancel"), null).show();
    }

    private void partnerUnlock() {
        final EditText c = codeField(L.t("رمز الشريك", "Partner code"));
        new AlertDialog.Builder(this).setTitle(L.t("إيقاف الوضع الصارم فوراً", "Turn off strict mode now")).setView(c)
            .setMessage(L.t("شريكك وحده يعرف الرمز. بعد 3 محاولات خاطئة يُقفل لمدة نصف ساعة.", "Only your partner knows the code. After 3 wrong tries it locks for half an hour."))
            .setPositiveButton(L.t("تأكيد", "Confirm"), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                if (s.checkPartner(c.getText().toString())) { s.forceStrictOff(); toast(L.t("تم إيقاف الوضع الصارم.", "Strict mode turned off.")); }
                else toast(L.t("الرمز غير صحيح، أو عليك الانتظار قليلاً.", "Wrong code, or you need to wait a bit."));
                render();
            } })
            .setNegativeButton(L.t("إلغاء", "Cancel"), null).show();
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
            items[i] = labelOf(pk) + " — " + (s.usedToday(pk) / 60000) + L.t(" من ", " of ") + s.limits().get(pk) + L.t(" دقيقة اليوم", " min today");
        }
        new AlertDialog.Builder(this).setTitle(L.t("حدود الوقت اليومية", "Daily time limits"))
            .setItems(items, new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int i) {
                pickMinutes(pkgs.get(i), true);
            } })
            .setPositiveButton(L.t("إضافة تطبيق", "Add app"), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                final List<String[]> apps = launcherApps();
                String[] labels = new String[apps.size()];
                for (int i = 0; i < labels.length; i++) labels[i] = apps.get(i)[0];
                new AlertDialog.Builder(MainActivity.this).setTitle(L.t("اختر التطبيق", "Choose the app"))
                    .setItems(labels, new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d2, int i) {
                        pickMinutes(apps.get(i)[1], false);
                    } }).show();
            } })
            .setNegativeButton(L.t("إغلاق", "Close"), null).show();
    }

    private void pickMinutes(final String pkg, boolean existing) {
        final int[] mins = { 10, 15, 30, 45, 60, 90, 120 };
        String[] labels = new String[mins.length + (existing ? 1 : 0)];
        for (int i = 0; i < mins.length; i++) labels[i] = mins[i] + L.t(" دقيقة باليوم", " min per day");
        if (existing) labels[mins.length] = L.t("إزالة الحد", "Remove limit");
        new AlertDialog.Builder(this).setTitle(labelOf(pkg))
            .setItems(labels, new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int i) {
                int m = i < mins.length ? mins[i] : 0;
                if (!s.setLimit(pkg, m)) toast(L.t("في الوضع الصارم يمكنك تقليل الوقت فقط.", "In strict mode you can only reduce the time."));
                render();
            } }).show();
    }

    private void editBlockScreen() {
        final int[] secs = { 0, 5, 10, 20, 30, 60 };
        String[] labels = new String[secs.length + 1];
        for (int i = 0; i < secs.length; i++) labels[i] = L.t("عدّاد ", "Countdown ") + secs[i] + L.t(" ثانية قبل إمكانية الإغلاق", " s before it can be closed");
        labels[secs.length] = L.t("رسالتي الخاصة على شاشة الحجب", "My own message on the block screen");
        new AlertDialog.Builder(this).setTitle(L.t("شاشة الحجب", "Block screen"))
            .setItems(labels, new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int i) {
                if (i < secs.length) {
                    if (!s.setCountdown(secs[i])) toast(L.t("في الوضع الصارم يمكنك إطالة العدّاد فقط.", "In strict mode you can only lengthen the countdown."));
                    render();
                    return;
                }
                final EditText e = new EditText(MainActivity.this);
                e.setText(s.customMsg());
                e.setHint(L.t("مثلاً: تذكّر لماذا بدأت", "e.g. Remember why you started"));
                e.setTextDirection(View.TEXT_DIRECTION_RTL);
                new AlertDialog.Builder(MainActivity.this).setTitle(L.t("رسالتك إلى نفسك", "Your message to yourself")).setView(e)
                    .setMessage(L.t("تظهر على شاشة الحجب بدل العبارات الجاهزة. اتركها فارغة لاستعادة العبارات الجاهزة.", "Shown on the block screen instead of the built-in lines. Leave empty to bring them back."))
                    .setPositiveButton(L.t("حفظ", "Save"), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d2, int w) {
                        s.setCustomMsg(e.getText().toString()); toast(L.t("تم الحفظ ✓", "Saved ✓")); } })
                    .setNegativeButton(L.t("إلغاء", "Cancel"), null).show();
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
            final boolean want = sw.isChecked();
            if (!s.setFlag(key, want)) {
                sw.setChecked(!want);
                if (want) { toast(L.t("اختر التطبيقات المسموحة أولاً.", "Choose the allowed apps first.")); return; }
                toast(L.t("الوضع الصارم مفعّل، لا يمكنك إيقاف الحماية.", "Strict mode is on; you can't turn protection off."));
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
            items[i] = w.startsWith("=") ? w.substring(1) + L.t("  (كلمة كاملة)", "  (whole word)") : w;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(this)
            .setTitle(items.length == 0 ? L.t("يوجد ", "There are ") + (BuiltinWords.LIST.length + BuiltinWords2.LIST.length) + L.t(" كلمة مدمجة (مخفية لحمايتك). أضف كلماتك:", " built-in words (hidden for your protection). Add your own:")
                                        : L.t("كلماتك (+", "Your words (+") + (BuiltinWords.LIST.length + BuiltinWords2.LIST.length) + L.t(" كلمة مدمجة مخفية)", " hidden built-in)"))
            .setItems(items, new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, final int i) {
                if (s.isStrict()) { toast(L.t("لا يمكن حذف الكلمات في الوضع الصارم.", "Words can't be removed in strict mode.")); return; }
                new AlertDialog.Builder(MainActivity.this).setMessage(L.t("هل تحذف «", "Remove \"") + items[i] + L.t("»؟", "\"?"))
                    .setPositiveButton(L.t("حذف", "Remove"), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d2, int w) {
                        s.removeKeyword(words.get(i)); GuardService.matcherVersion++; render(); } })
                    .setNegativeButton(L.t("لا", "No"), null).show();
            } })
            .setPositiveButton(L.t("إضافة كلمة", "Add word"), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) { addKeyword(); } })
            .setNegativeButton(L.t("إغلاق", "Close"), null);
        b.show();
    }

    private void addKeyword() {
        final EditText e = new EditText(this);
        e.setHint(L.t("مثلاً: كلمة أو اسم موقع", "e.g. a word or a site name"));
        e.setInputType(InputType.TYPE_CLASS_TEXT);
        e.setTextDirection(View.TEXT_DIRECTION_RTL);
        new AlertDialog.Builder(this).setTitle(L.t("إضافة كلمة", "Add word")).setView(e)
            .setMessage(L.t("ستُحجب أي صفحة أو شاشة تحتوي على هذه الكلمة.", "Any page or screen containing this word will be blocked."))
            .setPositiveButton(L.t("إضافة", "Add"), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                if (s.addKeyword(e.getText().toString())) { GuardService.matcherVersion++; toast(L.t("تمت الإضافة ✓", "Added ✓")); }
                else toast(L.t("الكلمة يجب أن تكون 3 أحرف على الأقل، وغير مكررة.", "The word must be at least 3 letters and not already listed."));
                render();
            } })
            .setNegativeButton(L.t("إلغاء", "Cancel"), null).show();
    }

    private void editApps() {
        final List<String[]> apps = launcherApps();
        final List<String> blocked = s.apps();
        final String[] labels = new String[apps.size()];
        final boolean[] checked = new boolean[apps.size()];
        for (int i = 0; i < labels.length; i++) { labels[i] = apps.get(i)[0]; checked[i] = blocked.contains(apps.get(i)[1]); }
        new AlertDialog.Builder(this).setTitle(L.t("اختر التطبيقات التي تريد حظرها", "Choose the apps to block"))
            .setMultiChoiceItems(labels, checked, new DialogInterface.OnMultiChoiceClickListener() {
                @Override public void onClick(DialogInterface d, int i, boolean c) {
                    String pkg = apps.get(i)[1];
                    if (c) s.addApp(pkg);
                    else if (!s.removeApp(pkg)) {
                        ((AlertDialog) d).getListView().setItemChecked(i, true); checked[i] = true;
                        toast(L.t("لا يمكن إلغاء الحظر في الوضع الصارم.", "Apps can't be unblocked in strict mode."));
                    }
                } })
            .setPositiveButton(L.t("تم", "Done"), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) { render(); } })
            .show();
    }

    private static String hm(int minutes) { return String.format(java.util.Locale.US, "%d:%02d", minutes / 60, minutes % 60); }

    private void pickAdhkarTimes() {
        final int[] mo = { 5 * 60, 5 * 60 + 30, 6 * 60, 6 * 60 + 30, 7 * 60, 7 * 60 + 30, 8 * 60, 9 * 60 };
        final int[] ev = { 16 * 60, 17 * 60, 18 * 60, 19 * 60, 20 * 60, 21 * 60, 22 * 60 };
        final String[] ml = new String[mo.length], el = new String[ev.length];
        for (int i = 0; i < mo.length; i++) ml[i] = hm(mo[i]);
        for (int i = 0; i < ev.length; i++) el[i] = hm(ev[i]);
        new AlertDialog.Builder(this).setTitle(L.t("وقت أذكار الصباح", "Morning adhkar time"))
            .setItems(ml, new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, final int i) {
                new AlertDialog.Builder(MainActivity.this).setTitle(L.t("وقت أذكار المساء", "Evening adhkar time"))
                    .setItems(el, new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d2, int j) {
                        s.setAdhkarTimes(mo[i], ev[j]); toast(L.t("تم الحفظ ✓", "Saved ✓")); render(); } }).show();
            } }).show();
    }

    private View nightRow() {
        LinearLayout row = Ui.hbox(this);
        row.setPadding(0, Ui.dp(this, 12), 0, 0);
        LinearLayout t = Ui.vbox(this);
        t.addView(Ui.text(this, L.t("قفل الليل 🌙", "Night lock 🌙"), 16, Ui.TEXT, true));
        t.addView(Ui.text(this, s.flag("night")
            ? L.t("مفعّل من الساعة " + s.nightStart() + " إلى " + s.nightEnd() + " صباحاً. تُقفل التطبيقات إلا الاتصال والرسائل والمنبّه والقرآن.",
                  "On from " + s.nightStart() + ":00 to " + s.nightEnd() + ":00. Apps are locked except calls, messages, alarm and Quran.")
            : L.t("الليل والسرير أخطر الأوقات. يُقفل التطبيقات كل ليلة تلقائياً، ولا يمكن إيقافه أو تقصيره في الوضع الصارم.",
                  "Night and bed are the riskiest times. Locks apps every night automatically; in strict mode it can't be turned off or shortened."), 12, Ui.MUTED, false));
        row.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        final Switch sw = new Switch(this);
        sw.setChecked(s.flag("night"));
        sw.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            if (!sw.isChecked()) {
                if (!s.setNight(false, s.nightStart(), s.nightEnd())) { sw.setChecked(true); toast(L.t("الوضع الصارم مفعّل، لا يمكن إيقاف قفل الليل.", "Strict mode is on; night lock can't be turned off.")); }
                render(); return;
            }
            sw.setChecked(s.flag("night"));
            pickNight();
        } });
        row.addView(sw);
        return row;
    }

    private void pickNight() {
        final int[][] opts = { { 23, 5 }, { 0, 5 }, { 0, 6 }, { 1, 5 }, { 22, 6 } };
        String[] labels = new String[opts.length];
        for (int i = 0; i < opts.length; i++) labels[i] = L.t("من " + opts[i][0] + ":00 إلى " + opts[i][1] + ":00", "From " + opts[i][0] + ":00 to " + opts[i][1] + ":00");
        new AlertDialog.Builder(this).setTitle(L.t("وقت قفل الليل", "Night lock hours"))
            .setItems(labels, new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int i) {
                if (s.setNight(true, opts[i][0], opts[i][1])) toast(L.t("قفل الليل مفعّل 🌙", "Night lock is on 🌙"));
                else toast(L.t("في الوضع الصارم يمكنك إطالة الوقت فقط.", "In strict mode you can only make it longer."));
                render();
            } }).show();
    }

    private void editKidApps() {
        final List<String[]> apps = launcherApps();
        final List<String> allowed = s.kidApps();
        final String[] labels = new String[apps.size()];
        final boolean[] checked = new boolean[apps.size()];
        for (int i = 0; i < labels.length; i++) { labels[i] = apps.get(i)[0]; checked[i] = allowed.contains(apps.get(i)[1]); }
        new AlertDialog.Builder(this).setTitle(L.t("اختر التطبيقات المسموحة للطفل", "Choose the apps your child may use"))
            .setMultiChoiceItems(labels, checked, new DialogInterface.OnMultiChoiceClickListener() {
                @Override public void onClick(DialogInterface d, int i, boolean c) {
                    String pkg = apps.get(i)[1];
                    boolean ok = c ? s.addKidApp(pkg) : s.removeKidApp(pkg);
                    if (!ok) {
                        ((AlertDialog) d).getListView().setItemChecked(i, !c); checked[i] = !c;
                        toast(L.t("لا يمكن تعديل القائمة في الوضع الصارم.", "The list can't be changed in strict mode."));
                    }
                } })
            .setPositiveButton(L.t("تم", "Done"), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) { render(); } })
            .show();
    }

    private void editKidSites() {
        final List<String> sites = s.kidSites();
        final String[] items = sites.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle(L.t("المواقع المسموحة (اضغط للحذف)", "Allowed sites (tap to remove)"))
            .setItems(items, new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int i) {
                if (!s.removeKidSite(items[i])) toast(L.t("لا يمكن التعديل في الوضع الصارم.", "Can't change this in strict mode."));
                render(); } })
            .setPositiveButton(L.t("إضافة موقع", "Add site"), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                final EditText e = new EditText(MainActivity.this);
                e.setHint("example.com");
                e.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI | android.text.InputType.TYPE_CLASS_TEXT);
                new AlertDialog.Builder(MainActivity.this).setTitle(L.t("أضف موقعاً", "Add a site")).setView(e)
                    .setPositiveButton(L.t("إضافة", "Add"), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d2, int w2) {
                        toast(s.addKidSite(e.getText().toString()) ? L.t("تمت الإضافة ✓", "Added ✓") : L.t("رابط غير صالح، أو الوضع الصارم مفعّل.", "Invalid address, or strict mode is on."));
                        render(); } })
                    .setNegativeButton(L.t("إلغاء", "Cancel"), null).show();
            } })
            .setNegativeButton(L.t("إغلاق", "Close"), null).show();
    }

    private void confirmStrict() {
        new AlertDialog.Builder(this).setTitle(L.t("تفعيل الوضع الصارم؟", "Turn on strict mode?"))
            .setMessage(L.t("بعد التفعيل:\n• لا يمكنك إيقاف أي حماية.\n• لا يمكنك حذف التطبيق.\n• تُقفل صفحات DNS الخاص والـVPN الدائم وإعادة ضبط المصنع والمجلد الآمن.\n• للإيقاف يجب أن تطلب وتنتظر 24 ساعة.\n\nهل أنت متأكد؟", "After turning it on:\n• You can't turn off any protection.\n• You can't uninstall the app.\n• Private DNS, always-on VPN, factory reset and Secure Folder pages are locked.\n• To turn it off you must request it and wait 24 hours.\n\nAre you sure?"))
            .setPositiveButton(L.t("فعّل، أنا قادر 💪", "Turn on, I can do this 💪"), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                s.enableStrict(); toast(L.t("الوضع الصارم مفعّل 🔒", "Strict mode is on 🔒")); render(); } })
            .setNegativeButton(L.t("ليس الآن", "Not now"), null).show();
    }

    private void confirmUnlock() {
        new AlertDialog.Builder(this).setTitle(L.t("هل أنت متأكد؟", "Are you sure?"))
            .setMessage(L.t("إن كانت لحظة ضعف فستمضي. اشرب ماءً، واخرج قليلاً، ثم عد.\n\nإن كنت ما زلت تريد ذلك، سيبدأ عدّاد مدته 24 ساعة، ويمكنك إلغاؤه في أي وقت.", "If it's a weak moment, it will pass. Drink water, step outside, then come back.\n\nIf you still want it, a 24-hour countdown will start, and you can cancel it any time."))
            .setPositiveButton(L.t("ابدأ العدّاد", "Start countdown"), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                s.requestUnlock(); render(); } })
            .setNegativeButton(L.t("لا، أبقِه مقفلاً 💪", "No, keep it locked 💪"), null).show();
    }

    private String fmtRemaining() {
        long ms = Math.max(0, s.liveRemaining());
        long hh = ms / 3600000, mm = (ms / 60000) % 60, ss = (ms / 1000) % 60;
        return String.format(java.util.Locale.US, "%02d:%02d:%02d", hh, mm, ss);
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}
