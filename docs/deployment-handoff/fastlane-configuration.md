# Fastlane Configuration Guide

Complete fastlane setup for automated 4-tier deployments across iOS, Android, and Web platforms.

## Overview

Fastlane automates iOS and Android build and deployment workflows. This guide covers installation, Fastfile structure, lane definitions, credential management, and deployment summaries. Budget 3-4 hours for complete setup.

## Why Fastlane?

**Without fastlane**: Manual Xcode archiving, exporting, uploading to TestFlight, gradle commands, Play Console uploads.

**With fastlane**: One command deploys to the correct tier with proper signing, metadata, and distribution.

```bash
# Before: 10+ manual steps
# After:
fastlane qual_ios
```

## Installation

### Install Fastlane

**Option 1: Homebrew (macOS, recommended)**
```bash
brew install fastlane
```

**Option 2: RubyGems**
```bash
sudo gem install fastlane -NV
```

**Option 3: Bundler (project-local)**
```bash
# Create Gemfile in project root
echo 'source "https://rubygems.org"' > Gemfile
echo 'gem "fastlane"' >> Gemfile

# Install
bundle install

# Use via bundle exec
bundle exec fastlane [lane]
```

### Verify Installation

```bash
fastlane --version
# Expected: fastlane 2.210.0 or higher
```

## Project Structure

Fastlane requires separate configurations for iOS and Android:

```
project-root/
├── ios/
│   └── fastlane/
│       ├── Fastfile          # iOS lanes and actions
│       ├── Appfile           # iOS app identifiers
│       └── README.md         # Auto-generated documentation
├── android/
│   └── fastlane/
│       ├── Fastfile          # Android lanes and actions
│       ├── Appfile           # Android package names
│       └── README.md         # Auto-generated documentation
└── scripts/
    └── deploy.sh             # Master deployment script (calls fastlane)
```

## iOS Fastlane Configuration

### Initialize iOS Fastlane

```bash
cd ios
fastlane init

# Choose option 2: "Automate beta distribution to TestFlight"
# Enter Apple ID: your@email.com
# Enter App Identifier: com.[YOUR_COMPANY].[YOUR_APP]
# Follow prompts
```

This creates `ios/fastlane/Fastfile` and `ios/fastlane/Appfile`.

### ios/fastlane/Appfile

```ruby
# Apple Developer account
apple_id("your@email.com")

# Team ID (find at https://developer.apple.com → Account → Membership)
team_id("YOUR_TEAM_ID")

# iTunes Connect Team ID (usually same as team_id)
itc_team_id("YOUR_TEAM_ID")

# Default app identifier (can be overridden in lanes)
app_identifier("com.[YOUR_COMPANY].[YOUR_APP]")
```

### ios/fastlane/Fastfile

Complete Fastfile with all four tiers:

