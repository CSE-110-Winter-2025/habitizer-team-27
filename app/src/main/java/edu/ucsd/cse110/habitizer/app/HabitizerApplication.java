package edu.ucsd.cse110.habitizer.app;

import android.app.Application;
import android.util.Log;

import edu.ucsd.cse110.habitizer.lib.data.InMemoryDataSource;
import edu.ucsd.cse110.habitizer.lib.domain.RoutineRepository;
import edu.ucsd.cse110.habitizer.lib.domain.TaskRepository;

public class HabitizerApplication extends Application {
    private InMemoryDataSource dataSource;
    private TaskRepository taskRepository;
    private RoutineRepository routineRepository;

    @Override
    public void onCreate() {
        super.onCreate();

        this.dataSource = InMemoryDataSource.fromDefault();
        this.taskRepository = new TaskRepository(dataSource);
        this.routineRepository = new RoutineRepository(dataSource);

        // Debug: Print loaded data
        Log.d("HabitizerApp", "Tasks: " + dataSource.getTasks());
        Log.d("HabitizerApp", "Routines: " + dataSource.getRoutines());
    }

    public TaskRepository getTaskRepository() {
        return taskRepository;
    }

    public RoutineRepository getRoutineRepository() {
        return routineRepository;
    }

    public InMemoryDataSource getDataSource() {
        return dataSource;
    }
}
