package edu.ucsd.cse110.habitizer.lib.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.time.LocalDateTime;

import edu.ucsd.cse110.habitizer.lib.domain.timer.TaskTimer;

public class Task implements Serializable {
    private @Nullable Integer id;
    private String taskName;
    private boolean isCompleted = false;

    private boolean isCheckedOff;

    private boolean isSkipped = false;

    private long duration = 0;

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

    public void reset() {
        this.isCompleted = false;
        this.duration = 0;
        this.isCheckedOff = false;
    }

    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }

    public void setCheckedOff(boolean checkedOff) {
        this.isCheckedOff = checkedOff;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getDuration() {
        return duration;
    }

    public void setDurationAndComplete(long duration) {
        this.duration = duration;
        this.isCompleted = true;
        this.isCheckedOff = true;
        this.isSkipped = false;
    }

    // Returns if task is checked off
    public boolean isCheckedOff() {
        return isCheckedOff;
    }

    // Getters
    public String getTaskName() {
        return taskName;
    }

    @Override
    public String toString() {
        return taskName;
    }

    public Integer getTaskId() {
        return id;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setSkipped(boolean skipped) {
        isSkipped = skipped;
    }

    public boolean isSkipped() {
        return isSkipped;
    }
}
