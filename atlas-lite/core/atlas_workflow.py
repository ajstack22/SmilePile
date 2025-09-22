#!/usr/bin/env python3
"""
Atlas Master Workflow Orchestrator
Complete workflow: Research → Story → Plan → Adversarial → Implement → Test → Validate
Each phase requires confirmation before proceeding
"""

import sys
import json
from pathlib import Path
from datetime import datetime
from typing import Dict, List, Optional

class AtlasWorkflow:
    """Master workflow orchestrator with checkpoint management"""

    def __init__(self, task_type: str, description: str):
        self.task_type = task_type
        self.description = description
        self.atlas_dir = Path(__file__).parent
        self.story_id = self._generate_story_id()
        self.story_path = self._get_story_path()
        self.current_phase = 0
        self.phases = [
            "RESEARCH",
            "STORY_CREATION",
            "PLANNING",
            "ADVERSARIAL_REVIEW",
            "IMPLEMENTATION",
            "TESTING",
            "VALIDATION"
        ]

    def _generate_story_id(self) -> str:
        """Generate appropriate story ID"""
        if self.task_type == "bug":
            return f"BUG-{datetime.now().strftime('%Y%m%d-%H%M%S')}"
        else:
            features_dir = self.atlas_dir / '09_STORIES' / 'features'
            features_dir.mkdir(parents=True, exist_ok=True)
            story_num = len(list(features_dir.glob('ATLAS-*.md'))) + 1
            return f"ATLAS-{story_num:03d}"

    def _get_story_path(self) -> Path:
        """Get path for story file"""
        subdir = 'bugs' if self.task_type == 'bug' else 'features'
        title = self.description[:50].replace(' ', '-').lower()
        filename = f"{self.story_id}-{title}.md"
        return self.atlas_dir / '09_STORIES' / subdir / filename

    def print_workflow(self):
        """Print the complete workflow with checkpoints"""

        print(f"""
╔══════════════════════════════════════════════════════════════════════╗
║                    ATLAS MASTER WORKFLOW                            ║
║                  With Checkpoint Confirmation                       ║
╚══════════════════════════════════════════════════════════════════════╝

📋 TASK TYPE: {self.task_type.upper()}
📝 DESCRIPTION: {self.description}
📁 STORY ID: {self.story_id}
📍 STORY PATH: {self.story_path}

══════════════════════════════════════════════════════════════════════

WORKFLOW INSTRUCTIONS:
1. Complete each phase thoroughly
2. After each phase, ask for confirmation
3. Show your work at each checkpoint
4. Wait for approval before proceeding

══════════════════════════════════════════════════════════════════════

PHASE 1: RESEARCH
─────────────────
□ Locate all relevant files
□ Understand current implementation
□ Document existing behavior
□ Identify all affected components

When complete, ask: "Research phase complete. I found [X files/components].
Ready to review my findings?"

══════════════════════════════════════════════════════════════════════

PHASE 2: STORY CREATION
───────────────────────
□ Create story file: {self.story_path}
□ Write problem/feature statement
□ Define acceptance criteria
□ List success metrics
□ Document requirements

When complete, ask: "Story created with [X acceptance criteria].
Ready for me to show you the story?"

══════════════════════════════════════════════════════════════════════

PHASE 3: PLANNING
────────────────
□ List all files to modify
□ Detail specific changes needed
□ Define implementation order
□ Identify dependencies
□ Estimate complexity

When complete, ask: "Implementation plan ready with [X file changes].
Ready to review the plan?"

══════════════════════════════════════════════════════════════════════

PHASE 4: ADVERSARIAL REVIEW
───────────────────────────
□ Find edge cases
□ Identify what could break
□ Check for missed requirements
□ Review all layout variants
□ Consider error scenarios
□ Think about race conditions

When complete, ask: "Adversarial review found [X potential issues].
Ready to see what I found?"

══════════════════════════════════════════════════════════════════════

PHASE 5: IMPLEMENTATION
──────────────────────
□ Follow the approved plan
□ Implement changes systematically
□ Update story progress
□ Handle edge cases found
□ Add error handling

After each file, ask: "Updated [filename]. [X of Y files complete].
Continue with next file?"

══════════════════════════════════════════════════════════════════════

PHASE 6: TESTING
───────────────
□ Run existing tests
□ Add new test cases
□ Test edge cases
□ Verify all requirements
□ Check different configurations

When complete, ask: "Testing complete. [X tests passed].
Ready to see test results?"

══════════════════════════════════════════════════════════════════════

PHASE 7: VALIDATION
──────────────────
□ Verify all acceptance criteria met
□ Update story with evidence
□ Document what was done
□ Mark story as COMPLETE
□ Summarize changes

When complete, ask: "All requirements validated.
Ready to close this story?"

══════════════════════════════════════════════════════════════════════

⚠️ IMPORTANT RULES:
- DO NOT skip phases
- DO NOT proceed without confirmation
- DO NOT write code before Phase 5
- DO show your work at each checkpoint
- DO update the story file throughout

Start with PHASE 1: RESEARCH now.
After completing research, ask for confirmation before proceeding.
""")

def main():
    if len(sys.argv) < 3:
        print("""
Atlas Master Workflow Orchestrator

Usage:
  python3 atlas/atlas_workflow.py bug "description of bug"
  python3 atlas/atlas_workflow.py feature "description of feature"

Examples:
  python3 atlas/atlas_workflow.py bug "App crashes on import"
  python3 atlas/atlas_workflow.py feature "Add 3-dot menu to categories"
        """)
        sys.exit(1)

    task_type = sys.argv[1].lower()
    if task_type not in ['bug', 'feature']:
        print(f"Error: task type must be 'bug' or 'feature', not '{task_type}'")
        sys.exit(1)

    description = ' '.join(sys.argv[2:])

    workflow = AtlasWorkflow(task_type, description)
    workflow.print_workflow()

if __name__ == '__main__':
    main()