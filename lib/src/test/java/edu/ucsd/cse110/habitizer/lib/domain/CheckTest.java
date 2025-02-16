package edu.ucsd.cse110.habitizer.lib.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;

import edu.ucsd.cse110.habitizer.lib.domain.timer.RoutineTimer;
import edu.ucsd.cse110.habitizer.lib.domain.timer.TaskTimer;
import edu.ucsd.cse110.habitizer.lib.data.InMemoryDataSource;

public class CheckTest {


    // Tests if we are able to checkoff tasks
    @Test
    public void testCheckOffTask() {

        Task task = new Task(100, "Brush teeth", false);

        assertFalse(task.isCheckedOff());
        task.setCheckedOff(true);
        assertTrue(task.isCheckedOff());

    }

    // Tests if you are able to check off in a routine
    @Test
    public void testCheckOffRoutine() {
        Routine morningRoutine = new Routine(1, "Morning Routine");


        Task task1 = new Task(100, "Wash face", false);
        Task task2 = new Task(101, "Brush teeth", false);
        Task task3 = new Task(102, "Eat Breakfast", false);
        morningRoutine.addTask(task1);
        morningRoutine.addTask(task2);
        morningRoutine.addTask(task3);

        assertFalse(task1.isCheckedOff());
        assertFalse(task2.isCheckedOff());

        task1.setCheckedOff(true);
        assertTrue(task1.isCheckedOff());

        task2.setCheckedOff(true);
        assertTrue(task2.isCheckedOff());

    }

    @Test
    public void testAutoCompleteFlow() {
        // Setup routine with 3 tasks
        Routine morningRoutine = new Routine(1, "Morning Routine");


        Task task1 = new Task(100, "Wash face", false);
        Task task2 = new Task(101, "Brush teeth", false);
        Task task3 = new Task(102, "Eat Breakfast", false);

        morningRoutine.addTask(task1);
        morningRoutine.addTask(task2);
        morningRoutine.addTask(task3);

        // Verify initial state
        assertFalse(morningRoutine.autoCompleteRoutine());

        // Check off all tasks
        morningRoutine.getTasks().forEach(task ->
                task.setCheckedOff(true));

        // Validate auto-complete
        assertTrue(morningRoutine.autoCompleteRoutine());
    }

}
