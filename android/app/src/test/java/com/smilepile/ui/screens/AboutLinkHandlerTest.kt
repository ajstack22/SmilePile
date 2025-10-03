package com.smilepile.ui.screens

import android.content.Context
import com.smilepile.R
import com.smilepile.utils.BrowserHelper
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for AboutLinkHandler functionality in SettingsScreen
 */
class AboutLinkHandlerTest {

    private lateinit var context: Context
    private var showError: Boolean = false
    private var errorMessage: String = ""
    private var isProcessing: Boolean = false
    private var onShowErrorCalled: Boolean = false
    private var onDismissErrorCalled: Boolean = false
    private var startProcessingCalled: Boolean = false
    private var stopProcessingCalled: Boolean = false

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        showError = false
        errorMessage = ""
        isProcessing = false
        onShowErrorCalled = false
        onDismissErrorCalled = false
        startProcessingCalled = false
        stopProcessingCalled = false

        // Mock BrowserHelper object
        mockkObject(BrowserHelper)

        // Setup default mock responses
        every { context.getString(R.string.privacy_policy_url) } returns "https://example.com/privacy"
        every { context.getString(R.string.terms_of_service_url) } returns "https://example.com/terms"
        every { context.getString(R.string.support_email) } returns "support@example.com"
        every { context.getString(R.string.error_browser_unavailable) } returns "Browser unavailable"
        every { context.getString(R.string.error_email_unavailable, any()) } returns "Email client unavailable"
    }

    @After
    fun tearDown() {
        unmockkObject(BrowserHelper)
    }

    private fun createHandler(): TestableAboutLinkHandler {
        return TestableAboutLinkHandler(
            context = context,
            showError = showError,
            errorMessage = errorMessage,
            isProcessing = isProcessing,
            onShowError = { msg ->
                errorMessage = msg
                showError = true
                onShowErrorCalled = true
            },
            onDismissError = {
                showError = false
                onDismissErrorCalled = true
            },
            onStartProcessing = {
                isProcessing = true
                startProcessingCalled = true
            },
            onStopProcessing = {
                isProcessing = false
                stopProcessingCalled = true
            }
        )
    }

    @Test
    fun `openPrivacyPolicy succeeds when browser is available`() {
        // Given
        every { BrowserHelper.openUrl(any(), any()) } returns true
        val handler = createHandler()

        // When
        handler.openPrivacyPolicy()

        // Then
        assertTrue(startProcessingCalled)
        assertFalse(onShowErrorCalled)
        verify { BrowserHelper.openUrl(context, "https://example.com/privacy") }
    }

    @Test
    fun `openPrivacyPolicy shows error when browser is unavailable`() {
        // Given
        every { BrowserHelper.openUrl(any(), any()) } returns false
        val handler = createHandler()

        // When
        handler.openPrivacyPolicy()

        // Then
        assertTrue(startProcessingCalled)
        assertTrue(onShowErrorCalled)
        assertEquals("Browser unavailable", errorMessage)
        assertTrue(showError)
    }

    @Test
    fun `openTermsOfService succeeds when browser is available`() {
        // Given
        every { BrowserHelper.openUrl(any(), any()) } returns true
        val handler = createHandler()

        // When
        handler.openTermsOfService()

        // Then
        assertTrue(startProcessingCalled)
        assertFalse(onShowErrorCalled)
        verify { BrowserHelper.openUrl(context, "https://example.com/terms") }
    }

    @Test
    fun `openTermsOfService shows error when browser is unavailable`() {
        // Given
        every { BrowserHelper.openUrl(any(), any()) } returns false
        val handler = createHandler()

        // When
        handler.openTermsOfService()

        // Then
        assertTrue(startProcessingCalled)
        assertTrue(onShowErrorCalled)
        assertEquals("Browser unavailable", errorMessage)
    }

    @Test
    fun `openSupport succeeds when email client is available`() {
        // Given
        every { BrowserHelper.openEmailClient(any(), any()) } returns true
        val handler = createHandler()

        // When
        handler.openSupport()

        // Then
        assertTrue(startProcessingCalled)
        assertFalse(onShowErrorCalled)
        verify { BrowserHelper.openEmailClient(context, "support@example.com") }
    }

    @Test
    fun `openSupport shows error when email client is unavailable`() {
        // Given
        every { BrowserHelper.openEmailClient(any(), any()) } returns false
        val handler = createHandler()

        // When
        handler.openSupport()

        // Then
        assertTrue(startProcessingCalled)
        assertTrue(onShowErrorCalled)
        assertEquals("Email client unavailable", errorMessage)
    }

    @Test
    fun `dismissError calls onDismissError`() {
        // Given
        val handler = createHandler()
        showError = true

        // When
        handler.dismissError()

        // Then
        assertTrue(onDismissErrorCalled)
    }

    @Test
    fun `handleLink does not execute when already processing`() {
        // Given
        isProcessing = true
        every { BrowserHelper.openUrl(any(), any()) } returns true
        val handler = createHandler()

        // When
        handler.openPrivacyPolicy()

        // Then
        // Should not call BrowserHelper since already processing
        verify(exactly = 0) { BrowserHelper.openUrl(any(), any()) }
    }

    @Test
    fun `initial state properties are correct`() {
        // Given
        showError = true
        errorMessage = "Test error"
        isProcessing = true
        val handler = createHandler()

        // Then
        assertTrue(handler.showError)
        assertEquals("Test error", handler.errorMessage)
        assertTrue(handler.isProcessing)
    }

    // Helper class to test the AboutLinkHandler logic
    private class TestableAboutLinkHandler(
        private val context: Context,
        val showError: Boolean,
        val errorMessage: String,
        val isProcessing: Boolean,
        private val onShowError: (String) -> Unit,
        private val onDismissError: () -> Unit,
        private val onStartProcessing: () -> Unit,
        private val onStopProcessing: () -> Unit
    ) {
        private fun handleLink(action: () -> Boolean, errorMsg: String) {
            if (isProcessing) return

            onStartProcessing()
            if (!action()) {
                onShowError(errorMsg)
            }

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                onStopProcessing()
            }, 500)
        }

        fun openPrivacyPolicy() {
            handleLink(
                action = {
                    BrowserHelper.openUrl(context, context.getString(R.string.privacy_policy_url))
                },
                errorMsg = context.getString(R.string.error_browser_unavailable)
            )
        }

        fun openTermsOfService() {
            handleLink(
                action = {
                    BrowserHelper.openUrl(context, context.getString(R.string.terms_of_service_url))
                },
                errorMsg = context.getString(R.string.error_browser_unavailable)
            )
        }

        fun openSupport() {
            handleLink(
                action = {
                    BrowserHelper.openEmailClient(context, context.getString(R.string.support_email))
                },
                errorMsg = context.getString(R.string.error_email_unavailable, context.getString(R.string.support_email))
            )
        }

        fun dismissError() {
            onDismissError()
        }
    }
}
