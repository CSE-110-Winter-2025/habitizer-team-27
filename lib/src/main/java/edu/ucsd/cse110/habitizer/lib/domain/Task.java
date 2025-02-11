package edu.ucsd.cse110.habitizer.lib.domain;

import edu.ucsd.cse110.habitizer.lib.timer.TaskTimer;

public class Task {
    private final String taskName;
    private final TaskTimer taskTimer = new TaskTimer();
    private boolean isCompleted = false;

    public Task(String taskName) {
        this.taskName = taskName;
    }

    // Start the task
    public void startTask() {
        taskTimer.start();
    }

    // End the task
    public void completeTask() {
        taskTimer.end();
        isCompleted = true;
    }

    // Get the time for the task (round down)
    public long getDurationMinutes() {
        return taskTimer.getElapsedMinutes();
    }

    // Getters
    public String getTaskName() {
        return taskName;
    }

    public boolean isCompleted() {
        return isCompleted;
    }
}