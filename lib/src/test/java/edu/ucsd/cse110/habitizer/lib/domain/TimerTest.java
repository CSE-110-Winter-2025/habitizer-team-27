package edu.ucsd.cse110.habitizer.lib.domain;

import static org.junit.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import edu.ucsd.cse110.habitizer.lib.timer.RoutineTimer;
import edu.ucsd.cse110.habitizer.lib.timer.TaskTimer;

public class TimerTest {

    LocalDateTime time1 = LocalDateTime.of(2025, 2, 1, 8, 0); // 8:00 AM, 02/01/2025
    LocalDateTime time2 = LocalDateTime.of(2025, 2, 1, 8, 30); // 8:30 AM, 02/01/2025
    LocalDateTime time3 = LocalDateTime.of(2025, 2, 1, 9, 0); // 9:00 AM, 02/01/2025

    // Checks that starting a timer while running does not do anything
    @Test
    public void testDoubleStartTimer() {
        RoutineTimer rTimer = new RoutineTimer();
        TaskTimer tTimer = new TaskTimer();

        rTimer.start(time1);
        rTimer.start(time2);
        assertEquals(rTimer.getStartTime(), time1);

        tTimer.start(time1);
        tTimer.start(time2);
        assertEquals(tTimer.getStartTime(), time1);
    }

    // Checks that ending a timer while stopped does not do anything
    @Test
    public void testDoubleEndTimer() throws Exception {
        RoutineTimer rTimer = new RoutineTimer();
        TaskTimer tTimer = new TaskTimer();

        rTimer.start(time1);
        rTimer.end(time2);
        rTimer.end(time3);
        assertEquals(rTimer.getEndTime(), time2);

        tTimer.start(time1);
        tTimer.end(time3);
        tTimer.end(time2);
        assertEquals(tTimer.getEndTime(), time3);
    }
}
