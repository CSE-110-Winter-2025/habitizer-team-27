package edu.ucsd.cse110.habitizer.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import edu.ucsd.cse110.habitizer.app.data.db.HabitizerRepository;
import edu.ucsd.cse110.habitizer.app.databinding.ActivityMainBinding;
import edu.ucsd.cse110.habitizer.app.ui.homescreen.HomeScreenFragment;
import edu.ucsd.cse110.habitizer.app.ui.routine.RoutineFragment;
import edu.ucsd.cse110.habitizer.app.util.RoutineStateManager;
import edu.ucsd.cse110.habitizer.lib.data.InMemoryDataSource;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.RoutineRepository;
import edu.ucsd.cse110.habitizer.lib.domain.Task;
import edu.ucsd.cse110.habitizer.lib.domain.TaskRepository;
import edu.ucsd.cse110.habitizer.app.HabitizerApplication;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding view;
    private boolean isShowingRoutine = false;
    private HabitizerRepository repository;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private RoutineStateManager routineStateManager;
    
    // Static flag to track if app is in foreground
    public static boolean isAppInForeground = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called - initializing main activity");
        setTitle(R.string.app_name);

        this.view = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(view.getRoot());
        
        // Get repository instance
        repository = HabitizerRepository.getInstance(this);
        
        // Initialize routine state manager
        routineStateManager = new RoutineStateManager(this);
        
        // Only show home screen if this is a fresh launch (not a configuration change or restart)
        if (savedInstanceState == null) {
            Log.d(TAG, "Fresh launch - checking for active routines");
            
            // Check if we have an active routine to restore
            checkForActiveRoutine();
        } else {
            Log.d(TAG, "App restarted with saved state - preserving current fragment");
            // Check what fragment is currently being displayed
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment instanceof RoutineFragment) {
                Log.d(TAG, "Restored to RoutineFragment - setting isShowingRoutine to true");
                isShowingRoutine = true;
            } else {
                Log.d(TAG, "Restored to HomeScreenFragment or other - setting isShowingRoutine to false");
                isShowingRoutine = false;
            }
        }
        
        // Verify routines are loaded
        verifyRoutinesLoaded();
    }
    
    /**
     * Check for active routines and navigate to them if found
     */
    private void checkForActiveRoutine() {
        // Check if we have an active routine saved in preferences
        if (routineStateManager.hasRunningRoutine()) {
            int routineId = routineStateManager.getRunningRoutineId();
            Log.d(TAG, "Found active routine with ID: " + routineId);
            
            // Navigate to active routine
            if (routineId >= 0) {
                // Delay slightly to ensure repository is initialized
                handler.postDelayed(() -> {
                    Log.d(TAG, "Navigating to active routine: " + routineId);
                    // Specify this is NOT from home screen (should restore state)
                    navigateToRoutine(routineId, false);
                }, 500);
                return;
            }
        }
        
        // Default to showing home screen if no active routine
        Log.d(TAG, "No active routine found - showing home screen");
        showHomeScreen();
    }
    
    /**
     * Navigate to a specific routine by ID
     * @param routineId The ID of the routine to navigate to
     * @param fromHomeScreen Whether this navigation is from the home screen (start fresh) or from auto-restore
     */
    public void navigateToRoutine(int routineId, boolean fromHomeScreen) {
        Log.d(TAG, "Navigating to routine: " + routineId + (fromHomeScreen ? " from home screen" : " from auto-restore"));
        
        // Set the isShowingRoutine flag to true
        isShowingRoutine = true;
        
        // If coming from home screen, clear any existing saved state for this routine
        if (fromHomeScreen && routineStateManager != null) {
            // Check if there's a saved state for this specific routine
            if (routineStateManager.hasRunningRoutine() && routineStateManager.getRunningRoutineId() == routineId) {
                Log.d(TAG, "Clearing saved state for routine " + routineId + " when starting fresh from home screen");
                routineStateManager.clearRunningRoutineState();
            }
        }
        
        // Create fragment instance with arguments
        RoutineFragment routineFragment = RoutineFragment.newInstance(routineId);
        
        // Use replace to avoid fragment stack issues
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, routineFragment)
                .commit();
        
        Log.d(TAG, "Navigated to routine: " + routineId + 
              (fromHomeScreen ? " - routine will start fresh" : " - routine will be automatically paused"));
    }
    
    /**
     * Navigate to a specific routine by ID (backwards compatibility)
     * @param routineId The ID of the routine to navigate to
     */
    public void navigateToRoutine(int routineId) {
        // Default to assuming this is not from home screen (preserving old behavior)
        navigateToRoutine(routineId, false);
    }
    
    /**
     * Verify routines are loaded and show home screen
     */
    private void verifyRoutinesLoaded() {
        Log.d(TAG, "Starting verification of loaded routines");
        
        // Check if routines are loaded
        List<Routine> routines = repository.getRoutines().getValue();
        if (routines != null) {
            Log.d(TAG, "Found " + routines.size() + " routines initially");
            for (Routine r : routines) {
                Log.d(TAG, "  Routine: " + r.getRoutineName() + " (ID: " + r.getRoutineId() + ")");
            }

            // Check if both Morning and Evening routines are present
            boolean hasMorning = false;
            boolean hasEvening = false;
            for (Routine r : routines) {
                if ("Morning".equals(r.getRoutineName())) hasMorning = true;
                if ("Evening".equals(r.getRoutineName())) hasEvening = true;
            }

            if (hasMorning && hasEvening) {
                Log.d(TAG, "Both Morning and Evening routines found. Refreshing home screen.");
                // Refresh home screen in case routines were loaded after initial display
                if (!isShowingRoutine) {
                    // Delay slightly to ensure UI is ready
                    handler.postDelayed(this::refreshHomeScreen, 300);
                }
            } else if (routines.size() > 0) {
                // At least we found some routines - show them while we wait for others
                Log.d(TAG, "Found " + routines.size() + " routines but not both defaults. Showing what we have.");
                if (!isShowingRoutine) {
                    handler.postDelayed(this::refreshHomeScreen, 300);
                }
                // And keep checking
                Log.d(TAG, "Will check again for all default routines shortly");
                handler.postDelayed(this::verifyRoutinesLoaded, 1000);
            } else {
                Log.d(TAG, "No routines found, will check again shortly");
                // Schedule check in a second
                forceRefreshRoutines();
                handler.postDelayed(this::verifyRoutinesLoaded, 1500);
            }
        } else {
            Log.d(TAG, "No routines list available yet (null value), will check again shortly");
            // Try to force a refresh
            forceRefreshRoutines();
            // Schedule check in a second
            handler.postDelayed(this::verifyRoutinesLoaded, 1500);
        }
    }
    
    /**
     * Show the home screen in the fragment container
     */
    public void showHomeScreen() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, HomeScreenFragment.newInstance())
                .commit();
        isShowingRoutine = false;
        Log.d(TAG, "Navigated to home screen from routine");
    }
    
    /**
     * Refresh the home screen by recreating the fragment
     */
    private void refreshHomeScreen() {
        if (!isShowingRoutine) {
            Log.d(TAG, "Refreshing home screen to ensure routines are displayed");
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, HomeScreenFragment.newInstance())
                    .commit();
        }
    }

    private void swapFragments() {
        if (isShowingRoutine) {
            int defaultRoutineId = 1;
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, RoutineFragment.newInstance(defaultRoutineId))
                    .commit();
        } else {
            showHomeScreen();
        }
        isShowingRoutine = !isShowingRoutine;
    }

    /**
     * Force refresh routines from the repository, useful when the app starts
     */
    private void forceRefreshRoutines() {
        try {
            Log.d(TAG, "Force refreshing routines from repository");
            repository.refreshRoutines();
            
            // Check if default routines were already created at least once
            SharedPreferences prefs = getSharedPreferences("habitizer_prefs", MODE_PRIVATE);
            boolean defaultRoutinesCreated = prefs.getBoolean("default_routines_created", false);
            
            // If we've already created default routines once, don't try to recreate them
            if (defaultRoutinesCreated) {
                Log.d(TAG, "Default routines were previously created - not attempting to recreate");
                return;
            }
            
            // Only for first launch: Check if we need to create default routines
            if (repository.getRoutines() == null || 
                repository.getRoutines().getValue() == null || 
                repository.getRoutines().getValue().isEmpty()) {
                Log.d(TAG, "No routines found during force refresh, triggering default routine creation");
                
                // Create default routines directly if none exist
                handler.postDelayed(() -> {
                    Log.d(TAG, "Adding default routines as emergency measure");
                    addDefaultRoutines();
                }, 800);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during force refresh", e);
        }
    }

    /**
     * Emergency method to add default routines directly if repository initialization failed
     */
    private void addDefaultRoutines() {
        try {
            // Check if we've already created default routines before
            SharedPreferences prefs = getSharedPreferences("habitizer_prefs", MODE_PRIVATE);
            boolean defaultRoutinesCreated = prefs.getBoolean("default_routines_created", false);
            
            if (defaultRoutinesCreated) {
                Log.d(TAG, "Default routines were already created during a previous launch - skipping emergency creation");
                return;
            }
            
            List<Routine> existingRoutines = repository.getRoutines().getValue();
            if (existingRoutines == null || existingRoutines.isEmpty()) {
                Log.d(TAG, "No routines found, adding emergency default routines");
                
                // Create Morning routine
                Routine morningRoutine = new Routine(0, "Morning");
                morningRoutine.addTask(new Task(0, "Shower", false));
                morningRoutine.addTask(new Task(1, "Brush teeth", false));
                morningRoutine.addTask(new Task(2, "Dress", false));
                morningRoutine.addTask(new Task(3, "Make coffee", false));
                morningRoutine.addTask(new Task(4, "Make lunch", false));
                repository.addRoutine(morningRoutine);
                
                // Create Evening routine
                Routine eveningRoutine = new Routine(1, "Evening");
                eveningRoutine.addTask(new Task(100, "Charge devices", false));
                eveningRoutine.addTask(new Task(101, "Prepare dinner", false));
                eveningRoutine.addTask(new Task(102, "Eat dinner", false));
                eveningRoutine.addTask(new Task(103, "Wash dishes", false));
                repository.addRoutine(eveningRoutine);
                
                // Mark that we've created default routines
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("default_routines_created", true);
                editor.apply();
                Log.d(TAG, "Successfully created emergency default routines and marked as complete");
                
                // Refresh home screen after a delay
                handler.postDelayed(this::refreshHomeScreen, 500);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding default routines", e);
        }
    }

    /**
     * Public method to allow fragments to force routine refresh
     */
    public void forceRefreshRoutinesPublic() {
        Log.d(TAG, "Public force refresh called from fragment");
        forceRefreshRoutines();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MainActivity.isAppInForeground = false;
        Log.d(TAG, "App entered background state");
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainActivity.isAppInForeground = true;
        Log.d(TAG, "App returned to foreground state");
    }

    public boolean isShowingRoutine() {
        return isShowingRoutine;
    }
    
    public void setShowingRoutine(boolean isShowingRoutine) {
        this.isShowingRoutine = isShowingRoutine;
        Log.d(TAG, "isShowingRoutine set to: " + isShowingRoutine);
    }
}