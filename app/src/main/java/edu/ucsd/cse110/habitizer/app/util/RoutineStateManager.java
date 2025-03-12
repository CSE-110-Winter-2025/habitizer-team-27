package edu.ucsd.cse110.habitizer.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;
import edu.ucsd.cse110.habitizer.app.data.db.HabitizerRepository;

/**
 * Utility class to manage saving and restoring the state of a running routine
 * when the app is closed and reopened.
 */
public class RoutineStateManager {
    private static final String TAG = "RoutineStateManager";
    private static final String PREFS_NAME = "routine_state";
    
    // Keys for SharedPreferences
    private static final String KEY_ACTIVE_ROUTINE_ID = "active_routine_id";
    private static final String KEY_ROUTINE_START_TIME = "routine_start_time";
    private static final String KEY_IS_PAUSED = "is_paused";
    private static final String KEY_PAUSE_TIME = "pause_time";
    private static final String KEY_HAS_ACTIVE_ROUTINE = "has_active_routine";
    
    // Additional keys for UI state
    private static final String KEY_ELAPSED_MINUTES = "elapsed_minutes";
    private static final String KEY_IS_TIMER_RUNNING = "is_timer_running";
    private static final String KEY_IS_MANUALLY_STARTED = "is_manually_started";
    private static final String KEY_IS_TIMER_STOPPED = "is_timer_stopped";
    private static final String KEY_TIME_BEFORE_PAUSE_MINUTES = "time_before_pause_minutes";
    private static final String KEY_TASK_TIME_BEFORE_PAUSE_MINUTES = "task_time_before_pause_minutes";
    private static final String KEY_TASK_SECONDS_BEFORE_PAUSE = "task_seconds_before_pause";
    private static final String KEY_CURRENT_TASK_INDEX = "current_task_index";
    
    // New keys for enhanced state saving
    private static final String KEY_GOAL_TIME = "goal_time";
    private static final String KEY_CURRENT_TASK_ELAPSED_TIME = "current_task_elapsed_time";
    private static final String KEY_TASKS_JSON = "tasks_json";
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    private final Context context;
    private final SharedPreferences prefs;
    
    public RoutineStateManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Save the state of a running routine including detailed task information
     * @param routine The routine to save
     * @param isTimerRunning Whether the timer is running
     * @param isPaused Whether the timer is paused
     * @param isManuallyStarted Whether the routine was manually started
     * @param isTimerStopped Whether the timer is stopped
     * @param timeBeforePauseMinutes Time before pause in minutes
     * @param taskTimeBeforePauseMinutes Task time before pause in minutes
     * @param taskSecondsBeforePause Task seconds before pause
     * @param currentTaskIndex Index of the current task
     * @param currentTaskElapsedTime Elapsed time of the current task in seconds
     */
    public void saveFullRoutineState(
            Routine routine, 
            boolean isTimerRunning,
            boolean isPaused, 
            boolean isManuallyStarted,
            boolean isTimerStopped,
            long timeBeforePauseMinutes,
            long taskTimeBeforePauseMinutes,
            int taskSecondsBeforePause,
            int currentTaskIndex,
            int currentTaskElapsedTime) {
        
        if (routine == null) {
            Log.w(TAG, "Attempted to save null routine state, clearing instead");
            clearRunningRoutineState();
            return;
        }
        
        if (!routine.isActive() && !isManuallyStarted) {
            Log.d(TAG, "Routine is not active and not manually started, not saving state");
            clearRunningRoutineState();
            return;
        }
        
        Log.d(TAG, "Saving full state for routine: " + routine.getRoutineName() + " (ID: " + routine.getRoutineId() + ")");
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_HAS_ACTIVE_ROUTINE, true);
        editor.putInt(KEY_ACTIVE_ROUTINE_ID, routine.getRoutineId());
        
        // Save routine timer state
        LocalDateTime startTime = routine.getRoutineTimer().getStartTime();
        if (startTime != null) {
            editor.putString(KEY_ROUTINE_START_TIME, DATE_TIME_FORMATTER.format(startTime));
        }
        
        // Save UI state
        editor.putBoolean(KEY_IS_PAUSED, isPaused);
        editor.putBoolean(KEY_IS_TIMER_RUNNING, isTimerRunning);
        editor.putBoolean(KEY_IS_MANUALLY_STARTED, isManuallyStarted);
        editor.putBoolean(KEY_IS_TIMER_STOPPED, isTimerStopped);
        
