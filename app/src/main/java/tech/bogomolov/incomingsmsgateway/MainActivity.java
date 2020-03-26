package tech.bogomolov.incomingsmsgateway;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Context context;
    private ListView listview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        context = this;
        listview = (ListView) findViewById(R.id.listView);

        loadPhoneList();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.btn_add);
        fab.setOnClickListener(this.showAddDialog());
    }

    private View.OnClickListener showAddDialog() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                View view = getLayoutInflater().inflate(R.layout.dialog_add, null);
                final EditText phoneInput = (EditText) view.findViewById(R.id.input_phone);
                final EditText urlInput = (EditText) view.findViewById(R.id.input_url);

                builder.setView(view);
                builder.setPositiveButton(R.string.btn_add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String phone = phoneInput.getText().toString();
                        if (TextUtils.isEmpty(phone)) {
                            return;
                        }

                        String url = urlInput.getText().toString();
                        if (TextUtils.isEmpty(url)) {
                            return;
                        }

                        SharedPreferences sharedPref = getSharedPreferences(
                                getString(R.string.key_phones_preference),
                                Context.MODE_PRIVATE
                        );
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString(phone, url);
                        editor.commit();
                        loadPhoneList();
                    }
                });
                builder.setNegativeButton(R.string.btn_cancel, null);
                builder.show();
            }
        };
    }

    private void loadPhoneList() {
        SharedPreferences sharedPref = context.getSharedPreferences(
                getString(R.string.key_phones_preference), Context.MODE_PRIVATE
        );
        Map<String, ?> configs = sharedPref.getAll();
        final ArrayList<String> listToRender = new ArrayList<String>();
        for (Map.Entry<String, ?> entry : configs.entrySet()) {
            listToRender.add(entry.getKey() + "\n" + entry.getValue());
        }

        ArrayAdapter aAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, listToRender);

        listview.setAdapter(aAdapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String text = (String) parent.getItemAtPosition(position);
                String lines[] = text.split("\\n");
                final String phoneNumber = lines[0];

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Delete record");
                builder.setMessage("Do you really want to delete " + phoneNumber + "?");

                builder.setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        SharedPreferences sharedPref = getSharedPreferences(
                                getString(R.string.key_phones_preference),
                                Context.MODE_PRIVATE
                        );
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.remove(phoneNumber);
                        editor.commit();
                        loadPhoneList();
                    }
                });
                builder.setNegativeButton(R.string.btn_cancel, null);
                builder.show();
            }
        });
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, 0);
        }
    }
}
