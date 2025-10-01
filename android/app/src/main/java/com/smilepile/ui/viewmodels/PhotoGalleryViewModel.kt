package com.smilepile.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smilepile.data.models.Category
import com.smilepile.data.models.Photo
import com.smilepile.data.repository.CategoryRepository
import com.smilepile.data.repository.PhotoRepository
import com.smilepile.operations.PhotoOperationsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotoGalleryViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val categoryRepository: CategoryRepository,
    private val photoOperationsManager: PhotoOperationsManager
) : ViewModel() {

    // Support multiple category filtering
    private val _selectedCategoryIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedCategoryIds: StateFlow<Set<Long>> = _selectedCategoryIds.asStateFlow()

    // Legacy single category support (for backward compatibility)
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    init {
        // Initialize default categories if needed
        viewModelScope.launch {
            categoryRepository.initializeDefaultCategories()

            // Collect both categories and photos to determine initial selection
            combine(
                categoryRepository.getAllCategoriesFlow(),
                photoRepository.getAllPhotosFlow()
            ) { categoriesList, photosList ->
                Pair(categoriesList, photosList)
            }.collect { (categoriesList, photosList) ->
                if (_selectedCategoryIds.value.isEmpty() && categoriesList.isNotEmpty()) {
                    if (photosList.isEmpty()) {
                        // Gallery is empty - auto-select first category for clear UX
                        val firstCategoryId = categoriesList.first().id
                        _selectedCategoryIds.value = setOf(firstCategoryId)
                        _selectedCategoryId.value = firstCategoryId
                        println("SmilePile Debug: Gallery empty - auto-selected first category (${categoriesList.first().displayName})")
                    } else {
                        // Gallery has photos - default to showing all photos (no filter)
                        _selectedCategoryIds.value = emptySet()
                        println("SmilePile Debug: Initialized with no category filter (showing all photos)")
                    }
                }
            }
        }
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Multi-selection state
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedPhotos = MutableStateFlow<Set<Long>>(emptySet())
    val selectedPhotos: StateFlow<Set<Long>> = _selectedPhotos.asStateFlow()

    private val _isBatchOperationInProgress = MutableStateFlow(false)
    val isBatchOperationInProgress: StateFlow<Boolean> = _isBatchOperationInProgress.asStateFlow()

    // Get all categories for the filter chips
    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategoriesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Get ALL photos (unfiltered) for fullscreen navigation
    val allPhotos: StateFlow<List<Photo>> = photoRepository.getAllPhotosFlow()
        .catch { e ->
            println("SmilePile Error: Failed to load all photos: ${e.message}")
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Get photos based on selected category filters with error handling
    @OptIn(ExperimentalCoroutinesApi::class)
    val photos: StateFlow<List<Photo>> = _selectedCategoryIds
        .flatMapLatest { categoryIds ->
            when {
                categoryIds.isEmpty() -> {
                    // No filter - show all photos
                    photoRepository.getAllPhotosFlow()
                }
                categoryIds.size == 1 -> {
                    // Single category filter
                    photoRepository.getPhotosByCategoryFlow(categoryIds.first())
                }
                else -> {
                    // Multiple category filter - photos in ANY of the selected categories
                    photoRepository.getPhotosInCategoriesFlow(categoryIds.toList())
                }
            }
        }
        .catch { e ->
            _error.value = "Failed to load photos: ${e.message}"
            println("SmilePile Error: Failed to load photos: ${e.message}")
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Combined UI state - split into two combines due to parameter limit
    private val baseUiState = combine(
        photos,
        categories,
        selectedCategoryIds,
        isLoading,
        error
    ) { photos, categories, selectedCategoryIds, isLoading, error ->
        BaseUiState(photos, categories, selectedCategoryIds, isLoading, error)
    }

    private val selectionState = combine(
        isSelectionMode,
        selectedPhotos,
        isBatchOperationInProgress
    ) { isSelectionMode, selectedPhotos, isBatchOperationInProgress ->
        SelectionState(isSelectionMode, selectedPhotos, isBatchOperationInProgress)
    }

    val uiState: StateFlow<PhotoGalleryUiState> = combine(
        baseUiState,
        selectionState
    ) { base, selection ->
        PhotoGalleryUiState(
            photos = base.photos,
            categories = base.categories,
            selectedCategoryIds = base.selectedCategoryIds,
            selectedCategoryId = base.selectedCategoryIds.firstOrNull(), // For backward compatibility
            isLoading = base.isLoading,
            error = base.error,
            isSelectionMode = selection.isSelectionMode,
            selectedPhotos = selection.selectedPhotos,
            isBatchOperationInProgress = selection.isBatchOperationInProgress
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PhotoGalleryUiState()
    )

    private data class BaseUiState(
        val photos: List<Photo>,
        val categories: List<Category>,
        val selectedCategoryIds: Set<Long>,
        val isLoading: Boolean,
        val error: String?
    )

    private data class SelectionState(
        val isSelectionMode: Boolean,
        val selectedPhotos: Set<Long>,
        val isBatchOperationInProgress: Boolean
    )

    fun selectCategory(categoryId: Long?) {
        viewModelScope.launch {
            try {
                if (categoryId == null) {
                    // Clear all filters
                    _selectedCategoryIds.value = emptySet()
                    _selectedCategoryId.value = null
                } else {
                    // Single category selection
                    _selectedCategoryIds.value = setOf(categoryId)
                    _selectedCategoryId.value = categoryId
                }
                _error.value = null

                // Log category change for debugging
                println("SmilePile Debug: Selected category changed to: $categoryId")
            } catch (e: Exception) {
                _error.value = "Failed to select category: ${e.message}"
                println("SmilePile Error: Failed to select category: ${e.message}")
            }
        }
    }

    fun toggleCategoryFilter(categoryId: Long) {
        viewModelScope.launch {
            try {
                val currentSelection = _selectedCategoryIds.value.toMutableSet()
                if (currentSelection.contains(categoryId)) {
                    currentSelection.remove(categoryId)
                } else {
                    currentSelection.add(categoryId)
                }
                _selectedCategoryIds.value = currentSelection

                // Update single category for backward compatibility
                _selectedCategoryId.value = currentSelection.firstOrNull()

                _error.value = null
                println("SmilePile Debug: Category filters updated to: $currentSelection")
            } catch (e: Exception) {
                _error.value = "Failed to toggle category filter: ${e.message}"
            }
        }
    }

    fun clearCategoryFilters() {
        _selectedCategoryIds.value = emptySet()
        _selectedCategoryId.value = null
    }

    fun selectAllCategories() {
        viewModelScope.launch {
            val allCategoryIds = categories.value.map { it.id }.toSet()
            _selectedCategoryIds.value = allCategoryIds
            _selectedCategoryId.value = allCategoryIds.firstOrNull()
        }
    }


    fun removePhotoFromLibrary(photo: Photo) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                photoRepository.removeFromLibrary(photo)
            } catch (e: Exception) {
                _error.value = "Failed to remove photo from library: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun movePhotoToCategory(photo: Photo, newCategoryId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val updatedPhoto = photo.copy(categoryId = newCategoryId)
                photoRepository.updatePhoto(updatedPhoto)
            } catch (e: Exception) {
                _error.value = "Failed to move photo: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Selection mode methods
    fun enterSelectionMode() {
        _isSelectionMode.value = true
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedPhotos.value = emptySet()
    }

    fun togglePhotoSelection(photoId: Long) {
        val currentSelection = _selectedPhotos.value.toMutableSet()
        if (currentSelection.contains(photoId)) {
            currentSelection.remove(photoId)
        } else {
            currentSelection.add(photoId)
        }
        _selectedPhotos.value = currentSelection

        // Exit selection mode if no photos are selected
        if (currentSelection.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    fun selectAllPhotos() {
        val allPhotoIds = uiState.value.photos.map { it.id }.toSet()
        _selectedPhotos.value = allPhotoIds
    }

    fun deselectAllPhotos() {
        _selectedPhotos.value = emptySet()
        _isSelectionMode.value = false
    }

    // Batch operations
    fun removeSelectedPhotosFromLibrary() {
        viewModelScope.launch {
            try {
                _isBatchOperationInProgress.value = true
                val photosToRemove = uiState.value.photos.filter {
                    _selectedPhotos.value.contains(it.id)
                }

                val result = photoOperationsManager.removeFromLibrary(photosToRemove)

                if (result.isCompleteSuccess) {
                    _error.value = "Successfully removed ${result.successCount} photos from library"
                } else {
                    _error.value = "Removed ${result.successCount} photos from library, failed to remove ${result.failureCount} photos"
                }

                exitSelectionMode()
            } catch (e: Exception) {
                _error.value = "Failed to remove photos from library: ${e.message}"
            } finally {
                _isBatchOperationInProgress.value = false
            }
        }
    }

    fun moveSelectedPhotosToCategory(categoryId: Long) {
        viewModelScope.launch {
            try {
                _isBatchOperationInProgress.value = true
                val photosToMove = uiState.value.photos.filter {
                    _selectedPhotos.value.contains(it.id)
                }

                val result = photoOperationsManager.movePhotosToCategory(photosToMove, categoryId)

                if (result.isCompleteSuccess) {
                    _error.value = "Successfully moved ${result.successCount} photos"
                } else {
                    _error.value = "Moved ${result.successCount} photos, failed to move ${result.failureCount} photos"
                }

                exitSelectionMode()
            } catch (e: Exception) {
                _error.value = "Failed to move photos: ${e.message}"
            } finally {
                _isBatchOperationInProgress.value = false
            }
        }
    }

    fun assignSelectedPhotosToCategories(categoryIds: List<Long>) {
        viewModelScope.launch {
            try {
                _isBatchOperationInProgress.value = true
                val photosToAssign = uiState.value.photos.filter {
                    _selectedPhotos.value.contains(it.id)
                }

                photosToAssign.forEach { photo ->
                    categoryRepository.assignPhotoToCategories(photo.id.toString(), categoryIds)
                }

                _error.value = "Successfully assigned ${photosToAssign.size} photos to ${categoryIds.size} categories"
                exitSelectionMode()
            } catch (e: Exception) {
                _error.value = "Failed to assign photos to categories: ${e.message}"
            } finally {
                _isBatchOperationInProgress.value = false
            }
        }
    }


    fun clearError() {
        _error.value = null
    }
}

data class PhotoGalleryUiState(
    val photos: List<Photo> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryIds: Set<Long> = emptySet(),
    val selectedCategoryId: Long? = null, // For backward compatibility
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSelectionMode: Boolean = false,
    val selectedPhotos: Set<Long> = emptySet(),
    val isBatchOperationInProgress: Boolean = false
) {
    val selectedPhotosCount: Int
        get() = selectedPhotos.size

    val hasSelectedPhotos: Boolean
        get() = selectedPhotos.isNotEmpty()

    val isAllPhotosSelected: Boolean
        get() = photos.isNotEmpty() && selectedPhotos.size == photos.size
}