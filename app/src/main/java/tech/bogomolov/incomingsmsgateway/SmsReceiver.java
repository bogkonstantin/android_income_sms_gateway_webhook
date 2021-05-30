package tech.bogomolov.incomingsmsgateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import org.json.JSONObject;

import java.util.Map;

public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }


        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null || pdus.length == 0) {
            return;
        }

        Map<String, ?> configs = this.getConfigs(context);

        for (Object pdu : pdus) {
            SmsMessage message = SmsMessage.createFromPdu((byte[]) pdu);
            String sender = message.getOriginatingAddress();

            for (Map.Entry<String, ?> entry : configs.entrySet()) {
                String entryKey = entry.getKey();
                if (sender.equals(entryKey) || entryKey.equals("*")) {

                    JSONObject messageJson = this.prepareMessage(sender, message.getMessageBody());

                    this.callWebHook((String) entry.getValue(), messageJson.toString());

                    break;
                }
            }
        }
    }

    protected void callWebHook(String url, String message) {
        new WebhookCaller().execute(url, message);
    }

    private Map<String, ?> getConfigs(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.key_phones_preference),
                Context.MODE_PRIVATE
        );
        return sharedPref.getAll();
    }

    private JSONObject prepareMessage(String sender, String message) {
        JSONObject messageData = new JSONObject();
        try {
            messageData.put("from", sender);
            messageData.put("text", message);
        } catch (Exception e) {
            Log.e("SmsGateway", "Exception prepareMessage" + e);
        }

        return messageData;
    }
}
