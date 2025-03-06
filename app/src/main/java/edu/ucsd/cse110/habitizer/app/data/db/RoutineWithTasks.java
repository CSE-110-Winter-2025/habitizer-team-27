package edu.ucsd.cse110.habitizer.app.data.db;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;

/**
 * Composite data class containing a routine and all its tasks
 */
public class RoutineWithTasks {
    private static final String TAG = "RoutineWithTasks";
    
    @Embedded
    public RoutineEntity routine;
    
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = @Junction(
            value = RoutineTaskCrossRef.class,
            parentColumn = "routine_id",
            entityColumn = "task_id"
        ),
        entity = TaskEntity.class
    )
    public List<TaskEntity> tasks;
    
    /**
     * Convert RoutineWithTasks to domain object Routine
     * @return Domain object Routine
     */
    public Routine toRoutine() {
        Routine domainRoutine = routine.toRoutine();
        
        // Clear any existing tasks because we'll add the tasks retrieved from the database
        domainRoutine.getTasks().clear();
        
        // Get task positions from database
        List<RoutineTaskCrossRef> taskPositions = null;
        try {
            // This retrieves task positions for this routine from cross-reference table
            AppDatabase db = AppDatabase.getInstance(null);
            if (db != null) {
                taskPositions = db.routineDao().getTaskPositions(routine.getId());
                Log.d(TAG, "Retrieved " + (taskPositions != null ? taskPositions.size() : 0) + 
                      " task positions for routine: " + routine.getRoutineName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting task positions", e);
        }
        
        // Process tasks if we have any
        if (tasks != null && !tasks.isEmpty()) {
            Log.d(TAG, "Processing " + tasks.size() + " tasks for routine: " + routine.getRoutineName());
            
            try {
                // Create a map to store tasks by ID for quick lookup
                Map<Integer, Task> taskMap = new HashMap<>();
                
                // Convert tasks to domain objects
                for (TaskEntity taskEntity : tasks) {
                    Task task = taskEntity.toTask();
                    taskMap.put(task.getTaskId(), task);
                    Log.d(TAG, "Converted task: " + task.getTaskName() + " (ID: " + task.getTaskId() + ") for routine " + routine.getRoutineName());
                }
                
                // If we have task positions, use them to order tasks
                if (taskPositions != null && !taskPositions.isEmpty()) {
                    Log.d(TAG, "Ordering tasks using position information from database");
                    
                    // Add tasks in the proper order
                    for (RoutineTaskCrossRef ref : taskPositions) {
                        Task task = taskMap.get(ref.taskId);
                        if (task != null) {
                            domainRoutine.addTask(task);
                            Log.d(TAG, "Added task at position " + ref.taskPosition + ": " + 
                                  task.getTaskName() + " (ID: " + task.getTaskId() + ")");
                        } else {
                            Log.w(TAG, "Could not find task with ID " + ref.taskId + " for position " + ref.taskPosition);
                        }
                    }
                } else {
                    // If no position info, just add all tasks
                    Log.d(TAG, "No position information available, adding tasks in default order");
                    for (Task task : taskMap.values()) {
                        domainRoutine.addTask(task);
                        Log.d(TAG, "Added task: " + task.getTaskName() + " (ID: " + task.getTaskId() + ")");
                    }
                }
                
                Log.d(TAG, "Successfully added " + domainRoutine.getTasks().size() + " tasks to routine " + routine.getRoutineName());
            } catch (Exception e) {
                Log.e(TAG, "Error processing tasks for routine " + routine.getRoutineName(), e);
                
                // Fallback to simple conversion if there's an error
                for (TaskEntity taskEntity : tasks) {
                    domainRoutine.addTask(taskEntity.toTask());
                }
            }
        } else {
            Log.w(TAG, "No tasks found for routine: " + routine.getRoutineName());
        }
        
        // Log the final result
        List<Task> finalTasks = domainRoutine.getTasks();
        Log.d(TAG, "Routine " + routine.getRoutineName() + " converted with " + finalTasks.size() + " tasks in order:");
        for (int i = 0; i < finalTasks.size(); i++) {
            Log.d(TAG, "  Position " + i + ": " + finalTasks.get(i).getTaskName() + " (ID: " + finalTasks.get(i).getTaskId() + ")");
        }
        
        return domainRoutine;
    }
} 