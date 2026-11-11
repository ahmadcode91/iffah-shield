package com.iffahshield.app;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/** Removes notifications from blocked apps, and any notification containing a blocked word. */
public class NotifService extends NotificationListenerService {
    private Store store;
    private Matcher matcher;
    private long builtVersion = -1;

    // Watchdog: this listener lives on when the guard (accessibility) service is switched off, so it can notice.
    private final android.os.Handler wh = new android.os.Handler(android.os.Looper.getMainLooper());
    private boolean wasOff = false;
    private final Runnable watchdog = new Runnable() { @Override public void run() {
        try {
            if (store == null) store = new Store(NotifService.this);
            String v = android.provider.Settings.Secure.getString(getContentResolver(), "enabled_accessibility_services");
            boolean on = v != null && v.contains(getPackageName() + "/");
            if (!on && !wasOff && store.isStrict()) {
                L.init(NotifService.this);
                store.logTamper(L.t("تم إيقاف خدمة الحماية من الإعدادات", "The protection service was switched off in Settings"));
                android.widget.Toast.makeText(NotifService.this, L.t("⚠️ حماية درع العفّة متوقفة! أعد تفعيلها من إمكانية الوصول.", "⚠️ Iffah Shield protection is off! Turn it back on in Accessibility."), android.widget.Toast.LENGTH_LONG).show();
            }
            if (on && wasOff) store.logTamper(L.t("عادت خدمة الحماية للعمل", "The protection service is back on"));
            wasOff = !on;
        } catch (Exception ignored) { }
        wh.postDelayed(this, 20_000);
    } };

    @Override public void onListenerConnected() {
        store = new Store(this);
        wh.removeCallbacks(watchdog);
        wh.post(watchdog);
        try {
            StatusBarNotification[] all = getActiveNotifications();
            if (all != null) for (StatusBarNotification s : all) check(s);
        } catch (Exception ignored) { }
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        try { check(sbn); } catch (Exception ignored) { }
    }

    private void check(StatusBarNotification sbn) {
        if (store == null) store = new Store(this);
        if (!store.flag("notif") || store.isPaused()) return;
        String pkg = sbn.getPackageName();
        if (GuardService.NEVER_BLOCK.contains(pkg) || GuardService.SKIP_WORDS.contains(pkg)) return;
        boolean kill = store.apps().contains(pkg) || store.overLimit(pkg);
        if (!kill && store.flag("words")) {
            if (matcher == null || builtVersion != GuardService.matcherVersion) {
                matcher = new Matcher(store.allWords());
                builtVersion = GuardService.matcherVersion;
            }
            Bundle x = sbn.getNotification().extras;
            StringBuilder b = new StringBuilder();
            if (x != null) for (String k : new String[] { Notification.EXTRA_TITLE, Notification.EXTRA_TEXT,
                    Notification.EXTRA_BIG_TEXT, Notification.EXTRA_SUB_TEXT, Notification.EXTRA_SUMMARY_TEXT }) {
                CharSequence c = x.getCharSequence(k);
                if (c != null) b.append(c).append(" \n ");
            }
            CharSequence tk = sbn.getNotification().tickerText;
            if (tk != null) b.append(tk);
            kill = matcher.find(Matcher.normalize(b)) != null;
        }
        if (kill) {
            cancelNotification(sbn.getKey());
            store.countBlock("notif");
        }
    }
}
