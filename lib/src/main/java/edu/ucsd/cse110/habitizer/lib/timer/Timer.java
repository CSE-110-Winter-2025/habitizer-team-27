// Base Timer class in timer package
package edu.ucsd.cse110.habitizer.lib.timer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;

public abstract class Timer {
    protected LocalDateTime startTime;
    protected LocalDateTime endTime;
    protected boolean isRunning = false;
    int numFastForward = 0;

    // Starts timer
    public void start(LocalDateTime startTime) {
        if (!isRunning) {
            this.startTime = startTime;
            isRunning = true;
        }
    }

    // Ends timer
    public void end(LocalDateTime endTime) {
        if (isRunning) {
            if (endTime.isBefore(startTime)) {
                System.out.println("End time before start - Invalid");
                return;
            }
            this.endTime = endTime;
            isRunning = false;
        }
    }

    // Gets total time since start, ROUNDED UP
    public abstract int getElapsedMinutes();

    // Common getters
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
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
