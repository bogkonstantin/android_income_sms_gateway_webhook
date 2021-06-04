package tech.bogomolov.incomingsmsgateway;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Map;

public class Config {
    final private Context context;

    private String sender;
    private String url;

    public Config(Context context) {
        this.context = context;
    }

    public String getSender() {
        return this.sender;
    }

    public Config setSender(String sender) {
        this.sender = sender;
        return this;
    }

    public String getUrl() {
        return this.url;
    }

    public Config setUrl(String url) {
        this.url = url;
        return this;
    }

    public void save() {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putString(this.sender, this.url);
        editor.commit();
    }

    public static ArrayList<Config> getAll(Context context) {
        SharedPreferences sharedPref = getPreference(context);
        Map<String, ?> sharedPrefs = sharedPref.getAll();

        ArrayList<Config> configs = new ArrayList<Config>();

        for (Map.Entry<String, ?> entry : sharedPrefs.entrySet()) {
            Config config = new Config(context);
            config.setSender(entry.getKey());
            config.setUrl((String) entry.getValue());
            configs.add(config);
        }

        return configs;
    }

    public void remove() {
        SharedPreferences.Editor editor = getEditor(context);
        editor.remove(this.getSender());
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
