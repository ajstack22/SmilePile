# Android Setup Guide

Complete Android configuration for 4-tier deployment system (QUAL, STAGE, BETA, PROD).

## Overview

This guide covers Android-specific setup including package names, product flavors, build types, keystores, gradle configuration, and fastlane setup. Budget 4-6 hours for initial setup.

## Package Name Strategy

StackMap's approach (recommended):

- **QUAL**: `com.[YOUR_COMPANY].[YOUR_APP].qual` (unique package)
- **STAGE**: `com.[YOUR_COMPANY].[YOUR_APP]` (base package)
- **BETA**: `com.[YOUR_COMPANY].[YOUR_APP]` (base package)
- **PROD**: `com.[YOUR_COMPANY].[YOUR_APP]` (base package)

**Why?** Play Console differentiates builds by testing tracks (Internal, Closed, Production), not by package name. Using the same package for STAGE/BETA/PROD simplifies distribution and Play Console management.

## Step 1: Keystore Setup

Android requires signing keys for all builds. Create keystores before configuring gradle.

### Generate Production Keystore

This keystore is used for STAGE, BETA, and PROD builds uploaded to Play Console.

```bash
# Navigate to android/app directory
cd android/app

# Generate keystore (valid for 10,000 days)
keytool -genkey -v -keystore [YOUR_APP]-production.keystore \
  -alias [YOUR_APP]-production-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# You'll be prompted for:
# - Keystore password (record securely!)
# - Key password (can be same as keystore password)
# - Name, Organizational Unit, Organization, City, State, Country
```

**CRITICAL**: Backup this keystore in multiple secure locations! If lost, you cannot update your app in Play Store.

### Generate QUAL Keystore (Optional)

For consistency, create a separate keystore for QUAL builds. Alternatively, use the debug keystore.

```bash
# Option 1: Separate QUAL keystore
keytool -genkey -v -keystore [YOUR_APP]-qual.keystore \
  -alias [YOUR_APP]-qual-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Option 2: Use Android debug keystore (default location)
# ~/.android/debug.keystore
# Alias: androiddebugkey
# Password: android
```

### Google Play App Signing (Recommended)

Google Play App Signing protects your production keystore by having Google manage the final signing key.

1. **Enroll in Play App Signing**:
   - Play Console → Your app → Release → Setup → App signing
   - Choose: "Let Google create and manage your app signing key" (recommended)
   - OR upload your existing keystore

2. **Use Upload Keystore**:
   - Generate a separate upload keystore (same process as production keystore)
   - Name: `[YOUR_APP]-upload.keystore`
   - Google re-signs with production key after upload

3. **Benefits**:
   - Google securely stores production key
   - Rotate upload key if compromised
   - Same production signature across all builds

**StackMap Approach**: Uses upload keystore with Google Play App Signing for production security.

### Secure Keystore Storage

```bash
# Move keystores to secure location outside project
mv [YOUR_APP]-production.keystore ~/keystores/

# Or keep in android/app but add to .gitignore
echo "*.keystore" >> .gitignore
echo "keystore.properties" >> .gitignore
```

## Step 2: Configure Gradle Signing

### Create keystore.properties

Create `android/keystore.properties` (add to .gitignore!):

```properties
# Production keystore (STAGE/BETA/PROD)
PROD_STORE_FILE=../keystores/[YOUR_APP]-production.keystore
PROD_STORE_PASSWORD=[YOUR_KEYSTORE_PASSWORD]
PROD_KEY_ALIAS=[YOUR_APP]-production-key
PROD_KEY_PASSWORD=[YOUR_KEY_PASSWORD]

# QUAL keystore (or use debug)
QUAL_STORE_FILE=../keystores/[YOUR_APP]-qual.keystore
QUAL_STORE_PASSWORD=[YOUR_QUAL_PASSWORD]
QUAL_KEY_ALIAS=[YOUR_APP]-qual-key
QUAL_KEY_PASSWORD=[YOUR_QUAL_KEY_PASSWORD]
```

**Alternative**: Use environment variables instead of keystore.properties for better security in CI/CD.

### Load keystore.properties in build.gradle

Edit `android/app/build.gradle`:

