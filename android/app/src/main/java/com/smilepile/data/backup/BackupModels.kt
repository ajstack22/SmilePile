package com.smilepile.data.backup

import com.smilepile.data.models.Category
import com.smilepile.data.models.Photo
import kotlinx.serialization.Serializable

/**
 * Backup version for compatibility checking
 * Version 1: JSON format
 * Version 2: ZIP format with photos and metadata
 */
const val CURRENT_BACKUP_VERSION = 2

/**
 * Backup format enumeration
 */
enum class BackupFormat {
    JSON,   // Version 1: JSON export without photo files
    ZIP     // Version 2: ZIP export with photo files included
}

/**
 * Root backup data structure containing all app data
 */
@Serializable
data class AppBackup(
    val version: Int = CURRENT_BACKUP_VERSION,
    val exportDate: Long = System.currentTimeMillis(),
    val appVersion: String = "",
    val format: String = BackupFormat.ZIP.name, // Default to ZIP for v2
    val categories: List<BackupCategory>,
    val photos: List<BackupPhoto>,
    val settings: BackupSettings,
    val photoManifest: List<PhotoManifestEntry> = emptyList() // For ZIP format tracking
)

/**
 * Serializable version of Category for backup
 */
@Serializable
data class BackupCategory(
    val id: Long,
    val name: String,
    val displayName: String,
    val position: Int,
    val iconResource: String? = null,
    val colorHex: String? = null,
    val isDefault: Boolean = false,
    val createdAt: Long
) {
    fun toCategory(): Category {
        return Category(
            id = id,
            name = name,
            displayName = displayName,
            position = position,
            iconResource = iconResource,
            colorHex = colorHex,
            isDefault = isDefault,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromCategory(category: Category): BackupCategory {
            return BackupCategory(
                id = category.id,
                name = category.name,
                displayName = category.displayName,
                position = category.position,
                iconResource = category.iconResource,
                colorHex = category.colorHex,
                isDefault = category.isDefault,
                createdAt = category.createdAt
            )
        }
    }
}

/**
 * Serializable version of Photo for backup
 */
@Serializable
data class BackupPhoto(
    val id: Long,
    val path: String,
    val categoryId: Long,
    val name: String,
    val isFromAssets: Boolean = false,
    val createdAt: Long,
    val fileSize: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val isFavorite: Boolean = false
) {
    fun toPhoto(): Photo {
        return Photo(
            id = id,
            path = path,
            categoryId = categoryId,
            name = name,
            isFromAssets = isFromAssets,
            createdAt = createdAt,
            fileSize = fileSize,
            width = width,
            height = height,
            isFavorite = isFavorite
        )
    }

    companion object {
        fun fromPhoto(photo: Photo): BackupPhoto {
            return BackupPhoto(
                id = photo.id,
                path = photo.path,
                categoryId = photo.categoryId,
                name = photo.name,
                isFromAssets = photo.isFromAssets,
                createdAt = photo.createdAt,
                fileSize = photo.fileSize,
                width = photo.width,
                height = photo.height,
                isFavorite = photo.isFavorite
            )
        }
    }
}

/**
 * App settings for backup
 */
@Serializable
data class BackupSettings(
    val isDarkMode: Boolean,
    val securitySettings: BackupSecuritySettings
)

/**
 * Security and parental control settings for backup
 * Note: PINs and patterns are excluded for security reasons
 */
@Serializable
data class BackupSecuritySettings(
    val hasPIN: Boolean,
    val hasPattern: Boolean,
    val kidSafeModeEnabled: Boolean,
    val cameraAccessAllowed: Boolean,
    val deleteProtectionEnabled: Boolean
)

/**
 * Import progress data
 */
data class ImportProgress(
    val totalItems: Int,
    val processedItems: Int,
    val currentOperation: String,
    val errors: List<String> = emptyList()
) {
    val percentage: Int
        get() = if (totalItems > 0) (processedItems * 100) / totalItems else 0
}

/**
 * Photo manifest entry for tracking photos in ZIP format
 * Maps database photo entries to their file locations in the ZIP
 */
@Serializable
data class PhotoManifestEntry(
    val photoId: Long,
    val originalPath: String,
    val zipEntryName: String,
    val fileName: String,
    val fileSize: Long,
    val checksum: String? = null // Optional for integrity verification
)

/**
 * Import result data
 */
data class ImportResult(
    val success: Boolean,
    val categoriesImported: Int,
    val photosImported: Int,
    val photosSkipped: Int,
    val photoFilesRestored: Int = 0, // New for ZIP format
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)