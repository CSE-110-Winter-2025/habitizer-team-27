package edu.ucsd.cse110.habitizer.lib.timer;

import java.time.Duration;

public class TaskTimer extends Timer {
    @Override
    public int getElapsedMinutes() {
        if (startTime == null || endTime == null) return 0;

        // calculating duration includes any fast forward clicks
        long durationSeconds = Duration.between(startTime, endTime).toSeconds() + 30L * numFastForward;
        return (int) Math.ceil(durationSeconds / 60.0);
    }
}
