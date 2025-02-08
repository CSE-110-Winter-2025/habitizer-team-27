package edu.ucsd.cse110.habitizer.lib.domain;

import java.time.LocalTime;

public class Task {
    /* The name of the task, if we need the description
    *of the task later on, we can add the fort and back
    *like the flash card */
    public String taskName;
    /* the status of the task, allow  user skip the task */
    public boolean isSkip;
    /* whenever the user clicks check off, the current
    *time will be stored here for further calculation */
    public LocalTime checkOffTime;

    /* Use to store the duration time for each task */
    private long durationTime;



    public Task(String taskName, boolean isSkip, LocalTime checkOffTime) {
        this.taskName = taskName;
        this.isSkip = isSkip;
        this.checkOffTime = checkOffTime;
        this.durationTime = 0;
    }

    /* Set the duration time */
    public void setDurationTime(long durationTime) {
        this.durationTime = durationTime;
    }

    /*Get the Task name */
    public String getTaskName() {
        return taskName;
    }

    /* Get skip status */
    public boolean isSkip() {
        return isSkip;
    }

    /* Get the check off time */
    public LocalTime getCheckOffTime() {
        return checkOffTime;
    }

    /* Get the duration time */
    public long getDurationTime() {
        return durationTime;
    }
}
