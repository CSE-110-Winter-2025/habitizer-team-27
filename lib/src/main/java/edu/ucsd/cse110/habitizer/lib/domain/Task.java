package edu.ucsd.cse110.habitizer.lib.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.time.LocalDateTime;

import edu.ucsd.cse110.habitizer.lib.domain.timer.TaskTimer;

public class Task implements Serializable {
    private final @Nullable Integer id;
    private final String taskName;
    private final TaskTimer taskTimer = new TaskTimer();
    private boolean isCompleted = false;
    private boolean isCheckedOff;

    public Task(@Nullable Integer id, String taskName, boolean isCheckedOff) {
        this.id = id;
        this.taskName = taskName;
        this.isCheckedOff = isCheckedOff;
    }

    public Task withId(int id) {
        return new Task(id, this.taskName, isCheckedOff);
    }

    // Start the task
    public void startTask() {
        taskTimer.start(LocalDateTime.now());
    }

    // End the task
    public void completeTask() {
        taskTimer.end(LocalDateTime.now());
        isCompleted = true;
    }

    // Get the time for the task (round down)
    public long getDurationMinutes() {
        return taskTimer.getElapsedMinutes();
    }

    // Returns if task is checked off
    public boolean isCheckedOff() {
        return isCheckedOff;
    }

    // Sets the Check
    public void setCheckedOff(boolean checkedOff) {
        this.isCheckedOff = checkedOff;
    }

    // Getters
    public String getTaskName() {
        return taskName;
    }

    @Override
    public String toString() {
        return taskName; // Return just the task name
    }

    public Integer getTaskId() { return id; }

    public boolean isCompleted() {
        return isCompleted;
    }
}
