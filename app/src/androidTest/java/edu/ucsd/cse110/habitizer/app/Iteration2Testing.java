package edu.ucsd.cse110.habitizer.app;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;

@RunWith(AndroidJUnit4.class)
public class Iteration2Testing {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    /**
     * test: end routine flow
     *  starts a routine
     *  taps "end routine" and verifies that the routine ends
     *  the expected text is "routine ended"
     */
    @Test
    public void testEndRoutineFlow() {
        //home screen has a ListView (@+id/card_list) of routines using routine_list.xml.
        //since there are both Morning and Evening routines the test uses (Morning) routine.
        onView(withId(R.id.start_routine_button)).perform(click());

        //now in fragment_routine_screen.xml tap the "End Routine" button.
        onView(withId(R.id.end_routine_button)).perform(click());

        //after ending we expect the button text to change to "routine ended".
        onView(withId(R.id.end_routine_button))
                .check(matches(withText("Routine Ended")));
    }

    /**
     * test: add a task to a routine
     *  after starting a routine tap "add task" to open the dialog
     *  type a new task name and confirm by tapping the "OK" button.
     *  verify the new task appears in the routine_list.
     */
    @Test
    public void testAddTaskFlow() {
        //start the routine from the home screen
        onView(withId(R.id.start_routine_button)).perform(click());

        //In fragment_routine_screen.xml tap the "add task" button.
        onView(withId(R.id.add_task_button)).perform(click());

        //the dialog (dialog_add_tasks.xml) appears enter the task name.
        onView(withId(R.id.task_name_edit_text))
                .perform(replaceText("Brush Teeth"), closeSoftKeyboard());

        //tap the "OK" button
        onView(withText("OK")).perform(click());

        //verify the new task "Brush Teeth" appears in the routine_list ListView.
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.task_name))
                .check(matches(withText("Brush Teeth")));
    }

    /**
     * test: adding a task with an empty name
     *  start a routine tap "add task" leave the name blank and tap "OK".
     */
    @Test
    public void testAddTaskEmptyName() {
        //start routine
        onView(withId(R.id.start_routine_button)).perform(click());

        //tap "Add Task"
        onView(withId(R.id.add_task_button)).perform(click());

        //do not enter any text and tap the "OK" button.
        onView(withText("OK")).perform(click());
    }

    /**
     * test: verify timer text updates after completing a task
     *  start a routine and verify the "actual_time" TextView initially displays "0".
     *  complete a task at position 0 by tapping
     */
    @Test
    public void testTimeUpdatesAfterCompletion() {
        //start routine
        onView(withId(R.id.start_routine_button)).perform(click());

        //verify the "actual_time" TextView initially displays "0".
        onView(withId(R.id.actual_time)).check(matches(withText("0")));

        //complete a task at position 0 by tapping its check box
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.check_task))
                .perform(click());
    }
}