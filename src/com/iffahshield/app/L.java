package com.iffahshield.app;

import android.content.Context;

import java.util.Locale;

/** Arabic / English UI text. "auto" follows the phone language (Arabic phones get Arabic, others English). */
final class L {
    static boolean en;
    private L() { }

    static void init(Context c) {
        String l = new Store(c).lang();
        en = "en".equals(l) || ("auto".equals(l) && !"ar".equals(Locale.getDefault().getLanguage()));
    }

    static String t(String ar, String english) { return en ? english : ar; }
}
