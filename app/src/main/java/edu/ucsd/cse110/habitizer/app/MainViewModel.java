package edu.ucsd.cse110.habitizer.app;

import edu.ucsd.cse110.habitizer.lib.domain.RoutineRepository;
import edu.ucsd.cse110.habitizer.lib.domain.TaskRepository;

public class MainViewModel {
    private static final String LOG_TAG = "MainViewModel";

    private final TaskRepository taskRepository;
    private final RoutineRepository routineRepository;

    public MainViewModel(TaskRepository taskRepository, RoutineRepository routineRepository) {
        this.taskRepository = taskRepository;
        this.routineRepository = routineRepository;
    }
}
