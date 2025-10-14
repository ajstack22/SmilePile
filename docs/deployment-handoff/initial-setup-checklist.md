# Initial Setup Checklist

Complete one-time setup tasks for your 4-tier deployment system. This should be done before your first deployment.

## Overview

This checklist walks through all prerequisites and one-time configurations. Budget 8-12 hours spread over 1-2 weeks due to approval delays from Apple and Google.

## Phase 1: Account and Store Setup (Week 1, Day 1)

### Apple Developer Account

- [ ] **Enroll in Apple Developer Program** ($99/year)
  - Visit https://developer.apple.com/programs/
  - Complete enrollment (can take 1-3 days for approval)
  - Verify account access to App Store Connect
  - Accept latest agreements in App Store Connect

- [ ] **Create App Store Connect App**
  - Log in to https://appstoreconnect.apple.com
  - Click "My Apps" → "+" → "New App"
  - Select iOS platform
  - Enter app name: `[YOUR_APP_NAME]`
  - Select primary language
  - Choose base bundle ID: `com.[YOUR_COMPANY].[YOUR_APP]`
  - SKU: `[YOUR_APP]-ios` (or your convention)
  - Select "Full Access" for user access

- [ ] **Configure TestFlight**
  - Navigate to TestFlight tab in App Store Connect
  - Create Internal Testing group (for STAGE)
  - Add internal team members as testers
  - Prepare External Testing group info (for BETA)
    - Group name: "[YOUR_APP] Beta Testers"
    - Public link: Enable if desired

### Google Play Console

- [ ] **Create Google Play Developer Account** ($25 one-time)
  - Visit https://play.google.com/console
  - Complete registration
  - Pay one-time fee
  - Verify account access

- [ ] **Create Google Play Console App**
  - Click "Create app" in Play Console
  - Enter app name: `[YOUR_APP_NAME]`
  - Select default language
  - App or game: Select appropriate type
  - Free or paid: Select pricing model
  - Complete declarations and consent
  - Click "Create app"

- [ ] **Configure Play Console Testing Tracks**
  - Navigate to "Testing" → "Internal testing"
  - Create internal testing release (for STAGE)
  - Add internal testers email list
  - Navigate to "Testing" → "Closed testing"
  - Create closed testing track (for BETA)
  - Set up beta tester list or opt-in URL

## Phase 2: iOS Configuration (Week 1, Days 2-3)

### Bundle Identifiers

- [ ] **Register QUAL Bundle ID**
  - Log in to https://developer.apple.com
  - Navigate to "Certificates, Identifiers & Profiles"
  - Click "Identifiers" → "+"
  - Select "App IDs" → "App"
  - Description: `[YOUR_APP] QUAL`
  - Bundle ID: Explicit → `com.[YOUR_COMPANY].[YOUR_APP].qual`
  - Capabilities: Select required capabilities (Push Notifications, etc.)
  - Click "Continue" → "Register"

- [ ] **Verify Base Bundle ID** (should exist from App Store Connect app creation)
  - Check for `com.[YOUR_COMPANY].[YOUR_APP]`
  - Used for STAGE, BETA, and PROD
  - Update capabilities if needed

### Code Signing

- [ ] **Generate Distribution Certificate**
  ```bash
  # Let fastlane handle this during first deployment
  # OR manually create in Apple Developer Portal:
  # Certificates → "+" → "Apple Distribution" → Follow prompts
  ```
  - Save `.p12` file securely (backup!)
  - Record password in secure location

- [ ] **Create App Store Connect API Key** (recommended for automation)
  - App Store Connect → Users and Access → Keys
  - Click "+" to generate new key
  - Name: `[YOUR_APP] Deployment`
  - Access: Admin or App Manager
  - Download `.p8` file (only available once!)
  - Record Issuer ID and Key ID
  - Store `.p8` file securely

### Provisioning Profiles

- [ ] **Create Provisioning Profiles**
  ```bash
  # Fastlane will auto-generate during first deployment
  # Profiles needed:
  # - com.[YOUR_COMPANY].[YOUR_APP].qual (QUAL)
  # - com.[YOUR_COMPANY].[YOUR_APP] (STAGE/BETA/PROD)
  ```
  - Ad Hoc profile for QUAL (simulator testing)
  - App Store profile for STAGE/BETA/PROD

## Phase 3: Android Configuration (Week 1, Days 3-4)

### Package Names

