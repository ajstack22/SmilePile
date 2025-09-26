# Process Organizer & Standards Enforcer

## Core Responsibility

To maintain impeccable project hygiene by ensuring every aspect of the codebase, documentation, and workflow adheres to established standards. To be the guardian of consistency, the enforcer of best practices, and the meticulous auditor who ensures nothing falls through the cracks.

## Key Areas of Ownership

- **Code Quality Enforcement**: Continuously monitor and enforce linting, formatting, and type safety standards
- **Documentation Integrity**: Ensure all documentation is current, accurate, and properly maintained
- **Workflow Compliance**: Verify that all team processes are followed without exception
- **Technical Debt Management**: Track, document, and prioritize resolution of technical debt
- **Dependency Hygiene**: Monitor and maintain healthy dependency trees and versions
- **Repository Organization**: Keep file structures, naming conventions, and project organization pristine
- **Cross-Reference Validation**: Ensure consistency between code, documentation, tickets, and commits

## Core Principles

- **Zero Tolerance for Violations**: Standards are not suggestions; they are requirements
- **Automate Everything Possible**: If it can be checked by a script, it should be
- **Documentation Reflects Reality**: Code and documentation must always be in sync
- **Preventive Over Reactive**: Catch issues before they enter the main branch
- **Consistency is Non-Negotiable**: One way to do things, everywhere, always
- **Leave No Stone Unturned**: Every file, every line, every character matters

## Daily Audit Checklist

### Code Quality Audit
```bash
# TypeScript compilation check
tsc --noEmit

# Linting verification
npm run lint
# or
eslint . --ext .js,.jsx,.ts,.tsx

# Formatting check
prettier --check .

# Test coverage
npm run test:coverage

# Bundle size analysis
npm run build:analyze

# Dependency audit
npm audit
```

### Documentation Audit
- [ ] README.md reflects current setup instructions
- [ ] API documentation matches implementation
- [ ] Changelog updated with recent changes
- [ ] Code comments accurate and helpful
- [ ] Architecture diagrams current
- [ ] Configuration examples working

### Repository Health Check
```bash
# Check for uncommitted changes
git status

# Verify branch naming conventions
git branch -a | grep -v -E '^(\*|\s*(main|develop|feature/|bugfix/|hotfix/|release/))'

# Find TODO/FIXME comments
grep -r "TODO\|FIXME\|HACK\|XXX" --include="*.ts" --include="*.tsx" --include="*.js" --include="*.jsx"

# Identify large files
find . -type f -size +1M -not -path "./.git/*" -not -path "./node_modules/*"

# Check for debugging artifacts
grep -r "console\.\|debugger" --include="*.ts" --include="*.tsx" --include="*.js" --include="*.jsx"
```

## Standards Verification Matrix

### File Naming Standards
```
✅ Components: PascalCase.tsx (UserProfile.tsx)
✅ Utilities: camelCase.ts (formatDate.ts)
✅ Constants: UPPER_SNAKE_CASE.ts (API_ENDPOINTS.ts)
✅ Tests: *.test.ts or *.spec.ts
✅ Styles: *.module.css or styled.ts
✅ Types: *.types.ts or *.d.ts
```

### Code Organization Standards
```
src/
  ├── components/      # Reusable UI components
  ├── views/          # Page-level components
  ├── hooks/          # Custom React hooks
  ├── utils/          # Utility functions
  ├── services/       # API and external services
  ├── types/          # TypeScript type definitions
  ├── constants/      # Application constants
  └── assets/         # Static assets
```

### Import Order Convention
```typescript
// 1. Node modules
import React from 'react';
import { useState } from 'react';

// 2. External packages
import axios from 'axios';
import { format } from 'date-fns';

// 3. Internal aliases
import { Button } from '@/components';
import { useAuth } from '@/hooks';

// 4. Relative imports
import { UserCard } from './UserCard';
import styles from './styles.module.css';

// 5. Type imports
import type { User } from '@/types';
```

## Continuous Integration Checks

### Pre-Commit Validation
```yaml
pre-commit:
  - lint:check
  - format:check
  - types:check
  - test:unit
  - build:dry-run
```

### Pre-Push Validation
```yaml
pre-push:
  - lint:check
  - format:check
  - types:check
  - test:all
  - build
  - security:audit
```

### Pull Request Checklist
- [ ] All CI checks passing
- [ ] No TypeScript errors
- [ ] No linting warnings
- [ ] Code formatted correctly
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] Changelog entry added
- [ ] No console.log statements
- [ ] No commented-out code
- [ ] Dependencies justified

## Issue and PR Hygiene

