package tech.bogomolov.incomingsmsgateway;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private Context context;
    private ListAdapter listAdapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable notificationRebindRetry =
            () -> NotificationListener.requestRebindIfEnabled(
                    getApplicationContext(), "activity resume retry");

    private static final int PERMISSION_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ArrayList<String> permissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECEIVE_SMS);
        }
        // Android 13+ gates the foreground-service "F" indicator behind a runtime
        // permission. Without it the service still runs and forwards SMS, but the
        // persistent notification never shows (issue #77). Treated as best-effort:
        // we ask for it, but its denial does not block the app like RECEIVE_SMS does.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (permissions.isEmpty()) {
            showList();
        } else {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != PERMISSION_CODE) {
            return;
        }
        for (int i = 0; i < permissions.length; i++) {
            if (!permissions[i].equals(Manifest.permission.RECEIVE_SMS)) {
                continue;
            }

            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                showList();
            } else {
                showInfo(getResources().getString(R.string.permission_needed));
            }

            return;
        }

        // RECEIVE_SMS wasn't part of this result (it was already granted, and only
        // the best-effort POST_NOTIFICATIONS was requested), so proceed as long as
        // RECEIVE_SMS is in fact still granted. Re-checking also handles the
        // empty-array case Android delivers when the dialog is cancelled.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            showList();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // OEM task cleaners can kill a NotificationListenerService without
        // delivering onListenerDisconnected. Opening the app must recover it
        // without requiring the user to toggle Notification access manually.
        NotificationListener.requestRebindIfEnabled(getApplicationContext(), "activity resume");
        mainHandler.removeCallbacks(notificationRebindRetry);
        mainHandler.postDelayed(notificationRebindRetry, 1000L);
        // Failures accrue in the background, so refresh the retry counter each
        // time the activity comes forward.
        invalidateOptionsMenu();
        // Rules can also change outside this screen (Settings -> import a backup),
        // so reload the list. Null until the permission flow has let showList() run.
        if (listAdapter != null) {
            listAdapter.clear();
            listAdapter.addAll(ForwardingConfig.getAll(this));
        }
    }

    @Override
    protected void onPause() {
        mainHandler.removeCallbacks(notificationRebindRetry);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_bar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem retryItem = menu.findItem(R.id.action_bar_retry_failed);
        int count = FailedMessage.getCount(this);
        retryItem.setVisible(count > 0);
        if (count > 0) {
            retryItem.setTitle(getString(R.string.menu_retry_failed, count));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_bar_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        if (id == R.id.action_bar_notification_access) {
            try {
                // Use the action string instead of the API-21 constant so the
                // APK remains safe on older devices (minSdk is 14).
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.notification_access_unavailable, Toast.LENGTH_LONG).show();
            }
            return true;
        }

        if (id == R.id.action_bar_retry_failed) {
            int count = FailedMessage.getCount(this);
            FailedMessage.retryAll(this);
            Toast.makeText(this, getString(R.string.retry_failed_toast, count), Toast.LENGTH_LONG).show();
            invalidateOptionsMenu();
            return true;
        }

        if (id == R.id.action_bar_syslogs) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View view = getLayoutInflater().inflate(R.layout.syslogs, null);

            String logs = "";
            try {
                // Runtime.exec does not interpret shell pipes. Use logcat's own
                // tag filters so receiver and webhook diagnostics are visible.
                String[] command = new String[]{
                        "logcat", "-d", "-v", "time", "-s",
                        "SmsBroadcastReceiver:I",
                        "NotificationListener:I",
                        "SmsGateway:I",
                        "RequestWorker:I",
                        "AndroidRuntime:E"};
                Process process = Runtime.getRuntime().exec(command);

                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    logs += line + "\n";
                }
            } catch (IOException ex) {
                logs = "getLog failed";
            }

            TextView logsTextContainer = view.findViewById(R.id.syslogs_text);
            logsTextContainer.setText(logs);

            TextView version = view.findViewById(R.id.syslogs_version);
            version.setText("v" + BuildConfig.VERSION_NAME);

            builder.setView(view);
            builder.setNegativeButton(R.string.btn_close, null);
            builder.setNeutralButton(R.string.btn_clear, null);

            final AlertDialog dialog = builder.show();
            Objects.requireNonNull(dialog.getWindow())
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                    .setOnClickListener(view1 -> {
                        String[] command = new String[]{"logcat", "-c"};
                        try {
                            Runtime.getRuntime().exec(command);
                        } catch (IOException e) {
                            Log.e("SmsGateway", "log clear error: " + e);
                        }
                        dialog.cancel();
                    });
        }

        return super.onOptionsItemSelected(item);
    }

    private void showList() {
        context = this;
        ListView listview = findViewById(R.id.listView);

        ArrayList<ForwardingConfig> configs = ForwardingConfig.getAll(context);

        // First-run / empty state: point the user at the + button instead of
        // leaving a blank screen.
        showInfo(configs.isEmpty() ? getString(R.string.empty_list_hint) : "");

        listAdapter = new ListAdapter(configs, context);
        listview.setAdapter(listAdapter);

        // Keep the empty-state hint in sync as rules are added or deleted.
        listAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                showInfo(listAdapter.getCount() == 0 ? getString(R.string.empty_list_hint) : "");
            }
        });

        FloatingActionButton fab = findViewById(R.id.btn_add);
        fab.setOnClickListener(this.showAddDialog());

        if (!this.isServiceRunning()) {
            this.startService();
        }
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (tech.bogomolov.incomingsmsgateway.SmsReceiverService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startService() {
        Context appContext = getApplicationContext();
        Intent intent = new Intent(this, SmsReceiverService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent);
        } else {
            appContext.startService(intent);
        }
    }

    private void showInfo(String text) {
        TextView notice = findViewById(R.id.info_notice);
        notice.setText(text);
        // The icon above the notice should disappear together with the text.
        findViewById(R.id.empty_state).setVisibility(
                text.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private View.OnClickListener showAddDialog() {
        return v -> {
            (new ForwardingConfigDialog(context, getLayoutInflater(), listAdapter)).showNew();
        };
    }
}
