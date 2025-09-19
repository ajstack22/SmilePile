package com.smilepile.app.fragments

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smilepile.app.R
import com.smilepile.app.managers.PhotoImportManager
import com.smilepile.app.managers.DatabaseCategoryManager
import kotlinx.coroutines.launch
import android.net.Uri
import com.smilepile.app.models.Photo
import java.util.UUID

class ParentModeFragment : Fragment() {

    private lateinit var photoImportManager: PhotoImportManager
    private lateinit var categoryManager: DatabaseCategoryManager
    private val selectedPhotos = mutableListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photoImportManager = PhotoImportManager(requireContext(), this)
        categoryManager = DatabaseCategoryManager(requireContext(), this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_parent_mode, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupManageCategoriesButton(view)
        setupImportButton(view)
        setupCategorySelection(view)
        setupExitButton(view)
        setupPhotoImportCallbacks()
    }

    private fun setupManageCategoriesButton(view: View) {
        val manageCategoriesButton = view.findViewById<Button>(R.id.manageCategoriesButton)
        manageCategoriesButton.setOnClickListener {
            // Navigate to CategoryManagementFragment
            val fragment = CategoryManagementFragment.newInstance()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupImportButton(view: View) {
        val importButton = view.findViewById<Button>(R.id.importPhotosButton)
        importButton.setOnClickListener {
            photoImportManager.selectPhotos()
        }
    }

    private fun setupCategorySelection(view: View) {
        val spinner = view.findViewById<Spinner>(R.id.categorySpinner)

        lifecycleScope.launch {
            val categories = categoryManager.getCategoriesAsync()
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                categories.map { it.displayName }
            )
            spinner.adapter = adapter
        }
    }

    private fun setupExitButton(view: View) {
        val exitButton = view.findViewById<Button>(R.id.exitParentModeButton)
        exitButton.setOnClickListener {
            // Navigate back to child mode
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupPhotoImportCallbacks() {
        photoImportManager.onPhotosSelected = { uris ->
            selectedPhotos.clear()
            selectedPhotos.addAll(uris)
            updateSelectedCountText()
            showImportPreview()
        }
    }

    private fun updateSelectedCountText() {
        val countText = requireView().findViewById<TextView>(R.id.selectedCountText)
        if (selectedPhotos.isEmpty()) {
            countText.text = "No photos selected"
        } else {
            countText.text = "${selectedPhotos.size} photo(s) selected"
        }
    }

    private fun showImportPreview() {
        // Show selected photos and confirm import
        lifecycleScope.launch {
            importSelectedPhotos()
        }
    }

    private suspend fun importSelectedPhotos() {
        val spinner = requireView().findViewById<Spinner>(R.id.categorySpinner)

        // Check if categories are available
        val categories = categoryManager.getCategoriesAsync()
        if (categories.isEmpty()) {
            Toast.makeText(
                context,
                "No categories available. Please create a category first.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (spinner.selectedItemPosition < 0 || spinner.selectedItemPosition >= categories.size) {
            Toast.makeText(
                context,
                "Please select a valid category.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val selectedCategory = categories[spinner.selectedItemPosition]
        var successCount = 0

        selectedPhotos.forEach { uri ->
            val internalPath = photoImportManager.copyPhotoToInternalStorage(
                uri,
                selectedCategory.id
            )

            internalPath?.let {
                val photo = Photo(
                    id = UUID.randomUUID().toString(),
                    path = it,
                    name = "Imported Photo ${System.currentTimeMillis()}",
                    categoryId = selectedCategory.id,
                    isFromAssets = false
                )
                val added = categoryManager.addPhotoAsync(photo)
                if (added) {
                    successCount++
                }
            }
        }

        Toast.makeText(
            context,
            "$successCount of ${selectedPhotos.size} photos imported successfully",
            Toast.LENGTH_SHORT
        ).show()

        // Clear selection
        selectedPhotos.clear()
        updateSelectedCountText()

        // Navigate back to child mode and refresh the category view
        parentFragmentManager.popBackStack()

        // Trigger a refresh of the CategorySelectionFragment
        parentFragmentManager.setFragmentResultListener("refreshCategories", viewLifecycleOwner) { _, _ ->
            // This will be handled by CategorySelectionFragment
        }
        parentFragmentManager.setFragmentResult("refreshCategories", Bundle())
    }

    companion object {
        fun newInstance(): ParentModeFragment {
            return ParentModeFragment()
        }
    }
}