package tech.bogomolov.incomingsmsgateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

/**
 * Instrumented tests for {@link ForwardingConfig} persistence: the save()/getAll()
 * round-trip, the legacy bare-URL fallback, and the missing-field defaults that keep
 * old stored configs loadable. These need a real SharedPreferences and org.json, so
 * they run on a device/emulator rather than the local JVM.
 */
@RunWith(AndroidJUnit4.class)
public class ForwardingConfigTest {

    private final Context context =
            InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Before
    public void clearSharedPrefs() {
        SharedPreferences.Editor editor = getEditor();
        editor.clear();
        editor.commit();
    }

    @Test
    public void testSaveAndGetAllRoundTrip() {
        ForwardingConfig config = new ForwardingConfig(context);
        config.setSender("+16505551111");
        config.setNotificationFilter("(?i)呱呱研选.*验证码");
        config.setUrl("https://example.com/hook");
        config.setSimSlot(2);
        config.setTemplate("{\"text\":\"%text%\"}");
        config.setHeaders("{\"X-Token\":\"abc\"}");
        config.setRetriesNumber(7);
        config.setIgnoreSsl(true);
        config.setChunkedMode(false);
        config.setIsSmsEnabled(false);
        config.setStoreFailed(true);
        config.setLocalMode(true);
        config.save();

        ArrayList<ForwardingConfig> all = ForwardingConfig.getAll(context);
        assertEquals(1, all.size());

        ForwardingConfig loaded = all.get(0);
        assertEquals("+16505551111", loaded.getSender());
        assertEquals("(?i)呱呱研选.*验证码", loaded.getNotificationFilter());
        assertEquals("https://example.com/hook", loaded.getUrl());
        assertEquals(2, loaded.getSimSlot());
        assertEquals("{\"text\":\"%text%\"}", loaded.getTemplate());
        assertEquals("{\"X-Token\":\"abc\"}", loaded.getHeaders());
        assertEquals(7, loaded.getRetriesNumber());
        assertTrue(loaded.getIgnoreSsl());
        assertFalse(loaded.getChunkedMode());
        assertFalse(loaded.getIsSmsEnabled());
        assertTrue(loaded.getStoreFailed());
        assertTrue(loaded.getLocalMode());
        assertNotNull(loaded.getKey());
    }

    @Test
    public void testRemove() {
        ForwardingConfig config = new ForwardingConfig(context);
        config.setSender("123");
        config.setUrl("https://example.com");
        config.setTemplate("{}");
        config.setHeaders("{}");
        config.setRetriesNumber(3);
        config.save();

        assertEquals(1, ForwardingConfig.getAll(context).size());

        config.remove();
        assertEquals(0, ForwardingConfig.getAll(context).size());
    }

    @Test
    public void testLegacyBareUrlFallback() {
        // Old app versions stored a bare URL string (not JSON) keyed by the sender.
        putRaw("oldSender", "http://legacy.example/hook");

        ArrayList<ForwardingConfig> all = ForwardingConfig.getAll(context);
        assertEquals(1, all.size());

        ForwardingConfig loaded = all.get(0);
        assertEquals("oldSender", loaded.getSender());
        assertEquals("http://legacy.example/hook", loaded.getUrl());
        assertEquals(ForwardingConfig.getDefaultJsonTemplate(), loaded.getTemplate());
        assertEquals(ForwardingConfig.getDefaultJsonHeaders(), loaded.getHeaders());
    }

    @Test
    public void testMissingRetriesNumberDefaults() throws Exception {
        putRaw("key1", baseJson().toString());

        ForwardingConfig loaded = ForwardingConfig.getAll(context).get(0);
        assertEquals(ForwardingConfig.getDefaultRetriesNumber(), loaded.getRetriesNumber());
    }

    @Test
    public void testMissingSimSlotDefaultsToZero() throws Exception {
        putRaw("key1", baseJson().toString());

        ForwardingConfig loaded = ForwardingConfig.getAll(context).get(0);
        assertEquals(0, loaded.getSimSlot());
    }

