package edu.ucsd.cse110.habitizer.lib.domain.timer;

import java.time.Duration;
import java.time.LocalDateTime;

public class RoutineTimer extends Timer {
    protected LocalDateTime currentTime;

    /**
     * Gets final elapsed time of routine once completed
     * @return total number of minutes since routine started, rounded UP
     */
    @Override
    public int getElapsedMinutes() {
        if (startTime == null || endTime == null) return 0;

        long durationSeconds = Duration.between(startTime, endTime).toSeconds();
        return (int) Math.ceil(durationSeconds / 60.0);
    }

    /**
     * Gets current elapsed time of routine
     * @param curTime current time
     * @return total number of minutes since routine started, rounded DOWN
     */
    public int getCurrentMinutes(LocalDateTime curTime) {
        if (startTime == null) return 0;

        long durationSeconds = Duration.between(startTime, curTime).toSeconds();
        return (int) Math.floor(durationSeconds / 60.0);
    }


}
