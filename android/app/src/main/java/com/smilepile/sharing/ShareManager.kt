package com.smilepile.sharing

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.smilepile.data.models.Photo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for sharing photos through native Android share sheet.
 * Supports single and multiple photo sharing with proper FileProvider URIs.
 */
@Singleton
class ShareManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val AUTHORITY = "com.smilepile.fileprovider"
        private const val IMAGE_MIME_TYPE = "image/*"
        private const val SHARE_CHOOSER_TITLE = "Share photo"
        private const val SHARE_MULTIPLE_CHOOSER_TITLE = "Share photos"
    }

    /**
     * Share a single photo through the native Android share sheet.
     * @param photo The photo to share
     * @return Intent configured for sharing, or null if photo doesn't exist
     */
    fun createShareIntent(photo: Photo): Intent? {
        val photoFile = File(photo.path)
        if (!photoFile.exists()) {
            return null
        }

        val uri = getUriForFile(photoFile)

        return Intent(Intent.ACTION_SEND).apply {
            type = IMAGE_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Share multiple photos through the native Android share sheet.
     * @param photos List of photos to share
     * @return Intent configured for sharing multiple photos
     */
    fun createShareMultipleIntent(photos: List<Photo>): Intent? {
        if (photos.isEmpty()) {
            return null
        }

        val uris = ArrayList<Uri>()

        photos.forEach { photo ->
            val photoFile = File(photo.path)
            if (photoFile.exists()) {
                uris.add(getUriForFile(photoFile))
            }
        }

        if (uris.isEmpty()) {
            return null
        }

        return Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = IMAGE_MIME_TYPE
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Launch the share sheet for a single photo.
     * @param context Activity or Fragment context
     * @param photo The photo to share
     */
    fun sharePhoto(context: Context, photo: Photo) {
        createShareIntent(photo)?.let { shareIntent ->
            val chooserIntent = Intent.createChooser(shareIntent, SHARE_CHOOSER_TITLE)
            context.startActivity(chooserIntent)
        }
    }

    /**
     * Launch the share sheet for multiple photos.
     * @param context Activity or Fragment context
     * @param photos List of photos to share
     */
    fun sharePhotos(context: Context, photos: List<Photo>) {
        createShareMultipleIntent(photos)?.let { shareIntent ->
            val chooserIntent = Intent.createChooser(shareIntent, SHARE_MULTIPLE_CHOOSER_TITLE)
            context.startActivity(chooserIntent)
        }
    }

    /**
     * Get a content URI for a file using FileProvider.
     * @param file The file to get URI for
     * @return Content URI for the file
     */
    private fun getUriForFile(file: File): Uri {
        return FileProvider.getUriForFile(context, AUTHORITY, file)
    }

    /**
     * Check if sharing is available on the device.
     * @return true if sharing is available
     */
    fun isSharingAvailable(): Boolean {
        val testIntent = Intent(Intent.ACTION_SEND).apply {
            type = IMAGE_MIME_TYPE
        }
        return testIntent.resolveActivity(context.packageManager) != null
    }
}