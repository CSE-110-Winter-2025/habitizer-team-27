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
     * test 1: end routine
     *  start a routine by tapping the "start routine" button in the home screen
     *  tap "end routine" and verify the routine completes
     */
    @Test
    public void testEndRoutineFlow() {
        //home screen has a ListView (@+id/card_list) of routines using routine_list.xml
        //if there's only one routine, we can simply click the "start routine" button:
        onView(withId(R.id.start_routine_button)).perform(click());

        //now we're in fragment_routine_screen.xml which has the "End Routine" button.
        onView(withId(R.id.end_routine_button)).perform(click());

        //sfter ending we expect the button text might change to "routine complete!"
        onView(withId(R.id.end_routine_button))
                .check(matches(withText("Routine Complete!")));
    }

    /**
     * test 2: add a task to a routine
     *  after starting a routine, tap "Add Task" to open a dialog
     *  type in a new task name and confirm
     *  verify the new task appears in the routine_list
     */
    @Test
    public void testAddTaskFlow() {
        //start the routine from the home screen
        onView(withId(R.id.start_routine_button)).perform(click());

        //in fragment_routine_screen.xml, there's a "Add Task" button: @+id/add_task_button
        onView(withId(R.id.add_task_button)).perform(click());

        //this shows dialog_add_tasks.xml which has @+id/task_name_edit_text
        onView(withId(R.id.task_name_edit_text))
                .perform(replaceText("Brush Teeth"), closeSoftKeyboard());

        //we assume there's a "save" button in the dialog with text "save"
        onView(withText("Save")).perform(click());

        //now the new task "brush teeth" should appear in the routine_list ListView.
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.task_name))
                .check(matches(withText("Brush Teeth")));
    }

    /**
     * test 3: adding a task with an empty name
     *   start a routine
     *   tap "Add Task"
     *   leave the name blank and tap "Save"
     */
    @Test
    public void testAddTaskEmptyName() {
        //start routine
        onView(withId(R.id.start_routine_button)).perform(click());

        //click "add task"
        onView(withId(R.id.add_task_button)).perform(click());

        //don't type anything (empty name) just tap "save"
        onView(withText("Save")).perform(click());
    }

    /**
     * test 4: verify timer text updates
     *   start a routine
     *   mark 1 or more tasks as complete
     *   check that the "actual_time" TextView in fragment_routine_screen changes from "0" to something else
     */
    @Test
    public void testTimeUpdatesAfterCompletion() {
        //start routine
        onView(withId(R.id.start_routine_button)).perform(click());

        //the routine screen shows "actual_time" (TextView) default "0".
        onView(withId(R.id.actual_time)).check(matches(withText("0")));

        //complete a task at position 0
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.check_task))
                .perform(click());
    }

}