# Wave 3: Android Tier Configuration - Research Findings

**Research Phase - General Purpose Agent**
**Date**: 2025-10-14
**Wave**: 3 of 10
**Story**: STORY-6.3-android-tier-config.md (to be created)

---

## Executive Summary

This research phase investigated SmilePile's current Android project structure to prepare for implementing the 4-tier deployment configuration (QUAL, STAGE, BETA, PROD). The investigation focused on understanding the Gradle build system, existing configurations, ProGuard setup, signing approach, and identifying the optimal approach for implementing tier-specific configurations using Android product flavors.

**Key Findings:**
- ✅ Modern Gradle Kotlin DSL (build.gradle.kts) architecture
- ✅ Clean project with Hilt DI and Jetpack Compose
- ✅ Existing 2-configuration setup (debug/release)
- ✅ ProGuard already configured for release builds
- ✅ Tiered testing infrastructure in place
- ⚠️ No existing product flavors
- ⚠️ No signing configuration (using debug keystore)
- ⚠️ No BUILD_TYPE_ENV detection mechanism
- ⚠️ No flavor-specific resources or configurations

---

## 1. Android Project Structure

### 1.1 Build System
**Build Configuration:**
```
/Users/adamstack/SmilePile/android/
├── build.gradle.kts (root)
├── app/
│   ├── build.gradle.kts (app module)
│   ├── tier-tests.gradle (tiered testing)
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       ├── test/
│       └── androidTest/
└── gradle.properties
```

**Gradle Version:**
- Android Gradle Plugin: 8.2.0
- Kotlin: 1.9.20
- Build System: Gradle with Kotlin DSL (.kts)

**Key Observations:**
- Modern Kotlin DSL throughout (not Groovy)
- Clean separation of concerns
- Already using JaCoCo for code coverage
- Tiered testing system already in place (tier-tests.gradle)

### 1.2 Current Build Configuration

**From build.gradle.kts (lines 14-46):**
```kotlin
android {
    namespace = "com.smilepile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smilepile"
        minSdk = 24
        targetSdk = 35
        versionCode = 251014001  // YYMMDDVVV format
        versionName = "25.10.14.001"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }
}
```

**Current State:**
- Package: `com.smilepile`
- No product flavors defined
- 2 build types: debug, release
- No signing configuration (defaults to debug keystore)
- buildConfig feature ENABLED (line 60)

---

## 2. Existing Product Flavors Analysis

### 2.1 Current Flavor Configuration

**Search Result:**
```bash
grep -n "flavorDimensions\|productFlavors" android/app/build.gradle.kts
# Result: NO MATCHES FOUND
```

**Conclusion**: Zero product flavors currently configured. Clean slate for implementation.

### 2.2 Flavor Strategy Comparison (iOS vs Android)

**iOS Approach (Wave 2):**
- 4 xcconfig files (Qual, Stage, Beta, Prod)
- 4 Xcode schemes
- Info.plist variables for tier detection
- BuildConfig.swift reads Info.plist

**Android Approach (Recommended for Wave 3):**
- 4 product flavors (qual, stage, beta, prod)
- Single flavor dimension: "tier"
- BuildConfigField for tier detection
- Kotlin module reads BuildConfig

**Parallel Pattern:**
| iOS | Android |
|-----|---------|
| Qual.xcconfig | productFlavors { qual {} } |
| Stage.xcconfig | productFlavors { stage {} } |
| Beta.xcconfig | productFlavors { beta {} } |
| Prod.xcconfig | productFlavors { prod {} } |
| BuildConfig.swift | BuildConfig.kt module |
| Info.plist BUILD_TYPE_ENV | buildConfigField "BUILD_TYPE_ENV" |

---

## 3. Build Types and Variants

### 3.1 Current Build Types

**Debug Build Type:**
- Minification: Disabled
- Debuggable: Yes
- Test coverage: Enabled
- Use case: Development

**Release Build Type:**
- Minification: Enabled (ProGuard)
- Debuggable: No
- Optimizations: Full
- Use case: Production

### 3.2 Recommended Build Variants Strategy

**After Adding Flavors, Expected Variants:**
```
qualDebug         - QUAL tier, debug mode (local testing)
qualRelease       - QUAL tier, release mode (optional, for testing ProGuard)
stageDebug        - STAGE tier, debug mode (optional, for debugging)
stageRelease      - STAGE tier, release mode (TestFlight equivalent)
betaDebug         - BETA tier, debug mode (optional)
betaRelease       - BETA tier, release mode (external testing)
prodDebug         - PROD tier, debug mode (NEVER use)
prodRelease       - PROD tier, release mode (Play Store)
```

**Primary Build Variants for Deployment:**
- `qualDebug` - Development (most common)
- `stageRelease` - Internal testing
- `betaRelease` - External testing
- `prodRelease` - App Store release

**Note**: Debug variants of stage/beta/prod are typically unused in Android world.

---

## 4. Signing Configuration

### 4.1 Current Signing Setup

**Search Results:**
```bash
grep -n "signingConfig\|keystore" android/app/build.gradle.kts
# Result: No signing config found

find android -name "*.keystore" -o -name "keystore.properties"
# Found: No keystore files (using debug keystore by default)
```

**Current State:**
- **NO production signing configuration**
- Using default debug keystore (automatically generated)
- Debug keystore location: `~/.android/debug.keystore`
- Debug keystore is NEVER suitable for production

### 4.2 Gitignore Status

**From .gitignore (lines 26, 48-50, 120, 155, 225, 231):**
```gitignore
local.properties
*.keystore
android/local.properties
android/keystore.properties
android/keystore.properties  # (duplicate entry)
**/keystores/*.keystore
```

**Status:** ✅ Properly configured to exclude signing credentials from git

### 4.3 Required Signing Configuration

**Must Create for Wave 3:**

