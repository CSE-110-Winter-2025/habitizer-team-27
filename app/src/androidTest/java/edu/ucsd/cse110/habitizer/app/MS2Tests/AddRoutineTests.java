package edu.ucsd.cse110.habitizer.app.MS2Tests;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;

import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.util.Log;

import androidx.test.espresso.Espresso;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import edu.ucsd.cse110.habitizer.app.MainActivity;
import edu.ucsd.cse110.habitizer.app.R;
import edu.ucsd.cse110.habitizer.app.TestHelper;
import edu.ucsd.cse110.habitizer.app.data.db.AppDatabase;
import edu.ucsd.cse110.habitizer.app.data.db.HabitizerRepository;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;

@RunWith(AndroidJUnit4.class)

// Tests adding a routine (US1)
public class AddRoutineTests {
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

    // Tests adding a new routine by clicking the "Add Routine" button
    @Test
    public void testAddRoutine() {
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
        var currentRoutine = routines.get(routines.size() - 1);
        assertEquals(currentRoutine.getRoutineName(), "New Routine");

        // Check that new routine has no tasks
        assertNull(currentRoutine.getTasks());
    }

    // Tests that start a default routine (or a routine with no tasks) automatically stops the routine
    @Test
    public void testStartEmptyRoutine() {
        Log.d(TAG, "Running testStartEmptyRoutine");

        // Clicks "Add Routine" button
        Espresso.onView(withText("Add Routine"))
                .perform(click());
        Espresso.onIdle();

        // Starts Routine
        onData(Matchers.anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(2)
                .onChildView(withId(R.id.start_routine_button))
                .perform(click());
        onView(withId(R.id.routine_name_task))
                .check(matches(withText("New Routine")));

        // Checks that routine has ended
        onView(withId(R.id.end_routine_button)).check(matches(not(isEnabled())));
        onView(withId(R.id.end_routine_button))
                .check(matches(withText(containsString("Routine Ended"))));
    }
}

