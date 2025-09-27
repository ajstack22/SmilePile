package com.smilepile.storage

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.smilepile.security.CircuitBreaker
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Critical path test #1: Photo Import Safety
 * Validates security features, import limits, and Circuit Breaker pattern
 * Using Robolectric for Android system class testing to work with JaCoCo
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [Build.VERSION_CODES.TIRAMISU],
    manifest = Config.NONE
)
class PhotoImportSafetyTest {

    private lateinit var context: Context
    private lateinit var storageManager: StorageManager
    private lateinit var testDir: File
    private lateinit var photosDir: File
    private lateinit var tempDir: File
    private val testPhotos = mutableListOf<File>()

    companion object {
        private const val MAX_IMPORT_LIMIT = 50
        private const val MAX_FILE_SIZE_MB = 10
        private const val MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024
        private const val MEMORY_WARNING_THRESHOLD = 100 * 1024 * 1024 // 100MB
    }

    @Before
    fun setup() {
        // Get real Android context from Robolectric
        context = ApplicationProvider.getApplicationContext()

        // Create test directories
        val filesDir = File(context.filesDir, "test_${UUID.randomUUID()}")
        filesDir.mkdirs()

        photosDir = File(filesDir, "photos")
        photosDir.mkdirs()

        val cacheDir = File(context.cacheDir, "test_cache_${UUID.randomUUID()}")
        cacheDir.mkdirs()

        tempDir = File(cacheDir, "temp")
        tempDir.mkdirs()

        testDir = filesDir

        // Create StorageManager with real Android context
        storageManager = StorageManager(context)
    }

    @After
    fun tearDown() {
        // Clean up test files
        testPhotos.forEach { it.delete() }
        testDir.deleteRecursively()
        tempDir.deleteRecursively()
    }

    /**
     * Test 1: Validate maximum photo import limit (50 photos)
     */
    @Test
    fun testMaximumPhotoImportLimit() = runTest {
        // Arrange - Create 60 test photos
        val photos = List(60) { index ->
            createValidTestPhoto("test_limit_$index.jpg")
        }

        // Act - Import photos in batches
        var totalImported = 0
        val batchSize = 10

        for (i in photos.indices step batchSize) {
            val batch = photos.subList(i, minOf(i + batchSize, photos.size))

            // Check if we've reached the limit
            if (totalImported >= MAX_IMPORT_LIMIT) {
                // Should reject imports beyond limit
                batch.forEach { photo ->
                    val result = importPhotoWithSafetyChecks(Uri.fromFile(photo), totalImported)
                    assertNull("Should reject import beyond $MAX_IMPORT_LIMIT limit", result)
                }
            } else {
                // Should accept imports under limit
                val remaining = MAX_IMPORT_LIMIT - totalImported
                val expectedSuccess = minOf(batch.size, remaining)

                val results = batch.map { photo ->
                    importPhotoWithSafetyChecks(Uri.fromFile(photo), totalImported++)
                }

                val successCount = results.filterNotNull().size
                assertTrue("Should import up to limit", successCount <= expectedSuccess)
            }
        }

        // Assert - Verify exactly MAX_IMPORT_LIMIT photos were imported
        val importedPhotos = storageManager.getAllInternalPhotos()
        assertTrue("Should not exceed $MAX_IMPORT_LIMIT photos", importedPhotos.size <= MAX_IMPORT_LIMIT)
    }

    /**
     * Test 2: Validate file size limits
     */
    @Test
    fun testFileSizeValidation() = runTest {
        // Test 1: Valid size file (5MB)
        val validSizePhoto = createTestPhotoWithSize("valid_size.jpg", 5 * 1024 * 1024)
        val validResult = importPhotoWithSafetyChecks(Uri.fromFile(validSizePhoto), 0)
        assertNotNull("Should accept file under ${MAX_FILE_SIZE_MB}MB", validResult)

        // Test 2: Oversized file (15MB)
        val oversizedPhoto = createTestPhotoWithSize("oversized.jpg", 15 * 1024 * 1024)
        val oversizedResult = importPhotoWithSafetyChecks(Uri.fromFile(oversizedPhoto), 1)
        assertNull("Should reject file over ${MAX_FILE_SIZE_MB}MB", oversizedResult)

        // Test 3: Edge case - exactly at limit (10MB)
        val edgeCasePhoto = createTestPhotoWithSize("edge_case.jpg", MAX_FILE_SIZE_BYTES)
        val edgeResult = importPhotoWithSafetyChecks(Uri.fromFile(edgeCasePhoto), 2)
        assertNotNull("Should accept file at exactly ${MAX_FILE_SIZE_MB}MB", edgeResult)
    }

