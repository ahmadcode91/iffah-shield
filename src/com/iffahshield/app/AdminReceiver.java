package com.iffahshield.app;

import android.app.admin.DeviceAdminReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/** Device admin: while active, Android refuses to uninstall Hisn (even via adb). No policies used. */
public class AdminReceiver extends DeviceAdminReceiver {
    static ComponentName component(Context c) { return new ComponentName(c, AdminReceiver.class); }

    @Override public CharSequence onDisableRequested(Context c, Intent i) {
        L.init(c);
        return L.t("هذا التطبيق يحميك. إن كانت لحظة ضعف فستمضي. عد واضغط «أشعر بالضعف الآن».", "This app protects you. If it's a weak moment, it will pass. Go back and tap \"I feel weak right now\".");
    }
}
