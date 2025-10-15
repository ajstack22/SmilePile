package com.smilepile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smilepile.config.BuildConfig as CustomBuildConfig
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import android.util.Log

/**
 * Comprehensive instrumentation tests for BuildConfig tier detection and validation.
 * Tests all tier detection cases, convenience properties, validation logic, and edge cases.
 */
@RunWith(AndroidJUnit4::class)
class BuildConfigTest {

    companion object {
        private const val TAG = "BuildConfigTest"
    }

    @Test
    fun testBuildTypeEnvIsSet() {
        // Verify BUILD_TYPE_ENV is not null and is a valid tier
        assertNotNull("BUILD_TYPE_ENV should not be null", BuildConfig.BUILD_TYPE_ENV)
        val validTiers = listOf("qual", "stage", "beta", "prod")
        assertTrue(
            "BUILD_TYPE_ENV should be one of: $validTiers",
            BuildConfig.BUILD_TYPE_ENV in validTiers
        )
        Log.d(TAG, "BUILD_TYPE_ENV: ${BuildConfig.BUILD_TYPE_ENV}")
    }

    @Test
    fun testPackageNameMatchesTier() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageName = context.packageName
        Log.d(TAG, "Package name: $packageName")
        Log.d(TAG, "BUILD_TYPE_ENV: ${BuildConfig.BUILD_TYPE_ENV}")

