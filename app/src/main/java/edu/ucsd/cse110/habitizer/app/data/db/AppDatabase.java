package edu.ucsd.cse110.habitizer.app.data.db;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

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
    version = 2,  // Updated from 1 to 2 for elapsed_seconds column
    exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static final String TAG = "AppDatabase";
    private static final String DATABASE_NAME = "habitizer-db";
    
    // Migration from version 1 to 2 - adds elapsed_seconds column to tasks table
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Log.d(TAG, "Migrating database from version 1 to 2");
            database.execSQL("ALTER TABLE tasks ADD COLUMN elapsed_seconds INTEGER NOT NULL DEFAULT 0");
        }
    };
    
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
                    // Determine if we're in test mode by checking the package name
                    boolean isTestMode = context.getPackageName().contains("test");
                    
                    if (isTestMode) {
                        Log.d(TAG, "Creating in-memory database for test environment");
                        // Use in-memory database for tests
                        INSTANCE = Room.inMemoryDatabaseBuilder(
                                context.getApplicationContext(),
                                AppDatabase.class)
                                .build();
                    } else {
                        Log.d(TAG, "Creating persistent database for normal use");
                        // Use persistent database for normal use
                        INSTANCE = Room.databaseBuilder(
                                context.getApplicationContext(),
                                AppDatabase.class,
                                DATABASE_NAME)
                                .addMigrations(MIGRATION_1_2)  // Add migration
                                .fallbackToDestructiveMigration()
                                .build();
                    }
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * Reset the database instance (for testing)
     */
    public static void resetInstance() {
        Log.d(TAG, "Resetting database instance");
        synchronized (AppDatabase.class) {
            if (INSTANCE != null) {
                INSTANCE.clearAllTables();
                INSTANCE = null;
            }
        }
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