package com.smilepile.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.max

class CategoryAdapter(
    private val context: Context,
    private val categories: List<String>,
    private val photos: List<Photo>,
    private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryImage: ImageView = itemView.findViewById(R.id.categoryImage)
        val categoryName: TextView = itemView.findViewById(R.id.categoryName)
        var loadedBitmap: Bitmap? = null

        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onCategoryClick(categories[adapterPosition])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]

        // Recycle previous bitmap to prevent memory leaks
        holder.loadedBitmap?.let { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        holder.loadedBitmap = null

        // Set category name
        holder.categoryName.text = category

        // Show placeholder while loading
        holder.categoryImage.setImageResource(android.R.drawable.ic_menu_gallery)

        // Load the first image from this category as preview
        val categoryPhotos = Photo.filterByCategory(photos, category)
        if (categoryPhotos.isNotEmpty()) {
            val previewPhoto = categoryPhotos.first()

            // Load category image asynchronously
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        loadBitmapSafely(previewPhoto.path, 300, 300) // Smaller size for category thumbnails
                    }

                    // Check if the holder is still valid (not recycled)
                    if (holder.adapterPosition == position && bitmap != null) {
                        holder.loadedBitmap = bitmap
                        holder.categoryImage.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    // Handle error by keeping the placeholder
                    // Error already shown as placeholder
                }
            }
        }
    }

    override fun getItemCount(): Int = categories.size

    override fun onViewRecycled(holder: CategoryViewHolder) {
        super.onViewRecycled(holder)
        // Recycle bitmap when view is recycled to prevent memory leaks
        holder.loadedBitmap?.let { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        holder.loadedBitmap = null
        holder.categoryImage.setImageDrawable(null)
    }

    /**
     * Safely loads a bitmap with proper memory management using inSampleSize
     * to prevent OutOfMemoryError - reused from ImagePagerAdapter
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
        val targetWidth = if (reqWidth > 0) reqWidth else 300
        val targetHeight = if (reqHeight > 0) reqHeight else 300

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