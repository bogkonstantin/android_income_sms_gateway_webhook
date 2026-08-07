package tech.bogomolov.incomingsmsgateway;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Receives readable notifications when an OEM does not deliver SMS_RECEIVED.
 * The user must explicitly enable notification access in Android settings; no
 * runtime permission can grant this access silently.
 */
public class NotificationListener extends NotificationListenerService {

    private static final String TAG = "NotificationListener";
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";

    private static final String[] BODY_KEYS = new String[]{
            Notification.EXTRA_TEXT,
            Notification.EXTRA_BIG_TEXT,
            Notification.EXTRA_SUB_TEXT,
            Notification.EXTRA_INFO_TEXT,
            Notification.EXTRA_SUMMARY_TEXT
    };

    private static final String[] REDACTED_TEXTS = new String[]{
            "Sensitive notification content hidden",
            "已隐藏敏感通知内容",
            "系統已隱藏含有私密資訊的通知內容",
            "敏感通知内容已隐藏"
    };

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.i(TAG, "notification listener connected");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        if (sbn == null || BuildConfig.APPLICATION_ID.equals(sbn.getPackageName())) {
            return;
        }

        Notification notification = sbn.getNotification();
        if (notification == null) {
            return;
        }

        int flags = notification.flags;
        if ((flags & Notification.FLAG_FOREGROUND_SERVICE) != 0
                || (flags & Notification.FLAG_ONGOING_EVENT) != 0) {
            return;
        }

        Bundle extras = notification.extras;
        if (extras == null) {
            Log.i(TAG, "notification has no extras, pkg=" + sbn.getPackageName());
            return;
        }

        String title = firstText(extras, Notification.EXTRA_TITLE, Notification.EXTRA_TITLE_BIG);
        String body = collectBody(extras);
        if (body.isEmpty()) {
            Log.i(TAG, "notification has no readable body, pkg=" + sbn.getPackageName());
            return;
        }
        // Keep this at INFO because the in-app syslog intentionally filters
        // out verbose logcat output. The body itself is never written to logs.
        Log.i(TAG, "notification posted pkg=" + sbn.getPackageName()
                + ", title=" + title + ", bodyLength=" + body.length());
        if (isRedacted(body)) {
            Log.w(TAG, "notification body is redacted, pkg=" + sbn.getPackageName());
            return;
        }

        String sender = title.trim().isEmpty() ? sbn.getPackageName() : title.trim();
        long timestamp = sbn.getPostTime() > 0 ? sbn.getPostTime() : System.currentTimeMillis();

        Log.i(TAG, "Notification received pkg=" + sbn.getPackageName()
                + ", sender=" + sender + ", bodyLength=" + body.length());

        SmsBroadcastReceiver.forwardNotificationMessage(
                getApplicationContext(),
                sender,
                body,
                timestamp
        );
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.w(TAG, "notification listener disconnected");
        requestRebindIfEnabled(getApplicationContext(), "listener disconnected");
    }

    /**
     * Rebinds after an OEM has killed the listener process. The framework only
     * accepts requestRebind when the user still grants notification access, so
     * check that secure setting first and make the recovery attempt observable
     * in the in-app syslog.
     */
    static void requestRebindIfEnabled(Context context, String reason) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }
        if (!isNotificationAccessEnabled(context)) {
            Log.i(TAG, "notification access is not enabled; skip rebind (" + reason + ")");
            return;
        }

        try {
            NotificationListenerService.requestRebind(
                    new ComponentName(context, NotificationListener.class));
            Log.i(TAG, "requested notification listener rebind (" + reason + ")");
        } catch (RuntimeException e) {
            Log.e(TAG, "failed to request notification listener rebind (" + reason + ")", e);
        }
    }

    static boolean isNotificationAccessEnabled(Context context) {
        String enabled = Settings.Secure.getString(
                context.getContentResolver(), ENABLED_NOTIFICATION_LISTENERS);
        if (enabled == null || enabled.isEmpty()) {
            return false;
        }

        ComponentName expected = new ComponentName(context, NotificationListener.class);
        String[] components = enabled.split(":");
        for (String component : components) {
            ComponentName enabledComponent = ComponentName.unflattenFromString(component);
            if (expected.equals(enabledComponent)) {
                return true;
            }
        }
        return false;
    }

    static String collectBody(Bundle extras) {
        Set<String> values = new LinkedHashSet<>();
        for (String key : BODY_KEYS) {
            CharSequence value = extras.getCharSequence(key);
            if (value != null && !value.toString().trim().isEmpty()) {
                values.add(value.toString().trim());
            }
        }

        CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (lines != null) {
            for (CharSequence line : lines) {
                if (line != null && !line.toString().trim().isEmpty()) {
                    values.add(line.toString().trim());
                }
            }
        }

        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) {
                result.append('\n');
            }
            result.append(value);
        }
        return result.toString();
    }

    private static String firstText(Bundle extras, String... keys) {
        for (String key : keys) {
            CharSequence value = extras.getCharSequence(key);
            if (value != null && !value.toString().trim().isEmpty()) {
                return value.toString().trim();
            }
        }
        return "";
    }

    private static boolean isRedacted(String body) {
        for (String redacted : REDACTED_TEXTS) {
            if (body.contains(redacted)) {
                return true;
            }
        }
        return false;
    }
}
