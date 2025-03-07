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
     * Get the database instance
     * @return The AppDatabase instance
     */
    public AppDatabase getDatabase() {
        return database;
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
                
                // Load routines and tasks using our ordered query
                List<RoutineWithTasks> routinesWithTasks = database.routineDao().getAllRoutinesWithTasksOrdered();
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
                // Ensure routine is inserted
                Log.d(TAG, "Inserting routine: " + routine.getRoutineName());
                RoutineEntity routineEntity = database.routineDao().find((int) routine.getRoutineId());
                if (routineEntity == null) {
                    Log.e(TAG, "Routine with ID " + routine.getRoutineId() + " does not exist. Creating a new one.");
                    routineEntity = RoutineEntity.fromRoutine(routine);
                    long routineId = database.routineDao().insert(routineEntity);
                    Log.d(TAG, "Inserted new routine with ID: " + routineId);
                } else {
                    Log.d(TAG, "Updating existing routine with ID: " + routine.getRoutineId());
                    routineEntity.setRoutineName(routine.getRoutineName());
                    database.routineDao().insert(routineEntity);
                }
                
                // Ensure tasks are inserted
                List<Task> tasks = routine.getTasks();
                for (int i = 0; i < tasks.size(); i++) {
                    Task task = tasks.get(i);
                    Log.d(TAG, "Inserting task: " + task.getTaskName());
                    TaskEntity taskEntity = database.taskDao().findByNameExact(task.getTaskName());
                    if (taskEntity == null) {
                        taskEntity = new TaskEntity();
                        taskEntity.setTaskName(task.getTaskName());
                        taskEntity.setCheckedOff(task.isCheckedOff());
                        long taskId = database.taskDao().insert(taskEntity);
                        if (taskId == -1) {
                            Log.e(TAG, "Failed to insert task: " + task.getTaskName());
                            return;
                        }
                        taskEntity.setId((int) taskId);
                        Log.d(TAG, "Inserted task with ID: " + taskId);
                    }

                    // Check existence before cross-reference
                    Log.d(TAG, "Checking existence of routine_id: " + routine.getRoutineId());
                    RoutineEntity existingRoutineEntity = database.routineDao().find((int) routine.getRoutineId());
                    if (existingRoutineEntity == null) {
                        Log.e(TAG, "Routine with ID " + routine.getRoutineId() + " does not exist.");
                        return;
                    }

                    Log.d(TAG, "Checking existence of task_id: " + taskEntity.getId());
                    TaskEntity existingTaskEntity = database.taskDao().find(taskEntity.getId());
                    if (existingTaskEntity == null) {
                        Log.e(TAG, "Task with ID " + taskEntity.getId() + " does not exist.");
                        return;
                    }

                    // Create cross reference
                    RoutineTaskCrossRef crossRef = new RoutineTaskCrossRef(
                        (int) routine.getRoutineId(),
                        taskEntity.getId(),
                        i  // Save the position of the task in the routine
                    );
                    database.routineDao().insertRoutineTaskCrossRef(crossRef);
                    Log.d(TAG, "Added task " + taskEntity.getTaskName() + " to routine " + routineEntity.getRoutineName());
                }
                
                // Update in-memory data
                List<Routine> currentRoutines = new ArrayList<>();
                List<Routine> existingRoutines = routinesSubject.getValue();
                if (existingRoutines != null) {
                    currentRoutines.addAll(existingRoutines);
                }
                
                // Check if the routine already exists and update it instead of adding a duplicate
                boolean routineExists = false;
                for (int i = 0; i < currentRoutines.size(); i++) {
                    if (currentRoutines.get(i).getRoutineId() == routine.getRoutineId()) {
                        currentRoutines.set(i, routine);
                        routineExists = true;
                        Log.d(TAG, "Updated existing routine in list with ID: " + routine.getRoutineId());
                        break;
                    }
                }
                
                // Only add if it doesn't exist
                if (!routineExists) {
                    currentRoutines.add(routine);
                    Log.d(TAG, "Added new routine to list with ID: " + routine.getRoutineId());
                }
                
                // Update observables on main thread
                final List<Routine> finalRoutines = currentRoutines;
                mainHandler.post(() -> {
                    routinesSubject.setValue(finalRoutines);
                    routinesLiveData.setValue(finalRoutines);
                    Log.d(TAG, "Updated observables with " + finalRoutines.size() + " routines");
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
                Log.d(TAG, "Updating routine: " + routine.getRoutineName() + " (ID: " + routine.getRoutineId() + ")");
                
                // Debug the tasks in the routine before saving
                List<Task> tasksList = routine.getTasks();
                Log.d(TAG, "Tasks in routine before saving (" + tasksList.size() + " tasks):");
                for (int i = 0; i < tasksList.size(); i++) {
                    Task t = tasksList.get(i);
                    Log.d(TAG, "  Position " + i + ": " + t.getTaskName() + " (ID: " + t.getTaskId() + ")");
                }
                
                // Delete old associations
                Log.d(TAG, "Deleting old task associations for routine ID: " + routine.getRoutineId());
                database.routineDao().deleteRoutineTaskCrossRefs(routine.getRoutineId());
                
                // Save routine to database
                RoutineEntity routineEntity = RoutineEntity.fromRoutine(routine);
                database.routineDao().insert(routineEntity);
                Log.d(TAG, "Saved routine entity to database: " + routineEntity.getRoutineName());
                
                // Save new associations
                List<Task> tasks = routine.getTasks();
                Log.d(TAG, "Saving " + tasks.size() + " task associations with positions");
                for (int i = 0; i < tasks.size(); i++) {
                    Task task = tasks.get(i);
                    RoutineTaskCrossRef crossRef = new RoutineTaskCrossRef(
                            routine.getRoutineId(), 
                            task.getTaskId(),
                            i  // Save the position of the task in the routine
                    );
                    database.routineDao().insertRoutineTaskCrossRef(crossRef);
                    Log.d(TAG, "  Saved task position " + i + ": " + task.getTaskName() + " (ID: " + task.getTaskId() + ")");
                }
                
                // Update in-memory data
                List<Routine> currentRoutines = new ArrayList<>(routinesSubject.getValue());
                for (int i = 0; i < currentRoutines.size(); i++) {
                    if (currentRoutines.get(i).getRoutineId() == routine.getRoutineId()) {
                        currentRoutines.set(i, routine);
                        Log.d(TAG, "Updated in-memory routine at position " + i);
                        break;
                    }
                }
                
                // Update observables on main thread
                final List<Routine> finalRoutines = currentRoutines;
                mainHandler.post(() -> {
                    routinesSubject.setValue(finalRoutines);
                    routinesLiveData.setValue(finalRoutines);
                    Log.d(TAG, "Updated routine observables on main thread");
                });
                
                Log.d(TAG, "Successfully updated routine: " + routine.getRoutineName() + " with " + tasks.size() + " tasks");
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
    
    /**
     * Remove a task from a routine but keep the task in the database
     * @param routineId ID of the routine
     * @param taskId ID of the task to remove from the routine
     */
    public void removeTaskFromRoutine(int routineId, int taskId) {
        executor.execute(() -> {
            try {
                // Delete the association between routine and task
                database.routineDao().deleteRoutineTaskCrossRef(routineId, taskId);
                
                Log.d(TAG, "Removed task ID: " + taskId + " from routine ID: " + routineId);
                
                // Get the updated routine with remaining tasks
                RoutineWithTasks routineWithTasks = database.routineDao().getRoutineWithTasks(routineId);
                if (routineWithTasks != null) {
                    Routine updatedRoutine = routineWithTasks.toRoutine();
                    
                    // Update in-memory data
                    List<Routine> currentRoutines = new ArrayList<>(routinesSubject.getValue());
                    for (int i = 0; i < currentRoutines.size(); i++) {
                        if (currentRoutines.get(i).getRoutineId() == routineId) {
                            currentRoutines.set(i, updatedRoutine);
                            break;
                        }
                    }
                    
                    // Update observables on main thread
                    final List<Routine> finalRoutines = currentRoutines;
                    mainHandler.post(() -> {
                        routinesSubject.setValue(finalRoutines);
                        routinesLiveData.setValue(finalRoutines);
                    });
                    
                    Log.d(TAG, "Updated routine after task removal. Routine now has " + 
                         updatedRoutine.getTasks().size() + " tasks");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error removing task from routine", e);
            }
        });
    }
    
    /**
     * Refresh routines data from database
     */
    public void refreshRoutines() {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Starting database refresh of routines");
                
                // Load routines and tasks using ordered query
                List<RoutineWithTasks> routinesWithTasks = database.routineDao().getAllRoutinesWithTasksOrdered();
                Log.d(TAG, "Loaded " + routinesWithTasks.size() + " routines with tasks from database");
                
                // Create a new list for domain objects
                List<Routine> routines = new ArrayList<>();
                
                // Check for duplicate routines in the database query result
                Map<Integer, Routine> uniqueRoutineMap = new HashMap<>();
                for (RoutineWithTasks routineWithTasks : routinesWithTasks) {
                    Routine routine = routineWithTasks.toRoutine();
                    uniqueRoutineMap.put(routine.getRoutineId(), routine);
                    
                    // Detailed logging for each routine's tasks
                    Log.d(TAG, "===== Task Details for Routine: " + routine.getRoutineName() + " =====");
                    Log.d(TAG, "Routine ID: " + routine.getRoutineId());
                    Log.d(TAG, "Task Count: " + routine.getTasks().size());
                    if (routine.getTasks().isEmpty()) {
                        Log.d(TAG, "This routine has NO TASKS");
                    } else {
                        for (Task task : routine.getTasks()) {
                            Log.d(TAG, "Task: ID=" + task.getTaskId() + ", Name='" + task.getTaskName() + 
                                  "', Completed=" + task.isCompleted() + ", Skipped=" + task.isSkipped());
                        }
                    }
                    Log.d(TAG, "=================================================");
                    
                    Log.d(TAG, "Processed routine: " + routine.getRoutineName() + " with ID " + routine.getRoutineId() + 
                          " and " + routine.getTasks().size() + " tasks");
                }
                
                routines.addAll(uniqueRoutineMap.values());
                
                if (uniqueRoutineMap.size() < routinesWithTasks.size()) {
                    Log.w(TAG, "Found and removed " + (routinesWithTasks.size() - uniqueRoutineMap.size()) + 
                           " duplicate routines during refresh");
                }
                
                Log.d(TAG, "Final routine list contains " + routines.size() + " routines");
                
                // Update observables on main thread with a guaranteed UI update
                final List<Routine> finalRoutines = routines;
                mainHandler.post(() -> {
                    // Update both subjects with the new data
                    routinesSubject.setValue(finalRoutines);
                    routinesLiveData.setValue(finalRoutines);
                    Log.d(TAG, "Routines refreshed with " + finalRoutines.size() + " routines");
                    
                    // Force a sanity check - verify the values were actually set
                    List<Routine> currentValue = routinesSubject.getValue();
                    Log.d(TAG, "After refresh, subject contains " + (currentValue != null ? currentValue.size() : 0) + " routines");
                });
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing routines", e);
            }
        });
    }
    
    /**
     * Force update the routines in the repository with the provided list.
     * This is primarily used for cleaning up duplicate routines.
     * @param routines The list of routines to set in the repository
     */
    public void setRoutines(List<Routine> routines) {
        Log.d(TAG, "Force updating repository with " + routines.size() + " routines");
        // Update in-memory subject and LiveData immediately for UI
        mainHandler.post(() -> {
            routinesLiveData.setValue(routines);
            routinesSubject.setValue(routines);
            Log.d(TAG, "In-memory routines updated with " + routines.size() + " routines");
        });
        
        // Update database on background thread
        executor.execute(() -> {
            try {
                // Start by getting the current database state
                List<RoutineEntity> existingRoutines = database.routineDao().findAll();
                Log.d(TAG, "Found " + existingRoutines.size() + " existing routines in database");
                
                // Transaction to delete old routines and add new ones
                database.runInTransaction(() -> {
                    // Clear cross references first to avoid foreign key constraints
                    database.routineDao().deleteAllRoutineTaskCrossRefs();
                    
                    // Delete all routines (using a clean slate approach)
                    database.routineDao().deleteAll();
                    
                    // Insert the new routines
                    for (Routine routine : routines) {
                        // Convert domain to entity
                        RoutineEntity routineEntity = new RoutineEntity();
                        routineEntity.setId(routine.getRoutineId());
                        routineEntity.setRoutineName(routine.getRoutineName());
                        
                        // Insert routine
                        long routineId = database.routineDao().insert(routineEntity);
                        Log.d(TAG, "Inserted routine: " + routineEntity.getRoutineName() + " with ID: " + routineId);
                        
                        // Insert task associations
                        for (Task task : routine.getTasks()) {
                            // Get or create task in database
                            TaskEntity taskEntity = database.taskDao().findByNameExact(task.getTaskName());
                            if (taskEntity == null) {
                                taskEntity = new TaskEntity();
                                taskEntity.setTaskName(task.getTaskName());
                                taskEntity.setCheckedOff(task.isCheckedOff());
                                long taskId = database.taskDao().insert(taskEntity);
                                taskEntity.setId((int)taskId);
                                Log.d(TAG, "Created new task: " + taskEntity.getTaskName() + " with ID: " + taskId);
                            }
                            
                            // Before inserting the cross-reference, check if the routine and task exist
                            Log.d(TAG, "Checking existence of routine_id: " + routineId);
                            RoutineEntity existingRoutineEntity = database.routineDao().find((int) routineId);
                            if (existingRoutineEntity == null) {
                                Log.e(TAG, "Routine with ID " + routineId + " does not exist.");
                                return;
                            }

                            Log.d(TAG, "Checking existence of task_id: " + task.getTaskId());
                            TaskEntity existingTaskEntity = database.taskDao().find(task.getTaskId());
                            if (existingTaskEntity == null) {
                                Log.e(TAG, "Task with ID " + task.getTaskId() + " does not exist.");
                                return;
                            }
                            
                            // Create cross reference
                            RoutineTaskCrossRef crossRef = new RoutineTaskCrossRef(
                                (int)routineId,
                                taskEntity.getId(),
                                0  // Default position
                            );
                            database.routineDao().insertRoutineTaskCrossRef(crossRef);
                            Log.d(TAG, "Added task " + taskEntity.getTaskName() + " to routine " + routineEntity.getRoutineName());
                        }
                    }
                });
                
                Log.d(TAG, "Database successfully updated with " + routines.size() + " routines");
                
                // Refresh to confirm changes
                refreshRoutines();
            } catch (Exception e) {
                Log.e(TAG, "Error updating routines in database", e);
            }
        });
    }
} 