```gradle
// Load keystore properties
def keystorePropertiesFile = rootProject.file("keystore.properties")
def keystoreProperties = new Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(new FileInputStream(keystorePropertiesFile))
}

android {
    // ... existing config

    signingConfigs {
        debug {
            // Default debug signing (for development)
            storeFile file('debug.keystore')
            storePassword 'android'
            keyAlias 'androiddebugkey'
            keyPassword 'android'
        }

        qual {
            if (keystorePropertiesFile.exists()) {
                storeFile file(keystoreProperties['QUAL_STORE_FILE'])
                storePassword keystoreProperties['QUAL_STORE_PASSWORD']
                keyAlias keystoreProperties['QUAL_KEY_ALIAS']
                keyPassword keystoreProperties['QUAL_KEY_PASSWORD']
            }
        }

        production {
            if (keystorePropertiesFile.exists()) {
                storeFile file(keystoreProperties['PROD_STORE_FILE'])
                storePassword keystoreProperties['PROD_STORE_PASSWORD']
                keyAlias keystoreProperties['PROD_KEY_ALIAS']
                keyPassword keystoreProperties['PROD_KEY_PASSWORD']
            }
        }
    }

    // ... rest of config
}
```

## Step 3: Configure Product Flavors

Product flavors create different app variants from a single codebase. Each tier is a flavor.

### Edit android/app/build.gradle

Add product flavors configuration:

```gradle
android {
    // ... existing config

    // Flavor dimensions (required)
    flavorDimensions "tier"

    productFlavors {
        qual {
            dimension "tier"
            applicationId "com.[YOUR_COMPANY].[YOUR_APP].qual"
            versionNameSuffix "-qual"
            resValue "string", "app_name", "[YOUR_APP] Qual"
            buildConfigField "String", "BUILD_TYPE_ENV", '"qual"'
            buildConfigField "String", "API_ENDPOINT", '"https://[YOUR_DOMAIN]/qual/api"'
            signingConfig signingConfigs.qual
        }

        stage {
            dimension "tier"
            applicationId "com.[YOUR_COMPANY].[YOUR_APP]"
            versionNameSuffix "-stage"
            resValue "string", "app_name", "[YOUR_APP] Stage"
            buildConfigField "String", "BUILD_TYPE_ENV", '"stage"'
            buildConfigField "String", "API_ENDPOINT", '"https://[YOUR_DOMAIN]/stage/api"'
            signingConfig signingConfigs.production
        }

        beta {
            dimension "tier"
            applicationId "com.[YOUR_COMPANY].[YOUR_APP]"
            versionNameSuffix "-beta"
            resValue "string", "app_name", "[YOUR_APP] Beta"
            buildConfigField "String", "BUILD_TYPE_ENV", '"beta"'
            buildConfigField "String", "API_ENDPOINT", '"https://[YOUR_DOMAIN]/beta/api"'
            signingConfig signingConfigs.production
        }

        prod {
            dimension "tier"
            applicationId "com.[YOUR_COMPANY].[YOUR_APP]"
            // No versionNameSuffix for production
            resValue "string", "app_name", "[YOUR_APP]"
            buildConfigField "String", "BUILD_TYPE_ENV", '"prod"'
            buildConfigField "String", "API_ENDPOINT", '"https://[YOUR_DOMAIN]/api"'
            signingConfig signingConfigs.production
        }
    }

    buildTypes {
        debug {
            // Used for QUAL local development
            signingConfig signingConfigs.debug
            debuggable true
            minifyEnabled false
        }

        release {
            // Used for STAGE/BETA/PROD
            signingConfig signingConfigs.production
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

**Key Points:**
- `buildConfigField`: Creates compile-time constants accessible in Java/Kotlin via `BuildConfig.BUILD_TYPE_ENV`
- `resValue`: Creates string resources for app name (appears on home screen)
- `applicationId`: Package name (QUAL has `.qual` suffix, others use base package)
- `versionNameSuffix`: Distinguishes builds in Play Console (except PROD)
- `signingConfig`: Links flavor to appropriate keystore

### Verify Build Variants

After configuring flavors, Android Studio shows build variants:

```
qualDebug       (QUAL + debug build type)
qualRelease     (QUAL + release build type)
stageDebug      (STAGE + debug build type)
stageRelease    (STAGE + release build type)
betaDebug       (BETA + debug build type)
betaRelease     (BETA + release build type)
prodDebug       (PROD + debug build type)
prodRelease     (PROD + release build type)
```

**Typical usage:**
- `qualDebug`: Local QUAL development
- `qualRelease`: QUAL deployment (simulator/emulator)
- `stageRelease`: STAGE deployment (Play Console Internal Testing)
- `betaRelease`: BETA deployment (Play Console Closed Testing)
- `prodRelease`: PROD deployment (Play Console Production)

## Step 4: Update AndroidManifest.xml

Use flavor-specific app name from `resValue`:

Edit `android/app/src/main/AndroidManifest.xml`:

```xml
<application
    android:name=".MainApplication"
    android:label="@string/app_name"
    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:allowBackup="false"
    android:theme="@style/AppTheme">
    <!-- ... rest of config -->
