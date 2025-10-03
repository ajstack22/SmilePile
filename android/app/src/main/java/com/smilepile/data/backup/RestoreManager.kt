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
import kotlinx.coroutines.flow.FlowCollector
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
                return@withContext Result.success(createInvalidZipResult(errors))
            }

            // Extract and validate metadata
            val tempDir = File(context.cacheDir, "validate_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            try {
                validateZipContents(zipFile, tempDir, checkIntegrity, errors, warnings)
            } finally {
                tempDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun validateZipContents(
        zipFile: File,
        tempDir: File,
        checkIntegrity: Boolean,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ): Result<BackupValidationResult> {
        val extractResult = ZipUtils.extractZip(zipFile, tempDir)
        if (extractResult.isFailure) {
            errors.add("Failed to extract ZIP: ${extractResult.exceptionOrNull()?.message}")
            return Result.success(createInvalidZipResult(errors))
        }

        val metadataFile = File(tempDir, ZipUtils.METADATA_FILE)
        if (!metadataFile.exists()) {
            errors.add("metadata.json not found in backup")
            return Result.success(createInvalidZipResult(errors))
        }

        val backupData = json.decodeFromString<AppBackup>(metadataFile.readText())
        validateBackupVersion(backupData, errors)

        val photosDir = File(tempDir, "photos")
        val hasPhotos = photosDir.exists() && photosDir.isDirectory
        val integrityPassed = performIntegrityChecks(backupData, photosDir, checkIntegrity, warnings)

        return Result.success(
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
    }

    private fun createInvalidZipResult(errors: List<String>): BackupValidationResult {
        return BackupValidationResult(
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
    }

    private fun validateBackupVersion(backupData: AppBackup, errors: MutableList<String>) {
        if (backupData.version < MIN_SUPPORTED_VERSION || backupData.version > MAX_SUPPORTED_VERSION) {
            errors.add("Unsupported backup version: ${backupData.version}")
        }
    }

    private fun performIntegrityChecks(
        backupData: AppBackup,
        photosDir: File,
        checkIntegrity: Boolean,
        warnings: MutableList<String>
    ): Boolean {
        if (!checkIntegrity || backupData.photoManifest.isEmpty()) {
            return true
        }

        var integrityPassed = true
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
        return integrityPassed
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
        try {
            val restoreContext = initializeRestoreContext(zipFile, options, progressCallback)

            executeStrategyPreparation(options, restoreContext, progressCallback)

            val categoriesResult = restoreCategoriesFromZip(restoreContext, options, progressCallback)

            val photosResult = restorePhotosFromZip(restoreContext, options, progressCallback)

            restoreSettingsIfRequested(restoreContext, options, progressCallback)

            cleanupRestoreTemp(restoreContext.tempDir)

            emitRestoreCompletion(restoreContext, categoriesResult, photosResult, progressCallback)

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

            if (existingCategory != null) {
                handleDuplicateCategory(categoryBackup, existingCategory, options)
            } else {
                insertNewCategory(categoryBackup)
            }
        } catch (e: Exception) {
            CategoryRestoreResult(
                imported = false,
                warning = "Failed to restore category: ${e.message}"
            )
        }
    }

    private suspend fun handleDuplicateCategory(
        categoryBackup: BackupCategory,
        existingCategory: com.smilepile.data.models.Category,
        options: RestoreOptions
    ): CategoryRestoreResult {
        return when (options.duplicateResolution) {
            DuplicateResolution.SKIP -> {
                CategoryRestoreResult(
                    imported = false,
                    warning = "Category already exists: ${categoryBackup.displayName}"
                )
            }
            DuplicateResolution.REPLACE -> {
                categoryRepository.deleteCategory(existingCategory)
                insertNewCategory(categoryBackup)
            }
            DuplicateResolution.RENAME -> {
                val newName = generateUniqueCategoryName(categoryBackup.name)
                val renamedCategory = categoryBackup.copy(name = newName)
                categoryRepository.insertCategory(renamedCategory.toCategory())
                CategoryRestoreResult(
                    imported = true,
                    warning = "Renamed category: ${categoryBackup.displayName} -> $newName"
                )
            }
            DuplicateResolution.ASK_USER -> {
                CategoryRestoreResult(
                    imported = false,
                    warning = "Category already exists: ${categoryBackup.displayName}"
                )
            }
        }
    }

    private suspend fun insertNewCategory(categoryBackup: BackupCategory): CategoryRestoreResult {
        categoryRepository.insertCategory(categoryBackup.toCategory())
        return CategoryRestoreResult(imported = true)
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
            val manifestEntry = backupData.photoManifest.find { it.photoId == photoBackup.id }

            val restoreContext = restorePhotoFile(
                manifestEntry,
                photoBackup.path,
                photosDir,
                thumbnailsDir,
                internalPhotosDir,
                internalThumbnailsDir,
                options
            )

            val finalPath = handlePhotoDuplicate(
                restoreContext.newPhotoPath,
                photoBackup.name,
                internalPhotosDir,
                options
            ) ?: return PhotoRestoreResult.Skipped("Duplicate photo: ${photoBackup.name}")

            val photoToInsert = photoBackup.toPhoto().copy(path = finalPath)
            photoRepository.insertPhoto(photoToInsert)

            return PhotoRestoreResult.Imported(restoreContext.fileRestored)
        } catch (e: Exception) {
            return PhotoRestoreResult.Failed("Failed to restore photo: ${e.message}")
        }
    }

    private suspend fun restorePhotoFile(
        manifestEntry: PhotoManifestEntry?,
        originalPath: String,
        photosDir: File,
        thumbnailsDir: File,
        internalPhotosDir: File,
        internalThumbnailsDir: File,
        options: RestoreOptions
    ): PhotoRestoreContext {
        if (manifestEntry == null) {
            return PhotoRestoreContext(originalPath, false)
        }

        val sourceFile = File(photosDir, manifestEntry.fileName)
        if (!sourceFile.exists()) {
            return PhotoRestoreContext(originalPath, false)
        }

        verifyPhotoIntegrity(sourceFile, manifestEntry, options)

        val destFile = File(internalPhotosDir, manifestEntry.fileName)
        sourceFile.copyTo(destFile, overwrite = true)

        restorePhotoThumbnail(manifestEntry, thumbnailsDir, internalThumbnailsDir, options)

        return PhotoRestoreContext(destFile.absolutePath, true)
    }

    private fun verifyPhotoIntegrity(
        sourceFile: File,
        manifestEntry: PhotoManifestEntry,
        options: RestoreOptions
    ) {
        if (options.validateIntegrity && manifestEntry.checksum != null) {
            val actualChecksum = calculateMD5(sourceFile)
            if (actualChecksum != manifestEntry.checksum) {
                throw SecurityException("Integrity check failed for ${manifestEntry.fileName}")
            }
        }
    }

    private fun restorePhotoThumbnail(
        manifestEntry: PhotoManifestEntry,
        thumbnailsDir: File,
        internalThumbnailsDir: File,
        options: RestoreOptions
    ) {
        if (options.restoreThumbnails) {
            val thumbSource = File(thumbnailsDir, "thumb_${manifestEntry.fileName}")
            if (thumbSource.exists()) {
                val thumbDest = File(internalThumbnailsDir, "thumb_${manifestEntry.fileName}")
                thumbSource.copyTo(thumbDest, overwrite = true)
            }
        }
    }

    private suspend fun handlePhotoDuplicate(
        photoPath: String,
        photoName: String,
        internalPhotosDir: File,
        options: RestoreOptions
    ): String? {
        val existingPhotos = photoRepository.getAllPhotos()
        val isDuplicate = existingPhotos.any { it.path == photoPath }

        if (!isDuplicate) {
            return photoPath
        }

        return when (options.duplicateResolution) {
            DuplicateResolution.SKIP -> null
            DuplicateResolution.REPLACE -> {
                val existingPhoto = existingPhotos.find { it.path == photoPath }
                if (existingPhoto != null) {
                    photoRepository.deletePhoto(existingPhoto)
                }
                photoPath
            }
            DuplicateResolution.RENAME -> {
                val newFileName = generateUniquePhotoName(File(photoPath).name)
                val renamedFile = File(internalPhotosDir, newFileName)
                val sourceFile = File(photoPath)
                if (sourceFile.renameTo(renamedFile)) {
                    renamedFile.absolutePath
                } else {
                    throw IllegalStateException("Failed to rename photo file: ${sourceFile.name}")
                }
            }
            DuplicateResolution.ASK_USER -> null
        }
    }

    private data class PhotoRestoreContext(
        val newPhotoPath: String,
        val fileRestored: Boolean
    )

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
     * Calculate MD5 checksum for backup file integrity verification
     *
     * Note: MD5 is used here solely for verifying backup file integrity during restore,
     * not for cryptographic security. This maintains compatibility with existing backup
     * files created by BackupManager.
     *
     * SonarQube Warning Suppression Justification:
     * - Use case: Non-cryptographic integrity verification of user backups
     * - Risk: Low - collision attacks not applicable to backup restore scenario
     * - Benefit: Backward compatibility with all existing user backup files
     * - Future: Will migrate to SHA-256 in v2 backup format
     */
    @Suppress("kotlin:S4790") // Weak hash algorithm - justified for non-security integrity check
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
     * Context for restore operations
     */
    private data class RestoreContext(
        val tempDir: File,
        val backupData: AppBackup,
        val totalItems: Int,
        val errors: MutableList<String>,
        val warnings: MutableList<String>,
        var processedItems: Int = 0
    )

    /**
     * Result of category restoration
     */
    private data class CategoriesRestoreResult(
        val imported: Int,
        val progress: ImportProgress
    )

    /**
     * Result of photo restoration
     */
    private data class PhotosRestoreResult(
        val imported: Int,
        val skipped: Int,
        val filesRestored: Int,
        val progress: ImportProgress
    )

    /**
     * Photo directories for restore
     */
    private data class PhotoDirectories(
        val photosDir: File,
        val thumbnailsDir: File,
        val internalPhotosDir: File,
        val internalThumbnailsDir: File
    )

    /**
     * Initialize restore context
     */
    private suspend fun FlowCollector<ImportProgress>.initializeRestoreContext(
        zipFile: File,
        options: RestoreOptions,
        progressCallback: ((current: Int, total: Int, operation: String) -> Unit)?
    ): RestoreContext {
        emit(ImportProgress(1, 0, "Extracting backup"))
        progressCallback?.invoke(20, 100, "Extracting backup")

        val tempDir = createRestoreTempDirectory()
        val backupData = extractAndParseBackup(zipFile, tempDir)
        val totalItems = backupData.categories.size + backupData.photos.size

        return RestoreContext(
            tempDir = tempDir,
            backupData = backupData,
            totalItems = totalItems,
            errors = mutableListOf(),
            warnings = mutableListOf()
        )
    }

    /**
     * Create temp directory for restore
     */
    private fun createRestoreTempDirectory(): File {
        val tempDir = File(context.cacheDir, "restore_temp_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        return tempDir
    }

    /**
     * Extract ZIP and parse backup metadata
     */
    private suspend fun extractAndParseBackup(zipFile: File, tempDir: File): AppBackup {
        val extractResult = ZipUtils.extractZip(zipFile, tempDir)
        if (extractResult.isFailure) {
            throw Exception("Failed to extract backup: ${extractResult.exceptionOrNull()?.message}")
        }

        val metadataFile = File(tempDir, ZipUtils.METADATA_FILE)
        return json.decodeFromString<AppBackup>(metadataFile.readText())
    }

    /**
     * Execute strategy preparation
     */
    private suspend fun FlowCollector<ImportProgress>.executeStrategyPreparation(
        options: RestoreOptions,
        restoreContext: RestoreContext,
        progressCallback: ((current: Int, total: Int, operation: String) -> Unit)?
    ) {
        if (options.strategy == ImportStrategy.REPLACE) {
            emit(ImportProgress(restoreContext.totalItems, restoreContext.processedItems, "Clearing existing data"))
            progressCallback?.invoke(30, 100, "Clearing existing data")
            clearAllData()
        }
    }

    /**
     * Restore all categories from ZIP
     */
    private suspend fun FlowCollector<ImportProgress>.restoreCategoriesFromZip(
        restoreContext: RestoreContext,
        options: RestoreOptions,
        progressCallback: ((current: Int, total: Int, operation: String) -> Unit)?
    ): CategoriesRestoreResult {
        emit(ImportProgress(restoreContext.totalItems, restoreContext.processedItems, "Restoring categories"))
        progressCallback?.invoke(40, 100, "Restoring categories")

        var categoriesImported = 0

        for (categoryBackup in restoreContext.backupData.categories) {
            processCategoryRestore(categoryBackup, options, restoreContext)?.let {
                if (it.imported) {
                    categoriesImported++
                }
            }

            restoreContext.processedItems++
            updateCategoryProgress(restoreContext, categoriesImported, progressCallback)
        }

        return CategoriesRestoreResult(
            imported = categoriesImported,
            progress = ImportProgress(
                restoreContext.totalItems,
                restoreContext.processedItems,
                "Restoring categories",
                restoreContext.errors
            )
        )
    }

    /**
     * Process single category restore with error handling
     */
    private suspend fun processCategoryRestore(
        categoryBackup: BackupCategory,
        options: RestoreOptions,
        restoreContext: RestoreContext
    ): CategoryRestoreResult? {
        return try {
            val result = restoreCategory(categoryBackup, options)

            if (!result.imported && result.warning != null) {
                restoreContext.warnings.add(result.warning)
            }

            result
        } catch (e: Exception) {
            restoreContext.errors.add("Failed to restore category '${categoryBackup.displayName}': ${e.message}")
            null
        }
    }

    /**
     * Update category restore progress
     */
    private suspend fun FlowCollector<ImportProgress>.updateCategoryProgress(
        restoreContext: RestoreContext,
        categoriesImported: Int,
        progressCallback: ((current: Int, total: Int, operation: String) -> Unit)?
    ) {
        val progress = 40 + ((restoreContext.processedItems * 30) / restoreContext.totalItems)
        progressCallback?.invoke(progress, 100, "Restoring categories ($categoriesImported/${restoreContext.backupData.categories.size})")
        emit(ImportProgress(restoreContext.totalItems, restoreContext.processedItems, "Restoring categories", restoreContext.errors))
    }

    /**
     * Restore all photos from ZIP
     */
    private suspend fun FlowCollector<ImportProgress>.restorePhotosFromZip(
        restoreContext: RestoreContext,
        options: RestoreOptions,
        progressCallback: ((current: Int, total: Int, operation: String) -> Unit)?
    ): PhotosRestoreResult {
        emit(ImportProgress(restoreContext.totalItems, restoreContext.processedItems, "Restoring photos"))
        progressCallback?.invoke(70, 100, "Restoring photos")

        val directories = preparePhotoDirectories(restoreContext, options)

        var photosImported = 0
        var photosSkipped = 0
        var photoFilesRestored = 0

        for (photoBackup in restoreContext.backupData.photos) {
            val result = processPhotoRestore(
                photoBackup,
                restoreContext.backupData,
                directories,
                options,
                restoreContext
            )

            when (result) {
                is PhotoRestoreResult.Imported -> {
                    photosImported++
                    if (result.fileRestored) photoFilesRestored++
                }
                is PhotoRestoreResult.Skipped -> {
                    photosSkipped++
                    if (result.reason != null) restoreContext.warnings.add(result.reason)
                }
                is PhotoRestoreResult.Failed -> {
                    restoreContext.errors.add(result.error)
                }
            }

            restoreContext.processedItems++
            updatePhotoProgress(restoreContext, photosImported, progressCallback)
        }

        return PhotosRestoreResult(
            imported = photosImported,
            skipped = photosSkipped,
            filesRestored = photoFilesRestored,
            progress = ImportProgress(restoreContext.totalItems, restoreContext.processedItems, "Restoring photos", restoreContext.errors)
        )
    }

    /**
     * Prepare photo directories
     */
    private fun preparePhotoDirectories(
        restoreContext: RestoreContext,
        options: RestoreOptions
    ): PhotoDirectories {
        val photosDir = File(restoreContext.tempDir, "photos")
        val thumbnailsDir = File(restoreContext.tempDir, "thumbnails")
        val internalPhotosDir = File(context.filesDir, "photos").apply { mkdirs() }
        val internalThumbnailsDir = if (options.restoreThumbnails) {
            File(context.filesDir, "thumbnails").apply { mkdirs() }
        } else {
            File(context.filesDir, "thumbnails")
        }

        return PhotoDirectories(photosDir, thumbnailsDir, internalPhotosDir, internalThumbnailsDir)
    }

    /**
     * Process single photo restore
     */
    private suspend fun processPhotoRestore(
        photoBackup: BackupPhoto,
        backupData: AppBackup,
        directories: PhotoDirectories,
        options: RestoreOptions,
        restoreContext: RestoreContext
    ): PhotoRestoreResult {
        return try {
            restorePhoto(
                photoBackup,
                backupData,
                directories.photosDir,
                directories.thumbnailsDir,
                directories.internalPhotosDir,
                directories.internalThumbnailsDir,
                options
            )
        } catch (e: Exception) {
            restoreContext.errors.add("Failed to restore photo '${photoBackup.name}': ${e.message}")
            PhotoRestoreResult.Failed(e.message ?: "Unknown error")
        }
    }

    /**
     * Update photo restore progress
     */
    private suspend fun FlowCollector<ImportProgress>.updatePhotoProgress(
        restoreContext: RestoreContext,
        photosImported: Int,
        progressCallback: ((current: Int, total: Int, operation: String) -> Unit)?
    ) {
        val progress = 70 + ((restoreContext.processedItems * 25) / restoreContext.totalItems)
        progressCallback?.invoke(progress, 100, "Restoring photos ($photosImported/${restoreContext.backupData.photos.size})")
        emit(ImportProgress(restoreContext.totalItems, restoreContext.processedItems, "Restoring photos", restoreContext.errors))
    }

    /**
     * Restore settings if requested
     */
    private suspend fun FlowCollector<ImportProgress>.restoreSettingsIfRequested(
        restoreContext: RestoreContext,
        options: RestoreOptions,
        progressCallback: ((current: Int, total: Int, operation: String) -> Unit)?
    ) {
        if (options.restoreSettings && restoreContext.backupData.settings != null) {
            emit(ImportProgress(restoreContext.totalItems, restoreContext.processedItems, "Restoring settings"))
            progressCallback?.invoke(95, 100, "Restoring settings")
            restoreSettings(restoreContext.backupData.settings)
        }
    }

    /**
     * Clean up temp directory
     */
    private fun cleanupRestoreTemp(tempDir: File) {
        tempDir.deleteRecursively()
    }

    /**
     * Emit restore completion
     */
    private suspend fun FlowCollector<ImportProgress>.emitRestoreCompletion(
        restoreContext: RestoreContext,
        categoriesResult: CategoriesRestoreResult,
        photosResult: PhotosRestoreResult,
        progressCallback: ((current: Int, total: Int, operation: String) -> Unit)?
    ) {
        progressCallback?.invoke(100, 100, "Restore completed")
        emit(ImportProgress(
            restoreContext.totalItems,
            restoreContext.processedItems,
            "Restore completed successfully",
            restoreContext.errors
        ))

        Log.i(TAG, "Restore completed: ${categoriesResult.imported} categories, ${photosResult.imported} photos, ${photosResult.filesRestored} files restored")
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