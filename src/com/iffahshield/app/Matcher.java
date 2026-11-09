package com.iffahshield.app;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Arabic/English-aware multi-keyword matcher (Aho-Corasick: one pass over the text for all words).
 * "=word" means whole word; otherwise substring. Also catches leetspeak (p0rn, s3x).
 */
public final class Matcher {
    private final List<HashMap<Character, Integer>> next = new ArrayList<HashMap<Character, Integer>>();
    private final List<Integer> fail = new ArrayList<Integer>();
    private final List<List<int[]>> out = new ArrayList<List<int[]>>(); // {patternIndex}
    private final List<String> pats = new ArrayList<String>();
    private final List<Boolean> whole = new ArrayList<Boolean>();

    public Matcher(List<String> words) {
        newNode();
        for (String w : words) {
            boolean isWhole = w.startsWith("=");
            String n = normalize(isWhole ? w.substring(1) : w).trim();
            if (n.length() == 0) continue;
            int idx = pats.size();
            pats.add(n); whole.add(isWhole);
            int s = 0;
            for (int i = 0; i < n.length(); i++) {
                char c = n.charAt(i);
                Integer t = next.get(s).get(c);
                if (t == null) { t = newNode(); next.get(s).put(c, t); }
                s = t;
            }
            out.get(s).add(new int[] { idx });
        }
        // BFS failure links
        ArrayDeque<Integer> q = new ArrayDeque<Integer>();
        for (Integer t : next.get(0).values()) { fail.set(t, 0); q.add(t); }
        while (!q.isEmpty()) {
            int r = q.poll();
            for (java.util.Map.Entry<Character, Integer> e : next.get(r).entrySet()) {
                char c = e.getKey(); int u = e.getValue();
                q.add(u);
                int f = fail.get(r);
                while (f != 0 && !next.get(f).containsKey(c)) f = fail.get(f);
                Integer g = next.get(f).get(c);
                fail.set(u, (g != null && g != u) ? g : 0);
                out.get(u).addAll(out.get(fail.get(u)));
            }
        }
    }

    private int newNode() {
        next.add(new HashMap<Character, Integer>());
        fail.add(0);
        out.add(new ArrayList<int[]>());
        return next.size() - 1;
    }

    public int size() { return pats.size(); }

    public static String normalize(CharSequence raw) {
        // NFKC folds full-width / stylised letters (e.g. math alphanumerics) into plain ones.
        String in = java.text.Normalizer.normalize(raw, java.text.Normalizer.Form.NFKC);
        StringBuilder b = new StringBuilder(in.length());
        boolean space = false;
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            if (c >= 'ً' && c <= 'ٟ') continue;      // tashkeel
            if (c == 'ٰ' || c == 'ـ') continue;      // superscript alef, tatweel
            if (c == '‏' || c == '‎' || c == '‌' || c == '‍' || c == '­') continue;
            switch (c) {
                case 'أ': case 'إ': case 'آ': case 'ٱ': c = 'ا'; break;
                case 'ة': c = 'ه'; break;
                case 'ى': c = 'ي'; break;
                case 'ؤ': c = 'و'; break;
                case 'ئ': c = 'ي'; break;
                case 'ک': c = 'ك'; break;
                case 'ی': c = 'ي'; break;
                default: c = Character.toLowerCase(c);
            }
            if (Character.isWhitespace(c) || Character.isSpaceChar(c)) {
                if (!space) b.append(' ');
                space = true;
            } else { b.append(c); space = false; }
        }
        return b.toString();
    }

    private static String leet(String t) {
        StringBuilder b = null;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i), r = c;
            switch (c) {
                case '0': r = 'o'; break; case '1': r = 'i'; break; case '3': r = 'e'; break;
                case '4': r = 'a'; break; case '5': r = 's'; break; case '@': r = 'a'; break;
                case '$': r = 's'; break; case '7': r = 't'; break;
            }
            if (r != c && b == null) { b = new StringBuilder(t); }
            if (b != null) b.setCharAt(i, r);
        }
        return b == null ? null : b.toString();
    }

    /** Returns the matched keyword or null. Input must already be normalized. */
    public String find(String text) {
        String r = scan(text);
        if (r != null) return r;
        String l = leet(text);
        if (l == null) return null;
        r = scan(l);
        if (r != null) return r;
        return text.indexOf('1') >= 0 ? scan(l.replace('i', 'l')) : null;
    }

    private String scan(String text) {
        int s = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            while (s != 0 && !next.get(s).containsKey(c)) s = fail.get(s);
            Integer t = next.get(s).get(c);
            s = t == null ? 0 : t;
            for (int[] o : out.get(s)) {
                String p = pats.get(o[0]);
                if (!whole.get(o[0])) return p;
                int st = i - p.length() + 1, e = i + 1;
                boolean okL = st == 0 || !Character.isLetterOrDigit(text.charAt(st - 1));
                boolean okR = e >= text.length() || !Character.isLetterOrDigit(text.charAt(e));
                if (okL && okR) return p;
            }
        }
        return null;
    }
}
