# Sprint 10 Security Enhancements - Command Reference

**Quick reference for implementation commands**
**Last Updated**: 2025-10-02

---

## Pre-Implementation Verification

```bash
# Verify tools installed
gitleaks --version                    # Expected: v8.x.x
license-checker --version             # Expected: v25.x.x+
node --version                        # Expected: v18.x or v20.x
npm --version                         # Expected: v9.x or v10.x

# Verify directory structure
ls -la /Users/adamstack/SmilePile/.github/
ls -la /Users/adamstack/SmilePile/website/
ls -la /Users/adamstack/SmilePile/android/

# Verify git status
cd /Users/adamstack/SmilePile
git status
```

---

## SECURITY-001: GitHub Dependabot

### Implementation

```bash
# Create .github directory (if needed)
mkdir -p /Users/adamstack/SmilePile/.github

# Create dependabot.yml (use Write tool)
# ... file creation ...

# Commit and push
cd /Users/adamstack/SmilePile
git add .github/dependabot.yml
git commit -m "chore: Enable GitHub Dependabot for npm, Gradle, and GitHub Actions"
git push origin main
```

### Verification

```bash
# Manual steps (GitHub UI):
# 1. Navigate to: https://github.com/[username]/SmilePile/security
# 2. Verify "Dependabot alerts" section exists
# 3. Check Settings > Security > Code security and analysis
# 4. Verify all 3 features enabled:
#    - Dependabot alerts: ON
#    - Dependabot security updates: ON
#    - Dependabot version updates: ON
```

---

## SECURITY-002: gitleaks Secret Scanning

### Phase 1: Historical Scan

```bash
cd /Users/adamstack/SmilePile

# Run full history scan (JSON)
gitleaks detect \
  --source . \
  --report-path gitleaks-report.json \
  --report-format json \
  --verbose

# Run full history scan (SARIF)
gitleaks detect \
  --source . \
  --report-path gitleaks-report.sarif \
  --report-format sarif \
  --verbose

# Check exit code
echo "Exit code: $?"
# 0 = no secrets, 1 = secrets found

# Review findings
cat gitleaks-report.json
```

### Phase 2: Create Configuration

```bash
cd /Users/adamstack/SmilePile

# Create .gitleaks.toml (use Write tool)
# ... file creation ...

# Test configuration
gitleaks detect --config .gitleaks.toml --source . --verbose

# Expected: Exit code 0 after allowlist applied
echo "Exit code: $?"
```

### Phase 3: Create Pre-commit Hook

```bash
cd /Users/adamstack/SmilePile

# Create .githooks directory
mkdir -p .githooks

# Create pre-commit hook (use Write tool)
# ... file creation ...

# Make executable
chmod +x .githooks/pre-commit

# Test hook (optional)
git config core.hooksPath .githooks
echo "test-key=sk-live-1234567890abcdefghijklmnopqrstuvwxyz" > test.txt
git add test.txt
git commit -m "test"
# Expected: Hook blocks commit
rm test.txt
git config --unset core.hooksPath
```

### Phase 4: Update .gitignore

```bash
cd /Users/adamstack/SmilePile

# Add to .gitignore
echo "" >> .gitignore
echo "# Gitleaks reports" >> .gitignore
echo "gitleaks-report.json" >> .gitignore
echo "gitleaks-report.sarif" >> .gitignore
echo "gitleaks-report.txt" >> .gitignore

git add .gitignore
```

### Phase 5: Commit All Changes

```bash
cd /Users/adamstack/SmilePile

git add .gitleaks.toml
git add .githooks/
git add SECURITY_AUDIT_RESULTS.md
git add SECURITY.md
git add .gitignore
git commit -m "chore: Integrate gitleaks secret scanning"
git push origin main
```

---

## SECURITY-004: License Compliance Checking

### Phase 1: Verify Installation

```bash
# Check if license-checker installed
which license-checker
# Expected: /Users/adamstack/.npm-global/bin/license-checker

# Verify version
license-checker --version

# Install if needed
npm install -g license-checker
```

### Phase 2: Run Initial Scan

```bash
cd /Users/adamstack/SmilePile/website

# Run summary check
license-checker --summary --production --excludePrivatePackages

# Generate CSV report
license-checker \
  --csv \
  --out license-report.csv \
  --production \
  --excludePrivatePackages

# Verify no prohibited licenses
license-checker \
  --failOn 'GPL;AGPL;SSPL' \
  --production \
  --excludePrivatePackages

echo "Exit code: $?"
# Expected: 0 (no prohibited licenses)

# Review CSV report
cat license-report.csv
```

### Phase 3: Update package.json

```bash
cd /Users/adamstack/SmilePile/website

# Edit package.json (use Edit tool) to add scripts:
# "licenses:check": "license-checker --summary --production --excludePrivatePackages"
# "licenses:report": "license-checker --csv --out license-report.csv --production --excludePrivatePackages"
# "licenses:verify": "license-checker --failOn 'GPL;AGPL;SSPL' --production --excludePrivatePackages"
# "licenses:summary": "license-checker --summary --production"
# "licenses:all": "npm run licenses:report && npm run licenses:verify"

# Test new scripts
npm run licenses:check
npm run licenses:report
npm run licenses:verify
npm run licenses:summary
npm run licenses:all
```