1. **Production Keystore** (for stage/beta/prod):
```bash
keytool -genkey -v -keystore ~/keystores/smilepile-production.keystore \
  -alias smilepile -keyalg RSA -keysize 2048 -validity 10000
```

2. **QUAL Keystore** (optional, can use debug keystore):
```bash
# Option 1: Use existing debug keystore
# Option 2: Create dedicated QUAL keystore for consistency
```

3. **keystore.properties** (NEVER commit):
```properties
# Production keystore
storeFile=/Users/adamstack/keystores/smilepile-production.keystore
storePassword=<SECURE_PASSWORD>
keyAlias=smilepile
keyPassword=<SECURE_PASSWORD>

# QUAL keystore (optional)
qualStoreFile=/Users/adamstack/keystores/smilepile-qual.keystore
qualStorePassword=<SECURE_PASSWORD>
qualKeyAlias=smilepile-qual
qualKeyPassword=<SECURE_PASSWORD>
```

### 4.4 Google Play App Signing Recommendation

**Best Practice:** Use Google Play App Signing
- Google manages production signing key
- Upload keystore used for build signing
- Automatic app signing on Play Console
- Key rotation and recovery support

**Implementation:** Enable during first Play Console upload (Wave 1 setup)

---

## 5. ProGuard/R8 Configuration

### 5.1 Existing ProGuard Rules

**File:** `/Users/adamstack/SmilePile/android/app/proguard-rules.pro`

**Current Rules (36 lines):**
```proguard
# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep data models
-keep class com.smilepile.data.models.** { *; }

# Keep Room database classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Parcelize support
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Material Components
-keep class com.google.android.material.** { *; }
-keep class androidx.** { *; }

# Custom views
-keep class com.smilepile.ui.views.** { *; }
```

**Status:** ✅ Already configured with essential rules

### 5.2 ProGuard Tier Considerations

**Additional Rules Needed for Wave 3:**
```proguard
# BuildConfig - Keep tier detection fields
-keep class com.smilepile.BuildConfig { *; }
-keep class com.smilepile.config.BuildConfigModule { *; }

# Hilt modules (if tier-specific)
-keep @dagger.hilt.InstallIn class * { *; }
```

**Flavor-Specific ProGuard:**
- QUAL: Can disable ProGuard for faster builds
- STAGE/BETA/PROD: Keep ProGuard enabled
- Consider flavor-specific rules if needed:
  ```kotlin
  productFlavors {
      prod {
          proguardFile("proguard-rules-prod.pro")
      }
  }
  ```

---

## 6. Package Naming Conventions

### 6.1 Current Package Structure

**Base Package:** `com.smilepile`

**Current applicationId:** `com.smilepile` (line 19 in build.gradle.kts)

**Source Structure:**
```
android/app/src/main/java/com/smilepile/
├── MainActivity.kt
├── SmilePileApplication.kt
├── data/
├── di/
├── managers/
├── security/
├── storage/
├── ui/
└── utils/
```

**Total Source Files:** 21 top-level directories

### 6.2 Package Naming Strategy for Tiers

**Recommended Approach (mirrors iOS):**

**Application IDs:**
- QUAL: `com.smilepile.qual` (unique for side-by-side)
- STAGE: `com.smilepile` (shared)
- BETA: `com.smilepile` (shared)
- PROD: `com.smilepile` (shared)

**Implementation in build.gradle.kts:**
```kotlin
productFlavors {
    create("qual") {
        applicationIdSuffix = ".qual"
        versionNameSuffix = "-qual"
    }
    create("stage") {
        versionNameSuffix = "-stage"
    }
    create("beta") {
        versionNameSuffix = "-beta"
    }
    create("prod") {
        // No suffix - clean production package
    }
}
```

**Impact:**
- QUAL can be installed alongside STAGE/BETA/PROD
- STAGE, BETA, PROD share package name (only one installable at a time)
- Matches iOS bundle ID strategy from Wave 2

### 6.3 AndroidManifest Considerations

**Current AndroidManifest.xml:**
```xml
<application
    android:name=".SmilePileApplication"
    android:label="@string/app_name"
    ...>
```

**Tier-Specific App Names:**
Must update strings.xml to use flavor-specific resources:

**Create flavor source sets:**
```
android/app/src/
├── qual/res/values/strings.xml       (app_name = "SmilePile Qual")
├── stage/res/values/strings.xml      (app_name = "SmilePile Stage")
├── beta/res/values/strings.xml       (app_name = "SmilePile Beta")
├── prod/res/values/strings.xml       (app_name = "SmilePile")
└── main/res/values/strings.xml       (fallback)
```

**File Provider Authority:**
Line 97 in AndroidManifest.xml:
```xml
android:authorities="${applicationId}.fileprovider"
```
✅ Already uses `${applicationId}` - will work with flavor suffixes automatically

---

## 7. Dependencies and Build Configuration

### 7.1 Current Dependencies

**Key Dependencies (from build.gradle.kts lines 100-213):**

**Core:**
- Hilt (Dependency Injection): 2.48
- Room (Database): 2.6.1
- Kotlin Coroutines: 1.7.3
- Kotlin Serialization: 1.6.0

**UI:**
- Jetpack Compose BOM: 2024.02.00
- Material 3
- Navigation Compose: 2.7.6
- Coil (Image Loading): 2.5.0

**Security:**
- AndroidX Security-Crypto: 1.1.0
- Biometric: 1.1.0

**Testing:**
- JUnit: 4.13.2
- MockK: 1.13.8
- Robolectric: 4.11.1
- Compose UI Test

**Total Dependencies:** 50+ libraries

### 7.2 Tier-Specific Dependency Needs

**Analysis:** No tier-specific dependencies required for Wave 3

**Rationale:**
- All tiers use same feature set
- Same UI components across tiers
- Same backend dependencies
- BUILD_TYPE_ENV used for endpoint routing (Wave 4), not dependency selection

