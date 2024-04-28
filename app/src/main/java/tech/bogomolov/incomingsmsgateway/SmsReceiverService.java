package tech.bogomolov.incomingsmsgateway;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.provider.Telephony;

import androidx.annotation.Nullable;

public class SmsReceiverService extends Service {

    BroadcastReceiver receiver;

    private static final String CHANNEL_ID = "SmsDefault";

    public SmsReceiverService() {
        receiver = new SmsBroadcastReceiver();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter filter = new IntentFilter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            filter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        } else {
            filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        }

        registerReceiver(receiver, filter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getText(R.string.notification_channel),
                    NotificationManager.IMPORTANCE_NONE);

            notificationManager.createNotificationChannel(channel);

            Notification notification =
                    new Notification.Builder(this, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_f)
                            .setColor(getColor(R.color.colorPrimary))
                            .setOngoing(true)
                            .build();

            startForeground(1, notification);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(receiver);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}