</application>
```

The `android:label="@string/app_name"` references the flavor-specific name set by `resValue`.

## Step 5: Create Native BuildConfig Module

Expose `BUILD_TYPE_ENV` to JavaScript for runtime API endpoint routing.

### Kotlin Implementation (Recommended)

Create `android/app/src/main/java/com/[YOUR_COMPANY]/[YOUR_APP]/BuildConfigModule.kt`:

```kotlin
package com.[YOUR_COMPANY].[YOUR_APP]

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.module.annotations.ReactModule

@ReactModule(name = BuildConfigModule.NAME)
class BuildConfigModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return NAME
    }

    override fun getConstants(): Map<String, Any> {
        val constants: MutableMap<String, Any> = HashMap()

        // Expose BUILD_TYPE_ENV from gradle buildConfigField
        constants["BUILD_TYPE_ENV"] = BuildConfig.BUILD_TYPE_ENV

        return constants
    }

    companion object {
        const val NAME = "BuildConfigModule"
    }
}
```

### Java Implementation (Alternative)

Create `android/app/src/main/java/com/[YOUR_COMPANY]/[YOUR_APP]/BuildConfigModule.java`:

```java
package com.[YOUR_COMPANY].[YOUR_APP];

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.module.annotations.ReactModule;

import java.util.HashMap;
import java.util.Map;

@ReactModule(name = BuildConfigModule.NAME)
public class BuildConfigModule extends ReactContextBaseJavaModule {
    public static final String NAME = "BuildConfigModule";

    public BuildConfigModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();

        // Expose BUILD_TYPE_ENV from gradle buildConfigField
        constants.put("BUILD_TYPE_ENV", BuildConfig.BUILD_TYPE_ENV);

        return constants;
    }
}
```

### Register Module with React Native

Create package class:

**Kotlin**: `android/app/src/main/java/com/[YOUR_COMPANY]/[YOUR_APP]/BuildConfigPackage.kt`

```kotlin
package com.[YOUR_COMPANY].[YOUR_APP]

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

class BuildConfigPackage : ReactPackage {
    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        return listOf(BuildConfigModule(reactContext))
    }

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return emptyList()
    }
}
```

**Java**: `android/app/src/main/java/com/[YOUR_COMPANY]/[YOUR_APP]/BuildConfigPackage.java`

```java
package com.[YOUR_COMPANY].[YOUR_APP];

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BuildConfigPackage implements ReactPackage {
    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        List<NativeModule> modules = new ArrayList<>();
        modules.add(new BuildConfigModule(reactContext));
        return modules;
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Collections.emptyList();
    }
}
```

### Register Package in MainApplication

Edit `android/app/src/main/java/com/[YOUR_COMPANY]/[YOUR_APP]/MainApplication.kt` (or `.java`):

**Kotlin**:
```kotlin
override fun getPackages(): List<ReactPackage> {
    return PackageList(this).packages.apply {
        // Add your custom package
        add(BuildConfigPackage())
    }
}
```

**Java**:
```java
@Override
protected List<ReactPackage> getPackages() {
    List<ReactPackage> packages = new PackageList(this).getPackages();
    // Add your custom package
    packages.add(new BuildConfigPackage());
    return packages;
}
```

**StackMap Reference**: See `/android/app/src/main/java/com/stackmapnative/BuildConfigModule.kt` and `BuildConfigPackage.kt`

## Step 6: Configure Fastlane

Set up fastlane for automated Android deployments.

### Initialize Fastlane (if not done)

```bash
cd android
fastlane init

# Choose option 4: "Manual setup"
# Follow prompts
```

### Create android/fastlane/Fastfile

See [fastlane-configuration.md](./fastlane-configuration.md) for complete Fastfile setup.

**Key lanes for Android:**

```ruby
lane :qual_android do
  gradle(
    task: "clean assembleQualRelease",
    project_dir: "android/"
  )

  # QUAL is never uploaded - only built for local testing
  puts "QUAL build complete: android/app/build/outputs/apk/qual/release/"
end

lane :stage_android do
  gradle(
    task: "clean bundleStageRelease",
    project_dir: "android/"
  )

  upload_to_play_store(
    track: "internal",
    aab: "android/app/build/outputs/bundle/stageRelease/app-stage-release.aab",
    skip_upload_metadata: true,
    skip_upload_images: true,
    skip_upload_screenshots: true
  )
end

