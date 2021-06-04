package tech.bogomolov.incomingsmsgateway;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private Context context;
    private ListAdapter listAdapter;

    public void onDeleteClick(View view) {
        final int position = (int) view.getTag(R.id.delete_button);
        final Config config = listAdapter.getItem(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete record");
        builder.setMessage("Do you really want to delete \"" + (config.getSender().equals("*") ? "Any" : config.getSender()) + "\" record?");

        builder.setPositiveButton(R.string.btn_delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                listAdapter.remove(config);
                config.remove();
            }
        });
        builder.setNegativeButton(R.string.btn_cancel, null);
        builder.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        context = this;
        ListView listview = findViewById(R.id.listView);

        ArrayList<Config> configs = Config.getAll(context);
        listAdapter = new ListAdapter(configs, context);

        listview.setAdapter(listAdapter);

        FloatingActionButton fab = findViewById(R.id.btn_add);
        fab.setOnClickListener(this.showAddDialog());
    }

    private View.OnClickListener showAddDialog() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                View view = getLayoutInflater().inflate(R.layout.dialog_add, null);
                final EditText senderInput = view.findViewById(R.id.input_phone);
                final EditText urlInput = view.findViewById(R.id.input_url);

                builder.setView(view);
                builder.setPositiveButton(R.string.btn_add, null);
                builder.setNegativeButton(R.string.btn_cancel, null);
                final AlertDialog dialog = builder.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
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

                        Config config = new Config(context);
                        config.setSender(sender);
                        config.setUrl(url);
                        config.save();

                        listAdapter.add(config);

                        dialog.dismiss();
                    }
                });
            }
        };
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, 0);
        }
    }
}
