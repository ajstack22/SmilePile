# Wave 5 Technical Planning: Fastlane Automation

## Implementation Overview

### Implementation Strategy

This implementation follows a **risk-mitigated, incremental approach**:

1. **iOS First**: Establish iOS Fastlane lanes and test locally before Android
2. **Android Second**: Apply learned patterns to Android implementation
3. **Integration Third**: Integrate Fastlane into existing deploy scripts
4. **CI/CD Fourth**: Extend to GitHub Actions after local validation
5. **End-to-End Testing**: Validate complete workflow across all tiers

**Key Principle**: Preserve all existing quality gates and version management. Fastlane replaces only the build/upload commands, not the testing or validation infrastructure.

### Implementation Order

1. **iOS Fastlane Setup** (60 minutes)
   - Create Appfile and Fastfile
   - Implement qual_ios lane (no credentials needed)
   - Test locally with simulator builds
   - Add stage_ios, beta_ios, prod_ios lanes
   - Test with TestFlight upload

2. **Android Fastlane Setup** (60 minutes)
   - Create Appfile and Fastfile
   - Implement qual_android lane (debug keystore only)
   - Test locally with emulator APK
   - Add stage_android, beta_android, prod_android lanes
   - Test with Play Console upload

3. **Gemfile Creation** (10 minutes)
   - Create Gemfile with fastlane dependency
   - Run bundle install
   - Commit Gemfile and Gemfile.lock

4. **Deploy Script Integration** (90 minutes)
   - Update deploy_qual.sh to use Fastlane
   - Create deploy_stage.sh
   - Create deploy_beta.sh
   - Update deploy_prod.sh
   - Test all scripts end-to-end

5. **Credentials Configuration** (30 minutes)
   - Configure App Store Connect API key
   - Configure Play Console service account
   - Verify production keystore configuration
   - Test credential-dependent lanes

6. **CI/CD Integration** (60 minutes)
   - Create GitHub Actions workflow
   - Configure GitHub Secrets
   - Test automated deployment
   - Verify uploads from CI

7. **End-to-End Testing** (60 minutes)
   - Test all 8 Fastlane lanes
   - Test all 4 deploy scripts
   - Verify version management
   - Test edge cases

**Total Estimated Time**: 6 hours

### Prerequisites Checklist

**Before Starting Implementation**:

- [ ] Fastlane 2.228.0 installed and accessible
- [ ] Ruby 3.3+ installed
- [ ] Bundler installed (`gem install bundler`)
- [ ] iOS Xcode project builds successfully
- [ ] Android Gradle project builds successfully
- [ ] All 4 iOS schemes exist (Qual, Stage, Beta, Prod)
- [ ] All 4 Android flavors exist (qual, stage, beta, prod)
- [ ] deploy_qual.sh runs successfully
- [ ] Tests pass for both platforms
- [ ] Git working directory is clean

**Credentials** (can be configured during implementation):

- [ ] Apple Developer account access
- [ ] App Store Connect access
- [ ] Google Play Console access
- [ ] Team ID: 84W9WSYQQB (confirmed from research)

**Development Environment**:

- [ ] macOS for iOS builds (required)
- [ ] Android SDK configured
- [ ] iOS Simulator available
- [ ] Android Emulator available

## File Creation Plan

### New Files to Create

1. **ios/fastlane/Appfile** - iOS app configuration (Apple ID, team ID)
2. **ios/fastlane/Fastfile** - iOS deployment lanes (qual, stage, beta, prod)
3. **android/fastlane/Appfile** - Android app configuration (package name, service account)
4. **android/fastlane/Fastfile** - Android deployment lanes (qual, stage, beta, prod)
5. **Gemfile** - Ruby dependency management (fastlane version lock)
6. **deploy/deploy_stage.sh** - STAGE tier deployment script
7. **deploy/deploy_beta.sh** - BETA tier deployment script
8. **.github/workflows/deploy-stage.yml** - CI/CD automation (optional)
9. **wave-evidence/wave-5/03-implementation-log.md** - Implementation tracking
10. **wave-evidence/wave-5/04-testing-validation.md** - Testing evidence

### Files to Modify

1. **deploy/deploy_qual.sh**
   - Replace `xcodebuild` command with `bundle exec fastlane qual_ios`
   - Replace `./gradlew assembleQualDebug` with `bundle exec fastlane qual_android`
   - Preserve all quality gates, version management, install logic

2. **deploy/deploy_prod.sh**
   - Add Fastlane integration for prod_ios and prod_android lanes
   - Add safety confirmation prompt
   - Ensure draft/manual submission mode

3. **android/app/keystore.properties** (if needed)
   - Update with production keystore path and credentials
   - Ensure proper permissions (600)

4. **.gitignore** (verify only)
   - Ensure credential files are ignored
   - Ensure build artifacts are ignored

### Files to Reference (No Changes)

These files inform implementation but are not modified:

1. **ios/SmilePile.xcodeproj/project.pbxproj** - iOS project configuration
2. **ios/SmilePile/Info.plist** - iOS app metadata
3. **ios/Qual.xcconfig** - QUAL tier iOS configuration
4. **ios/Stage.xcconfig** - STAGE tier iOS configuration
5. **ios/Beta.xcconfig** - BETA tier iOS configuration
6. **ios/Prod.xcconfig** - PROD tier iOS configuration
7. **android/app/build.gradle.kts** - Android build configuration
8. **deploy/lib/build_number.sh** - Version management (DO NOT MODIFY)
9. **deploy/lib/common.sh** - Common functions
10. **backlog/sprint-6/STORY-6.5-fastlane-automation.md** - Requirements reference

## iOS Fastlane Implementation

### File 1: ios/fastlane/Appfile

**Purpose**: Configure Apple Developer account and app identifiers

**Content Template**:
```ruby
# ios/fastlane/Appfile
# Apple Developer and App Store Connect configuration

app_identifier("com.smilepile")  # Base bundle ID
apple_id("developer@email.com")  # Apple Developer email
team_id("84W9WSYQQB")            # Team ID from Wave 1
itc_team_id("84W9WSYQQB")        # App Store Connect team ID

# Tier-specific bundle IDs will be handled in Fastfile via scheme selection
# Qual:  com.smilepile.qual (automatic from scheme)
# Stage: com.smilepile (base)
# Beta:  com.smilepile (base)
# Prod:  com.smilepile (base)
```

**Configuration Notes**:
- Use base bundle ID `com.smilepile` as default
- QUAL tier uses `com.smilepile.qual` (automatically selected via scheme)
- Apple ID should be replaced with actual developer email
- Team ID confirmed from Wave 1 research: `84W9WSYQQB`
- Same team ID for both development and App Store Connect

**Implementation Steps**:
1. Create ios/fastlane/ directory: `mkdir -p ios/fastlane`
2. Create Appfile with template above
3. Replace `developer@email.com` with actual Apple ID
4. Verify team ID matches project: `grep DEVELOPMENT_TEAM ios/SmilePile.xcodeproj/project.pbxproj`
5. Test syntax: `cd ios && bundle exec fastlane lanes` (should not error)

**Verification**:
```bash
# Verify Appfile exists
ls -la ios/fastlane/Appfile

# Test Fastlane can read it
cd ios && bundle exec fastlane lanes
# Should not show errors about missing configuration
```

### File 2: ios/fastlane/Fastfile

**Purpose**: Define 4 iOS deployment lanes (qual, stage, beta, prod)

**Complete Content**:
```ruby
# ios/fastlane/Fastfile
# SmilePile iOS Deployment Lanes

default_platform(:ios)

platform :ios do

  before_all do |lane|
    puts "========================================="
    puts "🚀 Starting iOS deployment: #{lane}"
    puts "========================================="

    # Version is managed by build_number.sh, not Fastlane
    # DO NOT use increment_build_number or increment_version_number
  end

  # ============================================================================
  # LANE 1: QUAL - Simulator builds for local testing
  # ============================================================================
  desc "Build QUAL for simulator testing"
  lane :qual_ios do
    gym(
      scheme: "SmilePile Qual",
      configuration: "Debug",
      xcconfig: "Qual.xcconfig",
      skip_package_ipa: true,
      skip_codesigning: false,
      sdk: "iphonesimulator",
      destination: "generic/platform=iOS Simulator",
      derived_data_path: "./DerivedData",
      output_directory: "./build/qual",
      clean: true,
      buildlog_path: "./build/logs"
    )

    puts "========================================="
    puts "✅ QUAL build complete!"
    puts "========================================="
    puts "App: DerivedData/Build/Products/Debug-iphonesimulator/SmilePile Qual.app"
    puts ""
    puts "Install to simulator:"
    puts "  xcrun simctl install booted '<path-to-app>'"
    puts ""
    puts "Launch:"
    puts "  xcrun simctl launch booted com.smilepile.qual"
    puts "========================================="
  end

  # ============================================================================
  # LANE 2: STAGE - TestFlight Internal Testing
  # ============================================================================
  desc "Build and upload STAGE to TestFlight Internal Testing"
  lane :stage_ios do
    # Clean build
    clear_derived_data

    # Build IPA
    gym(
      scheme: "SmilePile Stage",
      configuration: "Debug",
      xcconfig: "Stage.xcconfig",
      export_method: "app-store",
      derived_data_path: "./build/stage",
      output_directory: "./build/stage",
      output_name: "SmilePile-Stage",
      clean: true,
      buildlog_path: "./build/logs",
      export_options: {
        provisioningProfiles: {
          "com.smilepile" => "match AppStore com.smilepile"
        }
      }
    )

    # Upload to TestFlight
    pilot(
      skip_waiting_for_build_processing: true,
      distribute_external: false,
      groups: ["Internal Testers"],
      changelog: "Stage build for internal testing",
      notify_external_testers: false,
      app_identifier: "com.smilepile",
      team_id: "84W9WSYQQB"
    )

    puts "========================================="
    puts "✅ STAGE uploaded to TestFlight Internal Testing!"
    puts "========================================="
    puts "View in App Store Connect:"
    puts "  https://appstoreconnect.apple.com"
    puts ""
    puts "TestFlight Section: Internal Testing"
    puts "========================================="
  end

  # ============================================================================
  # LANE 3: BETA - TestFlight External Testing
  # ============================================================================
  desc "Build and upload BETA to TestFlight External Testing"
  lane :beta_ios do
    clear_derived_data

    gym(
      scheme: "SmilePile Beta",
      configuration: "Beta",
      xcconfig: "Beta.xcconfig",
      export_method: "app-store",
      derived_data_path: "./build/beta",
      output_directory: "./build/beta",
      output_name: "SmilePile-Beta",
      clean: true,
      buildlog_path: "./build/logs"
    )

    pilot(
      skip_waiting_for_build_processing: false,
      distribute_external: true,
      groups: ["Beta Testers"],
      changelog: "Beta build for external testing",
      notify_external_testers: true,
      app_identifier: "com.smilepile",
      team_id: "84W9WSYQQB"
    )

    puts "========================================="
    puts "✅ BETA uploaded to TestFlight External Testing!"
    puts "========================================="
    puts "View in App Store Connect:"
    puts "  https://appstoreconnect.apple.com"
    puts ""
    puts "TestFlight Section: External Testing"
    puts "Beta testers will be notified automatically"
    puts "========================================="
  end

  # ============================================================================
  # LANE 4: PROD - App Store
  # ============================================================================
  desc "Build and upload PROD to App Store Connect"
  lane :prod_ios do
    clear_derived_data

    gym(
      scheme: "SmilePile Prod",
      configuration: "Release",
      xcconfig: "Prod.xcconfig",
      export_method: "app-store",
      derived_data_path: "./build/prod",
      output_directory: "./build/prod",
      output_name: "SmilePile-Prod",
      clean: true,
      buildlog_path: "./build/logs"
    )

    # Upload but don't auto-submit
    deliver(
      skip_metadata: true,
      skip_screenshots: true,
      submit_for_review: false,
      force: true,
      app_identifier: "com.smilepile",
      team_id: "84W9WSYQQB"
    )

    puts "========================================="
    puts "✅ PROD uploaded to App Store Connect!"
    puts "========================================="
    puts "⚠️  MANUAL SUBMISSION REQUIRED"
    puts ""
    puts "Next Steps:"
    puts "  1. Log in to App Store Connect"
    puts "  2. Navigate to SmilePile app"
    puts "  3. Review build metadata"
    puts "  4. Submit for App Store review"
    puts ""
    puts "🔗 https://appstoreconnect.apple.com"
    puts "========================================="
  end

  after_all do |lane|
    puts "========================================="
    puts "✅ iOS lane '#{lane}' completed successfully"
    puts "========================================="
  end

  error do |lane, exception|
    puts "========================================="
    puts "❌ iOS lane '#{lane}' failed"
    puts "Error: #{exception.message}"
    puts "========================================="
  end

end
```

**Lane-Specific Notes**:

**Lane 1: qual_ios**
- **Purpose**: Local simulator testing only
- **No credentials needed**: Uses development signing automatically
- **Output**: .app bundle in DerivedData (not IPA)
- **Integration**: Called by deploy_qual.sh
- **Testing**: Can test immediately after Fastfile creation

