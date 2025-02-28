package edu.ucsd.cse110.habitizer.app.data.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

/**
 * Represents a many-to-many relationship between routines and tasks
 * A routine can contain multiple tasks, and a task can belong to multiple routines
 */
@Entity(
    tableName = "routine_task_cross_refs",
    primaryKeys = {"routine_id", "task_id"},
    foreignKeys = {
        @ForeignKey(
            entity = RoutineEntity.class,
            parentColumns = "id",
            childColumns = "routine_id",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = TaskEntity.class,
            parentColumns = "id",
            childColumns = "task_id",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index("routine_id"),
        @Index("task_id")
    }
)
public class RoutineTaskCrossRef {
    
    @ColumnInfo(name = "routine_id")
    public int routineId;
    
    @ColumnInfo(name = "task_id")
    public int taskId;
    
    @ColumnInfo(name = "task_position")
    public int taskPosition;
    
    public RoutineTaskCrossRef() {}
    
    @androidx.room.Ignore
    public RoutineTaskCrossRef(int routineId, int taskId, int taskPosition) {
        this.routineId = routineId;
        this.taskId = taskId;
        this.taskPosition = taskPosition;
    }
} 