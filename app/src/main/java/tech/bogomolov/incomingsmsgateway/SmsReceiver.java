package tech.bogomolov.incomingsmsgateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;

import org.json.JSONObject;

import java.util.Map;

public class SmsReceiver extends BroadcastReceiver {
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(SMS_RECEIVED)) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus.length == 0) {
                    return;
                }

                SharedPreferences sharedPref = context.getSharedPreferences(
                        "phones",
                        Context.MODE_PRIVATE
                );
                Map<String, ?> configs = sharedPref.getAll();
                for (int i = 0; i < pdus.length; i++) {
                    SmsMessage message = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    String sender = message.getOriginatingAddress();

                    for (Map.Entry<String, ?> entry : configs.entrySet()) {
                        if (sender.equals(entry.getKey())) {
                            JSONObject messageData = new JSONObject();
                            try {
                                messageData.put("from", sender);
                                messageData.put("text", message.getMessageBody());
                            } catch (Exception e) {
                                // TODO handle
                            }

                            String[] params = new String[2];
                            params[0] = (String) entry.getValue();
                            params[1] = messageData.toString();

                            WebhookCaller webhookCaller = new WebhookCaller();
                            webhookCaller.execute(params);
                            break;
                        }
                    }
                }
            }
        }
    }
}
