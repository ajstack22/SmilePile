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
import com.smilepile.data.dao.PhotoCategoryDao
import com.smilepile.data.entities.CategoryEntity
import com.smilepile.data.entities.PhotoEntity
import com.smilepile.data.entities.PhotoCategoryJoin

/**
 * Main Room database for SmilePile application
 * Contains all entities and provides access to DAOs
 */
@Database(
    entities = [
        PhotoEntity::class,
        CategoryEntity::class,
        PhotoCategoryJoin::class
    ],
    version = 6,
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

    /**
     * Provides access to PhotoCategoryDao for photo-category relationship operations
     */
    abstract fun photoCategoryDao(): PhotoCategoryDao

    companion object {
        // Singleton prevents multiple instances of database opening at the same time
        @Volatile
        private var INSTANCE: SmilePileDatabase? = null

        // Migration from version 5 to 6: Add PhotoCategoryJoin table and icon_name column
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create photo_category_join table for many-to-many relationships
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS photo_category_join (
                        photo_id TEXT NOT NULL,
                        category_id INTEGER NOT NULL,
                        assigned_at INTEGER NOT NULL,
                        is_primary INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(photo_id, category_id),
                        FOREIGN KEY(photo_id) REFERENCES photo_entities(id) ON DELETE CASCADE,
                        FOREIGN KEY(category_id) REFERENCES category_entities(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Create indices for better query performance
                database.execSQL("CREATE INDEX IF NOT EXISTS index_photo_category_join_photo_id ON photo_category_join(photo_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_photo_category_join_category_id ON photo_category_join(category_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_photo_category_join_assigned_at ON photo_category_join(assigned_at)")

                // Add icon_name column to category_entities
                database.execSQL("ALTER TABLE category_entities ADD COLUMN icon_name TEXT DEFAULT NULL")

                // Migrate existing photo-category relationships from PhotoEntity.categoryId
                // to the new join table
                database.execSQL("""
                    INSERT INTO photo_category_join (photo_id, category_id, assigned_at, is_primary)
                    SELECT id, category_id, timestamp, 1
                    FROM photo_entities
                    WHERE category_id IS NOT NULL AND category_id > 0
                """.trimIndent())
            }
        }

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
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
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