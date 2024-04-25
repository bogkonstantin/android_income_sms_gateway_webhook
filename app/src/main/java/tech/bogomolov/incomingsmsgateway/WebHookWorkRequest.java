package tech.bogomolov.incomingsmsgateway;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.SSLCertificateSocketFactory;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

import tech.bogomolov.incomingsmsgateway.SSLSocketFactory.TLSSocketFactory;

public class WebHookWorkRequest extends Worker {

    public final static String DATA_URL = "URL";
    public final static String DATA_TEXT = "TEXT";
    public final static String DATA_HEADERS = "HEADERS";
    public final static String DATA_IGNORE_SSL = "IGNORE_SSL";
    public final static String DATA_MAX_RETRIES = "MAX_RETRIES";
    public final static String DATA_CHUNKED_MODE = "CHUNKED_MODE";

    public static final String RESULT_SUCCESS = "success";
    public static final String RESULT_ERROR = "error";
    public static final String RESULT_RETRY = "error_retry";

    public WebHookWorkRequest(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        int maxRetries = getInputData().getInt(DATA_MAX_RETRIES, 10);

        if (getRunAttemptCount() > maxRetries) {
            return Result.failure();
        }

        String url = getInputData().getString(DATA_URL);
        String text = getInputData().getString(DATA_TEXT);
        String headers = getInputData().getString(DATA_HEADERS);
        boolean ignoreSsl = getInputData().getBoolean(DATA_IGNORE_SSL, false);
        boolean useChunkedMode = getInputData().getBoolean(DATA_CHUNKED_MODE, true);

        String result = this.makeRequest(url, text, headers, ignoreSsl, useChunkedMode);

        if (result.equals(RESULT_RETRY)) {
            return Result.retry();
        }

        if (result.equals(RESULT_ERROR)) {
            return Result.failure();
        }

        return Result.success();
    }

    @SuppressLint({"AllowAllHostnameVerifier"})
    private String makeRequest(
            String urlString,
            String text,
            String headers,
            boolean ignoreSsl,
            boolean useChunkedMode
    ) {
        String result = RESULT_SUCCESS;

        Log.i("SmsGateway", "request " + urlString);

        HttpURLConnection urlConnection = null;

        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();

            if (urlConnection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) urlConnection).setSSLSocketFactory(
                        new TLSSocketFactory(ignoreSsl)
                );

                if (ignoreSsl) {
                    ((HttpsURLConnection) urlConnection).setHostnameVerifier(new AllowAllHostnameVerifier());
                }
            }

            urlConnection.setDoOutput(true);
            if (useChunkedMode) {
                urlConnection.setChunkedStreamingMode(0);
            } else {
                urlConnection.setFixedLengthStreamingMode(text.length());
            }

            urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            JSONObject headersObj = new JSONObject(headers);
            Iterator<String> keys = headersObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (headersObj.get(key) instanceof JSONObject) {
                    Log.e("SmsGateway", "only string supported in json");
                    continue;
                }

                urlConnection.setRequestProperty(key, (String) headersObj.get(key));
            }

            OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());

            BufferedWriter writer;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
            } else {
                writer = new BufferedWriter(new OutputStreamWriter(out));
            }

            writer.write(text);
            writer.flush();
            writer.close();
            out.close();

            new BufferedInputStream(urlConnection.getInputStream());

            char code = Integer.toString(urlConnection.getResponseCode()).charAt(0);
            if (!Character.toString(code).equals("2")) {
                result = RESULT_RETRY;
            }
        } catch (MalformedURLException e) {
            result = RESULT_ERROR;
            Log.e("SmsGateway", "MalformedURLException " + e);
        } catch (IOException e) {
            result = RESULT_RETRY;
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
