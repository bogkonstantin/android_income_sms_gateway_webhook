package tech.bogomolov.incomingsmsgateway;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;

import java.util.ArrayList;

public class ListAdapter extends ArrayAdapter<ForwardingConfig> {
    final private ArrayList<ForwardingConfig> dataSet;
    Context context;

    public ListAdapter(ArrayList<ForwardingConfig> data, Context context) {
        super(context, R.layout.list_item, data);
        this.dataSet = data;
        this.context = context;
    }

    @Override
    public int getCount() {
        return this.dataSet.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View row = convertView;
        if (null == convertView) {
            row = inflater.inflate(R.layout.list_item, parent, false);
        }

        ForwardingConfig config = getItem(position);

        String senderText = config.getSender();
        String asterisk = context.getString(R.string.asterisk);
        String any = context.getString(R.string.any);
        TextView sender = row.findViewById(R.id.text_sender);
        sender.setText(senderText.equals(asterisk) ? any : senderText);

        TextView url = row.findViewById(R.id.text_url);
        url.setText(config.getUrl());

        TextView template = row.findViewById(R.id.text_template);
        template.setText(config.getTemplate());

        TextView headers = row.findViewById(R.id.text_headers);
        headers.setText(config.getHeaders());

        SwitchCompat switchSmsOnOff = row.findViewById(R.id.switch_sms_on_off);
        TextView switchSmsLabel = row.findViewById(R.id.text_sms_on_off);
        if (config.getIsSmsEnabled()) {
            switchSmsOnOff.setChecked(true);
            switchSmsLabel.setText(R.string.btn_on);
        } else {
            switchSmsOnOff.setChecked(false);
            switchSmsLabel.setText(R.string.btn_off);
        }

        switchSmsOnOff.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                config.setIsSmsEnabled(true);
                switchSmsLabel.setText(R.string.btn_on);
            } else {
                config.setIsSmsEnabled(false);
                switchSmsLabel.setText(R.string.btn_off);
            }
            config.save();
        });

        View editButton = row.findViewById(R.id.edit_button);
        editButton.setTag(R.id.edit_button, position);
        editButton.setOnClickListener(this::onEditClick);

        View deleteButton = row.findViewById(R.id.delete_button);
        deleteButton.setTag(R.id.delete_button, position);
        deleteButton.setOnClickListener(this::onDeleteClick);

        return row;
    }

    public void onEditClick(View view) {
        ListAdapter listAdapter = this;
        final int position = (int) view.getTag(R.id.edit_button);
        final ForwardingConfig config = listAdapter.getItem(position);
        (new ForwardingConfigDialog(
                context,
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE),
                listAdapter
        )).showEdit(config);
    }

    public void onDeleteClick(View view) {
        ListAdapter listAdapter = this;
        final int position = (int) view.getTag(R.id.delete_button);
        final ForwardingConfig config = listAdapter.getItem(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setTitle(R.string.delete_record);
        String asterisk = context.getString(R.string.asterisk);
        String any = context.getString(R.string.any);
        String message = context.getString(R.string.confirm_delete);
        message = String.format(message, (config.getSender().equals(asterisk) ? any : config.getSender()));
        builder.setMessage(message);

        builder.setPositiveButton(R.string.btn_delete, (dialog, id) -> {
            listAdapter.remove(config);
            config.remove();
        });
        builder.setNegativeButton(R.string.btn_cancel, null);
        builder.show();
    }
}
