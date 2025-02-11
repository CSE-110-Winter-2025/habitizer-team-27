package edu.ucsd.cse110.habitizer.lib.timer;

import java.time.Duration;
import java.time.LocalTime;

public class RoutineTimer extends Timer {
    protected LocalTime currentTime;

    @Override
    public int getElapsedMinutes() {
        if (startTime == null || endTime == null) return 0;

        // calculation includes any fast forward clicks (each adds 30s)
        long durationSeconds = Duration.between(startTime, endTime).toSeconds() + 30L * numFastForward;
        return (int) Math.ceil(durationSeconds / 60.0);
    }

    // Gets current time ROUNDED DOWN, used for display
    public int getCurrentMinutes() {
        if (startTime == null) return 0;
        currentTime = LocalTime.now();

        // calculation includes any fast forward clicks (each adds 30s)
        long durationSeconds = Duration.between(startTime, currentTime).toSeconds() + 30L * numFastForward;
        return (int) Math.floor(durationSeconds / 60.0);
    }
}
