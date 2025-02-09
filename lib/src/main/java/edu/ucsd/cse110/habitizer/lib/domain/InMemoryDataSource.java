package edu.ucsd.cse110.habitizer.lib.domain;

import java.util.HashMap;
import java.util.Map;

public class InMemoryDataSource {
    private final Map<String, Routine> routines = new HashMap<>();

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