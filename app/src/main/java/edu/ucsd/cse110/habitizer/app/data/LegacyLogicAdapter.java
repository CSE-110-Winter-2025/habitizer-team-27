package edu.ucsd.cse110.habitizer.app.data;

import java.util.List;

import edu.ucsd.cse110.habitizer.app.HabitizerApplication;
import edu.ucsd.cse110.habitizer.app.data.db.HabitizerRepository;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;
import edu.ucsd.cse110.observables.Subject;

/**
 * Adapter class that provides the same interface as the old HabitizerLogic but uses the new Repository implementation
 * This class helps with smooth migration, but in the long term, the Repository should be used directly
 */
public class LegacyLogicAdapter {
    private final HabitizerRepository repository;
    
    public LegacyLogicAdapter() {
        this.repository = HabitizerApplication.getRepository();
    }
    
    public Subject<List<Task>> getTasks() {
        return repository.getTasks();
    }
    
    public Subject<List<Routine>> getRoutines() {
        return repository.getRoutines();
    }
    
    public void addTask(Task task) {
        repository.addTask(task);
    }
    
    public void updateTask(Task task) {
        repository.updateTask(task);
    }
    
    public void removeTask(int taskId) {
        repository.deleteTask(taskId);
    }
    
    public void addRoutine(Routine routine) {
        repository.addRoutine(routine);
    }
    
    public void updateRoutine(Routine routine) {
        repository.updateRoutine(routine);
    }
    
    public void removeRoutine(int routineId) {
        repository.deleteRoutine(routineId);
    }
    
    /**
     * Get a LegacyLogicAdapter instance that is compatible with old code
     * @return An implementation of the LegacyLogicAdapter
     */
    public static LegacyLogicAdapter getCompatInstance() {
        return new LegacyLogicAdapter();
    }
    
    // Add the putRoutine method for compatibility with TaskAdapter
    public void putRoutine(Routine routine) {
        repository.updateRoutine(routine);
    }
} 