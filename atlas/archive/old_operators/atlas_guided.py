#!/usr/bin/env python3
"""
Atlas Guided Workflow - LLM drives but asks for confirmation at each step
"""

import sys
from datetime import datetime
from pathlib import Path

def create_guided_workflow(task_type: str, description: str) -> None:
    """Create a workflow that prompts the LLM to ask for confirmation"""

    timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")

    if task_type == "bug":
        story_id = f"BUG-{timestamp}"
        story_dir = "bugs"
    else:
        story_num = len(list((Path.cwd() / 'atlas' / '09_STORIES' / 'features').glob('*.md'))) + 1
        story_id = f"ATLAS-{story_num:03d}"
        story_dir = "features"

    print(f"""
╔═══════════════════════════════════════════════════════════════╗
║            ATLAS GUIDED WORKFLOW - CHECKPOINT MODE            ║
╚═══════════════════════════════════════════════════════════════╝

📋 TASK: {description}
📁 Story ID: {story_id}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

INSTRUCTIONS FOR YOU (THE LLM):

1. Complete each step
2. After EACH step, ask: "Ready for me to [next step]?"
3. Wait for confirmation before proceeding
4. If user says "no" or gives feedback, adjust accordingly

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

YOUR WORKFLOW STEPS:

□ STEP 1: Create story file atlas/09_STORIES/{story_dir}/{story_id}.md
  → Then ask: "Story created. Ready for me to research the current implementation?"

□ STEP 2: Research and document current state
  → Then ask: "Research complete. Ready for me to create the implementation plan?"

□ STEP 3: Create detailed implementation plan
  → Then ask: "Plan complete. Ready for me to show you the plan for approval?"

□ STEP 4: Show the plan and wait for approval
  → Then ask: "Shall I proceed with implementation?"

□ STEP 5: Implement systematically
  → After each file, ask: "File X updated. Ready for me to continue with file Y?"

□ STEP 6: Test and verify
  → Then ask: "Ready for me to update the story with results?"

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

REMEMBER: ASK FOR CONFIRMATION AFTER EACH STEP.
The user will guide you with "yes", "no", or specific feedback.

Start with Step 1 now, then ask for confirmation.
""")

def main():
    if len(sys.argv) < 3:
        print("Usage: python3 atlas/atlas_guided.py [bug|feature] \"description\"")
        sys.exit(1)

    task_type = sys.argv[1].lower()
    description = ' '.join(sys.argv[2:])

    create_guided_workflow(task_type, description)

if __name__ == '__main__':
    main()