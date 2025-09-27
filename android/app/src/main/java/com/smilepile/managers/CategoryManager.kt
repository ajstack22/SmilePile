package com.smilepile.managers

import com.smilepile.data.dao.CategoryDao
import com.smilepile.data.dao.PhotoCategoryDao
import com.smilepile.data.entities.CategoryEntity
import com.smilepile.data.entities.PhotoCategoryJoin
import com.smilepile.data.models.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for comprehensive category operations
 * Handles CRUD operations, photo associations, and batch operations
 */
@Singleton
class CategoryManager @Inject constructor(
    private val categoryDao: CategoryDao,
    private val photoCategoryDao: PhotoCategoryDao
) {

    // ===== Default Categories =====

    companion object {
        val DEFAULT_CATEGORIES = listOf(
            CategoryInfo(
                displayName = "Family",
                colorHex = "#E91E63",
                iconName = "family_restroom",
                position = 0
            ),
            CategoryInfo(
                displayName = "Friends",
                colorHex = "#2196F3",
                iconName = "group",
                position = 1
            ),
            CategoryInfo(
                displayName = "Nature",
                colorHex = "#4CAF50",
                iconName = "forest",
                position = 2
            ),
            CategoryInfo(
                displayName = "Travel",
                colorHex = "#FF9800",
                iconName = "flight_takeoff",
                position = 3
            ),
            CategoryInfo(
                displayName = "Food",
                colorHex = "#FF5722",
                iconName = "restaurant",
                position = 4
            ),
            CategoryInfo(
                displayName = "Events",
                colorHex = "#9C27B0",
                iconName = "celebration",
                position = 5
            ),
            CategoryInfo(
                displayName = "Pets",
                colorHex = "#795548",
                iconName = "pets",
                position = 6
            ),
            CategoryInfo(
                displayName = "Work",
                colorHex = "#607D8B",
                iconName = "work",
                position = 7
            ),
            CategoryInfo(
                displayName = "Sports",
                colorHex = "#00BCD4",
                iconName = "sports_soccer",
                position = 8
            ),
            CategoryInfo(
                displayName = "Art",
                colorHex = "#FFC107",
                iconName = "palette",
                position = 9
            )
        )

        val CATEGORY_COLORS = listOf(
            "#E91E63", // Pink
            "#F44336", // Red
            "#FF5722", // Deep Orange
            "#FF9800", // Orange
            "#FFC107", // Amber
            "#FFEB3B", // Yellow
            "#CDDC39", // Lime
            "#8BC34A", // Light Green
            "#4CAF50", // Green
            "#009688", // Teal
            "#00BCD4", // Cyan
            "#03A9F4", // Light Blue
            "#2196F3", // Blue
            "#3F51B5", // Indigo
            "#673AB7", // Deep Purple
            "#9C27B0", // Purple
            "#795548", // Brown
            "#9E9E9E", // Grey
            "#607D8B"  // Blue Grey
        )

        val CATEGORY_ICONS = listOf(
            "family_restroom",
            "group",
            "person",
            "child_care",
            "face",
            "mood",
            "pets",
            "forest",
            "park",
            "beach_access",
            "flight_takeoff",
            "directions_car",
            "directions_bike",
            "train",
            "restaurant",
            "fastfood",
            "cake",
            "local_cafe",
            "celebration",
            "event",
            "today",
            "schedule",
            "work",
            "business",
            "school",
            "sports_soccer",
            "sports_basketball",
            "sports_tennis",
            "fitness_center",
            "palette",
            "brush",
            "photo_camera",
            "music_note",
            "home",
            "favorite",
            "star",
            "whatshot"
        )
    }

    data class CategoryInfo(
        val displayName: String,
        val colorHex: String,
        val iconName: String? = null,
        val position: Int
    )

    data class CategoryWithPhotos(
        val category: CategoryEntity,
        val photoCount: Int,
        val recentPhotoIds: List<String> = emptyList()
    )

    // ===== CRUD Operations =====

    /**
     * Create a new category
     * @param displayName Display name for the category
     * @param colorHex Color hex code
     * @param iconName Optional material icon name
     * @return The ID of the created category
     */
    suspend fun createCategory(
        displayName: String,
        colorHex: String = CATEGORY_COLORS.random(),
        iconName: String? = null
    ): Long {
        require(displayName.isNotBlank()) { "Category display name cannot be blank" }
        require(colorHex.matches(Regex("^#[0-9A-Fa-f]{6}$"))) { "Invalid color hex format" }

        // Check if category already exists
        if (categoryDao.existsByDisplayName(displayName)) {
            throw IllegalArgumentException("Category '$displayName' already exists")
        }

        val position = categoryDao.getCount()
        val category = CategoryEntity(
            displayName = displayName,
            colorHex = colorHex,
            iconName = iconName,
            position = position,
            isDefault = false
        )

        return categoryDao.insert(category)
    }

    /**
     * Create multiple categories at once
     * @param categories List of CategoryInfo
     * @return List of created category IDs
     */
    suspend fun createCategories(categories: List<CategoryInfo>): List<Long> {
        val entities = categories.mapIndexed { index, info ->
            CategoryEntity(
                displayName = info.displayName,
                colorHex = info.colorHex,
                iconName = info.iconName,
                position = info.position,
                isDefault = false
            )
        }
        return categoryDao.insertAll(entities)
    }

    /**
     * Initialize default categories if none exist
     */
    suspend fun initializeDefaultCategories() {
        if (categoryDao.getCount() == 0) {
            val entities = DEFAULT_CATEGORIES.map { info ->
                CategoryEntity(
                    displayName = info.displayName,
                    colorHex = info.colorHex,
                    iconName = info.iconName,
                    position = info.position,
                    isDefault = true
                )
            }
            categoryDao.insertAll(entities)
        }
    }

    /**
     * Update an existing category
     * @param categoryId The category ID
     * @param displayName New display name
     * @param colorHex New color hex code
     * @param iconName New icon name
     */
    suspend fun updateCategory(
        categoryId: Long,
        displayName: String? = null,
        colorHex: String? = null,
        iconName: String? = null
    ): Boolean {
        val category = categoryDao.getById(categoryId) ?: return false

        // Check for duplicate name if updating display name
        if (displayName != null && displayName != category.displayName) {
            if (categoryDao.existsByDisplayNameExcludingId(displayName, categoryId)) {
                throw IllegalArgumentException("Category '$displayName' already exists")
            }
        }

        val updatedCategory = category.copy(
            displayName = displayName ?: category.displayName,
            colorHex = colorHex ?: category.colorHex,
            iconName = iconName ?: category.iconName
        )

        return categoryDao.update(updatedCategory) > 0
    }

    /**
     * Delete a category
     * @param categoryId The category ID
     * @param reassignToCategory Optional category to reassign photos to
     * @return True if deleted successfully
     */
    suspend fun deleteCategory(categoryId: Long, reassignToCategory: Long? = null): Boolean {
        // Prevent deleting last category
        if (categoryDao.getCount() <= 1) {
            throw IllegalStateException("Cannot delete the last category")
        }

        // Reassign photos if specified
        if (reassignToCategory != null) {
            val photosToReassign = photoCategoryDao.getPhotosInCategory(categoryId)
            photosToReassign.forEach { photo ->
                photoCategoryDao.removePhotoFromCategory(photo.id, categoryId)
                photoCategoryDao.insertPhotoCategoryJoin(
                    PhotoCategoryJoin(
                        photoId = photo.id,
                        categoryId = reassignToCategory
                    )
                )
            }
        }

        return categoryDao.deleteById(categoryId) > 0
    }

    // ===== Photo-Category Association =====

    /**
     * Assign a photo to categories
     * @param photoId The photo ID
     * @param categoryIds List of category IDs
     * @param primaryCategoryId Optional primary category ID
     */
    suspend fun assignPhotoToCategories(
        photoId: String,
        categoryIds: List<Long>,
        primaryCategoryId: Long? = null
    ) {
        require(categoryIds.isNotEmpty()) { "Must specify at least one category" }

        photoCategoryDao.updatePhotoCategories(photoId, categoryIds)

        // Set primary category if specified
        if (primaryCategoryId != null && primaryCategoryId in categoryIds) {
            photoCategoryDao.setPrimaryCategory(photoId, primaryCategoryId)
        } else if (categoryIds.size == 1) {
            // If only one category, make it primary
            photoCategoryDao.setPrimaryCategory(photoId, categoryIds.first())
        }
    }

    /**
     * Batch assign multiple photos to a category
     * @param photoIds List of photo IDs
     * @param categoryId The category ID
     */
    suspend fun batchAssignPhotosToCategory(
        photoIds: List<String>,
        categoryId: Long
    ) {
        require(photoIds.isNotEmpty()) { "Must specify at least one photo" }
        photoCategoryDao.assignPhotosToCategory(photoIds, categoryId)
    }

    /**
     * Remove photos from a category
     * @param photoIds List of photo IDs
     * @param categoryId The category ID
     */
    suspend fun removePhotosFromCategory(
        photoIds: List<String>,
        categoryId: Long
    ) {
        photoIds.forEach { photoId ->
            photoCategoryDao.removePhotoFromCategory(photoId, categoryId)
        }
    }

    /**
     * Move photos from one category to another
     * @param photoIds List of photo IDs
     * @param fromCategoryId Source category ID
     * @param toCategoryId Destination category ID
     */
    suspend fun movePhotosToCategory(
        photoIds: List<String>,
        fromCategoryId: Long,
        toCategoryId: Long
    ) {
        photoIds.forEach { photoId ->
            photoCategoryDao.removePhotoFromCategory(photoId, fromCategoryId)
            photoCategoryDao.insertPhotoCategoryJoin(
                PhotoCategoryJoin(
                    photoId = photoId,
                    categoryId = toCategoryId
                )
            )
        }
    }

    // ===== Query Operations =====

    /**
     * Get all categories
     * @return Flow of all categories
     */
    fun getAllCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getAll()
    }

    /**
     * Get all categories with photo counts
     * @return Flow of categories with photo counts
     */
    fun getCategoriesWithCounts(): Flow<List<CategoryWithPhotos>> {
        return categoryDao.getAll().map { categories ->
            categories.map { category ->
                CategoryWithPhotos(
                    category = category,
                    photoCount = photoCategoryDao.getPhotoCountInCategory(category.id)
                )
            }
        }
    }

    /**
     * Search categories by name
     * @param query Search query
     * @return Flow of matching categories
     */
    fun searchCategories(query: String): Flow<List<CategoryEntity>> {
        return if (query.isBlank()) {
            categoryDao.getAll()
        } else {
            categoryDao.searchByDisplayName(query)
        }
    }

    /**
     * Get categories for a photo
     * @param photoId The photo ID
     * @return List of categories
     */
    suspend fun getCategoriesForPhoto(photoId: String): List<CategoryEntity> {
        return photoCategoryDao.getCategoriesForPhoto(photoId)
    }

    /**
     * Get categories for a photo as Flow
     * @param photoId The photo ID
     * @return Flow of categories
     */
    fun getCategoriesForPhotoFlow(photoId: String): Flow<List<CategoryEntity>> {
        return photoCategoryDao.getCategoriesForPhotoFlow(photoId)
    }

    /**
     * Get primary category for a photo
     * @param photoId The photo ID
     * @return Primary category or null
     */
    suspend fun getPrimaryCategory(photoId: String): CategoryEntity? {
        return photoCategoryDao.getPrimaryCategory(photoId)
    }

    /**
     * Check if a photo belongs to a category
     * @param photoId The photo ID
     * @param categoryId The category ID
     * @return True if photo belongs to category
     */
    suspend fun isPhotoInCategory(photoId: String, categoryId: Long): Boolean {
        return photoCategoryDao.isPhotoInCategory(photoId, categoryId)
    }

    /**
     * Get uncategorized photo count
     * @return Number of uncategorized photos
     */
    suspend fun getUncategorizedPhotoCount(): Int {
        return photoCategoryDao.getUncategorizedPhotos().size
    }

    // ===== Reorder Categories =====

    /**
     * Reorder categories
     * @param categoryIds List of category IDs in new order
     */
    suspend fun reorderCategories(categoryIds: List<Long>) {
        categoryIds.forEachIndexed { index, categoryId ->
            val category = categoryDao.getById(categoryId)
            category?.let {
                categoryDao.update(it.copy(position = index))
            }
        }
    }
}