**Lane 2: stage_ios**
- **Purpose**: Internal TestFlight testing for team
- **Credentials needed**: App Store Connect API key
- **Output**: IPA uploaded to TestFlight Internal
- **Distribution**: Internal testers only (no external approval needed)
- **Testing**: Requires TestFlight access

**Lane 3: beta_ios**
- **Purpose**: External TestFlight testing for beta users
- **Credentials needed**: App Store Connect API key
- **Output**: IPA uploaded to TestFlight External
- **Distribution**: External testers (requires App Store review approval)
- **Testing**: Requires TestFlight access and beta tester group

**Lane 4: prod_ios**
- **Purpose**: Production App Store submission
- **Credentials needed**: App Store Connect API key
- **Output**: IPA uploaded to App Store Connect
- **Distribution**: Manual submission required (safety gate)
- **Testing**: Requires App Store Connect access

**iOS Implementation Steps**:

1. **Create directory structure**:
   ```bash
   mkdir -p ios/fastlane
   mkdir -p ios/build/{qual,stage,beta,prod,logs}
   ```

2. **Create Appfile** (File 1 above)

3. **Create Fastfile** (File 2 above)

4. **Test syntax**:
   ```bash
   cd ios
   bundle exec fastlane lanes
   # Expected output: 4 lanes listed
   ```

5. **Test QUAL lane** (no credentials needed):
   ```bash
   cd ios
   bundle exec fastlane qual_ios
   # Expected: Build succeeds, .app created
   ```

6. **Verify build output**:
   ```bash
   ls -la ios/DerivedData/Build/Products/Debug-iphonesimulator/
   # Should show "SmilePile Qual.app"
   ```

7. **Test STAGE lane** (requires credentials - see Credentials Configuration section):
   ```bash
   cd ios
   bundle exec fastlane stage_ios
   # Expected: IPA uploaded to TestFlight
   ```

8. **Verify in App Store Connect**:
   - Log in to https://appstoreconnect.apple.com
   - Navigate to SmilePile app
   - Check TestFlight > Internal Testing
   - Should see new build

**iOS Edge Cases and Solutions**:

**Edge Case 1: Provisioning Profile Issues**
- **Symptom**: "No matching provisioning profiles found"
- **Solution**: Use automatic signing initially, or set up match
- **Workaround**: Manually download profiles from App Store Connect

**Edge Case 2: Code Signing Identity Issues**
- **Symptom**: "No signing certificate found"
- **Solution**: Fastlane will prompt for credentials
- **Workaround**: Sign in Xcode first, then run Fastlane

**Edge Case 3: Export Options Issues**
- **Symptom**: "Export failed"
- **Solution**: Customize export_options hash in gym call
- **Workaround**: Generate export options plist manually

**Edge Case 4: Build Configuration Name Mismatch**
- **Symptom**: "Configuration 'Debug' not found for Stage scheme"
- **Solution**: Verify configuration names in Xcode project
- **Command**: `xcodebuild -project SmilePile.xcodeproj -list`

## Android Fastlane Implementation

### File 3: android/fastlane/Appfile

**Purpose**: Configure Google Play package name and service account

**Content Template**:
```ruby
# android/fastlane/Appfile
# Google Play Console configuration

json_key_file("~/.fastlane/play-store-credentials.json")  # Service account JSON
package_name("com.smilepile")  # Base package name

# Tier-specific package names handled in Fastfile via flavor selection
# Qual:  com.smilepile.qual (automatic from flavor)
# Stage: com.smilepile (base)
# Beta:  com.smilepile (base)
# Prod:  com.smilepile (base)
```

**Configuration Notes**:
- Service account JSON should be at `~/.fastlane/play-store-credentials.json`
- File permissions must be 600 (read/write owner only)
- Base package name is `com.smilepile`
- QUAL uses `com.smilepile.qual` (automatically selected via flavor)

**Implementation Steps**:
1. Create android/fastlane/ directory: `mkdir -p android/fastlane`
2. Create Appfile with template above
3. Ensure service account JSON exists (see Credentials Configuration section)
4. Set permissions: `chmod 600 ~/.fastlane/play-store-credentials.json`
5. Test syntax: `cd android && bundle exec fastlane lanes`

**Verification**:
```bash
# Verify Appfile exists
ls -la android/fastlane/Appfile

# Verify service account exists
ls -la ~/.fastlane/play-store-credentials.json

# Test Fastlane can read it
cd android && bundle exec fastlane lanes
# Should not show errors about missing JSON key
```

### File 4: android/fastlane/Fastfile

**Purpose**: Define 4 Android deployment lanes (qual, stage, beta, prod)

**Complete Content**:
```ruby
# android/fastlane/Fastfile
# SmilePile Android Deployment Lanes

default_platform(:android)

platform :android do

  before_all do |lane|
    puts "========================================="
    puts "🚀 Starting Android deployment: #{lane}"
    puts "========================================="

    # Version is managed by build_number.sh, not Fastlane
    # DO NOT use increment_version_code or increment_version_name
  end

  # ============================================================================
  # LANE 1: QUAL - APK builds for local emulator testing
  # ============================================================================
  desc "Build QUAL APK for emulator testing"
  lane :qual_android do
    gradle(
      task: "clean assembleQualDebug",
      project_dir: ".",
      properties: {
        "android.injected.signing.store.file" => File.expand_path("~/.android/debug.keystore"),
        "android.injected.signing.store.password" => "android",
        "android.injected.signing.key.alias" => "androiddebugkey",
        "android.injected.signing.key.password" => "android"
      }
    )

    apk_path = lane_context[SharedValues::GRADLE_APK_OUTPUT_PATH]

    puts "========================================="
    puts "✅ QUAL APK built successfully!"
    puts "========================================="
    puts "APK: #{apk_path}"
    puts ""
    puts "Install to emulator:"
    puts "  adb install -r #{apk_path}"
    puts ""
    puts "Launch app:"
    puts "  adb shell monkey -p com.smilepile.qual -c android.intent.category.LAUNCHER 1"
    puts "========================================="
  end

  # ============================================================================
  # LANE 2: STAGE - Play Console Internal Testing
  # ============================================================================
  desc "Build and upload STAGE to Play Console Internal Testing"
  lane :stage_android do
    gradle(
      task: "clean bundleStageRelease",
      project_dir: ".",
      print_command: true,
      print_command_output: true
    )

    upload_to_play_store(
      track: "internal",
      release_status: "completed",
      aab: "app/build/outputs/bundle/stageRelease/app-stage-release.aab",
      skip_upload_apk: true,
      skip_upload_metadata: true,
      skip_upload_changelogs: false,
      skip_upload_images: true,
      skip_upload_screenshots: true,
      validate_only: false
    )

    puts "========================================="
    puts "✅ STAGE uploaded to Play Console Internal Testing!"
    puts "========================================="
    puts "View in Play Console:"
    puts "  https://play.google.com/console"
    puts ""
    puts "Section: Testing > Internal testing"
    puts "========================================="
  end

  # ============================================================================
  # LANE 3: BETA - Play Console Closed Testing
  # ============================================================================
  desc "Build and upload BETA to Play Console Closed Testing"
  lane :beta_android do
    gradle(
      task: "clean bundleBetaRelease",
      project_dir: ".",
      print_command: true
    )

    upload_to_play_store(
      track: "beta",
      release_status: "completed",
      aab: "app/build/outputs/bundle/betaRelease/app-beta-release.aab",
      skip_upload_apk: true,
      skip_upload_metadata: true,
      skip_upload_changelogs: false,
      skip_upload_images: true,
      skip_upload_screenshots: true
    )

    puts "========================================="
    puts "✅ BETA uploaded to Play Console Closed Testing!"
    puts "========================================="
    puts "View in Play Console:"
    puts "  https://play.google.com/console"
    puts ""
    puts "Section: Testing > Closed testing"
    puts "Beta testers can now download the app"
    puts "========================================="
  end

  # ============================================================================
  # LANE 4: PROD - Play Console Production
  # ============================================================================
  desc "Build and upload PROD to Play Console Production"
  lane :prod_android do
    gradle(
      task: "clean bundleProdRelease",
      project_dir: ".",
      print_command: true
    )

    upload_to_play_store(
      track: "production",
      release_status: "draft",
      aab: "app/build/outputs/bundle/prodRelease/app-prod-release.aab",
      skip_upload_apk: true,
      skip_upload_metadata: true,
      skip_upload_changelogs: false,
      skip_upload_images: true,
      skip_upload_screenshots: true,
      rollout: "0.1"
    )

    puts "========================================="
    puts "✅ PROD uploaded to Play Console (draft)!"
    puts "========================================="
    puts "⚠️  MANUAL ROLLOUT REQUIRED"
    puts ""
    puts "Next Steps:"
    puts "  1. Log in to Play Console"
    puts "  2. Navigate to SmilePile app"
    puts "  3. Review release in Production track"
    puts "  4. Manually promote from draft"
    puts "  5. Control rollout percentage"
    puts ""
    puts "🔗 https://play.google.com/console"
    puts "========================================="
  end

  after_all do |lane|
    puts "========================================="
    puts "✅ Android lane '#{lane}' completed successfully"
    puts "========================================="
  end

  error do |lane, exception|
    puts "========================================="
    puts "❌ Android lane '#{lane}' failed"
    puts "Error: #{exception.message}"
    puts "========================================="
  end

end
```

**Lane-Specific Notes**:

**Lane 1: qual_android**
- **Purpose**: Local emulator testing only
- **No credentials needed**: Uses debug keystore
- **Output**: APK in app/build/outputs/apk/qual/debug/
- **Integration**: Called by deploy_qual.sh
- **Testing**: Can test immediately after Fastfile creation

**Lane 2: stage_android**
- **Purpose**: Internal testing for team
- **Credentials needed**: Play Console service account JSON
- **Output**: AAB uploaded to Internal Testing track
- **Distribution**: Internal testers only
- **Testing**: Requires Play Console access

**Lane 3: beta_android**
- **Purpose**: Closed testing for beta users
- **Credentials needed**: Play Console service account JSON
- **Output**: AAB uploaded to Closed Testing track
- **Distribution**: Beta testers in closed testing group
- **Testing**: Requires Play Console access

**Lane 4: prod_android**
- **Purpose**: Production release
- **Credentials needed**: Play Console service account JSON
- **Output**: AAB uploaded as draft to Production track
- **Distribution**: Manual rollout control (10% initial)
- **Testing**: Requires Play Console access

**Android Implementation Steps**:

1. **Create directory structure**:
   ```bash
   mkdir -p android/fastlane
   mkdir -p android/app/build/outputs/{apk,bundle}
   ```

2. **Create Appfile** (File 3 above)

3. **Create Fastfile** (File 4 above)

4. **Test syntax**:
   ```bash
   cd android
   bundle exec fastlane lanes
   # Expected output: 4 lanes listed
   ```

5. **Test QUAL lane** (no credentials needed):
   ```bash
   cd android
   bundle exec fastlane qual_android
   # Expected: APK built successfully
   ```

6. **Verify build output**:
   ```bash
   ls -la android/app/build/outputs/apk/qual/debug/
   # Should show app-qual-debug.apk
   ```

7. **Test STAGE lane** (requires credentials - see Credentials Configuration section):
   ```bash
   cd android
   bundle exec fastlane stage_android
   # Expected: AAB uploaded to Play Console
   ```

8. **Verify in Play Console**:
   - Log in to https://play.google.com/console
   - Navigate to SmilePile app
   - Check Testing > Internal testing
   - Should see new release

**Android Edge Cases and Solutions**:

**Edge Case 1: Keystore Configuration Issues**
- **Symptom**: "Keystore file not found"
- **Solution**: Verify keystore.properties has correct paths
- **Command**: `cat android/app/keystore.properties`

**Edge Case 2: Service Account Permissions**
- **Symptom**: "Insufficient permissions to upload"
- **Solution**: Grant "Release Manager" role in Play Console
- **Location**: Play Console > Setup > API access

**Edge Case 3: AAB Path Issues**
- **Symptom**: "AAB file not found"
- **Solution**: Verify flavor names match build.gradle.kts
- **Command**: `ls android/app/build/outputs/bundle/`

**Edge Case 4: Track Configuration Issues**
- **Symptom**: "Track 'internal' not found"
- **Solution**: Create testing tracks in Play Console first
- **Location**: Play Console > Testing > Internal testing (click "Create release")

## Gemfile Implementation

### File 5: Gemfile (Project Root)

**Purpose**: Manage Fastlane and Ruby dependencies with version locking

**Content**:
```ruby
# Gemfile
# Ruby dependencies for SmilePile deployment automation

source "https://rubygems.org"

# Fastlane - Mobile automation tool
gem "fastlane", "~> 2.228.0"  # Lock to current version to prevent breaking changes

# CocoaPods - iOS dependency management
# Uncomment if iOS project uses CocoaPods (check for ios/Podfile)
# gem "cocoapods", "~> 1.15"

# Optional Fastlane plugins (uncomment if needed)
# gem "fastlane-plugin-firebase_app_distribution"  # Firebase App Distribution
# gem "fastlane-plugin-versioning"                 # Version management
```

