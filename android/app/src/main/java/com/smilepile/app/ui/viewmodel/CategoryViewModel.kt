package com.smilepile.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.smilepile.app.data.repository.CategoryRepository
import com.smilepile.app.data.database.entities.Category
import kotlinx.coroutines.flow.Flow

/**
 * ViewModel for Category operations
 *
 * Manages UI-related data for categories and provides reactive data streams
 * for ViewPager2 implementation
 */
class CategoryViewModel(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    /**
     * Get all categories as LiveData for UI binding
     */
    val categories = categoryRepository.getAllCategoriesFlow().asLiveData()

    /**
     * Get all categories as Flow for reactive updates
     */
    fun getCategoriesFlow(): Flow<List<Category>> {
        return categoryRepository.getAllCategoriesFlow()
    }

    /**
     * Get category by ID
     */
    suspend fun getCategoryById(categoryId: Long): Category? {
        return categoryRepository.getCategoryById(categoryId)
    }

    /**
     * Update photo count for category
     */
    suspend fun updatePhotoCount(categoryId: Long, count: Int) {
        categoryRepository.updatePhotoCount(categoryId, count)
    }
}

/**
 * Factory for creating CategoryViewModel instances
 */
class CategoryViewModelFactory(
    private val categoryRepository: CategoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CategoryViewModel(categoryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}