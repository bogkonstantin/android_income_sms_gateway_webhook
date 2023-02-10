package tech.bogomolov.incomingsmsgateway;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Telephony;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(AndroidJUnit4.class)
public class SmsReceiverTest {

    Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Before
    public void clearSharedPrefs() {
        SharedPreferences.Editor editor = this.getEditor();
        editor.clear();
        editor.commit();
    }

    @Test
    public void testEmptyConfig() {
        SmsReceiver receiver = this.getSmsReceiver();
        receiver.onReceive(appContext, this.getIntent());

        Mockito.verify(receiver, Mockito.times(0))
                .callWebHook(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void testSmsPassedToWebhookByWildcard() {
        this.setPhoneConfig(appContext, appContext.getString(R.string.asterisk));
        SmsReceiver receiver = this.getSmsReceiver();
        receiver.onReceive(appContext, this.getIntent());

        Mockito.verify(receiver, Mockito.times(1))
                .callWebHook(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void testSmsPassedToWebhookByNumber() {
        this.setPhoneConfig(appContext, this.getSender());
        SmsReceiver receiver = this.getSmsReceiver();
        receiver.onReceive(appContext, this.getIntent());

        Mockito.verify(receiver, Mockito.times(1))
                .callWebHook(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void testSmsNotPassedToWebhook() {
        this.setPhoneConfig(appContext, "wrongSender");
        SmsReceiver receiver = this.getSmsReceiver();
        receiver.onReceive(appContext, this.getIntent());

        Mockito.verify(receiver, Mockito.times(0))
                .callWebHook(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void testMultiplePdus() {
        this.setPhoneConfig(appContext, appContext.getString(R.string.asterisk));
        SmsReceiver receiver = this.getSmsReceiver();
        receiver.onReceive(appContext, this.getIntentMultiPdus());

        Mockito.verify(receiver, Mockito.times(1))
                .callWebHook(Mockito.anyString(),
                        Mockito.contains("\"text\":\"TestTest\""),
                        Mockito.anyString(), Mockito.anyBoolean());
    }

    private void setPhoneConfig(Context context, String phone) {
        SharedPreferences.Editor editor = this.getEditor();
        editor.putString(phone, "test");
        editor.commit();
    }

    private SharedPreferences.Editor getEditor() {
        SharedPreferences sharedPref = appContext.getSharedPreferences(
                appContext.getString(R.string.key_phones_preference),
                Context.MODE_PRIVATE
        );
        return sharedPref.edit();
    }

    private Intent getIntent() {
        Intent intent = new Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        intent.putExtra("pdus", this.getTestPdu());
        return intent;
    }

    private Intent getIntentMultiPdus() {
        Intent intent = new Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        intent.putExtra("pdus", this.getTestMultiplePdu());
        return intent;
    }

    private SmsReceiver getSmsReceiver() {
        SmsReceiver receiver = Mockito.mock(SmsReceiver.class);
        Mockito.doCallRealMethod()
                .when(receiver).onReceive(Mockito.any(Context.class), Mockito.any(Intent.class));
        Mockito.doNothing().when(receiver)
                .callWebHook(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());

        return receiver;
    }

    private String getSender() {
        return "+16505551111";
    }

    private byte[][] getTestPdu() {
        String pdu = "07914151551512f2040B916105551511f100006060605130308A04D4F29C0E";
        byte[][] pdus = new byte[1][];
        pdus[0] = hexToByteArray(pdu);

        return pdus;
    }

    private byte[][] getTestMultiplePdu() {
        String pdu = "07914151551512f2040B916105551511f100006060605130308A04D4F29C0E";

        byte[][] pdus = new byte[2][];
        pdus[0] = hexToByteArray(pdu);
        pdus[1] = hexToByteArray(pdu);

        return pdus;
    }

    private byte[] hexToByteArray(String hex) {
        hex = hex.length() % 2 != 0 ? "0" + hex : hex;

        byte[] b = new byte[hex.length() / 2];

        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(hex.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }
}
