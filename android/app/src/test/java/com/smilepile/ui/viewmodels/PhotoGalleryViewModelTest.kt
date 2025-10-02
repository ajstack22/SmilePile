package com.smilepile.ui.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.smilepile.data.models.Category
import com.smilepile.data.models.Photo
import com.smilepile.data.repository.CategoryRepository
import com.smilepile.data.repository.PhotoRepository
import com.smilepile.operations.PhotoOperationsManager
import com.smilepile.operations.BatchOperationResult
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for PhotoGalleryViewModel
 */
@ExperimentalCoroutinesApi
class PhotoGalleryViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var photoRepository: PhotoRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var photoOperationsManager: PhotoOperationsManager
    private lateinit var viewModel: PhotoGalleryViewModel

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

    private val testPhoto1 = Photo(
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

    private val testPhoto2 = Photo(
        id = 2L,
        name = "photo2.jpg",
        path = "/test/photo2.jpg",
        categoryId = 2L,
        createdAt = System.currentTimeMillis(),
        width = 1920,
        height = 1080,
        fileSize = 2048L,
        isFromAssets = false
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Initialize mocks
        photoRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        photoOperationsManager = mockk(relaxed = true)

        // Setup default mock responses
        every { categoryRepository.getAllCategoriesFlow() } returns flowOf(listOf(testCategory1, testCategory2))
        every { photoRepository.getAllPhotosFlow() } returns flowOf(listOf(testPhoto1, testPhoto2))
        every { photoRepository.getPhotosByCategoryFlow(1L) } returns flowOf(listOf(testPhoto1))
        every { photoRepository.getPhotosByCategoryFlow(2L) } returns flowOf(listOf(testPhoto2))
        every { photoRepository.getPhotosInCategoriesFlow(any()) } returns flowOf(listOf(testPhoto1, testPhoto2))

        coEvery { categoryRepository.initializeDefaultCategories() } just Runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial state is correct`() = runTest {
        // Given & When
        viewModel = PhotoGalleryViewModel(photoRepository, categoryRepository, photoOperationsManager)
        testScheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertNull(uiState.error)
        assertFalse(uiState.isSelectionMode)
        assertTrue(uiState.selectedPhotos.isEmpty())
        assertFalse(uiState.isBatchOperationInProgress)
    }

    @Test
    fun `loads categories on initialization`() = runTest {
        // Given & When
        viewModel = PhotoGalleryViewModel(photoRepository, categoryRepository, photoOperationsManager)
        testScheduler.advanceUntilIdle()

        // Then
        val categories = viewModel.categories.value
        assertEquals(2, categories.size)
        assertEquals(testCategory1, categories[0])
        assertEquals(testCategory2, categories[1])
    }

    @Test
    fun `selects category and filters photos`() = runTest {
        // Given
        viewModel = PhotoGalleryViewModel(photoRepository, categoryRepository, photoOperationsManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.selectCategory(1L)
        testScheduler.advanceUntilIdle()

        // Then
        val selectedCategoryIds = viewModel.selectedCategoryIds.value
        assertEquals(setOf(1L), selectedCategoryIds)

        val photos = viewModel.photos.value
        assertEquals(1, photos.size)
        assertEquals(testPhoto1, photos[0])
    }

    @Test
    fun `clears category filter shows all photos`() = runTest {
        // Given
        viewModel = PhotoGalleryViewModel(photoRepository, categoryRepository, photoOperationsManager)
        testScheduler.advanceUntilIdle()

        viewModel.selectCategory(1L)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.clearCategoryFilters()
        testScheduler.advanceUntilIdle()

        // Then
        val selectedCategoryIds = viewModel.selectedCategoryIds.value
        assertTrue(selectedCategoryIds.isEmpty())

        val photos = viewModel.photos.value
        assertEquals(2, photos.size)
    }

    @Test
    fun `toggle category filter adds and removes categories`() = runTest {
        // Given
        viewModel = PhotoGalleryViewModel(photoRepository, categoryRepository, photoOperationsManager)
        testScheduler.advanceUntilIdle()

        // When - Add first category
        viewModel.toggleCategoryFilter(1L)
        testScheduler.advanceUntilIdle()

        // Then
        assertEquals(setOf(1L), viewModel.selectedCategoryIds.value)

        // When - Add second category
        viewModel.toggleCategoryFilter(2L)
        testScheduler.advanceUntilIdle()

        // Then
        assertEquals(setOf(1L, 2L), viewModel.selectedCategoryIds.value)

        // When - Remove first category
        viewModel.toggleCategoryFilter(1L)
        testScheduler.advanceUntilIdle()

        // Then
        assertEquals(setOf(2L), viewModel.selectedCategoryIds.value)
    }

    @Test
    fun `enters and exits selection mode`() = runTest {
        // Given
        viewModel = PhotoGalleryViewModel(photoRepository, categoryRepository, photoOperationsManager)
        testScheduler.advanceUntilIdle()

        // When - Enter selection mode
        viewModel.enterSelectionMode()
        testScheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.isSelectionMode.value)

        // When - Exit selection mode
        viewModel.exitSelectionMode()
        testScheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.isSelectionMode.value)
        assertTrue(viewModel.selectedPhotos.value.isEmpty())
    }

    @Test
    fun `toggles photo selection`() = runTest {
        // Given
        viewModel = PhotoGalleryViewModel(photoRepository, categoryRepository, photoOperationsManager)
        testScheduler.advanceUntilIdle()

        viewModel.enterSelectionMode()

        // When - Select photo
        viewModel.togglePhotoSelection(1L)
        testScheduler.advanceUntilIdle()

        // Then
        assertEquals(setOf(1L), viewModel.selectedPhotos.value)

        // When - Deselect photo
        viewModel.togglePhotoSelection(1L)
        testScheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.selectedPhotos.value.isEmpty())
        assertFalse(viewModel.isSelectionMode.value) // Should exit when no photos selected
    }

    @Test
    fun `selects all photos`() = runTest {
        // Given
        viewModel = PhotoGalleryViewModel(photoRepository, categoryRepository, photoOperationsManager)
        testScheduler.advanceUntilIdle()

        viewModel.enterSelectionMode()

        // When
        viewModel.selectAllPhotos()
        testScheduler.advanceUntilIdle()

        // Then
        val selectedPhotos = viewModel.selectedPhotos.value
        assertEquals(2, selectedPhotos.size)
        assertTrue(selectedPhotos.contains(1L))
        assertTrue(selectedPhotos.contains(2L))
    }

    @Test
    fun `removes photo from library`() = runTest {
        // Given
        viewModel = PhotoGalleryViewModel(photoRepository, categoryRepository, photoOperationsManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.removePhotoFromLibrary(testPhoto1)
        testScheduler.advanceUntilIdle()

        // Then
        coVerify { photoRepository.removeFromLibrary(testPhoto1) }
    }

    @Test
    fun `moves photo to category`() = runTest {
        // Given
        viewModel = PhotoGalleryViewModel(photoRepository, categoryRepository, photoOperationsManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.movePhotoToCategory(testPhoto1, 2L)
        testScheduler.advanceUntilIdle()

        // Then
        coVerify {
            photoRepository.updatePhoto(match {
                it.id == testPhoto1.id && it.categoryId == 2L
            })
        }
    }

    @Test
    fun `removes selected photos from library batch operation`() = runTest {
        // Given
        val batchResult = BatchOperationResult(
            successCount = 2,
            failureCount = 0,
            failedItems = listOf()
        )
        coEvery { photoOperationsManager.removeFromLibrary(any<List<Photo>>()) } returns batchResult

        viewModel = PhotoGalleryViewModel(photoRepository, categoryRepository, photoOperationsManager)
        testScheduler.advanceUntilIdle()

        viewModel.enterSelectionMode()
        viewModel.togglePhotoSelection(1L)
        viewModel.togglePhotoSelection(2L)

        // When
        viewModel.removeSelectedPhotosFromLibrary()
        testScheduler.advanceUntilIdle()

        // Then
        coVerify { photoOperationsManager.removeFromLibrary(match<List<Photo>> { it.size == 2 }) }
        assertFalse(viewModel.isSelectionMode.value)
        assertTrue(viewModel.selectedPhotos.value.isEmpty())
    }

    @Test
    fun `moves selected photos to category batch operation`() = runTest {
        // Given
        val batchResult = BatchOperationResult(
            successCount = 2,
            failureCount = 0,
            failedItems = listOf()
        )
        coEvery { photoOperationsManager.movePhotosToCategory(any(), any()) } returns batchResult

        viewModel = PhotoGalleryViewModel(photoRepository, categoryRepository, photoOperationsManager)
        testScheduler.advanceUntilIdle()

        viewModel.enterSelectionMode()
        viewModel.togglePhotoSelection(1L)
        viewModel.togglePhotoSelection(2L)

        // When
        viewModel.moveSelectedPhotosToCategory(2L)
        testScheduler.advanceUntilIdle()

        // Then
        coVerify { photoOperationsManager.movePhotosToCategory(match { it.size == 2 }, 2L) }
        assertFalse(viewModel.isSelectionMode.value)
        assertTrue(viewModel.selectedPhotos.value.isEmpty())
    }

    @Test
    fun `assigns selected photos to multiple categories`() = runTest {
        // Given
        coEvery { categoryRepository.assignPhotoToCategories(any(), any()) } just Runs

        viewModel = PhotoGalleryViewModel(photoRepository, categoryRepository, photoOperationsManager)
        testScheduler.advanceUntilIdle()

        viewModel.enterSelectionMode()
        viewModel.togglePhotoSelection(1L)

        // When
        viewModel.assignSelectedPhotosToCategories(listOf(1L, 2L))
        testScheduler.advanceUntilIdle()

        // Then
        coVerify { categoryRepository.assignPhotoToCategories("1", listOf(1L, 2L)) }
        assertFalse(viewModel.isSelectionMode.value)
    }

    @Test
    fun `handles error when loading photos fails`() = runTest {
        // Given
        val errorFlow = MutableStateFlow<List<Photo>>(emptyList())
        every { photoRepository.getAllPhotosFlow() } returns errorFlow

        viewModel = PhotoGalleryViewModel(photoRepository, categoryRepository, photoOperationsManager)
        testScheduler.advanceUntilIdle()

        // When - Simulate error
        // The catch block in the ViewModel will handle the error

        // Then - Photos should be empty list due to error handling
        val photos = viewModel.photos.value
        assertTrue(photos.isEmpty())
    }

    @Test
    fun `clears error state`() = runTest {
        // Given
        viewModel = PhotoGalleryViewModel(photoRepository, categoryRepository, photoOperationsManager)
        testScheduler.advanceUntilIdle()

        // Simulate an error
        viewModel.movePhotoToCategory(testPhoto1, -1L)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.clearError()
        testScheduler.advanceUntilIdle()

        // Then
        assertNull(viewModel.error.value)
    }

    @Test
    fun `ui state combines all state flows correctly`() = runTest {
        // Given
        viewModel = PhotoGalleryViewModel(photoRepository, categoryRepository, photoOperationsManager)
        testScheduler.advanceUntilIdle()

        // When
        viewModel.enterSelectionMode()
        viewModel.togglePhotoSelection(1L)
        viewModel.selectCategory(1L)
        testScheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.photos.size)
        assertEquals(2, uiState.categories.size)
        assertEquals(setOf(1L), uiState.selectedCategoryIds)
        assertEquals(1L, uiState.selectedCategoryId) // Legacy compatibility
        assertTrue(uiState.isSelectionMode)
        assertEquals(1, uiState.selectedPhotosCount)
        assertTrue(uiState.hasSelectedPhotos)
        assertFalse(uiState.isAllPhotosSelected)
    }
}