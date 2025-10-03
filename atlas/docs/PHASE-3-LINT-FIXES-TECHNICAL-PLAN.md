# Phase 3 Technical Plan: Android Lint Fixes (LINT-1 to LINT-6)

**Document Version:** 1.0
**Created:** 2025-10-03
**Status:** Ready for Implementation
**Total Story Points:** 19 (1+1+1+3+5+8)

---

## Executive Summary

This document provides detailed technical implementation plans for all 6 lint fix user stories identified in Phase 1 research. The fixes range from critical security vulnerabilities to platform updates and code quality improvements.

**Implementation Approach:**
- **Phase A (Quick Wins):** LINT-1, LINT-2, LINT-3 - Deploy together in one release
- **Phase B (Platform Update):** LINT-5 - Update target SDK to 35
- **Phase C (UI Fixes):** LINT-4 - Fix Scaffold padding (depends on LINT-5)
- **Phase D (Dependencies):** LINT-6 - Update dependencies in 3 batches

---

## PHASE A: Quick Wins (LINT-1, LINT-2, LINT-3)

### Overview
Three low-risk, high-impact fixes that can be deployed together in a single release.

**Total Story Points:** 3 (1+1+1)
**Estimated Time:** 2-3 hours
**Risk Level:** LOW

---

### LINT-1: Fix Deprecated ExifInterface with Security Vulnerabilities

**Priority:** CRITICAL (P0)
**Story Points:** 1
**Risk:** LOW - Direct API-compatible replacement

#### Current State Analysis

**Affected Files:**
1. `/Users/adamstack/SmilePile/android/app/build.gradle.kts` - Line 152
2. `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/storage/PhotoImportManager.kt` - Line 8
3. `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/utils/ImageProcessor.kt` - Line 6

**Current Dependencies:**
- `androidx.exifinterface:exifinterface:1.3.7` (DEPRECATED, 4 security vulnerabilities)

**Current Import Statements:**
- PhotoImportManager.kt: `import androidx.exifinterface.media.ExifInterface`
- ImageProcessor.kt: `import android.media.ExifInterface` (ANDROID FRAMEWORK - DIFFERENT ISSUE)

#### Technical Implementation

**Step 1: Update build.gradle.kts dependency**

File: `/Users/adamstack/SmilePile/android/app/build.gradle.kts`

**BEFORE (Line 152):**
```kotlin
// EXIF Interface for metadata extraction
implementation("androidx.exifinterface:exifinterface:1.3.7")
```

**AFTER:**
```kotlin
// EXIF Interface for metadata extraction (Media3)
implementation("androidx.media3:media3-exifinterface:1.4.1")
```

**Step 2: Update PhotoImportManager.kt import**

File: `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/storage/PhotoImportManager.kt`

**BEFORE (Line 8):**
```kotlin
import androidx.exifinterface.media.ExifInterface
```

**AFTER:**
```kotlin
import androidx.media3.exifinterface.ExifInterface
```

**Step 3: Update ImageProcessor.kt import**

File: `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/utils/ImageProcessor.kt`

**CRITICAL FINDING:** ImageProcessor.kt currently uses `import android.media.ExifInterface` (line 6) which is the ANDROID FRAMEWORK version, NOT the androidx version. This needs to be updated to use the Media3 version for consistency.

**BEFORE (Line 6):**
```kotlin
import android.media.ExifInterface
```

**AFTER:**
```kotlin
import androidx.media3.exifinterface.ExifInterface
```

#### Verification Commands

```bash
# 1. Verify dependency change
grep -n "exifinterface" /Users/adamstack/SmilePile/android/app/build.gradle.kts

# 2. Verify no old androidx imports remain
grep -r "androidx.exifinterface.media.ExifInterface" /Users/adamstack/SmilePile/android/

# 3. Verify no android.media.ExifInterface remains
grep -r "import android.media.ExifInterface" /Users/adamstack/SmilePile/android/

# 4. Verify new Media3 imports
grep -r "androidx.media3.exifinterface.ExifInterface" /Users/adamstack/SmilePile/android/

# 5. Build Android app
cd /Users/adamstack/SmilePile && ./gradlew :android:app:assembleDebug

# 6. Run tests
cd /Users/adamstack/SmilePile && ./gradlew :android:app:testDebugUnitTest
```

#### Expected Results
- ✅ All imports updated to `androidx.media3.exifinterface.ExifInterface`
- ✅ Build succeeds without errors
- ✅ All tests pass
- ✅ Dependabot alert count decreases by 4

#### Rollback Plan
```bash
# Revert dependency in build.gradle.kts
implementation("androidx.exifinterface:exifinterface:1.3.7")

# Revert imports in PhotoImportManager.kt
import androidx.exifinterface.media.ExifInterface

# Revert imports in ImageProcessor.kt
import android.media.ExifInterface
```

---

### LINT-2: Fix Missing Android 11+ Package Visibility Declarations

**Priority:** HIGH (P1)
**Story Points:** 1
**Risk:** LOW - Additive change only

#### Current State Analysis

**Affected File:**
- `/Users/adamstack/SmilePile/android/app/src/main/AndroidManifest.xml`

**Current Structure:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Permissions -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    ...

    <!-- Features -->
    <uses-feature ... />

    <application ...>
        <!-- Activities -->
    </application>
