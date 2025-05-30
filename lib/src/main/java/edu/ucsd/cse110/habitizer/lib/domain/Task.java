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

    private boolean isSkipped = false;

    private int duration = 0;
    
    // For storing seconds for tasks < 1 minute
    private int elapsedSeconds = 0;

    // Add this new field
    private long elapsedTimeMillis;

    // Your existing constructors

    // Add these two new methods
    /**
     * Sets the elapsed time for this task in milliseconds
     * @param elapsedTimeMillis The elapsed time in milliseconds
     */
    public void setElapsedTimeMillis(long elapsedTimeMillis) {
        this.elapsedTimeMillis = elapsedTimeMillis;
    }

    /**
     * Gets the elapsed time for this task in milliseconds
     * @return The elapsed time in milliseconds
     */
    public long getElapsedTimeMillis() {
        return elapsedTimeMillis;
    }

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
        this.elapsedSeconds = 0;
        this.isCheckedOff = false;
        this.isSkipped = false;
    }


    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    public int getElapsedSeconds() {
        return elapsedSeconds;
    }
    
    public void setElapsedSeconds(int seconds) {
        this.elapsedSeconds = seconds;
    }
    
    /**
     * Returns true if this task should display time in seconds rather than minutes
     */
    public boolean shouldShowInSeconds() {
        // Show in seconds if task is completed and took less than 60 seconds 
        // OR if duration is less than 1 minute and elapsedSeconds is recorded
        return isCompleted && (elapsedSeconds > 0 && elapsedSeconds < 60);
    }

    public void setDurationAndComplete(int duration) {
        this.duration = duration;
        this.isCompleted = true;
        this.isCheckedOff = true;
        this.isSkipped = false;
    }

    // Returns if task is checked off
    public boolean isCheckedOff() {
        return isCheckedOff;
    }

    // Sets the Check
    public void setCheckedOff(boolean checkedOff) {
        this.isCheckedOff = checkedOff;
        this.isCompleted = checkedOff;
        this.isSkipped = !checkedOff;
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

    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }

    public void setSkipped(boolean skipped) {
        isSkipped = skipped;
    }

    public boolean isSkipped() {
        return isSkipped;
    }
}