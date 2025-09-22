#!/usr/bin/env python3
"""
Atlas Story Generator
Creates properly formatted Atlas story from research
"""

import sys
from datetime import datetime
from pathlib import Path

def generate_story_template(task_type: str, story_id: str, description: str) -> str:
    """Generate appropriate story template"""

    if task_type == "bug":
        return f"""# {story_id}: {description}

## Status: IN_PROGRESS

## Problem Statement
**User Report**: {description}
**Severity**: [HIGH/MEDIUM/LOW]
**First Observed**: {datetime.now().isoformat()}

## Research Findings
### Current Implementation
[Document what you found during research]

### Files Affected
- [ ] File 1: [path] - [what needs to change]
- [ ] File 2: [path] - [what needs to change]

### Root Cause
[Based on research, what is causing this issue?]

## Acceptance Criteria
1. **GIVEN** [context]
   **WHEN** [action]
   **THEN** [expected result]

2. **GIVEN** [context]
   **WHEN** [action]
   **THEN** [expected result]

## Implementation Plan
Step 1: [Specific action]
Step 2: [Specific action]
Step 3: [Specific action]

## Edge Cases to Handle
- [ ] [Edge case 1]
- [ ] [Edge case 2]

## Testing Requirements
- [ ] Reproduce issue before fix
- [ ] Verify fix resolves issue
- [ ] Add regression test
- [ ] Test on different configurations

## Evidence Requirements
- [ ] Before: [screenshot/log of issue]
- [ ] After: [screenshot/log of fix]
- [ ] Test results showing fix works

## Progress Tracking
- [ ] Research complete
- [ ] Story documented
- [ ] Plan approved
- [ ] Implementation complete
- [ ] Tests passing
- [ ] Validation complete
"""

    else:  # feature
        return f"""# {story_id}: {description}

## Status: IN_PROGRESS

## Feature Description
{description}

## User Story
**As a** [user type]
**I want** [feature]
**So that** [benefit]

## Research Findings
### Current State
[What exists now based on research]

### Files to Modify
- [ ] File 1: [path] - [what changes]
- [ ] File 2: [path] - [what changes]

### Technical Approach
[How will you implement this?]

## Acceptance Criteria
1. **GIVEN** [context]
   **WHEN** [action]
   **THEN** [expected result]

2. **GIVEN** [context]
   **WHEN** [action]
   **THEN** [expected result]

3. **GIVEN** [context]
   **WHEN** [action]
   **THEN** [expected result]

## Success Metrics
- [ ] [Measurable outcome 1]
- [ ] [Measurable outcome 2]

## Implementation Plan
Phase 1: [What]
- Step 1.1: [Specific action]
- Step 1.2: [Specific action]

Phase 2: [What]
- Step 2.1: [Specific action]
- Step 2.2: [Specific action]

## Edge Cases & Variants
- [ ] Landscape orientation
- [ ] Tablet layouts
- [ ] Dark theme
- [ ] Accessibility mode
- [ ] Different screen sizes

## Testing Requirements
- [ ] Unit tests for logic
- [ ] UI tests for interactions
- [ ] Edge case testing
- [ ] Performance testing
- [ ] Accessibility testing

## Evidence Requirements
- [ ] Screenshots of new feature
- [ ] Test results
- [ ] Performance metrics
- [ ] User feedback

## Progress Tracking
- [x] Research complete
- [x] Story created
- [ ] Plan approved
- [ ] Implementation complete
- [ ] Tests passing
- [ ] Validation complete
"""

def print_story_guide(task_type: str, description: str):
    """Print story creation guide"""

    # Generate story ID
    if task_type == "bug":
        story_id = f"BUG-{datetime.now().strftime('%Y%m%d-%H%M%S')}"
        story_dir = "bugs"
    else:
        story_id = f"ATLAS-XXX"  # Placeholder
        story_dir = "features"

    story_path = f"atlas/09_STORIES/{story_dir}/{story_id}-{description[:30].replace(' ', '-').lower()}.md"

    print(f"""
╔══════════════════════════════════════════════════════════════════════╗
║                      ATLAS STORY CREATION                           ║
║                    Document Before You Code                         ║
╚══════════════════════════════════════════════════════════════════════╝

📁 CREATE FILE: {story_path}
📋 STORY TYPE: {task_type.upper()}

══════════════════════════════════════════════════════════════════════

STORY SECTIONS TO COMPLETE:

1. RESEARCH FINDINGS
─────────────────
Document what you discovered:
- Current implementation details
- All files that need changes
- Edge cases found
- Layout variants discovered

2. ACCEPTANCE CRITERIA
────────────────────
Use Given/When/Then format:
- Cover happy path
- Cover error cases
- Cover edge cases
- Be specific and testable

3. IMPLEMENTATION PLAN
────────────────────
Detailed steps:
- Specific file changes
- Order of operations
- Risk mitigation
- Rollback plan if needed

4. TESTING REQUIREMENTS
─────────────────────
What needs to be tested:
- Functionality tests
- Edge case tests
- Regression tests
- Performance tests

══════════════════════════════════════════════════════════════════════

STORY TEMPLATE:
""")

    print(generate_story_template(task_type, story_id, description))

    print("""
══════════════════════════════════════════════════════════════════════

After creating the story, ask:
"Story created at [path] with:
- [X acceptance criteria]
- [Y files to modify]
- [Z test requirements]

Ready for you to review the story?"
""")

def main():
    if len(sys.argv) < 3:
        print("Usage: python3 atlas/atlas_story.py [bug|feature] \"description\"")
        sys.exit(1)

    task_type = sys.argv[1].lower()
    description = ' '.join(sys.argv[2:])

    print_story_guide(task_type, description)

if __name__ == '__main__':
    main()