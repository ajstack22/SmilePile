package com.smilepile.app

/**
 * Data class representing a photo with category information
 */
data class Photo(
    val id: String,
    val path: String,
    val category: String,
    val displayName: String = path.substringAfterLast('/').substringBeforeLast('.')
) {
    companion object {
        /**
         * Creates a list of Photo objects from image paths, inferring categories from file names
         */
        fun fromImagePaths(imagePaths: List<String>): List<Photo> {
            return imagePaths.mapIndexed { index, path ->
                val fileName = path.substringAfterLast('/')
                val category = inferCategoryFromFileName(fileName)
                Photo(
                    id = "photo_$index",
                    path = path,
                    category = category,
                    displayName = fileName.substringBeforeLast('.')
                )
            }
        }

        /**
         * Infers category from file name patterns
         * For now, we'll create simple categories based on file names
         */
        private fun inferCategoryFromFileName(fileName: String): String {
            return when {
                fileName.contains("sample_1") || fileName.contains("sample_2") -> "Animals"
                fileName.contains("sample_3") || fileName.contains("sample_4") -> "Family"
                fileName.contains("sample_5") || fileName.contains("sample_6") -> "Fun Times"
                else -> "General"
            }
        }

        /**
         * Gets all unique categories from a list of photos
         */
        fun getCategories(photos: List<Photo>): List<String> {
            return photos.map { it.category }.distinct().sorted()
        }

        /**
         * Filters photos by category
         */
        fun filterByCategory(photos: List<Photo>, category: String): List<Photo> {
            return photos.filter { it.category == category }
        }
    }
}