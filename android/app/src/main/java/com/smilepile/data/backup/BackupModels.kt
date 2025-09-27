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
    val height: Int = 0
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
            height = height
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
                height = photo.height
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

/**
 * Backup options for selective backup
 */
@Serializable
data class BackupOptions(
    val includePhotos: Boolean = true,
    val includeThumbnails: Boolean = true,
    val includeSettings: Boolean = true,
    val selectedCategories: List<Long>? = null, // null means all categories
    val dateRangeStart: Long? = null,
    val dateRangeEnd: Long? = null,
    val compressionLevel: CompressionLevel = CompressionLevel.MEDIUM,
    val encryptSensitiveData: Boolean = true,
    val includeMetadata: Boolean = true
)

/**
 * Compression levels for backup
 */
enum class CompressionLevel {
    LOW,    // Fast compression, larger file size
    MEDIUM, // Balanced compression
    HIGH    // Best compression, slower
}

/**
 * Export format options
 */
enum class ExportFormat {
    ZIP,            // Standard ZIP with photos
    JSON,           // JSON metadata only
    HTML_GALLERY,   // HTML gallery with thumbnails
    PDF_CATALOG     // PDF catalog format
}

/**
 * Restore options for import process
 */
@Serializable
data class RestoreOptions(
    val strategy: ImportStrategy = ImportStrategy.MERGE,
    val duplicateResolution: DuplicateResolution = DuplicateResolution.SKIP,
    val validateIntegrity: Boolean = true,
    val restoreThumbnails: Boolean = true,
    val restoreSettings: Boolean = true,
    val dryRun: Boolean = false // Preview without actually importing
)

/**
 * Duplicate resolution strategy
 */
enum class DuplicateResolution {
    SKIP,           // Skip duplicate photos
    REPLACE,        // Replace with imported version
    RENAME,         // Rename imported photo
    ASK_USER        // Ask user for each duplicate
}

/**
 * Backup schedule configuration
 */
@Serializable
data class BackupSchedule(
    val enabled: Boolean = false,
    val frequency: BackupFrequency = BackupFrequency.WEEKLY,
    val time: String = "02:00", // Time in HH:mm format
    val dayOfWeek: Int = 1, // 1-7 for weekly
    val dayOfMonth: Int = 1, // 1-31 for monthly
    val wifiOnly: Boolean = true,
    val chargeOnly: Boolean = true,
    val lastBackupTime: Long? = null,
    val nextScheduledTime: Long? = null
)

/**
 * Backup frequency options
 */
enum class BackupFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    MANUAL
}

/**
 * Backup history entry
 */
@Serializable
data class BackupHistoryEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long,
    val fileName: String,
    val filePath: String?,
    val fileSize: Long,
    val format: BackupFormat,
    val photosCount: Int,
    val categoriesCount: Int,
    val compressionLevel: CompressionLevel,
    val success: Boolean,
    val errorMessage: String? = null,
    val automatic: Boolean = false
)

/**
 * Incremental backup metadata
 */
@Serializable
data class IncrementalBackupMetadata(
    val baseBackupId: String,
    val baseBackupDate: Long,
    val changedPhotos: List<Long>,
    val deletedPhotos: List<Long>,
    val changedCategories: List<Long>,
    val deletedCategories: List<Long>,
    val incrementalDate: Long = System.currentTimeMillis()
)

/**
 * Backup validation result
 */
data class BackupValidationResult(
    val isValid: Boolean,
    val version: Int,
    val format: BackupFormat,
    val hasMetadata: Boolean,
    val hasPhotos: Boolean,
    val photosCount: Int,
    val categoriesCount: Int,
    val integrityCheckPassed: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * Export progress data
 */
data class ExportProgress(
    val totalItems: Int,
    val processedItems: Int,
    val currentOperation: String,
    val currentFile: String? = null,
    val bytesProcessed: Long = 0,
    val totalBytes: Long = 0,
    val errors: List<String> = emptyList()
) {
    val percentage: Int
        get() = if (totalItems > 0) (processedItems * 100) / totalItems else 0

    val bytesPercentage: Int
        get() = if (totalBytes > 0) ((bytesProcessed * 100) / totalBytes).toInt() else 0
}