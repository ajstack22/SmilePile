# Wave 7 Launch Prompt: Complete Wave 6 & Deploy QUAL Tier

## Overview

Complete Wave 6 (QUAL Tier Validation) by finishing Phases 6-9 of the Atlas workflow, then prepare for Wave 8 (STAGE tier deployment).

---

## Current Status

### ✅ Wave 6 Phases Complete (1-5)
- **Phase 1: Research** - Identified critical test task name mismatch
- **Phase 2: Story** - Created STORY-6.6-qual-tier-validation.md
- **Phase 3: Planning** - Detailed implementation plan
- **Phase 4: Security/Peer Review** - Found & addressed CRITICAL issues
- **Phase 5: Implementation** - All 6 fixes applied (including iOS simulator bug)

### 📊 Implementation Summary
**Files Modified**: 1 (`deploy/deploy_qual.sh`)
**Lines Changed**: 70 total
**Fixes Applied**:
1. ✅ Android test task names (testTier1Critical, testTier2Important, testTier3UI)
2. ✅ jq dependency check with install instructions
3. ✅ CRITICAL security fix: iOS simulator input validation (command injection prevention)
4. ✅ Dynamic iOS simulator detection with intelligent fallback
5. ✅ Manylla commit paradox fix (validate-first, commit-after)
6. ✅ iOS simulator filtering (iPhone/iPad only, exclude Mac/Watch)

### ⏳ Wave 6 Remaining (Phases 6-9)
- **Phase 6**: End-to-end testing (both platforms)
- **Phase 7**: Acceptance criteria validation
- **Phase 8**: Evidence organization & documentation
- **Phase 9**: Final deployment with commit

---

## Wave 7 Mission: Complete Wave 6 (Phases 6-9)

Execute the final 4 phases of the Atlas workflow to complete Wave 6 validation and deployment.

---

## Phase 6: End-to-End Testing (UX + Peer Review in Parallel)

### Testing Objectives
Validate that ALL fixes work correctly in real deployment scenarios across both platforms.

### Test Scenarios

#### Scenario 1: Git Lock Resolution
**Current Issue**: Git index.lock exists from previous run
```bash
# Clear lock
rm -f /Users/adamstack/SmilePile/.git/index.lock

# Verify cleared
ls -la /Users/adamstack/SmilePile/.git/ | grep index.lock  # Should be empty
```

#### Scenario 2: Full QUAL Deployment (Both Platforms)
```bash
# Clean deployment with commit
./deploy/deploy_qual.sh both
```

**Expected Results**:
- ✅ Manylla pattern message: "Uncommitted changes detected - will be included in commit"
- ✅ All Tier 1 tests pass (Android + iOS)
- ✅ All Tier 2 tests pass (Android + iOS)
- ✅ All Tier 3 tests pass or warn (Android + iOS)
- ✅ SonarCloud analysis completes
- ✅ Android APK builds via Fastlane qual_android
- ✅ iOS app builds via Fastlane qual_ios
- ✅ Android APK installs on emulator
- ✅ iOS app installs on iPhone/iPad simulator (NOT Mac/Watch)
- ✅ Both apps launch successfully
- ✅ Git commit created with all changes
- ✅ Git tag created (qual-25.10.15.XXX)
- ✅ Changes pushed to GitHub

#### Scenario 3: Android-Only Deployment
```bash
SKIP_COMMIT=true ./deploy/deploy_qual.sh android
```

**Validation**:
- Tests run for Android only
- APK builds and installs
- No iOS build attempted

#### Scenario 4: iOS-Only Deployment
```bash
SKIP_COMMIT=true ./deploy/deploy_qual.sh ios
```

**Validation**:
- Tests run for iOS only
- App builds and installs on simulator
- Correct simulator selected (iPhone/iPad, not Mac)
- No Android build attempted

#### Scenario 5: Quality Gate Validation

**Test Tier 1 Blocking**:
```bash
# Simulate Tier 1 test failure
# Modify a Tier 1 test to fail temporarily
# Run deployment - should BLOCK
```

**Test Tier 2 Blocking**:
```bash
# Simulate Tier 2 test failure
# Run deployment - should BLOCK
```

**Test Tier 3 Warning**:
```bash
# Simulate Tier 3 test failure
# Run deployment - should WARN but CONTINUE
```

