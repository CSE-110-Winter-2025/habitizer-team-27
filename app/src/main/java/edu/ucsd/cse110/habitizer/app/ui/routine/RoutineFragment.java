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
        final String TAG = "ELAPSED_TIME_DEBUG";
        
        // This logic ensures we have the right routine loaded
        Log.d(TAG, "onCreateView called");
        activityModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        repository = HabitizerApplication.getRepository();
        binding = FragmentRoutineScreenBinding.inflate(inflater, container, false);
        
        int routineId = getArguments().getInt(ARG_ROUTINE_ID, -1);
        Log.d(TAG, "Routine ID from arguments: " + routineId);
        
        // Routine should already be in the application memory
        currentRoutine = activityModel.getRoutineById(routineId);
        if (currentRoutine == null) {
            Log.e(TAG, "Couldn't find routine with ID " + routineId + ", trying repository directly");
            
            // Try to get the routine from the repository
            try {
                if (repository != null) {
                    currentRoutine = repository.getRoutines().getValue()
                        .stream()
                        .filter(r -> r.getRoutineId() == routineId)
                        .findFirst()
                        .orElse(null);
                    
                    if (currentRoutine != null) {
                        Log.d(TAG, "Found routine in repository: " + currentRoutine.getRoutineName());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error trying to find routine in repository", e);
            }
            
            // If still null, return and show error
            if (currentRoutine == null) {
                Log.e(TAG, "Unable to find routine with ID " + routineId);
                Toast.makeText(requireContext(), "Error: Routine not found", Toast.LENGTH_SHORT).show();
                requireActivity().onBackPressed();
                return binding.getRoot();
            }
        }
        
        Log.d(TAG, "Loaded routine: " + currentRoutine.getRoutineName() + 
              " with " + currentRoutine.getTasks().size() + " tasks");
        
        boolean isDefaultRoutine = currentRoutine.getRoutineName().equals("Morning") || 
                                 currentRoutine.getRoutineName().equals("Evening");
        
        Log.d(TAG, "Is default routine: " + isDefaultRoutine);
        
        // Reset tasks when starting a routine from the home screen
        resetRoutineState();
        
        // Set routine name and goal time display
        binding.routineNameTask.setText(currentRoutine.getRoutineName());
        binding.currentTaskElapsedTime.setText("Elapsed time of the current task: 0m"); // Initialize with default value
        updateRoutineGoalDisplay(currentRoutine.getGoalTime());
        
        // Handle case where routine should be active and timers need initialization
        if (currentRoutine.getTasks().size() > 0) {
            Log.d(TAG, "Routine has tasks, marking as manually started");
            manuallyStarted = true;
            
            // Check if this is a default routine before conditionally starting
            if (isDefaultRoutine) {
                Log.d(TAG, "This is a default routine, ensuring it gets started");
                // Force start for default routines
                currentRoutine.startRoutine(LocalDateTime.now());
            } else {
                // For custom routines, use the normal approach
                Log.d(TAG, "Initializing routine timers for custom routine");
                currentRoutine.startRoutine(LocalDateTime.now());
            }
        } else {
            Log.d(TAG, "Routine has no tasks");
        }
        
        // Initialize ListView and Adapter
        ListView taskListView = binding.routineList;
        if (taskListView == null) {
            Log.e(TAG, "taskListView is null! Layout issue?");
        } else {
            Log.d(TAG, "ListView found with ID: " + taskListView.getId());
        }
        
        Log.d(TAG, "Setting up task adapter");
        
        ArrayList<Task> initialTasks = new ArrayList<>(currentRoutine.getTasks());
        Log.d(TAG, "Initial tasks count for adapter: " + initialTasks.size());
        
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
        Log.d(TAG, "Task adapter set on ListView");
        
        // Set up the buttons
        setupRoutineButtons();
        
        // Initialize the timer update mechanism
        initTimerUpdates();
        
        // Update the elapsed time immediately
        updateTimeDisplay();
        
        // Force an immediate update of the task elapsed time display
        Log.d(TAG, "Forcing update of task elapsed time display");
        updateCurrentTaskElapsedTime();
        
        // Save the routine state to ensure changes persist
        if (repository != null && currentRoutine != null) {
            Log.d(TAG, "Saving initial routine state to repository");
            repository.updateRoutine(currentRoutine);
        }
        
        // Log the initial state
        Log.d(TAG, "Initial state - isActive: " + (currentRoutine != null ? currentRoutine.isActive() : "null") +
              ", task count: " + (currentRoutine != null ? currentRoutine.getTasks().size() : 0));

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
                Log.d("RoutineFragment", "Expected seconds from wall time: " + expectedSeconds + "s");
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
        
        // Update the elapsed time of the current active task
        updateCurrentTaskElapsedTime();

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
        
        // Check for goal time display
        updateRoutineGoalDisplay(currentRoutine.getGoalTime());
    }
    
    /**
     * Update the display of the current task's elapsed time
     */
    private void updateCurrentTaskElapsedTime() {
        final String TAG = "ELAPSED_TIME_DEBUG";
        
        Log.d(TAG, "updateCurrentTaskElapsedTime() called");
        Log.d(TAG, "Routine: " + (currentRoutine != null ? currentRoutine.getRoutineName() + " (ID: " + currentRoutine.getRoutineId() + ")" : "null"));
        Log.d(TAG, "Routine active: " + (currentRoutine != null ? currentRoutine.isActive() : "null routine"));
        Log.d(TAG, "manuallyStarted: " + manuallyStarted);
        Log.d(TAG, "isTimerRunning: " + isTimerRunning);

        if (currentRoutine == null) {
            Log.d(TAG, "Routine is null, clearing elapsed time text");
            binding.currentTaskElapsedTime.setText("");
            return;
        }

        // Always show the task timer status, even if routine isn't "active"
        // Find the first uncompleted task (current active task)
        List<Task> tasks = currentRoutine.getTasks();
        Log.d(TAG, "Total tasks in routine: " + tasks.size());
        
        Task currentTask = null;
        
        for (Task task : tasks) {
            Log.d(TAG, "Checking task: " + task.getTaskName() + ", completed: " + task.isCompleted() + ", skipped: " + task.isSkipped());
            if (!task.isCompleted() && !task.isSkipped()) {
                currentTask = task;
                break;
            }
        }
        
        if (currentTask == null) {
            // No active task found
            Log.d(TAG, "No active task found");
            
            // If the routine is active and has tasks, show a default elapsed time
            // This handles the case where tasks might be incorrectly marked as skipped
            if (currentRoutine.isActive() && !tasks.isEmpty()) {
                Log.d(TAG, "Routine is active with tasks but no active task found, showing default elapsed time");
                binding.currentTaskElapsedTime.setText("Elapsed time of the current task: 0m");
                
                // Try to un-skip the first task to make it active
                if (tasks.size() > 0) {
                    Task firstTask = tasks.get(0);
                    Log.d(TAG, "Attempting to un-skip first task: " + firstTask.getTaskName());
                    firstTask.setSkipped(false);
                    
                    // Save the change
                    if (repository != null) {
                        repository.updateRoutine(currentRoutine);
                    }
                }
            } else {
                binding.currentTaskElapsedTime.setText("");
            }
            return;
        }
        
        Log.d(TAG, "Current active task: " + currentTask.getTaskName());
        
        // Calculate the elapsed time for this task
        long elapsedTimeSeconds = 0;
        LocalDateTime taskStart = null;
        
        // Check if task timer is initialized
        if (currentRoutine.getTaskTimer() != null) {
            taskStart = currentRoutine.getTaskTimer().getStartTime();
        }
        
        // Debug timer state
        Log.d(TAG, "Task timer start time: " + taskStart);
        Log.d(TAG, "Routine timer start time: " + 
              (currentRoutine.getRoutineTimer() != null ? currentRoutine.getRoutineTimer().getStartTime() : "null"));
        
        // If this is a default routine (Morning/Evening) that isn't active yet, force-start it
        String routineName = currentRoutine.getRoutineName();
        if (tasks.size() > 0 && 
            (routineName.equals("Morning") || routineName.equals("Evening")) && 
            !currentRoutine.isActive()) {
            
            Log.d(TAG, "Force-starting default routine: " + routineName);
            manuallyStarted = true;
            isTimerRunning = true;
            currentRoutine.startRoutine(LocalDateTime.now());
            
            // Get updated start time
            taskStart = currentRoutine.getTaskTimer().getStartTime();
            Log.d(TAG, "Updated task timer start time after force-start: " + taskStart);
        }
        
        // If task timer isn't initialized but routine timer is, use routine start time
        if (taskStart == null && currentRoutine.getRoutineTimer() != null && 
            currentRoutine.getRoutineTimer().getStartTime() != null) {
            
            taskStart = currentRoutine.getRoutineTimer().getStartTime();
            Log.d(TAG, "Using routine start time as fallback: " + taskStart);
        }
        
        // Even if we don't have a valid start time, show initial elapsed time of 0
        if (taskStart == null) {
            Log.d(TAG, "No valid start time available, showing default elapsed time");
            binding.currentTaskElapsedTime.setText("Elapsed time of the current task: 0m");
            return;
        }
        
        LocalDateTime now = currentRoutine.getCurrentTime();
        LocalDateTime currentDateTime = LocalDateTime.now();
        
        Log.d(TAG, "Current time from routine: " + now);
        Log.d(TAG, "Current wall time: " + currentDateTime);
        
        if (!isTimerRunning) {
            // If timer is paused, use the current time from the routine
            elapsedTimeSeconds = java.time.Duration.between(taskStart, now).getSeconds();
        } else {
            // If timer is running, use the current wall time
            elapsedTimeSeconds = java.time.Duration.between(taskStart, currentDateTime).getSeconds();
        }
        
        // Ensure non-negative time
        elapsedTimeSeconds = Math.max(0, elapsedTimeSeconds);
        
        // For running tasks, round DOWN to minutes (integer division)
        long elapsedMinutes = elapsedTimeSeconds / 60;
        
        // Update the text view with the new format
        String elapsedTimeText = "Elapsed time of the current task: " + elapsedMinutes + "m";
        Log.d(TAG, "Setting elapsed time text: " + elapsedTimeText);
        binding.currentTaskElapsedTime.setText(elapsedTimeText);
        
        Log.d(TAG, "Calculated elapsed time: " + elapsedMinutes + "m (" + elapsedTimeSeconds + "s)");
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

    /**
     * Set up all the button click listeners and initial states
     */
    private void setupRoutineButtons() {
        binding.addTaskButton.setOnClickListener(v -> {
            CreateTaskDialogFragment dialog = CreateTaskDialogFragment.newInstance(this::addTaskToRoutine);
            dialog.show(getParentFragmentManager(), "CreateTaskDialog");
        });

        binding.expectedTime.setOnClickListener(v -> {
            SetRoutineTimeDialogFragment dialog = SetRoutineTimeDialogFragment.newInstance(this::updateRoutineGoalDisplay);
            dialog.show(getParentFragmentManager(), "SetTimeDialog");
        });

        binding.endRoutineButton.setOnClickListener(v -> {
            // If routine is active, end it
            if (currentRoutine.isActive()) {
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
                    Log.d("RoutineFragment", "Refreshed task adapter after routine end");
                }
                
                // Save the routine state to make sure the end time is preserved
                repository.updateRoutine(currentRoutine);
                
                // Update the UI
                updateTimeDisplay();
            }
        });

        binding.stopTimerButton.setOnClickListener(v -> {
            if (currentRoutine.isActive()) {
                // Pause at current simulated time
                currentRoutine.pauseTime(LocalDateTime.now());
                
                // Update UI and state
                updateTimeDisplay();
                binding.stopTimerButton.setEnabled(false);
                isTimerRunning = false;
                
                Log.d("RoutineFragment", "Timer paused");
            }
        });

        binding.homeButton.setOnClickListener(v -> {
            // Navigate back to HomeScreenFragment
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });

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
        
        // Initial button states
        boolean hasActiveTasks = false;
        for (Task task : currentRoutine.getTasks()) {
            if (!task.isCompleted() && !task.isSkipped()) {
                hasActiveTasks = true;
                break;
            }
        }
        
        // Enable/disable buttons based on routine state
        if (currentRoutine.isActive() && hasActiveTasks) {
            binding.endRoutineButton.setEnabled(true);
            binding.stopTimerButton.setEnabled(isTimerRunning);
            binding.fastForwardButton.setEnabled(true);
            binding.homeButton.setEnabled(false);
        } else {
            binding.endRoutineButton.setEnabled(false);
            binding.stopTimerButton.setEnabled(false);
            binding.fastForwardButton.setEnabled(false);
            binding.homeButton.setEnabled(true);
        }
    }

    /**
     * Reset the routine state to start fresh
     */
    private void resetRoutineState() {
        final String TAG = "ELAPSED_TIME_DEBUG";
        
        if (currentRoutine == null) {
            Log.d(TAG, "resetRoutineState: Routine is null, cannot reset");
            return;
        }
        
        Log.d(TAG, "resetRoutineState: Resetting routine state for: " + currentRoutine.getRoutineName());
        
        boolean isDefaultRoutine = currentRoutine.getRoutineName().equals("Morning") || 
                                 currentRoutine.getRoutineName().equals("Evening");
        
        // End any active routine
        if (currentRoutine.isActive()) {
            Log.d(TAG, "resetRoutineState: Ending previously active routine");
            currentRoutine.endRoutine(LocalDateTime.now());
        }
        
        // Reset all tasks
        int taskCount = currentRoutine.getTasks().size();
        Log.d(TAG, "resetRoutineState: Resetting " + taskCount + " tasks");
        
        for (Task task : currentRoutine.getTasks()) {
            Log.d(TAG, "resetRoutineState: Resetting task: " + task.getTaskName() + 
                  ", was completed: " + task.isCompleted() + 
                  ", was checked off: " + task.isCheckedOff() + 
                  ", was skipped: " + task.isSkipped());
            task.reset();
            
            // Double-check that isSkipped is definitely false
            if (task.isSkipped()) {
                Log.d(TAG, "resetRoutineState: Task still marked as skipped after reset, explicitly un-skipping: " + task.getTaskName());
                task.setSkipped(false);
            }
            
            // Verify task state after reset
            Log.d(TAG, "resetRoutineState: Task after reset - completed: " + task.isCompleted() + 
                  ", checked off: " + task.isCheckedOff() + 
                  ", skipped: " + task.isSkipped());
        }
        
        // Make sure timers are not running
        if (currentRoutine.getRoutineTimer() != null && currentRoutine.getRoutineTimer().isRunning()) {
            Log.d(TAG, "resetRoutineState: Stopping routine timer");
            currentRoutine.getRoutineTimer().end(LocalDateTime.now());
        }
        
        if (currentRoutine.getTaskTimer() != null && currentRoutine.getTaskTimer().isRunning()) {
            Log.d(TAG, "resetRoutineState: Stopping task timer");
            currentRoutine.getTaskTimer().end(LocalDateTime.now());
        }
        
        // For default routines, always set to manually started
        if (isDefaultRoutine) {
            Log.d(TAG, "resetRoutineState: This is a default routine, ensuring it's marked as manually started");
            manuallyStarted = true;
        } else {
            // For custom routines, check if they have tasks
            manuallyStarted = currentRoutine.getTasks().size() > 0;
            Log.d(TAG, "resetRoutineState: Custom routine, setting manuallyStarted=" + manuallyStarted);
        }
        
        // Always enable timer running state
        isTimerRunning = true;
        
        // Save the fresh state
        if (repository != null) {
            Log.d(TAG, "resetRoutineState: Saving reset routine state to repository");
            repository.updateRoutine(currentRoutine);
        }
        
        // Final state check
        Log.d(TAG, "resetRoutineState: Final state - manuallyStarted=" + manuallyStarted + 
              ", isTimerRunning=" + isTimerRunning + 
              ", routine active=" + currentRoutine.isActive());
    }
}