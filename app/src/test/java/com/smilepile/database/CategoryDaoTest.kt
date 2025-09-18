package com.smilepile.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smilepile.database.entities.Category
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * Unit tests for CategoryDao
 */
@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: SmilePileDatabase
    private lateinit var categoryDao: CategoryDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SmilePileDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        categoryDao = database.categoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertCategory_ReturnsId() = runTest {
        val category = Category.create("Test Category")
        val id = categoryDao.insertCategory(category)
        assertTrue(id > 0)
    }

    @Test
    fun getAllActiveCategories_ReturnsActiveCategoriesOnly() = runTest {
        // Insert active category
        val activeCategory = Category.create("Active Category")
        categoryDao.insertCategory(activeCategory)

        // Insert inactive category
        val inactiveCategory = Category.create("Inactive Category").copy(isActive = false)
        categoryDao.insertCategory(inactiveCategory)

        val activeCategories = categoryDao.getAllActiveCategories().first()
        assertEquals(1, activeCategories.size)
        assertEquals("Active Category", activeCategories[0].name)
    }

    @Test
    fun getCategoryById_ReturnsCorrectCategory() = runTest {
        val category = Category.create("Test Category")
        val id = categoryDao.insertCategory(category)

        val retrieved = categoryDao.getCategoryById(id)
        assertNotNull(retrieved)
        assertEquals("Test Category", retrieved?.name)
    }

    @Test
    fun updateCategory_UpdatesSuccessfully() = runTest {
        val category = Category.create("Original Name")
        val id = categoryDao.insertCategory(category)

        val updated = category.copy(id = id, name = "Updated Name")
        categoryDao.updateCategory(updated)

        val retrieved = categoryDao.getCategoryById(id)
        assertEquals("Updated Name", retrieved?.name)
    }

    @Test
    fun softDeleteCategory_SetsInactive() = runTest {
        val category = Category.create("Test Category")
        val id = categoryDao.insertCategory(category)

        categoryDao.softDeleteCategory(id)

        val retrieved = categoryDao.getCategoryById(id)
        assertFalse(retrieved?.isActive ?: true)
    }

    @Test
    fun getNextDisplayOrder_ReturnsCorrectValue() = runTest {
        // Initially should return 1 for empty table
        assertEquals(1, categoryDao.getNextDisplayOrder())

        // Insert category with display order 5
        val category = Category.create("Test", displayOrder = 5)
        categoryDao.insertCategory(category)

        // Should return 6
        assertEquals(6, categoryDao.getNextDisplayOrder())
    }

    @Test
    fun isCategoryNameTaken_ChecksCorrectly() = runTest {
        val category = Category.create("Unique Name")
        categoryDao.insertCategory(category)

        // Should be taken
        assertTrue(categoryDao.isCategoryNameTaken("Unique Name") > 0)

        // Should not be taken
        assertEquals(0, categoryDao.isCategoryNameTaken("Different Name"))
    }

    @Test
    fun reorderCategories_UpdatesDisplayOrders() = runTest {
        val category1 = Category.create("Category 1", displayOrder = 1)
        val category2 = Category.create("Category 2", displayOrder = 2)

        val id1 = categoryDao.insertCategory(category1)
        val id2 = categoryDao.insertCategory(category2)

        // Reorder categories
        categoryDao.reorderCategories(listOf(
            id1 to 2,
            id2 to 1
        ))

        val retrieved1 = categoryDao.getCategoryById(id1)
        val retrieved2 = categoryDao.getCategoryById(id2)

        assertEquals(2, retrieved1?.displayOrder)
        assertEquals(1, retrieved2?.displayOrder)
    }
}