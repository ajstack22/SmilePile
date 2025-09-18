package com.smilepile.app.data.database

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room type converters for SmilePile database.
 * Handles conversion between complex types and primitive types that Room can persist.
 */
class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.filter { it.isNotBlank() }
    }
}