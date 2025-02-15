package edu.ucsd.cse110.habitizer.app;

import static org.junit.Assert.*;


import android.content.Context;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import edu.ucsd.cse110.habitizer.app.ui.routine.TaskAdapter;
import edu.ucsd.cse110.habitizer.lib.data.InMemoryDataSource;
import edu.ucsd.cse110.habitizer.lib.domain.Task;
import edu.ucsd.cse110.habitizer.lib.domain.TaskRepository;
import edu.ucsd.cse110.habitizer.lib.domain.timer.RoutineTimer;


public class SkipTest {
    private Context context;
    private TaskRepository taskRepo;
    private TaskAdapter adapter;
    private RoutineTimer timer;
    private InMemoryDataSource dataSource;

    private final List<Task> morningRoutine = Arrays.asList(
            new Task(0, "Shower"),
            new Task(1, "Brush teeth"),
            new Task(2, "Dress")
    );

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        dataSource = new InMemoryDataSource();
        taskRepo = new TaskRepository(dataSource);

        // Initialize repository with tasks
        morningRoutine.forEach(taskRepo::save);

        timer = new RoutineTimer();
        adapter = new TaskAdapter(context, R.layout.task_page, taskRepo.findAll().getValue());
    }

    @Test
    public void testScenario1_SkipAndCompleteNext() {
        // Start routine timer
        LocalDateTime startTime = LocalDateTime.now();
        timer.start(startTime);

        // Complete Shower (2m10s elapsed)
        Task shower = taskRepo.find(0).getValue();
        shower.completeTask();
        taskRepo.save(shower);
        timer.advanceTime(130); // 2m10s

        // Skip Brush teeth (no action)

        // Complete Dress (5m20s later)
        Task dress = taskRepo.find(2).getValue();
        timer.advanceTime(320); // 5m20s
        dress.completeTask();
        taskRepo.save(dress);

        // Verify total time (130+320=450s = 7.5m → 8m)
        timer.end(startTime.plusSeconds(450));
        assertEquals(8, timer.getElapsedMinutes());

//        // Verify UI
          adapter = new TaskAdapter(context, R.layout.task_page, taskRepo.findAll().getValue());
          assertTaskDisplayedCorrectly(0, "Shower", "2m", true);  // Completed first task
          assertTaskDisplayedCorrectly(1, "Brush teeth", "", false); // Skipped
          assertTaskDisplayedCorrectly(2, "Dress", "8m", true);  // Final task
    }

    @Test
    public void testScenario2_SkipMultipleTasks() {
        LocalDateTime startTime = LocalDateTime.now();
        timer.start(startTime);

        // Skip Shower after 2m10s
        timer.advanceTime(130);

        // Skip Brush teeth after 3m20s
        timer.advanceTime(200);

        // Complete Dress after 5m20s
        Task dress = taskRepo.find(2).getValue();
        timer.advanceTime(320);
        dress.completeTask();
        taskRepo.save(dress);

        // Total time: 130+200+320=650s = 10m50s → 11m
        timer.end(startTime.plusSeconds(650));
        assertEquals(11, timer.getElapsedMinutes());

       // Verify UI
        adapter = new TaskAdapter(context, R.layout.task_page, taskRepo.findAll().getValue());
        assertTaskDisplayedCorrectly(0, "Shower", "", false);
        assertTaskDisplayedCorrectly(1, "Brush teeth", "", false);
        assertTaskDisplayedCorrectly(2, "Dress", "11m", true);
    }

    private void assertTaskDisplayedCorrectly(int position, String expectedName,
                                              String expectedTime, boolean isCompleted) {
        var view = adapter.getView(position, null, null);

        // Match XML IDs
        TextView nameView = view.findViewById(R.id.task_name);
        assertEquals(expectedName, nameView.getText());

        TextView timeView = view.findViewById(R.id.task_time);
        assertEquals(expectedTime, timeView.getText().toString());

        // Fix checkbox ID to match XML
        CheckBox checkBox = view.findViewById(R.id.check_task);
        assertEquals(isCompleted, checkBox.isChecked());
    }

}

