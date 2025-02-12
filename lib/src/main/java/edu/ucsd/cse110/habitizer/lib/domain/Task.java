package edu.ucsd.cse110.habitizer.lib.domain;

import androidx.annotation.Nullable;

import java.io.Serializable;

import edu.ucsd.cse110.habitizer.lib.timer.TaskTimer;

public class Task implements Serializable {
    private final @Nullable Integer id;
    private final String taskName;
    private final TaskTimer taskTimer = new TaskTimer();
    private boolean isCompleted = false;

    public Task(@Nullable Integer id, String taskName) {
        this.id = id;
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

    public Integer getTaskId() { return id; }

    public boolean isCompleted() {
        return isCompleted;
    }
}