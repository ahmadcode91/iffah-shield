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

    @Override public void onListenerConnected() {
        store = new Store(this);
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
