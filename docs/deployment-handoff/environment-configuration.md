# Environment Configuration Guide

Complete guide to BUILD_TYPE_ENV implementation, API endpoint routing, and runtime environment detection.

## Overview

This guide covers how to compile BUILD_TYPE_ENV into each build, expose it to JavaScript, and use it for API endpoint routing. The goal: each build knows which API to use, with no runtime switching or configuration.

## Core Concept: Compiled Build Type

**Key principle**: BUILD_TYPE_ENV is determined at build time, not runtime. Each build is compiled for a specific tier and cannot be changed after compilation.

```
QUAL build    → BUILD_TYPE_ENV = "qual"    → https://[YOUR_DOMAIN]/qual/api
STAGE build   → BUILD_TYPE_ENV = "stage"   → https://[YOUR_DOMAIN]/stage/api
BETA build    → BUILD_TYPE_ENV = "beta"    → https://[YOUR_DOMAIN]/beta/api
PROD build    → BUILD_TYPE_ENV = "prod"    → https://[YOUR_DOMAIN]/api
```

**Why?** Security and clarity. Users can't accidentally switch environments. Support can identify environment from build alone.

## iOS Implementation

### Step 1: Configure xcconfig Files

BUILD_TYPE_ENV is set in each xcconfig file and compiled into Info.plist.

**ios/Qual.xcconfig**:
```xcconfig
BUILD_TYPE_ENV = qual
```

**ios/Stage.xcconfig**:
```xcconfig
BUILD_TYPE_ENV = stage
```

**ios/Beta.xcconfig**:
```xcconfig
BUILD_TYPE_ENV = beta
```

**ios/Prod.xcconfig**:
```xcconfig
BUILD_TYPE_ENV = prod
```

### Step 2: Add to Info.plist

Edit `ios/[YOUR_APP]/Info.plist` to include BUILD_TYPE_ENV:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <!-- ... existing keys -->

    <!-- Build type for environment detection -->
    <key>BUILD_TYPE_ENV</key>
    <string>$(BUILD_TYPE_ENV)</string>

    <!-- ... rest of keys -->
</dict>
</plist>
```

The `$(BUILD_TYPE_ENV)` variable is replaced at build time with the value from xcconfig.

### Step 3: Create Native Module

Create `ios/[YOUR_APP]/BuildConfigModule.swift`:

```swift
import Foundation

@objc(BuildConfigModule)
class BuildConfigModule: NSObject {

  @objc
  static func requiresMainQueueSetup() -> Bool {
    return false
  }

  @objc
  func constantsToExport() -> [AnyHashable : Any]! {
    // Read BUILD_TYPE_ENV from Info.plist
    let buildType = Bundle.main.object(forInfoDictionaryKey: "BUILD_TYPE_ENV") as? String ?? "unknown"

    // Also expose other build info if needed
    let bundleId = Bundle.main.bundleIdentifier ?? "unknown"
    let appVersion = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "unknown"
    let buildNumber = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "unknown"

    return [
      "BUILD_TYPE_ENV": buildType,
      "BUNDLE_ID": bundleId,
      "APP_VERSION": appVersion,
      "BUILD_NUMBER": buildNumber
    ]
  }
}
```

**Bridging Header** (if needed for Objective-C/Swift interop):

Create `ios/[YOUR_APP]/[YOUR_APP]-Bridging-Header.h`:

```objc
#import <React/RCTBridgeModule.h>
```

Update Xcode project settings:
- Build Settings → Objective-C Bridging Header → `[YOUR_APP]/[YOUR_APP]-Bridging-Header.h`

### Step 4: Verify iOS Implementation

Build each tier and verify BUILD_TYPE_ENV:

```bash
# Build QUAL
cd ios
xcodebuild -workspace [YOUR_APP].xcworkspace \
  -scheme "[YOUR_APP] Qual" \
  -configuration Debug \
  -sdk iphonesimulator