**Implementation Steps**:

1. **Create Gemfile in project root**:
   ```bash
   cd /Users/adamstack/SmilePile
   # Create Gemfile with content above
   ```

2. **Check if CocoaPods is needed**:
   ```bash
   # Check if iOS project uses CocoaPods
   ls ios/Podfile
   # If exists, uncomment cocoapods line in Gemfile
   ```

3. **Run bundle install**:
   ```bash
   bundle install
   # Expected: Installs fastlane and generates Gemfile.lock
   ```

4. **Verify Gemfile.lock created**:
   ```bash
   ls -la Gemfile.lock
   # Should exist with locked dependency versions
   ```

5. **Add to git**:
   ```bash
   git add Gemfile Gemfile.lock
   # Both files should be committed for version consistency
   ```

6. **Test fastlane via bundler**:
   ```bash
   bundle exec fastlane --version
   # Expected: 2.228.0 or similar
   ```

**Gemfile Notes**:

- **Version Locking**: Use `~> 2.228.0` to lock to minor version (prevents breaking changes)
- **Gemfile.lock**: MUST be committed to ensure consistent versions across environments
- **CocoaPods**: Only needed if iOS project has Podfile (check before uncommenting)
- **Plugins**: Uncomment only if needed (keep dependencies minimal)
- **Bundle Prefix**: Always use `bundle exec fastlane` to ensure correct version

**Verification Commands**:
```bash
# Verify fastlane installed
bundle exec fastlane --version

# Verify bundler can resolve dependencies
bundle check

# List installed gems
bundle list | grep fastlane
```

## Deploy Script Integration

### File 6: deploy/deploy_qual.sh (Modifications)

**Current Workflow** (from Phase 1 research):
1. Prerequisites check (environment, tools, git status)
2. SonarCloud analysis (code quality gate)
3. Tiered testing (unit, integration, e2e) for Android + iOS
4. Version management (build_number.sh increments version)
5. Android build via `./gradlew assembleQualDebug`
6. iOS build via `xcodebuild`
7. Simulator/emulator install and launch
8. Git commit with version bump

**Modification Strategy**:
- Replace ONLY the build commands (steps 5-6)
- Preserve ALL quality gates (steps 1-4)
- Preserve install/launch logic (step 7)
- Preserve git commit (step 8)

**Section 1: Android Deployment Function** (approximate lines 382-475)

**BEFORE (current)**:
```bash
deploy_android_local() {
    log INFO "Building Android QUAL..."

    cd "$PROJECT_ROOT/android"

    # Build APK using Gradle
    ./gradlew assembleQualDebug || {
        log ERROR "Android build failed"
        return 1
    }

    # Find APK path
    local apk_path="app/build/outputs/apk/qual/debug/app-qual-debug.apk"

    # ... install and launch logic continues ...
}
```

**AFTER (with Fastlane)**:
```bash
deploy_android_local() {
    log INFO "Building Android QUAL..."

    cd "$PROJECT_ROOT/android"

    # Build APK using Fastlane (replaces gradlew command)
    bundle exec fastlane qual_android || {
        log ERROR "Android Fastlane build failed"
        return 1
    }

    # Find APK path (same as before)
    local apk_path="app/build/outputs/apk/qual/debug/app-qual-debug.apk"

    # ... install and launch logic continues unchanged ...
    # (ADB install, emulator detection, app launch - all preserved)
}
```

**Section 2: iOS Deployment Function** (approximate lines 477-549)

**BEFORE (current)**:
```bash
deploy_ios_local() {
    if [[ "$OS_TYPE" != "Darwin" ]]; then
        log WARN "iOS deployment skipped (not on macOS)"
        return 0
    fi

    log INFO "Building iOS QUAL..."

    cd "$PROJECT_ROOT/ios"

    # Build using xcodebuild
    xcodebuild build \
        -project SmilePile.xcodeproj \
        -scheme "SmilePile Qual" \
        -configuration Debug \
        -destination 'platform=iOS Simulator,name=iPhone 16' \
        -derivedDataPath ./DerivedData || {
        log ERROR "iOS build failed"
        return 1
    }

    # Find .app path
    local app_path="DerivedData/Build/Products/Debug-iphonesimulator/SmilePile Qual.app"

    # ... install and launch logic continues ...
}
```

**AFTER (with Fastlane)**:
```bash
deploy_ios_local() {
    if [[ "$OS_TYPE" != "Darwin" ]]; then
        log WARN "iOS deployment skipped (not on macOS)"
        return 0
    fi

    log INFO "Building iOS QUAL..."

    cd "$PROJECT_ROOT/ios"

    # Build using Fastlane (replaces xcodebuild command)
    bundle exec fastlane qual_ios || {
        log ERROR "iOS Fastlane build failed"
        return 1
    }

    # Find .app path (same as before)
    local app_path="DerivedData/Build/Products/Debug-iphonesimulator/SmilePile Qual.app"

    # ... install and launch logic continues unchanged ...
    # (xcrun simctl install, simulator detection, app launch - all preserved)
}
```

**Key Principles of Integration**:

1. **Minimal Changes**: Only replace build commands, nothing else
2. **Preserve Quality Gates**: All tests, SonarCloud, version management unchanged
3. **Preserve Install Logic**: ADB/xcrun commands unchanged
4. **Preserve Environment Variables**: SKIP_TESTS, DRY_RUN, SKIP_COMMIT all work
5. **Preserve Error Handling**: Exit codes and error messages unchanged

**Implementation Steps**:

1. **Backup original script**:
   ```bash
   cp deploy/deploy_qual.sh deploy/deploy_qual.sh.backup
   ```

2. **Locate Android deployment function**:
   ```bash
   grep -n "deploy_android_local" deploy/deploy_qual.sh
   # Note the line number
   ```

3. **Replace gradlew command with Fastlane**:
   - Find: `./gradlew assembleQualDebug`
   - Replace: `bundle exec fastlane qual_android`

4. **Locate iOS deployment function**:
   ```bash
   grep -n "deploy_ios_local" deploy/deploy_qual.sh
   # Note the line number
   ```

5. **Replace xcodebuild command with Fastlane**:
   - Find: `xcodebuild build -project ... -scheme ...`
   - Replace: `bundle exec fastlane qual_ios`

6. **Test modified script**:
   ```bash
   DRY_RUN=true SKIP_TESTS=true ./deploy/deploy_qual.sh both
   # Expected: Should show Fastlane commands would be called
   ```

7. **Test real deployment**:
   ```bash
   ./deploy/deploy_qual.sh both
   # Expected: Tests run, builds succeed, apps install
   ```

8. **Verify git diff**:
   ```bash
   git diff deploy/deploy_qual.sh
   # Should show only build command changes
   ```

**Verification Checklist**:
- [ ] Tests still run before build
- [ ] SonarCloud analysis still runs
- [ ] Version numbers still increment
- [ ] Fastlane builds succeed
- [ ] Apps still install to simulator/emulator
- [ ] Apps still launch correctly
- [ ] Git commit still happens
- [ ] Environment variables still work (SKIP_TESTS, DRY_RUN, SKIP_COMMIT)

### File 7: deploy/deploy_stage.sh (New File)

**Purpose**: Deploy STAGE tier to TestFlight Internal Testing + Play Console Internal Testing

**Complete Content**:
```bash
#!/bin/bash
# deploy/deploy_stage.sh
# Deploy STAGE tier to TestFlight Internal + Play Console Internal Testing
#
# Usage:
#   ./deploy/deploy_stage.sh [ios|android|both]
#
# Environment Variables:
#   SKIP_TESTS=true       Skip test execution (NOT RECOMMENDED)
#   SKIP_COMMIT=true      Skip git commit
#   DRY_RUN=true          Show what would happen without executing

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DEPLOY_ROOT="$SCRIPT_DIR"

# Source common functions
source "${DEPLOY_ROOT}/lib/common.sh"
source "${DEPLOY_ROOT}/lib/build_number.sh"

# Parse arguments
PLATFORM="${1:-both}"  # ios, android, or both

# Validate platform
if [[ "$PLATFORM" != "ios" && "$PLATFORM" != "android" && "$PLATFORM" != "both" ]]; then
    log ERROR "Invalid platform: $PLATFORM"
    log INFO "Usage: $0 [ios|android|both]"
    exit 1
fi

log INFO "========================================="
log INFO "🚀 STAGE Tier Deployment"
log INFO "========================================="
log INFO "Platform: $PLATFORM"
log INFO "Target: TestFlight Internal + Play Console Internal"
log INFO ""

# ============================================================================
# STEP 1: Prerequisites Check
# ============================================================================
log INFO "📋 Step 1: Checking prerequisites..."

check_prerequisites "$PLATFORM"

log SUCCESS "✅ Prerequisites check passed"
log INFO ""

# ============================================================================
# STEP 2: Update Version Numbers
# ============================================================================
log INFO "📝 Step 2: Updating version numbers..."

# Update version for both platforms (ensures consistency)
update_version_all_platforms "$PLATFORM"

log SUCCESS "✅ Version numbers updated"
log INFO ""

# ============================================================================
# STEP 3: Run Tiered Tests
# ============================================================================
if [[ "${SKIP_TESTS:-false}" != "true" ]]; then
    log INFO "🧪 Step 3: Running tiered tests..."

    run_tests "$PLATFORM"

    log SUCCESS "✅ All tests passed"
    log INFO ""
else
    log WARN "⚠️  Step 3: Tests skipped (SKIP_TESTS=true)"
    log WARN "⚠️  This is NOT RECOMMENDED for STAGE deployments"
    log INFO ""
fi

# ============================================================================
# STEP 4: Deploy iOS to TestFlight Internal Testing
# ============================================================================
if [[ "$PLATFORM" == "ios" || "$PLATFORM" == "both" ]]; then
    if [[ "$OS_TYPE" != "Darwin" ]]; then
        log WARN "⚠️  iOS deployment skipped (not on macOS)"
        log INFO ""
    else
        log INFO "📱 Step 4a: Building and uploading iOS STAGE..."

        if [[ "${DRY_RUN:-false}" == "true" ]]; then
            log WARN "[DRY RUN] Would execute: cd ios && bundle exec fastlane stage_ios"
        else
            cd "$PROJECT_ROOT/ios"
            bundle exec fastlane stage_ios || {
                log ERROR "iOS STAGE deployment failed"
                log ERROR "Check logs in ios/build/logs/"
                exit 1
            }
        fi

        log SUCCESS "✅ iOS STAGE uploaded to TestFlight Internal Testing"
        log INFO "🔗 View: https://appstoreconnect.apple.com"
        log INFO ""
    fi
fi

# ============================================================================
# STEP 5: Deploy Android to Play Console Internal Testing
# ============================================================================
if [[ "$PLATFORM" == "android" || "$PLATFORM" == "both" ]]; then
    log INFO "🤖 Step 4b: Building and uploading Android STAGE..."

    if [[ "${DRY_RUN:-false}" == "true" ]]; then
        log WARN "[DRY RUN] Would execute: cd android && bundle exec fastlane stage_android"
    else
        cd "$PROJECT_ROOT/android"
        bundle exec fastlane stage_android || {
            log ERROR "Android STAGE deployment failed"
            log ERROR "Check logs in android/app/build/logs/"
            exit 1
        }
    fi

    log SUCCESS "✅ Android STAGE uploaded to Play Console Internal Testing"
    log INFO "🔗 View: https://play.google.com/console"
    log INFO ""
fi

# ============================================================================
# STEP 6: Git Commit
# ============================================================================
if [[ "${SKIP_COMMIT:-false}" != "true" ]]; then
    log INFO "📝 Step 5: Committing changes..."

    if [[ "${DRY_RUN:-false}" == "true" ]]; then
        log WARN "[DRY RUN] Would commit version bump"
    else
        cd "$PROJECT_ROOT"
        commit_changes "stage" "$PLATFORM"
    fi

    log SUCCESS "✅ Changes committed"
    log INFO ""
else
    log WARN "⚠️  Step 5: Git commit skipped (SKIP_COMMIT=true)"
    log INFO ""
fi

# ============================================================================
# STEP 7: Deployment Summary
# ============================================================================
log INFO "========================================="
log SUCCESS "🎉 STAGE Deployment Complete!"
log INFO "========================================="
log INFO ""
log INFO "Deployment Details:"
log INFO "  Platform: $PLATFORM"

# Get version numbers
if [[ -f "$PROJECT_ROOT/.build_number" ]]; then
    VERSION_MAJOR=$(head -n1 "$PROJECT_ROOT/.build_number")
    VERSION_MINOR=$(tail -n1 "$PROJECT_ROOT/.build_number")
    log INFO "  Version: ${VERSION_MAJOR}.${VERSION_MINOR}"
fi

log INFO ""
log INFO "Testing Links:"
if [[ "$PLATFORM" == "ios" || "$PLATFORM" == "both" ]]; then
    log INFO "  iOS TestFlight: https://appstoreconnect.apple.com"
    log INFO "                  Section: TestFlight > Internal Testing"
fi
if [[ "$PLATFORM" == "android" || "$PLATFORM" == "both" ]]; then
    log INFO "  Android Play Console: https://play.google.com/console"
    log INFO "                        Section: Testing > Internal testing"
fi
log INFO ""
log INFO "Next Steps:"
log INFO "  1. Verify builds in respective dashboards"
log INFO "  2. Test on internal tester devices"
log INFO "  3. Verify no regressions"
log INFO "  4. Proceed to BETA when ready"
log INFO "========================================="
```