    @Test
    public void testMissingIsSmsEnabledDefaultsToTrue() throws Exception {
        putRaw("key1", baseJson().toString());

        ForwardingConfig loaded = ForwardingConfig.getAll(context).get(0);
        assertTrue(loaded.getIsSmsEnabled());
    }

    @Test
    public void testMissingSslAndChunkedKeepDefaults() throws Exception {
        // Old entries without ignore_ssl / chunked_mode must still load and keep
        // the field defaults (ignoreSsl=false, chunkedMode=true).
        putRaw("key1", baseJson().toString());

        ForwardingConfig loaded = ForwardingConfig.getAll(context).get(0);
        assertFalse(loaded.getIgnoreSsl());
        assertTrue(loaded.getChunkedMode());
    }

    @Test
    public void testMissingStoreFailedDefaultsToFalse() throws Exception {
        // Configs saved before the "store failed" feature must load with it off.
        putRaw("key1", baseJson().toString());

        ForwardingConfig loaded = ForwardingConfig.getAll(context).get(0);
        assertFalse(loaded.getStoreFailed());
    }

    @Test
    public void testMissingLocalModeDefaultsToFalse() throws Exception {
        // Configs saved before the "local network mode" feature must load with it
        // off, so existing rules keep the NetworkType.CONNECTED constraint.
        putRaw("key1", baseJson().toString());

        ForwardingConfig loaded = ForwardingConfig.getAll(context).get(0);
        assertFalse(loaded.getLocalMode());
    }

    @Test
    public void testStoreFailedSurvivesNullHmacSecret() throws Exception {
        // store_failed is read before the (absent-when-null) HMAC secret, so it
        // must not be reset to its default when no secret is stored.
        ForwardingConfig config = new ForwardingConfig(context);
        config.setSender("+16505551111");
        config.setUrl("https://example.com");
        config.setTemplate("{}");
        config.setHeaders("{}");
        config.setRetriesNumber(3);
        config.setStoreFailed(true);
        config.setSignHmacSha256(false);
        config.setSignHmacSha256Secret(null);
        config.save();

        ForwardingConfig loaded = ForwardingConfig.getAll(context).get(0);
        assertTrue(loaded.getStoreFailed());
    }

    @Test
    public void testMissingSenderFallsBackToEntryKey() throws Exception {
        // An entry whose JSON has no "sender" should fall back to the map key.
        JSONObject json = baseJson();
        json.remove("sender");
        putRaw("entryKeyAsSender", json.toString());

        ForwardingConfig loaded = ForwardingConfig.getAll(context).get(0);
        assertEquals("entryKeyAsSender", loaded.getSender());
    }

    @Test
    public void testExportImportRoundTrip() throws Exception {
        // A rule carrying sensitive fields (secret, headers) and non-default flags
        // must survive an export → wipe → import cycle byte-for-byte, including key.
        ForwardingConfig config = new ForwardingConfig(context);
        config.setSender("+16505551111");
        config.setNotificationFilter("(?i)验证码");
        config.setUrl("https://example.com/hook");
        config.setSimSlot(2);
        config.setTemplate("{\"text\":\"%text%\"}");
        config.setHeaders("{\"Authorization\":\"Bearer abc\"}");
        config.setRetriesNumber(7);
        config.setIgnoreSsl(true);
        config.setChunkedMode(false);
        config.setIsSmsEnabled(false);
        config.setSignHmacSha256(true);
        config.setSignHmacSha256Secret("topsecret");
        config.setStoreFailed(true);
        config.setLocalMode(true);
        config.save();
        String originalKey = ForwardingConfig.getAll(context).get(0).getKey();

        String exported = ForwardingConfig.exportToJson(context);

        // Wipe, simulating a fresh device, then import.
        clearSharedPrefs();
        assertEquals(0, ForwardingConfig.getAll(context).size());

        int imported = ForwardingConfig.importFromJson(context, exported);
        assertEquals(1, imported);

        ForwardingConfig loaded = ForwardingConfig.getAll(context).get(0);
        assertEquals(originalKey, loaded.getKey());
        assertEquals("+16505551111", loaded.getSender());
        assertEquals("(?i)验证码", loaded.getNotificationFilter());
        assertEquals("https://example.com/hook", loaded.getUrl());
        assertEquals(2, loaded.getSimSlot());
        assertEquals("{\"text\":\"%text%\"}", loaded.getTemplate());
        assertEquals("{\"Authorization\":\"Bearer abc\"}", loaded.getHeaders());
        assertEquals(7, loaded.getRetriesNumber());
        assertTrue(loaded.getIgnoreSsl());
        assertFalse(loaded.getChunkedMode());
        assertFalse(loaded.getIsSmsEnabled());
        assertTrue(loaded.getSignHmacSha256());
        assertEquals("topsecret", loaded.getSignHmacSha256Secret());
        assertTrue(loaded.getStoreFailed());
        assertTrue(loaded.getLocalMode());
    }

