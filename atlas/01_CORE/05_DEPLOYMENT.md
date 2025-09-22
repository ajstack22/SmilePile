# 05: Deployment Standards

These standards define the process and quality gates for deploying the application to any environment. The goal is to make deployments safe, reliable, and routine.

## 1. The Deployment Script

-   **Single Point of Entry**: All deployments must be executed through a single, version-controlled deployment script. Manual deployments are strictly forbidden except in a declared emergency.
-   **Automated Enforcement**: This script is the final quality gate and must be programmed to automatically enforce all critical checks.

### Mandatory Script Checks

The script must automatically perform these checks in order and fail immediately if any check does not pass:

1.  **Git Status Clean**: Ensure there are no uncommitted changes.
2.  **Validation Passes**: Run all lint, type-check, and test commands.
3.  **Automated Thresholds**: Check for `console.log` count, `TODO`/`FIXME` count, and other project-specific metrics.
4.  **No Exposed Secrets**: Scan the codebase for any accidentally committed API keys or secrets.
5.  **Build Succeeds**: The production build command must complete successfully.

## 2. Pre-Deployment Checklist

Before running the deployment script, the following manual checks must be completed:

-   [ ] All required approvals have been granted (e.g., Peer Review, PM sign-off).
-   [ ] All P0 (critical) and P1 (high) issues related to the release have been resolved.
-   [ ] The `RELEASE_NOTES.md` or equivalent documentation has been updated for the version being deployed.
-   [ ] A rollback plan is in place and understood by the person deploying.

## 3. Go/No-Go Criteria

This is the final human checkpoint before a deployment to production. A deployment is a "No-Go" if any of the following are true:

-   Any of the automated script checks are failing.
-   There are known data loss bugs or data integrity issues in the release.
-   The release fails to meet the project's documented performance standards.
-   The release has not been tested on all required platforms.
-   The rollback plan is not ready or has not been tested.

## 4. Emergency Procedures

### P0 (Critical Issue) Response Protocol

1.  **Assess**: Immediately determine the scope and impact of the issue.
2.  **Stop**: All non-essential development work is halted.
3.  **Fix**: Implement the minimal required change to resolve the issue.
4.  **Test**: The fix must be tested on all platforms, even in an emergency.
5.  **Deploy**: Deploy the fix immediately upon successful testing.
6.  **Monitor**: Closely monitor the system for at least 24 hours to ensure the fix is effective.
7.  **Post-Mortem**: Conduct a post-mortem to understand the root cause and add new preventative checks.

### Rollback

If a deployment introduces a critical issue, the first response should be to execute the rollback plan to immediately restore the previous stable version. The fix can then be worked on without ongoing user impact.
