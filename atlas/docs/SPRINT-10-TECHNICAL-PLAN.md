# Phase 3: Technical Implementation Plan
## Sprint 10 Security Enhancements (SECURITY-001 to SECURITY-004)

**Document Version**: 1.0
**Created**: 2025-10-02
**Status**: READY FOR IMPLEMENTATION
**Total Estimated Effort**: 13.5 hours

---

## Executive Summary

This document provides a detailed technical implementation plan for four security enhancement stories in Sprint 10. The stories add automated security scanning and compliance checking to the SmilePile project without requiring code changes to the iOS/Android applications.

**Stories in Scope**:
1. SECURITY-001: Enable GitHub Dependabot (1.5 hours)
2. SECURITY-002: Integrate gitleaks Secret Scanning (3.5 hours)
3. SECURITY-003: Add ESLint Security Plugins (5 hours)
4. SECURITY-004: Add License Compliance Checking (3.5 hours)

**Key Characteristics**:
- All stories are configuration-based (no mobile app code changes)
- Low risk of breaking existing functionality
- Immediate security value upon completion
- Each story is independently deployable

---

## Implementation Order & Rationale

### Recommended Sequence

**1. SECURITY-001: Enable GitHub Dependabot (1.5 hours)**
- **Why First**: Safest implementation, zero code changes, pure configuration
- **Risk Level**: LOW - Only adds a YAML file to .github/
- **Dependencies**: None
- **Rollback**: Delete .github/dependabot.yml if needed
- **Value**: Immediate vulnerability detection across npm, Gradle, and GitHub Actions

**2. SECURITY-002: Integrate gitleaks Secret Scanning (3.5 hours)**
- **Why Second**: Read-only audit, no workflow changes, provides historical baseline
- **Risk Level**: LOW - Only scans existing code, no enforcement
- **Dependencies**: gitleaks already installed at /opt/homebrew/bin/gitleaks
- **Rollback**: Delete .gitleaks.toml and audit reports
- **Value**: One-time historical audit + ongoing prevention capability

**3. SECURITY-004: Add License Compliance Checking (3.5 hours)**
- **Why Third**: Read-only analysis, no build process changes
- **Risk Level**: LOW - license-checker already installed globally
- **Dependencies**: None (runs independently of other stories)
- **Rollback**: Remove npm scripts from package.json
- **Value**: Legal risk mitigation, no GPL/AGPL dependencies verified

**4. SECURITY-003: Add ESLint Security Plugins (5 hours)**
- **Why Last**: Requires npm install, potential for errors requiring fixes
- **Risk Level**: MEDIUM - May discover security issues requiring code changes
- **Dependencies**: None, but benefits from gitleaks config being in place
- **Rollback**: Remove ESLint packages and configuration
- **Value**: Ongoing security linting for website codebase

### Rationale for Order

1. **Start with safest**: Dependabot requires no local setup
2. **Build knowledge progressively**: Each story adds complexity
3. **Delay potentially blocking work**: ESLint may find issues requiring fixes
4. **Independent value delivery**: Each story can be deployed separately

---

## Story 1: Enable GitHub Dependabot

### Overview
Create GitHub Dependabot configuration to automatically scan for vulnerable dependencies in npm (website), Gradle (android), and GitHub Actions workflows.

### Pre-Implementation Checklist
- [ ] Verify GitHub repository settings allow Dependabot
- [ ] Confirm main branch protection rules won't block Dependabot PRs
- [ ] Review existing open PRs (none expected on first run)

### Implementation Steps

#### Phase 1: Create Configuration File (30 minutes)

**File to Create**: `/Users/adamstack/SmilePile/.github/dependabot.yml`