**Future Consideration:**
If debugging tools needed only in QUAL:
```kotlin
dependencies {
    qualImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
    // Other tiers won't include LeakCanary
}
```

### 7.3 BuildConfig Feature

**Already Enabled (line 60):**
```kotlin
buildFeatures {
    buildConfig = true  // ✅ Critical for BUILD_TYPE_ENV
}
```

**This enables:**
- `com.smilepile.BuildConfig` class generation
- Access to `BuildConfig.DEBUG`, `BuildConfig.BUILD_TYPE`, etc.
- Custom fields via `buildConfigField()`

**Perfect for tier detection:**
```kotlin
productFlavors {
    create("qual") {
        buildConfigField("String", "BUILD_TYPE_ENV", "\"qual\"")
    }
}
```

---

## 8. BUILD_TYPE_ENV Implementation Strategy

### 8.1 Current BuildConfig Usage

**Search Results:**
```bash
grep -r "BuildConfig" android/app/src/main/java/com/smilepile/ | head -20

Found 5 files:
- SettingsScreen.kt (version display)
- BackupManager.kt (debug checks)
- PerformanceUtils.kt (debug logging)
- PerformanceConfig.kt (debug monitoring)
- ImageLoadingModule.kt (cache config)
```

**Example from PerformanceConfig.kt (line 200):**
```kotlin
fun enableScrollPerformanceMonitoring(enabled: Boolean) {
    scrollPerformanceEnabled = enabled && com.smilepile.BuildConfig.DEBUG
}
```

**Current BuildConfig Access:** ✅ Already using `BuildConfig.DEBUG`

### 8.2 iOS BUILD_TYPE_ENV Pattern (from Wave 2)

**iOS BuildConfig.swift Pattern:**
```swift
public struct BuildConfig {
    public static var buildType: String {
        guard let buildType = Bundle.main.object(
            forInfoDictionaryKey: "BUILD_TYPE_ENV"
        ) as? String else {
            return "qual"  // Fallback
        }
        return buildType
    }

    public static var isQual: Bool { buildType == "qual" }
    public static var isStage: Bool { buildType == "stage" }
    public static var isBeta: Bool { buildType == "beta" }
    public static var isProd: Bool { buildType == "prod" }
}
```

### 8.3 Android BuildConfig Pattern (Recommended)

**Step 1: Add buildConfigField in product flavors**
```kotlin
// android/app/build.gradle.kts
android {
    flavorDimensions += "tier"

    productFlavors {
        create("qual") {
            dimension = "tier"
            buildConfigField("String", "BUILD_TYPE_ENV", "\"qual\"")
        }
        create("stage") {
            dimension = "tier"
            buildConfigField("String", "BUILD_TYPE_ENV", "\"stage\"")
        }
        create("beta") {
            dimension = "tier"
            buildConfigField("String", "BUILD_TYPE_ENV", "\"beta\"")
        }
        create("prod") {
            dimension = "tier"
            buildConfigField("String", "BUILD_TYPE_ENV", "\"prod\"")
        }
    }
}
```

**Generated BuildConfig Class:**
```kotlin
package com.smilepile

object BuildConfig {
    const val BUILD_TYPE_ENV = "qual"  // or "stage", "beta", "prod"
    const val DEBUG = true  // or false
    const val APPLICATION_ID = "com.smilepile.qual"
    const val BUILD_TYPE = "debug"  // or "release"
    const val VERSION_CODE = 251014001
    const val VERSION_NAME = "25.10.14.001"
}
```

**Step 2: Create BuildConfig.kt module** (mirrors iOS pattern)
```kotlin
// android/app/src/main/java/com/smilepile/config/BuildConfig.kt
package com.smilepile.config

object BuildConfig {
    /**
     * Current deployment tier: qual, stage, beta, or prod
     * Read from generated BuildConfig class (set by product flavor)
     */
    val buildType: String
        get() = com.smilepile.BuildConfig.BUILD_TYPE_ENV

    /** Returns true if running in QUAL tier (local development) */
    val isQual: Boolean
        get() = buildType == "qual"

    /** Returns true if running in STAGE tier (internal testing) */
    val isStage: Boolean
        get() = buildType == "stage"

    /** Returns true if running in BETA tier (external testing) */
    val isBeta: Boolean
        get() = buildType == "beta"

    /** Returns true if running in PROD tier (Play Store) */
    val isProd: Boolean
        get() = buildType == "prod"

    /** Human-readable tier name */
    val tierDisplayName: String
        get() = when (buildType) {
            "qual" -> "QUAL"
            "stage" -> "STAGE"
            "beta" -> "BETA"
            "prod" -> "PROD"
            else -> "UNKNOWN"
        }

    /** Application ID (package name) */
    val applicationId: String
        get() = com.smilepile.BuildConfig.APPLICATION_ID

    /** Version name */
    val versionName: String
        get() = com.smilepile.BuildConfig.VERSION_NAME
}
```

**Step 3: Usage in code**
```kotlin
import com.smilepile.config.BuildConfig

class SomeViewModel {
    fun someFunction() {
        if (BuildConfig.isQual) {
            // QUAL-specific behavior
        }

        val endpoint = when {
            BuildConfig.isQual -> "https://api-qual.smilepile.com"
            BuildConfig.isStage -> "https://api-stage.smilepile.com"
            BuildConfig.isBeta -> "https://api-beta.smilepile.com"
            BuildConfig.isProd -> "https://api.smilepile.com"
            else -> "https://api-qual.smilepile.com"
        }
    }
}
```

### 8.4 Hilt Integration (Optional)

