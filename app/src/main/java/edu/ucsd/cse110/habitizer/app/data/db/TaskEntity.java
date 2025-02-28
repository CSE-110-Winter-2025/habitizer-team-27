package edu.ucsd.cse110.habitizer.app.data.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import edu.ucsd.cse110.habitizer.lib.domain.Task;

/**
 * Room entity class representing the task table in the database
 */
@Entity(tableName = "tasks")
public class TaskEntity {
    
    @PrimaryKey
    @ColumnInfo(name = "id")
    private Integer id;
    
    @ColumnInfo(name = "task_name")
    private String taskName;
    
    @ColumnInfo(name = "is_checked_off")
    private boolean isCheckedOff;
    
    @ColumnInfo(name = "is_completed")
    private boolean isCompleted;
    
    @ColumnInfo(name = "is_skipped")
    private boolean isSkipped;
    
    @ColumnInfo(name = "duration")
    private int duration;
    
    // No-args constructor required by Room
    public TaskEntity() {}
    
    // Convenience constructor
    @androidx.room.Ignore
    public TaskEntity(Integer id, String taskName, boolean isCheckedOff, boolean isCompleted, boolean isSkipped, int duration) {
        this.id = id;
        this.taskName = taskName;
        this.isCheckedOff = isCheckedOff;
        this.isCompleted = isCompleted;
        this.isSkipped = isSkipped;
        this.duration = duration;
    }
    
    // Convert Task object to TaskEntity
    public static TaskEntity fromTask(Task task) {
        return new TaskEntity(
            task.getTaskId(),
            task.getTaskName(),
            task.isCheckedOff(),
            task.isCompleted(),
            task.isSkipped(),
            task.getDuration()
        );
    }
    
    // Convert TaskEntity back to Task object
    public Task toTask() {
        Task task = new Task(id, taskName, isCheckedOff);
        if (isCompleted) {
            task.setDurationAndComplete(duration);
        }
        task.setSkipped(isSkipped);
        return task;
    }
    
    // Getters and setters
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getTaskName() {
        return taskName;
    }
    
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
    
    public boolean isCheckedOff() {
        return isCheckedOff;
    }
    
    public void setCheckedOff(boolean checkedOff) {
        isCheckedOff = checkedOff;
    }
    
    public boolean isCompleted() {
        return isCompleted;
    }
    
    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
    
    public boolean isSkipped() {
        return isSkipped;
    }
    
    public void setSkipped(boolean skipped) {
        isSkipped = skipped;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public void setDuration(int duration) {
        this.duration = duration;
    }
} 