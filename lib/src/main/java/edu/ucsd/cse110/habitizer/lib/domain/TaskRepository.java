package edu.ucsd.cse110.habitizer.lib.domain;

import java.util.List;

import edu.ucsd.cse110.habitizer.lib.data.InMemoryDataSource;
import edu.ucsd.cse110.observables.Subject;

public class TaskRepository {
    private final InMemoryDataSource dataSource;

    public TaskRepository(InMemoryDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Subject<Task> find(int id) {
        return dataSource.getTaskSubject(id);
    }

    public Subject<List<Task>> findAll() {
        return dataSource.getAllTasksSubject();
    }

    public void save(Task task) {
        dataSource.putTask(task);
    }
}
