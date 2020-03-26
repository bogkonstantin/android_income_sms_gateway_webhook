package tech.bogomolov.incomingsmsgateway;

import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class WebhookCaller extends AsyncTask<String, String, String> {

    @Override
    protected String doInBackground(String... params) {
        String urlString = params[0];
        String text = params[1];

        try {
            URL url = new URL(urlString);
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setChunkedStreamingMode(0);

            OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
            writer.write(text);
            writer.flush();
            writer.close();
            out.close();

            try {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                int code = urlConnection.getResponseCode();
                if (code != HttpsURLConnection.HTTP_ACCEPTED) {
                    // TODO handle error
                }
            } finally {
                urlConnection.disconnect();
            }
        } catch (Exception e) {
            // TODO handle error
        }

        return "";
    }
}
