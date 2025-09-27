# SonarCloud Setup for SmilePile

## Overview

SmilePile uses **SonarCloud** for continuous code quality inspection. As a **public repository**, we get **unlimited free analysis** with no LOC restrictions!

## Quick Start

### 1. Get Your Token

If you don't already have a SonarCloud token from your other projects:

1. Go to https://sonarcloud.io
2. Sign in with GitHub
3. Click your avatar → **My Account** → **Security**
4. Generate a new token
5. Save it securely

### 2. Set Token

```bash
# Set for current session
export SONAR_TOKEN='your-token-here'

# Or save permanently
echo 'export SONAR_TOKEN="your-token-here"' >> ~/.zshrc

# Or create env file (shared with Manylla/StackMap)
echo 'SONAR_TOKEN="your-token-here"' > ~/.manylla-env
```

### 3. Run Analysis

```bash
# Make script executable (first time only)
chmod +x scripts/sonar-analysis.sh

# Run full analysis
./scripts/sonar-analysis.sh
```

That's it! Run as often as you want - public repos have no limits.

## What Gets Analyzed

### Android
- **Languages**: Java, Kotlin
- **Source**: `android/app/src/main`
- **Tests**: JUnit tests with JaCoCo coverage
- **Lint**: Android Lint reports

### iOS
- **Languages**: Swift, Objective-C
- **Source**: `ios/SmilePile`
- **Tests**: XCTest with coverage
- **Lint**: SwiftLint reports

## Viewing Results

After analysis, check your metrics at:

- **Dashboard**: https://sonarcloud.io/project/overview?id=ajstack22_SmilePile
- **Issues**: https://sonarcloud.io/project/issues?id=ajstack22_SmilePile
- **Security**: https://sonarcloud.io/project/security_hotspots?id=ajstack22_SmilePile
- **Coverage**: https://sonarcloud.io/component_measures?id=ajstack22_SmilePile&metric=coverage

### Quality Gate

The default quality gate checks:
- Coverage ≥ 80%
- Duplicated Lines < 3%
- Maintainability Rating: A
- Reliability Rating: A
- Security Rating: A

## Best Practices

### When to Run

Since it's free for public repos:
- ✅ Before every deployment
- ✅ After major changes
- ✅ During code reviews
- ✅ Weekly for metrics tracking
- ✅ Whenever you want!

### Fixing Issues

1. Check the **Issues** tab in SonarCloud
2. Filter by severity (Blocker → Info)
3. Each issue shows:
   - Why it's a problem
   - How to fix it
   - Code examples

### Improving Coverage

```bash
# Android: Run tests with coverage
cd android && ./gradlew jacocoTestReport

# iOS: Enable coverage in Xcode
# Product → Scheme → Edit Scheme → Test → Coverage
```

## Configuration

### Project Settings

The `sonar-project.properties` file configures:
- Organization: `ajstack22`
- Project Key: `ajstack22_SmilePile`
- Sources: Android and iOS code
- Exclusions: Generated files, tests, dependencies

### Customizing Analysis

Edit `sonar-project.properties` to:
```properties
# Exclude specific files
sonar.exclusions=**/Generated/**,**/Pods/**

# Focus on specific paths
sonar.inclusions=**/critical/**

# Adjust coverage exclusions
sonar.coverage.exclusions=**/*View.swift,**/*Entity.swift
```

## CI/CD Integration

### Option 1: In Deployment Scripts

Add to your deployment scripts:
```bash
# In deploy_qual.sh or deploy_prod.sh
echo "Running code analysis..."
./scripts/sonar-analysis.sh
```

### Option 2: GitHub Actions (Optional)

Since it's unlimited, you could run on every push:
```yaml
name: SonarCloud
on:
  push:
    branches: [main, develop]
  pull_request:

jobs:
  sonarcloud:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: SonarCloud Scan
        uses: SonarSource/sonarcloud-github-action@master
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

## Troubleshooting

### Token Not Found
```bash
# Check current token
echo $SONAR_TOKEN

# The script checks these locations:
# 1. $SONAR_TOKEN environment variable
# 2. ~/.manylla-env file
# 3. ~/.smilepile-env file
```

### Build Failures
The analysis continues even if builds fail. To fix:
```bash
# Android
cd android && ./gradlew build

# iOS
cd ios && xcodebuild -project SmilePile.xcodeproj build
```

### No Coverage Showing
```bash
# Ensure tests are running
cd android && ./gradlew test
cd ios && xcodebuild test -scheme SmilePile

# Check coverage is enabled in both platforms
```

## Benefits of Public Repo

✅ **Unlimited analysis** - No LOC restrictions
✅ **All features free** - PR decoration, branch analysis
✅ **No quota management** - Analyze as often as needed
✅ **Full history** - Keep all analysis results
✅ **Public badges** - Show quality metrics in README

## Adding Badges to README

Add these to your README.md:
```markdown
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ajstack22_SmilePile&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ajstack22_SmilePile)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=ajstack22_SmilePile&metric=coverage)](https://sonarcloud.io/summary/new_code?id=ajstack22_SmilePile)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=ajstack22_SmilePile&metric=bugs)](https://sonarcloud.io/summary/new_code?id=ajstack22_SmilePile)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=ajstack22_SmilePile&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=ajstack22_SmilePile)
```

## Next Steps

1. Run your first analysis:
   ```bash
   ./scripts/sonar-analysis.sh
   ```

2. Review the results at [SonarCloud](https://sonarcloud.io/project/overview?id=ajstack22_SmilePile)

3. Fix any critical issues

4. Add analysis to your workflow (deployment scripts, CI/CD, etc.)

Enjoy unlimited code quality analysis! 🎉