```yaml
version: 2
updates:
  # Website npm dependencies
  - package-ecosystem: "npm"
    directory: "/website"
    schedule:
      interval: "weekly"
      day: "monday"
      time: "09:00"
    open-pull-requests-limit: 5
    groups:
      patch-updates:
        patterns:
          - "*"
        update-types:
          - "patch"
    labels:
      - "dependencies"
      - "website"
    commit-message:
      prefix: "chore(deps)"

  # Android Gradle dependencies
  - package-ecosystem: "gradle"
    directory: "/android"
    schedule:
      interval: "weekly"
      day: "monday"
      time: "09:00"
    open-pull-requests-limit: 5
    groups:
      patch-updates:
        patterns:
          - "*"
        update-types:
          - "patch"
    labels:
      - "dependencies"
      - "android"
    commit-message:
      prefix: "chore(deps)"

  # GitHub Actions dependencies
  - package-ecosystem: "github-actions"
    directory: "/"
    schedule:
      interval: "weekly"
      day: "monday"
      time: "09:00"
    open-pull-requests-limit: 3
    labels:
      - "dependencies"
      - "ci"
    commit-message:
      prefix: "chore(ci)"
```

**Commands to Execute**:
```bash
# Create directory if not exists
mkdir -p /Users/adamstack/SmilePile/.github

# Validate YAML syntax
cat /Users/adamstack/SmilePile/.github/dependabot.yml

# Commit to repository
cd /Users/adamstack/SmilePile
git add .github/dependabot.yml
git commit -m "chore: Enable GitHub Dependabot for npm, Gradle, and GitHub Actions"
git push origin main
```

#### Phase 2: Verify GitHub Integration (20 minutes)

**Manual Verification Steps**:
1. Navigate to GitHub repository: https://github.com/[username]/SmilePile
2. Go to Settings > Security > Code security and analysis
3. Verify "Dependabot alerts" is enabled (should auto-enable)
4. Verify "Dependabot security updates" is enabled
5. Verify "Dependabot version updates" is enabled
6. Wait 5-10 minutes for initial scan

**Expected Outcomes**:
- Dependabot section appears in Security tab
- All 3 ecosystems recognized (npm, gradle, github-actions)
- Initial scan completes (may find existing vulnerabilities)
- No errors in GitHub Actions logs

#### Phase 3: Documentation (15 minutes)

Create documentation for Dependabot workflow and update README with security practices section.

#### Phase 4: Validation (15 minutes)

**Validation Checklist**:
- [ ] .github/dependabot.yml exists and committed
- [ ] GitHub Security tab shows Dependabot section
- [ ] All 3 ecosystems detected (verify in Insights > Dependency graph)
- [ ] No syntax errors in configuration
- [ ] Documentation created and committed
- [ ] Initial scan completed (check for alerts)

**Success Criteria**:
- Configuration file valid and committed
- Dependabot active in GitHub UI
- 3/3 ecosystems detected
- Documentation complete

### Potential Issues & Mitigation

| Issue | Probability | Mitigation |
|-------|-------------|------------|
| Dependabot finds many vulnerabilities | MEDIUM | Expected on first run; create separate stories to address |
| Configuration syntax errors | LOW | Validate YAML before committing |
| GitHub doesn't recognize ecosystems | LOW | Verify package.json, build.gradle.kts exist in correct locations |
| Too many PRs created at once | LOW | PR limits configured (3-5 max per ecosystem) |

### Testing Strategy

**Pre-Deployment**:
- [ ] Validate YAML syntax with online validator
- [ ] Review configuration against Dependabot documentation
- [ ] Verify file paths are correct (/website, /android, /)

**Post-Deployment**:
- [ ] Check GitHub Security tab within 15 minutes
- [ ] Verify ecosystems detected within 30 minutes
- [ ] Monitor for Dependabot PRs over next 24 hours
- [ ] Review any security alerts generated

### Rollback Plan

If Dependabot causes issues:
1. Delete `.github/dependabot.yml`
2. Commit and push
3. Disable Dependabot in GitHub Settings > Security
4. Close any open Dependabot PRs
5. Document reason for rollback

**Rollback Time**: < 5 minutes

---

## Story 2: Integrate gitleaks Secret Scanning

### Overview
Run full git history scan for secrets, create configuration to reduce false positives, and optionally provide pre-commit hook for developers.

### Pre-Implementation Checklist
- [ ] Verify gitleaks installed: `/opt/homebrew/bin/gitleaks --version`
- [ ] Ensure git repository is clean (no uncommitted changes during scan)
- [ ] Back up repository (optional, for safety)
- [ ] Have plan for secret rotation if secrets found

### Implementation Steps

#### Phase 1: Full Git History Scan (1 hour)

