package edu.ucsd.cse110.habitizer.lib.domain;

import edu.ucsd.cse110.habitizer.lib.data.InMemoryDataSource;
import edu.ucsd.cse110.observables.Subject;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class RoutineRepositoryTest {
    private RoutineRepository routineRepository;
    private InMemoryDataSource dataSource;

    @Before
    public void setUp() {
        dataSource = new InMemoryDataSource();
        routineRepository = new RoutineRepository(dataSource);
    }

    @Test
    public void testCount() {
        assertEquals(0, routineRepository.count());

        Routine routine1 = new Routine(1, "Morning Routine");
        routineRepository.save(routine1);

        assertEquals(1, routineRepository.count());
    }

    @Test
    public void testFind() {
        Routine routine1 = new Routine(1, "Morning Routine");
        routineRepository.save(routine1);

        Subject<Routine> subject = routineRepository.find(1);
        assertNotNull(subject);
        assertEquals("Morning Routine", subject.getValue().getRoutineName());
    }

    @Test
    public void testGetRoutine() {
        Routine routine1 = new Routine(1, "Workout Routine");
        routineRepository.save(routine1);

        Routine fetchedRoutine = routineRepository.getRoutine(1);
        assertNotNull(fetchedRoutine);
        assertEquals("Workout Routine", fetchedRoutine.getRoutineName());
    }

    @Test
    public void testFindAll() {
        Routine routine1 = new Routine(1, "Routine 1");
        Routine routine2 = new Routine(2, "Routine 2");

        routineRepository.save(routine1);
        routineRepository.save(routine2);

        Subject<List<Routine>> allRoutines = routineRepository.findAll();
        assertNotNull(allRoutines);
        assertEquals(2, allRoutines.getValue().size());
    }

    @Test
    public void testSave() {
        Routine routine1 = new Routine(1, "New Routine");
        routineRepository.save(routine1);

        Routine retrievedRoutine = routineRepository.getRoutine(1);
        assertNotNull(retrievedRoutine);
        assertEquals("New Routine", retrievedRoutine.getRoutineName());
    }
}