**Implementation Steps**:

1. **Create script**:
   ```bash
   touch deploy/deploy_stage.sh
   # Add content above
   ```

2. **Make executable**:
   ```bash
   chmod +x deploy/deploy_stage.sh
   ```

3. **Test dry run**:
   ```bash
   DRY_RUN=true SKIP_TESTS=true ./deploy/deploy_stage.sh both
   # Expected: Shows what would happen, doesn't execute
   ```

4. **Test with tests**:
   ```bash
   DRY_RUN=true ./deploy/deploy_stage.sh both
   # Expected: Runs tests, then shows what would happen
   ```

5. **Test real deployment** (iOS only first):
   ```bash
   ./deploy/deploy_stage.sh ios
   # Expected: Tests run, iOS uploads to TestFlight
   ```

6. **Test real deployment** (both platforms):
   ```bash
   ./deploy/deploy_stage.sh both
   # Expected: Tests run, both platforms upload
   ```

**Verification**:
- [ ] Script is executable
- [ ] DRY_RUN mode works
- [ ] SKIP_TESTS environment variable works
- [ ] SKIP_COMMIT environment variable works
- [ ] iOS uploads to TestFlight Internal
- [ ] Android uploads to Play Console Internal
- [ ] Version numbers increment correctly
- [ ] Git commit happens (if not skipped)

### File 8: deploy/deploy_beta.sh (New File)

**Purpose**: Deploy BETA tier to TestFlight External Testing + Play Console Closed Testing

**Complete Content**:
```bash
#!/bin/bash
# deploy/deploy_beta.sh
# Deploy BETA tier to TestFlight External + Play Console Closed Testing
#
# Usage:
#   ./deploy/deploy_beta.sh [ios|android|both]
#
# Environment Variables:
#   SKIP_TESTS=true       Skip test execution (NOT RECOMMENDED)
#   SKIP_COMMIT=true      Skip git commit
#   DRY_RUN=true          Show what would happen without executing

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DEPLOY_ROOT="$SCRIPT_DIR"

# Source common functions
source "${DEPLOY_ROOT}/lib/common.sh"
source "${DEPLOY_ROOT}/lib/build_number.sh"

# Parse arguments
PLATFORM="${1:-both}"  # ios, android, or both

# Validate platform
if [[ "$PLATFORM" != "ios" && "$PLATFORM" != "android" && "$PLATFORM" != "both" ]]; then
    log ERROR "Invalid platform: $PLATFORM"
    log INFO "Usage: $0 [ios|android|both]"
    exit 1
fi

log INFO "========================================="
log INFO "🚀 BETA Tier Deployment"
log INFO "========================================="
log INFO "Platform: $PLATFORM"
log INFO "Target: TestFlight External + Play Console Closed Testing"
log INFO ""

# ============================================================================
# STEP 1: Prerequisites Check
# ============================================================================
log INFO "📋 Step 1: Checking prerequisites..."

check_prerequisites "$PLATFORM"

log SUCCESS "✅ Prerequisites check passed"
log INFO ""

# ============================================================================
# STEP 2: Update Version Numbers
# ============================================================================
log INFO "📝 Step 2: Updating version numbers..."

# Update version for both platforms (ensures consistency)
update_version_all_platforms "$PLATFORM"

log SUCCESS "✅ Version numbers updated"
log INFO ""

# ============================================================================
# STEP 3: Run Tiered Tests
# ============================================================================
if [[ "${SKIP_TESTS:-false}" != "true" ]]; then
    log INFO "🧪 Step 3: Running tiered tests..."

    run_tests "$PLATFORM"

    log SUCCESS "✅ All tests passed"
    log INFO ""
else
    log WARN "⚠️  Step 3: Tests skipped (SKIP_TESTS=true)"
    log WARN "⚠️  This is NOT RECOMMENDED for BETA deployments"
    log INFO ""
fi

# ============================================================================
# STEP 4: Deploy iOS to TestFlight External Testing
# ============================================================================
if [[ "$PLATFORM" == "ios" || "$PLATFORM" == "both" ]]; then
    if [[ "$OS_TYPE" != "Darwin" ]]; then
        log WARN "⚠️  iOS deployment skipped (not on macOS)"
        log INFO ""
    else
        log INFO "📱 Step 4a: Building and uploading iOS BETA..."
        log WARN "⚠️  External TestFlight requires Apple review approval"

        if [[ "${DRY_RUN:-false}" == "true" ]]; then
            log WARN "[DRY RUN] Would execute: cd ios && bundle exec fastlane beta_ios"
        else
            cd "$PROJECT_ROOT/ios"
            bundle exec fastlane beta_ios || {
                log ERROR "iOS BETA deployment failed"
                log ERROR "Check logs in ios/build/logs/"
                exit 1
            }
        fi

        log SUCCESS "✅ iOS BETA uploaded to TestFlight External Testing"
        log INFO "🔗 View: https://appstoreconnect.apple.com"
        log WARN "⚠️  Build will be pending Apple review for external distribution"
        log INFO ""
    fi
fi

# ============================================================================
# STEP 5: Deploy Android to Play Console Closed Testing
# ============================================================================
if [[ "$PLATFORM" == "android" || "$PLATFORM" == "both" ]]; then
    log INFO "🤖 Step 4b: Building and uploading Android BETA..."

    if [[ "${DRY_RUN:-false}" == "true" ]]; then
        log WARN "[DRY RUN] Would execute: cd android && bundle exec fastlane beta_android"
    else
        cd "$PROJECT_ROOT/android"
        bundle exec fastlane beta_android || {
            log ERROR "Android BETA deployment failed"
            log ERROR "Check logs in android/app/build/logs/"
            exit 1
        }
    fi

    log SUCCESS "✅ Android BETA uploaded to Play Console Closed Testing"
    log INFO "🔗 View: https://play.google.com/console"
    log INFO ""
fi

# ============================================================================
# STEP 6: Git Commit
# ============================================================================
if [[ "${SKIP_COMMIT:-false}" != "true" ]]; then
    log INFO "📝 Step 5: Committing changes..."

    if [[ "${DRY_RUN:-false}" == "true" ]]; then
        log WARN "[DRY RUN] Would commit version bump"
    else
        cd "$PROJECT_ROOT"
        commit_changes "beta" "$PLATFORM"
    fi

    log SUCCESS "✅ Changes committed"
    log INFO ""
else
    log WARN "⚠️  Step 5: Git commit skipped (SKIP_COMMIT=true)"
    log INFO ""
fi

# ============================================================================
# STEP 7: Deployment Summary
# ============================================================================
log INFO "========================================="
log SUCCESS "🎉 BETA Deployment Complete!"
log INFO "========================================="
log INFO ""
log INFO "Deployment Details:"
log INFO "  Platform: $PLATFORM"

# Get version numbers
if [[ -f "$PROJECT_ROOT/.build_number" ]]; then
    VERSION_MAJOR=$(head -n1 "$PROJECT_ROOT/.build_number")
    VERSION_MINOR=$(tail -n1 "$PROJECT_ROOT/.build_number")
    log INFO "  Version: ${VERSION_MAJOR}.${VERSION_MINOR}"
fi

log INFO ""
log INFO "Testing Links:"
if [[ "$PLATFORM" == "ios" || "$PLATFORM" == "both" ]]; then
    log INFO "  iOS TestFlight: https://appstoreconnect.apple.com"
    log INFO "                  Section: TestFlight > External Testing"
    log WARN "                  Status: Pending Apple review"
fi
if [[ "$PLATFORM" == "android" || "$PLATFORM" == "both" ]]; then
    log INFO "  Android Play Console: https://play.google.com/console"
    log INFO "                        Section: Testing > Closed testing"
fi
log INFO ""
log INFO "Next Steps:"
log INFO "  1. Verify builds in respective dashboards"
if [[ "$PLATFORM" == "ios" || "$PLATFORM" == "both" ]]; then
    log INFO "  2. Wait for Apple TestFlight review (iOS)"
fi
log INFO "  3. Notify beta testers when available"
log INFO "  4. Monitor feedback and crash reports"
log INFO "  5. Proceed to PROD when ready"
log INFO "========================================="
```

**Implementation Steps**:

1. **Create script**:
   ```bash
   touch deploy/deploy_beta.sh
   # Add content above
   ```

2. **Make executable**:
   ```bash
   chmod +x deploy/deploy_beta.sh
   ```

3. **Test dry run**:
   ```bash
   DRY_RUN=true SKIP_TESTS=true ./deploy/deploy_beta.sh both
   ```

4. **Test real deployment**:
   ```bash
   ./deploy/deploy_beta.sh both
   ```

**Verification**:
- [ ] Script is executable
- [ ] iOS uploads to TestFlight External
- [ ] Android uploads to Play Console Closed Testing
- [ ] All environment variables work

### File 9: deploy/deploy_prod.sh (Modifications)

**Current Status**: Script may exist but likely incomplete

**Required Modifications**:

1. **Add safety confirmation prompt**
2. **Integrate Fastlane prod lanes**
3. **Ensure draft/manual submission mode**
4. **Add production-specific warnings**

**Key Sections to Add/Modify**:

**Section 1: Safety Confirmation** (add near start):
```bash
# Safety confirmation for production deployment
log WARN "========================================="
log WARN "⚠️  PRODUCTION DEPLOYMENT"
log WARN "========================================="
log WARN "This will upload to:"
log WARN "  - iOS: App Store Connect (manual submission required)"
log WARN "  - Android: Play Console Production (draft, manual rollout required)"
log WARN ""
log WARN "Platform: $PLATFORM"
log WARN ""
read -p "Type 'PROD' to confirm production deployment: " confirmation

if [[ "$confirmation" != "PROD" ]]; then
    log ERROR "Deployment cancelled (confirmation failed)"
    log INFO "You typed: '$confirmation'"
    log INFO "Required: 'PROD'"
    exit 1
fi

log INFO "Production deployment confirmed"
log INFO ""
```

**Section 2: iOS Production Deployment** (replace existing):
```bash
if [[ "$PLATFORM" == "ios" || "$PLATFORM" == "both" ]]; then
    if [[ "$OS_TYPE" != "Darwin" ]]; then
        log WARN "⚠️  iOS deployment skipped (not on macOS)"
    else
        log INFO "📱 Building and uploading iOS PROD..."

        cd "$PROJECT_ROOT/ios"
        bundle exec fastlane prod_ios || {
            log ERROR "iOS PROD deployment failed"
            exit 1
        }

        log SUCCESS "✅ iOS PROD uploaded to App Store Connect"
        log WARN "⚠️  MANUAL SUBMISSION REQUIRED in App Store Connect"
    fi
fi
```

**Section 3: Android Production Deployment** (replace existing):
```bash
if [[ "$PLATFORM" == "android" || "$PLATFORM" == "both" ]]; then
    log INFO "🤖 Building and uploading Android PROD..."

    cd "$PROJECT_ROOT/android"
    bundle exec fastlane prod_android || {
        log ERROR "Android PROD deployment failed"
        exit 1
    }

    log SUCCESS "✅ Android PROD uploaded to Play Console (draft)"
    log WARN "⚠️  MANUAL ROLLOUT REQUIRED in Play Console"
fi
```

**Implementation Steps**:

1. **Check if deploy_prod.sh exists**:
   ```bash
   ls -la deploy/deploy_prod.sh
   ```

2. **If exists, back up**:
   ```bash
   cp deploy/deploy_prod.sh deploy/deploy_prod.sh.backup
   ```

3. **If doesn't exist, create from deploy_beta.sh template**:
   ```bash
   cp deploy/deploy_beta.sh deploy/deploy_prod.sh
   # Then modify for production
   ```

4. **Add safety confirmation** (Section 1 above)

5. **Update iOS deployment** (Section 2 above)

6. **Update Android deployment** (Section 3 above)

7. **Update summary messages** to emphasize manual submission

8. **Test dry run**:
   ```bash
   DRY_RUN=true SKIP_TESTS=true ./deploy/deploy_prod.sh both
   # Should prompt for 'PROD' confirmation
   ```

**Verification**:
- [ ] Safety confirmation prompt works
- [ ] Script rejects deployment if not confirmed with 'PROD'
- [ ] iOS uploads as draft (manual submission required)
- [ ] Android uploads as draft (manual rollout required)
- [ ] All quality gates still enforce

