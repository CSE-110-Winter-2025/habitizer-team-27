package edu.ucsd.cse110.habitizer.lib.timer;

import java.time.Duration;
import java.time.LocalTime;

public class RoutineTimer {
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isRunning = false;

    // Start the timer for routine
    public void start() {
        if (!isRunning) {
            startTime = LocalTime.now();
            isRunning = true;
        }
    }

    // End the timer for routine
    public void end() {
        if (isRunning) {
            endTime = LocalTime.now();
            isRunning = false;
        }
    }

    // Get the total time for the routine(round up)
    public long getElapsedMinutes() {
        if (startTime == null || endTime == null) return 0;
        long durationSeconds = Duration.between(startTime, endTime).toSeconds();
        long durationMinutes = durationSeconds / 60; // convert to minute
        if (durationSeconds % 60 != 0) {
            durationMinutes += 1; // round up
        }
        return durationMinutes;
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