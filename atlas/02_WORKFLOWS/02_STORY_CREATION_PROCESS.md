# Story Creation Process

## Overview
A structured process for creating well-formed user stories, bug reports, and epics that ensure clear requirements, testable acceptance criteria, and measurable success metrics. This process prevents ambiguity and rework by capturing complete requirements upfront.

## When to Use
- New feature request from stakeholder
- Bug report from user or QA
- Epic for major initiative
- Breaking down large requirements
- Refining vague requests into actionable work

## Process Script
**Script**: `automation/processes/create_story.py`
**Usage**:
```bash
python create_story.py feature     # User story
python create_story.py bug         # Bug report
python create_story.py epic        # High-level epic
python create_story.py interactive # Guided creation
```

## Process Owner
**Role**: PRODUCT MANAGER or ORCHESTRATOR
- PM role for direct story creation
- Orchestrator coordinates when multiple stories needed
- Never writes stories directly, uses agents

## Story Types and Templates

### Feature Story
**Purpose**: Define new functionality

**Structure**:
```markdown
# Story: [ID] - [Title]
Date: YYYY-MM-DD
Type: feature
Status: DRAFT

## User Story
As a [user role]
I want [feature/capability]
So that [benefit/value]

## Context and Background
[Why needed? Problem solved?]

## Acceptance Criteria
Scenario 1: [Primary use case]
  Given [initial context]
  When [action taken]
  Then [expected outcome]

Scenario 2: [Edge case]
  Given [context]
  When [action]
  Then [outcome]

## Success Metrics
- [Measurable outcome 1]
- [Measurable outcome 2]

## Technical Requirements
### Dependencies
- [External service/component]

### Performance Requirements
- [Response time expectations]

### Security Requirements
- [Auth/authorization needs]

## Evidence Requirements
- [ ] Screenshots of working feature
- [ ] Test results
- [ ] Performance benchmarks
- [ ] Documentation updated

## Out of Scope
[What's NOT included]

## Risks and Mitigations
| Risk | Impact | Mitigation |
|------|--------|------------|
```

### Bug Report
**Purpose**: Document defects systematically

**Key Sections**:
- **Description**: Clear, concise bug description
- **Steps to Reproduce**: Numbered list with result
- **Expected vs Actual**: What should vs does happen
- **Environment**: Platform, version, dependencies
- **Error Logs**: Messages and stack traces
- **Impact Analysis**: User and business impact
- **Root Cause**: Initial investigation
- **Proposed Fix**: Solution suggestions
- **Verification Steps**: How to verify fix

**Severity Levels**:
- `Critical`: System unusable, data loss
- `High`: Major feature broken, no workaround
- `Medium`: Feature impaired, workaround exists
- `Low`: Minor issue, cosmetic

### Epic Story
**Purpose**: High-level initiative planning

**Structure**:
- Vision Statement (1-2 sentences)
- Problem Statement (current vs desired)
- Success Criteria (measurable outcomes)
- User Stories breakdown
- Dependencies and risks
- Stakeholder identification

### Interactive Story Creation
**Purpose**: Guided requirements gathering

**Conversation Flow**:

#### Phase 1: Understanding
Questions to ask:
- What problem are you solving?
- Who will use this feature?
- What's the current workaround?
- What's the impact if we don't do this?

#### Phase 2: Requirements
For each requirement:
- Can you give a specific example?
- What happens if [edge case]?
- How do we measure success?
- What's NOT included?

#### Phase 3: Technical Research
Research needed:
- Existing patterns to follow
- Potential conflicts
- Performance implications
- Security considerations

#### Phase 4: Story Creation
- Create formatted story
- Review with stakeholder
- Make refinements
- Finalize for development

## The Creation Process

### Step 1: Gather Information
**Orchestrator Action**: Spawn information gathering agent

**Activities**:
- Interview stakeholders
- Understand problem space
- Identify users and needs
- Define success criteria

**Success Criteria**:
- Problem clearly understood
- Users identified
- Success measurable

### Step 2: Define Requirements
**Orchestrator Action**: Spawn requirements analyst agent

**Activities**:
- Break down into specific requirements
- Identify edge cases
- Define acceptance criteria
- Specify non-functional requirements

**Success Criteria**:
- Requirements unambiguous
- Acceptance criteria testable
- Edge cases documented

### Step 3: Technical Analysis
**Orchestrator Action**: Spawn technical research agent

**Activities**:
- Research implementation approaches
- Identify dependencies
- Assess complexity
- Evaluate risks

**Success Criteria**:
- Technical approach clear
- Dependencies identified
- Risks documented

### Step 4: Story Documentation
**Orchestrator Action**: Spawn story writer agent

**Activities**:
- Create story using template
- Include all gathered information
- Format for clarity
- Add metadata

**Success Criteria**:
- Story follows template
- All sections complete
- Ready for review

### Step 5: Validation
**Orchestrator Action**: Spawn validation agent

**Activities**:
- Check story completeness
- Verify acceptance criteria testable
- Confirm success metrics measurable
- Validate with stakeholder

**Success Criteria**:
- Story passes quality checks
- Stakeholder approves
- Ready for development

## Quality Checklist

### Good Story Criteria (INVEST)
- **I**ndependent: Can be developed separately
- **N**egotiable: Details can be discussed
- **V**aluable: Delivers user value
- **E**stimable: Can be sized
- **S**mall: Fits in one sprint
- **T**estable: Clear acceptance criteria

### Common Pitfalls
- ❌ Vague requirements ("make it better")
- ❌ Technical tasks disguised as stories
- ❌ Missing acceptance criteria
- ❌ Unmeasurable success metrics
- ❌ No edge case consideration
- ❌ Incomplete dependency identification

## Script Details

### Output Location
Stories created in `atlas/09_STORIES/`:
```
atlas/09_STORIES/
├── features/
│   └── S001-user-authentication.md
├── bugs/
│   └── B001-login-timeout.md
├── epics/
│   └── E001-mobile-app.md
└── templates/
    └── story_template.md
```

### State Management
Story creation state in `.atlas/stories/`:
```json
{
  "story_id": "S001",
  "type": "feature",
  "status": "draft",
  "created": "2024-01-15T10:00:00",
  "stakeholder": "ProductOwner",
  "refinement_sessions": 2
}
```

## Best Practices

### 1. Start with Why
Document why this work is needed before what needs to be done.

### 2. Be Specific
- Replace "user-friendly" with "loads in <2 seconds"
- Replace "handle errors" with "show specific error message"

### 3. Consider Edge Cases
- What if network fails?
- What if user enters invalid data?
- What if multiple users act simultaneously?

### 4. Define Non-Goals
Explicitly state what's NOT included to prevent scope creep.

### 5. Include Evidence Requirements
Define upfront what proof is needed to mark complete.

## Success Metrics

- **Requirement Clarity**: <5% stories need clarification during development
- **Acceptance Coverage**: 100% stories have testable criteria
- **Story Sizing**: 90% stories complete within sprint
- **Rework Rate**: <10% stories need requirement changes
- **Stakeholder Satisfaction**: Stories accurately capture needs

## Integration Points

- **Output**: Stories for Adversarial Workflow
- **Input**: Requirements from stakeholders
- **Integrates with**:
  - Research Process (for feasibility)
  - Adversarial Workflow (for implementation)
  - Troubleshooting (for bug stories)

## When Story Creation Fails

If unable to create clear story:
1. Schedule stakeholder workshop
2. Create spike for research
3. Break into smaller pieces
4. Document assumptions and risks
5. Mark as "needs refinement"