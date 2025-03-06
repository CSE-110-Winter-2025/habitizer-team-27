package edu.ucsd.cse110.habitizer.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import edu.ucsd.cse110.habitizer.lib.domain.Routine;
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
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    private final Context context;
    private final SharedPreferences prefs;
    
    public RoutineStateManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
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
        editor.apply();
    }
} 