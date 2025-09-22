# Troubleshooting Process

## Overview
A systematic, phase-based approach to debugging issues that transforms reactive firefighting into methodical problem-solving. This process identifies root causes, tests solutions scientifically, and verifies fixes completely.

## When to Use
- Bug reports from users or QA
- Performance degradation
- Unexpected behavior in production
- Integration failures
- Platform-specific issues

## Process Script
**Script**: `automation/processes/troubleshoot.py`
**Usage**: `python troubleshoot.py "path/to/bug-story.md" --claude`

## Process Owner
**Role**: ORCHESTRATOR
- The Orchestrator coordinates troubleshooting
- Spawns specialized agents for each phase
- Never debugs directly, always delegates

## The 5-Phase Process

### Phase 1: Reproduction
**Objective**: Reproduce the exact issue as reported

**Orchestrator Actions**:
1. Spawn reproduction agent with bug story
2. Review reproduction results
3. Decide if reproduction was successful
4. If not reproduced, spawn environment analysis agent

**Agent Activities**:
- Set up matching environment
- Follow reproduction steps exactly
- Document actual behavior
- Capture error logs and screenshots
- Confirm reproduction success/failure

**Evidence Required**:
- Screenshots/videos of the issue
- Complete error logs
- Environment configuration
- Exact steps taken

**Success Criteria**:
- Issue reproduced exactly as reported
- OR clear understanding why it cannot be reproduced

### Phase 2: Diagnosis
**Objective**: Identify root causes through multi-angle investigation

**Orchestrator Actions**:
1. Spawn multiple diagnosis agents IN PARALLEL:
   - Error Analysis Agent
   - Code History Agent
   - Configuration Agent
2. Synthesize findings from all agents
3. Identify primary suspects

**Investigation Areas**:
- **Error Analysis**: Deep dive into messages and stack traces
- **Code History**: When did this last work? What changed?
- **Configuration**: Environment settings and dependencies
- **Similar Patterns**: Related code that might have same issue

**Research Commands** (Agents use):
```bash
# Search for error patterns
grep -r "error message" --include="*.log"

# Check recent changes
git log --since="1 week ago" -- affected/file.js

# Find similar implementations
grep -r "similar_function" --include="*.js"
```

**Success Criteria**:
- Root cause hypothesis identified
- Multiple evidence points support hypothesis

### Phase 3: Hypothesis Testing
**Objective**: Systematically test potential fixes

**Orchestrator Actions**:
1. Prioritize hypotheses based on diagnosis
2. Spawn hypothesis testing agent for each
3. Review test results
4. Decide on working solution

**Scientific Method**:
```
For each hypothesis:
1. State hypothesis clearly
2. Make minimal change to test
3. Run verification steps
4. Document result: FIXED/PARTIAL/NO_CHANGE/WORSE
5. Revert if unsuccessful
6. Move to next hypothesis
```

**Testing Protocol**:
- ONE change at a time
- Always revert failed attempts
- Keep detailed notes
- Isolate variables

**Success Criteria**:
- At least one hypothesis shows FIXED result
- No negative side effects observed

### Phase 4: Implementation
**Objective**: Apply the complete fix properly

**Orchestrator Actions**:
1. Spawn implementation agent with proven fix
2. Review implementation
3. Spawn test creation agent
4. Verify all acceptance criteria met

**Implementation Steps**:
1. Create fix branch: `fix/[bug-id]-[severity]`
2. Implement verified solution
3. Add regression tests
4. Update documentation
5. Verify all acceptance criteria

**Quality Checklist**:
- ✓ Root cause addressed (not just symptoms)
- ✓ Edge cases handled
- ✓ Tests prevent recurrence
- ✓ Documentation updated
- ✓ No new issues introduced

**Success Criteria**:
- Fix implemented cleanly
- Tests pass
- Code review approved

### Phase 5: Verification
**Objective**: Confirm complete resolution

**Orchestrator Actions**:
1. Spawn verification agent
2. Review verification report
3. Make decision: RESOLVED or NEEDS_MORE_WORK
4. Update bug story status

**Verification Matrix**:
| Test | Expected | Actual | Pass/Fail |
|------|----------|--------|-----------|
| Original reproduction | No longer occurs | | |
| Acceptance criteria 1 | Met | | |
| Acceptance criteria 2 | Met | | |
| Regression suite | All pass | | |
| Performance impact | Negligible | | |

**Final Checklist**:
- [ ] Original issue resolved
- [ ] All acceptance criteria met
- [ ] No regressions introduced
- [ ] Tests added and passing
- [ ] Documentation updated
- [ ] Story status updated

**Success Criteria**:
- All verification checks pass
- Bug marked as RESOLVED

## Script Details

### Command Options
```bash
# Full 5-phase troubleshooting
python troubleshoot.py "bug-story.md" --claude

# Individual phases (for complex bugs)
python troubleshoot.py "bug-story.md" --phase 1  # Reproduction only
python troubleshoot.py "bug-story.md" --phase 2  # Diagnosis only
python troubleshoot.py "bug-story.md" --phase 3  # Hypothesis testing
python troubleshoot.py "bug-story.md" --phase 4  # Implementation
python troubleshoot.py "bug-story.md" --phase 5  # Verification
```

### Bug Story Format
The script expects bug stories with these sections:
- Overview (Severity, Priority, Status)
- Description
- Steps to Reproduce
- Expected vs Actual Behavior
- Environment details
- Error Messages/Logs
- Root Cause Analysis (initial thoughts)
- Proposed Fix suggestions
- Verification Steps
- Acceptance Criteria

### State Management
Progress saved in `.atlas/troubleshooting/`:
```json
{
  "bug_id": "B010",
  "phase": 3,
  "status": "in_progress",
  "timestamp": "2024-01-15T14:30:00",
  "hypotheses_tested": [
    {"hypothesis": "Config issue", "result": "NO_CHANGE"},
    {"hypothesis": "Cache problem", "result": "FIXED"}
  ]
}
```

## Common Patterns

### Performance Issues
1. Reproduce with profiling enabled
2. Identify bottlenecks
3. Test optimization hypotheses
4. Verify no functionality impact

### Race Conditions
1. Add logging to trace execution
2. Introduce controlled delays
3. Test synchronization fixes
4. Stress test solution

### Platform-Specific Bugs
1. Reproduce on affected platform
2. Compare with working platform
3. Identify differences
4. Test platform-specific fixes

## Success Metrics

- **First-Time Fix Rate**: >80% of bugs fixed correctly first time
- **Root Cause Identification**: 100% of fixes address root cause
- **Regression Rate**: <5% of fixed bugs reoccur
- **Time to Resolution**: Predictable based on severity
- **Documentation Quality**: Every bug improves docs/tests

## Integration Points

- **Input**: Bug stories from `09_STORIES/bugs/`
- **Output**: Fix implementation and verification
- **Integrates with**:
  - Adversarial Workflow (for code review)
  - Story Creation (for bug report format)
  - Research Process (for investigating patterns)

## Anti-Patterns to Avoid

- ❌ Fixing symptoms without finding root cause
- ❌ Making multiple changes at once
- ❌ Skipping reproduction phase
- ❌ Not adding regression tests
- ❌ Marking resolved without full verification

## When Troubleshooting Fails

If the process doesn't resolve the issue:
1. Escalate to senior team member
2. Consider architectural issue
3. Research similar issues in community
4. Document as known issue with workaround
5. Create follow-up research task