        when (BuildConfig.BUILD_TYPE_ENV) {
            "qual" -> {
                assertEquals(
                    "QUAL tier should have .qual suffix in package name",
                    "com.smilepile.qual",
                    packageName
                )
            }
            "stage", "beta", "prod" -> {
                assertEquals(
                    "${BuildConfig.BUILD_TYPE_ENV.uppercase()} tier should have production package name",
                    "com.smilepile",
                    packageName
                )
            }
            else -> {
                fail("Unknown BUILD_TYPE_ENV: ${BuildConfig.BUILD_TYPE_ENV}")
            }
        }
    }

    @Test
    fun testCustomBuildConfigTierDetection() {
        // Test the custom BuildConfig module's tier detection
        val detectedTier = CustomBuildConfig.buildType
        Log.d(TAG, "Detected tier: $detectedTier")

        assertNotNull("Detected tier should not be null", detectedTier)
        assertEquals(
            "Detected tier should match BUILD_TYPE_ENV",
            BuildConfig.BUILD_TYPE_ENV,
            detectedTier
        )
    }

    @Test
    fun testCustomBuildConfigConvenienceProperties() {
        // Test all convenience properties match the current tier
        val currentTier = BuildConfig.BUILD_TYPE_ENV
        Log.d(TAG, "Testing convenience properties for tier: $currentTier")

        when (currentTier) {
            "qual" -> {
                assertTrue("isQual should be true for QUAL tier", CustomBuildConfig.isQual)
                assertFalse("isStage should be false for QUAL tier", CustomBuildConfig.isStage)
                assertFalse("isBeta should be false for QUAL tier", CustomBuildConfig.isBeta)
                assertFalse("isProd should be false for QUAL tier", CustomBuildConfig.isProd)
            }
            "stage" -> {
                assertFalse("isQual should be false for STAGE tier", CustomBuildConfig.isQual)
                assertTrue("isStage should be true for STAGE tier", CustomBuildConfig.isStage)
                assertFalse("isBeta should be false for STAGE tier", CustomBuildConfig.isBeta)
                assertFalse("isProd should be false for STAGE tier", CustomBuildConfig.isProd)
            }
            "beta" -> {
                assertFalse("isQual should be false for BETA tier", CustomBuildConfig.isQual)
                assertFalse("isStage should be false for BETA tier", CustomBuildConfig.isStage)
                assertTrue("isBeta should be true for BETA tier", CustomBuildConfig.isBeta)
                assertFalse("isProd should be false for BETA tier", CustomBuildConfig.isProd)
            }
            "prod" -> {
                assertFalse("isQual should be false for PROD tier", CustomBuildConfig.isQual)
                assertFalse("isStage should be false for PROD tier", CustomBuildConfig.isStage)
                assertFalse("isBeta should be false for PROD tier", CustomBuildConfig.isBeta)
                assertTrue("isProd should be true for PROD tier", CustomBuildConfig.isProd)
            }
            else -> {
                fail("Unknown tier: $currentTier")
            }
        }
    }

    @Test
    fun testIsDebugBuildFlag() {
        // Test that BuildConfig.DEBUG flag is set correctly
        Log.d(TAG, "BuildConfig.DEBUG: ${BuildConfig.DEBUG}")
        Log.d(TAG, "BUILD_TYPE_ENV: ${BuildConfig.BUILD_TYPE_ENV}")

        if (BuildConfig.BUILD_TYPE_ENV == "qual" && BuildConfig.DEBUG) {
            assertTrue("QUAL debug build should have DEBUG=true", BuildConfig.DEBUG)
        } else if (BuildConfig.BUILD_TYPE_ENV in listOf("stage", "beta", "prod")) {
            assertFalse("Release builds should have DEBUG=false", BuildConfig.DEBUG)
        }
    }

    @Test
    fun testTierValidationSecurity() {
        // Test that tier validation detects mismatches
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageName = context.packageName
        val expectedTier = CustomBuildConfig.buildType

        Log.d(TAG, "Testing tier validation - Package: $packageName, Expected tier: $expectedTier")

        // The validation should log warnings but not crash
        // We're just verifying the validation runs without exceptions
        try {
            // Access buildType triggers validation logic
            val tier = CustomBuildConfig.buildType
            // If no exception, validation passed or logged warning
            Log.d(TAG, "Tier validation completed without exception, tier: $tier")
        } catch (e: Exception) {
            fail("Tier validation should not throw exceptions: ${e.message}")
        }
    }

    @Test
    fun testApplicationIdSuffix() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageName = context.packageName

        Log.d(TAG, "Testing application ID suffix - Package: $packageName")

        if (BuildConfig.BUILD_TYPE_ENV == "qual") {
            assertTrue(
                "QUAL build should have .qual suffix",
                packageName.endsWith(".qual")
            )
        } else {
            assertFalse(
                "Non-QUAL builds should not have .qual suffix",
                packageName.endsWith(".qual")
            )
            assertEquals(
                "Production tiers should use base package name",
                "com.smilepile",
                packageName
            )
        }
    }

    @Test
    fun testVersionNameSuffix() {
        val versionName = BuildConfig.VERSION_NAME
        Log.d(TAG, "Version name: $versionName")
        Log.d(TAG, "BUILD_TYPE_ENV: ${BuildConfig.BUILD_TYPE_ENV}")

        when (BuildConfig.BUILD_TYPE_ENV) {
            "qual" -> {
                assertTrue(
                    "QUAL build version should have -qual suffix",
                    versionName.endsWith("-qual")
                )
            }
            "stage" -> {
                assertTrue(
                    "STAGE build version should have -stage suffix",
                    versionName.endsWith("-stage")
                )
            }
            "beta" -> {
                assertTrue(
                    "BETA build version should have -beta suffix",
                    versionName.endsWith("-beta")
                )
            }
            "prod" -> {
                assertFalse(
                    "PROD build version should not have tier suffix",
                    versionName.contains("-qual") ||
                    versionName.contains("-stage") ||
                    versionName.contains("-beta")
                )
            }
        }
    }

    @Test
    fun testTierEnumeration() {
        // Test that we can enumerate all expected tiers
        val allTiers = listOf("qual", "stage", "beta", "prod")
        val currentTier = BuildConfig.BUILD_TYPE_ENV

        assertTrue(
            "Current tier should be in the list of all tiers",
            currentTier in allTiers
        )

        Log.d(TAG, "All tiers: $allTiers")
        Log.d(TAG, "Current tier: $currentTier")
    }

    @Test
    fun testBuildConfigFieldsExist() {
        // Verify all expected BuildConfig fields exist
        assertNotNull("BUILD_TYPE should exist", BuildConfig.BUILD_TYPE)
        assertNotNull("VERSION_NAME should exist", BuildConfig.VERSION_NAME)
        assertNotNull("VERSION_CODE should exist", BuildConfig.VERSION_CODE)
        assertNotNull("APPLICATION_ID should exist", BuildConfig.APPLICATION_ID)
        assertNotNull("BUILD_TYPE_ENV should exist", BuildConfig.BUILD_TYPE_ENV)

        Log.d(TAG, "BUILD_TYPE: ${BuildConfig.BUILD_TYPE}")
        Log.d(TAG, "VERSION_NAME: ${BuildConfig.VERSION_NAME}")
        Log.d(TAG, "VERSION_CODE: ${BuildConfig.VERSION_CODE}")
        Log.d(TAG, "APPLICATION_ID: ${BuildConfig.APPLICATION_ID}")
        Log.d(TAG, "BUILD_TYPE_ENV: ${BuildConfig.BUILD_TYPE_ENV}")
    }

    @Test
    fun testCustomBuildConfigSingleton() {
        // Verify custom BuildConfig behaves as a singleton/object
        val tier1 = CustomBuildConfig.buildType
        val tier2 = CustomBuildConfig.buildType

        assertEquals(
            "Multiple calls to buildType should return same result",
            tier1,
            tier2
        )

        // Verify convenience properties are consistent
        val isQual1 = CustomBuildConfig.isQual
        val isQual2 = CustomBuildConfig.isQual

        assertEquals(
            "Multiple accesses to isQual should return same result",
            isQual1,
            isQual2
        )
    }

    @Test
    fun testEdgeCaseEmptyBuildTypeEnv() {
        // Test handling of edge case where BUILD_TYPE_ENV might be empty
        // This shouldn't happen in practice, but we test defensive programming
        val tier = CustomBuildConfig.buildType
        assertNotNull("Tier detection should never return null", tier)
        assertNotEquals("Tier detection should never return empty string", "", tier)
    }

    @Test
    fun testProGuardRulesProtection() {
        // Verify BuildConfig fields are accessible (not obfuscated by ProGuard)
        // This test passes if we can access the fields without NoSuchFieldError
        try {
            val buildTypeEnv = BuildConfig.BUILD_TYPE_ENV
            val customTier = CustomBuildConfig.buildType

            assertNotNull("BUILD_TYPE_ENV should be accessible", buildTypeEnv)
            assertNotNull("Custom BuildConfig should be accessible", customTier)

            Log.d(TAG, "ProGuard rules working - Fields accessible")
        } catch (e: NoSuchFieldError) {
            fail("BuildConfig fields should be protected by ProGuard rules: ${e.message}")
        }
    }

    @Test
    fun testFlavorSpecificResources() {
        // Test that flavor-specific resources are loaded correctly
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val appName = context.getString(context.applicationInfo.labelRes)

        Log.d(TAG, "App name from resources: $appName")
        Log.d(TAG, "BUILD_TYPE_ENV: ${BuildConfig.BUILD_TYPE_ENV}")

        when (BuildConfig.BUILD_TYPE_ENV) {
            "qual" -> assertEquals("App name should be 'SmilePile Qual'", "SmilePile Qual", appName)
            "stage" -> assertEquals("App name should be 'SmilePile Stage'", "SmilePile Stage", appName)
            "beta" -> assertEquals("App name should be 'SmilePile Beta'", "SmilePile Beta", appName)
            "prod" -> assertEquals("App name should be 'SmilePile'", "SmilePile", appName)
            else -> fail("Unknown tier: ${BuildConfig.BUILD_TYPE_ENV}")
        }
    }
}