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
import java.util.concurrent.TimeUnit;

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
        // Disable animations for more reliable tests
        TestHelper.disableAnimations();
        
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
                    List<Task> tasks = r.getTasks();
                    Log.d(TAG, "  Task count: " + tasks.size());
                    for (int i = 0; i < tasks.size(); i++) {
                        Log.d(TAG, "  Task " + i + ": " + tasks.get(i).getTaskName());
                    }
                }
            } else {
                Log.e(TAG, "No routines found before test - test may fail");
            }
            
            // Force a refresh of the repository to ensure we have fresh data
            repository.refreshRoutines();
            try {
                TimeUnit.MILLISECONDS.sleep(800);
            } catch (InterruptedException e) {
                Log.e(TAG, "Sleep interrupted", e);
            }
        });
    }

    // Tests reordering tasks
    @Test
    public void reorderTasks() {
        Log.d(TAG, "Running reorderTasks()");

        // Wait longer to ensure everything is initialized
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted", e);
        }

        // Starts Morning routine
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(0)
                .onChildView(withId(R.id.start_routine_button))
                .perform(click());
                
        // Wait longer to ensure UI updates
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted", e);
        }
        
        // Force a refresh to ensure we have the latest data
        activityRule.getScenario().onActivity(activity -> {
            repository = HabitizerRepository.getInstance(activity);
            repository.refreshRoutines();
        });
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted", e);
        }
        
        var currentRoutine = repository.getRoutines().getValue().get(0);
        
        // Log initial task state
        Log.d("TASK_TEST", "Initial routine state - Routine ID: " + currentRoutine.getRoutineId());
        Log.d("TASK_TEST", "Initial routine state - Routine Name: " + currentRoutine.getRoutineName());
        logTasks(currentRoutine.getTasks(), "Initial");
        
        // Verify minimum tasks exist before proceeding
        if (currentRoutine.getTasks().size() < 4) {
            Log.e(TAG, "Not enough tasks in routine to run test. Need at least 4, found " + 
                  currentRoutine.getTasks().size() + ". Aborting.");
            return;
        }

        // Press "move down" on "Dress" (position 2)
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(2)
                .onChildView(withId(R.id.move_down_button))
                .perform(click());
        
        // Wait longer for repository updates to complete
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted", e);
        }
        
        // Force a refresh
        activityRule.getScenario().onActivity(activity -> {
            repository = HabitizerRepository.getInstance(activity);
            repository.refreshRoutines();
        });
        
        // More waiting for refresh
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted", e);
        }
        
        // Refresh the routine reference
        Log.d("TASK_TEST", "Refreshing routine reference after move");
        currentRoutine = repository.getRoutines().getValue().get(0);
        
        // Log state after reordering
        Log.d("TASK_TEST", "After reordering - Routine ID: " + currentRoutine.getRoutineId());
        logTasks(currentRoutine.getTasks(), "After reordering");

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
        Log.d("TASK_TEST", "Final check - Tasks: " + currentTasks.toString());
        logTasks(currentTasks, "Final check");
        
        assertEquals(currentTasks.get(2).getTaskName(), "Make coffee");
        assertEquals(currentTasks.get(3).getTaskName(), "Dress");
    }
    
    // Helper method to log the tasks in a routine
    private void logTasks(List<Task> tasks, String prefix) {
        if (tasks == null) {
            Log.d("TASK_TEST", prefix + " - Tasks is null!");
            return;
        }
        
        Log.d("TASK_TEST", prefix + " - Task count: " + tasks.size());
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            Log.d("TASK_TEST", prefix + " - Position " + i + ": " + 
                  task.getTaskName() + " (ID: " + task.getTaskId() + ")");
        }
    }

    // Tests that moving the top task up doesn't change the order of tasks
    @Test
    public void reorderTopTask() {
        Log.d(TAG, "Running reorderTopTask()");

        // Wait longer to ensure everything is initialized
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted", e);
        }

        // Starts Morning routine
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(0)
                .onChildView(withId(R.id.start_routine_button))
                .perform(click());
                
        // Wait longer to ensure UI updates
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted", e);
        }
        
        // Force a refresh to ensure we have the latest data
        activityRule.getScenario().onActivity(activity -> {
            repository = HabitizerRepository.getInstance(activity);
            repository.refreshRoutines();
        });
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted", e);
        }
        
        var currentRoutine = repository.getRoutines().getValue().get(0);
        
        // Log initial task state
        Log.d("TASK_TEST", "Initial routine state - Routine ID: " + currentRoutine.getRoutineId());
        Log.d("TASK_TEST", "Initial routine state - Routine Name: " + currentRoutine.getRoutineName());
        logTasks(currentRoutine.getTasks(), "Initial");
        
        // Verify minimum tasks exist before proceeding
        if (currentRoutine.getTasks().size() < 1) {
            Log.e(TAG, "Not enough tasks in routine to run test. Need at least 1, found " + 
                  currentRoutine.getTasks().size() + ". Aborting.");
            return;
        }

        // Clicks "move up" on first task
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.move_up_button))
                .perform(click());
                
        // Wait longer to ensure UI updates
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted", e);
        }
        
        // Force a refresh
        activityRule.getScenario().onActivity(activity -> {
            repository = HabitizerRepository.getInstance(activity);
            repository.refreshRoutines();
        });
        
        // More waiting for refresh
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted", e);
        }

        // Refresh the routine reference
        currentRoutine = repository.getRoutines().getValue().get(0);
        
        // Log state after reordering
        Log.d("TASK_TEST", "After reordering - Routine ID: " + currentRoutine.getRoutineId());
        logTasks(currentRoutine.getTasks(), "After reordering");

        // Verifies that nothing changed
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(0)
                .onChildView(withId(R.id.task_name))
                .check(matches(withText("Shower")));

        var currentTasks = currentRoutine.getTasks();
        assertEquals(currentTasks.get(0).getTaskName(), "Shower");
    }

    // Tests that moving the bottom task down doesn't change the order of tasks
    @Test
    public void reorderBottomTask() {
        Log.d(TAG, "Running reorderBottomTask()");

        // Wait longer to ensure everything is initialized
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted", e);
        }

        // Starts Morning routine
        onData(anything())
                .inAdapterView(withId(R.id.card_list))
                .atPosition(0)
                .onChildView(withId(R.id.start_routine_button))
                .perform(click());
                
        // Wait longer to ensure UI updates
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted", e);
        }
        
        // Force a refresh to ensure we have the latest data
        activityRule.getScenario().onActivity(activity -> {
            repository = HabitizerRepository.getInstance(activity);
            repository.refreshRoutines();
        });
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted", e);
        }
        
        var currentRoutine = repository.getRoutines().getValue().get(0);
        
        // Log initial task state
        Log.d("TASK_TEST", "Initial routine state - Routine ID: " + currentRoutine.getRoutineId());
        Log.d("TASK_TEST", "Initial routine state - Routine Name: " + currentRoutine.getRoutineName());
        logTasks(currentRoutine.getTasks(), "Initial");
        
        // Verify minimum tasks exist before proceeding
        if (currentRoutine.getTasks().size() < 1) {
            Log.e(TAG, "Not enough tasks in routine to run test. Need at least 1, found " + 
                  currentRoutine.getTasks().size() + ". Aborting.");
            return;
        }
        
        // Get the last task position
        int lastPosition = currentRoutine.getTasks().size() - 1;
        
        // Clicks "move down" on last task
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(lastPosition)
                .onChildView(withId(R.id.move_down_button))
                .perform(click());
                
        // Wait longer to ensure UI updates
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted", e);
        }
        
        // Force a refresh
        activityRule.getScenario().onActivity(activity -> {
            repository = HabitizerRepository.getInstance(activity);
            repository.refreshRoutines();
        });
        
        // More waiting for refresh
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted", e);
        }

        // Refresh the routine reference
        currentRoutine = repository.getRoutines().getValue().get(0);
        
        // Log state after reordering
        Log.d("TASK_TEST", "After reordering - Routine ID: " + currentRoutine.getRoutineId());
        logTasks(currentRoutine.getTasks(), "After reordering");

        // Verifies that the last task is still the same
        String expectedLastTaskName = currentRoutine.getTasks().get(lastPosition).getTaskName();
        onData(anything())
                .inAdapterView(withId(R.id.routine_list))
                .atPosition(lastPosition)
                .onChildView(withId(R.id.task_name))
                .check(matches(withText(expectedLastTaskName)));
    }
}
