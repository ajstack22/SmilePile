package com.smilepile.app.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.smilepile.app.data.database.dao.AlbumDao
import com.smilepile.app.data.database.dao.CategoryDao
import com.smilepile.app.data.database.dao.PhotoDao
import com.smilepile.app.data.database.entities.Album
import com.smilepile.app.data.database.entities.Category
import com.smilepile.app.data.database.entities.Photo

/**
 * Test database configuration for SmilePile app.
 * Used for unit testing database operations and performance.
 */
@Database(
    entities = [Photo::class, Category::class, Album::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TestSmilePileDatabase : RoomDatabase() {

    abstract fun photoDao(): PhotoDao
    abstract fun categoryDao(): CategoryDao
    abstract fun albumDao(): AlbumDao

    companion object {
        fun createInMemoryDatabase(context: Context): TestSmilePileDatabase {
            return Room.inMemoryDatabaseBuilder(
                context,
                TestSmilePileDatabase::class.java
            )
            .allowMainThreadQueries() // For testing only
            .build()
        }
    }
}