#!/usr/bin/env python3
"""
Atlas Backlog Automation System
Automates 80%+ of backlog management tasks
"""

import json
import yaml
import os
from pathlib import Path
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Tuple
import subprocess
import re
from dataclasses import dataclass, asdict
from enum import Enum

class StoryStatus(Enum):
    BACKLOG = "backlog"
    READY = "ready"
    IN_PROGRESS = "in_progress"
    IN_REVIEW = "in_review"
    TESTING = "testing"
    DONE = "done"
    BLOCKED = "blocked"

class StoryType(Enum):
    FEATURE = "feature"
    BUG = "bug"
    TECH_DEBT = "tech_debt"
    RESEARCH = "research"
    OPTIMIZATION = "optimization"

@dataclass
class Story:
    id: str
    title: str
    type: StoryType
    status: StoryStatus
    priority: int
    estimate: int  # Story points
    wave: Optional[int] = None
    epic: Optional[str] = None
    dependencies: List[str] = None
    acceptance_criteria: List[str] = None
    created_date: str = None
    completed_date: Optional[str] = None
    assigned_to: Optional[str] = None
    labels: List[str] = None
    implementation_notes: Optional[str] = None
    test_coverage: Optional[float] = None
    quality_score: Optional[float] = None

    def __post_init__(self):
        if self.dependencies is None:
            self.dependencies = []
        if self.acceptance_criteria is None:
            self.acceptance_criteria = []
        if self.labels is None:
            self.labels = []
        if self.created_date is None:
            self.created_date = datetime.now().isoformat()

