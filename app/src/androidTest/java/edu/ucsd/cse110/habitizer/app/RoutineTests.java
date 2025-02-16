package edu.ucsd.cse110.habitizer.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDateTime;

import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;

public class RoutineTests {
    @Test
    public void testFastForwardAfterPause() {

        LocalDateTime time1 = LocalDateTime.of(2025, 2, 1, 8, 0, 0); // 8:00:00 AM, 02/01/2025
        LocalDateTime time2 = LocalDateTime.of(2025, 2, 1, 8, 30, 15); // 8:30:15 AM, 02/01/2025

        Routine morningRoutine = new Routine(1, "Morning Routine");
        morningRoutine.addTask(new Task(100, "Eat Breakfast", false));
        morningRoutine.addTask(new Task(101, "Brush Hair", false));
        morningRoutine.addTask(new Task(102, "Pack Backpack", false));

        morningRoutine.startRoutine(time1);
        // check routine start times
        assertEquals(morningRoutine.getRoutineTimer().getStartTime(), time1);
        assertEquals(morningRoutine.getTaskTimer().getStartTime(), time1);

        morningRoutine.pauseTime(time2);

        // check end time is still null
        assertNull(morningRoutine.getRoutineTimer().getEndTime());

        // check current durations
        assertEquals(30, morningRoutine.getRoutineTimer().getCurrentMinutes(time2));

        // fast forward by 5 minutes -- current time now 8:35:15
        for (int i = 0; i < 10; i++) {
            morningRoutine.fastForwardTime();
        }
        assertEquals(35, morningRoutine.getRoutineTimer().getCurrentMinutes(morningRoutine.getCurrentTime()));

        // complete a task (should be 36 minutes)
        morningRoutine.completeTask("Eat Breakfast");
        assertEquals(36, morningRoutine.getTasks().get(0).getDuration());

        // end routine at "real" time
        // should end at "current" fast-forwarded time
        morningRoutine.endRoutine(time2);
        assertEquals(36, morningRoutine.getRoutineDurationMinutes());
    }

    @Test
    public void testFastForwardWithoutPause() {
        LocalDateTime time1 = LocalDateTime.now();

        Routine morningRoutine = new Routine(1, "Morning Routine");
        morningRoutine.addTask(new Task(100, "Eat Breakfast", false));
        morningRoutine.addTask(new Task(101, "Brush Hair", false));
        morningRoutine.addTask(new Task(102, "Pack Backpack", false));

        morningRoutine.startRoutine(time1);
        // check routine start times
        assertEquals(morningRoutine.getRoutineTimer().getStartTime(), time1);
        assertEquals(morningRoutine.getTaskTimer().getStartTime(), time1);

        // fast forward by 2:30 minutes
        for (int i = 0; i < 5; i++) {
            morningRoutine.fastForwardTime();
        }
        assertEquals(2, morningRoutine.getRoutineTimer().getCurrentMinutes(morningRoutine.getCurrentTime()));

        // complete a task (should be 3 minutes)
        morningRoutine.completeTask("Eat Breakfast");
        assertEquals(3, morningRoutine.getTasks().get(0).getDuration());

        // end routine at "real" time
        morningRoutine.endRoutine(time1);
        assertEquals(3, morningRoutine.getRoutineDurationMinutes());
    }
}
