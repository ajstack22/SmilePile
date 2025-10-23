# CLAUDE.md - SmilePile Development Rules

## PRIMARY DEVELOPMENT METHODOLOGY: ATLAS WORKFLOW

### MANDATORY: Use Atlas Agent-Driven Workflow for ALL Features
All development MUST follow the 9-phase Atlas workflow using specialized agents:

**Phase 1**: Research (general-purpose agent)
**Phase 2**: Story Creation (product-manager agent)
**Phase 3**: Planning (developer agent)
**Phase 4**: Security Review (security + peer-reviewer agents in parallel)
**Phase 5**: Implementation (developer agent)
**Phase 6**: Testing (ux-analyst + peer-reviewer agents in parallel)
**Phase 7**: Validation (product-manager agent)
**Phase 8**: Clean-up (general-purpose agent)
**Phase 9**: Deployment (devops agent with deploy_qual.sh)

See `/Users/adamstack/SmilePile/atlas/docs/AGENT_WORKFLOW.md` for full details.

### CRITICAL: Never Skip Tests Without Explicit Permission
- Tests MUST run via deploy_qual.sh
- Fix failing tests, don't skip them
- User must explicitly say "skip tests" for any override

## CORE PRINCIPLES
1. **DO EXACTLY WHAT'S ASKED** - Nothing more, nothing less
2. **NEVER CREATE FILES** - Edit existing files only (unless explicitly requested)
3. **FOLLOW EXISTING PATTERNS** - Check neighboring files first, copy their style

## FORBIDDEN ACTIONS
- NO search/filter features unless requested
- NO favorites/bookmarks unless requested
- NO sorting/ordering unless requested
- NO "nice to have" additions
- NO new dependencies without permission
- NO emoji in code or commits
- NO comments unless requested

## BEFORE MAKING CHANGES
1. Read existing code patterns
2. Use only current libraries (check package.json/Podfile)
3. Match exact formatting/style

## AFTER MAKING CHANGES
- iOS: Run xcodebuild
- Android: Run ./gradlew build
- Both: Check git diff before confirming completion

## PROJECT SPECIFICS
- iOS: Swift/SwiftUI, no UIKit unless existing
- Android: Kotlin/Compose
- Photo IDs: Use PHAsset.localIdentifier (iOS), Uri.toString (Android)
- Build: Use ./deploy/deploy_qual.sh for deployments

## APPROVED TEST DEVICES
### iOS Simulators (iOS 26.0 only)
- **iPhone 17** (CC571231-D473-43E5-97F1-83F8289D8153)
- **iPhone 17 Pro Max** (8314ACBC-020A-451F-A94F-B8D9B27227FD)
- **iPad Pro 13-inch (M4)** (B9D4F952-EE7D-45FC-B75F-F4F5ED2C169C)

### Android Emulators
- **Pixel 9** - Standard phone
- **Pixel 9 Pro XL** - Large phone
- **Pixel Tablet** - Tablet form factor
- **Television 4K** - TV platform (for StackMapTV)

**Note**: Deploy script also installs on any physically connected ADB devices

## IF SOMETHING WON'T WORK
**TELL THE USER IMMEDIATELY** - Don't take shortcuts or quick fixes