**Commands to Execute**:
```bash
cd /Users/adamstack/SmilePile

# Run full history scan (JSON report)
gitleaks detect \
  --source . \
  --report-path gitleaks-report.json \
  --report-format json \
  --verbose

# Run full history scan (SARIF report for readability)
gitleaks detect \
  --source . \
  --report-path gitleaks-report.sarif \
  --report-format sarif \
  --verbose

# Check exit code
echo "Exit code: $?"
# Exit code 0 = no secrets found
# Exit code 1 = secrets found
```

**Expected Outcomes**:
- **Best Case**: No secrets found (exit code 0)
- **Likely Case**: False positives found (test keys, examples)
- **Worst Case**: Real secrets found (requires immediate remediation)

**Analysis Process**:
1. Review `gitleaks-report.json` for findings
2. For each finding, determine:
   - True positive (real secret) → CRITICAL, rotate immediately
   - False positive (test key, example) → Add to allowlist
3. Document all findings in `SECURITY_AUDIT_RESULTS.md`

#### Phase 2: Create Gitleaks Configuration (1 hour)

**File to Create**: `/Users/adamstack/SmilePile/.gitleaks.toml`

```toml
title = "SmilePile Gitleaks Configuration"

[extend]
# Use default gitleaks rules as baseline
useDefault = true

[[rules]]
id = "generic-api-key"
description = "Detected a Generic API Key"
regex = '''(?i)(api[_-]?key|apikey)[\s]*[=:]\s*['"]?[a-zA-Z0-9]{32,}['"]?'''
tags = ["api", "key"]

[[rules]]
id = "google-oauth"
description = "Google OAuth Client Secret"
regex = '''[0-9]+-[0-9A-Za-z_]{32}\.apps\.googleusercontent\.com'''
tags = ["google", "oauth"]

[[rules]]
id = "aws-access-key"
description = "AWS Access Key ID"
regex = '''AKIA[0-9A-Z]{16}'''
tags = ["aws", "key"]

# Allowlist for known false positives
[allowlist]
description = "Allowlisted files and patterns"

# Example code and test files
paths = [
    '''.*_test\.kt''',           # Kotlin test files
    '''.*_test\.swift''',        # Swift test files
    '''.*Test\.java''',          # Java test files
    '''.*/fixtures/.*''',        # Test fixtures
    '''.*/examples/.*''',        # Example code
    '''atlas/examples/.*''',     # Atlas example documentation
]

# Allowlist specific patterns (update based on scan results)
regexes = [
    '''example-api-key-12345''',      # Obvious fake keys
    '''your-api-key-here''',          # Placeholder text
    '''sk-test-[a-zA-Z0-9]+''',       # Stripe test keys (publicly safe)
    '''AKIAIOSFODNN7EXAMPLE''',       # AWS example key from documentation
]

# Allowlist specific commits (add SHAs if needed)
commits = [
    # Add commit SHAs here if specific commits have allowlisted secrets
]
```

**Commands to Execute**:
```bash
cd /Users/adamstack/SmilePile

# Test configuration
gitleaks detect --config .gitleaks.toml --source . --verbose

# Refine allowlist based on remaining false positives
# Edit .gitleaks.toml as needed

# Final verification
gitleaks detect --config .gitleaks.toml --source . --verbose
echo "Exit code: $?"
# Goal: Exit code 0 (no findings after allowlist applied)
```

#### Phase 3: Create Pre-commit Hook Example (30 minutes)

**File to Create**: `/Users/adamstack/SmilePile/.githooks/pre-commit`

```bash
#!/bin/bash
# SmilePile gitleaks pre-commit hook
# Installation: git config core.hooksPath .githooks

echo "Running gitleaks secret scan on staged files..."

# Run gitleaks on staged files only
gitleaks protect --staged --verbose

if [ $? -eq 1 ]; then
    echo ""
    echo "Gitleaks detected secrets in your staged files!"
    echo "   Review findings above and remove secrets before committing."
    echo "   If this is a false positive, add to .gitleaks.toml allowlist."
    echo ""
    exit 1
fi

echo "No secrets detected"
exit 0
```

Make executable:
```bash
chmod +x /Users/adamstack/SmilePile/.githooks/pre-commit
```

#### Phase 4: Documentation (30 minutes)

