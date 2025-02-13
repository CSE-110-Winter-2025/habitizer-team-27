// Base Timer class in timer package
package edu.ucsd.cse110.habitizer.lib.domain.timer;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;

public abstract class Timer {
    protected LocalDateTime startTime;
    protected LocalDateTime endTime;
    protected boolean isRunning = false;

    // Starts timer
    public void start(LocalDateTime startTime) {
        if (!isRunning) {
            this.startTime = startTime;
            isRunning = true;
        }
    }

    // Ends timer
    public void end(LocalDateTime endTime) {
        if (isRunning) {
            if (endTime.isBefore(startTime)) {
                System.out.println("End time before start - Invalid");
                return;
            }
            this.endTime = endTime;
            isRunning = false;
        }
    }

    // Gets total time since start, ROUNDED UP
    public abstract int getElapsedMinutes();

    // Common getters
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public boolean isRunning() { return isRunning; }

    // Testing functions
    public void updateStartTime(LocalDateTime newStart) { startTime = newStart; }
    public void updateEndTime(LocalDateTime newEnd) { endTime = newEnd; }

    // Fast forward timer by 30 seconds
        // If timer not running (aka timer has a start and end time), update the end time
        // If timer is running (timer has only a start time), update the start time
    public void fastForward() {
        if (isRunning) {
            updateStartTime(startTime.minus(Duration.ofSeconds(30)));
        } else {
            updateEndTime(endTime.plus(Duration.ofSeconds(30)));
        }
    }
}
