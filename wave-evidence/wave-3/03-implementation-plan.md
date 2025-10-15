# Wave 3: Android 4-Tier Configuration - Implementation Plan

**Planning Phase - Developer Agent**
**Date**: 2025-10-14
**Wave**: 3 of 10
**Story**: STORY-6.3-android-tier-config.md

---

## Executive Summary

This document provides a step-by-step implementation plan for configuring Android with 4-tier deployment support (QUAL, STAGE, BETA, PROD). The implementation uses Gradle product flavors, signing configurations, and a Kotlin BuildConfig module to enable runtime tier detection through BUILD_TYPE_ENV.

**Estimated Implementation Time**: 4-6 hours
**Risk Level**: LOW
**Prerequisites**: Wave 1 Complete (Foundation Setup), Wave 2 Complete (iOS Configuration)

---

## Table of Contents

1. [Implementation Overview](#implementation-overview)
2. [Step-by-Step Implementation](#step-by-step-implementation)
3. [Keystore Creation](#keystore-creation)
4. [Product Flavors Configuration](#product-flavors-configuration)
5. [Signing Configuration](#signing-configuration)
6. [BuildConfig Kotlin Module](#buildconfig-kotlin-module)
7. [Flavor-Specific Resources](#flavor-specific-resources)
8. [ProGuard Rules](#proguard-rules)
9. [Build Verification](#build-verification)
10. [Deployment Script Integration](#deployment-script-integration)
11. [Testing Procedures](#testing-procedures)
12. [Rollback Plan](#rollback-plan)
13. [Time Estimates](#time-estimates)

---

## Implementation Overview

### What We're Building

A 4-tier configuration system that enables Android apps to:
- Detect their deployment tier at runtime (QUAL, STAGE, BETA, PROD)
- Use tier-specific application IDs and display names
- Build using Gradle product flavors
- Integrate with automated deployment scripts

### Implementation Sequence

```
1. Generate production keystore → 15 minutes
2. Create keystore.properties file → 5 minutes
3. Add product flavors to build.gradle.kts → 20 minutes
4. Configure signing configs → 15 minutes
5. Create BuildConfig.kt module → 15 minutes
6. Create flavor-specific resources → 20 minutes
7. Update ProGuard rules → 10 minutes
8. Build verification (all flavors) → 30 minutes
9. Update deployment script → 15 minutes
10. Create tests → 30 minutes
11. Final verification → 30 minutes
12. Documentation → 30 minutes
─────────────────────────────────────────────
Total: 3.75 hours (core implementation)
```

---

## Step-by-Step Implementation

### Phase 1: Generate Production Keystore (15 minutes)

#### Step 1.1: Create Keystore Directory

**Command**:
```bash
mkdir -p ~/keystores
cd ~/keystores
```

**Verification**:
```bash
ls -la ~/keystores
```

Expected: Directory exists

#### Step 1.2: Generate Production Keystore

**Command**:
```bash
keytool -genkey -v \
  -keystore ~/keystores/smilepile-production.keystore \
  -alias smilepile \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

**Interactive Prompts**:
- Enter keystore password: (Use strong 20+ character password)
- Re-enter new password: (Confirm password)
- What is your first and last name?: SmilePile Team
- What is the name of your organizational unit?: Engineering
- What is the name of your organization?: SmilePile
- What is the name of your City or Locality?: [Your City]
- What is the name of your State or Province?: [Your State]
- What is the two-letter country code?: [Your Country Code]
- Is CN=SmilePile Team, OU=Engineering... correct?: yes
- Enter key password for <smilepile>: (Press RETURN to use same password as keystore)

**CRITICAL SECURITY NOTES**:
1. **BACKUP THIS KEYSTORE IMMEDIATELY** to multiple secure locations:
   - Cloud storage (encrypted)
   - External hard drive
   - Password manager with file attachments
2. **NEVER commit keystore to Git** (already in .gitignore)
3. **Store password in secure password manager**
4. **Loss of keystore = cannot update app in Play Store**

**Verification**:
```bash
ls -la ~/keystores/smilepile-production.keystore
keytool -list -v -keystore ~/keystores/smilepile-production.keystore
```

Expected output:
```
Alias name: smilepile
Creation date: [current date]
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=SmilePile Team, OU=Engineering...
Issuer: CN=SmilePile Team, OU=Engineering...
Serial number: [hex number]
Valid from: [date] until: [date in ~27 years]
```

#### Step 1.3: Backup Keystore Immediately

**Commands**:
```bash
# Create encrypted backup (using zip with password)
zip -e ~/keystores/smilepile-production-backup.zip ~/keystores/smilepile-production.keystore

# Copy to project-level secure location (not committed to git)
cp ~/keystores/smilepile-production.keystore ~/keystores/smilepile-production-backup-$(date +%Y%m%d).keystore

# Verify backup
unzip -l ~/keystores/smilepile-production-backup.zip
```

**CRITICAL**: Document keystore password and backup locations in team password manager

---

### Phase 2: Create keystore.properties File (5 minutes)

#### Step 2.1: Create keystore.properties

**File Location**: `/Users/adamstack/SmilePile/android/keystore.properties`

**Action**: Create file with signing credentials

**Command**:
```bash
cd /Users/adamstack/SmilePile/android
touch keystore.properties
# Edit file with secure editor (DO NOT use version control for this step)
```

**File Contents**:
```properties
# ============================================================================
# SmilePile Android Keystore Configuration
# ============================================================================
# CRITICAL: NEVER COMMIT THIS FILE TO GIT
# This file is already in .gitignore
#
# Production keystore for STAGE/BETA/PROD releases
# QUAL uses debug keystore (automatically generated by Android SDK)

# Production Keystore Path (absolute path)
storeFile=/Users/adamstack/keystores/smilepile-production.keystore

# Production Keystore Password
storePassword=YOUR_SECURE_PASSWORD_HERE

# Key Alias
keyAlias=smilepile

# Key Password (usually same as storePassword)
keyPassword=YOUR_SECURE_PASSWORD_HERE
```

**IMPORTANT**: Replace `YOUR_SECURE_PASSWORD_HERE` with actual keystore password

**Verification**:
```bash
ls -la /Users/adamstack/SmilePile/android/keystore.properties
cat /Users/adamstack/SmilePile/android/keystore.properties | grep "storeFile"
```

Expected: File exists, shows correct keystore path

**Security Check**:
```bash
cd /Users/adamstack/SmilePile
git status | grep keystore.properties
```

Expected: No output (file should NOT appear in git status due to .gitignore)

If file appears in git status:
```bash
# Verify .gitignore contains keystore.properties
grep "keystore.properties" .gitignore

# If missing, add to .gitignore
echo "android/keystore.properties" >> .gitignore
```

---

### Phase 3: Add Product Flavors to build.gradle.kts (20 minutes)

#### Step 3.1: Open build.gradle.kts for Editing

**File Location**: `/Users/adamstack/SmilePile/android/app/build.gradle.kts`

**Current Structure** (lines 14-46):
```kotlin
android {
    namespace = "com.smilepile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smilepile"
        minSdk = 24
        targetSdk = 35
        versionCode = 251014001
        versionName = "25.10.14.001"
        // ... rest of defaultConfig
    }

    buildTypes {
        release { /* ... */ }
        debug { /* ... */ }
    }

    // Additional configurations
    compileOptions { /* ... */ }
    kotlinOptions { /* ... */ }
    buildFeatures { /* ... */ }
}
```

#### Step 3.2: Add Flavor Dimensions (Insert after defaultConfig block)

**Location**: After line 30 (after defaultConfig closing brace)

**Insert**:
```kotlin
    flavorDimensions += "tier"

    productFlavors {
        create("qual") {
            dimension = "tier"
            applicationIdSuffix = ".qual"
            versionNameSuffix = "-qual"
            buildConfigField("String", "BUILD_TYPE_ENV", "\"qual\"")
        }
        create("stage") {
            dimension = "tier"
            versionNameSuffix = "-stage"
            buildConfigField("String", "BUILD_TYPE_ENV", "\"stage\"")
        }
        create("beta") {
            dimension = "tier"
            versionNameSuffix = "-beta"
            buildConfigField("String", "BUILD_TYPE_ENV", "\"beta\"")
        }
        create("prod") {
            dimension = "tier"
            buildConfigField("String", "BUILD_TYPE_ENV", "\"prod\"")
        }
    }
```

**Complete Modified Section** (lines 14-73):
```kotlin
android {
    namespace = "com.smilepile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smilepile"
        minSdk = 24
        targetSdk = 35
        versionCode = 251014001  // YYMMDDVVV format as integer
        versionName = "25.10.14.001"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    flavorDimensions += "tier"

    productFlavors {
        create("qual") {
            dimension = "tier"
            applicationIdSuffix = ".qual"
            versionNameSuffix = "-qual"
            buildConfigField("String", "BUILD_TYPE_ENV", "\"qual\"")
        }
        create("stage") {
            dimension = "tier"
            versionNameSuffix = "-stage"
            buildConfigField("String", "BUILD_TYPE_ENV", "\"stage\"")
        }
        create("beta") {
            dimension = "tier"
            versionNameSuffix = "-beta"
            buildConfigField("String", "BUILD_TYPE_ENV", "\"beta\"")
        }
        create("prod") {
            dimension = "tier"
            buildConfigField("String", "BUILD_TYPE_ENV", "\"prod\"")
        }
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

    // Rest of configuration unchanged
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // ... etc
}
```

**Verification**:
```bash
cd /Users/adamstack/SmilePile/android
grep -A 30 "flavorDimensions" app/build.gradle.kts
```

Expected: Shows productFlavors block with all 4 tiers

#### Step 3.3: Optional - Add Variant Filter (Reduce Build Complexity)

**Location**: After productFlavors block

**Optional Addition** (recommended to disable unused variants):
```kotlin
    // Disable unnecessary build variants
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
```

**Result**: Only useful variants remain:
- qualDebug (development)
- qualRelease (optional - for testing ProGuard)
- stageRelease (TestFlight Internal equivalent)
- betaRelease (TestFlight External equivalent)
- prodRelease (Play Store)

---

### Phase 4: Configure Signing Configs (15 minutes)

#### Step 4.1: Add Signing Configuration

**Location**: Before `android {` block in build.gradle.kts (top of file, after plugin declarations)

**Insert at Line 13** (after plugin declarations, before android block):
```kotlin
// Load keystore properties for production signing
val keystorePropertiesFile = rootProject.file("android/keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}
```

**Verification of Insertion Point**:
```kotlin
// Line 1-12: Plugin declarations
plugins {
    id("com.android.application")
    // ... other plugins
    id("jacoco")
}

// Apply JaCoCo configuration
apply(from = "../jacoco.gradle")

// NEW: Insert keystore loading here (line 13)
val keystorePropertiesFile = rootProject.file("android/keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

// Existing android block starts
android {
    namespace = "com.smilepile"
    // ...
}
```

#### Step 4.2: Add SigningConfigs Block

**Location**: Inside android block, after flavorDimensions/productFlavors, before buildTypes

**Insert**:
```kotlin
    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("production") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }
```

#### Step 4.3: Update Release Build Type to Use Production Signing

**Location**: Inside buildTypes block, modify release configuration

**Before**:
```kotlin
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
```

**After**:
```kotlin
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("production")
            } else {
                signingConfigs.getByName("debug")
            }
        }
```

**Complete Signing Configuration Section**:
```kotlin
// TOP OF FILE (after plugins, before android block)
val keystorePropertiesFile = rootProject.file("android/keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    // ... namespace, compileSdk, defaultConfig, flavorDimensions, productFlavors ...

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("production") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("production")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    // ... rest of configuration
}
```

**Verification**:
```bash
cd /Users/adamstack/SmilePile/android
grep -A 10 "signingConfigs" app/build.gradle.kts
```

Expected: Shows production signing config with keystore loading

#### Step 4.4: Gradle Sync

**Command**:
```bash
cd /Users/adamstack/SmilePile/android
./gradlew --refresh-dependencies
```

**Expected Output**:
```
BUILD SUCCESSFUL in [X]s
```

**Important**: Gradle sync must complete successfully before proceeding

---

### Phase 5: Create BuildConfig Kotlin Module (15 minutes)

#### Step 5.1: Create Config Package Directory

**Command**:
```bash
cd /Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile
mkdir -p config
```

**Verification**:
```bash
ls -la /Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/config
```

Expected: Directory exists

#### Step 5.2: Create BuildConfig.kt File

**File Location**: `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/config/BuildConfig.kt`

**Complete File Contents**:
```kotlin
package com.smilepile.config

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
 */
object BuildConfig {
    /**
     * Current deployment tier: qual, stage, beta, or prod
     * Read from generated BuildConfig class (set by product flavor)
     */
    val buildType: String
        get() = com.smilepile.BuildConfig.BUILD_TYPE_ENV

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
```

**Command**:
```bash
cd /Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/config
touch BuildConfig.kt
# Copy content above into file
```

**Verification**:
```bash
ls -la /Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/config/BuildConfig.kt
wc -l /Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/config/BuildConfig.kt
```

Expected: File exists with ~90 lines

#### Step 5.3: Verify Kotlin File Compiles

**Command**:
```bash
cd /Users/adamstack/SmilePile/android
./gradlew compileQualDebugKotlin
```

**Expected Output**:
```
BUILD SUCCESSFUL in [X]s
```

**If compilation fails**:
- Check package name matches: `com.smilepile.config`
- Verify generated BuildConfig has BUILD_TYPE_ENV field
- Ensure buildConfig feature is enabled in build.gradle.kts (line 60)

---

### Phase 6: Create Flavor-Specific Resources (20 minutes)

#### Step 6.1: Create QUAL Resource Directory

**Command**:
```bash
cd /Users/adamstack/SmilePile/android/app/src
mkdir -p qual/res/values
```

#### Step 6.2: Create QUAL strings.xml

**File Location**: `/Users/adamstack/SmilePile/android/app/src/qual/res/values/strings.xml`

**File Contents**:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Wave 3: Android 4-Tier Configuration -->
    <!-- QUAL tier app name for side-by-side installation -->
    <string name="app_name">SmilePile Qual</string>
</resources>
```

**Command**:
```bash
cd /Users/adamstack/SmilePile/android/app/src/qual/res/values
touch strings.xml
# Copy content above into file
```

**Verification**:
```bash
cat /Users/adamstack/SmilePile/android/app/src/qual/res/values/strings.xml
```

#### Step 6.3: Create STAGE Resource Directory and strings.xml

**Commands**:
```bash
cd /Users/adamstack/SmilePile/android/app/src
mkdir -p stage/res/values
cat > stage/res/values/strings.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Wave 3: Android 4-Tier Configuration -->
    <!-- STAGE tier app name for internal testing -->
    <string name="app_name">SmilePile Stage</string>
</resources>
EOF
```

#### Step 6.4: Create BETA Resource Directory and strings.xml

**Commands**:
```bash
cd /Users/adamstack/SmilePile/android/app/src
mkdir -p beta/res/values
cat > beta/res/values/strings.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Wave 3: Android 4-Tier Configuration -->
    <!-- BETA tier app name for external testing -->
    <string name="app_name">SmilePile Beta</string>
</resources>
EOF
```

#### Step 6.5: Create PROD Resource Directory and strings.xml

**Commands**:
```bash
cd /Users/adamstack/SmilePile/android/app/src
mkdir -p prod/res/values
cat > prod/res/values/strings.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Wave 3: Android 4-Tier Configuration -->
    <!-- PROD tier app name for Play Store release -->
    <string name="app_name">SmilePile</string>
</resources>
EOF
```

#### Step 6.6: Verify All Flavor Resources Created

**Command**:
```bash
cd /Users/adamstack/SmilePile/android/app/src
find qual stage beta prod -name "strings.xml" -exec echo "File: {}" \; -exec cat {} \; -exec echo "" \;
```

**Expected Output**:
```
File: qual/res/values/strings.xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">SmilePile Qual</string>
</resources>

File: stage/res/values/strings.xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">SmilePile Stage</string>
</resources>

File: beta/res/values/strings.xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">SmilePile Beta</string>
</resources>

File: prod/res/values/strings.xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">SmilePile</string>
</resources>
```

#### Step 6.7: Verify Flavor Directory Structure

**Command**:
```bash
tree /Users/adamstack/SmilePile/android/app/src/qual /Users/adamstack/SmilePile/android/app/src/stage /Users/adamstack/SmilePile/android/app/src/beta /Users/adamstack/SmilePile/android/app/src/prod
```

**Expected Structure**:
```
qual
└── res
    └── values
        └── strings.xml
stage
└── res
    └── values
        └── strings.xml
beta
└── res
    └── values
        └── strings.xml
prod
└── res
    └── values
        └── strings.xml
```

---

### Phase 7: Update ProGuard Rules (10 minutes)

#### Step 7.1: Open proguard-rules.pro

**File Location**: `/Users/adamstack/SmilePile/android/app/proguard-rules.pro`

**Current Contents**: 36 lines of existing rules

#### Step 7.2: Add BuildConfig Protection Rules

**Location**: End of file (after line 36)

**Add**:
```proguard
# ============================================================================
# Wave 3: Android 4-Tier Configuration
# ============================================================================

# Protect BuildConfig tier detection fields
# Without these rules, R8 may optimize away BUILD_TYPE_ENV constant
-keep class com.smilepile.BuildConfig {
    public static final java.lang.String BUILD_TYPE_ENV;
    public static final java.lang.String APPLICATION_ID;
    public static final java.lang.String VERSION_NAME;
    public static final int VERSION_CODE;
}

# Protect custom BuildConfig module
# Ensures tier detection methods remain accessible
-keep class com.smilepile.config.BuildConfig { *; }
```

**Command**:
```bash
cd /Users/adamstack/SmilePile/android/app
cat >> proguard-rules.pro << 'EOF'

# ============================================================================
# Wave 3: Android 4-Tier Configuration
# ============================================================================

# Protect BuildConfig tier detection fields
# Without these rules, R8 may optimize away BUILD_TYPE_ENV constant
-keep class com.smilepile.BuildConfig {
    public static final java.lang.String BUILD_TYPE_ENV;
    public static final java.lang.String APPLICATION_ID;
    public static final java.lang.String VERSION_NAME;
    public static final int VERSION_CODE;
}

# Protect custom BuildConfig module
# Ensures tier detection methods remain accessible
-keep class com.smilepile.config.BuildConfig { *; }
EOF
```

**Verification**:
```bash
cat /Users/adamstack/SmilePile/android/app/proguard-rules.pro | tail -n 20
```

Expected: Shows new BuildConfig protection rules

**Line Count Check**:
```bash
wc -l /Users/adamstack/SmilePile/android/app/proguard-rules.pro
```

Expected: ~52 lines (36 original + 16 new)

---

### Phase 8: Build Verification (30 minutes)

#### Step 8.1: Clean Build

**Command**:
```bash
cd /Users/adamstack/SmilePile/android
./gradlew clean
```

**Expected**: `BUILD SUCCESSFUL`

#### Step 8.2: Build qualDebug Variant

**Command**:
```bash
cd /Users/adamstack/SmilePile/android
./gradlew assembleQualDebug 2>&1 | tee /tmp/build-qual-debug.log
```

**Success Indicator**: `BUILD SUCCESSFUL`

**Verification - Check APK Created**:
```bash
ls -lh /Users/adamstack/SmilePile/android/app/build/outputs/apk/qual/debug/
```

Expected: `app-qual-debug.apk` file exists

**Verification - Check Package Name**:
```bash
aapt dump badging /Users/adamstack/SmilePile/android/app/build/outputs/apk/qual/debug/app-qual-debug.apk | grep package
```

Expected output:
```
package: name='com.smilepile.qual' versionCode='251014001' versionName='25.10.14.001-qual'
```

**Verification - Check App Name**:
```bash
aapt dump badging /Users/adamstack/SmilePile/android/app/build/outputs/apk/qual/debug/app-qual-debug.apk | grep application-label:
```

Expected output:
```
application-label:'SmilePile Qual'
```

#### Step 8.3: Build stageRelease Variant

**Command**:
```bash
cd /Users/adamstack/SmilePile/android
./gradlew assembleStageRelease 2>&1 | tee /tmp/build-stage-release.log
```

**Success Indicator**: `BUILD SUCCESSFUL`

**Verification**:
```bash
ls -lh /Users/adamstack/SmilePile/android/app/build/outputs/apk/stage/release/
aapt dump badging /Users/adamstack/SmilePile/android/app/build/outputs/apk/stage/release/app-stage-release.apk | grep package
```

Expected: `com.smilepile` with version suffix `-stage`

#### Step 8.4: Build betaRelease Variant

**Command**:
```bash
cd /Users/adamstack/SmilePile/android
./gradlew assembleBetaRelease 2>&1 | tee /tmp/build-beta-release.log
```

**Success Indicator**: `BUILD SUCCESSFUL`

**Verification**:
```bash
aapt dump badging /Users/adamstack/SmilePile/android/app/build/outputs/apk/beta/release/app-beta-release.apk | grep package
```

Expected: `com.smilepile` with version suffix `-beta`

#### Step 8.5: Build prodRelease Variant

**Command**:
```bash
cd /Users/adamstack/SmilePile/android
./gradlew assembleProdRelease 2>&1 | tee /tmp/build-prod-release.log
```

**Success Indicator**: `BUILD SUCCESSFUL`

**Verification**:
```bash
aapt dump badging /Users/adamstack/SmilePile/android/app/build/outputs/apk/prod/release/app-prod-release.apk | grep package
```

Expected: `com.smilepile` with NO version suffix (clean production)

#### Step 8.6: Build Summary

**Command**:
```bash
cd /Users/adamstack/SmilePile/android
for variant in qualDebug stageRelease betaRelease prodRelease; do
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Variant: $variant"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    ./gradlew assemble${variant} | grep -E "BUILD SUCCESSFUL|BUILD FAILED"
done
```

**Expected Output**:
```
Variant: qualDebug
BUILD SUCCESSFUL in [X]s

Variant: stageRelease
BUILD SUCCESSFUL in [X]s

Variant: betaRelease
BUILD SUCCESSFUL in [X]s

Variant: prodRelease
BUILD SUCCESSFUL in [X]s
```

#### Step 8.7: Verify ProGuard Doesn't Break Release Builds

**Check Release Build Logs**:
```bash
grep -i "R8\|proguard\|minification" /tmp/build-stage-release.log | head -n 20
```

**Expected**: No errors related to BuildConfig or tier detection

**Test BuildConfig Access in Release Build** (manual verification):
- Install stageRelease APK on device/emulator
- Launch app
- Navigate to Settings screen (which uses BuildConfig for version display)
- Verify no crashes

---

### Phase 9: Update Deployment Script (15 minutes)

#### Step 9.1: Update deploy_qual.sh - Build Command

**File Location**: `/Users/adamstack/SmilePile/deploy/deploy_qual.sh`

**Location**: Line 392 in `deploy_android_local()` function

**Before**:
```bash
./gradlew assembleDebug || {
    log ERROR "Android build failed"
    return 1
}
```

**After**:
```bash
./gradlew assembleQualDebug || {
    log ERROR "Android build failed"
    return 1
}
```

**Change**: `assembleDebug` → `assembleQualDebug`

#### Step 9.2: Update deploy_qual.sh - APK Path

**Location**: Line 398

**Before**:
```bash
local apk_path="$PROJECT_ROOT/android/app/build/outputs/apk/debug/app-debug.apk"
```

**After**:
```bash
local apk_path="$PROJECT_ROOT/android/app/build/outputs/apk/qual/debug/app-qual-debug.apk"
```

**Change**: Path now includes flavor directory `/qual/debug/` and flavor-specific filename

#### Step 9.3: Update deploy_qual.sh - Package Name for Launch

**Location**: Line 454

**Before**:
```bash
adb -s "$device" shell monkey -p com.smilepile -c android.intent.category.LAUNCHER 1
```

**After**:
```bash
adb -s "$device" shell monkey -p com.smilepile.qual -c android.intent.category.LAUNCHER 1
```

**Change**: `com.smilepile` → `com.smilepile.qual`

#### Step 9.4: Complete Modified Section

**Lines 382-468** (deploy_android_local function):
```bash
# Deploy to Android devices
deploy_android_local() {
    print_header "Android Local Deployment"

    cd "$PROJECT_ROOT/android"

    # Build APK
    log INFO "Building Android APK..."
    if [[ "$DRY_RUN" == "true" ]]; then
        log INFO "DRY RUN: Would build APK"
    else
        ./gradlew assembleQualDebug || {
            log ERROR "Android build failed"
            return 1
        }
    fi

    local apk_path="$PROJECT_ROOT/android/app/build/outputs/apk/qual/debug/app-qual-debug.apk"

    if [[ ! -f "$apk_path" ]] && [[ "$DRY_RUN" != "true" ]]; then
        log ERROR "APK not found at: $apk_path"
        return 1
    fi

    # Get connected devices and emulators
    log INFO "Checking for Android devices..."
    local devices=$(adb devices | grep -E "device$|emulator" | cut -f1 || true)

    # ... [device detection and emulator start code unchanged] ...

    # Deploy to each device
    for device in $devices; do
        log INFO "Deploying to device: $device"

        if [[ "$DRY_RUN" == "true" ]]; then
            log INFO "DRY RUN: Would install APK on $device"
        else
            adb -s "$device" install -r "$apk_path" || {
                log ERROR "Failed to install on device: $device"
                continue
            }

            # Launch app
            log INFO "Launching app on $device..."
            adb -s "$device" shell monkey -p com.smilepile.qual -c android.intent.category.LAUNCHER 1
        fi

        log SUCCESS "Deployed to device: $device"
    done

    # Copy APK to artifacts with version number
    mkdir -p "$DEPLOY_ROOT/artifacts/qual"
    if [[ "$DRY_RUN" != "true" ]]; then
        cp "$apk_path" "$DEPLOY_ROOT/artifacts/qual/SmilePile-v${VERSION_NAME}-qual.apk"
        log INFO "APK saved to artifacts as SmilePile-v${VERSION_NAME}-qual.apk"
    fi

    log SUCCESS "Android local deployment completed"
}
```

#### Step 9.5: Test Deployment Script (Dry Run)

**Command**:
```bash
cd /Users/adamstack/SmilePile
SKIP_TESTS=true DRY_RUN=true ./deploy/deploy_qual.sh android
```

**Expected Output**:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Android Local Deployment
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[INFO] Building Android APK...
[INFO] DRY RUN: Would build APK
```

#### Step 9.6: Test Deployment Script (Full Run)

**Prerequisites**:
- Android emulator running OR device connected
- ADB available in PATH

**Command**:
```bash
cd /Users/adamstack/SmilePile
SKIP_TESTS=true ./deploy/deploy_qual.sh android
```

**Expected Outcome**:
- Builds qualDebug successfully
- Installs app-qual-debug.apk to devices/emulators
- Launches com.smilepile.qual
- App appears on device with name "SmilePile Qual"

**Verification on Device**:
```bash
adb shell pm list packages | grep smilepile
```

Expected output:
```
package:com.smilepile.qual
```

**Verify App Name on Device**:
```bash
adb shell dumpsys package com.smilepile.qual | grep applicationInfo
```

---

### Phase 10: Create Tests (30 minutes)

#### Step 10.1: Create BuildConfigTest.kt

**File Location**: `/Users/adamstack/SmilePile/android/app/src/test/java/com/smilepile/config/BuildConfigTest.kt`

**Create Directory**:
```bash
cd /Users/adamstack/SmilePile/android/app/src/test/java/com/smilepile
mkdir -p config
```

**Complete File Contents**:
```kotlin
package com.smilepile.config

import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for BuildConfig tier detection functionality.
 * Validates BUILD_TYPE_ENV is correctly set per flavor.
 *
 * Wave 3: Android 4-Tier Configuration
 * Story: STORY-6.3-android-tier-config.md
 *
 * Run these tests for each build variant to verify tier detection:
 * - ./gradlew testQualDebugUnitTest
 * - ./gradlew testStageReleaseUnitTest
 * - ./gradlew testBetaReleaseUnitTest
 * - ./gradlew testProdReleaseUnitTest
 */
class BuildConfigTest {

    // MARK: - Build Type Detection

    @Test
    fun `build type should be valid tier`() {
        val validTiers = listOf("qual", "stage", "beta", "prod")
        assertTrue(
            "BUILD_TYPE_ENV must be qual, stage, beta, or prod. Got: ${BuildConfig.buildType}",
            validTiers.contains(BuildConfig.buildType)
        )
    }

    @Test
    fun `build type should never be null or empty`() {
        assertNotNull("buildType should not be null", BuildConfig.buildType)
        assertTrue("buildType should not be empty", BuildConfig.buildType.isNotEmpty())
    }

    @Test
    fun `exactly one tier should be active`() {
        val activeTiers = listOf(
            BuildConfig.isQual,
            BuildConfig.isStage,
            BuildConfig.isBeta,
            BuildConfig.isProd
        ).count { it }

        assertEquals(
            "Exactly one tier should be active. Active count: $activeTiers",
            1,
            activeTiers
        )
    }

    // MARK: - Tier Helper Methods

    @Test
    fun `qual tier detection should be consistent`() {
        if (BuildConfig.buildType == "qual") {
            assertTrue("isQual should be true when buildType is qual", BuildConfig.isQual)
            assertFalse("isStage should be false when buildType is qual", BuildConfig.isStage)
            assertFalse("isBeta should be false when buildType is qual", BuildConfig.isBeta)
            assertFalse("isProd should be false when buildType is qual", BuildConfig.isProd)
        }
    }

    @Test
    fun `stage tier detection should be consistent`() {
        if (BuildConfig.buildType == "stage") {
            assertFalse("isQual should be false when buildType is stage", BuildConfig.isQual)
            assertTrue("isStage should be true when buildType is stage", BuildConfig.isStage)
            assertFalse("isBeta should be false when buildType is stage", BuildConfig.isBeta)
            assertFalse("isProd should be false when buildType is stage", BuildConfig.isProd)
        }
    }

    @Test
    fun `beta tier detection should be consistent`() {
        if (BuildConfig.buildType == "beta") {
            assertFalse("isQual should be false when buildType is beta", BuildConfig.isQual)
            assertFalse("isStage should be false when buildType is beta", BuildConfig.isStage)
            assertTrue("isBeta should be true when buildType is beta", BuildConfig.isBeta)
            assertFalse("isProd should be false when buildType is beta", BuildConfig.isProd)
        }
    }

    @Test
    fun `prod tier detection should be consistent`() {
        if (BuildConfig.buildType == "prod") {
            assertFalse("isQual should be false when buildType is prod", BuildConfig.isQual)
            assertFalse("isStage should be false when buildType is prod", BuildConfig.isStage)
            assertFalse("isBeta should be false when buildType is prod", BuildConfig.isBeta)
            assertTrue("isProd should be true when buildType is prod", BuildConfig.isProd)
        }
    }

    // MARK: - Display Properties

    @Test
    fun `tier display name should match build type`() {
        val expectedName = when (BuildConfig.buildType) {
            "qual" -> "QUAL"
            "stage" -> "STAGE"
            "beta" -> "BETA"
            "prod" -> "PROD"
            else -> "UNKNOWN"
        }
        assertEquals(
            "tierDisplayName should match buildType",
            expectedName,
            BuildConfig.tierDisplayName
        )
    }

    @Test
    fun `tier display name should be uppercase`() {
        assertEquals(
            "tierDisplayName should be all uppercase",
            BuildConfig.tierDisplayName,
            BuildConfig.tierDisplayName.uppercase()
        )
    }

    // MARK: - Application ID

    @Test
    fun `application ID should match tier expectations`() {
        when (BuildConfig.buildType) {
            "qual" -> {
                assertTrue(
                    "QUAL should have .qual suffix. Got: ${BuildConfig.applicationId}",
                    BuildConfig.applicationId.endsWith(".qual")
                )
                assertEquals(
                    "QUAL should use com.smilepile.qual",
                    "com.smilepile.qual",
                    BuildConfig.applicationId
                )
            }
            "stage", "beta", "prod" -> {
                assertEquals(
                    "STAGE/BETA/PROD should use com.smilepile. Got: ${BuildConfig.applicationId}",
                    "com.smilepile",
                    BuildConfig.applicationId
                )
            }
        }
    }

    @Test
    fun `application ID should match generated BuildConfig`() {
        assertEquals(
            "BuildConfig wrapper should match generated BuildConfig.APPLICATION_ID",
            com.smilepile.BuildConfig.APPLICATION_ID,
            BuildConfig.applicationId
        )
    }

    // MARK: - Version Information

    @Test
    fun `version name should include tier suffix for non-prod`() {
        when (BuildConfig.buildType) {
            "qual" -> assertTrue(
                "QUAL version should include -qual suffix",
                BuildConfig.versionName.contains("-qual")
            )
            "stage" -> assertTrue(
                "STAGE version should include -stage suffix",
                BuildConfig.versionName.contains("-stage")
            )
            "beta" -> assertTrue(
                "BETA version should include -beta suffix",
                BuildConfig.versionName.contains("-beta")
            )
            "prod" -> assertFalse(
                "PROD version should NOT include tier suffix",
                BuildConfig.versionName.contains("-")
            )
        }
    }

    @Test
    fun `version code should be valid integer`() {
        assertTrue(
            "Version code should be positive",
            BuildConfig.versionCode > 0
        )
    }

    @Test
    fun `version name should follow date format`() {
        // Version format: YY.MM.DD.###[-tier]
        val versionPattern = Regex("""^\d{2}\.\d{2}\.\d{2}\.\d{3}(-\w+)?$""")
        assertTrue(
            "Version name should match YY.MM.DD.###[-tier] format. Got: ${BuildConfig.versionName}",
            versionPattern.matches(BuildConfig.versionName)
        )
    }

    // MARK: - Integration Tests

    @Test
    fun `print build configuration for verification`() {
        // This test always passes - it prints info for manual verification
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("BUILD CONFIGURATION DETECTION")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("Build Type:         ${BuildConfig.buildType}")
        println("Tier Display Name:  ${BuildConfig.tierDisplayName}")
        println("Application ID:     ${BuildConfig.applicationId}")
        println("Version Name:       ${BuildConfig.versionName}")
        println("Version Code:       ${BuildConfig.versionCode}")
        println("Is Debug:           ${BuildConfig.isDebug}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Verify tier-specific details
        when (BuildConfig.buildType) {
            "qual" -> {
                println("QUAL Tier Verification:")
                println("  - Unique package: ${BuildConfig.applicationId}")
                println("  - Side-by-side install: YES")
                println("  - Signing: Debug keystore")
            }
            "stage" -> {
                println("STAGE Tier Verification:")
                println("  - Production package: ${BuildConfig.applicationId}")
                println("  - TestFlight equivalent: Internal Testing")
                println("  - Signing: Production keystore")
            }
            "beta" -> {
                println("BETA Tier Verification:")
                println("  - Production package: ${BuildConfig.applicationId}")
                println("  - TestFlight equivalent: External Testing")
                println("  - Signing: Production keystore")
            }
            "prod" -> {
                println("PROD Tier Verification:")
                println("  - Production package: ${BuildConfig.applicationId}")
                println("  - Play Store release: YES")
                println("  - Signing: Production keystore")
            }
        }
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        assertTrue(true) // Always pass
    }

    // MARK: - Negative Tests

    @Test
    fun `build type should be lowercase`() {
        assertEquals(
            "buildType should be lowercase",
            BuildConfig.buildType,
            BuildConfig.buildType.lowercase()
        )
    }

    @Test
    fun `tier display name should never be UNKNOWN`() {
        assertNotEquals(
            "tierDisplayName should not be UNKNOWN (indicates invalid BUILD_TYPE_ENV)",
            "UNKNOWN",
            BuildConfig.tierDisplayName
        )
    }
}
```

**Command**:
```bash
cd /Users/adamstack/SmilePile/android/app/src/test/java/com/smilepile/config
touch BuildConfigTest.kt
# Copy content above into file
```

**Verification**:
```bash
ls -la /Users/adamstack/SmilePile/android/app/src/test/java/com/smilepile/config/BuildConfigTest.kt
wc -l /Users/adamstack/SmilePile/android/app/src/test/java/com/smilepile/config/BuildConfigTest.kt
```

Expected: File exists with ~250 lines

#### Step 10.2: Run Tests for qualDebug Variant

**Command**:
```bash
cd /Users/adamstack/SmilePile/android
./gradlew testQualDebugUnitTest --tests "com.smilepile.config.BuildConfigTest"
```

**Expected Output**:
```
BUILD SUCCESSFUL in [X]s
[X] tests completed
```

**Verify Test Output**:
```bash
cd /Users/adamstack/SmilePile/android
./gradlew testQualDebugUnitTest --tests "com.smilepile.config.BuildConfigTest.print build configuration for verification"
```

Should print:
```
Build Type:         qual
Tier Display Name:  QUAL
Application ID:     com.smilepile.qual
Version Name:       25.10.14.001-qual
```

#### Step 10.3: Run Tests for Other Variants (Optional Verification)

**Stage Release**:
```bash
./gradlew testStageReleaseUnitTest --tests "com.smilepile.config.BuildConfigTest"
```

**Beta Release**:
```bash
./gradlew testBetaReleaseUnitTest --tests "com.smilepile.config.BuildConfigTest"
```

**Prod Release**:
```bash
./gradlew testProdReleaseUnitTest --tests "com.smilepile.config.BuildConfigTest"
```

#### Step 10.4: Run Existing Tier Tests with qualDebug

**Command**:
```bash
cd /Users/adamstack/SmilePile/android
./gradlew app:testQualDebugTier1Critical
./gradlew app:testQualDebugTier2Important
```

**Expected**: All tests should pass (same as before Wave 3 changes)

---

### Phase 11: Final Verification (30 minutes)

#### Step 11.1: Clean Build All Primary Variants

**Command**:
```bash
cd /Users/adamstack/SmilePile/android
./gradlew clean

for variant in QualDebug StageRelease BetaRelease ProdRelease; do
    echo "========================================="
    echo "Building variant: $variant"
    echo "========================================="
    ./gradlew assemble${variant} | grep -E "BUILD SUCCESSFUL|BUILD FAILED|error:"
done
```

**Expected**: All 4 variants show `BUILD SUCCESSFUL`

#### Step 11.2: Verify APK Package Names

**Command**:
```bash
cd /Users/adamstack/SmilePile/android

echo "QUAL Package Name:"
aapt dump badging app/build/outputs/apk/qual/debug/app-qual-debug.apk | grep "^package:"

echo ""
echo "STAGE Package Name:"
aapt dump badging app/build/outputs/apk/stage/release/app-stage-release.apk | grep "^package:"

echo ""
echo "BETA Package Name:"
aapt dump badging app/build/outputs/apk/beta/release/app-beta-release.apk | grep "^package:"

echo ""
echo "PROD Package Name:"
aapt dump badging app/build/outputs/apk/prod/release/app-prod-release.apk | grep "^package:"
```

**Expected Output**:
```
QUAL Package Name:
package: name='com.smilepile.qual' versionCode='251014001' versionName='25.10.14.001-qual'

STAGE Package Name:
package: name='com.smilepile' versionCode='251014001' versionName='25.10.14.001-stage'

BETA Package Name:
package: name='com.smilepile' versionCode='251014001' versionName='25.10.14.001-beta'

PROD Package Name:
package: name='com.smilepile' versionCode='251014001' versionName='25.10.14.001'
```

#### Step 11.3: Verify App Names

**Command**:
```bash
cd /Users/adamstack/SmilePile/android

echo "QUAL App Name:"
aapt dump badging app/build/outputs/apk/qual/debug/app-qual-debug.apk | grep "application-label:"

echo "STAGE App Name:"
aapt dump badging app/build/outputs/apk/stage/release/app-stage-release.apk | grep "application-label:"

echo "BETA App Name:"
aapt dump badging app/build/outputs/apk/beta/release/app-beta-release.apk | grep "application-label:"

echo "PROD App Name:"
aapt dump badging app/build/outputs/apk/prod/release/app-prod-release.apk | grep "application-label:"
```

**Expected Output**:
```
QUAL App Name:
application-label:'SmilePile Qual'

STAGE App Name:
application-label:'SmilePile Stage'

BETA App Name:
application-label:'SmilePile Beta'

PROD App Name:
application-label:'SmilePile'
```

#### Step 11.4: Verify Git Changes

**Command**:
```bash
cd /Users/adamstack/SmilePile
git status
```

**Expected Files Modified**:
- `android/app/build.gradle.kts` (modified)
- `android/app/proguard-rules.pro` (modified)
- `deploy/deploy_qual.sh` (modified)

**Expected Files Added**:
- `android/app/src/main/java/com/smilepile/config/BuildConfig.kt` (new)
- `android/app/src/qual/res/values/strings.xml` (new)
- `android/app/src/stage/res/values/strings.xml` (new)
- `android/app/src/beta/res/values/strings.xml` (new)
- `android/app/src/prod/res/values/strings.xml` (new)
- `android/app/src/test/java/com/smilepile/config/BuildConfigTest.kt` (new)

**Expected Files NOT in Git** (verify .gitignore working):
- `android/keystore.properties` (should NOT appear in git status)
- `~/keystores/smilepile-production.keystore` (external to repo)

**Verification**:
```bash
git status | grep keystore.properties
# Should return nothing (file is ignored)
```

#### Step 11.5: Side-by-Side Installation Test

**Test**: Install QUAL alongside STAGE on same device/emulator

**Commands**:
```bash
cd /Users/adamstack/SmilePile/android

# Build both variants
./gradlew assembleQualDebug
./gradlew assembleStageRelease

# Install QUAL
adb install app/build/outputs/apk/qual/debug/app-qual-debug.apk

# Install STAGE (should succeed without uninstalling QUAL)
adb install app/build/outputs/apk/stage/release/app-stage-release.apk

# Verify both installed
adb shell pm list packages | grep smilepile
```

**Expected Output**:
```
package:com.smilepile
package:com.smilepile.qual
```

**Verification**: Both apps should appear on device home screen:
- "SmilePile Qual"
- "SmilePile Stage"

#### Step 11.6: Test Deployment Script End-to-End

**Command**:
```bash
cd /Users/adamstack/SmilePile
SKIP_TESTS=true ./deploy/deploy_qual.sh android
```

**Expected Outcome**:
- Builds qualDebug successfully
- Installs to connected devices/emulators
- Launches com.smilepile.qual
- App shows "SmilePile Qual" on device

**Verification**:
```bash
# Check app is installed
adb shell pm list packages | grep smilepile.qual

# Check app is running
adb shell pidof com.smilepile.qual
```

---

## Keystore Creation

### Production Keystore Generation

**Full Command with All Options**:
```bash
keytool -genkey -v \
  -keystore ~/keystores/smilepile-production.keystore \
  -alias smilepile \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=SmilePile Team, OU=Engineering, O=SmilePile, L=City, ST=State, C=US" \
  -storepass [SECURE_PASSWORD] \
  -keypass [SECURE_PASSWORD]
```

**Non-Interactive Command** (for CI/CD environments):
```bash
keytool -genkey -v \
  -keystore ~/keystores/smilepile-production.keystore \
  -alias smilepile \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=SmilePile Team, OU=Engineering, O=SmilePile, L=San Francisco, ST=CA, C=US" \
  -storepass "${KEYSTORE_PASSWORD}" \
  -keypass "${KEYSTORE_PASSWORD}" \
  -noprompt
```

### Keystore Information Verification

**List Keystore Contents**:
```bash
keytool -list -v -keystore ~/keystores/smilepile-production.keystore
```

**Export Certificate (for Google Play App Signing)**:
```bash
keytool -export \
  -keystore ~/keystores/smilepile-production.keystore \
  -alias smilepile \
  -file ~/keystores/smilepile-production.cert
```

### Keystore Backup Strategy

**Recommended Backup Locations**:
1. **Encrypted Cloud Storage**: Google Drive, Dropbox, iCloud (in encrypted container)
2. **Password Manager**: 1Password, LastPass (as secure note with file attachment)
3. **External Hard Drive**: Encrypted USB drive in physical safe
4. **Team Vault**: Shared team password manager

**Backup Commands**:
```bash
# Create timestamped backup
cp ~/keystores/smilepile-production.keystore \
   ~/keystores/backup/smilepile-production-$(date +%Y%m%d).keystore

# Create encrypted zip
zip -e ~/keystores/backup/smilepile-keystore-backup.zip \
   ~/keystores/smilepile-production.keystore

# Verify backup integrity
keytool -list -keystore ~/keystores/backup/smilepile-production-$(date +%Y%m%d).keystore
```

---

## Product Flavors Configuration

### Complete build.gradle.kts Product Flavors Section

```kotlin
android {
    namespace = "com.smilepile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smilepile"
        minSdk = 24
        targetSdk = 35
        versionCode = 251014001
        versionName = "25.10.14.001"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Wave 3: 4-Tier Configuration
    flavorDimensions += "tier"

    productFlavors {
        create("qual") {
            dimension = "tier"
            applicationIdSuffix = ".qual"
            versionNameSuffix = "-qual"
            buildConfigField("String", "BUILD_TYPE_ENV", "\"qual\"")
        }
        create("stage") {
            dimension = "tier"
            versionNameSuffix = "-stage"
            buildConfigField("String", "BUILD_TYPE_ENV", "\"stage\"")
        }
        create("beta") {
            dimension = "tier"
            versionNameSuffix = "-beta"
            buildConfigField("String", "BUILD_TYPE_ENV", "\"beta\"")
        }
        create("prod") {
            dimension = "tier"
            buildConfigField("String", "BUILD_TYPE_ENV", "\"prod\"")
        }
    }

    // Optional: Reduce build variant complexity
    variantFilter {
        if (name.startsWith("stage") && name.endsWith("Debug")) {
            ignore = true
        }
        if (name.startsWith("beta") && name.endsWith("Debug")) {
            ignore = true
        }
        if (name.startsWith("prod") && name.endsWith("Debug")) {
            ignore = true
        }
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("production") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("production")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    // Rest of configuration unchanged
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true  // CRITICAL: Required for BUILD_TYPE_ENV
    }

    // ... rest of file unchanged
}
```

### Build Variant Matrix

**After Configuration**:
```
Variants (with variantFilter):
✓ qualDebug         - Development (most common)
✗ qualRelease       - Optional (testing ProGuard with QUAL tier)
✗ stageDebug        - Not needed (disabled by variantFilter)
✓ stageRelease      - Internal testing (TestFlight Internal equivalent)
✗ betaDebug         - Not needed (disabled by variantFilter)
✓ betaRelease       - External testing (TestFlight External equivalent)
✗ prodDebug         - NEVER use (disabled by variantFilter)
✓ prodRelease       - Play Store release
```

**Primary Variants**: qualDebug, stageRelease, betaRelease, prodRelease

---

## Signing Configuration

### Complete Signing Configuration

```kotlin
// TOP OF FILE (after plugins, after jacoco apply)
// Load keystore properties for production signing
val keystorePropertiesFile = rootProject.file("android/keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    // ... namespace, compileSdk, defaultConfig, flavorDimensions, productFlavors ...

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("production") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Apply production signing to release builds
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("production")
            } else {
                // Fallback to debug if keystore not configured
                signingConfigs.getByName("debug")
            }
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
            // Debug builds automatically use debug keystore
        }
    }

    // ... rest of configuration
}
```

### Signing Per Tier

| Tier | Build Type | Keystore Used | Purpose |
|------|-----------|---------------|---------|
| QUAL | Debug | Android SDK debug keystore | Local development |
| QUAL | Release | Production keystore | Optional (testing ProGuard) |
| STAGE | Release | Production keystore | Internal testing |
| BETA | Release | Production keystore | External testing |
| PROD | Release | Production keystore | Play Store |

**Debug Keystore Location** (auto-generated):
- macOS: `~/.android/debug.keystore`
- Linux: `~/.android/debug.keystore`
- Windows: `C:\Users\[username]\.android\debug.keystore`

**Debug Keystore Properties**:
- Alias: androiddebugkey
- Password: android
- Valid for: 365 days (auto-renewed)
- Common Name: Android Debug

---

## BuildConfig Kotlin Module

See Phase 5, Step 5.2 for complete file contents (90 lines).

### Usage Examples

**In ViewModel**:
```kotlin
import com.smilepile.config.BuildConfig

class SettingsViewModel : ViewModel() {
    fun getVersionInfo(): String {
        return "Version ${BuildConfig.versionName} (${BuildConfig.tierDisplayName})"
    }

    fun getApiEndpoint(): String {
        return when {
            BuildConfig.isQual -> "https://api-qual.smilepile.com"
            BuildConfig.isStage -> "https://api-stage.smilepile.com"
            BuildConfig.isBeta -> "https://api-beta.smilepile.com"
            BuildConfig.isProd -> "https://api.smilepile.com"
            else -> "https://api-qual.smilepile.com"
        }
    }

    fun shouldShowDebugMenu(): Boolean {
        return BuildConfig.isQual || BuildConfig.isStage
    }
}
```

**In Composable UI**:
```kotlin
import com.smilepile.config.BuildConfig

@Composable
fun SettingsScreen() {
    Column {
        Text("App Version: ${BuildConfig.versionName}")
        Text("Build Tier: ${BuildConfig.tierDisplayName}")
        Text("Package: ${BuildConfig.applicationId}")

        if (BuildConfig.isQual || BuildConfig.isStage) {
            Button(onClick = { /* Show debug menu */ }) {
                Text("Debug Menu")
            }
        }
    }
}
```

**In Hilt Module** (optional):
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object BuildConfigModule {

    @Provides
    @Named("buildTier")
    fun provideBuildTier(): String {
        return com.smilepile.config.BuildConfig.buildType
    }

    @Provides
    @Named("apiBaseUrl")
    fun provideApiBaseUrl(): String {
        return when {
            com.smilepile.config.BuildConfig.isQual -> "https://api-qual.smilepile.com"
            com.smilepile.config.BuildConfig.isStage -> "https://api-stage.smilepile.com"
            com.smilepile.config.BuildConfig.isBeta -> "https://api-beta.smilepile.com"
            com.smilepile.config.BuildConfig.isProd -> "https://api.smilepile.com"
            else -> "https://api-qual.smilepile.com"
        }
    }
}
```

---

## Flavor-Specific Resources

### Resource Merging Priority

```
Build Variant (highest priority)
  ↓
Build Type (debug/release)
  ↓
Product Flavor (qual/stage/beta/prod)
  ↓
Main Source Set (lowest priority)
```

**Example**: For `qualDebug` variant:
1. Check `src/qualDebug/res/values/strings.xml` (highest)
2. Check `src/debug/res/values/strings.xml`
3. Check `src/qual/res/values/strings.xml`
4. Check `src/main/res/values/strings.xml` (fallback)

### Complete Flavor Resource Structure

```
android/app/src/
├── main/
│   └── res/
│       └── values/
│           └── strings.xml (fallback values)
├── qual/
│   └── res/
│       └── values/
│           └── strings.xml (app_name = "SmilePile Qual")
├── stage/
│   └── res/
│       └── values/
│           └── strings.xml (app_name = "SmilePile Stage")
├── beta/
│   └── res/
│       └── values/
│           └── strings.xml (app_name = "SmilePile Beta")
└── prod/
    └── res/
        └── values/
            └── strings.xml (app_name = "SmilePile")
```

### Future Enhancement: Tier-Specific App Icons

**Optional for Wave 3, recommended for future**:
```
android/app/src/
├── qual/res/
│   └── mipmap-*/
│       ├── ic_launcher.png (QUAL icon - e.g., with badge)
│       └── ic_launcher_round.png
├── stage/res/
│   └── mipmap-*/
│       └── ... (STAGE icon)
├── beta/res/
│   └── mipmap-*/
│       └── ... (BETA icon)
└── prod/res/
    └── mipmap-*/
        └── ... (PROD icon - final branding)
```

---

## ProGuard Rules

### Complete ProGuard Rules for Wave 3

See Phase 7, Step 7.2 for exact rules to add.

### ProGuard Verification Commands

**Test Release Build with ProGuard**:
```bash
cd /Users/adamstack/SmilePile/android
./gradlew assembleStageRelease

# Check if BuildConfig is preserved
unzip -p app/build/outputs/apk/stage/release/app-stage-release.apk classes.dex > /tmp/classes.dex
strings /tmp/classes.dex | grep "BUILD_TYPE_ENV"
```

Expected: Should find "BUILD_TYPE_ENV" string in APK

**Analyze ProGuard Mapping** (for debugging obfuscation):
```bash
cat app/build/outputs/mapping/stageRelease/mapping.txt | grep BuildConfig
```

---

## Build Verification

### Build Verification Checklist

**For Each Variant**:
- [ ] Builds without errors
- [ ] No warnings related to flavors
- [ ] Correct package name in APK
- [ ] Correct version name with tier suffix
- [ ] Correct app display name
- [ ] APK installs on device/emulator
- [ ] App launches successfully
- [ ] BuildConfig.buildType returns correct value

### Package Name Verification Commands

```bash
cd /Users/adamstack/SmilePile/android

# QUAL
aapt dump badging app/build/outputs/apk/qual/debug/app-qual-debug.apk | grep "^package:"
# Expected: name='com.smilepile.qual' versionName='[VERSION]-qual'

# STAGE
aapt dump badging app/build/outputs/apk/stage/release/app-stage-release.apk | grep "^package:"
# Expected: name='com.smilepile' versionName='[VERSION]-stage'

# BETA
aapt dump badging app/build/outputs/apk/beta/release/app-beta-release.apk | grep "^package:"
# Expected: name='com.smilepile' versionName='[VERSION]-beta'

# PROD
aapt dump badging app/build/outputs/apk/prod/release/app-prod-release.apk | grep "^package:"
# Expected: name='com.smilepile' versionName='[VERSION]' (no suffix)
```

### APK Size Analysis

```bash
cd /Users/adamstack/SmilePile/android

# Compare APK sizes
ls -lh app/build/outputs/apk/qual/debug/app-qual-debug.apk
ls -lh app/build/outputs/apk/stage/release/app-stage-release.apk
ls -lh app/build/outputs/apk/prod/release/app-prod-release.apk
```

**Expected**: Release APKs should be smaller than debug (ProGuard enabled)

---

## Deployment Script Integration

### Complete Modified deploy_qual.sh Sections

See Phase 9 for complete modified function.

### Testing Deployment Script

**Dry Run** (no actual installation):
```bash
cd /Users/adamstack/SmilePile
SKIP_TESTS=true DRY_RUN=true ./deploy/deploy_qual.sh android
```

**With Tests**:
```bash
cd /Users/adamstack/SmilePile
./deploy/deploy_qual.sh android
```

**Skip Commit** (build and deploy only):
```bash
cd /Users/adamstack/SmilePile
SKIP_TESTS=true SKIP_COMMIT=true ./deploy/deploy_qual.sh android
```

### Verification Commands After Deployment

```bash
# Check app installed
adb shell pm list packages | grep smilepile.qual

# Check app version
adb shell dumpsys package com.smilepile.qual | grep versionName

# Launch app manually
adb shell am start -n com.smilepile.qual/com.smilepile.MainActivity

# Check app logs
adb logcat | grep SmilePile
```

---

## Testing Procedures

### Test Execution Matrix

| Test Suite | Variant | Expected buildType | Command |
|------------|---------|-------------------|---------|
| BuildConfigTest | qualDebug | qual | `./gradlew testQualDebugUnitTest --tests BuildConfigTest` |
| BuildConfigTest | stageRelease | stage | `./gradlew testStageReleaseUnitTest --tests BuildConfigTest` |
| BuildConfigTest | betaRelease | beta | `./gradlew testBetaReleaseUnitTest --tests BuildConfigTest` |
| BuildConfigTest | prodRelease | prod | `./gradlew testProdReleaseUnitTest --tests BuildConfigTest` |
| Tier 1 Tests | qualDebug | qual | `./gradlew app:testQualDebugTier1Critical` |
| Tier 2 Tests | qualDebug | qual | `./gradlew app:testQualDebugTier2Important` |
| Tier 3 Tests | qualDebug | qual | `./gradlew app:testQualDebugTier3UI` |

### Integration with Existing Tier Tests

**Current Command** (before Wave 3):
```bash
./gradlew app:testTier1Critical
```

**New Command** (after Wave 3):
```bash
./gradlew app:testQualDebugTier1Critical
```

**Note**: Gradle now requires flavor to be specified since multiple flavors exist

### Test Coverage Report

**Generate Coverage for qualDebug**:
```bash
cd /Users/adamstack/SmilePile/android
./gradlew testQualDebugUnitTest jacocoQualDebugTestReport
```

**View Report**:
```bash
open app/build/reports/jacoco/jacocoQualDebugTestReport/html/index.html
```

---

## Rollback Plan

### If Implementation Fails

**Severity**: LOW - Changes are isolated and non-breaking

**Rollback Steps**:

#### Step 1: Revert Git Changes

```bash
cd /Users/adamstack/SmilePile
git status
git checkout -- android/app/build.gradle.kts
git checkout -- android/app/proguard-rules.pro
git checkout -- deploy/deploy_qual.sh
```

#### Step 2: Remove New Files

```bash
# Remove BuildConfig module
rm -rf android/app/src/main/java/com/smilepile/config/

# Remove flavor resources
rm -rf android/app/src/qual/
rm -rf android/app/src/stage/
rm -rf android/app/src/beta/
rm -rf android/app/src/prod/

# Remove test file
rm android/app/src/test/java/com/smilepile/config/BuildConfigTest.kt
```

#### Step 3: Remove keystore.properties (if reverting completely)

```bash
rm android/keystore.properties
```

**IMPORTANT**: Keep keystore file itself for future use:
```bash
# DO NOT DELETE:
# ~/keystores/smilepile-production.keystore
```

#### Step 4: Gradle Clean

```bash
cd /Users/adamstack/SmilePile/android
./gradlew clean
./gradlew assembleDebug
```

**Expected**: Original build works as before Wave 3

#### Step 5: Test Original Deployment

```bash
cd /Users/adamstack/SmilePile
./deploy/deploy_qual.sh android
```

**Expected**: Original deployment flow works

### Partial Rollback

**If only specific components fail**:

**Remove flavors, keep signing**:
- Revert build.gradle.kts productFlavors block only
- Keep signingConfigs (useful for future)

**Remove BuildConfig module only**:
- Delete `config/BuildConfig.kt`
- Keep flavors and resources

**Remove flavor resources only**:
- Delete flavor directories
- Keep BuildConfig module and flavors

### Prevention of Data Loss

**Before Implementation**:
```bash
cd /Users/adamstack/SmilePile
git stash save "Pre-Wave-3-Backup"
```

**To Restore**:
```bash
git stash pop
```

---

## Time Estimates

### Detailed Time Breakdown

| Phase | Task | Estimated Time | Dependencies |
|-------|------|---------------|--------------|
| 1 | Generate production keystore | 15 min | None |
| 2 | Create keystore.properties | 5 min | Phase 1 |
| 3 | Add product flavors to build.gradle.kts | 20 min | None |
| 4 | Configure signing configs | 15 min | Phase 2, 3 |
| 5 | Create BuildConfig.kt module | 15 min | Phase 3 |
| 6 | Create flavor-specific resources | 20 min | Phase 3 |
| 7 | Update ProGuard rules | 10 min | Phase 3 |
| 8 | Build verification (all flavors) | 30 min | Phase 3-7 |
| 9 | Update deployment script | 15 min | Phase 8 |
| 10 | Create tests | 30 min | Phase 5 |
| 11 | Final verification | 30 min | All phases |
| 12 | Documentation | 30 min | Phase 11 |
| **TOTAL** | **Core Implementation** | **3.75 hours** | |
| | **With Buffer (25%)** | **4.75 hours** | |

### Critical Path

**Longest Sequence**:
1. Generate keystore (15 min)
2. Create keystore.properties (5 min)
3. Add product flavors (20 min)
4. Configure signing (15 min)
5. Build verification (30 min)
6. Final verification (30 min)

**Critical Path Total**: 1.9 hours

**Parallel Tasks**:
- BuildConfig.kt (can be done while Gradle syncs)
- Flavor resources (can be done in parallel with BuildConfig.kt)
- ProGuard rules (can be done anytime after flavors added)
- Tests (can be written while builds are running)
- Documentation (can be written throughout)

### Factors Affecting Time

**Faster (Optimistic: 3 hours)**:
- Experienced with Gradle configuration
- No build errors
- Keystore generation straightforward
- Tests pass immediately

**Slower (Pessimistic: 6 hours)**:
- First time with product flavors
- Gradle sync issues
- Build errors requiring debugging
- Signing configuration problems
- Test failures requiring fixes

### Recommended Schedule

**Session 1 (2 hours)**:
- Phases 1-5 (keystore, flavors, signing, BuildConfig)
- Includes Gradle syncs and initial build verification

**Session 2 (2 hours)**:
- Phases 6-9 (resources, ProGuard, build verification, deployment script)
- Full testing of all variants

**Session 3 (1 hour)**:
- Phases 10-12 (tests, final verification, documentation)

**Total**: 5 hours over 1 day (or 2-3 shorter sessions)

---

## Success Criteria

### Implementation Complete When:

- [ ] Production keystore generated and backed up
- [ ] keystore.properties created (NOT committed to git)
- [ ] Product flavors configured (qual, stage, beta, prod)
- [ ] Signing configs implemented for all tiers
- [ ] BuildConfig.kt module created and working
- [ ] Flavor-specific resources created (app names)
- [ ] ProGuard rules protect BuildConfig fields
- [ ] All 4 primary variants build successfully
- [ ] BUILD_TYPE_ENV correctly detected at runtime
- [ ] Package names match requirements (com.smilepile.qual for QUAL)
- [ ] App display names show correctly per tier
- [ ] QUAL can be installed alongside STAGE/BETA/PROD
- [ ] Deployment script updated and tested
- [ ] BuildConfigTest validates tier detection
- [ ] All existing tier tests pass for qualDebug
- [ ] Release builds succeed with ProGuard enabled
- [ ] No build warnings related to flavors
- [ ] Documentation complete in wave-evidence/wave-3/
- [ ] Changes committed with descriptive message

### Key Metrics

**Build Success Rate**: 100% (4/4 primary variants build without errors)
**Test Pass Rate**: 100% (including new BuildConfigTest)
**Runtime Detection**: BUILD_TYPE_ENV correctly detected for all tiers
**Zero Regression**: All existing functionality works as before
**Security**: keystore.properties NOT in version control

---

## Next Steps After Implementation

### Wave 4: Server-Side Tier Validation

Implement tier verification on backend:
- API endpoints validate tier from client requests
- Tier-specific rate limiting
- Tier-specific feature flags

### Wave 5: Tier-Specific Features

Enable tier-specific behavior:
- Debug menus in QUAL/STAGE
- Analytics sampling rates per tier
- API endpoint configuration per tier
- Tier-specific logging levels

### Wave 6: Play Store Automation

Integrate with fastlane/Gradle Play Publisher:
- Automated STAGE builds to Internal Testing track
- Automated BETA builds to Closed Testing track
- Automated PROD builds to Production track
- Release notes generation per tier

---

## References

### Documentation
- Research Findings: `/Users/adamstack/SmilePile/wave-evidence/wave-3/01-research-findings.md`
- Story: `/Users/adamstack/SmilePile/backlog/sprint-6/STORY-6.3-android-tier-config.md`
- iOS Implementation (Wave 2): `/Users/adamstack/SmilePile/wave-evidence/wave-2/02-implementation-plan.md`

### Android/Gradle Resources
- Product Flavors: https://developer.android.com/build/build-variants
- Build Configuration: https://developer.android.com/build/configure
- Signing Your App: https://developer.android.com/studio/publish/app-signing
- ProGuard/R8: https://developer.android.com/build/shrink-code

### SmilePile Conventions
- Version System: YY.MM.DD.### (date-based with sequence)
- Build Scripts: `/Users/adamstack/SmilePile/deploy/`
- Android Source: `/Users/adamstack/SmilePile/android/app/src/`

---

**Plan Created**: 2025-10-14
**Created By**: Developer Agent
**Wave**: 3 of 10
**Story**: STORY-6.3-android-tier-config.md
**Status**: Ready for Security Review (Phase 4)