**If needed for DI:**
```kotlin
// android/app/src/main/java/com/smilepile/di/BuildConfigModule.kt
@Module
@InstallIn(SingletonComponent::class)
object BuildConfigModule {

    @Provides
    @Named("buildType")
    fun provideBuildType(): String {
        return com.smilepile.config.BuildConfig.buildType
    }

    @Provides
    @Named("isDebug")
    fun provideIsDebug(): Boolean {
        return com.smilepile.config.BuildConfig.isQual ||
               com.smilepile.BuildConfig.DEBUG
    }
}
```

---

## 9. Resource Management Strategy

### 9.1 Current Resources Structure

**Main Resources:**
```
android/app/src/main/res/
├── values/
│   ├── strings.xml (169 lines)
│   ├── colors.xml
│   ├── themes.xml
│   └── font_certs.xml
├── mipmap-*/
│   ├── ic_launcher.png
│   └── ic_launcher_round.png
├── drawable/
└── xml/
    ├── backup_rules.xml
    ├── data_extraction_rules.xml
    └── file_paths.xml
```

**Current app_name:** `<string name="app_name">SmilePile</string>` (line 3)

### 9.2 Tier-Specific Resources

**Required for Wave 3:**

**Create flavor source sets:**
```
android/app/src/
├── qual/
│   └── res/
│       └── values/
│           └── strings.xml
├── stage/
│   └── res/
│       └── values/
│           └── strings.xml
├── beta/
│   └── res/
│       └── values/
│           └── strings.xml
└── prod/
    └── res/
        └── values/
            └── strings.xml
```

**qual/res/values/strings.xml:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">SmilePile Qual</string>
</resources>
```

**stage/res/values/strings.xml:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">SmilePile Stage</string>
</resources>
```

**beta/res/values/strings.xml:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">SmilePile Beta</string>
</resources>
```

**prod/res/values/strings.xml:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">SmilePile</string>
</resources>
```

### 9.3 App Icon Differentiation (Future)

**Optional for Wave 3, Recommended for Future:**
```
android/app/src/
├── qual/res/mipmap-*/
│   ├── ic_launcher.png (QUAL icon - e.g., with badge)
│   └── ic_launcher_round.png
├── stage/res/mipmap-*/
│   └── ... (STAGE icon)
├── beta/res/mipmap-*/
│   └── ... (BETA icon)
└── prod/res/mipmap-*/
    └── ... (PROD icon - final branding)
```

**Defer to future iteration unless user requests**

---

## 10. Testing Strategy

### 10.1 Existing Test Infrastructure

**Tiered Testing Script:** `/Users/adamstack/SmilePile/android/app/tier-tests.gradle`

**Test Tiers (109 lines):**
- Tier 1: Critical (Security, Data Integrity) - BLOCKING
- Tier 2: Important (ViewModels, Repositories) - BLOCKING
- Tier 3: UI (Components, Integration) - WARNING ONLY
- Smoke Tests: Quick validation subset

**Test Execution (from deploy_qual.sh):**
```bash
./gradlew app:testTier1Critical  # MUST PASS
./gradlew app:testTier2Important # MUST PASS
./gradlew app:testTier3UI        # WARNING if fails
```

**Status:** ✅ Already integrated into deploy_qual.sh (lines 159-272)

### 10.2 Flavor-Specific Testing

**Test Variants After Flavor Implementation:**
```
testQualDebugUnitTest
testStageReleaseUnitTest
testBetaReleaseUnitTest
testProdReleaseUnitTest
```

**Recommended Test Additions for Wave 3:**

**Create BuildConfigTest.kt:**
```kotlin
package com.smilepile.config

import org.junit.Test
import org.junit.Assert.*

class BuildConfigTest {

    @Test
    fun `build type should be valid tier`() {
        val validTiers = listOf("qual", "stage", "beta", "prod")
        assertTrue(
            "BUILD_TYPE_ENV must be qual, stage, beta, or prod",
            validTiers.contains(BuildConfig.buildType)
        )
    }

    @Test
    fun `tier display name should match build type`() {
        val expectedName = when (BuildConfig.buildType) {
            "qual" -> "QUAL"
            "stage" -> "STAGE"
            "beta" -> "BETA"
            "prod" -> "PROD"
            else -> "UNKNOWN"
        }
        assertEquals(expectedName, BuildConfig.tierDisplayName)
    }

    @Test
    fun `application ID should match tier expectations`() {
        when (BuildConfig.buildType) {
            "qual" -> assertTrue(
                "QUAL should have .qual suffix",
                BuildConfig.applicationId.endsWith(".qual")
            )
            "stage", "beta", "prod" -> assertEquals(
                "Stage/Beta/Prod should use base package",
                "com.smilepile",
                BuildConfig.applicationId
            )
        }
    }
}
```

### 10.3 Integration with deploy_qual.sh

**Current Android Test Execution (line 392):**
```bash
./gradlew assembleDebug
```

**After Wave 3 Implementation:**
```bash
# Update to use specific flavor variant
./gradlew assembleQualDebug

# APK path will change:
# OLD: app/build/outputs/apk/debug/app-debug.apk
# NEW: app/build/outputs/apk/qual/debug/app-qual-debug.apk
```

**Required Updates in deploy_qual.sh:**
1. Change `assembleDebug` → `assembleQualDebug`
2. Update APK path to include flavor directory
3. Update package name for app launch: `com.smilepile.qual`

---

## 11. Deployment Script Integration

### 11.1 Current Android Deployment (deploy_qual.sh)

**Relevant Lines:**

**Build Command (line 392):**
```bash
./gradlew assembleDebug
```

**APK Path (line 398):**
```bash
local apk_path="$PROJECT_ROOT/android/app/build/outputs/apk/debug/app-debug.apk"
```

**Package Name for Launch (line 454):**
```bash
adb -s "$device" shell monkey -p com.smilepile -c android.intent.category.LAUNCHER 1
```

### 11.2 Required Modifications for Wave 3

**Build Command Update:**
```bash
# OLD
./gradlew assembleDebug

# NEW
./gradlew assembleQualDebug
```

