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
import edu.ucsd.cse110.habitizer.lib.domain.timer.Timer;
import edu.ucsd.cse110.habitizer.lib.domain.timer.TaskTimer;

import edu.ucsd.cse110.habitizer.app.util.RoutineStateManager;

public class RoutineFragment extends Fragment {
    private static final String TAG = "RoutineFragment";
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
                
                // IMPORTANT: In mock mode, save the current mock time as the routine time
                // This ensures the correct time is shown when the routine ends
                if (isStopTimerPressed && timeBeforePauseMinutes > 0) {
                    // Before ending, record the current mock time in the routine
                    long routineDurationSeconds = timeBeforePauseMinutes * 60;
                    
                    // IMPORTANT: When ending routine in mock mode after a restart, add an additional 1 minute
                    boolean isAfterRestart = isPaused; // If we're in paused state, it means we're after a restart
                    if (isAfterRestart) {
                        Log.d("EndRoutine", "Adding 1 additional minute to routine duration since we're ending after a restart");
                        routineDurationSeconds += 60; // Add 60 seconds (1 minute)
                        timeBeforePauseMinutes += 1; // Update timeBeforePauseMinutes for consistency
                    }
                    
                    LocalDateTime mockStartTime = LocalDateTime.now().minusSeconds(routineDurationSeconds);
                    LocalDateTime mockEndTime = LocalDateTime.now();
                    
                    // Adjust the routine's timer to match our mock values
                    if (currentRoutine.getRoutineTimer() != null) {
                        currentRoutine.getRoutineTimer().updateStartTime(mockStartTime);
                        Log.d("EndRoutine", "Updated routine start time to match mock duration: " + 
                               mockStartTime + " (for " + timeBeforePauseMinutes + "m)");
                    }
                    
                    Log.d("EndRoutine", "Ending routine in mock mode with saved time: " + 
                           timeBeforePauseMinutes + "m" + (isAfterRestart ? " (including +1m bonus)" : ""));
                }
                
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
                
                // Clear the task elapsed time display when routine ends
                binding.currentTaskElapsedTime.setText("");
                Log.d("RoutineFragment", "Cleared task elapsed time display when ending routine");
                
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
                
                // Store current time for consistent updates
                LocalDateTime mockStartTime = LocalDateTime.now();
                