Create `SECURITY.md` with gitleaks usage instructions and update `CONTRIBUTING.md` with secret scanning best practices.

#### Phase 5: Update .gitignore (5 minutes)

Update `/Users/adamstack/SmilePile/.gitignore`:
```bash
# Add gitleaks report files (should not be committed)
gitleaks-report.json
gitleaks-report.sarif
gitleaks-report.txt
```

#### Phase 6: Validation (20 minutes)

**Validation Checklist**:
- [ ] Full git history scan completed
- [ ] SECURITY_AUDIT_RESULTS.md created and documents findings
- [ ] .gitleaks.toml configuration created
- [ ] Configuration reduces false positives to acceptable level (<5)
- [ ] Pre-commit hook created in .githooks/
- [ ] Pre-commit hook is executable (chmod +x)
- [ ] .githooks/README.md documents installation
- [ ] SECURITY.md created or updated
- [ ] CONTRIBUTING.md updated with secret scanning practices
- [ ] .gitignore updated to exclude report files
- [ ] All files committed to repository

**Success Criteria**:
- Historical scan complete and documented
- Configuration file functional
- False positive rate < 5 findings
- Documentation comprehensive
- Pre-commit hook tested (optional)

### Potential Issues & Mitigation

| Issue | Probability | Mitigation |
|-------|-------------|------------|
| Real secrets found in history | MEDIUM | Have secret rotation plan ready; document in audit results |
| Too many false positives | HIGH | Iteratively refine .gitleaks.toml allowlist |
| Large repository slow to scan | LOW | Initial scan is one-time; ongoing scans are incremental |
| Developers bypass pre-commit hook | MEDIUM | Hook is optional; rely on education and documentation |

### Testing Strategy

**Pre-Deployment**:
- [ ] Test gitleaks command executes successfully
- [ ] Verify .gitleaks.toml syntax is valid
- [ ] Test pre-commit hook with fake secret

**Post-Deployment**:
- [ ] Run full scan with config: `gitleaks detect --config .gitleaks.toml --source . --verbose`
- [ ] Verify allowlist reduces findings appropriately
- [ ] Test pre-commit hook (optional)
- [ ] Verify documentation is clear and actionable

### Rollback Plan

If gitleaks integration causes issues:
1. Delete `.gitleaks.toml`
2. Delete `.githooks/pre-commit` and `.githooks/README.md`
3. Remove gitleaks section from SECURITY.md
4. Remove gitleaks references from CONTRIBUTING.md
5. Remove gitleaks-report.* from .gitignore
6. Commit and push
7. Document reason for rollback

**Rollback Time**: < 10 minutes

---

## Story 3: Add License Compliance Checking

### Overview
Scan npm dependencies for license compliance, flag copyleft licenses (GPL, AGPL), and generate CSV report.

### Pre-Implementation Checklist
- [ ] Verify license-checker installed: `license-checker --version`
- [ ] Review current dependencies: `cat /Users/adamstack/SmilePile/website/package.json`
- [ ] Have list of alternative libraries ready (in case GPL/AGPL found)

### Implementation Steps

#### Phase 1: Verify Installation (15 minutes)

**Commands to Execute**:
```bash
# Check if license-checker installed
which license-checker
# Expected: /Users/adamstack/.npm-global/bin/license-checker

# Verify version
license-checker --version

# If not installed or outdated:
npm install -g license-checker
```

#### Phase 2: Run Initial License Scan (30 minutes)

**Commands to Execute**:
```bash
cd /Users/adamstack/SmilePile/website

# Run summary check
license-checker --summary --production --excludePrivatePackages

# Run full CSV report
license-checker \
  --csv \
  --out license-report.csv \
  --production \
  --excludePrivatePackages

# Check for prohibited licenses
license-checker \
  --failOn 'GPL;AGPL;SSPL' \
  --production \
  --excludePrivatePackages

echo "Exit code: $?"
# Exit code 0 = no prohibited licenses found
# Exit code 1 = prohibited licenses found
```

**Analysis Process**:
1. Review `license-report.csv` for all licenses
2. Check summary output for license distribution
3. Identify any prohibited licenses (GPL, AGPL, SSPL)
4. Identify any "UNKNOWN" licenses (requires investigation)
5. Document findings

