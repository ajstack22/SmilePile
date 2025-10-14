# 4-Tier Deployment Handoff - COMPLETE

## Documentation Package Created

**Date:** October 13, 2025
**Total Documentation:** 10 comprehensive markdown files (7,119 lines)
**Total Size:** ~180 KB

## Files Created

### 1. README.md (6.8 KB)
Main entry point with overview, quick start, and navigation.

**Key sections:**
- What this is and why 4 tiers
- Key design decisions (single bundle ID strategy, compiled build type)
- Time estimates for setup
- Prerequisites checklist
- Quick start guide
- Platform-specific notes
- Security warnings

### 2. initial-setup-checklist.md (15 KB)
Complete one-time setup checklist with 10 phases.

**Key sections:**
- Account and store setup (Apple, Google)
- iOS configuration (bundle IDs, certificates)
- Android configuration (package names, keystores)
- Development environment setup
- Secrets management
- Configuration files
- Deployment scripts
- First deployment tests
- Beta/Prod preparation
- Team onboarding

**Timeline:** 8-12 hours over 1-2 weeks

### 3. ios-setup-guide.md (17 KB)
Complete iOS configuration for 4-tier system.

**Key sections:**
- Bundle ID strategy (QUAL with .qual suffix, others use base ID)
- xcconfig file creation for all 4 tiers
- Xcode scheme configuration
- Native BuildConfig module (Swift/Objective-C)
- Fastlane setup
- Code signing (automatic vs fastlane match)
- Verification steps
- First deployment testing
- Common issues and solutions
- iOS-specific considerations (icons, entitlements, TestFlight)

**Timeline:** 6-8 hours

### 4. android-setup-guide.md (23 KB)
Complete Android configuration for 4-tier system.

**Key sections:**
- Package name strategy (QUAL with .qual suffix, others use base)
- Keystore setup (production, upload, QUAL)
- Product flavors in build.gradle
- Signing configs
- Native BuildConfig module (Kotlin/Java)
- Fastlane setup
- Play Console access configuration
- Verification steps
- First deployment testing
- Common issues and solutions
- Android-specific considerations (icons, ProGuard, APK vs AAB)

**Timeline:** 4-6 hours

### 5. fastlane-configuration.md (19 KB)
Automated deployment setup with fastlane.

**Key sections:**
- Why fastlane (automation benefits)
- Installation options
- iOS Fastfile with 4 lanes (qual_ios, stage_ios, beta_ios, prod_ios)
- Android Fastfile with 4 lanes (qual_android, stage_android, beta_android, prod_android)
- Credential management (API keys, service accounts)
- Deployment summaries
- Integration with master deploy script
- Best practices
- Common issues

**Timeline:** 3-4 hours

### 6. environment-configuration.md (20 KB)
BUILD_TYPE_ENV implementation and API routing.

**Key sections:**
- Core concept: compiled build type (not runtime)
- iOS implementation (xcconfig, Info.plist, native module)
- Android implementation (product flavors, buildConfigField, native module)
- JavaScript implementation (buildConfig.js, API client)
- Web implementation (environment variables)
- Platform-agnostic approach
- Runtime environment detection (for debugging)
- Naming conventions across tiers
- Security considerations
- Troubleshooting

**Timeline:** 2-3 hours

### 7. deployment-workflow.md (17 KB)
Day-to-day deployment procedures.

**Key sections:**
- Master deployment script usage
- Tier usage guidelines (QUAL multiple/day, STAGE 1-3/week, BETA 1-2/week, PROD weekly/bi-weekly)
- Pre-deployment validation
- Deployment process (5 steps)
- Verification steps (BUILD_TYPE_ENV, API endpoints, bundle IDs)
- Deployment locking
- Deployment summaries
- Common deployment scenarios
- Rollback procedures
- Monitoring post-deployment
- Troubleshooting
- Best practices

**Timeline:** 5-15 minutes per deployment (after setup)

### 8. secrets-and-credentials.md (19 KB)
Secure credential management.

**Key sections:**
- Critical principle: never commit secrets
- Required secrets by platform (iOS, Android, Web)
- iOS secrets setup (Apple ID, API keys, certificates, match passphrase)
- Android secrets setup (keystores, keystore.properties, service account)
- Web secrets setup (SSH keys, environment variables)
- Sharing secrets with team (password manager, secure file sharing)
- Environment variables for CI/CD
- Secret rotation procedures
- Backup strategy (3-2-1 rule)
- Security best practices
- Emergency procedures (lost keystore, compromised credentials)

**Timeline:** 2-3 hours for initial setup

### 9. troubleshooting.md (23 KB)
Common issues and solutions.

**Key sections:**
- General deployment issues (lock files, git status, missing files)
- iOS deployment issues (code signing, build errors, upload errors, TestFlight)
- Android deployment issues (keystore errors, build errors, upload errors, Play Console)
- BUILD_TYPE_ENV issues
- Fastlane issues
- Performance issues (slow builds)
- Getting more help (logs, verbose mode, support contacts)
- Preventive measures

**Complete error catalog with step-by-step solutions**

### 10. reference-implementations.md (22 KB)
StackMap's production code examples.

**Key sections:**
- iOS configuration files (xcconfig, schemes, native modules, fastlane)
- Android configuration files (build.gradle, native modules, fastlane)
- JavaScript configuration (buildConfig module)
- Deployment scripts (master script, tier scripts, supporting libraries)
- Documentation references
- .gitignore patterns
- Common patterns and customization points
- Testing your implementation
- Getting the most from references

**Complete file path references with customization guidance**

## Documentation Characteristics

