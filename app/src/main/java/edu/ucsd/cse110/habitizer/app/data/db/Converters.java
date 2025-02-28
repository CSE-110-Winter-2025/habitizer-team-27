package edu.ucsd.cse110.habitizer.app.data.db;

import androidx.room.TypeConverter;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

/**
 * Room database type converters
 */
public class Converters {
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
} 