**APK Path Update:**
```bash
# OLD
local apk_path="$PROJECT_ROOT/android/app/build/outputs/apk/debug/app-debug.apk"

# NEW
local apk_path="$PROJECT_ROOT/android/app/build/outputs/apk/qual/debug/app-qual-debug.apk"
```

**Package Name Update:**
```bash
# OLD
adb -s "$device" shell monkey -p com.smilepile -c android.intent.category.LAUNCHER 1

# NEW
adb -s "$device" shell monkey -p com.smilepile.qual -c android.intent.category.LAUNCHER 1
```

**Artifact Copy Update (line 463):**
```bash
# OLD
cp "$apk_path" "$DEPLOY_ROOT/artifacts/qual/SmilePile-v${VERSION_NAME}-qual.apk"

# NEW (path already includes qual, just needs verification)
cp "$apk_path" "$DEPLOY_ROOT/artifacts/qual/SmilePile-v${VERSION_NAME}-qual.apk"
```

### 11.3 Future Deployment Scripts

**Wave 6 will create:**
- `deploy/deploy_stage.sh` → `assembleStageRelease` → Play Internal Testing
- `deploy/deploy_beta.sh` → `assembleBetaRelease` → Play Closed Testing
- `deploy/deploy_prod.sh` → `assembleProdRelease` → Play Production

**Bundle Generation for Play Store:**
```bash
# AAB (Android App Bundle) for Play Console
./gradlew bundleStageRelease   # Generates .aab for STAGE
./gradlew bundleBetaRelease    # Generates .aab for BETA
./gradlew bundleProdRelease    # Generates .aab for PROD
```

---

## 12. Security Considerations

### 12.1 Keystore Security

**Critical Security Requirements:**

1. **NEVER commit keystores to git**
   - ✅ Already in .gitignore: `*.keystore`, `**/keystores/*.keystore`

2. **NEVER commit keystore.properties**
   - ✅ Already in .gitignore: `android/keystore.properties` (lines 155, 225)

3. **Backup keystores to multiple secure locations**
   - Production keystore: Cloud storage + local encrypted backup + team vault
   - QUAL keystore: Less critical, can regenerate

4. **Use strong passwords**
   - Minimum 20 characters
   - Mix of uppercase, lowercase, numbers, symbols
   - Store in password manager (1Password, LastPass, etc.)

### 12.2 ProGuard Security

**Existing Rules Analysis:**
```proguard
# GOOD: Keeps line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable

# GOOD: Obfuscates source file names
-renamesourcefileattribute SourceFile

# GOOD: Protects data models from over-optimization
-keep class com.smilepile.data.models.** { *; }
```

**Additional Security Rules for Wave 3:**
```proguard
# Protect BuildConfig tier detection
-keep class com.smilepile.BuildConfig {
    public static final java.lang.String BUILD_TYPE_ENV;
}

# Protect custom BuildConfig module
-keep class com.smilepile.config.BuildConfig { *; }

# Remove logging in production
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
```

### 12.3 BUILD_TYPE_ENV Tampering Protection

**Threat:** Malicious user could decompile APK and change BUILD_TYPE_ENV

**Mitigation:**
1. **ProGuard obfuscation** (already enabled for release)
2. **Server-side validation** (Wave 4: API endpoint must verify tier)
3. **Certificate pinning** (future consideration)
4. **Runtime integrity checks** (optional):

```kotlin
object BuildConfigValidator {
    fun validateTier(): Boolean {
        // Check that BUILD_TYPE_ENV matches expected package name
        val packageName = BuildConfig.applicationId
        val tier = BuildConfig.buildType

        return when (tier) {
            "qual" -> packageName.endsWith(".qual")
            "stage", "beta", "prod" -> packageName == "com.smilepile"
            else -> false
        }
    }
}
```

### 12.4 Google Play App Signing

**Recommended Approach:**

**Upload Keystore Strategy:**
1. Generate production keystore (for signing uploads)
2. Enable Google Play App Signing on first upload
3. Google generates and manages app signing key
4. Upload key used for future uploads
5. Google re-signs with app signing key

**Benefits:**
- Google holds the ultimate signing key (recovery possible)
- Upload key can be reset if compromised
- Automatic APK optimization
- Security updates without developer intervention

**Implementation:** Wave 1 or first STAGE deployment

---

## 13. Potential Conflicts and Challenges

### 13.1 Build Variant Explosion

**Challenge:** 4 flavors × 2 build types = 8 build variants

**Variants:**
```
qualDebug, qualRelease
stageDebug, stageRelease
betaDebug, betaRelease
prodDebug, prodRelease
```

**Mitigation:**
- Disable unused variants:
```kotlin
android {
    variantFilter {
        if (name.startsWith("stage") && name.endsWith("Debug")) {
            ignore = true  // stageDebug not needed
        }
        if (name.startsWith("beta") && name.endsWith("Debug")) {
            ignore = true  // betaDebug not needed
        }
        if (name.startsWith("prod") && name.endsWith("Debug")) {
            ignore = true  // prodDebug NEVER use
        }
    }
}
```

**Result:** 5 useful variants (qualDebug, qualRelease, stageRelease, betaRelease, prodRelease)

### 13.2 Dependency Resolution Conflicts

**Potential Issue:** Flavor-specific dependencies causing conflicts

**Current Status:** Not applicable (no flavor-specific dependencies planned)

**If Needed:**
```kotlin
dependencies {
    qualImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
    // Ensure no conflicts with main dependencies
}
```

### 13.3 Resource Merging

**Challenge:** Flavor resources override main resources

**Resolution Order:**
```
Build Variant (highest priority)
  ↓
Build Type (debug/release)
  ↓
Product Flavor (qual/stage/beta/prod)
  ↓
Main Source Set (lowest priority)
```

**Best Practice:**
- Keep shared resources in `main/res/`
- Only override tier-specific values in flavor source sets
- Document overrides in flavor directories