## Credentials Configuration

### Prerequisite: App Store Connect API Key

**Purpose**: Enable automated iOS uploads without interactive authentication

**Generation Steps**:

1. **Log in to App Store Connect**:
   - Navigate to https://appstoreconnect.apple.com
   - Sign in with Apple Developer account

2. **Navigate to API Keys**:
   - Click "Users and Access" (top navigation)
   - Click "Keys" tab
   - Click "+" button to create new key

3. **Create Key**:
   - Name: "SmilePile Fastlane Automation"
   - Access: "App Manager" (minimum required role)
   - Click "Generate"

4. **Download Key**:
   - Click "Download API Key" (only available once!)
   - File will be named: `AuthKey_XXXXXXXXXX.p8`
   - Note the Key ID (shown on screen)
   - Note the Issuer ID (shown on screen)

5. **Store Key Securely**:
   ```bash
   # Create fastlane credentials directory
   mkdir -p ~/.fastlane
   chmod 700 ~/.fastlane

   # Move API key
   mv ~/Downloads/AuthKey_XXXXXXXXXX.p8 ~/.fastlane/
   chmod 600 ~/.fastlane/AuthKey_XXXXXXXXXX.p8
   ```

6. **Backup Key** (CRITICAL):
   ```bash
   # USB drive backup
   cp ~/.fastlane/AuthKey_XXXXXXXXXX.p8 /Volumes/USB/backups/

   # Encrypted cloud storage (example)
   # Store in password manager with Key ID and Issuer ID
   ```

**Fastfile Integration**:

Add to `ios/fastlane/Fastfile` before_all block:

```ruby
before_all do |lane|
  puts "🚀 Starting iOS deployment: #{lane}"

  # Configure App Store Connect API key
  app_store_connect_api_key(
    key_id: "XXXXXXXXXX",              # Replace with your Key ID
    issuer_id: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",  # Replace with Issuer ID
    key_filepath: "~/.fastlane/AuthKey_XXXXXXXXXX.p8"   # Path to .p8 file
  )
end
```

**Verification**:
```bash
# Test API key works
cd ios
bundle exec fastlane stage_ios
# Should upload without prompting for password
```

### Prerequisite: Play Console Service Account

**Purpose**: Enable automated Android uploads to Google Play Console

**Generation Steps**:

1. **Log in to Google Play Console**:
   - Navigate to https://play.google.com/console
   - Select SmilePile app

2. **Navigate to API Access**:
   - Click "Setup" in left sidebar
   - Click "API access"

3. **Create or Use Service Account**:
   - If no service account exists:
     - Click "Create new service account"
     - Follow link to Google Cloud Console
     - Create new service account
     - Name: "SmilePile Fastlane Automation"
   - If service account exists:
     - Use existing account

4. **Grant Permissions**:
   - In Play Console API access page
   - Find service account in list
   - Click "Grant access"
   - Select "Release Manager" role (minimum required)
   - Click "Apply"

5. **Download JSON Key**:
   - In Google Cloud Console
   - Navigate to "IAM & Admin" > "Service Accounts"
   - Find SmilePile service account
   - Click "Keys" tab
   - Click "Add Key" > "Create new key"
   - Select "JSON" format
   - Click "Create"
   - File downloads: `smilepile-xxxxxxxxxxxx-xxxxxxxxxxxxxx.json`

6. **Store Key Securely**:
   ```bash
   # Move to fastlane directory with standard name
   mv ~/Downloads/smilepile-*.json ~/.fastlane/play-store-credentials.json
   chmod 600 ~/.fastlane/play-store-credentials.json
   ```

7. **Backup Key** (CRITICAL):
   ```bash
   # USB drive backup
   cp ~/.fastlane/play-store-credentials.json /Volumes/USB/backups/

   # Store in password manager or encrypted cloud storage
   ```

**Appfile Reference**:

Already configured in `android/fastlane/Appfile`:

```ruby
json_key_file("~/.fastlane/play-store-credentials.json")
package_name("com.smilepile")
```

**Verification**:
```bash
# Test service account works
cd android
bundle exec fastlane stage_android
# Should upload without prompting for credentials
```

### Prerequisite: Production Keystore (Android)

**Purpose**: Sign Android release builds with production certificate

**Verification Steps**:

1. **Check if production keystore exists**:
   ```bash
   # Check keystore.properties
   cat android/app/keystore.properties

   # Look for storeFile path
   # Example: storeFile=/Users/adamstack/keystores/smilepile-production.keystore
   ```

2. **Verify keystore file exists**:
   ```bash
   # Check if file exists at path from keystore.properties
   ls -la ~/keystores/smilepile-production.keystore
   ```

**If Production Keystore Doesn't Exist**:

**CRITICAL WARNING**: If you lose the production keystore, you CANNOT update your app in Play Store. You would have to publish a completely new app with a different package name.

**Generation Steps**:

1. **Create keystore directory**:
   ```bash
   mkdir -p ~/keystores
   chmod 700 ~/keystores
   ```

2. **Generate production keystore**:
   ```bash
   keytool -genkeypair \
     -alias smilepile-prod \
     -keyalg RSA \
     -keysize 2048 \
     -validity 10000 \
     -keystore ~/keystores/smilepile-production.keystore

   # You will be prompted for:
   # - Keystore password (create strong password, store in password manager)
   # - Key password (can be same as keystore password)
   # - Name, organization details (enter as prompted)
   ```

3. **Secure keystore file**:
   ```bash
   chmod 600 ~/keystores/smilepile-production.keystore
   ```

4. **CRITICAL: Backup keystore immediately**:
   ```bash
   # Backup 1: USB drive
   cp ~/keystores/smilepile-production.keystore /Volumes/USB/backups/

   # Backup 2: Encrypted cloud storage (e.g., 1Password, LastPass secure notes)
   # Base64 encode for storage:
   base64 < ~/keystores/smilepile-production.keystore > ~/Desktop/keystore-backup.txt
   # Store this text in password manager

   # Backup 3: Secure location #2 (different physical location)
   ```

5. **Update keystore.properties**:
   ```bash
   # Edit android/app/keystore.properties
   cat > android/app/keystore.properties << EOF
   storeFile=/Users/adamstack/keystores/smilepile-production.keystore
   storePassword=YOUR_STORE_PASSWORD
   keyAlias=smilepile-prod
   keyPassword=YOUR_KEY_PASSWORD
   EOF

   chmod 600 android/app/keystore.properties
   ```

6. **Verify keystore.properties is in .gitignore**:
   ```bash
   grep "keystore.properties" .gitignore
   # Should be listed (credentials must never be committed)
   ```

**Keystore Configuration for Tiers**:

- **QUAL**: Uses debug keystore (`~/.android/debug.keystore`)
- **STAGE**: Uses production keystore (for testing release signing)
- **BETA**: Uses production keystore
- **PROD**: Uses production keystore

**Verification**:
```bash
# Test production keystore works
cd android
bundle exec fastlane stage_android
# Should build AAB successfully with production signing
```

**Security Checklist**:
- [ ] Production keystore backed up to USB drive
- [ ] Production keystore backed up to encrypted cloud storage
- [ ] Production keystore backed up to secure location #2
- [ ] Keystore passwords stored in password manager
- [ ] keystore.properties in .gitignore
- [ ] Keystore file has 600 permissions
- [ ] Keystore directory has 700 permissions

## Testing Strategy

### Local Testing Sequence

**Phase 1: Syntax Validation** (5 minutes)

**Purpose**: Verify Fastfiles are syntactically correct before attempting builds

```bash
# Test iOS Fastfile syntax
cd /Users/adamstack/SmilePile/ios
bundle exec fastlane lanes

# Expected output:
# -------------------------
# --- Available lanes ---
# -------------------------
# ios
#   ios qual_ios
#   ios stage_ios
#   ios beta_ios
#   ios prod_ios

# Test Android Fastfile syntax
cd /Users/adamstack/SmilePile/android
bundle exec fastlane lanes

# Expected output:
# -------------------------
# --- Available lanes ---
# -------------------------
# android
#   android qual_android
#   android stage_android
#   android beta_android
#   android prod_android
```

**Verification**:
- [ ] iOS Fastfile shows 4 lanes
- [ ] Android Fastfile shows 4 lanes
- [ ] No syntax errors reported
- [ ] No missing dependencies

**Phase 2: QUAL Lane Testing** (15 minutes)

**Purpose**: Test local builds without requiring credentials

**Test 1: iOS QUAL Build**
```bash
cd /Users/adamstack/SmilePile/ios
bundle exec fastlane qual_ios

# Expected output:
# - Build starts
# - Compiles source files
# - Links executable
# - Creates .app bundle
# - Shows app path: DerivedData/Build/Products/Debug-iphonesimulator/SmilePile Qual.app
```

**Verification**:
```bash
# Verify .app exists
ls -la ios/DerivedData/Build/Products/Debug-iphonesimulator/
# Should show "SmilePile Qual.app"

# Verify bundle ID
/usr/libexec/PlistBuddy -c "Print :CFBundleIdentifier" "ios/DerivedData/Build/Products/Debug-iphonesimulator/SmilePile Qual.app/Info.plist"
# Should output: com.smilepile.qual
```

**Test 2: Android QUAL Build**
```bash
cd /Users/adamstack/SmilePile/android
bundle exec fastlane qual_android

# Expected output:
# - Gradle build starts
# - Compiles Kotlin source
# - Packages APK
# - Shows APK path: app/build/outputs/apk/qual/debug/app-qual-debug.apk
```

**Verification**:
```bash
# Verify APK exists
ls -la android/app/build/outputs/apk/qual/debug/
# Should show "app-qual-debug.apk"

# Verify package name
aapt dump badging android/app/build/outputs/apk/qual/debug/app-qual-debug.apk | grep package
# Should output: package: name='com.smilepile.qual'
```

**Phase 2 Checklist**:
- [ ] iOS QUAL build succeeds
- [ ] iOS .app bundle created
- [ ] iOS bundle ID correct (com.smilepile.qual)
- [ ] Android QUAL build succeeds
- [ ] Android APK created
- [ ] Android package name correct (com.smilepile.qual)

**Phase 3: Deploy Script Testing** (20 minutes)

**Purpose**: Test Fastlane integration with existing deploy scripts

**Test 1: deploy_qual.sh Dry Run**
```bash
cd /Users/adamstack/SmilePile
DRY_RUN=true SKIP_TESTS=true ./deploy/deploy_qual.sh both

# Expected output:
# - [DRY RUN] Would run tests
# - [DRY RUN] Would execute: bundle exec fastlane qual_ios
# - [DRY RUN] Would execute: bundle exec fastlane qual_android
# - [DRY RUN] Would install to simulators
```

**Test 2: deploy_qual.sh Real Execution**
```bash
cd /Users/adamstack/SmilePile
./deploy/deploy_qual.sh both

# Expected output:
# - Prerequisites check passes
# - SonarCloud analysis runs (or skips if configured)
# - Tests run and pass
# - Version numbers increment
# - Fastlane qual_ios builds
# - Fastlane qual_android builds
# - Apps install to simulator/emulator
# - Git commit happens
```

**Test 3: deploy_stage.sh Dry Run**
```bash
cd /Users/adamstack/SmilePile
DRY_RUN=true SKIP_TESTS=true ./deploy/deploy_stage.sh both

# Expected output:
# - [DRY RUN] Would execute: bundle exec fastlane stage_ios
# - [DRY RUN] Would execute: bundle exec fastlane stage_android
```

**Test 4: Environment Variable Testing**
```bash
# Test SKIP_TESTS
SKIP_TESTS=true ./deploy/deploy_qual.sh both
# Expected: Tests skipped, builds succeed

# Test SKIP_COMMIT
SKIP_COMMIT=true ./deploy/deploy_qual.sh both
# Expected: No git commit happens

# Test platform selection
./deploy/deploy_qual.sh ios
# Expected: Only iOS builds
```

**Phase 3 Checklist**:
- [ ] deploy_qual.sh dry run works
- [ ] deploy_qual.sh real execution works
- [ ] deploy_stage.sh dry run works
- [ ] SKIP_TESTS environment variable works
- [ ] SKIP_COMMIT environment variable works
- [ ] DRY_RUN environment variable works
- [ ] Platform selection works (ios/android/both)

**Phase 4: Upload Testing** (30 minutes)

**PREREQUISITE**: Credentials must be configured (see Credentials Configuration section)

**Test 1: iOS STAGE Upload to TestFlight**
```bash
cd /Users/adamstack/SmilePile/ios
bundle exec fastlane stage_ios

# Expected output:
# - Build starts
# - IPA created
# - Upload to App Store Connect begins
# - Upload completes
# - Success message with TestFlight link
```

**Verification**:
1. Log in to https://appstoreconnect.apple.com
2. Navigate to SmilePile app
3. Click "TestFlight" tab
4. Click "Internal Testing" section
5. Verify new build appears
6. Check version and build numbers match

