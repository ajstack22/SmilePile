#!/usr/bin/env python3
"""
Atlas Research Phase Guide
Ensures thorough investigation before implementation
"""

import sys

def print_research_guide(task_type: str, description: str):
    """Print research phase guide"""

    print(f"""
╔══════════════════════════════════════════════════════════════════════╗
║                      ATLAS RESEARCH PHASE                           ║
║                    Investigation Before Action                      ║
╚══════════════════════════════════════════════════════════════════════╝

📋 TASK: {description}
📊 TYPE: {task_type.upper()}

══════════════════════════════════════════════════════════════════════

RESEARCH CHECKLIST:

1. LOCATE FILES
─────────────
□ Find primary implementation files
□ Find layout files (all variants!)
□ Find resource files (strings, styles, menus)
□ Find test files
□ Find related documentation

2. UNDERSTAND CURRENT STATE
──────────────────────────
□ How does it currently work?
□ What components are involved?
□ What are the dependencies?
□ What patterns are used?
□ What could break if changed?

3. IDENTIFY SCOPE
────────────────
□ What needs to change?
□ What must NOT change?
□ What might be affected?
□ What edge cases exist?
□ What variants exist (tablets, landscape, etc)?

4. DOCUMENT FINDINGS
──────────────────
Record in story:
- File paths found
- Current behavior
- Required changes
- Potential risks
- Questions to clarify

══════════════════════════════════════════════════════════════════════

RESEARCH COMMANDS TO USE:

# Find all layout variants
find . -path "*/res/layout*" -name "*[relevant]*.xml"

# Search for class/component usage
grep -r "ClassName" --include="*.kt" --include="*.java"

# Find resource references
grep -r "@+id/element_id" --include="*.xml"

# Look for string resources
grep -r "@string/key_name"

══════════════════════════════════════════════════════════════════════

DO NOT:
- Make assumptions about implementation
- Skip checking layout variants
- Ignore test files
- Forget about themes/styles
- Miss configuration files

══════════════════════════════════════════════════════════════════════

When research is complete, ask:
"Research phase complete. I found:
- [X primary files]
- [Y layout variants]
- [Z test files]
- [Key findings]

Ready to create the story with these findings?"
""")

def main():
    if len(sys.argv) < 3:
        print("Usage: python3 atlas/atlas_research.py [bug|feature] \"description\"")
        sys.exit(1)

    task_type = sys.argv[1]
    description = ' '.join(sys.argv[2:])

    print_research_guide(task_type, description)

if __name__ == '__main__':
    main()