- [ ] **Verify Package Name in Play Console**
  - Navigate to your app in Play Console
  - Dashboard → "App details"
  - Package name: `com.[YOUR_COMPANY].[YOUR_APP]` (base)
  - This is used for STAGE, BETA, and PROD

- [ ] **Plan QUAL Package Name**
  - QUAL will use: `com.[YOUR_COMPANY].[YOUR_APP].qual`
  - This is only for local testing (never uploaded to Play Console)

### Keystore Setup

- [ ] **Generate Production Keystore**
  ```bash
  keytool -genkey -v -keystore [YOUR_APP]-production.keystore \
    -alias [YOUR_APP]-production \
    -keyalg RSA -keysize 2048 -validity 10000

  # Enter password (record securely!)
  # Enter details (name, org, city, state, country)
  # Confirm details
  ```
  - Store keystore file securely (BACKUP MULTIPLE LOCATIONS!)
  - Record password, alias, and key password
  - This keystore is used for STAGE, BETA, and PROD

- [ ] **Generate QUAL Keystore** (optional, for consistency)
  ```bash
  keytool -genkey -v -keystore [YOUR_APP]-qual.keystore \
    -alias [YOUR_APP]-qual \
    -keyalg RSA -keysize 2048 -validity 10000
  ```
  - Used for QUAL builds only (local testing)
  - Can use debug keystore instead if preferred

- [ ] **Create Upload Key** (recommended)
  - Google recommends separate upload key for Play Store
  - Follow same process as production keystore
  - Name: `[YOUR_APP]-upload.keystore`
  - Enroll in Play App Signing (Google manages final signing key)

### Play Console App Signing

- [ ] **Enroll in Google Play App Signing**
  - Play Console → Your app → Release → Setup → App signing
  - Option 1: "Let Google create and manage your app signing key" (recommended)
  - Option 2: "Export and upload a key from a Java keystore"
  - Complete enrollment
  - Download upload certificate (PEM format)
  - This is required for STAGE/BETA/PROD uploads

## Phase 4: Development Environment (Week 1, Day 4)

### Tools Installation

- [ ] **Install/Update Xcode** (macOS only)
  ```bash
  # Install from App Store
  # Verify installation
  xcode-select --install
  xcodebuild -version
  ```
  - Version 14.0+ recommended
  - Accept license agreements

- [ ] **Install Android Studio/SDK**
  ```bash
  # Verify installation
  echo $ANDROID_HOME
  # Should point to Android SDK location
  ```
  - Android SDK 33+ recommended
  - Accept licenses: `$ANDROID_HOME/tools/bin/sdkmanager --licenses`

- [ ] **Install fastlane**
  ```bash
  # Install via Homebrew (macOS)
  brew install fastlane

  # OR via RubyGems
  sudo gem install fastlane

  # Verify installation
  fastlane --version
  ```
  - Version 2.210+ recommended

- [ ] **Install Node.js and npm**
  ```bash
  node --version  # v18+ recommended
  npm --version   # v9+ recommended
  ```

### Project Dependencies

- [ ] **Install Project Dependencies**
  ```bash
  cd /path/to/your/project
  npm install

  # iOS dependencies
  cd ios && pod install && cd ..

  # Verify React Native CLI
  npx react-native --version
  ```

- [ ] **Initialize Git (if not already)**
  ```bash
  git init
  git add .
  git commit -m "Initial commit before 4-tier setup"
  ```

## Phase 5: Secrets and Environment Variables (Week 1, Day 5)

### Create Secrets Files

- [ ] **Create `.env` Files** (add to .gitignore!)
  ```bash
  # Create environment files for each tier
  touch .env.qual .env.stage .env.beta .env.prod

  # Add to .gitignore
  echo ".env*" >> .gitignore
  ```

- [ ] **Populate Environment Variables**
  ```
  # .env.qual
  API_ENDPOINT=https://[YOUR_DOMAIN]/qual/api
  BUILD_TYPE=qual

  # .env.stage
  API_ENDPOINT=https://[YOUR_DOMAIN]/stage/api
  BUILD_TYPE=stage

  # .env.beta
  API_ENDPOINT=https://[YOUR_DOMAIN]/beta/api
  BUILD_TYPE=beta

  # .env.prod
  API_ENDPOINT=https://[YOUR_DOMAIN]/api
  BUILD_TYPE=prod
  ```

### Fastlane Credentials

