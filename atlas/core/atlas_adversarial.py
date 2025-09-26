#!/usr/bin/env python3
"""
Atlas Adversarial Review
Forces thorough review to find what was missed
"""

import sys
from pathlib import Path

def run_adversarial_review(story_path: str):
    """Run adversarial review checklist"""

    print(f"""
╔══════════════════════════════════════════════════════════════════════╗
║                    ATLAS ADVERSARIAL REVIEW                         ║
║                   Finding What You Missed                           ║
╚══════════════════════════════════════════════════════════════════════╝

📁 REVIEWING: {story_path}

══════════════════════════════════════════════════════════════════════

ADVERSARIAL CHECKLIST - Find Everything That Could Go Wrong:

UI EDGE CASES
─────────────
□ Did you check ALL layout variants?
  - layout/ (default)
  - layout-land/ (landscape)
  - layout-large/ (tablets)
  - layout-h840dp/ (large screens)
  - layout-sw600dp/ (7" tablets)
  - layout-sw720dp/ (10" tablets)

□ Did you check ALL theme variants?
  - Light theme
  - Dark theme
  - High contrast

□ Did you find ALL places this UI element appears?
  - Main layouts
  - Include files
  - Fragment layouts
  - Dialog layouts
  - Menu resources

STATE EDGE CASES
────────────────
□ What happens when:
  - Screen rotates mid-action?
  - App goes to background?
  - Memory is low?
  - Network disconnects?
  - Permission is denied?

□ Race conditions:
  - User taps multiple times quickly?
  - Multiple operations at once?
  - Gesture conflicts?

DATA EDGE CASES
──────────────
□ What if data is:
  - Empty/null
  - Extremely large
  - Malformed
  - In unexpected format
  - Missing required fields

ERROR SCENARIOS
──────────────
□ What errors aren't handled:
  - File not found
  - Out of memory
  - Disk full
  - Corrupted data
  - Timeout

ACCESSIBILITY
────────────
□ Did you consider:
  - Screen readers
  - Keyboard navigation
  - Voice control
  - Large text mode
  - RTL languages

PERFORMANCE
──────────
□ What about:
  - Slow devices
  - Large datasets
  - Animation jank
  - Memory leaks
  - Battery drain

COMPATIBILITY
────────────
□ Will it work on:
  - Minimum API level
  - Maximum API level
  - Different screen sizes
  - Different manufacturers
  - Custom Android skins

══════════════════════════════════════════════════════════════════════

WHAT TO DO WITH FINDINGS:

1. For each issue found, ask yourself:
   "Would this break the feature?"
   "Would this confuse users?"
   "Is this an edge case we must handle?"

2. Update the story with:
   - Additional requirements discovered
   - Edge cases to handle
   - Files missed in initial review

3. Ask for confirmation:
   "Adversarial review found [X issues].
    - [List main issues]
    Should I update the plan to address these?"

══════════════════════════════════════════════════════════════════════

COMMON MISSES (Check These!):

For UI Changes:
- Forgot landscape layouts
- Missed tablet layouts
- Didn't check dark theme
- Fragment transaction edge cases

For Features:
- No error handling
- No loading states
- No empty states
- No offline handling

For Bugs:
- Fixed symptom not cause
- Didn't add regression test
- Missed related code
- Introduced new bugs

══════════════════════════════════════════════════════════════════════

Start reviewing now. Be adversarial. Find problems.
Your job is to break the implementation before users do.
""")

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 atlas/atlas_adversarial.py <story-path>")
        print("Example: python3 atlas/atlas_adversarial.py atlas/09_STORIES/features/ATLAS-001.md")
        sys.exit(1)

    story_path = sys.argv[1]
    run_adversarial_review(story_path)

if __name__ == '__main__':
    main()