# 4-Tier Deployment System - Handoff Documentation

Complete setup guide for implementing a 4-tier deployment system (QUAL, STAGE, BETA, PROD) for React Native apps with web deployment.

## What This Is

This documentation package provides everything needed to set up a professional 4-tier deployment pipeline for your React Native application. The system was battle-tested in production on StackMap and provides:

- **QUAL** - Development testing environment (multiple deployments per day)
- **STAGE** - Internal team validation (mobile-only, pre-beta testing)
- **BETA** - Closed beta testing with real users (1-2 deployments per week)
- **PROD** - Production release (weekly/bi-weekly deployments)

## Why 4 Tiers?

**QUAL**: Fast iteration for developers. Deploy multiple times per day to test changes quickly without affecting other tiers.

**STAGE**: Internal validation gate. Test with your team on real devices before exposing to beta testers. Shares database with QUAL for realistic testing.

**BETA**: Controlled user testing. Get feedback from a closed group before public release. Uses production database to ensure data consistency.

**PROD**: Public release. Stable, well-tested builds for all users.

## Key Design Decisions

### Single Bundle ID/Package Name Strategy

**iOS**: `com.[YOUR_COMPANY].[YOUR_APP]` for STAGE, BETA, and PROD. Only QUAL uses `com.[YOUR_COMPANY].[YOUR_APP].qual`

**Android**: `com.[YOUR_COMPANY].[YOUR_APP]` for STAGE, BETA, and PROD. Only QUAL uses `com.[YOUR_COMPANY].[YOUR_APP].qual`

**Why?** App stores differentiate builds by internal/external TestFlight tracks and Play Console tracks, not bundle IDs. This simplifies code signing and distribution.

### Compiled Build Type

Each build has `BUILD_TYPE_ENV` compiled in at build time (qual/stage/beta/prod). This determines API endpoints and is immutable after compilation. No runtime switching between environments.

**Why?** Security and clarity. Users can't accidentally switch to wrong API. Support can identify environment from build alone.

### Store Distribution

- **QUAL**: Simulator/emulator only (never distributed through stores)
- **STAGE**: TestFlight Internal Testing / Play Console Internal Testing
- **BETA**: TestFlight External Testing / Play Console Closed Testing
- **PROD**: App Store / Play Store Production

## Time Estimates

- **Initial Setup**: 8-12 hours (spread over 1-2 weeks due to Apple/Google approval delays)
- **First QUAL Deployment**: 2-4 hours (iOS certificates, Android keystore, fastlane setup)
- **First STAGE Deployment**: 1-2 hours (configure internal testing)
- **First BETA Deployment**: 2-4 hours (external testing approval, beta tester setup)
- **First PROD Deployment**: 1-2 hours (final review submission)

## Prerequisites

Before starting, ensure you have:

- [ ] React Native project with iOS and Android
- [ ] macOS machine with Xcode installed (for iOS builds)
- [ ] Apple Developer account ($99/year)
- [ ] Google Play Developer account ($25 one-time)
- [ ] Access to App Store Connect and Google Play Console
- [ ] Node.js and npm installed
- [ ] Ruby installed (for fastlane)
- [ ] Basic understanding of iOS code signing and Android keystores

## Documentation Structure

Read these documents in order:

1. **[initial-setup-checklist.md](./initial-setup-checklist.md)** - Start here! One-time setup tasks and checklist
2. **[ios-setup-guide.md](./ios-setup-guide.md)** - Complete iOS configuration
3. **[android-setup-guide.md](./android-setup-guide.md)** - Complete Android configuration
4. **[fastlane-configuration.md](./fastlane-configuration.md)** - Fastlane setup for automated deployments
5. **[environment-configuration.md](./environment-configuration.md)** - BUILD_TYPE_ENV and API routing
6. **[deployment-workflow.md](./deployment-workflow.md)** - Day-to-day deployment commands
7. **[secrets-and-credentials.md](./secrets-and-credentials.md)** - Managing credentials securely
8. **[troubleshooting.md](./troubleshooting.md)** - Common issues and solutions
9. **[reference-implementations.md](./reference-implementations.md)** - StackMap examples and code

## Quick Start

### 1. Complete Initial Setup (Week 1)

```bash
# Follow initial-setup-checklist.md
# - Create App Store Connect app
# - Create Google Play Console app
# - Generate certificates and keystores
```

### 2. Configure iOS (Day 1-2)

```bash
# Follow ios-setup-guide.md
# - Set up bundle IDs
# - Create xcconfig files
# - Configure fastlane
```

### 3. Configure Android (Day 2-3)

```bash
# Follow android-setup-guide.md
# - Set up package names
# - Create product flavors
# - Configure fastlane
```

### 4. Test First Deployment (Day 3-4)

```bash
# Deploy QUAL to verify setup
./scripts/deploy.sh qual --all

# Verify on simulator/emulator
# Check API endpoints
# Confirm build type detection
```

### 5. Progress Through Tiers (Weeks 2-3)

```bash
# STAGE (internal team testing)
./scripts/deploy.sh stage --all

# BETA (closed beta testers)
./scripts/deploy.sh beta --all

# PROD (public release)
./scripts/deploy.sh prod --all
```

## Platform-Specific Notes

### iOS

- Requires macOS for building
- Code signing is complex but automated via fastlane
- TestFlight review takes 1-2 days for first submission
- Internal testing available immediately after upload
- External testing requires Apple review

### Android

- Can build on any platform (macOS, Linux, Windows)
- Keystore management is critical (backup securely!)
- Play Console internal testing available immediately
- Closed testing available immediately (no review)
- Production review takes 1-7 days

### Web

- Deploy to different subpaths: `/qual/`, `/stage/`, `/beta/`, `/` (prod)
- Same BUILD_TYPE_ENV system as mobile
- Single build step for all tiers
- No store approval required

## Key Differences from StackMap

This documentation is generalized from StackMap's implementation. You may need to adapt:

- **API Endpoints**: StackMap uses `stackmap.app/[tier]/api`. Adjust for your backend.
- **Bundle IDs**: Replace `com.adamstack.stackmapnative` with your identifiers.
- **Deployment Scripts**: StackMap's scripts include web deployment. You may only need mobile.
- **Native Modules**: StackMap uses BuildConfigModule for runtime detection. Pattern is reusable.

## Support and Updates

This documentation is based on StackMap's production system (January 2025). For questions or improvements:

1. Check [troubleshooting.md](./troubleshooting.md) first
2. Review StackMap's implementation in [reference-implementations.md](./reference-implementations.md)
3. Consult Apple/Google documentation for store-specific issues

## Security Note

Never commit secrets to git! Use environment variables, fastlane credentials, or secure secret management. See [secrets-and-credentials.md](./secrets-and-credentials.md) for details.

## Next Steps

Start with **[initial-setup-checklist.md](./initial-setup-checklist.md)** to begin your 4-tier deployment setup.
