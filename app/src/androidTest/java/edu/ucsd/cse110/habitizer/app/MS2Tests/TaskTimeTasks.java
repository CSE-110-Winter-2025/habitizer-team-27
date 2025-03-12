package edu.ucsd.cse110.habitizer.app.MS2Tests;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.anything;

import android.util.Log;

import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.hamcrest.Matchers;
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

public class TaskTimeTasks {
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

    // This test checks that if a task is completed in under 5 seconds (or immediately), it rounds up to 5 seconds
    @Test
    public void testImmediateFinish() {
        // Start morning routine
        Log.d(TAG, "Running testImmediateFinish");

        // Starts Morning routine
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(0)
                .onChildView(withId(R.id.start_routine_button))
                .perform(click());

        // Pause time
        onView(withId(R.id.stop_timer_button)).perform(click());

        // Check off "Shower"
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.check_task))
                .perform(click());
        Espresso.onIdle();

        // Check that time is "5s" and task is checked off
        onData(Matchers.anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.task_time))
                .check(matches(withText("5s")));
        onData(Matchers.anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.check_task))
                .check(matches(isChecked()));
    }

    // This test checks that a task completed under a minute as rounded to the nearest 5 seconds
    @Test
    public void testSecondDisplay() {
        // Start morning routine
        Log.d(TAG, "Running testSecondDisplay");

        // Starts Morning routine
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(0)
                .onChildView(withId(R.id.start_routine_button))
                .perform(click());

        // Pause time
        onView(withId(R.id.stop_timer_button)).perform(click());

        // Jump forward 15 seconds
        onView(withId(R.id.stop_timer_button)).perform(click());

        // Check off "Shower"
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.check_task))
                .perform(click());
        Espresso.onIdle();

        // Check that time is "20s" and task is checked off
        onData(Matchers.anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.task_time))
                .check(matches(withText("20s")));
        onData(Matchers.anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.check_task))
                .check(matches(isChecked()));
    }

    // This test checks that a task completed over a minute shows completion time in minute increments, not seconds
    @Test
    public void testCompletedOverMinute() {
        // Start morning routine
        Log.d(TAG, "Running testCompletedOverMinute");

        // Starts Morning routine
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(0)
                .onChildView(withId(R.id.start_routine_button))
                .perform(click());

        // Pause time
        onView(withId(R.id.stop_timer_button)).perform(click());

        // Jump forward 1:30
        for (int i = 0; i < 6; i++) {
            onView(withId(R.id.stop_timer_button)).perform(click());
        }
        Espresso.onIdle();

        // Check off "Shower"
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.check_task))
                .perform(click());
        Espresso.onIdle();

        // Check that time is "2m" and task is checked off
        onData(Matchers.anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.task_time))
                .check(matches(withText("2m")));
        onData(Matchers.anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.check_task))
                .check(matches(isChecked()));
    }
}
