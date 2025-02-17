package edu.ucsd.cse110.habitizer.app;

import static androidx.test.espresso.Espresso.onData;
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

import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;

@RunWith(AndroidJUnit4.class)
// Test Editing a routine by adding a task after a routine as already been made
public class EditRoutineTests {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    // adding one task to the routine
    @Test
    public void testAddTask() {
        Routine morningRoutine = new Routine(1, "Morning Routine");
        morningRoutine.addTask(new Task(100, "Eat Breakfast", false));
        morningRoutine.addTask(new Task(101, "Brush Hair", false));
        morningRoutine.addTask(new Task(102, "Pack Backpack", false));

        Task test1 = new Task(103, "Brush Teeth", false);
        morningRoutine.addTask(test1);
        assertTrue(morningRoutine.getTasks().contains(test1));
    }

    // adding 3 tasks to the routine
    @Test
    public void addMultipleTasks() {
        Routine eveningRoutine = new Routine(1, "Evening Routine");
        eveningRoutine.addTask(new Task(100, "Walk dogs", false));
        eveningRoutine.addTask(new Task(101, "Feed dogs", false));
        eveningRoutine.addTask(new Task(102, "Cook dinner", false));

        Task test1 = new Task(103, "Eat Dinner", false);
        Task test2 = new Task(104, "Wash Dishes", false);
        Task test3 = new Task(105, "Clean kitchen", false);
        eveningRoutine.addTask(test1);
        eveningRoutine.addTask(test2);
        eveningRoutine.addTask(test3);
        assertTrue(eveningRoutine.getTasks().contains(test1));
        assertTrue(eveningRoutine.getTasks().contains(test2));
        assertTrue(eveningRoutine.getTasks().contains(test3));
    }

    // add task to empty routine
    @Test
    public void addTaskEmptyRoutine() {
        Routine eveningRoutine = new Routine(1, "Evening Routine");

        Task test1 = new Task(101, "Eat Dinner", false);

        eveningRoutine.addTask(test1);

        assertTrue(eveningRoutine.getTasks().contains(test1));
    }

    // adds 3 tasks to an empty routine
    @Test
    public void addMultiEmptyRoutine() {
        Routine eveningRoutine = new Routine(1, "Evening Routine");

        Task test1 = new Task(101, "Eat Dinner", false);
        Task test2 = new Task(102, "Wash Dishes", false);
        Task test3 = new Task(103, "Clean kitchen", false);
        eveningRoutine.addTask(test1);
        eveningRoutine.addTask(test2);
        eveningRoutine.addTask(test3);
        assertTrue(eveningRoutine.getTasks().contains(test1));
        assertTrue(eveningRoutine.getTasks().contains(test2));
        assertTrue(eveningRoutine.getTasks().contains(test3));
    }

    // check if new added test is the last on the list
    @Test
    public void lastTask() {

        Espresso.onView(allOf(
                withId(R.id.start_routine_button), hasSibling(withText("Morning"))
        )).perform(click());

        Espresso.onView(withId(R.id.add_task_button))
                .perform(click());

        Espresso.onView(withId(R.id.task_name_edit_text))
                .check(matches(isDisplayed()))
                .perform(typeText("Sweep kitchen"), closeSoftKeyboard());


        Espresso.onView(withText("OK"))
                .perform(click());

        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(7)
                .perform(scrollTo());

        Espresso.onView(withText("Sweep kitchen"))
                .check(matches(isDisplayed()));

    }

}

