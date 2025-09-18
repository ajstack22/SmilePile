package com.smilepile.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import java.io.IOException

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide system UI for fullscreen experience
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_main)

        // Set up ViewPager2 with images from assets folder
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val imagePaths = loadImagePathsFromAssets()
        viewPager.adapter = ImagePagerAdapter(this, imagePaths)
    }

    private fun loadImagePathsFromAssets(): List<String> {
        return try {
            val imageFiles = assets.list("sample_images") ?: emptyArray()
            imageFiles
                .filter { it.endsWith(".png", ignoreCase = true) || it.endsWith(".jpg", ignoreCase = true) || it.endsWith(".jpeg", ignoreCase = true) }
                .map { "sample_images/$it" }
                .sorted()
        } catch (e: IOException) {
            // If we can't read the assets folder, return an empty list
            emptyList()
        }
    }
}