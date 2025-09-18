package com.smilepile.app

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.smilepile.app.data.repository.CategoryRepository
import com.smilepile.app.ui.adapters.CategoryPagerAdapter
import com.smilepile.app.ui.viewmodel.CategoryViewModel
import com.smilepile.app.ui.viewmodel.CategoryViewModelFactory
import kotlinx.coroutines.launch
import android.widget.LinearLayout

/**
 * Main Activity for SmilePile app.
 * Entry point for the application with ViewPager2 category navigation.
 * Implements F0001 and F0010 requirements for horizontal swipe navigation.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var categoryAdapter: CategoryPagerAdapter
    private lateinit var toolbar: MaterialToolbar
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var fabAddPhoto: FloatingActionButton
    private lateinit var loadingIndicator: CircularProgressIndicator
    private lateinit var emptyStateLayout: LinearLayout

    // ViewModel with factory
    private val categoryViewModel: CategoryViewModel by viewModels {
        CategoryViewModelFactory(CategoryRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupViewPager()
        setupToolbar()
        setupBottomNavigation()
        setupFloatingActionButton()
        observeCategories()
    }

    /**
     * Initialize all view references
     */
    private fun initializeViews() {
        viewPager = findViewById(R.id.view_pager)
        toolbar = findViewById(R.id.toolbar)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        fabAddPhoto = findViewById(R.id.fab_add_photo)
        loadingIndicator = findViewById(R.id.loading_indicator)
        emptyStateLayout = findViewById(R.id.empty_state_layout)
    }

    /**
     * Setup ViewPager2 with CategoryPagerAdapter
     * Configures for horizontal swipe navigation with <100ms response time
     */
    private fun setupViewPager() {
        categoryAdapter = CategoryPagerAdapter(this)
        viewPager.adapter = categoryAdapter

        // Configure ViewPager2 for optimal performance
        viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        viewPager.offscreenPageLimit = 1 // Preload one page for smooth scrolling

        // Enable user input for swipe gestures
        viewPager.isUserInputEnabled = true

        // Set page transformer for smooth transitions
        viewPager.setPageTransformer { page, position ->
            // Simple alpha transformation for smooth transitions
            when {
                position < -1 -> page.alpha = 0f
                position <= 1 -> page.alpha = 1f - kotlin.math.abs(position) * 0.3f
                else -> page.alpha = 0f
            }
        }

        // Register page change callback
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Update UI based on selected category
                val category = categoryAdapter.getCategoryAt(position)
                category?.let {
                    toolbar.title = it.name
                }
            }
        })
    }

    /**
     * Setup toolbar with app branding
     */
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        toolbar.title = getString(R.string.app_name)
    }

    /**
     * Setup bottom navigation (placeholder for future features)
     */
    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_categories -> {
                    // Already on categories page
                    true
                }
                R.id.nav_import -> {
                    // TODO: Implement photo import
                    true
                }
                R.id.nav_settings -> {
                    // TODO: Implement settings
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Setup floating action button for adding photos
     */
    private fun setupFloatingActionButton() {
        fabAddPhoto.setOnClickListener {
            // TODO: Implement add photo functionality
            // For now, just provide haptic feedback
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    /**
     * Observe category data from ViewModel
     * Updates ViewPager2 adapter when categories change
     */
    private fun observeCategories() {
        showLoading(true)

        lifecycleScope.launch {
            categoryViewModel.getCategoriesFlow().collect { categories ->
                showLoading(false)

                if (categories.isEmpty()) {
                    showEmptyState(true)
                    showViewPager(false)
                } else {
                    showEmptyState(false)
                    showViewPager(true)
                    categoryAdapter.updateCategories(categories)

                    // Set initial title to first category
                    if (categories.isNotEmpty()) {
                        toolbar.title = categories[0].name
                    }
                }
            }
        }
    }

    /**
     * Show/hide loading indicator
     */
    private fun showLoading(show: Boolean) {
        loadingIndicator.visibility = if (show)
            android.view.View.VISIBLE else android.view.View.GONE
    }

    /**
     * Show/hide empty state layout
     */
    private fun showEmptyState(show: Boolean) {
        emptyStateLayout.visibility = if (show)
            android.view.View.VISIBLE else android.view.View.GONE
    }

    /**
     * Show/hide ViewPager2
     */
    private fun showViewPager(show: Boolean) {
        viewPager.visibility = if (show)
            android.view.View.VISIBLE else android.view.View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up ViewPager2 adapter
        viewPager.adapter = null
    }
}