**Expected Outcomes Based on Current Dependencies**:
```json
{
  "dependencies": {
    "astro": "^5.14.1",           // Expected: MIT
    "@astrojs/sitemap": "^3.0.0", // Expected: MIT
    "@astrojs/tailwind": "^5.0.0", // Expected: MIT
    "tailwindcss": "^3.4.0"       // Expected: MIT
  },
  "devDependencies": {
    "astro-compress": "^2.0.0"    // Expected: MIT
  }
}
```

**Likely Result**: All MIT licensed (no issues expected)

#### Phase 3: Create Configuration & Scripts (1 hour)

**File to Modify**: `/Users/adamstack/SmilePile/website/package.json`

Add the following scripts:
```json
{
  "scripts": {
    "dev": "astro dev",
    "build": "astro build",
    "preview": "astro preview",
    "astro": "astro",
    "licenses:check": "license-checker --summary --production --excludePrivatePackages",
    "licenses:report": "license-checker --csv --out license-report.csv --production --excludePrivatePackages",
    "licenses:verify": "license-checker --failOn 'GPL;AGPL;SSPL' --production --excludePrivatePackages",
    "licenses:summary": "license-checker --summary --production",
    "licenses:all": "npm run licenses:report && npm run licenses:verify"
  }
}
```

#### Phase 4: Create License Policy Document (1 hour)

**File to Create**: `/Users/adamstack/SmilePile/LICENSE_POLICY.md`

This document defines acceptable open-source licenses, prohibited licenses (GPL, AGPL, SSPL), and the compliance process for adding new dependencies.

#### Phase 5: Documentation (30 minutes)

Update `website/README.md` and `CONTRIBUTING.md` with license compliance instructions and process for adding new dependencies.

#### Phase 6: Update .gitignore (5 minutes)

Update `/Users/adamstack/SmilePile/website/.gitignore`:
```bash
# License reports (regenerate on demand, don't commit)
license-report.csv
```

#### Phase 7: Validation (30 minutes)

**Validation Checklist**:
- [ ] license-checker verified installed
- [ ] Initial license scan completed
- [ ] license-report.csv generated and reviewed
- [ ] No GPL, AGPL, or SSPL licenses found (or exceptions documented)
- [ ] npm scripts added to package.json
- [ ] All npm scripts tested and functional
- [ ] LICENSE_POLICY.md created and comprehensive
- [ ] LICENSE_EXCEPTIONS.md created (even if empty)
- [ ] website/README.md updated with license instructions
- [ ] CONTRIBUTING.md updated with compliance process
- [ ] website/.gitignore updated to exclude license-report.csv
- [ ] All files committed to repository

**Success Criteria**:
- `npm run licenses:verify` exits with code 0 (success)
- License report shows only approved licenses
- Documentation is clear and actionable
- Policy document is comprehensive

### Potential Issues & Mitigation

| Issue | Probability | Mitigation |
|-------|-------------|------------|
| GPL/AGPL dependency found | LOW | Have list of alternatives ready; current stack is all MIT |
| "UNKNOWN" license found | MEDIUM | Manually check package repository for LICENSE file |
| Transitive dependency has prohibited license | LOW | license-checker scans full tree; evaluate necessity |
| License changes in future updates | MEDIUM | Run licenses:verify on every dependency update |

### Testing Strategy

**Pre-Deployment**:
- [ ] Test license-checker on current dependencies
- [ ] Verify all current licenses are MIT or Apache-2.0
- [ ] Test npm scripts execute without errors
- [ ] Review CSV report format and completeness

**Post-Deployment**:
- [ ] Run full license check: `npm run licenses:all`
- [ ] Verify exit code 0 (no prohibited licenses)
- [ ] Review license-report.csv for accuracy

### Rollback Plan

If license compliance checking causes issues:
1. Remove npm scripts from `website/package.json`
2. Delete `LICENSE_POLICY.md`
3. Delete `LICENSE_EXCEPTIONS.md`
4. Remove license compliance sections from README and CONTRIBUTING.md
5. Remove `license-report.csv` from .gitignore
6. Commit and push
7. Document reason for rollback

**Rollback Time**: < 10 minutes

---

## Story 4: Add ESLint Security Plugins

