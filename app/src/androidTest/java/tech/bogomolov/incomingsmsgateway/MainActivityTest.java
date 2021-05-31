package tech.bogomolov.incomingsmsgateway;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule(MainActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.RECEIVE_SMS");

    @Test
    public void testAddDialogOpen() {
        onView(withId(R.id.btn_add)).perform(click());
        onView(withId(R.id.dialog_add)).check(matches(isDisplayed()));
    }

    @Test
    public void testAddNewRecord() {
        String sender = "1234";
        String url = "https://example.com";

        onView(withId(R.id.btn_add)).perform(click());
        onView(withId(R.id.input_phone)).perform(typeText(sender));
        onView(withId(R.id.input_url)).perform(typeText(url));

        onView(withText("Add")).perform(click());

        onView(allOf(withId(android.R.id.text1), withParent(withId(R.id.listView))))
                .check(matches(withText(containsString(sender))))
                .check(matches(withText(containsString(url))));

        onView(withId(R.id.dialog_add)).check(doesNotExist());
    }

    @Test
    public void testEmptySenderError() {
        onView(withId(R.id.btn_add)).perform(click());
        ViewInteraction dialog = onView(withId(R.id.dialog_add));

        onView(withId(R.id.input_url))
                .perform(typeText("https://example.com"));

        onView(withText("Add")).perform(click());

        onView(withId(R.id.input_phone))
                .check(matches(hasErrorText(getResourceString(R.string.error_empty_sender))));

        dialog.check(matches(isDisplayed()));
    }

    @Test
    public void testEmptyUrlError() {
        onView(withId(R.id.btn_add)).perform(click());
        ViewInteraction dialog = onView(withId(R.id.dialog_add));

        onView(withId(R.id.input_phone))
                .perform(typeText("test"));

        onView(withText("Add")).perform(click());

        onView(withId(R.id.input_url))
                .check(matches(hasErrorText(getResourceString(R.string.error_empty_url))));

        dialog.check(matches(isDisplayed()));
    }

    @Test
    public void testWrongUrlError() {
        onView(withId(R.id.btn_add)).perform(click());
        ViewInteraction dialog = onView(withId(R.id.dialog_add));

        onView(withId(R.id.input_phone))
                .perform(typeText("test"));

        onView(withId(R.id.input_url))
                .perform(typeText("not url"));

        onView(withText("Add")).perform(click());

        onView(withId(R.id.input_url))
                .check(matches(hasErrorText(getResourceString(R.string.error_wrong_url))));

        dialog.check(matches(isDisplayed()));
    }

    private String getResourceString(int id) {
        Context targetContext = ApplicationProvider.getApplicationContext();
        return targetContext.getResources().getString(id);
    }
}
