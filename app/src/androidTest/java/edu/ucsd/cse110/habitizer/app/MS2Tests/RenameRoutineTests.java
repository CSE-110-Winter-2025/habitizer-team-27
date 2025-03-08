package edu.ucsd.cse110.habitizer.app.MS2Tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

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

public class RenameRoutineTests {
    private static final String TAG = "RenameRoutineTest";
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

    // This tests renaming an existing routine
    @Test
    public void testRenamingExistingRoutine() {
        Log.d(TAG, "Running testRenamingExistingRoutine");

        // Wait a moment to ensure UI is fully loaded
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // Ignore
        }
        Espresso.onIdle();

        // Click edit routine button
        onView(withId(R.id.edit_routine_button)).perform(click());

        // Click on "Morning" in the list of routines
        onView(withText("Select Routine to Edit")).check(matches(isDisplayed()));
        onView(withText("Morning")).perform(click());

        // Type in new routine name
        onView(withText("Edit Routine Name")).check(matches(isDisplayed()));
        onView(withText("Morning")).perform(typeText("Test"), closeSoftKeyboard());
        onView(withText("Save")).perform(click());


    }
}
