package com.iffahshield.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** All persistent state. Single SharedPreferences file, no network, no content stored. */
public final class Store {
    public static final long UNLOCK_DELAY_MS = 24L * 60 * 60 * 1000; // 24h of device-on time

    // "=" prefix means whole-word match; otherwise substring match. Stored normalized-friendly.
    /** Defaults shipped in v1.0-1.2; now part of BuiltinWords, stripped from the user list on upgrade. */
    public static final String[] DEFAULT_WORDS = {
        "porn", "pornhub", "xvideos", "xnxx", "xhamster", "redtube", "youporn", "brazzers",
        "onlyfans", "fansly", "chaturbate", "stripchat", "bongacams", "livejasmin", "spankbang",
        "eporner", "rule34", "hentai", "nsfw", "nudes", "erotic", "camgirl", "playboy",
        "=sex", "=sexy", "=nude", "=naked", "=xxx", "=milf", "=boobs", "=tits", "=pussy",
        "=lingerie", "=striptease", "hot girls", "=18+",
        "سكس", "=سكسي", "اباحي", "اباحيه", "بورن", "=نيك", "=نيكني", "=بزاز", "=طيز",
        "شرموطه", "=قحبه", "=عاريه", "=عاري", "=عاريات", "سحاق", "=لواط", "افلام جنس", "=جنسي",
        "مقاطع ساخنه", "بنات ساخنه", "مساج ساخن", "+18"
    };

    public static final String[] DEFAULT_APPS = {
        "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.zhiliaoapp.musically.go"
    };

    private final SharedPreferences p;
    private final Context ctx;

    public Store(Context c) {
        ctx = c.getApplicationContext();
        p = c.getApplicationContext().getSharedPreferences("shield", Context.MODE_PRIVATE);
        if (!p.contains("init")) {
            p.edit().putBoolean("init", true)
                .putBoolean("reels", true).putBoolean("words", true).putBoolean("incognito", true)
                .putBoolean("strict", false).putLong("unlockRemaining", -1)
                .putString("keywords", "").putString("apps", join(DEFAULT_APPS))
                .putLong("streakStart", System.currentTimeMillis())
                .apply();
        }
        if (!p.contains("v3")) {
            List<String> mine = new ArrayList<String>(split(p.getString("keywords", "")));
            mine.removeAll(Arrays.asList(DEFAULT_WORDS));
            p.edit().putBoolean("v3", true).putString("keywords", join(mine.toArray(new String[0]))).apply();
        }
        if (!p.contains("v4")) {
            p.edit().putBoolean("v4", true).putBoolean("safesearch", true).putBoolean("newBrowsers", true)
                .putBoolean("adhkar", false).putBoolean("night", false).putBoolean("kids", false).apply();
        }
        if (!p.contains("v2")) {
            p.edit().putBoolean("v2", true).putBoolean("stories", false).putBoolean("newApps", false)
                .putBoolean("notif", true).putInt("countdown", 5).apply();
        }
    }

    static String join(String[] a) {
        StringBuilder b = new StringBuilder();
        for (String s : a) { if (b.length() > 0) b.append('\n'); b.append(s); }
        return b.toString();
    }

    static List<String> split(String s) {
        List<String> out = new ArrayList<String>();
        if (s == null) return out;
        for (String x : s.split("\n")) { x = x.trim(); if (x.length() > 0) out.add(x); }
        return out;
    }

    public boolean flag(String k) { return p.getBoolean(k, "images".equals(k)); }   // image guard is on by default

    /** Turning a protection OFF is refused while strict mode is on. */
    public boolean setFlag(String k, boolean v) {
        if (!v && isStrict()) return false;
        if (v && "kids".equals(k) && kidApps().isEmpty()) return false; // would lock every app
        p.edit().putBoolean(k, v).apply();
        return true;
    }

    /** The user's own words (shown in the UI). */
    public List<String> keywords() { return split(p.getString("keywords", "")); }

