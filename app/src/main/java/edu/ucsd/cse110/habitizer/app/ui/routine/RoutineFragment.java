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

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.time.LocalDateTime;
import java.util.ArrayList;

import edu.ucsd.cse110.habitizer.app.HabitizerApplication;
import edu.ucsd.cse110.habitizer.app.MainViewModel;
import edu.ucsd.cse110.habitizer.app.R;
import edu.ucsd.cse110.habitizer.app.data.LegacyLogicAdapter;
import edu.ucsd.cse110.habitizer.app.data.db.HabitizerRepository;
import edu.ucsd.cse110.habitizer.app.databinding.FragmentRoutineScreenBinding;
import edu.ucsd.cse110.habitizer.app.ui.dialog.CreateTaskDialogFragment;
import edu.ucsd.cse110.habitizer.app.ui.dialog.SetRoutineTimeDialogFragment;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;

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

    public RoutineFragment() {
        // required empty public constructor
    }

    private void initTimerUpdates() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                // Only update if app is in foreground
                if (edu.ucsd.cse110.habitizer.app.MainActivity.isAppInForeground) {
                    updateTimeDisplay();
                }

                timerHandler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        };

        // Start
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

        // Get routine
        this.currentRoutine = activityModel.getRoutineRepository().getRoutine(routineId);
        Log.d("RoutineFragment", "Current routine: " + (currentRoutine != null ? currentRoutine.getRoutineName() : "null"));
        
        // Check if the routine is active (was started from the home page)
        if (currentRoutine != null && currentRoutine.isActive() && !currentRoutine.getTasks().isEmpty()) {
            Log.d("RoutineFragment", "Routine is active and has tasks, marking as manually started");
            // If the routine was started from home page and has tasks, mark it as manually started
            manuallyStarted = true;
        } else {
            Log.d("RoutineFragment", "Routine is not active or empty, not marking as manually started");
            manuallyStarted = false;
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("RoutineFragment", "onCreateView called");
        binding = FragmentRoutineScreenBinding.inflate(inflater, container, false);
        Log.d("RoutineFragment", "Binding inflated");

        // Explicitly set the actual time to "-" at initialization to prevent flicker
        binding.actualTime.setText("-");

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
        if (!currentRoutine.getTasks().isEmpty()) {
            Log.d("RoutineFragment", "Starting routine with tasks");
            // Only auto-start if coming from home screen (was active)
            if (currentRoutine.isActive()) {
                manuallyStarted = true;
                currentRoutine.startRoutine(LocalDateTime.now());
            }
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
                
                // Explicitly set the button text to "Routine Ended" when ending the routine
                binding.endRoutineButton.setText("Routine Ended");
                binding.endRoutineButton.setEnabled(false);
                binding.stopTimerButton.setEnabled(false);
                binding.fastForwardButton.setEnabled(false);
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
            if (currentRoutine.isActive()) {
                // Pause at current simulated time
                currentRoutine.pauseTime(LocalDateTime.now());
                updateTimeDisplay();
            }
            binding.stopTimerButton.setEnabled(false);
            isTimerRunning = false;
        });

        binding.homeButton.setOnClickListener(v -> {
            // Navigate back to HomeScreenFragment
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        // Initial state setup
        binding.homeButton.setEnabled(false);

        binding.fastForwardButton.setOnClickListener(v -> {
            // Fast forward 30 seconds
            currentRoutine.fastForwardTime();

            // Force immediate UI update
            updateTimeDisplay();

            // If routine completed via FF, update state
            if (currentRoutine.autoCompleteRoutine()) {
                binding.endRoutineButton.setEnabled(false);
                isTimerRunning = false;
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("RoutineFragment", "onViewCreated called");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("RoutineFragment", "onResume called");
        
        // Resume timer if it was running before app was minimized
        if (timeWhenMinimized != null && wasTimerRunningBeforeMinimize && currentRoutine != null && currentRoutine.isActive()) {
            Log.d("RoutineFragment", "Resuming timer after app was minimized");
            
            // Calculate time difference between when app was minimized and now
            LocalDateTime now = LocalDateTime.now();
            long secondsDifference = java.time.Duration.between(timeWhenMinimized, now).getSeconds();
            
            Log.d("RoutineFragment", "App was in background for " + secondsDifference + 
                  " seconds (from " + timeWhenMinimized + " to " + now + ")");
            
            // Resume timer using the resumeTime method
            currentRoutine.resumeTime(now);
            
            // Resume timer state
            isTimerRunning = true;
            
            // Reset the flag and time
            wasTimerRunningBeforeMinimize = false;
            timeWhenMinimized = null;
            
            // Force update display
            updateTimeDisplay();
            
            Log.d("RoutineFragment", "Timer resumed, current duration: " + 
                  currentRoutine.getRoutineDurationMinutes() + "m");
        }
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
            
            // Start the routine if it's not already active
            if (!currentRoutine.isActive()) {
                currentRoutine.startRoutine(LocalDateTime.now());
                Log.d("RoutineFragment", "Starting routine after adding task");
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

    private void updateTimeDisplay() {
        // Ensure the routine is properly initialized
        if (currentRoutine == null) {
            Log.e("RoutineFragment", "Cannot update time display - routine is null");
            binding.actualTime.setText("-");
            return;
        }
        
        // Get current routine duration from the routine
        long minutes = currentRoutine.getRoutineDurationMinutes();
        
        // Log detailed timing information for debugging
        Log.d("RoutineFragment", "=== TIME DISPLAY UPDATE ===");
        Log.d("RoutineFragment", "Minutes to display: " + minutes);
        Log.d("RoutineFragment", "Routine active: " + currentRoutine.isActive());
        Log.d("RoutineFragment", "Timer running: " + isTimerRunning);
        Log.d("RoutineFragment", "Manually started: " + manuallyStarted);
        
        // Check the routine's internal timer state
        if (currentRoutine.getRoutineTimer() != null) {
            LocalDateTime startTime = currentRoutine.getRoutineTimer().getStartTime();
            LocalDateTime endTime = currentRoutine.getRoutineTimer().getEndTime();
            Log.d("RoutineFragment", "Routine timer start: " + startTime);
            Log.d("RoutineFragment", "Routine timer end: " + endTime);
            
            // Calculate the expected time based on the current time
            if (startTime != null) {
                long expectedSeconds = java.time.Duration.between(startTime, LocalDateTime.now()).getSeconds();
                Log.d("RoutineFragment", "Expected duration (raw): " + (expectedSeconds / 60.0) + " minutes");
            }
        }
        
        // Only show minutes if there's a meaningful value to display (> 0)
        // Always show "-" when minutes is 0, regardless of other states
        if (minutes == 0) {
            binding.actualTime.setText("-");
        } else {
            binding.actualTime.setText(String.format("%d%s", minutes, "m"));
        }

        boolean hasTasks = !currentRoutine.getTasks().isEmpty();
        
        // Only auto-activate the routine if we've manually started it AND it has tasks
        // This preserves the user's control over when the routine starts
        if (hasTasks && manuallyStarted && !currentRoutine.isActive()) {
            // If it has tasks but isn't active, activate it unless explicitly ended
            if (!binding.endRoutineButton.getText().toString().equals("Routine Ended")) {
                Log.d("RoutineFragment", "Auto-activating routine that has tasks and is manually started");
                currentRoutine.startRoutine(LocalDateTime.now());
            }
        }
        
        boolean routineIsActive = currentRoutine.isActive();
        Log.d("RoutineFragment", "UpdateTimeDisplay - hasTasks: " + hasTasks + ", isActive: " + routineIsActive + ", manuallyStarted: " + manuallyStarted + ", time: " + minutes + "m");

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
            binding.fastForwardButton.setEnabled(false);
            binding.homeButton.setEnabled(true);
        } else if (!hasTasks || !manuallyStarted) {
            // Cases 1 & 2: Empty routine OR routine with tasks that isn't manually started
            // Keep text as "End Routine" and DISABLE the button
            binding.endRoutineButton.setText("End Routine");
            binding.endRoutineButton.setEnabled(false);
            binding.stopTimerButton.setEnabled(false);
            binding.fastForwardButton.setEnabled(false);
            binding.homeButton.setEnabled(true);
        } else {
            // Case 3: Routine with tasks that is manually started - enable "End Routine" to end it
            binding.endRoutineButton.setText("End Routine");
            binding.endRoutineButton.setEnabled(true);
            binding.stopTimerButton.setEnabled(isTimerRunning);
            binding.fastForwardButton.setEnabled(true);
            binding.homeButton.setEnabled(false);
        }

        // Update task list times
        taskAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timerHandler.removeCallbacks(timerRunnable);
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
     * Update the UI to reflect that the routine has ended
     * Called when the routine is automatically completed due to all tasks being checked
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
        binding.fastForwardButton.setEnabled(false);
        binding.homeButton.setEnabled(true);
        
        // Force update the time display
        updateTimeDisplay();
        
        // Ensure task adapter refreshes to update checkbox states
        if (taskAdapter != null) {
            taskAdapter.notifyDataSetChanged();
            Log.d("RoutineFragment", "Refreshed task adapter to update checkbox states");
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
}