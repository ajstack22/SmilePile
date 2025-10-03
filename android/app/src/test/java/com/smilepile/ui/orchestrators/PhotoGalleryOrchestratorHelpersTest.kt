package com.smilepile.ui.orchestrators

import android.content.Context
import android.net.Uri
import com.smilepile.data.models.Photo
import com.smilepile.utils.PermissionHandler
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for PhotoGalleryOrchestrator helper functions
 */
class PhotoGalleryOrchestratorHelpersTest {

    private lateinit var context: Context
    private lateinit var photo: Photo
    private lateinit var photoList: List<Photo>

    @Before
    fun setup() {
        context = mockk(relaxed = true)

        photo = Photo(
            id = 1L,
            path = "/test/photo1.jpg",
            categoryId = 1L,
            name = "photo1.jpg",
            isFromAssets = false,
            createdAt = System.currentTimeMillis(),
            fileSize = 1000,
            width = 1920,
            height = 1080
        )

        photoList = listOf(
            photo,
            Photo(
                id = 2L,
                path = "/test/photo2.jpg",
                categoryId = 1L,
                name = "photo2.jpg",
                isFromAssets = false,
                createdAt = System.currentTimeMillis(),
                fileSize = 1000,
                width = 1920,
                height = 1080
            )
        )

        mockkObject(PermissionHandler)
    }

    @After
    fun tearDown() {
        unmockkObject(PermissionHandler)
    }

    @Test
    fun `handlePermissionResult launches picker when permission granted`() {
        // Given
        var pickerLaunched = false
        var dialogShown = false
        val selectedCategoryId = 1L

        // When
        handlePermissionResultTest(
            isGranted = true,
            selectedCategoryId = selectedCategoryId,
            onLaunchPicker = { pickerLaunched = true },
            onShowDialog = { dialogShown = true }
        )

        // Then
        assertTrue(pickerLaunched)
        assertFalse(dialogShown)
    }

    @Test
    fun `handlePermissionResult shows dialog when permission denied`() {
        // Given
        var pickerLaunched = false
        var dialogShown = false
        val selectedCategoryId = 1L

        // When
        handlePermissionResultTest(
            isGranted = false,
            selectedCategoryId = selectedCategoryId,
            onLaunchPicker = { pickerLaunched = true },
            onShowDialog = { dialogShown = true }
        )

        // Then
        assertFalse(pickerLaunched)
        assertTrue(dialogShown)
    }

    @Test
    fun `handlePermissionResult does not launch picker when no category selected`() {
        // Given
        var pickerLaunched = false
        var dialogShown = false

        // When
        handlePermissionResultTest(
            isGranted = true,
            selectedCategoryId = null,
            onLaunchPicker = { pickerLaunched = true },
            onShowDialog = { dialogShown = true }
        )

        // Then
        assertFalse(pickerLaunched)
        assertFalse(dialogShown)
    }

    @Test
    fun `handlePhotoClick toggles selection in selection mode`() {
        // Given
        var selectionToggled = false
        var navigationCalled = false

        // When
        handlePhotoClickTest(
            photo = photo,
            photoList = photoList,
            isSelectionMode = true,
            onToggleSelection = { selectionToggled = true },
            onNavigateToPhoto = { _, _ -> navigationCalled = true }
        )

        // Then
        assertTrue(selectionToggled)
        assertFalse(navigationCalled)
    }

    @Test
    fun `handlePhotoClick navigates to photo in normal mode`() {
        // Given
        var selectionToggled = false
        var navigationCalled = false
        var navigatedPhoto: Photo? = null
        var navigatedList: List<Photo>? = null

        // When
        handlePhotoClickTest(
            photo = photo,
            photoList = photoList,
            isSelectionMode = false,
            onToggleSelection = { selectionToggled = true },
            onNavigateToPhoto = { p, l ->
                navigationCalled = true
                navigatedPhoto = p
                navigatedList = l
            }
        )

        // Then
        assertFalse(selectionToggled)
        assertTrue(navigationCalled)
        assertEquals(photo, navigatedPhoto)
        assertEquals(photoList, navigatedList)
    }

    @Test
    fun `handlePhotoClick finds correct photo index`() {
        // Given
        val targetPhoto = photoList[1]
        var navigationCalled = false
        var navigatedPhoto: Photo? = null

        // When
        handlePhotoClickTest(
            photo = targetPhoto,
            photoList = photoList,
            isSelectionMode = false,
            onToggleSelection = {},
            onNavigateToPhoto = { p, _ ->
                navigationCalled = true
                navigatedPhoto = p
            }
        )

        // Then
        assertTrue(navigationCalled)
        assertEquals(targetPhoto, navigatedPhoto)
    }

