package edu.ucsd.cse110.habitizer.app.ui.routine;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import edu.ucsd.cse110.habitizer.app.HabitizerApplication;
import edu.ucsd.cse110.habitizer.app.MainActivity;
import edu.ucsd.cse110.habitizer.app.MainViewModel;
import edu.ucsd.cse110.habitizer.app.R;
import edu.ucsd.cse110.habitizer.app.data.LegacyLogicAdapter;
import edu.ucsd.cse110.habitizer.app.data.db.HabitizerRepository;
import edu.ucsd.cse110.habitizer.app.databinding.FragmentRoutineScreenBinding;
import edu.ucsd.cse110.habitizer.app.ui.dialog.CreateTaskDialogFragment;
import edu.ucsd.cse110.habitizer.app.ui.dialog.SetRoutineTimeDialogFragment;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;
import edu.ucsd.cse110.habitizer.app.util.RoutineStateManager;

public class RoutineFragment extends Fragment {
    private MainViewModel activityModel;
    private FragmentRoutineScreenBinding binding;
    private TaskAdapter taskAdapter;
    private HabitizerRepository repository;

    private static final String ARG_ROUTINE_ID = "routine_id";
    private Routine currentRoutine;

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private boolean isTimerRunning = true;
    private static final int UPDATE_INTERVAL_MS = 1000;

    // Add a flag to prevent recursive updates
    private boolean isUpdatingFromObserver = false;

    private boolean manuallyStarted = false;
    
    // Add variables to track timer state when app is minimized
    private boolean wasTimerRunningBeforeMinimize = false;
    private LocalDateTime timeWhenMinimized = null;

    private boolean isPaused = false; // Add this flag to track pause state
    private boolean isStopTimerPressed = false; // Add this flag to track stop timer state
    
    // Add variable to store time before pause
    private long timeBeforePauseMinutes = 0;

    // Add a new field to track the current task's elapsed time before pause
    private long taskTimeBeforePauseMinutes = 0;
    private int taskSecondsBeforePause = 0;

    private RoutineStateManager routineStateManager;
    private Runnable stateSaveRunnable;
    private static final int STATE_SAVE_INTERVAL_MS = 1000; // Save state every second

    public RoutineFragment() {
        // required empty public constructor
    }

    private void initTimerUpdates() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                // Only update if app is in foreground and not in mock mode
                if (edu.ucsd.cse110.habitizer.app.MainActivity.isAppInForeground && !isStopTimerPressed && !isPaused) {
                    // Update both the routine time display and task elapsed time
                    updateTimeDisplay();
                    updateCurrentTaskElapsedTime(); // Explicitly call this for each update
                    
                    // Log time updates for debugging
                    Log.d("TimerUpdate", "Timer update: routine active=" + 
                         (currentRoutine != null ? currentRoutine.isActive() : "null routine") + 
                         ", isTimerRunning=" + isTimerRunning +
                         ", isPaused=" + isPaused);
                } else if (isStopTimerPressed) {
                    // In mock mode, we only want UI updates, not timer updates
                    if (!isPaused) {
                        // Only update in mock mode if not paused
                        updateTimeDisplay();
                        updateCurrentTaskElapsedTime();
                    }
                    Log.d("TimerUpdate", "In mock mode: isPaused=" + isPaused);
                } else if (isPaused) {
                    // When paused, we just want to keep the UI showing the paused values
                    Log.d("TimerUpdate", "Timer is paused - not updating time");
                }

