package edu.ucsd.cse110.habitizer.app.MS2Tests;


import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.anything;

import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import edu.ucsd.cse110.habitizer.app.MainActivity;
import edu.ucsd.cse110.habitizer.app.R;
import edu.ucsd.cse110.habitizer.app.TestHelper;
import edu.ucsd.cse110.habitizer.app.data.db.HabitizerRepository;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;

@RunWith(AndroidJUnit4.class)
public class DeleteRoutineTests {
    private static final String TAG = "DeleteRoutineTests";
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

    @Test
    public void testDeleteRoutine(){
        Log.d(TAG, "Running testDeleteRoutine");

        // Wait a moment to ensure UI is fully loaded
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // Ignore
        }
        Espresso.onIdle();

        // Clicks remove button
        onData(Matchers.anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(0)
                .onChildView(withId(R.id.delete_routine_button))
                .perform(click());
        Espresso.onIdle();

        // Wait a bit more after clicking delete a routine
        try {
            Thread.sleep(1000); // Wait for 1 second (adjust as needed)
        } catch (InterruptedException e) {
            // Ignore
        }
        Espresso.onIdle(); // Wait again after sleep

        Espresso.onView(withText("DELETE"))
                .perform(click());

        Espresso.onIdle();

        // Wait a bit more after clicking delete a routine
        try {
            Thread.sleep(1000); // Wait for 1 second (adjust as needed)
        } catch (InterruptedException e) {
            // Ignore
        }
        Espresso.onIdle(); // Wait again after sleep

        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(0)
                .onChildView(withId(R.id.start_routine_button))
                .check(matches(not((withText("Morning")))));

        // Restart the app
        activityRule.getScenario().close();
        ActivityScenario.launch(MainActivity.class, null);

        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(0)
                .onChildView(withId(R.id.start_routine_button))
                .check(matches(not((withText("Morning")))));
    }
}
