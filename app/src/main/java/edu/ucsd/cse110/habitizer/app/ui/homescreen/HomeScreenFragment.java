package edu.ucsd.cse110.habitizer.app.ui.homescreen;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import edu.ucsd.cse110.habitizer.app.MainActivity;
import edu.ucsd.cse110.habitizer.app.R;
import edu.ucsd.cse110.habitizer.app.ui.dialog.CreateRoutineDialogFragment;
import edu.ucsd.cse110.habitizer.app.ui.routine.RoutineFragment;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.app.MainViewModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import edu.ucsd.cse110.observables.Observer;


public class HomeScreenFragment extends Fragment {
    private static final String TAG = "HomeScreenFragment";
    private final List<Routine> routines = new ArrayList<>();
    private MainViewModel activityModel;
    private Observer<List<Routine>> routineObserver;
    private boolean isFirstLoad = true;
    private HomeAdapter adapter;
    private static final int REFRESH_DELAY = 1000; // 1 second
    private static final int MAX_REFRESH_ATTEMPTS = 3; // Maximum number of refresh attempts
    private int refreshAttempts = 0; // Counter for refresh attempts
    private Handler refreshHandler = new Handler(Looper.getMainLooper());

    public HomeScreenFragment() {
        // Required empty public constructor
    }

    public static HomeScreenFragment newInstance() {
        return new HomeScreenFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize ViewModel
        var modelOwner = requireActivity();
        var modelFactory = ViewModelProvider.Factory.from(MainViewModel.initializer);
        var modelProvider = new ViewModelProvider(modelOwner, modelFactory);
        this.activityModel = modelProvider.get(MainViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called - initializing HomeScreenFragment UI");
        View view = inflater.inflate(R.layout.fragment_home_page, container, false);
        
        ListView listView = view.findViewById(R.id.card_list);
        adapter = new HomeAdapter(
                requireContext(),
                routines,
                routineId -> {
                    // Get the routine from the repository
                    Routine routine = activityModel.getRoutineRepository().getRoutine(routineId);
        
                    // Special handling for Morning routine with ID 0 to prevent duplication
                    boolean isMorningRoutineWithIdZero = routine != null && 
                                                       "Morning".equals(routine.getRoutineName()) && 
                                                       routine.getRoutineId() == 0;
                    
                    Log.d(TAG, "Routine click: " + (routine != null ? routine.getRoutineName() : "null") + 
                          " with ID: " + routineId + ", is Morning with ID 0: " + isMorningRoutineWithIdZero);
                    
                    // Only start the routine timer if it has tasks
                    // We won't even call startRoutine for empty routines
                    if (routine != null && !routine.getTasks().isEmpty()) {
                        Log.d(TAG, "Starting routine with tasks: " + routine.getRoutineName());
                        routine.startRoutine(LocalDateTime.now());
                        
                        // For Morning routine with ID 0, update it locally without calling save
                        // This prevents repository from creating a duplicate
                        if (!isMorningRoutineWithIdZero) {
                            activityModel.getRoutineRepository().save(routine);
                            Log.d(TAG, "Saved routine state to repository");
                        } else {
                            Log.d(TAG, "Morning routine with ID 0 - not saving to prevent duplication");
                        }
                    } else if (routine != null) {
                        Log.d(TAG, "Not starting empty routine: " + routine.getRoutineName());
                    }
                    
                    // Navigate to the routine screen regardless
                    navigateToRoutine(routineId);
                }
        );
        listView.setAdapter(adapter);
        Log.d(TAG, "ListView adapter set with " + routines.size() + " initial routines");
        
        // Set up the Add Routine button
        Button addRoutineButton = view.findViewById(R.id.add_routine_button);
        addRoutineButton.setOnClickListener(v -> {
            createNewRoutine("New Routine");
        });

        // Remove previous observer if it exists when fragment is recreated
        if (routineObserver != null) {
            activityModel.getRoutineRepository().findAll().removeObserver(routineObserver);
            Log.d(TAG, "Removed previous observer");
        }

        // Create and register a new observer
        routineObserver = routines -> {
            Log.d(TAG, "Observer triggered with " + (routines != null ? routines.size() : 0) + " routines");
                  
            // Log existing routines in the list
            Log.d(TAG, "Current routines in list before update: " + this.routines.size());
            for (int i = 0; i < this.routines.size(); i++) {
                Routine r = this.routines.get(i);
                Log.d(TAG, "  " + i + ": " + r.getRoutineId() + " - " + r.getRoutineName());
            }
            
            // Check if routines is not null
            if (routines != null) {
                // Log incoming routines
                Log.d(TAG, "Incoming routines:");
                for (int i = 0; i < routines.size(); i++) {
                    Routine r = routines.get(i);
                    Log.d(TAG, "  " + i + ": " + r.getRoutineId() + " - " + r.getRoutineName() + 
                          " with " + r.getTasks().size() + " tasks");
                }
                
                // Clear the list first before adding new routines
                this.routines.clear();
                
                // Add all routines including duplicates - no filtering
                this.routines.addAll(routines);
                
                Log.d(TAG, "Added all " + this.routines.size() + " routines including duplicates");
                
                // Force UI update on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    // Update the adapter
                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "IMPORTANT: Adapter notified of data change with " + this.routines.size() + " routines");
                });
            } else {
                Log.e(TAG, "CRITICAL ERROR: Received null routine list from repository!");
                // Force a refresh from MainActivity
                if (getActivity() instanceof MainActivity) {
                    Log.d(TAG, "Requesting force refresh from MainActivity");
                    ((MainActivity) getActivity()).forceRefreshRoutinesPublic();
                }
            }
            
