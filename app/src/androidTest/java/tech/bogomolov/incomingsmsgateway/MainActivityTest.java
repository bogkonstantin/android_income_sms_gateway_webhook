package tech.bogomolov.incomingsmsgateway;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
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

    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule(MainActivity.class);


    @Before
    public void clearSharedPrefs() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.key_phones_preference),
                Context.MODE_PRIVATE
        );
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
    }

    @Test
    public void testAddDialogOpen() {
        onView(withId(R.id.btn_add)).perform(click());
        onView(withId(R.id.dialog_add)).check(matches(isDisplayed()));
    }

    @Test
    public void testEmptySenderError() {
        onView(withId(R.id.btn_add)).perform(click());
        ViewInteraction dialog = onView(withId(R.id.dialog_add));

        onView(withId(R.id.input_url))
                .perform(typeText("https://example.com"));

        onView(withText(R.string.btn_add)).perform(click());

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

        onView(withText(R.string.btn_add)).perform(click());

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

        onView(withText(R.string.btn_add)).perform(click());

        onView(withId(R.id.input_url))
                .check(matches(hasErrorText(getResourceString(R.string.error_wrong_url))));

        dialog.check(matches(isDisplayed()));
    }

    @Test
    public void testAddDeleteRecord() {
        String sender = "1234";
        String url = "https://example.com";

        onView(withId(R.id.btn_add)).perform(click());
        onView(withId(R.id.input_phone)).perform(typeText(sender));
        onView(withId(R.id.input_url)).perform(typeText(url));

        onView(withText(R.string.btn_add)).perform(click());

        ViewInteraction record = onView(allOf(
                hasDescendant(withText(containsString(sender))),
                hasDescendant(withText(containsString(url))),
                isDescendantOfA(withId(R.id.listView)))
        );
        record.check(matches(isDisplayed()));

        onView(withId(R.id.dialog_add)).check(doesNotExist());


        ViewInteraction deleteButton = onView(allOf(
                withId(R.id.delete_button),
                isDescendantOfA(withId(R.id.listView)),
                withText(R.string.btn_delete),
                isDisplayed())
        );
        deleteButton.perform(click());

        onView(withText(R.string.delete_record)).check(matches(isDisplayed()));

        onView(allOf(withId(android.R.id.button1), withText(R.string.btn_delete))).perform(click());

        onView(withText(R.string.delete_record)).check(doesNotExist());
        record.check(doesNotExist());
        onView(withId(R.id.dialog_add)).check(doesNotExist());
    }

    private String getResourceString(int id) {
        Context targetContext = ApplicationProvider.getApplicationContext();
        return targetContext.getResources().getString(id);
    }
}
