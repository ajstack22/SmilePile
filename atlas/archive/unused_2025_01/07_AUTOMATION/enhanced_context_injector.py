#!/usr/bin/env python3
"""
Enhanced Atlas Context Injector - Manifest-driven context assembly for LLM tasks
Programmatically builds and injects complete context packages based on task profiles
"""

import json
import os
import sys
import argparse
import hashlib
import time
import yaml
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any
from datetime import datetime
from concurrent.futures import ThreadPoolExecutor, as_completed
import re

class EnhancedContextInjector:
    """
    Manifest-driven context injection system for deterministic LLM context delivery
    """

    def __init__(self, manifest_path: Optional[str] = None):
        self.atlas_dir = Path(__file__).parent.parent
        if manifest_path:
            self.manifest_path = Path(manifest_path)
        else:
            self.manifest_path = self.atlas_dir / 'config' / 'context_manifest.json'
        self.cache_dir = self.atlas_dir / '.atlas' / 'context_cache'
        self.cache_dir.mkdir(parents=True, exist_ok=True)

        # Load checklists configuration
        self.checklists_path = self.atlas_dir / '06_CHECKLISTS' / 'CONTEXT_CHECKLISTS.yaml'
        self.checklists = self._load_checklists()

        self.manifest = self._load_manifest()
        self.file_cache = {}
        self.token_cache = {}

    def _load_checklists(self) -> Dict:
        """Load verification checklists configuration"""
        if not self.checklists_path.exists():
            return {}

        try:
            with open(self.checklists_path) as f:
                checklists_data = yaml.safe_load(f)
                return checklists_data.get('checklists', {})
        except Exception as e:
            print(f"Warning: Could not load checklists: {e}")
            return {}

    def _format_checklist(self, profile_name: str) -> str:
        """Format checklist for a given profile as markdown"""
        if not self.checklists or profile_name not in self.checklists:
            return ""

        checklist = self.checklists[profile_name]
        output = []
        output.append("## ✅ Verification Checklist")
        output.append(f"**{checklist.get('name', profile_name)}**")

        if 'description' in checklist:
            output.append(f"_{checklist['description']}_")

        output.append("")

        # Format checklist items
        items = checklist.get('items', [])
        required_count = sum(1 for item in items if item.get('required', False))

        output.append("### Required Checks:")
        for item in items:
            if item.get('required', False):
                output.append(f"- [ ] {item['check']}")

        if any(not item.get('required', False) for item in items):
            output.append("\n### Optional Checks:")
            for item in items:
                if not item.get('required', False):
                    output.append(f"- [ ] {item['check']}")

        output.append(f"\n_Complete {required_count} required checks before proceeding_")

        return "\n".join(output)

    def _load_manifest(self) -> Dict:
        """Load and validate the context manifest"""
        if not self.manifest_path.exists():
            raise FileNotFoundError(f"Context manifest not found: {self.manifest_path}")

        with open(self.manifest_path) as f:
            manifest = json.load(f)

        # Validate manifest structure
        self._validate_manifest(manifest)
        return manifest

    def _validate_manifest(self, manifest: Dict):
        """Validate manifest structure and file paths"""
        required_keys = ['version', 'context_profiles', 'dependency_definitions']
        for key in required_keys:
            if key not in manifest:
                raise ValueError(f"Manifest missing required key: {key}")

        # Validate file paths if enabled
        if manifest.get('global_settings', {}).get('validation', {}).get('check_file_exists', True):
            invalid_files = []
            for profile_name, profile in manifest['context_profiles'].items():
                for category, files in profile.get('files', {}).items():
                    for file_info in files:
                        file_path = self.atlas_dir / file_info['path']
                        if not file_path.exists():
                            invalid_files.append(f"{profile_name}/{file_info['path']}")

            if invalid_files:
                print(f"Warning: {len(invalid_files)} files not found in manifest")
                for f in invalid_files[:5]:
                    print(f"  - {f}")

    def build_context(
        self,
        task: str,
        feature: Optional[str] = None,
        additional_files: Optional[List[str]] = None,
        verbose: bool = False
    ) -> Dict:
        """
        Build a complete context package for a task

        Args:
            task: Task profile name from manifest
            feature: Optional feature modifier
            additional_files: Extra files to include
            verbose: Enable detailed logging

        Returns:
            Dictionary with assembled context and metadata
        """
        if task not in self.manifest['context_profiles']:
            available = ', '.join(self.manifest['context_profiles'].keys())
            raise ValueError(f"Unknown task profile: {task}. Available: {available}")

        profile = self.manifest['context_profiles'][task]
        size_budget = profile.get('size_budget', 40000)

        # Check cache
        cache_key = self._get_cache_key(task, feature, additional_files)
        cached = self._get_cached_context(cache_key)
        if cached and not verbose:
            return cached

        # Collect files to include
        files_to_load = self._collect_files(profile, feature, additional_files)

        # Load files with budget management
        context_parts = []
        metadata = {
            'task': task,
            'feature': feature,
            'timestamp': datetime.now().isoformat(),
            'files_included': [],
            'files_excluded': [],
            'dependencies_resolved': [],
            'total_tokens': 0,
            'cache_key': cache_key
        }

        current_size = 0

        # Load files in priority order
        for file_info in sorted(files_to_load, key=lambda x: x.get('priority', 99)):
            file_path = self.atlas_dir / file_info['path']

            if not file_path.exists():
                metadata['files_excluded'].append({
                    'path': file_info['path'],
                    'reason': 'file_not_found'
                })
                continue

            file_content, file_size = self._load_file_with_size(file_path, file_info)

            # Check budget
            if current_size + file_size > size_budget:
                # Try to generate summary if enabled
                if self.manifest.get('global_settings', {}).get('summary_generation', {}).get('enabled'):
                    summary = self._generate_summary(file_content, 500)
                    if summary:
                        context_parts.append(f"\n## Summary of {file_info['path']}\n{summary}\n")
                        metadata['files_excluded'].append({
                            'path': file_info['path'],
                            'reason': 'size_budget_exceeded',
                            'summary_included': True
                        })
                else:
                    metadata['files_excluded'].append({
                        'path': file_info['path'],
                        'reason': 'size_budget_exceeded'
                    })
                continue

            # Add to context
            context_parts.append(f"\n## File: {file_info['path']}\n{file_content}\n")
            current_size += file_size
            metadata['files_included'].append({
                'path': file_info['path'],
                'size': file_size,
                'priority': file_info.get('priority', 99)
            })

        # Resolve and add dependencies
        dependencies = profile.get('dependencies', [])
        if feature and 'feature_modifiers' in profile:
            feature_mod = profile['feature_modifiers'].get(feature, {})
            dependencies.extend(feature_mod.get('dependencies', []))

        for dep_name in dependencies:
            dep_content, dep_files = self._resolve_dependency(dep_name, size_budget - current_size)
            if dep_content:
                context_parts.append(f"\n## Dependency: {dep_name}\n{dep_content}\n")
                metadata['dependencies_resolved'].append({
                    'name': dep_name,
                    'files': dep_files
                })
                current_size += len(dep_content)

        # Add checklist if available for this profile
        checklist_content = self._format_checklist(task)
        if checklist_content:
            context_parts.append(f"\n\n{checklist_content}")
            metadata['checklist_included'] = True
        else:
            metadata['checklist_included'] = False

        # Assemble final context
        context = ''.join(context_parts)
        metadata['total_tokens'] = self._estimate_tokens(context)
        metadata['total_size'] = len(context)

        # Cache the result
        self._cache_context(cache_key, context, metadata)

        if verbose:
            self._print_build_report(metadata)

        return {
            'context': context,
            'metadata': metadata
        }

    def _collect_files(
        self,
        profile: Dict,
        feature: Optional[str],
        additional_files: Optional[List[str]]
    ) -> List[Dict]:
        """Collect all files to be loaded for a profile"""
        files_to_load = []

        # Add profile files
        for category, files in profile.get('files', {}).items():
            for file_info in files:
                if isinstance(file_info, str):
                    files_to_load.append({'path': file_info, 'priority': 99})
                else:
                    files_to_load.append(file_info)

        # Add feature-specific files
        if feature and 'feature_modifiers' in profile:
            feature_mod = profile['feature_modifiers'].get(feature, {})
            for file_path in feature_mod.get('additional_files', []):
                files_to_load.append({'path': file_path, 'priority': 50})

        # Add additional files
        if additional_files:
            for file_path in additional_files:
                files_to_load.append({'path': file_path, 'priority': 75})

        # Process include patterns
        if 'includes_patterns' in profile:
            for pattern in profile['includes_patterns']:
                matching_files = self._find_files_by_pattern(pattern)
                for file_path in matching_files:
                    files_to_load.append({'path': str(file_path), 'priority': 80})

        # Remove excluded patterns
        if 'excludes_patterns' in profile:
            exclude_patterns = profile['excludes_patterns']
            files_to_load = [
                f for f in files_to_load
                if not any(self._matches_pattern(f['path'], p) for p in exclude_patterns)
            ]

        return files_to_load

    def _load_file_with_size(self, file_path: Path, file_info: Dict) -> Tuple[str, int]:
        """Load file content and calculate size"""
        # Check cache
        cache_key = str(file_path)
        if cache_key in self.file_cache:
            content = self.file_cache[cache_key]
        else:
            with open(file_path) as f:
                content = f.read()
            self.file_cache[cache_key] = content

        # Handle section filtering
        if 'sections' in file_info:
            content = self._extract_sections(content, file_info['sections'])

        # Handle summary only
        if file_info.get('summary_only'):
            content = self._generate_summary(content, 1000)

        return content, len(content)

    def _resolve_dependency(self, dep_name: str, remaining_budget: int) -> Tuple[str, List[str]]:
        """Resolve a dependency and return its content"""
        if dep_name not in self.manifest['dependency_definitions']:
            return "", []

        dep_def = self.manifest['dependency_definitions'][dep_name]
        dep_parts = []
        dep_files = []
        current_size = 0

        for file_path in dep_def.get('files', []):
            if '#' in file_path:
                # Handle section references
                file_path, section = file_path.split('#', 1)

            full_path = self.atlas_dir / file_path
            if full_path.exists() and current_size < remaining_budget:
                with open(full_path) as f:
                    content = f.read()

                if current_size + len(content) < remaining_budget:
                    dep_parts.append(content)
                    dep_files.append(file_path)
                    current_size += len(content)

        return '\n'.join(dep_parts), dep_files

    def _find_files_by_pattern(self, pattern: str) -> List[Path]:
        """Find files matching a glob pattern"""
        matches = []
        base_path = self.atlas_dir

        # Convert ** patterns for pathlib
        if '**' in pattern:
            matches.extend(base_path.rglob(pattern.replace('**/', '')))
        else:
            matches.extend(base_path.glob(pattern))

        return [m.relative_to(base_path) for m in matches if m.is_file()]

    def _matches_pattern(self, file_path: str, pattern: str) -> bool:
        """Check if file path matches a pattern"""
        import fnmatch
        return fnmatch.fnmatch(file_path, pattern)

    def _extract_sections(self, content: str, sections: List[str]) -> str:
        """Extract specific sections from markdown content"""
        extracted = []
        lines = content.split('\n')
        current_section = None
        section_content = []

        for line in lines:
            # Check for section headers
            if line.startswith('#'):
                if current_section:
                    # Save previous section if it was requested
                    section_name = current_section.lower().replace(' ', '-')
                    if any(s.lower() in section_name for s in sections):
                        extracted.append('\n'.join(section_content))

                current_section = line.strip('#').strip()
                section_content = [line]
            elif current_section:
                section_content.append(line)

        # Handle last section
        if current_section:
            section_name = current_section.lower().replace(' ', '-')
            if any(s.lower() in section_name for s in sections):
                extracted.append('\n'.join(section_content))

        return '\n\n'.join(extracted) if extracted else content[:2000]  # Fallback to first 2000 chars

    def _generate_summary(self, content: str, max_length: int) -> str:
        """Generate a summary of content"""
        # Simple summarization: extract key sections and first paragraphs
        lines = content.split('\n')
        summary_lines = []
        current_length = 0

        # Priority: headers, first paragraphs, lists
        for line in lines:
            if current_length >= max_length:
                break

            # Include headers
            if line.startswith('#'):
                summary_lines.append(line)
                current_length += len(line)
            # Include list items (limited)
            elif line.strip().startswith(('- ', '* ', '1. ')):
                if len(summary_lines) < 20:  # Limit list items
                    summary_lines.append(line)
                    current_length += len(line)
            # Include first paragraph after header
            elif line.strip() and len(summary_lines) > 0 and summary_lines[-1].startswith('#'):
                summary_lines.append(line[:200])  # Truncate long paragraphs
                current_length += len(line)

        return '\n'.join(summary_lines)

    def _estimate_tokens(self, text: str) -> int:
        """Estimate token count for text"""
        # Use simple estimation: ~4 characters per token
        multiplier = self.manifest.get('global_settings', {}).get('token_estimation_multiplier', 0.25)
        return int(len(text) * multiplier)

    def _get_cache_key(self, task: str, feature: Optional[str], additional_files: Optional[List[str]]) -> str:
        """Generate cache key for context configuration"""
        key_parts = [task]
        if feature:
            key_parts.append(feature)
        if additional_files:
            key_parts.extend(sorted(additional_files))

        key_string = '|'.join(key_parts)
        return hashlib.md5(key_string.encode()).hexdigest()

    def _get_cached_context(self, cache_key: str) -> Optional[Dict]:
        """Retrieve cached context if available and valid"""
        if not self.manifest.get('global_settings', {}).get('cache_enabled', True):
            return None

        cache_file = self.cache_dir / f"{cache_key}.json"
        if not cache_file.exists():
            return None

        with open(cache_file) as f:
            cached = json.load(f)

        # Check TTL
        ttl = self.manifest.get('global_settings', {}).get('cache_ttl_seconds', 900)
        cached_time = datetime.fromisoformat(cached['metadata']['timestamp'])
        age = (datetime.now() - cached_time).total_seconds()

        if age > ttl:
            return None

        return cached

    def _cache_context(self, cache_key: str, context: str, metadata: Dict):
        """Cache context for reuse"""
        if not self.manifest.get('global_settings', {}).get('cache_enabled', True):
            return

        cache_file = self.cache_dir / f"{cache_key}.json"
        with open(cache_file, 'w') as f:
            json.dump({'context': context, 'metadata': metadata}, f)

    def _print_build_report(self, metadata: Dict):
        """Print detailed report of context building"""
        print("\n" + "="*60)
        print("Context Build Report")
        print("="*60)
        print(f"Task: {metadata['task']}")
        print(f"Feature: {metadata.get('feature', 'None')}")
        print(f"Timestamp: {metadata['timestamp']}")
        print(f"\nFiles Included ({len(metadata['files_included'])}):")
        for f in metadata['files_included']:
            print(f"  ✓ {f['path']} (priority: {f['priority']}, size: {f['size']})")

        if metadata['files_excluded']:
            print(f"\nFiles Excluded ({len(metadata['files_excluded'])}):")
            for f in metadata['files_excluded']:
                print(f"  ✗ {f['path']} (reason: {f['reason']})")

        if metadata['dependencies_resolved']:
            print(f"\nDependencies Resolved ({len(metadata['dependencies_resolved'])}):")
            for d in metadata['dependencies_resolved']:
                print(f"  • {d['name']} ({len(d['files'])} files)")

        print(f"\nTotal Size: {metadata['total_size']} bytes")
        print(f"Estimated Tokens: {metadata['total_tokens']}")
        print("="*60 + "\n")

    def validate_manifest(self) -> Dict:
        """Validate the entire manifest for issues"""
        issues = {
            'errors': [],
            'warnings': [],
            'info': []
        }

        # Check for circular dependencies
        if self.manifest.get('global_settings', {}).get('validation', {}).get('check_circular_deps', True):
            circular = self._check_circular_dependencies()
            if circular:
                issues['errors'].extend([f"Circular dependency: {c}" for c in circular])

        # Check file existence
        missing_files = []
        for profile_name, profile in self.manifest['context_profiles'].items():
            for category, files in profile.get('files', {}).items():
                for file_info in files:
                    path = file_info['path'] if isinstance(file_info, dict) else file_info
                    full_path = self.atlas_dir / path
                    if not full_path.exists():
                        if isinstance(file_info, dict) and file_info.get('required'):
                            issues['errors'].append(f"Required file missing: {path} in {profile_name}")
                        else:
                            issues['warnings'].append(f"File not found: {path} in {profile_name}")

        # Check size budgets
        for profile_name, profile in self.manifest['context_profiles'].items():
            estimated_size = self._estimate_profile_size(profile)
            budget = profile.get('size_budget', 40000)
            if estimated_size > budget:
                issues['warnings'].append(
                    f"Profile {profile_name} may exceed budget: {estimated_size} > {budget}"
                )

        # Info about profiles
        issues['info'].append(f"Total profiles: {len(self.manifest['context_profiles'])}")
        issues['info'].append(f"Total dependencies: {len(self.manifest['dependency_definitions'])}")

        return issues

    def _check_circular_dependencies(self) -> List[str]:
        """Check for circular dependencies in manifest"""
        circular = []

        def check_dep(dep_name: str, chain: List[str]) -> bool:
            if dep_name in chain:
                circular.append(' -> '.join(chain + [dep_name]))
                return True

            if dep_name not in self.manifest['dependency_definitions']:
                return False

            # Check nested dependencies (if implemented)
            return False

        for profile_name, profile in self.manifest['context_profiles'].items():
            for dep in profile.get('dependencies', []):
                check_dep(dep, [profile_name])

        return circular

    def _estimate_profile_size(self, profile: Dict) -> int:
        """Estimate total size of a profile"""
        total_size = 0

        for category, files in profile.get('files', {}).items():
            for file_info in files:
                path = file_info['path'] if isinstance(file_info, dict) else file_info
                if path in self.manifest.get('file_registry', {}):
                    size = self.manifest['file_registry'][path].get('size', 1000)
                else:
                    # Estimate based on file existence
                    full_path = self.atlas_dir / path
                    if full_path.exists():
                        size = full_path.stat().st_size
                    else:
                        size = 1000  # Default estimate
                total_size += size

        return total_size

    def auto_inject_for_agent(self, agent_type: str, task_description: str,
                              parent_context: Optional[str] = None) -> Dict:
        """
        Automatically determine and inject context for an agent
        Called by Task tool integration before spawning

        Args:
            agent_type: Type of agent being spawned
            task_description: Description of the task
            parent_context: Optional context from parent agent

        Returns:
            Dictionary with context and metadata
        """
        # Map agent type to profile
        profile = self._map_agent_type_to_profile(agent_type)

        # Override with keyword detection if needed
        detected_profile = self._detect_profile_from_description(task_description)
        if detected_profile and agent_type == 'general-purpose':
            profile = detected_profile

        # Extract feature from description
        feature = self._extract_feature_from_description(task_description)

        # Detect if additional files needed
        additional_files = self._detect_relevant_files(task_description)

        # Build context
        result = self.build_context(
            task=profile,
            feature=feature,
            additional_files=additional_files,
            verbose=False
        )

        # Merge with parent context if provided
        if parent_context:
            result['context'] = self._merge_with_parent_context(parent_context, result['context'])
            result['metadata']['has_parent_context'] = True

        result['metadata']['agent_type'] = agent_type
        result['metadata']['detected_profile'] = profile
        result['metadata']['detected_feature'] = feature

        return result

    def _map_agent_type_to_profile(self, agent_type: str) -> str:
        """Map agent type to context profile"""
        mapping = {
            'general-purpose': 'orchestration',
            'ui-developer': 'ui_development',
            'backend-developer': 'backend_development',
            'story-writer': 'story_creation',
            'bug-fixer': 'troubleshooting',
            'researcher': 'research',
            'reviewer': 'code_review',
            'tester': 'testing',
            'test-specialist': 'testing',
            'documenter': 'documentation',
            'deployer': 'deployment'
        }
        return mapping.get(agent_type, 'orchestration')

    def _detect_profile_from_description(self, description: str) -> Optional[str]:
        """Detect appropriate profile from task description"""
        desc_lower = description.lower()

        # Keyword to profile mapping
        detections = [
            (['bug', 'fix', 'crash', 'error', 'debug'], 'troubleshooting'),
            (['story', 'requirement', 'epic', 'acceptance'], 'story_creation'),
            (['ui', 'frontend', 'component', 'react', 'vue'], 'ui_development'),
            (['api', 'backend', 'endpoint', 'database'], 'backend_development'),
            (['test', 'spec', 'coverage', 'assertion'], 'testing'),
            (['review', 'quality', 'refactor'], 'code_review'),
            (['research', 'investigate', 'analyze'], 'research'),
            (['deploy', 'release', 'production'], 'deployment')
        ]

        for keywords, profile in detections:
            if any(kw in desc_lower for kw in keywords):
                return profile

        return None

    def _extract_feature_from_description(self, description: str) -> Optional[str]:
        """Extract feature modifier from task description"""
        desc_lower = description.lower()

        feature_keywords = {
            'authentication': ['auth', 'login', 'oauth', 'jwt', 'password'],
            'performance': ['performance', 'optimize', 'slow', 'speed', 'latency'],
            'database': ['database', 'sql', 'query', 'migration', 'schema']
        }

        for feature, keywords in feature_keywords.items():
            if any(kw in desc_lower for kw in keywords):
                return feature

        return None

    def _detect_relevant_files(self, description: str) -> List[str]:
        """Detect files mentioned in the task description"""
        import re

        files = []
        # Look for file paths
        patterns = [
            r'[\'"`]([^\'"`]+\.(py|js|ts|md|yaml|json))[\'"`]',
            r'(?:file|path):\s*([^\s]+\.\w+)'
        ]

        for pattern in patterns:
            matches = re.findall(pattern, description)
            for match in matches:
                file_path = match[0] if isinstance(match, tuple) else match
                files.append(file_path)

        return files

    def _merge_with_parent_context(self, parent_context: str, child_context: str) -> str:
        """Merge parent and child context avoiding duplication"""
        # Simple line-based deduplication
        parent_lines = set(parent_context.split('\n'))
        child_lines = child_context.split('\n')

        merged = []
        merged.append("## Inherited Context")
        merged.append(parent_context)
        merged.append("\n## Task-Specific Context")

        for line in child_lines:
            if line not in parent_lines:
                merged.append(line)

        return '\n'.join(merged)

    def list_profiles(self) -> List[Dict]:
        """List all available context profiles"""
        profiles = []
        for name, profile in self.manifest['context_profiles'].items():
            profiles.append({
                'name': name,
                'description': profile.get('description', 'No description'),
                'priority': profile.get('priority', 99),
                'size_budget': profile.get('size_budget', 40000),
                'dependencies': len(profile.get('dependencies', [])),
                'files': sum(len(files) for files in profile.get('files', {}).values())
            })

        return sorted(profiles, key=lambda x: x['priority'])

    def export_context(self, task: str, output_file: str, feature: Optional[str] = None):
        """Export context to a file for manual review"""
        result = self.build_context(task, feature, verbose=True)

        with open(output_file, 'w') as f:
            f.write("# Atlas Context Export\n")
            f.write(f"# Task: {task}\n")
            f.write(f"# Feature: {feature or 'None'}\n")
            f.write(f"# Generated: {datetime.now().isoformat()}\n")
            f.write(f"# Total Tokens: {result['metadata']['total_tokens']}\n")
            f.write("\n" + "="*80 + "\n\n")
            f.write(result['context'])

        print(f"Context exported to: {output_file}")
        return output_file


