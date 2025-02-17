package edu.ucsd.cse110.habitizer.app;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;

import android.widget.CheckBox;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class Iteration1Testing {

    // This rule launches MainActivity before each test.
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    /**
     * Test 1: Verify that tapping the "Start Morning Routine" button displays the routine.
     *
     * Finds a button on the screen by looking for the text “Start Morning Routine”
     *  and taps it.
     * Then it checks that a view with the text “Shower” (one of the routine steps) is displayed.
     *
     */
    @Test
    public void testStartRoutine() {
        // Tap the button labeled "Start Morning Routine"
        Espresso.onView(allOf(
                withId(R.id.start_routine_button), hasSibling(withText("Morning"))
        )).perform(click());

        // Verify that a routine step (e.g., "Shower") is now visible on screen.
        Espresso.onView(ViewMatchers.withText("Shower"))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    /**
     * Test 2: Verify that tapping the "Shower" step marks it as complete.
     * For this simple test, we assume that when a step is marked complete,
     * the text "1m" (representing the duration) appears.
     */
    @Test
    public void testMarkStepComplete() {
        // Start the routine first.
        Espresso.onView(allOf(
                withId(R.id.start_routine_button), hasSibling(withText("Morning"))
        )).perform(click());

        // Ensure the "Shower" step is visible.
        Espresso.onView(ViewMatchers.withText("Shower"))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        // Tap the "Shower" step.
        Espresso.onView(allOf(
                isAssignableFrom(CheckBox.class),
                hasSibling(withText("Shower"))
        )).perform(click());

//
//        // Verify that the text "1m" is displayed (indicating the step was completed).
         Espresso.onView(ViewMatchers.withText("1m"))
              .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }
}