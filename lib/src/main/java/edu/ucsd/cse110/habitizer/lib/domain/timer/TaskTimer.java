package edu.ucsd.cse110.habitizer.lib.domain.timer;

import java.time.Duration;
import java.time.LocalDateTime;

public class TaskTimer extends Timer {
    @Override
    public void start(LocalDateTime start) {
        super.start(start);
        this.endTime = null;
    }

    /**
     * Get final elapsed time of task once completed
     * @return total number of minutes since routine started, rounded UP for tasks >= 30 seconds
     */
    @Override
    public int getElapsedMinutes() {
        if (startTime == null || endTime == null) return 0;

        // calculating duration includes any fast forward clicks
        long durationSeconds = Duration.between(startTime, endTime).toSeconds();
        
        // Ensure tasks always take at least 5 seconds
        if (durationSeconds < 5) {
            durationSeconds = 5;
        }
        
        // For tasks under a minute, report 0 minutes to ensure they show in seconds
        if (durationSeconds < 60) {
            return 0;
        }
        
        // For tasks over 1 minute, round up as before
        return (int) Math.ceil(durationSeconds / 60.0);
    }
    
    /**
     * Get elapsed seconds for the task
     * @return total number of seconds since task started
     */
    @Override
    public long getElapsedSeconds() {
        if (startTime == null) return 0;
        
        // If timer is not running, use endTime. Otherwise use current time.
        LocalDateTime endPoint = isRunning ? LocalDateTime.now() : endTime;
        if (endPoint == null) {
            // If timer is not running and no end time is set, use current time
            endPoint = LocalDateTime.now();
        }
        
        long durationSeconds = Duration.between(startTime, endPoint).getSeconds();
        
        // Ensure tasks always take at least 5 seconds
        if (durationSeconds < 5) {
            durationSeconds = 5;
        }
        
        return durationSeconds;
    }

    /**
     * Get elapsed seconds rounded down (for display when timer is running or paused)
     * @return Elapsed seconds rounded down to the nearest 5 seconds
     */
    public long getElapsedSecondsRoundedDown() {
        long rawElapsedSeconds = super.getElapsedSeconds();
        
        // Round down to nearest 5 seconds
        // Example: 12 seconds becomes 10 seconds
        long roundedDown = (rawElapsedSeconds / 5) * 5;
        
        // Log the rounding operation
        System.out.println("TaskTimer: Rounding down elapsed seconds from " + 
                          rawElapsedSeconds + " to " + roundedDown);
        
        return roundedDown;
    }

    /**
     * Get elapsed seconds rounded up (for display when task is completed)
     * @return Elapsed seconds rounded up to the nearest 5 seconds
     */
    public long getElapsedSecondsRoundedUp() {
        long rawElapsedSeconds = super.getElapsedSeconds();
        
        // Round up to nearest 5 seconds
        // Example: 12 seconds becomes 15 seconds
        long roundedUp = (long) Math.ceil(rawElapsedSeconds / 5.0) * 5;
        
        // Log the rounding operation
        System.out.println("TaskTimer: Rounding up elapsed seconds from " + 
                          rawElapsedSeconds + " to " + roundedUp);
        
        return roundedUp;
    }
}