- [ ] **Initialize fastlane match** (recommended for iOS code signing)
  ```bash
  cd ios
  fastlane match init
  # Choose storage option (git repo recommended)
  # Create private git repo for certificates
  # Record encryption password
  ```

- [ ] **Store App Store Connect Credentials**
  ```bash
  # Option 1: API Key (recommended)
  # Store .p8 file in secure location
  # Reference in Fastfile

  # Option 2: Apple ID credentials
  fastlane fastlane-credentials add --username your@email.com
  ```

- [ ] **Store Google Play Credentials**
  ```bash
  # Create service account JSON in Play Console
  # Play Console → Setup → API access → Create service account
  # Grant "Release Manager" role
  # Download JSON key
  # Store in secure location (e.g., android/play-store-credentials.json)
  # Add to .gitignore!
  ```

## Phase 6: Configuration Files (Week 2, Days 1-2)

### iOS Configuration

- [ ] **Create xcconfig Files**
  - Follow [ios-setup-guide.md](./ios-setup-guide.md)
  - Create `Qual.xcconfig`, `Stage.xcconfig`, `Beta.xcconfig`, `Prod.xcconfig`
  - Configure bundle IDs, display names, API endpoints

- [ ] **Configure Xcode Schemes**
  - Follow [ios-setup-guide.md](./ios-setup-guide.md)
  - Create schemes for each tier
  - Link to respective xcconfig files

- [ ] **Create iOS Fastfile**
  - Follow [fastlane-configuration.md](./fastlane-configuration.md)
  - Define lanes: `qual_ios`, `stage_ios`, `beta_ios`, `prod_ios`

### Android Configuration

- [ ] **Configure Product Flavors**
  - Follow [android-setup-guide.md](./android-setup-guide.md)
  - Edit `android/app/build.gradle`
  - Define flavors: `qual`, `stage`, `beta`, `prod`
  - Configure signing configs

- [ ] **Create Android Fastfile**
  - Follow [fastlane-configuration.md](./fastlane-configuration.md)
  - Define lanes: `qual_android`, `stage_android`, `beta_android`, `prod_android`

### JavaScript/TypeScript Configuration

- [ ] **Create Build Config Module**
  - Follow [environment-configuration.md](./environment-configuration.md)
  - Create `src/config/buildConfig.js` or `.ts`
  - Implement `BUILD_TYPE_ENV` detection
  - Configure API endpoint routing

- [ ] **Create Native Modules** (for runtime BUILD_TYPE detection)
  - Follow [environment-configuration.md](./environment-configuration.md)
  - iOS: Create `BuildConfigModule.swift` or `.m`
  - Android: Create `BuildConfigModule.kt` or `.java`
  - Bridge to JavaScript

## Phase 7: Deployment Scripts (Week 2, Day 3)

### Master Deployment Script

- [ ] **Create `scripts/deploy.sh`**
  - Follow [deployment-workflow.md](./deployment-workflow.md)
  - Implement tier routing (qual, stage, beta, prod)
  - Add validation and locking
  - Include platform flags (--web, --ios, --android, --all)

### Tier-Specific Scripts

- [ ] **Create tier deployment scripts**
  ```bash
  scripts/deploy/qual_deploy.sh
  scripts/deploy/deploy_stage.sh
  scripts/deploy/deploy_beta.sh
  scripts/deploy/prod_deploy.sh
  ```
  - Follow [deployment-workflow.md](./deployment-workflow.md)
  - Each calls appropriate fastlane lanes
  - Implements tier-specific validation

### Supporting Libraries

- [ ] **Create deployment support scripts**
  ```bash
  scripts/deploy/lib/validation.sh      # Pre-deployment checks
  scripts/deploy/lib/reporting.sh       # Deployment summaries
  scripts/deploy/lib/quality-gates.sh   # Quality checks
  ```

## Phase 8: First Deployment Test (Week 2, Days 4-5)

### QUAL Deployment

- [ ] **Test QUAL iOS**
  ```bash
  ./scripts/deploy.sh qual --ios
  # Verify build succeeds
  # Check simulator installation
  # Verify API endpoint (qual)
  # Confirm bundle ID: com.[YOUR_COMPANY].[YOUR_APP].qual
  ```

- [ ] **Test QUAL Android**
  ```bash
  ./scripts/deploy.sh qual --android
  # Verify build succeeds
  # Install on emulator
  # Verify API endpoint (qual)
  # Confirm package name: com.[YOUR_COMPANY].[YOUR_APP].qual
  ```

