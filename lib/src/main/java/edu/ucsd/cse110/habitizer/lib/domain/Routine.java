package edu.ucsd.cse110.habitizer.lib.domain;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Routine {
    private final String routineName; // Marked as final (immutable)
    private final List<Task> tasks; // List of tasks in the routine
    private boolean isRoutineStarted = false; // Tracks routine state
    private LocalTime routineStartTime; // Start time of the routine

    public Routine(String routineName) {
        this.routineName = routineName;
        this.tasks = new ArrayList<>();
    }

    // Start the routine and record the start time
    public void startRoutine() {
        if (isRoutineStarted) return; // Prevent duplicate starts
        isRoutineStarted = true;
        routineStartTime = LocalTime.now();
    }

    // Add a completed task to the routine
    public void addCompletedTask(Task task) {
        tasks.add(task);
    }

    // End the routine and calculate total duration
    public void endRoutine() {
        if (!isRoutineStarted) return; // Prevent duplicate ends
        isRoutineStarted = false;

        LocalTime endTime = LocalTime.now();
        long totalDuration = Duration.between(routineStartTime, endTime).toMinutes();
    }

    // Getters
    public String getRoutineName() { return routineName; }
    public List<Task> getTasks() { return tasks; }
    public LocalTime getRoutineStartTime() { return routineStartTime; }
}