### Overview
Install ESLint with security plugins to detect vulnerabilities (XSS, hardcoded secrets, unsafe patterns) in website JavaScript/TypeScript code.

### Pre-Implementation Checklist
- [ ] Review website codebase structure: `/Users/adamstack/SmilePile/website/src/`
- [ ] Verify Node.js version: `node --version` (need 18+)
- [ ] Back up website directory (optional, for safety)
- [ ] Allocate time for fixing critical errors (up to 2 hours)

### Implementation Steps

#### Phase 1: Installation (30 minutes)

**Commands to Execute**:
```bash
cd /Users/adamstack/SmilePile/website

# Install ESLint and security plugins
npm install --save-dev \
  eslint@^8.57.0 \
  eslint-plugin-security@^2.1.0 \
  eslint-plugin-no-secrets@^1.0.2 \
  @typescript-eslint/parser@^6.21.0 \
  @typescript-eslint/eslint-plugin@^6.21.0 \
  eslint-plugin-astro@^0.31.4

# Verify installation
npx eslint --version
```

#### Phase 2: Configuration (1 hour)

**File to Create**: `/Users/adamstack/SmilePile/website/.eslintrc.js`

Configuration with security rules, secret detection, and Astro support.

**File to Create**: `/Users/adamstack/SmilePile/website/.eslintignore`

Ignore patterns for build outputs, dependencies, and config files.

#### Phase 3: Fix Critical Errors (2 hours)

Process:
1. Run linter and capture output
2. Categorize findings (Critical/Warnings/False positives)
3. Fix errors iteratively
4. Re-run linter after each fix

**Commands to Execute**:
```bash
cd /Users/adamstack/SmilePile/website

# Run linter with detailed output
npx eslint . --ext .js,.jsx,.ts,.tsx,.astro --format codeframe

# Attempt auto-fix for safe issues
npx eslint . --ext .js,.jsx,.ts,.tsx,.astro --fix

# Review remaining errors
npx eslint . --ext .js,.jsx,.ts,.tsx,.astro

# Goal: 0 critical errors, <10 warnings
```

#### Phase 4: Integration (30 minutes)

**File to Modify**: `/Users/adamstack/SmilePile/website/package.json`

Add lint scripts:
```json
{
  "scripts": {
    "lint": "eslint . --ext .js,.jsx,.ts,.tsx,.astro",
    "lint:fix": "eslint . --ext .js,.jsx,.ts,.tsx,.astro --fix",
    "lint:security": "eslint . --ext .js,.jsx,.ts,.tsx,.astro --plugin security --plugin no-secrets",
    "lint:report": "eslint . --ext .js,.jsx,.ts,.tsx,.astro --format json --output-file eslint-report.json",
    "prebuild": "npm run lint"
  }
}
```

#### Phase 5: Documentation (30 minutes)

Update `website/README.md` with linting instructions and create `.vscode/settings.json` for IDE integration.

Update `CONTRIBUTING.md` with code quality and linting workflow.

#### Phase 6: Create Test File (15 minutes)

Create temporary test file to validate ESLint detects security issues, then delete after testing.

#### Phase 7: Update .gitignore (5 minutes)

Update `/Users/adamstack/SmilePile/website/.gitignore`:
```bash
# ESLint reports (regenerate on demand, don't commit)
eslint-report.json
```

#### Phase 8: Validation (30 minutes)

**Validation Checklist**:
- [ ] ESLint and all plugins installed
- [ ] .eslintrc.js configuration created
- [ ] .eslintignore file created
- [ ] npm run lint executes without critical errors
- [ ] npm run lint:fix works correctly
- [ ] npm run lint:security works
- [ ] npm run lint:report generates JSON report
- [ ] prebuild script runs linting before build
- [ ] Test file detected all 5 security issues
- [ ] Test file deleted after validation
- [ ] .vscode/settings.json created for IDE integration
- [ ] website/README.md updated with linting instructions
- [ ] CONTRIBUTING.md updated with linting workflow
- [ ] website/.gitignore updated to exclude eslint-report.json
- [ ] All critical errors fixed (0 errors remaining)
- [ ] All files committed to repository

