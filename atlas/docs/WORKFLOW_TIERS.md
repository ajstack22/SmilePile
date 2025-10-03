# Atlas Workflow Tiers

## Philosophy

Not every task needs a 9-phase workflow. Atlas provides **three tiers** based on task complexity and risk.

## Quick Reference

| Task Type | Tier | Phases | Time | Example |
|-----------|------|--------|------|---------|
| Trivial change | **Quick** | 2 phases | 5-10 min | Color change, text update, typo fix |
| Standard feature/bug | **Standard** | 5 phases | 30-60 min | Bug fix, small feature, refactor |
| Epic/Complex feature | **Full** | 9 phases | 2-4 hours | New module, cross-platform feature, major refactor |

---

## Tier 1: Quick Workflow (Trivial Changes)

**Use for:**
- UI text changes
- Color/style tweaks
- Simple configuration updates
- Documentation fixes
- Single-line bug fixes

**Workflow:**
```
Phase 1: Make Change (Developer)
  - Locate code
  - Make change
  - Verify locally

Phase 2: Deploy (DevOps)
  - Run tests
  - Deploy via quality script
```

**Command:**
```
"Quick change: [DESCRIPTION]. Use Atlas Quick workflow - make change and deploy."
```

**Skip:**
- Story creation (no story needed)
- Planning (change is obvious)
- Adversarial review (minimal risk)
- Validation (tests cover it)

---

## Tier 2: Standard Workflow (Most Tasks)

**Use for:**
- Bug fixes
- Small features (1-3 files)
- Code refactoring
- Test additions
- Configuration changes with logic

**Workflow:**
```
Phase 1: Research
  - Understand current implementation
  - Identify all affected files

Phase 2: Plan
  - Design approach
  - List file changes

Phase 3: Implement
  - Make changes
  - Update tests

Phase 4: Review
  - Peer review for edge cases
  - Security check if applicable

Phase 5: Deploy
  - Run full test suite
  - Deploy via quality script
```

**Command:**
```
"Standard task: [DESCRIPTION]. Use Atlas Standard workflow - research, plan, implement, review, deploy."
```

**Skip:**
- Story creation (lightweight requirements doc instead)
- Separate validation phase (review covers it)
- Separate testing phase (included in implementation)
- Clean-up phase (minimal artifacts)

---

## Tier 3: Full Workflow (Complex Features)

**Use for:**
- New modules/features (4+ files)
- Cross-platform features
- Major refactoring
- Security-critical changes
- API design
- Database schema changes

**Workflow:**
```
Phase 1: Research
  - Deep codebase exploration
  - Pattern identification
  - Dependency analysis

Phase 2: Story Creation
  - Formal user story
  - Acceptance criteria
  - Success metrics

Phase 3: Planning
  - Technical design
  - File-by-file implementation plan
  - Risk assessment

Phase 4: Adversarial Review
  - Security audit
  - Edge case analysis
  - Performance implications

Phase 5: Implementation
  - Parallel coding when possible
  - Incremental testing
  - Documentation updates

Phase 6: Testing
  - Functional testing
  - UI/UX verification
  - Code quality review

Phase 7: Validation
  - Acceptance criteria verification
  - Product sign-off

Phase 8: Clean-up
  - Remove temp files
  - Organize documentation
  - Archive evidence

Phase 9: Deployment
  - Full test suite
  - Quality gates
  - Deploy via quality script
```

**Command:**
```
"Complex feature: [DESCRIPTION]. Use Atlas Full workflow with all 9 phases."
```

---

## Decision Matrix

### Start with Quick if:
- ✅ Change affects 1 file
- ✅ No logic changes
- ✅ Zero risk of side effects
- ✅ Existing tests cover it
- ✅ Takes < 5 minutes to understand

### Use Standard if:
- ✅ Affects 2-5 files
- ✅ Some logic changes
- ✅ Low-medium risk
- ✅ Needs new tests
- ✅ Requires design thinking

### Use Full if:
- ✅ Affects 6+ files OR creates new module
- ✅ Complex logic/state management
- ✅ Security implications
- ✅ Cross-platform coordination
- ✅ Needs formal requirements
- ✅ High risk of breaking changes

---

## Tier Examples

### Quick Workflow Examples

**Color Change:**
```
"Change primary button color from blue to green. Atlas Quick workflow."
```

**Text Update:**
```
"Update welcome message on login screen. Atlas Quick workflow."
```

**Typo Fix:**
```
"Fix typo in error message. Atlas Quick workflow."
```

### Standard Workflow Examples

**Bug Fix:**
```
"Fix null pointer when user uploads photo without category. Atlas Standard workflow."
```

**Small Feature:**
```
"Add confirmation dialog before deleting photos. Atlas Standard workflow."
```

**Refactor:**
```
"Extract photo validation logic into separate utility class. Atlas Standard workflow."
```

### Full Workflow Examples

**Epic Feature:**
```
"Implement dark mode with theme persistence and component updates across both platforms. Atlas Full workflow."
```

**Cross-Platform Feature:**
```
"Add cloud backup with encryption, progress tracking, and restore capability. Atlas Full workflow."
```

**Major Refactor:**
```
"Migrate from Room to SQLDelight for shared database layer. Atlas Full workflow."
```

---

## Escalation Rules

### When to Escalate from Quick → Standard:
- During implementation, you realize it affects multiple files
- Tests fail and require new test cases
- Edge cases emerge that need peer review

### When to Escalate from Standard → Full:
- Scope expands to 6+ files
- Security concerns emerge
- Cross-platform coordination needed
- Formal requirements become necessary

### How to Escalate:
```
"Escalating to [TIER] workflow. [REASON]"
```

Then restart from Phase 1 of the new tier.

---

## Phase Collapsing Guidelines

Even within tiers, you can intelligently collapse phases:

### Safe to Collapse:
- **Research + Planning** (if you already know the code)
- **Implementation + Testing** (for test-driven development)
- **Review + Validation** (if reviewer can validate acceptance criteria)

### Never Collapse:
- **Implementation + Deployment** (always review before deploy)
- **Security Review** (when security is involved)
- **Testing** (always verify before shipping)

---

## Anti-Patterns

### ❌ Don't Do This:
```
"Use Quick workflow for adding new authentication system"
(Too complex - use Full)

"Use Full workflow for fixing a typo"
(Overkill - use Quick)

"Skip deployment phase to save time"
(Never skip - tests are mandatory)
```

### ✅ Do This Instead:
```
"Use Full workflow for adding authentication system"

"Use Quick workflow for typo - just fix and deploy"

"Standard workflow - implement, review, then deploy with tests"
```

---

## Success Metrics by Tier

### Quick Workflow Success:
- Change deployed in < 15 minutes
- Tests pass
- No rollbacks

### Standard Workflow Success:
- Feature complete in < 2 hours
- All edge cases covered
- Tests pass
- Peer review approved

### Full Workflow Success:
- Epic complete with full documentation
- 100% acceptance criteria met
- Zero defects in production
- Full evidence trail

---

## Default Recommendation

**When in doubt, use Standard workflow.** It's the right balance for 80% of tasks.

Only drop to Quick when the change is genuinely trivial, and only escalate to Full when complexity/risk clearly demands it.
