package com.smilepile.app.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.smilepile.app.data.database.entities.Category
import com.smilepile.app.ui.fragments.CategoryFragment

/**
 * ViewPager2 adapter for category navigation
 *
 * Manages CategoryFragment instances for smooth horizontal swiping
 * Optimized for performance with <100ms response times
 */
class CategoryPagerAdapter(
    fragmentActivity: FragmentActivity,
    private var categories: List<Category> = emptyList()
) : FragmentStateAdapter(fragmentActivity) {

    /**
     * Update categories and refresh adapter
     */
    fun updateCategories(newCategories: List<Category>) {
        val oldSize = categories.size
        categories = newCategories
        val newSize = categories.size

        when {
            oldSize == 0 && newSize > 0 -> {
                // Initial load
                notifyDataSetChanged()
            }
            oldSize > 0 && newSize == 0 -> {
                // All items removed
                notifyItemRangeRemoved(0, oldSize)
            }
            oldSize < newSize -> {
                // Items added
                notifyItemRangeChanged(0, oldSize)
                notifyItemRangeInserted(oldSize, newSize - oldSize)
            }
            oldSize > newSize -> {
                // Items removed
                notifyItemRangeChanged(0, newSize)
                notifyItemRangeRemoved(newSize, oldSize - newSize)
            }
            else -> {
                // Same size, items might have changed
                notifyDataSetChanged()
            }
        }
    }

    /**
     * Get category at position
     */
    fun getCategoryAt(position: Int): Category? {
        return if (position >= 0 && position < categories.size) {
            categories[position]
        } else {
            null
        }
    }

    override fun getItemCount(): Int = categories.size

    override fun createFragment(position: Int): Fragment {
        return if (position < categories.size) {
            CategoryFragment.newInstance(categories[position])
        } else {
            // Fallback fragment for invalid positions
            CategoryFragment()
        }
    }

    override fun getItemId(position: Int): Long {
        return if (position < categories.size) {
            categories[position].id
        } else {
            position.toLong()
        }
    }

    override fun containsItem(itemId: Long): Boolean {
        return categories.any { it.id == itemId }
    }
}