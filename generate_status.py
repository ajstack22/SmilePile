#!/usr/bin/env python3
"""
Generate project status page from Atlas metadata
"""

import json
import os
from datetime import datetime
from pathlib import Path

def load_metadata():
    """Load the Atlas metadata"""
    metadata_file = Path('.atlas/backlog_metadata.json')
    if metadata_file.exists():
        with open(metadata_file, 'r') as f:
            return json.load(f)
    return {}

def generate_status_page():
    """Generate the status page markdown"""
    metadata = load_metadata()

    # Count items by status
    features = metadata.get('features', [])
    bugs = metadata.get('bugs', [])
    epics = metadata.get('epics', [])

    # Categorize features
    done_features = []
    in_progress_features = []
    backlog_features = []

    for feature in features:
        status = feature.get('status', 'backlog')
        if status in ['done', 'completed']:
            done_features.append(feature)
        elif status in ['in_progress', 'in-progress']:
            in_progress_features.append(feature)
        else:
            backlog_features.append(feature)

    # Sort backlog by priority
    priority_order = {'critical': 0, 'high': 1, 'medium': 2, 'low': 3}
    backlog_features.sort(key=lambda x: priority_order.get(x.get('priority', 'low'), 3))

    # Generate markdown
    md = f"""# SmilePile Project Status

*Last Updated: {datetime.now().strftime('%Y-%m-%d %H:%M')}*

## 📊 Overall Progress

### Epic Status
| Epic | Title | Status | Features |
|------|-------|--------|----------|
"""

    for epic in epics:
        # Count features for this epic
        epic_features = [f for f in features if epic['id'] in f.get('file_path', '')]
        status_icon = '🟡' if epic.get('status') == 'planning' else '🟢' if epic.get('status') == 'done' else '🔵'
        md += f"| {epic['id']} | {epic['title']} | {status_icon} {epic.get('status', 'planning').title()} | {len(epic_features)} features |\n"

    md += f"""

### Summary Stats
- **Total Features**: {len(features)}
- **Completed**: {len(done_features)} ({len(done_features)*100//len(features) if features else 0}%)
- **In Progress**: {len(in_progress_features)}
- **Backlog**: {len(backlog_features)}
- **Bugs**: {len([b for b in bugs if b.get('status') not in ['resolved', 'closed']])} active

---

## 🐛 Active Bugs

| ID | Title | Severity | Status |
|----|-------|----------|--------|
"""

    for bug in bugs:
        if bug.get('status') not in ['resolved', 'closed']:
            status_icon = '🔴' if bug.get('status') == 'open' else '🟡'
            title = bug['title'][:60] + '...' if len(bug['title']) > 60 else bug['title']
            md += f"| {bug['id']} | {title} | {bug.get('severity', 'medium').title()} | {status_icon} {bug.get('status', 'open').title()} |\n"

    md += """

---

## 📋 Kanban Board

### 🚀 Done
"""

    if done_features:
        md += "| ID | Title | Priority |\n|----|-------|----------|\n"
        for feature in done_features[:15]:  # Limit to 15 most recent
            md += f"| {feature['id']} | {feature['title']} | {feature.get('priority', 'medium').title()} |\n"
        if len(done_features) > 15:
            md += f"| ... | *and {len(done_features)-15} more* | |\n"
    else:
        md += "*(No completed features)*\n"

    md += "\n### 🔄 In Progress\n"

    if in_progress_features:
        md += "| ID | Title | Priority |\n|----|-------|----------|\n"
        for feature in in_progress_features:
            md += f"| {feature['id']} | {feature['title']} | {feature.get('priority', 'medium').title()} |\n"
    else:
        md += "*(No features in progress)*\n"

    md += "\n### 📚 Backlog (Top Priority)\n"

    if backlog_features:
        md += "| ID | Title | Priority |\n|----|-------|----------|\n"
        for feature in backlog_features[:10]:  # Show top 10
            md += f"| {feature['id']} | {feature['title']} | {feature.get('priority', 'medium').title()} |\n"
        if len(backlog_features) > 10:
            md += f"| ... | *and {len(backlog_features)-10} more in backlog* | |\n"
    else:
        md += "*(No features in backlog)*\n"

    md += """

---

## 📝 File Organization

### Naming Convention
- **Active**: `F0016_implement_file_picker.md`
- **Completed**: `_F0001_create_android_project.md`

### Directory Structure
```
features/     # Feature stories
bugs/         # Bug reports
epics/        # Epic definitions
tech_debt/    # Technical debt items
```

---

*Generated automatically from Atlas metadata*
"""

    return md

def main():
    """Main entry point"""
    status_md = generate_status_page()

    # Write to file
    with open('PROJECT_STATUS.md', 'w') as f:
        f.write(status_md)

    print("✅ Project status page generated: PROJECT_STATUS.md")

    # Also print a quick summary
    metadata = load_metadata()
    features = metadata.get('features', [])
    done = len([f for f in features if f.get('status') in ['done', 'completed']])
    in_progress = len([f for f in features if f.get('status') in ['in_progress', 'in-progress']])
    backlog = len([f for f in features if f.get('status') not in ['done', 'completed', 'in_progress', 'in-progress']])

    print(f"\n📊 Quick Summary:")
    print(f"  ✅ Done: {done}")
    print(f"  🔄 In Progress: {in_progress}")
    print(f"  📚 Backlog: {backlog}")
    print(f"  🐛 Active Bugs: {len([b for b in metadata.get('bugs', []) if b.get('status') not in ['resolved', 'closed']])}")

if __name__ == "__main__":
    main()