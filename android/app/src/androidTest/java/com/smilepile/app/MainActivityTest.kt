package com.smilepile.app

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testActivityLaunches() {
        // Test that the activity launches successfully
        onView(withId(R.id.viewPager)).check(matches(isDisplayed()))
    }

    @Test
    fun testSwipeGestures() {
        // Test swipe functionality
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))
            .perform(swipeLeft())
            .perform(swipeLeft())
            .perform(swipeRight())
    }
}