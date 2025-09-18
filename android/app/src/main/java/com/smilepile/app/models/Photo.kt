package com.smilepile.app.models

/**
 * Represents a photo within a category in the SmilePile app.
 * Photos can be loaded from assets or potentially other sources in the future.
 */
data class Photo(
    val id: String,
    val path: String,
    val name: String,
    val categoryId: String,
    val position: Int = 0,
    val dateAdded: Long = System.currentTimeMillis(),
    val isFromAssets: Boolean = true
) {
    /**
     * Validates that the photo has required fields
     */
    fun isValid(): Boolean {
        return id.isNotBlank() && path.isNotBlank() && name.isNotBlank() && categoryId.isNotBlank()
    }

    /**
     * Gets the full asset path for loading from assets folder
     */
    fun getAssetPath(): String {
        return if (isFromAssets && !path.startsWith("sample_images/")) {
            "sample_images/$path"
        } else {
            path
        }
    }
}