        // Save elapsed time explicitly for more reliability
        long routineElapsedMinutes = Math.max(0, routine.getRoutineDurationMinutes());
        editor.putLong(KEY_ELAPSED_MINUTES, routineElapsedMinutes);
        Log.d(TAG, "Saved routine elapsed minutes: " + routineElapsedMinutes);
        
        editor.putLong(KEY_TIME_BEFORE_PAUSE_MINUTES, Math.max(0, timeBeforePauseMinutes));
        editor.putLong(KEY_TASK_TIME_BEFORE_PAUSE_MINUTES, Math.max(0, taskTimeBeforePauseMinutes));
        editor.putInt(KEY_TASK_SECONDS_BEFORE_PAUSE, Math.max(0, taskSecondsBeforePause));
        editor.putInt(KEY_CURRENT_TASK_INDEX, currentTaskIndex);
        
        // Save current task elapsed time explicitly
        editor.putInt(KEY_CURRENT_TASK_ELAPSED_TIME, Math.max(0, currentTaskElapsedTime));
        Log.d(TAG, "Saved current task elapsed time: " + currentTaskElapsedTime + " seconds");
        
        // Save goal time
        if (routine.getGoalTime() != null) {
            editor.putInt(KEY_GOAL_TIME, routine.getGoalTime());
        } else {
            editor.remove(KEY_GOAL_TIME);
        }
        
        // Save detailed task information
        try {
            JSONArray tasksArray = new JSONArray();
            // Make a copy of the task list to avoid potential race conditions
            List<Task> tasksCopy = new ArrayList<>(routine.getTasks());
            for (Task task : tasksCopy) {
                JSONObject taskObject = new JSONObject();
                taskObject.put("id", task.getTaskId());
                taskObject.put("name", task.getTaskName());
                taskObject.put("completed", task.isCompleted());
                taskObject.put("skipped", task.isSkipped());
                taskObject.put("duration", task.getDuration());
                taskObject.put("elapsedSeconds", task.getElapsedSeconds());
                tasksArray.put(taskObject);
            }
            
            editor.putString(KEY_TASKS_JSON, tasksArray.toString());
            Log.d(TAG, "Saved " + tasksCopy.size() + " tasks to SharedPreferences");
        } catch (JSONException e) {
            Log.e(TAG, "Error saving tasks to JSON", e);
        }
        
        if (isPaused || isTimerStopped) {
            // Save the pause time
            editor.putString(KEY_PAUSE_TIME, DATE_TIME_FORMATTER.format(routine.getCurrentTime()));
        }
        
