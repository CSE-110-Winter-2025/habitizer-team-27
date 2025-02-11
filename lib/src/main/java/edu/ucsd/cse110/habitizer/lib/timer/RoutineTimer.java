package edu.ucsd.cse110.habitizer.lib.timer;

import java.time.Duration;

public class RoutineTimer extends Timer {
    @Override
    public long getElapsedMinutes() {
        if (startTime == null || endTime == null) return 0;
        long durationSeconds = Duration.between(startTime, endTime).toSeconds();
        return (durationSeconds + 59) / 60; // Ceiling division
    }
}
