package com.smilepile.storage

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.*
import kotlin.math.max

/**
 * Utility class for ZIP operations with security protections
 * Handles creating and extracting ZIP files for photo backup/restore
 */
object ZipUtils {
    private const val TAG = "ZipUtils"

    // Security limits to prevent ZIP bombs
    private const val MAX_ENTRIES = 10000
    private const val MAX_UNCOMPRESSED_SIZE = 1024L * 1024L * 1024L // 1GB
    private const val MAX_COMPRESSION_RATIO = 100
    private const val BUFFER_SIZE = 8192

    // Standard directory structure in ZIP
    const val PHOTOS_DIR = "photos/"
    const val METADATA_FILE = "metadata.json"

    /**
     * Create a ZIP file from a directory containing photos and metadata
     * @param sourceDir Directory containing the files to ZIP
     * @param outputFile Target ZIP file
     * @param compressionLevel Compression level (Deflater.BEST_SPEED, DEFAULT_COMPRESSION, or BEST_COMPRESSION)
     * @param progressCallback Optional callback for progress updates (current, total)
     * @return Result indicating success or failure
     */
    suspend fun createZipFromDirectory(
        sourceDir: File,
        outputFile: File,
        compressionLevel: Int = Deflater.DEFAULT_COMPRESSION,
        progressCallback: ((current: Int, total: Int) -> Unit)? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!sourceDir.exists() || !sourceDir.isDirectory) {
                return@withContext Result.failure(IllegalArgumentException("Source directory does not exist"))
            }

            // Get all files to include in ZIP
            val filesToZip = mutableListOf<Pair<File, String>>()

            // Add metadata.json if it exists
            val metadataFile = File(sourceDir, METADATA_FILE)
            if (metadataFile.exists()) {
                filesToZip.add(metadataFile to METADATA_FILE)
            }

            // Add all files in photos directory
            val photosDir = File(sourceDir, "photos")
            if (photosDir.exists() && photosDir.isDirectory) {
                photosDir.listFiles()?.forEach { photoFile ->
                    if (photoFile.isFile) {
                        filesToZip.add(photoFile to "$PHOTOS_DIR${photoFile.name}")
                    }
                }
            }

            if (filesToZip.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No files found to archive"))
            }

            val totalFiles = filesToZip.size
            var processedFiles = 0

            // Create ZIP file with specified compression level
            ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zipOut ->
                zipOut.setLevel(compressionLevel)

