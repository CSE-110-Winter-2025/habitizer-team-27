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
        Log.d("REPOSITORY_UPDATE", "Starting updateRoutine for " + routine.getRoutineName() + " (ID: " + routine.getRoutineId() + ")");
        
        // Log task state
        List<Task> tasks = routine.getTasks();
        Log.d("REPOSITORY_UPDATE", "Task count: " + tasks.size());
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            Log.d("REPOSITORY_UPDATE", "Task at position " + i + ": " + task.getTaskName() + " (ID: " + task.getTaskId() + ")");
        }
        
        // Create a final copy of tasks list for use in lambda
        final List<Task> tasksList = new ArrayList<>(routine.getTasks());
        
        executor.execute(() -> {
            try {
                // Wrap everything in a transaction to ensure atomicity
                database.runInTransaction(() -> {
                    Log.d("REPOSITORY_UPDATE", "Deleting old task associations for routine " + routine.getRoutineId());
                    // Delete old associations
                    database.routineDao().deleteRoutineTaskCrossRefs(routine.getRoutineId());
                    
                    Log.d("REPOSITORY_UPDATE", "Saving routine entity to database");
                    // Save routine to database
                    RoutineEntity routineEntity = RoutineEntity.fromRoutine(routine);
                    database.routineDao().insert(routineEntity);
                    
                    Log.d("REPOSITORY_UPDATE", "Saving task associations");
                    // Save new associations
                    if (tasksList.isEmpty()) {
                        Log.e("REPOSITORY_UPDATE", "WARNING: Task list is empty for routine: " + routine.getRoutineName());
                    }
                    
                    int associationsSaved = 0;
                    
                    for (int i = 0; i < tasksList.size(); i++) {
                        Task task = tasksList.get(i);
                        RoutineTaskCrossRef crossRef = new RoutineTaskCrossRef(
                                routine.getRoutineId(), 
                                task.getTaskId(),
                                i  // index within the task list
                        );
                        
                        Log.d("REPOSITORY_UPDATE", "Saving cross-ref for task " + task.getTaskName() + 
                              " (ID: " + task.getTaskId() + ") at position " + i);
                              
                        database.routineDao().insertRoutineTaskCrossRef(crossRef);
                        associationsSaved++;
                        
                        // Also ensure the task itself is saved
                        TaskEntity taskEntity = TaskEntity.fromTask(task);
                        database.taskDao().insert(taskEntity);
                    }
                    
                    Log.d("REPOSITORY_UPDATE", "Transaction completed - saved " + associationsSaved + " task associations");
                });
                
                // Verify task associations were saved properly
                List<RoutineTaskCrossRef> savedAssocs = 
                    database.routineDao().getTaskPositions(routine.getRoutineId());
                Log.d("REPOSITORY_UPDATE", "Saved " + savedAssocs.size() + 
                      " task associations for routine " + routine.getRoutineName());
                
                if (savedAssocs.size() != tasksList.size()) {
                    Log.e("REPOSITORY_UPDATE", "ERROR: Mismatch in saved associations! Expected " + 
                         tasksList.size() + " but found " + savedAssocs.size());
                }
                
                // Brief delay to ensure all database writes are complete
                try {
                    Log.d("REPOSITORY_UPDATE", "Waiting for database operations to complete");
                    Thread.sleep(250); // Increased delay for better reliability
                } catch (InterruptedException e) {
                    Log.e("REPOSITORY_UPDATE", "Sleep interrupted", e);
                }
                
                Log.d("REPOSITORY_UPDATE", "Database update complete, refreshing in-memory data");
                
                // Force refresh of in-memory collections
                refreshRoutines();
                
                // Log the state after refresh
                mainHandler.post(() -> {
                    Log.d("REPOSITORY_UPDATE", "After refresh - Checking routine state");
                    List<Routine> allRoutines = routinesSubject.getValue();
                    if (allRoutines != null) {
                        for (Routine r : allRoutines) {
                            if (r.getRoutineId() == routine.getRoutineId()) {
                                Log.d("REPOSITORY_UPDATE", "Found updated routine: " + r.getRoutineName() + 
                                      " with " + r.getTasks().size() + " tasks");
                                for (int i = 0; i < r.getTasks().size(); i++) {
                                    Task t = r.getTasks().get(i);
                                    Log.d("REPOSITORY_UPDATE", "  Task at position " + i + ": " + 
                                          t.getTaskName() + " (ID: " + t.getTaskId() + ")");
                                }
                                if (r.getTasks().size() != tasksList.size()) {
                                    Log.e("REPOSITORY_UPDATE", "ERROR: Task count mismatch after refresh! " +
                                          "Original: " + tasksList.size() + ", After refresh: " + r.getTasks().size());
                                }
                                break;
                            }
                        }
                    }
                });
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
     * Refresh routines data from database
     */
    public void refreshRoutines() {
        executor.execute(() -> {
            try {
                Log.d("HabitizerRepository", "Starting database refresh of routines");
                
                // First check what task associations exist in the database
                List<RoutineTaskCrossRef> allTaskAssociations = database.routineDao().getAllTaskRelationshipsOrdered();
                Log.d("HabitizerRepository", "Found " + allTaskAssociations.size() + " task associations in database");
                
                // Extra verification - log the first 10 associations to help debug
                for (int i = 0; i < Math.min(10, allTaskAssociations.size()); i++) {
                    RoutineTaskCrossRef ref = allTaskAssociations.get(i);
                    Log.d("HabitizerRepository", "Association " + i + ": RoutineID=" + ref.routineId + 
                          ", TaskID=" + ref.taskId + ", Position=" + ref.taskPosition);
                }
                
                // Load routines with tasks
                List<RoutineWithTasks> routinesWithTasks = database.routineDao().getAllRoutinesWithTasksOrdered();
                Log.d("HabitizerRepository", "Loaded " + routinesWithTasks.size() + " routines with tasks from database");
                
                List<Routine> routines = new ArrayList<>();
                
                // For each routine, check its tasks
                for (RoutineWithTasks routineWithTasks : routinesWithTasks) {
                    RoutineEntity routineEntity = routineWithTasks.routine;
                    List<TaskEntity> taskEntities = routineWithTasks.tasks;
                    
                    Log.d("HabitizerRepository", "Routine: " + routineEntity.getRoutineName() + " (ID: " + routineEntity.getId() + 
                          ") has " + taskEntities.size() + " tasks");
                    
                    // Check for task associations directly for this routine
                    List<RoutineTaskCrossRef> routineAssociations = 
                        database.routineDao().getTaskPositions(routineEntity.getId());
                    Log.d("HabitizerRepository", "Found " + routineAssociations.size() + 
                          " associations for routine " + routineEntity.getRoutineName());
                    
                    // Check if the number of task entities matches the number of associations
                    if (taskEntities.size() != routineAssociations.size()) {
                        Log.w("HabitizerRepository", "Warning: Task count mismatch for routine " + 
                              routineEntity.getRoutineName() + ". Tasks: " + taskEntities.size() + 
                              ", Associations: " + routineAssociations.size());
                    }
                    
                    // Special handling for important routines
                    if ("Morning".equals(routineEntity.getRoutineName()) || "Evening".equals(routineEntity.getRoutineName())) {
                        if (taskEntities.isEmpty()) {
                            Log.e("HabitizerRepository", "CRITICAL: " + routineEntity.getRoutineName() + 
                                  " routine has NO TASKS!");
                            
                            // Extra debug - check if any associations exist for this routine
                            List<RoutineTaskCrossRef> assocs = 
                                database.routineDao().getTaskPositions(routineEntity.getId());
                            Log.e("HabitizerRepository", "Database shows " + assocs.size() + 
                                  " associations for " + routineEntity.getRoutineName());
                            
                            // Log all tasks in database to see if they exist
                            List<TaskEntity> allTasks = database.taskDao().findAll();
                            Log.e("HabitizerRepository", "Total tasks in database: " + allTasks.size());
                        }
                    }
                    
                    // Log the details of each task for debugging
                    for (int i = 0; i < taskEntities.size(); i++) {
                        TaskEntity taskEntity = taskEntities.get(i);
                        Log.d("HabitizerRepository", "  Task " + i + ": " + taskEntity.getTaskName() + 
                              " (ID: " + taskEntity.getId() + ")");
                    }
                    
                    // Create Routine object
                    Routine routine = routineEntity.toRoutine();
                    
                    // Add tasks to routine
                    for (TaskEntity taskEntity : taskEntities) {
                        Task task = taskEntity.toTask();
                        routine.addTask(task);
                    }
                    
                    routines.add(routine);
                }
                
                // Post updated routines to main handler
                mainHandler.post(() -> {
                    Log.d("HabitizerRepository", "Posting " + routines.size() + " refreshed routines to UI");
                    routinesSubject.setValue(routines);
                    
                    // Final verification of task counts after setting value
                    List<Routine> postedRoutines = routinesSubject.getValue();
                    if (postedRoutines != null) {
                        for (Routine r : postedRoutines) {
                            Log.d("HabitizerRepository", "Verified: " + r.getRoutineName() + 
                                  " has " + r.getTasks().size() + " tasks after refresh");
                        }
                    }
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