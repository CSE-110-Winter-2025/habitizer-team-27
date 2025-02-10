package edu.ucsd.cse110.habitizer.lib.domain;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Routine {
    private final String routineName; // Routine name (immutable)
    private final List<Task> tasks;   // List of tasks in the routine
    private LocalTime routineStartTime; // Routine start time
    private LocalTime routineEndTime;   // Routine end time
    private boolean isRoutineEnded = false; // Routine completion status

    public Routine(String routineName) {
        this.routineName = routineName;
        this.tasks = new ArrayList<>();
    }

    // Start the routine and record the start time
    public void startRoutine() {
        routineStartTime = LocalTime.now();
        isRoutineEnded = false;
    }

    // End the routine and record the end time
    public void endRoutine() {
        routineEndTime = LocalTime.now();
        isRoutineEnded = true;
    }

    // Add a task to the routine
    public void addTask(Task task) {
        tasks.add(task);
    }

    // Start a specific task and record its start time
    public void startTask(String taskName) {
        Task task = tasks.stream()
                .filter(t -> t.getTaskName().equals(taskName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskName));
        task.startTask(LocalTime.now());
    }

    // Complete a specific task and record its end time
    public void completeTask(String taskName) {
        Task task = tasks.stream()
                .filter(t -> t.getTaskName().equals(taskName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskName));
        task.completeTask(LocalTime.now());
    }

    // Calculate the total routine duration in minutes (rounded up)
    public long getRoutineDurationMinutes() {
        if (routineStartTime == null || routineEndTime == null) return 0;
        long durationSeconds = Duration.between(routineStartTime, routineEndTime).toSeconds();
        long durationMinutes = durationSeconds / 60; // Truncate to minutes
        if (durationSeconds % 60 != 0) {
            durationMinutes += 1; // Round up if there are remaining seconds
        }
        return durationMinutes;
    }

    // Getters
    public String getRoutineName() { return routineName; }
    public List<Task> getTasks() { return tasks; }
    public LocalTime getRoutineStartTime() { return routineStartTime; }
    public LocalTime getRoutineEndTime() { return routineEndTime; }
    public boolean isRoutineEnded() { return isRoutineEnded; }
}