package tech.bogomolov.incomingsmsgateway;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

public class ForwardingConfigDialog {

    public static void showNewDialog(
            Context context, LayoutInflater layoutInflater, ListAdapter listAdapter) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = layoutInflater.inflate(R.layout.dialog_add, null);

        final EditText templateInput = view.findViewById(R.id.input_json_template);
        final EditText headersInput = view.findViewById(R.id.input_json_headers);
        final CheckBox ignoreSslCheckbox = view.findViewById(R.id.input_ignore_ssl);

        templateInput.setText(ForwardingConfig.getDefaultJsonTemplate());
        headersInput.setText(ForwardingConfig.getDefaultJsonHeaders());

        builder.setView(view);
        builder.setPositiveButton(R.string.btn_add, null);
        builder.setNegativeButton(R.string.btn_cancel, null);

        final AlertDialog dialog = builder.show();
        Objects.requireNonNull(dialog.getWindow())
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view1 -> {
                    ForwardingConfig config = ForwardingConfigDialog
                            .populateConfig(view, context, new ForwardingConfig(context));
                    if (config == null) {
                        return;
                    }

                    listAdapter.add(config);
                    dialog.dismiss();
                });
    }

    public static void showEditDialog(
            ForwardingConfig config,
            Context context,
            LayoutInflater layoutInflater,
            ListAdapter listAdapter
    ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = layoutInflater.inflate(R.layout.dialog_add, null);

        final EditText phoneInput = view.findViewById(R.id.input_phone);
        phoneInput.setText(config.getSender());

        final EditText urlInput = view.findViewById(R.id.input_url);
        urlInput.setText(config.getUrl());



        final EditText templateInput = view.findViewById(R.id.input_json_template);
        templateInput.setText(config.getTemplate());

        final EditText headersInput = view.findViewById(R.id.input_json_headers);
        headersInput.setText(config.getHeaders());

        final CheckBox ignoreSslCheckbox = view.findViewById(R.id.input_ignore_ssl);
        ignoreSslCheckbox.setChecked(config.getIgnoreSsl());

        builder.setView(view);
        builder.setPositiveButton(R.string.btn_save, null);
        builder.setNegativeButton(R.string.btn_cancel, null);

        final AlertDialog dialog = builder.show();
        Objects.requireNonNull(dialog.getWindow())
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view1 -> {
                    ForwardingConfig configUpdated = ForwardingConfigDialog.populateConfig(view, context, config);
                    if (configUpdated == null) {
                        return;
                    }
                    listAdapter.notifyDataSetChanged();
                    dialog.dismiss();
                });
    }

    public static ForwardingConfig populateConfig(View view, Context context, ForwardingConfig config) {
        final EditText senderInput = view.findViewById(R.id.input_phone);
        String sender = senderInput.getText().toString();
        if (TextUtils.isEmpty(sender)) {
            senderInput.setError(context.getString(R.string.error_empty_sender));
            return null;
        }

        final EditText urlInput = view.findViewById(R.id.input_url);
        String url = urlInput.getText().toString();
        if (TextUtils.isEmpty(url)) {
            urlInput.setError(context.getString(R.string.error_empty_url));
            return null;
        }
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            urlInput.setError(context.getString(R.string.error_wrong_url));
            return null;
        }

        final EditText templateInput = view.findViewById(R.id.input_json_template);
        String template = templateInput.getText().toString();
        try {
            new JSONObject(template);
        } catch (JSONException e) {
            templateInput.setError(context.getString(R.string.error_wrong_json));
            return null;
        }

        final EditText headersInput = view.findViewById(R.id.input_json_headers);
        String headers = headersInput.getText().toString();
        try {
            new JSONObject(headers);
        } catch (JSONException e) {
            headersInput.setError(context.getString(R.string.error_wrong_json));
            return null;
        }

        final CheckBox ignoreSslCheckbox = view.findViewById(R.id.input_ignore_ssl);
        boolean ignoreSsl = ignoreSslCheckbox.isChecked();

        config.setSender(sender);
        config.setUrl(url);
        config.setTemplate(template);
        config.setHeaders(headers);
        config.setIgnoreSsl(ignoreSsl);
        config.save();

        return config;
    }
}