</manifest>
```

**Missing:** `<queries>` section for Android 11+ package visibility

#### Technical Implementation

**Insert `<queries>` section AFTER `<uses-feature>` tags and BEFORE `<application>` tag**

File: `/Users/adamstack/SmilePile/android/app/src/main/AndroidManifest.xml`

**EXACT PLACEMENT (After line 21, before line 23):**

```xml
    <uses-feature
        android:name="android.hardware.camera.autofocus"
        android:required="false" />

    <!-- Package visibility queries for Android 11+ (API 30+) -->
    <queries>
        <!-- Camera intent for taking photos -->
        <intent>
            <action android:name="android.media.action.IMAGE_CAPTURE" />
        </intent>

        <!-- Photo picker and gallery intents -->
        <intent>
            <action android:name="android.intent.action.GET_CONTENT" />
            <data android:mimeType="image/*" />
        </intent>

        <intent>
            <action android:name="android.intent.action.PICK" />
            <data android:mimeType="image/*" />
        </intent>
    </queries>

    <application
        android:name=".SmilePileApplication"
        ...
```

#### Verification Commands

```bash
# 1. Verify queries section added
grep -A 20 "<queries>" /Users/adamstack/SmilePile/android/app/src/main/AndroidManifest.xml

# 2. Verify all three intents present
grep "IMAGE_CAPTURE\|GET_CONTENT\|PICK" /Users/adamstack/SmilePile/android/app/src/main/AndroidManifest.xml

# 3. Run Android Lint
cd /Users/adamstack/SmilePile && ./gradlew :android:app:lint

# 4. Check for package visibility warnings
cd /Users/adamstack/SmilePile && ./gradlew :android:app:lint | grep -i "query\|visibility"

# 5. Build Android app
cd /Users/adamstack/SmilePile && ./gradlew :android:app:assembleDebug
```

#### Expected Results
- ✅ `<queries>` section present with 3 intent declarations
- ✅ No lint warnings about QueryIntentActivitiesOptions
- ✅ Build succeeds without errors
- ✅ Manifest validates correctly

#### Rollback Plan
```bash
# Simply remove the entire <queries> section from AndroidManifest.xml
```

---

### LINT-3: Resize Oversized Vector Icon from 256dp to 200dp

**Priority:** LOW (P3)
**Story Points:** 1
**Risk:** VERY LOW - Simple viewport change

#### Current State Analysis

**Affected File:**
- `/Users/adamstack/SmilePile/android/app/src/main/res/drawable/ic_smilepile_logo.xml`

**CRITICAL FINDING:** The story mentions `ic_camera.xml` but no such file exists in the drawable directory. The actual file is `ic_smilepile_logo.xml` which has a 256x256 viewport.

**Current Files Found:**
1. `ic_smilepile_logo.xml` - 256x256 viewport (NEEDS FIX)
2. `ic_launcher_foreground.xml` - May also need checking

**Current Viewport (Lines 2-5):**
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="256dp"
    android:height="256dp"
    android:viewportWidth="256"
    android:viewportHeight="256">
```

#### Technical Implementation

File: `/Users/adamstack/SmilePile/android/app/src/main/res/drawable/ic_smilepile_logo.xml`

**BEFORE (Lines 2-5):**
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="256dp"
    android:height="256dp"
    android:viewportWidth="256"
    android:viewportHeight="256">
```

**AFTER:**
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="200dp"
    android:height="200dp"
    android:viewportWidth="200"
    android:viewportHeight="200">
```

**PATH DATA SCALING REQUIRED:** YES

Since the icon uses absolute coordinates in pathData, all coordinate values must be scaled by a factor of **200/256 = 0.78125**.

**Scaling Examples:**
- `M80,176` → `M62.5,137.5` (80 × 0.78125 = 62.5, 176 × 0.78125 = 137.5)
- `m-64,0` → `m-50,0` (64 × 0.78125 = 50)
- `a64,64` → `a50,50` (64 × 0.78125 = 50)
- `M128,128` → `M100,100` (128 × 0.78125 = 100)

**ALTERNATIVE APPROACH (SIMPLER):** Keep the viewport at 256 but change only the width/height attributes to 200dp. This preserves all path coordinates but scales the rendered output.

**RECOMMENDED SIMPLER FIX:**
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="200dp"
    android:height="200dp"
    android:viewportWidth="256"
    android:viewportHeight="256">
```

This approach:
- Changes physical size to 200dp × 200dp
- Keeps viewport at 256 × 256
- Requires NO path data changes
- Achieves the same lint fix goal

#### Verification Commands

```bash
# 1. Check viewport and size
grep -n "width\|height\|viewport" /Users/adamstack/SmilePile/android/app/src/main/res/drawable/ic_smilepile_logo.xml

# 2. Run Android Lint
cd /Users/adamstack/SmilePile && ./gradlew :android:app:lint

# 3. Check for VectorDrawableCompat warnings
cd /Users/adamstack/SmilePile && ./gradlew :android:app:lint | grep -i "vector"

# 4. Build Android app
cd /Users/adamstack/SmilePile && ./gradlew :android:app:assembleDebug

# 5. Visual verification (manual)
# Install app and verify logo displays correctly in UI
```

#### Expected Results
- ✅ Width and height attributes = 200dp
- ✅ No VectorDrawableCompat lint warnings
- ✅ Build succeeds without errors
- ✅ Visual inspection confirms logo displays correctly

#### Rollback Plan
```bash
# Revert width/height to 256dp in ic_smilepile_logo.xml
android:width="256dp"
android:height="256dp"
```

---

### Phase A Testing Strategy

**Build Validation:**
```bash
# Clean build
cd /Users/adamstack/SmilePile && ./gradlew clean

