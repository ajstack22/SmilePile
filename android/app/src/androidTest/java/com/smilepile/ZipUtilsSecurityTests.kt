package com.smilepile

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.smilepile.storage.ZipUtils
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.system.measureTimeMillis

/**
 * Security and performance tests for ZipUtils
 * Tests ZIP bomb detection, path traversal protection, and performance limits
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class ZipUtilsSecurityTests {

    private lateinit var context: Context
    private lateinit var testDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testDir = File(context.cacheDir, "zip_security_test_${System.currentTimeMillis()}")
        testDir.mkdirs()
    }

    @After
    fun tearDown() {
        testDir.deleteRecursively()
    }

    /**
     * Test ZIP bomb detection - files with extremely high compression ratios
     */
    @Test
    fun testZipBombDetection() = runBlocking {
        val zipBombFile = File(testDir, "zip_bomb.zip")

        // Create a ZIP with suspiciously high compression ratio
        ZipOutputStream(FileOutputStream(zipBombFile)).use { zip ->
            val entry = ZipEntry("bomb.txt").apply {
                // Claim very large uncompressed size with tiny compressed size
                size = 1024L * 1024L * 1024L // 1GB uncompressed
                compressedSize = 100L // 100 bytes compressed - ratio of 10M:1
            }
            zip.putNextEntry(entry)
            // Write minimal data
            zip.write("X".repeat(50).toByteArray())
            zip.closeEntry()
        }

        // ZIP bomb should be detected during structure validation
        val validationResult = ZipUtils.validateZipStructure(zipBombFile)
        assertTrue("ZIP bomb should be detected and rejected", validationResult.isFailure)

        val exception = validationResult.exceptionOrNull()
        assertNotNull("Should have exception", exception)
        assertTrue("Should be security exception", exception is SecurityException)
        assertTrue("Should mention compression ratio",
            exception!!.message?.contains("compression ratio") ?: false)

        // ZIP bomb should also be rejected during extraction
        val extractDir = File(testDir, "extract_bomb")
        extractDir.mkdirs()

        val extractResult = ZipUtils.extractZip(zipBombFile, extractDir)
        assertTrue("ZIP bomb extraction should fail", extractResult.isFailure)
        assertTrue("Should be security exception",
            extractResult.exceptionOrNull() is SecurityException)
    }

    /**
     * Test path traversal attack protection
     */
    @Test
    fun testPathTraversalProtection() = runBlocking {
        val maliciousZip = File(testDir, "path_traversal.zip")

        ZipOutputStream(FileOutputStream(maliciousZip)).use { zip ->
            // Attempt various path traversal attacks
            val maliciousPaths = listOf(
                "../../../etc/passwd",
                "..\\..\\..\\windows\\system32\\config\\sam",
                "/etc/shadow",
                "\\windows\\system32\\drivers\\etc\\hosts",
                "photos/../../../sensitive_file.txt",
                "photos/..\\..\\..\\steal_data.txt"
            )

            maliciousPaths.forEach { maliciousPath ->
                zip.putNextEntry(ZipEntry(maliciousPath))
                zip.write("malicious content".toByteArray())
                zip.closeEntry()
            }
        }

        // Path traversal should be detected during structure validation
        val validationResult = ZipUtils.validateZipStructure(maliciousZip)
        assertTrue("Path traversal should be detected", validationResult.isFailure)
        assertTrue("Should be security exception",
            validationResult.exceptionOrNull() is SecurityException)

        // Should also be blocked during extraction
        val extractDir = File(testDir, "extract_traversal")
        extractDir.mkdirs()

        val extractResult = ZipUtils.extractZip(maliciousZip, extractDir)
        assertTrue("Path traversal extraction should fail", extractResult.isFailure)

        // Ensure no files were created outside the target directory
        val parentFiles = testDir.listFiles() ?: emptyArray()
        val suspiciousFiles = parentFiles.filter {
            it.name.contains("passwd") || it.name.contains("shadow") ||
            it.name.contains("hosts") || it.name.contains("sensitive") ||
            it.name.contains("steal")
        }
        assertTrue("No traversal files should be created", suspiciousFiles.isEmpty())
    }

    /**
     * Test protection against excessive entry counts
     */
    @Test
    fun testExcessiveEntryProtection() = runBlocking {
        val excessiveZip = File(testDir, "excessive_entries.zip")

        ZipOutputStream(FileOutputStream(excessiveZip)).use { zip ->
            // Create more than the maximum allowed entries (10,000+)
            repeat(10001) { index ->
                zip.putNextEntry(ZipEntry("file_$index.txt"))
                zip.write("content $index".toByteArray())
                zip.closeEntry()
            }
        }

        // Excessive entries should be detected
        val extractDir = File(testDir, "extract_excessive")
        extractDir.mkdirs()

        val extractResult = ZipUtils.extractZip(excessiveZip, extractDir)
        assertTrue("Excessive entries should be rejected", extractResult.isFailure)
        assertTrue("Should be security exception",
            extractResult.exceptionOrNull() is SecurityException)
        assertTrue("Should mention entry limit",
            extractResult.exceptionOrNull()?.message?.contains("too many entries") ?: false)
    }

    /**
     * Test protection against excessive uncompressed size
     */
    @Test
    fun testExcessiveUncompressedSizeProtection() = runBlocking {
        val oversizedZip = File(testDir, "oversized.zip")

        ZipOutputStream(FileOutputStream(oversizedZip)).use { zip ->
            // Create entries that would exceed 1GB total uncompressed
            repeat(20) { index ->
                val entry = ZipEntry("large_file_$index.dat").apply {
                    size = 100L * 1024L * 1024L // 100MB each, 2GB total
                }
                zip.putNextEntry(entry)
                // Write some data (but not the full claimed size)
                zip.write("large file content $index".toByteArray())
                zip.closeEntry()
            }
        }

        val extractDir = File(testDir, "extract_oversized")
        extractDir.mkdirs()

        val extractResult = ZipUtils.extractZip(oversizedZip, extractDir)
        assertTrue("Oversized ZIP should be rejected", extractResult.isFailure)
        assertTrue("Should be security exception",
            extractResult.exceptionOrNull() is SecurityException)
        assertTrue("Should mention size limit",
            extractResult.exceptionOrNull()?.message?.contains("exceeds limit") ?: false)
    }

    /**
     * Test ZIP info extraction security
     */
    @Test
    fun testZipInfoSecurity() = runBlocking {
        // Create a ZIP with suspicious characteristics
        val suspiciousZip = File(testDir, "suspicious.zip")

        ZipOutputStream(FileOutputStream(suspiciousZip)).use { zip ->
            // Add normal entry
            zip.putNextEntry(ZipEntry("normal.txt"))
            zip.write("normal content".toByteArray())
            zip.closeEntry()

            // Add entry with suspicious path
            zip.putNextEntry(ZipEntry("../evil.txt"))
            zip.write("evil content".toByteArray())
            zip.closeEntry()
        }

        // ZIP info should work but report the entries
        val infoResult = ZipUtils.getZipInfo(suspiciousZip)
        assertTrue("ZIP info should succeed", infoResult.isSuccess)

        val zipInfo = infoResult.getOrThrow()
        assertEquals("Should report correct entry count", 2, zipInfo.entryCount)
        assertTrue("Should include suspicious entry",
            zipInfo.entries.any { it.name.contains("../evil.txt") })
    }

    /**
     * Test performance with legitimate large ZIPs
     */
    @Test
    fun testLargeZipPerformance() = runBlocking {
        val largeZip = File(testDir, "large_legitimate.zip")

        // Create a large but legitimate ZIP
        val createTime = measureTimeMillis {
            ZipOutputStream(FileOutputStream(largeZip)).use { zip ->
                repeat(1000) { index ->
                    zip.putNextEntry(ZipEntry("photos/photo_$index.jpg"))
                    // Simulate photo data (relatively small for test speed)
                    val photoData = ByteArray(5 * 1024) { (it % 256).toByte() } // 5KB each
                    zip.write(photoData)
                    zip.closeEntry()
                }

                // Add metadata
                zip.putNextEntry(ZipEntry(ZipUtils.METADATA_FILE))
                zip.write("""{"version": 2, "photoCount": 1000}""".toByteArray())
                zip.closeEntry()
            }
        }

        println("Large ZIP creation time: ${createTime}ms")
        assertTrue("Large ZIP creation should be reasonable", createTime < 30000) // 30 seconds max

        // Test validation performance
        val validationTime = measureTimeMillis {
            val validationResult = ZipUtils.validateZipStructure(largeZip)
            assertTrue("Large legitimate ZIP should validate", validationResult.isSuccess)

            val structure = validationResult.getOrThrow()
            assertTrue("Should have metadata", structure.hasMetadata)
            assertTrue("Should have photos", structure.hasPhotosDirectory)
            assertEquals("Should count all photos", 1000, structure.photoCount)
        }

        println("Large ZIP validation time: ${validationTime}ms")
        assertTrue("Validation should be reasonable", validationTime < 10000) // 10 seconds max

        // Test extraction performance
        val extractDir = File(testDir, "extract_large")
        extractDir.mkdirs()

        val extractTime = measureTimeMillis {
            val extractResult = ZipUtils.extractZip(largeZip, extractDir)
            assertTrue("Large ZIP extraction should succeed", extractResult.isSuccess)

            val extractedFiles = extractResult.getOrThrow()
            assertEquals("Should extract all files", 1001, extractedFiles.size) // 1000 photos + metadata
        }

        println("Large ZIP extraction time: ${extractTime}ms")
        assertTrue("Extraction should be reasonable", extractTime < 30000) // 30 seconds max

        // Verify extracted content
        val metadataFile = File(extractDir, ZipUtils.METADATA_FILE)
        assertTrue("Metadata should be extracted", metadataFile.exists())

        val photosDir = File(extractDir, "photos")
        assertTrue("Photos directory should exist", photosDir.exists())
        assertEquals("Should have all photo files", 1000, photosDir.listFiles()?.size ?: 0)
    }

    /**
     * Test ZIP creation with progress tracking
     */
    @Test
    fun testZipCreationWithProgress() = runBlocking {
        val sourceDir = File(testDir, "source")
        sourceDir.mkdirs()

        // Create source files
        File(sourceDir, ZipUtils.METADATA_FILE).writeText("""{"test": true}""")

        val photosDir = File(sourceDir, "photos")
        photosDir.mkdirs()

        repeat(100) { index ->
            val photoFile = File(photosDir, "photo_$index.jpg")
            photoFile.writeBytes(ByteArray(1024) { it.toByte() })
        }

        val outputZip = File(testDir, "progress_test.zip")

        var progressCalls = 0
        var lastCurrent = 0
        var maxTotal = 0

        val result = ZipUtils.createZipFromDirectory(
            sourceDir = sourceDir,
            outputFile = outputZip
        ) { current, total ->
            progressCalls++
            maxTotal = maxOf(maxTotal, total)

            // Verify progress consistency
            assertTrue("Current should not decrease", current >= lastCurrent)
            assertTrue("Current should not exceed total", current <= total)

            lastCurrent = current
        }

        assertTrue("ZIP creation should succeed", result.isSuccess)
        assertTrue("Progress callback should be called", progressCalls > 0)
        assertEquals("Final current should equal total", maxTotal, lastCurrent)
        assertTrue("Should track all files", maxTotal >= 101) // metadata + 100 photos
    }

    /**
     * Test malformed ZIP handling
     */
    @Test
    fun testMalformedZipHandling() = runBlocking {
        // Test 1: Completely invalid file
        val invalidFile = File(testDir, "not_a_zip.zip")
        invalidFile.writeText("This is not a ZIP file at all!")

        val invalidResult = ZipUtils.validateZipStructure(invalidFile)
        assertTrue("Invalid file should fail validation", invalidResult.isFailure)

        // Test 2: Truncated ZIP
        val truncatedZip = File(testDir, "truncated.zip")
        truncatedZip.writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04)) // ZIP signature only

        val truncatedResult = ZipUtils.validateZipStructure(truncatedZip)
        assertTrue("Truncated ZIP should fail validation", truncatedResult.isFailure)

        // Test 3: ZIP with corrupted central directory
        val corruptedZip = File(testDir, "corrupted.zip")
        FileOutputStream(corruptedZip).use { out ->
            // Write valid ZIP header
            out.write(byteArrayOf(0x50, 0x4B, 0x03, 0x04))
            // Write some random bytes
            repeat(100) { out.write((it % 256).toByte().toInt()) }
        }

        val corruptedResult = ZipUtils.extractZip(corruptedZip, File(testDir, "corrupted_extract"))
        assertTrue("Corrupted ZIP should fail extraction", corruptedResult.isFailure)
    }

    /**
     * Test edge cases in entry name sanitization
     */
    @Test
    fun testEntrySanitization() = runBlocking {
        val sanitizationZip = File(testDir, "sanitization_test.zip")

        ZipOutputStream(FileOutputStream(sanitizationZip)).use { zip ->
            val problematicNames = listOf(
                "",
                "   ",
                "/",
                "//",
                "normal_file.txt",
                "folder/file.txt",
                "/absolute/path.txt",
                "folder//double_slash.txt",
                "unicode_测试.txt",
                "spaces in name.txt",
                "very".repeat(100) + ".txt" // Very long name
            )

            problematicNames.forEach { name ->
                try {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write("content for $name".toByteArray())
                    zip.closeEntry()
                } catch (e: Exception) {
                    // Some names might be rejected by ZipEntry itself
                    println("Entry rejected by ZipEntry: $name - ${e.message}")
                }
            }
        }

        // Test that the ZIP can be processed safely
        val extractDir = File(testDir, "sanitization_extract")
        extractDir.mkdirs()

        val extractResult = ZipUtils.extractZip(sanitizationZip, extractDir)
        // Should either succeed with sanitized names or fail safely
        if (extractResult.isSuccess) {
            val extractedFiles = extractResult.getOrThrow()

            // Verify no files were created outside the target directory
            assertFalse("No files should escape extraction directory",
                extractedFiles.any { !it.absolutePath.startsWith(extractDir.absolutePath) })
        }
    }
}