package com.iffahshield.app;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GuardService extends AccessibilityService {

    static volatile boolean running = false;
    static volatile long matcherVersion = 0;

    // Never scanned for keywords (our UI, terminal, keyboards, system UI, private chats, settings).
    static final Set<String> SKIP_WORDS = new HashSet<String>(Arrays.asList(
        "com.android.phone", "com.samsung.android.dialer", "com.google.android.dialer", "com.android.dialer",
        "com.samsung.android.incallui", "com.android.incallui", "com.android.server.telecom", "com.android.emergency",
        "com.samsung.android.contacts", "com.google.android.contacts", "com.android.contacts",
        "com.samsung.android.app.telephonyui", "com.google.android.apps.safetyhub",
        "com.iffahshield.app", "com.android.systemui", "com.sec.android.app.launcher",
        "com.samsung.android.honeyboard", "com.google.android.inputmethod.latin",
        "com.whatsapp", "com.whatsapp.w4b", "com.google.android.gm", "com.samsung.android.messaging",
        "com.android.settings"));

    // Apps that must never be blocked or time-limited.
    static final Set<String> NEVER_BLOCK = new HashSet<String>(Arrays.asList(
        "com.iffahshield.app", "com.android.settings",
        "com.android.systemui", "com.sec.android.app.launcher", "com.android.phone",
        "com.samsung.android.dialer", "com.google.android.dialer", "com.samsung.android.incallui",
        "com.android.server.telecom", "com.android.dialer", "com.android.incallui",
        "com.android.emergency", "com.samsung.android.app.telephonyui", "com.google.android.apps.safetyhub"));

    // Still usable during a panic lockdown.
    private static final Set<String> LOCKDOWN_ALLOW = new HashSet<String>(Arrays.asList(
        "com.samsung.android.messaging", "com.google.android.apps.messaging", "com.whatsapp",
        "com.sec.android.app.camera", "com.sec.android.app.clockpackage", "com.google.android.deskclock",
        "com.sec.android.app.popupcalculator", "com.google.android.apps.maps", "com.samsung.android.app.notes",
        "com.samsung.android.contacts", "com.google.android.contacts", "com.samsung.android.calendar"));

    // Also usable during the night lock (Quran and alarms).
    private static final Set<String> NIGHT_ALLOW = new HashSet<String>(Arrays.asList(
        "com.quran.labs.androidquran", "com.greentech.quran", "com.mmmoussa.quran", "com.sec.android.app.clockpackage",
        "com.google.android.deskclock"));

    private static final Set<String> BROWSERS = new HashSet<String>(Arrays.asList(
        "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.sec.android.app.sbrowser",
        "org.mozilla.firefox", "org.mozilla.focus", "com.opera.browser", "com.opera.mini.native",
        "com.brave.browser", "com.microsoft.emmx", "com.duckduckgo.mobile.android",
        "com.UCMobile.intl", "com.kiwibrowser.browser", "com.vivaldi.browser",
        "com.google.android.googlequicksearchbox"));

    // Screens where protection gets turned off. Checked only in strict mode.
    private static final Set<String> GUARDED_PKGS = new HashSet<String>(Arrays.asList(
        "com.android.settings", "com.android.settings.intelligence",
        "com.google.android.packageinstaller", "com.android.packageinstaller",
        "com.samsung.android.packageinstaller", "com.samsung.android.lool",
        "com.samsung.android.sm.devicesecurity", "com.android.vending",
        "com.google.android.permissioncontroller", "com.samsung.accessibility"));

    // Separate spaces where neither Hisn nor Proton protect: blocked outright in strict mode.
    private static final String[] ESCAPE_PKG_PREFIXES = {
        "com.samsung.knox.securefolder", "com.samsung.android.da.daagent" };

    private static final String[] OWN_LABELS = { "درع العفه", "iffah shield" };
    private static final String[] PROTECTED_LABELS = { };
    private static final String[] DANGER = {
        "الغاء التثبيت", "فرض الايقاف", "تعطيل", "حذف ملف تعريف", "ازاله",
        "uninstall", "force stop", "disable", "remove" };
    private static final String[] SETTINGS_TERMS = {
        "dns خاص", "dns الخاص", "private dns",
        "حظر الاتصالات من دون vpn", "تشغيل vpn دائما", "always-on vpn", "block connections without vpn",
        "اعاده ضبط بيانات المصنع", "factory data reset",
        "المجلد الامن", "secure folder", "الماسنجر المزدوج", "dual messenger",
        "اضافه ضيف", "add guest", "اضافه مستخدم", "add user",
        "التاريخ والوقت التلقائي", "تعيين الوقت", "تعيين التاريخ", "automatic date and time" };
    private static final String[] INCOGNITO = {
        "وضع التصفح المتخفي", "اصبحت في وضع التصفح المتخفي", "انتقلت الى وضع التصفح المتخفي",
        "you've gone incognito", "you’ve gone incognito", "incognito mode", "secret mode is on",
        "الوضع السري قيد التشغيل", "private browsing mode", "ntp_incognito_marker" };

    private static final String[][] REEL_IDS = {
        { "com.instagram.android", "com.instagram.android:id/clips_viewer_view_pager" },
        { "com.instagram.android", "com.instagram.android:id/clips_viewer_container" },
        { "com.google.android.youtube", "com.google.android.youtube:id/reel_recycler" },
        { "com.google.android.youtube", "com.google.android.youtube:id/reel_player_page_container" },
        { "com.google.android.youtube", "com.google.android.youtube:id/shorts_container" },
    };
    private static final String[][] STORY_IDS = {
        { "com.instagram.android", "com.instagram.android:id/reel_viewer_media_container" },
        { "com.instagram.android", "com.instagram.android:id/reel_viewer_root" },
    };

    private Store store;
    private Matcher matcher;
    private long builtVersion = -1;
    private final Handler h = new Handler(Looper.getMainLooper());
    private long lastScan = 0, coolUntil = 0;
    private int screenW = 1080, screenH = 2340;
    private final Map<String, Boolean> launchable = new HashMap<String, Boolean>();

    // foreground usage tracking (for daily limits)
    private String fgPkg = null;
    private long fgSince = 0;
    private int ticks = 0;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (store.tick()) toast(L.t("انتهت مهلة الـ24 ساعة، وتم إيقاف الوضع الصارم.", "The 24 hours are over; strict mode is now off."));
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isInteractive()) setForeground(null);
            else {
                L.init(GuardService.this);
                String cur = fgPkg, top = "";
                try {
                    AccessibilityNodeInfo r = getRootInActiveWindow();
                    if (r != null) {
                        CharSequence rp = r.getPackageName();
                        r.recycle();
                        if (rp != null) {
                            String p = rp.toString();
                            top = p;
                            if (isLaunchable(p) && !p.equals("com.iffahshield.app") && !isExempt(p)) cur = p;
                            else if (isExempt(p)) cur = null;
                        }
                    }
                } catch (Exception ignored) { }
                setForeground(cur); // flush time so far
                if (!store.isPaused() && adhkarNeeded(top)) forceAdhkar();
                if (fgPkg != null && !inCall() && !isExempt(fgPkg) && !store.isPaused() && store.overLimit(fgPkg))
                    block("limit", L.t("انتهى وقتك اليومي لهذا التطبيق. سيُتاح غداً.", "Your daily time for this app is up. It opens again tomorrow."), true);
            }
            if (++ticks % 120 == 0) store.pruneUsage();
            h.postDelayed(this, 30_000);
        }
    };

    private final BroadcastReceiver pkgReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (i.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return;
            Uri u = i.getData();
            if (u == null || store == null) return;
            String pkg = u.getSchemeSpecificPart();
            if (pkg == null || isExempt(pkg)) return;
            if (getPackageManager().getLaunchIntentForPackage(pkg) == null) return;
            if (store.flag("newBrowsers") && isUnknownBrowser(pkg)) {
                if (store.addApp(pkg)) toast(L.t("تم حظر المتصفح الجديد تلقائياً.", "The new browser was blocked automatically."));
                return;
            }
            if (!store.flag("newApps")) return;
            if (store.addApp(pkg)) toast(L.t("تم حظر التطبيق الجديد تلقائياً.", "The new app was blocked automatically."));
        }
    };

    @Override protected void onServiceConnected() {
        store = new Store(this);
        running = true;
        DisplayMetrics m = getResources().getDisplayMetrics();
        screenW = m.widthPixels; screenH = m.heightPixels;
        h.removeCallbacks(ticker);
        h.post(ticker);
        IntentFilter f = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        f.addDataScheme("package");
        try { registerReceiver(pkgReceiver, f); } catch (Exception ignored) { }
        L.init(this);
        h.postDelayed(new Runnable() { @Override public void run() { blockUnknownBrowsers(); } }, 5000);
    }

    // ---------- browsers the app doesn't know: blocked, so they can't be used to slip past ----------
    private List<String> webHandlers() {
        List<String> out = new ArrayList<String>();
        try {
            Intent v = new Intent(Intent.ACTION_VIEW, Uri.parse("http://example.com/")).addCategory(Intent.CATEGORY_BROWSABLE);
            for (android.content.pm.ResolveInfo ri : getPackageManager().queryIntentActivities(v, 0x00020000 /* MATCH_ALL */))
                if (ri.activityInfo != null && !out.contains(ri.activityInfo.packageName)) out.add(ri.activityInfo.packageName);
        } catch (Exception ignored) { }
        return out;
    }

    private boolean isUnknownBrowser(String pkg) {
        return !BROWSERS.contains(pkg) && !NEVER_BLOCK.contains(pkg) && !pkg.equals("com.iffahshield.app") && webHandlers().contains(pkg);
    }

    private void blockUnknownBrowsers() {
        if (store == null || !store.flag("newBrowsers")) return;
        int n = 0;
        for (String pkg : webHandlers())
            if (isUnknownBrowser(pkg) && !isExempt(pkg) && getPackageManager().getLaunchIntentForPackage(pkg) != null && store.addApp(pkg)) n++;
        if (n > 0) toast(L.t("تم حظر " + n + " متصفح غير معروف.", n + " unknown browser(s) blocked."));
    }

    // ---------- adhkar lock ----------
    private static final Set<String> ADHKAR_ALLOW = new HashSet<String>(Arrays.asList(
        "com.iffahshield.app", "com.android.phone", "com.samsung.android.dialer", "com.google.android.dialer",
        "com.samsung.android.incallui", "com.android.server.telecom", "com.android.dialer", "com.android.incallui",
        "com.android.emergency", "com.samsung.android.app.telephonyui", "com.google.android.apps.safetyhub",
        "com.android.systemui", "com.samsung.android.honeyboard", "com.google.android.inputmethod.latin",
        "com.sec.android.app.clockpackage", "com.google.android.deskclock"));
    private long adhkarLaunchAt = 0, adhkarFirstLaunch = 0;
    private int adhkarLaunches = 0;
    private String adhkarGaveUp = null;
    private long adhkarGaveUpAt = 0;

    private boolean adhkarNeeded(String pkg) {
        if (inCall()) return false;
        String due = store.adhkarDue();
        if (due == null) return false;
        if (due.equals(adhkarGaveUp) && System.currentTimeMillis() - adhkarGaveUpAt < 5 * 60_000L) return false;
        try {
            android.app.KeyguardManager km = (android.app.KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null && km.isKeyguardLocked()) return false; // can't show over the lock screen; wait for unlock
        } catch (Exception ignored) { }
        if (ADHKAR_ALLOW.contains(pkg)) return false;
        try {
            android.telecom.TelecomManager tm = (android.telecom.TelecomManager) getSystemService(Context.TELECOM_SERVICE);
            if (tm != null && pkg.equals(tm.getDefaultDialerPackage())) return false;
        } catch (Exception ignored) { }
        return true;
    }

    private void forceAdhkar() {
        long now = System.currentTimeMillis();
        if (Math.abs(now - adhkarLaunchAt) < 1500) return;
        // Crash safety: if the screen never managed to show after several tries, pause forcing for 5 minutes.
        if (store.adhkarSeenAt() >= adhkarFirstLaunch) { adhkarLaunches = 0; adhkarFirstLaunch = now; }
        if (++adhkarLaunches > 8 && now - adhkarFirstLaunch > 20_000) {
            adhkarGaveUp = store.adhkarDue(); adhkarGaveUpAt = now; adhkarLaunches = 0; adhkarFirstLaunch = now; return;
        }
        adhkarLaunchAt = now;
        try {
            startActivity(new Intent(this, AdhkarActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION));
        } catch (Exception ignored) { }
    }

    // ---------- safe search ----------
    private static final String[] URL_IDS = { "url_bar", "location_bar", "url_field", "url_view", "address_bar", "addressbar", "mozac_browser_toolbar_url_view" };
    private long safeAt = 0;
    private String lastSafe = null;

    private String urlText(AccessibilityNodeInfo root) {
        ArrayDeque<AccessibilityNodeInfo> q = new ArrayDeque<AccessibilityNodeInfo>();
        q.add(AccessibilityNodeInfo.obtain(root));
        String found = null;
        int n = 0;
        while (!q.isEmpty()) {
            AccessibilityNodeInfo x = q.poll();
            if (found == null && n++ < 250) {
                CharSequence id = x.getViewIdResourceName();
                CharSequence cls = x.getClassName();
                boolean web = cls != null && cls.toString().contains("WebView");
                if (id != null && x.isVisibleToUser() && !x.isFocused()) {
                    String sid = id.toString();
                    for (String k : URL_IDS) if (sid.contains(k)) {
                        CharSequence t = x.getText();
                        if (t != null && t.length() > 4) found = t.toString();
                        break;
                    }
                }
                if (!web) for (int i = 0; i < x.getChildCount(); i++) {
                    AccessibilityNodeInfo c = x.getChild(i);
                    if (c != null) q.add(c);
                }
            }
            x.recycle();
        }
        return found;
    }

    /** Returns the same search forced to safe mode, "" to block it, or null when nothing needs changing. */
    static String safeUrl(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.indexOf(' ') >= 0 || t.indexOf('.') < 0) return null;
        if (!t.startsWith("http://") && !t.startsWith("https://")) t = "https://" + t;
        Uri u;
        try { u = Uri.parse(t); } catch (Exception e) { return null; }
        String host = u.getHost() == null ? "" : u.getHost().toLowerCase(java.util.Locale.US);
        String path = u.getPath() == null ? "" : u.getPath();
        String key = null, val = null;
        if ((host.startsWith("google.") || host.contains(".google.")) && (path.startsWith("/search") || path.startsWith("/imgres"))) { key = "safe"; val = "active"; }
        else if (host.endsWith("bing.com") && (path.startsWith("/search") || path.startsWith("/images") || path.startsWith("/videos"))) { key = "adlt"; val = "strict"; }
        else if (host.endsWith("duckduckgo.com") && u.getQueryParameter("q") != null) { key = "kp"; val = "1"; }
        else if (host.startsWith("search.yahoo.") || host.startsWith("images.search.yahoo.") || host.startsWith("video.search.yahoo.")) { key = "vm"; val = "r"; }
        else if (host.contains("yandex.") && (path.startsWith("/images") || path.startsWith("/video"))) return "";
        if (key == null) return null;
        if (val.equals(u.getQueryParameter(key))) return null;
        Uri.Builder b = u.buildUpon().clearQuery();
        for (String name : u.getQueryParameterNames()) {
            if (name.equals(key)) continue;
            for (String v : u.getQueryParameters(name)) b.appendQueryParameter(name, v);
        }
        b.appendQueryParameter(key, val);
        return b.build().toString();
    }

    private boolean enforceSafeSearch(AccessibilityNodeInfo root, String pkg) {
        if (SystemClock.uptimeMillis() - safeAt < 2500) return false;
        String safe = safeUrl(urlText(root));
        if (safe == null) return false;
        if (safe.length() > 0 && safe.equals(lastSafe) && SystemClock.uptimeMillis() - safeAt < 60_000) return false; // engine dropped the flag: don't loop
        lastSafe = safe;
        safeAt = SystemClock.uptimeMillis();
        if (safe.length() == 0) { blockBrowser(pkg, "safesearch", L.t("بحث الصور في هذا المحرك غير مُفلتر، لذلك هو محظور.", "Image search on this engine is unfiltered, so it is blocked.")); return true; }
        store.countBlock("safesearch");
        performGlobalAction(GLOBAL_ACTION_BACK);
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(safe)).setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception ignored) { }
        toast(L.t("البحث الآمن مفروض 🛡", "Safe search enforced 🛡"));
        return true;
    }

    // ---------- kids mode ----------
    private boolean kidsBlocked(AccessibilityNodeInfo root, String pkg) {
        if (!store.flag("kids") || inCall() || isExempt(pkg) || !isLaunchable(pkg)) return false;
        if (!store.kidApps().contains(pkg)) return true;
        if (!BROWSERS.contains(pkg)) return false;
        List<String> sites = store.kidSites();
        if (sites.isEmpty()) return false;
        String u = urlText(root);
        if (u == null) return kidLastBlocked;   // no address visible: keep the last decision
        String t = u.trim().toLowerCase(java.util.Locale.US);
        if (t.indexOf(' ') >= 0 || t.indexOf('.') < 0) return kidLastBlocked = true; // a search, not an allowed site
        if (!t.startsWith("http://") && !t.startsWith("https://")) t = "https://" + t;
        String host;
        try { host = Uri.parse(t).getHost(); } catch (Exception e) { return false; }
        if (host == null) return kidLastBlocked;
        for (String site : sites) if (host.equals(site) || host.endsWith("." + site)) return kidLastBlocked = false;
        return kidLastBlocked = true;
    }
    private boolean kidLastBlocked = true;

    @Override public void onDestroy() {
        running = false;
        h.removeCallbacks(ticker);
        try { unregisterReceiver(pkgReceiver); } catch (Exception ignored) { }
        setForeground(null);
        super.onDestroy();
    }

    @Override public void onInterrupt() { }

    private final Set<String> runtimeExempt = new HashSet<String>();
    private long exemptAt = 0;

    /** Default phone, SMS, home and keyboard apps can never be blocked, limited or locked down. */
    boolean isExempt(String pkg) {
        if (pkg == null) return true;
        if (NEVER_BLOCK.contains(pkg)) return true;
        long now = SystemClock.elapsedRealtime();
        if (now - exemptAt > 60_000 || exemptAt == 0) {
            exemptAt = now;
            runtimeExempt.clear();
            try {
                android.telecom.TelecomManager tm = (android.telecom.TelecomManager) getSystemService(Context.TELECOM_SERVICE);
                if (tm != null && tm.getDefaultDialerPackage() != null) runtimeExempt.add(tm.getDefaultDialerPackage());
            } catch (Exception ignored) { }
            try {
                String sms = android.provider.Telephony.Sms.getDefaultSmsPackage(this);
                if (sms != null) runtimeExempt.add(sms);
            } catch (Exception ignored) { }
            try {
                android.content.pm.ResolveInfo ri = getPackageManager().resolveActivity(
                    new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);
                if (ri != null && ri.activityInfo != null) runtimeExempt.add(ri.activityInfo.packageName);
            } catch (Exception ignored) { }
            try {
                String ime = android.provider.Settings.Secure.getString(getContentResolver(), "default_input_method");
                if (ime != null && ime.contains("/")) runtimeExempt.add(ime.substring(0, ime.indexOf('/')));
            } catch (Exception ignored) { }
        }
        return runtimeExempt.contains(pkg);
    }

    private boolean inCall() {
        try {
            android.media.AudioManager am = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int m = am == null ? 0 : am.getMode();
            return m == android.media.AudioManager.MODE_IN_CALL || m == android.media.AudioManager.MODE_IN_COMMUNICATION
                || m == android.media.AudioManager.MODE_RINGTONE;
        } catch (Exception e) { return false; }
    }

    private boolean isLaunchable(String pkg) {
        Boolean b = launchable.get(pkg);
        if (b == null) {
            b = getPackageManager().getLaunchIntentForPackage(pkg) != null;
            launchable.put(pkg, b);
        }
        return b;
    }

    private void setForeground(String pkg) {
        long now = SystemClock.elapsedRealtime();
        if (fgPkg != null) store.addUsage(fgPkg, Math.min(now - fgSince, 120_000));
        fgPkg = pkg;
        fgSince = now;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        if (store == null || e == null || e.getPackageName() == null) return;
        String evPkg = e.getPackageName().toString();
        boolean stateChange = e.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;

        if (stateChange) {
            if (evPkg.equals("com.sec.android.app.launcher") || (isExempt(evPkg) && !evPkg.equals("com.android.systemui"))) setForeground(null);
            else if (!evPkg.equals(fgPkg) && isLaunchable(evPkg) && !evPkg.equals("com.iffahshield.app")) setForeground(evPkg);
        }

        if (store.isPaused()) return;
        long now = SystemClock.uptimeMillis();
        if (now < coolUntil) return;
        if (!stateChange && now - lastScan < 300) return;
        lastScan = now;

        // Secure Folder / similar spaces: no protection exists inside them.
        if (store.isStrict()) for (String pre : ESCAPE_PKG_PREFIXES) if (evPkg.startsWith(pre)) {
            block("settings", L.t("المجلد الآمن مقفل في الوضع الصارم، لأنه لا حماية بداخله.", "Secure Folder is locked in strict mode, because there's no protection inside it."), true);
            return;
        }

        // Scan every visible app window (split screen, pop-up view), not only the focused one.
        List<AccessibilityNodeInfo> roots = new ArrayList<AccessibilityNodeInfo>();
        List<Boolean> active = new ArrayList<Boolean>();
        try {
            List<AccessibilityWindowInfo> ws = getWindows();
            if (ws != null) for (AccessibilityWindowInfo w : ws) {
                if (w.getType() == AccessibilityWindowInfo.TYPE_APPLICATION) {
                    AccessibilityNodeInfo r = w.getRoot();
                    if (r != null) { roots.add(r); active.add(w.isActive() || w.isFocused()); }
                }
                w.recycle();
            }
        } catch (Exception ignored) { }
        if (roots.isEmpty()) {
            AccessibilityNodeInfo r = getRootInActiveWindow();
            if (r == null) return;
            roots.add(r); active.add(true);
        }
        boolean multi = roots.size() > 1;
        try {
            for (int i = 0; i < roots.size(); i++) {
                if (process(roots.get(i), active.get(i) && !multi)) break;
            }
        } finally {
            for (AccessibilityNodeInfo r : roots) r.recycle();
        }
    }

    /** Returns true if it blocked something. soloActive: the only, focused window (BACK is safe). */
    private boolean process(AccessibilityNodeInfo root, boolean soloActive) {
        CharSequence rp = root.getPackageName();
        if (rp == null) return false;
        String pkg = rp.toString();
        if (pkg.equals("com.iffahshield.app")) return false;

        if (adhkarNeeded(pkg)) { forceAdhkar(); return true; }
        if (kidsBlocked(root, pkg)) {
            if (BROWSERS.contains(pkg) && store.kidApps().contains(pkg)) blockBrowser(pkg, "kids", L.t("هذا الموقع غير مسموح في وضع الأطفال.", "This site isn't allowed in kids mode."));
            else block("kids", L.t("هذا التطبيق غير مسموح في وضع الأطفال.", "This app isn't allowed in kids mode."), true);
            return true;
        }

        if (inCall() || isExempt(pkg)) { /* never touch calls, dialer, SMS, home, keyboard */ }
        else if (store.apps().contains(pkg)) {
            block("app", L.t("هذا التطبيق محظور.", "This app is blocked."), true); return true;
        }
        if (!inCall() && store.inLockdown() && !isExempt(pkg) && !LOCKDOWN_ALLOW.contains(pkg) && isLaunchable(pkg)) {
            block("panic", L.t("وضع الطوارئ مفعّل. التطبيقات مقفلة حتى تمضي اللحظة.", "Emergency lock is on. Apps are locked until the moment passes."), true); return true;
        }
        if (!inCall() && store.inNightLock() && !isExempt(pkg) && !LOCKDOWN_ALLOW.contains(pkg) && !NIGHT_ALLOW.contains(pkg) && isLaunchable(pkg)) {
            block("night", L.t("قفل الليل مفعّل 🌙 ضع الهاتف ونم، غداً يوم جديد.", "Night lock is on 🌙 Put the phone down and sleep. Tomorrow is a new day."), true); return true;
        }
        if (!inCall() && !isExempt(pkg) && store.overLimit(pkg)) {
            block("limit", L.t("انتهى وقتك اليومي لهذا التطبيق. سيُتاح غداً.", "Your daily time for this app is up. It opens again tomorrow."), true); return true;
        }

        if (store.isStrict() && GUARDED_PKGS.contains(pkg)) {
            String t = Matcher.normalize(collect(root, 900, false));
            if (guardHit(pkg, t)) { block("settings", L.t("هذا الإعداد مقفل في الوضع الصارم.", "This setting is locked in strict mode."), true); return true; }
        }

        if (store.flag("reels") && (byIds(root, pkg, REEL_IDS) || facebookReels(root, pkg))) {
            quietBack("reels", L.t("الريلز محظورة 🛡", "Reels are blocked 🛡"), soloActive); return true;
        }
        if (store.flag("stories") && byIds(root, pkg, STORY_IDS)) {
            quietBack("stories", L.t("الستوريات محظورة 🛡", "Stories are blocked 🛡"), soloActive); return true;
        }

        if (SKIP_WORDS.contains(pkg) || isExempt(pkg) || inCall()) return false;
        boolean browser = BROWSERS.contains(pkg);

        if (browser && store.flag("incognito")) {
            String t = Matcher.normalize(collectUi(root, 400));
            for (String w : INCOGNITO) if (t.contains(w)) {
                blockBrowser(pkg, "incognito", L.t("التصفح المتخفي محظور.", "Incognito browsing is blocked.")); return true;
            }
        }

        if (browser && store.flag("safesearch") && enforceSafeSearch(root, pkg)) return true;

        if (store.flag("words")) {
            ensureMatcher();
            String t = Matcher.normalize(collect(root, browser ? 1200 : 700, browser));
            if (matcher.find(t) != null) {
                if (browser) blockBrowser(pkg, "words", L.t("تم حجب محتوى غير لائق.", "Inappropriate content was blocked."));
                else block("words", L.t("تم حجب محتوى غير لائق.", "Inappropriate content was blocked."), !soloActive);
                return true;
            }
        }
        return false;
    }

    private boolean adminOn() {
        android.app.admin.DevicePolicyManager dpm = (android.app.admin.DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm != null && dpm.isAdminActive(AdminReceiver.component(this));
    }

    private boolean notifListenerOn() {
        String v = android.provider.Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return v != null && v.contains("com.iffahshield.app/");
    }

    private boolean guardHit(String pkg, String t) {
        for (String s : SETTINGS_TERMS) if (t.contains(s)) return true;
        // Let the user ACTIVATE uninstall protection even in strict mode (deactivation stays blocked).
        if (pkg.equals("com.android.settings") && (t.contains("مسوول") || t.contains("مسؤول") || t.contains("admin"))
                && !t.contains("الغاء التثبيت") && !t.contains("uninstall") && !adminOn()) return false;
        // Let the user grant notification access to Hisn even in strict mode (it only adds protection).
        if (pkg.equals("com.android.settings") && (t.contains("اشعار") || t.contains("notification"))
                && !t.contains("الغاء التثبيت") && !t.contains("uninstall") && !notifListenerOn()) return false;
        boolean danger = false;
        for (String d : DANGER) if (t.contains(d)) { danger = true; break; }
        for (String l : OWN_LABELS) if (t.contains(l)) {
            // Any settings/installer screen naming us is blocked; launcher only when an uninstall menu is open.
            if (!pkg.equals("com.sec.android.app.launcher") || danger) return true;
        }
        if (danger) for (String l : PROTECTED_LABELS) if (t.contains(l)) return true;
        return false;
    }

    private boolean big(AccessibilityNodeInfo n) {
        Rect b = new Rect();
        n.getBoundsInScreen(b);
        return b.width() > screenW * 0.6 && b.height() > screenH * 0.3;
    }

    private boolean byIds(AccessibilityNodeInfo root, String pkg, String[][] ids) {
        for (String[] r : ids) {
            if (!r[0].equals(pkg)) continue;
            List<AccessibilityNodeInfo> l = root.findAccessibilityNodeInfosByViewId(r[1]);
            if (l == null) continue;
            boolean hit = false;
            for (AccessibilityNodeInfo n : l) {
                if (!hit && n.isVisibleToUser() && big(n)) hit = true;
                n.recycle();
            }
            if (hit) return true;
        }
        return false;
    }

    /** Facebook hides view ids, so detect the Reels tab by its accessibility labels. */
    private boolean facebookReels(AccessibilityNodeInfo root, String pkg) {
        if (!pkg.equals("com.facebook.katana") && !pkg.equals("com.facebook.lite")) return false;
        ArrayDeque<AccessibilityNodeInfo> q = new ArrayDeque<AccessibilityNodeInfo>();
        q.add(AccessibilityNodeInfo.obtain(root));
        int n = 0;
        boolean hit = false;
        while (!q.isEmpty()) {
            AccessibilityNodeInfo x = q.poll();
            if (!hit && n++ < 500) {
                CharSequence d = x.getContentDescription();
                if (d != null && x.isVisibleToUser()) {
                    String s = Matcher.normalize(d);
                    if ((s.contains("تفاصيل علامه التبويب ريلز") || s.contains("reels tab details")) && big(x)) hit = true;
                    else if ((s.startsWith("ريلز، علامه التبويب") || s.startsWith("reels, tab")) && x.isSelected()) hit = true;
                }
                for (int i = 0; i < x.getChildCount(); i++) {
                    AccessibilityNodeInfo c = x.getChild(i);
                    if (c != null) q.add(c);
                }
            }
            x.recycle();
        }
        return hit;
    }

    private void ensureMatcher() {
        if (matcher == null || builtVersion != matcherVersion) {
            matcher = new Matcher(store.allWords());
            builtVersion = matcherVersion;
        }
    }

    /** Breadth-first text collection of visible nodes, bounded for speed. */
    /**
     * Browser UI that only lists titles of pages already seen (new-tab tiles, history suggestions,
     * tab switcher). Scanning it would lock the browser forever after one blocked page, so it is
     * skipped; opening any of those pages is still blocked by its own content and URL.
     */
    private static final String[] BROWSER_CHROME_IDS = {
        "tile", "mv_tiles", "suggestion", "omnibox_results", "tab_switcher", "tab_list", "tab_grid",
        "tab_title", "most_visited", "history_", "recent_tabs", "resumption", "magic_stack", "single_tab" };

    private static boolean browserChrome(AccessibilityNodeInfo n) {
        CharSequence id = n.getViewIdResourceName();
        if (id == null) return false;
        String s = id.toString();
        for (String k : BROWSER_CHROME_IDS) if (s.contains(k)) return true;
        return false;
    }

    static String collect(AccessibilityNodeInfo root, int maxNodes) { return collect(root, maxNodes, false); }

    /** Native browser UI text only: skips web page content and menus (for incognito detection). */
    static String collectUi(AccessibilityNodeInfo root, int maxNodes) {
        StringBuilder sb = new StringBuilder(512);
        ArrayDeque<AccessibilityNodeInfo> q = new ArrayDeque<AccessibilityNodeInfo>();
        q.add(AccessibilityNodeInfo.obtain(root));
        int n = 0;
        while (!q.isEmpty()) {
            AccessibilityNodeInfo x = q.poll();
            CharSequence cls = x.getClassName();
            CharSequence vid = x.getViewIdResourceName();
            String id = vid == null ? "" : vid.toString();
            boolean web = cls != null && cls.toString().contains("WebView");
            boolean menu = id.contains("menu") || id.contains("popup");
            if (n++ < maxNodes && !web && !menu) {
                if (x.isVisibleToUser()) {
                    if ((id.contains("incognito") || id.contains("secret")) && (id.contains("ntp") || id.contains("new_tab") || id.contains("page"))) sb.append(" ntp_incognito_marker \n ");
                    CharSequence t = x.getText(), d = x.getContentDescription();
                    if (t != null && t.length() > 0) sb.append(t).append(" \n ");
                    if (d != null && d.length() > 0) sb.append(d).append(" \n ");
                }
                for (int i = 0; i < x.getChildCount(); i++) {
                    AccessibilityNodeInfo c = x.getChild(i);
                    if (c != null) q.add(c);
                }
            }
            x.recycle();
        }
        return sb.toString();
    }

    static String collect(AccessibilityNodeInfo root, int maxNodes, boolean skipBrowserChrome) {
        StringBuilder sb = new StringBuilder(2048);
        ArrayDeque<AccessibilityNodeInfo> q = new ArrayDeque<AccessibilityNodeInfo>();
        q.add(AccessibilityNodeInfo.obtain(root));
        int n = 0;
        while (!q.isEmpty()) {
            AccessibilityNodeInfo x = q.poll();
            if (n++ < maxNodes && !(skipBrowserChrome && browserChrome(x))) {
                if (x.isVisibleToUser()) {
                    CharSequence t = x.getText(), d = x.getContentDescription();
                    if (t != null && t.length() > 0) sb.append(t).append(" \n ");
                    if (d != null && d.length() > 0) sb.append(d).append(" \n ");
                }
                for (int i = 0; i < x.getChildCount(); i++) {
                    AccessibilityNodeInfo c = x.getChild(i);
                    if (c != null) q.add(c);
                }
            }
            x.recycle();
        }
        return sb.toString();
    }

    private void quietBack(String category, String msg, boolean soloActive) {
        performGlobalAction(soloActive ? GLOBAL_ACTION_BACK : GLOBAL_ACTION_HOME);
        store.countBlock(category);
        coolUntil = SystemClock.uptimeMillis() + 700;
        toast(msg);
    }

    /** Leave the bad page (BACK), then open a clean page in the same browser so it never reopens on it. */
    private void blockBrowser(String pkg, String category, String reason) {
        coolUntil = SystemClock.uptimeMillis() + 1500;
        store.countBlock(category);
        performGlobalAction(GLOBAL_ACTION_BACK);
        try {
            String home = "https://www.google.com/";
            if (store.flag("kids") && !store.kidSites().isEmpty()) home = "https://" + store.kidSites().get(0) + "/";
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(home))
                .setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception ignored) { }
        final Intent i = new Intent(this, BlockActivity.class).putExtra("reason", reason)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        h.postDelayed(new Runnable() { @Override public void run() {
            try { startActivity(i); } catch (Exception ignored) { }
        } }, 400);
    }

    private void block(String category, String reason, boolean home) {
        coolUntil = SystemClock.uptimeMillis() + 1200;
        store.countBlock(category);
        performGlobalAction(home ? GLOBAL_ACTION_HOME : GLOBAL_ACTION_BACK);
        Intent i = new Intent(this, BlockActivity.class)
            .putExtra("reason", reason)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        try { startActivity(i); } catch (Exception ignored) { }
    }

    private void toast(final String s) {
        h.post(new Runnable() { @Override public void run() {
            Toast.makeText(GuardService.this, s, Toast.LENGTH_SHORT).show();
        } });
    }
}
