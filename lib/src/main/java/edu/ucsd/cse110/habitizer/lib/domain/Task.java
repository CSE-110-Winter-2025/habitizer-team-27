package edu.ucsd.cse110.habitizer.lib.domain;

import java.time.Duration;
import java.time.LocalTime;

public class Task {
    private final String taskName; // Marked as final (immutable)
    private final LocalTime startTime; // Marked as final (immutable)
    private LocalTime endTime; // Can be updated when task is completed
    private boolean isCompleted; // Tracks completion status

    public Task(String taskName, LocalTime startTime) {
        this.taskName = taskName;
        this.startTime = startTime;
        this.isCompleted = false; // Default to not completed
    }

    // Mark the task as completed and set the end time
    public void markCompleted(LocalTime endTime) {
        this.endTime = endTime;
        this.isCompleted = true;
    }

    // Dynamic duration calculation
    public long getDurationMinutes() {
        if (endTime == null || startTime == null) return 0;
        return Duration.between(startTime, endTime).toMinutes();
    }

    // Getters
    public String getTaskName() { return taskName; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public boolean isCompleted() { return isCompleted; }
}