package edu.ucsd.cse110.habitizer.lib.domain;

import java.time.Duration;
import java.time.LocalTime;

public class Task {
    private final String taskName; // Task name (immutable)
    private LocalTime startTime;   // Task start time
    private LocalTime endTime;     // Task end time
    private boolean isCompleted;   // Task completion status

    public Task(String taskName) {
        this.taskName = taskName;
        this.isCompleted = false;
    }

    // Start the task and record the start time
    public void startTask(LocalTime startTime) {
        this.startTime = startTime;
    }

    // Complete the task and record the end time
    public void completeTask(LocalTime endTime) {
        this.endTime = endTime;
        this.isCompleted = true;
    }

    // Calculate the task duration in minutes (rounded down)
    public long getDurationMinutes() {
        if (startTime == null || endTime == null) return 0;
        long durationSeconds = Duration.between(startTime, endTime).toSeconds();
        return durationSeconds / 60; // Round down
    }

    // Getters
    public String getTaskName() { return taskName; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public boolean isCompleted() { return isCompleted; }
}