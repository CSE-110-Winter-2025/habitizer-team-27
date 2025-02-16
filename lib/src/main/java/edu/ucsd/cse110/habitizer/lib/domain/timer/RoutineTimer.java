package edu.ucsd.cse110.habitizer.lib.domain.timer;

import java.time.Duration;
import java.time.LocalDateTime;

public class RoutineTimer extends Timer {

    @Override
    public void start(LocalDateTime start) {
        super.start(start);
        this.endTime = null;
    }

    public void end(LocalDateTime end) {
        super.end(end);
    }

    public boolean isActive() {
        return super.isRunning();
    }
    /**
     * Gets final elapsed time of routine once completed
     * @return total number of minutes since routine started, rounded UP
     */
    @Override
    public int getElapsedMinutes() {
        if (getStartTime() == null) return 0;
        if (getEndTime() == null) return 0;

        long durationSeconds = Duration.between(getStartTime(), getEndTime()).toSeconds();
        return (int) Math.ceil(durationSeconds / 60.0);
    }

    /**
     * Gets current elapsed time of routine
     * @param curTime current time
     * @return total number of minutes since routine started, rounded DOWN
     */
    public int getCurrentMinutes(LocalDateTime curTime) {
        if (getStartTime() == null) return 0;

        LocalDateTime effectiveEnd = getEndTime() != null ? getEndTime() : curTime;
        long durationSeconds = Duration.between(getStartTime(), effectiveEnd).toSeconds();
        return (int) Math.floor(durationSeconds / 60.0);
    }

    // Helper method for UI updates
    public int getLiveMinutes() {
        return isActive() ?
                getCurrentMinutes(LocalDateTime.now()) :
                getElapsedMinutes();
    }


}