# Build debug variant
cd /Users/adamstack/SmilePile && ./gradlew :android:app:assembleDebug

# Build release variant
cd /Users/adamstack/SmilePile && ./gradlew :android:app:assembleRelease

# Run unit tests
cd /Users/adamstack/SmilePile && ./gradlew :android:app:testDebugUnitTest

# Run lint
cd /Users/adamstack/SmilePile && ./gradlew :android:app:lint
```

**Manual Testing:**
1. Install app on Android 11+ device
2. Test camera intent launching
3. Test photo picker opening
4. Test gallery selection
5. Verify EXIF data extraction works
6. Verify logo displays correctly in UI

**Success Criteria:**
- ✅ All builds pass
- ✅ All unit tests pass
- ✅ Lint warnings cleared for all 3 issues
- ✅ No Dependabot alerts for ExifInterface
- ✅ Camera/gallery functionality works on Android 11+
- ✅ Logo displays correctly

---

## PHASE B: Platform Update (LINT-5)

### LINT-5: Update Android Target SDK from 34 to 35

**Priority:** MEDIUM (P2)
**Story Points:** 5
**Risk:** MEDIUM - Potential breaking changes
**Dependencies:** None (but blocks LINT-4)

#### Current State Analysis

**Affected Files:**
1. `/Users/adamstack/SmilePile/android/app/build.gradle.kts` - Lines 16, 21

**Current Configuration:**
```kotlin
android {
    namespace = "com.smilepile"
    compileSdk = 34  // Line 16 - UPDATE TO 35

    defaultConfig {
        applicationId = "com.smilepile"
        minSdk = 24
        targetSdk = 34  // Line 21 - UPDATE TO 35
        versionCode = 251003001
        versionName = "25.10.03.001"
        ...
    }
}
```

#### Technical Implementation

**Step 1: Update compileSdk and targetSdk**

File: `/Users/adamstack/SmilePile/android/app/build.gradle.kts`

**BEFORE (Lines 16, 21):**
```kotlin
android {
    namespace = "com.smilepile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.smilepile"
        minSdk = 24
        targetSdk = 34
```

**AFTER:**
```kotlin
android {
    namespace = "com.smilepile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smilepile"
        minSdk = 24
        targetSdk = 35
```

**Step 2: Review Android 15 (SDK 35) Breaking Changes**

Based on codebase analysis, the following areas require review:

1. **Photo Picker & Permissions**
   - ✅ Already using READ_MEDIA_IMAGES (correct for SDK 35)
   - ✅ Already using photo picker patterns
   - ⚠️ Verify runtime permission requests still work

2. **Camera Permissions**
   - ✅ CAMERA permission already declared
   - ✅ CameraX libraries already at 1.3.1
   - ⚠️ Verify camera functionality on Android 15

3. **Biometric Authentication**
   - ✅ androidx.biometric:biometric:1.1.0
   - ⚠️ May need update to 1.2.0 for SDK 35 compatibility

4. **Foreground Services**
   - ⚠️ No foreground services detected in manifest
   - ✅ No changes needed

5. **Edge-to-Edge Display**
   - ⚠️ Scaffold padding issues (LINT-4 will fix after this)
   - Material3 already in use

6. **Deprecated APIs**
   - ⚠️ Check for any deprecated API usage in build output

#### Android 15 Specific Testing Checklist

**Permissions Testing:**
- [ ] READ_MEDIA_IMAGES permission request
- [ ] CAMERA permission request
- [ ] Biometric authentication prompts
- [ ] Runtime permission rationale dialogs

**Photo Picker Testing:**
- [ ] Photo picker launches correctly
- [ ] Single photo selection works
- [ ] Multiple photo selection works
- [ ] Selected photos import successfully

**Camera Testing:**
- [ ] Camera intent launches
- [ ] Captured photos save correctly
- [ ] EXIF data preserved
- [ ] Camera permissions enforced

**UI/Material3 Testing:**
- [ ] App bar rendering
- [ ] Bottom navigation
- [ ] System bars (status/navigation)
- [ ] Edge-to-edge behavior
- [ ] Dark mode compatibility

**Security Testing:**
- [ ] Biometric unlock works
- [ ] PIN authentication works
- [ ] Encrypted SharedPreferences accessible
- [ ] Kids Mode lock functional

#### Verification Commands

```bash
# 1. Verify SDK updates
grep -n "compileSdk\|targetSdk" /Users/adamstack/SmilePile/android/app/build.gradle.kts

# 2. Build with warnings enabled
cd /Users/adamstack/SmilePile && ./gradlew :android:app:assembleDebug --warning-mode all

# 3. Check for deprecated API usage
cd /Users/adamstack/SmilePile && ./gradlew :android:app:lintDebug | grep -i "api\|deprecated"

# 4. Run all unit tests
cd /Users/adamstack/SmilePile && ./gradlew :android:app:test

# 5. Build release variant
cd /Users/adamstack/SmilePile && ./gradlew :android:app:assembleRelease

# 6. Run lint analysis
cd /Users/adamstack/SmilePile && ./gradlew :android:app:lint
```

#### Expected Results
- ✅ compileSdk = 35
- ✅ targetSdk = 35
- ✅ All builds pass (debug and release)
- ✅ All unit tests pass
- ✅ No new deprecation warnings
- ✅ Lint passes without critical issues

#### Rollback Plan
```bash
# Revert to SDK 34 in build.gradle.kts
compileSdk = 34
targetSdk = 34

# Rebuild
./gradlew clean assembleDebug
```

#### Risk Mitigation

**Pre-Update Actions:**
1. Create backup branch: `git checkout -b backup/pre-sdk-35`
2. Document current behavior with screenshots
3. Review [Android 15 Migration Guide](https://developer.android.com/about/versions/15/migration)
4. Check library compatibility with SDK 35

**During Update:**
1. Update one value at a time (compileSdk first, then targetSdk)
2. Build and test after each change
3. Address deprecation warnings immediately
4. Document any behavioral changes observed

**Post-Update:**
1. Full regression testing on Android 15 emulator
2. Test on physical Android 14 device (regression)
3. Test on Android 7.0 device (minSdk 24 verification)
4. Monitor build logs for new warnings
5. Beta deploy before full release

**Estimated Timeline:**
- Research & preparation: 1 hour
- Implementation: 30 minutes
- Testing: 2-3 hours
- Total: 3.5-4.5 hours

---

## PHASE C: UI Fixes (LINT-4)

### LINT-4: Fix Unused Scaffold Padding Parameters

**Priority:** MEDIUM (P2)
**Story Points:** 3
**Risk:** LOW - Standard Compose pattern
**Dependencies:** LINT-5 (must update targetSdk to 35 first)

#### Current State Analysis

**Affected Files:**
1. `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/ui/screens/SettingsScreen.kt` - Lines 106-127

**CRITICAL FINDING:** The story mentions `GalleryScreen.kt` but no such file was found in the codebase. Only `SettingsScreen.kt` exists and needs fixing.

**Current SettingsScreen.kt Structure (Lines 106-127):**
```kotlin
Scaffold(
    modifier = modifier,
    topBar = {
        AppHeaderComponent(
            onViewModeClick = onNavigateToKidsMode,
            showViewModeButton = true
        )
    }
) { scaffoldPaddingValues ->  // ← paddingValues PROVIDED but NOT USED
    SettingsContent(
        scaffoldPaddingValues = scaffoldPaddingValues,  // ← PASSED TO CONTENT
        paddingValues = paddingValues,
        ...
    )
}
```

**Analysis:** The scaffoldPaddingValues IS being used correctly - it's passed to SettingsContent which applies it (lines 210-214). This may be a FALSE POSITIVE in the lint report.

**SettingsContent Implementation (Lines 209-216):**
```kotlin
LazyColumn(
    modifier = Modifier
        .fillMaxSize()
        .padding(scaffoldPaddingValues)  // ← PADDING IS APPLIED HERE
        .padding(bottom = paddingValues.calculateBottomPadding())
        .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
) { ... }
```

#### Technical Implementation

**FINDING:** The code is ALREADY CORRECT. The scaffoldPaddingValues is being properly applied in SettingsContent.

**However**, if lint is still complaining, it may be because the lambda parameter name doesn't match expectations. Some versions of the Compose compiler expect specific parameter names.

**Option 1: Suppress lint if code is correct**
```kotlin
@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
Scaffold( ... ) { scaffoldPaddingValues -> ... }
```

**Option 2: Use standard parameter name**
```kotlin
Scaffold(
    modifier = modifier,
    topBar = { ... }
) { innerPadding ->  // ← Standard naming convention
    SettingsContent(
        scaffoldPaddingValues = innerPadding,
        ...
    )
}
```

**Recommended Action:**
1. First verify if lint actually reports this as an issue
2. If yes, use Option 2 (rename to `innerPadding`)
3. If no, document as false positive and close story

#### Search for GalleryScreen

Since the story mentions GalleryScreen.kt but it wasn't found, we need to search more thoroughly:

**Search Commands:**
```bash
# Search for any Gallery-related Compose screens
find /Users/adamstack/SmilePile/android -name "*Gallery*.kt" -type f

# Search for LazyVerticalGrid usage (Gallery pattern)
grep -r "LazyVerticalGrid" /Users/adamstack/SmilePile/android --include="*.kt"

# Search for GalleryScreen function
grep -r "fun GalleryScreen\|class GalleryScreen" /Users/adamstack/SmilePile/android --include="*.kt"
```

**If GalleryScreen doesn't exist:**
- Update story to remove GalleryScreen reference
- Focus only on SettingsScreen (if actually needed)
- Verify with lint that this is a real issue

#### Verification Commands

```bash
# 1. Check for UnusedMaterial3ScaffoldPaddingParameter warnings
cd /Users/adamstack/SmilePile && ./gradlew :android:app:lint | grep -i "scaffold\|padding"

# 2. Verify SettingsScreen padding application
grep -A 10 "fun SettingsContent" /Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/ui/screens/SettingsScreen.kt

# 3. Build Android app
cd /Users/adamstack/SmilePile && ./gradlew :android:app:assembleDebug

# 4. Run Compose compiler checks
cd /Users/adamstack/SmilePile && ./gradlew :android:app:compileDebugKotlin --warning-mode all
```

#### Expected Results
- ✅ No UnusedMaterial3ScaffoldPaddingParameter warnings
- ✅ Build succeeds without errors
- ✅ UI layout respects system bars

#### Estimated Timeline
- Investigation: 30 minutes
- Implementation: 15 minutes (if needed)
- Testing: 1 hour
- Total: 1.5-2 hours

**NOTE:** This story may be a false positive and require no changes. Verify with lint first.

---

## PHASE D: Dependency Updates (LINT-6)

### LINT-6: Update 13 Outdated Android Dependencies

**Priority:** LOW (P3)
**Story Points:** 8
**Risk:** MEDIUM - Multiple dependencies
**Dependencies:** Should complete LINT-1 first (ExifInterface will be removed)

#### Current State Analysis

**File:** `/Users/adamstack/SmilePile/android/app/build.gradle.kts`

**Current Dependency Versions (from build.gradle.kts):**

| Library | Current | Story Says Latest | Actual Status |
|---------|---------|-------------------|---------------|
| androidx.core:core-ktx | 1.12.0 | 1.15.0 | UPDATE NEEDED |
| androidx.lifecycle:lifecycle-runtime-ktx | 2.7.0 | 2.8.7 | UPDATE NEEDED |
| androidx.activity:activity-compose | 1.8.2 | 1.9.3 | UPDATE NEEDED |
| Compose BOM | 2024.02.00 | ? | VERIFY LATEST |
| androidx.navigation:navigation-compose | 2.7.6 | 2.8.4 | UPDATE NEEDED |
| androidx.hilt:hilt-navigation-compose | 1.1.0 | ? | VERIFY LATEST |
| Compose UI (via BOM) | BOM-managed | 1.7.5 | UPDATE BOM |
| Material3 (via BOM) | BOM-managed | 1.3.1 | UPDATE BOM |
| com.google.accompanist:accompanist-permissions | 0.32.0 | 0.37.0 | UPDATE NEEDED |

**CRITICAL FINDING:** The story mentions many Compose UI libraries explicitly versioned, but the actual build.gradle.kts uses a Compose BOM (Bill of Materials) which manages versions centrally.

**Current BOM Usage (Line 135):**
```kotlin
// Jetpack Compose
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.ui:ui")  // Version from BOM
implementation("androidx.compose.ui:ui-tooling-preview")  // Version from BOM
implementation("androidx.compose.material3:material3")  // Version from BOM
```

#### Batched Update Strategy

**Batch 1: Core AndroidX Libraries (Low Risk)**

Update independently versioned libraries:

```kotlin
// Core Android dependencies
implementation("androidx.core:core-ktx:1.15.0")  // 1.12.0 → 1.15.0
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")  // 2.7.0 → 2.8.7
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")  // 2.7.0 → 2.8.7
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")  // 2.7.0 → 2.8.7
implementation("androidx.activity:activity-ktx:1.9.3")  // 1.8.2 → 1.9.3
implementation("androidx.activity:activity-compose:1.9.3")  // 1.8.2 → 1.9.3
implementation("androidx.fragment:fragment-ktx:1.8.5")  // 1.6.2 → 1.8.5
```

**Verification After Batch 1:**
```bash
cd /Users/adamstack/SmilePile && ./gradlew :android:app:assembleDebug
cd /Users/adamstack/SmilePile && ./gradlew :android:app:testDebugUnitTest
```

**Batch 2: Compose BOM & Navigation (Medium Risk)**

Update Compose BOM and navigation:

```kotlin
// Jetpack Compose - Updated BOM
implementation(platform("androidx.compose:compose-bom:2025.01.00"))  // 2024.02.00 → 2025.01.00
// All Compose UI, Material3 versions now managed by BOM

// Navigation Component
implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")  // 2.7.6 → 2.8.5
implementation("androidx.navigation:navigation-ui-ktx:2.8.5")  // 2.7.6 → 2.8.5
implementation("androidx.navigation:navigation-compose:2.8.5")  // 2.7.6 → 2.8.5
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")  // 1.1.0 → 1.2.0
```

**Verification After Batch 2:**
```bash
cd /Users/adamstack/SmilePile && ./gradlew :android:app:assembleDebug
cd /Users/adamstack/SmilePile && ./gradlew :android:app:testDebugUnitTest
# Manual UI testing required
```

**Batch 3: Supporting Libraries (Low Risk)**

Update remaining libraries:

```kotlin
// Permissions (Accompanist)
implementation("com.google.accompanist:accompanist-permissions:0.36.0")  // 0.32.0 → 0.36.0
// NOTE: 0.37.0 may not exist or may be incompatible, verify first

// Coroutines (if needed)
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")  // 1.7.3 → 1.9.0
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")  // 1.7.3 → 1.9.0
```

**Verification After Batch 3:**
```bash
cd /Users/adamstack/SmilePile && ./gradlew :android:app:assembleDebug
cd /Users/adamstack/SmilePile && ./gradlew :android:app:testDebugUnitTest
cd /Users/adamstack/SmilePile && ./gradlew :android:app:lint
```

#### Pre-Implementation Research

**REQUIRED: Check latest versions before implementing**

```bash
# Check for dependency updates
cd /Users/adamstack/SmilePile && ./gradlew :android:app:dependencyUpdates

# Check Compose BOM versions
# Visit: https://developer.android.com/jetpack/compose/bom/bom-mapping

# Check Accompanist versions
# Visit: https://github.com/google/accompanist/releases
```

#### Complete Dependency Update List

**File:** `/Users/adamstack/SmilePile/android/app/build.gradle.kts`

**BEFORE:**
```kotlin
dependencies {
    // Core Android dependencies
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Permissions (Accompanist)
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
}
```

**AFTER:**
```kotlin
dependencies {
    // Core Android dependencies
    implementation("androidx.core:core-ktx:1.15.0")  // UPDATED
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")  // UPDATED
    implementation("androidx.activity:activity-ktx:1.9.3")  // UPDATED
    implementation("androidx.fragment:fragment-ktx:1.8.5")  // UPDATED

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")  // UPDATED
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")  // UPDATED

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")  // UPDATED
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")  // UPDATED

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))  // UPDATED BOM
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")  // UPDATED
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")  // UPDATED
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")  // UPDATED
    implementation("androidx.navigation:navigation-compose:2.8.5")  // UPDATED
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")  // UPDATED

    // Permissions (Accompanist)
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")  // UPDATED
}
```

#### Verification Commands

```bash
# After EACH batch:

