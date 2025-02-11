package edu.ucsd.cse110.habitizer.lib.timer;

import java.time.Duration;

public class TaskTimer extends Timer {
    @Override
    public long getElapsedMinutes() {
        if (startTime == null || endTime == null) return 0;
        long durationSeconds = Duration.between(startTime, endTime).toSeconds();
        return durationSeconds / 60; // Floor division
    }
}
