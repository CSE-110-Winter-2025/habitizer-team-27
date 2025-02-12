package edu.ucsd.cse110.habitizer.lib.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;
import edu.ucsd.cse110.observables.PlainMediatorSubject;
import edu.ucsd.cse110.observables.PlainMutableSubject;
import edu.ucsd.cse110.observables.Subject;

public class InMemoryDataSource {

    // Observers for routines
    private final Map<Integer, Routine> routines = new HashMap<>();
    private final Map<Integer, PlainMutableSubject<Routine>> routineSubjects = new HashMap<>();
    private final PlainMutableSubject<List<Routine>> allRoutinesSubjects = new PlainMediatorSubject<>();

    // Observers for tasks
    private final Map<Integer, Task> tasks = new HashMap<>();
    private final Map<Integer, PlainMutableSubject<Task>> taskSubjects = new HashMap<>();
    private final PlainMutableSubject<List<Task>> allTasksSubjects = new PlainMutableSubject<>();

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

    public final static List<Routine> DEFAULT_ROUTINES = List.of(
            new Routine(0, "Morning"),
            new Routine(1, "Evening")
    );

    public InMemoryDataSource() { }

    public InMemoryDataSource fromDefault() {
        var data = new InMemoryDataSource();

        for (Routine routine : DEFAULT_ROUTINES) {
            data.putRoutine(routine);
        }

        for (Task task : DEFAULT_MORNING) {
            data.putTask(task);
        }

        return data;
    }

    // Routine Functions
    public List<Routine> getRoutines() {
        return List.copyOf(routines.values());
    }

    public Routine getRoutine(int id) {
        return routines.get(id);
    }

    public Subject<Routine> getRoutineSubject(int id) {
        if (!routineSubjects.containsKey(id)) {
            var subject = new PlainMutableSubject<Routine>();
            subject.setValue(getRoutine(id));
            routineSubjects.put(id, subject);
        }
        return routineSubjects.get(id);
    }

    public Subject<List<Routine>> getAllRoutinesSubject() {
        return allRoutinesSubjects;
    }

    public void putRoutine(Routine routine) {
        routines.put(routine.getRoutineId(), routine);
        if (routineSubjects.containsKey(routine.getRoutineId())) {
            routineSubjects.get(routine.getRoutineId()).setValue(routine);
        }
        allRoutinesSubjects.setValue(getRoutines());
    }

    // Task Functions
    public List<Task> getTasks() {
        return List.copyOf(tasks.values());
    }

    public Task getTask(int id) {
        return tasks.get(id);
    }

    public Subject<Task> getTaskSubject(int id) {
        if (!taskSubjects.containsKey(id)) {
            var subject = new PlainMutableSubject<Task>();
            subject.setValue(getTask(id));
            taskSubjects.put(id, subject);
        }
        return taskSubjects.get(id);
    }

    public Subject<List<Task>> getAllTasksSubject() {
        return allTasksSubjects;
    }

    public void putTask(Task task) {
        tasks.put(task.getTaskId(), task);
        if (taskSubjects.containsKey(task.getTaskId())) {
            taskSubjects.get(task.getTaskId()).setValue(task);
        }
        allTasksSubjects.setValue(getTasks());
    }
}