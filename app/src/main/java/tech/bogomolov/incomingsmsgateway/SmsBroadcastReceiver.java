package tech.bogomolov.incomingsmsgateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.work.Data;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SmsBroadcastReceiver extends BroadcastReceiver {

    private static final long FORWARD_DEDUPE_WINDOW_MS = 30_000L;
    private static final int MAX_DEDUPE_ENTRIES = 512;
    private static final Map<String, RecentForwarding> RECENT_FORWARDINGS = new ConcurrentHashMap<>();

    private static final class RecentForwarding {
        private final String source;
        private final long timestamp;

        private RecentForwarding(String source, long timestamp) {
            this.source = source;
            this.timestamp = timestamp;
        }
    }

    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        Log.i("SmsBroadcastReceiver", "SMS_RECEIVED broadcast received");

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            Log.w("SmsBroadcastReceiver", "SMS broadcast has no extras");
            return;
        }

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null || pdus.length == 0) {
            Log.w("SmsBroadcastReceiver", "SMS broadcast has no PDUs");
            return;
        }

        String format = bundle.getString("format");
        StringBuilder content = new StringBuilder();
        SmsMessage firstMessage = null;
        for (int i = 0; i < pdus.length; i++) {
            SmsMessage message = parseMessage((byte[]) pdus[i], format);
            if (message == null) {
                continue;
            }
            if (firstMessage == null) {
                firstMessage = message;
            }
            content.append(message.getDisplayMessageBody());
        }

        if (firstMessage == null) {
            Log.e("SmsBroadcastReceiver", "Unable to parse any SMS PDU (format=" + format + ")");
            return;
        }

        ArrayList<ForwardingConfig> configs = ForwardingConfig.getAll(context);
        String asterisk = context.getString(R.string.asterisk);

        String sender = firstMessage.getOriginatingAddress();
        if (sender == null || sender.trim().isEmpty()) {
            // Some devices omit the originating address for short-code or
            // alphanumeric SMS. Keep wildcard rules usable instead of dropping the SMS.
            Log.w("SmsBroadcastReceiver", "SMS has no originating address (format=" + format + ")");
            sender = "Unknown sender";
        }

        // Do not log the SMS body: verification codes are sensitive. These fields
        // are enough to compare normal and service messages in logcat.
        Log.i("SmsBroadcastReceiver", "Received SMS format=" + format
                + ", parts=" + pdus.length
                + ", sender=" + sender
                + ", bodyLength=" + content.length()
                + ", rules=" + configs.size());

        int slotId = this.detectSim(bundle) + 1;
        String slotName = "undetected";
        if (slotId < 0) {
            slotId = 0;
        }
        if (slotId > 0) {
            slotName = "sim" + slotId;
        }

        forwardMessage(context, sender, content.toString(), slotName, slotId,
                firstMessage.getTimestampMillis(), "sms", this);
    }

    /**
     * Sends a message through all matching rules. Both SMS_RECEIVED and the
     * notification-listener fallback use this method so sender/filter/HMAC/
     * retry behaviour cannot drift between the two input paths.
     *
     * @param simSlot one-based SIM slot, or 0 when the source has no SIM data
     * @param source diagnostic source label (for example "sms" or "notification")
     */
    static boolean forwardMessage(Context context, String sender, String content,
                                  String slotName, int simSlot, long timeStamp, String source) {
        return forwardMessage(context, sender, content, slotName, simSlot, timeStamp,
                source, null);
    }

    static boolean forwardNotificationMessage(Context context, String sender, String content,
                                               long timeStamp) {
        return forwardMessage(context, sender, content, "notification", 0, timeStamp,
                "notification", null);
    }

    private static boolean forwardMessage(Context context, String sender, String content,
                                          String slotName, int simSlot, long timeStamp,
                                          String source, SmsBroadcastReceiver receiver) {
        if (context == null || content == null || content.isEmpty()) {
            return false;
        }

        ArrayList<ForwardingConfig> configs = ForwardingConfig.getAll(context);
        String asterisk = context.getString(R.string.asterisk);
        boolean forwarded = false;

        for (ForwardingConfig config : configs) {
            if (!matchesSender(config, sender, asterisk)) {
                continue;
            }
            if (!config.getIsSmsEnabled()) {
                continue;
            }
            if ("notification".equals(source)) {
                String notificationFilter = config.getNotificationFilter();
                if (!matchesNotificationFilter(notificationFilter, content)) {
                    continue;
                }
            } else if (!matchesFilter(config.getSmsFilter(), content)) {
                continue;
            }
            // A notification has no reliable SIM identity. Rules pinned to a
            // specific slot remain SMS-only instead of being sent from an
            // arbitrary account.
            if (config.getSimSlot() > 0 && config.getSimSlot() != simSlot) {
                continue;
            }

            String dedupeKey = buildDedupeKey(config, sender, content);
            if (isRecentDuplicate(dedupeKey, source)) {
                Log.i("SmsBroadcastReceiver", "Skipping duplicate " + source
                        + " notification sender=" + sender);
                forwarded = true;
                continue;
            }

            if (receiver != null) {
                // Keep the instance callback for the existing instrumentation
                // tests and for OEMs that subclass the receiver.
                receiver.callWebHook(config, sender, slotName, content, timeStamp);
            } else {
                enqueueWebhook(context, config, sender, slotName, content, timeStamp);
            }
            Log.i("SmsBroadcastReceiver", "Enqueued " + source + " webhook sender="
                    + sender + ", slot=" + slotName);
            forwarded = true;
        }

        if (!forwarded) {
            Log.w("SmsBroadcastReceiver", "No forwarding rule matched " + source
                    + " sender=" + sender + ", bodyLength=" + content.length());
        }
        return forwarded;
    }

    private SmsMessage parseMessage(byte[] pdu, String format) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && format != null && !format.isEmpty()) {
                return SmsMessage.createFromPdu(pdu, format);
            }
            return SmsMessage.createFromPdu(pdu);
        } catch (RuntimeException exception) {
            Log.e("SmsBroadcastReceiver", "Unable to parse SMS PDU (format=" + format + ")", exception);
            return null;
        }
    }

    protected void callWebHook(ForwardingConfig config, String sender, String slotName,
                               String content, long timeStamp) {
        enqueueWebhook(this.context, config, sender, slotName, content, timeStamp);
    }

    private static void enqueueWebhook(Context context, ForwardingConfig config, String sender,
                                       String slotName, String content, long timeStamp) {

        String message = config.prepareMessage(sender, content, slotName, timeStamp);

        Data data = new Data.Builder()
                .putString(RequestWorker.DATA_URL, config.getUrl())
                .putString(RequestWorker.DATA_TEXT, message)
                .putString(RequestWorker.DATA_HEADERS, config.getHeaders())
                .putBoolean(RequestWorker.DATA_IGNORE_SSL, config.getIgnoreSsl())
                .putBoolean(RequestWorker.DATA_CHUNKED_MODE, config.getChunkedMode())
                .putInt(RequestWorker.DATA_MAX_RETRIES, config.getRetriesNumber())
                .putBoolean(RequestWorker.DATA_SIGN_HMAC_SHA256, config.getSignHmacSha256())
                .putString(RequestWorker.DATA_SIGN_HMAC_SHA256_SECRET, config.getSignHmacSha256Secret())
                .putBoolean(RequestWorker.DATA_STORE_FAILED, config.getStoreFailed())
                .putBoolean(RequestWorker.DATA_LOCAL_MODE, config.getLocalMode())
                .build();

        RequestWorker.enqueue(context, data);
    }

    private static String buildDedupeKey(ForwardingConfig config, String sender, String content) {
        String configKey = config.getKey();
        if (configKey == null || configKey.isEmpty()) {
            configKey = config.getSender() + "|" + config.getUrl();
        }
        String normalized = content.trim().replaceAll("\\s+", " ");
        return configKey + "\u0000" + sender + "\u0000" + normalized;
    }

    private static boolean isRecentDuplicate(String key, String source) {
        long now = System.currentTimeMillis();
        RecentForwarding previous = RECENT_FORWARDINGS.get(key);
        if (previous != null) {
            if (("notification".equals(source) || !previous.source.equals(source))
                    && now - previous.timestamp < FORWARD_DEDUPE_WINDOW_MS) {
                return true;
            }
        }
        RECENT_FORWARDINGS.put(key, new RecentForwarding(source, now));

        if (RECENT_FORWARDINGS.size() > MAX_DEDUPE_ENTRIES) {
            for (Map.Entry<String, RecentForwarding> entry : RECENT_FORWARDINGS.entrySet()) {
                if (now - entry.getValue().timestamp >= FORWARD_DEDUPE_WINDOW_MS) {
                    RECENT_FORWARDINGS.remove(entry.getKey(), entry.getValue());
                }
            }
        }
        return false;
    }

    // Per-config sender match. The asterisk wildcard always means "any sender"
    // regardless of the regex flag. When the rule opts into regex matching (issue
    // #88 — e.g. an Indian sender ID like AB-CTAXKR whose operator prefix rotates),
    // the configured sender is a Java regex tested against the incoming address with
    // find() (substring), mirroring the content filter. Unlike the content filter
    // this fails *closed*: an invalid pattern matches nothing, so a typo cannot leak
    // unrelated senders to the endpoint. The default (flag off) is the historic
    // exact String.equals match, so every existing stored rule is unchanged.
    static boolean matchesSender(ForwardingConfig config, String sender, String asterisk) {
        String configured = config.getSender();
        if (configured.equals(asterisk)) {
            return true;
        }
        if (config.getIsSenderRegex()) {
            try {
                return Pattern.compile(configured).matcher(sender).find();
            } catch (PatternSyntaxException e) {
                Log.e("SmsBroadcastReceiver",
                        "Invalid sender regex \"" + configured + "\": " + e.getMessage());
                return false;
            }
        }
        return sender.equals(configured);
    }

    // Per-config content filter (issue #52). An empty filter forwards every
    // message (the historic behaviour). A non-empty filter is a Java regex tested
    // against the SMS body with find() (substring match): the message is forwarded
    // only when the regex matches. The single regex covers both directions —
    // "OTP" forwards messages that contain OTP, while a negative-lookahead such as
    // "(?s)^(?!.*OTP)" forwards every message that does NOT contain it. An invalid
    // pattern fails open (forwards and logs) so a typo never silently drops SMS,
    // mirroring the "never crash forwarding" rule used by the %Regex% placeholder.
    static boolean matchesFilter(String filter, String content) {
        if (filter == null || filter.isEmpty()) {
            return true;
        }
        try {
            return Pattern.compile(filter).matcher(content).find();
        } catch (PatternSyntaxException e) {
            Log.e("SmsBroadcastReceiver", "Invalid filter regex \"" + filter + "\": " + e.getMessage());
            return true;
        }
    }

    // Notification filters are opt-in and fail closed: an invalid pattern
    // must never cause arbitrary notification text to be forwarded.
    static boolean matchesNotificationFilter(String filter, String content) {
        if (filter == null || filter.isEmpty()) {
            return true;
        }
        try {
            return Pattern.compile(filter).matcher(content).find();
        } catch (PatternSyntaxException e) {
            Log.e("SmsBroadcastReceiver", "Invalid notification filter regex \""
                    + filter + "\": " + e.getMessage());
            return false;
        }
    }

    private int detectSim(Bundle bundle) {
        int slotId = -1;
        Set<String> keySet = bundle.keySet();
        for (String key : keySet) {
            switch (key) {
                case "phone":
                    slotId = bundle.getInt("phone", -1);
                    break;
                case "slot":
                    slotId = bundle.getInt("slot", -1);
                    break;
                case "simId":
                    slotId = bundle.getInt("simId", -1);
                    break;
                case "simSlot":
                    slotId = bundle.getInt("simSlot", -1);
                    break;
                case "slot_id":
                    slotId = bundle.getInt("slot_id", -1);
                    break;
                case "simnum":
                    slotId = bundle.getInt("simnum", -1);
                    break;
                case "slotId":
                    slotId = bundle.getInt("slotId", -1);
                    break;
                case "slotIdx":
                    slotId = bundle.getInt("slotIdx", -1);
                    break;
                case "android.telephony.extra.SLOT_INDEX":
                    slotId = bundle.getInt("android.telephony.extra.SLOT_INDEX", -1);
                    break;
                default:
                    if (key.toLowerCase().contains("slot") | key.toLowerCase().contains("sim")) {
                        String value = bundle.getString(key, "-1");
                        if (value.equals("0") | value.equals("1") | value.equals("2")) {
                            slotId = bundle.getInt(key, -1);
                        }
                    }
            }

            if (slotId != -1) {
                break;
            }
        }

        return slotId;
    }
}
