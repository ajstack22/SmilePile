package com.smilepile.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.smilepile.R
import com.smilepile.database.entities.Category
import com.smilepile.ui.viewmodels.CategoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/**
 * Fragment displaying a single category with full-screen cover image
 *
 * Designed for child-friendly interaction with large text and visuals (F0011)
 */
@AndroidEntryPoint
class CategoryFragment : Fragment() {

    private val viewModel: CategoryViewModel by viewModels()

    private var categoryId: Long = 0L

    // UI Views
    private lateinit var coverImageView: ImageView
    private lateinit var categoryNameText: TextView
    private lateinit var photoCountText: TextView
    private lateinit var emptyStateView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryId = arguments?.getLong(ARG_CATEGORY_ID) ?: 0L
        Timber.d("CategoryFragment created for categoryId: $categoryId")
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

        initializeViews(view)
        setupObservers()

        // Load category data
        viewModel.loadCategory(categoryId)
    }

    private fun initializeViews(view: View) {
        coverImageView = view.findViewById(R.id.category_cover_image)
        categoryNameText = view.findViewById(R.id.category_name)
        photoCountText = view.findViewById(R.id.photo_count)
        emptyStateView = view.findViewById(R.id.empty_state)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe category data
                launch {
                    viewModel.category.collect { category ->
                        category?.let { updateCategoryUI(it) }
                    }
                }

                // Observe photo count
                launch {
                    viewModel.photoCount.collect { count ->
                        updatePhotoCount(count)
                    }
                }

                // Observe loading state
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        // Could add loading indicator here if needed
                        Timber.d("Loading state: $isLoading")
                    }
                }

                // Observe error state
                launch {
                    viewModel.error.collect { error ->
                        error?.let {
                            Timber.e("CategoryFragment error: $it")
                            showEmptyState()
                        }
                    }
                }
            }
        }
    }

    private fun updateCategoryUI(category: Category) {
        Timber.d("Updating UI for category: ${category.name}")

        // Update category name with large, child-friendly text
        categoryNameText.text = category.name

        // Load cover image if available
        if (!category.coverImagePath.isNullOrEmpty()) {
            loadCoverImage(category.coverImagePath)
        } else {
            // Show default placeholder or empty state
            showDefaultCoverImage()
        }

        // Hide empty state since we have content
        emptyStateView.visibility = View.GONE
    }

    private fun loadCoverImage(imagePath: String) {
        val imageFile = File(imagePath)

        if (imageFile.exists()) {
            Glide.with(this)
                .load(imageFile)
                .transform(
                    CenterCrop(),
                    RoundedCorners(resources.getDimensionPixelSize(R.dimen.corner_radius_medium))
                )
                .placeholder(R.drawable.photo_placeholder)
                .error(R.drawable.photo_placeholder)
                .into(coverImageView)
        } else {
            Timber.w("Cover image not found at path: $imagePath")
            showDefaultCoverImage()
        }
    }

    private fun showDefaultCoverImage() {
        // Load a default placeholder image
        Glide.with(this)
            .load(R.drawable.photo_placeholder)
            .transform(
                CenterCrop(),
                RoundedCorners(resources.getDimensionPixelSize(R.dimen.corner_radius_medium))
            )
            .into(coverImageView)
    }

    private fun updatePhotoCount(count: Int) {
        photoCountText.text = when (count) {
            0 -> getString(R.string.no_photos)
            1 -> getString(R.string.one_photo)
            else -> getString(R.string.photo_count, count)
        }

        // Show empty state if no photos
        if (count == 0) {
            showEmptyState()
        }
    }

    private fun showEmptyState() {
        emptyStateView.visibility = View.VISIBLE
        // Could hide other content or show helpful message
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("CategoryFragment view destroyed for categoryId: $categoryId")
    }

    companion object {
        private const val ARG_CATEGORY_ID = "category_id"

        /**
         * Create new instance of CategoryFragment for specific category
         */
        fun newInstance(categoryId: Long): CategoryFragment {
            return CategoryFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_CATEGORY_ID, categoryId)
                }
            }
        }
    }
}