lane :beta_android do
  gradle(
    task: "clean bundleBetaRelease",
    project_dir: "android/"
  )

  upload_to_play_store(
    track: "beta",  # Or your closed testing track name
    aab: "android/app/build/outputs/bundle/betaRelease/app-beta-release.aab",
    skip_upload_metadata: true,
    skip_upload_images: true,
    skip_upload_screenshots: true
  )
end

lane :prod_android do
  gradle(
    task: "clean bundleProdRelease",
    project_dir: "android/"
  )

  upload_to_play_store(
    track: "production",
    aab: "android/app/build/outputs/bundle/prodRelease/app-prod-release.aab",
    skip_upload_metadata: true,
    skip_upload_images: true,
    skip_upload_screenshots: true
  )
end
```

**StackMap Reference**: See `/android/fastlane/Fastfile` for complete implementations

### Configure Play Console Access

1. **Create Service Account**:
   - Play Console → Setup → API access
   - Link Google Cloud project (if not linked)
   - Create new service account
   - Grant "Release Manager" role

2. **Download JSON Key**:
   - Click service account → Actions → Manage keys
   - Add key → Create new key → JSON
   - Download JSON file
   - Store securely: `android/play-store-credentials.json`
   - Add to .gitignore!

3. **Reference in Fastfile**:
   ```ruby
   lane :stage_android do
     upload_to_play_store(
       json_key: "play-store-credentials.json",
       # ... rest of config
     )
   end
   ```

## Step 7: Verify Configuration

### Build Each Flavor Locally

```bash
cd android

# QUAL
./gradlew assembleQualRelease
# Output: android/app/build/outputs/apk/qual/release/app-qual-release.apk

# STAGE
./gradlew bundleStageRelease
# Output: android/app/build/outputs/bundle/stageRelease/app-stage-release.aab

# BETA
./gradlew bundleBetaRelease
# Output: android/app/build/outputs/bundle/betaRelease/app-beta-release.aab

# PROD
./gradlew bundleProdRelease
# Output: android/app/build/outputs/bundle/prodRelease/app-prod-release.aab
```

### Verify BUILD_TYPE_ENV Detection

Add temporary logging to your app:

```javascript
// App.js or index.js
import { NativeModules } from 'react-native';

console.log('BUILD_TYPE_ENV:', NativeModules.BuildConfigModule.BUILD_TYPE_ENV);
```

Build and run each flavor, verify logcat output:
- QUAL: `BUILD_TYPE_ENV: qual`
- STAGE: `BUILD_TYPE_ENV: stage`
- BETA: `BUILD_TYPE_ENV: beta`
- PROD: `BUILD_TYPE_ENV: prod`

### Verify Package Names

```bash
# Extract package name from APK/AAB
aapt dump badging android/app/build/outputs/apk/qual/release/app-qual-release.apk | grep package

# Expected outputs:
# QUAL: package: name='com.[YOUR_COMPANY].[YOUR_APP].qual'
# STAGE/BETA/PROD: package: name='com.[YOUR_COMPANY].[YOUR_APP]'
```

## Step 8: First Deployments

### Deploy QUAL

```bash
cd /path/to/project
./scripts/deploy.sh qual --android

# Verify:
# - Build succeeds
# - APK generated: android/app/build/outputs/apk/qual/release/
# - Install on emulator manually
# - Package name: com.[YOUR_COMPANY].[YOUR_APP].qual
# - App name: [YOUR_APP] Qual
# - BUILD_TYPE_ENV: qual
```

### Deploy STAGE

```bash
./scripts/deploy.sh stage --android

# Verify:
# - Build succeeds
# - AAB uploaded to Play Console
# - Internal Testing track receives build
# - Install via Play Console link on device
# - Package name: com.[YOUR_COMPANY].[YOUR_APP]
# - App name: [YOUR_APP] Stage
# - BUILD_TYPE_ENV: stage
```

### Deploy BETA

```bash
./scripts/deploy.sh beta --android

# Verify:
# - Build succeeds
# - AAB uploaded to Play Console
# - Closed Testing track receives build (no review required)
# - Beta testers can install immediately
# - Package name: com.[YOUR_COMPANY].[YOUR_APP]
# - App name: [YOUR_APP] Beta
# - BUILD_TYPE_ENV: beta
```

### Deploy PROD

```bash
./scripts/deploy.sh prod --android

