package edu.ucsd.cse110.habitizer.lib.data;

import java.util.List;

import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;

/**
 * Interface for persisting and retrieving data
 */
public interface DataStorage {
    /**
     * Save a list of tasks to persistent storage
     * @param tasks List of tasks to save
     */
    void saveTasks(List<Task> tasks);
    
    /**
     * Load tasks from persistent storage
     * @return List of loaded tasks, or empty list if none found
     */
    List<Task> loadTasks();
    
    /**
     * Save a list of routines to persistent storage
     * @param routines List of routines to save
     */
    void saveRoutines(List<Routine> routines);
    
    /**
     * Load routines from persistent storage
     * @return List of loaded routines, or empty list if none found
     */
    List<Routine> loadRoutines();
} 