```ruby
default_platform(:ios)

platform :ios do

  # QUAL LANE - Build for simulator (local testing)
  desc "Build QUAL for simulator testing"
  lane :qual_ios do
    # Ensure clean build
    clean_build_artifacts

    # Increment build number (optional, for tracking)
    increment_build_number(xcodeproj: "StackMapNative.xcodeproj")

    # Build for simulator
    build_app(
      scheme: "[YOUR_APP] Qual",
      configuration: "Debug",
      xcconfig: "Qual.xcconfig",
      skip_package_ipa: true,  # Simulator build, no IPA needed
      sdk: "iphonesimulator",
      derived_data_path: "build/qual",
      output_directory: "build/qual",
      clean: true
    )

    puts "QUAL build complete! Install on simulator:"
    puts "  xcrun simctl install booted build/qual/[YOUR_APP].app"
  end

  # STAGE LANE - TestFlight Internal Testing
  desc "Build and upload STAGE to TestFlight Internal Testing"
  lane :stage_ios do
    # Ensure clean build
    clean_build_artifacts

    # Increment build number
    increment_build_number(xcodeproj: "[YOUR_APP].xcodeproj")

    # Sync code signing (if using match)
    # match(type: "appstore", readonly: true, app_identifier: "com.[YOUR_COMPANY].[YOUR_APP]")

    # Build for device
    build_app(
      scheme: "[YOUR_APP] Stage",
      configuration: "Release",
      xcconfig: "Stage.xcconfig",
      export_method: "app-store",
      derived_data_path: "build/stage",
      output_directory: "build/stage",
      clean: true
    )

    # Upload to TestFlight
    upload_to_testflight(
      skip_waiting_for_build_processing: true,  # Don't wait for Apple processing
      distribute_external: false,                # Internal only
      groups: ["Internal Testers"],             # TestFlight group name
      changelog: read_changelog,                # Read from CHANGELOG or PENDING_CHANGES
      notify_external_testers: false
    )

    puts "STAGE uploaded to TestFlight Internal Testing!"
    puts "Check status: https://appstoreconnect.apple.com"
  end

  # BETA LANE - TestFlight External Testing
  desc "Build and upload BETA to TestFlight External Testing"
  lane :beta_ios do
    # Ensure clean build
    clean_build_artifacts

    # Increment build number
    increment_build_number(xcodeproj: "[YOUR_APP].xcodeproj")

    # Sync code signing (if using match)
    # match(type: "appstore", readonly: true, app_identifier: "com.[YOUR_COMPANY].[YOUR_APP]")

    # Build for device
    build_app(
      scheme: "[YOUR_APP] Beta",
      configuration: "Release",
      xcconfig: "Beta.xcconfig",
      export_method: "app-store",
      derived_data_path: "build/beta",
      output_directory: "build/beta",
      clean: true
    )

    # Upload to TestFlight
    upload_to_testflight(
      skip_waiting_for_build_processing: true,
      distribute_external: true,                # External testers
      groups: ["Beta Testers"],                 # TestFlight group name
      changelog: read_changelog,
      notify_external_testers: true             # Email notification
    )

    puts "BETA uploaded to TestFlight External Testing!"
    puts "External testers will receive email notification."
  end

  # PROD LANE - App Store Production
  desc "Build and upload PROD to App Store"
  lane :prod_ios do
    # Ensure clean build
    clean_build_artifacts

    # Increment build number
    increment_build_number(xcodeproj: "[YOUR_APP].xcodeproj")

    # Sync code signing (if using match)
    # match(type: "appstore", readonly: true, app_identifier: "com.[YOUR_COMPANY].[YOUR_APP]")

    # Build for device
    build_app(
      scheme: "[YOUR_APP] Prod",
      configuration: "Release",
      xcconfig: "Prod.xcconfig",
      export_method: "app-store",
      derived_data_path: "build/prod",
      output_directory: "build/prod",
      clean: true
    )

    # Upload to App Store (but don't auto-submit)
    upload_to_app_store(
      skip_metadata: true,           # Don't update metadata
      skip_screenshots: true,        # Don't update screenshots
      submit_for_review: false,      # Manual submission
      force: true,                   # Skip HTML report verification
      precheck_include_in_app_purchases: false
    )

    puts "PROD uploaded to App Store Connect!"
    puts "Ready for manual submission: https://appstoreconnect.apple.com"
  end

  # HELPER: Read changelog from PENDING_CHANGES.md
  def read_changelog
    changelog_path = "../PENDING_CHANGES.md"
    if File.exist?(changelog_path)
      # Read first section (up to next ## or end of file)
      content = File.read(changelog_path)
      sections = content.split(/^## /)
      return sections[1].split("\n")[1..-1].join("\n").strip if sections.size > 1
    end
    "No changelog available"
  end

  # HELPER: Clean build artifacts
  lane :clean_build_artifacts do
    sh("rm -rf ../build/")
  end
end
```

**StackMap Reference**: See `/ios/fastlane/Fastfile` for production implementation with error handling and advanced features.

### iOS Fastlane Plugins

Useful plugins for iOS deployments:

```bash
# Install plugins
fastlane add_plugin increment_build_number
fastlane add_plugin badge  # Add badge to app icon (QUAL, STAGE, BETA)

# Use badge plugin in lanes
lane :qual_ios do
  badge(
    shield: "QUAL-orange",
    no_badge: false,
    shield_no_resize: true
  )
  # ... rest of lane
end
```

## Android Fastlane Configuration

### Initialize Android Fastlane

```bash
cd android
fastlane init

# Choose option 4: "Manual setup"
# Follow prompts
```

### android/fastlane/Appfile

