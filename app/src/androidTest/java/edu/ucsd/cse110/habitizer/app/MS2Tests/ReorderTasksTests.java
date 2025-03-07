package edu.ucsd.cse110.habitizer.app.MS2Tests;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.anything;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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

        // Starts Morning routine
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(0)
                .onChildView(withId(R.id.start_routine_button))
                .perform(click());
        
        // Get initial tasks for reference
        Routine initialRoutine = repository.getRoutines().getValue().get(0);
        Log.d(TAG, "Initial tasks: " + initialRoutine.getTasks());
        for (int i = 0; i < initialRoutine.getTasks().size(); i++) {
            Log.d(TAG, "Initial position " + i + ": " + initialRoutine.getTasks().get(i).getTaskName());
        }

        // Add initial delay to stabilize UI
        try {
            Log.d(TAG, "Initial delay before reordering...");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted", e);
        }

        // Press "move down" on "Dress" (at position 2)
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(2)
                .onChildView(withId(R.id.move_down_button))
                .perform(click());
                
        // Add a significant delay to ensure repository operations complete
        try {
            Log.d(TAG, "Waiting for repository update to complete...");
            Thread.sleep(3000);  // Increased to 3 seconds for more reliable results
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted", e);
        }

        // Check UI has updated - items have swapped places
        Log.d(TAG, "Checking UI updates...");
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
                
        Log.d(TAG, "UI updates verified.");
        
        // Add additional delay before checking repository
        try {
            Log.d(TAG, "Waiting a bit more for repository...");
            Thread.sleep(2000);  // Another 2 seconds
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted", e);
        }
        
        Log.d(TAG, "Getting latest routine data from repository...");
        activityRule.getScenario().onActivity(activity -> {
            // Get repository reference directly from activity
            repository = HabitizerRepository.getInstance(activity);
        });
        
        // Get the LATEST repository data
        List<Routine> latestRoutines = repository.getRoutines().getValue();
        Log.d(TAG, "Latest routines size: " + (latestRoutines != null ? latestRoutines.size() : "null"));
        
        if (latestRoutines != null && !latestRoutines.isEmpty()) {
            Routine updatedRoutine = latestRoutines.get(0);
            List<Task> currentTasks = updatedRoutine.getTasks();
            
            Log.d(TAG, "Updated routine: " + updatedRoutine.getRoutineName() + " with " + currentTasks.size() + " tasks");
            for (int i = 0; i < currentTasks.size(); i++) {
                Task task = currentTasks.get(i);
                Log.d(TAG, "Position " + i + ": " + task.getTaskName() + " (ID: " + task.getTaskId() + ")");
            }
            
            // Verify positions 2 and 3 have the expected tasks
            if (currentTasks.size() > 3) {
                String pos2Name = currentTasks.get(2).getTaskName();
                String pos3Name = currentTasks.get(3).getTaskName();
                
                assertEquals("Expected 'Make coffee' at position 2 but found '" + pos2Name + "'", 
                            "Make coffee", pos2Name);
                assertEquals("Expected 'Dress' at position 3 but found '" + pos3Name + "'", 
                            "Dress", pos3Name);
            } else {
                fail("Not enough tasks in the list (need at least 4, found " + currentTasks.size() + ")");
            }
        } else {
            // If we can't get the latest data, fail the test
            Log.e(TAG, "Failed to get updated routine data from repository");
            fail("Could not retrieve updated routine data from repository");
        }
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
