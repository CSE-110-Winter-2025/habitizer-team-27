// Base Timer class in timer package
package edu.ucsd.cse110.habitizer.lib.timer;

import java.time.Duration;
import java.time.LocalTime;

public abstract class Timer {
    protected LocalTime startTime;
    protected LocalTime endTime;
    protected boolean isRunning = false;
    int numFastForward = 0;

    // Starts timer
    public void start() {
        if (!isRunning) {
            startTime = LocalTime.now();
            isRunning = true;
        }
    }

    // Ends timer
    public void end() {
        if (isRunning) {
            endTime = LocalTime.now();
            isRunning = false;
        }
    }

    // Gets total time since start, ROUNDED UP
    public abstract int getElapsedMinutes();

    // Common getters
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public boolean isRunning() { return isRunning; }

    // Testing functions

    /*
     We want to be able to add time to a current timer and see how things progress
     Stopping timer = end()
     Every time addTime() is called, also getElapsedMinutes() to see if there are any updates
        that need to be made in display
     */
    // Adds 30 seconds to time
    public void addTime() {
        numFastForward++;
    }
}
