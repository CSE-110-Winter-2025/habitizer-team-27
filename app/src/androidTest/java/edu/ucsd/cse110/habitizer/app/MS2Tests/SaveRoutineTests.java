package edu.ucsd.cse110.habitizer.app.MS2Tests;


import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

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
import edu.ucsd.cse110.habitizer.lib.domain.Routine;

//BDD Scenarios:
//Scenario #1: Remembering Edited Routines
//Given Belinda has a routine named “Morning” with tasks “Brush teeth,” “Dress,” and “Pack backpack,” in order,
//And Belinda adds a task “Make coffee” to the end of the routine
//And she renames the task “Brush teeth” to “Use mouthwash”
//And she deletes the task “Dress”
//When Belinda exits the app
//And restarts the app later
//Then the “Morning” routine has tasks “Use mouthwash,” “Pack backpack,” and “Make coffee,” in order
//
//Scenario #2: Exiting the App in a Routine
//Given Belinda is currently in a routine named “Morning” with tasks “Brush teeth,” “Dress,” and “Pack backpack”
//And the routine timer reads “30m”
//And the task timer reads “19m”
//And Belinda completed “Brush teeth” within “10m”
//When Belinda exits the app
//Then the task and routine timer both automatically pause
//When Belinda restarts the app
//Then the app reloads on the “Morning” routine screen
//And the routine time reads “30m”
//And the task time reads “19m”
//And the task “Brush teeth” is still checked off with “10m” next to it
//And the routine and task time start counting up time again
public class SaveRoutineTests {
    private static final String TAG = "SaveRoutineTest";
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
    public void editRoutineNameTest() {
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
        onView(withText("Evening")).perform(click());

        // Type in new routine name
        onView(withText("Edit Routine Name")).check(matches(isDisplayed()));
        onView(withClassName(is(EditText.class.getName()))).perform(typeText("Test"), closeSoftKeyboard());

        Espresso.onIdle();
        onView(withText("Save")).perform(click());

        // Check routine has been renamed
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(1)
                .onChildView(withId(R.id.routine_name))
                .check(matches(withText("Test")));

        // Start routine and check the name is still correct
        onData(Matchers.anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(1)
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
                .atPosition(1)
                .onChildView(withId(R.id.routine_name))
                .check(matches(withText("Test")));
    }


    @Test
    public void pauseRoutineTest() {
        Log.d(TAG, "Running pauseRoutineTest");

        // Wait a moment to ensure UI is fully loaded
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // Ignore
        }
        Espresso.onIdle();

        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(0)
                .onChildView(withId(R.id.start_routine_button))
                .perform(click());
        Espresso.onIdle();

        for(int i = 0; i < 38; i++){
            Espresso.onIdle();
            onView(withId(R.id.stop_timer_button)).perform(click());
            Espresso.onIdle();
        }

        onView(allOf(
                withId(R.id.check_task),
                hasSibling(withText("Brush teeth"))
        )).perform(click());
        Espresso.onIdle();

        for(int i = 0; i < 77; i++){
            Espresso.onIdle();
            onView(withId(R.id.stop_timer_button)).perform(click());
            Espresso.onIdle();
        }

        Espresso.onIdle();
        activityRule.getScenario().close();
        ActivityScenario.launch(MainActivity.class, null);
        Espresso.onIdle();

        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(0)
                .onChildView(withId(R.id.start_routine_button))
                .perform(click());
        Espresso.onIdle();


        onView(allOf(
                withId(R.id.actual_time),
                withText(containsString("29m"))
        )).check(matches(isDisplayed()));

        onView(allOf(
                withId(R.id.task_time),
                withText("10m"),
                hasSibling(withText("Brush teeth"))
        )).check(matches(isDisplayed()));
        Espresso.onIdle();


        onView(allOf(
                withId(R.id.current_task_elapsed_time),
                withText(containsString("19m"))
        )).check(matches(isDisplayed()));
    }

}

