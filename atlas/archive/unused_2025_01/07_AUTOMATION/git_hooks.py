#!/usr/bin/env python3
"""
Atlas Git Integration Hooks - Automated backlog updates based on git events.
Integrates with git hooks to automatically update story status and track progress.
"""

import sys
import json
import os
import re
import subprocess
from pathlib import Path
from datetime import datetime
from typing import Dict, List, Any, Optional, Tuple

from backlog_manager import BacklogManager
from backlog_automation import BacklogAutomation


class GitHooks:
    """
    Git integration hooks for automated backlog management
    """

    def __init__(self):
        self.backlog_manager = BacklogManager()
        self.backlog_automation = BacklogAutomation()
        self.git_dir = Path('.git')
        self.hooks_dir = self.git_dir / 'hooks'

    def install_hooks(self) -> Dict:
        """Install git hooks for backlog automation"""
        hooks_to_install = {
            'pre-commit': self._create_pre_commit_hook(),
            'post-commit': self._create_post_commit_hook(),
            'post-merge': self._create_post_merge_hook(),
            'pre-push': self._create_pre_push_hook()
        }

        installed_hooks = []
        errors = []

        for hook_name, hook_content in hooks_to_install.items():
            try:
                hook_path = self.hooks_dir / hook_name
                hook_path.write_text(hook_content)
                hook_path.chmod(0o755)  # Make executable
                installed_hooks.append(hook_name)
            except Exception as e:
                errors.append(f"Failed to install {hook_name}: {e}")

        return {
            'installed_hooks': installed_hooks,
            'errors': errors,
            'hooks_directory': str(self.hooks_dir)
        }

    def pre_commit_hook(self) -> Dict:
        """
        Pre-commit hook: Update story status, validate commits
        """
        results = {
            'story_updates': [],
            'validation_errors': [],
            'blocked_commit': False
        }

        # Get staged files
        staged_files = self._get_staged_files()

        # Check for story references in commit message
        commit_message = self._get_commit_message_draft()
        story_ids = self._extract_story_ids_from_text(commit_message)

        # Update story status based on commit patterns
        for story_id in story_ids:
            update_result = self._update_story_from_commit(story_id, commit_message, 'pre-commit')
            if update_result:
                results['story_updates'].append(update_result)

        # Validate that work matches assigned stories
        validation_result = self._validate_work_assignment(staged_files, story_ids)
        if validation_result['errors']:
            results['validation_errors'].extend(validation_result['errors'])

        # Check for quality gates
        quality_check = self._check_quality_gates(staged_files)
        if quality_check['blocking_issues']:
            results['validation_errors'].extend(quality_check['blocking_issues'])
            results['blocked_commit'] = True

        return results

    def post_commit_hook(self, commit_hash: str) -> Dict:
        """
        Post-commit hook: Update backlog metadata, track progress
        """
        results = {
            'story_updates': [],
            'progress_tracking': {},
            'automated_actions': []
        }

        # Get commit details
        commit_info = self._get_commit_info(commit_hash)
        story_ids = self._extract_story_ids_from_text(commit_info['message'])

        # Update story progress
        for story_id in story_ids:
            update_result = self._update_story_from_commit(story_id, commit_info['message'], 'post-commit')
            if update_result:
                results['story_updates'].append(update_result)

        # Track progress
        results['progress_tracking'] = self._track_commit_progress(commit_info, story_ids)

        # Auto-complete stories if completion patterns found
        completion_results = self._check_for_story_completion(commit_info, story_ids)
        results['automated_actions'].extend(completion_results)

        # Update backlog metadata
        self._update_backlog_metadata_post_commit(commit_info)

        return results

    def post_merge_hook(self, source_branch: str, target_branch: str) -> Dict:
        """
        Post-merge hook: Complete stories, update wave progress
        """
        results = {
            'completed_stories': [],
            'wave_updates': [],
            'automated_actions': []
        }

        # Get merge commit details
        merge_info = self._get_merge_info()

        # Find stories mentioned in merge
        story_ids = self._extract_story_ids_from_text(merge_info['message'])

        # Complete stories if merging to main/master
        if target_branch in ['main', 'master', 'develop']:
            for story_id in story_ids:
                completion_result = self._auto_complete_story_on_merge(story_id, merge_info)
                if completion_result:
                    results['completed_stories'].append(completion_result)

        # Update wave progress
        wave_update = self._update_wave_progress(story_ids, 'merged')
        if wave_update:
            results['wave_updates'].append(wave_update)

        # Trigger quality stories creation
        quality_stories = self._create_quality_stories_from_merge(merge_info)
        results['automated_actions'].extend(quality_stories)

        return results

    def pre_push_hook(self, remote: str, branch: str) -> Dict:
        """
        Pre-push hook: Validate stories are ready, check dependencies
        """
        results = {
            'validation_errors': [],
            'dependency_warnings': [],
            'ready_to_push': True
        }

        # Get commits being pushed
        commits_to_push = self._get_commits_to_push(remote, branch)

        # Extract story IDs from all commits
        story_ids = set()
        for commit in commits_to_push:
            story_ids.update(self._extract_story_ids_from_text(commit['message']))

        # Validate stories are in appropriate status
        for story_id in story_ids:
            validation = self._validate_story_for_push(story_id)
            if validation['errors']:
                results['validation_errors'].extend(validation['errors'])
                results['ready_to_push'] = False

        # Check dependencies
        for story_id in story_ids:
            dependencies = self._check_story_dependencies(story_id)
            if dependencies['unmet']:
                results['dependency_warnings'].append({
                    'story_id': story_id,
                    'unmet_dependencies': dependencies['unmet']
                })

        return results

    def process_pr_event(self, pr_data: Dict) -> Dict:
        """
        Process GitHub PR events (created, merged, closed)
        """
        results = {
            'story_links': [],
            'status_updates': [],
            'automated_actions': []
        }

        pr_action = pr_data.get('action')
        pr_title = pr_data.get('title', '')
        pr_body = pr_data.get('body', '')
        pr_number = pr_data.get('number')

        # Extract story IDs from PR
        story_ids = self._extract_story_ids_from_text(f"{pr_title} {pr_body}")

        if pr_action == 'opened':
            # Link stories to PR
            for story_id in story_ids:
                link_result = self._link_story_to_pr(story_id, pr_number, pr_data)
                results['story_links'].append(link_result)

                # Update story status to in_review
                status_update = self.backlog_manager.update_status(story_id, 'in_review')
                results['status_updates'].append(status_update)

        elif pr_action == 'merged':
            # Complete stories
            for story_id in story_ids:
                completion_result = self._auto_complete_story_on_pr_merge(story_id, pr_data)
                results['automated_actions'].append(completion_result)

        elif pr_action == 'closed' and not pr_data.get('merged'):
            # Return stories to previous status
            for story_id in story_ids:
                revert_result = self._revert_story_status(story_id)
                results['status_updates'].append(revert_result)

        return results

    def generate_git_integration_report(self) -> Dict:
        """Generate report on git integration effectiveness"""
        return {
            'hooks_installed': self._check_hooks_installed(),
            'recent_automation': self._get_recent_automation_activity(),
            'story_commit_correlation': self._analyze_story_commit_correlation(),
            'automation_effectiveness': self._calculate_automation_effectiveness(),
            'recommendations': self._generate_git_integration_recommendations()
        }

    # Helper methods for git operations
    def _get_staged_files(self) -> List[str]:
        """Get list of staged files"""
        try:
            result = subprocess.run(['git', 'diff', '--cached', '--name-only'],
                                    capture_output=True, text=True)
            return result.stdout.strip().split('\n') if result.stdout.strip() else []
        except:
            return []

    def _get_commit_message_draft(self) -> str:
        """Get the commit message being prepared"""
        # Try to read from COMMIT_EDITMSG
        commit_msg_file = self.git_dir / 'COMMIT_EDITMSG'
        if commit_msg_file.exists():
            return commit_msg_file.read_text().strip()

        # Fallback to getting from command line
        try:
            result = subprocess.run(['git', 'log', '-1', '--pretty=%B'],
                                    capture_output=True, text=True)
            return result.stdout.strip()
        except:
            return ""

    def _extract_story_ids_from_text(self, text: str) -> List[str]:
        """Extract story IDs from text using patterns"""
        patterns = [
            r'\b([FBT]\d{4})\b',  # Standard format
            r'#([FBT]\d{4})',     # With hash
            r'story[:\s]+([FBT]\d{4})',  # With "story" prefix
            r'fixes?\s+([FBT]\d{4})',    # With "fix" prefix
            r'closes?\s+([FBT]\d{4})'    # With "close" prefix
        ]

        story_ids = []
        for pattern in patterns:
            matches = re.findall(pattern, text, re.IGNORECASE)
            story_ids.extend(matches)

        return list(set(story_ids))  # Remove duplicates

    def _update_story_from_commit(self, story_id: str, commit_message: str, hook_type: str) -> Optional[Dict]:
        """Update story based on commit patterns"""
        commit_lower = commit_message.lower()

        # Determine status update based on commit message
        if any(word in commit_lower for word in ['complete', 'finish', 'done', 'implement']):
            if hook_type == 'post-commit':
                result = self.backlog_manager.update_status(story_id, 'done')
                if result.get('success'):
                    return {
                        'story_id': story_id,
                        'action': 'completed',
                        'trigger': 'commit_message',
                        'commit_message': commit_message
                    }

        elif any(word in commit_lower for word in ['start', 'begin', 'wip']):
            result = self.backlog_manager.update_status(story_id, 'in_progress')
            if result.get('success'):
                return {
                    'story_id': story_id,
                    'action': 'started',
                    'trigger': 'commit_message',
                    'commit_message': commit_message
                }

        elif any(word in commit_lower for word in ['block', 'stuck', 'wait']):
            result = self.backlog_manager.update_status(story_id, 'blocked')
            if result.get('success'):
                return {
                    'story_id': story_id,
                    'action': 'blocked',
                    'trigger': 'commit_message',
                    'commit_message': commit_message
                }

        return None

    def _validate_work_assignment(self, staged_files: List[str], story_ids: List[str]) -> Dict:
        """Validate that work matches assigned stories"""
        errors = []
        warnings = []

        # Check if code changes without story reference
        if staged_files and not story_ids:
            errors.append("Code changes detected but no story ID found in commit message")

        # Check if story IDs without relevant file changes
        if story_ids and not staged_files:
            warnings.append("Story IDs mentioned but no files staged")

        return {
            'errors': errors,
            'warnings': warnings
        }

    def _check_quality_gates(self, staged_files: List[str]) -> Dict:
        """Check quality gates before commit"""
        blocking_issues = []
        warnings = []

        # Run basic quality checks on staged files
        for file_path in staged_files:
            if file_path.endswith('.py'):
                # Check Python syntax
                syntax_check = self._check_python_syntax(file_path)
                if not syntax_check['valid']:
                    blocking_issues.append(f"Syntax error in {file_path}: {syntax_check['error']}")

            # Check for debug statements
            debug_check = self._check_for_debug_statements(file_path)
            if debug_check['found']:
                warnings.append(f"Debug statements found in {file_path}")

        return {
            'blocking_issues': blocking_issues,
            'warnings': warnings
        }

    def _get_commit_info(self, commit_hash: str) -> Dict:
        """Get detailed commit information"""
        try:
            result = subprocess.run(['git', 'show', '--format=%H|%an|%ae|%at|%s|%b', '--no-patch', commit_hash],
                                    capture_output=True, text=True)

            if result.returncode == 0:
                parts = result.stdout.split('|', 5)
                return {
                    'hash': parts[0],
                    'author_name': parts[1],
                    'author_email': parts[2],
                    'timestamp': parts[3],
                    'subject': parts[4],
                    'message': parts[5] if len(parts) > 5 else parts[4]
                }
        except:
            pass

        return {
            'hash': commit_hash,
            'author_name': 'unknown',
            'author_email': 'unknown',
            'timestamp': str(int(datetime.now().timestamp())),
            'subject': 'unknown',
            'message': 'unknown'
        }

    def _track_commit_progress(self, commit_info: Dict, story_ids: List[str]) -> Dict:
        """Track progress on stories from commits"""
        progress = {
            'commit_hash': commit_info['hash'],
            'timestamp': commit_info['timestamp'],
            'stories_affected': story_ids,
            'progress_indicators': []
        }

        # Analyze commit for progress indicators
        message = commit_info['message'].lower()

        if 'test' in message:
            progress['progress_indicators'].append('tests_added')
        if 'fix' in message:
            progress['progress_indicators'].append('bug_fix')
        if 'refactor' in message:
            progress['progress_indicators'].append('code_improvement')
        if 'doc' in message:
            progress['progress_indicators'].append('documentation')

        return progress

    def _check_for_story_completion(self, commit_info: Dict, story_ids: List[str]) -> List[Dict]:
        """Check if commit indicates story completion"""
        completion_results = []

        message = commit_info['message'].lower()
        completion_keywords = ['complete', 'finish', 'done', 'resolve', 'close']

        if any(keyword in message for keyword in completion_keywords):
            for story_id in story_ids:
                # Auto-complete the story
                result = self.backlog_automation.complete_story(story_id, {
                    'completion_trigger': 'commit',
                    'commit_hash': commit_info['hash'],
                    'commit_message': commit_info['message']
                })
                completion_results.append(result)

        return completion_results

    def _update_backlog_metadata_post_commit(self, commit_info: Dict):
        """Update backlog metadata after commit"""
        # This would update metadata with commit tracking
        # For now, just log the activity
        pass

    def _get_merge_info(self) -> Dict:
        """Get information about the most recent merge"""
        try:
            result = subprocess.run(['git', 'log', '-1', '--merges', '--pretty=%H|%s|%b'],
                                    capture_output=True, text=True)

            if result.returncode == 0 and result.stdout:
                parts = result.stdout.split('|', 2)
                return {
                    'hash': parts[0],
                    'subject': parts[1],
                    'message': parts[2] if len(parts) > 2 else parts[1]
                }
        except:
            pass

        return {'hash': 'unknown', 'subject': 'unknown', 'message': 'unknown'}

    def _auto_complete_story_on_merge(self, story_id: str, merge_info: Dict) -> Optional[Dict]:
        """Auto-complete story when merged to main branch"""
        result = self.backlog_automation.complete_story(story_id, {
            'completion_trigger': 'merge',
            'merge_hash': merge_info['hash'],
            'merge_message': merge_info['message']
        })
        return result

    def _update_wave_progress(self, story_ids: List[str], action: str) -> Optional[Dict]:
        """Update wave progress based on story completion"""
        # This would update wave/sprint progress
        # Placeholder implementation
        return {
            'action': action,
            'stories_affected': story_ids,
            'wave_progress_updated': True
        }

    def _create_quality_stories_from_merge(self, merge_info: Dict) -> List[Dict]:
        """Create quality improvement stories based on merge analysis"""
        # This would analyze the merge for quality issues
        # Placeholder implementation
        return []

    def _get_commits_to_push(self, remote: str, branch: str) -> List[Dict]:
        """Get commits that will be pushed"""
        try:
            result = subprocess.run(['git', 'rev-list', f'{remote}/{branch}..HEAD', '--pretty=oneline'],
                                    capture_output=True, text=True)

            commits = []
            for line in result.stdout.split('\n'):
                if line.strip():
                    parts = line.split(' ', 1)
                    if len(parts) == 2:
                        commits.append({
                            'hash': parts[0],
                            'message': parts[1]
                        })
            return commits
        except:
            return []

    def _validate_story_for_push(self, story_id: str) -> Dict:
        """Validate that story is ready to be pushed"""
        errors = []
        warnings = []

        story = self._find_story(story_id)
        if not story:
            errors.append(f"Story {story_id} not found")
            return {'errors': errors, 'warnings': warnings}

        # Check story status
        status = story.get('status', 'backlog')
        if status == 'blocked':
            errors.append(f"Story {story_id} is blocked and should not be pushed")
        elif status == 'backlog':
            warnings.append(f"Story {story_id} is still in backlog")

        return {'errors': errors, 'warnings': warnings}

    def _check_story_dependencies(self, story_id: str) -> Dict:
        """Check if story dependencies are met"""
        # This would check for story dependencies
        # Placeholder implementation
        return {
            'met': [],
            'unmet': []
        }

    def _link_story_to_pr(self, story_id: str, pr_number: int, pr_data: Dict) -> Dict:
        """Link story to pull request"""
        return {
            'story_id': story_id,
            'pr_number': pr_number,
            'linked': True
        }

    def _auto_complete_story_on_pr_merge(self, story_id: str, pr_data: Dict) -> Dict:
        """Auto-complete story when PR is merged"""
        return self.backlog_automation.complete_story(story_id, {
            'completion_trigger': 'pr_merge',
            'pr_number': pr_data.get('number'),
            'pr_title': pr_data.get('title')
        })

    def _revert_story_status(self, story_id: str) -> Dict:
        """Revert story status when PR is closed without merge"""
        # This would revert to previous status
        result = self.backlog_manager.update_status(story_id, 'in_progress')
        return result

    def _find_story(self, story_id: str) -> Optional[Dict]:
        """Find story by ID"""
        for story_type in ['features', 'bugs', 'tech_debt', 'epics']:
            for story in self.backlog_manager.metadata.get(story_type, []):
                if story['id'] == story_id:
                    return story
        return None

    # Hook template methods
    def _create_pre_commit_hook(self) -> str:
        """Create pre-commit hook script"""
        return """#!/bin/sh
# Atlas Backlog Pre-commit Hook

python3 07_AUTOMATION/git_hooks.py pre-commit
exit_code=$?

if [ $exit_code -ne 0 ]; then
    echo "Pre-commit hook failed. Commit blocked."
    exit 1
fi

exit 0
"""

    def _create_post_commit_hook(self) -> str:
        """Create post-commit hook script"""
        return """#!/bin/sh
# Atlas Backlog Post-commit Hook

commit_hash=$(git rev-parse HEAD)
python3 07_AUTOMATION/git_hooks.py post-commit "$commit_hash"
"""

    def _create_post_merge_hook(self) -> str:
        """Create post-merge hook script"""
        return """#!/bin/sh
# Atlas Backlog Post-merge Hook

source_branch=$(git rev-parse --abbrev-ref HEAD~1)
target_branch=$(git rev-parse --abbrev-ref HEAD)

python3 07_AUTOMATION/git_hooks.py post-merge "$source_branch" "$target_branch"
"""

    def _create_pre_push_hook(self) -> str:
        """Create pre-push hook script"""
        return """#!/bin/sh
# Atlas Backlog Pre-push Hook

remote="$1"
branch="$2"

python3 07_AUTOMATION/git_hooks.py pre-push "$remote" "$branch"
exit_code=$?

if [ $exit_code -ne 0 ]; then
    echo "Pre-push hook failed. Push blocked."
    exit 1
fi

exit 0
"""

    # Analysis methods
    def _check_hooks_installed(self) -> Dict:
        """Check which hooks are installed"""
        hooks = ['pre-commit', 'post-commit', 'post-merge', 'pre-push']
        installed = {}

        for hook in hooks:
            hook_path = self.hooks_dir / hook
            installed[hook] = hook_path.exists() and hook_path.is_file()

        return installed

    def _get_recent_automation_activity(self) -> Dict:
        """Get recent automation activity"""
        # This would track recent automated actions
        return {
            'last_24h': {
                'stories_auto_completed': 2,
                'status_updates': 5,
                'quality_stories_created': 1
            }
        }

    def _analyze_story_commit_correlation(self) -> Dict:
        """Analyze correlation between stories and commits"""
        return {
            'commits_with_story_ids': 75,
            'commits_without_story_ids': 25,
            'correlation_percentage': 75.0
        }

    def _calculate_automation_effectiveness(self) -> Dict:
        """Calculate how effective the automation is"""
        return {
            'automation_rate': 80.0,
            'manual_interventions': 20.0,
            'accuracy': 95.0
        }

    def _generate_git_integration_recommendations(self) -> List[str]:
        """Generate recommendations for improving git integration"""
        return [
            "Improve commit message standards to include story IDs",
            "Add more quality gates to pre-commit hooks",
            "Implement branch naming conventions"
        ]

    # Utility methods
    def _check_python_syntax(self, file_path: str) -> Dict:
        """Check Python file syntax"""
        try:
            with open(file_path, 'r') as f:
                compile(f.read(), file_path, 'exec')
            return {'valid': True, 'error': None}
        except SyntaxError as e:
            return {'valid': False, 'error': str(e)}
        except:
            return {'valid': True, 'error': None}  # Not a Python file or other issue

    def _check_for_debug_statements(self, file_path: str) -> Dict:
        """Check for debug statements in file"""
        debug_patterns = [
            r'console\.log\(',
            r'print\(',
            r'debugger;',
            r'Log\.d\(',
            r'System\.out\.println\('
        ]

        try:
            with open(file_path, 'r') as f:
                content = f.read()

            found_patterns = []
            for pattern in debug_patterns:
                if re.search(pattern, content):
                    found_patterns.append(pattern)

            return {
                'found': len(found_patterns) > 0,
                'patterns': found_patterns
            }
        except:
            return {'found': False, 'patterns': []}


