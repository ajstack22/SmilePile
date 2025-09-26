#!/usr/bin/env python3
"""
Atlas Checkpoint Manager
Provides clear checkpoints and confirmation prompts for LLMs
"""

import sys
from pathlib import Path
from typing import Optional

class CheckpointManager:
    """Manages workflow checkpoints and confirmations"""

    def __init__(self):
        self.checkpoints = {
            "research": [
                "Located all relevant files",
                "Understood current implementation",
                "Documented findings in story"
            ],
            "story": [
                "Created story file with all sections",
                "Added acceptance criteria",
                "Defined success metrics"
            ],
            "planning": [
                "Listed all files to change",
                "Detailed specific modifications",
                "Ordered implementation steps"
            ],
            "adversarial": [
                "Checked all layout variants",
                "Found edge cases",
                "Identified potential failures"
            ],
            "implementation": [
                "Following approved plan",
                "Updating story progress",
                "Handling edge cases"
            ],
            "testing": [
                "Ran existing tests",
                "Added new test cases",
                "Verified requirements"
            ],
            "validation": [
                "All criteria met",
                "Story updated with evidence",
                "Ready to close"
            ]
        }

    def print_checkpoint(self, phase: str, context: Optional[str] = None):
        """Print checkpoint for a specific phase"""

        prompts = {
            "research": {
                "start": "Starting RESEARCH phase. I'll locate and analyze all relevant files.",
                "complete": "Research complete. Found {context}. Ready to create the story?"
            },
            "story": {
                "start": "Creating story file with problem statement, acceptance criteria, and requirements.",
                "complete": "Story created with {context}. Ready for you to review it?"
            },
            "planning": {
                "start": "Creating implementation plan with specific file changes.",
                "complete": "Plan ready with {context}. Should I show you the plan?"
            },
            "adversarial": {
                "start": "Running adversarial review to find edge cases and potential issues.",
                "complete": "Adversarial review found {context}. Ready to address these issues?"
            },
            "implementation": {
                "start": "Beginning implementation following the approved plan.",
                "progress": "Updated {context}. Continue with next file?",
                "complete": "Implementation complete. Ready to run tests?"
            },
            "testing": {
                "start": "Running tests to verify all requirements.",
                "complete": "Testing complete. {context}. Ready to see results?"
            },
            "validation": {
                "start": "Validating all acceptance criteria are met.",
                "complete": "Validation complete. Ready to close the story?"
            }
        }

        if phase not in prompts:
            return

        print(f"""
╔══════════════════════════════════════════════════════════════════════╗
║                         CHECKPOINT: {phase.upper()}
╚══════════════════════════════════════════════════════════════════════╝

Current Phase Tasks:
{chr(10).join(f'□ {task}' for task in self.checkpoints.get(phase, []))}

══════════════════════════════════════════════════════════════════════

ASK THE USER ONE OF THESE:

If starting the phase:
→ "{prompts[phase]['start']}"

If completing the phase:
→ "{prompts[phase]['complete'].format(context=context or '[details]')}"

If mid-implementation:
→ "{prompts[phase].get('progress', '').format(context=context or '[file]')}"

══════════════════════════════════════════════════════════════════════

WAIT FOR USER RESPONSE:
- "yes" or "continue" → Proceed to next phase
- "no" or "wait" → Show current work and wait
- Specific feedback → Adjust based on feedback
- "show me" → Display the current work
""")

def print_confirmation_guide():
    """Print guide for asking confirmations"""

    print("""
╔══════════════════════════════════════════════════════════════════════╗
║                 ATLAS CONFIRMATION GUIDE FOR LLMs                   ║
╚══════════════════════════════════════════════════════════════════════╝

HOW TO ASK FOR CONFIRMATION AT EACH STEP:

1. AFTER RESEARCH
──────────────
"I've completed research and found:
- [X relevant files]
- [Current implementation details]
- [Issues identified]

Ready for me to create the story document?"

2. AFTER STORY CREATION
────────────────────
"Story created at [path] with:
- [X acceptance criteria]
- [Y success metrics]
- [Problem statement]

Ready for you to review the story?"

3. AFTER PLANNING
──────────────
"Implementation plan ready:
- [X files to modify]
- [Specific changes listed]
- [Ordered steps]

Should I show you the detailed plan for approval?"

4. AFTER ADVERSARIAL REVIEW
─────────────────────────
"Adversarial review complete. Found:
- [X potential edge cases]
- [Y missing requirements]
- [Z layout variants to check]

Ready to update the plan with these findings?"

5. DURING IMPLEMENTATION
─────────────────────
"Updated [filename]:
- [What was changed]
- [X of Y files complete]

Continue with [next file]?"

6. AFTER TESTING
─────────────
"Testing complete:
- [X/Y tests passing]
- [Coverage percentage]
- [Any failures found]

Ready to see detailed results?"

7. AFTER VALIDATION
────────────────
"All acceptance criteria validated:
- [Checklist of met criteria]
- [Evidence collected]

Ready to mark story as COMPLETE?"

══════════════════════════════════════════════════════════════════════

REMEMBER:
- Always be specific about what you found/did
- Include counts and concrete details
- Wait for explicit confirmation
- Show work when asked
""")

def main():
    if len(sys.argv) < 2:
        print("""
Atlas Checkpoint Manager

Usage:
  python3 atlas/atlas_checkpoint.py guide        # Show confirmation guide
  python3 atlas/atlas_checkpoint.py research     # Show research checkpoint
  python3 atlas/atlas_checkpoint.py story        # Show story checkpoint
  python3 atlas/atlas_checkpoint.py planning     # Show planning checkpoint
  python3 atlas/atlas_checkpoint.py adversarial  # Show adversarial checkpoint
  python3 atlas/atlas_checkpoint.py implementation # Show implementation checkpoint
  python3 atlas/atlas_checkpoint.py testing      # Show testing checkpoint
  python3 atlas/atlas_checkpoint.py validation   # Show validation checkpoint
        """)
        sys.exit(1)

    command = sys.argv[1].lower()

    if command == "guide":
        print_confirmation_guide()
    else:
        manager = CheckpointManager()
        context = sys.argv[2] if len(sys.argv) > 2 else None
        manager.print_checkpoint(command, context)

if __name__ == '__main__':
    main()