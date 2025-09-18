package com.smilepile.app.models

/**
 * Represents a category for organizing photos in the SmilePile app.
 * Categories group related photos together and provide child-friendly organization.
 */
data class Category(
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
}