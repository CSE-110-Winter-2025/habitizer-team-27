package edu.ucsd.cse110.habitizer.app;

import static androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.viewmodel.ViewModelInitializer;

import edu.ucsd.cse110.habitizer.lib.domain.RoutineRepository;
import edu.ucsd.cse110.habitizer.lib.domain.TaskRepository;
import edu.ucsd.cse110.observables.PlainMutableSubject;
import edu.ucsd.cse110.observables.Subject;

public class MainViewModel extends ViewModel {
    private static final String LOG_TAG = "MainViewModel";

    private final TaskRepository taskRepository;
    private final RoutineRepository routineRepository;

    private final Subject<String> goalTime;

    public static final ViewModelInitializer<MainViewModel> initializer =
            new ViewModelInitializer<>(
                    MainViewModel.class,
                    creationExtras -> {
                        var app = (HabitizerApplication) creationExtras.get(APPLICATION_KEY);
                        assert app != null;
                        return new MainViewModel(app.getTaskRepository(), app.getRoutineRepository());
                    });

    public MainViewModel(TaskRepository taskRepository, RoutineRepository routineRepository) {
        this.taskRepository = taskRepository;
        this.routineRepository = routineRepository;

        this.goalTime = new PlainMutableSubject<>();

    }

    public TaskRepository getTaskRepository() {
        return taskRepository;
    }

    public RoutineRepository getRoutineRepository() {
        return routineRepository;
    }

    public void updateRoutineTime(int time, int routineId) {
        routineRepository.getRoutine(routineId).updateGoalTime(time);
    }

    public Subject<String> getGoalTime() {
        return goalTime;
    }
}
