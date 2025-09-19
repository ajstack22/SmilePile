package com.smilepile.app.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.smilepile.app.R
import com.smilepile.app.adapters.CategoryManagementAdapter
import com.smilepile.app.managers.DatabaseCategoryManager
import com.smilepile.app.models.Category
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Fragment for managing categories - add, edit, delete functionality.
 * Provides a comprehensive interface for category management in parent mode.
 */
class CategoryManagementFragment : Fragment() {

    private lateinit var categoryManager: DatabaseCategoryManager
    private lateinit var categoriesRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var adapter: CategoryManagementAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryManager = DatabaseCategoryManager(requireContext(), this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_category_management, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)
        setupRecyclerView()
        setupClickListeners(view)
        loadCategories()
    }

    private fun setupViews(view: View) {
        categoriesRecyclerView = view.findViewById(R.id.categoriesRecyclerView)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
    }

    private fun setupRecyclerView() {
        adapter = CategoryManagementAdapter(
            onEditCategory = { category -> showEditCategoryDialog(category) },
            onDeleteCategory = { category -> showDeleteConfirmationDialog(category) }
        )

        categoriesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        categoriesRecyclerView.adapter = adapter
    }

    private fun setupClickListeners(view: View) {
        // Back button
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Add category button
        view.findViewById<MaterialButton>(R.id.addCategoryButton).setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val categories = categoryManager.getCategoriesAsync()
                updateUI(categories)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Failed to load categories: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateUI(categories: List<Category>) {
        if (categories.isEmpty()) {
            categoriesRecyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            categoriesRecyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            adapter.submitList(categories)
        }
    }

    private fun showAddCategoryDialog() {
        val context = requireContext()

        // Create custom layout for the dialog
        val linearLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
        }

        val titleText = TextView(context).apply {
            text = "Add New Category"
            textSize = 20f
            setTextColor(resources.getColor(android.R.color.black, null))
            setPadding(0, 0, 0, 24)
        }

        val nameEditText = EditText(context).apply {
            hint = "Category name (e.g., Animals, Vehicles)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            filters = arrayOf(InputFilter.LengthFilter(30)) // Limit to 30 characters
            setPadding(16, 16, 16, 16)
            setBackgroundResource(android.R.drawable.edit_text)
        }

        val displayNameEditText = EditText(context).apply {
            hint = "Display name for children (e.g., Cute Animals, Cool Cars)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            filters = arrayOf(InputFilter.LengthFilter(40)) // Limit to 40 characters
            setPadding(16, 16, 16, 16)
            setBackgroundResource(android.R.drawable.edit_text)
        }

        linearLayout.addView(titleText)
        linearLayout.addView(nameEditText)
        linearLayout.addView(displayNameEditText)

        AlertDialog.Builder(context)
            .setView(linearLayout)
            .setPositiveButton("Add") { _, _ ->
                val name = nameEditText.text.toString().trim()
                val displayName = displayNameEditText.text.toString().trim()

                if (validateCategoryInput(name, displayName)) {
                    addCategory(name, displayName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditCategoryDialog(category: Category) {
        val context = requireContext()

        // Create custom layout for the dialog
        val linearLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
        }

        val titleText = TextView(context).apply {
            text = "Edit Category"
            textSize = 20f
            setTextColor(resources.getColor(android.R.color.black, null))
            setPadding(0, 0, 0, 24)
        }

        val nameEditText = EditText(context).apply {
            setText(category.name)
            hint = "Category name"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            filters = arrayOf(InputFilter.LengthFilter(30))
            setPadding(16, 16, 16, 16)
            setBackgroundResource(android.R.drawable.edit_text)
        }

        val displayNameEditText = EditText(context).apply {
            setText(category.displayName)
            hint = "Display name for children"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            filters = arrayOf(InputFilter.LengthFilter(40))
            setPadding(16, 16, 16, 16)
            setBackgroundResource(android.R.drawable.edit_text)
        }

        linearLayout.addView(titleText)
        linearLayout.addView(nameEditText)
        linearLayout.addView(displayNameEditText)

        AlertDialog.Builder(context)
            .setView(linearLayout)
            .setPositiveButton("Save") { _, _ ->
                val newName = nameEditText.text.toString().trim()
                val newDisplayName = displayNameEditText.text.toString().trim()

                if (validateCategoryInput(newName, newDisplayName)) {
                    updateCategory(category, newName, newDisplayName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmationDialog(category: Category) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete '${category.displayName}'?\n\nThis will also delete all photos in this category. This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteCategory(category)
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun validateCategoryInput(name: String, displayName: String): Boolean {
        return when {
            name.isBlank() -> {
                Toast.makeText(context, "Category name cannot be empty", Toast.LENGTH_SHORT).show()
                false
            }
            displayName.isBlank() -> {
                Toast.makeText(context, "Display name cannot be empty", Toast.LENGTH_SHORT).show()
                false
            }
            name.length < 2 -> {
                Toast.makeText(context, "Category name must be at least 2 characters", Toast.LENGTH_SHORT).show()
                false
            }
            displayName.length < 2 -> {
                Toast.makeText(context, "Display name must be at least 2 characters", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun addCategory(name: String, displayName: String) {
        lifecycleScope.launch {
            try {
                val category = Category(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    displayName = displayName,
                    coverImagePath = null,
                    description = "",
                    photoCount = 0,
                    position = System.currentTimeMillis().toInt() // Use timestamp for ordering
                )

                val success = categoryManager.addCategoryAsync(category)
                if (success) {
                    Toast.makeText(context, "Category added successfully", Toast.LENGTH_SHORT).show()
                    loadCategories() // Refresh the list
                } else {
                    Toast.makeText(context, "Failed to add category", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error adding category: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateCategory(originalCategory: Category, newName: String, newDisplayName: String) {
        lifecycleScope.launch {
            try {
                // Remove the old category
                val removeSuccess = categoryManager.removeCategoryAsync(originalCategory.id)
                if (!removeSuccess) {
                    Toast.makeText(context, "Failed to update category", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Add the updated category with same ID and properties
                val updatedCategory = originalCategory.copy(
                    name = newName,
                    displayName = newDisplayName
                )

                val addSuccess = categoryManager.addCategoryAsync(updatedCategory)
                if (addSuccess) {
                    Toast.makeText(context, "Category updated successfully", Toast.LENGTH_SHORT).show()
                    loadCategories() // Refresh the list
                } else {
                    // Try to restore the original category if update failed
                    categoryManager.addCategoryAsync(originalCategory)
                    Toast.makeText(context, "Failed to update category", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error updating category: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun deleteCategory(category: Category) {
        lifecycleScope.launch {
            try {
                val success = categoryManager.removeCategoryAsync(category.id)
                if (success) {
                    Toast.makeText(
                        context,
                        "'${category.displayName}' deleted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadCategories() // Refresh the list
                } else {
                    Toast.makeText(context, "Failed to delete category", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error deleting category: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        fun newInstance(): CategoryManagementFragment {
            return CategoryManagementFragment()
        }
    }
}