### 13.4 Hilt/DI Flavor Variants

**Challenge:** Hilt modules may need flavor-specific configurations

**Current Status:** No flavor-specific DI needed for Wave 3

**If Needed (future):**
```
android/app/src/
├── qual/java/com/smilepile/di/
│   └── QualNetworkModule.kt (debug network configs)
└── prod/java/com/smilepile/di/
    └── ProdNetworkModule.kt (production network configs)
```

### 13.5 Testing Across Flavors

**Challenge:** Tests must run on correct flavor variants

**Solution:**
```bash
# Test specific flavor
./gradlew testQualDebugUnitTest

# Test all flavors (long)
./gradlew testDebugUnitTest  # Runs all debug flavors

# Recommended: Only test primary variants
./gradlew testQualDebugUnitTest
./gradlew testProdReleaseUnitTest
```

**Integration with deploy_qual.sh:**
- Update tier test tasks to run `testQualDebugUnitTest`
- Ensure test coverage includes BuildConfig validation

---

## 14. Android vs iOS Pattern Comparison

### 14.1 Configuration Approach

| Aspect | iOS (Wave 2) | Android (Wave 3) |
|--------|-------------|------------------|
| **Config Files** | 4 xcconfig files | Product flavors in build.gradle.kts |
| **Build Schemes** | 4 Xcode schemes | Build variants (flavor × buildType) |
| **Tier Detection** | Info.plist → BuildConfig.swift | buildConfigField → BuildConfig.kt |
| **Package/Bundle ID** | PRODUCT_BUNDLE_IDENTIFIER | applicationId + applicationIdSuffix |
| **App Name** | APP_DISPLAY_NAME in xcconfig | strings.xml in flavor source sets |
| **Signing** | CODE_SIGN_IDENTITY in xcconfig | signingConfigs in build.gradle.kts |
| **Build Tool** | xcodebuild | Gradle |

### 14.2 Deployment Parity

| Tier | iOS | Android |
|------|-----|---------|
| **QUAL** | `SmilePile Qual.app`<br/>Bundle: `com.smilepile.qual` | `app-qual-debug.apk`<br/>Package: `com.smilepile.qual` |
| **STAGE** | `SmilePile Stage.app`<br/>Bundle: `com.smilepile` | `app-stage-release.aab`<br/>Package: `com.smilepile` |
| **BETA** | `SmilePile Beta.app`<br/>Bundle: `com.smilepile` | `app-beta-release.aab`<br/>Package: `com.smilepile` |
| **PROD** | `SmilePile.app`<br/>Bundle: `com.smilepile` | `app-prod-release.aab`<br/>Package: `com.smilepile` |

### 14.3 BUILD_TYPE_ENV Detection

**iOS Pattern (from BuildConfig.swift):**
```swift
public static var buildType: String {
    guard let buildType = bundle.object(
        forInfoDictionaryKey: "BUILD_TYPE_ENV"
    ) as? String else {
        return "qual"
    }
    return buildType
}
```

**Android Pattern (recommended):**
```kotlin
object BuildConfig {
    val buildType: String
        get() = com.smilepile.BuildConfig.BUILD_TYPE_ENV
}
```

**Key Difference:**
- iOS reads from Info.plist at runtime
- Android uses compile-time constant from generated BuildConfig

**Advantage:** Android approach is more secure (compile-time, harder to tamper)

---

## 15. Implementation Recommendations

### 15.1 File Changes Required

**Files to Modify:**
1. `android/app/build.gradle.kts` - Add product flavors and signing configs
2. `deploy/deploy_qual.sh` - Update build commands and paths
3. `android/app/proguard-rules.pro` - Add BuildConfig protection rules

**Files to Create:**
1. `android/keystore.properties` - Signing credentials (NEVER commit)
2. `android/app/src/main/java/com/smilepile/config/BuildConfig.kt` - Tier detection module
3. `android/app/src/qual/res/values/strings.xml` - QUAL app name
4. `android/app/src/stage/res/values/strings.xml` - STAGE app name
5. `android/app/src/beta/res/values/strings.xml` - BETA app name
6. `android/app/src/prod/res/values/strings.xml` - PROD app name
7. `android/app/src/test/java/com/smilepile/config/BuildConfigTest.kt` - Tier validation tests

**Keystores to Generate:**
1. `~/keystores/smilepile-production.keystore` - For STAGE/BETA/PROD
2. `~/keystores/smilepile-qual.keystore` - For QUAL (optional, can use debug)

### 15.2 Implementation Order

**Recommended Sequence (mirrors Wave 2 iOS approach):**
1. Generate keystores and create keystore.properties
2. Add flavorDimensions to build.gradle.kts
3. Define 4 product flavors (qual, stage, beta, prod)
4. Add buildConfigField("BUILD_TYPE_ENV") to each flavor
5. Configure signingConfigs for each flavor
6. Create flavor-specific resource directories
7. Create BuildConfig.kt module in config/ package
8. Update deploy_qual.sh for qualDebug variant
9. Create BuildConfigTest.kt unit tests
10. Build and verify each flavor

### 15.3 Testing Strategy

**Build Verification:**
```bash
# Build each flavor
./gradlew assembleQualDebug
./gradlew assembleStageRelease
./gradlew assembleBetaRelease
./gradlew assembleProdRelease

# Verify package names
aapt dump badging app/build/outputs/apk/qual/debug/app-qual-debug.apk | grep package
# Expected: package: name='com.smilepile.qual'

aapt dump badging app/build/outputs/apk/prod/release/app-prod-release.apk | grep package
# Expected: package: name='com.smilepile'

# Verify app names
aapt dump badging app/build/outputs/apk/qual/debug/app-qual-debug.apk | grep application-label
# Expected: application-label:'SmilePile Qual'
```