- [ ] **Test QUAL Web** (if applicable)
  ```bash
  ./scripts/deploy.sh qual --web
  # Verify build succeeds
  # Deploy to /qual/ path
  # Test in browser
  # Verify API endpoint (qual)
  ```

### STAGE Deployment

- [ ] **Test STAGE iOS**
  ```bash
  ./scripts/deploy.sh stage --ios
  # Verify upload to TestFlight
  # Check processing status in App Store Connect
  # Distribute to Internal Testing group
  # Install via TestFlight on device
  # Verify API endpoint (stage)
  # Confirm bundle ID: com.[YOUR_COMPANY].[YOUR_APP]
  ```

- [ ] **Test STAGE Android**
  ```bash
  ./scripts/deploy.sh stage --android
  # Verify upload to Play Console
  # Check Internal Testing track
  # Install via Play Console link
  # Verify API endpoint (stage)
  # Confirm package name: com.[YOUR_COMPANY].[YOUR_APP]
  ```

## Phase 9: Beta and Production Preparation (Week 3+)

### BETA Setup

- [ ] **Configure External TestFlight** (iOS)
  - Add beta testers to External Testing group
  - Submit for TestFlight review (first time only, 1-2 days)
  - Await approval before distributing BETA builds

- [ ] **Configure Closed Testing** (Android)
  - Set up closed testing email list
  - OR create opt-in URL for beta testers
  - No review required for closed testing

- [ ] **Test BETA Deployment**
  ```bash
  ./scripts/deploy.sh beta --all
  # iOS: Verify TestFlight external distribution
  # Android: Verify Play Console closed testing
  # Confirm beta API endpoint on both platforms
  ```

### PROD Setup

- [ ] **Prepare App Store Listing** (iOS)
  - Complete all metadata in App Store Connect
  - Upload screenshots (all required sizes)
  - Write description, keywords, support URL
  - Set pricing and availability
  - Complete age rating questionnaire
  - Prepare for first review submission

- [ ] **Prepare Play Store Listing** (Android)
  - Complete all metadata in Play Console
  - Upload screenshots, feature graphic
  - Write description, short description
  - Set pricing and availability
  - Complete content rating questionnaire
  - Complete store listing sections

- [ ] **Test PROD Build** (don't submit yet!)
  ```bash
  ./scripts/deploy.sh prod --all
  # Verify builds compile with prod configuration
  # Test locally before first submission
  # Confirm prod API endpoint
  ```

## Phase 10: Documentation and Team Onboarding (Week 3+)

### Internal Documentation

- [ ] **Document secrets locations**
  - Keystore passwords
  - App Store Connect API keys
  - Play Console service account
  - fastlane match password

- [ ] **Create runbook for team**
  - Deployment commands
  - Troubleshooting common issues
  - Emergency rollback procedures

- [ ] **Set up deployment notifications**
  - Slack/Discord/Email notifications
  - CI/CD integration (if applicable)

### Team Training

- [ ] **Train team on deployment workflow**
  - Walk through each tier
  - Explain promotion strategy
  - Review quality gates
  - Practice deployments together

- [ ] **Grant necessary access**
  - App Store Connect access (admin/app manager roles)
  - Google Play Console access (release manager role)
  - Certificate/keystore access (secure sharing)

## Completion Checklist

When all phases are complete, verify:

- [ ] QUAL builds and runs on simulator/emulator
- [ ] STAGE uploads to internal testing successfully
- [ ] BETA available to external testers (after review)
- [ ] PROD build tested locally (ready for submission)
- [ ] All secrets documented and backed up
- [ ] Team trained on deployment workflow
- [ ] Deployment scripts tested and working

## Next Steps

After completing this checklist:

1. Make your first production submission (PROD tier)
2. Establish deployment cadence (QUAL daily, BETA weekly, PROD bi-weekly)
3. Monitor deployments and refine process
4. Document lessons learned for your team

## Estimated Timeline Summary

- **Week 1**: Account setup, certificates, keystores, environment config (20-25 hours)
- **Week 2**: Configuration files, first QUAL/STAGE deployments (15-20 hours)
- **Week 3**: BETA setup, team training, documentation (10-15 hours)
- **Ongoing**: Reviews and approvals from Apple/Google (1-7 days each)

**Total Effort**: 45-60 hours of active work, 2-3 weeks calendar time

You're now ready to move to platform-specific setup guides!
