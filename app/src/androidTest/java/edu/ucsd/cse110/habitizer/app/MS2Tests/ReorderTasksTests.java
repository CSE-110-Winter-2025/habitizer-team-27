package edu.ucsd.cse110.habitizer.app.MS2Tests;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.anything;
import static org.junit.Assert.assertEquals;

import android.util.Log;

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
import edu.ucsd.cse110.habitizer.lib.domain.Task;

public class ReorderTasksTests {
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

    // Tests reordering tasks
    @Test
    public void reorderTasks() {
        Log.d(TAG, "Running reorderTasks()");

//                new Task(0, "Shower", false),
//                new Task(1, "Brush teeth", false),
//                new Task(2, "Dress", false),
//                new Task(3, "Make coffee", false),
//                new Task(4, "Make lunch", false),
//                new Task(5, "Dinner prep", false),
//                new Task(6, "Pack bag", false)

        // Starts Morning routine
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(0)
                .onChildView(withId(R.id.start_routine_button))
                .perform(click());
        var currentRoutine = repository.getRoutines().getValue().get(0);

        // Press "move down" on "Dress"
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(2)
                .onChildView(withId(R.id.move_down_button))
                .perform(click());

        // Check items have swapped places ("Dress" and "Make coffee")
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(2)
                .onChildView(withId(R.id.task_name))
                .check(matches(withText("Make coffee")));
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(3)
                .onChildView(withId(R.id.task_name))
                .check(matches(withText("Dress")));

        // Check database has also changed
        var currentTasks = currentRoutine.getTasks();
        Log.d("Current Tasks", "Value: " + currentTasks.toString());
        assertEquals(currentTasks.get(2).getTaskName(), "Make coffee");
        assertEquals(currentTasks.get(3).getTaskName(), "Dress");
    }

    // Tests that moving the top task up doesn't change the order of tasks
    @Test
    public void reorderTopTask() {
        Log.d(TAG, "Running reorderTopTask()");

        // Starts Morning routine
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(0)
                .onChildView(withId(R.id.start_routine_button))
                .perform(click());

        // Clicks "move up" on first task ("Brush teeth")
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.move_up_button))
                .perform(click());

        // Verifies that nothing changed
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.task_name))
                .check(matches(withText("Shower")));

        var currentRoutine = repository.getRoutines().getValue().get(0);
        var currentTasks = currentRoutine.getTasks();

        assertEquals(currentTasks.get(0).getTaskName(), "Shower");
    }

    // Tests that moving the bottom task down doesn't change the order of tasks
    @Test
    public void reorderBottomTask() {
        Log.d(TAG, "Running reorderBottomTask()");

        // Starts Morning routine
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(0)
                .onChildView(withId(R.id.start_routine_button))
                .perform(click());
        var currentRoutine = repository.getRoutines().getValue().get(0);
        var currentTasks = currentRoutine.getTasks();
        int taskSize = currentTasks.size();

        // Clicks "move down" on last task ("Pack bag")
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(taskSize - 1)
                .onChildView(withId(R.id.move_down_button))
                .perform(click());

        // Verifies that nothing changed
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(taskSize - 1)
                .onChildView(withId(R.id.task_name))
                .check(matches(withText("Pack bag")));

        assertEquals(currentTasks.get(taskSize - 1).getTaskName(), "Pack bag");
    }
}
