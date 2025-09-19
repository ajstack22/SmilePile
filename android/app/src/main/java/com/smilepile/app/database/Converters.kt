package com.smilepile.app.database

import androidx.room.TypeConverter
import java.util.Date

/**
 * Type converters for Room database
 *
 * Room can only persist primitive types and their boxed alternatives.
 * This class provides converters for common types that need to be stored in the database.
 */
class Converters {

    /**
     * Convert Date to Long for storage in database
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    /**
     * Convert Long timestamp to Date
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    /**
     * Convert List<String> to String for storage
     * Used for storing lists of strings as comma-separated values
     */
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    /**
     * Convert String to List<String>
     * Used for converting comma-separated values back to lists
     */
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
    }

    /**
     * Convert List<Long> to String for storage
     * Used for storing lists of IDs as comma-separated values
     */
    @TypeConverter
    fun fromLongList(value: List<Long>?): String? {
        return value?.joinToString(",")
    }

    /**
     * Convert String to List<Long>
     * Used for converting comma-separated values back to lists of IDs
     */
    @TypeConverter
    fun toLongList(value: String?): List<Long>? {
        return value?.split(",")?.mapNotNull {
            it.trim().toLongOrNull()
        }?.filter { it > 0 }
    }
}