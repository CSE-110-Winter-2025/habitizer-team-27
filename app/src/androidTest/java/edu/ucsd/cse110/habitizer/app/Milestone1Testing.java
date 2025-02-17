package edu.ucsd.cse110.habitizer.app;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isNotClickable;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.anything;

import android.app.Instrumentation;
import android.view.View;
import android.view.ViewGroup;

@RunWith(AndroidJUnit4.class)
public class Milestone1Testing {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    /**
     * test: start routine
     *  taps the "start Routine" button from the home screen
     *  verifies that the routine screen displays the routine name
     */
    @Test
    public void testStartRoutine() {
        //on the home screen there is a ListView of routines (routine_list.xml)
        //with a "start routine" button (R.id.start_routine_button)
        Espresso.onView(allOf(
                withId(R.id.start_routine_button), hasSibling(withText("Morning"))
        )).perform(click());

        //verify that the routine screen is shown
        //in fragment_routine_screen.xm  there's a TextView with id routine_name_task.
        onView(withId(R.id.routine_name_task))
                .check(matches(withText("Morning")));
    }

    /**
     * test: mark step complete
     *  starts the routine.
     *  marks the first task as complete.
     *  verifies that the task's display is updated.
     */
    @Test
    public void testMarkStepComplete() {
        //start the routine.
        Espresso.onView(allOf(
                withId(R.id.start_routine_button), hasSibling(withText("Morning"))
        )).perform(click());

        //assume the routine screen contains a ListView
        //the first task (position 0) is "shower" with a checkBox
        //tap the CheckBox to mark the task complete.
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.check_task))
                .perform(click());

        //after marking complete assume the task's time is updated from its default "" to a duration "1m").
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.task_time))
                .check(matches(withText("1m")));
    }

    public void checkTaskCompleted(double timeTaken, String expectedTime, int position) {
        for (int i = 0; i < timeTaken * 2; i++) {
            onView(withId(R.id.fast_forward_button)).perform(click());
        }
        Espresso.onIdle();

        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(position)
                .onChildView(withId(R.id.check_task))
                .perform(click());
        Espresso.onIdle();

        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(position)
                .onChildView(withId(R.id.task_time))
                .check(matches(withText(expectedTime)));
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(position)
                .onChildView(withId(R.id.check_task))
                .check(matches(isChecked()));
    }

    /** End to End Scenario #1 **/
    @Test
    public void testEndToEndOne() {
        // Start Morning Routine
        // Check that routine started successfully
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(0)
                .onChildView(withId(R.id.start_routine_button))
                .perform(click());
        onView(withId(R.id.routine_name_task))
                .check(matches(withText("Morning")));

        // Stop timer for testing purposes
        onView(withId(R.id.stop_timer_button)).perform(click());

        // Set routine goal time to be 45 minutes
        // Initially is nothing
        onView(withId(R.id.expected_time)).check(matches(withText("-")));

        onView(withId(R.id.expected_time)).perform(click());
        onView(withId(R.id.routine_goal_time_edit)).perform(typeText("45"), closeSoftKeyboard());
        onView(withText("OK")).perform(click());

        onView(withId(R.id.expected_time)).check(matches(withText("45m")));

        // #1: Tap Shower after 19 minutes
        // Change number of fast forward to account for round up
        checkTaskCompleted(18.5, "19m", 0);

        // #2: Tap Brush teeth after 4 minutes
        checkTaskCompleted(4, "4m", 1);

        // #3: tap Dress after 11 minutes
        checkTaskCompleted(11, "11m", 2);

        // #4: tap Make Coffee after 11 minutes
        checkTaskCompleted(11, "11m", 3);

        // #5: Skip Make Lunch = do nothing

        // #6: Tap Dinner Prep after 1 minute
        checkTaskCompleted(1, "1m", 5);

        // #7: Tap Pack Bag after 1 minutes
        checkTaskCompleted(1, "1m", 6);

        // End Routine
        onView(withId(R.id.end_routine_button)).perform(click());
        onView(withId(R.id.actual_time)).check(matches(withText("47m")));
    }

    @Test
    public void testEndToEndTwo() {
        // Start Evening Routine
        // Check that routine started successfully
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(1)
                .onChildView(withId(R.id.start_routine_button))
                .perform(click());
        onView(withId(R.id.routine_name_task))
                .check(matches(withText("Evening")));

        // Stop timer for testing purposes
        onView(withId(R.id.stop_timer_button)).perform(click());

        // #1: Charge devices after 30 seconds
        checkTaskCompleted(0.5, "1m", 0);

        // #2: Make Dinner after 30 minutes
        checkTaskCompleted(30.0, "30m", 1);

        // #3: Eat Dinner after 60 minutes
        checkTaskCompleted(60.0, "60m", 2);

        // #4: Wash dishes after 30 minutes
        checkTaskCompleted(30.0, "30m", 3);

        // #5: Pack Bag after 30 minutes
        checkTaskCompleted(30.0, "30m", 4);

        // #6: Do homework after 60 minutes
        checkTaskCompleted(60.0, "60m", 5);
        Espresso.onIdle();

        // End Routine Automatically
        // onView(withId(R.id.end_routine_button)).check(matches(isNotClickable()));
        onView(withId(R.id.actual_time)).check(matches(withText("210m")));
    }

    @Test
    public void testEndToEndThree() {
        // Start Evening Routine
        // Check that routine started successfully
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(1)
                .onChildView(withId(R.id.start_routine_button))
                .perform(click());
        onView(withId(R.id.routine_name_task))
                .check(matches(withText("Evening")));

        // Add new item
        onView(withId(R.id.add_task_button)).perform(click());
        Espresso.onIdle();
        onView(withId(R.id.task_name_edit_text)).perform(typeText("Brush Teeth"), closeSoftKeyboard());
        onView(withText("OK")).perform(click());

        // Check item exists
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(6)
                .onChildView(withId(R.id.task_name))
                .check(matches(withText("Brush Teeth")));
    }
}