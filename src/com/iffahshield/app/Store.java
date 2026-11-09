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

    public Store(Context c) {
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

    public boolean flag(String k) { return p.getBoolean(k, false); }

    /** Turning a protection OFF is refused while strict mode is on. */
    public boolean setFlag(String k, boolean v) {
        if (!v && isStrict()) return false;
        p.edit().putBoolean(k, v).apply();
        return true;
    }

    /** The user's own words (shown in the UI). */
    public List<String> keywords() { return split(p.getString("keywords", "")); }

    /** Built-in hidden list + the user's words: what the matcher actually uses. */
    public List<String> allWords() {
        List<String> a = new ArrayList<String>(Arrays.asList(BuiltinWords.LIST));
        a.addAll(keywords());
        return a;
    }

    // ---------- panic / partner / reasons ----------
    public long lockUntil() { return p.getLong("lockUntil", 0); }
    public boolean inLockdown() { return System.currentTimeMillis() < lockUntil(); }
    public void lockdown(int minutes) {
        long until = System.currentTimeMillis() + minutes * 60_000L;
        if (until > lockUntil()) p.edit().putLong("lockUntil", until).apply();
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
        long wait = p.getLong("partnerLockUntil", 0);
        if (SystemClock.elapsedRealtime() < wait) return false;
        boolean ok = sha(code).equals(p.getString("partnerHash", ""));
        int fails = ok ? 0 : p.getInt("partnerFails", 0) + 1;
        SharedPreferences.Editor e = p.edit().putInt("partnerFails", fails);
        if (fails >= 3) e.putLong("partnerLockUntil", SystemClock.elapsedRealtime() + 30 * 60_000L).putInt("partnerFails", 0);
        e.apply();
        return ok;
    }
    public List<String> apps() { return split(p.getString("apps", "")); }

    public boolean addKeyword(String w) { return addTo("keywords", w); }
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
            .putLong("lastTick", SystemClock.elapsedRealtime()).apply();
    }

    public void cancelUnlock() { p.edit().putLong("unlockRemaining", -1).apply(); }

    public long unlockRemaining() { return p.getLong("unlockRemaining", -1); }

    /** Remaining time including the part not yet saved by the service's 30s tick. */
    public long liveRemaining() {
        long r = unlockRemaining();
        if (r < 0) return r;
        long d = SystemClock.elapsedRealtime() - p.getLong("lastTick", SystemClock.elapsedRealtime());
        if (d < 0 || d > 10 * 60 * 1000) d = 0;
        return Math.max(0, r - d);
    }

    /** Called periodically by the service. Returns true when strict mode just ended. */
    public boolean tick() {
        long rem = unlockRemaining();
        long now = SystemClock.elapsedRealtime();
        long last = p.getLong("lastTick", now);
        SharedPreferences.Editor e = p.edit().putLong("lastTick", now);
        if (rem < 0 || !isStrict()) { e.apply(); return false; }
        long d = now - last;
        if (d < 0 || d > 10 * 60 * 1000) d = 0; // reboot or long gap: don't count, can't be gamed
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
}