#### Scenario 6: Flag Validation
```bash
# Test SKIP_TESTS
SKIP_TESTS=true ./deploy/deploy_qual.sh android

# Test DRY_RUN
DRY_RUN=true ./deploy/deploy_qual.sh both

# Test SKIP_SONAR
SKIP_SONAR=true ./deploy/deploy_qual.sh both

# Test SKIP_COMMIT
SKIP_COMMIT=true ./deploy/deploy_qual.sh both
```

### Parallel Agent Execution

**Launch TWO agents in parallel** (single message, two Task tool calls):

**Agent 1: UX-Analyst**
- Execute all test scenarios
- Document actual vs expected results
- Capture screenshots of apps running
- Verify BUILD_TYPE_ENV shows "qual"
- Test app functionality on devices

**Agent 2: Peer-Reviewer**
- Code review of all 6 fixes
- Verify no regressions introduced
- Check edge cases (no simulator, network issues, etc.)
- Validate rollback procedures
- Review git commit message and tag format

### Deliverables
- `/wave-evidence/wave-6/07-testing-report.md`
- Screenshots: `wave-evidence/wave-6/screenshots/`
- Test execution logs
- Pass/fail matrix for all scenarios

---

## Phase 7: Acceptance Criteria Validation (Product-Manager Agent)

### Agent Mission
Validate that ALL acceptance criteria from STORY-6.6 are met.

### Story Reference
`/Users/adamstack/SmilePile/backlog/sprint-6/STORY-6.6-qual-tier-validation.md`

### Validation Checklist

**AC1: Critical Bug Fixes** ✅
- [x] Android test task names corrected
- [x] Dependency checks added (jq)
- [x] iOS simulator detection made dynamic

**AC2: QUAL Deployment Success**
- [ ] ./deploy/deploy_qual.sh both executes successfully end-to-end
- [ ] All Tier 1 tests pass (blocking)
- [ ] All Tier 2 tests pass (blocking)
- [ ] All Tier 3 tests complete (warnings only)
- [ ] SonarCloud analysis completes
- [ ] Version increments correctly
- [ ] iOS build succeeds via Fastlane
- [ ] Android build succeeds via Fastlane

**AC3: Artifact Verification**
- [ ] iOS app installed on simulator
- [ ] Android APK installed on emulator
- [ ] Both apps launch without crashes
- [ ] BUILD_TYPE_ENV displays "qual"
- [ ] App names display "SmilePile Qual"

**AC4: Git Workflow Validation**
- [ ] Git commit created
- [ ] Git tag created (qual-YYMMDDVVV)
- [ ] Changes pushed successfully
- [ ] .build_number updated

**AC5: Quality Gate Verification**
- [ ] Tier 1 failure blocks deployment
- [ ] Tier 2 failure blocks deployment
- [ ] Tier 3 failure warns but continues
- [ ] SKIP_TESTS=true bypasses tests
- [ ] DRY_RUN=true skips commit

**AC6: Documentation**
- [ ] QUAL deployment guide created
- [ ] Troubleshooting guide created
- [ ] Quality gates documented
- [ ] Flag usage documented
- [ ] Wave 6 completion evidence created

### Deliverables
- `/wave-evidence/wave-6/08-acceptance-criteria-validation.md`
- Sign-off: APPROVED / NEEDS WORK
- List any remaining blockers

---

## Phase 8: Clean-up & Organization (General-Purpose Agent)

### Agent Mission
Organize all Wave 6 artifacts and create comprehensive documentation.

### Tasks

#### 1. Evidence Organization
```
wave-evidence/wave-6/
├── 01-research-findings.md          ✅ (exists)
├── 02-story-creation.md             ✅ (exists)
├── 03-technical-planning.md         ✅ (exists)
├── 04-security-review.md            ✅ (exists)
├── 05-peer-review.md                ✅ (exists)
├── 06-implementation-summary.md     ✅ (exists)
├── 07-testing-report.md             ⏳ (Phase 6)
├── 08-acceptance-criteria-validation.md  ⏳ (Phase 7)
├── 09-deployment-log.md             ⏳ (Phase 9)
├── WAVE-6-COMPLETE.md               ⏳ (Phase 8)
└── screenshots/
    ├── android-qual-running.png
    └── ios-qual-running.png
```

#### 2. Create QUAL Deployment Guide
**File**: `/docs/qual-deployment-guide.md`

