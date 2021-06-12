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
        WorkInfo workInfo = this.getWorkInfo("https://example.com", "test");
        assertThat(workInfo.getState(), is(WorkInfo.State.SUCCEEDED));
    }

    @Test
    public void testHttpSuccess() throws Exception {
        WorkInfo workInfo = this.getWorkInfo("http://example.com", "test");
        assertThat(workInfo.getState(), is(WorkInfo.State.SUCCEEDED));
    }

    @Test
    public void testError() throws Exception {
        WorkInfo workInfo = this.getWorkInfo("not a url", "test");
        assertThat(workInfo.getState(), is(WorkInfo.State.FAILED));
    }

    private WorkInfo getWorkInfo(String url, String text) throws Exception {
        Data input = new Data.Builder()
                .put(WebHookWorkRequest.DATA_URL, url)
                .put(WebHookWorkRequest.DATA_TEXT, text)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(WebHookWorkRequest.class)
                .setInputData(input)
                .build();

        WorkManager workManager = WorkManager.getInstance(getApplicationContext());
        workManager.enqueue(request).getResult().get();
        return workManager.getWorkInfoById(request.getId()).get();
    }
}