# 1. Show updated dependency versions
cd /Users/adamstack/SmilePile && ./gradlew :android:app:dependencies | grep -E "androidx\.|accompanist\.|kotlinx"

# 2. Clean build
cd /Users/adamstack/SmilePile && ./gradlew clean

# 3. Build debug variant
cd /Users/adamstack/SmilePile && ./gradlew :android:app:assembleDebug

# 4. Run unit tests
cd /Users/adamstack/SmilePile && ./gradlew :android:app:testDebugUnitTest

# 5. Run lint
cd /Users/adamstack/SmilePile && ./gradlew :android:app:lint

# After ALL batches:

# 6. Build release variant
cd /Users/adamstack/SmilePile && ./gradlew :android:app:assembleRelease

# 7. Check for dependency conflicts
cd /Users/adamstack/SmilePile && ./gradlew :android:app:dependencies --configuration debugRuntimeClasspath

# 8. Verify no new Dependabot alerts
# Check GitHub Dependabot tab
```

#### Manual Testing Checklist

**After Batch 1 (Core Libraries):**
- [ ] App launches successfully
- [ ] Basic navigation works
- [ ] No crashes on startup

**After Batch 2 (Compose & Navigation):**
- [ ] Gallery screen renders correctly
- [ ] Settings screen renders correctly
- [ ] Navigation between screens works
- [ ] Material3 components render correctly
- [ ] Dark mode works
- [ ] System bars render correctly

**After Batch 3 (Supporting Libraries):**
- [ ] Permission requests work (camera, storage)
- [ ] Photo import functionality works
- [ ] Settings save/load correctly
- [ ] Biometric authentication works
- [ ] PIN lock works

#### Rollback Plan

**If issues occur in any batch:**

```bash
# 1. Identify which batch caused the issue
# 2. Revert only that batch in build.gradle.kts
# 3. Rebuild and test
./gradlew clean assembleDebug