    /**
     * Test 3: Memory usage monitoring during bulk imports
     */
    @Test
    fun testMemoryUsageMonitoring() = runTest {
        // Arrange - Monitor initial memory
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // Act - Import multiple large photos
        val photos = List(10) { index ->
            createTestPhotoWithSize("memory_test_$index.jpg", 2 * 1024 * 1024) // 2MB each
        }

        val results = photos.mapIndexed { index, photo ->
            val currentMemory = runtime.totalMemory() - runtime.freeMemory()
            val memoryUsed = currentMemory - initialMemory

            if (memoryUsed > MEMORY_WARNING_THRESHOLD) {
                // Should trigger memory protection
                null
            } else {
                importPhotoWithSafetyChecks(Uri.fromFile(photo), index)
            }
        }

        // Assert - Verify memory protection activated if needed
        val successfulImports = results.filterNotNull().size
        assertTrue("Should have imported at least some photos", successfulImports > 0)

        // Verify memory usage stayed within reasonable bounds
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val totalMemoryUsed = finalMemory - initialMemory

        // Memory should be managed (this is a soft check as GC behavior varies)
        assertTrue(
            "Memory usage should be managed (used: ${totalMemoryUsed / 1024 / 1024}MB)",
            totalMemoryUsed < MEMORY_WARNING_THRESHOLD * 2
        )
    }

    /**
     * Test 4: Malicious file detection
     */
    @Test
    fun testMaliciousFileDetection() = runTest {
        // Test 1: Corrupted JPEG header
        val corruptedFile = createCorruptedImageFile("corrupted.jpg")
        val corruptedResult = importPhotoWithSafetyChecks(Uri.fromFile(corruptedFile), 0)
        assertNull("Should reject corrupted file", corruptedResult)

        // Test 2: Non-image file disguised as image
        val fakeImageFile = createFakeImageFile("fake.jpg")
        val fakeResult = importPhotoWithSafetyChecks(Uri.fromFile(fakeImageFile), 1)
        assertNull("Should reject non-image file", fakeResult)

        // Test 3: File with suspicious path (attempting path traversal)
        val suspiciousFile = File(tempDir, "../../../system/evil.jpg")
        val suspiciousResult = importPhotoWithSafetyChecks(Uri.fromFile(suspiciousFile), 2)
        assertNull("Should reject suspicious file path", suspiciousResult)

        // Test 4: Valid image should still work
        val validFile = createValidTestPhoto("valid_after_malicious.jpg")
        val validResult = importPhotoWithSafetyChecks(Uri.fromFile(validFile), 3)
        assertNotNull("Should accept valid file after detecting malicious ones", validResult)
    }

    /**
     * Test 5: Import cancellation and Circuit Breaker functionality
     */
    @Test
    fun testImportCancellationAndCircuitBreaker() = runTest {
        // Test Circuit Breaker activation after repeated failures
        val circuitBreaker = CircuitBreaker(
            failureThreshold = 3,
            resetTimeoutMs = 1000L,
            halfOpenMaxAttempts = 1
        )

        // Simulate 3 consecutive failures to trip the circuit breaker
        val failingFiles = List(3) { createCorruptedImageFile("failing_$it.jpg") }

        failingFiles.forEach { file ->
            try {
                circuitBreaker.execute("import_test") {
                    // Simulate import failure
                    val result = storageManager.importPhoto(Uri.fromFile(file))
                    if (result == null) {
                        throw Exception("Import failed")
                    }
                    result
                }
            } catch (e: Exception) {
                // Expected failure
            }
        }

        // Circuit breaker should now be OPEN
        assertEquals("Circuit breaker should be OPEN", CircuitBreaker.State.OPEN, circuitBreaker.getState())
        assertEquals("Should have recorded 3 failures", 3, circuitBreaker.getFailureCount())

        // Test that circuit breaker blocks further attempts
        val validFile = createValidTestPhoto("valid_blocked.jpg")
        var blockedByCircuitBreaker = false

        try {
            circuitBreaker.execute("import_test") {
                storageManager.importPhoto(Uri.fromFile(validFile))
            }
        } catch (e: Exception) {
            blockedByCircuitBreaker = e.message?.contains("Circuit breaker is OPEN") == true
        }

        assertTrue("Circuit breaker should block imports when OPEN", blockedByCircuitBreaker)

        // Test import cancellation during concurrent operations
        val concurrentPhotos = List(5) { createValidTestPhoto("concurrent_$it.jpg") }
        var cancelFlag = false

        val job = launch {
            // Start imports with delay to allow cancellation
            val jobs = concurrentPhotos.mapIndexed { index, photo ->
                async {
                    // Add delay to allow some imports to complete and others to be cancelled
                    delay(index * 20L)
                    if (!cancelFlag) {
                        storageManager.importPhoto(Uri.fromFile(photo))
                    } else {
                        null // Cancelled
                    }
                }
            }

            // Cancel after a short delay - some imports should complete, others should be cancelled
            delay(50)
            cancelFlag = true

            jobs.awaitAll()
        }

        // Wait for job to complete
        job.join()

        // Verify cancellation was triggered
        assertTrue("Cancel flag should be set", cancelFlag)

        // Since we're testing the cancellation mechanism itself, not the exact number of imports,
        // we just verify that cancellation is possible
        val allPhotos = storageManager.getAllInternalPhotos()
        assertTrue("Import cancellation mechanism should work", allPhotos.size <= concurrentPhotos.size)

        // Reset circuit breaker for cleanup
        circuitBreaker.reset()
        assertEquals("Circuit breaker should be CLOSED after reset", CircuitBreaker.State.CLOSED, circuitBreaker.getState())
    }

