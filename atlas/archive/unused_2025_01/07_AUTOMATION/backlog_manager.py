#!/usr/bin/env python3
"""
Atlas Backlog Manager - Standardized backlog management with clear item types
F#### - Features, B#### - Bugs, T#### - Technical Debt, E#### - Epics
"""

import sys
import json
import os
from pathlib import Path
from datetime import datetime

class BacklogManager:
    """
    Manages standardized backlog items with priority methodology
    """

    def __init__(self):
        self.features_dir = Path('backlog/features')
        self.features_dir.mkdir(parents=True, exist_ok=True)
        self.bugs_dir = Path('backlog/bugs')
        self.bugs_dir.mkdir(parents=True, exist_ok=True)
        self.tech_debt_dir = Path('backlog/tech_debt')
        self.tech_debt_dir.mkdir(parents=True, exist_ok=True)
        self.epics_dir = Path('backlog/epics')
        self.epics_dir.mkdir(parents=True, exist_ok=True)

        self.metadata_file = Path('.atlas/backlog_metadata.json')
        self.metadata_file.parent.mkdir(parents=True, exist_ok=True)
        self.metadata = self.load_metadata()

    def load_metadata(self):
        """Load backlog metadata"""
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
            'epics': []
        }

    def save_metadata(self):
        """Save backlog metadata"""
        with open(self.metadata_file, 'w') as f:
            json.dump(self.metadata, f, indent=2)

    def create_feature(self, title, description=None, acceptance_criteria=None,
                      priority='medium', business_value=None, effort=None):
        """
        Create a feature (F####)

        Priority: Based on business value and user impact
        """
        feature_id = f"F{self.metadata['next_feature_id']:04d}"
        self.metadata['next_feature_id'] += 1

        feature = {
            'id': feature_id,
            'type': 'feature',
            'title': title,
            'description': description or '',
            'acceptance_criteria': acceptance_criteria or [],
            'priority': self._validate_priority(priority),
            'business_value': business_value or 'medium',
            'effort': effort or 'medium',
            'status': 'backlog',
            'created_at': datetime.now().isoformat(),
            'file_path': str(self.features_dir / f"{feature_id}.md")
        }

        # Calculate priority score
        feature['priority_score'] = self.calculate_priority_score(feature)

        # Create feature file
        self._create_markdown_file(feature)

        self.metadata['features'].append(feature)
        self.save_metadata()

        return {
            'action': 'create_feature',
            'item': feature,
            'priority_score': feature['priority_score']
        }

    def create_bug(self, title, description=None, steps_to_reproduce=None,
                   severity='medium', user_impact='medium'):
        """
        Create a bug (B####)

        Priority: Based on severity and user impact
        """
        bug_id = f"B{self.metadata['next_bug_id']:04d}"
        self.metadata['next_bug_id'] += 1

        bug = {
            'id': bug_id,
            'type': 'bug',
            'title': title,
            'description': description or '',
            'steps_to_reproduce': steps_to_reproduce or [],
            'severity': severity,
            'user_impact': user_impact,
            'priority': self._severity_to_priority(severity, user_impact),
            'status': 'backlog',
            'created_at': datetime.now().isoformat(),
            'file_path': str(self.bugs_dir / f"{bug_id}.md")
        }

        bug['priority_score'] = self.calculate_priority_score(bug)
        self._create_markdown_file(bug)

        self.metadata['bugs'].append(bug)
        self.save_metadata()

        return {
            'action': 'create_bug',
            'item': bug,
            'priority_score': bug['priority_score']
        }

    def create_tech_debt(self, title, description=None, impact=None,
                        effort='medium', risk='medium'):
        """
        Create technical debt item (T####)

        Priority: Based on risk and long-term impact
        """
        tech_debt_id = f"T{self.metadata['next_tech_debt_id']:04d}"
        self.metadata['next_tech_debt_id'] += 1

        tech_debt = {
            'id': tech_debt_id,
            'type': 'tech_debt',
            'title': title,
            'description': description or '',
            'impact': impact or '',
            'effort': effort,
            'risk': risk,
            'priority': self._tech_debt_priority(risk, effort),
            'status': 'backlog',
            'created_at': datetime.now().isoformat(),
            'file_path': str(self.tech_debt_dir / f"{tech_debt_id}.md")
        }

        tech_debt['priority_score'] = self.calculate_priority_score(tech_debt)
        self._create_markdown_file(tech_debt)

        self.metadata['tech_debt'].append(tech_debt)
        self.save_metadata()

        return {
            'action': 'create_tech_debt',
            'item': tech_debt,
            'priority_score': tech_debt['priority_score']
        }

    def create_epic(self, title, description=None, features=None,
                   business_goal=None, target_date=None):
        """
        Create an epic (E####)

        Priority: Based on strategic importance
        """
        epic_id = f"E{self.metadata['next_epic_id']:04d}"
        self.metadata['next_epic_id'] += 1

        epic = {
            'id': epic_id,
            'type': 'epic',
            'title': title,
            'description': description or '',
            'features': features or [],
            'business_goal': business_goal or '',
            'target_date': target_date,
            'priority': 'high' if business_goal else 'medium',
            'status': 'planning',
            'created_at': datetime.now().isoformat(),
            'file_path': str(self.epics_dir / f"{epic_id}.md")
        }

        epic['priority_score'] = self.calculate_priority_score(epic)
        self._create_markdown_file(epic)

        self.metadata['epics'].append(epic)
        self.save_metadata()

        return {
            'action': 'create_epic',
            'item': epic,
            'priority_score': epic['priority_score']
        }

    def calculate_priority_score(self, item):
        """
        Calculate priority score using WSJF (Weighted Shortest Job First) methodology

        Score = (Business Value + Time Criticality + Risk/Opportunity) / Job Size
        """
        base_scores = {
            'critical': 1000,
            'high': 750,
            'medium': 500,
            'low': 250
        }

        score = base_scores.get(item.get('priority', 'medium'), 500)

        # Type-specific scoring
        if item['type'] == 'bug':
            # Bugs: Severity × User Impact
            severity_scores = {'critical': 400, 'high': 300, 'medium': 200, 'low': 100}
            impact_scores = {'critical': 300, 'high': 200, 'medium': 100, 'low': 50}

            score += severity_scores.get(item.get('severity', 'medium'), 200)
            score += impact_scores.get(item.get('user_impact', 'medium'), 100)

        elif item['type'] == 'feature':
            # Features: Business Value / Effort
            value_scores = {'critical': 400, 'high': 300, 'medium': 200, 'low': 100}
            effort_penalty = {'small': 1.5, 'medium': 1.0, 'large': 0.7, 'xl': 0.5}

            value_score = value_scores.get(item.get('business_value', 'medium'), 200)
            effort_mult = effort_penalty.get(item.get('effort', 'medium'), 1.0)
            score += int(value_score * effort_mult)

        elif item['type'] == 'tech_debt':
            # Tech Debt: Risk × (1 / Effort)
            risk_scores = {'critical': 400, 'high': 300, 'medium': 200, 'low': 100}
            effort_penalty = {'small': 1.5, 'medium': 1.0, 'large': 0.7, 'xl': 0.5}

            risk_score = risk_scores.get(item.get('risk', 'medium'), 200)
            effort_mult = effort_penalty.get(item.get('effort', 'medium'), 1.0)
            score += int(risk_score * effort_mult)

        # Status boost
        status_boost = {
            'blocked': 500,      # Unblock ASAP
            'in_progress': 300,  # Finish what's started
            'in_review': 250,    # Almost done
            'ready': 100,        # Ready to start
            'backlog': 0
        }
        score += status_boost.get(item.get('status', 'backlog'), 0)

        # Age factor (older items get slight boost)
        if 'created_at' in item:
            try:
                created = datetime.fromisoformat(item['created_at'])
                age_days = (datetime.now() - created).days
                score += min(age_days * 5, 200)  # Max 200 point age bonus
            except:
                pass

        return score

    def _validate_priority(self, priority):
        """Validate priority value"""
        valid = ['critical', 'high', 'medium', 'low']
        return priority.lower() if priority.lower() in valid else 'medium'

    def _severity_to_priority(self, severity, user_impact):
        """Map bug severity and impact to priority"""
        if severity == 'critical' or user_impact == 'critical':
            return 'critical'
        elif severity == 'high' or user_impact == 'high':
            return 'high'
        elif severity == 'low' and user_impact == 'low':
            return 'low'
        return 'medium'

    def _tech_debt_priority(self, risk, effort):
        """Calculate tech debt priority"""
        if risk == 'critical':
            return 'critical' if effort != 'xl' else 'high'
        elif risk == 'high':
            return 'high' if effort in ['small', 'medium'] else 'medium'
        elif risk == 'low':
            return 'low'
        return 'medium'

    def _create_markdown_file(self, item):
        """Create markdown file for backlog item"""
        type_names = {
            'feature': 'Feature',
            'bug': 'Bug',
            'tech_debt': 'Technical Debt',
            'epic': 'Epic'
        }

        content = f"""# {item['id']}: {item['title']}

## Type
{type_names[item['type']]}

## Priority
{item['priority'].title()} (Score: {item.get('priority_score', 0)})

## Status
{item['status']}

## Description
{item.get('description', 'To be defined')}

"""

        # Type-specific sections
        if item['type'] == 'feature':
            content += f"""## Business Value
{item.get('business_value', 'Medium')}

## Effort
{item.get('effort', 'Medium')}

## Acceptance Criteria
"""
            for criterion in item.get('acceptance_criteria', ['To be defined']):
                content += f"- [ ] {criterion}\n"

        elif item['type'] == 'bug':
            content += f"""## Severity
{item.get('severity', 'Medium')}

## User Impact
{item.get('user_impact', 'Medium')}

## Steps to Reproduce
"""
            for step in item.get('steps_to_reproduce', ['To be defined']):
                content += f"1. {step}\n"

        elif item['type'] == 'tech_debt':
            content += f"""## Risk Level
{item.get('risk', 'Medium')}

## Effort Required
{item.get('effort', 'Medium')}

## Impact if Not Addressed
{item.get('impact', 'To be defined')}
"""

        elif item['type'] == 'epic':
            content += f"""## Business Goal
{item.get('business_goal', 'To be defined')}

## Target Date
{item.get('target_date', 'TBD')}

## Features
"""
            for feature in item.get('features', ['To be defined']):
                content += f"- [ ] {feature}\n"

        content += f"""
## Created
{item['created_at']}
"""

        with open(item['file_path'], 'w') as f:
            f.write(content)

    def get_prioritized_backlog(self, item_type='all', limit=20):
        """Get prioritized backlog"""
        items = []

        if item_type in ['feature', 'all']:
            items.extend(self.metadata['features'])
        if item_type in ['bug', 'all']:
            items.extend(self.metadata['bugs'])
        if item_type in ['tech_debt', 'all']:
            items.extend(self.metadata['tech_debt'])
        if item_type in ['epic', 'all']:
            items.extend(self.metadata['epics'])

        # Sort by priority score
        items.sort(key=lambda x: self.calculate_priority_score(x), reverse=True)

        return items[:limit] if limit else items

    def update_status(self, item_id, new_status):
        """Update item status"""
        # Find item across all types
        for item_type in ['features', 'bugs', 'tech_debt', 'epics']:
            for item in self.metadata.get(item_type, []):
                if item['id'] == item_id:
                    item['status'] = new_status
                    item['updated_at'] = datetime.now().isoformat()

                    if new_status == 'done':
                        item['completed_at'] = datetime.now().isoformat()
                    elif new_status == 'in_progress':
                        item['started_at'] = datetime.now().isoformat()

                    self.save_metadata()
                    return {'success': True, 'item': item}

        return {'error': f'Item {item_id} not found'}

