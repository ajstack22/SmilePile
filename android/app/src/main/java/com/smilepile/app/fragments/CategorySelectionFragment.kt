package com.smilepile.app.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import com.smilepile.app.CategoryAdapter
import com.smilepile.app.MainActivity
import com.smilepile.app.R
import com.smilepile.app.managers.DatabaseCategoryManager
import com.smilepile.app.models.Category
import com.smilepile.app.models.Photo

class CategorySelectionFragment : Fragment() {

    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var categoryManager: DatabaseCategoryManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_category_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        categoryManager = DatabaseCategoryManager(requireContext(), this)
        setupRecyclerView(view)
        setupParentModeAccess(view)
        loadCategories()

        // Listen for refresh request after photo import
        parentFragmentManager.setFragmentResultListener("refreshCategories", viewLifecycleOwner) { _, _ ->
            Log.d("CategorySelectionFragment", "Refreshing categories after photo import")
            loadCategories()
        }
    }

    private fun setupRecyclerView(view: View) {
        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView)

        // Use GridLayoutManager with 2 columns for tablets, adjustable for different screen sizes
        val spanCount = if (resources.configuration.screenWidthDp >= 600) 3 else 2
        categoryRecyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val categoriesWithPhotos = categoryManager.getAllCategoriesWithPhotosAsync()
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
                Log.d("CategorySelectionFragment", "Loaded ${categories.size} categories with ${photos.size} photos")
            } catch (e: Exception) {
                Log.e("CategorySelectionFragment", "Failed to load categories", e)

                // Fallback to synchronous method for backward compatibility
                try {
                    val categoriesWithPhotos = categoryManager.getAllCategoriesWithPhotos()
                    val categories = categoriesWithPhotos.map { it.category.displayName }
                    val photos = categoriesWithPhotos.flatMap { categoryWithPhotos ->
                        categoryWithPhotos.photos.map { photo ->
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
                    Log.d("CategorySelectionFragment", "Fallback: Loaded ${categories.size} categories with ${photos.size} photos")
                } catch (fallbackE: Exception) {
                    Log.e("CategorySelectionFragment", "Both async and sync methods failed", fallbackE)
                }
            }
        }
    }

    private fun onCategorySelected(category: String) {
        // Navigate to PhotoFragment using the activity's navigation manager
        (activity as? MainActivity)?.showPhotosForCategory(category)
    }

    // Add long press listener for parent mode
    private fun setupParentModeAccess(view: View) {
        // Set long press on the entire fragment view
        view.setOnLongClickListener {
            Log.d("CategorySelectionFragment", "Long press detected on view")
            showParentModeAccessDialog()
            true
        }

        // Also set on RecyclerView for better coverage
        val recyclerView = view.findViewById<RecyclerView>(R.id.categoryRecyclerView)
        recyclerView.setOnLongClickListener {
            Log.d("CategorySelectionFragment", "Long press detected on RecyclerView")
            showParentModeAccessDialog()
            true
        }

        // Add floating action button click listener
        val parentModeButton = view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.parentModeButton)
        if (parentModeButton != null) {
            Log.d("CategorySelectionFragment", "Found FAB, making visible and setting click listener")
            parentModeButton.visibility = android.view.View.VISIBLE
            parentModeButton.setOnClickListener {
                Log.d("CategorySelectionFragment", "FAB clicked")
                showParentModeAccessDialog()
            }
        } else {
            Log.e("CategorySelectionFragment", "FAB (parentModeButton) not found in layout!")
        }

        // Add top button click listener - using MaterialButton
        val parentModeButtonTop = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.parentModeButtonTop)
        if (parentModeButtonTop != null) {
            Log.d("CategorySelectionFragment", "Found parentModeButtonTop, making visible and setting click listener")
            parentModeButtonTop.visibility = android.view.View.VISIBLE
            parentModeButtonTop.setOnClickListener {
                Log.d("CategorySelectionFragment", "Import Photos button clicked")
                showParentModeAccessDialog()
            }
        } else {
            Log.e("CategorySelectionFragment", "parentModeButtonTop not found in layout!")
        }

        // Log the view hierarchy for debugging
        Log.d("CategorySelectionFragment", "View class: ${view.javaClass.simpleName}")
        Log.d("CategorySelectionFragment", "Child count: ${(view as? android.view.ViewGroup)?.childCount ?: 0}")
    }

    private fun showParentModeAccessDialog() {
        Log.d("CategorySelectionFragment", "showParentModeAccessDialog called")
        val num1 = (1..5).random()
        val num2 = (1..5).random()
        val answer = num1 + num2
        Log.d("CategorySelectionFragment", "Showing math dialog: $num1 + $num2 = $answer")

        val editText = EditText(context).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Parent Mode Access")
            .setMessage("What is $num1 + $num2?")
            .setView(editText)
            .setPositiveButton("Enter") { _, _ ->
                if (editText.text.toString().toIntOrNull() == answer) {
                    navigateToParentMode()
                } else {
                    Toast.makeText(context, "Incorrect answer", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToParentMode() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, ParentModeFragment())
            .addToBackStack(null)
            .commit()
    }

    companion object {
        fun newInstance(): CategorySelectionFragment {
            return CategorySelectionFragment()
        }
    }
}