            // Log final state
            Log.d(TAG, "Updated routines list now has " + this.routines.size() + " routines");
            
            // If this is the first load and we don't have both default routines (Morning and Evening), schedule a refresh
            if (isFirstLoad && (this.routines.size() < 2 || !hasDefaultRoutines())) {
                Log.d(TAG, "First load detected with " + this.routines.size() + " routines, but missing default routines. Scheduling refresh...");
                scheduleRefresh();
            } else {
                refreshAttempts = 0; // Reset refresh attempts when we have all routines
            }
            
            isFirstLoad = false;
        };
        
        // Observe changes to the list of routines
        activityModel.getRoutineRepository().findAll().observe(routineObserver);
        Log.d(TAG, "Registered new observer");
        
        return view;
    }
    
    private void createNewRoutine(String routineName) {
        // Create a new routine with the given name
        Log.d(TAG, "Creating new routine with name: " + routineName);
        
        // Generate a unique ID for the new routine
        int newRoutineId = generateUniqueRoutineId();
        
        // Create the routine object
        Routine newRoutine = new Routine(newRoutineId, routineName);
        
        // Add the routine to the repository
        activityModel.getRoutineRepository().save(newRoutine);
        
        Log.d(TAG, "New routine created with ID: " + newRoutineId);
    }
    
    private int generateUniqueRoutineId() {
        // Find the highest routine ID and add 1
        int maxId = 0;
        for (Routine routine : routines) {
            if (routine.getRoutineId() > maxId) {
                maxId = routine.getRoutineId();
            }
        }
        return maxId + 1;
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove observer when fragment is destroyed
        if (routineObserver != null) {
            activityModel.getRoutineRepository().findAll().removeObserver(routineObserver);
            Log.d(TAG, "Removed observer in onDestroyView");
        }
    }

    private void navigateToRoutine(int routineId) {
        Log.d(TAG, "Navigating to routine with ID: " + routineId);
        Routine routine = activityModel.getRoutineRepository().getRoutine(routineId);
        Log.d(TAG, "Routine found: " + (routine != null ? routine.getRoutineName() : "null") + 
              " with " + (routine != null ? routine.getTasks().size() : 0) + " tasks");

        // Store the current routine ID in a tag to prevent duplication when coming back
        if (routine != null && "Morning".equals(routine.getRoutineName())) {
            // Store this ID to prevent duplication
            if (getActivity() != null) {
                getActivity().getIntent().putExtra("LAST_MORNING_ROUTINE_ID", routineId);
                Log.d(TAG, "Saved Morning routine ID: " + routineId + " to prevent duplication");
            }
        }

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, RoutineFragment.newInstance(routineId));
        transaction.addToBackStack(null);
        transaction.commit();
        
        Log.d(TAG, "FragmentTransaction committed");
    }

    /**
     * Schedule a refresh of the data after a delay
     */
    private void scheduleRefresh() {
        if (refreshAttempts < MAX_REFRESH_ATTEMPTS) {
            refreshAttempts++;
            Log.d(TAG, "Scheduling refresh attempt " + refreshAttempts + "/" + MAX_REFRESH_ATTEMPTS);
            refreshHandler.postDelayed(() -> {
                Log.d(TAG, "Refreshing routines list (attempt " + refreshAttempts + ")");
                // Force a refresh of the data by re-observing
                if (routineObserver != null) {
                    activityModel.getRoutineRepository().findAll().removeObserver(routineObserver);
                    activityModel.getRoutineRepository().findAll().observe(routineObserver);
                }
            }, REFRESH_DELAY * refreshAttempts); // Increase delay based on attempt number
        } else {
            Log.d(TAG, "Maximum refresh attempts reached. Giving up.");
        }
    }
    
    /**
     * Check if both default routines (Morning and Evening) are present
     */
    private boolean hasDefaultRoutines() {
        boolean hasMorning = false;
        boolean hasEvening = false;
        
        for (Routine routine : routines) {
            String name = routine.getRoutineName();
            if ("Morning".equals(name)) {
                hasMorning = true;
            } else if ("Evening".equals(name)) {
                hasEvening = true;
            }
            
            if (hasMorning && hasEvening) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        // If we only have one routine, try refreshing the data
        if (routines.size() < 2) {
            Log.d(TAG, "onResume: Only " + routines.size() + " routines found. Refreshing data...");
            scheduleRefresh();
        }
    }
}
