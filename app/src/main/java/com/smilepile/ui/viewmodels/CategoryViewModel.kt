package com.smilepile.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smilepile.database.entities.Category
import com.smilepile.database.repository.CategoryRepository
import com.smilepile.database.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for CategoryFragment
 *
 * Manages category data and photo count for full-screen category display
 * Designed for child-friendly category browsing in F0011
 */
@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val photoRepository: PhotoRepository
) : ViewModel() {

    private val _category = MutableStateFlow<Category?>(null)
    val category: StateFlow<Category?> = _category.asStateFlow()

    private val _photoCount = MutableStateFlow(0)
    val photoCount: StateFlow<Int> = _photoCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Load category by ID and its photo count
     */
    fun loadCategory(categoryId: Long) {
        if (categoryId <= 0) {
            Timber.w("Invalid category ID: $categoryId")
            _error.value = "Invalid category ID"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Load category data
                categoryRepository.getCategoryById(categoryId).fold(
                    onSuccess = { category ->
                        _category.value = category
                        if (category != null) {
                            Timber.d("Loaded category: ${category.name}")
                            // Load photo count for this category
                            loadPhotoCount(categoryId)
                        } else {
                            Timber.w("Category not found for ID: $categoryId")
                            _error.value = "Category not found"
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to load category $categoryId")
                        _error.value = "Failed to load category: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading category $categoryId")
                _error.value = "Error loading category: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load photo count for the category
     */
    private suspend fun loadPhotoCount(categoryId: Long) {
        try {
            categoryRepository.getPhotoCount(categoryId).fold(
                onSuccess = { count ->
                    _photoCount.value = count
                    Timber.d("Category $categoryId has $count photos")
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load photo count for category $categoryId")
                    _photoCount.value = 0
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Error loading photo count for category $categoryId")
            _photoCount.value = 0
        }
    }

    /**
     * Refresh category data
     */
    fun refresh() {
        _category.value?.let { category ->
            loadCategory(category.id)
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }
}