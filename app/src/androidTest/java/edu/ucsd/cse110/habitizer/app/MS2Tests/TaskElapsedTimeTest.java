package edu.ucsd.cse110.habitizer.app.MS2Tests;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;

public class TaskElapsedTimeTest {

    private Routine testRoutine;
    private Task brushTeethTask;
    private SimpleTaskTimer taskTimer;
    private TaskTimerDisplay timerDisplay;

    @Before
    public void setUp() {
        // Create test routine with a task
        testRoutine = new Routine(1, "Morning Routine");
        brushTeethTask = new Task(1, "Brush teeth", false);
        testRoutine.addTask(brushTeethTask);

        // Create and initialize the task timer
        taskTimer = new SimpleTaskTimer();

        // Create display that will format time values
        timerDisplay = new TaskTimerDisplay();
    }


    @Test
    public void testTaskTimeDisplay() {
        // Set task elapsed time to 30 minutes and 30 seconds
        taskTimer.setCurrentTask(brushTeethTask);
        taskTimer.setElapsedTimeMillis(TimeUnit.MINUTES.toMillis(30) + TimeUnit.SECONDS.toMillis(30));

        // When checking the elapsed time display
        String displayValue = timerDisplay.formatTaskTime(taskTimer.getElapsedTimeMillis());

        // Then the task time should display "30m"
        assertEquals("Task time should display '30m' for 30 minutes and 30 seconds",
                "30m", displayValue);
    }

    @Test
    public void testCompletingTaskTimeDisplay() {
        // Given task has been running for 18 minutes and 30 seconds
        taskTimer.setCurrentTask(brushTeethTask);
        taskTimer.setElapsedTimeMillis(TimeUnit.MINUTES.toMillis(18) + TimeUnit.SECONDS.toMillis(30));

        // Then the task time should display "18m"
        String initialDisplay = timerDisplay.formatTaskTime(taskTimer.getElapsedTimeMillis());
        assertEquals("Task time should initially display '18m'", "18m", initialDisplay);

        // When task is checked off as completed
        long finalTimeMillis = taskTimer.getElapsedTimeMillis();
        brushTeethTask.setCompleted(true);
        brushTeethTask.setElapsedTimeMillis(finalTimeMillis);
        taskTimer.clearCurrentTask();

        // Then the task timer should display "0m"
        String afterCompletionDisplay = timerDisplay.formatTaskTime(taskTimer.getElapsedTimeMillis());
        assertEquals("Task time should display '-' after task completion", "-", afterCompletionDisplay);

        // And the completed task should show "19m" (rounded up from 18m 30s)
        String taskCompletionTime = timerDisplay.formatTaskCompletionTime(brushTeethTask.getElapsedTimeMillis());
        assertEquals("Completed task should show '19m'", "19m", taskCompletionTime);
    }

    // Helper class to simulate task timer functionality
    private static class SimpleTaskTimer {
        private Task currentTask;
        private long elapsedTimeMillis = 0;

        public void setCurrentTask(Task task) {
            this.currentTask = task;
        }

        public Task getCurrentTask() {
            return currentTask;
        }

        public void clearCurrentTask() {
            this.currentTask = null;
            this.elapsedTimeMillis = 0;
        }

        public void setElapsedTimeMillis(long millis) {
            this.elapsedTimeMillis = millis;
        }

        public long getElapsedTimeMillis() {
            return currentTask != null ? elapsedTimeMillis : 0;
        }
    }

    // Helper class to format timer displays
    private static class TaskTimerDisplay {
        public String formatTaskTime(long timeMillis) {
            if (timeMillis == 0) {
                return "-"; // No active task or no time recorded
            }

            // Convert to minutes, ignoring seconds for display
            long minutes = TimeUnit.MILLISECONDS.toMinutes(timeMillis);
            return minutes + "m";
        }

        public String formatTaskCompletionTime(long timeMillis) {
            // Round up to nearest minute for completed tasks
            long minutes = (timeMillis + TimeUnit.SECONDS.toMillis(30)) / TimeUnit.MINUTES.toMillis(1);
            return minutes + "m";
        }
    }
}