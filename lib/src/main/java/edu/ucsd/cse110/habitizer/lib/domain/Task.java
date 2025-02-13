package edu.ucsd.cse110.habitizer.lib.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.time.LocalDateTime;

import edu.ucsd.cse110.habitizer.lib.domain.timer.TaskTimer;

public class Task implements Serializable {
    private  @Nullable Integer id;
    private  String taskName;
    private  TaskTimer taskTimer = new TaskTimer();
    private boolean isCompleted = false;

    public Task(@Nullable Integer id, String taskName) {
        this.id = id;
        this.taskName = taskName;
    }

    public void setTaskId(int id) {
        this.id = id;
    }
    public void setTaskName(String taskName) {
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

    @Override
    public String toString() {
        return taskName; // Return just the task name
    }

    public Integer getTaskId() { return id; }

    public boolean isCompleted() {
        return isCompleted;
    }
}