                filesToZip.forEach { (file, entryName) ->
                    try {
                        // Create ZIP entry
                        val entry = ZipEntry(entryName).apply {
                            time = file.lastModified()
                            size = file.length()
                        }
                        zipOut.putNextEntry(entry)

                        // Copy file data
                        BufferedInputStream(FileInputStream(file)).use { input ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                zipOut.write(buffer, 0, bytesRead)
                            }
                        }

                        zipOut.closeEntry()
                        processedFiles++
                        progressCallback?.invoke(processedFiles, totalFiles)

                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to add file to ZIP: ${file.name}", e)
                        throw e
                    }
                }
            }

            Log.i(TAG, "Successfully created ZIP with $processedFiles files: ${outputFile.absolutePath}")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create ZIP file", e)
            Result.failure(e)
        }
    }

    /**
     * Add individual files to a ZIP stream
     * @param zipOut Target ZIP output stream
     * @param files List of files to add with their entry names
     * @param progressCallback Optional callback for progress updates
     */
    suspend fun addFilesToZip(
        zipOut: ZipOutputStream,
        files: List<Pair<File, String>>,
        progressCallback: ((current: Int, total: Int) -> Unit)? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var processedFiles = 0
            val totalFiles = files.size

            files.forEach { (file, entryName) ->
                if (!file.exists() || !file.isFile) {
                    Log.w(TAG, "Skipping non-existent file: ${file.absolutePath}")
                    return@forEach
                }

                // Validate entry name for security
                val sanitizedEntryName = sanitizeEntryName(entryName)

                val entry = ZipEntry(sanitizedEntryName).apply {
                    time = file.lastModified()
                    size = file.length()
                }

                zipOut.putNextEntry(entry)

                BufferedInputStream(FileInputStream(file)).use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        zipOut.write(buffer, 0, bytesRead)
                    }
                }

                zipOut.closeEntry()
                processedFiles++
                progressCallback?.invoke(processedFiles, totalFiles)
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to add files to ZIP", e)
            Result.failure(e)
        }
    }

    /**
     * Extract ZIP file to destination directory with security protections
     * @param zipFile Source ZIP file
     * @param destDir Destination directory
     * @param progressCallback Optional callback for progress updates
     * @return Result containing list of extracted files or error
     */
    suspend fun extractZip(
        zipFile: File,
        destDir: File,
        progressCallback: ((current: Int, total: Int) -> Unit)? = null
    ): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            if (!zipFile.exists() || !zipFile.isFile) {
                return@withContext Result.failure(IllegalArgumentException("ZIP file does not exist"))
            }

            if (!destDir.exists()) {
                destDir.mkdirs()
            }

            val validationResult = validateZipSecurityFirstPass(zipFile)
            if (validationResult.isFailure) {
                return@withContext Result.failure(validationResult.exceptionOrNull()!!)
            }

            val entryCount = validationResult.getOrNull() ?: 0

            val structureValidation = validateZipStructure(zipFile)
            if (structureValidation.isFailure) {
                return@withContext Result.failure(structureValidation.exceptionOrNull()!!)
            }

            extractZipSecondPass(zipFile, destDir, entryCount, progressCallback)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract ZIP file", e)
            Result.failure(e)
        }
    }

    /**
     * Validate ZIP structure for backup format
     * Checks for required metadata.json and photos/ directory
     * @param zipFile ZIP file to validate
     * @return Result indicating if structure is valid
     */
    suspend fun validateZipStructure(zipFile: File): Result<ZipStructure> = withContext(Dispatchers.IO) {
        try {
            var hasMetadata = false
            var hasPhotosDir = false
            var photoCount = 0
            val photoFiles = mutableListOf<String>()

            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zipIn ->
                var entry: ZipEntry?
                while (zipIn.nextEntry.also { entry = it } != null) {
                    val currentEntry = entry!!
                    val entryName = currentEntry.name

                    when {
                        entryName == METADATA_FILE -> {
                            hasMetadata = true
                        }
                        entryName.startsWith(PHOTOS_DIR) && !currentEntry.isDirectory -> {
                            hasPhotosDir = true
                            photoCount++
                            photoFiles.add(entryName.removePrefix(PHOTOS_DIR))
                        }
                        entryName == PHOTOS_DIR -> {
                            hasPhotosDir = true
                        }
                    }

                    zipIn.closeEntry()
                }
            }

            if (!hasMetadata) {
                return@withContext Result.failure(
                    IllegalArgumentException("ZIP does not contain required metadata.json")
                )
            }

            val structure = ZipStructure(
                hasMetadata = hasMetadata,
                hasPhotosDirectory = hasPhotosDir,
                photoCount = photoCount,
                photoFiles = photoFiles
            )

            Result.success(structure)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to validate ZIP structure", e)
            Result.failure(e)
        }
    }

    /**
     * Sanitize entry name to prevent path traversal attacks
     * @param entryName Original entry name from ZIP
     * @return Sanitized entry name
     */
    private fun sanitizeEntryName(entryName: String): String {
        // Remove any path traversal attempts
        val sanitized = entryName.replace("\\", "/")
            .replace("../", "")
            .replace("..\\", "")
            .trim('/')

        // Ensure we don't have absolute paths
        return if (sanitized.startsWith("/")) {
            sanitized.substring(1)
        } else {
            sanitized
        }
    }

    /**
     * Get ZIP file information without extracting
     * @param zipFile ZIP file to analyze
     * @return Result containing ZIP information
     */
    suspend fun getZipInfo(zipFile: File): Result<ZipInfo> = withContext(Dispatchers.IO) {
        try {
            var entryCount = 0
            var totalUncompressedSize = 0L
            var totalCompressedSize = 0L
            val entries = mutableListOf<ZipEntryInfo>()

            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zipIn ->
                var entry: ZipEntry?
                while (zipIn.nextEntry.also { entry = it } != null) {
                    val currentEntry = entry!!
                    entryCount++
                    totalUncompressedSize += currentEntry.size
                    totalCompressedSize += currentEntry.compressedSize

                    entries.add(
                        ZipEntryInfo(
                            name = currentEntry.name,
                            size = currentEntry.size,
                            compressedSize = currentEntry.compressedSize,
                            lastModified = currentEntry.time,
                            isDirectory = currentEntry.isDirectory
                        )
                    )

                    zipIn.closeEntry()
                }
            }

            val zipInfo = ZipInfo(
                entryCount = entryCount,
                totalUncompressedSize = totalUncompressedSize,
                totalCompressedSize = totalCompressedSize,
                compressionRatio = if (totalCompressedSize > 0) {
                    (totalUncompressedSize.toDouble() / totalCompressedSize.toDouble())
                } else 0.0,
                entries = entries
            )

            Result.success(zipInfo)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get ZIP info", e)
            Result.failure(e)
        }
    }

    private suspend fun validateZipSecurityFirstPass(zipFile: File): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var totalUncompressedSize = 0L
            var entryCount = 0

            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zipIn ->
                var entry: ZipEntry?
                while (zipIn.nextEntry.also { entry = it } != null) {
                    val currentEntry = entry!!
                    entryCount++

                    val validationResult = validateZipEntryForSecurity(currentEntry, entryCount, totalUncompressedSize)
                    if (validationResult.isFailure) {
                        return@withContext validationResult
                    }

                    if (currentEntry.size > 0) {
                        totalUncompressedSize += currentEntry.size
                    }

                    zipIn.closeEntry()
                }
            }

            Result.success(entryCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun validateZipEntryForSecurity(
        entry: ZipEntry,
        currentCount: Int,
        currentTotalSize: Long
    ): Result<Int> {
        if (currentCount > MAX_ENTRIES) {
            return Result.failure(
                SecurityException("ZIP contains too many entries (max: $MAX_ENTRIES)")
            )
        }

        val sanitizedName = sanitizeEntryName(entry.name)
        if (sanitizedName != entry.name) {
            return Result.failure(
                SecurityException("ZIP contains unsafe path: ${entry.name}")
            )
        }

        if (entry.size > 0 && currentTotalSize + entry.size > MAX_UNCOMPRESSED_SIZE) {
            return Result.failure(
                SecurityException("ZIP uncompressed size exceeds limit")
            )
        }

        if (entry.size > 0 && entry.compressedSize > 0) {
            val ratio = entry.size / max(entry.compressedSize, 1)
            if (ratio > MAX_COMPRESSION_RATIO) {
                return Result.failure(
                    SecurityException("ZIP compression ratio too high (potential ZIP bomb)")
                )
            }
        }

        return Result.success(0)
    }

    private suspend fun extractZipSecondPass(
        zipFile: File,
        destDir: File,
        totalEntries: Int,
        progressCallback: ((current: Int, total: Int) -> Unit)?
    ): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            val extractedFiles = mutableListOf<File>()
            var processedEntries = 0

            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zipIn ->
                var entry: ZipEntry?
                while (zipIn.nextEntry.also { entry = it } != null) {
                    val currentEntry = entry!!

                    if (currentEntry.isDirectory) {
                        createZipDirectory(destDir, currentEntry.name)
                    } else {
                        val extractedFile = extractZipFile(zipIn, destDir, currentEntry)
                        extractedFiles.add(extractedFile)
                    }

                    zipIn.closeEntry()
                    processedEntries++
                    progressCallback?.invoke(processedEntries, totalEntries)
                }
            }

            Log.i(TAG, "Successfully extracted ${extractedFiles.size} files to: ${destDir.absolutePath}")
            Result.success(extractedFiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createZipDirectory(destDir: File, entryName: String) {
        val dir = File(destDir, entryName)
        dir.mkdirs()
    }

    private fun extractZipFile(
        zipIn: ZipInputStream,
        destDir: File,
        entry: ZipEntry
    ): File {
        val sanitizedName = sanitizeEntryName(entry.name)
        val outputFile = File(destDir, sanitizedName)
        outputFile.parentFile?.mkdirs()

        BufferedOutputStream(FileOutputStream(outputFile)).use { output ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (zipIn.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                if (totalBytesRead > MAX_UNCOMPRESSED_SIZE) {
                    throw SecurityException("File size exceeds limit during extraction")
                }
            }
        }

        if (!outputFile.setLastModified(entry.time)) {
            Log.w(TAG, "Failed to set last modified time for: ${outputFile.name}")
        }

        return outputFile
    }
}

/**
 * Data class representing ZIP file structure validation results
 */
data class ZipStructure(
    val hasMetadata: Boolean,
    val hasPhotosDirectory: Boolean,
    val photoCount: Int,
    val photoFiles: List<String>
)

/**
 * Data class containing ZIP file information
 */
data class ZipInfo(
    val entryCount: Int,
    val totalUncompressedSize: Long,
    val totalCompressedSize: Long,
    val compressionRatio: Double,
    val entries: List<ZipEntryInfo>
)

/**
 * Data class representing individual ZIP entry information
 */
data class ZipEntryInfo(
    val name: String,
    val size: Long,
    val compressedSize: Long,
    val lastModified: Long,
    val isDirectory: Boolean
)