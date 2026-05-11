package tech.bogomolov.incomingsmsgateway;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.Configuration;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class WebhookCallerTest {

    @Before
    public void setup() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration config = new Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .setExecutor(new SynchronousExecutor())
                .build();

        WorkManagerTestInitHelper.initializeTestWorkManager(
                context, config);
    }

    @Test
    public void testHttpsSuccess() throws Exception {
        WorkInfo workInfo = this.getWorkInfo("https://example.com", "test", "{}", false);
        assertThat(workInfo.getState(), is(WorkInfo.State.SUCCEEDED));
    }

    @Test
    public void testTlsV1Disabled() throws Exception {
        WorkInfo workInfo = this.getWorkInfo("https://wordpress.com", "test", "{}", false);
        assertThat(workInfo.getState(), is(WorkInfo.State.SUCCEEDED));
    }

    @Test
    public void testTlsV1DisabledSslIgnore() throws Exception {
        WorkInfo workInfo = this.getWorkInfo("https://wordpress.com", "test", "{}", true);
        assertThat(workInfo.getState(), is(WorkInfo.State.SUCCEEDED));
    }

    @Test
    public void testHttpSuccess() throws Exception {
        WorkInfo workInfo = this.getWorkInfo("http://example.com", "test", "{}", false);
        assertThat(workInfo.getState(), is(WorkInfo.State.SUCCEEDED));
    }

    @Test
    public void testError() throws Exception {
        WorkInfo workInfo = this.getWorkInfo("not a url", "test", "{}", false);
        assertThat(workInfo.getState(), is(WorkInfo.State.FAILED));
    }

    @Test
    public void testSelfSignedCert() throws Exception {
        WorkInfo workInfo = this.getWorkInfo(
                "https://self-signed.badssl.com/", "test", "{\"User-Agent\":\"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36\"}", true);
        assertThat(workInfo.getState(), is(WorkInfo.State.SUCCEEDED));
    }

    private WorkInfo getWorkInfo(String url, String text, String headers, boolean ignoreSsl) throws Exception {
        Data input = new Data.Builder()
                .put(RequestWorker.DATA_URL, url)
                .put(RequestWorker.DATA_TEXT, text)
                .put(RequestWorker.DATA_HEADERS, headers)
                .put(RequestWorker.DATA_IGNORE_SSL, ignoreSsl)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(RequestWorker.class)
                .setInputData(input)
                .build();

        WorkManager workManager = WorkManager.getInstance(getApplicationContext());
        workManager.enqueue(request).getResult().get();
        return workManager.getWorkInfoById(request.getId()).get();
    }
}
