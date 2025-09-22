#!/usr/bin/env python3
"""
Atlas Story Creation Script - Designed to be called by Claude
This script creates structured user stories, bugs, and epics for development
"""

import sys
import json
import os
from pathlib import Path
from datetime import datetime

class StoryCreator:
    """
    Creates and manages development stories, bugs, and epics
    """

    def __init__(self):
        self.features_dir = Path('features')
        self.features_dir.mkdir(exist_ok=True)
        self.bugs_dir = Path('bugs')
        self.bugs_dir.mkdir(exist_ok=True)
        self.tech_debt_dir = Path('tech_debt')
        self.tech_debt_dir.mkdir(exist_ok=True)
        self.epics_dir = Path('epics')
        self.epics_dir.mkdir(exist_ok=True)
        self.metadata_file = Path('.atlas/backlog_metadata.json')
        self.metadata_file.parent.mkdir(parents=True, exist_ok=True)
        self.metadata = self.load_metadata()

        # Legacy support - map old stories to features
        self.stories_dir = self.features_dir

    def load_metadata(self):
        """Load stories metadata"""
        if self.metadata_file.exists():
            with open(self.metadata_file, 'r') as f:
                return json.load(f)
        return {
            'next_feature_id': 1,
            'next_bug_id': 1,
            'next_tech_debt_id': 1,
            'next_epic_id': 1,
            'features': [],
            'bugs': [],
            'tech_debt': [],
            'epics': [],
            # Legacy support
            'stories': [],
            'next_story_id': 1
        }

    def save_metadata(self):
        """Save stories metadata"""
        with open(self.metadata_file, 'w') as f:
            json.dump(self.metadata, f, indent=2)

    def create_feature(self, title, description=None, acceptance_criteria=None, priority='medium', business_value=None):
        """
        Create a feature

        Args:
            title: Story title
            description: Detailed description
            acceptance_criteria: List of acceptance criteria
            priority: Priority level (high, medium, low)

        Returns:
            Feature creation details
        """
        feature_id = f"F{self.metadata['next_feature_id']:04d}"
        self.metadata['next_feature_id'] += 1

        # Create a brief slug from the title (max 30 chars, lowercase, underscores)
        title_slug = title.lower()
        title_slug = ''.join(c if c.isalnum() or c.isspace() else '' for c in title_slug)
        title_slug = '_'.join(title_slug.split())[:30]

        feature = {
            'id': feature_id,
            'type': 'feature',
            'title': title,
            'description': description or '',
            'acceptance_criteria': acceptance_criteria or [],
            'priority': self._validate_priority(priority),
            'business_value': business_value,
            'status': 'backlog',
            'created_at': datetime.now().isoformat(),
            'file_path': str(self.features_dir / f"{feature_id}_{title_slug}.md")
        }

        # Create feature file
        content = f"""# {feature_id}: {title}

## Type
Feature

## Priority
{priority}

## Status
backlog

## Description
{description or 'To be defined'}

## Acceptance Criteria
"""
        if acceptance_criteria:
            for criterion in acceptance_criteria:
                content += f"- [ ] {criterion}\n"
        else:
            content += "- [ ] To be defined\n"

        content += f"""
## Technical Notes
_Space for implementation details_

## Testing Requirements
_Define test scenarios_

## Dependencies
_List any dependencies_

## Created
{feature['created_at']}
"""

        with open(feature['file_path'], 'w') as f:
            f.write(content)

        self.metadata['features'].append(feature)
        # Also add to stories for legacy support
        self.metadata['stories'].append(feature)
        self.save_metadata()

        return {
            'action': 'create_feature',
            'feature': feature,
            'file_created': feature['file_path']
        }

    def create_story(self, *args, **kwargs):
        """Legacy support - redirects to create_feature"""
        return self.create_feature(*args, **kwargs)

    def create_bug(self, title, description=None, steps_to_reproduce=None, expected_behavior=None, actual_behavior=None, severity='medium'):
        """
        Create a bug report

        Args:
            title: Bug title
            description: Bug description
            steps_to_reproduce: List of reproduction steps
            expected_behavior: What should happen
            actual_behavior: What actually happens
            severity: Severity level (critical, high, medium, low)

        Returns:
            Bug creation details
        """
        bug_id = f"B{self.metadata['next_bug_id']:04d}"
        self.metadata['next_bug_id'] += 1

        # Create a brief slug from the title (max 30 chars, lowercase, underscores)
        title_slug = title.lower()
        title_slug = ''.join(c if c.isalnum() or c.isspace() else '' for c in title_slug)
        title_slug = '_'.join(title_slug.split())[:30]

        bug = {
            'id': bug_id,
            'type': 'bug',
            'title': title,
            'description': description or '',
            'steps_to_reproduce': steps_to_reproduce or [],
            'expected_behavior': expected_behavior or '',
            'actual_behavior': actual_behavior or '',
            'severity': severity,
            'priority': self._severity_to_priority(severity),
            'status': 'open',
            'created_at': datetime.now().isoformat(),
            'file_path': str(self.bugs_dir / f"{bug_id}_{title_slug}.md")
        }

        # Create bug file
        content = f"""# {bug_id}: {title}

## Type
Bug

## Severity
{severity}

## Status
open

## Description
{description or 'To be defined'}

## Steps to Reproduce
"""
        if steps_to_reproduce:
            for i, step in enumerate(steps_to_reproduce, 1):
                content += f"{i}. {step}\n"
        else:
            content += "1. To be defined\n"

        content += f"""
## Expected Behavior
{expected_behavior or 'To be defined'}

## Actual Behavior
{actual_behavior or 'To be defined'}

## Environment
_Specify environment details_

## Possible Root Cause
_Initial analysis_

## Proposed Fix
_Solution approach_

## Created
{bug['created_at']}
"""

        with open(bug['file_path'], 'w') as f:
            f.write(content)

        self.metadata['bugs'].append(bug)
        self.save_metadata()

        return {
            'action': 'create_bug',
            'bug': bug,
            'file_created': bug['file_path']
        }

    def create_tech_debt(self, title, description=None, impact=None, effort=None, priority='medium'):
        """
        Create a tech debt item

        Args:
            title: Tech debt title
            description: Detailed description
            impact: Impact description
            effort: Effort estimate (small, medium, large)
            priority: Priority level (critical, high, medium, low)

        Returns:
            Tech debt creation details
        """
        tech_debt_id = f"T{self.metadata['next_tech_debt_id']:04d}"
        self.metadata['next_tech_debt_id'] += 1

        tech_debt = {
            'id': tech_debt_id,
            'type': 'tech_debt',
            'title': title,
            'description': description or '',
            'impact': impact or '',
            'effort': effort or 'medium',
            'priority': priority,
            'status': 'backlog',
            'created_at': datetime.now().isoformat(),
            'file_path': str(self.tech_debt_dir / f"{tech_debt_id}.md")
        }

        # Create tech debt file
        content = f"""# {tech_debt_id}: {title}

## Type
Tech Debt

## Priority
{priority}

## Effort
{effort or 'medium'}

## Status
backlog

## Description
{description or 'To be defined'}

## Impact
{impact or 'To be defined'}

## Proposed Solution
_Define approach to address this tech debt_

## Created
{tech_debt['created_at']}
"""

        with open(tech_debt['file_path'], 'w') as f:
            f.write(content)

        self.metadata['tech_debt'].append(tech_debt)
        self.save_metadata()

        return {
            'action': 'create_tech_debt',
            'tech_debt': tech_debt,
            'file_created': tech_debt['file_path']
        }

    def create_epic(self, title, description=None, stories=None, objectives=None):
        """
        Create an epic

        Args:
            title: Epic title
            description: Epic description
            stories: List of story IDs or descriptions
            objectives: List of epic objectives

        Returns:
            Epic creation details
        """
        epic_id = f"E{self.metadata['next_epic_id']:03d}"
        self.metadata['next_epic_id'] += 1

        epic = {
            'id': epic_id,
            'type': 'epic',
            'title': title,
            'description': description or '',
            'stories': stories or [],
            'objectives': objectives or [],
            'status': 'planning',
            'created_at': datetime.now().isoformat(),
            'file_path': str(self.epics_dir / f"{epic_id}.md")
        }

        # Create epic file
        content = f"""# {epic_id}: {title}

## Type
Epic

## Status
planning

## Description
{description or 'To be defined'}

## Objectives
"""
        if objectives:
            for objective in objectives:
                content += f"- {objective}\n"
        else:
            content += "- To be defined\n"

        content += """
## User Stories
"""
        if stories:
            for story in stories:
                content += f"- [ ] {story}\n"
        else:
            content += "- [ ] Stories to be created\n"

        content += f"""
## Success Criteria
_Define what success looks like_

## Timeline
_Estimated timeline_

## Risks & Mitigations
_Identify potential risks_

## Created
{epic['created_at']}
"""

        with open(epic['file_path'], 'w') as f:
            f.write(content)

        self.metadata['epics'].append(epic)
        self.save_metadata()

        return {
            'action': 'create_epic',
            'epic': epic,
            'file_created': epic['file_path']
        }

    def list_items(self, item_type='all', status=None):
        """
        List features, bugs, tech debt, or epics

        Args:
            item_type: Type to list (feature, story, bug, tech_debt, epic, all)
            status: Filter by status

        Returns:
            List of items
        """
        items = []

        if item_type in ['feature', 'story', 'all']:
            # Support both features and legacy stories
            features = self.metadata.get('features', [])
            stories = self.metadata.get('stories', [])
            all_features = features if features else stories
            if status:
                all_features = [f for f in all_features if f.get('status') == status]
            items.extend(all_features)

        if item_type in ['bug', 'all']:
            bugs = self.metadata.get('bugs', [])
            if status:
                bugs = [b for b in bugs if b.get('status') == status]
            items.extend(bugs)

        if item_type in ['tech_debt', 'all']:
            tech_debt = self.metadata.get('tech_debt', [])
            if status:
                tech_debt = [t for t in tech_debt if t.get('status') == status]
            items.extend(tech_debt)

        if item_type in ['epic', 'all']:
            epics = self.metadata.get('epics', [])
            if status:
                epics = [e for e in epics if e.get('status') == status]
            items.extend(epics)

        # Add [DONE] marker to completed items
        for item in items:
            if item.get('status') in ['done', 'completed', 'resolved', 'closed']:
                item['display_title'] = f"{item['title']} [DONE]"
            else:
                item['display_title'] = item['title']

        return {
            'action': 'list_items',
            'type_filter': item_type,
            'status_filter': status,
            'count': len(items),
            'items': items
        }

    def _validate_priority(self, priority):
        """Validate and normalize priority level"""
        valid_priorities = ['critical', 'high', 'medium', 'low']
        if priority.lower() in valid_priorities:
            return priority.lower()
        return 'medium'

    def _severity_to_priority(self, severity):
        """Convert bug severity to priority"""
        severity_map = {
            'critical': 'critical',
            'high': 'high',
            'medium': 'medium',
            'low': 'low'
        }
        return severity_map.get(severity, 'medium')

    def update_status(self, item_id, new_status):
        """
        Update the status of an item

        Args:
            item_id: ID of the item (F0001, S001, B0001, T0001, E001)
            new_status: New status value

        Returns:
            Update confirmation
        """
        # Determine item type from ID prefix
        if item_id.startswith('F'):
            items = self.metadata.get('features', [])
            item_type = 'feature'
        elif item_id.startswith('S'):
            items = self.metadata.get('stories', [])
            item_type = 'story'
        elif item_id.startswith('B'):
            items = self.metadata.get('bugs', [])
            item_type = 'bug'
        elif item_id.startswith('T'):
            items = self.metadata.get('tech_debt', [])
            item_type = 'tech_debt'
        elif item_id.startswith('E'):
            items = self.metadata.get('epics', [])
            item_type = 'epic'
        else:
            return {
                'error': f'Unknown item type for ID: {item_id}'
            }

        # Find and update item
        for item in items:
            if item['id'] == item_id:
                old_status = item['status']
                old_file_path = item['file_path']

                # Handle file renaming for completed/reopened items
                # Create a brief slug from the title (max 30 chars, lowercase, underscores)
                title_slug = item.get('title', '').lower()
                title_slug = ''.join(c if c.isalnum() or c.isspace() else '' for c in title_slug)
                title_slug = '_'.join(title_slug.split())[:30]

                if new_status in ['done', 'completed', 'resolved', 'closed'] and old_status not in ['done', 'completed', 'resolved', 'closed']:
                    # Add underscore prefix when completing
                    dir_path = os.path.dirname(old_file_path)
                    base_name = os.path.basename(old_file_path)
                    if not base_name.startswith('_'):
                        # Get the item ID (e.g., F0030) - handle both old format (F0030.md) and new (F0030_title.md)
                        base_without_ext = base_name.replace('.md', '')
                        item_id_part = base_without_ext.split('_')[0]  # Get just the ID part
                        # Create new filename: _F0030_brief_title.md
                        new_file_path = os.path.join(dir_path, f'_{item_id_part}_{title_slug}.md')
                        if os.path.exists(old_file_path):
                            os.rename(old_file_path, new_file_path)
                        item['file_path'] = new_file_path
                elif old_status in ['done', 'completed', 'resolved', 'closed'] and new_status not in ['done', 'completed', 'resolved', 'closed']:
                    # Remove underscore prefix when reopening (but keep title)
                    dir_path = os.path.dirname(old_file_path)
                    base_name = os.path.basename(old_file_path)
                    if base_name.startswith('_'):
                        # Extract the item ID from the filename
                        base_without_ext = base_name.replace('.md', '')
                        parts = base_without_ext[1:].split('_', 1)  # Remove leading underscore and split
                        item_id_part = parts[0]  # Get the ID part (F0030)
                        # Keep the title in the new filename: F0030_title.md
                        new_file_path = os.path.join(dir_path, f'{item_id_part}_{title_slug}.md')
                        if os.path.exists(old_file_path):
                            os.rename(old_file_path, new_file_path)
                        item['file_path'] = new_file_path

                item['status'] = new_status
                item['updated_at'] = datetime.now().isoformat()

                # Add completed timestamp when moving to done
                if new_status in ['done', 'completed', 'resolved', 'closed'] and old_status not in ['done', 'completed', 'resolved', 'closed']:
                    item['completed_at'] = datetime.now().isoformat()

                # Track when item moves to in_progress
                if new_status == 'in_progress' and old_status != 'in_progress':
                    item['started_at'] = datetime.now().isoformat()

                self.save_metadata()

                # Also update the markdown file
                if os.path.exists(item['file_path']):
                    with open(item['file_path'], 'r') as f:
                        content = f.read()

                    # Replace status line
                    lines = content.split('\n')
                    for i, line in enumerate(lines):
                        if line.startswith('## Status'):
                            if i + 1 < len(lines):
                                lines[i + 1] = new_status
                            break

                    with open(item['file_path'], 'w') as f:
                        f.write('\n'.join(lines))

                return {
                    'action': 'update_status',
                    'item_id': item_id,
                    'item_type': item_type,
                    'old_status': old_status,
                    'new_status': new_status,
                    'file_renamed': old_file_path != item['file_path'],
                    'file_path': item['file_path']
                }

        return {
            'error': f'Item not found: {item_id}'
        }

    def get_item(self, item_id):
        """
        Get details of a specific item

        Args:
            item_id: ID of the item

        Returns:
            Item details
        """
        # Search in all item types
        all_items = (
            self.metadata.get('features', []) +
            self.metadata.get('stories', []) +
            self.metadata.get('bugs', []) +
            self.metadata.get('tech_debt', []) +
            self.metadata.get('epics', [])
        )

        for item in all_items:
            if item['id'] == item_id:
                # Read the markdown file for full content
                if os.path.exists(item['file_path']):
                    with open(item['file_path'], 'r') as f:
                        item['content'] = f.read()
                return {
                    'action': 'get_item',
                    'item': item
                }

        return {
            'error': f'Item not found: {item_id}'
        }

