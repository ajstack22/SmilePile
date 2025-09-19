package com.smilepile.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smilepile.app.R
import com.smilepile.app.models.Category

/**
 * Adapter for displaying categories in the category management interface.
 * Provides edit and delete functionality for each category.
 */
class CategoryManagementAdapter(
    private val onEditCategory: (Category) -> Unit,
    private val onDeleteCategory: (Category) -> Unit
) : ListAdapter<Category, CategoryManagementAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_management, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryNameText: TextView = itemView.findViewById(R.id.categoryNameText)
        private val categoryInfoText: TextView = itemView.findViewById(R.id.categoryInfoText)
        private val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(category: Category) {
            categoryNameText.text = category.displayName
            categoryInfoText.text = "${category.photoCount} photos"

            editButton.setOnClickListener {
                onEditCategory(category)
            }

            deleteButton.setOnClickListener {
                onDeleteCategory(category)
            }

            // Make the entire item clickable for editing
            itemView.setOnClickListener {
                onEditCategory(category)
            }
        }
    }

    private class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
}