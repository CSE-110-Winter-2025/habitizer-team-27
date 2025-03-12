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
    public int getElapsedSeconds() {
        if (startTime == null || endTime == null) return 0;
        
        long durationSeconds = Duration.between(startTime, endTime).toSeconds();
        
        // Ensure tasks always take at least 5 seconds
        if (durationSeconds < 5) {
            durationSeconds = 5;
        }
        
        return (int) durationSeconds;
    }
}