```ruby
# Package name (base package, used for STAGE/BETA/PROD)
package_name("com.[YOUR_COMPANY].[YOUR_APP]")

# Path to Play Console service account JSON
json_key_file("play-store-credentials.json")
```

### android/fastlane/Fastfile

Complete Fastfile with all four tiers:

```ruby
default_platform(:android)

platform :android do

  # QUAL LANE - Build APK for local testing
  desc "Build QUAL APK for emulator testing"
  lane :qual_android do
    # Clean previous builds
    gradle(
      task: "clean",
      project_dir: "."
    )

    # Build QUAL APK
    gradle(
      task: "assembleQualRelease",
      project_dir: ".",
      properties: {
        "android.injected.signing.store.file" => ENV["QUAL_KEYSTORE_PATH"],
        "android.injected.signing.store.password" => ENV["QUAL_KEYSTORE_PASSWORD"],
        "android.injected.signing.key.alias" => ENV["QUAL_KEY_ALIAS"],
        "android.injected.signing.key.password" => ENV["QUAL_KEY_PASSWORD"]
      }
    )

    apk_path = lane_context[SharedValues::GRADLE_APK_OUTPUT_PATH]
    puts "QUAL APK built: #{apk_path}"
    puts "Install on emulator:"
    puts "  adb install #{apk_path}"
  end

  # STAGE LANE - Upload to Play Console Internal Testing
  desc "Build and upload STAGE to Play Console Internal Testing"
  lane :stage_android do
    # Clean previous builds
    gradle(task: "clean", project_dir: ".")

    # Build STAGE AAB
    gradle(
      task: "bundleStageRelease",
      project_dir: "."
    )

    # Upload to Play Console Internal Testing track
    upload_to_play_store(
      track: "internal",
      aab: "app/build/outputs/bundle/stageRelease/app-stage-release.aab",
      skip_upload_metadata: true,
      skip_upload_changelogs: false,
      skip_upload_images: true,
      skip_upload_screenshots: true,
      release_status: "completed",  # Immediately available to internal testers
      version_code: get_version_code
    )

    puts "STAGE uploaded to Play Console Internal Testing!"
    puts "Internal testers can install via Play Console link."
  end

  # BETA LANE - Upload to Play Console Closed Testing
  desc "Build and upload BETA to Play Console Closed Testing"
  lane :beta_android do
    # Clean previous builds
    gradle(task: "clean", project_dir: ".")

    # Build BETA AAB
    gradle(
      task: "bundleBetaRelease",
      project_dir: "."
    )

    # Upload to Play Console Closed Testing track
    upload_to_play_store(
      track: "beta",  # Or your closed testing track name
      aab: "app/build/outputs/bundle/betaRelease/app-beta-release.aab",
      skip_upload_metadata: true,
      skip_upload_changelogs: false,
      skip_upload_images: true,
      skip_upload_screenshots: true,
      release_status: "completed",
      version_code: get_version_code
    )

    puts "BETA uploaded to Play Console Closed Testing!"
    puts "Beta testers can install immediately (no review required)."
  end

  # PROD LANE - Upload to Play Console Production
  desc "Build and upload PROD to Play Console Production"
  lane :prod_android do
    # Clean previous builds
    gradle(task: "clean", project_dir: ".")

    # Build PROD AAB
    gradle(
      task: "bundleProdRelease",
      project_dir: "."
    )

    # Upload to Play Console Production track
    upload_to_play_store(
      track: "production",
      aab: "app/build/outputs/bundle/prodRelease/app-prod-release.aab",
      skip_upload_metadata: true,
      skip_upload_changelogs: false,
      skip_upload_images: true,
      skip_upload_screenshots: true,
      release_status: "draft",  # Manual rollout control
      version_code: get_version_code
    )

    puts "PROD uploaded to Play Console Production (draft)!"
    puts "Go to Play Console to manually roll out."
  end

  # HELPER: Get version code from gradle
  def get_version_code
    # Read versionCode from build.gradle
    gradle_file = File.read("app/build.gradle")
    version_code = gradle_file.match(/versionCode (\d+)/)[1].to_i
    version_code
  end
end
```

**StackMap Reference**: See `/android/fastlane/Fastfile` for production implementation.

