package edu.ucsd.cse110.habitizer.lib.domain;

import edu.ucsd.cse110.habitizer.lib.timer.RoutineTimer;
import edu.ucsd.cse110.habitizer.lib.timer.TaskTimer;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Routine {
    private final String routineName;
    private final List<Task> tasks = new ArrayList<>();
    private final RoutineTimer routineTimer = new RoutineTimer();

    public Routine(String routineName) {
        this.routineName = routineName;
    }

    // Start the routine
    public void startRoutine() {
        routineTimer.start(LocalDateTime.now());
    }

    // End the routine
    public void endRoutine() {
        routineTimer.end(LocalDateTime.now());
    }

    // Get the time for the routine(round up)
    public long getRoutineDurationMinutes() {
        return routineTimer.getElapsedMinutes();
    }

    // Add the task
    public void addTask(Task task) {
        tasks.add(task);
    }

    // Start the task
    public void startTask(String taskName) {
        Task task = tasks.stream()
                .filter(t -> t.getTaskName().equals(taskName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskName));
        task.startTask();
    }

    // End the task
    public void completeTask(String taskName) {
        Task task = tasks.stream()
                .filter(t -> t.getTaskName().equals(taskName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskName));
        task.completeTask();
    }

    // Getters
    public String getRoutineName() {
        return routineName;
    }

    public List<Task> getTasks() {
        return tasks;
    }
}