**Test 2: Android STAGE Upload to Play Console**
```bash
cd /Users/adamstack/SmilePile/android
bundle exec fastlane stage_android

# Expected output:
# - Build starts
# - AAB created
# - Upload to Play Console begins
# - Upload completes
# - Success message with Play Console link
```

**Verification**:
1. Log in to https://play.google.com/console
2. Navigate to SmilePile app
3. Click "Testing" > "Internal testing"
4. Verify new release appears
5. Check version and build numbers match

**Test 3: End-to-End STAGE Deployment**
```bash
cd /Users/adamstack/SmilePile
./deploy/deploy_stage.sh both

# Expected output:
# - All quality gates pass
# - Version increments
# - Both platforms upload successfully
# - Git commit happens
```

**Phase 4 Checklist**:
- [ ] iOS uploads to TestFlight Internal successfully
- [ ] iOS build appears in App Store Connect
- [ ] Android uploads to Play Console Internal successfully
- [ ] Android release appears in Play Console
- [ ] Version numbers are correct in both dashboards
- [ ] End-to-end deploy_stage.sh works

**Phase 5: Version Management Testing** (15 minutes)

**Purpose**: Verify version numbers increment correctly and sync across platforms

**Test 1: Initial Version Check**
```bash
cd /Users/adamstack/SmilePile

# Check .build_number
cat .build_number
# Shows: 25.10.15
#        001

# Check iOS Info.plist
/usr/libexec/PlistBuddy -c "Print :CFBundleShortVersionString" ios/SmilePile/Info.plist
# Should match: 25.10.15

/usr/libexec/PlistBuddy -c "Print :CFBundleVersion" ios/SmilePile/Info.plist
# Should match: 1 (or current build number)

# Check Android build.gradle.kts
grep "versionName" android/app/build.gradle.kts
grep "versionCode" android/app/build.gradle.kts
```

**Test 2: Version Increment**
```bash
# Run deployment
./deploy/deploy_qual.sh both

# Check .build_number again
cat .build_number
# Daily counter should increment: 001 -> 002

# Verify iOS updated
/usr/libexec/PlistBuddy -c "Print :CFBundleVersion" ios/SmilePile/Info.plist
# Should match new build number

# Verify Android updated
grep "versionCode" android/app/build.gradle.kts
# Should match new build number
```

**Test 3: Cross-Platform Consistency**
```bash
# Deploy iOS only
./deploy/deploy_qual.sh ios

# Check both platforms still have same version
# (build_number.sh updates both even if only deploying one)
```

**Phase 5 Checklist**:
- [ ] .build_number increments correctly
- [ ] iOS Info.plist version matches
- [ ] iOS Info.plist build number matches
- [ ] Android versionName matches
- [ ] Android versionCode matches
- [ ] Versions stay in sync across platforms

### Edge Case Testing

**Test 1: Missing Credentials** (10 minutes)

**Purpose**: Verify helpful error messages when credentials are missing

```bash
# Temporarily remove iOS API key
mv ~/.fastlane/AuthKey_*.p8 ~/.fastlane/AuthKey_*.p8.backup

# Try to run stage_ios
cd /Users/adamstack/SmilePile/ios
bundle exec fastlane stage_ios

# Expected output:
# - Error about missing API key
# - Clear message about what's needed
# - No cryptic error messages

# Restore credentials
mv ~/.fastlane/AuthKey_*.p8.backup ~/.fastlane/AuthKey_*.p8

# Temporarily remove Android service account
mv ~/.fastlane/play-store-credentials.json ~/.fastlane/play-store-credentials.json.backup

# Try to run stage_android
cd /Users/adamstack/SmilePile/android
bundle exec fastlane stage_android

# Expected output:
# - Error about missing JSON key
# - Clear message about what's needed

# Restore credentials
mv ~/.fastlane/play-store-credentials.json.backup ~/.fastlane/play-store-credentials.json
```

**Verification**:
- [ ] Missing iOS API key shows clear error
- [ ] Missing Android service account shows clear error
- [ ] Errors don't crash script
- [ ] Errors suggest what to do

**Test 2: Test Failures Block Deployment** (10 minutes)

**Purpose**: Verify quality gates still enforce

```bash
# Introduce intentional test failure
# Example: Edit a test file to fail
# (Specific implementation depends on test framework)

# Try to run deployment
./deploy/deploy_stage.sh both

# Expected output:
# - Tests run
# - Tests fail
# - Deployment aborted
# - Fastlane never called
# - No upload happens

# Fix test failure

# Run deployment again
./deploy/deploy_stage.sh both

# Expected output:
# - Tests pass
# - Deployment proceeds
```

**Verification**:
- [ ] Failing tests block deployment
- [ ] Fastlane not called when tests fail
- [ ] No uploads when tests fail
- [ ] Clear error message shown

**Test 3: Build Failures** (10 minutes)

**Purpose**: Verify build errors are caught and reported clearly

```bash
# Introduce compilation error
# Example: Add syntax error to source file

# Try to run qual lane
cd /Users/adamstack/SmilePile/ios
bundle exec fastlane qual_ios

# Expected output:
# - Compilation fails
# - Clear error message
# - Build log path shown
# - Script exits with error code

# Fix compilation error

# Verify builds work again
bundle exec fastlane qual_ios
```

**Verification**:
- [ ] Build errors caught correctly
- [ ] Error messages are clear
- [ ] Build log paths shown
- [ ] Script exits with non-zero code

**Test 4: Partial Platform Deployment** (10 minutes)

**Purpose**: Verify platform selection works correctly

```bash
# Test iOS only
./deploy/deploy_qual.sh ios
# Expected: Only iOS builds, Android skipped

# Test Android only
./deploy/deploy_qual.sh android
# Expected: Only Android builds, iOS skipped

# Test both
./deploy/deploy_qual.sh both
# Expected: Both platforms build

# Test invalid platform
./deploy/deploy_qual.sh invalid
# Expected: Error message, usage shown
```

**Verification**:
- [ ] iOS-only deployment works
- [ ] Android-only deployment works
- [ ] Both platforms deployment works
- [ ] Invalid platform shows error

**Test 5: macOS Check for iOS** (5 minutes)

**Purpose**: Verify iOS deployment skips gracefully on non-macOS

```bash
# Simulate non-macOS environment
OS_TYPE="Linux" ./deploy/deploy_qual.sh ios

# Expected output:
# - Warning: iOS deployment skipped (not on macOS)
# - Script continues without error
# - No crash
```

**Verification**:
- [ ] Non-macOS shows warning
- [ ] Script doesn't crash
- [ ] Graceful skip message

## CI/CD Integration

### GitHub Actions Modifications

**File 10: .github/workflows/deploy-stage.yml (New)**

**Purpose**: Automated STAGE tier deployment from CI/CD pipeline

**Complete Content**:
```yaml
name: Deploy STAGE Tier

on:
  push:
    branches:
      - main  # Or develop branch if using gitflow
  workflow_dispatch:  # Allow manual trigger from GitHub UI
    inputs:
      platform:
        description: 'Platform to deploy'
        required: true
        default: 'both'
        type: choice
        options:
          - ios
          - android
          - both

jobs:
  deploy-stage:
    name: Deploy STAGE to TestFlight and Play Console
    runs-on: macos-latest  # macOS required for iOS builds

    steps:
      # ========================================================================
      # STEP 1: Checkout code
      # ========================================================================
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0  # Full history for proper versioning

      # ========================================================================
      # STEP 2: Set up Ruby environment
      # ========================================================================
      - name: Set up Ruby
        uses: ruby/setup-ruby@v1
        with:
          ruby-version: '3.3'
          bundler-cache: true  # Automatically runs bundle install

      # ========================================================================
      # STEP 3: Set up Node.js (if needed for tests)
      # ========================================================================
      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'

      # ========================================================================
      # STEP 4: Install dependencies
      # ========================================================================
      - name: Install npm dependencies
        run: npm ci

      - name: Install Ruby dependencies
        run: bundle install

      # ========================================================================
      # STEP 5: Set up iOS credentials
      # ========================================================================
      - name: Set up iOS certificates
        env:
          ASC_KEY_ID: ${{ secrets.ASC_KEY_ID }}
          ASC_ISSUER_ID: ${{ secrets.ASC_ISSUER_ID }}
          ASC_KEY_CONTENT: ${{ secrets.ASC_KEY_CONTENT }}
        run: |
          mkdir -p ~/.fastlane
          echo "$ASC_KEY_CONTENT" | base64 --decode > ~/.fastlane/AuthKey_${ASC_KEY_ID}.p8
          chmod 600 ~/.fastlane/AuthKey_${ASC_KEY_ID}.p8

      # ========================================================================
      # STEP 6: Set up Android credentials
      # ========================================================================
      - name: Set up Android credentials
        env:
          PLAY_CONSOLE_JSON: ${{ secrets.PLAY_CONSOLE_JSON }}
          ANDROID_KEYSTORE: ${{ secrets.ANDROID_KEYSTORE }}
          ANDROID_KEYSTORE_PASSWORD: ${{ secrets.ANDROID_KEYSTORE_PASSWORD }}
          ANDROID_KEY_PASSWORD: ${{ secrets.ANDROID_KEY_PASSWORD }}
        run: |
          # Play Console service account
          mkdir -p ~/.fastlane
          echo "$PLAY_CONSOLE_JSON" | base64 --decode > ~/.fastlane/play-store-credentials.json
          chmod 600 ~/.fastlane/play-store-credentials.json

          # Android keystore
          mkdir -p ~/keystores
          echo "$ANDROID_KEYSTORE" | base64 --decode > ~/keystores/smilepile-production.keystore
          chmod 600 ~/keystores/smilepile-production.keystore

          # Update keystore.properties
          cat > android/app/keystore.properties << EOF
          storeFile=/Users/runner/keystores/smilepile-production.keystore
          storePassword=${ANDROID_KEYSTORE_PASSWORD}
          keyAlias=smilepile-prod
          keyPassword=${ANDROID_KEY_PASSWORD}
          EOF
          chmod 600 android/app/keystore.properties

      # ========================================================================
      # STEP 7: Run tests
      # ========================================================================
      - name: Run tiered tests
        run: |
          # Determine platform (workflow_dispatch input or 'both')
          PLATFORM="${{ github.event.inputs.platform || 'both' }}"

          # Run tests via deploy script library
          if [[ -f ./deploy/lib/run_tests.sh ]]; then
            ./deploy/lib/run_tests.sh "$PLATFORM"
          else
            echo "Warning: Test script not found, skipping tests"
          fi

      # ========================================================================
      # STEP 8: Deploy iOS STAGE
      # ========================================================================
      - name: Deploy iOS STAGE
        if: github.event.inputs.platform != 'android'
        env:
          FASTLANE_USER: ${{ secrets.APPLE_ID }}
          FASTLANE_PASSWORD: ${{ secrets.APPLE_PASSWORD }}
          ASC_KEY_ID: ${{ secrets.ASC_KEY_ID }}
          ASC_ISSUER_ID: ${{ secrets.ASC_ISSUER_ID }}
        run: |
          cd ios

          # Update Fastfile with API key configuration
          # (API key already set up in step 5)

          bundle exec fastlane stage_ios

      # ========================================================================
      # STEP 9: Deploy Android STAGE
      # ========================================================================
      - name: Deploy Android STAGE
        if: github.event.inputs.platform != 'ios'
        env:
          SUPPLY_JSON_KEY: ${{ secrets.PLAY_CONSOLE_JSON }}
        run: |
          cd android
          bundle exec fastlane stage_android

      # ========================================================================
      # STEP 10: Upload build artifacts (for debugging)
      # ========================================================================
      - name: Upload iOS build logs
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: ios-build-logs
          path: ios/build/logs/
          retention-days: 7

      - name: Upload Android build logs
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: android-build-logs
          path: android/app/build/outputs/logs/
          retention-days: 7

      # ========================================================================
      # STEP 11: Notify success
      # ========================================================================
      - name: Notify deployment success
        if: success()
        run: |
          echo "========================================="
          echo "✅ STAGE Deployment Successful"
          echo "========================================="
          echo "iOS TestFlight: https://appstoreconnect.apple.com"
          echo "Android Play Console: https://play.google.com/console"
          echo "========================================="

      # ========================================================================
      # STEP 12: Notify failure
      # ========================================================================
      - name: Notify deployment failure
        if: failure()
        run: |
          echo "========================================="
          echo "❌ STAGE Deployment Failed"
          echo "========================================="
          echo "Check workflow logs for details"
          echo "Build logs uploaded as artifacts"
          echo "========================================="
```

**GitHub Secrets Configuration**:

Navigate to: GitHub Repository → Settings → Secrets and variables → Actions

**Required Secrets**:

1. **APPLE_ID**: Apple Developer email (e.g., developer@smilepile.com)
2. **APPLE_PASSWORD**: App-specific password (NOT Apple ID password)
   - Generate: appleid.apple.com → Security → App-Specific Passwords
