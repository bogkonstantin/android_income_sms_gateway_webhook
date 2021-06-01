package tech.bogomolov.incomingsmsgateway;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class WebhookCaller extends AsyncTask<String, String, String> {

    public static final String RESULT_SUCCESS = "success";
    public static final String RESULT_ERROR = "error";
    public static final String RESULT_CONNECTION_ERROR = "connection_error";

    @Override
    protected String doInBackground(String... params) {
        String urlString = params[0];
        String text = params[1];
        String result = RESULT_SUCCESS;

        HttpsURLConnection urlConnection = null;

        try {
            URL url = new URL(urlString);
            urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setChunkedStreamingMode(0);

            OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
            writer.write(text);
            writer.flush();
            writer.close();
            out.close();

            new BufferedInputStream(urlConnection.getInputStream());
        } catch (MalformedURLException e) {
            result = RESULT_ERROR;
            Log.e("SmsGateway", "Exception " + e);
        } catch (IOException e) {
            result = RESULT_CONNECTION_ERROR;
            Log.e("SmsGateway", "Exception " + e);
        } catch (Exception e) {
            result = RESULT_ERROR;
            Log.e("SmsGateway", "Exception " + e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return result;
    }
}