    /** Built-in hidden list + the user's words: what the matcher actually uses. */
    public List<String> allWords() {
        List<String> a = new ArrayList<String>(Arrays.asList(BuiltinWords.LIST));
        a.addAll(Arrays.asList(BuiltinWords2.LIST));
        a.addAll(keywords());
        return a;
    }

    // ---------- panic / partner / reasons ----------
    /** Remaining lockdown in ms (0 = none). Uses boot-relative time, so changing the clock can't end it. */
    public long lockRemaining() {
        if (p.getInt("lockBoot", -2) != boot()) return 0;
        return Math.max(0, p.getLong("lockUntilE", 0) - SystemClock.elapsedRealtime());
    }
    public long lockUntil() { return System.currentTimeMillis() + lockRemaining(); }
    public boolean inLockdown() { return lockRemaining() > 0; }
    public void lockdown(int minutes) {
        long until = SystemClock.elapsedRealtime() + minutes * 60_000L;
        if (minutes * 60_000L > lockRemaining()) p.edit().putLong("lockUntilE", until).putInt("lockBoot", boot()).apply();
    }
    public String reasons() { return p.getString("reasons", ""); }
    public void setReasons(String r) { p.edit().putString("reasons", r == null ? "" : r.trim()).apply(); }
    public String friendPhone() { return p.getString("friendPhone", ""); }
    public void setFriendPhone(String n) { p.edit().putString("friendPhone", n == null ? "" : n.trim()).apply(); }
    public int panics() { return p.getInt("panics", 0); }
    public void countPanic() { p.edit().putInt("panics", panics() + 1).apply(); }

