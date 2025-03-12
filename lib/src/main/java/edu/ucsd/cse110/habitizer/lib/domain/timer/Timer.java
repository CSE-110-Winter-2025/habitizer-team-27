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
        // Only set start time if the timer isn't already running
        if (!isRunning) {
            this.startTime = startTime;
            isRunning = true;
            // Reset end time when starting/restarting the timer
            this.endTime = null;
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

    /**
     * Gets the total elapsed time in seconds
     * @return The elapsed time in seconds
     */
    public long getElapsedSeconds() {
        if (startTime == null) {
            return 0;
        }
        
        LocalDateTime endPoint = isRunning ? LocalDateTime.now() : endTime;
        if (endPoint == null) {
            // If timer is not running and no end time is set, use current time
            endPoint = LocalDateTime.now();
        }
        
        // Calculate the duration between the start time and the end point
        Duration duration = Duration.between(startTime, endPoint);
        
        // Return the total seconds (ensuring non-negative value)
        return Math.max(0, duration.getSeconds());
    }

    // Common getters
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public boolean isRunning() { return isRunning; }

    // Testing functions
    public void updateStartTime(LocalDateTime newStart) { 
        startTime = newStart;
        // Print debug info to verify the start time was updated
        System.out.println("Timer start time updated to: " + newStart);
    }
    
    public void updateEndTime(LocalDateTime newEnd) { endTime = newEnd; }
}
