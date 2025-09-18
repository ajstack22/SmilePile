package com.smilepile.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.smilepile.database.converters.Converters
import com.smilepile.database.dao.CategoryDao
import com.smilepile.database.dao.PhotoDao
import com.smilepile.database.entities.Category
import com.smilepile.database.entities.Photo

/**
 * SmilePile Room Database
 *
 * Version 1: Initial database schema with Category and Photo entities
 * Optimized for photo gallery performance with proper indexing
 */
@Database(
    entities = [
        Category::class,
        Photo::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SmilePileDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun photoDao(): PhotoDao

    companion object {
        const val DATABASE_NAME = "smilepile_database"

        @Volatile
        private var INSTANCE: SmilePileDatabase? = null

        /**
         * Get database instance with proper configuration for performance
         */
        fun getDatabase(context: Context): SmilePileDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmilePileDatabase::class.java,
                    DATABASE_NAME
                )
                    .addTypeConverter(Converters())
                    .addCallback(DatabaseCallback())
                    .enableMultiInstanceInvalidation()
                    .setJournalMode(RoomDatabase.JournalMode.WAL) // Write-Ahead Logging for better performance
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Database callback for initialization and configuration
         */
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Optimize database for gallery performance
                db.execSQL("PRAGMA synchronous = NORMAL")
                db.execSQL("PRAGMA cache_size = 10000")
                db.execSQL("PRAGMA temp_store = MEMORY")
                db.execSQL("PRAGMA mmap_size = 268435456") // 256MB
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Ensure WAL mode is enabled
                db.execSQL("PRAGMA journal_mode = WAL")
                // Optimize for read-heavy workload (photo browsing)
                db.execSQL("PRAGMA cache_size = 10000")
            }
        }

        /**
         * Clear all data (for testing or reset functionality)
         */
        suspend fun clearDatabase(context: Context) {
            val database = getDatabase(context)
            database.clearAllTables()
        }

        /**
         * Close database instance
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }

    /**
     * Clear all tables while maintaining structure
     */
    suspend fun clearAllData() {
        clearAllTables()
    }

    /**
     * Database health check
     */
    suspend fun checkDatabaseHealth(): Boolean {
        return try {
            // Simple query to verify database is working
            categoryDao().getAllCategories()
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Database configuration constants for performance tuning
 */
object DatabaseConfig {
    // Performance thresholds
    const val TARGET_QUERY_TIME_MS = 50
    const val MAX_PHOTOS_PER_CATEGORY = 1000
    const val PAGINATION_PAGE_SIZE = 20

    // Cache sizes
    const val DATABASE_CACHE_SIZE = 10000
    const val MMAP_SIZE = 268435456 // 256MB

    // Query limits for UI performance
    const val CATEGORY_LOAD_LIMIT = 50
    const val PHOTO_PREVIEW_LIMIT = 10
}