# Agent Orchestration Examples

## Quick Start Templates

### 1. Simple Bug Fix
```markdown
Fix bug: [DESCRIPTION]

Phase 1: Launch researcher agent to find the bug location and understand the issue
Phase 2: Launch product-manager agent to document the bug and define fix criteria
Phase 3: Launch developer agent to plan the fix
Phase 4: Launch peer-reviewer agent to review approach for side effects
Phase 5: Launch developer agent to implement the fix
Phase 6: Launch QA agent to verify the fix works
Phase 7: Launch product-manager agent to validate fix meets criteria
Phase 8: Launch organizer agent to clean up and close ticket
```

### 2. New Feature Implementation
```markdown
Implement feature: [DESCRIPTION]

PARALLEL Phase 1:
- Researcher Agent 1: Search for UI components
- Researcher Agent 2: Search for backend logic
- Researcher Agent 3: Search for existing patterns

Phase 2: Product Manager creates comprehensive story

PARALLEL Phase 3:
- Developer Agent: Create backend plan
- UI Expert Agent: Create frontend plan
- DevOps Agent: Plan deployment changes

Phase 4: Peer Reviewer examines all plans for issues

PARALLEL Phase 5:
- Developer Agent 1: Implement backend
- Developer Agent 2: Implement frontend
- Developer Agent 3: Update tests
- Developer Agent 4: Update documentation

PARALLEL Phase 6:
- QA Agent: Functional testing
- Peer Reviewer: Code quality review
- Performance Agent: Load testing

Phase 7: Product Manager validates all criteria

Phase 8: Organizer cleans up and archives
```

### 3. Cross-Platform Feature
```markdown
Add [FEATURE] to iOS and Android

PARALLEL Phase 1:
- Researcher: iOS codebase exploration
- Researcher: Android codebase exploration
- Researcher: Shared logic exploration

Phase 2: Product Manager creates unified story

PARALLEL Phase 3:
- ios-expert: iOS implementation plan
- android-expert: Android implementation plan
- developer: Shared logic plan

Phase 4: Peer Reviewer reviews all platform plans

PARALLEL Phase 5:
- ios-expert: Implement iOS changes
- android-expert: Implement Android changes
- developer: Implement shared components

PARALLEL Phase 6:
- QA Agent: Test iOS
- QA Agent: Test Android
- QA Agent: Integration testing

Phase 7: Product Manager validates both platforms

Phase 8: Organizer manages platform-specific cleanup
```

### 4. Emergency Hotfix
```markdown
URGENT: Fix [CRITICAL ISSUE]

IMMEDIATE PARALLEL:
- Researcher: Find issue root cause
- Peer Reviewer: Assess impact and risks
- DevOps: Prepare rollback plan

FAST Phase 2: Product Manager documents minimal fix criteria

PARALLEL Implementation:
- Developer: Implement fix
- QA: Prepare test cases
- DevOps: Prepare deployment

PARALLEL Verification:
- QA: Test fix
- Peer Reviewer: Verify no regressions
- DevOps: Stage deployment

Product Manager: Quick validation

PARALLEL Cleanup:
- Organizer: Document incident
- Developer: Create follow-up tickets
- DevOps: Monitor deployment
```

## Detailed Example: Dark Mode Feature

### User Request
"Add dark mode support to the application settings"

### Agent Orchestration

```markdown
## Phase 1: Research (15 minutes)

PARALLEL EXECUTION:
```
Task 1: Launch general-purpose agent as researcher
"Search for all theme-related files, find colors, styles, and theme definitions"

Task 2: Launch general-purpose agent as researcher
"Find settings implementation, preferences storage, and toggle patterns"

Task 3: Launch general-purpose agent as researcher
"Identify all UI components that need theme support"
```

Wait for all agents to complete and combine findings.

## Phase 2: Story Creation (10 minutes)

Launch product-manager agent:
"Create story for dark mode feature using research:
- Theme files found in: /styles/themes/
- Settings in: /views/SettingsView
- 47 components need theme support
Create acceptance criteria for toggle, persistence, and appearance"

Output: ATLAS-042-dark-mode-toggle.md

## Phase 3: Planning (15 minutes)

PARALLEL EXECUTION:
```
Task 1: Launch developer agent
"Plan theme service implementation: state management, theme switching,
persistence, and system theme detection"

Task 2: Launch developer agent
"Plan UI updates for 47 components to support dynamic theming"

Task 3: Launch ios-expert agent
"Plan iOS-specific dark mode integration with system preferences"

Task 4: Launch android-expert agent
"Plan Android material theme implementation"
```

Combine plans into master implementation strategy.

## Phase 4: Adversarial Review (10 minutes)

Launch peer-reviewer agent:
"Review dark mode plans for:
- System theme conflicts
- Performance with theme switching
- Accessibility in dark mode
- Component visibility issues
- Edge cases with media/images
- Migration for existing users"

Issues found:
- Need fallback for old OS versions
- Consider high contrast mode
- Handle theme change during operations

## Phase 5: Implementation (30 minutes)

PARALLEL EXECUTION:
```
Task 1: Launch developer agent
"Implement ThemeService with:
- Theme state management
- System theme detection
- Theme persistence
- Theme change events"

