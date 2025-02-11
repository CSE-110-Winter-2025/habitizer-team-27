package edu.ucsd.cse110.habitizer.lib.domain;

import org.junit.Test;

import java.time.LocalTime;
import static org.junit.Assert.*;

import edu.ucsd.cse110.habitizer.lib.timer.RoutineTimer;
import edu.ucsd.cse110.habitizer.lib.timer.TaskTimer;

public class TimerTest {
    RoutineTimer rTimer = new RoutineTimer();
    TaskTimer tTimer = new TaskTimer();

    LocalTime time1 = LocalTime.of(8, 0); // 8:00 AM
    LocalTime time2 = LocalTime.of(8, 30); // 8:30 AM

    @Test
    // Checks that starting a timer while running does not do anything
    public void doubleStartTimer() {
        rTimer.start(time1);
        rTimer.start(time2);
        assertEquals(rTimer.getStartTime(), time1);

        tTimer.start(time1);
        tTimer.start(time2);
        assertEquals(tTimer.getStartTime(), time1);
    }
}
