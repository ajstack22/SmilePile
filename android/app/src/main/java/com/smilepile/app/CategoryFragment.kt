package com.smilepile.app

import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CategoryFragment : Fragment() {

    private var photos: List<Photo> = emptyList()
    private var onCategorySelected: ((String) -> Unit)? = null

    companion object {
        fun newInstance(photos: List<Photo>): CategoryFragment {
            return CategoryFragment().apply {
                this.photos = photos
            }
        }
    }

    fun setOnCategorySelectedListener(listener: (String) -> Unit) {
        onCategorySelected = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_category, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.categoryRecyclerView)

        // Set up grid layout with 2 columns
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // Get categories from photos
        val categories = Photo.getCategories(photos)

        // Set up adapter
        val adapter = CategoryAdapter(
            requireContext(),
            categories,
            photos
        ) { category ->
            onCategorySelected?.invoke(category)
        }

        recyclerView.adapter = adapter

        // Add gesture handling for enhanced child-friendly interactions
        setupGestureHandling(recyclerView)
    }

    private fun setupGestureHandling(recyclerView: RecyclerView) {
        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                // Detect downward swipe on category items for quick entry
                val deltaY = e2.y - (e1?.y ?: 0f)
                val deltaX = e2.x - (e1?.x ?: 0f)

                // Check if it's primarily a vertical swipe downward
                if (kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX) &&
                    deltaY > 100 && // Swipe down threshold
                    kotlin.math.abs(velocityY) > 100) { // Minimum velocity

                    // Find the category under the touch point and trigger selection
                    val childView = recyclerView.findChildViewUnder(e2.x, e2.y)
                    childView?.let { child ->
                        val position = recyclerView.getChildAdapterPosition(child)
                        if (position != RecyclerView.NO_POSITION) {
                            val categories = Photo.getCategories(photos)
                            if (position < categories.size) {
                                onCategorySelected?.invoke(categories[position])
                                return true
                            }
                        }
                    }
                }
                return false
            }
        })

        recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(e)
                return false // Don't intercept, just detect gestures
            }
        })
    }

    /**
     * Updates the photos and refreshes the category view
     */
    fun updatePhotos(newPhotos: List<Photo>) {
        photos = newPhotos
        view?.findViewById<RecyclerView>(R.id.categoryRecyclerView)?.adapter?.notifyDataSetChanged()
    }
}