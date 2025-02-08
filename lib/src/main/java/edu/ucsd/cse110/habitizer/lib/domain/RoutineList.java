package edu.ucsd.cse110.habitizer.lib.domain;

import java.util.ArrayList;
import java.util.List;


public class RoutineList {
    private final List<Routine> routines;

    public RoutineList() {
        this.routines = new ArrayList<>();
    }

    /*might be have more routine per day so I created the Routine list
    * so we can manage all the routine in one place */
    public void addRoutine(Routine routine) {
        routines.add(routine);
    }

    public List<Routine> getAllRoutines() {
        return routines;
    }

    /*In case we need to pull out one of the routine*/
    public Routine getRoutineByName(String name) {
        for (Routine routine : routines) {
            if (routine.getRoutineName().equals(name)) {
                return routine;  // get the Routine
            }
        }
        return null;
    }

    /* allow user to remove the whole routine from the list */
    public void removeRoutine(String name) {
        routines.removeIf(routine -> routine.getRoutineName().equals(name));
    }
}
