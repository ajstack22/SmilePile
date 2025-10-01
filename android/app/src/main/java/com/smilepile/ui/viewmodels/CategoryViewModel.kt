package com.smilepile.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smilepile.data.models.Category
import com.smilepile.data.repository.CategoryRepository
import com.smilepile.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val photoRepository: PhotoRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _editingCategory = MutableStateFlow<Category?>(null)
    val editingCategory: StateFlow<Category?> = _editingCategory.asStateFlow()

    // Get all categories
    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategoriesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Get categories with photo counts
    @OptIn(ExperimentalCoroutinesApi::class)
    val categoriesWithCounts: StateFlow<List<CategoryWithCount>> = categories
        .flatMapLatest { categoryList ->
            combine(
                categoryList.map { category ->
                    combine(
                        MutableStateFlow(category),
                        MutableStateFlow(0) // We'll calculate this separately
                    ) { cat, count ->
                        CategoryWithCount(cat, count)
                    }
                }
            ) { categoriesWithCounts ->
                categoriesWithCounts.toList()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Initialize and get photo counts for categories
    init {
        refreshCategoriesWithCounts()
    }

    fun refreshCategoriesWithCounts() {
        viewModelScope.launch {
            try {
                val categoryList = categoryRepository.getAllCategories()
                val categoriesWithCounts = categoryList.map { category ->
                    val photoCount = photoRepository.getPhotoCategoryCount(category.id)
                    CategoryWithCount(category, photoCount)
                }
                _categoriesWithCountsInternal.value = categoriesWithCounts
            } catch (e: Exception) {
                _error.value = "Failed to load categories: ${e.message}"
            }
        }
    }

    private val _categoriesWithCountsInternal = MutableStateFlow<List<CategoryWithCount>>(emptyList())
    val categoriesWithCountsFlow: StateFlow<List<CategoryWithCount>> = _categoriesWithCountsInternal.asStateFlow()

    fun showAddCategoryDialog() {
        _editingCategory.value = null
        _showAddDialog.value = true
    }

    fun showEditCategoryDialog(category: Category) {
        _editingCategory.value = category
        _showAddDialog.value = true
    }

    fun hideDialog() {
        _showAddDialog.value = false
        _editingCategory.value = null
    }

    fun addCategory(displayName: String, colorHex: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                if (!validateCategoryName(displayName)) return@launch
                if (!checkForDuplicateName(displayName)) return@launch

                val newCategory = createNewCategory(displayName, colorHex)
                categoryRepository.insertCategory(newCategory)
                hideDialog()
                refreshCategoriesWithCounts()
            } catch (e: Exception) {
                _error.value = "Failed to add category: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun validateCategoryName(displayName: String): Boolean {
        if (displayName.isBlank()) {
            _error.value = "Category name cannot be empty"
            return false
        }
        return true
    }

    private suspend fun checkForDuplicateName(displayName: String, excludeId: Long? = null): Boolean {
        val normalizedName = normalizeDisplayName(displayName)
        val existingCategory = categoryRepository.getCategoryByName(normalizedName)
        if (existingCategory != null && existingCategory.id != excludeId) {
            _error.value = "Category '$displayName' already exists"
            return false
        }
        return true
    }

    private fun normalizeDisplayName(displayName: String): String {
        return displayName.trim().lowercase().replace(" ", "_")
    }

    private suspend fun createNewCategory(displayName: String, colorHex: String): Category {
        val categories = categoryRepository.getAllCategories()
        val nextPosition = (categories.maxOfOrNull { it.position } ?: -1) + 1

        return Category(
            name = normalizeDisplayName(displayName),
            displayName = displayName.trim(),
            position = nextPosition,
            colorHex = colorHex,
            isDefault = false
        )
    }

    fun updateCategory(category: Category, displayName: String, colorHex: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                if (!validateCategoryName(displayName)) return@launch
                if (displayName.trim() != category.displayName) {
                    if (!checkForDuplicateName(displayName, category.id)) return@launch
                }

                val updatedCategory = category.copy(
                    name = normalizeDisplayName(displayName),
                    displayName = displayName.trim(),
                    colorHex = colorHex
                )

                categoryRepository.updateCategory(updatedCategory)
                hideDialog()
                refreshCategoriesWithCounts()
            } catch (e: Exception) {
                _error.value = "Failed to update category: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun canDeleteCategory(category: Category): Pair<Boolean, String?> {
        // Check if this is the last category
        val categoryCount = categoriesWithCountsFlow.value.size
        if (categoryCount <= 1) {
            return Pair(false, "Cannot delete the last remaining category")
        }
        return Pair(true, null)
    }

    fun getCategoryPhotoCount(categoryId: Long, callback: (Int) -> Unit) {
        viewModelScope.launch {
            val count = photoRepository.getPhotoCategoryCount(categoryId)
            callback(count)
        }
    }

    fun deleteCategory(category: Category, deletePhotos: Boolean = false) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Check if this is the last category
                val categoryCount = categoryRepository.getCategoryCount()
                if (categoryCount <= 1) {
                    _error.value = "Cannot delete the last remaining category"
                    return@launch
                }

                // If deletePhotos is true, delete all photos in this category first
                if (deletePhotos) {
                    val photosInCategory = photoRepository.getPhotosByCategory(category.id)
                    photosInCategory.forEach { photo ->
                        photoRepository.deletePhoto(photo)
                    }
                }

                categoryRepository.deleteCategory(category)
                refreshCategoriesWithCounts()
            } catch (e: Exception) {
                _error.value = "Failed to delete category: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    // Predefined colors for category selection
    companion object {
        val PREDEFINED_COLORS = listOf(
            "#4CAF50", // Green
            "#2196F3", // Blue
            "#FF9800", // Orange
            "#9C27B0", // Purple
            "#F44336", // Red
            "#FF5722", // Deep Orange
            "#795548", // Brown
            "#607D8B", // Blue Grey
            "#E91E63", // Pink
            "#009688", // Teal
            "#FFEB3B", // Yellow
            "#3F51B5"  // Indigo
        )
    }
}

data class CategoryWithCount(
    val category: Category,
    val photoCount: Int
)