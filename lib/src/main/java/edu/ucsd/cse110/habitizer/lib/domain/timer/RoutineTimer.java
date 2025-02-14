package edu.ucsd.cse110.habitizer.lib.domain.timer;

import java.time.Duration;
import java.time.LocalDateTime;

public class RoutineTimer extends Timer {
    protected LocalDateTime currentTime;
    private int advancedSeconds = 0;  // Track skipped time

    /**
     * Advance the timer by specified seconds to simulate task skipping
     * @param seconds Number of seconds to advance
     */
    public void advanceTime(int seconds) {
        this.advancedSeconds += seconds;
    }

    /**
     * Gets final elapsed time of routine once completed
     * @return total number of minutes since routine started, rounded UP
     */
    @Override
    public int getElapsedMinutes() {
        if (startTime == null || endTime == null) return 0;

        // calculation includes any fast forward clicks (each adds 30s)
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
        this.currentTime = curTime;

        // calculation includes any fast forward clicks (each adds 30s)
        long durationSeconds = Duration.between(startTime, currentTime).toSeconds();
        return (int) Math.floor(durationSeconds / 60.0);
    }


}
