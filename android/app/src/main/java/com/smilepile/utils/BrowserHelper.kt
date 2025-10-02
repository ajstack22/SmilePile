package com.smilepile.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.smilepile.R

/**
 * Helper class for safely opening web URLs and email links.
 * Validates URLs before opening and provides secure error handling.
 */
object BrowserHelper {

    /**
     * Opens a web URL in a Chrome Custom Tab with security validation.
     * @param context Android context
     * @param urlString URL to open (must be HTTPS)
     * @return true if successful, false if validation failed or browser unavailable
     */
    fun openUrl(context: Context, urlString: String): Boolean {
        // Validate URL scheme is HTTPS
        if (!isValidUrl(urlString)) {
            return false
        }

        return try {
            val uri = Uri.parse(urlString)
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()

            // Check if Chrome Custom Tabs is available
            val packageName = getCustomTabsPackageName(context)
            if (packageName != null) {
                customTabsIntent.intent.setPackage(packageName)
            }

            customTabsIntent.launchUrl(context, uri)
            true
        } catch (e: Exception) {
            // Safe error handling - do not expose URL in error message
            false
        }
    }

    /**
     * Opens an email client with a mailto link.
     * @param context Android context
     * @param email Email address to send to
     * @return true if email client available, false otherwise
     */
    fun openEmailClient(context: Context, email: String): Boolean {
        return try {
            val mailtoUri = Uri.parse("mailto:$email")

            // Validate mailto scheme
            if (!isValidMailtoUrl(mailtoUri.toString())) {
                return false
            }

            val intent = Intent(Intent.ACTION_SENDTO, mailtoUri)

            // Check if email client is available
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Validates that a URL uses HTTPS scheme.
     * @param urlString URL to validate
     * @return true if URL is HTTPS, false otherwise
     */
    private fun isValidUrl(urlString: String): Boolean {
        return try {
            val uri = Uri.parse(urlString)
            uri.scheme?.equals("https", ignoreCase = true) == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Validates that a URL uses mailto scheme.
     * @param urlString URL to validate
     * @return true if URL is mailto, false otherwise
     */
    private fun isValidMailtoUrl(urlString: String): Boolean {
        return try {
            val uri = Uri.parse(urlString)
            uri.scheme?.equals("mailto", ignoreCase = true) == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets the package name of a browser that supports Chrome Custom Tabs.
     * @param context Android context
     * @return Package name of browser, or null if none available
     */
    private fun getCustomTabsPackageName(context: Context): String? {
        val packageManager = context.packageManager
        val activityIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"))
        val resolvedActivities = packageManager.queryIntentActivities(activityIntent, 0)

        // Prefer Chrome if available
        resolvedActivities.forEach { info ->
            if (info.activityInfo.packageName == "com.android.chrome") {
                return info.activityInfo.packageName
            }
        }

        // Return any available browser
        return resolvedActivities.firstOrNull()?.activityInfo?.packageName
    }
}
