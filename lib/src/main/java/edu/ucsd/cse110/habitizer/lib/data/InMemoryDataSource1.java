package edu.ucsd.cse110.habitizer.lib.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;
import edu.ucsd.cse110.observables.PlainMutableSubject;

public class InMemoryDataSource1 {
    // Routines are identified by their name
    private final Map<String, Routine> routines = new HashMap<>();
    private final Map<String, PlainMutableSubject<Routine>> routineSubjects= new HashMap<>();
    private final PlainMutableSubject<List<Routine>> allRoutineSubjects = new PlainMutableSubject<>();

     public final static List<Task> DEFAULT_MORNING = List.of(
            new Task(0, "Shower"),
            new Task(1, "Brush teeth"),
            new Task(2, "Dress"),
            new Task(3, "Make coffee"),
            new Task(4, "Make lunch"),
            new Task(5, "Dinner prep"),
            new Task(6, "Pack bag")
    );

    public final static List<Task> DEFAULT_EVENING = List.of(
            new Task(0, "Charge devices"),
            new Task(1, "Prepare dinner"),
            new Task(2, "Eat dinner"),
            new Task(3, "Wash dishes"),
            new Task(4, "Homework")
    );

    public InMemoryDataSource1() {

    }

    public static InMemoryDataSource1 fromDefault() {
        var data = new InMemoryDataSource1();

        Routine morning = new Routine("Morning Routine");
        DEFAULT_MORNING.forEach(morning::addTask);
        data.routines.put(morning.getRoutineName(), morning);

        Routine evening = new Routine("Evening Routine");
        DEFAULT_EVENING.forEach(evening::addTask);
        data.routines.put(evening.getRoutineName(), evening);

        return data;
    }

    // Routine getters
    public List<Routine> getRoutines() {
        return List.copyOf(routines.values());
    }

    public Routine getRoutine(String name) {
        return routines.get(name);
    }

    public PlainMutableSubject<Routine> getRoutineSubject(String name) {
        if (!routineSubjects.containsKey(name)) {
            var subject = new PlainMutableSubject<Routine>();
            subject.setValue(getRoutine(name));
            routineSubjects.put(name, subject);
        }
        return routineSubjects.get(name);
    }

    public PlainMutableSubject<List<Routine>> getAllRoutineSubjects() {
        return allRoutineSubjects;
    }



}