Task 2: Launch developer agent
"Update base components:
- Create themed color system
- Update typography for themes
- Add theme context provider"

Task 3: Launch developer agent
"Update 20 primary components for theme support"

Task 4: Launch developer agent
"Update 27 secondary components for theme support"

Task 5: Launch developer agent
"Implement settings UI:
- Add theme toggle
- Add theme preview
- Connect to ThemeService"
```

Monitor progress, coordinate any conflicts.

## Phase 6: Testing (20 minutes)

PARALLEL EXECUTION:
```
Task 1: Launch general-purpose agent as QA
"Test theme switching:
- Toggle works correctly
- Persistence across restarts
- All screens update properly
- No visual glitches"

Task 2: Launch general-purpose agent as QA
"Test edge cases:
- Theme change during operations
- System theme changes
- Performance with rapid switching"

Task 3: Launch peer-reviewer agent
"Review implementation:
- Code quality
- Pattern consistency
- Performance implications
- Accessibility compliance"
```

Results: All tests passing, 2 minor issues found and fixed.

## Phase 7: Validation (5 minutes)

Launch product-manager agent:
"Validate dark mode implementation:
✅ Toggle in settings works
✅ Theme persists across sessions
✅ All UI elements properly themed
✅ Smooth transitions
✅ Accessibility maintained
✅ System theme respected
All acceptance criteria MET"

## Phase 8: Clean-up (10 minutes)

Launch general-purpose agent as organizer:
"Clean up dark mode implementation:
- Remove 3 temporary test files
- Archive 2 research documents
- Update README with theme instructions
- Mark ATLAS-042 as COMPLETE
- Create follow-up ticket for theme customization
- Update app documentation"

TOTAL TIME: ~2 hours (vs 8+ hours traditional)
```

## Orchestration Patterns

### Pattern 1: Parallel Research
```
PARALLEL:
- Agent 1: grep "searchTerm1"
- Agent 2: find "*.pattern"
- Agent 3: analyze dependencies
Combine: Merge all findings
```

### Pattern 2: Pipeline Processing
```
Agent 1 output → Agent 2 input → Agent 3 input
Example: Research → Story → Planning
```

### Pattern 3: Divide and Conquer
```
Split work into N parts
Launch N agents in parallel
Combine results
Example: Update 50 components with 5 agents, 10 each
```

### Pattern 4: Specialized Teams
```
Frontend Team: UI agents working in parallel
Backend Team: API agents working in parallel
Coordinate: Sync at integration points
```

### Pattern 5: Verification Matrix
```
For each component:
- Developer implements
- QA tests
- Reviewer validates
All in parallel across components
```

## Command Templates

### Basic Agent Launch
```
Launch a [agent-type] agent to [specific task].
Provide context: [relevant information].
Expected output: [what you need back].
```

### Parallel Agent Launch
```
Launch the following agents in parallel:
1. [agent-type]: [task 1]
2. [agent-type]: [task 2]
3. [agent-type]: [task 3]
Wait for all to complete before proceeding.
```

### Sequential with Context
```
Step 1: Launch [agent] to [task], save output as CONTEXT_A
Step 2: Launch [agent] with CONTEXT_A to [next task]
Step 3: Launch [agent] with combined context to [final task]
```

### Conditional Orchestration
```
Launch [agent] to assess [situation]
IF [condition]:
  Launch [agent-type-1] for [scenario 1]
ELSE:
  Launch [agent-type-2] for [scenario 2]
```

## Tips for Effective Orchestration

### 1. Maximize Parallelism
- Identify independent tasks
- Launch agents simultaneously
- Avoid unnecessary sequencing

### 2. Clear Task Definition
- Specific objectives for each agent
- Clear success criteria
- Explicit output requirements

### 3. Context Management
- Pass relevant findings between agents
- Don't duplicate work
- Maintain information continuity

### 4. Error Handling
- Plan for agent failures
- Have fallback strategies
- Maintain partial progress

### 5. Checkpoint Strategy
- Confirm after each phase
- Save intermediate results
- Allow for phase repetition

## Common Orchestration Mistakes

### ❌ Over-Sequencing
**Wrong**: Research → Story → Research → Planning
**Right**: Parallel Research → Story → Planning

### ❌ Vague Instructions
**Wrong**: "Fix the bug"
**Right**: "Fix null pointer exception in UserService.login() method when email is empty"

### ❌ Missing Context
**Wrong**: Launch new agent without previous findings
**Right**: Pass research findings to story creation agent

### ❌ No Coordination
**Wrong**: Parallel agents modifying same file
**Right**: Partition work or sequence file modifications

### ❌ Skipping Validation
**Wrong**: Implement → Deploy
**Right**: Implement → Test → Validate → Deploy