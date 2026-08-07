package tech.bogomolov.incomingsmsgateway;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pure-JVM unit tests for the static sender/content matchers in
 * {@link SmsBroadcastReceiver}. Both are plain string/regex logic that touch no
 * Android APIs, so they run on the local JVM. {@code matchesSender} reads only
 * the sender and regex flag off the config, so a null-context config is enough.
 */
public class SmsBroadcastReceiverMatchTest {

    private static final String ASTERISK = "*";

    private ForwardingConfig sender(String sender, boolean isRegex) {
        ForwardingConfig config = new ForwardingConfig(null);
        config.setSender(sender);
        config.setIsSenderRegex(isRegex);
        return config;
    }

    @Test
    public void exactMatchIsDefault() {
        assertTrue(SmsBroadcastReceiver.matchesSender(sender("VM-HDFCBK", false), "VM-HDFCBK", ASTERISK));
        assertFalse(SmsBroadcastReceiver.matchesSender(sender("VM-HDFCBK", false), "AB-HDFCBK", ASTERISK));
    }

    @Test
    public void exactMatchDoesNotTreatMetacharactersAsRegex() {
        // A stored literal sender that is an invalid/odd regex must keep matching
        // itself exactly when the regex flag is off — this is the backward-compat
        // guarantee for existing rules.
        ForwardingConfig config = sender("+12025550123", false);
        assertTrue(SmsBroadcastReceiver.matchesSender(config, "+12025550123", ASTERISK));
        assertFalse(SmsBroadcastReceiver.matchesSender(config, "12025550123", ASTERISK));
    }

    @Test
    public void asteriskMatchesAnySender() {
        assertTrue(SmsBroadcastReceiver.matchesSender(sender(ASTERISK, false), "anything", ASTERISK));
    }

    @Test
    public void asteriskMatchesEvenWhenRegexFlagIsOn() {
        // "*" is an invalid standalone regex; the wildcard must short-circuit
        // before any compilation so it never fails closed.
        assertTrue(SmsBroadcastReceiver.matchesSender(sender(ASTERISK, true), "anything", ASTERISK));
    }

    @Test
    public void regexMatchesRotatingOperatorPrefix() {
        // Issue #88: AB-CTAXKR, AV-CTAXKR, JD-CTAXKR are the same entity.
        ForwardingConfig config = sender("-CTAXKR", true);
        assertTrue(SmsBroadcastReceiver.matchesSender(config, "AB-CTAXKR", ASTERISK));
        assertTrue(SmsBroadcastReceiver.matchesSender(config, "AV-CTAXKR", ASTERISK));
        assertFalse(SmsBroadcastReceiver.matchesSender(config, "AB-OTHER", ASTERISK));
    }

    @Test
    public void regexUsesSubstringFind() {
        // find() semantics, mirroring the content filter: an unanchored pattern
        // matches anywhere. This is exactly why regex matching is opt-in.
        assertTrue(SmsBroadcastReceiver.matchesSender(sender("12345", true), "9912345", ASTERISK));
    }

    @Test
    public void invalidRegexFailsClosed() {
        // Unlike the content filter, a broken sender regex must match nothing so a
        // typo cannot leak unrelated senders to the endpoint.
        assertFalse(SmsBroadcastReceiver.matchesSender(sender("+1202", true), "+12025550123", ASTERISK));
    }

    @Test
    public void emptyFilterForwardsEverything() {
        assertTrue(SmsBroadcastReceiver.matchesFilter("", "any body"));
        assertTrue(SmsBroadcastReceiver.matchesFilter(null, "any body"));
    }

    @Test
    public void filterMatchesSubstring() {
        assertTrue(SmsBroadcastReceiver.matchesFilter("OTP", "Your OTP is 1234"));
        assertFalse(SmsBroadcastReceiver.matchesFilter("OTP", "Your code is 1234"));
    }

    @Test
    public void invalidFilterFailsOpen() {
        // The content filter deliberately fails open so a typo never silently
        // drops SMS — the opposite of the sender matcher.
        assertTrue(SmsBroadcastReceiver.matchesFilter("[unclosed", "any body"));
    }

    @Test
    public void notificationFilterMatchesOnlyNotificationContent() {
        assertTrue(SmsBroadcastReceiver.matchesNotificationFilter(
                "(?i)呱呱研选.*验证码", "【呱呱研选】您的验证码为：420790"));
        assertFalse(SmsBroadcastReceiver.matchesNotificationFilter(
                "(?i)呱呱研选.*验证码", "【呱呱研选】活动提醒"));
    }

    @Test
    public void emptyNotificationFilterAllowsAllReadableNotificationText() {
        assertTrue(SmsBroadcastReceiver.matchesNotificationFilter("", "你好，你中奖了"));
        assertTrue(SmsBroadcastReceiver.matchesNotificationFilter(null, "普通微信消息"));
    }

    @Test
    public void invalidNotificationFilterFailsClosed() {
        assertFalse(SmsBroadcastReceiver.matchesNotificationFilter("[unclosed", "any body"));
    }
}
