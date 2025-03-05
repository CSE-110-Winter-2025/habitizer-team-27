//package edu.ucsd.cse110.habitizer.app.MS1Tests;
//
//import static org.junit.Assert.*;
//
//import android.content.Context;
//import android.widget.CheckBox;
//import android.widget.TextView;
//
//import androidx.test.core.app.ApplicationProvider;
//import androidx.test.ext.junit.runners.AndroidJUnit4;
//
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Objects;
//
//import edu.ucsd.cse110.habitizer.app.R;
//import edu.ucsd.cse110.habitizer.app.ui.routine.TaskAdapter;
//import edu.ucsd.cse110.habitizer.lib.data.InMemoryDataSource;
//import edu.ucsd.cse110.habitizer.lib.domain.Routine;
//import edu.ucsd.cse110.habitizer.lib.domain.Task;
//import edu.ucsd.cse110.habitizer.lib.domain.TaskRepository;
//
//@RunWith(AndroidJUnit4.class)
//public class SkipTest {
//    private static final int SHOWER_ID = 0;
//    private static final int BRUSH_TEETH_ID = 1;
//    private static final int DRESS_ID = 2;
//
//    private final List<Task> morningTasks = Arrays.asList(
//            new Task(SHOWER_ID, "Shower", false),
//            new Task(BRUSH_TEETH_ID, "Brush teeth", false),
//            new Task(DRESS_ID, "Dress", false)
//    );
//
//    private Context context;
//    private TaskRepository taskRepo;
//    private TaskAdapter adapter;
//    private Routine morningRoutine;
//
//    private InMemoryDataSource dataSource;
//
//
//
//    @Before
//    public void setUp() {
//        context = ApplicationProvider.getApplicationContext();
//        dataSource = new InMemoryDataSource();
//        taskRepo = new TaskRepository(dataSource);
//
//        morningRoutine = new Routine(0, "Morning Routine");
//        morningTasks.forEach(task -> {
//            taskRepo.save(task);
//            morningRoutine.addTask(task);
//        });
//
//        adapter = new TaskAdapter(context, R.layout.task_page, taskRepo.findAll().getValue(), morningRoutine, dataSource, null);
//
//    }
//
//    @Test
//    public void testScenario1_SkipAndCompleteNext() throws InterruptedException {
//        morningRoutine.startRoutine(LocalDateTime.now());
//
//        // Complete Shower
//        completeTask(SHOWER_ID, 0);
//
//        // Skip Brush Teeth after 2 min 10 s
//        for (int i = 0; i < 130 / 30; i++) {
//            morningRoutine.fastForwardTime();
//        }
//        morningRoutine.fastForwardTime();
//
//
//        // Complete Dress after 5 min 20 s
//        for (int i = 0; i < 320 / 30; i++) {
//            morningRoutine.fastForwardTime();
//        }
//        morningRoutine.fastForwardTime();
//
//        completeTask(DRESS_ID, 0);
//
//        // Verify UI
//        assertTaskDisplayed(BRUSH_TEETH_ID, "", false);
//        assertTaskDisplayed(DRESS_ID, "8m", true);
//    }
//
//    @Test
//    public void testScenario2_SkipMultipleTasks() throws InterruptedException {
//        morningRoutine.startRoutine(LocalDateTime.now());
//
//        // Skip Shower after 2 min 10 s
//        for (int i = 0; i < 130 / 30; i++) {
//            morningRoutine.fastForwardTime();
//        }
//        morningRoutine.fastForwardTime();
//
//        // Skip Brush Teeth after 3 min 20 s
//        for (int i = 0; i < 200 / 30; i++) {
//            morningRoutine.fastForwardTime();
//        }
//        morningRoutine.fastForwardTime();
//
//        // Complete Dress after 5 min 20 s
//        for (int i = 0; i < 320 / 30; i++) {
//            morningRoutine.fastForwardTime();
//        }
//
//        completeTask(DRESS_ID, 0);
//
//        // Verify UI
//        assertTaskDisplayed(SHOWER_ID, "", false);
//        assertTaskDisplayed(BRUSH_TEETH_ID, "", false);
//        assertTaskDisplayed(DRESS_ID, "11m", true);
//    }
//
//
//
//    private void completeTask(int taskId, int duration) {
//        Task task = getTask(taskId);
//        morningRoutine.advanceTime(duration);
//        morningRoutine.completeTask(morningTasks.get(taskId).toString());
//    }
//
//    private Task getTask(int taskId) {
//        Task task = taskRepo.find(taskId).getValue();
//        assertNotNull("Task not found: " + taskId, task);
//        return task;
//    }
//
//    private void assertTaskDisplayed(int taskId, String expectedTime, boolean isCompleted) {
//        int position = morningTasks.indexOf(taskRepo.find(taskId).getValue());
//        var view = adapter.getView(position, null, null);
//
//        TextView nameView = view.findViewById(R.id.task_name);
//        assertEquals(Objects.requireNonNull(taskRepo.find(taskId).getValue()).getTaskName(), nameView.getText());
//
//        TextView timeView = view.findViewById(R.id.task_time);
//        assertEquals(expectedTime, timeView.getText().toString());
//
//        CheckBox checkBox = view.findViewById(R.id.check_task);
//        assertEquals(isCompleted, checkBox.isChecked());
//    }
//}