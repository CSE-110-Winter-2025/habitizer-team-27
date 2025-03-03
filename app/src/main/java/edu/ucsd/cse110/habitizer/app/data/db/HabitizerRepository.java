package edu.ucsd.cse110.habitizer.app.data.db;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;
import edu.ucsd.cse110.observables.MutableSubject;
import edu.ucsd.cse110.observables.Subject;
import edu.ucsd.cse110.observables.PlainMutableSubject;

/**
 * Application data repository, encapsulating Room database operations
 */
public class HabitizerRepository {
    private static final String TAG = "HabitizerRepository";
    private static int instanceCount = 0;
    private final int instanceId;
    
    private final AppDatabase database;
    private final Executor executor;
    private final Handler mainHandler;
    
    // Observable data collections
    private final MutableSubject<List<Task>> tasksSubject = new PlainMutableSubject<>(new ArrayList<>());
    private final MutableSubject<List<Routine>> routinesSubject = new PlainMutableSubject<>(new ArrayList<>());
    
    // LiveData for caching data
    private final MutableLiveData<List<Task>> tasksLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Routine>> routinesLiveData = new MutableLiveData<>(new ArrayList<>());
    
    /**
     * Singleton instance
     */
    private static HabitizerRepository INSTANCE;
    
    /**
     * Get repository instance
     * @param context Application context
     * @return Repository instance
     */
    public static HabitizerRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (HabitizerRepository.class) {
                if (INSTANCE == null) {
                    Log.d(TAG, "Creating new repository instance");
                    INSTANCE = new HabitizerRepository(context);
                }
            }
        } else {
            Log.d(TAG, "Returning existing repository instance #" + INSTANCE.instanceId);
        }
        return INSTANCE;
    }
    
    /**
     * Reset the repository instance (for testing)
     * This will also reset the database instance
     */
    public static void resetInstance() {
        Log.d(TAG, "Resetting repository instance");
        synchronized (HabitizerRepository.class) {
            INSTANCE = null;
            // Also reset the database
            AppDatabase.resetInstance();
        }
    }
    
    /**
     * Private constructor
     * @param context Application context
     */
    private HabitizerRepository(Context context) {
        instanceId = ++instanceCount;
        Log.d(TAG, "Initializing repository instance #" + instanceId);
        
        this.database = AppDatabase.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        // Load initial data from database
        loadInitialData();
    }
    
    /**
     * Load initial data from database
     */
    private void loadInitialData() {
        Log.d(TAG, "Starting to load initial data for instance #" + instanceId);
        executor.execute(() -> {
            try {
                // Load tasks
                List<TaskEntity> taskEntities = database.taskDao().findAll();
                List<Task> tasks = new ArrayList<>();
                for (TaskEntity entity : taskEntities) {
                    tasks.add(entity.toTask());
                }
                Log.d(TAG, "Loaded " + tasks.size() + " tasks from database");
                
                // Load routines and tasks
                List<RoutineWithTasks> routinesWithTasks = database.routineDao().getAllRoutinesWithTasks();
                List<Routine> routines = new ArrayList<>();
                
                // Check for duplicate routines in the database query result
                Map<Integer, Routine> uniqueRoutineMap = new HashMap<>();
                for (RoutineWithTasks routineWithTasks : routinesWithTasks) {
                    Routine routine = routineWithTasks.toRoutine();
                    uniqueRoutineMap.put(routine.getRoutineId(), routine);
                }
                
                routines.addAll(uniqueRoutineMap.values());
                
                if (uniqueRoutineMap.size() < routinesWithTasks.size()) {
                    Log.w(TAG, "Found and removed " + (routinesWithTasks.size() - uniqueRoutineMap.size()) + 
                           " duplicate routines in database query result");
                }
                
                Log.d(TAG, "Loaded " + routines.size() + " unique routines from database");
                
                // Log details of each routine
                if (routines.size() > 0) {
                    for (int i = 0; i < routines.size(); i++) {
                        Routine routine = routines.get(i);
                        Log.d(TAG, "Routine " + i + ": id=" + routine.getRoutineId() + 
                              ", name=" + routine.getRoutineName() + 
                              ", tasks=" + routine.getTasks().size());
                    }
                }
                
                // Update observables on main thread
                final List<Task> finalTasks = tasks;
                final List<Routine> finalRoutines = routines;
                mainHandler.post(() -> {
                    Log.d(TAG, "Setting repository subjects with " + finalTasks.size() + 
                          " tasks and " + finalRoutines.size() + " routines");
                    
                    tasksSubject.setValue(finalTasks);
                    routinesSubject.setValue(finalRoutines);
                    
                    // Update LiveData
                    tasksLiveData.setValue(finalTasks);
                    routinesLiveData.setValue(finalRoutines);
                    
                    Log.d(TAG, "Initial data loaded for instance #" + instanceId);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading data from database", e);
            }
        });
    }
    
    /**
     * Get observable for all tasks
     * @return Task observable
     */
    public Subject<List<Task>> getTasks() {
        Log.d(TAG, "getTasks() called for instance #" + instanceId + 
              " with " + (tasksSubject.getValue() != null ? tasksSubject.getValue().size() : 0) + " tasks");
        return tasksSubject;
    }
    
    /**
     * Get observable for all routines
     * @return Routine observable
     */
    public Subject<List<Routine>> getRoutines() {
        Log.d(TAG, "getRoutines() called for instance #" + instanceId + 
              " with " + (routinesSubject.getValue() != null ? routinesSubject.getValue().size() : 0) + " routines");
        return routinesSubject;
    }
    
    /**
     * Get LiveData for all tasks
     * @return Task LiveData
     */
    public LiveData<List<Task>> getTasksAsLiveData() {
        return tasksLiveData;
    }
    
    /**
     * Get LiveData for all routines
     * @return Routine LiveData
     */
    public LiveData<List<Routine>> getRoutinesAsLiveData() {
        return routinesLiveData;
    }
    
    /**
     * Add a task
     * @param task Task to add
     */
    public void addTask(Task task) {
        executor.execute(() -> {
            try {
                // Save to database
                TaskEntity entity = TaskEntity.fromTask(task);
                database.taskDao().insert(entity);
                
                // Update in-memory data
                List<Task> currentTasks = new ArrayList<>(tasksSubject.getValue());
                currentTasks.add(task);
                
                // Update observables on main thread
                final List<Task> finalTasks = currentTasks;
                mainHandler.post(() -> {
                    tasksSubject.setValue(finalTasks);
                    tasksLiveData.setValue(finalTasks);
                });
                
                Log.d(TAG, "Added task: " + task.getTaskName());
            } catch (Exception e) {
                Log.e(TAG, "Error adding task", e);
            }
        });
    }
    
    /**
     * Update a task
     * @param task Task to update
     */
    public void updateTask(Task task) {
        executor.execute(() -> {
            try {
                // Save to database
                TaskEntity entity = TaskEntity.fromTask(task);
                database.taskDao().insert(entity);
                
                // Update in-memory data
                List<Task> currentTasks = new ArrayList<>(tasksSubject.getValue());
                for (int i = 0; i < currentTasks.size(); i++) {
                    if (currentTasks.get(i).getTaskId() == task.getTaskId()) {
                        currentTasks.set(i, task);
                        break;
                    }
                }
                
                // Update observables on main thread
                final List<Task> finalTasks = currentTasks;
                mainHandler.post(() -> {
                    tasksSubject.setValue(finalTasks);
                    tasksLiveData.setValue(finalTasks);
                });
                
                Log.d(TAG, "Updated task: " + task.getTaskName());
            } catch (Exception e) {
                Log.e(TAG, "Error updating task", e);
            }
        });
    }
    
    /**
     * Delete a task
     * @param taskId ID of the task to delete
     */
    public void deleteTask(int taskId) {
        executor.execute(() -> {
            try {
                // Delete from database
                database.taskDao().delete(taskId);
                
                // Update in-memory data
                List<Task> currentTasks = new ArrayList<>(tasksSubject.getValue());
                for (int i = 0; i < currentTasks.size(); i++) {
                    if (currentTasks.get(i).getTaskId() == taskId) {
                        currentTasks.remove(i);
                        break;
                    }
                }
                
                // Update observables on main thread
                final List<Task> finalTasks = currentTasks;
                mainHandler.post(() -> {
                    tasksSubject.setValue(finalTasks);
                    tasksLiveData.setValue(finalTasks);
                });
                
                Log.d(TAG, "Deleted task with ID: " + taskId);
            } catch (Exception e) {
                Log.e(TAG, "Error deleting task", e);
            }
        });
    }
    
    /**
     * Add a routine
     * @param routine Routine to add
     */
    public void addRoutine(Routine routine) {
        executor.execute(() -> {
            try {
                // Save to database
                RoutineEntity routineEntity = RoutineEntity.fromRoutine(routine);
                long routineId = database.routineDao().insert(routineEntity);
                
                // Save associations between routine and tasks
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
                
                // Update in-memory data
                List<Routine> currentRoutines = new ArrayList<>(routinesSubject.getValue());
                currentRoutines.add(routine);
                
                // Update observables on main thread
                final List<Routine> finalRoutines = currentRoutines;
                mainHandler.post(() -> {
                    routinesSubject.setValue(finalRoutines);
                    routinesLiveData.setValue(finalRoutines);
                });
                
                Log.d(TAG, "Added routine: " + routine.getRoutineName() + " with " + tasks.size() + " tasks");
            } catch (Exception e) {
                Log.e(TAG, "Error adding routine", e);
            }
        });
    }
    
    /**
     * Update a routine
     * @param routine Routine to update
     */
    public void updateRoutine(Routine routine) {
        executor.execute(() -> {
            try {
                // Delete old associations
                database.routineDao().deleteRoutineTaskCrossRefs(routine.getRoutineId());
                
                // Save routine to database
                RoutineEntity routineEntity = RoutineEntity.fromRoutine(routine);
                database.routineDao().insert(routineEntity);
                
                // Save new associations
                List<Task> tasks = routine.getTasks();
                for (int i = 0; i < tasks.size(); i++) {
                    Task task = tasks.get(i);
                    RoutineTaskCrossRef crossRef = new RoutineTaskCrossRef(
                            routine.getRoutineId(), 
                            task.getTaskId(),
                            i  // Save the position of the task in the routine
                    );
                    database.routineDao().insertRoutineTaskCrossRef(crossRef);
                }
                
                // Update in-memory data
                List<Routine> currentRoutines = new ArrayList<>(routinesSubject.getValue());
                for (int i = 0; i < currentRoutines.size(); i++) {
                    if (currentRoutines.get(i).getRoutineId() == routine.getRoutineId()) {
                        currentRoutines.set(i, routine);
                        break;
                    }
                }
                
                // Update observables on main thread
                final List<Routine> finalRoutines = currentRoutines;
                mainHandler.post(() -> {
                    routinesSubject.setValue(finalRoutines);
                    routinesLiveData.setValue(finalRoutines);
                });
                
                Log.d(TAG, "Updated routine: " + routine.getRoutineName() + " with " + tasks.size() + " tasks");
            } catch (Exception e) {
                Log.e(TAG, "Error updating routine", e);
            }
        });
    }
    
    /**
     * Delete a routine
     * @param routineId ID of the routine to delete
     */
    public void deleteRoutine(int routineId) {
        executor.execute(() -> {
            try {
                // Delete associations between routine and tasks
                database.routineDao().deleteRoutineTaskCrossRefs(routineId);
                
                // Delete routine from database
                database.routineDao().delete(routineId);
                
                // Update in-memory data
                List<Routine> currentRoutines = new ArrayList<>(routinesSubject.getValue());
                for (int i = 0; i < currentRoutines.size(); i++) {
                    if (currentRoutines.get(i).getRoutineId() == routineId) {
                        currentRoutines.remove(i);
                        break;
                    }
                }
                
                // Update observables on main thread
                final List<Routine> finalRoutines = currentRoutines;
                mainHandler.post(() -> {
                    routinesSubject.setValue(finalRoutines);
                    routinesLiveData.setValue(finalRoutines);
                });
                
                Log.d(TAG, "Deleted routine with ID: " + routineId);
            } catch (Exception e) {
                Log.e(TAG, "Error deleting routine", e);
            }
        });
    }
} 