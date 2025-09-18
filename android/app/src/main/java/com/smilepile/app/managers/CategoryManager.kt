package com.smilepile.app.managers

import com.smilepile.app.models.Category
import com.smilepile.app.models.Photo
import com.smilepile.app.models.CategoryWithPhotos

/**
 * Manages categories and photos in the SmilePile app.
 * Provides in-memory storage and operations for category and photo data.
 */
class CategoryManager {
    private val categories = mutableMapOf<String, Category>()
    private val photos = mutableMapOf<String, Photo>()
    private val categoryPhotos = mutableMapOf<String, MutableList<String>>()

    init {
        initializeSampleData()
    }

    /**
     * Gets all categories sorted by position
     */
    fun getCategories(): List<Category> {
        return categories.values.sortedBy { it.position }
    }

    /**
     * Gets a category by ID
     */
    fun getCategory(categoryId: String): Category? {
        return categories[categoryId]
    }

    /**
     * Gets all photos for a specific category
     */
    fun getPhotosForCategory(categoryId: String): List<Photo> {
        val photoIds = categoryPhotos[categoryId] ?: return emptyList()
        return photoIds.mapNotNull { photos[it] }.sortedBy { it.position }
    }

    /**
     * Gets a category with its photos
     */
    fun getCategoryWithPhotos(categoryId: String): CategoryWithPhotos? {
        val category = getCategory(categoryId) ?: return null
        val categoryPhotos = getPhotosForCategory(categoryId)
        return CategoryWithPhotos(category, categoryPhotos)
    }

    /**
     * Gets all categories with their photos
     */
    fun getAllCategoriesWithPhotos(): List<CategoryWithPhotos> {
        return getCategories().map { category ->
            CategoryWithPhotos(category, getPhotosForCategory(category.id))
        }
    }

    /**
     * Gets all photos across all categories
     */
    fun getAllPhotos(): List<Photo> {
        return photos.values.sortedWith(compareBy({ it.categoryId }, { it.position }))
    }

    /**
     * Gets all photo paths for use with ImagePagerAdapter (maintains backward compatibility)
     */
    fun getAllPhotoPaths(): List<String> {
        return getAllPhotos().map { it.getAssetPath() }
    }

    /**
     * Adds a new category
     */
    fun addCategory(category: Category): Boolean {
        if (!category.isValid() || categories.containsKey(category.id)) {
            return false
        }
        categories[category.id] = category
        categoryPhotos[category.id] = mutableListOf()
        return true
    }

    /**
     * Adds a new photo to a category
     */
    fun addPhoto(photo: Photo): Boolean {
        if (!photo.isValid() || photos.containsKey(photo.id) || !categories.containsKey(photo.categoryId)) {
            return false
        }
        photos[photo.id] = photo
        categoryPhotos[photo.categoryId]?.add(photo.id)

        // Update category photo count
        val category = categories[photo.categoryId]
        if (category != null) {
            val updatedCategory = category.copy(photoCount = getPhotosForCategory(photo.categoryId).size)
            categories[photo.categoryId] = updatedCategory
        }
        return true
    }

    /**
     * Removes a category and all its photos
     */
    fun removeCategory(categoryId: String): Boolean {
        if (!categories.containsKey(categoryId)) {
            return false
        }

        // Remove all photos in this category
        categoryPhotos[categoryId]?.forEach { photoId ->
            photos.remove(photoId)
        }

        // Remove category and its photo mapping
        categories.remove(categoryId)
        categoryPhotos.remove(categoryId)
        return true
    }

    /**
     * Removes a photo from its category
     */
    fun removePhoto(photoId: String): Boolean {
        val photo = photos[photoId] ?: return false

        photos.remove(photoId)
        categoryPhotos[photo.categoryId]?.remove(photoId)

        // Update category photo count
        val category = categories[photo.categoryId]
        if (category != null) {
            val updatedCategory = category.copy(photoCount = getPhotosForCategory(photo.categoryId).size)
            categories[photo.categoryId] = updatedCategory
        }
        return true
    }

    /**
     * Initializes the manager with sample data distributed across 3 categories
     */
    private fun initializeSampleData() {
        // Sample image files from assets/sample_images/
        val sampleImages = listOf(
            "sample_1.png", "sample_2.png", "sample_3.png",
            "sample_4.png", "sample_5.png", "sample_6.png"
        )

        // Create 3 sample categories
        val animalsCategory = Category(
            id = "animals",
            name = "animals",
            displayName = "Animals",
            coverImagePath = null,
            description = "Fun animal pictures",
            position = 0
        )

        val familyCategory = Category(
            id = "family",
            name = "family",
            displayName = "Family",
            coverImagePath = null,
            description = "Happy family moments",
            position = 1
        )

        val funTimesCategory = Category(
            id = "fun_times",
            name = "fun_times",
            displayName = "Fun Times",
            coverImagePath = null,
            description = "Good times and memories",
            position = 2
        )

        // Add categories
        addCategory(animalsCategory)
        addCategory(familyCategory)
        addCategory(funTimesCategory)

        // Distribute sample images across categories
        sampleImages.forEachIndexed { index, imageName ->
            val categoryId = when (index % 3) {
                0 -> "animals"
                1 -> "family"
                else -> "fun_times"
            }

            val photo = Photo(
                id = "photo_${index + 1}",
                path = imageName,
                name = imageName.substringBeforeLast('.'),
                categoryId = categoryId,
                position = index / 3,
                isFromAssets = true
            )

            addPhoto(photo)
        }
    }
}