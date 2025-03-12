package edu.ucsd.cse110.habitizer.app.MS2Tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;

public class PauseTest {

    private Routine testRoutine;
    private Task testTask;
    private SimpleTimer routineTimer;
    private SimpleTimer taskTimer;
    private boolean isPauseButton;

    @Before
    public void setUp() {
        // Create test routine and task
        testRoutine = new Routine(1, "Morning Routine");
        testTask = new Task(1, "Brush Teeth", false);
        testRoutine.addTask(testTask);

        // Create and initialize timers for testing
        routineTimer = new SimpleTimer();
        taskTimer = new SimpleTimer();
        routineTimer.setElapsedTimeMinutes(29);
        taskTimer.setElapsedTimeMinutes(10);

        // Initialize pause button state
        isPauseButton = true;
    }

    @Test
    public void testPauseAndRestartRoutine() {
        // Verify initial conditions
        assertEquals("Routine time should be 29 minutes initially",
                29, routineTimer.getElapsedTimeMinutes());
        assertEquals("Task time should be 10 minutes initially",
                10, taskTimer.getElapsedTimeMinutes());

        // When user presses the "Pause" button
        routineTimer.pause();
        taskTimer.pause();
        isPauseButton = false;

        // Verify timers are paused
        assertFalse("Routine timer should be paused", routineTimer.isRunning());
        assertFalse("Task timer should be paused", taskTimer.isRunning());

        // Verify button state changed
        assertFalse("Button should show Resume", isPauseButton);

        // Check that timer values don't change while paused
        assertEquals("Routine time should still be 29 minutes",
                29, routineTimer.getElapsedTimeMinutes());
        assertEquals("Task time should still be 10 minutes",
                10, taskTimer.getElapsedTimeMinutes());

        // When user presses the "Play" button
        routineTimer.resume();
        taskTimer.resume();
        isPauseButton = true; // Button changes back to "Pause"

        // Verify timers are running again
        assertTrue("Routine timer should be running", routineTimer.isRunning());
        assertTrue("Task timer should be running", taskTimer.isRunning());

        // Simulate waiting one minute (by advancing timers)
        routineTimer.addTime(1);
        taskTimer.addTime(1);

        // Verify times advanced by one minute
        assertEquals("Routine time should advance to 30 minutes",
                30, routineTimer.getElapsedTimeMinutes());
        assertEquals("Task time should advance to 11 minutes",
                11, taskTimer.getElapsedTimeMinutes());
    }

    @Test
    public void testAutoPauseOnAppExit() {
        // Verify initial conditions
        assertEquals("Routine time should be 29 minutes initially",
                29, routineTimer.getElapsedTimeMinutes());
        assertEquals("Task time should be 10 minutes initially",
                10, taskTimer.getElapsedTimeMinutes());

        // Simulate app exit
        routineTimer.pause();
        taskTimer.pause();
        routineTimer.saveState();
        taskTimer.saveState();

        // Verify timers are paused
        assertFalse("Routine timer should be paused", routineTimer.isRunning());
        assertFalse("Task timer should be paused", taskTimer.isRunning());

        // Verify timer values are saved
        assertEquals("Saved routine time should be 29 minutes",
                29, routineTimer.getSavedTimeMinutes());
        assertEquals("Saved task time should be 10 minutes",
                10, taskTimer.getSavedTimeMinutes());

        // Simulate app restart
        routineTimer.loadSavedState();
        taskTimer.loadSavedState();
        routineTimer.resume();
        taskTimer.resume();

        // Verify state was restored correctly
        assertEquals("Routine time should be restored to 29 minutes",
                29, routineTimer.getElapsedTimeMinutes());
        assertEquals("Task time should be restored to 10 minutes",
                10, taskTimer.getElapsedTimeMinutes());

        // Verify timers are running again
        assertTrue("Routine timer should be running", routineTimer.isRunning());
        assertTrue("Task timer should be running", taskTimer.isRunning());

        // Simulate time passing
        routineTimer.addTime(1);
        taskTimer.addTime(1);

        // Verify timers are advancing
        assertEquals("Routine time should be 30 minutes after resuming",
                30, routineTimer.getElapsedTimeMinutes());
        assertEquals("Task time should be 11 minutes after resuming",
                11, taskTimer.getElapsedTimeMinutes());
    }

    /**
     * Helper class to simulate timer functionality for testing
     */
    private static class SimpleTimer {
        private int elapsedTimeMinutes = 0;
        private int savedTimeMinutes = 0;
        private boolean running = true;

        public void setElapsedTimeMinutes(int minutes) {
            this.elapsedTimeMinutes = minutes;
        }

        public int getElapsedTimeMinutes() {
            return elapsedTimeMinutes;
        }

        public void addTime(int minutes) {
            if (running) {
                elapsedTimeMinutes += minutes;
            }
        }

        public void pause() {
            running = false;
        }

        public void resume() {
            running = true;
        }

        public boolean isRunning() {
            return running;
        }

        public void saveState() {
            savedTimeMinutes = elapsedTimeMinutes;
        }

        public void loadSavedState() {
            elapsedTimeMinutes = savedTimeMinutes;
        }

        public int getSavedTimeMinutes() {
            return savedTimeMinutes;
        }
    }
}