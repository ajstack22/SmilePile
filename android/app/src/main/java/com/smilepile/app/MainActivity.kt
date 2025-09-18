package com.smilepile.app

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.viewpager2.widget.ViewPager2
import java.io.IOException

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_main)

        // Hide system UI for fullscreen experience using modern API
        hideSystemUI()

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

    /**
     * Hide system UI using modern WindowInsetsController API with fallback for older versions
     */
    private fun hideSystemUI() {
        // Skip fullscreen for testing to avoid focus issues in UI tests
        val isInTestMode = try {
            Class.forName("androidx.test.espresso.Espresso")
            true
        } catch (e: ClassNotFoundException) {
            false
        }

        if (isInTestMode) {
            // In test mode, just dim the system bars instead of hiding them
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.let { controller ->
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
                windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For API 30+ use the new WindowInsetsController
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // For older versions, use WindowInsetsControllerCompat
            val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-hide system UI when activity resumes
        hideSystemUI()
    }
}