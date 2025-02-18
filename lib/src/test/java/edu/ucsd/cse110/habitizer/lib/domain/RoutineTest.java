package edu.ucsd.cse110.habitizer.lib.domain;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.*;

public class RoutineTest {
    private Routine routine;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Before
    public void setUp() {
        routine = new Routine(1, "Test Routine");
        startTime = LocalDateTime.of(2025, 2, 1, 8, 0, 0);
        endTime = LocalDateTime.of(2025, 2, 1, 8, 30, 0);
    }

    @Test
    public void testStartRoutine() {
        routine.startRoutine(startTime);
        assertTrue(routine.isActive());
    }

    @Test
    public void testEndRoutine() {
        routine.startRoutine(startTime);
        routine.endRoutine(endTime);
        assertFalse(routine.isActive());
    }

    @Test
    public void testAddTask() {
        Task task = new Task(1, "Test Task", false);
        routine.addTask(task);
        assertTrue(routine.getTasks().contains(task));
    }

    @Test
    public void testCompleteTask() {
        Task task = new Task(1, "Test Task", false);
        routine.addTask(task);
        routine.startRoutine(startTime);
        routine.completeTask("Test Task");

        assertTrue(task.isCompleted());
    }

    @Test(expected = IllegalArgumentException.class)  // JUnit 4: 替换 assertThrows
    public void testCompleteTask_NotFound() {
        routine.startRoutine(startTime);
        routine.completeTask("Nonexistent Task");
    }

    @Test
    public void testAutoCompleteRoutine() {
        Task task = new Task(1, "Test Task", false);
        routine.addTask(task);
        routine.startRoutine(startTime);
        routine.completeTask("Test Task");

        assertTrue(routine.autoCompleteRoutine());
    }

    @Test
    public void testPauseTime() {
        routine.startRoutine(startTime);
        LocalDateTime pauseTime = startTime.plusMinutes(10);
        routine.pauseTime(pauseTime);

        assertEquals(pauseTime, routine.getCurrentTime());
    }

    @Test
    public void testFastForwardTime() {
        routine.startRoutine(startTime);
        routine.pauseTime(startTime);
        routine.fastForwardTime();

        assertEquals(startTime.plusSeconds(30), routine.getCurrentTime());
    }

    @Test
    public void testAdvanceTime() {
        routine.startRoutine(startTime);
        routine.advanceTime(60);

        assertEquals(startTime.plusSeconds(60), routine.getCurrentTime());
    }
}