def main():
    """
    Entry point when Claude runs this script

    Usage:
        python 02_create_story.py feature "Title" --description "Desc" --priority high
        python 02_create_story.py bug "Bug title" --severity critical --priority high
        python 02_create_story.py tech_debt "Title" --priority medium --effort large
        python 02_create_story.py epic "Epic title" --description "Epic desc"
        python 02_create_story.py list [feature|bug|tech_debt|epic|all] --status [status]
        python 02_create_story.py update F0001 in_progress
        python 02_create_story.py get B0001
    """
    creator = StoryCreator()

    # Parse command line arguments
    args = sys.argv[1:]

    if not args:
        # Return usage information
        usage = {
            'script': '02_create_story.py',
            'description': 'Story, bug, and epic creation tool',
            'commands': [
                {
                    'command': 'feature "title" --description "desc" --priority [critical|high|medium|low]',
                    'description': 'Create a feature (F####)'
                },
                {
                    'command': 'bug "title" --severity [critical|high|medium|low] --priority [critical|high|medium|low]',
                    'description': 'Create a bug report (B####)'
                },
                {
                    'command': 'tech_debt "title" --priority [critical|high|medium|low] --effort [small|medium|large]',
                    'description': 'Create tech debt item (T####)'
                },
                {
                    'command': 'epic "title" --description "desc" --priority [critical|high|medium|low]',
                    'description': 'Create an epic (E###)'
                },
                {
                    'command': 'list [feature|bug|tech_debt|epic|all] --status [status]',
                    'description': 'List items'
                },
                {
                    'command': 'update [item_id] [new_status]',
                    'description': 'Update item status'
                },
                {
                    'command': 'get [item_id]',
                    'description': 'Get item details'
                }
            ]
        }
        print(json.dumps(usage, indent=2))
        return

    command = args[0]

    if command in ['story', 'feature']:
        # Parse feature creation arguments (story is legacy alias)
        title = args[1] if len(args) > 1 else 'Untitled Feature'
        description = None
        priority = 'medium'
        acceptance_criteria = []
        business_value = None

        # Parse optional arguments
        i = 2
        while i < len(args):
            if args[i] == '--description' and i + 1 < len(args):
                description = args[i + 1]
                i += 2
            elif args[i] == '--priority' and i + 1 < len(args):
                priority = args[i + 1]
                i += 2
            elif args[i] == '--criteria' and i + 1 < len(args):
                acceptance_criteria = args[i + 1].split('|')
                i += 2
            elif args[i] == '--business_value' and i + 1 < len(args):
                business_value = args[i + 1]
                i += 2
            else:
                i += 1

        result = creator.create_feature(title, description, acceptance_criteria, priority, business_value)
        print(json.dumps(result, indent=2))

    elif command == 'bug':
        # Parse bug creation arguments
        title = args[1] if len(args) > 1 else 'Untitled Bug'
        description = None
        severity = 'medium'
        steps = []
        expected = None
        actual = None

        # Parse optional arguments
        i = 2
        while i < len(args):
            if args[i] == '--description' and i + 1 < len(args):
                description = args[i + 1]
                i += 2
            elif args[i] == '--severity' and i + 1 < len(args):
                severity = args[i + 1]
                i += 2
            elif args[i] == '--steps' and i + 1 < len(args):
                steps = args[i + 1].split('|')
                i += 2
            elif args[i] == '--expected' and i + 1 < len(args):
                expected = args[i + 1]
                i += 2
            elif args[i] == '--actual' and i + 1 < len(args):
                actual = args[i + 1]
                i += 2
            else:
                i += 1

        result = creator.create_bug(title, description, steps, expected, actual, severity)
        print(json.dumps(result, indent=2))

    elif command == 'tech_debt':
        # Parse tech debt creation arguments
        title = args[1] if len(args) > 1 else 'Untitled Tech Debt'
        description = None
        impact = None
        effort = 'medium'
        priority = 'medium'

        # Parse optional arguments
        i = 2
        while i < len(args):
            if args[i] == '--description' and i + 1 < len(args):
                description = args[i + 1]
                i += 2
            elif args[i] == '--impact' and i + 1 < len(args):
                impact = args[i + 1]
                i += 2
            elif args[i] == '--effort' and i + 1 < len(args):
                effort = args[i + 1]
                i += 2
            elif args[i] == '--priority' and i + 1 < len(args):
                priority = args[i + 1]
                i += 2
            else:
                i += 1

        result = creator.create_tech_debt(title, description, impact, effort, priority)
        print(json.dumps(result, indent=2))

    elif command == 'epic':
        # Parse epic creation arguments
        title = args[1] if len(args) > 1 else 'Untitled Epic'
        description = None
        stories = []
        objectives = []

        # Parse optional arguments
        i = 2
        while i < len(args):
            if args[i] == '--description' and i + 1 < len(args):
                description = args[i + 1]
                i += 2
            elif args[i] == '--stories' and i + 1 < len(args):
                stories = args[i + 1].split('|')
                i += 2
            elif args[i] == '--objectives' and i + 1 < len(args):
                objectives = args[i + 1].split('|')
                i += 2
            else:
                i += 1

        result = creator.create_epic(title, description, stories, objectives)
        print(json.dumps(result, indent=2))

    elif command == 'list':
        # Parse list arguments
        item_type = args[1] if len(args) > 1 else 'all'
        status = None

        if '--status' in args:
            idx = args.index('--status')
            if idx + 1 < len(args):
                status = args[idx + 1]

        result = creator.list_items(item_type, status)
        print(json.dumps(result, indent=2))

    elif command == 'update':
        # Parse update arguments
        if len(args) < 3:
            print(json.dumps({'error': 'Usage: update [item_id] [new_status]'}, indent=2))
            return

        item_id = args[1]
        new_status = args[2]
        result = creator.update_status(item_id, new_status)
        print(json.dumps(result, indent=2))

    elif command == 'get':
        # Parse get arguments
        if len(args) < 2:
            print(json.dumps({'error': 'Usage: get [item_id]'}, indent=2))
            return

        item_id = args[1]
        result = creator.get_item(item_id)
        print(json.dumps(result, indent=2))

    else:
        print(json.dumps({'error': f'Unknown command: {command}'}, indent=2))

if __name__ == "__main__":
    main()