package edu.ucsd.cse110.habitizer.app.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

/**
 * Data Access Object for routines
 */
@Dao
public interface RoutineDao {
    
    /**
     * Insert a routine, replace if ID already exists
     * @param routine Routine to insert
     * @return ID of the inserted routine
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(RoutineEntity routine);
    
    /**
     * Insert multiple routines, replace if IDs already exist
     * @param routines List of routines to insert
     * @return List of IDs of the inserted routines
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<RoutineEntity> routines);
    
    /**
     * Insert routine and task association relationship
     * @param crossRef Routine and task association
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRoutineTaskCrossRef(RoutineTaskCrossRef crossRef);
    
    /**
     * Find a routine by ID
     * @param id Routine ID
     * @return Found routine
     */
    @Query("SELECT * FROM routines WHERE id = :id")
    RoutineEntity find(int id);
    
    /**
     * Find a routine by ID, returns LiveData
     * @param id Routine ID
     * @return LiveData of the found routine
     */
    @Query("SELECT * FROM routines WHERE id = :id")
    LiveData<RoutineEntity> findAsLiveData(int id);
    
    /**
     * Find all routines
     * @return List of all routines
     */
    @Query("SELECT * FROM routines")
    List<RoutineEntity> findAll();
    
    /**
     * Find all routines, returns LiveData
     * @return LiveData of all routines
     */
    @Query("SELECT * FROM routines")
    LiveData<List<RoutineEntity>> findAllAsLiveData();
    
    /**
     * Find a routine and all its associated tasks
     * @param routineId Routine ID
     * @return Routine with tasks
     */
    @Transaction
    @Query("SELECT * FROM routines WHERE id = :routineId")
    RoutineWithTasks getRoutineWithTasks(int routineId);
    
    /**
     * Find a routine and all its associated tasks, returns LiveData
     * @param routineId Routine ID
     * @return LiveData of routine with tasks
     */
    @Transaction
    @Query("SELECT * FROM routines WHERE id = :routineId")
    LiveData<RoutineWithTasks> getRoutineWithTasksAsLiveData(int routineId);
    
    /**
     * Find all routines and their associated tasks
     * @return List of all routines with tasks
     */
    @Transaction
    @Query("SELECT * FROM routines")
    List<RoutineWithTasks> getAllRoutinesWithTasks();
    
    /**
     * Find all routines and their associated tasks, returns LiveData
     * @return LiveData of all routines with tasks
     */
    @Transaction
    @Query("SELECT * FROM routines")
    LiveData<List<RoutineWithTasks>> getAllRoutinesWithTasksAsLiveData();
    
    /**
     * Count the number of routines
     * @return Number of routines
     */
    @Query("SELECT COUNT(*) FROM routines")
    int count();
    
    /**
     * Delete a routine
     * @param id ID of the routine to delete
     */
    @Query("DELETE FROM routines WHERE id = :id")
    void delete(int id);
    
    /**
     * Delete associations between a routine and tasks
     * @param routineId Routine ID
     */
    @Query("DELETE FROM routine_task_cross_refs WHERE routine_id = :routineId")
    void deleteRoutineTaskCrossRefs(int routineId);
    
    /**
     * Delete all routines
     */
    @Query("DELETE FROM routines")
    void deleteAll();
    
    /**
     * Delete all associations between routines and tasks
     */
    @Query("DELETE FROM routine_task_cross_refs")
    void deleteAllRoutineTaskCrossRefs();
} 