**Runtime Verification:**
```kotlin
// In app code, log on startup:
Log.d("BuildConfig", "Tier: ${BuildConfig.buildType}")
Log.d("BuildConfig", "Package: ${BuildConfig.applicationId}")
Log.d("BuildConfig", "Display: ${BuildConfig.tierDisplayName}")
```

### 15.4 Rollback Plan

**If Implementation Fails:**
1. Remove product flavors from build.gradle.kts
2. Delete flavor source sets (qual/, stage/, beta/, prod/)
3. Delete BuildConfig.kt module
4. Revert deploy_qual.sh changes
5. Git revert to previous commit
6. Continue using existing debug/release builds

**Rollback Difficulty:** LOW (no breaking changes to existing functionality)

---

## 16. Wave 2 iOS Learnings Applied to Wave 3

### 16.1 Lessons from iOS Implementation

**From Wave 2 Evidence:**

**What Went Well:**
1. ✅ XCConfig pattern clean and maintainable
2. ✅ BuildConfig module provides simple API
3. ✅ Tier detection works reliably
4. ✅ Side-by-side installation for QUAL valuable

**What Could Improve:**
1. ⚠️ Initial xcconfig values weren't applied (hardcoded in project.pbxproj)
2. ⚠️ Manual Xcode scheme creation tedious
3. ⚠️ Test-safe bundle initialization required