**Contents**:
- Overview of QUAL tier purpose
- Prerequisites (tools, credentials)
- Step-by-step deployment instructions
- Common deployment commands
- Flag reference (SKIP_TESTS, DRY_RUN, etc.)
- Expected output examples
- Timing expectations (<10 minutes)

#### 3. Create Troubleshooting Guide
**File**: `/docs/qual-troubleshooting-guide.md`

**Contents**:
- Common issues and solutions
- Test task not found → Check Gradle tasks
- Simulator not found → Install via Xcode
- Git lock file → Remove .git/index.lock
- jq not found → brew install jq
- SonarCloud timeout → SKIP_SONAR=true
- Network issues → Retry or skip
- Platform-specific issues

#### 4. Create Quality Gates Documentation
**File**: `/docs/quality-gates.md`

**Contents**:
- Tier 1: Critical tests (Security, Data Integrity) - BLOCKS
- Tier 2: Important tests (ViewModels, Repositories) - BLOCKS
- Tier 3: UI tests (Components, Integration) - WARNS
- Test coverage requirements
- SonarCloud quality gates (Tier 3)
- How to skip tests (when appropriate)

#### 5. Update Main Documentation
Update `/docs/DEPLOYMENT_ROADMAP.md`:
- Mark Wave 6 as COMPLETE
- Add links to new documentation
- Update completion date

#### 6. Create Wave 6 Completion Report
**File**: `/wave-evidence/wave-6/WAVE-6-COMPLETE.md`

**Contents**:
- Executive summary
- All phases completed (1-9)
- Total time spent
- Files created/modified
- Issues fixed (1 CRITICAL, 3 HIGH, 2 MEDIUM)
- Acceptance criteria status (all met)
- Lessons learned
- Next steps (Wave 7/8)

### Deliverables
- 3 new documentation files
- Organized wave-evidence directory
- Updated roadmap
- Wave 6 completion report

---

## Phase 9: Final Deployment (DevOps Agent)

### Agent Mission
Execute final QUAL deployment with all fixes, commit everything, and close Wave 6.

### Pre-Deployment Checklist
- [ ] All Phase 6 tests passed
- [ ] All Phase 7 acceptance criteria met
- [ ] All Phase 8 documentation complete
- [ ] Git repository clean (no index.lock)
- [ ] Ready for 10-minute deployment window

### Deployment Command
```bash
# Clear any git locks first
rm -f .git/index.lock

# Final deployment with commit
./deploy/deploy_qual.sh both
```

### Expected Outcome
```
✅ All tests pass (Tier 1, 2, 3)
✅ SonarCloud analysis complete
✅ Android APK builds via Fastlane
✅ iOS app builds via Fastlane
✅ Android installs on emulator
✅ iOS installs on iPhone/iPad simulator
✅ Both apps launch successfully
✅ Git commit created:
    "feat: Wave 6 - QUAL tier validation complete

    Fixed 6 critical deployment issues:
    - Android test task names corrected
    - Added jq dependency validation
    - CRITICAL: iOS simulator input validation (command injection fix)
    - Dynamic iOS simulator detection with intelligent fallback
    - Manylla commit paradox resolution (validate-first pattern)
    - iOS simulator filtering (iPhone/iPad only)

    All quality gates verified:
    - Tier 1/2 tests block on failure
    - Tier 3 tests warn but continue
    - SonarCloud integrated
    - Fastlane lanes operational

    🤖 Generated with Claude Code

    Co-Authored-By: Claude <noreply@anthropic.com>"

✅ Git tag: qual-25.10.15.XXX
✅ Pushed to GitHub
```

### Post-Deployment Validation
```bash
# Verify commit
git log -1 --stat

# Verify tag
git tag -l "qual-*" | tail -1

# Verify pushed
git log origin/main..HEAD  # Should be empty

# Check artifacts
ls -lh deploy/artifacts/qual/
```

### Deployment Evidence
Create `/wave-evidence/wave-6/09-deployment-log.md`:
- Deployment timestamp
- Git commit SHA
- Git tag name
- Version deployed (25.10.15.XXX)
- Deployment duration
- Success/failure status
- Artifacts created
- Next steps

### Close Story
Update `/backlog/sprint-6/STORY-6.6-qual-tier-validation.md`:
- Status: COMPLETE
- Completion date: 2025-10-15
- All acceptance criteria: MET
- Evidence location: wave-evidence/wave-6/

