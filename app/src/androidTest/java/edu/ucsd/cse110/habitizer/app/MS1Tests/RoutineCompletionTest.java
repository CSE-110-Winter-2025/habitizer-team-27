package edu.ucsd.cse110.habitizer.app.MS1Tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;

import android.widget.CheckBox;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ucsd.cse110.habitizer.app.MainActivity;
import edu.ucsd.cse110.habitizer.app.R;

@RunWith(AndroidJUnit4.class)
public class RoutineCompletionTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    // Scenario 1: All tasks completed
    @Test
    public void testCompleteAllTasks() {
        // Start morning routine
        onView(CoreMatchers.allOf(
                ViewMatchers.withId(R.id.start_routine_button),
                hasSibling(withText("Evening"))
        )).perform(click());

        // Complete all tasks by clicking their checkboxes
        String[] taskNames = {
                "Charge devices",

                "Prepare dinner",

                "Eat dinner",

                "Wash dishes",

                "Pack bag",

                "Homework",
        };

        for (int i = 0; i < taskNames.length; ++i) {
            onView(allOf(
                    isAssignableFrom(CheckBox.class),
                    hasSibling(withText(taskNames[i]))
            )).perform(click());
        }

        // Verify completion
        onView(withId(R.id.actual_time)).check(matches(isDisplayed()));
        onView(withId(R.id.end_routine_button))
                .check(matches(withText("Routine Ended")));
    }

    // Scenario 2: Skip at least one task
    @Test
    public void testCompleteWithSkippedTasks() {
        // Start routine
        onView(allOf(
                withId(R.id.start_routine_button),
                hasSibling(withText("Morning"))
        )).perform(click());

        // Complete all except last task
        String[] taskNames = {
                "Shower",
                "Brush teeth",
                "Dress",
                "Make coffee",
                "Make lunch",
                "Dinner prep",
        };

        for (int i = 0; i < taskNames.length; i++) {
            onView(allOf(
                    isAssignableFrom(CheckBox.class),
                    hasSibling(withText(taskNames[i]))
            )).perform(click());
        }

        // Force complete with skipped task
        onView(withId(R.id.end_routine_button)).perform(click());

        // Verify partial completion
        onView(withId(R.id.actual_time)).check(matches(isDisplayed()));
        onView(withId(R.id.end_routine_button))
                .check(matches(withText(containsString("Routine Ended"))));
    }
}