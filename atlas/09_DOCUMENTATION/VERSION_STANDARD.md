# Atlas Version Standardization

## Version Format

### Development Builds
```
YYYY.MM.DD.VVV
```
- **YYYY**: Full year (e.g., 2024)
- **MM**: Month with leading zero (01-12)
- **DD**: Day with leading zero (01-31)
- **VVV**: Build number for that day (001-999)

Example: `2024.03.15.001` (First build on March 15, 2024)

### Production Releases
```
YYYY.MM.VV
```
- **YYYY**: Full year
- **MM**: Month with leading zero
- **VV**: Release version for that month (01-99)

Example: `2024.03.01` (First release in March 2024)

## Version Display Requirements

### Placement: "Immediately Visible Yet Subtle"

#### Mobile Apps (iOS/Android)
```
Settings Screen:
┌─────────────────────────┐
│ Settings                │
│ ─────────────────────  │
│ Profile                 │
│ Notifications           │
│ Privacy                 │
│                        │
│                        │
│ About                  │
│ ─────────────────────  │
│ Version 2024.03.15.001 │  <- Bottom of settings, gray text
└─────────────────────────┘

Login Screen (footer):
┌─────────────────────────┐
│                        │
│     Welcome Back       │
│   [Email field]        │
│   [Password field]     │
│   [Login Button]       │
│                        │
│  v2024.03.15.001       │  <- Small, centered, 50% opacity
└─────────────────────────┘
```

#### Web Applications
```html
<!-- Footer component -->
<footer>
  <div class="version">v2024.03.15.001</div>
</footer>

<!-- CSS -->
.version {
  position: fixed;
  bottom: 10px;
  right: 10px;
  font-size: 10px;
  color: rgba(128, 128, 128, 0.5);
  user-select: all; /* Allow copying */
}

<!-- Also in HTML meta -->
<meta name="version" content="2024.03.15.001">
```

#### Desktop Applications
- **Windows/Mac**: Help → About dialog
- **Status bar**: Bottom right corner
- **Window title**: AppName (v2024.03.15.001) - only in dev builds

## Implementation Files

### Android (build.gradle.kts)
```kotlin
android {
    defaultConfig {
        versionCode = 20240315001
        versionName = "2024.03.15.001"
    }
}
```

### iOS (Info.plist)
```xml
<key>CFBundleShortVersionString</key>
<string>2024.03.15</string>
<key>CFBundleVersion</key>
<string>2024.03.15.001</string>
```

### Web (package.json)
```json
{
  "version": "2024.03.15.001",
  "displayVersion": "2024.03.15.001"
}
```

### Version File (version.json)
Every project should have a `version.json` at the root:
```json
{
  "version": "2024.03.15.001",
  "buildDate": "2024-03-15T14:30:00Z",
  "environment": "development",
  "release": {
    "version": "2024.03.01",
    "date": "2024-03-01T00:00:00Z"
  }
}
```

## Automatic Version Generation

### Script: generate_version.py
```python
#!/usr/bin/env python3
import json
from datetime import datetime
from pathlib import Path

def generate_version(build_number=1, is_release=False):
    now = datetime.now()

    if is_release:
        # Production release: YYYY.MM.VV
        version = f"{now.year}.{now.month:02d}.{build_number:02d}"
    else:
        # Development build: YYYY.MM.DD.VVV
        version = f"{now.year}.{now.month:02d}.{now.day:02d}.{build_number:03d}"

    version_data = {
        "version": version,
        "buildDate": now.isoformat(),
        "environment": "production" if is_release else "development"
    }

    # Save to version.json
    with open("version.json", "w") as f:
        json.dump(version_data, f, indent=2)

    return version

# Usage in build process
version = generate_version(build_number=1, is_release=False)
print(f"Generated version: {version}")
```

## Version Display Components

### React Component
```jsx
// VersionDisplay.jsx
import React from 'react';
import packageJson from '../package.json';

const VersionDisplay = ({ subtle = true }) => {
  const style = subtle ? {
    fontSize: '10px',
    color: 'rgba(128, 128, 128, 0.5)',
    position: 'fixed',
    bottom: '10px',
    right: '10px'
  } : {
    fontSize: '14px',
    color: '#666'
  };

  return (
    <div style={style} className="version-display">
      v{packageJson.version}
    </div>
  );
};
```

### Android Kotlin
```kotlin
// VersionDisplay.kt
class VersionDisplay {
    companion object {
        fun getVersionString(context: Context): String {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return "v${pInfo.versionName}"
        }
    }
}

// In Settings Activity
versionTextView.text = VersionDisplay.getVersionString(this)
versionTextView.alpha = 0.5f  // Subtle
```

### SwiftUI (iOS)
```swift
// VersionView.swift
struct VersionView: View {
    var version: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "Unknown"
    }

    var body: some View {
        Text("v\(version)")
            .font(.caption2)
            .foregroundColor(.gray.opacity(0.5))
    }
}
```

## Version Visibility Rules

### Always Visible
- Settings/About screen
- Error reports (for debugging)
- API responses (in headers)
- Logs (first line of every log file)

### Conditionally Visible
- Dev builds: Always show
- Beta builds: Show with "β" symbol
- Production: Only in settings

### Click/Tap Behavior
- **Single tap**: Copy version to clipboard
- **Long press**: Show build details (date, environment, commit hash)
- **Triple tap** (dev only): Show debug menu

## Version in Error Reporting

All error reports should include:
```json
{
  "error": "...",
  "version": "2024.03.15.001",
  "platform": "android",
  "timestamp": "2024-03-15T14:30:00Z"
}
```

## CI/CD Integration

### GitHub Actions Example
```yaml
- name: Generate Version
  run: |
    BUILD_NUM=$(date +%Y%m%d)$(printf "%03d" $GITHUB_RUN_NUMBER)
    VERSION=$(date +%Y.%m.%d).$(printf "%03d" $GITHUB_RUN_NUMBER)
    echo "VERSION=$VERSION" >> $GITHUB_ENV
    echo "$VERSION" > version.txt
```

## Best Practices

1. **Consistency**: Use same format across all platforms
2. **Automation**: Generate versions in CI/CD, not manually
3. **Visibility**: Subtle but findable when needed
4. **Debugging**: Always include in error reports
5. **User-Friendly**: Don't show internal build numbers to end users

## Atlas Script Integration

Add to `04_release_deployment.py`:
```python
def generate_build_version():
    """Generate version for current build"""
    now = datetime.now()
    build_num = get_daily_build_number()  # Track per day
    return f"{now.year}.{now.month:02d}.{now.day:02d}.{build_num:03d}"

def generate_release_version():
    """Generate version for production release"""
    now = datetime.now()
    release_num = get_monthly_release_number()
    return f"{now.year}.{now.month:02d}.{release_num:02d}"
```

## Summary

- **Dev format**: `YYYY.MM.DD.VVV` (daily builds)
- **Prod format**: `YYYY.MM.VV` (monthly releases)
- **Display**: Subtle, bottom-right or in settings
- **Color**: 50% opacity gray
- **Size**: 10-12px (small but readable)
- **Always in**: Error reports, logs, settings
- **Generated by**: CI/CD pipeline automatically