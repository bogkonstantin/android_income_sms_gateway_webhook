package tech.bogomolov.incomingsmsgateway;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class WebhookCallerTest {

    @Test
    public void testSuccess() {
        WebhookCaller webhookCaller = new WebhookCaller();
        String result = webhookCaller.doInBackground("https://example.com", "test");

        assertEquals(result, WebhookCaller.RESULT_SUCCESS);
    }

    @Test
    public void testError() {
        WebhookCaller webhookCaller = new WebhookCaller();
        String result = webhookCaller.doInBackground("not a url", "test");

        assertEquals(result, WebhookCaller.RESULT_ERROR);
    }

    @Test
    public void testConnectionError() {
        WebhookCaller webhookCaller = new WebhookCaller();
        String result = webhookCaller.doInBackground("https://urlisnotexisterrortest.ee", "test");

        assertEquals(result, WebhookCaller.RESULT_CONNECTION_ERROR);
    }
}
