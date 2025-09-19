package com.smilepile.app.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.smilepile.app.models.Category

/**
 * Room entity representing a category for organizing photos in the SmilePile app.
 * Categories group related photos together and provide child-friendly organization.
 */
@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["position"]),
        Index(value = ["name"], unique = true)
    ]
)
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val displayName: String, // Child-friendly label
    val coverImagePath: String?,
    val description: String = "",
    val photoCount: Int = 0,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Validates that the category has required fields
     */
    fun isValid(): Boolean {
        return id.isNotBlank() && name.isNotBlank() && displayName.isNotBlank()
    }

    /**
     * Converts this entity to a domain model
     */
    fun toDomainModel(): Category {
        return Category(
            id = id,
            name = name,
            displayName = displayName,
            coverImagePath = coverImagePath,
            description = description,
            photoCount = photoCount,
            position = position,
            createdAt = createdAt
        )
    }

    companion object {
        /**
         * Creates an entity from a domain model
         */
        fun fromDomainModel(category: Category): CategoryEntity {
            return CategoryEntity(
                id = category.id,
                name = category.name,
                displayName = category.displayName,
                coverImagePath = category.coverImagePath,
                description = category.description,
                photoCount = category.photoCount,
                position = category.position,
                createdAt = category.createdAt
            )
        }
    }
}