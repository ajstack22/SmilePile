package com.smilepile.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import com.smilepile.app.database.entities.PhotoEntity
import com.smilepile.app.database.entities.CategoryEntity
import com.smilepile.app.database.dao.PhotoDao
import com.smilepile.app.database.dao.CategoryDao

/**
 * SmilePile Room Database
 *
 * This is the main database class for the SmilePile application.
 * It serves as the main access point for the underlying connection to the app's persisted data.
 *
 * The database is configured with:
 * - Version 2 (soft delete and cascade fix)
 * - Schema export enabled for migration support
 * - Data-preserving migration strategy (NO destructive fallback)
 * - Singleton pattern for proper resource management
 * - Database integrity verification
 */
@Database(
    entities = [
        PhotoEntity::class,
        CategoryEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SmilePileDatabase : RoomDatabase() {

    // DAO abstract methods
    abstract fun photoDao(): PhotoDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        // Singleton prevents multiple instances of database opening at the same time
        @Volatile
        private var INSTANCE: SmilePileDatabase? = null

        /**
         * Migration from version 1 to 2: Add soft delete support and fix CASCADE delete issue
         * This migration adds isDeleted and deletedAt columns, makes categoryId nullable,
         * and changes the foreign key constraint from CASCADE to SET_NULL.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d("SmilePileDatabase", "Starting migration from version 1 to 2")

                try {
                    // Step 1: Add soft delete columns to existing photos table
                    database.execSQL("ALTER TABLE photos ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE photos ADD COLUMN deletedAt INTEGER")

                    // Step 2: Create index for soft delete queries
                    database.execSQL("CREATE INDEX index_photos_isDeleted ON photos(isDeleted)")

                    // Step 3: Make categoryId nullable and change foreign key constraint
                    // We need to recreate the table to change the foreign key constraint

                    // Create new table with correct schema
                    database.execSQL("""
                        CREATE TABLE photos_new (
                            id TEXT NOT NULL PRIMARY KEY,
                            path TEXT NOT NULL,
                            name TEXT NOT NULL,
                            categoryId TEXT,
                            position INTEGER NOT NULL DEFAULT 0,
                            dateAdded INTEGER NOT NULL,
                            isFromAssets INTEGER NOT NULL DEFAULT 1,
                            isDeleted INTEGER NOT NULL DEFAULT 0,
                            deletedAt INTEGER,
                            FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL
                        )
                    """)

                    // Copy all existing data to new table
                    database.execSQL("""
                        INSERT INTO photos_new (id, path, name, categoryId, position, dateAdded, isFromAssets, isDeleted, deletedAt)
                        SELECT id, path, name, categoryId, position, dateAdded, isFromAssets, 0, null FROM photos
                    """)

                    // Drop old table
                    database.execSQL("DROP TABLE photos")

                    // Rename new table to original name
                    database.execSQL("ALTER TABLE photos_new RENAME TO photos")

                    // Recreate all indexes
                    database.execSQL("CREATE INDEX index_photos_categoryId_position ON photos(categoryId, position)")
                    database.execSQL("CREATE INDEX index_photos_categoryId ON photos(categoryId)")
                    database.execSQL("CREATE INDEX index_photos_isDeleted ON photos(isDeleted)")

                    Log.d("SmilePileDatabase", "Migration from version 1 to 2 completed successfully")
                } catch (e: Exception) {
                    Log.e("SmilePileDatabase", "Migration from version 1 to 2 failed", e)
                    throw e
                }
            }
        }

        /**
         * Gets the singleton instance of the database.
         *
         * @param context Application context
         * @param scope Coroutine scope for database operations
         * @return SmilePileDatabase instance
         */
        fun getDatabase(
            context: Context,
            scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
        ): SmilePileDatabase {
            // If the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmilePileDatabase::class.java,
                    "smilepile_database"
                )
                    // Add migration to preserve user data during schema changes
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(SmilePileDatabaseCallback(scope))
                    .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * Database callback for initialization and integrity verification
         */
        private class SmilePileDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Log.d("SmilePileDatabase", "Database created successfully, version: ${db.version}")
                // Populate database in background when it's first created
                INSTANCE?.let { database ->
                    scope.launch {
                        try {
                            populateDatabase(database)
                        } catch (e: Exception) {
                            Log.e("SmilePileDatabase", "Failed to populate database", e)
                        }
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Verify database integrity on open
                Log.d("SmilePileDatabase", "Database opened successfully, version: ${db.version}")

                // Verify critical tables exist
                try {
                    db.query("SELECT COUNT(*) FROM categories").use { cursor ->
                        if (cursor.moveToFirst()) {
                            val categoryCount = cursor.getInt(0)
                            Log.d("SmilePileDatabase", "Categories table accessible, count: $categoryCount")
                        }
                    }
                    db.query("SELECT COUNT(*) FROM photos").use { cursor ->
                        if (cursor.moveToFirst()) {
                            val photoCount = cursor.getInt(0)
                            Log.d("SmilePileDatabase", "Photos table accessible, count: $photoCount")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SmilePileDatabase", "Database integrity check failed", e)
                }
            }

            /**
             * Populate the database with initial data if needed
             */
            private suspend fun populateDatabase(database: SmilePileDatabase) {
                val categoryDao = database.categoryDao()
                val photoDao = database.photoDao()

                // Check if database is already populated
                if (categoryDao.getCategoryCount() > 0) {
                    return
                }

                // Sample image files from assets/sample_images/
                val sampleImages = listOf(
                    "sample_1.png", "sample_2.png", "sample_3.png",
                    "sample_4.png", "sample_5.png", "sample_6.png"
                )

                // Create 3 sample categories matching CategoryManager
                val categories = listOf(
                    CategoryEntity(
                        id = "animals",
                        name = "animals",
                        displayName = "Animals",
                        coverImagePath = null,
                        description = "Fun animal pictures",
                        position = 0
                    ),
                    CategoryEntity(
                        id = "family",
                        name = "family",
                        displayName = "Family",
                        coverImagePath = null,
                        description = "Happy family moments",
                        position = 1
                    ),
                    CategoryEntity(
                        id = "fun_times",
                        name = "fun_times",
                        displayName = "Fun Times",
                        coverImagePath = null,
                        description = "Good times and memories",
                        position = 2
                    )
                )

                // Insert categories
                categoryDao.insertCategories(categories)

                // Create and insert photos
                val photos = sampleImages.mapIndexed { index, imageName ->
                    val categoryId = when (index % 3) {
                        0 -> "animals"
                        1 -> "family"
                        else -> "fun_times"
                    }

                    PhotoEntity(
                        id = "photo_${index + 1}",
                        path = imageName,
                        name = imageName.substringBeforeLast('.'),
                        categoryId = categoryId,
                        position = index / 3,
                        isFromAssets = true
                    )
                }

                photoDao.insertPhotos(photos)

                // Update category photo counts
                categories.forEach { category ->
                    val photoCount = photoDao.getPhotoCountForCategory(category.id)
                    val updatedCategory = category.copy(photoCount = photoCount)
                    categoryDao.updateCategory(updatedCategory)
                }
            }
        }
    }
}