    public boolean hasPartner() { return p.getString("partnerHash", "").length() > 0; }
    static String sha(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(("iffah:" + s).getBytes("UTF-8"));
            StringBuilder b = new StringBuilder();
            for (byte x : d) b.append(String.format(Locale.US, "%02x", x));
            return b.toString();
        } catch (Exception e) { return ""; }
    }
    /** Setting the first code is always allowed; changing/removing it needs the current code. */
    public boolean setPartnerCode(String oldCode, String newCode) {
        if (hasPartner() && !checkPartner(oldCode)) return false;
        // No new partner while strict mode is on: otherwise you could set a code and use it to unlock at once.
        if (!hasPartner() && isStrict()) return false;
        p.edit().putString("partnerHash", newCode == null || newCode.length() == 0 ? "" : sha(newCode)).apply();
        return true;
    }
    public boolean checkPartner(String code) {
        if (!hasPartner() || code == null) return false;
        long wait = p.getLong("partnerLockWall", 0), nowW = System.currentTimeMillis();
        if (nowW < wait && wait - nowW <= 30 * 60_000L) return false;
        boolean ok = sha(code).equals(p.getString("partnerHash", ""));
        int fails = ok ? 0 : p.getInt("partnerFails", 0) + 1;
        SharedPreferences.Editor e = p.edit().putInt("partnerFails", fails);
        if (fails >= 3) e.putLong("partnerLockWall", System.currentTimeMillis() + 30 * 60_000L).putInt("partnerFails", 0);
        e.apply();
        return ok;
    }
    public List<String> apps() { return split(p.getString("apps", "")); }

    /** At least 3 real characters, so one short word can't brick the phone in strict mode. */
    public boolean addKeyword(String w) {
        if (w == null) return false;
        String core = w.trim();
        if (core.startsWith("=")) core = core.substring(1).trim();
        if (core.length() < 3) return false;
        return addTo("keywords", w);
    }
    public boolean addApp(String pkg) { return addTo("apps", pkg); }
    public boolean removeKeyword(String w) { return removeFrom("keywords", w); }
    public boolean removeApp(String pkg) { return removeFrom("apps", pkg); }

    private boolean addTo(String key, String v) {
        v = v.trim();
        if (v.length() == 0) return false;
        Set<String> s = new LinkedHashSet<String>(split(p.getString(key, "")));
        if (!s.add(v)) return false;
        p.edit().putString(key, join(s.toArray(new String[0]))).apply();
        return true;
    }

    private boolean removeFrom(String key, String v) {
        if (isStrict()) return false;
        Set<String> s = new LinkedHashSet<String>(split(p.getString(key, "")));
        if (!s.remove(v)) return false;
        p.edit().putString(key, join(s.toArray(new String[0]))).apply();
        return true;
    }

    // ---------- block screen ----------
    public int countdown() { return p.getInt("countdown", 5); }
    /** In strict mode the countdown can only go up. */
    public boolean setCountdown(int sec) {
        if (isStrict() && sec < countdown()) return false;
        p.edit().putInt("countdown", sec).apply();
        return true;
    }
    public String customMsg() { return p.getString("customMsg", ""); }
    public void setCustomMsg(String m) { p.edit().putString("customMsg", m == null ? "" : m.trim()).apply(); }

    // ---------- daily time limits ----------
    /** pkg -> minutes per day */
    public java.util.Map<String, Integer> limits() {
        java.util.Map<String, Integer> m = new java.util.LinkedHashMap<String, Integer>();
        for (String l : split(p.getString("limits", ""))) {
            int i = l.lastIndexOf('=');
            if (i <= 0) continue;
            try { m.put(l.substring(0, i), Integer.parseInt(l.substring(i + 1))); } catch (Exception ignored) { }
        }
        return m;
    }
    /** Strict mode: a limit can be added or lowered, never raised or removed (minutes<=0 means remove). */
    public boolean setLimit(String pkg, int minutes) {
        java.util.Map<String, Integer> m = limits();
        Integer old = m.get(pkg);
        if (isStrict() && old != null && (minutes <= 0 || minutes > old)) return false;
        if (minutes <= 0) m.remove(pkg); else m.put(pkg, minutes);
        StringBuilder b = new StringBuilder();
        for (java.util.Map.Entry<String, Integer> e : m.entrySet()) {
            if (b.length() > 0) b.append('\n');
            b.append(e.getKey()).append('=').append(e.getValue());
        }
        p.edit().putString("limits", b.toString()).apply();
        return true;
    }
    public long usedToday(String pkg) { return p.getLong("use_" + today() + "_" + pkg, 0); }
    public void addUsage(String pkg, long ms) {
        if (ms <= 0) return;
        String k = "use_" + today() + "_" + pkg;
        p.edit().putLong(k, p.getLong(k, 0) + ms).apply();
    }
    /** Drop usage counters from previous days (called a few times a day). */
    public void pruneUsage() {
        String keep = "use_" + today() + "_";
        SharedPreferences.Editor e = p.edit();
        for (String k : p.getAll().keySet()) if (k.startsWith("use_") && !k.startsWith(keep)) e.remove(k);
        e.apply();
    }

    public boolean overLimit(String pkg) {
        Integer m = limits().get(pkg);
        return m != null && usedToday(pkg) >= m * 60_000L;
    }

    // ---------- strict mode ----------
    public boolean isStrict() { return p.getBoolean("strict", false); }

    public void enableStrict() {
        p.edit().putBoolean("strict", true).putLong("unlockRemaining", -1)
            .putBoolean("reels", true).putBoolean("words", true).putBoolean("incognito", true)
            .putBoolean("notif", true).apply();
    }

    /** Starts the 24h countdown. Counted only while the phone is on, so changing the clock does nothing. */
    public void requestUnlock() {
        if (!isStrict() || unlockRemaining() >= 0) return;
        p.edit().putLong("unlockRemaining", UNLOCK_DELAY_MS)
            .putLong("lastTick", SystemClock.elapsedRealtime()).putInt("lastBoot", boot()).apply();
    }

    public void cancelUnlock() { p.edit().putLong("unlockRemaining", -1).apply(); }

    public long unlockRemaining() { return p.getLong("unlockRemaining", -1); }

    /** Boot counter: lets timers count real powered-on time (sleep included) but not time while switched off. */
    int boot() {
        try { return android.provider.Settings.Global.getInt(ctx.getContentResolver(), "boot_count"); }
        catch (Exception e) { return -1; }
    }

    /** Remaining time including the part not yet saved by the service's 30s tick. */
    public long liveRemaining() {
        long r = unlockRemaining();
        if (r < 0) return r;
        long d = SystemClock.elapsedRealtime() - p.getLong("lastTick", SystemClock.elapsedRealtime());
        if (d < 0 || boot() != p.getInt("lastBoot", boot())) d = 0;
        return Math.max(0, r - d);
    }

    /** Called periodically by the service. Returns true when strict mode just ended. */
    public boolean tick() {
        long rem = unlockRemaining();
        long now = SystemClock.elapsedRealtime();
        long last = p.getLong("lastTick", now);
        int b = boot();
        boolean sameBoot = b == p.getInt("lastBoot", b) && b != -1;
        SharedPreferences.Editor e = p.edit().putLong("lastTick", now).putInt("lastBoot", b);
        if (rem < 0 || !isStrict()) { e.apply(); return false; }
        long d = now - last;
        // Same boot: elapsedRealtime includes deep sleep, so the phone was on the whole time. New boot: don't count.
        if (d < 0 || !sameBoot) d = 0;
        rem -= d;
        if (rem <= 0) {
            e.putBoolean("strict", false).putLong("unlockRemaining", -1).apply();
            return true;
        }
        e.putLong("unlockRemaining", rem).apply();
        return false;
    }

    public void forceStrictOff() { p.edit().putBoolean("strict", false).putLong("unlockRemaining", -1).apply(); }

    // ---------- adb pause (emergency only) ----------
    public void pauseFor(long ms) { p.edit().putLong("pauseUntil", System.currentTimeMillis() + ms).apply(); }
    public void resume() { p.edit().putLong("pauseUntil", 0).apply(); }
    public boolean isPaused() { return System.currentTimeMillis() < p.getLong("pauseUntil", 0); }

    // ---------- stats (counts only, never content) ----------
    private static String today() { return new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date()); }

    // ---------- tamper log: times the guard service was off while the phone was in use ----------
    public void heartbeat(boolean interactive) {
        p.edit().putLong("aliveWall", System.currentTimeMillis()).putBoolean("aliveAwake", interactive).putInt("aliveBoot", boot()).apply();
    }
    /** Called when the guard service (re)starts. Logs the gap if it died while the screen was on. */
    public void checkGap() {
        long last = p.getLong("aliveWall", 0), now = System.currentTimeMillis();
        int b = boot();
        if (last > 0 && p.getBoolean("aliveAwake", false) && b != -1 && b == p.getInt("aliveBoot", -2) && now - last > 3 * 60_000L)
            logTamper(L.t("خدمة الحماية كانت متوقفة ", "The protection service was off for ") + ((now - last) / 60_000L) + L.t(" دقيقة", " min"));
        heartbeat(true);
    }
    public void logTamper(String msg) {
        String line = new SimpleDateFormat("MM-dd HH:mm", Locale.US).format(new Date()) + "  " + msg;
        String all = p.getString("tamperLog", "");
        if (all.startsWith(line)) return;
        String[] old = all.length() == 0 ? new String[0] : all.split("\n");
        StringBuilder sb = new StringBuilder(line);
        for (int i = 0; i < old.length && i < 19; i++) sb.append('\n').append(old[i]);
        p.edit().putString("tamperLog", sb.toString()).putInt("tamperCount", p.getInt("tamperCount", 0) + 1).apply();
    }
    public String tamperLog() { return p.getString("tamperLog", ""); }
    public int tamperCount() { return p.getInt("tamperCount", 0); }

    // ---------- 40-day journey ----------
    public int journeyDay() { return p.getInt("journeyDay", 0); }
    public boolean journeyDoneToday() { return today().equals(p.getString("journeyDate", "")); }
    public void completeJourney() {
        if (journeyDoneToday()) return;
        p.edit().putInt("journeyDay", Math.min(Journey.DAYS.length, journeyDay() + 1)).putString("journeyDate", today()).apply();
    }

    // ---------- daily self check ----------
    /** 0 = clean day, 1 = fought an urge and won, 2 = slipped; -1 = not answered. */
    public int checkin(String day) { return p.getInt("ci:" + day, -1); }
    public int checkinToday() { return checkin(today()); }
    public void setCheckin(int v) { p.edit().putInt("ci:" + today(), v).apply(); }
    public String lastDays(int n) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd", Locale.US);
        for (int i = n - 1; i >= 0; i--) {
            int v = checkin(f.format(new Date(System.currentTimeMillis() - i * 86400000L)));
            sb.append(v == 0 ? "🟢" : v == 1 ? "💪" : v == 2 ? "🔴" : "⚪");
        }
        return sb.toString();
    }
    public void logUrgeHour() {
        int hr = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        p.edit().putInt("uh:" + hr, p.getInt("uh:" + hr, 0) + 1).apply();
    }
    /** The hour of day with most urges, or -1 while there is too little data. */
    public int riskyHour() {
        int best = -1, max = 0, total = 0;
        for (int i = 0; i < 24; i++) { int c = p.getInt("uh:" + i, 0); total += c; if (c > max) { max = c; best = i; } }
        return total >= 3 ? best : -1;
    }

    public void countBlock(String category) {
        String t = today();
        int todayCount = t.equals(p.getString("todayDate", "")) ? p.getInt("today", 0) : 0;
        p.edit().putString("todayDate", t).putInt("today", todayCount + 1)
            .putInt("total", p.getInt("total", 0) + 1)
            .putInt("cat_" + category, p.getInt("cat_" + category, 0) + 1).apply();
    }

    public int todayCount() { return today().equals(p.getString("todayDate", "")) ? p.getInt("today", 0) : 0; }
    public int total() { return p.getInt("total", 0); }
    public int cat(String c) { return p.getInt("cat_" + c, 0); }

    public int streakDays() {
        long d = System.currentTimeMillis() - p.getLong("streakStart", System.currentTimeMillis());
        return (int) Math.max(0, d / (24L * 60 * 60 * 1000));
    }

    public void resetStreak() { p.edit().putLong("streakStart", System.currentTimeMillis()).apply(); }
    public int bestStreak() { return Math.max(p.getInt("bestStreak", 0), streakDays()); }
    public void saveBest() { p.edit().putInt("bestStreak", bestStreak()).apply(); }

    // ---------- language ("auto" follows the phone) ----------
    public String lang() { return p.getString("lang", "auto"); }
    public void setLang(String l) { p.edit().putString("lang", l).apply(); }

    // ---------- adhkar (optional; when on, the phone opens only after finishing them) ----------
    public static final int MORNING_END = 14 * 60, EVENING_END = 4 * 60;
    public int morningStart() { return p.getInt("mStart", 7 * 60 + 30); }
    public int eveningStart() { return p.getInt("eStart", 21 * 60); }
    public void setAdhkarTimes(int m, int e) { p.edit().putInt("mStart", m).putInt("eStart", e).apply(); }

    /** Session key for "now": "m:yyyyMMdd" / "e:yyyyMMdd", or null outside both windows. */
    public String sessionNow() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        int m = c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE);
        SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd", Locale.US);
        if (m >= morningStart() && m < MORNING_END) return "m:" + f.format(c.getTime());
        if (m >= eveningStart()) return "e:" + f.format(c.getTime());
        if (m < EVENING_END) { c.add(java.util.Calendar.DAY_OF_MONTH, -1); return "e:" + f.format(c.getTime()); }
        return null;
    }
    public String adhkarDue() {
        if (!flag("adhkar")) return null;
        String k = sessionNow();
        if (k == null || p.getBoolean("dk_" + k, false)) return null;
        return k;
    }
    public void adhkarDone(String key) {
        p.edit().putBoolean("dk_" + key, true).remove("dpos_" + key).remove("dcnt_" + key)
            .putInt("adhkarTotal", p.getInt("adhkarTotal", 0) + 1).apply();
        pruneAdhkar();
    }
    public int adhkarTotal() { return p.getInt("adhkarTotal", 0); }
    public int adhkarPos(String key) { return p.getInt("dpos_" + key, 0); }
    public int adhkarCount(String key) { return p.getInt("dcnt_" + key, 0); }
    public void saveAdhkar(String key, int pos, int cnt) { p.edit().putInt("dpos_" + key, pos).putInt("dcnt_" + key, cnt).apply(); }
    private void pruneAdhkar() {
        SharedPreferences.Editor e = p.edit();
        String min = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date(System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000));
        for (String k : p.getAll().keySet()) {
            int i = k.indexOf(':');
            if ((k.startsWith("dk_") || k.startsWith("dpos_") || k.startsWith("dcnt_")) && i > 0 && k.substring(i + 1).compareTo(min) < 0) e.remove(k);
        }
        e.apply();
    }
    public void adhkarSeen() { p.edit().putLong("adhkarSeen", System.currentTimeMillis()).apply(); }
    public long adhkarSeenAt() { return p.getLong("adhkarSeen", 0); }

    // ---------- night lock ----------
    public int nightStart() { return p.getInt("nightStart", 0); }
    public int nightEnd() { return p.getInt("nightEnd", 5); }
    /** Turning it on (or longer) is always allowed; off or shorter only outside strict mode. */
    public boolean setNight(boolean on, int start, int end) {
        if (isStrict() && flag("night")) {
            if (!on) return false;
            for (int h = nightStart(); h != nightEnd(); h = (h + 1) % 24)
                if (!inWindow(h, start, end)) return false;
        }
        p.edit().putBoolean("night", on).putInt("nightStart", start).putInt("nightEnd", end).apply();
        return true;
    }
    static boolean inWindow(int h, int s, int e) { return s <= e ? (h >= s && h < e) : (h >= s || h < e); }
    public boolean inNightLock() {
        if (!flag("night")) return false;
        int h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        int s = nightStart(), e = nightEnd();
        return s <= e ? (h >= s && h < e) : (h >= s || h < e);
    }

    // ---------- kids mode: only allowed apps and sites open ----------
    public List<String> kidApps() { return split(p.getString("kidApps", "")); }
    public List<String> kidSites() { return split(p.getString("kidSites", "")); }
    /** Adding to the allow lists widens access, so it is refused in strict mode. */
    public boolean addKidApp(String pkg) { return !(isStrict() && flag("kids")) && addTo("kidApps", pkg); }
    public boolean removeKidApp(String pkg) { return removeFrom("kidApps", pkg); }
    public boolean addKidSite(String host) {
        host = host == null ? "" : host.trim().toLowerCase(Locale.US).replaceFirst("^https?://", "").replaceFirst("^www\\.", "");
        int sl = host.indexOf('/'); if (sl > 0) host = host.substring(0, sl);
        return host.contains(".") && !(isStrict() && flag("kids")) && addTo("kidSites", host);
    }
    public boolean removeKidSite(String h) { return removeFrom("kidSites", h); }

    // ---------- recovery: urge triggers and slips (counts only) ----------
    public static final String[] TRIGGERS_AR = { "ملل/فراغ", "وحدة", "تعب/نعاس", "توتر/حزن", "في السرير/الليل", "صورة أو مقطع رأيته" };
    public static final String[] TRIGGERS_EN = { "Boredom", "Loneliness", "Tiredness", "Stress/sadness", "In bed/at night", "Something I saw" };
    public static String[] triggers() { return L.en ? TRIGGERS_EN : TRIGGERS_AR; }
    public void logTrigger(int i) { logUrgeHour(); p.edit().putInt("trig_" + i, p.getInt("trig_" + i, 0) + 1).apply(); }
    public int trigger(int i) { return p.getInt("trig_" + i, 0); }
    public void logUrgeWon() { p.edit().putInt("urgesWon", p.getInt("urgesWon", 0) + 1).apply(); }
    public int urgesWon() { return p.getInt("urgesWon", 0); }
    public void logSlip() { p.edit().putInt("slips", p.getInt("slips", 0) + 1).apply(); saveBest(); resetStreak(); }
    public String plan() { return p.getString("plan", ""); }
    public void setPlan(String s) { p.edit().putString("plan", s == null ? "" : s.trim()).apply(); }
}