### Writing Style
- Clear, step-by-step instructions
- "Why" explanations for key decisions
- Copy-paste ready commands with placeholders
- Checklists and numbered procedures
- Time estimates for each phase
- Platform-specific considerations highlighted
- Cross-references between documents

### Target Audience
- Engineers familiar with iOS and Android
- Setting up deployment system for first time
- Need complete end-to-end guidance
- Want to understand rationale, not just steps

### Key Differentiators
- **4-tier focus:** QUAL, STAGE, BETA, PROD with clear usage guidelines
- **Single bundle ID strategy:** Explained with rationale
- **Compiled BUILD_TYPE_ENV:** Security and clarity benefits
- **StackMap references:** Production-tested examples with customization guidance
- **First submission focus:** Apps not yet in stores
- **Complete secrets management:** Security-first approach

## Usage Instructions

### For New Projects

1. **Read README.md first** - Understand system and decide if 4-tier is right for you
2. **Follow initial-setup-checklist.md** - Complete all 10 phases in order
3. **Platform-specific setup** - iOS and Android guides in parallel
4. **Configure automation** - Fastlane and environment configuration
5. **Test deployments** - QUAL → STAGE → BETA → PROD progression
6. **Secure credentials** - Secrets management and team sharing
7. **Reference StackMap** - Adapt production examples to your project

### For Existing Projects

1. **Audit current system** - Compare with 4-tier approach
2. **Identify gaps** - What's missing from your deployment workflow?
3. **Incremental adoption** - Start with QUAL, add tiers as needed
4. **Adapt, don't copy** - StackMap's system is reference, not template
5. **Document changes** - Why you diverged from reference implementation

### For Team Onboarding

1. **README.md** - System overview for all team members
2. **deployment-workflow.md** - Day-to-day usage for developers
3. **secrets-and-credentials.md** - Access and security for DevOps/leads
4. **troubleshooting.md** - Bookmark for when issues arise

## StackMap-Specific Notes

### What to Replace

1. **Bundle ID / Package Name**
   - Find: `com.adamstack.stackmapnative`
   - Replace with: `com.[YOUR_COMPANY].[YOUR_APP]`

2. **App Name**
   - Find: `StackMap`, `StackMapNative`
   - Replace with: `[YOUR_APP]`

3. **API Domain**
   - Find: `stackmap.app`
   - Replace with: `[YOUR_DOMAIN]`

4. **Xcode Project Name**
   - Find: `StackMapNative.xcodeproj`
   - Replace with: `[YOUR_APP].xcodeproj`

5. **File Paths**
   - All paths are relative to project root
   - Adjust if your project structure differs

### What to Keep

1. **4-tier strategy** - Well-tested progression
2. **Single bundle ID approach** - Simplifies store management
3. **Compiled BUILD_TYPE_ENV** - Security and clarity
4. **Fastlane automation** - Saves hours per deployment
5. **Master deployment script** - Single entry point prevents errors

### What to Consider Changing

1. **Tier names** - Some teams use dev/qa/staging/prod
2. **Deployment frequency** - Adjust to your team's cadence
3. **Quality gates** - Add/remove checks based on your process
4. **Notification systems** - Slack vs email vs other
5. **Version number management** - Manual vs automated vs semantic

## Success Criteria

After implementing this system, you should be able to:

- [ ] Deploy QUAL multiple times per day with single command
- [ ] Deploy STAGE to internal testers with confidence
- [ ] Deploy BETA to external testers for feedback
- [ ] Deploy PROD to public with proper quality gates
- [ ] Identify which tier any build belongs to
- [ ] Route API requests to correct endpoint automatically
- [ ] Share deployment credentials securely with team
- [ ] Troubleshoot common issues using documentation
- [ ] Onboard new team members to deployment workflow
- [ ] Roll back deployments if issues arise

## Time Investment Summary

### Initial Setup (One-Time)
- Account setup: 4-6 hours (spread over days for approvals)
- iOS configuration: 6-8 hours
- Android configuration: 4-6 hours
- Fastlane setup: 3-4 hours
- Environment configuration: 2-3 hours
- Secrets management: 2-3 hours
- First deployment tests: 4-6 hours

**Total:** 25-36 hours of active work, 2-3 weeks calendar time

### Ongoing (Per Deployment)
- QUAL: 5-10 minutes
- STAGE: 10-15 minutes
- BETA: 10-15 minutes
- PROD: 15-20 minutes

**ROI:** After ~10 deployments, automation pays for itself vs manual process

## Next Steps

1. **Review README.md** - Understand system completely
2. **Start initial-setup-checklist.md** - Begin Phase 1
3. **Follow guides in order** - Don't skip steps
4. **Test in QUAL first** - Validate setup before higher tiers
5. **Document your journey** - Note issues and solutions specific to your project
6. **Share with team** - Train team on new deployment workflow

## Support and Maintenance

### Keeping Documentation Updated

As you discover issues or improvements:
1. Update relevant guide with findings
2. Add to troubleshooting.md if new issue
3. Share insights with team
4. Consider contributing back improvements

### When to Revisit

- **Quarterly:** Review secrets and access
- **After major React Native upgrade:** Verify fastlane compatibility
- **After team changes:** Update access and rotate secrets
- **After store policy changes:** Update procedures

## Final Notes

This documentation represents StackMap's production-tested 4-tier deployment system, battle-hardened through hundreds of deployments. It's comprehensive, opinionated, and designed for teams serious about deployment quality.

**Key philosophy:** Automation and clarity prevent errors. Invest time in setup to save time and reduce stress in operations.

**Good luck with your deployment system!**

---

**Documentation created by:** Claude Code (DevOps persona)
**Based on:** StackMap production deployment system (January 2025)
**For questions:** Refer to troubleshooting.md or StackMap reference implementations
