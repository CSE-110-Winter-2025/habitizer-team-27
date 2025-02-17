package edu.ucsd.cse110.habitizer.app;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;

import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertTrue;

import androidx.test.espresso.Espresso;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class US2Tests {
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    // Tests completing a step
    @Test
    public void completeTask() {
        onView(allOf(
                withId(R.id.start_routine_button), hasSibling(withText("Morning"))
        )).perform(click());


        onView(allOf(
                withId(R.id.check_task),
                hasSibling(withText("Shower"))
        )).perform(click());


        onView(allOf(
                withId(R.id.task_time),
                withText("1m"),
                hasSibling(withText("Shower"))
        )).check(matches(isDisplayed()));
    }

    @Test
    public void completesMultiStep() {
        onView(allOf(
                withId(R.id.start_routine_button), hasSibling(withText("Morning"))
        )).perform(click());

        onView(allOf(
                withId(R.id.check_task),
                hasSibling(withText("Shower"))
        )).perform(click());

        onView(allOf(
                withId(R.id.check_task),
                hasSibling(withText("Dress"))
        )).perform(click());

        onView(allOf(
                withId(R.id.check_task),
                hasSibling(withText("Make coffee"))
        )).perform(click());


        onView(allOf(
                withId(R.id.task_time),
                withText("1m"),
                hasSibling(withText("Shower"))
        )).check(matches(isDisplayed()));


        onView(allOf(
                withId(R.id.task_time),
                withText("1m"),
                hasSibling(withText("Dress"))
        )).check(matches(isDisplayed()));


        onView(allOf(
                withId(R.id.task_time),
                withText("1m"),
                hasSibling(withText("Make coffee"))
        )).check(matches(isDisplayed()));

    }

    // skip tasks and able to end routine.
    @Test
    public void skipTask(){
        onView(allOf(
                withId(R.id.start_routine_button),
                hasSibling(withText("Morning"))
        )).perform(click());

        Espresso.onIdle();

        onView(allOf(
                withId(R.id.check_task),
                hasSibling(withText("Shower"))
        )).perform(click());
        Espresso.onIdle();

        onView(allOf(
                withId(R.id.check_task),
                hasSibling(withText("Brush teeth"))
        )).perform(click());
        Espresso.onIdle();

        onView(allOf(
                withId(R.id.check_task),
                hasSibling(withText("Dress"))
        )).perform(click());
        Espresso.onIdle();

        onView(allOf(
                withId(R.id.check_task),
                hasSibling(withText("Make coffee"))
        )).perform(click());
        Espresso.onIdle();

        Espresso.onIdle();
        onView(allOf(
                withId(R.id.end_routine_button)
        )).perform(click());

        Espresso.onIdle();
        Espresso.onIdle();
        Espresso.onIdle();
        onView(allOf(
                withId(R.id.actual_time),
                withText(containsString("1m"))
        )).check(matches(isDisplayed()));

    }
}