**Success Criteria**:
- `npm run lint` exits with code 0 (success, no errors)
- Warnings < 10 (if any)
- Critical security errors = 0
- Documentation is comprehensive
- IDE integration configured

### Potential Issues & Mitigation

| Issue | Probability | Mitigation |
|-------|-------------|------------|
| Many security errors found | HIGH | Allocate 2 hours for fixing; prioritize critical errors |
| False positives overwhelm | MEDIUM | Carefully add to ignoreContent; document justification |
| Linter slows down build | LOW | ESLint is fast; website codebase is small |
| Plugin incompatibilities | LOW | Use tested version combinations from story |
| Astro-specific linting issues | MEDIUM | Use eslint-plugin-astro for Astro file support |

### Testing Strategy

**Pre-Deployment**:
- [ ] Test ESLint on clean codebase (existing code)
- [ ] Verify test-security.js triggers all expected errors
- [ ] Test auto-fix doesn't break code
- [ ] Test prebuild script integration

**Post-Deployment**:
- [ ] Run full lint: `npm run lint`
- [ ] Verify 0 critical errors
- [ ] Test build process: `npm run build` (linting runs first)
- [ ] Test IDE integration in VS Code
- [ ] Intentionally introduce security issue, verify linter catches it

### Rollback Plan

If ESLint integration causes issues:
1. Uninstall ESLint packages: `npm uninstall eslint eslint-plugin-security eslint-plugin-no-secrets @typescript-eslint/parser @typescript-eslint/eslint-plugin eslint-plugin-astro`
2. Delete `.eslintrc.js`
3. Delete `.eslintignore`
4. Delete `.vscode/settings.json`
5. Remove lint scripts from package.json
6. Remove `prebuild` script
7. Remove linting sections from README and CONTRIBUTING.md
8. Remove eslint-report.json from .gitignore
9. Commit and push
10. Document reason for rollback

**Rollback Time**: < 15 minutes

---

## Cross-Story Risk Assessment

### Risk Matrix

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Multiple stories fail simultaneously | LOW | HIGH | Independent implementations; each can be rolled back separately |
| Configuration conflicts between tools | LOW | MEDIUM | Tools are independent (Dependabot/GitHub, gitleaks/local, ESLint/website only) |
| Overwhelming number of findings | MEDIUM | MEDIUM | Expect findings on first run; triage by severity; address critical issues first |
| Developer workflow disruption | LOW | MEDIUM | All tools are opt-in or non-blocking initially; education and documentation provided |
| Build process breakage | LOW | HIGH | ESLint prebuild is only build-blocking change; thoroughly test before committing |
| GitHub API rate limits (Dependabot) | LOW | LOW | Dependabot is GitHub native, no external API limits |

### Dependency Analysis

**No dependencies between stories**:
- SECURITY-001 (Dependabot): Pure GitHub configuration
- SECURITY-002 (gitleaks): Local tool, independent
- SECURITY-003 (ESLint): Website-only, npm-based
- SECURITY-004 (License checker): Website-only, npm-based, read-only

**Benefit of combined implementation**:
- Gitleaks and ESLint both detect secrets (complementary, not conflicting)
- License checker verifies ESLint plugin licenses are compliant
- Dependabot will monitor ESLint package vulnerabilities

---

## Testing Strategy (Overall)

### Pre-Implementation Testing

**Environment Verification**:
```bash
# Verify all required tools installed
gitleaks --version                    # Should: v8.x.x
license-checker --version             # Should: v25.x.x or newer
node --version                        # Should: v18.x or v20.x
npm --version                         # Should: v9.x or v10.x

# Verify directory structure
ls -la /Users/adamstack/SmilePile/.github/
ls -la /Users/adamstack/SmilePile/website/
ls -la /Users/adamstack/SmilePile/android/

# Verify git repository status
cd /Users/adamstack/SmilePile
git status
# Should: Clean working directory or only expected changes
```

### Integration Testing

After all 4 stories implemented:

**Combined Verification**:
```bash
cd /Users/adamstack/SmilePile

# 1. Verify Dependabot (GitHub UI)
# - Check GitHub Security tab
# - Verify 3 ecosystems detected

# 2. Run gitleaks scan
gitleaks detect --config .gitleaks.toml --source . --verbose

# 3. Run license compliance
cd website
npm run licenses:verify

# 4. Run ESLint
npm run lint

# 5. Test build process
npm run build
```