# Verify Info.plist in built app
plutil -p build/Build/Products/Debug-iphonesimulator/[YOUR_APP].app/Info.plist | grep BUILD_TYPE_ENV
# Expected: "BUILD_TYPE_ENV" => "qual"
```

Repeat for STAGE, BETA, PROD with respective schemes.

## Android Implementation

### Step 1: Configure build.gradle Product Flavors

BUILD_TYPE_ENV is set in each product flavor using `buildConfigField`.

Edit `android/app/build.gradle`:

```gradle
android {
    // ... existing config

    flavorDimensions "tier"

    productFlavors {
        qual {
            dimension "tier"
            applicationId "com.[YOUR_COMPANY].[YOUR_APP].qual"
            buildConfigField "String", "BUILD_TYPE_ENV", '"qual"'
            buildConfigField "String", "API_ENDPOINT", '"https://[YOUR_DOMAIN]/qual/api"'
            resValue "string", "app_name", "[YOUR_APP] Qual"
        }

        stage {
            dimension "tier"
            applicationId "com.[YOUR_COMPANY].[YOUR_APP]"
            buildConfigField "String", "BUILD_TYPE_ENV", '"stage"'
            buildConfigField "String", "API_ENDPOINT", '"https://[YOUR_DOMAIN]/stage/api"'
            resValue "string", "app_name", "[YOUR_APP] Stage"
        }

        beta {
            dimension "tier"
            applicationId "com.[YOUR_COMPANY].[YOUR_APP]"
            buildConfigField "String", "BUILD_TYPE_ENV", '"beta"'
            buildConfigField "String", "API_ENDPOINT", '"https://[YOUR_DOMAIN]/beta/api"'
            resValue "string", "app_name", "[YOUR_APP] Beta"
        }

        prod {
            dimension "tier"
            applicationId "com.[YOUR_COMPANY].[YOUR_APP]"
            buildConfigField "String", "BUILD_TYPE_ENV", '"prod"'
            buildConfigField "String", "API_ENDPOINT", '"https://[YOUR_DOMAIN]/api"'
            resValue "string", "app_name", "[YOUR_APP]"
        }
    }
}
```

**Key Points:**
- `buildConfigField` creates a compile-time constant in `BuildConfig.java` (auto-generated)
- Double quotes are escaped: `'"qual"'` becomes `"qual"` in Java/Kotlin
- Accessible via `BuildConfig.BUILD_TYPE_ENV` in Java/Kotlin

### Step 2: Create Native Module

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

        // Expose BUILD_TYPE_ENV from BuildConfig (auto-generated by gradle)
        constants["BUILD_TYPE_ENV"] = BuildConfig.BUILD_TYPE_ENV

        // Also expose other build info if needed
        constants["PACKAGE_NAME"] = BuildConfig.APPLICATION_ID
        constants["APP_VERSION"] = BuildConfig.VERSION_NAME
        constants["BUILD_NUMBER"] = BuildConfig.VERSION_CODE.toString()
        constants["API_ENDPOINT"] = BuildConfig.API_ENDPOINT

        return constants
    }

    companion object {
        const val NAME = "BuildConfigModule"
    }
}
```

### Step 3: Register Module

Create `android/app/src/main/java/com/[YOUR_COMPANY]/[YOUR_APP]/BuildConfigPackage.kt`:

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

Register in `MainApplication.kt`:

```kotlin
override fun getPackages(): List<ReactPackage> {
    return PackageList(this).packages.apply {
        // Add BuildConfigPackage
        add(BuildConfigPackage())
    }
}
```

### Step 4: Verify Android Implementation

Build each flavor and verify BUILD_TYPE_ENV:

```bash
cd android

# Build QUAL
./gradlew assembleQualRelease

# Decompile and check BuildConfig
# (BuildConfig.java is auto-generated in build/generated/source/buildConfig/)
cat app/build/generated/source/buildConfig/qual/release/com/[YOUR_COMPANY]/[YOUR_APP]/BuildConfig.java | grep BUILD_TYPE_ENV
# Expected: public static final String BUILD_TYPE_ENV = "qual";
```

Repeat for stage, beta, prod flavors.

## JavaScript/TypeScript Implementation

### Step 1: Create buildConfig Module

Create `src/config/buildConfig.ts` (or `.js`):

