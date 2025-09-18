package com.smilepile.app

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

class NavigationManager(private val activity: FragmentActivity, private val containerId: Int) {

    private var photos: List<Photo> = emptyList()
    private var currentState: NavigationState = NavigationState.CATEGORIES

    enum class NavigationState {
        CATEGORIES,
        PHOTOS
    }

    /**
     * Initializes the navigation with photo data and shows the category view
     */
    fun initialize(photos: List<Photo>) {
        this.photos = photos
        showCategorySelection()
    }

    /**
     * Shows the category selection screen
     */
    fun showCategorySelection() {
        currentState = NavigationState.CATEGORIES

        val fragment = CategoryFragment.newInstance(photos)
        fragment.setOnCategorySelectedListener { category ->
            showPhotosForCategory(category)
        }

        replaceFragment(fragment, "CategoryFragment")
    }

    /**
     * Shows photos for a specific category
     */
    fun showPhotosForCategory(category: String) {
        currentState = NavigationState.PHOTOS

        val fragment = PhotoFragment.newInstance(photos, category)
        fragment.setOnBackPressedListener {
            navigateBack()
        }

        replaceFragmentWithAnimation(fragment, "PhotoFragment")
    }

    /**
     * Handles back navigation
     */
    fun navigateBack(): Boolean {
        return when (currentState) {
            NavigationState.PHOTOS -> {
                showCategorySelection()
                true
            }
            NavigationState.CATEGORIES -> {
                // Let the activity handle this (typically exit the app)
                false
            }
        }
    }

    /**
     * Updates the photo data and refreshes current view
     */
    fun updatePhotos(newPhotos: List<Photo>) {
        this.photos = newPhotos

        when (currentState) {
            NavigationState.CATEGORIES -> {
                val fragment = activity.supportFragmentManager.findFragmentByTag("CategoryFragment") as? CategoryFragment
                fragment?.updatePhotos(newPhotos)
            }
            NavigationState.PHOTOS -> {
                // For photos view, we might need to navigate back to categories if the current category no longer exists
                val categories = Photo.getCategories(newPhotos)
                val currentFragment = activity.supportFragmentManager.findFragmentByTag("PhotoFragment") as? PhotoFragment

                // For simplicity, navigate back to categories when photos are updated
                showCategorySelection()
            }
        }
    }

    /**
     * Gets the current navigation state
     */
    fun getCurrentState(): NavigationState = currentState

    /**
     * Replaces the current fragment without animation
     */
    private fun replaceFragment(fragment: Fragment, tag: String) {
        activity.supportFragmentManager.beginTransaction()
            .replace(containerId, fragment, tag)
            .commit()
    }

    /**
     * Replaces the current fragment with slide animation
     */
    private fun replaceFragmentWithAnimation(fragment: Fragment, tag: String) {
        activity.supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,  // Enter animation
                R.anim.slide_out_left,  // Exit animation
                R.anim.slide_in_left,   // Pop enter animation
                R.anim.slide_out_right  // Pop exit animation
            )
            .replace(containerId, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }

    /**
     * Handles configuration changes by preserving state
     */
    fun onConfigurationChanged() {
        // Fragment manager handles fragment recreation automatically
        // We just need to ensure our listeners are re-established
        when (currentState) {
            NavigationState.CATEGORIES -> {
                val fragment = activity.supportFragmentManager.findFragmentByTag("CategoryFragment") as? CategoryFragment
                fragment?.setOnCategorySelectedListener { category ->
                    showPhotosForCategory(category)
                }
            }
            NavigationState.PHOTOS -> {
                val fragment = activity.supportFragmentManager.findFragmentByTag("PhotoFragment") as? PhotoFragment
                fragment?.setOnBackPressedListener {
                    navigateBack()
                }
            }
        }
    }
}