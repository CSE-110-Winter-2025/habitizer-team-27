package edu.ucsd.cse110.habitizer.app.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import org.threeten.bp.LocalDateTime;

/**
 * Room database for the application
 */
@Database(
    entities = {
        TaskEntity.class,
        RoutineEntity.class,
        RoutineTaskCrossRef.class
    },
    version = 1,
    exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "habitizer-db";
    
    // Singleton pattern
    private static volatile AppDatabase INSTANCE;
    
    /**
     * Get database instance
     * @param context Application context
     * @return Database instance
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * Get task DAO
     * @return Task DAO
     */
    public abstract TaskDao taskDao();
    
    /**
     * Get routine DAO
     * @return Routine DAO
     */
    public abstract RoutineDao routineDao();
} 