# Verify:
# - Build succeeds
# - AAB uploaded to Play Console
# - Production track (manual rollout control)
# - Submit for review (1-7 days)
# - Package name: com.[YOUR_COMPANY].[YOUR_APP]
# - App name: [YOUR_APP]
# - BUILD_TYPE_ENV: prod
```

## Common Issues and Solutions

### "Keystore file not found"

**Solution**: Verify path in `keystore.properties` or use absolute paths:
```properties
PROD_STORE_FILE=/Users/[USERNAME]/keystores/[YOUR_APP]-production.keystore
```

### "Could not find or load main class org.gradle.wrapper.GradleWrapperMain"

**Solution**: Regenerate gradle wrapper:
```bash
cd android
gradle wrapper
```

### "Failed to finalize session: INSTALL_FAILED_UPDATE_INCOMPATIBLE"

**Solution**: Different signing keys between installed version and new build. Uninstall first:
```bash
adb uninstall com.[YOUR_COMPANY].[YOUR_APP]
```

### "BUILD_TYPE_ENV undefined in JavaScript"

**Solution**:
1. Verify `buildConfigField` in build.gradle product flavor
2. Clean and rebuild: `./gradlew clean assembleQualRelease`
3. Check BuildConfigModule is registered in MainApplication

### "Upload to Play Console failed: Package name mismatch"

**Solution**: Ensure first upload for an app uses the base package name (no suffix):
- First upload: `com.[YOUR_COMPANY].[YOUR_APP]` (STAGE or PROD)
- Do NOT upload QUAL build (has `.qual` suffix) to Play Console

### Gradle build extremely slow

**Solution**: Increase Gradle memory in `android/gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.configureondemand=true
org.gradle.daemon=true
```

## Android-Specific Considerations

### App Icons

Consider creating distinct icons for QUAL/STAGE/BETA:

1. Create icon sets:
   - `android/app/src/qual/res/mipmap-*/ic_launcher.png` (QUAL icon)
   - `android/app/src/stage/res/mipmap-*/ic_launcher.png` (STAGE icon)
   - `android/app/src/beta/res/mipmap-*/ic_launcher.png` (BETA icon)
   - `android/app/src/prod/res/mipmap-*/ic_launcher.png` (PROD icon, or use main)

2. Gradle automatically selects correct icon based on flavor

### ProGuard/R8 Configuration

If using code obfuscation (minifyEnabled true for release), update `android/app/proguard-rules.pro`:

```proguard
# Keep BuildConfig for runtime access
-keep class com.[YOUR_COMPANY].[YOUR_APP].BuildConfig { *; }

# Keep native modules
-keep class com.[YOUR_COMPANY].[YOUR_APP].BuildConfigModule { *; }
-keep class com.[YOUR_COMPANY].[YOUR_APP].BuildConfigPackage { *; }
```

### APK vs AAB

- **APK**: Android Package (installable file)
  - Use for QUAL (local testing)
  - Larger size (contains all architectures)

- **AAB**: Android App Bundle (upload to Play Console)
  - Use for STAGE/BETA/PROD
  - Play Console generates optimized APKs per device
  - Smaller downloads for users

**Gradle tasks:**
- APK: `assembleQualRelease`
- AAB: `bundleStageRelease`, `bundleBetaRelease`, `bundleProdRelease`

### Version Codes and Version Names

Ensure version codes increment for each upload to Play Console:

```gradle
android {
    defaultConfig {
        versionCode 1
        versionName "1.0.0"
    }

    productFlavors {
        // versionNameSuffix automatically appends to versionName
        qual {
            versionNameSuffix "-qual"  // Results in "1.0.0-qual"
        }
        stage {
            versionNameSuffix "-stage"  // Results in "1.0.0-stage"
        }
        // ... etc
    }
}
```

**Important**: versionCode must be unique and increasing for each upload. Consider automating:
```gradle
versionCode Integer.parseInt(new Date().format("yyMMddHH"))
```

## Next Steps

After completing Android setup:

1. Configure fastlane in [fastlane-configuration.md](./fastlane-configuration.md)
2. Set up build config in [environment-configuration.md](./environment-configuration.md)
3. Test deployments following [deployment-workflow.md](./deployment-workflow.md)
4. Review [secrets-and-credentials.md](./secrets-and-credentials.md) for secure keystore management

## StackMap Reference Files

Complete working examples from StackMap:

- `/android/app/build.gradle` (productFlavors, signingConfigs, buildTypes)
- `/android/app/src/main/java/com/stackmapnative/BuildConfigModule.kt`
- `/android/app/src/main/java/com/stackmapnative/BuildConfigPackage.kt`
- `/android/app/src/main/java/com/stackmapnative/MainApplication.kt`
- `/android/fastlane/Fastfile` (lanes: qual_android, stage_android, beta_android, prod_android)

See [reference-implementations.md](./reference-implementations.md) for detailed code examples.