def main():
    """
    Entry point for backlog manager

    Usage:
        python3 backlog_manager.py feature "Title" --priority high --value high --effort medium
        python3 backlog_manager.py bug "Title" --severity critical --impact high
        python3 backlog_manager.py tech_debt "Title" --risk high --effort large
        python3 backlog_manager.py epic "Title" --goal "Business goal"
        python3 backlog_manager.py list [all|feature|bug|tech_debt|epic]
        python3 backlog_manager.py update F0001 in_progress
        python3 backlog_manager.py priority_guide
    """
    manager = BacklogManager()
    args = sys.argv[1:]

    if not args:
        print(json.dumps({
            'error': 'No command provided',
            'usage': 'See --help for usage'
        }, indent=2))
        return

    command = args[0]

    if command == 'feature':
        title = args[1] if len(args) > 1 else 'Untitled Feature'
        kwargs = {}

        i = 2
        while i < len(args):
            if args[i] == '--priority' and i+1 < len(args):
                kwargs['priority'] = args[i+1]
                i += 2
            elif args[i] == '--value' and i+1 < len(args):
                kwargs['business_value'] = args[i+1]
                i += 2
            elif args[i] == '--effort' and i+1 < len(args):
                kwargs['effort'] = args[i+1]
                i += 2
            elif args[i] == '--description' and i+1 < len(args):
                kwargs['description'] = args[i+1]
                i += 2
            else:
                i += 1

        result = manager.create_feature(title, **kwargs)
        print(json.dumps(result, indent=2))

    elif command == 'bug':
        title = args[1] if len(args) > 1 else 'Untitled Bug'
        kwargs = {}

        i = 2
        while i < len(args):
            if args[i] == '--severity' and i+1 < len(args):
                kwargs['severity'] = args[i+1]
                i += 2
            elif args[i] == '--impact' and i+1 < len(args):
                kwargs['user_impact'] = args[i+1]
                i += 2
            elif args[i] == '--description' and i+1 < len(args):
                kwargs['description'] = args[i+1]
                i += 2
            else:
                i += 1

        result = manager.create_bug(title, **kwargs)
        print(json.dumps(result, indent=2))

    elif command == 'tech_debt':
        title = args[1] if len(args) > 1 else 'Untitled Tech Debt'
        kwargs = {}

        i = 2
        while i < len(args):
            if args[i] == '--risk' and i+1 < len(args):
                kwargs['risk'] = args[i+1]
                i += 2
            elif args[i] == '--effort' and i+1 < len(args):
                kwargs['effort'] = args[i+1]
                i += 2
            elif args[i] == '--impact' and i+1 < len(args):
                kwargs['impact'] = args[i+1]
                i += 2
            else:
                i += 1

        result = manager.create_tech_debt(title, **kwargs)
        print(json.dumps(result, indent=2))

    elif command == 'list':
        item_type = args[1] if len(args) > 1 else 'all'
        items = manager.get_prioritized_backlog(item_type)

        print(f"\n{'='*80}")
        print(f"PRIORITIZED BACKLOG ({item_type.upper()})")
        print(f"{'='*80}\n")

        for item in items:
            icon = {
                'feature': '✨',
                'bug': '🐛',
                'tech_debt': '🔧',
                'epic': '📚'
            }.get(item['type'], '📝')

            priority_icon = {
                'critical': '🔴',
                'high': '🟠',
                'medium': '🟡',
                'low': '🟢'
            }.get(item['priority'], '')

            print(f"{icon} [{item['id']}] {priority_icon} {item['title'][:50]}")
            print(f"    Priority Score: {manager.calculate_priority_score(item)}")
            print(f"    Status: {item['status']}")
            print()

    elif command == 'priority_guide':
        print("""
BACKLOG PRIORITY METHODOLOGY
=============================

PRIORITY CALCULATION (WSJF - Weighted Shortest Job First)
Score = (Business Value + Time Criticality + Risk) / Job Size

ITEM TYPE PRIORITIES
--------------------

FEATURES (F####)
Priority based on:
- Business Value (critical/high/medium/low)
- Effort Required (small/medium/large/xl)
- User Impact
Formula: Business Value × (1/Effort)

BUGS (B####)
Priority based on:
- Severity (critical/high/medium/low)
- User Impact (critical/high/medium/low)
- System Stability
Formula: Severity × User Impact

TECHNICAL DEBT (T####)
Priority based on:
- Risk Level (critical/high/medium/low)
- Future Impact
- Effort to Fix (small/medium/large/xl)
Formula: Risk × (1/Effort)

EPICS (E####)
Priority based on:
- Strategic Importance
- Business Goals
- Timeline

PRIORITY LEVELS
---------------
🔴 CRITICAL (Score: 1000+)
- Production breaking bugs
- Security vulnerabilities
- Revenue impacting features
- Critical tech debt

🟠 HIGH (Score: 750-999)
- Major bugs affecting many users
- High-value features
- Risky technical debt
- Strategic epics

🟡 MEDIUM (Score: 500-749)
- Standard features
- Non-critical bugs
- Improvement tech debt
- Planning epics

🟢 LOW (Score: <500)
- Nice-to-have features
- Minor bugs
- Low-risk tech debt
- Future epics

BOOSTING FACTORS
----------------
+500 pts: Blocked items (unblock ASAP)
+300 pts: In-progress items (finish what's started)
+5 pts/day: Age factor (older items get priority)

EXAMPLES
--------
1. Critical Bug (B0001): Severity=Critical, Impact=High
   Score: 1000 + 400 + 200 = 1600

2. High-Value Feature (F0001): Value=High, Effort=Small
   Score: 750 + (300 × 1.5) = 1200

3. Risky Tech Debt (T0001): Risk=High, Effort=Medium
   Score: 500 + (300 × 1.0) = 800
""")

    elif command == 'update':
        if len(args) < 3:
            print(json.dumps({'error': 'Usage: update [item_id] [new_status]'}, indent=2))
            return

        result = manager.update_status(args[1], args[2])
        print(json.dumps(result, indent=2))

    elif command == '--help':
        print("""
Atlas Backlog Manager - Standardized Backlog with Priority Methodology

COMMANDS:
  feature "Title" [options]     Create feature (F####)
    --priority [critical|high|medium|low]
    --value [critical|high|medium|low]
    --effort [small|medium|large|xl]
    --description "Description"

  bug "Title" [options]          Create bug (B####)
    --severity [critical|high|medium|low]
    --impact [critical|high|medium|low]
    --description "Description"

  tech_debt "Title" [options]    Create tech debt (T####)
    --risk [critical|high|medium|low]
    --effort [small|medium|large|xl]
    --impact "Impact description"

  epic "Title" [options]         Create epic (E####)
    --goal "Business goal"
    --target "Target date"

  list [type]                    Show prioritized backlog
  update [ID] [status]           Update item status
  priority_guide                 Show priority methodology

EXAMPLES:
  python3 backlog_manager.py feature "Add user login" --priority high --value high --effort medium
  python3 backlog_manager.py bug "App crashes on startup" --severity critical --impact critical
  python3 backlog_manager.py tech_debt "Refactor database layer" --risk medium --effort large
  python3 backlog_manager.py list all
  python3 backlog_manager.py update F0001 in_progress
""")

if __name__ == "__main__":
    main()