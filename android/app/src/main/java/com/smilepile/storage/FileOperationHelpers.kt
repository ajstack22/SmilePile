package com.smilepile.storage

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Helper functions for safe file operations with proper error handling
 */
object FileOperationHelpers {
    private const val TAG = "FileOperationHelpers"
    private const val TEMP_DIR_NAME = "temp_operations"
    private const val MAX_RETRIES = 3
    private const val RETRY_DELAY_MS = 100L

    // Mutex for directory operations to prevent race conditions
    private val directoryLocks = ConcurrentHashMap<String, Mutex>()

    /**
     * Atomically move a file from source to destination
     * Uses rename when possible, falls back to copy+delete
     */
    suspend fun atomicMove(source: File, destination: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // First try atomic rename (fastest)
                if (source.renameTo(destination)) {
                    return@withContext true
                }

                // Fallback to copy + delete
                val tempFile = File(destination.parentFile, "${destination.name}.tmp")

                // Copy to temp file first
                if (!copyFile(source, tempFile)) {
                    tempFile.delete()
                    return@withContext false
                }

                // Verify copy integrity
                if (!verifyFileIntegrity(source, tempFile)) {
                    tempFile.delete()
                    return@withContext false
                }

                // Atomic rename from temp to final
                if (!tempFile.renameTo(destination)) {
                    tempFile.delete()
                    return@withContext false
                }

                // Delete source only after successful move
                source.delete()
                true
            } catch (e: Exception) {
                Log.e(TAG, "Atomic move failed: ${e.message}")
                false
            }
        }
    }

    /**
     * Copy file with verification
     */
    private suspend fun copyFile(source: File, destination: File): Boolean {
        return withContext(Dispatchers.IO) {
            var sourceChannel: FileChannel? = null
            var destChannel: FileChannel? = null

            try {
                sourceChannel = FileInputStream(source).channel
                destChannel = FileOutputStream(destination).channel

                val size = sourceChannel.size()
                var position = 0L

                while (position < size) {
                    val transferred = sourceChannel.transferTo(
                        position,
                        size - position,
                        destChannel
                    )
                    position += transferred
                }

                true
            } catch (e: IOException) {
                Log.e(TAG, "File copy failed: ${e.message}")
                false
            } finally {
                sourceChannel?.close()
                destChannel?.close()
            }
        }
    }

    /**
     * Verify file integrity using MD5 checksum
     */
    suspend fun verifyFileIntegrity(file1: File, file2: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val hash1 = calculateFileHash(file1)
                val hash2 = calculateFileHash(file2)
                hash1 == hash2
            } catch (e: Exception) {
                Log.e(TAG, "Integrity check failed: ${e.message}")
                false
            }
        }
    }

    /**
     * Calculate MD5 hash of a file
     */
    private suspend fun calculateFileHash(file: File): String {
        return withContext(Dispatchers.IO) {
            val digest = MessageDigest.getInstance("MD5")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
    }

    /**
     * Ensure directory exists with thread-safe creation
     */
    suspend fun ensureDirectoryExists(path: String): File? {
        val mutex = directoryLocks.computeIfAbsent(path) { Mutex() }

        return mutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    val dir = File(path)
                    when {
                        dir.exists() && dir.isDirectory -> dir
                        dir.exists() && !dir.isDirectory -> {
                            Log.e(TAG, "Path exists but is not a directory: $path")
                            null
                        }
                        dir.mkdirs() -> dir
                        else -> {
                            Log.e(TAG, "Failed to create directory: $path")
                            null
                        }
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "Security exception creating directory: ${e.message}")
                    null
                }
            }
        }
    }

    /**
     * Clean up temporary files older than specified age
     */
    suspend fun cleanupTempFiles(context: android.content.Context, maxAgeMs: Long = 3600000L) {
        withContext(Dispatchers.IO) {
            try {
                val tempDir = File(context.cacheDir, TEMP_DIR_NAME)
                if (tempDir.exists() && tempDir.isDirectory) {
                    val cutoffTime = System.currentTimeMillis() - maxAgeMs
                    tempDir.listFiles()?.forEach { file ->
                        if (file.lastModified() < cutoffTime) {
                            file.deleteRecursively()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Temp file cleanup failed: ${e.message}")
            }
            Unit
        }
    }

    /**
     * Safe file deletion with verification
     */
    suspend fun safeDelete(file: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!file.exists()) {
                    return@withContext true
                }

                // For directories, delete recursively
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    // For files, try to overwrite with zeros first (security)
                    try {
                        FileOutputStream(file).use { fos ->
                            val zeros = ByteArray(1024)
                            val size = file.length()
                            var written = 0L
                            while (written < size) {
                                val toWrite = minOf(zeros.size.toLong(), size - written)
                                fos.write(zeros, 0, toWrite.toInt())
                                written += toWrite
                            }
                        }
                    } catch (e: IOException) {
                        // Overwrite failed, continue with normal delete
                    }

                    file.delete()
                }

                // Verify deletion
                !file.exists()
            } catch (e: Exception) {
                Log.e(TAG, "Safe delete failed: ${e.message}")
                false
            }
        }
    }

    /**
     * Get available storage space
     */
    fun getAvailableSpace(file: File): Long {
        return try {
            file.usableSpace
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get available space: ${e.message}")
            0L
        }
    }

    /**
     * Check if file operation will exceed storage quota
     */
    fun checkStorageQuota(file: File, requiredSpace: Long, safetyMargin: Long = 50 * 1024 * 1024): Boolean {
        val available = getAvailableSpace(file)
        return available > (requiredSpace + safetyMargin)
    }
}