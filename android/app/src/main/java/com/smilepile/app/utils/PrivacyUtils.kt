package com.smilepile.app.utils

import android.content.Context
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException

/**
 * Utility class for privacy-related operations, particularly EXIF metadata removal
 */
object PrivacyUtils {
    private const val TAG = "PrivacyUtils"

    /**
     * Strips EXIF metadata from an image file for privacy protection
     * @param filePath The absolute path to the image file
     * @return true if successfully stripped or no metadata existed, false if failed
     */
    fun stripExifMetadata(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists() || !file.isFile) {
                Log.w(TAG, "File does not exist or is not a file: $filePath")
                return false
            }

            val exifInterface = ExifInterface(filePath)

            // List of EXIF tags that could contain sensitive information
            val sensitiveTags = listOf(
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_GPS_DATESTAMP,
                ExifInterface.TAG_GPS_PROCESSING_METHOD,
                ExifInterface.TAG_GPS_SPEED,
                ExifInterface.TAG_GPS_SPEED_REF,
                ExifInterface.TAG_GPS_TRACK,
                ExifInterface.TAG_GPS_TRACK_REF,
                ExifInterface.TAG_GPS_IMG_DIRECTION,
                ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
                ExifInterface.TAG_GPS_DEST_LATITUDE,
                ExifInterface.TAG_GPS_DEST_LONGITUDE,
                ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
                ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_DATETIME_DIGITIZED,
                ExifInterface.TAG_SUBSEC_TIME,
                ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
                ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_SOFTWARE,
                ExifInterface.TAG_ARTIST,
                ExifInterface.TAG_COPYRIGHT,
                ExifInterface.TAG_USER_COMMENT,
                ExifInterface.TAG_IMAGE_UNIQUE_ID,
                ExifInterface.TAG_CAMERA_OWNER_NAME,
                ExifInterface.TAG_BODY_SERIAL_NUMBER,
                ExifInterface.TAG_LENS_SERIAL_NUMBER
            )

            var metadataRemoved = false

            // Remove sensitive EXIF tags
            for (tag in sensitiveTags) {
                val value = exifInterface.getAttribute(tag)
                if (value != null) {
                    exifInterface.setAttribute(tag, null)
                    metadataRemoved = true
                }
            }

            // Save the changes if any metadata was removed
            if (metadataRemoved) {
                exifInterface.saveAttributes()
                Log.d(TAG, "Successfully stripped EXIF metadata from: $filePath")
            } else {
                Log.d(TAG, "No sensitive EXIF metadata found in: $filePath")
            }

            true
        } catch (e: IOException) {
            Log.e(TAG, "IOException while stripping EXIF metadata from: $filePath", e)
            false
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while accessing file: $filePath", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while stripping EXIF metadata from: $filePath", e)
            false
        }
    }

    /**
     * Checks if a file contains sensitive EXIF metadata
     * @param filePath The absolute path to the image file
     * @return true if sensitive metadata is found, false otherwise
     */
    fun hasSensitiveMetadata(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists() || !file.isFile) {
                return false
            }

            val exifInterface = ExifInterface(filePath)

            // Check for GPS coordinates
            val latLong = exifInterface.latLong
            if (latLong != null) {
                return true
            }

            // Check for timestamps that could reveal when the photo was taken
            val dateTime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            if (!dateTime.isNullOrBlank()) {
                return true
            }

            // Check for device information
            val make = exifInterface.getAttribute(ExifInterface.TAG_MAKE)
            val model = exifInterface.getAttribute(ExifInterface.TAG_MODEL)
            if (!make.isNullOrBlank() || !model.isNullOrBlank()) {
                return true
            }

            false
        } catch (e: Exception) {
            Log.w(TAG, "Error checking for sensitive metadata in: $filePath", e)
            false
        }
    }

    /**
     * Gets a summary of EXIF metadata in a file (for debugging purposes)
     * @param filePath The absolute path to the image file
     * @return A map of EXIF tag names to values, or empty map if error
     */
    fun getExifSummary(filePath: String): Map<String, String> {
        return try {
            val file = File(filePath)
            if (!file.exists() || !file.isFile) {
                return emptyMap()
            }

            val exifInterface = ExifInterface(filePath)
            val summary = mutableMapOf<String, String>()

            // Common EXIF tags to check
            val commonTags = listOf(
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_IMAGE_WIDTH,
                ExifInterface.TAG_IMAGE_LENGTH,
                ExifInterface.TAG_ORIENTATION
            )

            for (tag in commonTags) {
                val value = exifInterface.getAttribute(tag)
                if (!value.isNullOrBlank()) {
                    summary[tag] = value
                }
            }

            summary
        } catch (e: Exception) {
            Log.w(TAG, "Error getting EXIF summary for: $filePath", e)
            emptyMap()
        }
    }

    /**
     * Verify app has no internet permission for child safety
     */
    fun verifyNoInternetAccess(context: Context): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_PERMISSIONS
            )

            val hasInternetPermission = packageInfo.requestedPermissions?.contains(
                android.Manifest.permission.INTERNET
            ) == true

            Log.d(TAG, "Internet permission check: ${if (hasInternetPermission) "FOUND" else "NOT FOUND"}")
            !hasInternetPermission
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check internet permission", e)
            false
        }
    }

    /**
     * Get safe file name without exposing original path
     */
    fun getSafeFileName(originalName: String): String {
        // Remove path information, keep only safe filename
        val name = File(originalName).name
        // Remove special characters, keep only alphanumeric, dots, hyphens, and underscores
        val safeName = name.replace(Regex("[^a-zA-Z0-9._-]"), "_")

        // Ensure filename is not empty and has reasonable length
        return if (safeName.isBlank()) {
            "photo_${System.currentTimeMillis()}.jpg"
        } else {
            safeName.take(100) // Limit length to prevent filesystem issues
        }
    }

    /**
     * Get privacy status summary for the app
     */
    fun getPrivacyStatus(context: Context): PrivacyStatus {
        val noInternet = verifyNoInternetAccess(context)

        return PrivacyStatus(
            internetDisabled = noInternet,
            exifStrippingEnabled = true, // Always enabled in our app
            childSafeMode = noInternet
        )
    }

    data class PrivacyStatus(
        val internetDisabled: Boolean,
        val exifStrippingEnabled: Boolean,
        val childSafeMode: Boolean
    ) {
        fun getStatusText(): String {
            return buildString {
                appendLine("✓ EXIF metadata: Automatically removed")
                if (internetDisabled) {
                    appendLine("✓ Internet: Disabled (child-safe)")
                } else {
                    appendLine("⚠ Internet: Enabled")
                }
                if (childSafeMode) {
                    appendLine("✓ Child-safe mode: Active")
                } else {
                    appendLine("○ Child-safe mode: Inactive")
                }
            }.trimEnd()
        }
    }
}