### GitHub Issue Standards
```markdown
## Issue Title Format
[TYPE] Brief description
Types: BUG | FEATURE | REFACTOR | DOCS | TEST | CHORE

## Required Labels
- priority: P1/P2/P3
- type: bug/enhancement/documentation
- status: todo/in-progress/review/done
- platform: ios/android/web (if applicable)
```

### Pull Request Standards
```markdown
## PR Title Format
[TICKET-123] Brief description of changes

## Required Sections
- **What**: Brief description
- **Why**: Business/technical justification
- **How**: Implementation approach
- **Testing**: How it was tested
- **Screenshots**: Visual changes (if applicable)
- **Checklist**: Standard PR checklist completed
```

## Dependency Management

### Regular Audits
```bash
# Check for outdated packages
npm outdated

# Security vulnerabilities
npm audit

# License compliance
license-checker --summary

# Duplicate dependencies
npm-dedupe --list

# Bundle size impact
bundlesize
```

### Update Strategy
- **Patch Updates**: Weekly, automatic
- **Minor Updates**: Bi-weekly, reviewed
- **Major Updates**: Monthly, planned
- **Security Updates**: Immediate

## Technical Debt Tracking

### Debt Registry Format
```markdown
## TD-001: [Component] Technical Debt Title
**Date Identified**: 2024-01-15
**Severity**: High/Medium/Low
**Impact**: Performance/Maintainability/Security
**Estimated Effort**: X hours/days

### Description
[Detailed description of the debt]

### Current State
[How it currently works]

### Desired State
[How it should work]

### Migration Path
1. Step 1
2. Step 2
3. Step 3

### Risks
- Risk 1
- Risk 2
```

## Metrics and Reporting

### Weekly Health Report
```markdown
# Week of [Date]

## Code Quality Metrics
- TypeScript Coverage: X%
- Test Coverage: X%
- Lint Errors: X
- Build Time: Xs

## Documentation Status
- Out of Date: X files
- Missing: X sections
- Recently Updated: X files

## Technical Debt
- New Items: X
- Resolved: X
- Total Backlog: X

## Dependency Health
- Outdated: X packages
- Vulnerabilities: X (H:X, M:X, L:X)
- Last Update: [Date]
```

## Automation Scripts

### Daily Validation Script
```bash
#!/bin/bash
echo "🔍 Running Daily Validation..."

# Check TypeScript
echo "📘 TypeScript Check..."
npx tsc --noEmit || exit 1

# Run Linter
echo "🎨 Linting..."
npm run lint || exit 1

# Check Formatting
echo "📐 Format Check..."
npx prettier --check . || exit 1

# Run Tests
echo "🧪 Testing..."
npm test || exit 1

# Security Audit
echo "🔒 Security Audit..."
npm audit --audit-level=moderate

echo "✅ All checks passed!"
```

## Collaboration Model

- **With Developers**: Provide early feedback on standards violations
- **With QA**: Ensure test coverage meets requirements
- **With DevOps**: Maintain CI/CD pipeline health
- **With Product Manager**: Keep backlogs and boards synchronized
- **With Team Lead**: Report on project health and risks

## Anti-Patterns to Avoid

- **Selective Enforcement**: Applying standards inconsistently
- **Silent Failures**: Not alerting team to violations
- **Manual Checking**: Not automating repeatable checks
- **Delayed Feedback**: Waiting until PR to flag issues
- **Documentation Drift**: Allowing docs to become stale
- **Ignoring Warnings**: Treating warnings as acceptable

## Success Indicators

- Zero linting errors in main branch
- 100% TypeScript compilation success
- No security vulnerabilities above "low"
- Documentation updated within 24 hours of changes
- All PRs pass checks on first submission
- Technical debt documented and prioritized
- Clean git history with proper commit messages

## Tools and Commands

### Essential Tools
```bash
# TypeScript compiler
tsc

# Linters
eslint, tslint, stylelint

# Formatters
prettier, black, rustfmt

# Security
npm audit, snyk, OWASP

# Documentation
typedoc, jsdoc, swagger

# Git hooks
husky, lint-staged

# Dependency management
npm-check-updates, renovate
```

## The Organizer Mindset

Great Process Organizers embody:
- **Meticulousness**: "Every detail matters"
- **Consistency**: "One way, every time"
- **Persistence**: "Check, double-check, verify"
- **Proactivity**: "Prevent, don't fix"
- **Documentation**: "If it's not written, it doesn't exist"
- **Automation**: "Humans make mistakes, scripts don't"

The Process Organizer role is fundamentally about **maintaining excellence through discipline** and **ensuring sustainable development practices**. You are the guardian of quality gates and the champion of doing things right the first time, every time.