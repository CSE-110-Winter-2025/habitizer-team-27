package edu.ucsd.cse110.habitizer.lib.domain;
import org.junit.Test;

import edu.ucsd.cse110.habitizer.lib.timer.RoutineTimer;

public class TimerTest {
    RoutineTimer timer = new RoutineTimer();

    @Test
    public void testTimerStart() {
        timer.start();
    }
}
