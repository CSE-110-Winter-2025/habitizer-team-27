package edu.ucsd.cse110.habitizer.lib.domain;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestTask {
    private Task task;
    private final int TEST_ID = 1;
    private final String TEST_NAME = "Test Task";

    @Before
    public void setUp() {
        // Initialize a fresh task with default values
        task = new Task(TEST_ID, TEST_NAME, false);
    }

    @Test
    public void testTaskInitialization() {
        // Verify properties are set correctly
        assertEquals("Task ID should match constructor value",
                TEST_ID, (long) task.getTaskId());
        assertEquals("Task name should match constructor value",
                TEST_NAME, task.getTaskName());
        assertFalse("New task should not be completed",
                task.isCompleted());
    }

    @Test
    public void testStateModification() {
        // Test direct state changes
        task.setCompleted(true);
        assertTrue("Completed state should be updatable",
                task.isCompleted());

        task.setCheckedOff(true);
        assertTrue("Check-off state should be settable",
                task.isCheckedOff());
    }

    @Test
    public void testDurationManagement() {
        // Test duration field accessors
        int testDuration = 45;
        task.setDuration(testDuration);
        assertEquals("Duration should be settable",
                testDuration, (int) task.getDuration());
    }

    @Test
    public void testNameUpdate() {
        // Test name modification
        String updatedName = "Updated Task Name";
        task.setTaskName(updatedName);
        assertEquals("Task name should be updatable",
                updatedName, task.getTaskName());
    }
}