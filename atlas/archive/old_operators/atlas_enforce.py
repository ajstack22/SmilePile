#!/usr/bin/env python3
"""
Atlas Process Enforcer - Forces LLM to follow Atlas workflow
NO SHORTCUTS. NO SKIPPING. FULL PROCESS ONLY.
"""

import sys
from pathlib import Path
from datetime import datetime

def enforce_bug_workflow(description: str) -> None:
    """Enforce strict Atlas bug workflow"""

    timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    story_id = f"BUG-{timestamp}"

    print(f"""
╔═══════════════════════════════════════════════════════════════╗
║              ATLAS WORKFLOW ENFORCEMENT MODE                  ║
║                    NO SHORTCUTS ALLOWED                       ║
╚═══════════════════════════════════════════════════════════════╝

⛔ STOP. DO NOT WRITE ANY CODE YET.
⛔ STOP. DO NOT FIX ANYTHING YET.
⛔ STOP. FOLLOW THESE STEPS EXACTLY.

📋 BUG REPORT: {description}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

MANDATORY STEP 1: CREATE BUG STORY
─────────────────────────────────
Create file: atlas/09_STORIES/bugs/{story_id}.md
Include ALL sections from the Atlas bug template.
DO NOT SKIP THIS. DO NOT PROCEED UNTIL COMPLETE.

MANDATORY STEP 2: RESEARCH & DOCUMENT
──────────────────────────────────────
In the bug story, document:
- Current implementation analysis
- Root cause hypothesis
- All affected files
DO NOT WRITE CODE. ONLY RESEARCH AND DOCUMENT.

MANDATORY STEP 3: CREATE IMPLEMENTATION PLAN
─────────────────────────────────────────────
In the bug story, write detailed plan:
- Exact files to modify
- Exact changes needed
- Test scenarios
GET APPROVAL BEFORE PROCEEDING.

MANDATORY STEP 4: IMPLEMENT WITH CHECKLIST
───────────────────────────────────────────
Only NOW can you write code.
Check off each item in the story as you complete it.

MANDATORY STEP 5: VERIFY & DOCUMENT
────────────────────────────────────
- Run tests
- Take screenshots
- Update story with evidence
- Mark as RESOLVED only when 100% complete

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

⚠️  IF YOU SKIP ANY STEP, YOU HAVE FAILED.
⚠️  IF YOU WRITE CODE BEFORE STEP 4, YOU HAVE FAILED.
⚠️  IF YOU DON'T UPDATE THE STORY, YOU HAVE FAILED.

Start with STEP 1 now. Create the bug story file first.
""")

def enforce_feature_workflow(description: str) -> None:
    """Enforce strict Atlas feature workflow"""

    story_num = len(list((Path.cwd() / 'atlas' / '09_STORIES' / 'features').glob('*.md'))) + 1
    story_id = f"ATLAS-{story_num:03d}"

    print(f"""
╔═══════════════════════════════════════════════════════════════╗
║              ATLAS WORKFLOW ENFORCEMENT MODE                  ║
║                 FEATURE IMPLEMENTATION                        ║
╚═══════════════════════════════════════════════════════════════╝

⛔ STOP. DO NOT WRITE ANY CODE YET.

📋 FEATURE REQUEST: {description}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

PHASE 1: STORY CREATION (NO CODE!)
───────────────────────────────────
□ Create: atlas/09_STORIES/features/{story_id}.md
□ Write user story format
□ Define ALL acceptance criteria
□ List ALL files that need changes
□ Document current state thoroughly

PHASE 2: ADVERSARIAL REVIEW (NO CODE!)
────────────────────────────────────────
□ List everything that could go wrong
□ Identify ALL edge cases
□ Find ALL places UI elements exist
□ Document every layout variant

PHASE 3: IMPLEMENTATION PLAN (NO CODE!)
─────────────────────────────────────────
□ Create step-by-step plan in story
□ Get approval on approach
□ Verify plan covers ALL cases

PHASE 4: SYSTEMATIC IMPLEMENTATION
───────────────────────────────────
□ Follow plan EXACTLY
□ Check off items as completed
□ Test after EACH change
□ Verify nothing missed

PHASE 5: VERIFICATION
─────────────────
□ Test all screen sizes
□ Test all edge cases
□ Document with screenshots
□ Update story to COMPLETE

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

YOU MUST COMPLETE EACH PHASE BEFORE MOVING TO THE NEXT.
NO SHORTCUTS. NO ASSUMPTIONS. FOLLOW THE PROCESS.

Begin with PHASE 1. Create the story file NOW.
""")

def main():
    if len(sys.argv) < 3:
        print("""
Usage:
  python3 atlas/atlas_enforce.py bug "description"
  python3 atlas/atlas_enforce.py feature "description"
        """)
        sys.exit(1)

    mode = sys.argv[1].lower()
    description = ' '.join(sys.argv[2:])

    if mode == 'bug':
        enforce_bug_workflow(description)
    elif mode == 'feature':
        enforce_feature_workflow(description)
    else:
        print(f"Unknown mode: {mode}. Use 'bug' or 'feature'")
        sys.exit(1)

if __name__ == '__main__':
    main()