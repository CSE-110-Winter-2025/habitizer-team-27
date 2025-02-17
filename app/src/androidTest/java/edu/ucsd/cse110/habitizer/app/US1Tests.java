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
import static org.junit.Assert.assertTrue;

import androidx.test.espresso.Espresso;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
public class US1Tests {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    // makes sure all default tasks are found
    @Test
    public void showAllTasks(){

        onView(allOf(
                withId(R.id.start_routine_button), hasSibling(withText("Morning"))
        )).perform(click());

        onView(withText("Shower")).check(matches(isDisplayed()));
        onView(withText("Brush teeth")).check(matches(isDisplayed()));
        onView(withText("Dress")).check(matches(isDisplayed()));
        onView(withText("Make coffee")).check(matches(isDisplayed()));
        onView(withText("Make lunch")).check(matches(isDisplayed()));
        onView(withText("Dinner prep")).check(matches(isDisplayed()));


    }

    // tests that time is getting tracked and rounded properly
    @Test
    public void timeTracking() {
        onView(allOf(
                withId(R.id.start_routine_button),
                hasSibling(withText("Morning"))
        )).perform(click());

        onView(allOf(
                withId(R.id.check_task),
                hasSibling(withText("Shower"))
        )).perform(click());

        onView(allOf(
                withId(R.id.check_task),
                hasSibling(withText("Brush teeth"))
        )).perform(click());

        onView(allOf(
                withId(R.id.task_time),
                withText("1m"),
                hasSibling(withText("Shower"))
        )).check(matches(isDisplayed()));

        onView(allOf(
                withId(R.id.task_time),
                withText("1m"),
                hasSibling(withText("Brush teeth"))
        )).check(matches(isDisplayed()));

    }

}
