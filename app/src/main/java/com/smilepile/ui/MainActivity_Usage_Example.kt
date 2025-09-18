package com.smilepile.ui

/**
 * Example of how to use CategoryFragment with ViewPager2 in MainActivity
 *
 * This is a reference implementation showing how the parent developer
 * can integrate the CategoryFragment into their ViewPager2 setup.
 *
 * Note: This is an example file - the actual MainActivity implementation
 * will be created by the parent developer as mentioned in the context.
 */

/*
class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var categoryAdapter: CategoryPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupViewPager()
    }

    private fun setupViewPager() {
        viewPager = findViewById(R.id.view_pager)

        // Create adapter using the factory (with Hilt injection)
        val adapterFactory: CategoryPagerAdapterFactory by inject()
        categoryAdapter = adapterFactory.create(this)

        // Configure ViewPager2
        viewPager.adapter = categoryAdapter
        viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        viewPager.offscreenPageLimit = 1

        // The adapter will automatically observe categories from the repository
        // and update the ViewPager2 as categories are added/removed

        // Optional: Add page change listener for UI updates
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val category = categoryAdapter.getCategoryAt(position)
                // Update UI based on current category (e.g., toolbar title)
                supportActionBar?.title = category?.name ?: getString(R.string.app_name)
            }
        })
    }
}

Layout example (activity_main.xml):
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.viewpager2.widget.ViewPager2
        android:id="@+id/view_pager"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
*/

/**
 * Key Features Implemented for F0011:
 *
 * 1. ✅ CategoryFragment class created with proper lifecycle management
 * 2. ✅ Full-screen category cover image display using ImageView
 * 3. ✅ Prominent category name with large, child-friendly text (32sp)
 * 4. ✅ PhotoDao connection through CategoryRepository for photo counts
 * 5. ✅ Graceful empty state handling with helpful messages
 * 6. ✅ Child-friendly design with 64dp touch targets and large text
 * 7. ✅ Integration with existing database layer (no modifications)
 * 8. ✅ Proper ViewPager2 adapter for horizontal swipe navigation
 *
 * Resources Created:
 * - CategoryFragment.kt - Main fragment implementation
 * - CategoryViewModel.kt - ViewModel for data management
 * - fragment_category.xml - Full-screen layout with cover image
 * - CategoryPagerAdapter.kt - Already existed and compatible
 * - dimens.xml - Child-friendly dimensions
 * - Additional colors and drawables as needed
 *
 * The fragment is ready to be used in a ViewPager2 setup by the parent developer.
 */