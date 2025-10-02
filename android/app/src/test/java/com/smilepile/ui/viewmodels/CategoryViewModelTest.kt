package com.smilepile.ui.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.smilepile.data.models.Category
import com.smilepile.data.models.Photo
import com.smilepile.data.repository.CategoryRepository
import com.smilepile.data.repository.PhotoRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for CategoryViewModel
 */
@ExperimentalCoroutinesApi
class CategoryViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var photoRepository: PhotoRepository
    private lateinit var viewModel: CategoryViewModel

    private val testDispatcher = StandardTestDispatcher()

    // Test data
    private val testCategory1 = Category(
        id = 1L,
        name = "category1",
        displayName = "Category 1",
        position = 0,
        colorHex = "#FF0000",
        isDefault = true
    )

    private val testCategory2 = Category(
        id = 2L,
        name = "category2",
        displayName = "Category 2",
        position = 1,
        colorHex = "#00FF00",
        isDefault = false
    )

    private val testPhoto = Photo(
        id = 1L,
        name = "photo1.jpg",
        path = "/test/photo1.jpg",
        categoryId = 1L,
        createdAt = System.currentTimeMillis(),
        width = 1920,
        height = 1080,
        fileSize = 1024L,
        isFromAssets = false
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Initialize mocks
        categoryRepository = mockk(relaxed = true)
        photoRepository = mockk(relaxed = true)

        // Setup default mock responses
        every { categoryRepository.getAllCategoriesFlow() } returns flowOf(listOf(testCategory1, testCategory2))
        coEvery { categoryRepository.getAllCategories() } returns listOf(testCategory1, testCategory2)
        coEvery { photoRepository.getPhotoCategoryCount(1L) } returns 5
        coEvery { photoRepository.getPhotoCategoryCount(2L) } returns 3
        coEvery { categoryRepository.getCategoryCount() } returns 2
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial state is correct`() = runTest {
        // Given & When
        viewModel = CategoryViewModel(categoryRepository, photoRepository)
        testScheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.error.value)
        assertFalse(viewModel.showAddDialog.value)
        assertNull(viewModel.editingCategory.value)
    }

    @Test
    fun `loads categories on initialization`() = runTest {
        // Given & When
        viewModel = CategoryViewModel(categoryRepository, photoRepository)
        testScheduler.advanceUntilIdle()

        // Then
        val categories = viewModel.categories.value
        assertEquals(2, categories.size)
        assertEquals(testCategory1, categories[0])
        assertEquals(testCategory2, categories[1])
    }

    @Test
    fun `refreshes categories with counts`() = runTest {
        // Given
        viewModel = CategoryViewModel(categoryRepository, photoRepository)

        // When
        viewModel.refreshCategoriesWithCounts()
        testScheduler.advanceUntilIdle()

        // Then
        val categoriesWithCounts = viewModel.categoriesWithCountsFlow.value
        assertEquals(2, categoriesWithCounts.size)
        assertEquals(5, categoriesWithCounts[0].photoCount)
        assertEquals(3, categoriesWithCounts[1].photoCount)
    }

    @Test
    fun `shows add category dialog`() = runTest {
        // Given
        viewModel = CategoryViewModel(categoryRepository, photoRepository)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.showAddCategoryDialog()
        testScheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.showAddDialog.value)
        assertNull(viewModel.editingCategory.value)
    }

    @Test
    fun `shows edit category dialog`() = runTest {
        // Given
        viewModel = CategoryViewModel(categoryRepository, photoRepository)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.showEditCategoryDialog(testCategory1)
        testScheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.showAddDialog.value)
        assertEquals(testCategory1, viewModel.editingCategory.value)
    }

    @Test
    fun `hides dialog`() = runTest {
        // Given
        viewModel = CategoryViewModel(categoryRepository, photoRepository)
        viewModel.showEditCategoryDialog(testCategory1)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.hideDialog()
        testScheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.showAddDialog.value)
        assertNull(viewModel.editingCategory.value)
    }

    @Test
    fun `adds new category successfully`() = runTest {
        // Given
        coEvery { categoryRepository.getCategoryByName(any()) } returns null
        coEvery { categoryRepository.insertCategory(any()) } returns 1L

        viewModel = CategoryViewModel(categoryRepository, photoRepository)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.addCategory("New Category", "#FF00FF")
        testScheduler.advanceUntilIdle()

        // Then
        coVerify {
            categoryRepository.insertCategory(match {
                it.displayName == "New Category" &&
                it.name == "new_category" &&
                it.colorHex == "#FF00FF" &&
                it.position == 2
            })
        }
        assertFalse(viewModel.showAddDialog.value)
    }

    @Test
    fun `rejects empty category name`() = runTest {
        // Given
        viewModel = CategoryViewModel(categoryRepository, photoRepository)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.addCategory("", "#FF00FF")
        testScheduler.advanceUntilIdle()

        // Then
        assertEquals("Category name cannot be empty", viewModel.error.value)
        coVerify(exactly = 0) { categoryRepository.insertCategory(any()) }
    }

    @Test
    fun `rejects duplicate category name`() = runTest {
        // Given
        coEvery { categoryRepository.getCategoryByName("category_1") } returns testCategory1

        viewModel = CategoryViewModel(categoryRepository, photoRepository)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.addCategory("Category 1", "#FF00FF")
        testScheduler.advanceUntilIdle()

        // Then
        assertEquals("Category 'Category 1' already exists", viewModel.error.value)
        coVerify(exactly = 0) { categoryRepository.insertCategory(any()) }
    }

    @Test
    fun `updates category successfully`() = runTest {
        // Given
        coEvery { categoryRepository.getCategoryByName(any()) } returns null
        coEvery { categoryRepository.updateCategory(any()) } coAnswers { }

        viewModel = CategoryViewModel(categoryRepository, photoRepository)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.updateCategory(testCategory1, "Updated Name", "#00FFFF")
        testScheduler.advanceUntilIdle()

        // Then
        coVerify {
            categoryRepository.updateCategory(match {
                it.id == testCategory1.id &&
                it.displayName == "Updated Name" &&
                it.name == "updated_name" &&
                it.colorHex == "#00FFFF"
            })
        }
        assertFalse(viewModel.showAddDialog.value)
    }

    @Test
    fun `prevents deletion of last category`() = runTest {
        // Given
        coEvery { categoryRepository.getCategoryCount() } returns 1

        viewModel = CategoryViewModel(categoryRepository, photoRepository)
        viewModel.refreshCategoriesWithCounts()
        testScheduler.advanceUntilIdle()

        // Mock only one category in the list
        val singleCategoryFlow = flowOf(listOf(testCategory1))
        every { categoryRepository.getAllCategoriesFlow() } returns singleCategoryFlow
        coEvery { categoryRepository.getAllCategories() } returns listOf(testCategory1)

        viewModel.refreshCategoriesWithCounts()
        testScheduler.advanceUntilIdle()

        // When
        val (canDelete, message) = viewModel.canDeleteCategory(testCategory1)

        // Then
        assertFalse(canDelete)
        assertEquals("Cannot delete the last remaining category", message)
    }

    @Test
    fun `deletes category without photos`() = runTest {
        // Given
        coEvery { categoryRepository.deleteCategory(any()) } just Runs
        coEvery { photoRepository.getPhotosByCategory(1L) } returns emptyList()

        viewModel = CategoryViewModel(categoryRepository, photoRepository)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.deleteCategory(testCategory1, deletePhotos = false)
        testScheduler.advanceUntilIdle()

        // Then
        coVerify { categoryRepository.deleteCategory(testCategory1) }
        coVerify(exactly = 0) { photoRepository.deletePhoto(any()) }
    }

    @Test
    fun `deletes category with photos when requested`() = runTest {
        // Given
        coEvery { categoryRepository.deleteCategory(any()) } just Runs
        coEvery { photoRepository.getPhotosByCategory(1L) } returns listOf(testPhoto)
        coEvery { photoRepository.deletePhoto(any()) } just Runs

        viewModel = CategoryViewModel(categoryRepository, photoRepository)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.deleteCategory(testCategory1, deletePhotos = true)
        testScheduler.advanceUntilIdle()

        // Then
        coVerify { photoRepository.deletePhoto(testPhoto) }
        coVerify { categoryRepository.deleteCategory(testCategory1) }
    }

    @Test
    fun `gets category photo count with callback`() = runTest {
        // Given
        var receivedCount = -1
        viewModel = CategoryViewModel(categoryRepository, photoRepository)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.getCategoryPhotoCount(1L) { count ->
            receivedCount = count
        }
        testScheduler.advanceUntilIdle()

        // Then
        assertEquals(5, receivedCount)
    }

    @Test
    fun `handles error when adding category fails`() = runTest {
        // Given
        coEvery { categoryRepository.getCategoryByName(any()) } returns null
        coEvery { categoryRepository.insertCategory(any()) } throws RuntimeException("Database error")

        viewModel = CategoryViewModel(categoryRepository, photoRepository)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.addCategory("New Category", "#FF00FF")
        testScheduler.advanceUntilIdle()

        // Then
        assertNotNull(viewModel.error.value)
        assertTrue(viewModel.error.value?.contains("Failed to add category") == true)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `clears error state`() = runTest {
        // Given
        viewModel = CategoryViewModel(categoryRepository, photoRepository)
        viewModel.addCategory("", "#FF00FF") // Generate an error
        testScheduler.advanceUntilIdle()

        // When
        viewModel.clearError()
        testScheduler.advanceUntilIdle()

        // Then
        assertNull(viewModel.error.value)
    }

    @Test
    fun `normalizes display name correctly`() = runTest {
        // Given
        coEvery { categoryRepository.getCategoryByName("test_category_name") } returns null
        coEvery { categoryRepository.insertCategory(any()) } returns 1L

        viewModel = CategoryViewModel(categoryRepository, photoRepository)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.addCategory("  Test Category Name  ", "#FF00FF")
        testScheduler.advanceUntilIdle()

        // Then
        coVerify {
            categoryRepository.insertCategory(match {
                it.name == "test_category_name" &&
                it.displayName == "Test Category Name"
            })
        }
    }

    @Test
    fun `predefined colors are available`() {
        // Given & When
        val colors = CategoryViewModel.PREDEFINED_COLORS

        // Then
        assertEquals(12, colors.size)
        assertTrue(colors.contains("#4CAF50"))
        assertTrue(colors.contains("#2196F3"))
        assertTrue(colors.all { it.matches(Regex("^#[A-F0-9]{6}$")) })
    }
}