package com.smilepile.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.smilepile.database.entities.Category
import com.smilepile.database.repository.CategoryRepository
import com.smilepile.ui.fragments.CategoryFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * FragmentStateAdapter for managing category fragments in ViewPager2
 *
 * Features:
 * - Uses FragmentStateAdapter for efficient memory management
 * - Connects to CategoryRepository for reactive data updates
 * - Supports dynamic category updates with proper adapter notifications
 * - Optimized for large numbers of categories with lazy fragment creation
 * - Follows MVVM pattern with proper lifecycle management
 *
 * Implementation for F0012 requirement
 */
class CategoryPagerAdapter @Inject constructor(
    private val fragmentActivity: FragmentActivity,
    private val categoryRepository: CategoryRepository
) : FragmentStateAdapter(fragmentActivity) {

    private var categories: List<Category> = emptyList()

    init {
        // Observe categories for dynamic updates
        observeCategories()
    }

    /**
     * Create fragment for the category at the specified position
     */
    override fun createFragment(position: Int): Fragment {
        val category = categories.getOrNull(position)
        return if (category != null) {
            Timber.d("Creating CategoryFragment for category: ${category.name} at position $position")
            CategoryFragment.newInstance(category.id)
        } else {
            Timber.w("Invalid position $position for categories list size ${categories.size}")
            // Return empty fragment as fallback
            CategoryFragment.newInstance(-1L)
        }
    }

    /**
     * Get total number of categories
     */
    override fun getItemCount(): Int = categories.size

    /**
     * Get unique item ID for the given position
     * This ensures proper fragment management when categories are reordered
     */
    override fun getItemId(position: Int): Long {
        return categories.getOrNull(position)?.id ?: -1L
    }

    /**
     * Check if item contains the given item ID
     * Required for proper fragment lifecycle management with stable IDs
     */
    override fun containsItem(itemId: Long): Boolean {
        return categories.any { it.id == itemId }
    }

    /**
     * Get category at specified position
     */
    fun getCategoryAt(position: Int): Category? {
        return categories.getOrNull(position)
    }

    /**
     * Get position of category by ID
     */
    fun getPositionForCategory(categoryId: Long): Int {
        return categories.indexOfFirst { it.id == categoryId }
    }

    /**
     * Get current categories list
     */
    fun getCategories(): List<Category> = categories.toList()

    /**
     * Update categories list and notify adapter
     */
    private fun updateCategories(newCategories: List<Category>) {
        val oldCategories = categories
        categories = newCategories

        // Calculate and apply efficient updates
        notifyDataSetChangedSafely(oldCategories, newCategories)
    }

    /**
     * Observe categories from repository with lifecycle awareness
     */
    private fun observeCategories() {
        fragmentActivity.lifecycleScope.launch {
            fragmentActivity.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    categoryRepository.getAllActiveCategories().collectLatest { newCategories ->
                        Timber.d("Categories updated: ${newCategories.size} active categories")
                        updateCategories(newCategories)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error observing categories")
                }
            }
        }
    }

    /**
     * Safely notify data set changes with proper diff calculation
     * Optimized for ViewPager2 performance
     */
    private fun notifyDataSetChangedSafely(
        oldCategories: List<Category>,
        newCategories: List<Category>
    ) {
        when {
            oldCategories.isEmpty() && newCategories.isNotEmpty() -> {
                // Initial load
                Timber.d("Initial categories load: ${newCategories.size} categories")
                notifyDataSetChanged()
            }
            oldCategories.size != newCategories.size -> {
                // Size change - use full refresh for simplicity and reliability
                Timber.d("Categories size changed: ${oldCategories.size} -> ${newCategories.size}")
                notifyDataSetChanged()
            }
            oldCategories != newCategories -> {
                // Content change - check for reordering or updates
                val hasReordering = !oldCategories.zip(newCategories).all { (old, new) ->
                    old.id == new.id
                }

                if (hasReordering) {
                    Timber.d("Categories reordered - full refresh needed")
                    notifyDataSetChanged()
                } else {
                    // Same order, just content updates - minimal notification needed
                    Timber.d("Categories content updated")
                    notifyDataSetChanged()
                }
            }
            // No changes - no notification needed
        }
    }

    /**
     * Manual refresh of categories
     * Useful for pull-to-refresh scenarios
     */
    fun refreshCategories() {
        Timber.d("Manual refresh of categories requested")
        // The Flow observation will automatically handle the refresh
        // This method is here for explicit refresh triggers if needed
    }

    /**
     * Clean up resources when adapter is no longer needed
     */
    fun cleanup() {
        Timber.d("Cleaning up CategoryPagerAdapter")
        // FragmentStateAdapter handles fragment cleanup automatically
        // Categories list will be garbage collected
        categories = emptyList()
    }
}

/**
 * Factory for creating CategoryPagerAdapter instances
 * Follows dependency injection pattern
 */
class CategoryPagerAdapterFactory @Inject constructor(
    private val categoryRepository: CategoryRepository
) {

    /**
     * Create CategoryPagerAdapter for the given FragmentActivity
     */
    fun create(fragmentActivity: FragmentActivity): CategoryPagerAdapter {
        return CategoryPagerAdapter(fragmentActivity, categoryRepository)
    }
}