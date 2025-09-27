package com.smilepile.data.repository

import com.smilepile.data.dao.CategoryDao
import com.smilepile.data.dao.PhotoCategoryDao
import com.smilepile.data.entities.CategoryEntity
import com.smilepile.data.entities.PhotoCategoryJoin
import com.smilepile.data.models.Category
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.smilepile.di.IoDispatcher
/**
 * Implementation of CategoryRepository that uses Room database through CategoryDao
 * Handles data mapping between domain models (Category) and database entities (CategoryEntity)
 */
@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val photoCategoryDao: PhotoCategoryDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CategoryRepository {

    /**
     * Maps CategoryEntity to Category domain model
     */
    private fun CategoryEntity.toCategory(): Category {
        return Category(
            id = this.id, // Now directly uses Long
            name = this.displayName.lowercase().replace(" ", "_"), // Generate a normalized name from displayName
            displayName = this.displayName,
            position = this.position,
            iconResource = this.iconName, // Use iconName from entity
            colorHex = this.colorHex,
            isDefault = this.isDefault,
            createdAt = this.createdAt
        )
    }

    /**
     * Maps Category domain model to CategoryEntity
     */
    private fun Category.toCategoryEntity(): CategoryEntity {
        return CategoryEntity(
            id = this.id, // 0L means auto-generate
            displayName = this.displayName,
            colorHex = this.colorHex ?: "#4CAF50", // Default color if none provided
            position = this.position,
            isDefault = this.isDefault,
            createdAt = this.createdAt
        )
    }

    override suspend fun insertCategory(category: Category): Long = withContext(ioDispatcher) {
        try {
            val categoryEntity = category.toCategoryEntity()
            categoryDao.insert(categoryEntity)
        } catch (e: Exception) {
            throw CategoryRepositoryException("Failed to insert category: ${e.message}", e)
        }
    }

    override suspend fun insertCategories(categories: List<Category>): Unit = withContext(ioDispatcher) {
        try {
            val categoryEntities = categories.map { it.toCategoryEntity() }
            categoryDao.insertAll(categoryEntities)
        } catch (e: Exception) {
            throw CategoryRepositoryException("Failed to insert categories: ${e.message}", e)
        }
    }

    override suspend fun updateCategory(category: Category): Unit = withContext(ioDispatcher) {
        try {
            val categoryEntity = category.toCategoryEntity()
            val rowsAffected = categoryDao.update(categoryEntity)
            if (rowsAffected == 0) {
                throw CategoryRepositoryException("Category not found for update: ${category.id}")
            }
        } catch (e: Exception) {
            if (e is CategoryRepositoryException) throw e
            throw CategoryRepositoryException("Failed to update category: ${e.message}", e)
        }
    }

    override suspend fun deleteCategory(category: Category): Unit = withContext(ioDispatcher) {
        try {
            val categoryEntity = category.toCategoryEntity()
            val rowsAffected = categoryDao.delete(categoryEntity)
            if (rowsAffected == 0) {
                throw CategoryRepositoryException("Category not found for deletion: ${category.id}")
            }
        } catch (e: Exception) {
            if (e is CategoryRepositoryException) throw e
            throw CategoryRepositoryException("Failed to delete category: ${e.message}", e)
        }
    }

    override suspend fun getCategoryById(categoryId: Long): Category? = withContext(ioDispatcher) {
        try {
            categoryDao.getById(categoryId)?.toCategory()
        } catch (e: Exception) {
            throw CategoryRepositoryException("Failed to get category by ID: ${e.message}", e)
        }
    }

    override suspend fun getAllCategories(): List<Category> = withContext(ioDispatcher) {
        try {
            // Since CategoryDao.getAll returns Flow, we need to get the first emission
            categoryDao.getAll().first().map { it.toCategory() }
        } catch (e: Exception) {
            throw CategoryRepositoryException("Failed to get all categories: ${e.message}", e)
        }
    }

    override fun getAllCategoriesFlow(): Flow<List<Category>> {
        return categoryDao.getAll().map { categoryEntities ->
            categoryEntities.map { it.toCategory() }
        }
    }

    override suspend fun getCategoryByName(name: String): Category? = withContext(ioDispatcher) {
        try {
            // Convert name to display format for lookup
            val displayName = name.split("_").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
            categoryDao.getByDisplayName(displayName)?.toCategory()
        } catch (e: Exception) {
            throw CategoryRepositoryException("Failed to get category by name: ${e.message}", e)
        }
    }

    override suspend fun initializeDefaultCategories(): Unit = withContext(ioDispatcher) {
        try {
            // Check if categories already exist
            val existingCount = categoryDao.getCount()
            if (existingCount == 0) {
                // Insert default categories
                val defaultCategories = Category.getDefaultCategories()
                insertCategories(defaultCategories)
            }
        } catch (e: Exception) {
            throw CategoryRepositoryException("Failed to initialize default categories: ${e.message}", e)
        }
    }

    override suspend fun getCategoryCount(): Int = withContext(ioDispatcher) {
        try {
            categoryDao.getCount()
        } catch (e: Exception) {
            throw CategoryRepositoryException("Failed to get category count: ${e.message}", e)
        }
    }

    // ===== Batch Operations =====

    override suspend fun assignPhotosToCategory(
        photoIds: List<String>,
        categoryId: Long
    ): Unit = withContext(ioDispatcher) {
        try {
            val joins = photoIds.map { photoId ->
                PhotoCategoryJoin(
                    photoId = photoId,
                    categoryId = categoryId,
                    assignedAt = System.currentTimeMillis()
                )
            }
            photoCategoryDao.insertPhotoCategoryJoins(joins)
        } catch (e: Exception) {
            throw CategoryRepositoryException("Failed to assign photos to category: ${e.message}", e)
        }
    }

    override suspend fun removePhotosFromCategory(
        photoIds: List<String>,
        categoryId: Long
    ): Unit = withContext(ioDispatcher) {
        try {
            photoIds.forEach { photoId ->
                photoCategoryDao.removePhotoFromCategory(photoId, categoryId)
            }
        } catch (e: Exception) {
            throw CategoryRepositoryException("Failed to remove photos from category: ${e.message}", e)
        }
    }

    override suspend fun assignPhotoToCategories(
        photoId: String,
        categoryIds: List<Long>
    ): Unit = withContext(ioDispatcher) {
        try {
            photoCategoryDao.updatePhotoCategories(photoId, categoryIds)
        } catch (e: Exception) {
            throw CategoryRepositoryException("Failed to assign photo to categories: ${e.message}", e)
        }
    }

    override suspend fun getCategoriesForPhoto(photoId: String): List<Category> = withContext(ioDispatcher) {
        try {
            photoCategoryDao.getCategoriesForPhoto(photoId).map { it.toCategory() }
        } catch (e: Exception) {
            throw CategoryRepositoryException("Failed to get categories for photo: ${e.message}", e)
        }
    }

    override fun getCategoriesForPhotoFlow(photoId: String): Flow<List<Category>> {
        return photoCategoryDao.getCategoriesForPhotoFlow(photoId).map { entities ->
            entities.map { it.toCategory() }
        }
    }

    override suspend fun getPhotoCountInCategory(categoryId: Long): Int = withContext(ioDispatcher) {
        try {
            photoCategoryDao.getPhotoCountInCategory(categoryId)
        } catch (e: Exception) {
            throw CategoryRepositoryException("Failed to get photo count in category: ${e.message}", e)
        }
    }
}

/**
 * Custom exception for CategoryRepository operations
 */
class CategoryRepositoryException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)