3. **ASC_KEY_ID**: App Store Connect API Key ID (e.g., ABC123XYZ)
4. **ASC_ISSUER_ID**: App Store Connect Issuer ID (UUID format)
5. **ASC_KEY_CONTENT**: App Store Connect API Key (base64 encoded .p8 file)
6. **PLAY_CONSOLE_JSON**: Play Console service account JSON (base64 encoded)
7. **ANDROID_KEYSTORE**: Production keystore file (base64 encoded)
8. **ANDROID_KEYSTORE_PASSWORD**: Keystore password
9. **ANDROID_KEY_PASSWORD**: Key password (often same as keystore password)

**Secret Generation Commands**:

```bash
# Base64 encode App Store Connect API key
base64 < ~/.fastlane/AuthKey_XXXXXXXXXX.p8 | pbcopy
# Paste as ASC_KEY_CONTENT

# Base64 encode Play Console service account
base64 < ~/.fastlane/play-store-credentials.json | pbcopy
# Paste as PLAY_CONSOLE_JSON

# Base64 encode Android keystore
base64 < ~/keystores/smilepile-production.keystore | pbcopy
# Paste as ANDROID_KEYSTORE
```

**Implementation Steps**:

1. **Create workflow directory**:
   ```bash
   mkdir -p .github/workflows
   ```

2. **Create workflow file**:
   ```bash
   touch .github/workflows/deploy-stage.yml
   # Add content above
   ```

3. **Configure GitHub Secrets** (see list above)

4. **Commit and push workflow**:
   ```bash
   git add .github/workflows/deploy-stage.yml
   git commit -m "Add STAGE deployment workflow"
   git push
   ```

5. **Test manual trigger**:
   - Navigate to GitHub repository
   - Click "Actions" tab
   - Select "Deploy STAGE Tier" workflow
   - Click "Run workflow"
   - Select platform (ios/android/both)
   - Click "Run workflow"

6. **Monitor workflow execution**:
   - Watch logs in real-time
   - Check for errors
   - Verify uploads in App Store Connect and Play Console

7. **Test automatic trigger**:
   - Push commit to main branch
   - Workflow should trigger automatically
   - Verify deployment succeeds

**Verification Checklist**:
- [ ] Workflow file created
- [ ] All 9 GitHub Secrets configured
- [ ] Manual workflow trigger works
- [ ] Automatic workflow trigger works
- [ ] Tests run in CI
- [ ] iOS uploads to TestFlight from CI
- [ ] Android uploads to Play Console from CI
- [ ] Build logs available as artifacts
- [ ] Success/failure notifications work

## Implementation Checklist

### Phase A: iOS Fastlane Setup (60 minutes)

- [ ] Create ios/fastlane/ directory
- [ ] Create ios/fastlane/Appfile with Apple ID and team ID
- [ ] Create ios/fastlane/Fastfile with qual_ios lane
- [ ] Test syntax: `cd ios && bundle exec fastlane lanes`
- [ ] Test qual_ios lane: `cd ios && bundle exec fastlane qual_ios`
- [ ] Verify .app bundle created
- [ ] Add stage_ios lane to Fastfile
- [ ] Add beta_ios lane to Fastfile
- [ ] Add prod_ios lane to Fastfile
- [ ] Configure App Store Connect API key in Fastfile
- [ ] Test stage_ios lane: `cd ios && bundle exec fastlane stage_ios`
- [ ] Verify upload in App Store Connect

### Phase B: Android Fastlane Setup (60 minutes)

- [ ] Create android/fastlane/ directory
- [ ] Create android/fastlane/Appfile with package name
- [ ] Create android/fastlane/Fastfile with qual_android lane
- [ ] Test syntax: `cd android && bundle exec fastlane lanes`
- [ ] Test qual_android lane: `cd android && bundle exec fastlane qual_android`
- [ ] Verify APK created
- [ ] Add stage_android lane to Fastfile
- [ ] Add beta_android lane to Fastfile
- [ ] Add prod_android lane to Fastfile
- [ ] Configure Play Console service account in Appfile
- [ ] Verify production keystore configured
- [ ] Test stage_android lane: `cd android && bundle exec fastlane stage_android`
- [ ] Verify upload in Play Console

### Phase C: Gemfile (10 minutes)

- [ ] Create Gemfile in project root
- [ ] Add fastlane dependency (~> 2.228.0)
- [ ] Check if CocoaPods needed (look for ios/Podfile)
- [ ] Run: `bundle install`
- [ ] Verify Gemfile.lock created
- [ ] Test: `bundle exec fastlane --version`
- [ ] Commit Gemfile and Gemfile.lock
- [ ] Test: `bundle check`

### Phase D: Deploy Script Integration (90 minutes)

- [ ] Back up deploy/deploy_qual.sh
- [ ] Update deploy_qual.sh: Replace xcodebuild with fastlane qual_ios
- [ ] Update deploy_qual.sh: Replace gradlew with fastlane qual_android
- [ ] Test deploy_qual.sh dry run
- [ ] Test deploy_qual.sh real execution
- [ ] Verify quality gates still enforce
- [ ] Verify version increments correctly
- [ ] Verify git commits still work
- [ ] Create deploy/deploy_stage.sh from template
- [ ] Make deploy_stage.sh executable
- [ ] Test deploy_stage.sh dry run
- [ ] Test deploy_stage.sh real execution
- [ ] Create deploy/deploy_beta.sh from template
- [ ] Make deploy_beta.sh executable
- [ ] Test deploy_beta.sh dry run
- [ ] Update/create deploy/deploy_prod.sh
- [ ] Add safety confirmation to deploy_prod.sh
- [ ] Test deploy_prod.sh dry run

### Phase E: Credentials (30 minutes)

- [ ] Generate App Store Connect API key
- [ ] Download .p8 file
- [ ] Note Key ID and Issuer ID
- [ ] Store .p8 in ~/.fastlane/
- [ ] Set permissions: chmod 600
- [ ] Back up .p8 file (USB + cloud)
- [ ] Update ios/fastlane/Fastfile with API key configuration
- [ ] Generate/verify Play Console service account
- [ ] Download service account JSON
- [ ] Store JSON in ~/.fastlane/play-store-credentials.json
- [ ] Set permissions: chmod 600
- [ ] Back up service account JSON
- [ ] Verify production keystore exists or generate
- [ ] Back up production keystore (3 locations)
- [ ] Update android/app/keystore.properties
- [ ] Verify keystore.properties in .gitignore

### Phase F: Testing (60 minutes)

- [ ] Test iOS syntax validation (fastlane lanes)
- [ ] Test Android syntax validation (fastlane lanes)
- [ ] Test iOS qual_ios lane
- [ ] Test Android qual_android lane
- [ ] Test iOS stage_ios lane (requires credentials)
- [ ] Test Android stage_android lane (requires credentials)
- [ ] Test iOS beta_ios lane
- [ ] Test Android beta_android lane
- [ ] Test deploy_qual.sh with Fastlane
- [ ] Test deploy_stage.sh with Fastlane
- [ ] Test deploy_beta.sh with Fastlane
- [ ] Verify version increments correctly
- [ ] Verify TestFlight uploads appear
- [ ] Verify Play Console uploads appear
- [ ] Test edge case: Missing credentials
- [ ] Test edge case: Test failures block deployment
- [ ] Test edge case: Build failures
- [ ] Test edge case: Platform selection (ios/android/both)
- [ ] Test edge case: macOS check for iOS

### Phase G: CI/CD (60 minutes)

- [ ] Create .github/workflows/ directory
- [ ] Create .github/workflows/deploy-stage.yml
- [ ] Configure GitHub Secret: APPLE_ID
- [ ] Configure GitHub Secret: APPLE_PASSWORD
- [ ] Configure GitHub Secret: ASC_KEY_ID
- [ ] Configure GitHub Secret: ASC_ISSUER_ID
- [ ] Configure GitHub Secret: ASC_KEY_CONTENT (base64 encoded)
- [ ] Configure GitHub Secret: PLAY_CONSOLE_JSON (base64 encoded)
- [ ] Configure GitHub Secret: ANDROID_KEYSTORE (base64 encoded)
- [ ] Configure GitHub Secret: ANDROID_KEYSTORE_PASSWORD
- [ ] Configure GitHub Secret: ANDROID_KEY_PASSWORD
- [ ] Commit workflow file
- [ ] Test manual workflow trigger
- [ ] Test automatic workflow trigger (push to main)
- [ ] Verify tests run in CI
- [ ] Verify iOS uploads from CI
- [ ] Verify Android uploads from CI
- [ ] Verify build logs available as artifacts

### Phase H: Documentation (30 minutes)

- [ ] Create wave-evidence/wave-5/03-implementation-log.md
- [ ] Document all file creations
- [ ] Document all file modifications
- [ ] Document credential setup steps
- [ ] Create wave-evidence/wave-5/04-testing-validation.md
- [ ] Document all test results
- [ ] Include screenshots of uploads
- [ ] Create wave-evidence/wave-5/WAVE-5-COMPLETE.md
- [ ] Update docs/deployment-handoff/fastlane-configuration.md
- [ ] Update README if needed

## Risk Mitigation

### Risk 1: Credential Configuration Errors

**Impact**: HIGH - Deployments fail if credentials missing or incorrect

**Probability**: MEDIUM - First-time setup is error-prone

**Mitigation Strategies**:

1. **Test QUAL lanes first** (no credentials needed)
   - Validates Fastfile syntax
   - Validates build process
   - Ensures local environment correct

2. **Verify credentials before using**:
   ```bash
   # iOS: Check API key exists
   ls -la ~/.fastlane/AuthKey_*.p8

   # Android: Check service account exists
   ls -la ~/.fastlane/play-store-credentials.json

   # Android: Check keystore exists
   cat android/app/keystore.properties
   ```

3. **Use clear error messages**:
   - Fastlane will show clear errors if credentials missing
   - Add verification steps to deploy scripts

4. **Keep manual fallback**:
   - Original xcodebuild/gradlew commands preserved in backups
   - Can revert if Fastlane issues persist

**Detection**: Deployment fails with authentication errors

**Recovery**: Fix credential configuration, retry deployment

### Risk 2: Code Signing Issues (iOS)

**Impact**: HIGH - iOS builds fail if signing incorrect

**Probability**: MEDIUM - Common first-time setup issue

**Mitigation Strategies**:

1. **Use automatic signing initially**:
   - Let Xcode/Fastlane handle provisioning
   - Manual signing only if required

2. **Test in Xcode first**:
   - Build in Xcode GUI before Fastlane
   - Ensures signing configured correctly

3. **Fastlane prompts for credentials**:
   - Will ask for Apple ID password if needed
   - Interactive authentication available

4. **Use App Store Connect API key**:
   - Bypasses interactive authentication
   - More reliable for CI/CD

**Detection**: Build fails with code signing errors

**Recovery**:
- Sign in Xcode first
- Download provisioning profiles manually
- Configure API key correctly

### Risk 3: Version Number Conflicts

**Impact**: MEDIUM - Version numbers could get out of sync

**Probability**: LOW - build_number.sh is preserved

**Mitigation Strategies**:

1. **DO NOT use Fastlane version actions**:
   - No `increment_build_number`
   - No `increment_version_number`
   - Fastlane only reads versions, never writes

2. **Keep build_number.sh logic**:
   - All version management via existing scripts
   - Fastlane just uses the versions

3. **Test version increments thoroughly**:
   - Verify .build_number updates
   - Verify iOS Info.plist updates
   - Verify Android build.gradle.kts updates

4. **Document version management**:
   - Clear in planning: Fastlane doesn't manage versions
   - Clear in implementation

**Detection**: Version numbers don't increment or mismatch

**Recovery**: Manually fix version numbers, investigate build_number.sh

### Risk 4: Upload Failures

**Impact**: MEDIUM - Builds succeed but uploads fail

**Probability**: LOW - Fastlane handles uploads well

**Mitigation Strategies**:

1. **Start with internal testing tracks**:
   - Lower stakes than production
   - Can retry without consequences

2. **Fastlane has automatic retry logic**:
   - Retries failed uploads automatically
   - Handles transient network issues

3. **Manual upload fallback**:
   - Can always upload via App Store Connect GUI
   - Can always upload via Play Console GUI

4. **Monitor upload logs**:
   - Fastlane shows detailed upload progress
   - Logs saved to build/logs/

**Detection**: Upload fails after successful build

**Recovery**:
- Check credentials
- Check network connectivity
- Retry with Fastlane
- Manual upload if needed

### Risk 5: Breaking Existing Quality Gates

**Impact**: HIGH - Could bypass tests or validation

**Probability**: LOW - Minimal changes to quality gates

**Mitigation Strategies**:

1. **Preserve all quality gate logic**:
   - Only replace build commands
   - Tests, SonarCloud, version management unchanged

2. **Test quality gates thoroughly**:
   - Verify tests still block deployment
   - Verify version management still works
   - Verify git commits still happen

3. **Environment variables preserved**:
   - SKIP_TESTS still works
   - SKIP_COMMIT still works
   - DRY_RUN still works