                // Stop real timer functionality
                if (currentRoutine.isActive()) {
                    // Save the current time before switching to mock
                    // CRITICAL FIX: Always get the current routine duration when entering mock mode
                    // This ensures we start from the correct time on first entry
                    timeBeforePauseMinutes = currentRoutine.getRoutineDurationMinutes();
                    Log.d("MockMode-DEBUG", "ENTERING MOCK MODE: Setting initial timeBeforePauseMinutes = " + 
                          timeBeforePauseMinutes + "m from routine duration");
                    
                    // IMPORTANT: Make sure task timer is completely stopped when entering mock mode
                    if (currentRoutine.getTaskTimer() != null) {
                        // Get the actual elapsed time before stopping the timer
                        long elapsedSeconds = 0;
                        if (currentRoutine.getTaskTimer() instanceof TaskTimer) {
                            TaskTimer specificTaskTimer = (TaskTimer) currentRoutine.getTaskTimer();
                            elapsedSeconds = specificTaskTimer.getElapsedSecondsRoundedDown();
                            Log.d("MockMode", "Using actual elapsed time (rounded down): " + elapsedSeconds + "s");
                        } else {
                            // Fallback if casting fails - manually round down
                            elapsedSeconds = currentRoutine.getTaskTimer().getElapsedSeconds();
                            elapsedSeconds = (elapsedSeconds / 5) * 5; // Manual round down to nearest 5s
                            Log.d("MockMode", "Manual rounding DOWN to: " + elapsedSeconds + "s");
                        }
                        
                        // End the timer first
                        if (currentRoutine.getTaskTimer().isRunning()) {
                            Log.d("MockMode", "Explicitly stopping task timer when entering mock mode");
                            currentRoutine.getTaskTimer().end(mockStartTime);
                        }
                        
                        // Explicitly reset internal state to ensure timer doesn't continue running in background
                        try {
                            // Force reset the timer's state by stopping and starting it at the current time
                            currentRoutine.getTaskTimer().end(mockStartTime); // Make sure it's stopped
                            currentRoutine.getTaskTimer().start(mockStartTime); // Create a fresh timer
                            currentRoutine.getTaskTimer().end(mockStartTime); // Then immediately stop it
                            
                            Log.d("MockMode", "Full task timer reset performed to ensure clean state");
                        } catch (Exception e) {
                            Log.e("MockMode", "Error while resetting task timer: " + e.getMessage());
                        }
                        
                        // Store the elapsed time into our mock mode variables
                        if (elapsedSeconds >= 60) {
                            taskTimeBeforePauseMinutes = elapsedSeconds / 60;
                            taskSecondsBeforePause = (int)(elapsedSeconds % 60);
                            Log.d("MockMode", "Storing elapsed time in minutes and seconds: " + 
                                taskTimeBeforePauseMinutes + "m " + taskSecondsBeforePause + "s");
                        } else {
                            taskTimeBeforePauseMinutes = 0;
                            taskSecondsBeforePause = (int)elapsedSeconds;
                            Log.d("MockMode", "Storing elapsed time in seconds only: " + taskSecondsBeforePause + "s");
                        }
                    }
                    
                    // Pause at current simulated time, but don't affect the pause state
                    // This allows separate tracking of pause button vs stop timer button
                    currentRoutine.pauseTime(mockStartTime);
                    
                    // Save the current task's elapsed time for the mock mode
                    // IMPORTANT: Save this AFTER stopping the timer to get accurate values
                    saveCurrentTaskElapsedTime();
                    
                    // We don't need to reset to 15 seconds since we're now using the actual elapsed time
                    // The values have already been set correctly in the timer stop code above
                    
                    Log.d("MockMode", "Entered mock mode - task timer running: " + 
                          (currentRoutine.getTaskTimer() != null ? currentRoutine.getTaskTimer().isRunning() : "null timer") +
                          ", using actual elapsed time: " + 
                          (taskTimeBeforePauseMinutes > 0 ? 
                            taskTimeBeforePauseMinutes + "m" : 
                            taskSecondsBeforePause + "s"));
                    
                    // IMPORTANT: Update task adapter with mock mode status
                    if (taskAdapter != null) {
                        taskAdapter.setMockModeActive(true);
                        Log.d("MockMode", "Updated TaskAdapter with mock mode status");
                    }
                    
                    updateTimeDisplay();
                    updateCurrentTaskElapsedTime();
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
                    
                    // Get total seconds by combining minutes and seconds
                    long totalSeconds = taskTimeBeforePauseMinutes * 60 + taskSecondsBeforePause + 15;
                    
                    // Update minutes and seconds
                    taskTimeBeforePauseMinutes = totalSeconds / 60;
                    taskSecondsBeforePause = (int)(totalSeconds % 60);
                    
                    Log.d("AdvanceTimer", "Advanced task timer: original=" + 
                          (totalSeconds - 15) + "s, new=" + totalSeconds + "s (" +
                          taskTimeBeforePauseMinutes + "m " + taskSecondsBeforePause + "s)");

                    // Also update the timeBeforePauseMinutes to move the routine timer forward
                    // This is crucial for updating the routine time display after restart
                    if (timeBeforePauseMinutes > 0) {
                        // Save the original value for logging
                        long oldValue = timeBeforePauseMinutes;
                        
                        // CRITICAL FIX: Use a more explicit calculation to avoid integer division issues
                        // Convert minutes to seconds, add 15, convert back to minutes
                        totalSeconds = timeBeforePauseMinutes * 60;
                        totalSeconds += 15;
                        timeBeforePauseMinutes = totalSeconds / 60;
                        
                        Log.d("AdvanceTimer", "Advanced mock time increments: old=" + oldValue + 
                              "m, new=" + timeBeforePauseMinutes + "m (added 15s)");
                        
                        // Get the routine's actual duration for calculating the combined time
                        long routineDuration = currentRoutine.getRoutineDurationMinutes();
                        long combinedTime = routineDuration + timeBeforePauseMinutes;
                        
                        // CRITICAL FIX: Force an update to the UI with the combined value
                        binding.actualTime.setText(String.valueOf(combinedTime) + "m");
                        
                        // Log the combined time that will be displayed
                        Log.d("MockMode-DEBUG", "ADVANCE TIMER: Combined time for display: " + combinedTime + 
                              "m (routineDuration=" + routineDuration + "m + mockTimeIncrements=" + 
                              timeBeforePauseMinutes + "m)");
                    }
                    
                    // Force immediate UI update
                    updateTimeDisplay();
                    
                    // CRITICAL FIX: Save the updated state to ensure it persists across app restarts
                    // This is the key fix - we must save state AFTER updating timeBeforePauseMinutes
                    saveRoutineState();
                    
                    // Verify what values were actually saved by immediately retrieving them
                    if (routineStateManager != null) {
                        RoutineStateManager.RoutineUIState savedState = routineStateManager.getUIState();
                        if (savedState != null) {
                            Log.d("MockMode-DEBUG", "VERIFICATION: Saved values - timeBeforePauseMinutes: " + 
                                  savedState.timeBeforePauseMinutes + "m, elapsedMinutes: " + 
                                  savedState.elapsedMinutes + "m");
                        }
                    }
                    
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
                    
                    // IMPORTANT: Explicitly end the task timer before pausing the routine
                    // This ensures the task timer is properly paused
                    if (currentRoutine.getTaskTimer() != null && currentRoutine.getTaskTimer().isRunning()) {
                        Log.d("PauseButton", "Explicitly ending task timer before pausing routine");
                        currentRoutine.getTaskTimer().end(pauseTime);
                    }
                    
                    // Now pause the routine
                    currentRoutine.pauseTime(pauseTime);
                    
                    // Log the state after pausing
                    Log.d("PauseButton", "After pause - Routine duration: " + 
                          currentRoutine.getRoutineDurationMinutes() + "m, " +
                          "Current time: " + currentRoutine.getCurrentTime() + 
                          ", Task timer running: " + 
                          (currentRoutine.getTaskTimer() != null ? currentRoutine.getTaskTimer().isRunning() : "null timer"));
                    
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
                    
                    // DEBUG: Log the timeBeforePauseMinutes value being used for the display
                    Log.d("MockMode-DEBUG", "RESUME IN MOCK MODE - timeBeforePauseMinutes: " + timeBeforePauseMinutes + 
                          ", taskTimeBeforePauseMinutes: " + taskTimeBeforePauseMinutes + 
                          ", taskSecondsBeforePause: " + taskSecondsBeforePause);
                    
                    // IMPORTANT: In mock mode, DO NOT reset task time values
                    // This was causing the timer to reset after resume
                    // We want to keep the current mock values to continue from where we left off
                    Log.d("PauseButton", "Preserving mock mode timer values after resume: " +
                          taskTimeBeforePauseMinutes + "m " + taskSecondsBeforePause + "s");
                    
                    // Force update displays with current times
                    updateTimeDisplay();
                    updateCurrentTaskElapsedTime();
                    
                    // Force an explicit update of the routine time text to ensure it's displaying correctly
                    if (timeBeforePauseMinutes > 0) {
                        binding.actualTime.setText(String.valueOf(timeBeforePauseMinutes) + "m");
                        Log.d("MockMode-DEBUG", "Forcing UI update with timeBeforePauseMinutes: " + timeBeforePauseMinutes + "m");
                    }
                    
                    // IMPORTANT: Make sure we refresh adapter to enable task checkboxes again
                    if (taskAdapter != null) {
                        taskAdapter.setMockModeActive(true); // Signal adapter that we're in mock mode
                        taskAdapter.notifyDataSetChanged();
                        Log.d("PauseButton", "Refreshed task adapter after resuming in mock mode to enable checkboxes");
                    }
                    
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
                    
                    // Explicitly restart the task timer after resuming with the correct elapsed time
                    if (currentRoutine.getTaskTimer() != null && !currentRoutine.getTaskTimer().isRunning()) {
                        // Calculate total saved elapsed seconds by combining minutes and seconds
                        long totalElapsedSeconds = taskTimeBeforePauseMinutes * 60 + taskSecondsBeforePause;
                        
                        // Round down to nearest 5 seconds for consistency
                        totalElapsedSeconds = (totalElapsedSeconds / 5) * 5;
                        
                        Log.d("PauseButton", "Resuming task timer with total elapsed seconds: " + 
                               totalElapsedSeconds + " (from " + taskTimeBeforePauseMinutes + 
                               "m and " + taskSecondsBeforePause + "s)");
                        
                        // If we have saved elapsed time, adjust the start time accordingly
                        if (totalElapsedSeconds > 0) {
                            LocalDateTime adjustedStartTime = resumeTime.minusSeconds(totalElapsedSeconds);
                            Log.d("PauseButton", "Starting task timer with adjusted start time: " + 
                                   adjustedStartTime + " (total elapsed seconds: " + totalElapsedSeconds + ")");
                            currentRoutine.getTaskTimer().start(adjustedStartTime);
                        } else {
                            // Just start with current time if no elapsed time
                            Log.d("PauseButton", "Starting task timer with current time");
                            currentRoutine.getTaskTimer().start(resumeTime);
                        }
                    }
                    
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

        // In mock mode, always preserve the original time regardless of pause state
        if (isStopTimerPressed) {
            // CRITICAL FIX: In mock mode, ALWAYS combine routine duration with the mock time
            // Get the routine's actual duration (from timer)
            long routineDuration = currentRoutine.getRoutineDurationMinutes();
            
            // Calculate the total time to display by combining both values
            long combinedTimeMinutes = routineDuration + timeBeforePauseMinutes;
            
            // Log the time values for debugging
            Log.d("MockMode-DEBUG", "COMBINED TIME VALUES: routineDuration=" + routineDuration + 
                  "m + timeBeforePauseMinutes=" + timeBeforePauseMinutes + 
                  "m = combinedTimeMinutes=" + combinedTimeMinutes + "m");
            
            // Always display the combined time in mock mode
            binding.actualTime.setText(String.valueOf(combinedTimeMinutes) + "m");
            Log.d("MockMode-DEBUG", "DISPLAY UPDATE: Showing combined time: " + combinedTimeMinutes + 
                  "m in mock mode (routineDuration=" + routineDuration + 
                  "m, timeBeforePauseMinutes=" + timeBeforePauseMinutes + "m)");
            
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
        
        // IMPORTANT: In mock mode, preserve the mock time when ending the routine
        if (isStopTimerPressed && timeBeforePauseMinutes > 0) {
            // If in mock mode, make sure we preserve the mock time for display
            Log.d("RoutineFragment", "Auto-ending routine in mock mode with time: " + timeBeforePauseMinutes + "m");
            
            // Check if we're after a restart (indicated by paused state)
            boolean isAfterRestart = isPaused;
            if (isAfterRestart) {
                // Add 1 additional minute for consistency with manual ending
                timeBeforePauseMinutes += 1;
                Log.d("RoutineFragment", "Adding +1 minute bonus for restarted mock mode, new time: " + 
                      timeBeforePauseMinutes + "m");
            }
            
            // Make sure to update the display with the mock time directly
            binding.actualTime.setText(timeBeforePauseMinutes + "m");
        }
        
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
        
        // Clear the elapsed time display when routine ends
        binding.currentTaskElapsedTime.setText("");
        
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
                Log.d("RoutineFragment", "Saved routine state to repository");
            } else {
                Log.d("RoutineFragment", "Morning routine with ID 0 - not saving to repository to prevent duplication");
            }
        }
    }

    // Reset task time in mock mode when a task is checked off
    public void resetTaskTimeInMockMode() {
        // Only reset time if in mock mode
        if (isStopTimerPressed) {
            Log.d("TIMER_DEBUG", "Task checked off in mock mode - resetting task time to 0");
            // Reset the task time values
            taskTimeBeforePauseMinutes = 0;
            taskSecondsBeforePause = 0;
            
            // Update the display immediately to show the reset
            updateCurrentTaskElapsedTime();
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
        
        // Reset stop timer (mock mode) state if it was active
        boolean wasMockMode = isStopTimerPressed;
        if (isStopTimerPressed) {
            isStopTimerPressed = false;
            binding.stopTimerButton.setText("Switch to Mock");
            binding.stopTimerButton.setBackground(getResources().getDrawable(R.drawable.rounded_button_background));
            
            // Also reset the mock mode flag in TaskAdapter
            if (taskAdapter != null) {
                taskAdapter.setMockModeActive(false);
                Log.d("MockMode", "Reset mock mode flag in TaskAdapter when exiting mock mode");
            }
            
            // If we're exiting mock mode and the routine is active, we need to restart the real timers
            if (currentRoutine != null && currentRoutine.isActive() && !wasPaused) {
                LocalDateTime now = LocalDateTime.now();
                Log.d("MockMode", "Exiting mock mode - restarting real timers at " + now);
                
                // Calculate adjusted start time based on the elapsed time in mock mode
                long elapsedSeconds = 0;
                if (taskTimeBeforePauseMinutes > 0) {
                    elapsedSeconds = taskTimeBeforePauseMinutes * 60;
                } else if (taskSecondsBeforePause > 0) {
                    // Round down to nearest 5 seconds for consistency
                    elapsedSeconds = (taskSecondsBeforePause / 5) * 5;
                }
                
                // Restart the routine timer with the current time
                currentRoutine.resumeTime(now);
                
                // If we have accurate task elapsed time, restart the task timer with an adjusted start time
                if (elapsedSeconds > 0 && currentRoutine.getTaskTimer() != null) {
                    // Calculate an adjusted start time based on elapsed seconds
                    LocalDateTime adjustedStartTime = now.minusSeconds(elapsedSeconds);
                    
                    // Restart the task timer with the adjusted start time
                    currentRoutine.getTaskTimer().start(adjustedStartTime);
                    Log.d("MockMode", "Restarted task timer with adjusted start time: " + 
                          adjustedStartTime + " (elapsed seconds: " + elapsedSeconds + ")");
                }
            }
        }
        
        // Refresh task list if any state changed
        if ((wasPaused || wasMockMode) && taskAdapter != null) {
            taskAdapter.notifyDataSetChanged();
        }
    }

    // Add this getter method for the pause state
    public boolean isPaused() {
        return isPaused;
    }

    /**
     * Update the display of the current task's elapsed time
     */
    private void updateCurrentTaskElapsedTime() {
        Log.d("TIMER_DEBUG", "======= UPDATE CURRENT TASK ELAPSED TIME =======");
        logTimerState("UPDATE_TASK_TIME - Start of method");
        
        // Don't show elapsed time if routine has ended
        if (isRoutineEnded()) {
            Log.d("TIMER_DEBUG", "Routine has ended, hiding elapsed time display");
            binding.currentTaskElapsedTime.setText("");
            return;
        }
        
        // Get the current task
        Task currentTask = getCurrentTask();
        if (currentTask == null) {
            Log.d("TIMER_DEBUG", "No current task, returning");
            return;
        }
        
        // Log the current saved values
        Log.d("TIMER_DEBUG", "Current saved values - taskTimeBeforePauseMinutes: " + 
              taskTimeBeforePauseMinutes + ", taskSecondsBeforePause: " + taskSecondsBeforePause);
        Log.d("TIMER_DEBUG", "isPaused: " + isPaused + ", isTimerRunning: " + isTimerRunning + 
              ", isStopTimerPressed (mock mode): " + isStopTimerPressed);
        
        // Check if we're in mock mode
        if (isStopTimerPressed) {
            Log.d("TIMER_DEBUG", "In mock mode, using saved values or mock timer values");
            
            // In mock mode, use the saved values for display
            String timeDisplay;
            
            // Calculate total seconds by combining minutes and seconds
            long totalSeconds = taskTimeBeforePauseMinutes * 60 + taskSecondsBeforePause;
            Log.d("TIMER_DEBUG", "Mock mode - calculating total time: " + taskTimeBeforePauseMinutes + 
                  "m + " + taskSecondsBeforePause + "s = " + totalSeconds + "s");
            
            if (totalSeconds >= 60) {
                // First round down total seconds to nearest 5 for consistency
                long roundedSeconds = (totalSeconds / 5) * 5;
                
                // Convert to minutes
                long minutes = roundedSeconds / 60;
                timeDisplay = minutes + "m";
                Log.d("TIMER_DEBUG", "Mock mode - rounded and converted to minutes: " + 
                      totalSeconds + "s → " + roundedSeconds + "s → " + minutes + "m");
            } else if (totalSeconds > 0) {
                // If less than 60 seconds, round down to nearest 5
                long roundedSeconds = (totalSeconds / 5) * 5;
                timeDisplay = roundedSeconds + "s";
                Log.d("TIMER_DEBUG", "Mock mode - rounded seconds: " + 
                      totalSeconds + "s → " + roundedSeconds + "s");
            } else {
                // Default to 0s if no saved time
                timeDisplay = "0s";
                Log.d("TIMER_DEBUG", "Mock mode - no saved time, defaulting to 0s");
            }
            
            // Use consistent format and update display
            binding.currentTaskElapsedTime.setText("Elapsed time of the current task: " + timeDisplay);
            Log.d("TIMER_DEBUG", "Set elapsed time display (mock mode): " + timeDisplay);
            
            return; // Exit early since we've handled the mock mode case
        }
        
        // Check if the timer is running or paused
        if (isPaused) {
            Log.d("TIMER_DEBUG", "Timer is paused, using saved values");
            
            // We're in a paused state, so use the saved values for display
            String timeDisplay;
            
            if (taskTimeBeforePauseMinutes > 0) {

                timeDisplay = taskTimeBeforePauseMinutes + "m";
                Log.d("TIMER_DEBUG", "Using minutes for display: " + timeDisplay);

                Log.d(TAG, "In paused state with saved task minutes: " + taskTimeBeforePauseMinutes + "m");
                binding.currentTaskElapsedTime.setText("Elapsed time of the current task: " + taskTimeBeforePauseMinutes + "m");

            } else if (taskSecondsBeforePause > 0) {
                // For seconds in paused state, round DOWN to the nearest 5 seconds for display
                // Integer division automatically rounds down, so we can use taskSecondsBeforePause / 5 * 5
                int roundedSeconds = (taskSecondsBeforePause / 5) * 5;
                timeDisplay = roundedSeconds + "s";
                Log.d("TIMER_DEBUG", "Paused state - rounded DOWN from " + taskSecondsBeforePause + 
                      "s to " + roundedSeconds + "s");
            } else {
                timeDisplay = "0s";
                Log.d("TIMER_DEBUG", "No saved time, using 0s");
            }
            
            binding.currentTaskElapsedTime.setText("Elapsed time of the current task: " + timeDisplay);
            Log.d("TIMER_DEBUG", "Set elapsed time display (paused): " + timeDisplay);
            
            logTimerState("UPDATE_TASK_TIME - After updating display");
        } else {
            Log.d("TIMER_DEBUG", "Timer is running, calculating current elapsed time");
            logTimerState("UPDATE_TASK_TIME - Before calculating elapsed time (running state)");
            
            // Get the task timer
            Timer taskTimer = currentRoutine.getTaskTimer();
            if (taskTimer == null) {
                Log.e("RoutineFragment", "Task timer is null");
                binding.currentTaskElapsedTime.setText("Elapsed time of the current task: 0s");
                Log.d("TIMER_DEBUG", "Task timer is null, defaulting to 0s");
                return;
            }
            
            // Print current task timer state
            Log.d("TIMER_DEBUG", "Task timer state: startTime=" + taskTimer.getStartTime() + 
                  ", isRunning=" + taskTimer.isRunning() +
                  ", elapsedSeconds=" + taskTimer.getElapsedSeconds());
            
            // Get the elapsed seconds using round DOWN function since timer is running
            long elapsedSeconds;
            if (taskTimer instanceof TaskTimer) {
                TaskTimer specificTaskTimer = (TaskTimer) taskTimer;
                elapsedSeconds = specificTaskTimer.getElapsedSecondsRoundedDown();
                Log.d("TIMER_DEBUG", "Using TaskTimer.getElapsedSecondsRoundedDown(): raw=" + 
                      specificTaskTimer.getElapsedSeconds() + "s, rounded DOWN=" + elapsedSeconds + "s");
            } else {
                // Fallback if casting fails - manually round down
                elapsedSeconds = taskTimer.getElapsedSeconds();
                elapsedSeconds = (elapsedSeconds / 5) * 5; // Manual round down to nearest 5s
                Log.d("TIMER_DEBUG", "Manual rounding DOWN to: " + elapsedSeconds + "s");
            }
        
            String timeDisplay;
        
            if (elapsedSeconds >= 60) {
                // Convert to minutes for display if >= 60 seconds
                long minutes = elapsedSeconds / 60;
                timeDisplay = minutes + "m";
                Log.d("TIMER_DEBUG", "Converting " + elapsedSeconds + "s to " + minutes + "m for display");
            } else {
                // For seconds, we already have the rounded down value
                timeDisplay = elapsedSeconds + "s";
                Log.d("TIMER_DEBUG", "Using seconds for display (already rounded down): " + timeDisplay);
            }
            
            binding.currentTaskElapsedTime.setText("Elapsed time of the current task: " + timeDisplay);
            Log.d("TIMER_DEBUG", "Set elapsed time display (running): " + timeDisplay);
            
            logTimerState("UPDATE_TASK_TIME - After updating display");
        }
    }

    /**
     * Helper method to save the current task's elapsed time before pausing
     */
    private void saveCurrentTaskElapsedTime() {
        final String TAG = "TaskPause";
        // Find the current active task
        if (currentRoutine == null) {
            Log.d(TAG, "No current routine found");
            return;
        }
        
        // In mock mode, we don't need to recalculate elapsed time from real timer
        // Because we're explicitly tracking it with taskSecondsBeforePause and taskTimeBeforePauseMinutes
        if (isStopTimerPressed) {
            Log.d(TAG, "In mock mode - using existing values instead of recalculating: " +
                  taskTimeBeforePauseMinutes + "m " + taskSecondsBeforePause + "s");
            return;
        }
        
        // Get all tasks and log their state
        List<Task> tasks = currentRoutine.getTasks();
        if (tasks.isEmpty()) {
            Log.d(TAG, "Task list is empty");
            return;
        }
        
        Log.d(TAG, "Task list size: " + tasks.size());
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            Log.d(TAG, "Task " + i + ": " + task.getTaskName() 
                + " - completed: " + task.isCompleted() 
                + ", skipped: " + task.isSkipped()
                + ", checked: " + task.isCheckedOff());
        }
        
        // First try to find first uncompleted and unskipped task
        Task currentTask = null;
        for (Task task : tasks) {
            if (!task.isCompleted() && !task.isSkipped()) {
                currentTask = task;
                Log.d(TAG, "Found active task (not completed, not skipped): " + task.getTaskName());
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

            long currentTaskElapsedTime = 0;
            if (currentRoutine.getTaskTimer() != null) {
                // Get current task elapsed time in seconds
                if (isPaused) {
                    // If we're paused, calculate total seconds by combining minutes and seconds
                    long totalSeconds = taskTimeBeforePauseMinutes * 60 + taskSecondsBeforePause;
                    if (totalSeconds > 0) {
                        // Round DOWN to nearest 5 seconds for consistency
                        currentTaskElapsedTime = (totalSeconds / 5) * 5;
                        Log.d("TIMER_DEBUG", "Saving total task time in paused state: " + 
                              taskTimeBeforePauseMinutes + "m + " + taskSecondsBeforePause + "s = " + 
                              totalSeconds + "s → rounded to " + currentTaskElapsedTime + "s");
                    }
                } else if (currentRoutine.getTaskTimer().isRunning()) {
                    // Get rounded DOWN seconds from running timer if available
                    if (currentRoutine.getTaskTimer() instanceof TaskTimer) {
                        TaskTimer taskTimer = (TaskTimer) currentRoutine.getTaskTimer();
                        currentTaskElapsedTime = taskTimer.getElapsedSecondsRoundedDown();
                        Log.d("TIMER_DEBUG", "Saving task time using getElapsedSecondsRoundedDown: " + 
                              currentTaskElapsedTime + "s");
                    } else {
                        // Otherwise manually round down
                        currentTaskElapsedTime = currentRoutine.getTaskTimer().getElapsedSeconds();
                        currentTaskElapsedTime = (currentTaskElapsedTime / 5) * 5; // Round DOWN
                        Log.d("TIMER_DEBUG", "Saving task time with manual rounding: " + 
                              currentTaskElapsedTime + "s");
                    }
                }
            }
            
            // IMPORTANT: In mock mode, we want to use timeBeforePauseMinutes directly as elapsedMinutes
            // This ensures mock time values persist across restarts
            long elapsedMinutesToSave = currentRoutine.getRoutineDurationMinutes();
            
            // CRITICAL FIX: Always use current timeBeforePauseMinutes if in mock mode
            // This ensures the updated time value is saved properly after each click of "Advance Mock Timer"
            if (isStopTimerPressed) {
                // Get the routine's actual duration
                long routineDuration = currentRoutine.getRoutineDurationMinutes();
                
                // IMPORTANT: In mock mode, we specifically save:
                // 1. The routine's actual duration (elapsedMinutesToSave)
                // 2. The mock time increments (timeBeforePauseMinutes)
                // This allows us to reconstruct the total time after restart
                elapsedMinutesToSave = routineDuration;
                
                // DO NOT change timeBeforePauseMinutes here - it tracks only the mock time increments
                
                // Log the values being saved
                Log.d("MockMode-DEBUG", "SAVING STATE: Mock mode with routineDuration=" + routineDuration + 
                      "m, mockTimeIncrements=" + timeBeforePauseMinutes + "m");
                
                // Log the combined time that will be displayed
                long combinedTime = routineDuration + timeBeforePauseMinutes;
                Log.d("MockMode-DEBUG", "SAVING STATE: Combined time for display will be: " + combinedTime + 
                      "m (routineDuration=" + routineDuration + "m + timeBeforePauseMinutes=" + 
                      timeBeforePauseMinutes + "m)");
            } else if (isPaused && timeBeforePauseMinutes > 0) {
                // Also use saved time if we're paused
                elapsedMinutesToSave = timeBeforePauseMinutes;
                Log.d("TIMER_DEBUG", "Using timeBeforePauseMinutes as elapsedMinutes in paused state: " + elapsedMinutesToSave + "m");
            }
            

            currentTaskElapsedTime = 0;
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
                (int) Math.min(currentTaskElapsedTime, Integer.MAX_VALUE)
            );
            
            Log.d("RoutineFragment", "Saved routine state: " + 
                  "isTimerRunning=" + isTimerRunning +
                  ", isPaused=" + isPaused + 
                  ", manuallyStarted=" + manuallyStarted +
                  ", isTimerStopped=" + isStopTimerPressed +

                  ", timeBeforePauseMinutes=" + timeBeforePauseMinutes +
                  ", elapsedMinutes=" + elapsedMinutesToSave +

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
                
                // Restore the mock mode state from saved state
                isStopTimerPressed = uiState.isTimerStopped;
                Log.d("RoutineFragment", "Restored mock mode state from isTimerStopped: " + isStopTimerPressed);
                
                // CRITICAL FIX: When in mock mode after restart, combine routine duration with saved elapsed time
                // This ensures we continue from the right time after restart
                if (isStopTimerPressed) {
                    // When restarting in mock mode, we need to understand what timeBeforePauseMinutes represents:
                    // It should ONLY represent the mock time increments, not the total time
                    // We'll display the combined time (routineDuration + timeBeforePauseMinutes) in updateTimeDisplay
                    
                    // Log the values for debugging
                    long routineDuration = currentRoutine.getRoutineDurationMinutes();
                    Log.d("MockMode-DEBUG", "RESTART: Mock mode active with routineDuration=" + routineDuration + 
                          "m, mockTimeIncrements=" + timeBeforePauseMinutes + "m");
                    
                    // Calculate the combined time for logging purposes
                    long combinedTime = routineDuration + timeBeforePauseMinutes;
                    Log.d("MockMode-DEBUG", "RESTART: Combined time for display will be: " + combinedTime + 
                          "m (routineDuration=" + routineDuration + "m + mockTimeIncrements=" + 
                          timeBeforePauseMinutes + "m)");
                    
                    // We don't modify timeBeforePauseMinutes here - updateTimeDisplay will combine the values
                }
                
                // Update the button text based on mock mode state
                if (isStopTimerPressed) {
                    binding.stopTimerButton.setText("Advance Mock Timer");
                    binding.stopTimerButton.setEnabled(true);
                    
                    // Also update TaskAdapter with mock mode state
                    if (taskAdapter != null) {
                        taskAdapter.setMockModeActive(true);
                        Log.d("RoutineFragment", "Updated TaskAdapter with mock mode status after restart");
                    }
                    
                    Log.d("RoutineFragment", "Restored mock mode UI: button text set to 'Advance Mock Timer'");
                } else {
                    binding.stopTimerButton.setText("Switch to Mock");
                    
                    // Make sure mock mode is disabled in TaskAdapter
                    if (taskAdapter != null) {
                        taskAdapter.setMockModeActive(false);
                    }
                }
                
                // Save the current time for the pause
                LocalDateTime pauseTime = LocalDateTime.now();
                
                // IMPORTANT: Explicitly ensure the task timer is paused
                if (currentRoutine.getTaskTimer() != null && currentRoutine.getTaskTimer().isRunning()) {
                    Log.d("TIMER_DEBUG", "Explicitly ending task timer during app restart");
                    currentRoutine.getTaskTimer().end(pauseTime);
                }
                
                // Adjust the task timer's start time based on saved elapsed time if needed
                if (currentRoutine.getTaskTimer() != null && taskSecondsBeforePause > 0) {
                    // Round DOWN to nearest 5 seconds for consistent display
                    int roundedSeconds = (taskSecondsBeforePause / 5) * 5;
                    
                    // Create an adjusted start time that would give us the correct elapsed time
                    // Start time = current time - elapsed seconds
                    LocalDateTime adjustedStartTime = pauseTime.minusSeconds(roundedSeconds);
                    
                    // First, make sure to end any running timer
                    if (currentRoutine.getTaskTimer().isRunning()) {
                        currentRoutine.getTaskTimer().end(pauseTime);
                    }
                    
                    // Then start with the adjusted time
                    currentRoutine.getTaskTimer().start(adjustedStartTime);
                    
                    // Then pause it immediately to keep it in a consistent state
                    currentRoutine.getTaskTimer().end(pauseTime);
                    
                    Log.d("TIMER_DEBUG", "Reset task timer with adjusted start time for " + 
                           roundedSeconds + "s display: " + adjustedStartTime);
                }
                
                // Now pause the routine
                currentRoutine.pauseTime(LocalDateTime.now());
                
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

                // Update the task elapsed time display with same format as during normal operation
                // Calculate total seconds by combining minutes and seconds for proper display
                long totalSeconds = taskTimeBeforePauseMinutes * 60 + taskSecondsBeforePause;
                Log.d("TIMER_DEBUG", "Restart - calculating total seconds: " + 
                      taskTimeBeforePauseMinutes + "m + " + taskSecondsBeforePause + "s = " + totalSeconds + "s");
                
                String timeDisplay;
                if (totalSeconds >= 60) {
                    // Convert to minutes for display if >= 60 seconds
                    // Round DOWN to nearest 5 seconds first
                    long roundedSeconds = (totalSeconds / 5) * 5;
                    long minutes = roundedSeconds / 60;
                    timeDisplay = minutes + "m";
                    Log.d("TIMER_DEBUG", "Restart - converting to minutes: " + 
                          totalSeconds + "s → " + roundedSeconds + "s → " + minutes + "m");
                } else if (totalSeconds > 0) {
                    // Round DOWN to nearest 5 seconds for seconds display
                    long roundedSeconds = (totalSeconds / 5) * 5;
                    timeDisplay = roundedSeconds + "s";
                    Log.d("TIMER_DEBUG", "Restart - using seconds (rounded down): " + 
                          totalSeconds + "s → " + roundedSeconds + "s");
                } else {
                    // Default to 0s if no saved time
                    timeDisplay = "0s";
                    Log.d("TIMER_DEBUG", "Restart - no saved time, defaulting to 0s");
                }
                
                // Use the exact same format as during normal operation
                binding.currentTaskElapsedTime.setText("Elapsed time of the current task: " + timeDisplay);
                Log.d("TIMER_DEBUG", "Set elapsed time display after restart: " + timeDisplay);
                
                
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
                } else {
                    // No task seconds saved, just show 0s
                    binding.currentTaskElapsedTime.setText("0s");
                    Log.d("RoutineFragment", "No task seconds saved, displaying 0s");
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

    /**
     * Helper method to get the current active task
     * @return The current active task, or null if none found
     */
    private Task getCurrentTask() {
        final String TAG = "GET_CURRENT_TASK";
        
        // Early exit if no routine
        if (currentRoutine == null) {
            Log.d(TAG, "Routine is null");
            return null;
        }
        
        // Find the first uncompleted task (current active task)
        List<Task> tasks = currentRoutine.getTasks();
        if (tasks.isEmpty()) {
            Log.d(TAG, "Task list is empty");
            return null;
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
        
        return currentTask;
    }

    /**
     * Helper method to log timer state for debugging
     * @param context Context message to include in the log
     */
    private void logTimerState(String context) {
        if (currentRoutine == null) {
            Log.d("TIMER_DEBUG", context + " - Routine is null");
            return;
        }
        
        Log.d("TIMER_DEBUG", "=== " + context + " ===");
        Log.d("TIMER_DEBUG", "isPaused: " + isPaused);
        Log.d("TIMER_DEBUG", "isTimerRunning: " + isTimerRunning);
        Log.d("TIMER_DEBUG", "isStopTimerPressed: " + isStopTimerPressed);
        Log.d("TIMER_DEBUG", "taskTimeBeforePauseMinutes: " + taskTimeBeforePauseMinutes);
        Log.d("TIMER_DEBUG", "taskSecondsBeforePause: " + taskSecondsBeforePause);
        
        if (currentRoutine.getRoutineTimer() != null) {
            Log.d("TIMER_DEBUG", "Routine timer - startTime: " + currentRoutine.getRoutineTimer().getStartTime());
            Log.d("TIMER_DEBUG", "Routine timer - isRunning: " + currentRoutine.getRoutineTimer().isRunning());
        }
        
        if (currentRoutine.getTaskTimer() != null) {
            Log.d("TIMER_DEBUG", "Task timer - startTime: " + currentRoutine.getTaskTimer().getStartTime());
            Log.d("TIMER_DEBUG", "Task timer - isRunning: " + currentRoutine.getTaskTimer().isRunning());
            Log.d("TIMER_DEBUG", "Task timer - elapsedSeconds: " + currentRoutine.getTaskTimer().getElapsedSeconds());
            
            if (currentRoutine.getTaskTimer() instanceof TaskTimer) {
                TaskTimer taskTimer = (TaskTimer) currentRoutine.getTaskTimer();
                Log.d("TIMER_DEBUG", "Task timer - elapsedSecondsRoundedDown: " + taskTimer.getElapsedSecondsRoundedDown());
                Log.d("TIMER_DEBUG", "Task timer - elapsedSecondsRoundedUp: " + taskTimer.getElapsedSecondsRoundedUp());
            }
        }
    }

    /**
     * Check if the routine has ended
     * @return true if the routine has ended
     */
    public boolean isRoutineEnded() {
        return binding != null && 
               binding.endRoutineButton != null && 
               binding.endRoutineButton.getText().toString().equals("Routine Ended");
    }

    /**
     * Get the current task elapsed time in seconds
     * This is used by TaskAdapter to check if we need to increment task completion time
     * @return The current task elapsed time in seconds
     */
    public long getCurrentTaskElapsedTimeInSeconds() {
        // Calculate total seconds by combining minutes and seconds
        long totalSeconds = taskTimeBeforePauseMinutes * 60 + taskSecondsBeforePause;
        
        // If we're not in mock mode and the timer is running, get the actual elapsed time
        if (!isStopTimerPressed && currentRoutine != null && currentRoutine.getTaskTimer() != null) {
            if (currentRoutine.getTaskTimer().isRunning()) {
                // Get elapsed seconds from the timer
                if (currentRoutine.getTaskTimer() instanceof TaskTimer) {
                    TaskTimer taskTimer = (TaskTimer) currentRoutine.getTaskTimer();
                    totalSeconds = taskTimer.getElapsedSecondsRoundedDown();
                    Log.d("TIMER_DEBUG", "Getting elapsed time from running timer: " + totalSeconds + "s");
                } else {
                    // Fallback if casting fails - manually round down
                    totalSeconds = currentRoutine.getTaskTimer().getElapsedSeconds();
                    totalSeconds = (totalSeconds / 5) * 5; // Round DOWN to nearest 5s
                    Log.d("TIMER_DEBUG", "Manual rounding DOWN to: " + totalSeconds + "s");
                }
            }
        }
        
        Log.d("TIMER_DEBUG", "Current task elapsed time: " + totalSeconds + "s");
        return totalSeconds;
    }
}