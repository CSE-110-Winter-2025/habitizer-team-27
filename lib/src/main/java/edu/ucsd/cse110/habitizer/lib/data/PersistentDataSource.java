package edu.ucsd.cse110.habitizer.lib.data;

import java.util.ArrayList;
import java.util.List;

import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;
import edu.ucsd.cse110.habitizer.lib.util.Logger;

/**
 * Data source that persists data using a DataStorage implementation.
 * Extends InMemoryDataSource but saves/loads data from persistent storage.
 */
public class PersistentDataSource extends InMemoryDataSource {
    private static final String TAG = "PersistentDataSource";
    private final DataStorage storage;
    
    /**
     * Create a new PersistentDataSource with the given storage implementation
     * @param storage The storage implementation to use
     */
    public PersistentDataSource(DataStorage storage) {
        super();
        this.storage = storage;
        loadSavedData();
    }
    
    /**
     * Factory method to create a PersistentDataSource from storage.
     * If no data exists in storage, it will initialize with default data.
     * @param storage The storage implementation to use
     * @return A new PersistentDataSource instance
     */
    public static PersistentDataSource fromStorage(DataStorage storage) {
        try {
            PersistentDataSource data = new PersistentDataSource(storage);
            
            // If no routines exist, initialize with defaults
            if (data.getRoutines().isEmpty()) {
                Logger.d(TAG, "No routines found in storage, initializing with defaults");
                // Create fresh copies of the default tasks to avoid any reference issues
                List<Task> morningTasks = new ArrayList<>();
                for (Task originalTask : DEFAULT_MORNING) {
                    Task newTask = new Task(originalTask.getTaskId(), originalTask.getTaskName(), originalTask.isCheckedOff());
                    morningTasks.add(newTask);
                }
                
                List<Task> eveningTasks = new ArrayList<>();
                for (Task originalTask : DEFAULT_EVENING) {
                    Task newTask = new Task(originalTask.getTaskId(), originalTask.getTaskName(), originalTask.isCheckedOff());
                    eveningTasks.add(newTask);
                }
                
                // Create fresh copies of default routines
                Routine morningRoutine = new Routine(DEFAULT_ROUTINES.get(0).getRoutineId(), DEFAULT_ROUTINES.get(0).getRoutineName());
                Routine eveningRoutine = new Routine(DEFAULT_ROUTINES.get(1).getRoutineId(), DEFAULT_ROUTINES.get(1).getRoutineName());
                
                // Add tasks to routines
                for (Task task : morningTasks) {
                    morningRoutine.addTask(task);
                    data.putTask(task);
                }
                
                for (Task task : eveningTasks) {
                    eveningRoutine.addTask(task);
                    data.putTask(task);
                }
                
                // Save the routines
                data.putRoutine(morningRoutine);
                data.putRoutine(eveningRoutine);
            } else {
                Logger.d(TAG, "Found " + data.getRoutines().size() + " routines in storage");
            }
            
            return data;
        } catch (Exception e) {
            Logger.e(TAG, "Error creating data source from storage", e);
            // If there's an error loading from storage, return a fresh instance with defaults
            PersistentDataSource backup = new PersistentDataSource(storage);
            backup.clearAndInitializeWithDefaults();
            return backup;
        }
    }
    
    /**
     * Clear all data and initialize with defaults
     */
    private void clearAndInitializeWithDefaults() {
        Logger.d(TAG, "Clearing data and initializing with defaults due to error");
        
        // Create fresh copies of the default tasks
        for (Task originalTask : DEFAULT_MORNING) {
            Task newTask = new Task(originalTask.getTaskId(), originalTask.getTaskName(), originalTask.isCheckedOff());
            super.putTask(newTask);
        }
        
        for (Task originalTask : DEFAULT_EVENING) {
            Task newTask = new Task(originalTask.getTaskId(), originalTask.getTaskName(), originalTask.isCheckedOff());
            super.putTask(newTask);
        }
        
        // Create fresh copies of default routines
        Routine morningRoutine = new Routine(DEFAULT_ROUTINES.get(0).getRoutineId(), DEFAULT_ROUTINES.get(0).getRoutineName());
        Routine eveningRoutine = new Routine(DEFAULT_ROUTINES.get(1).getRoutineId(), DEFAULT_ROUTINES.get(1).getRoutineName());
        
        // Add tasks to routines
        for (Task task : DEFAULT_MORNING) {
            morningRoutine.addTask(task);
        }
        
        for (Task task : DEFAULT_EVENING) {
            eveningRoutine.addTask(task);
        }
        
        // Save the routines
        super.putRoutine(morningRoutine);
        super.putRoutine(eveningRoutine);
        
        // Save to storage
        storage.saveTasks(getTasks());
        storage.saveRoutines(getRoutines());
    }
    
    /**
     * Load saved data from storage
     */
    private void loadSavedData() {
        try {
            // Load tasks first
            List<Task> savedTasks = storage.loadTasks();
            if (savedTasks != null && !savedTasks.isEmpty()) {
                Logger.d(TAG, "Loaded " + savedTasks.size() + " tasks from storage");
                for (Task task : savedTasks) {
                    if (task != null && task.getTaskId() != null) {
                        super.putTask(task);
                    } else {
                        Logger.w(TAG, "Skipping invalid task: " + task);
                    }
                }
            }
            
            // Then load routines
            List<Routine> savedRoutines = storage.loadRoutines();
            if (savedRoutines != null && !savedRoutines.isEmpty()) {
                Logger.d(TAG, "Loaded " + savedRoutines.size() + " routines from storage");
                for (Routine routine : savedRoutines) {
                    if (routine != null && routine.getRoutineId() != null) {
                        // Make sure each task reference in the routine exists in our data source
                        for (Task task : routine.getTasks()) {
                            if (task != null && task.getTaskId() != null && getTask(task.getTaskId()) == null) {
                                super.putTask(task);
                            }
                        }
                        super.putRoutine(routine);
                    } else {
                        Logger.w(TAG, "Skipping invalid routine: " + routine);
                    }
                }
            }
        } catch (Exception e) {
            Logger.e(TAG, "Error loading saved data", e);
        }
    }
    
    @Override
    public void putTask(Task task) {
        try {
            if (task != null) {
                super.putTask(task);
                // Save to persistent storage whenever a task is updated
                storage.saveTasks(getTasks());
            }
        } catch (Exception e) {
            Logger.e(TAG, "Error saving task", e);
        }
    }
    
    @Override
    public void putRoutine(Routine routine) {
        try {
            if (routine != null) {
                super.putRoutine(routine);
                // Save to persistent storage whenever a routine is updated
                storage.saveRoutines(getRoutines());
            }
        } catch (Exception e) {
            Logger.e(TAG, "Error saving routine", e);
        }
    }
} 