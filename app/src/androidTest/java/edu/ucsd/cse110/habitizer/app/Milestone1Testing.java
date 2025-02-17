package edu.ucsd.cse110.habitizer.app;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;

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
        onView(withId(R.id.start_routine_button)).perform(click());

        //verify that the routine screen is shown
        //in fragment_routine_screen.xm  there's a TextView with id routine_name_task.
        onView(withId(R.id.routine_name_task))
                .check(matches(withText("Routine Name")));
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
        onView(withId(R.id.start_routine_button)).perform(click());

        //assume the routine screen contains a ListView
        //the first task (position 0) is "shower" with a checkBox
        //tap the CheckBox to mark the task complete.
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.check_task))
                .perform(click());

        //after marking complete assume the task's time is updated from its default "[ - ]" to a duration "19m").
        //adjust "19m" if your implementation displays a different value
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.task_time))
                .check(matches(withText("19m")));
    }
}