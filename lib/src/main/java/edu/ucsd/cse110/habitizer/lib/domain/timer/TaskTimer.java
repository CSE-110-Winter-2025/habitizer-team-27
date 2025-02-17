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
     * @return total number of minutes since routine started, rounded UP
     */
    @Override
    public int getElapsedMinutes() {
        if (startTime == null || endTime == null) return 0;

        // calculating duration includes any fast forward clicks
        long durationSeconds = Duration.between(startTime, endTime).toSeconds();
        // only occurs if you click multiple tasks at the same paused time, which would in the real world be "1m"
        if (durationSeconds == 0) {
            return 1;
        }
        return (int) Math.ceil(durationSeconds / 60.0);
    }
}
