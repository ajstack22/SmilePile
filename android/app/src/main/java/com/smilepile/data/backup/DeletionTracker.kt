package com.smilepile.data.backup

import android.content.Context
import android.os.StatFs
import android.util.Log
import androidx.room.*
import com.smilepile.data.database.SmilePileDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Entity types that can be tracked for deletion
 */
enum class EntityType {
    PHOTO,
    CATEGORY,
    PHOTO_CATEGORY_JOIN
}

/**
 * Entity for tracking deleted items
 */
@Entity(tableName = "deletion_tracking")
data class DeletionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "entity_type")
    val entityType: EntityType,
    @ColumnInfo(name = "entity_id")
    val entityId: String,
    @ColumnInfo(name = "metadata")
    val metadata: ByteArray,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DeletionRecord

        if (id != other.id) return false
        if (entityType != other.entityType) return false
        if (entityId != other.entityId) return false
        if (!metadata.contentEquals(other.metadata)) return false
        if (deletedAt != other.deletedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + entityType.hashCode()
        result = 31 * result + entityId.hashCode()
        result = 31 * result + metadata.contentHashCode()
        result = 31 * result + deletedAt.hashCode()
        return result
    }
}

/**
 * Compressed archive for old deletion records
 */
@Entity(tableName = "deletion_archives")
data class CompressedArchive(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "data")
    val data: ByteArray,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CompressedArchive

        if (id != other.id) return false
        if (!data.contentEquals(other.data)) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}

/**
 * DAO for deletion tracking
 */
@Dao
interface DeletionDao {
    @Query("SELECT COUNT(*) FROM deletion_tracking")
    suspend fun getCount(): Int

    @Query("DELETE FROM deletion_tracking WHERE id IN (SELECT id FROM deletion_tracking ORDER BY deleted_at ASC LIMIT :count)")
    suspend fun purgeOldest(count: Int)

    @Query("SELECT * FROM deletion_tracking WHERE deleted_at < :threshold")
    suspend fun getOldRecords(threshold: Long): List<DeletionRecord>

    @Insert
    suspend fun insert(record: DeletionRecord)

    @Delete
    suspend fun deleteRecords(records: List<DeletionRecord>)

    @Query("SELECT * FROM deletion_tracking")
    suspend fun getAllRecords(): List<DeletionRecord>

    @Query("SELECT * FROM deletion_tracking WHERE entity_type = :entityType")
    suspend fun getRecordsByType(entityType: EntityType): List<DeletionRecord>

    @Query("DELETE FROM deletion_tracking WHERE deleted_at < :threshold")
    suspend fun deleteOlderThan(threshold: Long)

    @Query("SELECT * FROM deletion_tracking WHERE deleted_at >= :since")
    suspend fun getRecordsSince(since: Long): List<DeletionRecord>
}

/**
 * DAO for compressed archives
 */
@Dao
interface ArchiveDao {
    @Insert
    suspend fun insert(archive: CompressedArchive)

    @Query("SELECT * FROM deletion_archives")
    suspend fun getAllArchives(): List<CompressedArchive>

    @Query("DELETE FROM deletion_archives WHERE created_at < :threshold")
    suspend fun deleteOlderThan(threshold: Long)
}

/**
 * Managed deletion tracker with storage limits and compression
 * Implements security patches from Sprint 6
 */