def main():
    """CLI interface for enhanced context injector"""
    parser = argparse.ArgumentParser(description='Enhanced Atlas Context Injector')
    subparsers = parser.add_subparsers(dest='command', help='Available commands')

    # Build command
    build_parser = subparsers.add_parser('build', help='Build context for a task')
    build_parser.add_argument('--task', required=True, help='Task profile name')
    build_parser.add_argument('--feature', help='Optional feature modifier')
    build_parser.add_argument('--additional-files', nargs='+', help='Additional files to include')
    build_parser.add_argument('--verbose', action='store_true', help='Show detailed build report')
    build_parser.add_argument('--output', help='Output context to file instead of stdout')

    # Validate command
    validate_parser = subparsers.add_parser('validate', help='Validate manifest')

    # List command
    list_parser = subparsers.add_parser('list', help='List available profiles')

    # Export command
    export_parser = subparsers.add_parser('export', help='Export context to file')
    export_parser.add_argument('--task', required=True, help='Task profile name')
    export_parser.add_argument('--feature', help='Optional feature modifier')
    export_parser.add_argument('--output', required=True, help='Output file path')

    # Cache command
    cache_parser = subparsers.add_parser('cache', help='Manage context cache')
    cache_parser.add_argument('--clear', action='store_true', help='Clear all cached contexts')
    cache_parser.add_argument('--stats', action='store_true', help='Show cache statistics')

    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        return

    try:
        injector = EnhancedContextInjector()

        if args.command == 'build':
            result = injector.build_context(
                args.task,
                args.feature,
                args.additional_files,
                args.verbose
            )

            if args.output:
                with open(args.output, 'w') as f:
                    f.write(result['context'])
                print(f"Context written to: {args.output}")
                print(f"Total tokens: {result['metadata']['total_tokens']}")
            else:
                print(result['context'])

        elif args.command == 'validate':
            issues = injector.validate_manifest()

            if issues['errors']:
                print("ERRORS:")
                for error in issues['errors']:
                    print(f"  ✗ {error}")

            if issues['warnings']:
                print("\nWARNINGS:")
                for warning in issues['warnings']:
                    print(f"  ⚠ {warning}")

            if issues['info']:
                print("\nINFO:")
                for info in issues['info']:
                    print(f"  ℹ {info}")

            if not issues['errors']:
                print("\n✓ Manifest validation passed")

        elif args.command == 'list':
            profiles = injector.list_profiles()
            print("\nAvailable Context Profiles:")
            print("-" * 80)
            for p in profiles:
                print(f"\n{p['name']} (Priority: {p['priority']})")
                print(f"  Description: {p['description']}")
                print(f"  Size Budget: {p['size_budget']} bytes")
                print(f"  Files: {p['files']}, Dependencies: {p['dependencies']}")

        elif args.command == 'export':
            injector.export_context(args.task, args.output, args.feature)

        elif args.command == 'cache':
            if args.clear:
                cache_dir = injector.cache_dir
                for cache_file in cache_dir.glob('*.json'):
                    cache_file.unlink()
                print("Cache cleared")

            elif args.stats:
                cache_dir = injector.cache_dir
                cache_files = list(cache_dir.glob('*.json'))
                total_size = sum(f.stat().st_size for f in cache_files)
                print(f"Cache Statistics:")
                print(f"  Files: {len(cache_files)}")
                print(f"  Total Size: {total_size / 1024:.2f} KB")
                print(f"  Location: {cache_dir}")

    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)


if __name__ == '__main__':
    main()