---

## Success Criteria for Wave 7 Completion

### Wave 6 Phases 6-9 Complete ✅
- [x] Phase 6: End-to-end testing across all scenarios
- [x] Phase 7: All acceptance criteria validated and met
- [x] Phase 8: Documentation complete and organized
- [x] Phase 9: Final deployment successful with commit

### Deliverables Created
- [x] Testing report (Phase 6)
- [x] Acceptance criteria validation (Phase 7)
- [x] QUAL deployment guide
- [x] Troubleshooting guide
- [x] Quality gates documentation
- [x] Wave 6 completion report
- [x] Deployment log (Phase 9)

### Technical Validation
- [x] All 6 fixes working in production
- [x] Manylla pattern proven effective
- [x] Zero regressions introduced
- [x] All quality gates functional
- [x] Git workflow automated

### Ready for Wave 8
- [x] QUAL tier fully validated
- [x] Documentation comprehensive
- [x] Team can deploy independently
- [x] STAGE tier deployment script ready (deploy_stage.sh)

---

## Estimated Timeline

**Total for Wave 7 (Phases 6-9)**: 4-6 hours

- **Phase 6 (Testing)**: 2-3 hours
  - Test execution: 1 hour
  - Agent parallel work: 1 hour
  - Documentation: 30 minutes

- **Phase 7 (Validation)**: 30 minutes
  - Criteria review: 20 minutes
  - Sign-off: 10 minutes

- **Phase 8 (Clean-up)**: 1-2 hours
  - Documentation writing: 1 hour
  - Evidence organization: 30 minutes
  - Roadmap updates: 30 minutes

- **Phase 9 (Deployment)**: 30 minutes
  - Pre-deployment checks: 10 minutes
  - Deployment execution: 10 minutes
  - Post-deployment validation: 10 minutes

---

## Agent Execution Strategy

### Use Parallel Agents Where Possible

**Phase 6**: Launch UX-analyst AND peer-reviewer in parallel (single message, two Task tools)

**Phase 8**: Can launch general-purpose agent for documentation while reviewing validation

**Phase 9**: Single devops agent for final deployment

---

## Important Notes

### Atlas Methodology Compliance
- ✅ All 9 phases must complete
- ✅ Evidence created at each phase
- ✅ Specialized agents used correctly
- ✅ Parallel execution maximized
- ✅ Quality gates enforced (tests must pass)

### Manylla Pattern Success
The validate-first, commit-after pattern is proven and should be adopted for all future deployment scripts (deploy_stage.sh, deploy_beta.sh, deploy_prod.sh).

### Security Fixes Permanent
The CRITICAL command injection fix in iOS simulator detection must be maintained and audited regularly.

### Documentation Critical
The documentation created in Phase 8 is essential for team onboarding and Wave 8 (STAGE tier) execution.

---

## Next Wave Preview: Wave 8 (STAGE Tier)

After Wave 7 completion, Wave 8 will:
- Test deploy_stage.sh (already created in Wave 5)
- Upload to TestFlight Internal Testing (first real upload!)
- Upload to Play Console Internal Testing
- Validate team can install from stores
- Document STAGE deployment process

**Estimated**: 6-8 hours over 2-3 days (includes TestFlight processing wait)

---

## Launch Command

Execute Wave 7 by running the 9-phase Atlas workflow for Phases 6-9:

```
Begin Wave 7: Complete Wave 6 QUAL Tier Validation

Execute Phases 6-9 of the Atlas workflow:
- Phase 6: End-to-end testing with UX-analyst and peer-reviewer agents (parallel)
- Phase 7: Acceptance criteria validation with product-manager agent
- Phase 8: Documentation and clean-up with general-purpose agent
- Phase 9: Final deployment with devops agent

Context:
- Phases 1-5 complete (research, story, planning, security, implementation)
- All 6 fixes applied to deploy/deploy_qual.sh
- Git lock needs clearing: rm -f .git/index.lock
- Reference: /Users/adamstack/SmilePile/WAVE-7-PROMPT.md

Start with Phase 6 parallel testing agents.
```

---

**Document Version**: 1.0
**Created**: 2025-10-15
**Wave 6 Status**: Phases 1-5 complete, Phases 6-9 pending
**Estimated Completion**: 4-6 hours
**Next Wave**: Wave 8 - STAGE Tier Validation & First Upload
