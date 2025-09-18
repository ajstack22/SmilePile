package com.smilepile.app.models

/**
 * Represents a category with its associated photos for easy access and display.
 * This composite data class provides convenient access to category data and photo counts.
 */
data class CategoryWithPhotos(
    val category: Category,
    val photos: List<Photo>
) {
    /**
     * Returns the actual number of photos in this category
     */
    val photoCount: Int get() = photos.size

    /**
     * Returns the cover image path - either the first photo or the category's set cover image
     */
    val coverImage: String? get() = photos.firstOrNull()?.path ?: category.coverImagePath

    /**
     * Returns photos sorted by position
     */
    val sortedPhotos: List<Photo> get() = photos.sortedBy { it.position }

    /**
     * Validates that the category and all photos are valid
     */
    fun isValid(): Boolean {
        return category.isValid() && photos.all { it.isValid() && it.categoryId == category.id }
    }

    /**
     * Gets all photo paths for use with ImagePagerAdapter
     */
    fun getPhotoPaths(): List<String> {
        return sortedPhotos.map { it.getAssetPath() }
    }
}