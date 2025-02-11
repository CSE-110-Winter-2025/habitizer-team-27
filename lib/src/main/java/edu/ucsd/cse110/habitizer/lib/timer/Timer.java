// Base Timer class in timer package
package edu.ucsd.cse110.habitizer.lib.timer;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.Temporal;

public abstract class Timer {
    protected Temporal startTime;
    protected Temporal endTime;
    protected boolean isRunning = false;
    int numFastForward = 0;

    // Starts timer
    public void start(Temporal startTime) {
        if (!isRunning) {
            this.startTime = startTime;
            isRunning = true;
        }
    }

    // Ends timer
    public void end(Temporal endTime) {
        if (isRunning) {
            this.endTime = endTime;
            isRunning = false;
        }
    }

    // Gets total time since start, ROUNDED UP
    public abstract int getElapsedMinutes();

    // Common getters
    public Temporal getStartTime() { return startTime; }
    public Temporal getEndTime() { return endTime; }
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
