package tech.bogomolov.incomingsmsgateway;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ListAdapter extends ArrayAdapter<Config> {
    final private ArrayList<Config> dataSet;
    Context context;

    public ListAdapter(ArrayList<Config> data, Context context) {
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

        Config config = getItem(position);

        String senderText = config.getSender();
        TextView sender = row.findViewById(R.id.text_sender);
        sender.setText(senderText.equals("*") ? "Any" : senderText);

        TextView url = row.findViewById(R.id.text_url);
        url.setText(config.getUrl());

        View deleteButton = row.findViewById(R.id.delete_button);
        deleteButton.setTag(R.id.delete_button, position);

        return row;
    }
}
