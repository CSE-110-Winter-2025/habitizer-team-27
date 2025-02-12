package edu.ucsd.cse110.habitizer.lib.domain.timer;

import java.time.Duration;
import java.time.LocalTime;

public class TaskTimer {
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isRunning = false;

    // Start the timer for the task
    public void start() {
        if (!isRunning) {
            startTime = LocalTime.now();
            isRunning = true;
        }
    }

    // End the timer for the task
    public void end() {
        if (isRunning) {
            endTime = LocalTime.now();
            isRunning = false;
        }
    }

    // Get the time for the task(round down)
    public long getElapsedMinutes() {
        if (startTime == null || endTime == null) return 0;
        long durationSeconds = Duration.between(startTime, endTime).toSeconds();
        return durationSeconds / 60; // round down
    }

    // Getters
    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public boolean isRunning() {
        return isRunning;
    }
}