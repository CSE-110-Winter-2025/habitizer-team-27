package edu.ucsd.cse110.habitizer.lib.domain.timer;

import java.time.Duration;

public class TaskTimer extends Timer {
    /**
     * Get final elapsed time of task once completed
     * @return total number of minutes since routine started, rounded UP
     */
    @Override
    public int getElapsedMinutes() {
        if (startTime == null || endTime == null) return 0;

        // calculating duration includes any fast forward clicks
        long durationSeconds = Duration.between(startTime, endTime).toSeconds();
        return (int) Math.ceil(durationSeconds / 60.0);
    }
}