```typescript
import { NativeModules, Platform } from 'react-native';

interface BuildConfigModule {
  BUILD_TYPE_ENV: string;
  BUNDLE_ID?: string;      // iOS
  PACKAGE_NAME?: string;   // Android
  APP_VERSION: string;
  BUILD_NUMBER: string;
  API_ENDPOINT?: string;   // Android only (optional)
}

// Access native module
const nativeBuildConfig = NativeModules.BuildConfigModule as BuildConfigModule;

if (!nativeBuildConfig) {
  console.error('BuildConfigModule not found! Did you rebuild the app?');
}

// Export build type with type safety
export type BuildType = 'qual' | 'stage' | 'beta' | 'prod' | 'unknown';

export const BUILD_TYPE: BuildType = (nativeBuildConfig?.BUILD_TYPE_ENV || 'unknown') as BuildType;

// Export other build info
export const BUNDLE_ID = nativeBuildConfig?.BUNDLE_ID || nativeBuildConfig?.PACKAGE_NAME || 'unknown';
export const APP_VERSION = nativeBuildConfig?.APP_VERSION || 'unknown';
export const BUILD_NUMBER = nativeBuildConfig?.BUILD_NUMBER || 'unknown';

// Determine API endpoint based on BUILD_TYPE
export function getApiEndpoint(): string {
  switch (BUILD_TYPE) {
    case 'qual':
      return 'https://[YOUR_DOMAIN]/qual/api';
    case 'stage':
      return 'https://[YOUR_DOMAIN]/stage/api';
    case 'beta':
      return 'https://[YOUR_DOMAIN]/beta/api';
    case 'prod':
      return 'https://[YOUR_DOMAIN]/api';
    default:
      console.warn(`Unknown BUILD_TYPE: ${BUILD_TYPE}, defaulting to QUAL`);
      return 'https://[YOUR_DOMAIN]/qual/api';
  }
}

// Export API endpoint
export const API_ENDPOINT = getApiEndpoint();

// Helper to check if running in specific tier
export const isQual = BUILD_TYPE === 'qual';
export const isStage = BUILD_TYPE === 'stage';
export const isBeta = BUILD_TYPE === 'beta';
export const isProd = BUILD_TYPE === 'prod';

// Log build info on app start (remove in production)
if (__DEV__ || !isProd) {
  console.log('=== Build Configuration ===');
  console.log('BUILD_TYPE:', BUILD_TYPE);
  console.log('API_ENDPOINT:', API_ENDPOINT);
  console.log('BUNDLE_ID:', BUNDLE_ID);
  console.log('APP_VERSION:', APP_VERSION);
  console.log('BUILD_NUMBER:', BUILD_NUMBER);
  console.log('===========================');
}
```

### Step 2: Use in API Client

Create `src/services/apiClient.ts`:

```typescript
import { API_ENDPOINT, BUILD_TYPE } from '../config/buildConfig';

class ApiClient {
  private baseUrl: string;

  constructor() {
    this.baseUrl = API_ENDPOINT;
    console.log(`ApiClient initialized for ${BUILD_TYPE}: ${this.baseUrl}`);
  }

  async get(path: string): Promise<any> {
    const url = `${this.baseUrl}${path}`;
    console.log(`GET ${url}`);

    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    return response.json();
  }

  async post(path: string, data: any): Promise<any> {
    const url = `${this.baseUrl}${path}`;
    console.log(`POST ${url}`);

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    return response.json();
  }

  // ... other HTTP methods
}

export default new ApiClient();
```

### Step 3: Use Throughout App

```typescript
// App.tsx
import { BUILD_TYPE, isQual, isBeta } from './config/buildConfig';
import apiClient from './services/apiClient';

function App() {
  useEffect(() => {
    // Fetch data from appropriate API
    apiClient.get('/users/me')
      .then(user => console.log('Logged in user:', user))
      .catch(err => console.error('API error:', err));
  }, []);

  return (
    <View>
      <Text>Welcome to {BUILD_TYPE} environment!</Text>

      {/* Show debug info in non-prod builds */}
      {!isProd && (
        <View style={{ backgroundColor: 'yellow', padding: 10 }}>
          <Text>DEBUG: Running {BUILD_TYPE} build</Text>
        </View>
      )}

      {/* ... rest of app */}
    </View>
  );
}
```

## Web Implementation

For React Native Web or web-only deployments, BUILD_TYPE_ENV comes from environment variables.

### Step 1: Configure Environment Variables

Create `.env` files (add to .gitignore!):

**.env.qual**:
```
REACT_APP_BUILD_TYPE=qual
REACT_APP_API_ENDPOINT=https://[YOUR_DOMAIN]/qual/api
```

**.env.stage**:
```
REACT_APP_BUILD_TYPE=stage
REACT_APP_API_ENDPOINT=https://[YOUR_DOMAIN]/stage/api
```

**.env.beta**:
```
REACT_APP_BUILD_TYPE=beta
REACT_APP_API_ENDPOINT=https://[YOUR_DOMAIN]/beta/api
```

