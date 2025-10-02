# SmilePile Git Hooks

This directory contains optional Git hooks to help maintain code quality and security.

## Available Hooks

### pre-commit
Runs gitleaks secret scanning on staged files before allowing a commit.

**What it does**:
- Scans only staged files (fast)
- Detects hardcoded secrets, API keys, passwords
- Prevents commits if secrets found
- Uses `.gitleaks.toml` configuration (respects allowlist)

**Installation** (Optional):

To enable the pre-commit hook for this repository:

```bash
git config core.hooksPath .githooks
```

To disable:

```bash
git config --unset core.hooksPath
```

**Bypassing the hook** (Not Recommended):

If you need to commit despite the hook blocking (e.g., false positive you'll fix later):

```bash
git commit --no-verify -m "Your commit message"
```

## Why Optional?

Git hooks are optional to avoid disrupting developer workflow. However, we strongly recommend enabling them for:
- Additional safety net against accidental secret commits
- Fast feedback loop (catches issues before CI/CD)
- Learning tool for secure coding practices

## Troubleshooting

### Hook not running
- Verify installation: `git config core.hooksPath`
- Should output: `.githooks`
- Verify hook is executable: `ls -la .githooks/pre-commit`

### False positives
- Review the finding carefully
- If truly a false positive, add to `.gitleaks.toml` allowlist
- See `docs/security/gitleaks-scan-results.md` for examples

### Hook runs too slow
- Pre-commit hook only scans staged files (should be fast)
- If still slow, consider disabling and relying on CI/CD scanning

## Alternative: Manual Scanning

If you prefer not to use hooks, scan manually before pushing:

```bash
gitleaks protect --staged --verbose
```

Or scan entire repository:

```bash
gitleaks detect --config .gitleaks.toml --source . --verbose
```
