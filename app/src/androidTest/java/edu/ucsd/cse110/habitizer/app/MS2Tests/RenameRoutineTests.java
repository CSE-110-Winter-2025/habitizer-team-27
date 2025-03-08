package edu.ucsd.cse110.habitizer.app.MS2Tests;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;

import android.util.Log;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
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
import edu.ucsd.cse110.habitizer.app.ui.dialog.EditRoutineNameDialogFragment;
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
        onView(withId(R.id.rename_routine_button)).perform(click());
        Espresso.onIdle();

        // Click on "Morning" in the list of routines
        onView(withText("Select Routine to Edit")).check(matches(isDisplayed()));
        onView(withText("Morning")).perform(click());

        // Type in new routine name
        onView(withText("Edit Routine Name")).check(matches(isDisplayed()));
        onView(withClassName(is(EditText.class.getName()))).perform(typeText("Test"), closeSoftKeyboard());

        Espresso.onIdle();
        onView(withText("Save")).perform(click());

        // Check routine has been renamed
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(0)
                .onChildView(withId(R.id.routine_name))
                .check(matches(withText("Test")));

        // Start routine and check the name is still correct
        onData(Matchers.anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(0)
                .onChildView(withId(R.id.start_routine_button))
                .perform(click());
        onView(withId(R.id.routine_name_task))
                .check(matches(withText("Test")));

        // Restart the app and see if changes persist
        onView(withId(R.id.end_routine_button)).perform(click());

        activityRule.getScenario().close();
        ActivityScenario.launch(MainActivity.class, null);

        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(0)
                .onChildView(withId(R.id.routine_name))
                .check(matches(withText("Test")));
    }

    // This tests renaming a newly added routine
    @Test
    public void testRenamingNewRoutine() {
        Log.d(TAG, "Running testRenamingNewRoutine");

        // Wait a moment to ensure UI is fully loaded
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // Ignore
        }
        Espresso.onIdle();

        // Add new routine
        onView(withId(R.id.add_routine_button)).perform(click());

        // Click edit routine button
        onView(withId(R.id.rename_routine_button)).perform(click());
        Espresso.onIdle();

        // Click on "New Routine" in the list of routines
        onView(withText("Select Routine to Edit")).check(matches(isDisplayed()));
        onView(withText("New Routine")).perform(click());

        // Type in new routine name
        onView(withText("Edit Routine Name")).check(matches(isDisplayed()));
        onView(withClassName(is(EditText.class.getName()))).perform(typeText("Test"), closeSoftKeyboard());

        Espresso.onIdle();
        onView(withText("Save")).perform(click());

        // Check routine has been renamed
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(2)
                .onChildView(withId(R.id.routine_name))
                .check(matches(withText("Test")));

        // Start routine and check the name is still correct
        onData(Matchers.anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(2)
                .onChildView(withId(R.id.start_routine_button))
                .perform(click());
        onView(withId(R.id.routine_name_task))
                .check(matches(withText("Test")));

        // Restart the app and see if changes persist
        onView(withId(R.id.end_routine_button)).perform(click());

        activityRule.getScenario().close();
        ActivityScenario.launch(MainActivity.class, null);

        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(2)
                .onChildView(withId(R.id.routine_name))
                .check(matches(withText("Test")));
    }

    // This tests that you can't rename a routing to have a blank name
    @Test
    public void testRenamingBlankRoutine() {
        Log.d(TAG, "Running testRenamingBlankRoutine");

        // Wait a moment to ensure UI is fully loaded
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // Ignore
        }
        Espresso.onIdle();

        // Click edit routine button
        onView(withId(R.id.rename_routine_button)).perform(click());
        Espresso.onIdle();

        // Click on "New Routine" in the list of routines
        onView(withText("Select Routine to Edit")).check(matches(isDisplayed()));
        onView(withText("Morning")).perform(click());

        // Type in new routine name
        onView(withText("Edit Routine Name")).check(matches(isDisplayed()));
        onView(withClassName(is(EditText.class.getName()))).perform(typeText(""), closeSoftKeyboard());

        Espresso.onIdle();
        onView(withText("Save")).perform(click());

        // Check routine has not been renamed
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(0)
                .onChildView(withId(R.id.routine_name))
                .check(matches(withText("Morning")));
    }

}