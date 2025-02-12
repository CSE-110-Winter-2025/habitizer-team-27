package edu.ucsd.cse110.habitizer.lib.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.time.LocalDateTime;

import edu.ucsd.cse110.habitizer.lib.domain.timer.TaskTimer;

public class Task implements Serializable {
    private final @Nullable Integer id;
    private final @NonNull String taskName;
    private final TaskTimer taskTimer = new TaskTimer();
    private boolean isCompleted = false;

    public Task(@Nullable Integer id, @NonNull String taskName) {
        this.id = id;
        this.taskName = taskName;
    }

    public Task withId(int id) {
        return new Task(id, this.taskName);
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

    // Getters
    public String getTaskName() {
        return taskName;
    }

    public boolean isCompleted() {
        return isCompleted;
    }
}