class BacklogAutomation:
    """Main backlog automation system"""

    def __init__(self, project_root: Path = None):
        self.project_root = project_root or Path.cwd()
        self.atlas_dir = self.project_root / '.atlas'
        self.backlog_file = self.atlas_dir / 'backlog.json'
        self.metadata_file = self.atlas_dir / 'backlog_metadata.json'
        self.config_file = self.atlas_dir / 'backlog_config.yaml'

        self.atlas_dir.mkdir(exist_ok=True)
        self.load_backlog()
        self.load_config()

    def load_backlog(self):
        """Load backlog from JSON file"""
        if self.backlog_file.exists():
            with open(self.backlog_file) as f:
                data = json.load(f)
                self.stories = {
                    s['id']: Story(**s) for s in data.get('stories', [])
                }
        else:
            self.stories = {}

    def save_backlog(self):
        """Save backlog to JSON file"""
        data = {
            'stories': [asdict(s) for s in self.stories.values()],
            'last_updated': datetime.now().isoformat()
        }
        with open(self.backlog_file, 'w') as f:
            json.dump(data, f, indent=2, default=str)

    def load_config(self):
        """Load automation configuration"""
        if self.config_file.exists():
            with open(self.config_file) as f:
                self.config = yaml.safe_load(f)
        else:
            self.config = self.get_default_config()
            self.save_config()

    def save_config(self):
        """Save configuration"""
        with open(self.config_file, 'w') as f:
            yaml.dump(self.config, f, default_flow_style=False)

    def get_default_config(self) -> Dict:
        """Get default configuration"""
        return {
            'auto_complete': {
                'enabled': True,
                'scan_patterns': [
                    r'✅\s*Completed:\s*([FB]\d{4})',
                    r'Fixed\s+([FB]\d{4})',
                    r'Implemented\s+([FB]\d{4})'
                ],
                'test_pass_threshold': 0.95,
                'quality_threshold': 80
            },
            'prioritization': {
                'algorithm': 'weighted',
                'factors': {
                    'business_value': 0.35,
                    'technical_risk': 0.25,
                    'dependencies': 0.20,
                    'effort': 0.20
                }
            },
            'lifecycle': {
                'auto_transition': True,
                'review_required': True,
                'test_required': True
            },
            'notifications': {
                'slack_webhook': None,
                'email': None
            },
            'velocity': {
                'rolling_average_sprints': 3,
                'default_velocity': 20
            }
        }

    def scan_for_completed_stories(self) -> List[str]:
        """Scan codebase and commits for completed stories"""
        completed = []

        # Scan recent commits
        try:
            result = subprocess.run(
                ['git', 'log', '--oneline', '-50'],
                capture_output=True, text=True, cwd=self.project_root
            )

            for pattern in self.config['auto_complete']['scan_patterns']:
                matches = re.findall(pattern, result.stdout)
                completed.extend(matches)

        except subprocess.CalledProcessError:
            pass

        # Scan test results
        test_results = self.check_test_results()

        # Scan code comments
        code_markers = self.scan_code_markers()
        completed.extend(code_markers)

        return list(set(completed))

    def check_test_results(self) -> Dict:
        """Check test results for stories"""
        results = {}

        # Check for test result files
        test_dirs = [
            self.project_root / 'android' / 'app' / 'build' / 'test-results',
            self.project_root / 'test-results',
            self.project_root / '.atlas' / 'test-results'
        ]

        for test_dir in test_dirs:
            if test_dir.exists():
                for test_file in test_dir.glob('**/*.xml'):
                    # Parse test results
                    with open(test_file) as f:
                        content = f.read()
                        # Extract story IDs from test names
                        story_matches = re.findall(r'([FB]\d{4})', content)
                        for story_id in story_matches:
                            if story_id not in results:
                                results[story_id] = {'passed': 0, 'failed': 0}
                            # Simple check - would need proper XML parsing
                            if 'failure' not in content:
                                results[story_id]['passed'] += 1
                            else:
                                results[story_id]['failed'] += 1

        return results

    def scan_code_markers(self) -> List[str]:
        """Scan code for completion markers"""
        completed = []
        markers = ['COMPLETED:', 'DONE:', 'FIXED:']

        for ext in ['.kt', '.java', '.py', '.js', '.ts']:
            for file in self.project_root.rglob(f'*{ext}'):
                if '.atlas' in str(file) or 'build' in str(file):
                    continue

                try:
                    with open(file) as f:
                        content = f.read()
                        for marker in markers:
                            pattern = f'{marker}\\s*([FB]\\d{{4}})'
                            matches = re.findall(pattern, content)
                            completed.extend(matches)
                except:
                    continue

        return completed

    def auto_complete_stories(self):
        """Automatically mark stories as complete based on evidence"""
        completed_ids = self.scan_for_completed_stories()
        test_results = self.check_test_results()

        updates = []
        for story_id in completed_ids:
            if story_id in self.stories:
                story = self.stories[story_id]

                if story.status != StoryStatus.DONE:
                    # Check test coverage
                    test_pass = True
                    if story_id in test_results:
                        total = test_results[story_id]['passed'] + test_results[story_id]['failed']
                        if total > 0:
                            pass_rate = test_results[story_id]['passed'] / total
                            test_pass = pass_rate >= self.config['auto_complete']['test_pass_threshold']
                            story.test_coverage = pass_rate

                    # Check quality
                    quality_pass = True
                    if story.quality_score:
                        quality_pass = story.quality_score >= self.config['auto_complete']['quality_threshold']

                    if test_pass and quality_pass:
                        story.status = StoryStatus.DONE
                        story.completed_date = datetime.now().isoformat()
                        updates.append(story_id)

        if updates:
            self.save_backlog()
            print(f"✅ Auto-completed {len(updates)} stories: {', '.join(updates)}")

        return updates

    def calculate_priority(self, story: Story) -> int:
        """Calculate story priority using weighted factors"""
        if self.config['prioritization']['algorithm'] != 'weighted':
            return story.priority

        factors = self.config['prioritization']['factors']
        score = 0

        # Business value (based on type and labels)
        if story.type == StoryType.BUG:
            business_value = 90
        elif story.type == StoryType.FEATURE:
            business_value = 70
        elif 'critical' in story.labels:
            business_value = 95
        elif 'high-impact' in story.labels:
            business_value = 80
        else:
            business_value = 50

        score += business_value * factors['business_value']

        # Technical risk
        risk = 30  # Default medium risk
        if 'security' in story.labels:
            risk = 90
        elif 'performance' in story.labels:
            risk = 70
        elif story.type == StoryType.TECH_DEBT:
            risk = 60

        score += risk * factors['technical_risk']

        # Dependencies
        blocked_score = 100 if not story.dependencies else 50
        score += blocked_score * factors['dependencies']

        # Effort (inverse - smaller is better)
        effort_score = max(0, 100 - (story.estimate * 10))
        score += effort_score * factors['effort']

        return int(score)

    def prioritize_backlog(self):
        """Re-prioritize entire backlog"""
        for story in self.stories.values():
            if story.status in [StoryStatus.BACKLOG, StoryStatus.READY]:
                story.priority = self.calculate_priority(story)

        self.save_backlog()

        # Return sorted backlog
        prioritized = sorted(
            [s for s in self.stories.values()
             if s.status in [StoryStatus.BACKLOG, StoryStatus.READY]],
            key=lambda s: s.priority,
            reverse=True
        )

        return prioritized

    def manage_story_lifecycle(self, story_id: str, action: str) -> bool:
        """Manage story state transitions"""
        if story_id not in self.stories:
            return False

        story = self.stories[story_id]
        transitions = {
            'start': (StoryStatus.READY, StoryStatus.IN_PROGRESS),
            'submit': (StoryStatus.IN_PROGRESS, StoryStatus.IN_REVIEW),
            'approve': (StoryStatus.IN_REVIEW, StoryStatus.TESTING),
            'complete': (StoryStatus.TESTING, StoryStatus.DONE),
            'block': (story.status, StoryStatus.BLOCKED),
            'unblock': (StoryStatus.BLOCKED, StoryStatus.IN_PROGRESS)
        }

        if action in transitions:
            from_status, to_status = transitions[action]

            # Validate transition
            if story.status == from_status or action in ['block']:
                # Check dependencies
                if action == 'start':
                    for dep_id in story.dependencies:
                        if dep_id in self.stories:
                            if self.stories[dep_id].status != StoryStatus.DONE:
                                print(f"❌ Cannot start {story_id}: Dependency {dep_id} not complete")
                                return False

                # Update status
                story.status = to_status

                # Update dates
                if to_status == StoryStatus.DONE:
                    story.completed_date = datetime.now().isoformat()

                self.save_backlog()
                print(f"✅ {story_id}: {from_status.value} → {to_status.value}")
                return True

        return False

    def create_story(self, title: str, story_type: StoryType,
                    estimate: int = 3, **kwargs) -> Story:
        """Create new story"""
        # Generate ID
        prefix = 'F' if story_type == StoryType.FEATURE else 'B'
        existing_ids = [s for s in self.stories.keys() if s.startswith(prefix)]
        next_num = len(existing_ids) + 1
        story_id = f"{prefix}{next_num:04d}"

        story = Story(
            id=story_id,
            title=title,
            type=story_type,
            status=StoryStatus.BACKLOG,
            priority=50,
            estimate=estimate,
            **kwargs
        )

        # Auto-calculate priority
        story.priority = self.calculate_priority(story)

        self.stories[story_id] = story
        self.save_backlog()

        return story

    def bulk_update_stories(self, story_ids: List[str], updates: Dict):
        """Bulk update multiple stories"""
        updated = []
        for story_id in story_ids:
            if story_id in self.stories:
                story = self.stories[story_id]
                for key, value in updates.items():
                    if hasattr(story, key):
                        setattr(story, key, value)
                updated.append(story_id)

        if updated:
            self.save_backlog()
            print(f"✅ Updated {len(updated)} stories")

        return updated

    def get_wave_stories(self, wave: int) -> List[Story]:
        """Get all stories in a wave"""
        return [s for s in self.stories.values() if s.wave == wave]

    def plan_wave(self, num_stories: int, velocity: int = None) -> List[Story]:
        """Plan next wave based on velocity"""
        if velocity is None:
            velocity = self.config['velocity']['default_velocity']

        # Get prioritized ready stories
        available = self.prioritize_backlog()

        wave_stories = []
        total_points = 0

        for story in available:
            if total_points + story.estimate <= velocity:
                wave_stories.append(story)
                total_points += story.estimate

            if len(wave_stories) >= num_stories:
                break

        return wave_stories

    def generate_analytics(self) -> Dict:
        """Generate backlog analytics"""
        analytics = {
            'total_stories': len(self.stories),
            'by_status': {},
            'by_type': {},
            'velocity': self.calculate_velocity(),
            'cycle_time': self.calculate_cycle_time(),
            'blocked_stories': [],
            'high_priority': []
        }

        # Count by status
        for status in StoryStatus:
            count = len([s for s in self.stories.values() if s.status == status])
            analytics['by_status'][status.value] = count

        # Count by type
        for story_type in StoryType:
            count = len([s for s in self.stories.values() if s.type == story_type])
            analytics['by_type'][story_type.value] = count

        # Blocked stories
        analytics['blocked_stories'] = [
            s.id for s in self.stories.values()
            if s.status == StoryStatus.BLOCKED
        ]

        # High priority stories
        analytics['high_priority'] = [
            s.id for s in sorted(
                self.stories.values(),
                key=lambda s: s.priority,
                reverse=True
            )[:10]
        ]

        return analytics

    def calculate_velocity(self, num_sprints: int = None) -> float:
        """Calculate team velocity"""
        if num_sprints is None:
            num_sprints = self.config['velocity']['rolling_average_sprints']

        completed = [
            s for s in self.stories.values()
            if s.status == StoryStatus.DONE and s.completed_date
        ]

        if not completed:
            return self.config['velocity']['default_velocity']

        # Group by sprint (2-week periods)
        now = datetime.now()
        sprint_points = {}

        for story in completed:
            completed_date = datetime.fromisoformat(story.completed_date)
            weeks_ago = (now - completed_date).days // 14

            if weeks_ago < num_sprints:
                if weeks_ago not in sprint_points:
                    sprint_points[weeks_ago] = 0
                sprint_points[weeks_ago] += story.estimate

        if sprint_points:
            return sum(sprint_points.values()) / len(sprint_points)
        else:
            return self.config['velocity']['default_velocity']

    def calculate_cycle_time(self) -> float:
        """Calculate average cycle time in days"""
        completed = [
            s for s in self.stories.values()
            if s.status == StoryStatus.DONE and s.completed_date
        ]

        if not completed:
            return 0

        cycle_times = []
        for story in completed:
            start = datetime.fromisoformat(story.created_date)
            end = datetime.fromisoformat(story.completed_date)
            cycle_times.append((end - start).days)

        return sum(cycle_times) / len(cycle_times) if cycle_times else 0

    def export_markdown_report(self) -> str:
        """Export backlog as markdown report"""
        report = ["# Backlog Report",
                 f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M')}", ""]

        # Summary
        analytics = self.generate_analytics()
        report.append("## Summary")
        report.append(f"- Total Stories: {analytics['total_stories']}")
        report.append(f"- Velocity: {analytics['velocity']:.1f} points/sprint")
        report.append(f"- Avg Cycle Time: {analytics['cycle_time']:.1f} days")
        report.append("")

        # By Status
        report.append("## Status Distribution")
        for status, count in analytics['by_status'].items():
            report.append(f"- {status}: {count}")
        report.append("")

        # High Priority
        report.append("## High Priority Stories")
        for story_id in analytics['high_priority'][:5]:
            story = self.stories[story_id]
            report.append(f"- [{story.id}] {story.title} (Priority: {story.priority})")
        report.append("")

        # Blocked
        if analytics['blocked_stories']:
            report.append("## Blocked Stories")
            for story_id in analytics['blocked_stories']:
                story = self.stories[story_id]
                report.append(f"- [{story.id}] {story.title}")
            report.append("")

        return "\n".join(report)

def main():
    """CLI interface"""
    import argparse

    parser = argparse.ArgumentParser(description='Atlas Backlog Automation')
    parser.add_argument('action', choices=[
        'scan', 'prioritize', 'report', 'complete', 'create'
    ])
    parser.add_argument('--story-id', help='Story ID')
    parser.add_argument('--title', help='Story title')
    parser.add_argument('--type', choices=['feature', 'bug', 'tech_debt'])
    parser.add_argument('--estimate', type=int, default=3)

    args = parser.parse_args()

    automation = BacklogAutomation()

    if args.action == 'scan':
        automation.auto_complete_stories()
    elif args.action == 'prioritize':
        stories = automation.prioritize_backlog()
        print(f"Prioritized {len(stories)} stories")
        for story in stories[:10]:
            print(f"  {story.priority:3d} - [{story.id}] {story.title}")
    elif args.action == 'report':
        report = automation.export_markdown_report()
        print(report)
    elif args.action == 'complete' and args.story_id:
        automation.manage_story_lifecycle(args.story_id, 'complete')
    elif args.action == 'create' and args.title and args.type:
        story_type = StoryType[args.type.upper()]
        story = automation.create_story(args.title, story_type, args.estimate)
        print(f"Created {story.id}: {story.title}")

if __name__ == '__main__':
    main()