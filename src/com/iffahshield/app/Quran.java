package com.iffahshield.app;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quran text from the Tanzil Project (tanzil.net), "Simple" edition, verbatim, split by Mushaf page (604 pages).
 * assets/quran.txt lines: "#P n" page start, "S|sura|name" sura start, "B|basmala", "sura|aya|text".
 */
final class Quran {
    private Quran() { }
    static final int PAGES = 604;
    private static List<List<String>> pages;
    private static Map<String, String> verses;

    private static synchronized void load(Context c) {
        if (pages != null) return;
        List<List<String>> ps = new ArrayList<List<String>>();
        Map<String, String> vs = new HashMap<String, String>(8000);
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getAssets().open("quran.txt"), "UTF-8"));
            String ln; List<String> cur = null;
            while ((ln = r.readLine()) != null) {
                if (ln.startsWith("#P ")) { cur = new ArrayList<String>(); ps.add(cur); continue; }
                if (ln.startsWith("#") || cur == null) continue;
                cur.add(ln);
                if (Character.isDigit(ln.charAt(0))) {
                    int a = ln.indexOf('|'), b = ln.indexOf('|', a + 1);
                    vs.put(ln.substring(0, b), ln.substring(b + 1));
                }
            }
            r.close();
        } catch (Exception ignored) { }
        pages = ps; verses = vs;
    }

    static String num(int n) {
        String d = String.valueOf(n); StringBuilder b = new StringBuilder();
        for (char ch : d.toCharArray()) b.append((char) ('٠' + (ch - '0')));
        return b.toString();
    }

    /** Verses sura:from..to joined with ayah markers, e.g. "... ﴿٢٥٥﴾". */
    static String verses(Context c, int sura, int from, int to) {
        load(c);
        StringBuilder b = new StringBuilder();
        for (int a = from; a <= to; a++) {
            String t = verses.get(sura + "|" + a);
            if (t == null) continue;
            if (sura != 1 && sura != 9 && a == 1) {        // the basmala is stored on the first ayah: put it on its own line
                String[] w = t.split(" ");
                if (w.length > 4) { StringBuilder bs = new StringBuilder(); for (int i = 0; i < 4; i++) bs.append(i > 0 ? " " : "").append(w[i]);
                    b.append(bs).append('\n'); t = t.substring(bs.length()).trim(); }
            }
            b.append(t).append(" ﴿").append(num(a)).append("﴾ ");
        }
        return b.toString().trim();
    }

    /** One Mushaf page (1..604) as display text. */
    static String page(Context c, int page) {
        load(c);
        if (pages.isEmpty()) return "تعذّر تحميل المصحف.";
        List<String> p = pages.get(Math.max(1, Math.min(PAGES, page)) - 1);
        StringBuilder b = new StringBuilder();
        for (String ln : p) {
            if (ln.startsWith("S|")) {
                if (b.length() > 0) b.append("\n\n");
                b.append("۞ سورة ").append(ln.substring(ln.indexOf('|', 2) + 1)).append(" ۞\n");
            } else if (ln.startsWith("B|")) {
                b.append(ln.substring(2)).append('\n');
            } else {
                int a = ln.indexOf('|'), s = ln.indexOf('|', a + 1);
                b.append(ln.substring(s + 1)).append(" ﴿").append(num(Integer.parseInt(ln.substring(a + 1, s)))).append("﴾ ");
            }
        }
        return b.toString().trim();
    }

    /** Sura name(s) on a page, for the title. */
    static String suraOf(Context c, int page) {
        load(c);
        if (pages.isEmpty()) return "";
        List<String> p = pages.get(Math.max(1, Math.min(PAGES, page)) - 1);
        String first = p.get(0);
        int s = Integer.parseInt(first.startsWith("S|") ? first.substring(2, first.indexOf('|', 2)) : first.substring(0, first.indexOf('|')));
        for (String ln : p) if (ln.startsWith("S|") && Integer.parseInt(ln.substring(2, ln.indexOf('|', 2))) == s) return ln.substring(ln.indexOf('|', 2) + 1);
        // page starts mid-sura: find its name from earlier pages
        for (int i = page - 2; i >= 0; i--) for (String ln : pages.get(i))
            if (ln.startsWith("S|" + s + "|")) return ln.substring(ln.indexOf('|', 2) + 1);
        return "";
    }
}