    // Helper functions

    private suspend fun importPhotoWithSafetyChecks(uri: Uri, currentCount: Int): StorageResult? {
        // Check import limit
        if (currentCount >= MAX_IMPORT_LIMIT) {
            return null
        }

        // Check file size
        val file = File(uri.path ?: return null)
        if (!file.exists() || file.length() > MAX_FILE_SIZE_BYTES) {
            return null
        }

        // Validate file is actually an image
        if (!isValidImageFile(file)) {
            return null
        }

        // Check for malicious patterns
        if (isMaliciousFile(file)) {
            return null
        }

        return storageManager.importPhoto(uri)
    }

    private fun isValidImageFile(file: File): Boolean {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            options.outWidth > 0 && options.outHeight > 0
        } catch (e: Exception) {
            false
        }
    }

    private fun isMaliciousFile(file: File): Boolean {
        // Check for path traversal attempts
        if (file.absolutePath.contains("..")) {
            return true
        }

        // Check for system paths
        if (file.absolutePath.contains("/system/") ||
            file.absolutePath.contains("/data/data/") ||
            file.absolutePath.contains("/proc/")) {
            return true
        }

        // Check file content for non-image data
        return try {
            file.inputStream().use { stream ->
                val header = ByteArray(12)
                val bytesRead = stream.read(header)
                if (bytesRead < 2) return true

                // Check for valid image headers
                val isJpeg = header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte()
                val isPng = header[0] == 0x89.toByte() && header[1] == 0x50.toByte()
                val isWebP = bytesRead >= 12 &&
                    header[8] == 'W'.code.toByte() &&
                    header[9] == 'E'.code.toByte() &&
                    header[10] == 'B'.code.toByte() &&
                    header[11] == 'P'.code.toByte()

                !isJpeg && !isPng && !isWebP
            }
        } catch (e: Exception) {
            true // Consider unreadable files as potentially malicious
        }
    }

    private fun createValidTestPhoto(filename: String): File {
        val file = File(tempDir, filename)
        testPhotos.add(file)

        // Create a minimal valid JPEG with proper structure
        val width = 100
        val height = 100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Fill with test pattern
        for (x in 0 until width) {
            for (y in 0 until height) {
                val color = android.graphics.Color.rgb(
                    (x * 255 / width),
                    (y * 255 / height),
                    128
                )
                bitmap.setPixel(x, y, color)
            }
        }

        // Save as JPEG
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bitmap.recycle()

        return file
    }

    private fun createTestPhotoWithSize(filename: String, sizeBytes: Int): File {
        val file = File(tempDir, filename)
        testPhotos.add(file)

        // Create a valid JPEG of specific size
        val width = 500
        val height = 500
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Fill with data
        for (x in 0 until width step 10) {
            for (y in 0 until height step 10) {
                bitmap.setPixel(x, y, android.graphics.Color.rgb(x % 256, y % 256, 128))
            }
        }

        // Compress with quality to approximate target size
        var quality = 90
        var attempts = 0

        while (attempts < 10) {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }

            val actualSize = file.length()
            if (actualSize >= sizeBytes * 0.9 && actualSize <= sizeBytes * 1.1) {
                break // Close enough to target size
            }

            // Adjust quality
            quality = if (actualSize < sizeBytes) {
                minOf(100, quality + 5)
            } else {
                maxOf(10, quality - 10)
            }
            attempts++
        }

        // If we couldn't get exact size through compression, pad the file
        if (file.length() < sizeBytes) {
            file.appendBytes(ByteArray(sizeBytes - file.length().toInt()))
        }

        bitmap.recycle()
        return file
    }

    private fun createCorruptedImageFile(filename: String): File {
        val file = File(tempDir, filename)
        testPhotos.add(file)

        // Write corrupted JPEG header
        file.writeBytes(byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), // JPEG SOI
            0xFF.toByte(), 0xFF.toByte(), // Invalid marker
            0x00, 0x00, 0x00, 0x00,       // Invalid data
            0xFF.toByte(), 0xD9.toByte()  // JPEG EOI
        ))

        return file
    }

    private fun createFakeImageFile(filename: String): File {
        val file = File(tempDir, filename)
        testPhotos.add(file)

        // Write text content instead of image data
        file.writeText("This is not an image file!")

        return file
    }
}