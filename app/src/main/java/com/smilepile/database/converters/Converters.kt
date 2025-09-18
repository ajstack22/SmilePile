package com.smilepile.database.converters

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room type converters for custom data types
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
}