# Example: Revert Batch 2 (Compose BOM)
implementation(platform("androidx.compose:compose-bom:2024.02.00"))  # REVERT
implementation("androidx.navigation:navigation-compose:2.7.6")  # REVERT
```

**Full Rollback:**
```bash
git checkout HEAD -- android/app/build.gradle.kts
./gradlew clean assembleDebug
```

#### Estimated Timeline
- Research latest versions: 1 hour
- Batch 1 implementation + testing: 1.5 hours
- Batch 2 implementation + testing: 2 hours
- Batch 3 implementation + testing: 1.5 hours
- Final regression testing: 2 hours
- Total: 8 hours

---

## Implementation Order & Dependencies

### Dependency Graph

```
Phase A (Quick Wins) ─────────────────────┐
│                                          │
├─ LINT-1 (ExifInterface) ────────────────┤
├─ LINT-2 (Package Visibility) ───────────┤──── Deploy Together
├─ LINT-3 (Vector Icon) ──────────────────┤
│                                          │
└──────────────────────────────────────────┘
                 │
                 │ (Complete Phase A first)
                 ▼
           LINT-5 (TargetSDK 35) ───────────── Complete before LINT-4
                 │
                 │ (Blocks LINT-4)
                 ▼
           LINT-4 (Scaffold Padding) ─────────  Depends on LINT-5
                 │
                 │ (Can run anytime, preferably after LINT-1)
                 ▼
           LINT-6 (Dependencies) ──────────────  3 Batches with testing