**Applied to Android:**
1. ✅ Use buildConfigField (compile-time, can't be ignored like xcconfig)
2. ✅ Gradle handles variant generation automatically (no manual schemes)
3. ✅ BuildConfig.kt will use generated BuildConfig (no bundle/context needed)
4. ✅ Verify package names early in testing phase

### 16.2 iOS BuildConfig Pattern Adaptation

**iOS BuildConfig.swift Structure:**
```swift
public struct BuildConfig {
    public static var buildType: String { ... }
    public static var isQual: Bool { ... }
    public static var tierDisplayName: String { ... }
}
```

**Android BuildConfig.kt Adaptation:**
```kotlin
object BuildConfig {
    val buildType: String
        get() = com.smilepile.BuildConfig.BUILD_TYPE_ENV
    val isQual: Boolean
        get() = buildType == "qual"
    val tierDisplayName: String
        get() = when (buildType) { ... }
}
```

**Key Parallels:**
- Same property names (buildType, isQual, tierDisplayName)
- Same tier detection logic
- Same helper methods
- Platform-specific implementation details hidden

### 16.3 Documentation Patterns

**Wave 2 Created:**
- 01-research-findings.md (27 KB)
- 02-implementation-plan.md (60 KB)
- 03-security-audit.md (30 KB)
- Plus 7 more evidence files

**Wave 3 Will Follow:**
- Same 9-phase Atlas workflow
- Same evidence documentation structure
- Same parallel agent execution (security + peer-review)
- Parallel patterns to iOS for consistency

---

## 17. Critical Success Criteria

### 17.1 Must Have for Wave 3

**Build Success:**
- ✅ All 4 flavors build without errors
- ✅ Package names correct (com.smilepile.qual for QUAL, com.smilepile for others)
- ✅ App names display correctly per tier
- ✅ BUILD_TYPE_ENV accessible from Kotlin code
- ✅ Signing configs work for all tiers
- ✅ ProGuard rules don't break tier detection

**Deployment Integration:**
- ✅ deploy_qual.sh successfully builds qualDebug
- ✅ APK installs on devices/emulators
- ✅ App launches with correct package name
- ✅ No regression in existing functionality

**Testing:**
- ✅ Tier 1 tests pass for qualDebug variant
- ✅ Tier 2 tests pass for qualDebug variant
- ✅ BuildConfigTest validates tier detection
- ✅ ProGuard doesn't break release builds

### 17.2 Nice to Have (Defer if Needed)

**Optional for Wave 3:**
- 🔵 Tier-specific app icons (can add in future wave)
- 🔵 Flavor-specific ProGuard rules (only if issues arise)
- 🔵 Hilt DI modules for tier configs (Wave 4 if needed)
- 🔵 Automated testing of all 4 flavors (focus on qual + prod)

---

## 18. Risk Assessment

### 18.1 High Risk Items

**None identified for Wave 3**

**Rationale:**
- Product flavors are standard Android practice
- Build variant system well-established
- No architectural changes to app code
- Rollback is straightforward

### 18.2 Medium Risk Items

1. **Keystore Management**
   - Risk: Lost keystore = cannot update app in Play Store
   - Mitigation: Multiple encrypted backups, Google Play App Signing
   - Probability: Low (with proper backup procedures)

2. **ProGuard Breaking BuildConfig**
   - Risk: R8 optimizes away BUILD_TYPE_ENV constant
   - Mitigation: Keep rules already identified, test release builds
   - Probability: Low (BuildConfig typically preserved)

3. **Deployment Script Changes**
   - Risk: Wrong paths/commands in deploy_qual.sh
   - Mitigation: Thorough testing, parallel testing with current script
   - Probability: Low (well-defined changes)

### 18.3 Low Risk Items

1. **Flavor Resource Conflicts**
   - Risk: Flavor resources not overriding correctly
   - Mitigation: Test app name display, verify resource merging
   - Probability: Very Low (standard Android feature)

2. **Build Time Increase**
   - Risk: Multiple flavors slow down builds
   - Mitigation: Use variant filters to disable unused variants
   - Probability: Low (minimal impact)

---

## 19. File Paths Reference

### 19.1 Key Project Files

**Build Configuration:**
- Root Gradle: `/Users/adamstack/SmilePile/android/build.gradle.kts`
- App Gradle: `/Users/adamstack/SmilePile/android/app/build.gradle.kts`
- Gradle Properties: `/Users/adamstack/SmilePile/android/gradle.properties`
- ProGuard Rules: `/Users/adamstack/SmilePile/android/app/proguard-rules.pro`

**Android Manifest:**
- `/Users/adamstack/SmilePile/android/app/src/main/AndroidManifest.xml`

**Resources:**
- Strings: `/Users/adamstack/SmilePile/android/app/src/main/res/values/strings.xml`
- Colors: `/Users/adamstack/SmilePile/android/app/src/main/res/values/colors.xml`
- Themes: `/Users/adamstack/SmilePile/android/app/src/main/res/values/themes.xml`

**Application:**
- Main App: `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/SmilePileApplication.kt`
- MainActivity: `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/MainActivity.kt`

**Deployment:**
- QUAL Script: `/Users/adamstack/SmilePile/deploy/deploy_qual.sh`
- Build Libraries: `/Users/adamstack/SmilePile/deploy/lib/`

**Testing:**
- Tier Tests: `/Users/adamstack/SmilePile/android/app/tier-tests.gradle`
- Test Directory: `/Users/adamstack/SmilePile/android/app/src/test/`

### 19.2 Files to Create (Wave 3)

**Signing:**
- `~/keystores/smilepile-production.keystore` (BACKUP CRITICAL!)
- `~/keystores/smilepile-qual.keystore` (optional)
- `/Users/adamstack/SmilePile/android/keystore.properties` (NEVER commit)

**BuildConfig Module:**
- `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/config/BuildConfig.kt`

**Flavor Resources:**
- `/Users/adamstack/SmilePile/android/app/src/qual/res/values/strings.xml`
- `/Users/adamstack/SmilePile/android/app/src/stage/res/values/strings.xml`
- `/Users/adamstack/SmilePile/android/app/src/beta/res/values/strings.xml`
- `/Users/adamstack/SmilePile/android/app/src/prod/res/values/strings.xml`

**Tests:**
- `/Users/adamstack/SmilePile/android/app/src/test/java/com/smilepile/config/BuildConfigTest.kt`

**Evidence:**
- `/Users/adamstack/SmilePile/wave-evidence/wave-3/01-research-findings.md` (this file)
- `/Users/adamstack/SmilePile/wave-evidence/wave-3/02-implementation-plan.md`
- Additional evidence files as implementation progresses

---

## 20. Next Steps

### 20.1 Immediate Actions

**Phase 2: Story Creation (Product-Manager Agent):**
```
Create STORY-6.3-android-tier-config.md with acceptance criteria:
- Product flavors defined (qual, stage, beta, prod)
- Signing configurations for each flavor
- BUILD_TYPE_ENV buildConfigField in each flavor
- BuildConfig.kt module implemented and tested
- Flavor-specific resources (app names)
- All flavors build successfully
- BUILD_TYPE_ENV detection working at runtime
- deploy_qual.sh updated and verified
- Documentation updated
```

**Phase 3: Planning (Developer Agent):**
```
Create detailed implementation plan with:
- Complete build.gradle.kts product flavors configuration
- Signing configs with keystore.properties integration
- BuildConfig.kt full implementation
- Flavor resource directory structure and files
- deploy_qual.sh update specifications
- Testing verification commands
- Build validation procedures
```

### 20.2 Dependencies

**Prerequisites:**
- ✅ Wave 2 complete (iOS tier configuration done)
- ✅ Android project structure understood
- ✅ Gradle build system analyzed
- ✅ Testing infrastructure ready

**Blockers:**
- None identified

**Optional (can proceed without):**
- Wave 1 Play Console setup (can use debug signing for now)
- Production keystores (can generate during Wave 3)

### 20.3 Estimated Timeline

**Phase Estimates:**
- Phase 1 (Research): COMPLETE
- Phase 2 (Story): 30 minutes
- Phase 3 (Planning): 1-1.5 hours
- Phase 4 (Security Review): 30-45 minutes (parallel)
- Phase 5 (Implementation): 2-3 hours
- Phase 6 (Testing): 1-2 hours (parallel)
- Phase 7 (Validation): 30 minutes
- Phase 8 (Clean-up): 30 minutes
- Phase 9 (Deployment): 30 minutes

**Total**: 4-6 hours over 1-2 days

**Comparison to Wave 2:**
- Wave 2 iOS: 6-8 hours
- Wave 3 Android: 4-6 hours (slightly faster due to Gradle automation)

---

## 21. Conclusion

SmilePile's Android project is well-structured and ready for 4-tier configuration implementation. The modern Gradle Kotlin DSL architecture, existing Hilt DI setup, and clean Jetpack Compose codebase provide an excellent foundation. The product flavor approach parallels the iOS xcconfig pattern from Wave 2, ensuring consistency across platforms.

**Key Strengths:**
- ✅ Clean Gradle Kotlin DSL structure
- ✅ Modern architecture (Hilt + Compose)
- ✅ Existing tiered testing infrastructure
- ✅ BuildConfig feature already enabled
- ✅ ProGuard rules already configured
- ✅ Proper gitignore for secrets
- ✅ No legacy complexity or technical debt

**Implementation Readiness**: HIGH

**Risk Level**: LOW

**Recommended Approach**: Proceed with Phase 2 (Story Creation) to define detailed acceptance criteria and begin planning implementation. Follow Atlas 9-phase workflow with parallel agent execution where applicable.

**Parallel to iOS Wave 2:**
The Android implementation will mirror the iOS pattern:
- iOS xcconfig → Android product flavors
- iOS schemes → Android build variants
- iOS BuildConfig.swift → Android BuildConfig.kt
- Same tier names, same detection logic, same deployment strategy

**Expected Outcome**: Production-ready Android 4-tier deployment system with comprehensive tier detection, proper signing, and full integration with existing deployment infrastructure.

---

**Research Completed By**: General-Purpose Agent
**Date**: 2025-10-14
**Next Phase**: Story Creation (Product-Manager Agent)
**Story Reference**: /backlog/sprint-6/STORY-6.3-android-tier-config.md (to be created)
