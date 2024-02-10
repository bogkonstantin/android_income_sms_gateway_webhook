package tech.bogomolov.incomingsmsgateway;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public class ForwardingConfig {
    final private Context context;

    private static final String KEY_SENDER = "sender";
    private static final String KEY_URL = "url";
    private static final String KEY_TEMPLATE = "template";
    private static final String KEY_HEADERS = "headers";
    private static final String KEY_IGNORE_SSL = "ignore_ssl";

    private String id;
    private String sender;
    private String url;
    private String template;
    private String headers;
    private boolean ignoreSsl = false;

    public ForwardingConfig(Context context) {
        this.context = context;
        this.id = UUID.randomUUID().toString();
    }

    public ForwardingConfig(Context context, String id) {
        this.context = context;
        this.id = id;
    }

    public String getId() { return this.id; }

    public void setId(String id) { this.id = id; }

    public String getSender() {
        return this.sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public boolean getIgnoreSsl() {
        return this.ignoreSsl;
    }

    public void setIgnoreSsl(boolean ignoreSsl) {
        this.ignoreSsl = ignoreSsl;
    }

    public void save() {
        try {
            JSONObject json = new JSONObject();
            json.put(KEY_SENDER, this.sender);
            json.put(KEY_URL, this.url);
            json.put(KEY_TEMPLATE, this.template);
            json.put(KEY_HEADERS, this.headers);
            json.put(KEY_IGNORE_SSL, this.ignoreSsl);

            SharedPreferences.Editor editor = getEditor(context);
            editor.putString(this.id, json.toString());

            editor.commit();
        } catch (Exception e) {
            Log.e("ForwardingConfig", e.getMessage());
        }
    }

    public static String getDefaultJsonTemplate() {
        return "{\n  \"from\":\"%from%\",\n  \"text\":\"%text%\",\n  \"sentStamp\":%sentStamp%,\n  \"receivedStamp\":%receivedStamp%,\n  \"sim\":\"%sim%\"\n}";
    }

    public static String getDefaultJsonHeaders() {
        return "{\"User-agent\":\"SMS Forwarder App\"}";
    }

    public static ArrayList<ForwardingConfig> getAll(Context context) {
        SharedPreferences sharedPref = getPreference(context);
        Map<String, ?> sharedPrefs = sharedPref.getAll();

        ArrayList<ForwardingConfig> configs = new ArrayList<>();

        for (Map.Entry<String, ?> entry : sharedPrefs.entrySet()) {
            String value = (String) entry.getValue();

            if (value.charAt(0) != '{') continue;

            ForwardingConfig config = new ForwardingConfig(context, entry.getKey());

            try {
                JSONObject json = new JSONObject(value);
                config.setSender(json.getString(KEY_SENDER));
                config.setUrl(json.getString(KEY_URL));
                config.setTemplate(json.getString(KEY_TEMPLATE));
                config.setHeaders(json.getString(KEY_HEADERS));

                try {
                    config.setIgnoreSsl(json.getBoolean(KEY_IGNORE_SSL));
                } catch (JSONException ignored) {
                }
            } catch (JSONException e) {
                Log.e("ForwardingConfig", e.getMessage());
            }

            configs.add(config);
        }

        return configs;
    }

    public void remove() {
        SharedPreferences.Editor editor = getEditor(context);
        editor.remove(this.getId());
        editor.commit();
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
}