```

### Recommended Timeline

**Week 1:**
- Day 1: Phase A (LINT-1, LINT-2, LINT-3) - Implementation & Testing
- Day 2: Phase A - Deployment & Monitoring
- Day 3: Phase B (LINT-5) - Research & Implementation
- Day 4: Phase B (LINT-5) - Testing & Verification
- Day 5: Phase B (LINT-5) - Deployment

**Week 2:**
- Day 1: Phase C (LINT-4) - Investigation & Implementation
- Day 2: Phase C (LINT-4) - Testing & Deployment
- Day 3: Phase D (LINT-6 Batch 1) - Implementation & Testing
- Day 4: Phase D (LINT-6 Batch 2) - Implementation & Testing
- Day 5: Phase D (LINT-6 Batch 3) - Implementation & Testing

**Total Time:** 10 working days

---

## Risk Assessment & Mitigation

### Phase A (Quick Wins) - LOW RISK

**Risks:**
- ExifInterface API incompatibility
- Package visibility breaking camera/gallery
- Vector icon rendering incorrectly

**Mitigation:**
- API is documented as compatible
- Queries are additive, won't break existing functionality
- Keep viewport, only change size attributes
- Thorough testing before deployment

### Phase B (TargetSDK 35) - MEDIUM RISK

**Risks:**
- Breaking changes in Android 15
- Permission behavior changes
- Material3 rendering issues
- Deprecated API usage

**Mitigation:**
- Comprehensive testing on Android 15 emulator
- Regression testing on Android 14 and below
- Review Android 15 migration guide thoroughly
- Beta deployment before full release
- Have rollback plan ready

### Phase C (Scaffold Padding) - LOW RISK

**Risks:**
- UI overlap with system bars
- Padding calculation errors
- Screen-specific layout issues

**Mitigation:**
- Verify issue exists before implementing
- Test on different screen sizes
- Test with gesture/button navigation
- Test in portrait and landscape

### Phase D (Dependencies) - MEDIUM RISK

**Risks:**
- Breaking API changes in updates
- Version conflicts between dependencies
- Compose BOM incompatibilities
- Accompanist library deprecations

**Mitigation:**
- Batched updates with testing between
- Read release notes for each library
- Check for breaking changes
- Ability to rollback individual batches
- Extensive regression testing

---

## Testing Strategy

### Automated Testing

**Build Validation:**
```bash
# Full build suite
./gradlew clean
./gradlew :android:app:assembleDebug
./gradlew :android:app:assembleRelease
./gradlew :android:app:testDebugUnitTest
./gradlew :android:app:lint
```

**Continuous Integration:**
- All builds must pass before deployment
- Unit tests must pass 100%
- Lint warnings must be addressed
- No new security vulnerabilities

### Manual Testing

**Device Coverage:**
- Android 7.0 (API 24) - minSdk verification
- Android 11 (API 30) - Package visibility testing
- Android 14 (API 34) - Regression testing
- Android 15 (API 35) - Target SDK testing

**Functional Testing:**
- [ ] Photo import from gallery
- [ ] Photo capture from camera
- [ ] EXIF metadata extraction
- [ ] Photo display in gallery grid
- [ ] Settings save/load
- [ ] Biometric authentication
- [ ] PIN lock/unlock
- [ ] Dark mode switching
- [ ] Kids Mode toggle
- [ ] Backup/restore functionality

**UI Testing:**
- [ ] All screens render correctly
- [ ] No overlap with system bars
- [ ] Bottom navigation visible
- [ ] App bar visible
- [ ] Material3 components styled correctly
- [ ] Dark theme works
- [ ] Landscape orientation
- [ ] Different screen sizes (phone/tablet)

### Performance Testing

**Metrics to Monitor:**
- App startup time
- Photo import speed
- Gallery scroll performance
- Memory usage
- Battery consumption
- APK size changes

---

## Deployment Strategy

### Phase A Deployment (Quick Wins)

**Pre-Deployment:**
1. All 3 changes implemented
2. Build passes
3. Tests pass
4. Manual testing complete

**Deployment:**
1. Merge to main branch
2. Trigger CI/CD pipeline
3. Deploy to internal testing track
4. Monitor for 24 hours
5. Promote to beta
6. Monitor for 1 week
7. Promote to production

**Success Metrics:**
- Zero crashes related to changes
- Dependabot alerts reduced by 4
- No lint warnings for fixed issues
- Camera/gallery work on Android 11+

### Phase B Deployment (TargetSDK 35)

**Pre-Deployment:**
1. SDK updated to 35
2. All tests pass
3. Android 15 testing complete
4. Regression testing complete

**Deployment:**
1. Beta release first (2 weeks minimum)
2. Monitor crash reports closely
3. Gather user feedback
4. Address any issues found
5. Gradual rollout (10% → 50% → 100%)

**Success Metrics:**
- No increase in crash rate
- All features functional on Android 15
- No regression on older Android versions
- Google Play compliance

### Phase C Deployment (Scaffold Padding)

**Pre-Deployment:**
1. Issue verified to exist
2. Fix implemented (if needed)
3. UI testing complete
4. No overlap with system bars

**Deployment:**
1. Include with Phase B or separate release
2. Standard deployment process
3. Monitor for UI issues

**Success Metrics:**
- No UI overlap reported
- Lint warning cleared
- Positive user feedback on UI

### Phase D Deployment (Dependencies)

**Pre-Deployment:**
1. All 3 batches complete
2. Extensive regression testing
3. Performance metrics stable
4. No new Dependabot alerts

**Deployment:**
1. Can be standalone or combined with other phases
2. Beta release recommended
3. Monitor for library-related issues
4. Watch for performance regressions

**Success Metrics:**
- No library-related crashes
- Performance metrics maintained
- All features functional
- Reduced technical debt

---

## Documentation Requirements

### Code Documentation

**Update Comments:**
- ExifInterface usage patterns
- Package visibility requirements
- Vector drawable size guidelines
- Scaffold padding best practices

### Technical Documentation

**Update Files:**
- `/Users/adamstack/SmilePile/atlas/docs/PHASE-3-LINT-FIXES-TECHNICAL-PLAN.md` (this file)
- `/Users/adamstack/SmilePile/backlog/tech-debt/LINT-*.md` (story completion)
- Build configuration notes
- Dependency version tracking

### Release Notes

**For Each Phase:**
- Summary of changes
- Benefits to users
- Breaking changes (if any)
- Migration notes

---

## Verification & Sign-Off

### Phase A Verification

- [ ] ExifInterface dependency updated to Media3 1.4.1
- [ ] All ExifInterface imports updated to androidx.media3
- [ ] Package visibility queries added to AndroidManifest.xml
- [ ] Vector icon size reduced to 200dp
- [ ] All builds pass
- [ ] All tests pass
- [ ] Lint warnings cleared
- [ ] Dependabot alerts reduced by 4
- [ ] Manual testing complete
- [ ] Deployed to production

### Phase B Verification

- [ ] compileSdk updated to 35
- [ ] targetSdk updated to 35
- [ ] Android 15 testing complete
- [ ] Regression testing complete
- [ ] No new deprecation warnings
- [ ] All builds pass
- [ ] All tests pass
- [ ] Beta feedback addressed
- [ ] Deployed to production

### Phase C Verification

- [ ] Scaffold padding issue verified
- [ ] Fix implemented (if needed)
- [ ] Lint warning cleared
- [ ] UI testing complete
- [ ] No system bar overlap
- [ ] Deployed to production

### Phase D Verification

- [ ] All 13 dependencies updated
- [ ] Build passes for all batches
- [ ] Tests pass for all batches
- [ ] Regression testing complete
- [ ] Performance metrics stable
- [ ] Lint passes
- [ ] Dependabot alerts cleared
- [ ] Deployed to production

---

## Appendix A: File Change Summary

### Files Modified by Phase

**Phase A:**
- `/Users/adamstack/SmilePile/android/app/build.gradle.kts` (LINT-1)
- `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/storage/PhotoImportManager.kt` (LINT-1)
- `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/utils/ImageProcessor.kt` (LINT-1)
- `/Users/adamstack/SmilePile/android/app/src/main/AndroidManifest.xml` (LINT-2)
- `/Users/adamstack/SmilePile/android/app/src/main/res/drawable/ic_smilepile_logo.xml` (LINT-3)

**Phase B:**
- `/Users/adamstack/SmilePile/android/app/build.gradle.kts` (LINT-5)

**Phase C:**
- `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/ui/screens/SettingsScreen.kt` (LINT-4) - IF NEEDED

**Phase D:**
- `/Users/adamstack/SmilePile/android/app/build.gradle.kts` (LINT-6)

---

## Appendix B: Command Reference

### Quick Testing Commands

```bash
# Full build validation
cd /Users/adamstack/SmilePile
./gradlew clean
./gradlew :android:app:assembleDebug
./gradlew :android:app:testDebugUnitTest
./gradlew :android:app:lint