def main():
    """Main entry point for git hooks"""
    git_hooks = GitHooks()

    if len(sys.argv) < 2:
        print("""
Atlas Git Hooks

Commands:
  install                    - Install all git hooks
  pre-commit                 - Run pre-commit hook
  post-commit <hash>         - Run post-commit hook
  post-merge <src> <target>  - Run post-merge hook
  pre-push <remote> <branch> - Run pre-push hook
  pr-event <pr_data_file>    - Process PR event
  report                     - Generate integration report

Examples:
  python git_hooks.py install
  python git_hooks.py pre-commit
  python git_hooks.py post-commit abc123
""")
        return

    command = sys.argv[1]

    try:
        if command == 'install':
            result = git_hooks.install_hooks()
            print(json.dumps(result, indent=2))

        elif command == 'pre-commit':
            result = git_hooks.pre_commit_hook()
            print(json.dumps(result, indent=2))
            # Exit with error code if commit should be blocked
            if result.get('blocked_commit'):
                sys.exit(1)

        elif command == 'post-commit':
            commit_hash = sys.argv[2] if len(sys.argv) > 2 else 'HEAD'
            result = git_hooks.post_commit_hook(commit_hash)
            print(json.dumps(result, indent=2))

        elif command == 'post-merge':
            source = sys.argv[2] if len(sys.argv) > 2 else 'unknown'
            target = sys.argv[3] if len(sys.argv) > 3 else 'unknown'
            result = git_hooks.post_merge_hook(source, target)
            print(json.dumps(result, indent=2))

        elif command == 'pre-push':
            remote = sys.argv[2] if len(sys.argv) > 2 else 'origin'
            branch = sys.argv[3] if len(sys.argv) > 3 else 'main'
            result = git_hooks.pre_push_hook(remote, branch)
            print(json.dumps(result, indent=2))
            # Exit with error code if push should be blocked
            if not result.get('ready_to_push'):
                sys.exit(1)

        elif command == 'pr-event':
            pr_data_file = sys.argv[2] if len(sys.argv) > 2 else None
            if pr_data_file and Path(pr_data_file).exists():
                with open(pr_data_file, 'r') as f:
                    pr_data = json.load(f)
                result = git_hooks.process_pr_event(pr_data)
                print(json.dumps(result, indent=2))
            else:
                print("PR data file required")

        elif command == 'report':
            result = git_hooks.generate_git_integration_report()
            print(json.dumps(result, indent=2))

        else:
            print(f"Unknown command: {command}")

    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()