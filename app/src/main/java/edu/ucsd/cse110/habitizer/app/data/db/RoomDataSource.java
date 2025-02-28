package edu.ucsd.cse110.habitizer.app.data.db;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import edu.ucsd.cse110.habitizer.lib.data.DataStorage;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;

/**
 * Data source implementation using Room database
 */
public class RoomDataSource implements DataStorage {
    private static final String TAG = "RoomDataSource";
    
    private final AppDatabase database;
    private final Executor executor;
    
    /**
     * Constructor
     * @param context Application context
     */
    public RoomDataSource(Context context) {
        this.database = AppDatabase.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    @Override
    public void saveTasks(List<Task> tasks) {
        executor.execute(() -> {
            try {
                List<TaskEntity> taskEntities = new ArrayList<>();
                for (Task task : tasks) {
                    taskEntities.add(TaskEntity.fromTask(task));
                }
                database.taskDao().deleteAll();
                database.taskDao().insertAll(taskEntities);
                Log.d(TAG, "Saved " + tasks.size() + " tasks to database");
            } catch (Exception e) {
                Log.e(TAG, "Error saving tasks", e);
            }
        });
    }
    
    @Override
    public List<Task> loadTasks() {
        try {
            List<TaskEntity> taskEntities = database.taskDao().findAll();
            List<Task> tasks = new ArrayList<>();
            for (TaskEntity entity : taskEntities) {
                tasks.add(entity.toTask());
            }
            Log.d(TAG, "Loaded " + tasks.size() + " tasks from database");
            return tasks;
        } catch (Exception e) {
            Log.e(TAG, "Error loading tasks", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public void saveRoutines(List<Routine> routines) {
        executor.execute(() -> {
            try {
                // Clear old data
                database.routineDao().deleteAllRoutineTaskCrossRefs();
                database.routineDao().deleteAll();
                
                // Save routines
                for (Routine routine : routines) {
                    RoutineEntity routineEntity = RoutineEntity.fromRoutine(routine);
                    long routineId = database.routineDao().insert(routineEntity);
                    
                    // Save associations between routines and tasks
                    List<Task> tasks = routine.getTasks();
                    for (int i = 0; i < tasks.size(); i++) {
                        Task task = tasks.get(i);
                        RoutineTaskCrossRef crossRef = new RoutineTaskCrossRef(
                                (int) routineId, 
                                task.getTaskId(),
                                i  // Save the position of the task in the routine
                        );
                        database.routineDao().insertRoutineTaskCrossRef(crossRef);
                    }
                }
                Log.d(TAG, "Saved " + routines.size() + " routines to database");
            } catch (Exception e) {
                Log.e(TAG, "Error saving routines", e);
            }
        });
    }
    
    @Override
    public List<Routine> loadRoutines() {
        try {
            List<RoutineWithTasks> routinesWithTasks = database.routineDao().getAllRoutinesWithTasks();
            List<Routine> routines = new ArrayList<>();
            
            for (RoutineWithTasks routineWithTasks : routinesWithTasks) {
                routines.add(routineWithTasks.toRoutine());
            }
            
            Log.d(TAG, "Loaded " + routines.size() + " routines from database");
            return routines;
        } catch (Exception e) {
            Log.e(TAG, "Error loading routines", e);
            return new ArrayList<>();
        }
    }
} 