package com.smilepile.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 5,
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

        // Migration from version 4 to 5: Convert CategoryEntity ID from String to Long
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new category table with Long ID
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS category_entities_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        display_name TEXT NOT NULL,
                        color_hex TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        is_default INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                """.trimIndent())

                // Copy data from old table to new table, converting String IDs to Long
                database.execSQL("""
                    INSERT INTO category_entities_new (id, display_name, color_hex, position, is_default, created_at)
                    SELECT
                        CAST(id AS INTEGER),
                        display_name,
                        color_hex,
                        position,
                        is_default,
                        created_at
                    FROM category_entities
                    WHERE id GLOB '[0-9]*'
                """.trimIndent())

                // For non-numeric IDs, insert with auto-generated IDs
                database.execSQL("""
                    INSERT INTO category_entities_new (display_name, color_hex, position, is_default, created_at)
                    SELECT
                        display_name,
                        color_hex,
                        position,
                        is_default,
                        created_at
                    FROM category_entities
                    WHERE NOT id GLOB '[0-9]*'
                """.trimIndent())

                // Drop old table
                database.execSQL("DROP TABLE category_entities")

                // Rename new table to original name
                database.execSQL("ALTER TABLE category_entities_new RENAME TO category_entities")
            }
        }

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
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration() // Fallback for other version jumps
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