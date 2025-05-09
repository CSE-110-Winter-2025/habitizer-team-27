package edu.ucsd.cse110.habitizer.app;

import android.content.Context;
import android.util.Log;
import android.view.animation.Animation;

import androidx.test.platform.app.InstrumentationRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import edu.ucsd.cse110.habitizer.app.data.db.AppDatabase;
import edu.ucsd.cse110.habitizer.app.data.db.HabitizerRepository;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;

/**
 * Helper class for tests
 */
public class TestHelper {
    private static final String TAG = "TestHelper";
    
    // Default morning tasks for tests
    // Default data for initialization
    private static final List<Task> DEFAULT_MORNING_TASKS = List.of(
            new Task(0, "Shower", false),
            new Task(1, "Brush teeth", false),
            new Task(2, "Dress", false),
            new Task(3, "Make coffee", false),
            new Task(4, "Make lunch", false),
            new Task(5, "Dinner prep", false),
            new Task(6, "Pack bag", false)
    );

    private static final List<Task> DEFAULT_EVENING_TASKS = List.of(
            new Task(100, "Charge devices", false),
            new Task(101, "Prepare dinner", false),
            new Task(102, "Eat dinner", false),
            new Task(103, "Wash dishes", false),
            new Task(104, "Pack bag", false),
            new Task(105, "Homework", false)
    );
    
    /**
     * Initialize the database with default routines for testing
     * This is a blocking call that ensures the database is ready with test data
     */
    public static void initializeTestDatabase() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // Force complete reset of database and repository
        HabitizerRepository.resetInstance();
        AppDatabase.resetInstance();
        
        // Get a fresh repository instance
        HabitizerRepository repository = HabitizerRepository.getInstance(context);
        
        // Create a latch to ensure initialization completes
        CountDownLatch latch = new CountDownLatch(1);
        
        // Create and add test routines directly using the repository
        Thread initThread = new Thread(() -> {
            try {
                Log.d(TAG, "Starting test database initialization");
                
                // Create morning routine with tasks
                Routine morningRoutine = new Routine(0, "Morning");
                for (Task task : DEFAULT_MORNING_TASKS) {
                    // Add task to database first
                    repository.addTask(task);
                    // Add task to routine
                    morningRoutine.addTask(task);
                }
                
                // Add morning routine to database
                Log.d(TAG, "Adding morning routine to test database");
                repository.addRoutine(morningRoutine);
                
                // Small delay to ensure first routine is saved
                Thread.sleep(500);
                
                // Create evening routine with tasks
                Routine eveningRoutine = new Routine(1, "Evening");
                for (Task task : DEFAULT_EVENING_TASKS) {
                    // Add task to database first
                    repository.addTask(task);
                    // Add task to routine
                    eveningRoutine.addTask(task);
                }
                
                // Add evening routine to database
                Log.d(TAG, "Adding evening routine to test database");
                repository.addRoutine(eveningRoutine);
                
                // Small delay to ensure second routine is saved
                Thread.sleep(500);
                
                // Verify routines were added
                List<Routine> routines = repository.getRoutines().getValue();
                Log.d(TAG, "Verification: " + (routines != null ? routines.size() : 0) + " routines in test database");
                if (routines != null) {
                    for (Routine r : routines) {
                        Log.d(TAG, "Routine: " + r.getRoutineName() + " (ID: " + r.getRoutineId() + ") with " + 
                              r.getTasks().size() + " tasks");
                    }
                }
                
                // Signal initialization is complete
                latch.countDown();
                Log.d(TAG, "Test database initialization complete");
                
            } catch (Exception e) {
                Log.e(TAG, "Error initializing test database", e);
                // Still count down in case of error to avoid blocking tests
                latch.countDown();
            }
        });
        
        // Start initialization thread
        initThread.start();
        
        try {
            // Wait up to 10 seconds for initialization to complete
            boolean initialized = latch.await(10, TimeUnit.SECONDS);
            if (!initialized) {
                Log.e(TAG, "Timed out waiting for test database initialization");
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting for test database initialization", e);
        }
        
        // Double-check and log initialization result
        List<Routine> finalRoutines = repository.getRoutines().getValue();
        if (finalRoutines != null) {
            Log.d(TAG, "Final verification: " + finalRoutines.size() + " routines in test database");
            boolean hasMorning = false;
            boolean hasEvening = false;
            
            for (Routine r : finalRoutines) {
                if ("Morning".equals(r.getRoutineName())) hasMorning = true;
                if ("Evening".equals(r.getRoutineName())) hasEvening = true;
                Log.d(TAG, "Final routine: " + r.getRoutineName() + " (ID: " + r.getRoutineId() + ")");
            }
            
            if (!hasMorning || !hasEvening) {
                Log.e(TAG, "WARNING: Missing default routines after initialization");
            }
        } else {
            Log.e(TAG, "ERROR: No routines after test initialization");
        }
    }
    
    /**
     * Create a list of demo tasks
     * @return List of tasks for testing
     */
    public static List<Task> createDemoTasks() {
        List<Task> tasks = new ArrayList<>();
        tasks.add(new Task(1, "Brush teeth", false));
        tasks.add(new Task(2, "Shower", false));
        tasks.add(new Task(3, "Get dressed", false));
        tasks.add(new Task(4, "Breakfast", false));
        tasks.add(new Task(5, "Pack bag", false));
        return tasks;
    }
    
    /**
     * Disable animations on the device to make Espresso tests more reliable
     */
    public static void disableAnimations() {
        try {
            // Try using UiAutomation to disable animations
            int value = 0; // Animation scale (0 = disabled)
            InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("settings put global window_animation_scale " + value);
            InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("settings put global transition_animation_scale " + value);
            InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("settings put global animator_duration_scale " + value);
            
            Log.d(TAG, "Disabled animations for testing");
        } catch (Exception e) {
            Log.e(TAG, "Failed to disable animations", e);
        }
    }
} 