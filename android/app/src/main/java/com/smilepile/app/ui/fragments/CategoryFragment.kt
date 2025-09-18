package com.smilepile.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.smilepile.app.R
import com.smilepile.app.data.database.entities.Category

/**
 * Fragment displaying a single category in ViewPager2
 *
 * Shows category cover image, name, and photo count
 * Designed with child-friendly large touch targets
 */
class CategoryFragment : Fragment() {

    private var category: Category? = null

    companion object {
        private const val ARG_CATEGORY_ID = "category_id"
        private const val ARG_CATEGORY_NAME = "category_name"
        private const val ARG_CATEGORY_COLOR = "category_color"
        private const val ARG_PHOTO_COUNT = "photo_count"

        fun newInstance(category: Category): CategoryFragment {
            val fragment = CategoryFragment()
            val args = Bundle().apply {
                putLong(ARG_CATEGORY_ID, category.id)
                putString(ARG_CATEGORY_NAME, category.name)
                putString(ARG_CATEGORY_COLOR, category.colorCode)
                putInt(ARG_PHOTO_COUNT, category.photoCount)
            }
            fragment.arguments = args
            return fragment
        }
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

        // Get category data from arguments
        val categoryName = arguments?.getString(ARG_CATEGORY_NAME) ?: ""
        val photoCount = arguments?.getInt(ARG_PHOTO_COUNT) ?: 0

        // Find views
        val categoryImageView = view.findViewById<ImageView>(R.id.category_image)
        val categoryNameText = view.findViewById<TextView>(R.id.category_name)
        val photoCountText = view.findViewById<TextView>(R.id.photo_count)

        // Set category data
        categoryNameText.text = categoryName
        photoCountText.text = "$photoCount photos"

        // Set placeholder image based on category name
        val imageResource = when (categoryName.lowercase()) {
            "family" -> android.R.drawable.ic_menu_gallery
            "pets" -> android.R.drawable.ic_menu_camera
            "travel" -> android.R.drawable.ic_menu_mapmode
            "food" -> android.R.drawable.ic_menu_gallery
            else -> android.R.drawable.ic_menu_gallery
        }
        categoryImageView.setImageResource(imageResource)

        // Set click listener for category selection
        view.setOnClickListener {
            // Handle category selection
            // This will be expanded when photo viewing is implemented
        }
    }
}