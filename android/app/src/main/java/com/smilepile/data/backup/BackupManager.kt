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
import com.smilepile.storage.ZipUtils
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
import java.io.*
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import java.util.zip.Deflater

/**
 * Manager for handling app data backup and export functionality
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val categoryRepository: CategoryRepository,
    private val photoRepository: PhotoRepository,
    private val themeManager: ThemeManager,
    private val securePreferencesManager: SecurePreferencesManager,
    private val deletionTracker: ManagedDeletionTracker
) {

    companion object {
        private const val TAG = "BackupManager"
        private const val BACKUP_MIME_TYPE_JSON = "application/json"
        private const val BACKUP_MIME_TYPE_ZIP = "application/zip"
        private const val BACKUP_FILE_EXTENSION_JSON = ".json"
        private const val BACKUP_FILE_EXTENSION_ZIP = ".zip"
        private const val SMILEPILE_BACKUP_EXTENSION = ".smilepile"
        private const val MIN_SUPPORTED_VERSION = 1
        private const val MAX_SUPPORTED_VERSION = CURRENT_BACKUP_VERSION
        private const val THUMBNAIL_DIR = "thumbnails"
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Export app data to ZIP format with comprehensive backup options
     * @param options Backup options for selective backup
     * @param tempDir Temporary directory for staging files
     * @param progressCallback Optional callback for progress updates
     * @return Result containing the ZIP file path
     */
    suspend fun exportToZip(
        options: BackupOptions = BackupOptions(),
        tempDir: File? = null,
        progressCallback: ((current: Int, total: Int, operation: String) -> Unit)? = null
    ): Result<File> {
        return exportToZipWithOptions(options, tempDir, progressCallback)
    }

    /**
     * Legacy export method for backward compatibility
     */
    private suspend fun exportToZipLegacy(
        tempDir: File? = null,
        progressCallback: ((current: Int, total: Int, operation: String) -> Unit)? = null
    ): Result<File> {
        return try {
            val workDir = tempDir ?: File(context.cacheDir, "backup_temp_${System.currentTimeMillis()}")
            workDir.mkdirs()

            progressCallback?.invoke(0, 100, "Gathering app data")

            // Gather all data
            val categories = categoryRepository.getAllCategories()
            val photos = photoRepository.getAllPhotos()
            val isDarkMode = themeManager.isDarkMode.first()
            val securitySummary = securePreferencesManager.getSecuritySummary()

            progressCallback?.invoke(20, 100, "Preparing metadata")

            // Create photo manifest for ZIP tracking
            val photoManifest = mutableListOf<PhotoManifestEntry>()
            val photosDir = File(workDir, "photos")
            photosDir.mkdirs()

            progressCallback?.invoke(30, 100, "Copying photo files")

            // Copy photos to staging directory and build manifest
            var photosCopied = 0
            photos.forEachIndexed { index, photo ->
                try {
                    if (!photo.isFromAssets) {
                        val sourceFile = File(photo.path)
                        if (sourceFile.exists()) {
                            val fileName = "${photo.id}_${sourceFile.name}"
                            val destFile = File(photosDir, fileName)
                            sourceFile.copyTo(destFile, overwrite = true)

                            // Calculate checksum for integrity
                            val checksum = calculateMD5(destFile)

                            photoManifest.add(
                                PhotoManifestEntry(
                                    photoId = photo.id,
                                    originalPath = photo.path,
                                    zipEntryName = "${ZipUtils.PHOTOS_DIR}$fileName",
                                    fileName = fileName,
                                    fileSize = destFile.length(),
                                    checksum = checksum
                                )
                            )
                            photosCopied++
                        }
                    }

                    val progress = 30 + ((index + 1) * 40 / photos.size)
                    progressCallback?.invoke(progress, 100, "Copying photos ($photosCopied/${photos.size})")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to copy photo: ${photo.path}", e)
                }
            }

            progressCallback?.invoke(70, 100, "Creating backup metadata")

            // Convert to backup format
            val backupCategories = categories.map { BackupCategory.fromCategory(it) }
            val backupPhotos = photos.map { BackupPhoto.fromPhoto(it) }
            val backupSettings = BackupSettings(
                isDarkMode = isDarkMode,
                securitySettings = BackupSecuritySettings(
                    hasPIN = securitySummary.hasPIN,
                    hasPattern = securitySummary.hasPattern,
                    kidSafeModeEnabled = securitySummary.kidSafeModeEnabled,
                    deleteProtectionEnabled = securitySummary.deleteProtectionEnabled
                )
            )

            val appVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: Exception) {
                "unknown"
            }

            val appBackup = AppBackup(
                version = CURRENT_BACKUP_VERSION,
                exportDate = System.currentTimeMillis(),
                appVersion = appVersion,
                format = BackupFormat.ZIP.name,
                categories = backupCategories,
                photos = backupPhotos,
                settings = backupSettings,
                photoManifest = photoManifest
            )

            // Write metadata.json
            val metadataFile = File(workDir, ZipUtils.METADATA_FILE)
            val jsonString = json.encodeToString(appBackup)
            metadataFile.writeText(jsonString)

            progressCallback?.invoke(80, 100, "Creating ZIP archive")

            // Create ZIP file with compression level
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val zipFile = File(context.cacheDir, "SmilePile_Backup_${timestamp}.zip")

            val zipResult = ZipUtils.createZipFromDirectory(
                sourceDir = workDir,
                outputFile = zipFile,
                compressionLevel = Deflater.DEFAULT_COMPRESSION
            ) { current, total ->
                val progress = 80 + (current * 20 / total)
                progressCallback?.invoke(progress, 100, "Archiving files ($current/$total)")
            }

            // Clean up temp directory
            workDir.deleteRecursively()

            if (zipResult.isSuccess) {
                progressCallback?.invoke(100, 100, "Export completed")
                Result.success(zipFile)
            } else {
                Result.failure(zipResult.exceptionOrNull() ?: Exception("ZIP creation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Export all app data to JSON format (v1 compatibility)
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
                    height = photo.height
                )
            }

            val backupSettings = BackupSettings(
                isDarkMode = isDarkMode,
                securitySettings = BackupSecuritySettings(
                    hasPIN = securitySummary.hasPIN,
                    hasPattern = securitySummary.hasPattern,
                    kidSafeModeEnabled = securitySummary.kidSafeModeEnabled,
                    deleteProtectionEnabled = securitySummary.deleteProtectionEnabled
                )
            )

            // Get app version from BuildConfig if available
            val appVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: Exception) {
                "unknown"
            }

            // Create backup object for JSON format (v1 compatibility)
            val appBackup = AppBackup(
                version = 1, // Force version 1 for JSON format
                exportDate = System.currentTimeMillis(),
                appVersion = appVersion,
                format = BackupFormat.JSON.name,
                categories = backupCategories,
                photos = backupPhotos,
                settings = backupSettings,
                photoManifest = emptyList() // No photo manifest for JSON format
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
     * @param format Backup format to export (JSON or ZIP)
     * @return Intent for file picker
     */
    fun createExportIntent(format: BackupFormat = BackupFormat.ZIP): Intent {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        return when (format) {
            BackupFormat.JSON -> {
                val fileName = "smilepile_backup_$timestamp$BACKUP_FILE_EXTENSION_JSON"
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = BACKUP_MIME_TYPE_JSON
                    putExtra(Intent.EXTRA_TITLE, fileName)
                }
            }
            BackupFormat.ZIP -> {
                val fileName = "smilepile_backup_$timestamp$BACKUP_FILE_EXTENSION_ZIP"
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = BACKUP_MIME_TYPE_ZIP
                    putExtra(Intent.EXTRA_TITLE, fileName)
                }
            }
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
     * Write ZIP file to the selected URI
     * @param zipFile The ZIP file to copy
     * @param uri The destination URI from Storage Access Framework
     * @return Result indicating success or failure
     */
    suspend fun writeZipToFile(zipFile: File, uri: android.net.Uri): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(zipFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
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
     * Import data from ZIP backup file
     * @param zipFile ZIP backup file
     * @param strategy Import strategy (MERGE or REPLACE)
     * @param progressCallback Optional progress callback
     * @return Flow of import progress
     */
    suspend fun importFromZip(
        zipFile: File,
        strategy: ImportStrategy = ImportStrategy.MERGE,
        progressCallback: ((current: Int, total: Int, operation: String) -> Unit)? = null
    ): Flow<ImportProgress> = flow {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        var photoFilesRestored = 0

        try {
            emit(ImportProgress(1, 0, "Validating ZIP structure"))
            progressCallback?.invoke(0, 100, "Validating ZIP structure")

            val tempDir = extractAndValidateZip(zipFile, progressCallback)
            progressCallback?.invoke(30, 100, "Reading backup metadata")

            val backupData = readMetadataFromExtractedZip(tempDir)
            checkBackupVersion(backupData.version)

            val totalItems = backupData.categories.size + backupData.photos.size
            var processedItems = 0

            emit(ImportProgress(totalItems, processedItems, "Starting import"))
            progressCallback?.invoke(40, 100, "Starting import")

            if (strategy == ImportStrategy.REPLACE) {
                emit(ImportProgress(totalItems, processedItems, "Clearing existing data"))
                progressCallback?.invoke(45, 100, "Clearing existing data")
                clearAllData()
            }

            processedItems = importCategories(
                backupData,
                strategy,
                totalItems,
                processedItems,
                errors,
                warnings
            ) { progress ->
                emit(progress)
                progressCallback?.invoke(
                    50 + (progress.processedItems * 20 / backupData.categories.size),
                    100,
                    progress.currentOperation
                )
            }

            emit(ImportProgress(totalItems, processedItems, "Importing photos"))
            progressCallback?.invoke(70, 100, "Importing photos")

            val (imported, skipped, filesRestored) = importPhotosFromZip(
                backupData,
                tempDir,
                strategy,
                totalItems,
                processedItems,
                errors,
                warnings
            ) { progress ->
                emit(progress)
                progressCallback?.invoke(
                    70 + (progress.processedItems * 25 / backupData.photos.size),
                    100,
                    progress.currentOperation
                )
            }

            photoFilesRestored = filesRestored
            processedItems += imported + skipped

            tempDir.deleteRecursively()

            progressCallback?.invoke(100, 100, "Import completed")
            emit(ImportProgress(totalItems, processedItems, "Import completed", errors))

            Log.i(TAG, "ZIP import completed: $imported photos, $photoFilesRestored files restored")

        } catch (e: Exception) {
            errors.add("Import failed: ${e.message}")
            Log.e(TAG, "ZIP import failed", e)
            emit(ImportProgress(0, 0, "Import failed", errors))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Import data from JSON backup file (v1 compatibility)
     */
    suspend fun importFromJson(
        backupFile: File,
        strategy: ImportStrategy = ImportStrategy.MERGE
    ): Flow<ImportProgress> = flow {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        try {
            emit(ImportProgress(1, 0, "Reading backup file"))

            val backupData = readAndValidateBackup(backupFile)
            val totalItems = backupData.categories.size + backupData.photos.size
            var processedItems = 0

            emit(ImportProgress(totalItems, processedItems, "Starting import"))

            if (strategy == ImportStrategy.REPLACE) {
                emit(ImportProgress(totalItems, processedItems, "Clearing existing data"))
                clearAllData()
            }

            val categoriesImported = importCategories(backupData, strategy, totalItems, processedItems, errors, warnings) { progress ->
                emit(progress)
                processedItems = progress.processedItems
            }

            val (photosImported, photosSkipped) = importPhotos(backupData, strategy, totalItems, processedItems, errors, warnings) { progress ->
                emit(progress)
                processedItems = progress.processedItems
            }

            emit(ImportProgress(totalItems, processedItems, "Import completed", errors))
            Log.i(TAG, "Import completed: $categoriesImported categories, $photosImported photos imported, $photosSkipped photos skipped")

        } catch (e: Exception) {
            errors.add("Import failed: ${e.message}")
            Log.e(TAG, "Import failed", e)
            emit(ImportProgress(0, 0, "Import failed", errors))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun readAndValidateBackup(backupFile: File): AppBackup {
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

        checkBackupVersion(backupData.version)
        return backupData
    }

    private suspend fun importCategories(
        backupData: AppBackup,
        strategy: ImportStrategy,
        totalItems: Int,
        startIndex: Int,
        errors: MutableList<String>,
        warnings: MutableList<String>,
        onProgress: suspend (ImportProgress) -> Unit
    ): Int {
        var categoriesImported = 0
        var processedItems = startIndex

        for (categoryBackup in backupData.categories) {
            try {
                importSingleCategory(categoryBackup, strategy, warnings)
                categoriesImported++
            } catch (e: Exception) {
                errors.add("Failed to import category '${categoryBackup.displayName}': ${e.message}")
                Log.e(TAG, "Error importing category: ${categoryBackup.displayName}", e)
            }

            processedItems++
            onProgress(ImportProgress(totalItems, processedItems, "Importing categories", errors))
        }

        return categoriesImported
    }

    private suspend fun importSingleCategory(
        categoryBackup: BackupCategory,
        strategy: ImportStrategy,
        warnings: MutableList<String>
    ) {
        val existingCategory = categoryRepository.getCategoryByName(categoryBackup.name)

        if (strategy == ImportStrategy.MERGE && existingCategory != null) {
            val updatedCategory = categoryBackup.toCategory().copy(id = existingCategory.id)
            categoryRepository.updateCategory(updatedCategory)
            warnings.add("Updated existing category: ${categoryBackup.displayName}")
        } else {
            val categoryToInsert = if (strategy == ImportStrategy.REPLACE) {
                categoryBackup.toCategory()
            } else {
                categoryBackup.toCategory().copy(id = 0)
            }
            categoryRepository.insertCategory(categoryToInsert)
        }
    }

    private suspend fun importPhotos(
        backupData: AppBackup,
        strategy: ImportStrategy,
        totalItems: Int,
        startIndex: Int,
        errors: MutableList<String>,
        warnings: MutableList<String>,
        onProgress: suspend (ImportProgress) -> Unit
    ): Pair<Int, Int> {
        var photosImported = 0
        var photosSkipped = 0
        var processedItems = startIndex

        for (photoBackup in backupData.photos) {
            val result = processPhotoImport(photoBackup, backupData, strategy, warnings, errors)

            when (result) {
                is PhotoImportResult.Imported -> photosImported++
                is PhotoImportResult.Skipped -> photosSkipped++
                is PhotoImportResult.Failed -> {}
            }

            processedItems++
            onProgress(ImportProgress(totalItems, processedItems, "Importing photos", errors))
        }

        return Pair(photosImported, photosSkipped)
    }

    private suspend fun processPhotoImport(
        photoBackup: BackupPhoto,
        backupData: AppBackup,
        strategy: ImportStrategy,
        warnings: MutableList<String>,
        errors: MutableList<String>
    ): PhotoImportResult {
        return try {
            if (!validatePhotoForImport(photoBackup, strategy, warnings)) {
                return PhotoImportResult.Skipped
            }

            val actualCategoryId = resolveCategoryId(photoBackup, backupData, strategy) ?: run {
                errors.add("Category not found for photo: ${photoBackup.name}")
                return PhotoImportResult.Failed
            }

            val photoToInsert = if (strategy == ImportStrategy.REPLACE) {
                photoBackup.toPhoto()
            } else {
                photoBackup.toPhoto().copy(id = 0, categoryId = actualCategoryId)
            }

            photoRepository.insertPhoto(photoToInsert)
            PhotoImportResult.Imported
        } catch (e: Exception) {
            errors.add("Failed to import photo '${photoBackup.name}': ${e.message}")
            Log.e(TAG, "Error importing photo: ${photoBackup.name}", e)
            PhotoImportResult.Failed
        }
    }

    private suspend fun validatePhotoForImport(
        photoBackup: BackupPhoto,
        strategy: ImportStrategy,
        warnings: MutableList<String>
    ): Boolean {
        if (!photoBackup.isFromAssets && !validateMediaStoreUri(photoBackup.path)) {
            warnings.add("Skipped missing photo: ${photoBackup.name}")
            return false
        }

        if (strategy == ImportStrategy.MERGE) {
            val existingPhotos = photoRepository.getAllPhotos()
            if (existingPhotos.any { it.path == photoBackup.path }) {
                warnings.add("Skipped duplicate photo: ${photoBackup.name}")
                return false
            }
        }

        return true
    }

    private suspend fun resolveCategoryId(
        photoBackup: BackupPhoto,
        backupData: AppBackup,
        strategy: ImportStrategy
    ): Long? {
        return if (strategy == ImportStrategy.REPLACE) {
            if (categoryRepository.getCategoryById(photoBackup.categoryId) != null) {
                photoBackup.categoryId
            } else null
        } else {
            val categoryBackupForPhoto = backupData.categories.find { it.id == photoBackup.categoryId }
            categoryBackupForPhoto?.let {
                categoryRepository.getCategoryByName(it.name)?.id
            }
        }
    }

    private sealed class PhotoImportResult {
        object Imported : PhotoImportResult()
        object Skipped : PhotoImportResult()
        object Failed : PhotoImportResult()
    }

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
     * Clear all existing data (for REPLACE strategy or reset)
     */
    suspend fun clearAllData() {
        try {
            // Delete all photos first (due to foreign key constraints)
            val allPhotos = photoRepository.getAllPhotos()
            allPhotos.forEach { photo ->
                photoRepository.deletePhoto(photo)
            }

            // Delete ALL categories (including defaults)
            // This is used for complete reset and REPLACE imports
            val allCategories = categoryRepository.getAllCategories()
            allCategories.forEach { category ->
                categoryRepository.deleteCategory(category)
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

            if (!backupFile.name.endsWith(BACKUP_FILE_EXTENSION_JSON) &&
                !backupFile.name.endsWith(BACKUP_FILE_EXTENSION_ZIP) &&
                !backupFile.name.endsWith(SMILEPILE_BACKUP_EXTENSION)) {
                return@withContext Result.failure(IllegalArgumentException("Invalid backup file format"))
            }

            // Handle ZIP files differently
            if (backupFile.name.endsWith(BACKUP_FILE_EXTENSION_ZIP)) {
                return@withContext validateZipBackupFile(backupFile)
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
     * Extract and validate ZIP file
     */
    private suspend fun extractAndValidateZip(
        zipFile: File,
        progressCallback: ((current: Int, total: Int, operation: String) -> Unit)?
    ): File {
        val structureResult = ZipUtils.validateZipStructure(zipFile)
        if (structureResult.isFailure) {
            throw IllegalArgumentException("Invalid ZIP structure: ${structureResult.exceptionOrNull()?.message}")
        }

        progressCallback?.invoke(10, 100, "Extracting ZIP archive")

        val tempDir = File(context.cacheDir, "import_temp_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        val extractResult = ZipUtils.extractZip(zipFile, tempDir)
        if (extractResult.isFailure) {
            throw Exception("Failed to extract ZIP: ${extractResult.exceptionOrNull()?.message}")
        }

        return tempDir
    }

    /**
     * Read and parse metadata.json from extracted ZIP
     */
    private suspend fun readMetadataFromExtractedZip(tempDir: File): AppBackup {
        val metadataFile = File(tempDir, ZipUtils.METADATA_FILE)
        if (!metadataFile.exists()) {
            throw FileNotFoundException("metadata.json not found in ZIP")
        }

        val backupJson = metadataFile.readText()
        return try {
            json.decodeFromString<AppBackup>(backupJson)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid backup metadata format: ${e.message}")
        }
    }

    /**
     * Import photos from ZIP backup with file restoration
     */
    private suspend fun importPhotosFromZip(
        backupData: AppBackup,
        tempDir: File,
        strategy: ImportStrategy,
        totalItems: Int,
        startIndex: Int,
        errors: MutableList<String>,
        warnings: MutableList<String>,
        onProgress: suspend (ImportProgress) -> Unit
    ): Triple<Int, Int, Int> {
        var processedItems = startIndex
        var photosImported = 0
        var photosSkipped = 0
        var photoFilesRestored = 0

        val photosDir = File(tempDir, "photos")
        val internalPhotosDir = File(context.filesDir, "photos")
        internalPhotosDir.mkdirs()

        for (photoBackup in backupData.photos) {
            val result = processPhotoFromZip(
                photoBackup,
                backupData,
                photosDir,
                internalPhotosDir,
                strategy,
                warnings
            )

            when (result) {
                is ZipPhotoImportResult.Imported -> {
                    photosImported++
                    if (result.fileRestored) photoFilesRestored++
                }
                is ZipPhotoImportResult.Skipped -> photosSkipped++
                is ZipPhotoImportResult.Failed -> {
                    errors.add("Failed to import photo '${photoBackup.name}': ${result.error}")
                    Log.e(TAG, "Error importing photo: ${photoBackup.name}", result.exception)
                }
            }

            processedItems++
            onProgress(ImportProgress(totalItems, processedItems, "Importing photos", errors))
        }

        return Triple(photosImported, photosSkipped, photoFilesRestored)
    }

    /**
     * Process single photo import from ZIP
     */
    private suspend fun processPhotoFromZip(
        photoBackup: BackupPhoto,
        backupData: AppBackup,
        photosDir: File,
        internalPhotosDir: File,
        strategy: ImportStrategy,
        warnings: MutableList<String>
    ): ZipPhotoImportResult {
        return try {
            val (newPhotoPath, fileRestored) = restorePhotoFileFromZip(
                photoBackup,
                backupData,
                photosDir,
                internalPhotosDir,
                warnings
            )

            if (strategy == ImportStrategy.MERGE && isDuplicatePhoto(newPhotoPath)) {
                warnings.add("Skipped duplicate photo: ${photoBackup.name}")
                return ZipPhotoImportResult.Skipped
            }

            val actualCategoryId = resolveCategoryId(photoBackup, backupData, strategy)
                ?: photoBackup.categoryId

            val photoToInsert = if (strategy == ImportStrategy.REPLACE) {
                photoBackup.toPhoto().copy(path = newPhotoPath)
            } else {
                photoBackup.toPhoto().copy(
                    id = 0,
                    categoryId = actualCategoryId,
                    path = newPhotoPath
                )
            }

            photoRepository.insertPhoto(photoToInsert)
            ZipPhotoImportResult.Imported(fileRestored)

        } catch (e: Exception) {
            ZipPhotoImportResult.Failed(e.message ?: "Unknown error", e)
        }
    }

    /**
     * Restore photo file from ZIP to internal storage
     */
    private fun restorePhotoFileFromZip(
        photoBackup: BackupPhoto,
        backupData: AppBackup,
        photosDir: File,
        internalPhotosDir: File,
        warnings: MutableList<String>
    ): Pair<String, Boolean> {
        val manifestEntry = backupData.photoManifest.find { it.photoId == photoBackup.id }
        var newPhotoPath = photoBackup.path
        var fileRestored = false

        if (manifestEntry != null) {
            val sourceFile = File(photosDir, manifestEntry.fileName)
            if (sourceFile.exists()) {
                val destFile = File(internalPhotosDir, manifestEntry.fileName)
                sourceFile.copyTo(destFile, overwrite = true)
                newPhotoPath = destFile.absolutePath
                fileRestored = true
            } else {
                warnings.add("Photo file not found in ZIP: ${manifestEntry.fileName}")
            }
        }

        return Pair(newPhotoPath, fileRestored)
    }

    /**
     * Check if photo already exists (duplicate detection)
     */
    private suspend fun isDuplicatePhoto(photoPath: String): Boolean {
        val existingPhotos = photoRepository.getAllPhotos()
        return existingPhotos.any { it.path == photoPath }
    }

    /**
     * Result type for ZIP photo import operations
     */
    private sealed class ZipPhotoImportResult {
        data class Imported(val fileRestored: Boolean) : ZipPhotoImportResult()
        object Skipped : ZipPhotoImportResult()
        data class Failed(val error: String, val exception: Exception) : ZipPhotoImportResult()
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
                missingPhotos = missingPhotos.map { "${it.name} (${it.path})" },
                isZipFormat = backupData.format == BackupFormat.ZIP.name
            )

            Result.success(preview)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate ZIP backup file
     */
    private suspend fun validateZipBackupFile(zipFile: File): Result<AppBackup> = withContext(Dispatchers.IO) {
        try {
            val structureResult = ZipUtils.validateZipStructure(zipFile)
            if (structureResult.isFailure) {
                return@withContext Result.failure(structureResult.exceptionOrNull()!!)
            }

            // Extract and read metadata
            val tempDir = File(context.cacheDir, "validate_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            val extractResult = ZipUtils.extractZip(zipFile, tempDir)
            if (extractResult.isFailure) {
                tempDir.deleteRecursively()
                return@withContext Result.failure(extractResult.exceptionOrNull()!!)
            }

            val metadataFile = File(tempDir, ZipUtils.METADATA_FILE)
            val backupJson = metadataFile.readText()
            val backupData = json.decodeFromString<AppBackup>(backupJson)

            checkBackupVersion(backupData.version)

            tempDir.deleteRecursively()
            Result.success(backupData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calculate MD5 checksum for file integrity verification
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
     * Export app data with comprehensive backup options
     */
    private suspend fun exportToZipWithOptions(
        options: BackupOptions,
        tempDir: File? = null,
        progressCallback: ((current: Int, total: Int, operation: String) -> Unit)? = null
    ): Result<File> {
        return try {
            val workDir = tempDir ?: File(context.cacheDir, "backup_temp_${System.currentTimeMillis()}")
            workDir.mkdirs()

            progressCallback?.invoke(0, 100, "Gathering app data")

            // Filter data based on options
            val categories = if (options.selectedCategories != null) {
                categoryRepository.getAllCategories().filter { it.id in options.selectedCategories }
            } else {
                categoryRepository.getAllCategories()
            }

            var photos = photoRepository.getAllPhotos().filter { photo ->
                // Filter by categories if specified
                (options.selectedCategories == null || photo.categoryId in options.selectedCategories) &&
                // Filter by date range if specified
                (options.dateRangeStart == null || photo.createdAt >= options.dateRangeStart) &&
                (options.dateRangeEnd == null || photo.createdAt <= options.dateRangeEnd)
            }

            val isDarkMode = themeManager.isDarkMode.first()
            val securitySummary = securePreferencesManager.getSecuritySummary()

            progressCallback?.invoke(20, 100, "Preparing metadata")

            // Create directories
            val photoManifest = mutableListOf<PhotoManifestEntry>()
            val photosDir = if (options.includePhotos) File(workDir, "photos").apply { mkdirs() } else null
            val thumbnailsDir = if (options.includeThumbnails) File(workDir, THUMBNAIL_DIR).apply { mkdirs() } else null

            progressCallback?.invoke(30, 100, "Processing photos")

            // Process photos with compression
            if (options.includePhotos) {
                var photosCopied = 0
                photos.forEachIndexed { index, photo ->
                    try {
                        if (!photo.isFromAssets) {
                            val sourceFile = File(photo.path)
                            if (sourceFile.exists()) {
                                val fileName = "${photo.id}_${sourceFile.name}"
                                val destFile = File(photosDir!!, fileName)

                                // Apply compression if needed
                                when (options.compressionLevel) {
                                    CompressionLevel.HIGH -> compressPhoto(sourceFile, destFile, 70)
                                    CompressionLevel.MEDIUM -> compressPhoto(sourceFile, destFile, 85)
                                    CompressionLevel.LOW -> sourceFile.copyTo(destFile, overwrite = true)
                                }

                                // Generate thumbnail if requested
                                if (options.includeThumbnails) {
                                    generateThumbnail(sourceFile, File(thumbnailsDir!!, "thumb_$fileName"))
                                }

                                val checksum = calculateMD5(destFile)

                                photoManifest.add(
                                    PhotoManifestEntry(
                                        photoId = photo.id,
                                        originalPath = photo.path,
                                        zipEntryName = "photos/$fileName",
                                        fileName = fileName,
                                        fileSize = destFile.length(),
                                        checksum = checksum
                                    )
                                )
                                photosCopied++
                            }
                        }

                        val progress = 30 + ((index + 1) * 40 / photos.size)
                        progressCallback?.invoke(progress, 100, "Processing photos ($photosCopied/${photos.size})")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to process photo: ${photo.path}", e)
                    }
                }
            }

            progressCallback?.invoke(70, 100, "Creating backup metadata")

            // Prepare backup data
            val backupCategories = categories.map { BackupCategory.fromCategory(it) }
            val backupPhotos = photos.map { BackupPhoto.fromPhoto(it) }

            val backupSettings = if (options.includeSettings) {
                BackupSettings(
                    isDarkMode = isDarkMode,
                    securitySettings = BackupSecuritySettings(
                        hasPIN = securitySummary.hasPIN,
                        hasPattern = securitySummary.hasPattern,
                        kidSafeModeEnabled = securitySummary.kidSafeModeEnabled,
                        deleteProtectionEnabled = securitySummary.deleteProtectionEnabled
                    )
                )
            } else {
                BackupSettings(
                    isDarkMode = false,
                    securitySettings = BackupSecuritySettings(
                        hasPIN = false,
                        hasPattern = false,
                        kidSafeModeEnabled = false,
                        deleteProtectionEnabled = false
                    )
                )
            }

            val appVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: Exception) {
                "unknown"
            }

            val appBackup = AppBackup(
                version = CURRENT_BACKUP_VERSION,
                exportDate = System.currentTimeMillis(),
                appVersion = appVersion,
                format = BackupFormat.ZIP.name,
                categories = backupCategories,
                photos = backupPhotos,
                settings = backupSettings,
                photoManifest = photoManifest
            )

            // Write metadata.json
            val metadataFile = File(workDir, ZipUtils.METADATA_FILE)
            val jsonString = json.encodeToString(appBackup)
            metadataFile.writeText(jsonString)

            // Write categories.json for easier access
            val categoriesFile = File(workDir, "categories.json")
            categoriesFile.writeText(json.encodeToString(backupCategories))

            progressCallback?.invoke(80, 100, "Creating ZIP archive")

            // Create ZIP file with appropriate compression
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val zipFile = File(context.cacheDir, "SmilePile_Backup_${timestamp}.zip")

            val compressionLevel = when (options.compressionLevel) {
                CompressionLevel.HIGH -> Deflater.BEST_COMPRESSION
                CompressionLevel.MEDIUM -> Deflater.DEFAULT_COMPRESSION
                CompressionLevel.LOW -> Deflater.BEST_SPEED
            }

            val zipResult = ZipUtils.createZipFromDirectory(
                sourceDir = workDir,
                outputFile = zipFile,
                compressionLevel = compressionLevel
            ) { current, total ->
                val progress = 80 + (current * 20 / total)
                progressCallback?.invoke(progress, 100, "Archiving files ($current/$total)")
            }

            // Clean up temp directory
            workDir.deleteRecursively()

            if (zipResult.isSuccess) {
                progressCallback?.invoke(100, 100, "Export completed")

                // Save to backup history
                saveBackupHistory(
                    BackupHistoryEntry(
                        timestamp = System.currentTimeMillis(),
                        fileName = zipFile.name,
                        filePath = zipFile.absolutePath,
                        fileSize = zipFile.length(),
                        format = BackupFormat.ZIP,
                        photosCount = photos.size,
                        categoriesCount = categories.size,
                        compressionLevel = options.compressionLevel,
                        success = true
                    )
                )

                Result.success(zipFile)
            } else {
                Result.failure(zipResult.exceptionOrNull() ?: Exception("ZIP creation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Perform incremental backup
     */
    suspend fun performIncrementalBackup(
        baseBackupId: String,
        options: BackupOptions = BackupOptions(),
        progressCallback: ((current: Int, total: Int, operation: String) -> Unit)? = null
    ): Result<File> {
        return try {
            val lastBackup = getBackupHistory().find { it.id == baseBackupId }
                ?: return Result.failure(Exception("Base backup not found"))

            progressCallback?.invoke(0, 100, "Analyzing changes since last backup")

            // Get changes since last backup
            // TODO: Implement getPhotosModifiedAfter in PhotoRepository
            // val changedPhotos = photoRepository.getPhotosModifiedAfter(lastBackup.timestamp)
            // val changedCategories = categoryRepository.getCategoriesModifiedAfter(lastBackup.timestamp)

            // For now, get all photos/categories as a placeholder
            val changedPhotos = photoRepository.getAllPhotos().filter { it.createdAt > lastBackup.timestamp }
            val changedCategories = categoryRepository.getAllCategories().filter { it.createdAt > lastBackup.timestamp }

            if (changedPhotos.isEmpty() && changedCategories.isEmpty()) {
                return Result.failure(Exception("No changes since last backup"))
            }

            progressCallback?.invoke(20, 100, "Creating incremental backup")

            // Get deletion records since last backup
            val deletionRecords = deletionTracker.getDeletionsSince(lastBackup.timestamp)
            val deletedPhotos = deletionRecords
                .filter { it.entityType == EntityType.PHOTO }
                .map { it.entityId }
            val deletedCategories = deletionRecords
                .filter { it.entityType == EntityType.CATEGORY }
                .map { it.entityId }

            val incrementalMetadata = IncrementalBackupMetadata(
                baseBackupId = baseBackupId,
                baseBackupDate = lastBackup.timestamp,
                changedPhotos = changedPhotos.map { it.id },
                deletedPhotos = deletedPhotos.map { it.toLong() },
                changedCategories = changedCategories.map { it.id },
                deletedCategories = deletedCategories.map { it.toLong() }
            )

            // Create backup with only changed items
            val incrementalOptions = options.copy(
                selectedCategories = changedCategories.map { it.id },
                dateRangeStart = lastBackup.timestamp
            )

            exportToZipWithOptions(incrementalOptions, null, progressCallback)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Compress photo with quality setting
     */
    private suspend fun compressPhoto(source: File, dest: File, quality: Int) = withContext(Dispatchers.IO) {
        try {
            val bitmap = android.graphics.BitmapFactory.decodeFile(source.absolutePath)
            val outputStream = FileOutputStream(dest)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.close()
            bitmap.recycle()
        } catch (e: Exception) {
            // If compression fails, fall back to copy
            source.copyTo(dest, overwrite = true)
        }
    }

    /**
     * Generate thumbnail for photo
     */
    private suspend fun generateThumbnail(source: File, dest: File) = withContext(Dispatchers.IO) {
        try {
            val bitmap = android.graphics.BitmapFactory.decodeFile(source.absolutePath)
            val thumbnailSize = 200 // pixels
            val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(
                bitmap,
                thumbnailSize,
                thumbnailSize,
                true
            )
            val outputStream = FileOutputStream(dest)
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.close()
            bitmap.recycle()
            scaledBitmap.recycle()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to generate thumbnail for ${source.name}", e)
        }
    }

    /**
     * Encrypt security settings for sensitive data using AES-256-GCM with 600,000 PBKDF2 iterations
     */

    /**
     * Save backup history entry
     */
    private suspend fun saveBackupHistory(entry: BackupHistoryEntry) {
        try {
            val prefs = context.getSharedPreferences("backup_history", Context.MODE_PRIVATE)
            val history = getBackupHistory().toMutableList()
            history.add(0, entry) // Add at beginning

            // Keep only last 20 backups
            if (history.size > 20) {
                history.subList(20, history.size).clear()
            }

            val jsonHistory = json.encodeToString(history)
            prefs.edit().putString("history", jsonHistory).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save backup history", e)
        }
    }

    /**
     * Get backup history
     */
    suspend fun getBackupHistory(): List<BackupHistoryEntry> {
        return try {
            val prefs = context.getSharedPreferences("backup_history", Context.MODE_PRIVATE)
            val jsonHistory = prefs.getString("history", null) ?: return emptyList()
            json.decodeFromString<List<BackupHistoryEntry>>(jsonHistory)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load backup history", e)
            emptyList()
        }
    }

    /**
     * Schedule automatic backup
     */
    suspend fun scheduleBackup(schedule: BackupSchedule) {
        // This would integrate with WorkManager for scheduled backups
        // Implementation would depend on the actual scheduling requirements
        val prefs = context.getSharedPreferences("backup_settings", Context.MODE_PRIVATE)
        val jsonSchedule = json.encodeToString(schedule)
        prefs.edit().putString("schedule", jsonSchedule).apply()
    }

    /**
     * Get backup schedule
     */
    suspend fun getBackupSchedule(): BackupSchedule? {
        return try {
            val prefs = context.getSharedPreferences("backup_settings", Context.MODE_PRIVATE)
            val jsonSchedule = prefs.getString("schedule", null) ?: return null
            json.decodeFromString<BackupSchedule>(jsonSchedule)
        } catch (e: Exception) {
            null
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
    val missingPhotos: List<String>,
    val isZipFormat: Boolean = false
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