**.env.prod**:
```
REACT_APP_BUILD_TYPE=prod
REACT_APP_API_ENDPOINT=https://[YOUR_DOMAIN]/api
```

### Step 2: Configure Build Scripts

Update `package.json`:

```json
{
  "scripts": {
    "web:qual": "REACT_APP_ENV=qual react-scripts start",
    "web:build:qual": "REACT_APP_ENV=qual react-scripts build",
    "web:build:stage": "REACT_APP_ENV=stage react-scripts build",
    "web:build:beta": "REACT_APP_ENV=beta react-scripts build",
    "web:build:prod": "REACT_APP_ENV=prod react-scripts build"
  }
}
```

### Step 3: Access in JavaScript

```typescript
// src/config/buildConfig.ts (web version)
export const BUILD_TYPE = process.env.REACT_APP_BUILD_TYPE || 'qual';
export const API_ENDPOINT = process.env.REACT_APP_API_ENDPOINT || 'https://[YOUR_DOMAIN]/qual/api';

// Same exports as mobile version for consistency
export const isQual = BUILD_TYPE === 'qual';
export const isStage = BUILD_TYPE === 'stage';
export const isBeta = BUILD_TYPE === 'beta';
export const isProd = BUILD_TYPE === 'prod';
```

### Step 4: Platform-Agnostic buildConfig

Combine mobile and web into single module:

```typescript
// src/config/buildConfig.ts
import { Platform, NativeModules } from 'react-native';

// Mobile: Use native module
const nativeBuildConfig = NativeModules.BuildConfigModule;

// Web: Use environment variables
const webBuildType = process.env.REACT_APP_BUILD_TYPE || 'qual';
const webApiEndpoint = process.env.REACT_APP_API_ENDPOINT || 'https://[YOUR_DOMAIN]/qual/api';

// Determine BUILD_TYPE based on platform
export const BUILD_TYPE: BuildType = Platform.OS === 'web'
  ? webBuildType as BuildType
  : (nativeBuildConfig?.BUILD_TYPE_ENV || 'unknown') as BuildType;

// Determine API_ENDPOINT based on platform
export const API_ENDPOINT = Platform.OS === 'web'
  ? webApiEndpoint
  : getApiEndpoint();  // Calculated from BUILD_TYPE for mobile

// ... rest of exports
```

## Runtime Environment Detection

While BUILD_TYPE_ENV is compiled in, you may want runtime detection for debugging.

### Display Build Info

Create `src/components/BuildInfo.tsx`:

```typescript
import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { BUILD_TYPE, API_ENDPOINT, APP_VERSION, BUILD_NUMBER, isProd } from '../config/buildConfig';

export default function BuildInfo() {
  // Don't show in production
  if (isProd) return null;

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Build Info</Text>
      <Text style={styles.text}>Tier: {BUILD_TYPE.toUpperCase()}</Text>
      <Text style={styles.text}>API: {API_ENDPOINT}</Text>
      <Text style={styles.text}>Version: {APP_VERSION} ({BUILD_NUMBER})</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: 'rgba(255, 255, 0, 0.8)',
    padding: 10,
    borderRadius: 5,
  },
  title: {
    fontWeight: 'bold',
    fontSize: 14,
  },
  text: {
    fontSize: 12,
  },
});
```

Use in app:

```typescript
// App.tsx
import BuildInfo from './components/BuildInfo';

function App() {
  return (
    <View>
      <BuildInfo />
      {/* ... rest of app */}
    </View>
  );
}
```

### Settings Screen Tier Display

```typescript
// Settings.tsx
import { BUILD_TYPE, API_ENDPOINT } from '../config/buildConfig';

function Settings() {
  return (
    <View>
      <Text>App Version: {APP_VERSION}</Text>
      <Text>Build Number: {BUILD_NUMBER}</Text>
      <Text>Environment: {BUILD_TYPE.toUpperCase()}</Text>
      <Text>API: {API_ENDPOINT}</Text>
    </View>
  );
}
```

## Testing Environment Detection

Verify BUILD_TYPE_ENV is correctly detected:

```typescript
// __tests__/buildConfig.test.ts
import { BUILD_TYPE, API_ENDPOINT } from '../config/buildConfig';

describe('Build Configuration', () => {
  it('has valid BUILD_TYPE', () => {
    expect(['qual', 'stage', 'beta', 'prod']).toContain(BUILD_TYPE);
  });

  it('has API_ENDPOINT matching BUILD_TYPE', () => {
    switch (BUILD_TYPE) {
      case 'qual':
        expect(API_ENDPOINT).toContain('/qual/api');
        break;
      case 'stage':
        expect(API_ENDPOINT).toContain('/stage/api');
        break;
      case 'beta':
        expect(API_ENDPOINT).toContain('/beta/api');
        break;
      case 'prod':
        expect(API_ENDPOINT).not.toContain('/qual');
        expect(API_ENDPOINT).not.toContain('/stage');
        expect(API_ENDPOINT).not.toContain('/beta');
        break;
    }
  });
});
```

Run tests after each build to verify correct configuration.

## Naming Conventions

Consistent naming across tiers for clarity:

**App Display Names:**
- QUAL: "[YOUR_APP] Qual" (orange badge optional)
- STAGE: "[YOUR_APP] Stage" (blue badge optional)
- BETA: "[YOUR_APP] Beta" (green badge optional)
- PROD: "[YOUR_APP]" (no badge)

**Bundle IDs / Package Names:**
- QUAL: `com.[YOUR_COMPANY].[YOUR_APP].qual`
- STAGE/BETA/PROD: `com.[YOUR_COMPANY].[YOUR_APP]`

**API Endpoints:**
- QUAL: `https://[YOUR_DOMAIN]/qual/api`
- STAGE: `https://[YOUR_DOMAIN]/stage/api`
- BETA: `https://[YOUR_DOMAIN]/beta/api`
- PROD: `https://[YOUR_DOMAIN]/api`

**Version Name Suffixes (Android only):**
- QUAL: `1.0.0-qual`
- STAGE: `1.0.0-stage`
- BETA: `1.0.0-beta`
- PROD: `1.0.0` (no suffix)

## Security Considerations

### 1. No Runtime Switching

BUILD_TYPE_ENV is compiled in and cannot be changed at runtime. This prevents:
- Users accidentally hitting wrong API
- Debug builds in production
- Environment confusion during support

### 2. API Endpoint Validation

Always validate API responses:

```typescript
async function validateApi() {
  try {
    const response = await apiClient.get('/health');
    if (response.environment !== BUILD_TYPE) {
      console.error(`API environment mismatch! Expected ${BUILD_TYPE}, got ${response.environment}`);
    }
  } catch (err) {
    console.error('API health check failed:', err);
  }
}
```

### 3. Prevent Accidental Production API Access

In QUAL/STAGE/BETA builds, add safeguards:

```typescript
if (BUILD_TYPE !== 'prod' && API_ENDPOINT.includes('yourproductiondomain.com')) {
  throw new Error('Non-prod build attempting to access production API! Check configuration.');
}
```

## Troubleshooting

### BUILD_TYPE_ENV is "unknown"

**iOS**: Rebuild app, clean build folder (`Cmd+Shift+K` in Xcode)
**Android**: `./gradlew clean`, then rebuild

### API requests going to wrong endpoint

Verify BUILD_TYPE in console:
```javascript
console.log('BUILD_TYPE:', BUILD_TYPE);
console.log('API_ENDPOINT:', API_ENDPOINT);
```

If wrong, rebuild app with correct scheme/flavor.

### Native module not found

**iOS**: Ensure BuildConfigModule.swift is added to Xcode project target
**Android**: Verify BuildConfigPackage is registered in MainApplication

## Next Steps

After configuring BUILD_TYPE_ENV:

1. Test deployments following [deployment-workflow.md](./deployment-workflow.md)
2. Set up deployment scripts as described
3. Verify each tier hits correct API

## StackMap Reference Files

Complete working implementations:

- `/ios/Qual.xcconfig`, `Stage.xcconfig`, `Beta.xcconfig`, `Prod.xcconfig` (BUILD_TYPE_ENV definitions)
- `/ios/StackMapNative/BuildConfigModule.swift` (iOS native module)
- `/android/app/build.gradle` (buildConfigField definitions)
- `/android/app/src/main/java/com/stackmapnative/BuildConfigModule.kt` (Android native module)
- `/src/config/buildConfig.js` (JavaScript/TypeScript module)
- `/src/services/apiClient.js` (API client using BUILD_TYPE_ENV)

See [reference-implementations.md](./reference-implementations.md) for complete code examples.
