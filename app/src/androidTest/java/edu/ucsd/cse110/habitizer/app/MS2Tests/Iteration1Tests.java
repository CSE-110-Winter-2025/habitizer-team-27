package edu.ucsd.cse110.habitizer.app.MS2Tests;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.anything;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.util.Log;

import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import edu.ucsd.cse110.habitizer.app.MainActivity;
import edu.ucsd.cse110.habitizer.app.R;
import edu.ucsd.cse110.habitizer.app.TestHelper;
import edu.ucsd.cse110.habitizer.app.data.db.HabitizerRepository;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;

public class Iteration1Tests {
    private static final String TAG = "AddRoutineTests";
    HabitizerRepository repository;

    // Initialize test data before any tests run
    @BeforeClass
    public static void setupClass() {
        // Initialize the database with test data before any activities start
        Log.d(TAG, "Setting up test class - initializing test database");
        TestHelper.initializeTestDatabase();
    }

    // This rule is applied after @BeforeClass, so database will be ready when activity starts
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    // This runs before each test method, used to verify test state
    @Before
    public void setup() {
        Log.d(TAG, "Setting up test method - verifying database state");
        // Verify database state using repository
        activityRule.getScenario().onActivity(activity -> {
            repository = HabitizerRepository.getInstance(activity);
            List<Routine> routines = repository.getRoutines().getValue();
            if (routines != null) {
                Log.d(TAG, "Found " + routines.size() + " routines before test");
                for (Routine r : routines) {
                    Log.d(TAG, "  Routine: " + r.getRoutineName() + " (ID: " + r.getRoutineId() + ")");
                }
            } else {
                Log.e(TAG, "No routines found before test - test may fail");
            }
        });
    }

    // This tests runs through the functions of iteration 1
        // 1) Adds a routine
        // 2) Runs the newly added routine
        // 3) Adds tasks to the new routine
        // 4) Moves the tasks in the routine up and down
    @Test
    public void Iteration1Test1() {
        Log.d(TAG, "Running testAddRoutine");

        // Wait a moment to ensure UI is fully loaded
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // Ignore
        }
        Espresso.onIdle();

        // Clicks "Add Routine" button
        Espresso.onView(withText("Add Routine"))
                .perform(click());
        Espresso.onIdle();

        // Check that new routine with default name is at position 2 (after Morning and Evening routines)
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(2)
                .onChildView(withId(R.id.routine_name))
                .check(matches(withText("New Routine")));

        // Check that new routine exists in database
        List<Routine> routines = repository.getRoutines().getValue();
        var currentRoutine = routines.get(2);
        assertEquals(currentRoutine.getRoutineName(), "New Routine");

        // Check that new routine has no tasks
        assertNull(currentRoutine.getTasks());

        // Starts new routine
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(2)
                .onChildView(withId(R.id.start_routine_button))
                .perform(click());
        Espresso.onIdle();

        // Adds three new tasks
            // 1) "Make bed"
            // 2) "Cook breakfast"
            // 3) "Do laundry"
        onView(withId(R.id.add_task_button)).perform(click());
        Espresso.onIdle();
        onView(withId(R.id.task_name_edit_text)).perform(typeText("Make bed"), closeSoftKeyboard());
        onView(withText("OK")).perform(click());
        Espresso.onIdle();

        onView(withId(R.id.add_task_button)).perform(click());
        Espresso.onIdle();
        onView(withId(R.id.task_name_edit_text)).perform(typeText("Cook breakfast"), closeSoftKeyboard());
        onView(withText("OK")).perform(click());
        Espresso.onIdle();

        onView(withId(R.id.add_task_button)).perform(click());
        Espresso.onIdle();
        onView(withId(R.id.task_name_edit_text)).perform(typeText("Do laundry"), closeSoftKeyboard());
        onView(withText("OK")).perform(click());
        Espresso.onIdle();

        // Move "Cook breakfast" (1) above "Make bed" (0)
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(1)
                .onChildView(withId(R.id.move_up_button))
                .perform(click());
        Espresso.onIdle();

        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.task_name))
                .check(matches(withText("Cook breakfast")));
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(1)
                .onChildView(withId(R.id.task_name))
                .check(matches(withText("Make bed")));

        // Attempt to move "Do laundry" down (should not change anything)
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(2)
                .onChildView(withId(R.id.move_down_button))
                .perform(click());
        Espresso.onIdle();

        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(2)
                .onChildView(withId(R.id.task_name))
                .check(matches(withText("Do laundry")));

        // End the routine
        onData(anything())
                .inAdapterView(withId(R.id.end_routine_button)).perform(click());
    }
}
