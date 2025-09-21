package com.smilepile.data.database

import androidx.room.TypeConverter
import java.util.Date

/**
 * Type converters for Room database
 * Handles conversion between complex types and primitive types that Room can store
 */
class Converters {

    /**
     * Converts timestamp (Long) to Date
     * @param value Timestamp in milliseconds
     * @return Date object or null
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    /**
     * Converts Date to timestamp (Long)
     * @param date Date object
     * @return Timestamp in milliseconds or null
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    /**
     * Converts comma-separated string to List<String>
     * Useful for storing lists of strings in a single column
     * @param value Comma-separated string
     * @return List of strings or empty list
     */
    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        return value?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    /**
     * Converts List<String> to comma-separated string
     * @param list List of strings
     * @return Comma-separated string or null
     */
    @TypeConverter
    fun stringListToString(list: List<String>?): String? {
        return list?.joinToString(",")
    }
}