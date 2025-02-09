package edu.ucsd.cse110.habitizer.lib.domain;

import java.util.HashMap;
import java.util.Map;

import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;

public class InMemoryDataSource {
    private final Map<String, Routine> routines = new HashMap<>();

    // Constructor to initialize default routines
    public InMemoryDataSource() {
        initializeDefaultRoutines();
    }

    // Method to populate default routines
    private void initializeDefaultRoutines() {
        Routine morningRoutine = new Routine("Morning Routine");
        morningRoutine.addTask(new Task("Brush Teeth"));
        morningRoutine.addTask(new Task("Eat Breakfast"));
        routines.put(morningRoutine.getRoutineName(), morningRoutine);

        Routine eveningRoutine = new Routine("Evening Routine");
        eveningRoutine.addTask(new Task("Shower"));
        eveningRoutine.addTask(new Task("Read a Book"));
        routines.put(eveningRoutine.getRoutineName(), eveningRoutine);
    }

    // Add a routine to the data source
    public void addRoutine(Routine routine) {
        routines.put(routine.getRoutineName(), routine);
    }

    // Retrieve a routine by name
    public Routine getRoutine(String routineName) {
        return routines.get(routineName);
    }

    // Remove a routine by name
    public void removeRoutine(String routineName) {
        routines.remove(routineName);
    }

    // Get all routines
    public Map<String, Routine> getAllRoutines() {
        return routines;
    }
}