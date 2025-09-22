package com.smilepile.data.backup

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.smilepile.data.repository.CategoryRepository
import com.smilepile.data.repository.PhotoRepository
import com.smilepile.security.SecurePreferencesManager
import com.smilepile.theme.ThemeManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for handling app data backup and export functionality
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val categoryRepository: CategoryRepository,
    private val photoRepository: PhotoRepository,
    private val themeManager: ThemeManager,
    private val securePreferencesManager: SecurePreferencesManager
) {

    companion object {
        private const val TAG = "BackupManager"
        private const val BACKUP_MIME_TYPE = "application/json"
        private const val BACKUP_FILE_EXTENSION = ".json"
        private const val SMILEPILE_BACKUP_EXTENSION = ".smilepile"
        private const val MIN_SUPPORTED_VERSION = 1
        private const val MAX_SUPPORTED_VERSION = CURRENT_BACKUP_VERSION
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Export all app data to JSON format using Storage Access Framework
     * @return Result containing success status and any error message
     */
    suspend fun exportToJson(): Result<String> {
        return try {
            // Gather all data using repositories
            val categories = categoryRepository.getAllCategories()
            val photos = photoRepository.getAllPhotos()
            val isDarkMode = themeManager.isDarkMode.first()
            val securitySummary = securePreferencesManager.getSecuritySummary()

            // Convert to backup format using the updated structure from BackupModels.kt
            val backupCategories = categories.map { category ->
                BackupCategory(
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

            val backupPhotos = photos.map { photo ->
                BackupPhoto(
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

            val backupSettings = BackupSettings(
                isDarkMode = isDarkMode,
                securitySettings = BackupSecuritySettings(
                    hasPIN = securitySummary.hasPIN,
                    hasPattern = securitySummary.hasPattern,
                    kidSafeModeEnabled = securitySummary.kidSafeModeEnabled,
                    cameraAccessAllowed = securitySummary.cameraAccessAllowed,
                    deleteProtectionEnabled = securitySummary.deleteProtectionEnabled
                )
            )

            // Get app version from BuildConfig if available
            val appVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: Exception) {
                "unknown"
            }

            // Create backup object using the structure from BackupModels.kt
            val appBackup = AppBackup(
                version = CURRENT_BACKUP_VERSION,
                exportDate = System.currentTimeMillis(),
                appVersion = appVersion,
                categories = backupCategories,
                photos = backupPhotos,
                settings = backupSettings
            )

            // Convert to JSON
            val jsonString = json.encodeToString(appBackup)

            Result.success(jsonString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create an intent for saving backup file using Storage Access Framework
     * @return Intent for file picker
     */
    fun createExportIntent(): Intent {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "smilepile_backup_$timestamp$BACKUP_FILE_EXTENSION"

        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = BACKUP_MIME_TYPE
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
    }

    /**
     * Write JSON content to the selected file URI
     * @param jsonContent The JSON string to write
     * @param uri The file URI obtained from Storage Access Framework
     * @return Result indicating success or failure
     */
    suspend fun writeJsonToFile(jsonContent: String, uri: android.net.Uri): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonContent.toByteArray())
                outputStream.flush()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get backup statistics for user information
     * @return BackupStats containing counts of data to be backed up
     */
    suspend fun getBackupStats(): BackupStats {
        return try {
            val categoryCount = categoryRepository.getCategoryCount()
            val photoCount = photoRepository.getPhotoCount()

            BackupStats(
                categoryCount = categoryCount,
                photoCount = photoCount,
                success = true
            )
        } catch (e: Exception) {
            BackupStats(
                categoryCount = 0,
                photoCount = 0,
                success = false,
                errorMessage = e.message
            )
        }
    }

    /**
     * Import data from JSON backup file
     */
    suspend fun importFromJson(
        backupFile: File,
        strategy: ImportStrategy = ImportStrategy.MERGE
    ): Flow<ImportProgress> = flow {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        var categoriesImported = 0
        var photosImported = 0
        var photosSkipped = 0

        try {
            emit(ImportProgress(1, 0, "Reading backup file"))

            // Read and parse backup file
            val backupJson = withContext(Dispatchers.IO) {
                if (!backupFile.exists()) {
                    throw FileNotFoundException("Backup file not found: ${backupFile.absolutePath}")
                }
                backupFile.readText()
            }

            val backupData = try {
                json.decodeFromString<AppBackup>(backupJson)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid backup file format: ${e.message}")
            }

            emit(ImportProgress(1, 0, "Validating backup"))

            // Validate backup version
            checkBackupVersion(backupData.version)

            val totalItems = backupData.categories.size + backupData.photos.size
            var processedItems = 0

            emit(ImportProgress(totalItems, processedItems, "Starting import"))

            // Handle strategy
            if (strategy == ImportStrategy.REPLACE) {
                emit(ImportProgress(totalItems, processedItems, "Clearing existing data"))
                clearAllData()
            }

            // Import categories first
            emit(ImportProgress(totalItems, processedItems, "Importing categories"))
            for (categoryBackup in backupData.categories) {
                try {
                    val existingCategory = categoryRepository.getCategoryByName(categoryBackup.name)

                    if (strategy == ImportStrategy.MERGE && existingCategory != null) {
                        // Update existing category
                        val updatedCategory = categoryBackup.toCategory().copy(id = existingCategory.id)
                        categoryRepository.updateCategory(updatedCategory)
                        warnings.add("Updated existing category: ${categoryBackup.displayName}")
                    } else {
                        // Insert new category
                        val categoryToInsert = if (strategy == ImportStrategy.REPLACE) {
                            categoryBackup.toCategory()
                        } else {
                            categoryBackup.toCategory().copy(id = 0) // Let Room auto-generate ID for merge
                        }
                        categoryRepository.insertCategory(categoryToInsert)
                        categoriesImported++
                    }
                } catch (e: Exception) {
                    errors.add("Failed to import category '${categoryBackup.displayName}': ${e.message}")
                    Log.e(TAG, "Error importing category: ${categoryBackup.displayName}", e)
                }

                processedItems++
                emit(ImportProgress(totalItems, processedItems, "Importing categories", errors))
            }

            // Import photos
            emit(ImportProgress(totalItems, processedItems, "Importing photos"))
            for (photoBackup in backupData.photos) {
                try {
                    // Validate MediaStore URI if photo is not from assets
                    if (!photoBackup.isFromAssets) {
                        val isValid = validateMediaStoreUri(photoBackup.path)
                        if (!isValid) {
                            photosSkipped++
                            warnings.add("Skipped missing photo: ${photoBackup.name}")
                            processedItems++
                            continue
                        }
                    }

                    // Check for duplicates in merge mode
                    if (strategy == ImportStrategy.MERGE) {
                        val existingPhotos = photoRepository.getAllPhotos()
                        val isDuplicate = existingPhotos.any { it.path == photoBackup.path }

                        if (isDuplicate) {
                            photosSkipped++
                            warnings.add("Skipped duplicate photo: ${photoBackup.name}")
                            processedItems++
                            continue
                        }
                    }

                    // Verify category exists
                    val categoryExists = if (strategy == ImportStrategy.REPLACE) {
                        // In replace mode, categories should have been imported with their original IDs
                        categoryRepository.getCategoryById(photoBackup.categoryId) != null
                    } else {
                        // In merge mode, we need to find the category by name since IDs might have changed
                        val categoryBackupForPhoto = backupData.categories.find { it.id == photoBackup.categoryId }
                        if (categoryBackupForPhoto != null) {
                            categoryRepository.getCategoryByName(categoryBackupForPhoto.name) != null
                        } else {
                            false
                        }
                    }

                    if (!categoryExists) {
                        errors.add("Category not found for photo: ${photoBackup.name}")
                        processedItems++
                        continue
                    }

                    // Get the actual category ID for merge mode
                    val actualCategoryId = if (strategy == ImportStrategy.REPLACE) {
                        photoBackup.categoryId
                    } else {
                        val categoryBackupForPhoto = backupData.categories.find { it.id == photoBackup.categoryId }
                        categoryBackupForPhoto?.let {
                            categoryRepository.getCategoryByName(it.name)?.id
                        } ?: photoBackup.categoryId
                    }

                    // Insert photo
                    val photoToInsert = if (strategy == ImportStrategy.REPLACE) {
                        photoBackup.toPhoto()
                    } else {
                        photoBackup.toPhoto().copy(
                            id = 0, // Let Room auto-generate ID for merge
                            categoryId = actualCategoryId
                        )
                    }

                    photoRepository.insertPhoto(photoToInsert)
                    photosImported++

                } catch (e: Exception) {
                    errors.add("Failed to import photo '${photoBackup.name}': ${e.message}")
                    Log.e(TAG, "Error importing photo: ${photoBackup.name}", e)
                }

                processedItems++
                emit(ImportProgress(totalItems, processedItems, "Importing photos", errors))
            }

            // Final progress update
            emit(ImportProgress(
                totalItems,
                processedItems,
                "Import completed",
                errors
            ))

            Log.i(TAG, "Import completed: $categoriesImported categories, $photosImported photos imported, $photosSkipped photos skipped")

        } catch (e: Exception) {
            errors.add("Import failed: ${e.message}")
            Log.e(TAG, "Import failed", e)
            emit(ImportProgress(0, 0, "Import failed", errors))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Check if backup version is compatible
     */
    fun checkBackupVersion(version: Int) {
        if (version < MIN_SUPPORTED_VERSION || version > MAX_SUPPORTED_VERSION) {
            throw IllegalArgumentException(
                "Unsupported backup version: $version. " +
                "Supported versions: $MIN_SUPPORTED_VERSION-$MAX_SUPPORTED_VERSION"
            )
        }
    }

    /**
     * Validate if MediaStore URI still exists
     */
    suspend fun validateMediaStoreUri(uriString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)

            // For MediaStore URIs, check if the file still exists
            if (uriString.startsWith("content://media/")) {
                val contentResolver: ContentResolver = context.contentResolver
                contentResolver.query(
                    uri,
                    arrayOf(MediaStore.Images.Media._ID),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    cursor.count > 0
                } ?: false
            } else {
                // For file URIs, check if file exists
                val file = File(uri.path ?: uriString)
                file.exists() && file.isFile
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to validate URI: $uriString", e)
            false
        }
    }

    /**
     * Handle missing photos by logging and providing user feedback
     */
    fun handleMissingPhotos(missingPhotos: List<BackupPhoto>): List<String> {
        val warnings = mutableListOf<String>()

        missingPhotos.forEach { photo ->
            val warningMessage = "Photo no longer exists: ${photo.name} (${photo.path})"
            warnings.add(warningMessage)
            Log.w(TAG, "Missing photo during import: ${photo.name} at ${photo.path}")
        }

        return warnings
    }

    /**
     * Clear all existing data (for REPLACE strategy)
     */
    private suspend fun clearAllData() {
        try {
            // Delete all photos first (due to foreign key constraints)
            val allPhotos = photoRepository.getAllPhotos()
            allPhotos.forEach { photo ->
                photoRepository.deletePhoto(photo)
            }

            // Delete all categories except default ones
            val allCategories = categoryRepository.getAllCategories()
            allCategories.forEach { category ->
                if (!category.isDefault) {
                    categoryRepository.deleteCategory(category)
                }
            }

            Log.i(TAG, "Cleared all existing data")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear existing data", e)
            throw e
        }
    }

    /**
     * Validate backup file before import
     */
    suspend fun validateBackupFile(backupFile: File): Result<AppBackup> = withContext(Dispatchers.IO) {
        try {
            if (!backupFile.exists()) {
                return@withContext Result.failure(FileNotFoundException("Backup file not found"))
            }

            if (!backupFile.name.endsWith(BACKUP_FILE_EXTENSION) &&
                !backupFile.name.endsWith(SMILEPILE_BACKUP_EXTENSION)) {
                return@withContext Result.failure(IllegalArgumentException("Invalid backup file format"))
            }

            val backupJson = backupFile.readText()
            val backupData = json.decodeFromString<AppBackup>(backupJson)

            checkBackupVersion(backupData.version)

            Result.success(backupData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get backup preview information without importing
     */
    suspend fun getBackupPreview(backupFile: File): Result<BackupPreview> = withContext(Dispatchers.IO) {
        try {
            val validationResult = validateBackupFile(backupFile)
            if (validationResult.isFailure) {
                return@withContext Result.failure(validationResult.exceptionOrNull()!!)
            }

            val backupData = validationResult.getOrThrow()
            val missingPhotos = mutableListOf<BackupPhoto>()

            // Check which photos are missing
            for (photo in backupData.photos) {
                if (!photo.isFromAssets && !validateMediaStoreUri(photo.path)) {
                    missingPhotos.add(photo)
                }
            }

            val preview = BackupPreview(
                version = backupData.version,
                exportDate = backupData.exportDate,
                appVersion = backupData.appVersion,
                categoriesCount = backupData.categories.size,
                photosCount = backupData.photos.size,
                missingPhotosCount = missingPhotos.size,
                missingPhotos = missingPhotos.map { "${it.name} (${it.path})" }
            )

            Result.success(preview)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Backup preview information
 */
data class BackupPreview(
    val version: Int,
    val exportDate: Long,
    val appVersion: String,
    val categoriesCount: Int,
    val photosCount: Int,
    val missingPhotosCount: Int,
    val missingPhotos: List<String>
)

/**
 * Data class containing backup statistics
 */
data class BackupStats(
    val categoryCount: Int,
    val photoCount: Int,
    val success: Boolean,
    val errorMessage: String? = null
)