package com.smilepile.app

import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2

class PhotoFragment : Fragment() {

    private var photos: List<Photo> = emptyList()
    private var category: String = ""
    private var onBackPressed: (() -> Unit)? = null

    companion object {
        fun newInstance(photos: List<Photo>, category: String): PhotoFragment {
            return PhotoFragment().apply {
                this.photos = photos
                this.category = category
            }
        }
    }

    fun setOnBackPressedListener(listener: () -> Unit) {
        onBackPressed = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_photo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewPager = view.findViewById<ViewPager2>(R.id.photoViewPager)
        val backButton = view.findViewById<Button>(R.id.backButton)

        // Filter photos by category
        val filteredPhotos = Photo.filterByCategory(photos, category)

        // Convert Photo objects to paths for the existing ImagePagerAdapter
        val imagePaths = filteredPhotos.map { it.path }

        // Set up ViewPager2 with filtered photos
        viewPager.adapter = ImagePagerAdapter(requireContext(), imagePaths)

        // Set up back button
        backButton.setOnClickListener {
            onBackPressed?.invoke()
        }

        // Add swipe up gesture for back navigation (child-friendly)
        setupGestureHandling(view)
    }

    private fun setupGestureHandling(view: View) {
        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                // Detect upward swipe for back navigation (child-friendly)
                val deltaY = e2.y - (e1?.y ?: 0f)
                val deltaX = e2.x - (e1?.x ?: 0f)

                // Check if it's primarily a vertical swipe upward
                if (kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX) &&
                    deltaY < -100 && // Swipe up threshold
                    kotlin.math.abs(velocityY) > 100) { // Minimum velocity

                    onBackPressed?.invoke()
                    return true
                }
                return false
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                // Double tap can also trigger back navigation
                onBackPressed?.invoke()
                return true
            }
        })

        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false // Don't consume the event, let ViewPager2 handle swipes
        }
    }

    /**
     * Updates the photos and refreshes the photo view
     */
    fun updatePhotos(newPhotos: List<Photo>, newCategory: String) {
        photos = newPhotos
        category = newCategory

        view?.let { view ->
            val viewPager = view.findViewById<ViewPager2>(R.id.photoViewPager)
            val filteredPhotos = Photo.filterByCategory(photos, category)
            val imagePaths = filteredPhotos.map { it.path }
            viewPager.adapter = ImagePagerAdapter(requireContext(), imagePaths)
        }
    }
}