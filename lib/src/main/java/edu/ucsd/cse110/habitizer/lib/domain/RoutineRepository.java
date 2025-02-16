package edu.ucsd.cse110.habitizer.lib.domain;

import java.util.List;

import edu.ucsd.cse110.habitizer.lib.data.InMemoryDataSource;
import edu.ucsd.cse110.observables.Subject;

public class RoutineRepository {
    private final InMemoryDataSource dataSource;

    public int count() {
        return dataSource.getRoutineCount();
    }

    public RoutineRepository(InMemoryDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Subject<Routine> find(int id) {
        return dataSource.getRoutineSubject(id);
    }

    public Routine getRoutine(int id) {
        return dataSource.getRoutine(id);
    }

    public Subject<List<Routine>> findAll() {
        return dataSource.getAllRoutinesSubject();
    }

    public void save(Routine routine) {
        dataSource.putRoutine(routine);
    }
}
