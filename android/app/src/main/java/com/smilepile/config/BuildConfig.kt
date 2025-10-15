package com.smilepile.config

import android.util.Log

/**
 * Tier configuration module for SmilePile Android
 * Provides runtime detection of deployment tier (QUAL, STAGE, BETA, PROD)
 *
 * Wave 3: Android 4-Tier Configuration
 * Story: STORY-6.3-android-tier-config.md
 *
 * This module wraps the generated BuildConfig class and provides
 * convenient tier detection methods. BUILD_TYPE_ENV is set by
 * product flavors in build.gradle.kts.
 *
 * Security: Includes tier validation controls to detect tampering.
 */
object BuildConfig {
    /**
     * Current deployment tier: qual, stage, beta, or prod
     * Read from generated BuildConfig class (set by product flavor)
     *
     * Security: Validates tier matches package name to detect tampering
     */
    val buildType: String
        get() {
            val declaredTier = com.smilepile.BuildConfig.BUILD_TYPE_ENV

            // Security Control: Verify tier matches package name
            val packageName = com.smilepile.BuildConfig.APPLICATION_ID
            val expectedTier = when (packageName) {
                "com.smilepile.qual" -> "qual"
                "com.smilepile" -> {
                    // Could be stage, beta, or prod - trust BUILD_TYPE_ENV
                    // Server-side validation required (Wave 4)
                    declaredTier
                }
                else -> "unknown"
            }

            if (expectedTier != "unknown" && expectedTier != declaredTier && expectedTier != "stage" && expectedTier != "beta" && expectedTier != "prod") {
                // Log security violation but don't crash
                Log.e("BuildConfig", "SECURITY: Tier mismatch detected! package=$packageName, declared tier=$declaredTier, expected=$expectedTier")
            }

            return declaredTier
        }

    /**
     * Returns true if running in QUAL tier (local development)
     * QUAL uses unique package name (com.smilepile.qual) for side-by-side installation
     */
    val isQual: Boolean
        get() = buildType == "qual"

    /**
     * Returns true if running in STAGE tier (internal testing)
     * STAGE uses production package name (com.smilepile)
     */
    val isStage: Boolean
        get() = buildType == "stage"

    /**
     * Returns true if running in BETA tier (external testing)
     * BETA uses production package name (com.smilepile)
     */
    val isBeta: Boolean
        get() = buildType == "beta"

    /**
     * Returns true if running in PROD tier (Play Store)
     * PROD uses production package name (com.smilepile)
     */
    val isProd: Boolean
        get() = buildType == "prod"

    /**
     * Human-readable tier name (QUAL, STAGE, BETA, PROD)
     * Useful for display in settings or debug screens
     */
    val tierDisplayName: String
        get() = when (buildType) {
            "qual" -> "QUAL"
            "stage" -> "STAGE"
            "beta" -> "BETA"
            "prod" -> "PROD"
            else -> "UNKNOWN"
        }

    /**
     * Application ID (package name)
     * - QUAL: com.smilepile.qual
     * - STAGE/BETA/PROD: com.smilepile
     */
    val applicationId: String
        get() = com.smilepile.BuildConfig.APPLICATION_ID

    /**
     * Version name (e.g., "25.10.14.001-qual")
     * Includes tier suffix for non-PROD builds
     */
    val versionName: String
        get() = com.smilepile.BuildConfig.VERSION_NAME

    /**
     * Version code (integer format: YYMMDDVVV)
     * Used by Play Store for version ordering
     */
    val versionCode: Int
        get() = com.smilepile.BuildConfig.VERSION_CODE

    /**
     * Returns true if running in debug build type
     * Independent of tier (qual can be debug or release)
     */
    val isDebug: Boolean
        get() = com.smilepile.BuildConfig.DEBUG
}
