package com.smilepile.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

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

        // Set up ViewPager2 with 3 test images
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val imageResources = listOf(
            R.drawable.test_image,
            R.drawable.test_image2,
            R.drawable.test_image3
        )
        viewPager.adapter = ImagePagerAdapter(imageResources)
    }
}