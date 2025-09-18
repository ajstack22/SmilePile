package com.smilepile.app

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.recyclerview.widget.RecyclerView
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        // Allow time for the activity to fully initialize
        Thread.sleep(1500)
    }

    @After
    fun tearDown() {
        // Clean up any resources if needed
    }

    @Test
    fun testInitialCategoryViewIsDisplayed() {
        // Test that the app starts with category selection
        onView(withId(R.id.categoryRecyclerView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testCategoryToPhotoNavigation() {
        // Test navigation from category to photo view
        onView(withId(R.id.categoryRecyclerView))
            .check(matches(isDisplayed()))

        // Click on first category item
        onView(withId(R.id.categoryRecyclerView))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // Allow time for fragment transition
        Thread.sleep(1000)

        // Check that photo fragment is displayed
        onView(withId(R.id.photoViewPager))
            .check(matches(isDisplayed()))

        // Check that back button is displayed
        onView(withId(R.id.backButton))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testBackNavigationFromPhotos() {
        // Navigate to photos first
        onView(withId(R.id.categoryRecyclerView))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        Thread.sleep(1000)

        // Verify we're in photo view
        onView(withId(R.id.photoViewPager))
            .check(matches(isDisplayed()))

        // Click back button
        onView(withId(R.id.backButton))
            .perform(click())

        Thread.sleep(500)

        // Verify we're back to category view
        onView(withId(R.id.categoryRecyclerView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testMultipleCategoryNavigation() {
        // Test navigating to different categories
        val categoryCount = 3 // Based on our sample data

        for (i in 0 until categoryCount) {
            // Click on category
            onView(withId(R.id.categoryRecyclerView))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(i, click()))

            Thread.sleep(1000)

            // Verify photo view is displayed
            onView(withId(R.id.photoViewPager))
                .check(matches(isDisplayed()))

            // Navigate back
            onView(withId(R.id.backButton))
                .perform(click())

            Thread.sleep(500)

            // Verify we're back to categories
            onView(withId(R.id.categoryRecyclerView))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testDeviceBackButtonNavigation() {
        // Navigate to photos
        onView(withId(R.id.categoryRecyclerView))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        Thread.sleep(1000)

        // Verify we're in photo view
        onView(withId(R.id.photoViewPager))
            .check(matches(isDisplayed()))

        // Use device back button
        activityRule.scenario.onActivity { activity ->
            activity.onBackPressed()
        }

        Thread.sleep(500)

        // Verify we're back to category view
        onView(withId(R.id.categoryRecyclerView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationStateManagement() {
        // Test that navigation state is properly managed
        activityRule.scenario.onActivity { activity ->
            val navManager = activity.getNavigationManager()

            // Should start in categories state
            assert(navManager.getCurrentState() == NavigationManager.NavigationState.CATEGORIES)

            // Navigate to photos
            activity.showPhotosForCategory("Smiles")

            // Should be in photos state
            assert(navManager.getCurrentState() == NavigationManager.NavigationState.PHOTOS)

            // Navigate back
            activity.navigateBack()

            // Should be back to categories state
            assert(navManager.getCurrentState() == NavigationManager.NavigationState.CATEGORIES)
        }
    }
}