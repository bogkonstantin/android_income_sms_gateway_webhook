package tech.bogomolov.incomingsmsgateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;

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

        ArrayList<Config> configs = Config.getAll(context);

        for (Object pdu : pdus) {
            SmsMessage message = SmsMessage.createFromPdu((byte[]) pdu);
            String sender = message.getOriginatingAddress();

            for (Config config : configs) {
                if (sender.equals(config.getSender()) || config.getSender().equals("*")) {
                    JSONObject messageJson = this.prepareMessage(sender, message.getMessageBody());

                    this.callWebHook(config.getUrl(), messageJson.toString());
                    break;
                }
            }
        }
    }

    protected void callWebHook(String url, String message) {
        new WebhookCaller().execute(url, message);
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
