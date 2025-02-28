package edu.ucsd.cse110.habitizer.app.data.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;

/**
 * Room entity class representing the routine table in the database
 */
@Entity(tableName = "routines")
public class RoutineEntity {
    
    @PrimaryKey
    @ColumnInfo(name = "id")
    private Integer id;
    
    @ColumnInfo(name = "routine_name")
    private String routineName;
    
    @ColumnInfo(name = "goal_time")
    private Integer goalTime;
    
    @Ignore // Room ignores this field, not stored in the database
    private List<Task> tasks = new ArrayList<>();
    
    // No-args constructor required by Room
    public RoutineEntity() {}
    
    // Convenience constructor
    @androidx.room.Ignore
    public RoutineEntity(Integer id, String routineName, Integer goalTime) {
        this.id = id;
        this.routineName = routineName;
        this.goalTime = goalTime;
    }
    
    // Create RoutineEntity from a Routine
    public static RoutineEntity fromRoutine(Routine routine) {
        RoutineEntity entity = new RoutineEntity(
            routine.getRoutineId(),
            routine.getRoutineName(),
            routine.getGoalTime()
        );
        entity.tasks.addAll(routine.getTasks());
        return entity;
    }
    
    // Convert RoutineEntity back to Routine
    public Routine toRoutine() {
        Routine routine = new Routine(id, routineName);
        if (goalTime != null) {
            routine.updateGoalTime(goalTime);
        }
        
        // Add tasks to the routine
        for (Task task : tasks) {
            routine.addTask(task);
        }
        
        return routine;
    }
    
    // Getters and setters
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getRoutineName() {
        return routineName;
    }
    
    public void setRoutineName(String routineName) {
        this.routineName = routineName;
    }
    
    public Integer getGoalTime() {
        return goalTime;
    }
    
    public void setGoalTime(Integer goalTime) {
        this.goalTime = goalTime;
    }
    
    public List<Task> getTasks() {
        return tasks;
    }
    
    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }
    
    public void addTask(Task task) {
        this.tasks.add(task);
    }
} 