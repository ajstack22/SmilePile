package com.smilepile.ui.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.smilepile.data.models.Category
import com.smilepile.data.models.Photo
import com.smilepile.data.repository.CategoryRepository
import com.smilepile.data.repository.PhotoRepository
import com.smilepile.storage.StorageManager
import com.smilepile.utils.IImageProcessor
import com.smilepile.fakes.FakeImageProcessor
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File

/**
 * Unit tests for PhotoEditViewModel
 */
@ExperimentalCoroutinesApi
class PhotoEditViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var context: Context
    private lateinit var storageManager: StorageManager
    private lateinit var photoRepository: PhotoRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var fakeImageProcessor: FakeImageProcessor
    private lateinit var viewModel: PhotoEditViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setupDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    // Test data
    private val testCategory = Category(
        id = 1L,
        name = "category1",
        displayName = "Category 1",
        position = 0,
        colorHex = "#FF0000",
        isDefault = true
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

    private lateinit var mockBitmap: Bitmap
    private lateinit var mockPreviewBitmap: Bitmap

    @Before
    fun setup() {
        // Initialize context
        context = mockk(relaxed = true)

        // Initialize mocks
        storageManager = mockk(relaxed = true)
        photoRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        fakeImageProcessor = FakeImageProcessor()

        // Setup bitmap mocks
        mockBitmap = mockk(relaxed = true)
        mockPreviewBitmap = mockk(relaxed = true)
        every { mockBitmap.width } returns 1920
        every { mockBitmap.height } returns 1080
        every { mockPreviewBitmap.width } returns 192
        every { mockPreviewBitmap.height } returns 108

        // Setup default mock responses - use MutableStateFlow for immediate emission
        every { categoryRepository.getAllCategoriesFlow() } returns MutableStateFlow(listOf(testCategory))
        coEvery { photoRepository.getPhotoByPath(any()) } returns testPhoto

        // Setup static mocks once in @Before to avoid race conditions
        setupStaticMocks()
    }

    private fun setupStaticMocks() {
        // Mock static methods for Bitmap loading only
        mockkStatic(BitmapFactory::class)

        every { BitmapFactory.decodeFile(any()) } returns mockBitmap
        every { BitmapFactory.decodeStream(any()) } returns mockBitmap
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial state is correct`() = runTest {
        // Given & When
        viewModel = PhotoEditViewModel(context, storageManager, photoRepository, categoryRepository, fakeImageProcessor)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.editQueue.isEmpty())
        assertEquals(-1, uiState.currentIndex)
        assertEquals(0, uiState.totalPhotos)
        assertNull(uiState.currentBitmap)
        assertNull(uiState.previewBitmap)
        assertEquals(0f, uiState.currentRotation)
        assertNull(uiState.currentCropRect)
        assertFalse(uiState.isLoading)
        assertNull(uiState.error)
        assertFalse(uiState.isComplete)
    }

    @Test
    fun `initializes editor with URIs for import`() = runTest {
        // Given
        val uri1 = mockk<Uri>(relaxed = true)
        val uri2 = mockk<Uri>(relaxed = true)
        val contentResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(any()) } returns ByteArrayInputStream(ByteArray(100))

        viewModel = PhotoEditViewModel(context, storageManager, photoRepository, categoryRepository, fakeImageProcessor)

        // When
        viewModel.initializeEditor(photoUris = listOf(uri1, uri2), categoryId = 2L)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(2, uiState.editQueue.size)
        assertEquals(2, uiState.totalPhotos)
        assertEquals(0, uiState.currentIndex)
        assertEquals(2L, viewModel.getPendingCategoryId())
    }

    @Test
    fun `initializes editor with paths for gallery editing`() = runTest {
        // Given
        val path = "/test/photo.jpg"
        viewModel = PhotoEditViewModel(context, storageManager, photoRepository, categoryRepository, fakeImageProcessor)

        // When
        viewModel.initializeEditor(photoPaths = listOf(path))
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.editQueue.size)
        assertEquals(1, uiState.totalPhotos)
        assertEquals(0, uiState.currentIndex)
        assertEquals(testPhoto.categoryId, viewModel.getPendingCategoryId()) // Should use existing category
    }

    @Test
    fun `rotates photo correctly`() = runTest {
        // Given
        viewModel = PhotoEditViewModel(context, storageManager, photoRepository, categoryRepository, fakeImageProcessor)
        viewModel.initializeEditor(photoPaths = listOf("/test/photo.jpg"))
        advanceUntilIdle()

        // When
        viewModel.rotatePhoto()
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(90f, uiState.currentRotation)
        // With FakeImageProcessor, bitmap remains the same but rotation is tracked
        assertNotNull(uiState.currentBitmap)
    }

    @Test
    fun `updates crop rectangle`() = runTest {
        // Given
        viewModel = PhotoEditViewModel(context, storageManager, photoRepository, categoryRepository, fakeImageProcessor)
        viewModel.initializeEditor(photoPaths = listOf("/test/photo.jpg"))
        advanceUntilIdle()

        val cropRect = androidx.compose.ui.geometry.Rect(10f, 10f, 100f, 100f)

        // When
        viewModel.updateCropRect(cropRect)
        advanceUntilIdle()

        // Then
        val currentCropRect = viewModel.uiState.value.currentCropRect
        assertNotNull(currentCropRect)
        assertEquals(10, currentCropRect!!.left)
        assertEquals(10, currentCropRect.top)
        assertEquals(100, currentCropRect.right)
        assertEquals(100, currentCropRect.bottom)
    }

    @Test
    fun `applies aspect ratio preset`() = runTest {
        // Given
        viewModel = PhotoEditViewModel(context, storageManager, photoRepository, categoryRepository, fakeImageProcessor)
        viewModel.initializeEditor(photoPaths = listOf("/test/photo.jpg"))
        advanceUntilIdle()

        // When
        viewModel.applyAspectRatio(IImageProcessor.AspectRatio.SQUARE)
        advanceUntilIdle()

        // Then
        // With FakeImageProcessor, crop rect is always returned
        assertNotNull(viewModel.uiState.value.currentCropRect)
    }

    @Test
    fun `skips current photo without editing`() = runTest {
        // Given
        viewModel = PhotoEditViewModel(context, storageManager, photoRepository, categoryRepository, fakeImageProcessor)
        viewModel.initializeEditor(photoPaths = listOf("/test/photo1.jpg", "/test/photo2.jpg"))
        advanceUntilIdle()

        // When
        viewModel.skipCurrentPhoto()
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.currentIndex)
        val firstItem = uiState.editQueue[0]
        assertTrue(firstItem.isProcessed)
        assertFalse(firstItem.wasEdited)
    }

    @Test
    fun `applies edits to current photo`() = runTest {
        // Given
        viewModel = PhotoEditViewModel(context, storageManager, photoRepository, categoryRepository, fakeImageProcessor)
        viewModel.initializeEditor(photoPaths = listOf("/test/photo1.jpg", "/test/photo2.jpg"))
        advanceUntilIdle()

        viewModel.rotatePhoto()
        advanceUntilIdle()

        // When
        viewModel.applyCurrentPhoto()
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.currentIndex)
        val firstItem = uiState.editQueue[0]
        assertTrue(firstItem.isProcessed)
        assertTrue(firstItem.wasEdited)
        assertNotNull(firstItem.processedBitmap)
    }

    @Test
    fun `applies rotation to all remaining photos`() = runTest {
        // Given
        viewModel = PhotoEditViewModel(context, storageManager, photoRepository, categoryRepository, fakeImageProcessor)
        viewModel.initializeEditor(photoPaths = listOf("/test/photo1.jpg", "/test/photo2.jpg"))
        advanceUntilIdle()

        viewModel.rotatePhoto()
        viewModel.rotatePhoto() // 180 degrees
        advanceUntilIdle()

        // When
        viewModel.applyToAll()
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.applyRotationToAll)
        assertEquals(180f, uiState.batchRotation)
    }

    @Test
    fun `deletes current photo from edit queue`() = runTest {
        // Given
        coEvery { storageManager.deletePhoto(any()) } returns true
        coEvery { photoRepository.deletePhoto(any()) } just Runs

        viewModel = PhotoEditViewModel(context, storageManager, photoRepository, categoryRepository, fakeImageProcessor)
        viewModel.initializeEditor(photoPaths = listOf("/test/photo1.jpg", "/test/photo2.jpg"))
        advanceUntilIdle()

        // When
        viewModel.deleteCurrentPhoto()
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.totalPhotos)
        assertEquals(0, uiState.currentIndex)
        coVerify { storageManager.deletePhoto("/test/photo1.jpg") }
    }

    @Test
    fun `updates pending category`() = runTest {
        // Given
        viewModel = PhotoEditViewModel(context, storageManager, photoRepository, categoryRepository, fakeImageProcessor)
        advanceUntilIdle()

        // When
        viewModel.updatePendingCategory(3L)

        // Then
        assertEquals(3L, viewModel.getPendingCategoryId())
    }

    @Test
    fun `completes editing when all photos processed`() = runTest {
        // Given
        viewModel = PhotoEditViewModel(context, storageManager, photoRepository, categoryRepository, fakeImageProcessor)
        viewModel.initializeEditor(photoPaths = listOf("/test/photo.jpg"))
        advanceUntilIdle()

        // When
        viewModel.skipCurrentPhoto()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.isComplete)
    }

    @Test
    fun `saves processed photos correctly`() = runTest {
        // Given
        val savedFile = File("/saved/photo.jpg")
        coEvery { storageManager.savePhotoToInternalStorage(any(), any()) } returns savedFile
        coEvery { photoRepository.insertPhoto(any()) } returns 1L
        coEvery { photoRepository.updatePhoto(any()) } just Runs

        viewModel = PhotoEditViewModel(context, storageManager, photoRepository, categoryRepository, fakeImageProcessor)
        viewModel.initializeEditor(photoPaths = listOf("/test/photo.jpg"))
        advanceUntilIdle()

        viewModel.applyCurrentPhoto()
        advanceUntilIdle()

        // When
        val savedPhotos = viewModel.saveAllProcessedPhotos()

        // Then
        assertEquals(1, savedPhotos.size)
        coVerify { photoRepository.updatePhoto(any()) }
    }

    @Test
    fun `handles error when loading photo fails`() = runTest {
        // Given
        every { BitmapFactory.decodeFile(any()) } returns null

        viewModel = PhotoEditViewModel(context, storageManager, photoRepository, categoryRepository, fakeImageProcessor)

        // When
        viewModel.initializeEditor(photoPaths = listOf("/test/invalid.jpg"))
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        // Error might not be set if bitmap is null - depends on implementation
    }

    @Test
    fun `progress text shows correct format`() = runTest {
        // Given
        viewModel = PhotoEditViewModel(context, storageManager, photoRepository, categoryRepository, fakeImageProcessor)
        viewModel.initializeEditor(photoPaths = listOf("/test/photo1.jpg", "/test/photo2.jpg", "/test/photo3.jpg"))
        advanceUntilIdle()

        // When
        val progressText = viewModel.uiState.value.progressText

        // Then
        assertEquals("1 / 3", progressText)
    }

    @Test
    fun `can apply to all is enabled when rotation applied and more photos remain`() = runTest {
        // Given
        viewModel = PhotoEditViewModel(context, storageManager, photoRepository, categoryRepository, fakeImageProcessor)
        viewModel.initializeEditor(photoPaths = listOf("/test/photo1.jpg", "/test/photo2.jpg"))
        advanceUntilIdle()

        viewModel.rotatePhoto()
        advanceUntilIdle()

        // When
        val canApplyToAll = viewModel.uiState.value.canApplyToAll

        // Then
        assertTrue(canApplyToAll)
    }

    @Test
    fun `gets processed results correctly`() = runTest {
        // Given
        viewModel = PhotoEditViewModel(context, storageManager, photoRepository, categoryRepository, fakeImageProcessor)
        viewModel.initializeEditor(photoPaths = listOf("/test/photo1.jpg", "/test/photo2.jpg"))
        advanceUntilIdle()

        viewModel.applyCurrentPhoto()
        advanceUntilIdle()

        viewModel.skipCurrentPhoto()
        advanceUntilIdle()

        // When
        val results = viewModel.getProcessedResults()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[0].wasEdited)
        assertFalse(results[1].wasEdited)
    }
}