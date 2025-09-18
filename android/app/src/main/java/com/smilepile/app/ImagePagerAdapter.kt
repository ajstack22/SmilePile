package com.smilepile.app

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException

class ImagePagerAdapter(
    private val context: Context,
    private val imagePaths: List<String>
) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        if (imagePaths.isEmpty()) {
            // Show placeholder when no images are available
            holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            return
        }

        try {
            val inputStream = context.assets.open(imagePaths[position])
            val bitmap = BitmapFactory.decodeStream(inputStream)
            holder.imageView.setImageBitmap(bitmap)
            inputStream.close()
        } catch (e: IOException) {
            // Handle error by showing a placeholder or error image
            holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    override fun getItemCount(): Int = if (imagePaths.isEmpty()) 1 else imagePaths.size
}