package edu.ucsd.cse110.habitizer.app.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

/**
 * Data Access Object for tasks
 */
@Dao
public interface TaskDao {
    
    /**
     * Insert a task, replace if ID already exists
     * @param task Task to insert
     * @return ID of the inserted task
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TaskEntity task);
    
    /**
     * Insert multiple tasks, replace if IDs already exist
     * @param tasks List of tasks to insert
     * @return List of IDs of the inserted tasks
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<TaskEntity> tasks);
    
    /**
     * Find a task by ID
     * @param id Task ID
     * @return Found task
     */
    @Query("SELECT * FROM tasks WHERE id = :id")
    TaskEntity find(int id);
    
    /**
     * Find a task by ID, returns LiveData
     * @param id Task ID
     * @return LiveData of the found task
     */
    @Query("SELECT * FROM tasks WHERE id = :id")
    LiveData<TaskEntity> findAsLiveData(int id);
    
    /**
     * Find all tasks
     * @return List of all tasks
     */
    @Query("SELECT * FROM tasks")
    List<TaskEntity> findAll();
    
    /**
     * Find all tasks, returns LiveData
     * @return LiveData of all tasks
     */
    @Query("SELECT * FROM tasks")
    LiveData<List<TaskEntity>> findAllAsLiveData();
    
    /**
     * Count the number of tasks
     * @return Number of tasks
     */
    @Query("SELECT COUNT(*) FROM tasks")
    int count();
    
    /**
     * Delete a task
     * @param id ID of the task to delete
     */
    @Query("DELETE FROM tasks WHERE id = :id")
    void delete(int id);
    
    /**
     * Delete all tasks
     */
    @Query("DELETE FROM tasks")
    void deleteAll();
    
    /**
     * Find a task by exact name
     * @param taskName Task name to search for
     * @return Found task or null if not found
     */
    @Query("SELECT * FROM tasks WHERE task_name = :taskName LIMIT 1")
    TaskEntity findByNameExact(String taskName);
} 