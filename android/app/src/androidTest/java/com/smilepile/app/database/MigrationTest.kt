package com.smilepile.app.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Migration tests for SmilePile Database
 *
 * This test class ensures that database migrations preserve data integrity
 * and properly transform the schema between versions.
 *
 * CRITICAL: These tests prevent data loss during app updates.
 * All migrations must be tested before release.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SmilePileDatabase::class.java
    )

    /**
     * Test that version 1 database creates correctly with all required tables
     */
    @Test
    fun testDatabaseCreation_Version1() {
        // Create version 1 database
        val db = helper.createDatabase("test-db", 1)

        // Verify that all required tables exist
        try {
            // Test categories table structure
            val categoriesCursor = db.query("SELECT * FROM categories")
            val categoryColumns = categoriesCursor.columnNames.toList()
            categoriesCursor.close()

            // Verify categories table has expected columns
            assertTrue("Categories table should have 'id' column", categoryColumns.contains("id"))
            assertTrue("Categories table should have 'name' column", categoryColumns.contains("name"))
            assertTrue("Categories table should have 'displayName' column", categoryColumns.contains("displayName"))
            assertTrue("Categories table should have 'coverImagePath' column", categoryColumns.contains("coverImagePath"))
            assertTrue("Categories table should have 'description' column", categoryColumns.contains("description"))
            assertTrue("Categories table should have 'position' column", categoryColumns.contains("position"))
            assertTrue("Categories table should have 'photoCount' column", categoryColumns.contains("photoCount"))

            // Test photos table structure
            val photosCursor = db.query("SELECT * FROM photos")
            val photoColumns = photosCursor.columnNames.toList()
            photosCursor.close()

            // Verify photos table has expected columns
            assertTrue("Photos table should have 'id' column", photoColumns.contains("id"))
            assertTrue("Photos table should have 'path' column", photoColumns.contains("path"))
            assertTrue("Photos table should have 'name' column", photoColumns.contains("name"))
            assertTrue("Photos table should have 'categoryId' column", photoColumns.contains("categoryId"))
            assertTrue("Photos table should have 'position' column", photoColumns.contains("position"))
            assertTrue("Photos table should have 'isFromAssets' column", photoColumns.contains("isFromAssets"))

            // Test that we can insert sample data
            db.execSQL("""
                INSERT INTO categories (id, name, displayName, coverImagePath, description, position, photoCount)
                VALUES ('test_cat', 'test', 'Test Category', null, 'Test description', 0, 0)
            """)

            db.execSQL("""
                INSERT INTO photos (id, path, name, categoryId, position, isFromAssets)
                VALUES ('test_photo', 'test.jpg', 'Test Photo', 'test_cat', 0, 1)
            """)

            // Verify data was inserted correctly
            val verifyCategoryCursor = db.query("SELECT COUNT(*) FROM categories WHERE id = 'test_cat'")
            verifyCategoryCursor.moveToFirst()
            assertEquals("Should have inserted 1 category", 1, verifyCategoryCursor.getInt(0))
            verifyCategoryCursor.close()

            val verifyPhotoCursor = db.query("SELECT COUNT(*) FROM photos WHERE id = 'test_photo'")
            verifyPhotoCursor.moveToFirst()
            assertEquals("Should have inserted 1 photo", 1, verifyPhotoCursor.getInt(0))
            verifyPhotoCursor.close()

        } finally {
            db.close()
        }
    }

    /**
     * Test that the database can be opened without destructive migration
     */
    @Test
    fun testDatabaseOpenWithoutDestruction() {
        // Create database with sample data
        val db = helper.createDatabase("test-persistence", 1)

        // Insert test data
        db.execSQL("""
            INSERT INTO categories (id, name, displayName, coverImagePath, description, position, photoCount)
            VALUES ('preserve_test', 'preserve', 'Preserve Test', null, 'Data preservation test', 0, 1)
        """)

        db.execSQL("""
            INSERT INTO photos (id, path, name, categoryId, position, isFromAssets)
            VALUES ('preserve_photo', 'preserve.jpg', 'Preserve Photo', 'preserve_test', 0, 0)
        """)

        db.close()

        // Reopen the database (this would trigger destructive migration if enabled)
        val reopenedDb = helper.runMigrationsAndValidate("test-persistence", 1, false)

        // Verify our test data still exists
        val categoryCursor = reopenedDb.query("SELECT COUNT(*) FROM categories WHERE id = 'preserve_test'")
        categoryCursor.moveToFirst()
        assertEquals("Category data should be preserved", 1, categoryCursor.getInt(0))
        categoryCursor.close()

        val photoCursor = reopenedDb.query("SELECT COUNT(*) FROM photos WHERE id = 'preserve_photo'")
        photoCursor.moveToFirst()
        assertEquals("Photo data should be preserved", 1, photoCursor.getInt(0))
        photoCursor.close()

        reopenedDb.close()
    }

    /**
     * Test migration from version 1 to version 2
     * This migration adds soft delete support and fixes CASCADE delete issues
     */
    @Test
    fun migrate1To2() {
        // Create version 1 database with test data
        val db1 = helper.createDatabase("migration-test", 1)

        // Insert test data in version 1 format
        db1.execSQL("""
            INSERT INTO categories (id, name, displayName, coverImagePath, description, position, photoCount)
            VALUES ('migration_test', 'test', 'Migration Test', null, 'Testing migration', 0, 1)
        """)

        db1.execSQL("""
            INSERT INTO photos (id, path, name, categoryId, position, dateAdded, isFromAssets)
            VALUES ('migration_photo', 'test.jpg', 'Migration Photo', 'migration_test', 0, 1234567890, 1)
        """)

        db1.close()

        // Get the actual migration from the database class
        val migration1To2 = SmilePileDatabase.MIGRATION_1_2

        // Migrate to version 2
        val db2 = helper.runMigrationsAndValidate("migration-test", 2, true, migration1To2)

        // Verify data was preserved
        val categoryCursor = db2.query("SELECT * FROM categories WHERE id = ?", arrayOf("migration_test"))
        assertTrue("Category data should be preserved after migration", categoryCursor.moveToFirst())
        assertEquals("Category ID should be preserved", "migration_test", categoryCursor.getString(categoryCursor.getColumnIndex("id")))
        categoryCursor.close()

        // Verify photo data was preserved
        val photoCursor = db2.query("SELECT * FROM photos WHERE id = ?", arrayOf("migration_photo"))
        assertTrue("Photo data should be preserved after migration", photoCursor.moveToFirst())
        assertEquals("Photo ID should be preserved", "migration_photo", photoCursor.getString(photoCursor.getColumnIndex("id")))
        assertEquals("Category ID should be preserved", "migration_test", photoCursor.getString(photoCursor.getColumnIndex("categoryId")))

        // Verify new soft delete columns exist with default values
        assertEquals("isDeleted should default to 0", 0, photoCursor.getInt(photoCursor.getColumnIndex("isDeleted")))
        assertTrue("deletedAt should be null by default", photoCursor.isNull(photoCursor.getColumnIndex("deletedAt")))

        photoCursor.close()

        // Verify new indexes were created
        val indexCursor = db2.query("SELECT name FROM sqlite_master WHERE type='index' AND name='index_photos_isDeleted'")
        assertTrue("isDeleted index should be created", indexCursor.moveToFirst())
        indexCursor.close()

        // Test that categoryId can now be null (SET_NULL foreign key behavior)
        db2.execSQL("""
            INSERT INTO photos (id, path, name, categoryId, position, dateAdded, isFromAssets, isDeleted, deletedAt)
            VALUES ('orphan_photo', 'orphan.jpg', 'Orphan Photo', null, 0, 1234567890, 1, 0, null)
        """)

        val orphanCursor = db2.query("SELECT * FROM photos WHERE id = ?", arrayOf("orphan_photo"))
        assertTrue("Should be able to insert photo with null categoryId", orphanCursor.moveToFirst())
        assertTrue("categoryId should be null", orphanCursor.isNull(orphanCursor.getColumnIndex("categoryId")))
        orphanCursor.close()

        // Test soft delete functionality
        db2.execSQL("UPDATE photos SET isDeleted = 1, deletedAt = 9876543210 WHERE id = 'migration_photo'")

        val softDeletedCursor = db2.query("SELECT * FROM photos WHERE id = ? AND isDeleted = 1", arrayOf("migration_photo"))
        assertTrue("Photo should be soft deleted", softDeletedCursor.moveToFirst())
        assertEquals("deletedAt should be set", 9876543210L, softDeletedCursor.getLong(softDeletedCursor.getColumnIndex("deletedAt")))
        softDeletedCursor.close()

        db2.close()
    }

    /**
     * Test that deleting a category now sets photos to null instead of cascade deleting them
     */
    @Test
    fun testCascadeDeleteFix() {
        // Create version 2 database directly
        val db = helper.createDatabase("cascade-test", 2)

        // Insert test category and photo
        db.execSQL("""
            INSERT INTO categories (id, name, displayName, coverImagePath, description, position, photoCount)
            VALUES ('delete_test_cat', 'delete_test', 'Delete Test', null, 'Testing cascade fix', 0, 1)
        """)

        db.execSQL("""
            INSERT INTO photos (id, path, name, categoryId, position, dateAdded, isFromAssets, isDeleted, deletedAt)
            VALUES ('cascade_test_photo', 'cascade.jpg', 'Cascade Photo', 'delete_test_cat', 0, 1234567890, 1, 0, null)
        """)

        // Verify photo exists with category
        var photoCursor = db.query("SELECT * FROM photos WHERE id = 'cascade_test_photo'")
        assertTrue("Photo should exist before category deletion", photoCursor.moveToFirst())
        assertEquals("Photo should have categoryId", "delete_test_cat", photoCursor.getString(photoCursor.getColumnIndex("categoryId")))
        photoCursor.close()

        // Delete the category - this should set photo's categoryId to null, not delete the photo
        db.execSQL("DELETE FROM categories WHERE id = 'delete_test_cat'")

        // Verify photo still exists but with null categoryId
        photoCursor = db.query("SELECT * FROM photos WHERE id = 'cascade_test_photo'")
        assertTrue("Photo should still exist after category deletion", photoCursor.moveToFirst())
        assertTrue("Photo's categoryId should be null after category deletion", photoCursor.isNull(photoCursor.getColumnIndex("categoryId")))
        assertEquals("Photo should not be soft deleted", 0, photoCursor.getInt(photoCursor.getColumnIndex("isDeleted")))
        photoCursor.close()

        db.close()
    }

    /**
     * Test database integrity constraints
     */
    @Test
    fun testDatabaseConstraints() {
        val db = helper.createDatabase("constraints-test", 1)

        try {
            // Test foreign key constraint (photos must reference valid category)
            db.execSQL("""
                INSERT INTO categories (id, name, displayName, coverImagePath, description, position, photoCount)
                VALUES ('valid_category', 'valid', 'Valid Category', null, 'Valid category for FK test', 0, 0)
            """)

            // This should work - valid foreign key
            db.execSQL("""
                INSERT INTO photos (id, path, name, categoryId, position, isFromAssets)
                VALUES ('valid_photo', 'valid.jpg', 'Valid Photo', 'valid_category', 0, 0)
            """)

            // Verify the photo was inserted
            val cursor = db.query("SELECT COUNT(*) FROM photos WHERE categoryId = 'valid_category'")
            cursor.moveToFirst()
            assertEquals("Photo with valid category should be inserted", 1, cursor.getInt(0))
            cursor.close()

        } finally {
            db.close()
        }
    }

    /**
     * Test that database handles edge cases properly
     */
    @Test
    fun testDatabaseEdgeCases() {
        val db = helper.createDatabase("edge-cases-test", 1)

        try {
            // Test null values are handled correctly
            db.execSQL("""
                INSERT INTO categories (id, name, displayName, coverImagePath, description, position, photoCount)
                VALUES ('null_test', 'null_test', 'Null Test', null, null, 0, 0)
            """)

            // Test empty strings
            db.execSQL("""
                INSERT INTO categories (id, name, displayName, coverImagePath, description, position, photoCount)
                VALUES ('empty_test', '', 'Empty Test', '', '', 1, 0)
            """)

            // Verify data was inserted
            val cursor = db.query("SELECT COUNT(*) FROM categories WHERE id IN ('null_test', 'empty_test')")
            cursor.moveToFirst()
            assertEquals("Should handle null and empty values", 2, cursor.getInt(0))
            cursor.close()

        } finally {
            db.close()
        }
    }
}