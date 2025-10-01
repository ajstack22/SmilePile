package com.smilepile.data.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.smilepile.data.repository.CategoryRepository
import com.smilepile.data.repository.PhotoRepository
import com.smilepile.security.SecurePreferencesManager
import com.smilepile.storage.ZipUtils
import com.smilepile.theme.ThemeManager
import com.smilepile.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for handling backup restore operations with comprehensive validation and recovery
 */
@Singleton
class RestoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val categoryRepository: CategoryRepository,
    private val photoRepository: PhotoRepository,
    private val themeManager: ThemeManager,
    private val securePreferencesManager: SecurePreferencesManager
) {
    companion object {
        private const val TAG = "RestoreManager"
        private const val MIN_SUPPORTED_VERSION = 1
        private const val MAX_SUPPORTED_VERSION = CURRENT_BACKUP_VERSION
        private const val ROLLBACK_DIR = "rollback_temp"
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Validate backup file with comprehensive integrity checks
     */
    suspend fun validateBackup(
        backupFile: File,
        checkIntegrity: Boolean = true
    ): Result<BackupValidationResult> = withContext(Dispatchers.IO) {
        try {
            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()

            // Check file existence
            if (!backupFile.exists()) {
                return@withContext Result.failure(FileNotFoundException("Backup file not found"))
            }

            // Determine backup type
            val isZipBackup = backupFile.name.endsWith(".zip") || backupFile.name.endsWith(".smilepile")
            val isJsonBackup = backupFile.name.endsWith(".json")

            if (!isZipBackup && !isJsonBackup) {
                return@withContext Result.failure(IllegalArgumentException("Unsupported backup format"))
            }

            if (isZipBackup) {
                validateZipBackup(backupFile, checkIntegrity)
            } else {
                validateJsonBackup(backupFile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate ZIP backup structure and integrity
     */
    private suspend fun validateZipBackup(
        zipFile: File,
        checkIntegrity: Boolean
    ): Result<BackupValidationResult> = withContext(Dispatchers.IO) {
        try {
            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()

            // Validate ZIP structure
            val structureResult = ZipUtils.validateZipStructure(zipFile)
            if (structureResult.isFailure) {
                errors.add("Invalid ZIP structure: ${structureResult.exceptionOrNull()?.message}")
                return@withContext Result.success(
                    BackupValidationResult(
                        isValid = false,
                        version = 0,
                        format = BackupFormat.ZIP,
                        hasMetadata = false,
                        hasPhotos = false,
                        photosCount = 0,
                        categoriesCount = 0,
                        integrityCheckPassed = false,
                        errors = errors
                    )
                )
            }

            // Extract and validate metadata
            val tempDir = File(context.cacheDir, "validate_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            try {
                val extractResult = ZipUtils.extractZip(zipFile, tempDir)
                if (extractResult.isFailure) {
                    errors.add("Failed to extract ZIP: ${extractResult.exceptionOrNull()?.message}")
                    return@withContext Result.success(
                        BackupValidationResult(
                            isValid = false,
                            version = 0,
                            format = BackupFormat.ZIP,
                            hasMetadata = false,
                            hasPhotos = false,
                            photosCount = 0,
                            categoriesCount = 0,
                            integrityCheckPassed = false,
                            errors = errors
                        )
                    )
                }

                // Read metadata
                val metadataFile = File(tempDir, ZipUtils.METADATA_FILE)
                if (!metadataFile.exists()) {
                    errors.add("metadata.json not found in backup")
                    return@withContext Result.success(
                        BackupValidationResult(
                            isValid = false,
                            version = 0,
                            format = BackupFormat.ZIP,
                            hasMetadata = false,
                            hasPhotos = false,
                            photosCount = 0,
                            categoriesCount = 0,
                            integrityCheckPassed = false,
                            errors = errors
                        )
                    )
                }

                val backupData = json.decodeFromString<AppBackup>(metadataFile.readText())

                // Check version compatibility
                if (backupData.version < MIN_SUPPORTED_VERSION || backupData.version > MAX_SUPPORTED_VERSION) {
                    errors.add("Unsupported backup version: ${backupData.version}")
                }

                // Check for photos directory
                val photosDir = File(tempDir, "photos")
                val hasPhotos = photosDir.exists() && photosDir.isDirectory

                // Integrity checks if requested
                var integrityPassed = true
                if (checkIntegrity && backupData.photoManifest.isNotEmpty()) {
                    for (manifestEntry in backupData.photoManifest) {
                        val photoFile = File(photosDir, manifestEntry.fileName)
                        if (photoFile.exists()) {
                            if (manifestEntry.checksum != null) {
                                val actualChecksum = calculateMD5(photoFile)
                                if (actualChecksum != manifestEntry.checksum) {
                                    warnings.add("Checksum mismatch for ${manifestEntry.fileName}")
                                    integrityPassed = false
                                }
                            }
                        } else {
                            warnings.add("Missing photo file: ${manifestEntry.fileName}")
                        }
                    }
                }

                Result.success(
                    BackupValidationResult(
                        isValid = errors.isEmpty(),
                        version = backupData.version,
                        format = BackupFormat.ZIP,
                        hasMetadata = true,
                        hasPhotos = hasPhotos,
                        photosCount = backupData.photos.size,
                        categoriesCount = backupData.categories.size,
                        integrityCheckPassed = integrityPassed,
                        errors = errors,
                        warnings = warnings
                    )
                )
            } finally {
                tempDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate JSON backup structure
     */
    private suspend fun validateJsonBackup(
        jsonFile: File
    ): Result<BackupValidationResult> = withContext(Dispatchers.IO) {
        try {
            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()

            val backupJson = jsonFile.readText()
            val backupData = json.decodeFromString<AppBackup>(backupJson)

            // Check version compatibility
            if (backupData.version < MIN_SUPPORTED_VERSION || backupData.version > MAX_SUPPORTED_VERSION) {
                errors.add("Unsupported backup version: ${backupData.version}")
            }

            // Check for missing MediaStore URIs
            for (photo in backupData.photos) {
                if (!photo.isFromAssets) {
                    val isValid = validateMediaStoreUri(photo.path)
                    if (!isValid) {
                        warnings.add("Missing photo: ${photo.name}")
                    }
                }
            }

            Result.success(
                BackupValidationResult(
                    isValid = errors.isEmpty(),
                    version = backupData.version,
                    format = BackupFormat.JSON,
                    hasMetadata = true,
                    hasPhotos = false, // JSON doesn't include photo files
                    photosCount = backupData.photos.size,
                    categoriesCount = backupData.categories.size,
                    integrityCheckPassed = true, // No integrity checks for JSON
                    errors = errors,
                    warnings = warnings
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restore from backup with comprehensive options and rollback support
     */
    suspend fun restoreFromBackup(
        backupFile: File,
        options: RestoreOptions = RestoreOptions(),
        progressCallback: ((current: Int, total: Int, operation: String) -> Unit)? = null
    ): Flow<ImportProgress> = flow {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        var rollbackData: RollbackData? = null

        try {
            emit(ImportProgress(0, 0, "Validating backup"))
            progressCallback?.invoke(0, 100, "Validating backup")

            // Validate backup first
            val validationResult = validateBackup(backupFile, options.validateIntegrity).getOrThrow()
            if (!validationResult.isValid) {
                errors.addAll(validationResult.errors)
                emit(ImportProgress(0, 0, "Validation failed", errors))
                return@flow
            }

            // Dry run mode - just validate and return
            if (options.dryRun) {
                emit(ImportProgress(1, 1, "Dry run completed", errors))
                return@flow
            }

            // Create rollback snapshot if replacing
            if (options.strategy == ImportStrategy.REPLACE) {
                emit(ImportProgress(0, 0, "Creating rollback snapshot"))
                progressCallback?.invoke(10, 100, "Creating rollback snapshot")
                rollbackData = createRollbackSnapshot()
            }

            // Determine backup type and restore accordingly
            val isZipBackup = backupFile.name.endsWith(".zip") || backupFile.name.endsWith(".smilepile")

            if (isZipBackup) {
                restoreFromZipInternal(backupFile, options, progressCallback, rollbackData).collect { progress ->
                    emit(progress)
                }
            } else {
                restoreFromJsonInternal(backupFile, options, progressCallback, rollbackData).collect { progress ->
                    emit(progress)
                }
            }

        } catch (e: Exception) {
            errors.add("Restore failed: ${e.message}")
            Log.e(TAG, "Restore failed", e)

            // Attempt rollback if we have snapshot
            if (rollbackData != null) {
                emit(ImportProgress(0, 0, "Attempting rollback", errors))
                performRollback(rollbackData)
            }

            emit(ImportProgress(0, 0, "Restore failed", errors))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Internal ZIP restore implementation
     */
    private suspend fun restoreFromZipInternal(
        zipFile: File,
        options: RestoreOptions,
        progressCallback: ((current: Int, total: Int, operation: String) -> Unit)?,
        rollbackData: RollbackData?
    ): Flow<ImportProgress> = flow {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        var categoriesImported = 0
        var photosImported = 0
        var photosSkipped = 0
        var photoFilesRestored = 0

        try {
            emit(ImportProgress(1, 0, "Extracting backup"))
            progressCallback?.invoke(20, 100, "Extracting backup")

            // Extract ZIP
            val tempDir = File(context.cacheDir, "restore_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            val extractResult = ZipUtils.extractZip(zipFile, tempDir)
            if (extractResult.isFailure) {
                throw Exception("Failed to extract backup: ${extractResult.exceptionOrNull()?.message}")
            }

            // Read metadata
            val metadataFile = File(tempDir, ZipUtils.METADATA_FILE)
            val backupData = json.decodeFromString<AppBackup>(metadataFile.readText())

            val totalItems = backupData.categories.size + backupData.photos.size
            var processedItems = 0

            // Handle strategy
            if (options.strategy == ImportStrategy.REPLACE) {
                emit(ImportProgress(totalItems, processedItems, "Clearing existing data"))
                progressCallback?.invoke(30, 100, "Clearing existing data")
                clearAllData()
            }

            // Restore categories
            emit(ImportProgress(totalItems, processedItems, "Restoring categories"))
            progressCallback?.invoke(40, 100, "Restoring categories")

            for (categoryBackup in backupData.categories) {
                try {
                    val result = restoreCategory(categoryBackup, options)
                    if (result.imported) {
                        categoriesImported++
                    } else if (result.warning != null) {
                        warnings.add(result.warning)
                    }
                } catch (e: Exception) {
                    errors.add("Failed to restore category '${categoryBackup.displayName}': ${e.message}")
                }
                processedItems++
                val progress = 40 + ((processedItems * 30) / totalItems)
                progressCallback?.invoke(progress, 100, "Restoring categories ($categoriesImported/${backupData.categories.size})")
                emit(ImportProgress(totalItems, processedItems, "Restoring categories", errors))
            }

            // Restore photos
            emit(ImportProgress(totalItems, processedItems, "Restoring photos"))
            progressCallback?.invoke(70, 100, "Restoring photos")

            val photosDir = File(tempDir, "photos")
            val thumbnailsDir = File(tempDir, "thumbnails")
            val internalPhotosDir = File(context.filesDir, "photos")
            val internalThumbnailsDir = File(context.filesDir, "thumbnails")
            internalPhotosDir.mkdirs()
            if (options.restoreThumbnails) {
                internalThumbnailsDir.mkdirs()
            }

            for (photoBackup in backupData.photos) {
                try {
                    val result = restorePhoto(
                        photoBackup,
                        backupData,
                        photosDir,
                        thumbnailsDir,
                        internalPhotosDir,
                        internalThumbnailsDir,
                        options
                    )

                    when (result) {
                        is PhotoRestoreResult.Imported -> {
                            photosImported++
                            if (result.fileRestored) photoFilesRestored++
                        }
                        is PhotoRestoreResult.Skipped -> {
                            photosSkipped++
                            if (result.reason != null) warnings.add(result.reason)
                        }
                        is PhotoRestoreResult.Failed -> {
                            errors.add(result.error)
                        }
                    }
                } catch (e: Exception) {
                    errors.add("Failed to restore photo '${photoBackup.name}': ${e.message}")
                }

                processedItems++
                val progress = 70 + ((processedItems * 25) / totalItems)
                progressCallback?.invoke(progress, 100, "Restoring photos ($photosImported/${backupData.photos.size})")
                emit(ImportProgress(totalItems, processedItems, "Restoring photos", errors))
            }

            // Restore settings if requested
            if (options.restoreSettings && backupData.settings != null) {
                emit(ImportProgress(totalItems, processedItems, "Restoring settings"))
                progressCallback?.invoke(95, 100, "Restoring settings")
                restoreSettings(backupData.settings)
            }

            // Clean up temp directory
            tempDir.deleteRecursively()

            progressCallback?.invoke(100, 100, "Restore completed")
            emit(ImportProgress(
                totalItems,
                processedItems,
                "Restore completed successfully",
                errors
            ))

            Log.i(TAG, "Restore completed: $categoriesImported categories, $photosImported photos, $photoFilesRestored files restored")

        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Internal JSON restore implementation
     */
    private suspend fun restoreFromJsonInternal(
        jsonFile: File,
        options: RestoreOptions,
        progressCallback: ((current: Int, total: Int, operation: String) -> Unit)?,
        rollbackData: RollbackData?
    ): Flow<ImportProgress> = flow {
        // Similar to ZIP but without file restoration
        // Implementation would be similar to existing importFromJson in BackupManager
        // Reuse most of the logic but with RestoreOptions support
    }

    /**
     * Restore a single category
     */
    private suspend fun restoreCategory(
        categoryBackup: BackupCategory,
        options: RestoreOptions
    ): CategoryRestoreResult {
        return try {
            val existingCategory = categoryRepository.getCategoryByName(categoryBackup.name)

            when (options.duplicateResolution) {
                DuplicateResolution.SKIP -> {
                    if (existingCategory != null) {
                        return CategoryRestoreResult(
                            imported = false,
                            warning = "Category already exists: ${categoryBackup.displayName}"
                        )
                    }
                }
                DuplicateResolution.REPLACE -> {
                    if (existingCategory != null) {
                        categoryRepository.deleteCategory(existingCategory)
                    }
                }
                DuplicateResolution.RENAME -> {
                    if (existingCategory != null) {
                        val newName = generateUniqueCategoryName(categoryBackup.name)
                        val renamedCategory = categoryBackup.copy(name = newName)
                        categoryRepository.insertCategory(renamedCategory.toCategory())
                        return CategoryRestoreResult(
                            imported = true,
                            warning = "Renamed category: ${categoryBackup.displayName} -> $newName"
                        )
                    }
                }
                DuplicateResolution.ASK_USER -> {
                    // This would require UI interaction
                    // For now, default to skip
                    if (existingCategory != null) {
                        return CategoryRestoreResult(
                            imported = false,
                            warning = "Category already exists: ${categoryBackup.displayName}"
                        )
                    }
                }
            }

            categoryRepository.insertCategory(categoryBackup.toCategory())
            CategoryRestoreResult(imported = true)
        } catch (e: Exception) {
            CategoryRestoreResult(
                imported = false,
                warning = "Failed to restore category: ${e.message}"
            )
        }
    }

    /**
     * Restore a single photo
     */
    private suspend fun restorePhoto(
        photoBackup: BackupPhoto,
        backupData: AppBackup,
        photosDir: File,
        thumbnailsDir: File,
        internalPhotosDir: File,
        internalThumbnailsDir: File,
        options: RestoreOptions
    ): PhotoRestoreResult {
        try {
            // Find manifest entry for photo file
            val manifestEntry = backupData.photoManifest.find { it.photoId == photoBackup.id }
            var newPhotoPath = photoBackup.path
            var fileRestored = false

            if (manifestEntry != null) {
                // Restore photo file
                val sourceFile = File(photosDir, manifestEntry.fileName)
                if (sourceFile.exists()) {
                    // Verify integrity if requested
                    if (options.validateIntegrity && manifestEntry.checksum != null) {
                        val actualChecksum = calculateMD5(sourceFile)
                        if (actualChecksum != manifestEntry.checksum) {
                            return PhotoRestoreResult.Failed("Integrity check failed for ${manifestEntry.fileName}")
                        }
                    }

                    val destFile = File(internalPhotosDir, manifestEntry.fileName)
                    sourceFile.copyTo(destFile, overwrite = true)
                    newPhotoPath = destFile.absolutePath
                    fileRestored = true

                    // Restore thumbnail if available and requested
                    if (options.restoreThumbnails) {
                        val thumbSource = File(thumbnailsDir, "thumb_${manifestEntry.fileName}")
                        if (thumbSource.exists()) {
                            val thumbDest = File(internalThumbnailsDir, "thumb_${manifestEntry.fileName}")
                            thumbSource.copyTo(thumbDest, overwrite = true)
                        }
                    }
                }
            }

            // Check for duplicates
            val existingPhotos = photoRepository.getAllPhotos()
            val isDuplicate = existingPhotos.any { it.path == newPhotoPath }

            if (isDuplicate) {
                when (options.duplicateResolution) {
                    DuplicateResolution.SKIP -> {
                        return PhotoRestoreResult.Skipped("Duplicate photo: ${photoBackup.name}")
                    }
                    DuplicateResolution.REPLACE -> {
                        val existingPhoto = existingPhotos.find { it.path == newPhotoPath }
                        if (existingPhoto != null) {
                            photoRepository.deletePhoto(existingPhoto)
                        }
                    }
                    DuplicateResolution.RENAME -> {
                        val newFileName = generateUniquePhotoName(File(newPhotoPath).name)
                        val renamedFile = File(internalPhotosDir, newFileName)
                        val sourceFile = File(newPhotoPath)
                        if (!sourceFile.renameTo(renamedFile)) {
                            return PhotoRestoreResult.Failed("Failed to rename photo file: ${sourceFile.name}")
                        }
                        newPhotoPath = renamedFile.absolutePath
                    }
                    DuplicateResolution.ASK_USER -> {
                        // Would require UI interaction
                        return PhotoRestoreResult.Skipped("Duplicate photo (user decision pending): ${photoBackup.name}")
                    }
                }
            }

            // Insert photo
            val photoToInsert = photoBackup.toPhoto().copy(path = newPhotoPath)
            photoRepository.insertPhoto(photoToInsert)

            return PhotoRestoreResult.Imported(fileRestored)
        } catch (e: Exception) {
            return PhotoRestoreResult.Failed("Failed to restore photo: ${e.message}")
        }
    }

    /**
     * Restore app settings
     */
    private suspend fun restoreSettings(settings: BackupSettings) {
        try {
            // Restore theme
            themeManager.setThemeMode(
                if (settings.isDarkMode) ThemeMode.DARK else ThemeMode.LIGHT
            )

            // Note: Security settings like PIN/pattern are not restored for security reasons
            // Only restore non-sensitive settings
            if (settings.securitySettings.kidSafeModeEnabled) {
                // Enable Kids Mode if it was enabled in backup
                // This would need proper UI flow for PIN setup
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore settings", e)
        }
    }

    /**
     * Create rollback snapshot before restore
     */
    private suspend fun createRollbackSnapshot(): RollbackData {
        val categories = categoryRepository.getAllCategories()
        val photos = photoRepository.getAllPhotos()
        val isDarkMode = themeManager.isDarkMode.value

        // Create backup of current state
        val rollbackDir = File(context.cacheDir, "rollback_${System.currentTimeMillis()}")
        rollbackDir.mkdirs()

        return RollbackData(
            categories = categories,
            photos = photos,
            isDarkMode = isDarkMode,
            rollbackDir = rollbackDir
        )
    }

    /**
     * Perform rollback to previous state
     */
    private suspend fun performRollback(rollbackData: RollbackData) {
        try {
            Log.i(TAG, "Performing rollback to previous state")

            // Clear current data
            clearAllData()

            // Restore previous categories
            rollbackData.categories.forEach { category ->
                categoryRepository.insertCategory(category)
            }

            // Restore previous photos
            rollbackData.photos.forEach { photo ->
                photoRepository.insertPhoto(photo)
            }

            // Restore theme
            themeManager.setThemeMode(
                if (rollbackData.isDarkMode) ThemeMode.DARK else ThemeMode.LIGHT
            )

            // Clean up rollback directory
            rollbackData.rollbackDir.deleteRecursively()

            Log.i(TAG, "Rollback completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Rollback failed", e)
        }
    }

    /**
     * Clear all existing data
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
     * Validate MediaStore URI
     */
    private suspend fun validateMediaStoreUri(uriString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)

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
                val file = File(uri.path ?: uriString)
                file.exists() && file.isFile
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Calculate MD5 checksum
     */
    private fun calculateMD5(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().use { inputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Generate unique category name
     */
    private suspend fun generateUniqueCategoryName(baseName: String): String {
        val categories = categoryRepository.getAllCategories()
        var counter = 1
        var newName = baseName

        while (categories.any { it.name == newName }) {
            newName = "${baseName}_$counter"
            counter++
        }

        return newName
    }

    /**
     * Generate unique photo name
     */
    private fun generateUniquePhotoName(baseName: String): String {
        val timestamp = System.currentTimeMillis()
        val nameWithoutExtension = baseName.substringBeforeLast(".")
        val extension = baseName.substringAfterLast(".", "")
        return "${nameWithoutExtension}_${timestamp}${if (extension.isNotEmpty()) ".$extension" else ""}"
    }

    /**
     * Data class for rollback information
     */
    private data class RollbackData(
        val categories: List<com.smilepile.data.models.Category>,
        val photos: List<com.smilepile.data.models.Photo>,
        val isDarkMode: Boolean,
        val rollbackDir: File
    )

    /**
     * Category restore result
     */
    private data class CategoryRestoreResult(
        val imported: Boolean,
        val warning: String? = null
    )

    /**
     * Photo restore result
     */
    private sealed class PhotoRestoreResult {
        data class Imported(val fileRestored: Boolean) : PhotoRestoreResult()
        data class Skipped(val reason: String?) : PhotoRestoreResult()
        data class Failed(val error: String) : PhotoRestoreResult()
    }
}