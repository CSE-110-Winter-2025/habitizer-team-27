package edu.ucsd.cse110.habitizer.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import edu.ucsd.cse110.habitizer.app.databinding.ActivityMainBinding;
import edu.ucsd.cse110.habitizer.app.ui.homescreen.HomeScreenFragment;
import edu.ucsd.cse110.habitizer.app.ui.routine.RoutineFragment;
import edu.ucsd.cse110.habitizer.lib.data.InMemoryDataSource;
import edu.ucsd.cse110.habitizer.lib.domain.RoutineRepository;
import edu.ucsd.cse110.habitizer.lib.domain.TaskRepository;

import java.util.List;
import edu.ucsd.cse110.habitizer.app.data.db.HabitizerRepository;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding view;
    private boolean isShowingRoutine = false;
    private HabitizerRepository repository;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called - initializing main activity");
        setTitle(R.string.app_name);

        this.view = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(view.getRoot());
        
        // Get repository instance
        repository = HabitizerRepository.getInstance(this);
        
        // Show home screen initially
        showHomeScreen();
        
        // Verify routines are loaded
        verifyRoutinesLoaded();
    }
    
    /**
     * Verify routines are loaded and show home screen
     */
    private void verifyRoutinesLoaded() {
        // Check if routines are loaded
        List<Routine> routines = repository.getRoutines().getValue();
        if (routines != null) {
            Log.d(TAG, "Found " + routines.size() + " routines initially");
            for (Routine r : routines) {
                Log.d(TAG, "  Routine: " + r.getRoutineName() + " (ID: " + r.getRoutineId() + ")");
            }
            
            // Refresh home screen in case routines were loaded after initial display
            if (!isShowingRoutine && routines.size() > 0) {
                // Delay slightly to ensure UI is ready
                handler.postDelayed(this::refreshHomeScreen, 300);
            }
        } else {
            Log.d(TAG, "No routines found initially, will check again shortly");
            // Schedule check in a second
            handler.postDelayed(this::verifyRoutinesLoaded, 1000);
        }
    }
    
    /**
     * Show home screen
     */
    private void showHomeScreen() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, HomeScreenFragment.newInstance())
                .commit();
        isShowingRoutine = false;
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
}