package edu.ucsd.cse110.habitizer.lib.domain;

import static org.junit.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

import java.time.LocalTime;

import edu.ucsd.cse110.habitizer.lib.timer.RoutineTimer;
import edu.ucsd.cse110.habitizer.lib.timer.TaskTimer;

public class TimerTest {

    // Checks that starting a timer while running does not do anything
    @Test
    public void testDoubleStartTimer() {
        RoutineTimer rTimer = new RoutineTimer();
        TaskTimer tTimer = new TaskTimer();

        LocalTime time1 = LocalTime.of(8, 0); // 8:00 AM
        LocalTime time2 = LocalTime.of(8, 30);

        rTimer.start(time1);
        rTimer.start(time2);
        assertEquals(rTimer.getStartTime(), time1);

        tTimer.start(time1);
        tTimer.start(time2);
        assertEquals(tTimer.getStartTime(), time1);
    }
}
