package com.smilepile.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smilepile.app.CategoryAdapter
import com.smilepile.app.MainActivity
import com.smilepile.app.R
import com.smilepile.app.managers.CategoryManager
import com.smilepile.app.models.Category
import com.smilepile.app.models.Photo

class CategorySelectionFragment : Fragment() {

    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var categoryManager: CategoryManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_category_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        categoryManager = CategoryManager()
        setupRecyclerView(view)
        loadCategories()
    }

    private fun setupRecyclerView(view: View) {
        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView)

        // Use GridLayoutManager with 2 columns for tablets, adjustable for different screen sizes
        val spanCount = if (resources.configuration.screenWidthDp >= 600) 3 else 2
        categoryRecyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
    }

    private fun loadCategories() {
        val categoriesWithPhotos = categoryManager.getAllCategoriesWithPhotos()
        val categories = categoriesWithPhotos.map { it.category.displayName }
        val photos = categoriesWithPhotos.flatMap { categoryWithPhotos ->
            categoryWithPhotos.photos.map { photo ->
                // Convert models Photo to old Photo for adapter compatibility
                com.smilepile.app.Photo(
                    id = photo.id,
                    path = photo.getAssetPath(),
                    category = categoryWithPhotos.category.displayName,
                    displayName = photo.name
                )
            }
        }

        categoryAdapter = CategoryAdapter(
            context = requireContext(),
            categories = categories,
            photos = photos,
            onCategoryClick = { category ->
                onCategorySelected(category)
            }
        )

        categoryRecyclerView.adapter = categoryAdapter
    }

    private fun onCategorySelected(category: String) {
        // Navigate to PhotoFragment using the activity's navigation manager
        (activity as? MainActivity)?.showPhotosForCategory(category)
    }

    companion object {
        fun newInstance(): CategorySelectionFragment {
            return CategorySelectionFragment()
        }
    }
}