### Android Fastlane Plugins

Useful plugins for Android deployments:

```bash
# Install plugins
fastlane add_plugin increment_version_code

# Use in lanes to auto-increment version code
lane :stage_android do
  increment_version_code(
    gradle_file_path: "app/build.gradle"
  )
  # ... rest of lane
end
```

## Credential Management

### iOS Credentials

**Option 1: App Store Connect API Key (Recommended)**

1. Generate API Key:
   - App Store Connect → Users and Access → Keys
   - Click "+" → Name: "[YOUR_APP] Deployment"
   - Role: Admin or App Manager
   - Download `.p8` file (only available once!)

2. Store securely:
   ```bash
   mkdir -p ~/app-store-connect-api-keys
   mv AuthKey_XXXXXXXXXX.p8 ~/app-store-connect-api-keys/
   chmod 600 ~/app-store-connect-api-keys/AuthKey_XXXXXXXXXX.p8
   ```

3. Reference in Fastfile:
   ```ruby
   app_store_connect_api_key(
     key_id: "XXXXXXXXXX",
     issuer_id: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
     key_filepath: "~/app-store-connect-api-keys/AuthKey_XXXXXXXXXX.p8"
   )
   ```

**Option 2: Apple ID with App-Specific Password**

1. Generate app-specific password:
   - https://appleid.apple.com → Sign In
   - Security → App-Specific Passwords → Generate

2. Store in Fastlane credentials:
   ```bash
   fastlane fastlane-credentials add --username your@email.com
   # Enter app-specific password when prompted
   ```

3. Fastlane automatically uses stored credentials

### Android Credentials

**Play Console Service Account JSON**

1. Create service account (see [android-setup-guide.md](./android-setup-guide.md))

2. Store JSON securely:
   ```bash
   mv play-store-service-account.json android/fastlane/play-store-credentials.json
   chmod 600 android/fastlane/play-store-credentials.json
   ```

3. Add to .gitignore:
   ```bash
   echo "play-store-credentials.json" >> android/fastlane/.gitignore
   ```

4. Reference in Appfile:
   ```ruby
   json_key_file("play-store-credentials.json")
   ```

### Environment Variables (Alternative)

For CI/CD, use environment variables instead of files:

```bash
# iOS
export FASTLANE_USER="your@email.com"
export FASTLANE_PASSWORD="app-specific-password"
export FASTLANE_APPLE_APPLICATION_SPECIFIC_PASSWORD="app-specific-password"

# Android
export SUPPLY_JSON_KEY_DATA="$(cat play-store-credentials.json)"

# Keystores
export QUAL_KEYSTORE_PATH="/path/to/qual.keystore"
export QUAL_KEYSTORE_PASSWORD="password"
export PROD_KEYSTORE_PATH="/path/to/production.keystore"
export PROD_KEYSTORE_PASSWORD="password"
```

## Deployment Summaries

Fastlane can generate HTML summaries of deployments for team visibility.

### Enable Deployment Summary

Add to your lanes:

```ruby
lane :beta_ios do
  # ... build and upload steps

  # Generate summary
  create_deployment_summary(
    tier: "BETA",
    platform: "iOS",
    version: get_version_number(xcodeproj: "[YOUR_APP].xcodeproj"),
    build: get_build_number(xcodeproj: "[YOUR_APP].xcodeproj"),
    changelog: read_changelog
  )
end

# Helper method
def create_deployment_summary(tier:, platform:, version:, build:, changelog:)
  html = <<-HTML
  <!DOCTYPE html>
  <html>
  <head><title>#{tier} Deployment - #{platform}</title></head>
  <body>
    <h1>#{tier} Deployment Summary</h1>
    <p><strong>Platform:</strong> #{platform}</p>
    <p><strong>Version:</strong> #{version} (#{build})</p>
    <p><strong>Deployed:</strong> #{Time.now.strftime("%Y-%m-%d %H:%M:%S")}</p>
    <h2>Changes</h2>
    <pre>#{changelog}</pre>
  </body>
  </html>
  HTML

  File.write("../deployment-summary-#{tier.downcase}-#{platform.downcase}.html", html)
  puts "Deployment summary: deployment-summary-#{tier.downcase}-#{platform.downcase}.html"
end
```

