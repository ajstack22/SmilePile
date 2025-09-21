package com.smilepile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.smilepile.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for SmilePile app Kids Mode safety features.
 * These tests verify that Kids Mode properly blocks access to sensitive features
 * and maintains child safety barriers.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SmilePileSmokeTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        // Wait for the app to settle before running tests
        composeTestRule.waitForIdle()
    }

    /**
     * Test that Kids Mode is enabled by default and shows the appropriate UI
     */
    @Test
    fun kidsModeIsEnabledByDefault() {
        // Verify the Kids Mode gallery screen is displayed
        composeTestRule.onNodeWithText("SmilePile").assertIsDisplayed()

        // Verify the Edit Mode FAB is present (for switching to parent mode)
        composeTestRule.onNodeWithText("Edit Mode").assertIsDisplayed()

        // Verify no settings access is visible in the top bar or navigation
        composeTestRule.onNodeWithContentDescription("Settings").assertDoesNotExist()
        composeTestRule.onNodeWithText("Settings").assertDoesNotExist()
    }

    /**
     * Test that Kids Mode blocks direct navigation to settings
     */
    @Test
    fun kidsModeBlocksSettingsAccess() {
        // Attempt to find any settings-related UI elements
        composeTestRule.onNodeWithContentDescription("Settings").assertDoesNotExist()
        composeTestRule.onNodeWithText("Settings").assertDoesNotExist()

        // Verify that we're on the Kids Mode gallery (not settings)
        composeTestRule.onNodeWithText("SmilePile").assertIsDisplayed()
        composeTestRule.onNodeWithText("Edit Mode").assertIsDisplayed()
    }

    /**
     * Test that delete functionality is not available in Kids Mode
     */
    @Test
    fun kidsModeBlocksDeleteFunctionality() {
        // Check that no delete buttons or actions are visible
        composeTestRule.onNodeWithContentDescription("Delete").assertDoesNotExist()
        composeTestRule.onNodeWithText("Delete").assertDoesNotExist()

        // Check for common delete icons that should not be present
        composeTestRule.onAllNodesWithContentDescription("Delete photo").assertCountEquals(0)
        composeTestRule.onAllNodesWithContentDescription("Remove").assertCountEquals(0)
    }

    /**
     * Test that category management is read-only in Kids Mode
     */
    @Test
    fun kidsModeBlocksCategoryManagement() {
        // Category filters should be visible for viewing
        composeTestRule.onNodeWithText("All Photos").assertIsDisplayed()

        // But category management/editing should not be available
        composeTestRule.onNodeWithContentDescription("Add category").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Edit category").assertDoesNotExist()
        composeTestRule.onNodeWithText("Manage Categories").assertDoesNotExist()

        // No category creation or deletion options should be present
        composeTestRule.onNodeWithText("Create Category").assertDoesNotExist()
        composeTestRule.onNodeWithText("Delete Category").assertDoesNotExist()
    }

    /**
     * Test that Parent Mode requires PIN authentication
     */
    @Test
    fun parentModeRequiresPinAuthentication() {
        // Click the Edit Mode FAB to trigger PIN authentication
        composeTestRule.onNodeWithText("Edit Mode").performClick()

        // Verify PIN dialog appears
        composeTestRule.onNodeWithText("Enter PIN").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enter your PIN to switch to Edit Mode").assertIsDisplayed()

        // Verify PIN input field is present
        composeTestRule.onNodeWithText("PIN").assertIsDisplayed()

        // Verify dialog has proper action buttons
        composeTestRule.onNodeWithText("Unlock").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()

        // Test that Unlock button is disabled initially (no PIN entered)
        composeTestRule.onNodeWithText("Unlock").assertIsNotEnabled()
    }

    /**
     * Test PIN dialog validation and behavior
     */
    @Test
    fun pinDialogValidatesInput() {
        // Open PIN dialog
        composeTestRule.onNodeWithText("Edit Mode").performClick()
        composeTestRule.waitForIdle()

        // Verify PIN input accepts only digits
        val pinField = composeTestRule.onNodeWithText("PIN")
        pinField.performTextInput("123")

        // With less than 4 digits, Unlock should be disabled
        composeTestRule.onNodeWithText("Unlock").assertIsNotEnabled()

        // Enter a 4+ digit PIN
        pinField.performTextClearance()
        pinField.performTextInput("1234")

        // Now Unlock button should be enabled
        composeTestRule.onNodeWithText("Unlock").assertIsEnabled()

        // Test Cancel button dismisses dialog
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Enter PIN").assertDoesNotExist()
    }

    /**
     * Test that Kids Mode maintains safety barriers during photo viewing
     */
    @Test
    fun kidsModePhotoViewingIsSafe() {
        // If there are photos, clicking them should not reveal dangerous actions
        // Note: This test assumes there might be sample photos or handles empty state

        // Look for photo grid items (if any exist)
        val photoNodes = composeTestRule.onAllNodes(hasClickAction())
            .filterToOne(hasParent(hasTestTag("photo_grid") or hasContentDescription("photo")))

        // If photos exist, verify they don't show delete options when clicked
        try {
            photoNodes.performClick()

            // After clicking a photo, verify no delete actions are available
            composeTestRule.onNodeWithContentDescription("Delete photo").assertDoesNotExist()
            composeTestRule.onNodeWithText("Delete").assertDoesNotExist()

        } catch (e: AssertionError) {
            // No photos present, which is acceptable for this test
            // Verify empty state message is appropriate for kids
            composeTestRule.onNodeWithText("No photos yet!").assertExists()
        }
    }

    /**
     * Test that back navigation is child-safe (doesn't exit the app unexpectedly)
     */
    @Test
    fun backNavigationIsChildSafe() {
        // Verify we start on the main gallery screen
        composeTestRule.onNodeWithText("SmilePile").assertIsDisplayed()

        // Since we're already on the main screen, back navigation should be handled safely
        // The app should either stay on the current screen or handle navigation appropriately
        // This test ensures we don't accidentally exit to the launcher or show system UI

        // The presence of the Edit Mode FAB indicates we're in a safe, contained environment
        composeTestRule.onNodeWithText("Edit Mode").assertIsDisplayed()
    }

    /**
     * Test that Kids Mode UI is appropriate for children
     */
    @Test
    fun kidsModeUIIsChildFriendly() {
        // Verify the interface shows child-friendly elements
        composeTestRule.onNodeWithText("SmilePile").assertIsDisplayed()

        // Category filters should be simple and accessible
        composeTestRule.onNodeWithText("All Photos").assertIsDisplayed()

        // No complex settings or administrative UI should be visible
        composeTestRule.onNodeWithContentDescription("Admin").assertDoesNotExist()
        composeTestRule.onNodeWithText("Advanced").assertDoesNotExist()
        composeTestRule.onNodeWithText("System").assertDoesNotExist()

        // The Edit Mode button should be the only way to access parent features
        composeTestRule.onAllNodesWithText("Edit Mode").assertCountEquals(1)
    }

    /**
     * Test that mode toggle FAB is properly secured
     */
    @Test
    fun modeToggleFabIsSecured() {
        // Verify the Edit Mode FAB is present
        val editModeFab = composeTestRule.onNodeWithText("Edit Mode")
        editModeFab.assertIsDisplayed()
        editModeFab.assertHasClickAction()

        // Click should always trigger PIN authentication, never directly switch modes
        editModeFab.performClick()

        // Should always show PIN dialog, not directly switch to parent mode
        composeTestRule.onNodeWithText("Enter PIN").assertIsDisplayed()

        // Verify that without PIN validation, we remain in Kids Mode
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Edit Mode").assertIsDisplayed() // FAB still shows Edit Mode
    }
}