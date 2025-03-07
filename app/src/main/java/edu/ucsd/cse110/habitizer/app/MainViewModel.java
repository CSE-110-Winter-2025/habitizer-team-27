package edu.ucsd.cse110.habitizer.app;

import static androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.viewmodel.ViewModelInitializer;

import edu.ucsd.cse110.habitizer.lib.domain.RoutineRepository;
import edu.ucsd.cse110.habitizer.lib.domain.TaskRepository;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;

import java.util.List;

public class MainViewModel extends ViewModel {
    private static final String LOG_TAG = "MainViewModel";

    private final TaskRepository taskRepository;
    private final RoutineRepository routineRepository;

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

        // Create observable subjects
    }

    public TaskRepository getTaskRepository() {
        return taskRepository;
    }

    public RoutineRepository getRoutineRepository() {
        return routineRepository;
    }

    /**
     * Get a routine by its ID
     * @param routineId The ID of the routine to retrieve
     * @return The found routine, or null if not found
     */
    public Routine getRoutineById(int routineId) {
        // Use the routineRepository to find the routine by ID
        if (routineId < 0) {
            return null;
        }
        
        // First check if we can find it in the current list of routines
        List<Routine> routines = getRoutineRepository().findAll().getValue();
        if (routines != null) {
            for (Routine routine : routines) {
                if (routine.getRoutineId() == routineId) {
                    return routine;
                }
            }
        }
        
        // If not found in the list, try to get it directly from the repository
        return getRoutineRepository().find(routineId).getValue();
    }
}
