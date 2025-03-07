package edu.ucsd.cse110.habitizer.lib.domain;

import androidx.annotation.Nullable;

import edu.ucsd.cse110.habitizer.lib.domain.timer.RoutineTimer;
import edu.ucsd.cse110.habitizer.lib.domain.timer.TaskTimer;
import edu.ucsd.cse110.habitizer.lib.domain.timer.Timer;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Routine implements Serializable {
    private final @Nullable Integer id;
    private final String routineName;
    private final List<Task> tasks = new ArrayList<>();
    private final RoutineTimer routineTimer = new RoutineTimer();
    private final TaskTimer taskTimer = new TaskTimer();
    private @Nullable Integer goalTime;

    private LocalDateTime currentTime = LocalDateTime.now();
    private boolean timerStopped;
//    LocalDateTime time1 = LocalDateTime.of(2025, 2, 1, 8, 0, 0); // 8:00:00 AM, 02/01/2025
//    LocalDateTime time2 = LocalDateTime.of(2025, 2, 1, 8, 30, 15); // 8:30:15 AM, 02/01/2025

    public Routine(@Nullable Integer id, String routineName) {
        this.id = id;
        this.routineName = routineName;
        this.goalTime = null;
        timerStopped = false;
    }

    // Start the routine
    public void startRoutine(LocalDateTime startTime) {
        // Make sure to end any possibly running timers
        if (routineTimer.isRunning()) {
            routineTimer.end(startTime);
        }
        if (taskTimer.isRunning()) {
            taskTimer.end(startTime);
        }
        
        // Restart timers
        routineTimer.start(startTime);
        taskTimer.start(startTime);
        currentTime = startTime;
        timerStopped = false;
    }

    // End the routine
    public void endRoutine(LocalDateTime endTime) {
        // Add debug log
        System.out.println("Ending routine at: " + endTime + 
                          ", routine was active for: " + 
                          (routineTimer.getStartTime() != null ? 
                           java.time.Duration.between(routineTimer.getStartTime(), endTime).getSeconds() / 60.0 : 
                           "unknown") + " minutes");
        
        // If the timer was stopped, ignore the actual system time and use
        // our adjusted time
        if (timerStopped) {
            routineTimer.end(currentTime);
        }
        routineTimer.end(endTime);

        markSkippedTasks();
        
        // If the timer is still running for some reason, force it to end
        if (routineTimer.isRunning()) {
            routineTimer.end(endTime);
        }
    }

    private void markSkippedTasks() {
        for (Task task : tasks) {
            if (!task.isCompleted()) {
                task.setSkipped(true);
            }
        }
    }

    // Get the time for the routine
    public long getRoutineDurationMinutes() {
        return routineTimer.getLiveMinutes(timerStopped, currentTime);
    }

    public boolean isActive() {
        return routineTimer.isActive();
    }

    // Add the task
    public void addTask(Task task) {
        tasks.add(task);
        System.out.println("Habitizer-Tasks: Task added to " + routineName + ": " + task.getTaskName() + 
                          " (ID: " + task.getTaskId() + "), tasks list now has " + tasks.size() + " items");
    }


    // End the task
    public void completeTask(String taskName) {
        Task task = tasks.stream()
                .filter(t -> t.getTaskName().equals(taskName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskName));
        
        // Get the current time for consistency
        LocalDateTime endTimeForTask = timerStopped ? currentTime : LocalDateTime.now();
        
        // DEBUG: Add more detailed logging about timer state
        System.out.println("===== TASK COMPLETION DETAILS =====");
        System.out.println("Completing task: '" + taskName + "'");
        System.out.println("Current wall time: " + LocalDateTime.now());
        System.out.println("Time used for calculations: " + endTimeForTask);
        System.out.println("Routine timer start: " + routineTimer.getStartTime());
        System.out.println("Routine timer active: " + routineTimer.isActive());
        System.out.println("Task timer start: " + taskTimer.getStartTime());
        System.out.println("Task timer running: " + taskTimer.isRunning());
        System.out.println("timerStopped flag: " + timerStopped);
        
        // Ensure TaskTimer is properly initialized
        if (taskTimer.getStartTime() == null || !taskTimer.isRunning()) {
            // If TaskTimer is not initialized, use RoutineTimer's start time
            LocalDateTime startTime = routineTimer.getStartTime();
            if (startTime == null) {
                // If RoutineTimer is also not initialized, use current time
                startTime = LocalDateTime.now().minusSeconds(1); // Ensure at least 1 second
            }
            taskTimer.start(startTime);
        }
        
        // End the task timer with consistent time
        taskTimer.end(endTimeForTask);

        // Calculate elapsed minutes
        int elapsedMinutes = taskTimer.getElapsedMinutes();
        
        // Calculate raw duration for debugging
        double rawMinutes = 0;
        if (taskTimer.getStartTime() != null && taskTimer.getEndTime() != null) {
            long durationSeconds = java.time.Duration.between(taskTimer.getStartTime(), taskTimer.getEndTime()).getSeconds();
            rawMinutes = durationSeconds / 60.0;
        }
        
        System.out.println("Task duration calculation:");
        System.out.println("- Start time: " + taskTimer.getStartTime());
        System.out.println("- End time: " + taskTimer.getEndTime());
        System.out.println("- Raw duration: " + rawMinutes + " minutes");
        System.out.println("- Rounded duration: " + elapsedMinutes + " minutes");
        
        // Set the task duration and mark as complete
        task.setDurationAndComplete(elapsedMinutes);

        // Reset the timer of the task with the same time used for ending
        taskTimer.start(endTimeForTask);
        
        System.out.println("Task timer restarted at: " + endTimeForTask);
        System.out.println("==============================");
    }


    // Auto completes routine when everything is checked off
    public boolean autoCompleteRoutine() {
        boolean allCompleted = true;
        for (Task task : tasks) {
            if (!task.isCheckedOff()) {
                allCompleted = false;
            }
        }

        if (allCompleted && tasks.size() > 0) {
            // End the routine when all tasks are completed
            endRoutine(LocalDateTime.now());
            
            // Mark any unchecked tasks as skipped (though there shouldn't be any)
            for (Task task : tasks) {
                if (!task.isCompleted()) {
                    task.setSkipped(true);
                }
            }
        }
        return allCompleted && tasks.size() > 0;
    }

    public void updateGoalTime(@Nullable Integer goalTime) {
        this.goalTime = goalTime;
    }

    public String formatGoalTime() {
        if (goalTime == null) return "-";
        return goalTime.toString();
    }

    // Testing functions
    // If timer stopped, set "current" time var to a specified value
    // From this point on, everything we reference is in line with the "current" time
    // and not the actual system time
    public void pauseTime(LocalDateTime pauseTime) {
        currentTime = pauseTime;
        timerStopped = true;
    }
    
    // Resume the timer with the current system time
    public void resumeTime(LocalDateTime resumeTime) {
        // Calculate time difference between paused time and resume time
        long secondsDifference = java.time.Duration.between(currentTime, resumeTime).getSeconds();
        
        // Only adjust if we were paused
        if (timerStopped) {
            // Update the start times of both timers to account for the time the app was in background
            if (routineTimer.isActive()) {
                routineTimer.updateStartTime(routineTimer.getStartTime().plusSeconds(secondsDifference));
            }
            
            if (taskTimer.isRunning()) {
                taskTimer.updateStartTime(taskTimer.getStartTime().plusSeconds(secondsDifference));
            }
            
            // Update current time to the resume time
            currentTime = resumeTime;
            timerStopped = false;
        }
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

    public void moveTaskUp(Task task) {
        if (tasks.size() == 1 || tasks.indexOf(task) == 0) {
            System.out.println("TASK_SWAP: Cannot move task up - already at top or single task: " + task.getTaskName());
            return;
        }
        int i = tasks.indexOf(task);
        
        // Log before swap
        System.out.println("TASK_SWAP: Moving task UP - Before swap: Position " + i + 
                           ", Task: " + task.getTaskName() + 
                           ", Above task: " + tasks.get(i-1).getTaskName());
        
        Collections.swap(tasks, i, i-1);
        
        // Log after swap
        System.out.println("TASK_SWAP: After swap: Position " + (i-1) + 
                           ", Task: " + tasks.get(i-1).getTaskName() + 
                           ", Below task: " + tasks.get(i).getTaskName());
    }

    public void moveTaskDown(Task task) {
        if(tasks.size() == 1 || tasks.indexOf(task) == tasks.size()-1) {
            System.out.println("TASK_SWAP: Cannot move task down - already at bottom or single task: " + task.getTaskName());
            return;
        }
        int i = tasks.indexOf(task);
        
        // Log before swap
        System.out.println("TASK_SWAP: Moving task DOWN - Before swap: Position " + i + 
                           ", Task: " + task.getTaskName() + 
                           ", Below task: " + tasks.get(i+1).getTaskName());
        
        Collections.swap(tasks, i, i+1);
        
        // Log after swap
        System.out.println("TASK_SWAP: After swap: Position " + (i+1) + 
                           ", Task: " + tasks.get(i+1).getTaskName() + 
                           ", Above task: " + tasks.get(i).getTaskName());
    }

    /**
     * Remove a task from the routine
     * @param task The task to remove
     * @return true if the task was found and removed, false otherwise
     */
    public boolean removeTask(Task task) {
        if (task == null) {
            System.out.println("TASK_REMOVE: Cannot remove null task");
            return false;
        }
        
        int index = tasks.indexOf(task);
        
        if (index == -1) {
            System.out.println("TASK_REMOVE: Task not found in routine: " + task.getTaskName());
            return false;
        }
        
        Task removedTask = tasks.remove(index);
        System.out.println("TASK_REMOVE: Task removed from " + routineName + ": " + 
                          removedTask.getTaskName() + " (ID: " + removedTask.getTaskId() + 
                          "), tasks list now has " + tasks.size() + " items");
        return true;
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

    public @Nullable Integer getGoalTime() {
        return goalTime;
    }

}