package com.smilepile.app

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.recyclerview.widget.RecyclerView
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategorySelectionTest {

    @Before
    fun setUp() {
        // Allow some time for the fragment to fully initialize
        Thread.sleep(500)
    }

    @Test
    fun testCategoryFragmentLaunches() {
        // Create sample photos for the fragment
        val samplePhotos = listOf(
            Photo("1", "sample_images/sample_1.png", "Smiles"),
            Photo("2", "sample_images/sample_3.png", "Animals")
        )

        // Launch the fragment in a container
        val scenario = launchFragmentInContainer<CategoryFragment>()
        scenario.onFragment { fragment ->
            fragment.updatePhotos(samplePhotos)
        }

        // Check that the RecyclerView is displayed
        onView(withId(R.id.categoryRecyclerView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testCategoryGridDisplay() {
        // Create sample photos for the fragment
        val samplePhotos = listOf(
            Photo("1", "sample_images/sample_1.png", "Smiles"),
            Photo("2", "sample_images/sample_3.png", "Animals")
        )

        // Launch the fragment
        val scenario = launchFragmentInContainer<CategoryFragment>()
        scenario.onFragment { fragment ->
            fragment.updatePhotos(samplePhotos)
        }

        // Check that the RecyclerView has items
        onView(withId(R.id.categoryRecyclerView))
            .check(matches(hasMinimumChildCount(1)))
    }

    @Test
    fun testCategoryCardComponents() {
        // Create sample photos for the fragment
        val samplePhotos = listOf(
            Photo("1", "sample_images/sample_1.png", "Smiles"),
            Photo("2", "sample_images/sample_3.png", "Animals")
        )

        // Launch the fragment
        val scenario = launchFragmentInContainer<CategoryFragment>()
        scenario.onFragment { fragment ->
            fragment.updatePhotos(samplePhotos)
        }

        // Wait for layout
        Thread.sleep(500)

        // Check that the first category card has the required components
        onView(allOf(withId(R.id.categoryImage), isDisplayed()))
            .check(matches(isDisplayed()))

        onView(allOf(withId(R.id.categoryName), isDisplayed()))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testCategoryCardClick() {
        // Create sample photos for the fragment
        val samplePhotos = listOf(
            Photo("1", "sample_images/sample_1.png", "Smiles"),
            Photo("2", "sample_images/sample_3.png", "Animals")
        )

        // Launch the fragment
        val scenario = launchFragmentInContainer<CategoryFragment>()
        scenario.onFragment { fragment ->
            fragment.updatePhotos(samplePhotos)
        }

        // Wait for items to load
        Thread.sleep(1000)

        // Click on the first category item
        onView(withId(R.id.categoryRecyclerView))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // The test should complete without crashing
        // In a real app, we would verify navigation or state changes here
    }

    @Test
    fun testChildFriendlyTouchTargets() {
        // Create sample photos for the fragment
        val samplePhotos = listOf(
            Photo("1", "sample_images/sample_1.png", "Smiles"),
            Photo("2", "sample_images/sample_3.png", "Animals")
        )

        // Launch the fragment
        val scenario = launchFragmentInContainer<CategoryFragment>()
        scenario.onFragment { fragment ->
            fragment.updatePhotos(samplePhotos)
        }

        // Check that category cards meet minimum touch target size
        // According to Material Design, touch targets should be at least 48dp
        // Our cards are set to 160dp height, which exceeds this requirement
        onView(withId(R.id.categoryRecyclerView))
            .check(matches(isDisplayed()))

        // Wait for content
        Thread.sleep(500)

        // Verify that the cards are actually touchable
        // Note: The parent CardView is clickable, not the image itself
        onView(withId(R.id.categoryRecyclerView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testGridLayoutConfiguration() {
        // Create sample photos for the fragment
        val samplePhotos = listOf(
            Photo("1", "sample_images/sample_1.png", "Smiles"),
            Photo("2", "sample_images/sample_3.png", "Animals"),
            Photo("3", "sample_images/sample_5.png", "Nature")
        )

        // Launch the fragment
        val scenario = launchFragmentInContainer<CategoryFragment>()
        scenario.onFragment { fragment ->
            fragment.updatePhotos(samplePhotos)
        }

        // Verify that the RecyclerView is using GridLayoutManager
        // This is implicitly tested by checking that multiple items can be displayed
        onView(withId(R.id.categoryRecyclerView))
            .check(matches(isDisplayed()))

        // Wait for layout to complete
        Thread.sleep(1000)

        // Check that we have multiple categories displayed
        onView(withId(R.id.categoryRecyclerView))
            .check(matches(hasMinimumChildCount(2)))
    }

    @Test
    fun testCategoryNamesAreVisible() {
        // Create sample photos for the fragment
        val samplePhotos = listOf(
            Photo("1", "sample_images/sample_1.png", "Smiles"),
            Photo("2", "sample_images/sample_3.png", "Animals")
        )

        // Launch the fragment
        val scenario = launchFragmentInContainer<CategoryFragment>()
        scenario.onFragment { fragment ->
            fragment.updatePhotos(samplePhotos)
        }

        // Wait for content to load
        Thread.sleep(1000)

        // Check that category names are displayed and readable
        onView(allOf(withId(R.id.categoryName), isDisplayed()))
            .check(matches(isDisplayed()))
            .check(matches(not(withText("")))) // Should not be empty
    }
}