# Verify specific changes
grep -n "exifinterface" android/app/build.gradle.kts
grep -A 20 "<queries>" android/app/src/main/AndroidManifest.xml
grep -n "width\|height\|viewport" android/app/src/main/res/drawable/ic_smilepile_logo.xml
grep -n "compileSdk\|targetSdk" android/app/build.gradle.kts

# Dependency analysis
./gradlew :android:app:dependencies | grep -E "androidx\.|accompanist"
./gradlew :android:app:dependencyUpdates
```

### Git Workflow

```bash
# Create feature branches
git checkout -b feature/lint-quick-wins-phase-a
git checkout -b feature/lint-target-sdk-35-phase-b
git checkout -b feature/lint-scaffold-padding-phase-c
git checkout -b feature/lint-dependencies-phase-d

# Commit messages (follow existing pattern)
git commit -m "fix: Replace deprecated ExifInterface with Media3 (LINT-1)"
git commit -m "fix: Add Android 11+ package visibility declarations (LINT-2)"
git commit -m "chore: Resize logo icon viewport to 200dp (LINT-3)"
git commit -m "chore: Update Android targetSdk from 34 to 35 (LINT-5)"
git commit -m "fix: Apply Scaffold padding in Settings screen (LINT-4)"
git commit -m "chore: Update 13 Android dependencies to latest versions (LINT-6)"
```

---

## Appendix C: Research Links

### Android 15 (SDK 35)
- [Android 15 Behavior Changes](https://developer.android.com/about/versions/15/behavior-changes-15)
- [Migration Guide](https://developer.android.com/about/versions/15/migration)
- [Google Play Target API Requirements](https://support.google.com/googleplay/android-developer/answer/11926878)

### Package Visibility
- [Package Visibility Documentation](https://developer.android.com/training/package-visibility)
- [Queries Element Reference](https://developer.android.com/guide/topics/manifest/queries-element)

### Dependencies
- [AndroidX Release Notes](https://developer.android.com/jetpack/androidx/versions/all-channel)
- [Compose Release Notes](https://developer.android.com/jetpack/androidx/releases/compose)
- [Compose BOM Mapping](https://developer.android.com/jetpack/compose/bom/bom-mapping)
- [Material3 Release Notes](https://developer.android.com/jetpack/androidx/releases/compose-material3)
- [Accompanist Releases](https://github.com/google/accompanist/releases)

### ExifInterface
- [Media3 ExifInterface Documentation](https://developer.android.com/reference/androidx/media3/exifinterface/ExifInterface)

---

## Document Change Log

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-10-03 | Phase 3 Planning | Initial technical plan created |

---

**END OF TECHNICAL PLAN**
