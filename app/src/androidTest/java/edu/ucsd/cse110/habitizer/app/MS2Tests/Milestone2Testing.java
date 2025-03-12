package edu.ucsd.cse110.habitizer.app.MS2Tests;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.is;

import android.util.Log;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
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

public class Milestone2Testing {
    private static final String TAG = "Milestone2Tests";
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

    // This tests both end to end scenarios:
    // Busy Belinda catches up on a Friday evening and
    // Belinda gets going on a Saturday morning

    // We cannot split these into two tests because for each test, the testing repository is reset
    // Thus, if we were to split this into two tests, checking persistence of the "Friday evening" routine would not work
    // (since the repository has reset to the default values)
    @Test
    public void MS2Test1() {
        Log.d(TAG, "Running MS2Test1");

        /* *** Busy Belinda catches up on a Friday evening *** */
        // 1) Add a new routine ad check it exists after all other routines
        Espresso.onView(withText("Add Routine"))
                .perform(click());
        Espresso.onIdle();

        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(2)
                .onChildView(withId(R.id.routine_name))
                .check(matches(withText("New Routine")));

        // 2) Rename routine to "Friday Evening"
        // Click edit routine button
        onView(withId(R.id.rename_routine_button)).perform(click());
        Espresso.onIdle();

        // Click on "New Routine" in the list of routines
        onView(withText("Select Routine to Edit")).check(matches(isDisplayed()));
        onView(withText("New Routine")).perform(click());

        // Type in new routine name
        onView(withText("Edit Routine Name")).check(matches(isDisplayed()));
        onView(withClassName(is(EditText.class.getName()))).perform(typeText("Friday Evening"), closeSoftKeyboard());

        Espresso.onIdle();
        onView(withText("Save")).perform(click());

        // Check routine has been renamed
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(2)
                .onChildView(withId(R.id.routine_name))
                .check(matches(withText("Friday Evening")));

        // 3) Add new tasks and the goal time
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(2)
                .onChildView(withId(R.id.start_routine_button))
                .perform(click());
        Espresso.onIdle();

        // Adds three new tasks
            // 1) "Dinner"
            // 2) "Brush teeth"
            // 3) "Jammies"
        onView(withId(R.id.add_task_button)).perform(click());
        Espresso.onIdle();
        onView(withId(R.id.task_name_edit_text)).perform(typeText("Dinner"), closeSoftKeyboard());
        onView(withText("OK")).perform(click());
        Espresso.onIdle();

        onView(withId(R.id.add_task_button)).perform(click());
        Espresso.onIdle();
        onView(withId(R.id.task_name_edit_text)).perform(typeText("Brush teeth"), closeSoftKeyboard());
        onView(withText("OK")).perform(click());
        Espresso.onIdle();

        onView(withId(R.id.add_task_button)).perform(click());
        Espresso.onIdle();
        onView(withId(R.id.task_name_edit_text)).perform(typeText("Jammies"), closeSoftKeyboard());
        onView(withText("OK")).perform(click());
        Espresso.onIdle();

        // Update goal time
        onView(withId(R.id.expected_time)).perform(click());
        onView(withId(R.id.routine_goal_time_edit)).perform(typeText("90"), closeSoftKeyboard());
        onView(withText("OK")).perform(click());

        onView(withId(R.id.expected_time)).check(matches(withText("90m")));

        // 4) Add "Charge devices" and move it to the top
        onView(withId(R.id.add_task_button)).perform(click());
        Espresso.onIdle();
        onView(withId(R.id.task_name_edit_text)).perform(typeText("Charge devices"), closeSoftKeyboard());
        onView(withText("OK")).perform(click());
        Espresso.onIdle();

        for (int i = 3; i > 0; i--) {
            onData(anything())
                    .inAdapterView(withId(R.id.routine_list))
                    .atPosition(i)
                    .onChildView(withId(R.id.move_up_button))
                    .perform(click());
            Espresso.onIdle();
        }
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.task_name))
                .check(matches(withText("Charge devices")));

        // 5) Check off "Dinner" after 44 minutes
        // Restart routine so that times are correct
        onView(withId(R.id.end_routine_button)).perform(click());
        onView(withId(R.id.home_button)).perform(click());
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(2)
                .onChildView(withId(R.id.start_routine_button))
                .perform(click());
        Espresso.onIdle();

        // Pause time
        onView(withId(R.id.stop_timer_button)).perform(click());
        for (int i = 0; i < (44*4+3); i++) {
            onView(withId(R.id.stop_timer_button)).perform(click());
        }

        // Check routine time
        onView(withId(R.id.actual_time)).check(matches(withText("44m")));

        // Check off dinner and check time
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(1)
                .onChildView(withId(R.id.check_task))
                .perform(click());
        Espresso.onIdle();

        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(1)
                .onChildView(withId(R.id.task_time))
                .check(matches(withText("45m")));

        // 6) Pause routine
        onView(withId(R.id.pause_button)).perform(click());

        // 7) Resume routine
        onView(withId(R.id.pause_button)).perform(click());

        // 8) Check status bar values
        onView(withId(R.id.current_task_elapsed_time)).check(matches(withText("Elapsed time of the current task: 0s")));
        onView(withId(R.id.actual_time)).check(matches(withText("44m")));
        onView(withId(R.id.expected_time)).check(matches(withText("90m")));

        // 9) Check off "Charge devices" after 20 seconds
            // Note: since we can only increment every 15s, we check it off after 15 seconds
        onView(withId(R.id.stop_timer_button)).perform(click());
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.check_task))
                .perform(click());
        Espresso.onIdle();

        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.task_time))
                .check(matches(withText("15s")));
        onView(withId(R.id.actual_time)).check(matches(withText("45m")));

        // 10) End Routine
        onView(withId(R.id.end_routine_button)).perform(click());

        /* *** Belinda gets going on Saturday morning *** */
        // 1) Restart the app
        activityRule.getScenario().close();
        ActivityScenario.launch(MainActivity.class, null);

        // 2) Check "Friday evening" still exists
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(2)
                .onChildView(withId(R.id.routine_name))
                .check(matches(withText("Friday Evening")));
    }
}
