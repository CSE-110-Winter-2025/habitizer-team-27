// Base Timer class in timer package
package edu.ucsd.cse110.habitizer.lib.timer;

import java.time.Duration;
import java.time.LocalTime;

public abstract class Timer {
    protected LocalTime startTime;
    protected LocalTime endTime;
    protected boolean isRunning = false;

    public void start() {
        if (!isRunning) {
            startTime = LocalTime.now();
            isRunning = true;
        }
    }

    public void end() {
        if (isRunning) {
            endTime = LocalTime.now();
            isRunning = false;
        }
    }

    public abstract long getElapsedMinutes();

    // Common getters
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public boolean isRunning() { return isRunning; }
}
