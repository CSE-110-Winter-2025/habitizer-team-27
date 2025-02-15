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
    private int duration = 0;

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

    public void startTask(LocalDateTime startTime) {
        taskTimer.start(startTime);
    }

    public void completeTask(LocalDateTime endTime) {
        taskTimer.end(endTime);
        this.duration = taskTimer.getElapsedMinutes();
        isCompleted = true;

    }


    public int getDuration() {
        if (!isCompleted) {
            return taskTimer.getElapsedMinutes();
        }
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getTaskName() {
        return taskName;
    }

    @Override
    public String toString() {
        return taskName;
    }

    public Integer getTaskId() { return id; }

    public boolean isCompleted() {
        return isCompleted;
    }

}