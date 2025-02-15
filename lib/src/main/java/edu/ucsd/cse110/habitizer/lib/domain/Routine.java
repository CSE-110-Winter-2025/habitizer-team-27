package edu.ucsd.cse110.habitizer.lib.domain;

import androidx.annotation.Nullable;

import edu.ucsd.cse110.habitizer.lib.domain.timer.RoutineTimer;
import edu.ucsd.cse110.habitizer.lib.domain.timer.TaskTimer;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Routine implements Serializable {
    private final @Nullable Integer id;
    private final String routineName;
    private final List<Task> tasks = new ArrayList<>();
    private final RoutineTimer routineTimer = new RoutineTimer();
    private final TaskTimer taskTimer = new TaskTimer();

    public LocalDateTime currentTime = LocalDateTime.now();


    public Routine(@Nullable Integer id, String routineName) {
        this.id = id;
        this.routineName = routineName;
    }

    // Start the routine
    public void startRoutine() {
        routineTimer.start(currentTime);
        // Start the timer of the task automatically
        taskTimer.start(currentTime);
    }

    // End the routine
    public void endRoutine() {
        routineTimer.end(LocalDateTime.now());
    }

    // Get the time for the routine(round up)
    public long getRoutineDurationMinutes() {
        return routineTimer.getElapsedMinutes();
    }

    // Add the task
    public void addTask(Task task) {
        tasks.add(task);
    }


    // End the task
    public void completeTask(String taskName) {
        Task task = tasks.stream()
                .filter(t -> t.getTaskName().equals(taskName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskName));
        taskTimer.end(currentTime);

        int elapsedMinutes = taskTimer.getElapsedMinutes();
        task.setDurationAndComplete(elapsedMinutes);

        // Reset the timer of the task
        taskTimer.start(currentTime);
    }

    public void advanceTime(int seconds) {
        currentTime = currentTime.plusSeconds(seconds);
    }



    // Getters
    public String getRoutineName() {
        return routineName;
    }
    public Integer getRoutineId() { return id; }

    public List<Task> getTasks() {
        return tasks;
    }

}