                timerHandler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        };
        
        // Initialize state saving runnable to save UI state every second
        stateSaveRunnable = new Runnable() {
            @Override
            public void run() {
                // Only save state if we have a valid routine
                if (currentRoutine != null) {
                    saveRoutineState();
                }
                
                // Schedule the next state save
                timerHandler.postDelayed(this, STATE_SAVE_INTERVAL_MS);
            }
        };
        
        // Start the timer with the constant update interval
        timerHandler.post(timerRunnable);
    }

    public static RoutineFragment newInstance(int routineId) {
        RoutineFragment fragment = new RoutineFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ROUTINE_ID, routineId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("RoutineFragment", "onCreate called");

        var modelOwner = requireActivity();
        var modelFactory = ViewModelProvider.Factory.from(MainViewModel.initializer);
        var modelProvider = new ViewModelProvider(modelOwner, modelFactory);
        this.activityModel = modelProvider.get(MainViewModel.class);
        
        // Get repository instance
        this.repository = HabitizerApplication.getRepository();

        int routineId = getArguments().getInt(ARG_ROUTINE_ID);
        isTimerRunning = true;
        Log.d("RoutineFragment", "Routine ID from arguments: " + routineId);
        
        // Check if we're restoring from a saved state (app restart)
        if (savedInstanceState != null) {
            Log.d("RoutineFragment", "Restoring from saved instance state");
            
            // Restore state values
            boolean savedManuallyStarted = savedInstanceState.getBoolean("MANUALLY_STARTED", false);
            boolean savedIsTimerRunning = savedInstanceState.getBoolean("IS_TIMER_RUNNING", true);
            boolean savedIsPaused = savedInstanceState.getBoolean("IS_PAUSED", false);
            
            // Apply the restored values
            manuallyStarted = savedManuallyStarted;
            isTimerRunning = savedIsTimerRunning;
            isPaused = savedIsPaused;
            
            Log.d("RoutineFragment", "Restored state: manuallyStarted=" + manuallyStarted + 
                  ", isTimerRunning=" + isTimerRunning + 
                  ", isPaused=" + isPaused);
        }

        // Get routine
        this.currentRoutine = activityModel.getRoutineRepository().getRoutine(routineId);
        Log.d("RoutineFragment", "Current routine: " + (currentRoutine != null ? currentRoutine.getRoutineName() : "null"));
        
        // Check if the routine is active (was started from the home page)
        // We need to mark empty routines as manually started too, to prevent redirecting to homescreen
        // The timer logic elsewhere will still respect the requirement not to start timers for empty routines
        if (currentRoutine != null && currentRoutine.isActive()) {
            Log.d("RoutineFragment", "Routine is active, marking as manually started (has " + 
                   (currentRoutine.getTasks().size()) + " tasks)");
            // If the routine was started from home page, mark it as manually started
            // Even empty routines should be considered "manually started" to stay on the screen
            manuallyStarted = true;
        } else {
            Log.d("RoutineFragment", "Routine is not active, not marking as manually started");
            manuallyStarted = false;
        }

        // Initialize the routineStateManager
        routineStateManager = new RoutineStateManager(requireContext());
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("RoutineFragment", "onCreateView called");
        binding = FragmentRoutineScreenBinding.inflate(inflater, container, false);
        Log.d("RoutineFragment", "Binding inflated");

        // Explicitly set the actual time to "-" at initialization to prevent flicker
        binding.actualTime.setText("-");
        
        // Initialize pause button state
        isPaused = false;
        binding.pauseButton.setText("Pause");
        binding.pauseButton.setBackground(getResources().getDrawable(R.drawable.rounded_button_background));

        // Initialize stop timer state
        isStopTimerPressed = false;
        binding.stopTimerButton.setText("Switch to Mock");
        binding.stopTimerButton.setBackground(getResources().getDrawable(R.drawable.rounded_button_background));

        // Initialize timer updates but only start the timer if there are tasks
        initTimerUpdates();
        isTimerRunning = true;

        if (currentRoutine == null) {
            Log.e("RoutineFragment", "Current routine is null in onCreateView! Trying to recover...");
            
            // Try to get the routine ID from arguments again
            int routineId = getArguments().getInt(ARG_ROUTINE_ID, 0);
            Log.d("RoutineFragment", "Attempting to recover with routine ID: " + routineId);
            
            // Try to get the routine again
            currentRoutine = activityModel.getRoutineRepository().getRoutine(routineId);
            
            if (currentRoutine == null) {
                Log.e("RoutineFragment", "Recovery failed, still null. Using default routine.");
                // Create a temporary routine as a fallback
                currentRoutine = new Routine(routineId, "Routine " + routineId);
                
                // Try to save this temporary routine so it exists in the repository
                activityModel.getRoutineRepository().save(currentRoutine);
            } else {
                Log.d("RoutineFragment", "Recovery successful, got routine: " + currentRoutine.getRoutineName());
            }
        }

        Log.d("RoutineFragment", "Setting up routine with name: " + currentRoutine.getRoutineName());
        Log.d("RoutineFragment", "Current routine has " + currentRoutine.getTasks().size() + " tasks");
        
        // Clear "completed" statuses of all tasks
        for (Task task : currentRoutine.getTasks()) {
            task.reset();
        }
        
        // Reset the routine itself to ensure fresh timer state
        // This is important to prevent carrying over any stale timer state
        if (currentRoutine.isActive()) {
            Log.d("RoutineFragment", "Routine was already active, resetting it to ensure clean timer state");
            // End any existing routine first
            currentRoutine.endRoutine(LocalDateTime.now());
        }
        
        // Start fresh - set the proper routine initial state
        // Only start the timer if the routine has tasks (per requirement)
        if (!currentRoutine.getTasks().isEmpty() && currentRoutine.isActive()) {
            Log.d("RoutineFragment", "Starting routine with tasks: " + currentRoutine.getTasks().size());
            manuallyStarted = true;
            currentRoutine.startRoutine(LocalDateTime.now());
        } else if (currentRoutine.isActive()) {
            // For empty routines, still set manuallyStarted=true but don't start the timer
            Log.d("RoutineFragment", "Routine is active but empty, setting manuallyStarted without starting timer");
            manuallyStarted = true;
        }

        // Initialize ListView and Adapter
        ListView taskListView = binding.routineList;
        if (taskListView == null) {
            Log.e("RoutineFragment", "taskListView is null! Layout issue?");
        } else {
            Log.d("RoutineFragment", "ListView found with ID: " + taskListView.getId());
        }
        
        Log.d("RoutineFragment", "Setting up task adapter");
        
        ArrayList<Task> initialTasks = new ArrayList<>(currentRoutine.getTasks());
        Log.d("RoutineFragment", "Initial tasks count for adapter: " + initialTasks.size());
        
        taskAdapter = new TaskAdapter(
                requireContext(),
                R.layout.task_page,
                initialTasks,
                currentRoutine,
                LegacyLogicAdapter.getCompatInstance(), 
                getParentFragmentManager()
        );
        
        // Set this fragment as a reference for the TaskAdapter
        taskAdapter.setRoutineFragment(this);
        
        taskListView.setAdapter(taskAdapter);
        Log.d("RoutineFragment", "Task adapter set on ListView");

        // Set routine name and goal time display
        binding.routineNameTask.setText(currentRoutine.getRoutineName());
        updateRoutineGoalDisplay(currentRoutine.getGoalTime());

        // Observe task data
        Log.d("RoutineFragment", "Setting up task observer");
        var subject = activityModel.getRoutineRepository().find(currentRoutine.getRoutineId());
        subject.observe(routine -> {
            // Skip if we're already updating or if routine is null
            if (isUpdatingFromObserver || routine == null) {
                if (routine == null) {
                    Log.e("RoutineFragment", "Routine from observer is null!");
                }
                return;
            }
            
            // Set flag to prevent recursive updates
            isUpdatingFromObserver = true;
            
            try {
                Log.d("RoutineFragment", "Routine observer triggered");
                Log.d("RoutineFragment", "Task count in observer: " + routine.getTasks().size());
                
                // Only update if the data has actually changed
                if (!routinesEqual(currentRoutine, routine)) {
                    // Update the current routine reference
                    currentRoutine = routine;
                    
                    // Update adapter with new tasks
                    taskAdapter.clear();
                    taskAdapter.addAll(routine.getTasks());
                    taskAdapter.notifyDataSetChanged();
                    
                    // Also update the UI elements that show routine details
                    binding.routineNameTask.setText(routine.getRoutineName());
                    updateRoutineGoalDisplay(routine.getGoalTime());
                    
                    Log.d("RoutineFragment", "UI updated with " + routine.getTasks().size() + " tasks");
                } else {
                    Log.d("RoutineFragment", "Skipping update - routine data hasn't changed");
                }
            } finally {
                // Clear flag when done
                isUpdatingFromObserver = false;
            }
        });

        binding.addTaskButton.setOnClickListener(v -> {
            CreateTaskDialogFragment dialog = CreateTaskDialogFragment.newInstance(this::addTaskToRoutine);
            dialog.show(getParentFragmentManager(), "CreateTaskDialog");
        });

        binding.expectedTime.setOnClickListener(v -> {
            SetRoutineTimeDialogFragment dialog = SetRoutineTimeDialogFragment.newInstance(this::updateRoutineGoalDisplay);
            dialog.show(getParentFragmentManager(), "SetTimeDialog");
        });

        binding.endRoutineButton.setOnClickListener(v -> {
            // Determine action based on manuallyStarted flag, not button text
            if (!manuallyStarted) {
                // First press = start the routine
                manuallyStarted = true;
                isTimerRunning = true;
                
                // Reset button states
                resetButtonStates();
                
                // Ensure routine is active
                if (!currentRoutine.isActive()) {
                    currentRoutine.startRoutine(LocalDateTime.now());
                }
                
                // Update UI immediately
                updateTimeDisplay();
                
                // Special handling for Morning routine with ID 0 to prevent duplication
                boolean isMorningRoutineWithIdZero = currentRoutine != null && 
                                                  "Morning".equals(currentRoutine.getRoutineName()) && 
                                                  currentRoutine.getRoutineId() == 0;
                
                // Save state
                if (!isUpdatingFromObserver) {
                    // Always update local repository
                    repository.updateRoutine(currentRoutine);
                    
                    // But for Morning with ID 0, don't save to main repository to prevent duplication
                    if (!isMorningRoutineWithIdZero) {
                        activityModel.getRoutineRepository().save(currentRoutine);
                        Log.d("RoutineFragment", "Saved routine state to repository");
                    } else {
                        Log.d("RoutineFragment", "Morning routine with ID 0 - not saving to prevent duplication");
                    }
                }
            } else {
                // Second press = end the routine
                isTimerRunning = false;
                currentRoutine.endRoutine(LocalDateTime.now());
                manuallyStarted = false;  // Reset the manually started flag when ending
                
                // Clear active status in SharedPreferences
                if (routineStateManager != null) {
                    routineStateManager.clearRunningRoutineState();
                    Log.d("RoutineFragment", "Cleared active routine state when ending routine via button");
                }
                
                // Reset button states
                resetButtonStates();
                
                // Explicitly set the button text to "Routine Ended" when ending the routine
                binding.endRoutineButton.setText("Routine Ended");
                binding.endRoutineButton.setEnabled(false);
                binding.stopTimerButton.setEnabled(false);
                binding.pauseButton.setEnabled(false);
                binding.homeButton.setEnabled(true);
                
                // Refresh adapter to update checkbox states for any unchecked tasks
                if (taskAdapter != null) {
                    taskAdapter.notifyDataSetChanged();
                    Log.d("RoutineFragment", "Refreshed task adapter after manual routine end");
                }
                
                // Special handling for Morning routine with ID 0 to prevent duplication
                boolean isMorningRoutineWithIdZero = currentRoutine != null && 
                                                  "Morning".equals(currentRoutine.getRoutineName()) && 
                                                  currentRoutine.getRoutineId() == 0;
                
                // Save the routine state to make sure the end time is preserved
                if (!isUpdatingFromObserver) {
                    // Always update local repository
                    repository.updateRoutine(currentRoutine);
                    
                    // But for Morning with ID 0, don't save to main repository to prevent duplication
                    if (!isMorningRoutineWithIdZero) {
                        activityModel.getRoutineRepository().save(currentRoutine);
                        Log.d("RoutineFragment", "Saved routine state to repository");
                    } else {
                        Log.d("RoutineFragment", "Morning routine with ID 0 - not saving to prevent duplication");
                    }
                }
                
                updateTimeDisplay();
            }
        });

        binding.stopTimerButton.setOnClickListener(v -> {
            if (!isStopTimerPressed) {
                // First click - Switch to Mock functionality
                isStopTimerPressed = true;
                binding.stopTimerButton.setText("Advance Mock Timer");
                
                // Stop real timer functionality
                if (currentRoutine.isActive()) {
                    // Save the current time before switching to mock
                    if (timeBeforePauseMinutes == 0) {
                        timeBeforePauseMinutes = currentRoutine.getRoutineDurationMinutes();
                    }
                    
                    // Pause at current simulated time, but don't affect the pause state
                    // This allows separate tracking of pause button vs stop timer button
                    currentRoutine.pauseTime(LocalDateTime.now());
                    updateTimeDisplay();
                }
                
                // We don't set isTimerRunning to false here to allow mockup testing
                // even when the routine is paused
            } else {
                // Second click - Advance Mock Timer functionality
                
                // Only advance mock timer if routine is active AND not paused
                if (currentRoutine.isActive()) {
                    // If in paused state, don't allow advancing the timer
                    if (isPaused) {
                        Log.d("AdvanceTimer", "Cannot advance timer while paused in mock mode");
                        return;
                    }
                    
                    // Advance mock timer by 15 seconds
                    currentRoutine.fastForwardTime();

                    // If we have any saved task times from previous pause, clear them
                    // This ensures we'll calculate fresh times based on the advanced timer
                    taskTimeBeforePauseMinutes = 0;
                    taskSecondsBeforePause = 0;

                    // Force immediate UI update
                    updateTimeDisplay();
                    
                    // If routine completed via fast forward, update state
                    if (currentRoutine.autoCompleteRoutine()) {
                        binding.endRoutineButton.setEnabled(false);
                        isTimerRunning = false;
                    }
                    
                    // Always update the task elapsed time to keep displays in sync
                    updateCurrentTaskElapsedTime();
                } else {
                    // In case routine is not active, show a log message
                    Log.d("RoutineFragment", "Advance Mock Timer button clicked but routine is not active - no action taken");
                }
            }
        });

        binding.pauseButton.setOnClickListener(v -> {
            // Toggle between pause and resume
            if (!isPaused) {
                // Set pause state first, before any other operations
                isPaused = true;
                
                // IMPORTANT: Save the current task's elapsed time BEFORE pausing the routine timer
                // This ensures we capture the correct running time
                saveCurrentTaskElapsedTime();
                
                // Update button text and color
                binding.pauseButton.setText("Resume");
                binding.pauseButton.setBackground(getResources().getDrawable(R.drawable.rounded_button_selector));
                
                // Special handling in mock mode - don't affect the real timer
                if (isStopTimerPressed) {
                    // In mock mode, just update UI state and don't touch the real timer
                    // which should already be paused
                    Log.d("PauseButton", "Pausing in mock mode - not affecting real timer");
                    
                    // Make sure we save current task time for proper display while paused
                    // This ensures the task time remains fixed while paused in mock mode
                    if (taskTimeBeforePauseMinutes == 0 && taskSecondsBeforePause == 0) {
                        // Only save these values if they haven't been saved yet
                        saveCurrentTaskElapsedTime();
                        // Get and display the current state before pausing
                        // This is critical to keep task duration frozen while in paused state
                        Log.d("PauseButton", "Saved task time for pause in mock mode - seconds: " + 
                              taskSecondsBeforePause + ", minutes: " + taskTimeBeforePauseMinutes);
                    }
                    
                    // Force update displays with the saved values
                    updateTimeDisplay();
                    updateCurrentTaskElapsedTime();
                    
                    // Refresh task list to update checkboxes
                    if (taskAdapter != null) {
                        taskAdapter.notifyDataSetChanged();
                    }
                    return;
                }
                
                // Normal pause behavior for real timer mode
                // Now pause the routine timer (which will affect time calculations)
                if (currentRoutine.isActive()) {
                    // Save the current routine time before pausing
                    timeBeforePauseMinutes = currentRoutine.getRoutineDurationMinutes();
                    Log.d("PauseButton", "Saving routine time before pause: " + timeBeforePauseMinutes + "m");
                    
                    // Log detailed state before pausing
                    Log.d("PauseButton", "Before pause - Routine duration: " + 
                          currentRoutine.getRoutineDurationMinutes() + "m, " +
                          "Task duration: " + (taskTimeBeforePauseMinutes > 0 ? 
                            taskTimeBeforePauseMinutes + "m" : 
                            (taskSecondsBeforePause > 0 ? taskSecondsBeforePause + "s" : "0s")) + ", " +
                          "isStopTimerPressed: " + isStopTimerPressed);
                    
                    // Store current time for consistent updates
                    LocalDateTime pauseTime = LocalDateTime.now();
                    currentRoutine.pauseTime(pauseTime);
                    
                    // Log the state after pausing
                    Log.d("PauseButton", "After pause - Routine duration: " + 
                          currentRoutine.getRoutineDurationMinutes() + "m, " +
                          "Current time: " + currentRoutine.getCurrentTime());
                    
                    // Force update displays
                    updateTimeDisplay();
                    updateCurrentTaskElapsedTime();
                    
                    // Refresh task list to re-enable checkboxes
                    if (taskAdapter != null) {
                        taskAdapter.notifyDataSetChanged();
                    }
                }
            } else {
                // Resume from paused state
                
                // Log the state before resuming
                Log.d("PauseButton", "Before resume - Routine duration: " + 
                      currentRoutine.getRoutineDurationMinutes() + "m, " +
                      "Task duration: " + (taskTimeBeforePauseMinutes > 0 ? 
                        taskTimeBeforePauseMinutes + "m" : 
                        (taskSecondsBeforePause > 0 ? taskSecondsBeforePause + "s" : "0s")) + ", " +
                        "Saved routine time: " + timeBeforePauseMinutes + "m");
                
                // Update UI state first
                binding.pauseButton.setText("Pause");
                binding.pauseButton.setBackground(getResources().getDrawable(R.drawable.rounded_button_selector));
                
                // If in mock mode, we should maintain the mock time and not resume the real timer
                if (isStopTimerPressed) {
                    // Just update the UI state, don't actually resume the real timer
                    isPaused = false;
                    
                    // Log the saved task time for debugging
                    Log.d("PauseButton", "Resuming in mock mode with task time: " + 
                          (taskTimeBeforePauseMinutes > 0 ? 
                            taskTimeBeforePauseMinutes + "m" : 
                            (taskSecondsBeforePause > 0 ? taskSecondsBeforePause + "s" : "0s")) +
                          ", Routine time: " + currentRoutine.getRoutineDurationMinutes() + "m");
                    
                    // Reset saved task times to allow immediate updates when advancing timer
                    taskTimeBeforePauseMinutes = 0;
                    taskSecondsBeforePause = 0;
                    
                    // Force update displays with current times (not saved times)
                    updateTimeDisplay();
                    updateCurrentTaskElapsedTime();
                    
                    Log.d("PauseButton", "Resumed in mock mode - keeping mock timer active");
                } else {
                    // Normal resume behavior for real timer mode
                    
                    // Get current time for calculations
                    LocalDateTime resumeTime = LocalDateTime.now();
                    
                    // If the current duration is 0 but we have a saved time, use that to adjust the start time
                    if (currentRoutine.getRoutineDurationMinutes() == 0 && timeBeforePauseMinutes > 0) {
                        Log.d("PauseButton", "Adjusting routine start time to preserve duration: " + timeBeforePauseMinutes + "m");
                        
                        // Calculate how many seconds to adjust the start time
                        long adjustmentSeconds = timeBeforePauseMinutes * 60;
                        
                        // Adjust the start time of the routine timer
                        if (currentRoutine.getRoutineTimer() != null && currentRoutine.getRoutineTimer().getStartTime() != null) {
                            LocalDateTime adjustedStartTime = resumeTime.minusSeconds(adjustmentSeconds);
                            currentRoutine.getRoutineTimer().updateStartTime(adjustedStartTime);
                            Log.d("PauseButton", "Adjusted routine timer start time to: " + adjustedStartTime);
                            
                            // Also adjust the task timer start time to match
                            if (currentRoutine.getTaskTimer() != null) {
                                currentRoutine.getTaskTimer().updateStartTime(adjustedStartTime);
                                Log.d("PauseButton", "Adjusted task timer start time to: " + adjustedStartTime);
                                
                                // Ensure the task timer is running
                                if (!currentRoutine.getTaskTimer().isRunning()) {
                                    currentRoutine.getTaskTimer().start(adjustedStartTime);
                                    Log.d("PauseButton", "Started task timer with adjusted start time");
                                }
                            }
                        }
                    }
                    
                    // Resume with the current time only for real timer mode
                    currentRoutine.resumeTime(resumeTime);
                    
                    // Update timer state after resuming the routine timer
                    isPaused = false;
                    isTimerRunning = true;
                    
                    // Only clear the saved times in normal mode, not in mock mode
                    timeBeforePauseMinutes = 0;
                    taskTimeBeforePauseMinutes = 0;
                    taskSecondsBeforePause = 0;
                    
                    // Log the state after resuming
                    Log.d("PauseButton", "After resume - Routine duration: " + 
                          currentRoutine.getRoutineDurationMinutes() + "m, " +
                          "Current time: " + currentRoutine.getCurrentTime());
                    
                    // Force update displays
                    updateTimeDisplay();
                    updateCurrentTaskElapsedTime();
                }
            }
        });

        binding.homeButton.setOnClickListener(v -> {
            Log.d("RoutineFragment", "Home button clicked, navigating to home screen");
            
            // Save the routine state before navigating away
            if (currentRoutine != null) {
                saveRoutineState();
            }
            
            // Navigate back to HomeScreenFragment
            if (getActivity() instanceof MainActivity) {
                // Use the MainActivity's method to show the home screen
                ((MainActivity) getActivity()).showHomeScreen();
                Log.d("RoutineFragment", "Navigated to home screen using MainActivity method");
            } else {
                // Fallback to using back navigation
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
                Log.d("RoutineFragment", "Navigated to home screen using back dispatcher");
            }
        });

        // Initial state setup
        binding.homeButton.setEnabled(false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("RoutineFragment", "onViewCreated called");
        
        // After loading the routine, check if we need to restore saved UI state
        restoreSavedUIState();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("RoutineFragment", "onResume called");
        
        // If app was minimized, check how to resume
        if (timeWhenMinimized != null && wasTimerRunningBeforeMinimize) {
            Log.d("RoutineFragment", "App is being restored from minimized state with active routine");
            
            // Only handle if the routine is still active (not ended)
            if (currentRoutine != null && currentRoutine.isActive()) {
                // Calculate how long the app was in background (for logging)
                LocalDateTime now = LocalDateTime.now();
                long secondsInBackground = java.time.Duration.between(timeWhenMinimized, now).getSeconds();
                Log.d("RoutineFragment", "App was in background for " + secondsInBackground + " seconds");
                
                // Resume timer with the current time
                currentRoutine.resumeTime(now);
                isTimerRunning = true;
                
                // Reset the minimized flag
                timeWhenMinimized = null;
                wasTimerRunningBeforeMinimize = false;
            }
            
            // Force update display
            updateTimeDisplay();
            
            Log.d("RoutineFragment", "Timer resumed, current duration: " + 
                  currentRoutine.getRoutineDurationMinutes() + "m");
        }

        // Start the timer updates
        if (isTimerRunning && !isPaused) {
            timerHandler.post(timerRunnable);
        }
        
        // Start the state saving runnable with 1 second interval
        timerHandler.removeCallbacks(stateSaveRunnable); // Remove any existing callbacks first
        timerHandler.post(stateSaveRunnable);
        Log.d("RoutineFragment", "Started routine state saving runnable with " + STATE_SAVE_INTERVAL_MS + "ms interval");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("RoutineFragment", "onPause called");
        
        // Save timer state when app is minimized
        if (currentRoutine != null && currentRoutine.isActive() && isTimerRunning) {
            Log.d("RoutineFragment", "Pausing timer as app is being minimized");
            
            // Save current state
            wasTimerRunningBeforeMinimize = isTimerRunning;
            timeWhenMinimized = LocalDateTime.now();
            
            // Pause the timer but don't update UI yet (will update in onResume)
            currentRoutine.pauseTime(timeWhenMinimized);
        }

        // Save state immediately when pausing
        if (currentRoutine != null) {
            saveRoutineState();
        }
        
        // Remove any pending state save runnables
        timerHandler.removeCallbacks(stateSaveRunnable);
    }

    private void addTaskToRoutine(String taskName) {
        if (taskName == null || taskName.trim().isEmpty()) return;

        // Check if this is the first task being added
        boolean wasEmpty = currentRoutine.getTasks().isEmpty();

        // Create task with auto-increment ID based on timestamp for uniqueness
        int newTaskId = (int)(System.currentTimeMillis() % 100000);
        Task newTask = new Task(newTaskId, taskName, false);
        Log.d("RoutineFragment", "Creating new task with ID: " + newTaskId + ", name: " + taskName);

        // Add to current routine first (local update)
        currentRoutine.addTask(newTask);
        
        // Check if the routine has already been ended
        boolean routineEnded = binding.endRoutineButton.getText().toString().equals("Routine Ended");
        
        if (!routineEnded) {
            // Only auto-start the routine if it hasn't been ended yet
            manuallyStarted = true;
            isTimerRunning = true;
            
            // Reset button states when starting a routine
            resetButtonStates();
            
            // If this was the first task added to an empty routine, start the timer
            if (wasEmpty) {
                Log.d("RoutineFragment", "First task added to empty routine - starting timer");
                currentRoutine.startRoutine(LocalDateTime.now());
                
                // Update the UI to reflect that the timer has started
                updateTimeDisplay();
                updateCurrentTaskElapsedTime();
            }
        } else {
            // For tasks added after routine has ended, they should be disabled
            // Keep the routine in ended state - don't restart it
            Log.d("RoutineFragment", "Adding task to ended routine - not restarting timer");
        }
        
        // Update adapter immediately for responsive UI
        taskAdapter.clear();
        taskAdapter.addAll(currentRoutine.getTasks());
        taskAdapter.notifyDataSetChanged();
        Log.d("RoutineFragment", "Task adapter updated with " + currentRoutine.getTasks().size() + " tasks including new task");

        // Then update repositories if not in an observer update
        if (!isUpdatingFromObserver) {
            Log.d("RoutineFragment", "Saving task and routine to repositories");
            // First add the task to the task repository to ensure it exists
            repository.addTask(newTask);
            
            // Special handling for Morning routine with ID 0 to prevent duplication
            boolean isMorningRoutineWithIdZero = currentRoutine != null && 
                                              "Morning".equals(currentRoutine.getRoutineName()) && 
                                              currentRoutine.getRoutineId() == 0;
            
            // Then update the routine with the new task in the local repository
            repository.updateRoutine(currentRoutine);
            
            // But for Morning with ID 0, don't save to main repository to prevent duplication
            if (!isMorningRoutineWithIdZero) {
                // Also update the legacy repository for compatibility
                activityModel.getRoutineRepository().save(currentRoutine);
                Log.d("RoutineFragment", "Saved routine to repository");
            } else {
                Log.d("RoutineFragment", "Morning routine with ID 0 - not saving to repository to prevent duplication");
            }
        }

        // Manually call updateTimeDisplay to ensure button states are updated
        updateTimeDisplay();
    }

    private void updateRoutineGoalDisplay(@Nullable Integer newTime) {
        currentRoutine.updateGoalTime(newTime);
        @Nullable Integer goalTime = currentRoutine.getGoalTime();
        if (goalTime == null) {
            binding.expectedTime.setText("-");
        } else {
            binding.expectedTime.setText(String.format("%d%s", goalTime, "m"));
        }
        
        // Only update Room database if not already in an observer update
        if (!isUpdatingFromObserver) {
            Log.d("RoutineFragment", "Updating routine in repository with goal time: " + goalTime);
            repository.updateRoutine(currentRoutine);
        }
    }

    /**
     * Updates the display of the time elapsed in the textview.
     */
    private void updateTimeDisplay() {
        // Ensure the routine is properly initialized
        if (currentRoutine == null) {
            Log.e("RoutineFragment", "Cannot update time display - routine is null");
            binding.actualTime.setText("0m");  // Show "0m" instead of "-" when routine is null
            return;
        }

        // In mock mode with paused state, preserve the original time
        if (isStopTimerPressed && isPaused) {
            // Use the saved routine time directly in mock mode when paused
            if (timeBeforePauseMinutes > 0) {
                binding.actualTime.setText(String.valueOf(timeBeforePauseMinutes) + "m");
                Log.d("RoutineFragment", "Using saved time in mock mode: " + timeBeforePauseMinutes + "m");
            } else {
                // If no saved time (unlikely), show current routine time but don't update it
                binding.actualTime.setText(String.valueOf(currentRoutine.getRoutineDurationMinutes()) + "m");
                Log.d("RoutineFragment", "No saved time found in paused mock mode, using current: " + 
                    currentRoutine.getRoutineDurationMinutes() + "m");
            }
            return;
        }
        
        // Get current routine duration from the routine in minutes
        long minutesDuration = currentRoutine.getRoutineDurationMinutes();
        
        // Log detailed timing information for debugging
        Log.d("RoutineFragment", "=== TIME DISPLAY UPDATE ===");
        Log.d("RoutineFragment", "Minutes to display: " + minutesDuration);
        Log.d("RoutineFragment", "Saved time before pause: " + timeBeforePauseMinutes);
        Log.d("RoutineFragment", "Routine active: " + currentRoutine.isActive());
        Log.d("RoutineFragment", "Timer running: " + isTimerRunning);
        Log.d("RoutineFragment", "Manually started: " + manuallyStarted);
        Log.d("RoutineFragment", "Is paused: " + isPaused);
        Log.d("RoutineFragment", "Is stop timer pressed: " + isStopTimerPressed);
        
        // Check the routine's internal timer state
        if (currentRoutine.getRoutineTimer() != null) {
            LocalDateTime startTime = currentRoutine.getRoutineTimer().getStartTime();
            Log.d("RoutineFragment", "Routine timer start: " + startTime);
            Log.d("RoutineFragment", "Routine timer end: " + currentRoutine.getRoutineTimer().getEndTime());
        }
        
        // If we're in paused state and the calculated time is 0 or less than saved time,
        // use the saved time before pause instead - but ONLY if we're still in paused state
        if (isPaused && timeBeforePauseMinutes > 0 && minutesDuration == 0) {
            Log.d("RoutineFragment", "Using saved time before pause: " + timeBeforePauseMinutes + "m");
            minutesDuration = timeBeforePauseMinutes;
        }
        
        // Set routine time display with the 'm' suffix
        binding.actualTime.setText(minutesDuration + "m");
        
        boolean hasTasks = !currentRoutine.getTasks().isEmpty();
        
        // Only auto-activate the routine timer if it has tasks AND is manually started
        // This preserves the requirement that empty routines should not have their timers started
        if (hasTasks && manuallyStarted && !currentRoutine.isActive()) {
            // If it has tasks but isn't active, activate it unless explicitly ended
            if (!binding.endRoutineButton.getText().toString().equals("Routine Ended")) {
                Log.d("RoutineFragment", "Auto-activating routine that has tasks and is manually started");
                currentRoutine.startRoutine(LocalDateTime.now());
                
                // Update task elapsed time to show correct starting values
                updateCurrentTaskElapsedTime();
            }
        }
        
        boolean routineIsActive = currentRoutine.isActive();
        Log.d("RoutineFragment", "UpdateTimeDisplay - hasTasks: " + hasTasks + ", isActive: " + routineIsActive + 
              ", manuallyStarted: " + manuallyStarted + ", time: " + minutesDuration + "m");

        // Modified button logic:
        // 1. Empty routine: "End Routine" text, always disabled
        // 2. Routine with tasks that isn't manually started: "End Routine" text, disabled (user must start from home page)
        // 3. Routine with tasks that is manually started: "End Routine" text, enabled (to end routine)
        // 4. Routine that was ended: "Routine Ended" text, disabled

        if (binding.endRoutineButton.getText().toString().equals("Routine Ended")) {
            // Case 4: Routine was explicitly ended - maintain the ended state
            binding.endRoutineButton.setText("Routine Ended");
            binding.endRoutineButton.setEnabled(false);
            binding.stopTimerButton.setEnabled(false);
            binding.pauseButton.setEnabled(false); // Disable pause button when routine is ended
            binding.homeButton.setEnabled(true);
        } else if (!hasTasks || !manuallyStarted) {
            // Cases 1 & 2: Empty routine OR routine with tasks that isn't manually started
            // Keep text as "End Routine" and DISABLE the button
            binding.endRoutineButton.setText("End Routine");
            binding.endRoutineButton.setEnabled(false);
            binding.stopTimerButton.setEnabled(false);
            binding.pauseButton.setEnabled(false); // Disable pause button when routine isn't started
            binding.homeButton.setEnabled(true);
        } else {
            // Case 3: Routine with tasks that is manually started - enable buttons
            binding.endRoutineButton.setText("End Routine");
            binding.endRoutineButton.setEnabled(true);
            
            // Only enable stopTimerButton if not already pressed 
            // (since it becomes Fast Forward after press)
            if (!isStopTimerPressed) {
                // Always enable stop timer button, even when paused
                // This allows users to mock test even when paused
                binding.stopTimerButton.setEnabled(true);
            } else {
                // Always enable Fast Forward functionality after Stop Timer is pressed
                binding.stopTimerButton.setEnabled(true);
            }
            
            // Keep pause button enabled even when timer is paused
            binding.pauseButton.setEnabled(true);
            
            binding.homeButton.setEnabled(false);
        }

        // Update task list times
        taskAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.removeCallbacks(stateSaveRunnable);
    }

    // Helper method to compare routines
    private boolean routinesEqual(Routine r1, Routine r2) {
        if (r1 == r2) return true;
        if (r1 == null || r2 == null) return false;
        
        // Compare basic properties
        if (r1.getRoutineId() != r2.getRoutineId()) return false;
        if (!r1.getRoutineName().equals(r2.getRoutineName())) return false;
        
        // Compare task lists (basic check)
        if (r1.getTasks().size() != r2.getTasks().size()) return false;
        
        return true; // Consider equal for update purposes
    }

    /**
     * Updates the UI to show that the routine has ended.
     */
    public void updateUIForEndedRoutine() {
        if (binding == null) return;
        
        Log.d("RoutineFragment", "Updating UI for automatically ended routine");
        
        // Set the routine as ended
        isTimerRunning = false;
        manuallyStarted = false;
        
        // Update UI elements
        binding.endRoutineButton.setText("Routine Ended");
        binding.endRoutineButton.setEnabled(false);
        binding.stopTimerButton.setEnabled(false);
        binding.pauseButton.setEnabled(false); // Disable pause button when routine is ended
        binding.homeButton.setEnabled(true);
        
        // Reset button states
        resetButtonStates();
        
        // Force update the time display
        updateTimeDisplay();
        
        // Ensure task adapter refreshes to update checkbox states
        if (taskAdapter != null) {
            taskAdapter.notifyDataSetChanged();
            Log.d("RoutineFragment", "Refreshed task adapter to update checkbox states");
        }
        
        // Clear the saved routine state in SharedPreferences
        if (routineStateManager != null) {
            routineStateManager.clearRunningRoutineState();
            Log.d("RoutineFragment", "Cleared saved routine state after routine ended");
        }
        
        // Save the routine state to ensure the end time is preserved
        if (!isUpdatingFromObserver) {
            // Special handling for Morning routine with ID 0 to prevent duplication
            boolean isMorningRoutineWithIdZero = currentRoutine != null && 
                                              "Morning".equals(currentRoutine.getRoutineName()) && 
                                              currentRoutine.getRoutineId() == 0;
            
            // Always update local repository
            repository.updateRoutine(currentRoutine);
            
            // But for Morning with ID 0, don't save to main repository to prevent duplication
            if (!isMorningRoutineWithIdZero) {
                activityModel.getRoutineRepository().save(currentRoutine);
                Log.d("RoutineFragment", "Saved ended routine state to repository");
            }
        }
    }

    private void resetButtonStates() {
        // Reset pause state if it was paused
        boolean wasPaused = isPaused;
        if (isPaused) {
            isPaused = false;
            binding.pauseButton.setText("Pause");
            binding.pauseButton.setBackground(getResources().getDrawable(R.drawable.rounded_button_background));
        }
        
        // Reset stop timer state
        isStopTimerPressed = false;
        binding.stopTimerButton.setText("Switch to Mock");
        binding.stopTimerButton.setBackground(getResources().getDrawable(R.drawable.rounded_button_background));
        
        // Refresh task list if pause state changed
        if (wasPaused && taskAdapter != null) {
            taskAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Helper method to check if the routine has ended
     * @return true if the routine has ended, false otherwise
     */
    public boolean isRoutineEnded() {
        if (binding == null) return false;
        
        // Check if the end routine button text is "Routine Ended"
        boolean endButtonIndicatesEnded = "Routine Ended".equals(binding.endRoutineButton.getText().toString());
        
        // Also check the state of the routine's timer
        boolean routineTimerEnded = currentRoutine != null && 
                                   currentRoutine.getRoutineTimer() != null && 
                                   currentRoutine.getRoutineTimer().getEndTime() != null;
        
        Log.d("RoutineFragment", "Checking if routine ended: button=" + endButtonIndicatesEnded + 
              ", timer=" + routineTimerEnded);
              
        // Return true if either condition indicates the routine has ended
        return endButtonIndicatesEnded || routineTimerEnded;
    }
    
    /**
     * Helper method to check if the fragment is paused
     * @return true if the fragment is paused, false otherwise
     */
    public boolean isPaused() {
        return isPaused;
    }

    /**
     * Update the display of the current task's elapsed time
     */
    private void updateCurrentTaskElapsedTime() {
        final String TAG = "ELAPSED_TIME_DEBUG";
        
        // Early exit if no routine
        if (currentRoutine == null) {
            Log.d(TAG, "Routine is null, clearing elapsed time text");
            binding.currentTaskElapsedTime.setText("");
            return;
        }

        // Find the first uncompleted task (current active task)
        List<Task> tasks = currentRoutine.getTasks();
        if (tasks.isEmpty()) {
            Log.d(TAG, "Task list is empty, clearing elapsed time text");
            binding.currentTaskElapsedTime.setText("");
            return;
        }
        
        // First try to find a task that is not completed and not skipped
        Task currentTask = null;
        for (Task task : tasks) {
            if (!task.isCompleted() && !task.isSkipped()) {
                currentTask = task;
                Log.d(TAG, "Found active task: " + task.getTaskName());
                break;
            }
        }
        
        // If not found, try to find first unchecked task
        if (currentTask == null) {
            for (Task task : tasks) {
                if (!task.isCheckedOff()) {
                    currentTask = task;
                    Log.d(TAG, "Found unchecked task: " + task.getTaskName());
                    break;
                }
            }
        }
        
        // If still no active task found but routine has tasks, use first task
        if (currentTask == null && !tasks.isEmpty()) {
            Task firstTask = tasks.get(0);
            // Make sure it's not marked as skipped for display purposes
            firstTask.setSkipped(false);
            currentTask = firstTask;
            Log.d(TAG, "Using first task as fallback: " + firstTask.getTaskName());
            
            // Save the change
            if (repository != null) {
                repository.updateRoutine(currentRoutine);
            }
        }
        
        // If still no active task, show empty elapsed time
        if (currentTask == null) {
            Log.d(TAG, "No viable task found, clearing elapsed time text");
            binding.currentTaskElapsedTime.setText("");
            return;
        }
        
        // IMPORTANT: If we're in paused state, use the saved task time directly
        // Mock mode should show the saved time when paused
        if (isPaused) {
            // In mock mode with paused state - always use saved values and show exact minutes
            if (taskTimeBeforePauseMinutes > 0) {
                Log.d(TAG, "In paused state with saved task minutes: " + taskTimeBeforePauseMinutes + "m");
                binding.currentTaskElapsedTime.setText("Elapsed time of the current task: " + taskTimeBeforePauseMinutes + "m");
            } else if (taskSecondsBeforePause > 0) {
                // For tasks under a minute, show seconds
                int roundedSeconds = (int)(taskSecondsBeforePause / 5) * 5; // Round to nearest 5 seconds
                Log.d(TAG, "In paused state with saved task seconds: " + taskSecondsBeforePause + 
                      "s (rounded to " + roundedSeconds + "s)");
                binding.currentTaskElapsedTime.setText("Elapsed time of the current task: " + roundedSeconds + "s");
                return;
            }
            // If we don't have saved task time but we're paused, we should compute and save it now
            if (isPaused && (taskTimeBeforePauseMinutes == 0 && taskSecondsBeforePause == 0)) {
                saveCurrentTaskElapsedTime();
                Log.d(TAG, "Saved task time on demand: " + 
                    (taskTimeBeforePauseMinutes > 0 ? taskTimeBeforePauseMinutes + "m" : 
                    (taskSecondsBeforePause > 0 ? taskSecondsBeforePause + "s" : "0s")));
                
                // Now try to display saved values again
                if (taskTimeBeforePauseMinutes > 0) {
                    binding.currentTaskElapsedTime.setText("Elapsed time of the current task: " + taskTimeBeforePauseMinutes + "m");
                } else if (taskSecondsBeforePause > 0) {
                    int roundedSeconds = (int)(taskSecondsBeforePause / 5) * 5;
                    binding.currentTaskElapsedTime.setText("Elapsed time of the current task: " + roundedSeconds + "s");
                } else {
                    binding.currentTaskElapsedTime.setText("Elapsed time of the current task: 0s");
                }
                
                // Now return to avoid continuing with time calculation which could be incorrect in paused state
                return;
            }
            Log.d(TAG, "In paused state but no saved task time values found - calculating normally");
        }
        
        // Calculate elapsed time
        long elapsedTimeSeconds = 0;
        LocalDateTime taskStart = null;
        
        // Try to get task timer start time
        if (currentRoutine.getTaskTimer() != null) {
            taskStart = currentRoutine.getTaskTimer().getStartTime();
            Log.d(TAG, "Task timer start time: " + taskStart);
        }
        
        // If task timer isn't initialized but routine timer is, use routine start time
        if (taskStart == null && currentRoutine.getRoutineTimer() != null && 
            currentRoutine.getRoutineTimer().getStartTime() != null) {
            taskStart = currentRoutine.getRoutineTimer().getStartTime();
            Log.d(TAG, "Using routine timer start time: " + taskStart);
            
            // Force start the task timer if needed
            if (currentRoutine.getTaskTimer() != null && !currentRoutine.getTaskTimer().isRunning() && 
                currentRoutine.isActive() && isTimerRunning && !isPaused) {
                currentRoutine.getTaskTimer().start(taskStart);
                Log.d(TAG, "Started task timer with routine start time");
            }
        }
        
        // If we still don't have a valid start time, show initial elapsed time as 0m
        if (taskStart == null) {
            Log.d(TAG, "No valid start time found, showing 0m");
            binding.currentTaskElapsedTime.setText("Elapsed time of the current task: 0m");
            return;
        }
        
        // Calculate elapsed time based on timer state
        LocalDateTime now = currentRoutine.getCurrentTime();
        LocalDateTime currentDateTime = LocalDateTime.now();
        
        Log.d(TAG, "Current routine time: " + now);
        Log.d(TAG, "Current wall time: " + currentDateTime);
        
        // Always use the more accurate time based on routine state
        // - If timer is stopped via Stop Timer OR paused via Pause Button, use routine's current time
        // - Otherwise use the actual wall clock time
        if (isPaused || isStopTimerPressed || !isTimerRunning) {
            // In mock mode or when paused, use the current time from the routine
            if (now != null && taskStart != null) {
                elapsedTimeSeconds = java.time.Duration.between(taskStart, now).getSeconds();
                Log.d(TAG, "Using routine's current time for elapsed time calculation: " + now);
            } else {
                // Fallback to the saved times
                elapsedTimeSeconds = taskSecondsBeforePause > 0 ? taskSecondsBeforePause : taskTimeBeforePauseMinutes * 60;
                Log.d(TAG, "Using fallback saved time: " + elapsedTimeSeconds + "s");
            }
        } else {
            // If timer is running normally, use the current wall time
            elapsedTimeSeconds = java.time.Duration.between(taskStart, currentDateTime).getSeconds();
            Log.d(TAG, "Using wall clock time for elapsed time calculation: " + currentDateTime);
        }
        
        // Ensure non-negative time
        elapsedTimeSeconds = Math.max(0, elapsedTimeSeconds);
        
        String timeDisplay;
        
        // For running tasks less than a minute, display in 5-second increments
        if (elapsedTimeSeconds < 60) {
            // Round DOWN to nearest 5 seconds for running timer
            int roundedSeconds = (int)(elapsedTimeSeconds / 5) * 5;
            
            timeDisplay = roundedSeconds + "s";
            Log.d(TAG, "Showing seconds: " + roundedSeconds + "s (original: " + elapsedTimeSeconds + "s) [ROUNDED DOWN]");
        } else {
            // For tasks over a minute
            long elapsedMinutes;
            
            // If we're in mock mode with saved task time, use that value directly
            if (isStopTimerPressed && taskTimeBeforePauseMinutes > 0) {
                elapsedMinutes = taskTimeBeforePauseMinutes;
                Log.d(TAG, "Using saved task time in mock mode: " + elapsedMinutes + "m");
            } else {
                // Apply rounding based on the task/routine state
                // If task timer is running, round DOWN (floor)
                // If task timer has ended, round UP (ceil)
                if (currentRoutine.getTaskTimer() != null && currentRoutine.getTaskTimer().isRunning()) {
                    // For running timers, round DOWN as per requirement
                    elapsedMinutes = elapsedTimeSeconds / 60;
                    Log.d(TAG, "Task timer running - using FLOOR rounding: " + elapsedMinutes + "m");
                } else {
                    // For ended timers, round UP as per requirement
                    elapsedMinutes = (long)Math.ceil(elapsedTimeSeconds / 60.0);
                    Log.d(TAG, "Task timer ended - using CEILING rounding: " + elapsedMinutes + "m");
                }
            }
            
            // Ensure non-negative time
            elapsedMinutes = Math.max(0, elapsedMinutes);
            timeDisplay = elapsedMinutes + "m";
        }
        
        // Update the text view with the final result
        binding.currentTaskElapsedTime.setText("Elapsed time of the current task: " + timeDisplay);
        
        // Save the current task elapsed time for potential app restarts
        if (routineStateManager != null && (System.currentTimeMillis() % 10 == 0)) {
            // Only save occasionally to reduce overhead (roughly every 10 seconds)
            saveRoutineState();
            Log.d(TAG, "Saved routine state with current task elapsed time");
        }
    }

    /**
     * Save current task elapsed time when pausing
     */
    private void saveCurrentTaskElapsedTime() {
        final String TAG = "SAVE_TASK_TIME";
        
        // Early exit if no routine
        if (currentRoutine == null) return;
        
        // Find the first uncompleted task (current active task)
        List<Task> tasks = currentRoutine.getTasks();
        if (tasks.isEmpty()) return;
        
        // First try to find a task that is not completed and not skipped
        Task currentTask = null;
        for (Task task : tasks) {
            if (!task.isCompleted() && !task.isSkipped()) {
                currentTask = task;
                Log.d(TAG, "Found active task: " + task.getTaskName());
                break;
            }
        }
        
        // If not found, try to find first unchecked task
        if (currentTask == null) {
            for (Task task : tasks) {
                if (!task.isCheckedOff()) {
                    currentTask = task;
                    Log.d(TAG, "Found active task (not checked off): " + task.getTaskName());
                    break;
                }
            }
        }
        
        // If still not found, just use the first task
        if (currentTask == null && !tasks.isEmpty()) {
            currentTask = tasks.get(0);
            Log.d(TAG, "Using first task as fallback: " + currentTask.getTaskName());
        }
        
        if (currentTask == null) {
            Log.d(TAG, "No current task found even after fallbacks");
            return;
        }
        
        // Get the task start time from either task timer or routine timer
        LocalDateTime taskStart = null;
        if (currentRoutine.getTaskTimer() != null) {
            taskStart = currentRoutine.getTaskTimer().getStartTime();
            Log.d(TAG, "Using task timer start time: " + taskStart);
        }
        
        if (taskStart == null && currentRoutine.getRoutineTimer() != null) {
            taskStart = currentRoutine.getRoutineTimer().getStartTime();
            Log.d(TAG, "Using routine timer start time: " + taskStart);
        }
        
        if (taskStart == null) {
            Log.d(TAG, "No valid start time found");
            return;
        }
        
        // Use current wall time for accurate calculation BEFORE pausing
        LocalDateTime now;
        if (isStopTimerPressed) {
            // In mock mode, use the current routine time if available
            now = currentRoutine.getCurrentTime();
            if (now == null) {
                now = LocalDateTime.now();
            }
            Log.d(TAG, "Using current routine time for mock mode: " + now);
        } else {
            now = LocalDateTime.now();
            Log.d(TAG, "Using current wall time for calculation: " + now);
        }
        
        // Calculate elapsed time
        long elapsedTimeSeconds = java.time.Duration.between(taskStart, now).getSeconds();
        
        // Ensure non-negative time
        elapsedTimeSeconds = Math.max(0, elapsedTimeSeconds);
        
        // Reset previous values before saving new ones
        taskTimeBeforePauseMinutes = 0;
        taskSecondsBeforePause = 0;
        
        // Save appropriate values based on duration
        if (elapsedTimeSeconds < 60) {
            taskSecondsBeforePause = (int)elapsedTimeSeconds;
            Log.d(TAG, "Saved task time before pause: " + taskSecondsBeforePause + "s");
        } else {
            taskTimeBeforePauseMinutes = elapsedTimeSeconds / 60;
            Log.d(TAG, "Saved task time before pause: " + taskTimeBeforePauseMinutes + "m");
        }
        
        // Save to SharedPreferences too (only if routine is active)
        if (routineStateManager != null && (currentRoutine.isActive() || manuallyStarted)) {
            saveRoutineState();
            Log.d(TAG, "Saved task time to SharedPreferences");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("RoutineFragment", "onSaveInstanceState called");
        
        // Save the key state values
        if (currentRoutine != null) {
            outState.putInt("ROUTINE_ID", currentRoutine.getRoutineId());
            outState.putBoolean("MANUALLY_STARTED", manuallyStarted);
            outState.putBoolean("IS_TIMER_RUNNING", isTimerRunning);
            outState.putBoolean("IS_PAUSED", isPaused);
            Log.d("RoutineFragment", "Saved state: routineId=" + currentRoutine.getRoutineId() + 
                  ", manuallyStarted=" + manuallyStarted + 
                  ", isTimerRunning=" + isTimerRunning + 
                  ", isPaused=" + isPaused);
        }
    }

    /**
     * Save the current routine state to shared preferences
     */
    private void saveRoutineState() {
        if (currentRoutine == null) return;
        
        // Only save state if it's an active/interesting routine
        if (currentRoutine.isActive() || manuallyStarted) {
            // Find the current task index (for UI restoration)
            int currentTaskIndex = 0;
            List<Task> tasks = currentRoutine.getTasks();
            if (tasks != null && !tasks.isEmpty()) {
                // Find the first non-completed task as current
                for (int i = 0; i < tasks.size(); i++) {
                    if (!tasks.get(i).isCompleted()) {
                        currentTaskIndex = i;
                        break;
                    }
                }
            }
            
            // Get the current task elapsed time
            int currentTaskElapsedTime = 0;
            if (currentRoutine.getTaskTimer() != null && 
                currentRoutine.getTaskTimer().isRunning()) {
                // Get current task elapsed time in seconds
                if (isPaused && taskSecondsBeforePause > 0) {
                    // Use stored seconds if we're paused
                    currentTaskElapsedTime = taskSecondsBeforePause;
                } else {
                    // Otherwise calculate it from the timer
                    currentTaskElapsedTime = currentRoutine.getTaskTimer().getElapsedSeconds();
                }
            }
            
            // Make sure current time is updated in the routine if paused
            if (isPaused) {
                // Ensure the routine knows it's paused (check safely for isTimerStopped)
                try {
                    if (!currentRoutine.isTimerStopped()) {
                        currentRoutine.pauseTime(LocalDateTime.now());
                        Log.d("RoutineFragment", "Updated pause time in routine during saveRoutineState");
                    }
                } catch (Exception e) {
                    // If method doesn't exist or another error occurs, just pause anyway
                    currentRoutine.pauseTime(LocalDateTime.now());
                    Log.e("RoutineFragment", "Error checking timer stopped state: " + e.getMessage());
                }
            }
            
            // Save full state including UI state
            routineStateManager.saveFullRoutineState(
                currentRoutine,
                isTimerRunning,
                isPaused,
                manuallyStarted,
                isStopTimerPressed,
                timeBeforePauseMinutes,
                taskTimeBeforePauseMinutes,
                taskSecondsBeforePause,
                currentTaskIndex,
                currentTaskElapsedTime
            );
            
            Log.d("RoutineFragment", "Saved routine state: " + 
                  "isTimerRunning=" + isTimerRunning +
                  ", isPaused=" + isPaused + 
                  ", manuallyStarted=" + manuallyStarted +
                  ", isTimerStopped=" + isStopTimerPressed +
                  ", currentTaskElapsedTime=" + currentTaskElapsedTime + "s" +
                  ", goalTime=" + (currentRoutine.getGoalTime() != null ? currentRoutine.getGoalTime() : "null"));
        } else {
            // Clear state for inactive routines
            routineStateManager.clearRunningRoutineState();
            Log.d("RoutineFragment", "Cleared routine state as routine is inactive");
        }
    }

    /**
     * Restore saved UI state from RoutineStateManager
     */
    private void restoreSavedUIState() {
        if (currentRoutine == null) return;
        
        // Check if this is the active routine that was saved
        if (routineStateManager.hasRunningRoutine() && 
            routineStateManager.getRunningRoutineId() == currentRoutine.getRoutineId()) {
            
            Log.d("RoutineFragment", "Restoring UI state for active routine: " + currentRoutine.getRoutineName());
            
            // First restore the routine timer state
            routineStateManager.restoreRoutineState(currentRoutine);
            
            // Then restore UI state
            RoutineStateManager.RoutineUIState uiState = routineStateManager.getUIState();
            if (uiState != null) {
                // Keep manual start status and time values, but ALWAYS pause the routine on restart
                manuallyStarted = uiState.isManuallyStarted;
                
                // Explicitly set the time before pause to the saved elapsed minutes
                // Use the saved value if available, otherwise use the current duration
                if (uiState.elapsedMinutes > 0) {
                    timeBeforePauseMinutes = uiState.elapsedMinutes;
                    Log.d("RoutineFragment", "Loaded elapsed time from saved state: " + timeBeforePauseMinutes + "m");
                } else {
                    timeBeforePauseMinutes = currentRoutine.getRoutineDurationMinutes();
                    Log.d("RoutineFragment", "Using current routine duration as elapsed time: " + timeBeforePauseMinutes + "m");
                }
                
                // Load task time values
                taskTimeBeforePauseMinutes = uiState.taskTimeBeforePauseMinutes;
                
                // Explicitly set the task seconds before pause to the saved value
                if (uiState.currentTaskElapsedTime > 0) {
                    taskSecondsBeforePause = uiState.currentTaskElapsedTime;
                    Log.d("RoutineFragment", "Loaded task elapsed time from saved state: " + taskSecondsBeforePause + "s");
                } else if (uiState.taskSecondsBeforePause > 0) {
                    taskSecondsBeforePause = uiState.taskSecondsBeforePause;
                    Log.d("RoutineFragment", "Loaded task seconds from saved state: " + taskSecondsBeforePause + "s");
                }
                
                // Force pause state regardless of previous state
                isPaused = true;
                isTimerRunning = false;
                isStopTimerPressed = false;
                
                // Save the current time for the pause
                LocalDateTime pauseTime = LocalDateTime.now();
                currentRoutine.pauseTime(pauseTime);
                
                // Restore goal time if available
                if (uiState.goalTime != null) {
                    // Goal time is already restored in the routine object by restoreRoutineState
                    // We just need to update the UI
                    updateRoutineGoalDisplay(currentRoutine.getGoalTime());
                    Log.d("RoutineFragment", "Restored goal time: " + currentRoutine.getGoalTime());
                }
                
                Log.d("RoutineFragment", "Restored routine is automatically paused. Previous state was: " +
                      "isTimerRunning=" + uiState.isTimerRunning +
                      ", isPaused=" + uiState.isPaused +
                      ", manuallyStarted=" + manuallyStarted);
                
                // If the routine has tasks, update the adapter to show the current state
                List<Task> tasks = currentRoutine.getTasks();
                if (tasks != null && !tasks.isEmpty()) {
                    // Update the adapter with the current tasks
                    taskAdapter.clear();
                    taskAdapter.addAll(tasks);
                    taskAdapter.notifyDataSetChanged();
                    
                    // Log current task status
                    for (int i = 0; i < tasks.size(); i++) {
                        Task task = tasks.get(i);
                        Log.d("RoutineFragment", "Task " + i + ": " + task.getTaskName() + 
                              " - completed: " + task.isCompleted() + 
                              ", skipped: " + task.isSkipped() + 
                              ", duration: " + task.getDuration() + 
                              ", elapsedSeconds: " + task.getElapsedSeconds());
                    }
                    
                    Log.d("RoutineFragment", "Current task index from saved state: " + uiState.currentTaskIndex);
                    
                    // Restore task elapsed time
                    if (uiState.currentTaskElapsedTime > 0 && !tasks.get(uiState.currentTaskIndex).isCompleted()) {
                        taskSecondsBeforePause = uiState.currentTaskElapsedTime;
                        Log.d("RoutineFragment", "Restored current task elapsed time: " + taskSecondsBeforePause + "s");
                    }
                }
                
                // Force update the UI
                updateTimeDisplay();
                updateCurrentTaskElapsedTime();
                
                // Update UI buttons to reflect pause state
                updatePauseButtonStatus();
                
                // Do NOT start the timer as we want it paused
                // Instead, update the UI to reflect the paused state
                binding.pauseButton.setText("Resume");
                
                // IMPORTANT: Disable the home button since the routine is active
                binding.homeButton.setEnabled(false);
                Log.d("RoutineFragment", "Home button disabled for active routine on restart");
                
                // Explicitly update the elapsed time display with the restored value
                binding.actualTime.setText(timeBeforePauseMinutes + "m");
                
                // Update the task elapsed time display
                if (taskSecondsBeforePause > 0) {
                    // Format the time display based on saved task seconds
                    int minutes = taskSecondsBeforePause / 60;
                    int seconds = taskSecondsBeforePause % 60;
                    
                    // Display the time in the right format based on duration
                    if (minutes > 0) {
                        binding.currentTaskElapsedTime.setText(String.format("%d:%02d", minutes, seconds));
                    } else {
                        binding.currentTaskElapsedTime.setText(String.format("%ds", seconds));
                    }
                    
                    Log.d("RoutineFragment", "Updated task elapsed time display: " + 
                          (minutes > 0 ? String.format("%d:%02d", minutes, seconds) : String.format("%ds", seconds)));
                }
                
                // Save the state with the new pause info
                saveRoutineState();
            }
        }
    }
    
    /**
     * Update pause button status based on current state
     */
    private void updatePauseButtonStatus() {
        if (binding == null) return;
        
        if (isPaused) {
            binding.pauseButton.setText("Resume");
        } else {
            binding.pauseButton.setText("Pause");
        }
        
        // Enable/disable the pause button based on routine state
        boolean shouldEnablePauseButton = currentRoutine != null && 
                                         currentRoutine.isActive() && 
                                         manuallyStarted && 
                                         !currentRoutine.getTasks().isEmpty();
                                         
        binding.pauseButton.setEnabled(shouldEnablePauseButton);
    }
}