package com.smilepile.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.smilepile.app.data.database.entities.Photo
import com.smilepile.app.data.database.entities.Category
import com.smilepile.app.data.database.entities.Album
import com.smilepile.app.data.database.dao.PhotoDao
import com.smilepile.app.data.database.dao.CategoryDao
import com.smilepile.app.data.database.dao.AlbumDao

/**
 * Room database for SmilePile app.
 * This is the main database configuration for the application.
 */
@Database(
    entities = [Photo::class, Category::class, Album::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SmilePileDatabase : RoomDatabase() {

    abstract fun photoDao(): PhotoDao
    abstract fun albumDao(): AlbumDao
    abstract fun categoryDao(): CategoryDao
}