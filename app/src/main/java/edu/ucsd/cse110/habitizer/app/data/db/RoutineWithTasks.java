package edu.ucsd.cse110.habitizer.app.data.db;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.Comparator;
import java.util.List;

import edu.ucsd.cse110.habitizer.lib.domain.Routine;

/**
 * Composite data class containing a routine and all its tasks
 */
public class RoutineWithTasks {
    @Embedded
    public RoutineEntity routine;
    
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = @Junction(
            value = RoutineTaskCrossRef.class,
            parentColumn = "routine_id",
            entityColumn = "task_id"
        )
    )
    public List<TaskEntity> tasks;
    
    /**
     * Convert RoutineWithTasks to domain object Routine
     * @return Domain object Routine
     */
    public Routine toRoutine() {
        Routine domainRoutine = routine.toRoutine();
        
        // Clear any existing tasks because we'll add the tasks retrieved from the database
        domainRoutine.getTasks().clear();
        
        // Add all tasks to the routine
        if (tasks != null) {
            // Add tasks in order
            tasks.stream()
                .map(TaskEntity::toTask)
                .forEach(domainRoutine::addTask);
        }
        
        return domainRoutine;
    }
} 