### Phase 4: Update .gitignore

```bash
cd /Users/adamstack/SmilePile/website

# Add to .gitignore
echo "" >> .gitignore
echo "# License reports" >> .gitignore
echo "license-report.csv" >> .gitignore

git add .gitignore
```

### Phase 5: Commit All Changes

```bash
cd /Users/adamstack/SmilePile

git add LICENSE_POLICY.md
git add LICENSE_EXCEPTIONS.md
git add website/package.json
git add website/.gitignore
git add CONTRIBUTING.md
git commit -m "chore: Add license compliance checking"
git push origin main
```

---

## SECURITY-003: ESLint Security Plugins

### Phase 1: Installation

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
# Expected: v8.57.0 or similar

# Check plugins installed
npm list eslint-plugin-security
npm list eslint-plugin-no-secrets
```

### Phase 2: Configuration

```bash
cd /Users/adamstack/SmilePile/website

# Create .eslintrc.js (use Write tool)
# ... file creation ...

# Create .eslintignore (use Write tool)
# ... file creation ...

# Test configuration
npx eslint . --ext .js,.jsx,.ts,.tsx,.astro
# Review output
```

### Phase 3: Fix Critical Errors

```bash
cd /Users/adamstack/SmilePile/website

# Run linter with detailed output
npx eslint . --ext .js,.jsx,.ts,.tsx,.astro --format codeframe

# Attempt auto-fix
npx eslint . --ext .js,.jsx,.ts,.tsx,.astro --fix

# Review remaining errors
npx eslint . --ext .js,.jsx,.ts,.tsx,.astro

# Goal: 0 critical errors, <10 warnings
```

### Phase 4: Update package.json

```bash
cd /Users/adamstack/SmilePile/website

# Edit package.json (use Edit tool) to add scripts:
# "lint": "eslint . --ext .js,.jsx,.ts,.tsx,.astro"
# "lint:fix": "eslint . --ext .js,.jsx,.ts,.tsx,.astro --fix"
# "lint:security": "eslint . --ext .js,.jsx,.ts,.tsx,.astro --plugin security --plugin no-secrets"
# "lint:report": "eslint . --ext .js,.jsx,.ts,.tsx,.astro --format json --output-file eslint-report.json"
# "prebuild": "npm run lint"

# Test scripts
npm run lint
npm run lint:fix
npm run lint:security
npm run lint:report
```

### Phase 5: Create Test File (for validation)

```bash
cd /Users/adamstack/SmilePile/website

# Create test file (use Write tool)
cat > test-security.js << 'EOF'
// Test file - should trigger ESLint security errors
const apiKey = "sk-live-1234567890abcdefghijklmnopqrstuvwxyz";
eval("console.log('unsafe')");
const { exec } = require('child_process');
exec('ls -la');
const userInput = ".*";
new RegExp(userInput);
const crypto = require('crypto');
crypto.pseudoRandomBytes(16);
EOF

# Test linter detects issues
npx eslint test-security.js
# Expected: 5 errors reported

# Delete test file
rm test-security.js
```

### Phase 6: Create VS Code Settings

```bash
cd /Users/adamstack/SmilePile/website

# Create .vscode directory
mkdir -p .vscode

# Create settings.json (use Write tool)
# ... file creation ...
```

### Phase 7: Update .gitignore

```bash
cd /Users/adamstack/SmilePile/website

# Add to .gitignore
echo "" >> .gitignore
echo "# ESLint reports" >> .gitignore
echo "eslint-report.json" >> .gitignore

git add .gitignore
```

### Phase 8: Commit All Changes

```bash
cd /Users/adamstack/SmilePile

git add website/.eslintrc.js
git add website/.eslintignore
git add website/.vscode/settings.json
git add website/package.json
git add website/package-lock.json
git add website/.gitignore
git add CONTRIBUTING.md
git commit -m "chore: Add ESLint security plugins"
git push origin main
```

---

## Integration Testing

### Run All Security Checks

```bash
cd /Users/adamstack/SmilePile

# 1. Dependabot (GitHub UI)
# - Check: https://github.com/[username]/SmilePile/security
# - Verify: Dependabot section exists
# - Verify: 3 ecosystems detected

# 2. gitleaks scan
gitleaks detect --config .gitleaks.toml --source . --verbose
echo "Exit code: $?" # Expected: 0

# 3. License compliance
cd website
npm run licenses:verify
echo "Exit code: $?" # Expected: 0

# 4. ESLint
npm run lint
echo "Exit code: $?" # Expected: 0

# 5. Build process (includes linting via prebuild)
npm run build
echo "Exit code: $?" # Expected: 0
```

### Validate All Tools Functional

```bash
cd /Users/adamstack/SmilePile