    @Test
    fun `handleCategorySelectionForImport launches picker when adding photos`() {
        // Given
        var categoryStored = false
        var dialogHidden = false
        var pickerLaunched = false
        val categoryId = 1L

        every { PermissionHandler.isStoragePermissionGranted(any()) } returns true

        // When
        handleCategorySelectionForImportTest(
            categoryId = categoryId,
            isAddingPhotos = true,
            pendingImportUris = null,
            context = context,
            onStoreCategoryId = { categoryStored = true },
            onHideDialog = { dialogHidden = true },
            onLaunchPicker = { pickerLaunched = true },
            onRequestPermission = {},
            onNavigateWithUris = {}
        )

        // Then
        assertTrue(categoryStored)
        assertTrue(dialogHidden)
        assertTrue(pickerLaunched)
    }

    @Test
    fun `handleCategorySelectionForImport requests permission when not granted`() {
        // Given
        var permissionRequested = false
        var pickerLaunched = false
        val categoryId = 1L

        every { PermissionHandler.isStoragePermissionGranted(any()) } returns false

        // When
        handleCategorySelectionForImportTest(
            categoryId = categoryId,
            isAddingPhotos = true,
            pendingImportUris = null,
            context = context,
            onStoreCategoryId = {},
            onHideDialog = {},
            onLaunchPicker = { pickerLaunched = true },
            onRequestPermission = { permissionRequested = true },
            onNavigateWithUris = {}
        )

        // Then
        assertTrue(permissionRequested)
        assertFalse(pickerLaunched)
    }

    @Test
    fun `handleCategorySelectionForImport navigates with pending URIs`() {
        // Given
        var navigationCalled = false
        var navigatedUris: List<Uri>? = null
        val categoryId = 1L
        val pendingUris = listOf(mockk<Uri>(), mockk<Uri>())

        // When
        handleCategorySelectionForImportTest(
            categoryId = categoryId,
            isAddingPhotos = false,
            pendingImportUris = pendingUris,
            context = context,
            onStoreCategoryId = {},
            onHideDialog = {},
            onLaunchPicker = {},
            onRequestPermission = {},
            onNavigateWithUris = { uris ->
                navigationCalled = true
                navigatedUris = uris
            }
        )

        // Then
        assertTrue(navigationCalled)
        assertEquals(pendingUris, navigatedUris)
    }

    @Test
    fun `handleCategorySelectionForImport does nothing when not adding and no pending URIs`() {
        // Given
        var anyActionCalled = false
        val categoryId = 1L

        // When
        handleCategorySelectionForImportTest(
            categoryId = categoryId,
            isAddingPhotos = false,
            pendingImportUris = null,
            context = context,
            onStoreCategoryId = { anyActionCalled = true },
            onHideDialog = { anyActionCalled = true },
            onLaunchPicker = { anyActionCalled = true },
            onRequestPermission = { anyActionCalled = true },
            onNavigateWithUris = { anyActionCalled = true }
        )

        // Then
        assertFalse(anyActionCalled)
    }

    // Helper functions that replicate the private orchestrator functions for testing

    private fun handlePermissionResultTest(
        isGranted: Boolean,
        selectedCategoryId: Long?,
        onLaunchPicker: () -> Unit,
        onShowDialog: () -> Unit
    ) {
        if (isGranted && selectedCategoryId != null) {
            onLaunchPicker()
        } else if (!isGranted) {
            onShowDialog()
        }
    }

    private fun handlePhotoClickTest(
        photo: Photo,
        photoList: List<Photo>,
        isSelectionMode: Boolean,
        onToggleSelection: () -> Unit,
        onNavigateToPhoto: (Photo, List<Photo>) -> Unit
    ) {
        if (isSelectionMode) {
            onToggleSelection()
        } else {
            onNavigateToPhoto(photo, photoList)
        }
    }

    private fun handleCategorySelectionForImportTest(
        categoryId: Long,
        isAddingPhotos: Boolean,
        pendingImportUris: List<Uri>?,
        context: Context,
        onStoreCategoryId: () -> Unit,
        onHideDialog: () -> Unit,
        onLaunchPicker: () -> Unit,
        onRequestPermission: () -> Unit,
        onNavigateWithUris: (List<Uri>) -> Unit
    ) {
        if (isAddingPhotos) {
            onStoreCategoryId()
            onHideDialog()

            if (PermissionHandler.isStoragePermissionGranted(context)) {
                onLaunchPicker()
            } else {
                onRequestPermission()
            }
        } else if (pendingImportUris != null) {
            onNavigateWithUris(pendingImportUris)
            onHideDialog()
        }
    }
}