@Singleton
class ManagedDeletionTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: SmilePileDatabase
) {
    companion object {
        private const val TAG = "ManagedDeletionTracker"
        private const val MAX_DELETION_RECORDS = 10_000
        private const val LOW_STORAGE_THRESHOLD = 100L * 1024 * 1024 // 100MB
        private const val COMPRESSION_THRESHOLD = 1000 // Compress when > 1000 records
        private const val METADATA_MAX_SIZE = 1024 // 1KB per record
        private const val PURGE_BATCH_SIZE = 100
        private const val THIRTY_DAYS_MILLIS = 30L * 24 * 60 * 60 * 1000
    }

    private val dao: DeletionDao = database.deletionDao()
    private val archiveDao: ArchiveDao = database.archiveDao()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Track a deletion with storage management
     */
    suspend fun trackDeletion(
        entityType: EntityType,
        entityId: String,
        metadata: Map<String, Any> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        try {
            // Check available storage
            val availableStorage = getAvailableStorage()
            if (availableStorage < LOW_STORAGE_THRESHOLD) {
                // Emergency cleanup
                dao.purgeOldest(PURGE_BATCH_SIZE)
                Log.w(TAG, "Low storage: purging old deletion records")
            }

            // Check record count
            val currentCount = dao.getCount()
            if (currentCount >= MAX_DELETION_RECORDS) {
                dao.purgeOldest(PURGE_BATCH_SIZE)
                Log.i(TAG, "Max records reached: purging oldest $PURGE_BATCH_SIZE records")
            }

            // Validate and compress metadata
            val validatedMetadata = validateMetadata(metadata)
            val compressedMetadata = compressMetadata(validatedMetadata)

            require(compressedMetadata.size <= METADATA_MAX_SIZE) {
                "Metadata exceeds maximum size of $METADATA_MAX_SIZE bytes"
            }

            // Store deletion record
            dao.insert(
                DeletionRecord(
                    entityType = entityType,
                    entityId = entityId,
                    metadata = compressedMetadata,
                    deletedAt = System.currentTimeMillis()
                )
            )

            // Compress old records if needed
            if (currentCount > COMPRESSION_THRESHOLD) {
                compressOldRecords()
            }

            Log.d(TAG, "Tracked deletion: $entityType/$entityId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to track deletion: $entityType/$entityId", e)
            // Don't throw - deletion tracking should not break the app
        }
    }

    /**
     * Get deletion records since a specific timestamp
     */
    suspend fun getDeletionsSince(timestamp: Long): List<DeletionRecord> = withContext(Dispatchers.IO) {
        dao.getRecordsSince(timestamp)
    }

    /**
     * Get all deletion records for backup
     */
    suspend fun getAllDeletions(): List<DeletionRecord> = withContext(Dispatchers.IO) {
        dao.getAllRecords()
    }

    /**
     * Clean up old deletion records
     */
    suspend fun cleanupOldRecords(daysToKeep: Int = 90) = withContext(Dispatchers.IO) {
        val threshold = System.currentTimeMillis() - (daysToKeep * 24L * 60 * 60 * 1000)
        dao.deleteOlderThan(threshold)
        archiveDao.deleteOlderThan(threshold)
        Log.i(TAG, "Cleaned up deletion records older than $daysToKeep days")
    }

    /**
     * Compress old records to save space
     */
    private suspend fun compressOldRecords() = withContext(Dispatchers.IO) {
        try {
            val thirtyDaysAgo = System.currentTimeMillis() - THIRTY_DAYS_MILLIS
            val oldRecords = dao.getOldRecords(thirtyDaysAgo)

            val result = if (oldRecords.size > 100) {
                // Convert to serializable format
                val recordsData = oldRecords.map { record ->
                    mapOf(
                        "entityType" to record.entityType.name,
                        "entityId" to record.entityId,
                        "metadata" to decompressMetadata(record.metadata),
                        "deletedAt" to record.deletedAt
                    )
                }

                // Archive to compressed format
                val compressed = compress(json.encodeToString(recordsData))
                archiveDao.insert(CompressedArchive(data = compressed))

                // Remove from main table
                dao.deleteRecords(oldRecords)

                Log.i(TAG, "Compressed ${oldRecords.size} old deletion records")
            } else {
                // No compression needed
                Unit
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compress old records", e)
        }
    }

    /**
     * Validate metadata to prevent sensitive data leakage
     */
    private fun validateMetadata(metadata: Map<String, Any>): Map<String, Any> {
        return metadata.filter { (key, value) ->
            key.length <= 50 &&
            value.toString().length <= 500 &&
            !containsSensitiveData(value.toString())
        }
    }

    /**
     * Check if string contains sensitive data patterns
     */
    private fun containsSensitiveData(value: String): Boolean {
        val sensitivePatterns = listOf(
            "password", "pin", "pattern", "token", "key", "secret",
            "credential", "auth", "session"
        )
        val lowerValue = value.lowercase()
        return sensitivePatterns.any { pattern -> lowerValue.contains(pattern) }
    }

    /**
     * Compress metadata to save storage
     */
    private fun compressMetadata(metadata: Map<String, Any>): ByteArray {
        return if (metadata.isEmpty()) {
            ByteArray(0)
        } else {
            compress(json.encodeToString(metadata))
        }
    }

    /**
     * Decompress metadata for reading
     */
    private fun decompressMetadata(compressed: ByteArray): Map<String, Any> {
        return if (compressed.isEmpty()) {
            emptyMap()
        } else {
            try {
                json.decodeFromString(decompress(compressed))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to decompress metadata", e)
                emptyMap()
            }
        }
    }

    /**
     * GZIP compression
     */
    private fun compress(data: String): ByteArray {
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).use { gzip ->
            gzip.write(data.toByteArray())
        }
        return outputStream.toByteArray()
    }

    /**
     * GZIP decompression
     */
    private fun decompress(compressed: ByteArray): String {
        val inputStream = ByteArrayInputStream(compressed)
        return GZIPInputStream(inputStream).use { gzip ->
            gzip.readBytes().toString(Charsets.UTF_8)
        }
    }

    /**
     * Get available storage space
     */
    private fun getAvailableStorage(): Long {
        return try {
            val statFs = StatFs(context.filesDir.absolutePath)
            statFs.availableBytes
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get available storage", e)
            Long.MAX_VALUE // Assume plenty of space if we can't check
        }
    }

    /**
     * Get deletion statistics for monitoring
     */
    suspend fun getStatistics(): DeletionStatistics = withContext(Dispatchers.IO) {
        val totalRecords = dao.getCount()
        val photoRecords = dao.getRecordsByType(EntityType.PHOTO).size
        val categoryRecords = dao.getRecordsByType(EntityType.CATEGORY).size
        val archives = archiveDao.getAllArchives().size
        val availableStorage = getAvailableStorage()

        DeletionStatistics(
            totalRecords = totalRecords,
            photoRecords = photoRecords,
            categoryRecords = categoryRecords,
            compressedArchives = archives,
            availableStorageBytes = availableStorage,
            isNearLimit = totalRecords >= MAX_DELETION_RECORDS * 0.9
        )
    }
}

/**
 * Statistics about deletion tracking
 */
@Serializable
data class DeletionStatistics(
    val totalRecords: Int,
    val photoRecords: Int,
    val categoryRecords: Int,
    val compressedArchives: Int,
    val availableStorageBytes: Long,
    val isNearLimit: Boolean
)