        editor.apply();
        Log.d(TAG, "Saved full routine state: ID=" + routine.getRoutineId() + 
              ", isPaused=" + isPaused + 
              ", isTimerRunning=" + isTimerRunning +
              ", isManuallyStarted=" + isManuallyStarted +
              ", timeBeforePause=" + timeBeforePauseMinutes + "m" +
              ", goalTime=" + (routine.getGoalTime() != null ? routine.getGoalTime() : "null"));
    }
    
    /**
     * Save the state of a running routine
     * @param routine The routine to save
     */
    public void saveRunningRoutineState(Routine routine) {
        if (routine == null) {
            Log.w(TAG, "Attempted to save null routine state, clearing instead");
            clearRunningRoutineState();
            return;
        }
        
        if (!routine.isActive()) {
            Log.d(TAG, "Routine is not active, not saving state");
            clearRunningRoutineState();
            return;
        }
        
        Log.d(TAG, "Saving state for active routine: " + routine.getRoutineName() + " (ID: " + routine.getRoutineId() + ")");
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_HAS_ACTIVE_ROUTINE, true);
        editor.putInt(KEY_ACTIVE_ROUTINE_ID, routine.getRoutineId());
        
        // Save start time
        LocalDateTime startTime = routine.getRoutineTimer().getStartTime();
        if (startTime != null) {
            editor.putString(KEY_ROUTINE_START_TIME, DATE_TIME_FORMATTER.format(startTime));
        }
        
        // Check if routine is paused
        boolean isPaused = routine.getRoutineTimer().getEndTime() == null && routine.getCurrentTime() != null;
        editor.putBoolean(KEY_IS_PAUSED, isPaused);
        
        if (isPaused) {
            // Save the pause time
            editor.putString(KEY_PAUSE_TIME, DATE_TIME_FORMATTER.format(routine.getCurrentTime()));
        }
        
        editor.apply();
        Log.d(TAG, "Saved running routine state: ID=" + routine.getRoutineId() + 
              ", isPaused=" + isPaused + 
              ", startTime=" + (startTime != null ? startTime.toString() : "null"));
    }
    
    /**
     * Check if there is a saved running routine
     * @return true if there is a saved running routine
     */
    public boolean hasRunningRoutine() {
        return prefs.getBoolean(KEY_HAS_ACTIVE_ROUTINE, false);
    }
    
    /**
     * Get the ID of the saved running routine
     * @return The routine ID, or -1 if no routine is saved
     */
    public int getRunningRoutineId() {
        if (!hasRunningRoutine()) {
            return -1;
        }
        return prefs.getInt(KEY_ACTIVE_ROUTINE_ID, -1);
    }
    
    /**
     * Get the UI state for restored routine
     * @return A bundle containing UI state values
     */
    public RoutineUIState getUIState() {
        if (!hasRunningRoutine()) {
            return null;
        }
        
        RoutineUIState state = new RoutineUIState();
        state.isTimerRunning = prefs.getBoolean(KEY_IS_TIMER_RUNNING, true);
        state.isPaused = prefs.getBoolean(KEY_IS_PAUSED, false);
        state.isManuallyStarted = prefs.getBoolean(KEY_IS_MANUALLY_STARTED, false);
        state.isTimerStopped = prefs.getBoolean(KEY_IS_TIMER_STOPPED, false);
        state.timeBeforePauseMinutes = Math.max(0, prefs.getLong(KEY_TIME_BEFORE_PAUSE_MINUTES, 0));
        state.taskTimeBeforePauseMinutes = Math.max(0, prefs.getLong(KEY_TASK_TIME_BEFORE_PAUSE_MINUTES, 0));
        state.taskSecondsBeforePause = Math.max(0, prefs.getInt(KEY_TASK_SECONDS_BEFORE_PAUSE, 0));
        state.currentTaskIndex = prefs.getInt(KEY_CURRENT_TASK_INDEX, 0);
        state.elapsedMinutes = Math.max(0, prefs.getLong(KEY_ELAPSED_MINUTES, 0));
        state.currentTaskElapsedTime = Math.max(0, prefs.getInt(KEY_CURRENT_TASK_ELAPSED_TIME, 0));
        
        // Load goal time if saved
        if (prefs.contains(KEY_GOAL_TIME)) {
            state.goalTime = prefs.getInt(KEY_GOAL_TIME, 0);
        }
        
        // Load saved tasks
        state.savedTasks = loadSavedTasks();
        
        Log.d(TAG, "Retrieved UI state: " +
              "isTimerRunning=" + state.isTimerRunning +
              ", isPaused=" + state.isPaused +
              ", isManuallyStarted=" + state.isManuallyStarted +
              ", timeBeforePauseMinutes=" + state.timeBeforePauseMinutes +
              ", currentTaskElapsedTime=" + state.currentTaskElapsedTime);
        
        return state;
    }
    
    /**
     * Load saved tasks from SharedPreferences
     * @return List of saved tasks or null if no tasks were saved
     */
    private List<SavedTask> loadSavedTasks() {
        String tasksJson = prefs.getString(KEY_TASKS_JSON, null);
        if (tasksJson == null) {
            return null;
        }
        
        List<SavedTask> tasks = new ArrayList<>();
        try {
            JSONArray tasksArray = new JSONArray(tasksJson);
            for (int i = 0; i < tasksArray.length(); i++) {
                JSONObject taskObject = tasksArray.getJSONObject(i);
                SavedTask task = new SavedTask();
                task.id = taskObject.getInt("id");
                task.name = taskObject.getString("name");
                task.completed = taskObject.getBoolean("completed");
                task.skipped = taskObject.getBoolean("skipped");
                task.duration = taskObject.getInt("duration");
                task.elapsedSeconds = taskObject.getInt("elapsedSeconds");
                tasks.add(task);
            }
            
            Log.d(TAG, "Loaded " + tasks.size() + " tasks from SharedPreferences");
            return tasks;
        } catch (JSONException e) {
            Log.e(TAG, "Error loading tasks from JSON", e);
            return null;
        }
    }
    
    /**
     * Restore the state of a saved running routine
     * @param routine The routine to restore state for
     */
    public void restoreRoutineState(Routine routine) {
        if (routine == null || !hasRunningRoutine() || 
            routine.getRoutineId() != getRunningRoutineId()) {
            Log.d(TAG, "No saved state to restore for routine");
            return;
        }
        
        Log.d(TAG, "Restoring state for routine: " + routine.getRoutineName());
        
        // Restore goal time
        if (prefs.contains(KEY_GOAL_TIME)) {
            routine.updateGoalTime(prefs.getInt(KEY_GOAL_TIME, 0));
            Log.d(TAG, "Restored goal time: " + routine.getGoalTime());
        }
        
        // Get saved start time
        String startTimeStr = prefs.getString(KEY_ROUTINE_START_TIME, null);
        if (startTimeStr != null) {
            try {
                LocalDateTime startTime = LocalDateTime.parse(startTimeStr, DATE_TIME_FORMATTER);
                // Start the routine with the saved time
                routine.startRoutine(startTime);
                Log.d(TAG, "Restored routine start time: " + startTime);
                
                // Check if routine was paused
                boolean wasPaused = prefs.getBoolean(KEY_IS_PAUSED, false);
                if (wasPaused) {
                    String pauseTimeStr = prefs.getString(KEY_PAUSE_TIME, null);
                    if (pauseTimeStr != null) {
                        try {
                            LocalDateTime pauseTime = LocalDateTime.parse(pauseTimeStr, DATE_TIME_FORMATTER);
                            routine.pauseTime(pauseTime);
                            Log.d(TAG, "Restored routine pause time: " + pauseTime);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing pause time: " + pauseTimeStr, e);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing start time: " + startTimeStr, e);
            }
        }
        
        // Restore task state
        List<SavedTask> savedTasks = loadSavedTasks();
        if (savedTasks != null && !savedTasks.isEmpty()) {
            // Clear existing tasks
            List<Task> existingTasks = new ArrayList<>(routine.getTasks());
            for (Task task : existingTasks) {
                routine.removeTask(task);
            }
            
            // Add saved tasks
            for (SavedTask savedTask : savedTasks) {
                Task task = new Task(savedTask.id, savedTask.name, savedTask.completed);
                if (savedTask.completed) {
                    task.setDurationAndComplete(savedTask.duration);
                    task.setElapsedSeconds(savedTask.elapsedSeconds);
                }
                if (savedTask.skipped) {
                    task.setSkipped(true);
                }
                routine.addTask(task);
            }
            
            Log.d(TAG, "Restored " + savedTasks.size() + " tasks to routine");
        }
    }
    
    /**
     * Clear the saved routine state
     */
    public void clearRunningRoutineState() {
        Log.d(TAG, "Clearing saved routine state");
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_HAS_ACTIVE_ROUTINE, false);
        editor.remove(KEY_ACTIVE_ROUTINE_ID);
        editor.remove(KEY_ROUTINE_START_TIME);
        editor.remove(KEY_IS_PAUSED);
        editor.remove(KEY_PAUSE_TIME);
        editor.remove(KEY_IS_TIMER_RUNNING);
        editor.remove(KEY_IS_MANUALLY_STARTED);
        editor.remove(KEY_IS_TIMER_STOPPED);
        editor.remove(KEY_TIME_BEFORE_PAUSE_MINUTES);
        editor.remove(KEY_TASK_TIME_BEFORE_PAUSE_MINUTES);
        editor.remove(KEY_TASK_SECONDS_BEFORE_PAUSE);
        editor.remove(KEY_CURRENT_TASK_INDEX);
        editor.remove(KEY_ELAPSED_MINUTES);
        editor.remove(KEY_GOAL_TIME);
        editor.remove(KEY_CURRENT_TASK_ELAPSED_TIME);
        editor.remove(KEY_TASKS_JSON);
        editor.apply();
    }
    
    /**
     * Class to hold UI state values for a routine
     */
    public static class RoutineUIState {
        public boolean isTimerRunning = true;
        public boolean isPaused = false;
        public boolean isManuallyStarted = false;
        public boolean isTimerStopped = false;
        public long timeBeforePauseMinutes = 0;
        public long taskTimeBeforePauseMinutes = 0;
        public int taskSecondsBeforePause = 0;
        public int currentTaskIndex = 0;
        public long elapsedMinutes = 0;
        public Integer goalTime = null;
        public int currentTaskElapsedTime = 0;
        public List<SavedTask> savedTasks = null;
    }
    
    /**
     * Class to hold saved task information
     */
    public static class SavedTask {
        public int id;
        public String name;
        public boolean completed;
        public boolean skipped;
        public int duration;
        public int elapsedSeconds;
    }
} 