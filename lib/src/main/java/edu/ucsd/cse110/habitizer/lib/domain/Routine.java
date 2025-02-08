package edu.ucsd.cse110.habitizer.lib.domain;


import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Routine {
    private final String routineName;
    private final List<Task> tasks;
    private boolean isRoutineStarted = false;

    public Routine(String routineName) {
        this.routineName = routineName;
        this.tasks = new ArrayList<>();
    }

    public void startRoutine() {
        /*To ensure the user double click the start button
        * if click again, just simply return not thing*/
        if (isRoutineStarted) {
            System.out.println("Routine Begin!");
            return;
        }

        isRoutineStarted = true;
        LocalTime startTime = LocalTime.now();
        tasks.add(new Task(routineName, false, startTime)); 
    }

    public void checkOffTask(String taskName) {
        /*when check off the task, we need the task name to ensure
        * is the right task to check off*/
        LocalTime checkOffTime = LocalTime.now();
        tasks.add(new Task(taskName, false, checkOffTime));
    }

    public void skipTask(String taskName) {
        /*when skip the task, we also require the task name
        * to ensure the right task to skip and also set the
        * check off time is null, when we calculating the duration
        * of time  we don't count this task */
        tasks.add(new Task(taskName, true, null));
    }

    public void endRoutine() {
        /*To ensure the user double click the end button
         * if click again, just simply return not thing*/
        if (!isRoutineStarted) {
            System.out.println("Routine not Begin yet");
            return;
        }

        isRoutineStarted = false;
        System.out.println("\nEnd " + routineName + " Routine！");

        /*Check to see if there is an empty List*/
        if (tasks.size() < 2) {
            System.out.println("No record");
            return;
        }

        Task prevTask = tasks.get(0); //get the start time
        for (int i = 1; i < tasks.size(); i++) {
            Task currentTask = tasks.get(i);

            if (currentTask.isSkip()) {
                System.out.println(currentTask.getTaskName() + "Task Skipped");
            } else {
                long durationMinutes = Duration.between(prevTask.getCheckOffTime(), currentTask.getCheckOffTime()).toMinutes();
                currentTask.setDurationTime(durationMinutes);
                System.out.println(currentTask.getTaskName() + " Used " + currentTask.getDurationTime() + "minute");
                prevTask = currentTask;
            }
        }
    }
    //get the task info
    public List<Task> getTasks() {
        return tasks;
    }
    //get the routine's name
    public String getRoutineName() {
        return routineName;
    }
}