**StackMap Reference**: See `/scripts/deploy/lib/reporting.sh` for bash-based summary generation.

## Integration with Master Deploy Script

Fastlane lanes are called by the master deployment script (`scripts/deploy.sh`).

Example integration:

```bash
# scripts/deploy.sh (simplified)

tier="$1"  # qual, stage, beta, prod
platform="$2"  # --ios, --android, --all

case "$tier" in
  qual)
    if [[ "$platform" == "--ios" ]]; then
      cd ios && fastlane qual_ios
    elif [[ "$platform" == "--android" ]]; then
      cd android && fastlane qual_android
    fi
    ;;
  stage)
    if [[ "$platform" == "--ios" ]]; then
      cd ios && fastlane stage_ios
    elif [[ "$platform" == "--android" ]]; then
      cd android && fastlane stage_android
    fi
    ;;
  # ... etc
esac
```

**StackMap Reference**: See `/scripts/deploy.sh` and tier-specific scripts (`qual_deploy.sh`, `deploy_stage.sh`, etc.)

## Fastlane Best Practices

### 1. Version Your Fastfile

Commit `Fastfile` and `Appfile` to git for team consistency:
```bash
git add ios/fastlane/Fastfile ios/fastlane/Appfile
git add android/fastlane/Fastfile android/fastlane/Appfile
git commit -m "Add fastlane configuration"
```

### 2. Auto-Generate Documentation

Fastlane generates README.md for each platform:
```bash
fastlane docs
```

Commit generated docs for team reference.

### 3. Use Error Handling

Wrap critical steps in error handlers:
```ruby
lane :stage_ios do
  begin
    build_app(...)
    upload_to_testflight(...)
  rescue => exception
    slack(
      message: "STAGE iOS deployment failed: #{exception.message}",
      success: false
    )
    raise exception
  end
end
```

### 4. Parallel Execution

For independent tasks, use `sh` with background execution:
```ruby
lane :deploy_all do
  # Start iOS build in background
  ios_thread = Thread.new { fastlane_lane("stage_ios") }

  # Start Android build in background
  android_thread = Thread.new { fastlane_lane("stage_android") }

  # Wait for both
  ios_thread.join
  android_thread.join
end
```

### 5. Dry Run Mode

Add dry-run flag for testing:
```ruby
lane :stage_ios do |options|
  build_app(...)

  unless options[:dry_run]
    upload_to_testflight(...)
  else
    puts "DRY RUN: Would upload to TestFlight"
  end
end

# Call with: fastlane stage_ios dry_run:true
```

## Common Fastlane Issues

### "Could not find action, lane or variable"

**Solution**: Check spelling and ensure actions are installed:
```bash
fastlane actions  # List available actions
```

### "User credentials invalid"

**Solution**: Re-authenticate:
```bash
fastlane fastlane-credentials remove --username your@email.com
fastlane fastlane-credentials add --username your@email.com
```

### iOS build fails with code signing error

**Solution**: Use match for team consistency:
```bash
cd ios
fastlane match development  # For QUAL
fastlane match appstore     # For STAGE/BETA/PROD
```

### Android upload fails with "Package name mismatch"

**Solution**: Verify `package_name` in Appfile matches Play Console app.

### Fastlane hangs during upload

**Solution**: Add timeout:
```ruby
upload_to_testflight(
  skip_waiting_for_build_processing: true  # Don't wait for Apple processing
)
```

## Next Steps

After configuring fastlane:

1. Set up BUILD_TYPE_ENV in [environment-configuration.md](./environment-configuration.md)
2. Test deployments following [deployment-workflow.md](./deployment-workflow.md)
3. Secure credentials per [secrets-and-credentials.md](./secrets-and-credentials.md)

## StackMap Reference Files

Complete working fastlane configurations:

- `/ios/fastlane/Fastfile` (lanes: qual_ios, stage_ios, beta_ios, prod_ios)
- `/android/fastlane/Fastfile` (lanes: qual_android, stage_android, beta_android, prod_android)
- `/scripts/deploy.sh` (master script calling fastlane)
- `/scripts/deploy/lib/reporting.sh` (deployment summaries)

See [reference-implementations.md](./reference-implementations.md) for complete code examples.
