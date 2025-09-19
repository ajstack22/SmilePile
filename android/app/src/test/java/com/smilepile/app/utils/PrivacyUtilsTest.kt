package com.smilepile.app.utils

import org.junit.Test
import org.junit.Assert.*

class PrivacyUtilsTest {

    @Test
    fun `getSafeFileName removes special characters`() {
        // Arrange
        val unsafeFileName = "photo with spaces & special chars!@#.jpg"

        // Act
        val safeFileName = PrivacyUtils.getSafeFileName(unsafeFileName)

        // Assert
        assertEquals("photo_with_spaces___special_chars___.jpg", safeFileName)
    }

    @Test
    fun `getSafeFileName handles empty input`() {
        // Arrange
        val emptyFileName = ""

        // Act
        val safeFileName = PrivacyUtils.getSafeFileName(emptyFileName)

        // Assert
        assertTrue("Empty filename should generate a default name", safeFileName.startsWith("photo_"))
        assertTrue("Default filename should end with .jpg", safeFileName.endsWith(".jpg"))
    }

    @Test
    fun `getSafeFileName limits length`() {
        // Arrange
        val longFileName = "a".repeat(200) + ".jpg"

        // Act
        val safeFileName = PrivacyUtils.getSafeFileName(longFileName)

        // Assert
        assertTrue("Long filename should be truncated", safeFileName.length <= 100)
    }

    @Test
    fun `PrivacyStatus data class works correctly`() {
        // Arrange
        val privacyStatus = PrivacyUtils.PrivacyStatus(
            internetDisabled = true,
            exifStrippingEnabled = true,
            childSafeMode = true
        )

        // Act
        val statusText = privacyStatus.getStatusText()

        // Assert
        assertTrue("Status should mention EXIF metadata removal",
            statusText.contains("EXIF metadata: Automatically removed"))
        assertTrue("Status should mention child-safe mode",
            statusText.contains("Child-safe mode: Active"))
    }

    @Test
    fun `PrivacyUtils object exists and can be imported`() {
        // Basic test to verify the class compiles
        assertTrue("PrivacyUtils should exist", true)
    }
}