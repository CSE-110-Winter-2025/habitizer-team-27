package edu.ucsd.cse110.habitizer.app.data.db;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import edu.ucsd.cse110.habitizer.lib.domain.Task;

/**
 * Room database type converters
 */
public class Converters {
    private static final Gson gson = new Gson();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    /**
     * Convert LocalDateTime to String
     * @param dateTime Date and time
     * @return String representation
     */
    @TypeConverter
    public static String fromLocalDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(formatter);
    }
    
    /**
     * Convert String to LocalDateTime
     * @param value String representation
     * @return Date and time
     */
    @TypeConverter
    public static LocalDateTime toLocalDateTime(String value) {
        return value == null ? null : LocalDateTime.parse(value, formatter);
    }
    
    /**
     * Convert Task list to JSON string
     * @param tasks Task list
     * @return JSON string
     */
    @TypeConverter
    public static String fromTaskList(List<Task> tasks) {
        if (tasks == null) {
            return null;
        }
        return gson.toJson(tasks);
    }
    
    /**
     * Convert JSON string to Task list
     * @param value JSON string
     * @return Task list
     */
    @TypeConverter
    public static List<Task> toTaskList(String value) {
        if (value == null) {
            return new ArrayList<>();
        }
        Type listType = new TypeToken<List<Task>>() {}.getType();
        return gson.fromJson(value, listType);
    }
} 