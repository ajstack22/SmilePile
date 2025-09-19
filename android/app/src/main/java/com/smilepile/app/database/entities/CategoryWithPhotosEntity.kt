package com.smilepile.app.database.entities

import androidx.room.Embedded
import androidx.room.Relation
import com.smilepile.app.models.CategoryWithPhotos

/**
 * Room entity representing a category with its associated photos for easy access and display.
 * This composite data class provides convenient access to category data and photo counts.
 */
data class CategoryWithPhotosEntity(
    @Embedded
    val category: CategoryEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "categoryId"
    )
    val photos: List<PhotoEntity>
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
    val sortedPhotos: List<PhotoEntity> get() = photos.sortedBy { it.position }

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

    /**
     * Converts this entity to a domain model
     */
    fun toDomainModel(): CategoryWithPhotos {
        return CategoryWithPhotos(
            category = category.toDomainModel(),
            photos = photos.map { it.toDomainModel() }
        )
    }

    companion object {
        /**
         * Creates an entity from a domain model
         */
        fun fromDomainModel(categoryWithPhotos: CategoryWithPhotos): CategoryWithPhotosEntity {
            return CategoryWithPhotosEntity(
                category = CategoryEntity.fromDomainModel(categoryWithPhotos.category),
                photos = categoryWithPhotos.photos.map { PhotoEntity.fromDomainModel(it) }
            )
        }
    }
}