4. **Compare git diff**:
   - Verify only build commands changed
   - No unintended modifications

**Detection**: Tests don't run, versions don't increment, commits don't happen

**Recovery**: Revert deploy script changes, investigate issue

### Risk 6: CI/CD Integration Issues

**Impact**: MEDIUM - CI deployments fail

**Probability**: MEDIUM - GitHub Actions setup is complex

**Mitigation Strategies**:

1. **Test locally first**:
   - All lanes work locally before CI
   - Credentials validated locally

2. **Use manual workflow trigger initially**:
   - Don't auto-deploy from every commit at first
   - Test manually until stable

3. **Upload build logs as artifacts**:
   - Easy debugging if CI fails
   - Logs available for 7 days

4. **Keep local deployment working**:
   - CI is additive, not replacement
   - Local scripts still work

**Detection**: GitHub Actions workflow fails

**Recovery**:
- Check workflow logs
- Check GitHub Secrets
- Test locally to isolate issue
- Deploy locally if needed

## Success Criteria Verification

After implementation, verify these acceptance criteria from STORY-6.5:

### AC1: iOS Fastlane Configuration

**Verification Steps**:
```bash
# Check Appfile exists
ls -la ios/fastlane/Appfile

# Check Fastfile exists
ls -la ios/fastlane/Fastfile

# List lanes
cd ios && bundle exec fastlane lanes | grep -E "qual_ios|stage_ios|beta_ios|prod_ios"

# Test qual_ios
bundle exec fastlane qual_ios
```

**Pass Criteria**:
- [ ] Appfile exists with correct Apple ID and team ID
- [ ] Fastfile exists with 4 lanes
- [ ] All 4 lanes listed by `fastlane lanes`
- [ ] qual_ios builds successfully

### AC2: Android Fastlane Configuration

**Verification Steps**:
```bash
# Check Appfile exists
ls -la android/fastlane/Appfile

# Check Fastfile exists
ls -la android/fastlane/Fastfile

# List lanes
cd android && bundle exec fastlane lanes | grep -E "qual_android|stage_android|beta_android|prod_android"

# Test qual_android
bundle exec fastlane qual_android
```

**Pass Criteria**:
- [ ] Appfile exists with correct package name and service account
- [ ] Fastfile exists with 4 lanes
- [ ] All 4 lanes listed by `fastlane lanes`
- [ ] qual_android builds successfully

### AC3: QUAL Lane Functionality

**Verification Steps**:
```bash
# Test iOS QUAL
cd ios && bundle exec fastlane qual_ios
ls -la DerivedData/Build/Products/Debug-iphonesimulator/

# Test Android QUAL
cd android && bundle exec fastlane qual_android
ls -la app/build/outputs/apk/qual/debug/
```

**Pass Criteria**:
- [ ] iOS QUAL builds for simulator
- [ ] iOS .app bundle created
- [ ] Android QUAL builds APK
- [ ] Android APK created

### AC4: STAGE Lane Functionality

**Verification Steps**:
```bash
# Test iOS STAGE
cd ios && bundle exec fastlane stage_ios
# Check App Store Connect: https://appstoreconnect.apple.com

# Test Android STAGE
cd android && bundle exec fastlane stage_android
# Check Play Console: https://play.google.com/console
```

**Pass Criteria**:
- [ ] iOS STAGE uploads to TestFlight Internal
- [ ] Android STAGE uploads to Play Console Internal
- [ ] Builds visible in respective dashboards

### AC5: BETA Lane Functionality

**Verification Steps**:
```bash
# Test iOS BETA
cd ios && bundle exec fastlane beta_ios

# Test Android BETA
cd android && bundle exec fastlane beta_android
```

**Pass Criteria**:
- [ ] iOS BETA uploads to TestFlight External
- [ ] Android BETA uploads to Play Console Closed Testing
- [ ] Builds visible in respective dashboards

### AC6: PROD Lane Functionality

**Verification Steps**:
```bash
# Test iOS PROD
cd ios && bundle exec fastlane prod_ios

# Test Android PROD
cd android && bundle exec fastlane prod_android
```

**Pass Criteria**:
- [ ] iOS PROD uploads to App Store Connect (no auto-submit)
- [ ] Android PROD uploads to Play Console as draft
- [ ] Both require manual approval for release

### AC7: Deploy Script Integration

**Verification Steps**:
```bash
# Test updated deploy_qual.sh
./deploy/deploy_qual.sh both

# Test new deploy_stage.sh
./deploy/deploy_stage.sh both

# Test new deploy_beta.sh
./deploy/deploy_beta.sh both

# Test updated deploy_prod.sh
./deploy/deploy_prod.sh both
```

**Pass Criteria**:
- [ ] deploy_qual.sh uses Fastlane
- [ ] deploy_stage.sh works end-to-end
- [ ] deploy_beta.sh works end-to-end
- [ ] deploy_prod.sh requires manual confirmation
- [ ] All quality gates still enforce
- [ ] Version management still works

### AC8: Version Management Preserved

**Verification Steps**:
```bash
# Check version before deployment
cat .build_number

# Run deployment
./deploy/deploy_qual.sh both

# Check version after deployment
cat .build_number

# Verify iOS updated
/usr/libexec/PlistBuddy -c "Print :CFBundleVersion" ios/SmilePile/Info.plist

# Verify Android updated
grep "versionCode" android/app/build.gradle.kts
```

**Pass Criteria**:
- [ ] .build_number increments
- [ ] iOS Info.plist matches
- [ ] Android build.gradle.kts matches
- [ ] build_number.sh logic unchanged

### AC9: CI/CD Integration

**Verification Steps**:
```bash
# Check workflow exists
ls -la .github/workflows/deploy-stage.yml

# Check Gemfile exists
ls -la Gemfile Gemfile.lock

# Trigger manual workflow in GitHub Actions
# Check logs in GitHub Actions UI
```

**Pass Criteria**:
- [ ] Workflow file exists
- [ ] Gemfile and Gemfile.lock committed
- [ ] Manual workflow trigger works
- [ ] Automatic workflow trigger works
- [ ] Deployments succeed from CI

### AC10: Documentation Complete

**Verification Steps**:
```bash
# Check planning document exists
ls -la wave-evidence/wave-5/02-technical-planning.md

# Check implementation log exists
ls -la wave-evidence/wave-5/03-implementation-log.md

# Check testing validation exists
ls -la wave-evidence/wave-5/04-testing-validation.md

# Check completion document exists
ls -la wave-evidence/wave-5/WAVE-5-COMPLETE.md
```

**Pass Criteria**:
- [ ] Planning document complete
- [ ] Implementation log complete
- [ ] Testing validation complete
- [ ] Completion document complete
- [ ] All steps documented

### AC11: No Regressions

**Verification Steps**:
```bash
# Test existing quality gates
./deploy/deploy_qual.sh both
# Verify tests run

# Test environment variables
SKIP_TESTS=true ./deploy/deploy_qual.sh both
# Verify tests skipped

SKIP_COMMIT=true ./deploy/deploy_qual.sh both
# Verify no commit

DRY_RUN=true ./deploy/deploy_qual.sh both
# Verify dry run

# Test SonarCloud integration (if configured)
# Verify SonarCloud analysis runs
```

**Pass Criteria**:
- [ ] All existing quality gates still work
- [ ] Tests still block deployment on failure
- [ ] Environment variables still work
- [ ] Git commits still work
- [ ] SonarCloud integration still works
- [ ] No functionality lost

## File Summary

### New Files Created (10 files)

1. **ios/fastlane/Appfile** - iOS configuration (Apple ID, team ID)
2. **ios/fastlane/Fastfile** - iOS deployment lanes (4 lanes)
3. **android/fastlane/Appfile** - Android configuration (package, service account)
4. **android/fastlane/Fastfile** - Android deployment lanes (4 lanes)
5. **Gemfile** - Ruby dependency management
6. **Gemfile.lock** - Locked dependency versions (generated)
7. **deploy/deploy_stage.sh** - STAGE tier deployment script
8. **deploy/deploy_beta.sh** - BETA tier deployment script
9. **.github/workflows/deploy-stage.yml** - CI/CD automation (optional)
10. **wave-evidence/wave-5/02-technical-planning.md** - This document

### Files Modified (4 files)

1. **deploy/deploy_qual.sh**
   - Replace `xcodebuild` with `bundle exec fastlane qual_ios`
   - Replace `./gradlew assembleQualDebug` with `bundle exec fastlane qual_android`
   - Lines ~382-549 (Android and iOS deployment functions)

2. **deploy/deploy_prod.sh**
   - Add safety confirmation prompt
   - Replace build commands with Fastlane prod lanes
   - Ensure draft/manual submission mode

3. **android/app/keystore.properties** (if needed)
   - Update with production keystore path
   - Update with keystore passwords
   - Ensure correct permissions (600)

4. **.gitignore** (verify only)
   - Ensure ~/.fastlane/ ignored (credential files)
   - Ensure keystore.properties ignored
   - Ensure build artifacts ignored

### Files Referenced (No Changes)

These files are referenced during implementation but not modified:

1. **ios/SmilePile.xcodeproj/project.pbxproj** - iOS project configuration
2. **ios/SmilePile/Info.plist** - iOS app metadata (read for version)
3. **ios/Qual.xcconfig** - QUAL tier configuration
4. **ios/Stage.xcconfig** - STAGE tier configuration
5. **ios/Beta.xcconfig** - BETA tier configuration
6. **ios/Prod.xcconfig** - PROD tier configuration
7. **android/app/build.gradle.kts** - Android build configuration (read for version)
8. **deploy/lib/build_number.sh** - Version management (DO NOT MODIFY)
9. **deploy/lib/common.sh** - Common deploy functions
10. **deploy/lib/run_tests.sh** - Test execution
11. **backlog/sprint-6/STORY-6.5-fastlane-automation.md** - Requirements

## Next Steps

After this planning document is approved:

### Phase 4: Security Review + Peer Review (Parallel)

**Security Agent Tasks**:
- Review credential storage approach
- Review .gitignore configuration
- Review secret handling in CI/CD
- Review keystore backup strategy
- Identify security risks in implementation

**Peer-Reviewer Agent Tasks**:
- Review technical approach
- Review Fastlane lane implementations
- Review deploy script modifications
- Verify quality gates preserved
- Identify technical risks

**Expected Output**: Security review + peer review reports

### Phase 5: Implementation (Developer Agent)

**Tasks**:
- Follow this planning document step-by-step
- Create all 10 new files
- Modify 4 existing files
- Configure credentials
- Test all 8 Fastlane lanes
- Test all 4 deploy scripts
- Document implementation in 03-implementation-log.md

**Expected Output**: All files created/modified, local testing complete

### Phase 6: Testing + UX Validation (Parallel)

**UX-Analyst Tasks**:
- Test user-facing workflows
- Verify dashboard uploads
- Test error messages
- Verify success messages

**Peer-Reviewer Tasks**:
- Execute full test suite
- Verify all acceptance criteria
- Test edge cases
- Document test results in 04-testing-validation.md

**Expected Output**: Testing validation report

### Phase 7: Validation (Product Manager)

**Tasks**:
- Verify all 11 acceptance criteria met
- Verify all deliverables present
- Verify quality gates preserved
- Approve for deployment

**Expected Output**: Validation approval

### Phase 8: Clean-up (General-Purpose Agent)

**Tasks**:
- Organize wave-evidence files
- Create WAVE-5-COMPLETE.md
- Update deployment documentation
- Create handoff documentation

**Expected Output**: Complete documentation package

### Phase 9: Deployment (DevOps Agent)

**Tasks**:
- Deploy to QUAL tier
- Deploy to STAGE tier
- Deploy to BETA tier
- Verify all uploads
- Document deployment results

**Expected Output**: All tiers deployed successfully

---

## Estimated Timeline

**Total Implementation Time**: 6 hours (Phase 5 only)

**Breakdown**:
- iOS Fastlane Setup: 60 minutes
- Android Fastlane Setup: 60 minutes
- Gemfile Creation: 10 minutes
- Deploy Script Integration: 90 minutes
- Credentials Configuration: 30 minutes
- CI/CD Integration: 60 minutes
- Testing: 60 minutes
- Documentation: 30 minutes

**Total Wave 5 Time** (all phases): ~12 hours

**Breakdown**:
- Phase 1 (Research): 1 hour - COMPLETE
- Phase 2 (Story Creation): 1 hour - COMPLETE
- Phase 3 (Planning): 2 hours - THIS DOCUMENT
- Phase 4 (Security + Peer Review): 1 hour
- Phase 5 (Implementation): 6 hours
- Phase 6 (Testing + UX): 1 hour
- Phase 7 (Validation): 30 minutes
- Phase 8 (Clean-up): 30 minutes
- Phase 9 (Deployment): 30 minutes

---

**Planning Document Complete**
**Status**: Ready for Phase 4 (Security Review + Peer Review)
**Next Action**: Run security and peer-reviewer agents in parallel
**Ready**: ✅ YES
