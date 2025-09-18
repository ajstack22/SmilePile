package com.smilepile.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

class ImagePagerAdapter(
    private val context: Context,
    private val imagePaths: List<String>
) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        var loadedBitmap: Bitmap? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        // Recycle previous bitmap to prevent memory leaks
        holder.loadedBitmap?.let { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        holder.loadedBitmap = null

        if (imagePaths.isEmpty()) {
            // Show placeholder when no images are available
            holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            return
        }

        // Show placeholder while loading
        holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery)

        // Load image asynchronously to prevent ANR
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    loadBitmapSafely(imagePaths[position], holder.imageView.width, holder.imageView.height)
                }

                // Check if the holder is still valid (not recycled)
                if (holder.adapterPosition == position && bitmap != null) {
                    holder.loadedBitmap = bitmap
                    holder.imageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                // Handle error by keeping the placeholder
                // Error already shown as placeholder
            }
        }
    }

    override fun getItemCount(): Int = if (imagePaths.isEmpty()) 1 else imagePaths.size

    override fun onViewRecycled(holder: ImageViewHolder) {
        super.onViewRecycled(holder)
        // Recycle bitmap when view is recycled to prevent memory leaks
        holder.loadedBitmap?.let { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        holder.loadedBitmap = null
        holder.imageView.setImageDrawable(null)
    }

    /**
     * Safely loads a bitmap with proper memory management using inSampleSize
     * to prevent OutOfMemoryError
     */
    private fun loadBitmapSafely(imagePath: String, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            val inputStream = context.assets.open(imagePath)

            // First pass: get image dimensions without loading the full bitmap
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // Calculate inSampleSize to reduce memory usage
            val sampleSize = calculateInSampleSize(options, targetWidth, targetHeight)

            // Second pass: load the bitmap with calculated sample size
            val actualInputStream = context.assets.open(imagePath)
            val finalOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory than ARGB_8888
            }

            val bitmap = BitmapFactory.decodeStream(actualInputStream, null, finalOptions)
            actualInputStream.close()
            bitmap
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Calculate the largest inSampleSize value that is a power of 2 and keeps both
     * height and width larger than the requested height and width.
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        // Use reasonable defaults if target dimensions are not available
        val targetWidth = if (reqWidth > 0) reqWidth else 1080
        val targetHeight = if (reqHeight > 0) reqHeight else 1920

        if (height > targetHeight || width > targetWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= targetHeight && halfWidth / inSampleSize >= targetWidth) {
                inSampleSize *= 2
            }
        }

        return max(1, inSampleSize)
    }
}