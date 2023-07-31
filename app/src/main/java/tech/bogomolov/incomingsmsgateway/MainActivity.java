package tech.bogomolov.incomingsmsgateway;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private Context context;
    private ListAdapter listAdapter;

    private static final int PERMISSION_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, PERMISSION_CODE);
        } else {
            showList();
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
    }

    private void showList() {
        showInfo("");

        context = this;
        ListView listview = findViewById(R.id.listView);

        ArrayList<ForwardingConfig> configs = ForwardingConfig.getAll(context);

        listAdapter = new ListAdapter(configs, context);
        listview.setAdapter(listAdapter);

        FloatingActionButton fab = findViewById(R.id.btn_add);
        fab.setOnClickListener(this.showAddDialog());

        if (!this.isServiceRunning()) {
            this.startService();
        }
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if(tech.bogomolov.incomingsmsgateway.SmsReceiverService.class.getName().equals(service.service.getClassName())) {
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
    }

    private View.OnClickListener showAddDialog() {
        return v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View view = getLayoutInflater().inflate(R.layout.dialog_add, null);
            final EditText senderInput = view.findViewById(R.id.input_phone);
            final EditText urlInput = view.findViewById(R.id.input_url);
            final EditText templateInput = view.findViewById(R.id.input_json_template);
            final EditText headersInput = view.findViewById(R.id.input_json_headers);
            final CheckBox ignoreSslCheckbox = view.findViewById(R.id.input_ignore_ssl);

            templateInput.setText(ForwardingConfig.getDefaultJsonTemplate());
            headersInput.setText(ForwardingConfig.getDefaultJsonHeaders());

            builder.setView(view);
            builder.setPositiveButton(R.string.btn_add, null);
            builder.setNegativeButton(R.string.btn_cancel, null);
            final AlertDialog dialog = builder.show();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view1 -> {
                String sender = senderInput.getText().toString();
                if (TextUtils.isEmpty(sender)) {
                    senderInput.setError(getString(R.string.error_empty_sender));
                    return;
                }

                String url = urlInput.getText().toString();
                if (TextUtils.isEmpty(url)) {
                    urlInput.setError(getString(R.string.error_empty_url));
                    return;
                }

                try {
                    new URL(url);
                } catch (MalformedURLException e) {
                    urlInput.setError(getString(R.string.error_wrong_url));
                    return;
                }

                String template = templateInput.getText().toString();
                try {
                    new JSONObject(template);
                } catch (JSONException e) {
                    templateInput.setError(getString(R.string.error_wrong_json));
                    return;
                }

                String headers = headersInput.getText().toString();
                try {
                    new JSONObject(headers);
                } catch (JSONException e) {
                    headersInput.setError(getString(R.string.error_wrong_json));
                    return;
                }

                boolean ignoreSsl = ignoreSslCheckbox.isChecked();

                ForwardingConfig config = new ForwardingConfig(context);
                config.setSender(sender);
                config.setUrl(url);
                config.setTemplate(template);
                config.setHeaders(headers);
                config.setIgnoreSsl(ignoreSsl);
                config.save();

                listAdapter.add(config);

                dialog.dismiss();
            });
        };
    }

    public void showEditDialog(ForwardingConfig config) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = getLayoutInflater().inflate(R.layout.dialog_add, null);
        final EditText senderInput = view.findViewById(R.id.input_phone);
        final EditText urlInput = view.findViewById(R.id.input_url);
        final EditText templateInput = view.findViewById(R.id.input_json_template);
        final EditText headersInput = view.findViewById(R.id.input_json_headers);
        final CheckBox ignoreSslCheckbox = view.findViewById(R.id.input_ignore_ssl);

        templateInput.setText(config.getTemplate());
        headersInput.setText(config.getHeaders());
        senderInput.setText(config.getSender());
        urlInput.setText(config.getUrl());
        ignoreSslCheckbox.setChecked(config.getIgnoreSsl());

        builder.setView(view);
        builder.setPositiveButton(R.string.btn_update, null);
        builder.setNegativeButton(R.string.btn_cancel, null);
        final AlertDialog dialog = builder.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view1 -> {
            String sender = senderInput.getText().toString();
            if (TextUtils.isEmpty(sender)) {
                senderInput.setError(getString(R.string.error_empty_sender));
                return;
            }

            String url = urlInput.getText().toString();
            if (TextUtils.isEmpty(url)) {
                urlInput.setError(getString(R.string.error_empty_url));
                return;
            }

            try {
                new URL(url);
            } catch (MalformedURLException e) {
                urlInput.setError(getString(R.string.error_wrong_url));
                return;
            }

            String template = templateInput.getText().toString();
            try {
                new JSONObject(template);
            } catch (JSONException e) {
                templateInput.setError(getString(R.string.error_wrong_json));
                return;
            }

            String headers = headersInput.getText().toString();
            try {
                new JSONObject(headers);
            } catch (JSONException e) {
                headersInput.setError(getString(R.string.error_wrong_json));
                return;
            }

            boolean ignoreSsl = ignoreSslCheckbox.isChecked();

            config.remove();// remove the old config first

            config.setSender(sender);
            config.setUrl(url);
            config.setTemplate(template);
            config.setHeaders(headers);
            config.setIgnoreSsl(ignoreSsl);
            config.save();

            listAdapter.notifyDataSetChanged();

            dialog.dismiss();
        });
    }
}