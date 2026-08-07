package tech.bogomolov.incomingsmsgateway;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ForwardingConfig {
    final private Context context;

    private static final String KEY_KEY = "key";
    private static final String KEY_SENDER = "sender";
    private static final String KEY_IS_SENDER_REGEX = "is_sender_regex";
    private static final String KEY_SMS_FILTER = "sms_filter";
    private static final String KEY_NOTIFICATION_FILTER = "notification_filter";
    private static final String KEY_URL = "url";
    private static final String KEY_SIM_SLOT = "sim_slot";
    private static final String KEY_TEMPLATE = "template";
    private static final String KEY_HEADERS = "headers";
    private static final String KEY_RETRIES_NUMBER = "retries_number";
    private static final String KEY_IGNORE_SSL = "ignore_ssl";
    private static final String KEY_CHUNKED_MODE = "chunked_mode";
    private static final String KEY_IS_SMS_ENABLED = "is_sms_enabled";
    private static final String KEY_SIGN_HMAC_SHA256 = "sign_hmac_sha256";
    private static final String KEY_SIGN_HMAC_SHA256_SECRET = "sign_hmac_sha256_secret";
    private static final String KEY_STORE_FAILED = "store_failed";
    private static final String KEY_LOCAL_MODE = "local_mode";

    private String key;
    private String sender;
    private boolean isSenderRegex = false; // when true, sender is matched as a regex
    private String smsFilter = ""; // empty means forward every message
    private String notificationFilter = ""; // empty means forward every readable notification
    private String url;
    private int simSlot = 0; // 0 means any
    private String template;
    private String headers;
    private int retriesNumber;
    private boolean ignoreSsl = false;
    private boolean chunkedMode = true;
    private boolean isSmsEnabled = true;
    private boolean signHmacSha256 = false;
    private String signHmacSha256Secret;
    private boolean storeFailed = false;
    private boolean localMode = false; // forward without a validated internet connection

    public ForwardingConfig(Context context) {
        this.context = context;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }

    public String getSender() {
        return this.sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public boolean getIsSenderRegex() {
        return this.isSenderRegex;
    }

    public void setIsSenderRegex(boolean isSenderRegex) {
        this.isSenderRegex = isSenderRegex;
    }

    public String getSmsFilter() {
        return this.smsFilter;
    }

    public void setSmsFilter(String smsFilter) {
        this.smsFilter = smsFilter == null ? "" : smsFilter;
    }

    public String getNotificationFilter() {
        return this.notificationFilter;
    }

    public void setNotificationFilter(String notificationFilter) {
        this.notificationFilter = notificationFilter == null ? "" : notificationFilter;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getSimSlot() {
        return this.simSlot;
    }

    public void setSimSlot(int simSlot) {
        this.simSlot = simSlot;
    }

    public String getTemplate() {
        return this.template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getHeaders() {
        return this.headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public int getRetriesNumber() {
        return this.retriesNumber;
    }

    public void setRetriesNumber(int retriesNumber) {
        this.retriesNumber = retriesNumber;
    }

    public boolean getIgnoreSsl() {
        return this.ignoreSsl;
    }

    public void setIgnoreSsl(boolean ignoreSsl) {
        this.ignoreSsl = ignoreSsl;
    }

    public boolean getChunkedMode() {
        return this.chunkedMode;
    }

    public void setChunkedMode(boolean chunkedMode) {
        this.chunkedMode = chunkedMode;
    }

    public boolean getSignHmacSha256() {
        return this.signHmacSha256;
    }

    public void setSignHmacSha256(boolean signHmacSha256) {
        this.signHmacSha256 = signHmacSha256;
    }

    public String getSignHmacSha256Secret() {
        return this.signHmacSha256Secret;
    }

    public void setSignHmacSha256Secret(String signHmacSha256Secret) {
        this.signHmacSha256Secret = signHmacSha256Secret;
    }

    public boolean getStoreFailed() {
        return this.storeFailed;
    }

    public void setStoreFailed(boolean storeFailed) {
        this.storeFailed = storeFailed;
    }

    public boolean getLocalMode() {
        return this.localMode;
    }

    public void setLocalMode(boolean localMode) {
        this.localMode = localMode;
    }

    public boolean getIsSmsEnabled() {
        return this.isSmsEnabled;
    }

    public void setIsSmsEnabled(boolean isSmsEnabled) {
        this.isSmsEnabled = isSmsEnabled;
    }

    public static String getDefaultJsonTemplate() {
        return "{\n  \"from\":\"%from%\",\n  \"text\":\"%text%\",\n  \"sentStamp\":%sentStamp%,\n  \"receivedStamp\":%receivedStamp%,\n  \"sim\":\"%sim%\"\n}";
    }

    public static String getDefaultJsonHeaders() {
        return "{\"User-agent\":\"SMS Forwarder App\"}";
    }

    public static int getDefaultRetriesNumber() {
        return 10;
    }

    // Serializes this config to the same JSON object used both for SharedPreferences
    // storage and for backup export, so the two formats can never drift apart.
    // Generates the key if absent (save() relies on getKey() being populated).
    public JSONObject toJson() throws JSONException {
        if (this.getKey() == null) {
            this.setKey(this.generateKey());
        }

        JSONObject json = new JSONObject();
        json.put(KEY_KEY, this.getKey());
        json.put(KEY_SENDER, this.sender);
        json.put(KEY_IS_SENDER_REGEX, this.isSenderRegex);
        json.put(KEY_SMS_FILTER, this.smsFilter);
        json.put(KEY_NOTIFICATION_FILTER, this.notificationFilter);
        json.put(KEY_URL, this.url);
        json.put(KEY_SIM_SLOT, this.simSlot);
        json.put(KEY_TEMPLATE, this.template);
        json.put(KEY_HEADERS, this.headers);
        json.put(KEY_RETRIES_NUMBER, this.retriesNumber);
        json.put(KEY_IGNORE_SSL, this.ignoreSsl);
        json.put(KEY_CHUNKED_MODE, this.chunkedMode);
        json.put(KEY_IS_SMS_ENABLED, this.isSmsEnabled);
        json.put(KEY_SIGN_HMAC_SHA256, this.signHmacSha256);
        json.put(KEY_SIGN_HMAC_SHA256_SECRET, this.signHmacSha256Secret);
        json.put(KEY_STORE_FAILED, this.storeFailed);
        json.put(KEY_LOCAL_MODE, this.localMode);
        return json;
    }

    public void save() {
        try {
            JSONObject json = this.toJson();

            SharedPreferences.Editor editor = getEditor(context);
            editor.putString(this.getKey(), json.toString());

            editor.commit();
        } catch (Exception e) {
            Log.e("ForwardingConfig", e.getMessage());
        }
    }

    public static ArrayList<ForwardingConfig> getAll(Context context) {
        SharedPreferences sharedPref = getPreference(context);
        Map<String, ?> sharedPrefs = sharedPref.getAll();

        ArrayList<ForwardingConfig> configs = new ArrayList<>();

        for (Map.Entry<String, ?> entry : sharedPrefs.entrySet()) {
            configs.add(fromStoredValue(context, entry.getKey(), (String) entry.getValue()));
        }

        return configs;
    }

    // Deserializes one stored entry into a config. Shared by getAll() (reading the
    // SharedPreferences map) and importFromJson() (reading a backup file) so both
    // honor the same legacy fallback and per-field defaults. fallbackKey supplies
    // the key/sender for entries that predate those JSON fields (getAll passes the
    // map key; import has no map key and passes null, which leaves the generated
    // key to save()).
    private static ForwardingConfig fromStoredValue(Context context, String fallbackKey, String value) {
        ForwardingConfig config = new ForwardingConfig(context);
        config.setSender(fallbackKey);

        if (value.charAt(0) == '{') {
            try {
                JSONObject json = new JSONObject(value);

                if (json.has(KEY_KEY)) {
                    config.setKey(json.getString(KEY_KEY));
                } else {
                    config.setKey(fallbackKey);
                }

                if (json.has(KEY_SENDER)) {
                    config.setSender(json.getString(KEY_SENDER));
                } else {
                    config.setSender(fallbackKey);
                }

                if (json.has(KEY_SMS_FILTER)) {
                    config.setSmsFilter(json.getString(KEY_SMS_FILTER));
                }
                if (json.has(KEY_NOTIFICATION_FILTER)) {
                    config.setNotificationFilter(json.getString(KEY_NOTIFICATION_FILTER));
                }

                if (json.has(KEY_IS_SMS_ENABLED)) {
                    config.setIsSmsEnabled(json.getBoolean(KEY_IS_SMS_ENABLED));
                } else {
                    config.setIsSmsEnabled(true);
                }

                if (json.has(KEY_SIM_SLOT)) {
                    config.setSimSlot(json.getInt(KEY_SIM_SLOT));
                }

                config.setUrl(json.getString(KEY_URL));
                config.setTemplate(json.getString(KEY_TEMPLATE));
                config.setHeaders(json.getString(KEY_HEADERS));

                if (json.has(KEY_RETRIES_NUMBER)) {
                    config.setRetriesNumber(json.getInt(KEY_RETRIES_NUMBER));
                } else {
                    config.setRetriesNumber(ForwardingConfig.getDefaultRetriesNumber());
                }

                // Each optional field is guarded independently: a key absent in
                // an older stored config (or a null secret, which org.json drops
                // on save) must leave that one field at its default without
                // skipping the others.
                try {
                    if (json.has(KEY_IGNORE_SSL)) {
                        config.setIgnoreSsl(json.getBoolean(KEY_IGNORE_SSL));
                    }
                    if (json.has(KEY_CHUNKED_MODE)) {
                        config.setChunkedMode(json.getBoolean(KEY_CHUNKED_MODE));
                    }
                    if (json.has(KEY_SIGN_HMAC_SHA256)) {
                        config.setSignHmacSha256(json.getBoolean(KEY_SIGN_HMAC_SHA256));
                    }
                    if (json.has(KEY_SIGN_HMAC_SHA256_SECRET)) {
                        config.setSignHmacSha256Secret(json.getString(KEY_SIGN_HMAC_SHA256_SECRET));
                    }
                    if (json.has(KEY_STORE_FAILED)) {
                        config.setStoreFailed(json.getBoolean(KEY_STORE_FAILED));
                    }
                    if (json.has(KEY_LOCAL_MODE)) {
                        config.setLocalMode(json.getBoolean(KEY_LOCAL_MODE));
                    }
                    if (json.has(KEY_IS_SENDER_REGEX)) {
                        config.setIsSenderRegex(json.getBoolean(KEY_IS_SENDER_REGEX));
                    }
                } catch (JSONException ignored) {
                }
            } catch (JSONException e) {
                Log.e("ForwardingConfig", e.getMessage());
            }
        } else {
            config.setUrl(value);
            config.setTemplate(ForwardingConfig.getDefaultJsonTemplate());
            config.setHeaders(ForwardingConfig.getDefaultJsonHeaders());
        }

        return config;
    }

    // Serializes every stored rule to a JSON array string for backup (issue #76).
    // Scope is forwarding rules only — heartbeat settings and the stored
    // failed-message payloads live in their own SharedPreferences files and are
    // not touched. The output carries webhook URLs, custom headers and HMAC
    // secrets verbatim, so callers warn the user it is sensitive.
    public static String exportToJson(Context context) throws JSONException {
        JSONArray array = new JSONArray();
        for (ForwardingConfig config : getAll(context)) {
            array.put(config.toJson());
        }
        return array.toString(2);
    }

    // Restores rules from a backup produced by exportToJson(). Each rule keeps its
    // original key, so re-importing the same file overwrites the matching rule
    // (merge) rather than duplicating it, while rules with new keys are added.
    // Returns the number of rules imported. Throws JSONException on a malformed
    // file so the caller can report it.
    public static int importFromJson(Context context, String content) throws JSONException {
        JSONArray array = new JSONArray(content);
        int imported = 0;
        for (int i = 0; i < array.length(); i++) {
            JSONObject json = array.getJSONObject(i);
            ForwardingConfig config = fromStoredValue(context, null, json.toString());
            // A rule missing a required field parses to nulls (fromStoredValue
            // swallows the JSONException); saving it would NPE message
            // preparation on every later SMS, so skip it instead.
            if (config.getSender() == null || config.getUrl() == null
                    || config.getTemplate() == null || config.getHeaders() == null) {
                Log.e("ForwardingConfig", "skipping invalid backup rule: " + json);
                continue;
            }
            config.save();
            imported++;
        }
        return imported;
    }

    public void remove() {
        SharedPreferences.Editor editor = getEditor(context);
        editor.remove(this.getKey());
        editor.commit();
    }

    // Matches every supported placeholder in one pass. The "Regex=..." form
    // carries a user regex that runs up to the first *unescaped* % (the closing
    // delimiter); to include a literal % in the pattern, the user escapes it as
    // \% — which is also how Java regex already spells a literal %, so the
    // captured pattern can be compiled as-is. The body token is therefore either
    // an escape pair (\ + any char) or any char that is neither % nor backslash,
    // and the two branches don't overlap, so there is no catastrophic backtracking.
    // The "sentStamp=..." / "receivedStamp=..." forms (issue #42) carry an
    // optional SimpleDateFormat pattern running up to the first %; without it the
    // stamp stays bare epoch millis. Substituting all placeholders in a single
    // pass means an inserted value (e.g. an SMS body that itself looks like
    // "%from%") is never re-scanned.
    private static final Pattern PLACEHOLDER = Pattern.compile(
            "%(from|sentStamp(?:=[^%]+)?|receivedStamp(?:=[^%]+)?|sim|text"
                    + "|version|battery|power|network"
                    + "|Regex=(?:\\\\[\\s\\S]|[^%\\\\])+)%");

    public String prepareMessage(String from, String content, String sim, long timeStamp) {
        // Compute the receive time once so every %receivedStamp% in the template
        // (bare or formatted) reflects the same instant.
        long receivedStamp = System.currentTimeMillis();
        Matcher matcher = PLACEHOLDER.matcher(this.getTemplate());
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            // The parameterized forms ("sentStamp=<fmt>", "receivedStamp=<fmt>",
            // "Regex=<pattern>") carry their argument after the first '='. The '='
            // is unambiguous because no placeholder name contains one, and any
            // later '=' (e.g. inside a regex) belongs to the argument.
            String arg = null;
            int eq = placeholder.indexOf('=');
            if (eq >= 0) {
                arg = placeholder.substring(eq + 1);
                placeholder = placeholder.substring(0, eq);
            }
            String value;
            switch (placeholder) {
                case "from":
                    value = from;
                    break;
                case "sentStamp":
                    value = arg == null ? String.valueOf(timeStamp)
                            : StringEscapeUtils.escapeJson(formatStamp(arg, timeStamp));
                    break;
                case "receivedStamp":
                    value = arg == null ? String.valueOf(receivedStamp)
                            : StringEscapeUtils.escapeJson(formatStamp(arg, receivedStamp));
                    break;
                case "sim":
                    value = sim;
                    break;
                case "text":
                    value = StringEscapeUtils.escapeJson(content);
                    break;
                // Device-health data points (issue #39). These read live device
                // state via this.context; DeviceInfo is null-context safe so a
                // null context (unit tests) yields fallbacks rather than crashing.
                case "version":
                    value = DeviceInfo.getVersion();
                    break;
                case "battery":
                    value = String.valueOf(DeviceInfo.getBatteryPercentage(this.context));
                    break;
                case "power":
                    value = DeviceInfo.getPowerSource(this.context);
                    break;
                case "network":
                    value = DeviceInfo.getNetworkType(this.context);
                    break;
                default:
                    // placeholder == "Regex", arg == "<pattern>"
                    value = StringEscapeUtils.escapeJson(extractByRegex(arg, content));
                    break;
            }
            // quoteReplacement guards against $ / \ in the value being treated as
            // a regex group reference (would otherwise throw or corrupt output).
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    // Formats an epoch-millis timestamp with a user-supplied SimpleDateFormat
    // pattern in the device's local timezone (issue #42). Returns "" on an
    // invalid pattern so a bad format string never crashes message forwarding,
    // mirroring extractByRegex.
    private String formatStamp(String pattern, long epochMillis) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.getDefault());
            return format.format(new Date(epochMillis));
        } catch (IllegalArgumentException e) {
            Log.e("ForwardingConfig", "Invalid date format \"" + pattern + "\": " + e.getMessage());
            return "";
        }
    }

    // Extracts a substring of the SMS body using a user-supplied regex. If the
    // regex has a capturing group, group 1 is returned; otherwise the whole
    // match. Returns "" on no match or an invalid regex, so a bad pattern never
    // crashes message forwarding.
    private String extractByRegex(String regex, String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        try {
            Matcher matcher = Pattern.compile(regex).matcher(content);
            if (!matcher.find()) {
                return "";
            }
            if (matcher.groupCount() >= 1) {
                String group = matcher.group(1);
                return group == null ? "" : group;
            }
            return matcher.group();
        } catch (PatternSyntaxException e) {
            Log.e("ForwardingConfig", "Invalid regex \"" + regex + "\": " + e.getMessage());
            return "";
        }
    }

    private static SharedPreferences getPreference(Context context) {
        return context.getSharedPreferences(
                context.getString(R.string.key_phones_preference),
                Context.MODE_PRIVATE
        );
    }

    private static SharedPreferences.Editor getEditor(Context context) {
        SharedPreferences sharedPref = getPreference(context);
        return sharedPref.edit();
    }

    private String generateKey() {
        String stamp = Long.toString(System.currentTimeMillis());
        int randomNum = new Random().nextInt((999990 - 100000) + 1) + 100000;
        return stamp + '_' + randomNum;
    }
}
