package com.smilepile.data.repository

import com.smilepile.data.dao.CategoryDao
import com.smilepile.data.dao.PhotoCategoryDao
import com.smilepile.data.entities.CategoryEntity
import com.smilepile.data.entities.PhotoCategoryJoin
import com.smilepile.data.models.Category
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

/**
 * Unit tests for CategoryRepositoryImpl
 * Tests CRUD operations, data flows, error scenarios, and photo-category associations
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class CategoryRepositoryImplTest {

    private lateinit var categoryDao: CategoryDao
    private lateinit var photoCategoryDao: PhotoCategoryDao
    private lateinit var repository: CategoryRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        ShadowLog.stream = System.out
        Dispatchers.setMain(testDispatcher)
        categoryDao = mockk(relaxed = true)
        photoCategoryDao = mockk(relaxed = true)
        repository = CategoryRepositoryImpl(categoryDao, photoCategoryDao, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // Helper function to create test Category
    private fun createTestCategory(
        id: Long = 0L,
        name: String = "test_category",
        displayName: String = "Test Category",
        position: Int = 0,
        colorHex: String? = "#4CAF50",
        isDefault: Boolean = false,
        createdAt: Long = System.currentTimeMillis()
    ): Category {
        return Category(
            id = id,
            name = name,
            displayName = displayName,
            position = position,
            iconResource = "ic_category",
            colorHex = colorHex,
            isDefault = isDefault,
            createdAt = createdAt
        )
    }

    // Helper function to create test CategoryEntity
    private fun createTestCategoryEntity(
        id: Long = 1L,
        displayName: String = "Test Category",
        colorHex: String = "#4CAF50",
        position: Int = 0,
        isDefault: Boolean = false,
        createdAt: Long = System.currentTimeMillis()
    ): CategoryEntity {
        return CategoryEntity(
            id = id,
            displayName = displayName,
            colorHex = colorHex,
            position = position,
            isDefault = isDefault,
            createdAt = createdAt
        )
    }

    // ===== Insert Operations Tests =====

    @Test
    fun `insertCategory success`() = runTest {
        // Given
        val category = createTestCategory()
        coEvery { categoryDao.insert(any()) } returns 1L

        // When
        val result = repository.insertCategory(category)

        // Then
        assertEquals(1L, result)
        coVerify { categoryDao.insert(any()) }
    }

    @Test
    fun `insertCategory handles database exception`() = runTest {
        // Given
        val category = createTestCategory()
        coEvery { categoryDao.insert(any()) } throws RuntimeException("Database error")

        // When & Then
        try {
            repository.insertCategory(category)
            fail("Expected CategoryRepositoryException")
        } catch (e: CategoryRepositoryException) {
            assertTrue(e.message?.contains("Failed to insert category") == true)
        }
    }

    @Test
    fun `insertCategories success`() = runTest {
        // Given
        val categories = listOf(
            createTestCategory(name = "category1", displayName = "Category 1"),
            createTestCategory(name = "category2", displayName = "Category 2")
        )
        coEvery { categoryDao.insertAll(any()) } returns listOf(1L, 2L)

        // When
        repository.insertCategories(categories)

        // Then
        coVerify { categoryDao.insertAll(any()) }
    }

    @Test
    fun `insertCategories handles empty list`() = runTest {
        // Given
        val categories = emptyList<Category>()
        coEvery { categoryDao.insertAll(any()) } returns emptyList()

        // When
        repository.insertCategories(categories)

        // Then
        coVerify { categoryDao.insertAll(emptyList()) }
    }

    // ===== Update Operations Tests =====

    @Test
    fun `updateCategory success`() = runTest {
        // Given
        val category = createTestCategory(id = 1L)
        coEvery { categoryDao.update(any()) } returns 1

        // When
        repository.updateCategory(category)

        // Then
        coVerify { categoryDao.update(any()) }
    }

    @Test
    fun `updateCategory throws exception when category not found`() = runTest {
        // Given
        val category = createTestCategory(id = 999L)
        coEvery { categoryDao.update(any()) } returns 0

        // When & Then
        try {
            repository.updateCategory(category)
            fail("Expected CategoryRepositoryException")
        } catch (e: CategoryRepositoryException) {
            assertTrue(e.message?.contains("not found for update") == true)
        }
    }

    // ===== Delete Operations Tests =====

    @Test
    fun `deleteCategory success`() = runTest {
        // Given
        val category = createTestCategory(id = 1L)
        coEvery { categoryDao.delete(any()) } returns 1

        // When
        repository.deleteCategory(category)

        // Then
        coVerify { categoryDao.delete(any()) }
    }

    @Test
    fun `deleteCategory throws exception when category not found`() = runTest {
        // Given
        val category = createTestCategory(id = 999L)
        coEvery { categoryDao.delete(any()) } returns 0

        // When & Then
        try {
            repository.deleteCategory(category)
            fail("Expected CategoryRepositoryException")
        } catch (e: CategoryRepositoryException) {
            assertTrue(e.message?.contains("not found for deletion") == true)
        }
    }

    // ===== Query Operations Tests =====

    @Test
    fun `getCategoryById returns category when found`() = runTest {
        // Given
        val categoryId = 1L
        val categoryEntity = createTestCategoryEntity(id = categoryId)
        coEvery { categoryDao.getById(categoryId) } returns categoryEntity

        // When
        val result = repository.getCategoryById(categoryId)

        // Then
        assertNotNull(result)
        assertEquals(categoryId, result?.id)
        assertEquals("Test Category", result?.displayName)
    }

    @Test
    fun `getCategoryById returns null when not found`() = runTest {
        // Given
        val categoryId = 999L
        coEvery { categoryDao.getById(categoryId) } returns null

        // When
        val result = repository.getCategoryById(categoryId)

        // Then
        assertNull(result)
    }

    @Test
    fun `getAllCategories returns all categories`() = runTest {
        // Given
        val categoryEntities = listOf(
            createTestCategoryEntity(id = 1L, displayName = "Category 1"),
            createTestCategoryEntity(id = 2L, displayName = "Category 2"),
            createTestCategoryEntity(id = 3L, displayName = "Category 3")
        )
        coEvery { categoryDao.getAll() } returns flowOf(categoryEntities)

        // When
        val result = repository.getAllCategories()

        // Then
        assertEquals(3, result.size)
        assertEquals("Category 1", result[0].displayName)
    }

    @Test
    fun `getCategoryByName returns category when found`() = runTest {
        // Given
        val name = "test_category"
        val displayName = "Test Category"
        val categoryEntity = createTestCategoryEntity(displayName = displayName)
        coEvery { categoryDao.getByDisplayName(displayName) } returns categoryEntity

        // When
        val result = repository.getCategoryByName(name)

        // Then
        assertNotNull(result)
        assertEquals("test_category", result?.name)
        assertEquals(displayName, result?.displayName)
    }

    @Test
    fun `getCategoryByName handles underscore to space conversion`() = runTest {
        // Given
        val name = "my_special_category"
        val expectedDisplayName = "My Special Category"
        val categoryEntity = createTestCategoryEntity(displayName = expectedDisplayName)
        coEvery { categoryDao.getByDisplayName(expectedDisplayName) } returns categoryEntity

        // When
        val result = repository.getCategoryByName(name)

        // Then
        assertNotNull(result)
        assertEquals("my_special_category", result?.name)
        assertEquals(expectedDisplayName, result?.displayName)
    }

    // ===== Flow Operations Tests =====

    @Test
    fun `getAllCategoriesFlow emits all categories`() = runTest {
        // Given
        val categoryEntities = listOf(
            createTestCategoryEntity(id = 1L),
            createTestCategoryEntity(id = 2L)
        )
        coEvery { categoryDao.getAll() } returns flowOf(categoryEntities)

        // When
        val result = repository.getAllCategoriesFlow().first()

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getCategoriesForPhotoFlow emits categories correctly`() = runTest {
        // Given
        val photoId = "photo123"
        val categoryEntities = listOf(
            createTestCategoryEntity(id = 1L, displayName = "Category 1"),
            createTestCategoryEntity(id = 2L, displayName = "Category 2")
        )
        coEvery { photoCategoryDao.getCategoriesForPhotoFlow(photoId) } returns flowOf(categoryEntities)

        // When
        val result = repository.getCategoriesForPhotoFlow(photoId).first()

        // Then
        assertEquals(2, result.size)
        assertEquals("Category 1", result[0].displayName)
    }

    // ===== Default Categories Tests =====

    @Test
    fun `initializeDefaultCategories creates categories when none exist`() = runTest {
        // Given
        coEvery { categoryDao.getCount() } returns 0
        coEvery { categoryDao.insertAll(any()) } returns listOf(1L, 2L, 3L)

        // When
        repository.initializeDefaultCategories()

        // Then
        coVerify { categoryDao.getCount() }
        coVerify { categoryDao.insertAll(any()) }
    }

    @Test
    fun `initializeDefaultCategories skips when categories exist`() = runTest {
        // Given
        coEvery { categoryDao.getCount() } returns 5

        // When
        repository.initializeDefaultCategories()

        // Then
        coVerify { categoryDao.getCount() }
        coVerify(exactly = 0) { categoryDao.insertAll(any()) }
    }

    // ===== Count Operations Tests =====

    @Test
    fun `getCategoryCount returns correct count`() = runTest {
        // Given
        coEvery { categoryDao.getCount() } returns 7

        // When
        val result = repository.getCategoryCount()

        // Then
        assertEquals(7, result)
    }

    // ===== Photo-Category Association Tests =====

    @Test
    fun `assignPhotosToCategory creates joins successfully`() = runTest {
        // Given
        val photoIds = listOf("photo1", "photo2", "photo3")
        val categoryId = 1L
        coEvery { photoCategoryDao.insertPhotoCategoryJoins(any()) } just Runs

        // When
        repository.assignPhotosToCategory(photoIds, categoryId)

        // Then
        coVerify {
            photoCategoryDao.insertPhotoCategoryJoins(match {
                it.size == 3 &&
                it.all { join -> join.categoryId == categoryId } &&
                it.map { join -> join.photoId } == photoIds
            })
        }
    }

    @Test
    fun `removePhotosFromCategory removes joins successfully`() = runTest {
        // Given
        val photoIds = listOf("photo1", "photo2")
        val categoryId = 1L
        coEvery { photoCategoryDao.removePhotoFromCategory(any(), any()) } just Runs

        // When
        repository.removePhotosFromCategory(photoIds, categoryId)

        // Then
        coVerify(exactly = 2) { photoCategoryDao.removePhotoFromCategory(any(), categoryId) }
    }

    @Test
    fun `assignPhotoToCategories updates photo categories`() = runTest {
        // Given
        val photoId = "photo1"
        val categoryIds = listOf(1L, 2L, 3L)
        coEvery { photoCategoryDao.updatePhotoCategories(photoId, categoryIds) } just Runs

        // When
        repository.assignPhotoToCategories(photoId, categoryIds)

        // Then
        coVerify { photoCategoryDao.updatePhotoCategories(photoId, categoryIds) }
    }

    @Test
    fun `getCategoriesForPhoto returns categories`() = runTest {
        // Given
        val photoId = "photo1"
        val categoryEntities = listOf(
            createTestCategoryEntity(id = 1L),
            createTestCategoryEntity(id = 2L)
        )
        coEvery { photoCategoryDao.getCategoriesForPhoto(photoId) } returns categoryEntities

        // When
        val result = repository.getCategoriesForPhoto(photoId)

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getPhotoCountInCategory returns correct count`() = runTest {
        // Given
        val categoryId = 1L
        coEvery { photoCategoryDao.getPhotoCountInCategory(categoryId) } returns 15

        // When
        val result = repository.getPhotoCountInCategory(categoryId)

        // Then
        assertEquals(15, result)
    }

    // ===== Data Transformation Tests =====

    @Test
    fun `category to entity conversion preserves data`() = runTest {
        // Given
        val category = createTestCategory(
            id = 0L,
            displayName = "My Category",
            colorHex = "#FF5722",
            position = 3,
            isDefault = true,
            createdAt = 1000L
        )
        coEvery { categoryDao.insert(any()) } answers {
            val entity = firstArg<CategoryEntity>()
            assertEquals("My Category", entity.displayName)
            assertEquals("#FF5722", entity.colorHex)
            assertEquals(3, entity.position)
            assertTrue(entity.isDefault)
            assertEquals(1000L, entity.createdAt)
            1L
        }

        // When
        repository.insertCategory(category)

        // Then
        coVerify { categoryDao.insert(any()) }
    }

    @Test
    fun `category to entity uses provided color`() = runTest {
        // Given
        val category = createTestCategory(colorHex = "#FF5722")
        coEvery { categoryDao.insert(any()) } answers {
            val entity = firstArg<CategoryEntity>()
            assertEquals("#FF5722", entity.colorHex)
            1L
        }

        // When
        repository.insertCategory(category)

        // Then
        coVerify { categoryDao.insert(any()) }
    }

    @Test
    fun `entity to category conversion generates normalized name`() = runTest {
        // Given
        val categoryEntity = createTestCategoryEntity(
            id = 1L,
            displayName = "My Special Category"
        )
        coEvery { categoryDao.getById(1L) } returns categoryEntity

        // When
        val result = repository.getCategoryById(1L)

        // Then
        assertNotNull(result)
        assertEquals("my_special_category", result?.name)
        assertEquals("My Special Category", result?.displayName)
    }

    // ===== Error Handling Tests =====

    @Test
    fun `handles database connection error gracefully`() = runTest {
        // Given
        coEvery { categoryDao.getAll() } throws RuntimeException("Database connection lost")

        // When & Then
        try {
            repository.getAllCategories()
            fail("Expected CategoryRepositoryException")
        } catch (e: CategoryRepositoryException) {
            assertTrue(e.message?.contains("Failed to get all categories") == true)
        }
    }

    @Test
    fun `assignPhotosToCategory handles error`() = runTest {
        // Given
        val photoIds = listOf("photo1")
        val categoryId = 1L
        coEvery { photoCategoryDao.insertPhotoCategoryJoins(any()) } throws RuntimeException("Constraint violation")

        // When & Then
        try {
            repository.assignPhotosToCategory(photoIds, categoryId)
            fail("Expected CategoryRepositoryException")
        } catch (e: CategoryRepositoryException) {
            assertTrue(e.message?.contains("Failed to assign photos to category") == true)
        }
    }

    @Test
    fun `removePhotosFromCategory handles error`() = runTest {
        // Given
        val photoIds = listOf("photo1")
        val categoryId = 1L
        coEvery { photoCategoryDao.removePhotoFromCategory(any(), any()) } throws RuntimeException("Database error")

        // When & Then
        try {
            repository.removePhotosFromCategory(photoIds, categoryId)
            fail("Expected CategoryRepositoryException")
        } catch (e: CategoryRepositoryException) {
            assertTrue(e.message?.contains("Failed to remove photos from category") == true)
        }
    }

    @Test
    fun `getCategoryByName returns null for non-existent category`() = runTest {
        // Given
        val name = "non_existent"
        val displayName = "Non Existent"
        coEvery { categoryDao.getByDisplayName(displayName) } returns null

        // When
        val result = repository.getCategoryByName(name)

        // Then
        assertNull(result)
    }

    @Test
    fun `assignPhotosToCategory handles empty photo list`() = runTest {
        // Given
        val photoIds = emptyList<String>()
        val categoryId = 1L
        coEvery { photoCategoryDao.insertPhotoCategoryJoins(any()) } just Runs

        // When
        repository.assignPhotosToCategory(photoIds, categoryId)

        // Then
        coVerify {
            photoCategoryDao.insertPhotoCategoryJoins(match { it.isEmpty() })
        }
    }

    @Test
    fun `assignPhotoToCategories handles empty category list`() = runTest {
        // Given
        val photoId = "photo1"
        val categoryIds = emptyList<Long>()
        coEvery { photoCategoryDao.updatePhotoCategories(photoId, categoryIds) } just Runs

        // When
        repository.assignPhotoToCategories(photoId, categoryIds)

        // Then
        coVerify { photoCategoryDao.updatePhotoCategories(photoId, emptyList()) }
    }
}