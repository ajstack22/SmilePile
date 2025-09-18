package com.smilepile.app

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        // Allow some time for the activity to fully initialize
        Thread.sleep(1000)
    }

    @After
    fun tearDown() {
        // Clean up any idling resources if we had any
        // Currently no custom idling resources to unregister
    }

    @Test
    fun testActivityLaunches() {
        // Test that the activity launches successfully
        // Since we're now avoiding fullscreen in test mode, this should work
        onView(withId(R.id.viewPager)).check(matches(isDisplayed()))
    }

    @Test
    fun testSwipeGestures() {
        // Test swipe functionality
        // Since we're avoiding fullscreen in test mode, this should work without focus issues
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))
            .perform(swipeLeft())
            .perform(swipeLeft())
            .perform(swipeRight())
    }

    @Test
    fun testDynamicImageLoading() {
        // Test that ViewPager2 loads images from assets
        activityRule.scenario.onActivity { activity ->
            val viewPager = activity.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
            val adapter = viewPager.adapter as ImagePagerAdapter

            // Should have at least 1 item (either images or placeholder)
            assertTrue("Adapter should have at least 1 item", adapter.itemCount >= 1)
        }
    }

    @Test
    fun testImagePagerAdapterWithSampleImages() {
        // Test that the adapter properly loads sample images
        activityRule.scenario.onActivity { activity ->
            val imagePaths = try {
                val imageFiles = activity.assets.list("sample_images") ?: emptyArray()
                imageFiles
                    .filter { it.endsWith(".png", ignoreCase = true) || it.endsWith(".jpg", ignoreCase = true) || it.endsWith(".jpeg", ignoreCase = true) }
                    .map { "sample_images/$it" }
                    .sorted()
            } catch (e: Exception) {
                emptyList()
            }

            val adapter = ImagePagerAdapter(activity, imagePaths)

            if (imagePaths.isNotEmpty()) {
                assertEquals("Adapter count should match image count", imagePaths.size, adapter.itemCount)
            } else {
                assertEquals("Adapter should show 1 placeholder when no images", 1, adapter.itemCount)
            }
        }
    }
}