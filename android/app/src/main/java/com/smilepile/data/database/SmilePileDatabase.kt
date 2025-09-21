package com.smilepile.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.smilepile.data.dao.CategoryDao
import com.smilepile.data.dao.PhotoDao
import com.smilepile.data.entities.CategoryEntity
import com.smilepile.data.entities.PhotoEntity

/**
 * Main Room database for SmilePile application
 * Contains all entities and provides access to DAOs
 */
@Database(
    entities = [
        PhotoEntity::class,
        CategoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SmilePileDatabase : RoomDatabase() {

    /**
     * Provides access to PhotoDao for photo-related database operations
     */
    abstract fun photoDao(): PhotoDao

    /**
     * Provides access to CategoryDao for category-related database operations
     */
    abstract fun categoryDao(): CategoryDao

    companion object {
        // Singleton prevents multiple instances of database opening at the same time
        @Volatile
        private var INSTANCE: SmilePileDatabase? = null

        /**
         * Gets the singleton instance of the database
         * @param context Application context
         * @return SmilePileDatabase instance
         */
        fun getDatabase(context: Context): SmilePileDatabase {
            // If the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmilePileDatabase::class.java,
                    "smilepile_database"
                )
                    .fallbackToDestructiveMigration() // For development - remove in production
                    .build()
                INSTANCE = instance
                // Return instance
                instance
            }
        }

        /**
         * Destroys the database instance (useful for testing)
         */
        fun destroyInstance() {
            INSTANCE = null
        }
    }
}