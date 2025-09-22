# 02: Developer Workflow Standards

These standards define the precise, step-by-step workflow a developer must follow locally to ensure their work is clean, correct, and ready for review.

## 1. Before Starting Work

Before writing a single line of code, ensure your environment is clean and up-to-date.

```bash
# 1. Check your git status. It must be clean.
git status

# 2. Pull the latest code from the main branch.
git pull origin main

# 3. Install the exact dependencies from the lock file.
npm ci

# 4. Run all baseline validation checks to ensure the starting point is green.
npm run lint
npm run typecheck
npm run test
npm run build
```

## 2. During Development

-   **Commit Logically**: Make small, logical commits. Each commit should represent a single, complete thought or change. This makes reviews easier and allows for simple rollbacks if needed.
-   **Validate Continuously**: Run validation checks (`lint`, `typecheck`) frequently as you work. Do not wait until the end to discover and fix issues.
-   **Follow the Patterns**: Adhere strictly to the project's existing architectural and code quality standards. If you see multiple patterns, ask for clarification before adding another.

## 3. Before Submitting for Review

Before you create a pull request, you must perform a final, comprehensive self-validation. The goal is to catch any issues yourself before the Peer Reviewer does.

### Final Checklist

-   [ ] All functional requirements have been implemented.
-   [ ] All acceptance criteria have been met.
-   [ ] All validation commands pass (`lint`, `typecheck`, `test`, `build`).
-   [ ] All debugging statements (`console.log`) have been removed or conditionally wrapped.
-   [ ] The code has been tested on all required platforms (iOS, Android, Web).
-   [ ] All necessary documentation (e.g., release notes) has been updated.
-   [ ] You have gathered all required evidence (screenshots, command outputs) to prove your work is complete.

### Automated Thresholds

The deployment script will enforce certain thresholds. You must check these before submitting:

```bash
# Check for unwrapped console logs (must be 0 in production code)
grep -r "console\." src/ | grep -v "NODE_ENV"

# Check for tech debt markers (must be below project threshold)
grep -r "TODO\|FIXME" src/
```