# Check all configuration files exist
ls -la .github/dependabot.yml
ls -la .gitleaks.toml
ls -la .githooks/pre-commit
ls -la website/.eslintrc.js
ls -la website/.eslintignore
ls -la LICENSE_POLICY.md

# Check all documentation exists
ls -la SECURITY.md
ls -la SECURITY_AUDIT_RESULTS.md
ls -la LICENSE_EXCEPTIONS.md

# Verify git status
git status
# Expected: Clean working directory
```

---

## Rollback Commands

### SECURITY-001: Dependabot

```bash
cd /Users/adamstack/SmilePile
rm .github/dependabot.yml
git add .github/dependabot.yml
git commit -m "Rollback: Remove Dependabot configuration"
git push origin main

# Also: Disable in GitHub Settings > Security
```

### SECURITY-002: gitleaks

```bash
cd /Users/adamstack/SmilePile
rm .gitleaks.toml
rm -rf .githooks
rm SECURITY_AUDIT_RESULTS.md
# Revert SECURITY.md changes (use git checkout)
# Revert .gitignore changes
git checkout HEAD -- .gitignore
git add .
git commit -m "Rollback: Remove gitleaks integration"
git push origin main
```

### SECURITY-004: License Checker

```bash
cd /Users/adamstack/SmilePile
rm LICENSE_POLICY.md
rm LICENSE_EXCEPTIONS.md
# Revert website/package.json changes
cd website
git checkout HEAD -- package.json
git checkout HEAD -- .gitignore
cd ..
git add .
git commit -m "Rollback: Remove license compliance checking"
git push origin main
```

### SECURITY-003: ESLint

```bash
cd /Users/adamstack/SmilePile/website

# Uninstall ESLint packages
npm uninstall eslint eslint-plugin-security eslint-plugin-no-secrets \
  @typescript-eslint/parser @typescript-eslint/eslint-plugin \
  eslint-plugin-astro

# Remove configuration files
rm .eslintrc.js
rm .eslintignore
rm -rf .vscode

# Revert package.json and .gitignore
git checkout HEAD -- package.json
git checkout HEAD -- .gitignore

cd /Users/adamstack/SmilePile
git add .
git commit -m "Rollback: Remove ESLint security plugins"
git push origin main
```

---

## Troubleshooting

### gitleaks: Command Not Found

```bash
# Verify installation
which gitleaks

# If not found, install
brew install gitleaks

# Verify installation
gitleaks --version
```

### license-checker: Command Not Found

```bash
# Verify installation
which license-checker

# If not found, install globally
npm install -g license-checker

# Verify installation
license-checker --version
```

### ESLint: Plugin Not Found

```bash
cd /Users/adamstack/SmilePile/website

# Verify plugins installed
npm list eslint-plugin-security
npm list eslint-plugin-no-secrets

# Reinstall if needed
npm install --save-dev eslint-plugin-security eslint-plugin-no-secrets
```

### gitleaks: Exit Code 1 (Secrets Found)

```bash
# Review findings
cat gitleaks-report.json

# For each finding:
# 1. If true positive: Rotate credential immediately
# 2. If false positive: Add to .gitleaks.toml allowlist

# Re-run after fixing
gitleaks detect --config .gitleaks.toml --source . --verbose
```

### ESLint: Many Errors

```bash
cd /Users/adamstack/SmilePile/website

# Prioritize errors by severity
npx eslint . --ext .js,.jsx,.ts,.tsx,.astro --format codeframe

# Auto-fix safe issues
npx eslint . --ext .js,.jsx,.ts,.tsx,.astro --fix

# For remaining errors:
# 1. Fix critical security issues (eval, secrets, child_process)
# 2. Add false positives to .eslintrc.js ignoreContent
# 3. Document warnings in code comments for later
```

### Dependabot: Not Detecting Ecosystems

```bash
# Verify package.json exists
ls -la /Users/adamstack/SmilePile/website/package.json

# Verify build.gradle.kts exists
ls -la /Users/adamstack/SmilePile/android/build.gradle.kts

# Verify GitHub Actions workflows exist
ls -la /Users/adamstack/SmilePile/.github/workflows/

# Wait 10-15 minutes for GitHub to process
# Check Insights > Dependency graph
```

---

## Quick Status Check

```bash
cd /Users/adamstack/SmilePile

# Check all tools in one command
echo "=== Dependabot ===" && \
  (ls -la .github/dependabot.yml && echo "✓ Configured") || echo "✗ Not configured"

echo "=== gitleaks ===" && \
  (gitleaks detect --config .gitleaks.toml --source . --no-banner 2>&1 | tail -1)

echo "=== License Checker ===" && \
  (cd website && npm run licenses:verify > /dev/null 2>&1 && echo "✓ No prohibited licenses") || echo "✗ Issues found"

echo "=== ESLint ===" && \
  (cd website && npm run lint > /dev/null 2>&1 && echo "✓ Passes") || echo "✗ Errors found"

echo "=== Build ===" && \
  (cd website && npm run build > /dev/null 2>&1 && echo "✓ Success") || echo "✗ Failed"
```

---

**END OF COMMAND REFERENCE**