    @Test
    public void testImportMergesByKeyWithoutDuplicating() throws Exception {
        ForwardingConfig config = new ForwardingConfig(context);
        config.setSender("+16505551111");
        config.setUrl("https://example.com");
        config.setTemplate("{}");
        config.setHeaders("{}");
        config.setRetriesNumber(3);
        config.save();

        String exported = ForwardingConfig.exportToJson(context);

        // Importing the same backup over the existing rule overwrites the matching
        // key rather than producing a second copy.
        ForwardingConfig.importFromJson(context, exported);
        assertEquals(1, ForwardingConfig.getAll(context).size());
    }

    @Test
    public void testImportAddsNewRulesToExisting() throws Exception {
        // A backup with a rule whose key is absent from the device is added
        // alongside the existing rules.
        ForwardingConfig existing = new ForwardingConfig(context);
        existing.setSender("+1111");
        existing.setUrl("https://a.example");
        existing.setTemplate("{}");
        existing.setHeaders("{}");
        existing.setRetriesNumber(3);
        existing.save();

        String backup = "[{\"key\":\"999_111\",\"sender\":\"+2222\","
                + "\"url\":\"https://b.example\",\"template\":\"{}\",\"headers\":\"{}\"}]";
        ForwardingConfig.importFromJson(context, backup);

        assertEquals(2, ForwardingConfig.getAll(context).size());
    }

    @Test
    public void testImportSkipsRulesMissingRequiredFields() throws Exception {
        // A hand-edited or truncated backup rule without a URL must not be saved:
        // a half-built config would NPE message preparation on every later SMS.
        // Valid rules in the same file still import, and the count reflects only them.
        String backup = "[{\"sender\":\"+2222\",\"template\":\"{}\",\"headers\":\"{}\"},"
                + "{\"sender\":\"+3333\",\"url\":\"https://ok.example\","
                + "\"template\":\"{}\",\"headers\":\"{}\"}]";

        int imported = ForwardingConfig.importFromJson(context, backup);

        assertEquals(1, imported);
        assertEquals(1, ForwardingConfig.getAll(context).size());
        assertEquals("+3333", ForwardingConfig.getAll(context).get(0).getSender());
    }

    /** A minimal stored config that only carries the always-required fields. */
    private JSONObject baseJson() throws Exception {
        JSONObject json = new JSONObject();
        json.put("sender", "+16505551111");
        json.put("url", "https://example.com");
        json.put("template", "{}");
        json.put("headers", "{}");
        return json;
    }

    private void putRaw(String key, String value) {
        SharedPreferences.Editor editor = getEditor();
        editor.putString(key, value);
        editor.commit();
    }

    private SharedPreferences.Editor getEditor() {
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.key_phones_preference),
                Context.MODE_PRIVATE
        );
        return sharedPref.edit();
    }
}
