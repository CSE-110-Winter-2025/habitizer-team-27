package edu.ucsd.cse110.habitizer.lib.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.time.LocalDateTime;

import edu.ucsd.cse110.habitizer.lib.domain.timer.TaskTimer;

public class Task implements Serializable {
    private  @Nullable Integer id;
    private  String taskName;
    private boolean isCompleted = false;

    private boolean isCheckedOff;
    private int duration = 0;

    public Task(@Nullable Integer id, String taskName, boolean isCheckedOff) {
        this.id = id;
        this.taskName = taskName;
        this.isCheckedOff = isCheckedOff;
    }

    public void setTaskId(int id) {
        this.id = id;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public Task withId(int id) {
        return new Task(id, this.taskName, isCheckedOff);
    }


    public int getDuration() {
        return duration;
    }

    public void setDurationAndComplete(int duration) {
        this.duration = duration;
        this.isCompleted = true;
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
        return taskName;
    }

    public Integer getTaskId() { return id; }

    public boolean isCompleted() {
        return isCompleted;
    }

}