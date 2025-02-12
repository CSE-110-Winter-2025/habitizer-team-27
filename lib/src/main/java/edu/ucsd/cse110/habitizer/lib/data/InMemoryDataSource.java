package edu.ucsd.cse110.habitizer.lib.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;
import edu.ucsd.cse110.observables.PlainMutableSubject;

public class InMemoryDataSource {

    // Observers for routines
    private final Map<String, Routine> routines = new HashMap<>();
    private final Map<String, PlainMutableSubject<Routine>> routineSubjects = new HashMap<>();
    private final PlainMutableSubject<List<Routine>> allRoutineSubjects = new PlainMutableSubject<>();

    // Observers for morning tasks
    private final Map<String, Task> morningTasks = new HashMap<>();
    private final Map<String, PlainMutableSubject<Task>> morningTaskSubjects = new HashMap<>();
    private final PlainMutableSubject<List<Task>> allMorningTaskSubjects = new PlainMutableSubject<>();

    // Observers for night tasks
    private final Map<String, Task> eveningTasks = new HashMap<>();
    private final Map<String, PlainMutableSubject<Task>> eveningTaskSubjects = new HashMap<>();
    private final PlainMutableSubject<List<Task>> allEveningTaskSubjects = new PlainMutableSubject<>();

    /* public final static List<Task> DEFAULT_MORNING = List.of(
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
    ); */

    // Constructor to initialize default routines
    public InMemoryDataSource() {
        initializeDefaultRoutines();
    }

    // Method to populate default routines
    private void initializeDefaultRoutines() {
        Routine morningRoutine = new Routine("Morning Routine");
        morningRoutine.addTask(new Task(, "Brush Teeth"));
        morningRoutine.addTask(new Task(, "Eat Breakfast"));
        routines.put(morningRoutine.getRoutineName(), morningRoutine);

        Routine eveningRoutine = new Routine("Evening Routine");
        eveningRoutine.addTask(new Task(, "Shower"));
        eveningRoutine.addTask(new Task(, "Read a Book"));
        routines.put(eveningRoutine.getRoutineName(), eveningRoutine);
    }

    // Add a routine to the data source
    public void addRoutine(Routine routine) {
        routines.put(routine.getRoutineName(), routine);
    }

    // Retrieve a routine by name
    public Routine getRoutine(String routineName) {
        return routines.get(routineName);
    }

    // Remove a routine by name
    public void removeRoutine(String routineName) {
        routines.remove(routineName);
    }

    // Get all routines
    public Map<String, Routine> getAllRoutines() {
        return routines;
    }
}