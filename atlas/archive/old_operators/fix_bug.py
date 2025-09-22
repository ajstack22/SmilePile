#!/usr/bin/env python3
"""
Simple Atlas Bug Fix Launcher
Usage: python atlas/fix_bug.py "description of the bug"
"""

import sys
import os
from pathlib import Path
from datetime import datetime

def main():
    if len(sys.argv) < 2:
        print("Usage: python atlas/fix_bug.py \"description of the bug\"")
        print("Example: python atlas/fix_bug.py \"App crashes when clicking import button\"")
        sys.exit(1)

    bug_description = ' '.join(sys.argv[1:])

    # Run the workflow orchestrator
    atlas_dir = Path(__file__).parent
    workflow_script = atlas_dir / 'atlas_bug_workflow.py'

    print(f"""
╔════════════════════════════════════════════════════════════╗
║             🚨 ATLAS BUG FIX WORKFLOW 🚨                   ║
╚════════════════════════════════════════════════════════════╝

📝 Bug Report: {bug_description}

Initiating Atlas workflow with:
✅ Automatic bug story creation
✅ Troubleshooting context injection
✅ Verification checklist
✅ Full workflow guidance

""")

    import subprocess
    result = subprocess.run(
        [sys.executable, str(workflow_script), '-d', bug_description],
        capture_output=False
    )

    return result.returncode

if __name__ == '__main__':
    sys.exit(main())