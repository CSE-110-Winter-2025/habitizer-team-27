package edu.ucsd.cse110.habitizer.lib.domain;

import androidx.annotation.Nullable;

import edu.ucsd.cse110.habitizer.lib.domain.timer.RoutineTimer;
import edu.ucsd.cse110.habitizer.lib.domain.timer.TaskTimer;
import edu.ucsd.cse110.habitizer.lib.domain.timer.Timer;

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

    private LocalDateTime currentTime = LocalDateTime.now();
    private boolean timerStopped;
//    LocalDateTime time1 = LocalDateTime.of(2025, 2, 1, 8, 0, 0); // 8:00:00 AM, 02/01/2025
//    LocalDateTime time2 = LocalDateTime.of(2025, 2, 1, 8, 30, 15); // 8:30:15 AM, 02/01/2025

    public Routine(@Nullable Integer id, String routineName) {
        this.id = id;
        this.routineName = routineName;
        timerStopped = false;
    }

    // Start the routine
    public void startRoutine(LocalDateTime startTime) {
        routineTimer.start(startTime);
        // Start the timer of the task automatically
        taskTimer.start(startTime);
        currentTime = startTime;
    }

    // End the routine
    public void endRoutine(LocalDateTime endTime) {
        // If the timer was stopped, ignore the actual system time and use
        // our adjusted time
        if (timerStopped) {
            routineTimer.end(currentTime);
        }
        routineTimer.end(endTime);
    }

    // Get the time for the routine
    public long getRoutineDurationMinutes() {

        return routineTimer.getLiveMinutes();
    }

    public boolean isActive() {
        return routineTimer.isActive();
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
        if (timerStopped) {
            taskTimer.end(currentTime);
        } else {
            taskTimer.end(LocalDateTime.now());
        }

        int elapsedMinutes = taskTimer.getElapsedMinutes();
        task.setDurationAndComplete(elapsedMinutes);

        // Reset the timer of the task
        if (timerStopped) {
            taskTimer.start(currentTime);
        } else {
            taskTimer.start(LocalDateTime.now());
        }
    }

    // Auto completes routine when everything is checked off
    public boolean autoCompleteRoutine() {
        for(int i = 0; i < tasks.size(); i++){
            if(!tasks.get(i).isCheckedOff()){
                return false;
            }
        }
        endRoutine(LocalDateTime.now());
        return true;
    }

    // Testing functions
    // If timer stopped, set "current" time var to a specified value
    // From this point on, everything we reference is in line with the "current" time
    // and not the actual system time
    public void pauseTime(LocalDateTime pauseTime) {
        currentTime = pauseTime;
        timerStopped = true;
    }

    // Fast forward by thirty seconds
    public void fastForwardTime() {
        // If timer is still running, update the start time of task and routine to mimic fast-forward
        if (!timerStopped) {
            routineTimer.updateStartTime(routineTimer.getStartTime().minusSeconds(30));
            taskTimer.updateStartTime(taskTimer.getStartTime().minusSeconds(30));
        }
        // If timer not running, then we "fast forward" time by 30 seconds
        else {
            currentTime = currentTime.plusSeconds(30);
        }
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

    public RoutineTimer getRoutineTimer() {
        return routineTimer;
    }

    public TaskTimer getTaskTimer() {
        return taskTimer;
    }

    public LocalDateTime getCurrentTime() {
        return currentTime;
    }

}