---

## Success Metrics

### Story-Level Metrics

| Story | Success Criteria | Measurement |
|-------|------------------|-------------|
| SECURITY-001 | Dependabot active | GitHub Security tab shows Dependabot; 3/3 ecosystems detected |
| SECURITY-002 | Git history clean or findings documented | gitleaks scan complete; SECURITY_AUDIT_RESULTS.md created; exit code 0 with config |
| SECURITY-003 | ESLint passes | `npm run lint` exit code 0; 0 critical errors |
| SECURITY-004 | No prohibited licenses | `npm run licenses:verify` exit code 0; no GPL/AGPL/SSPL |

### Sprint-Level Metrics

**Quantitative**:
- Configuration files created: 10+ files
- Documentation pages created: 5+ documents
- Security tools integrated: 4 tools
- Estimated security confidence increase: 85% → 95%

**Qualitative**:
- Automated vulnerability detection operational
- Historical secret audit complete
- License compliance verified
- Developer workflow documented
- All changes non-breaking to existing functionality

---

## Appendix A: File Inventory

### Files to Create

| File Path | Story | Purpose |
|-----------|-------|---------|
| `.github/dependabot.yml` | SEC-001 | Dependabot configuration |
| `.gitleaks.toml` | SEC-002 | Gitleaks configuration with allowlist |
| `SECURITY_AUDIT_RESULTS.md` | SEC-002 | Gitleaks historical scan results |
| `.githooks/pre-commit` | SEC-002 | Pre-commit hook for secret scanning |
| `.githooks/README.md` | SEC-002 | Hook installation instructions |
| `SECURITY.md` | SEC-002 | Security policy and practices |
| `LICENSE_POLICY.md` | SEC-004 | License compliance policy |
| `LICENSE_EXCEPTIONS.md` | SEC-004 | License exception tracking |
| `website/.eslintrc.js` | SEC-003 | ESLint configuration |
| `website/.eslintignore` | SEC-003 | ESLint ignore patterns |
| `website/.vscode/settings.json` | SEC-003 | VS Code ESLint integration |

### Files to Modify

| File Path | Story | Changes |
|-----------|-------|---------|
| `CONTRIBUTING.md` | SEC-001, SEC-002, SEC-003, SEC-004 | Add security workflow sections |
| `website/package.json` | SEC-003, SEC-004 | Add lint and license scripts, add ESLint dependencies |
| `website/README.md` | SEC-003, SEC-004 | Add linting and license instructions |
| `.gitignore` | SEC-002 | Ignore gitleaks report files |
| `website/.gitignore` | SEC-003, SEC-004 | Ignore ESLint and license reports |

---

## Appendix B: Estimated Timeline

### Day 1 (4 hours)
- **Hour 1**: SECURITY-001 (Dependabot) - Complete implementation
- **Hour 2-3**: SECURITY-002 (gitleaks) - Historical scan and configuration
- **Hour 4**: SECURITY-002 (gitleaks) - Documentation and validation

### Day 2 (4.5 hours)
- **Hour 1**: SECURITY-002 (gitleaks) - Complete pre-commit hook
- **Hour 2-3**: SECURITY-004 (License checker) - Implementation and documentation
- **Hour 4**: SECURITY-004 (License checker) - Validation

### Day 3 (5 hours)
- **Hour 1**: SECURITY-003 (ESLint) - Installation and configuration
- **Hour 2-4**: SECURITY-003 (ESLint) - Fix critical errors (allocate extra time)
- **Hour 5**: SECURITY-003 (ESLint) - Documentation and validation

**Total**: 13.5 hours across 3 days

**Buffer**: Each story includes 15-30 minute buffer; total sprint buffer ~1.5 hours

---

## Document Approval

**Prepared By**: Developer Agent (Atlas Phase 3)
**Date**: 2025-10-02
**Status**: READY FOR IMPLEMENTATION

**Next Steps**:
1. Review this plan with stakeholders
2. Obtain approvals
3. Proceed to Atlas Phase 4: Security Review
4. Then to Atlas Phase 5: Implementation

---

**END OF TECHNICAL IMPLEMENTATION PLAN**
