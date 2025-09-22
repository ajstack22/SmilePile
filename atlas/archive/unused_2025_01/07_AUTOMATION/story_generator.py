#!/usr/bin/env python3
"""
Atlas Story Generator - Template-based story creation
"""

import json
from pathlib import Path
from typing import Dict, List, Optional
from dataclasses import dataclass
from enum import Enum

class StoryTemplate:
    """Templates for different story types"""

    FEATURE_TEMPLATE = {
        'title': '{feature_name}',
        'acceptance_criteria': [
            'User can {primary_action}',
            '{validation_behavior}',
            'Changes persist after {persistence_scenario}',
            'Error handling for {error_scenarios}'
        ],
        'implementation_notes': """
        1. Create UI components for {ui_elements}
        2. Implement {business_logic}
        3. Add data layer for {data_requirements}
        4. Include unit tests for {test_areas}
        5. Update documentation
        """,
        'labels': ['feature', 'user-facing'],
        'estimate': 5
    }

    BUG_TEMPLATE = {
        'title': 'Fix: {bug_description}',
        'acceptance_criteria': [
            '{expected_behavior} works correctly',
            'No regression in {related_features}',
            'Fix verified on {platforms}',
            'Root cause documented'
        ],
        'implementation_notes': """
        1. Reproduce issue in {environment}
        2. Identify root cause in {suspected_area}
        3. Implement fix
        4. Add regression test
        5. Verify on all affected platforms
        """,
        'labels': ['bug', 'defect'],
        'estimate': 3
    }

    OPTIMIZATION_TEMPLATE = {
        'title': 'Optimize: {optimization_target}',
        'acceptance_criteria': [
            '{metric} improved by {target_percentage}%',
            'No functionality regression',
            'Performance validated with {validation_method}',
            'Optimization documented'
        ],
        'implementation_notes': """
        1. Baseline current {metric}
        2. Profile {profiling_areas}
        3. Implement optimization for {optimization_areas}
        4. Measure improvement
        5. Document changes and impact
        """,
        'labels': ['optimization', 'performance'],
        'estimate': 5
    }

    TECH_DEBT_TEMPLATE = {
        'title': 'Refactor: {refactor_target}',
        'acceptance_criteria': [
            'Code follows {standards}',
            'All tests pass',
            'No functional changes',
            'Technical debt reduced in {debt_area}'
        ],
        'implementation_notes': """
        1. Identify refactoring opportunities in {target_area}
        2. Create safety net with tests
        3. Refactor {refactor_scope}
        4. Validate no regression
        5. Update documentation
        """,
        'labels': ['tech-debt', 'refactoring'],
        'estimate': 4
    }

    SECURITY_TEMPLATE = {
        'title': 'Security: {security_issue}',
        'acceptance_criteria': [
            'Vulnerability {vulnerability_type} mitigated',
            'Security scan passes',
            'No performance degradation',
            'Security measures documented'
        ],
        'implementation_notes': """
        1. Assess security vulnerability
        2. Implement security controls
        3. Add security tests
        4. Run security scan
        5. Document security measures
        """,
        'labels': ['security', 'critical'],
        'estimate': 4
    }

class EpicDecomposer:
    """Decompose epics into stories"""

    def __init__(self):
        self.patterns = {
            'authentication': [
                {'title': 'User Registration', 'type': 'feature', 'estimate': 5},
                {'title': 'User Login', 'type': 'feature', 'estimate': 3},
                {'title': 'Password Reset', 'type': 'feature', 'estimate': 3},
                {'title': 'Session Management', 'type': 'feature', 'estimate': 4},
                {'title': 'OAuth Integration', 'type': 'feature', 'estimate': 5}
            ],
            'crud': [
                {'title': 'Create {entity}', 'type': 'feature', 'estimate': 3},
                {'title': 'Read/List {entity}', 'type': 'feature', 'estimate': 2},
                {'title': 'Update {entity}', 'type': 'feature', 'estimate': 3},
                {'title': 'Delete {entity}', 'type': 'feature', 'estimate': 2},
                {'title': 'Search/Filter {entity}', 'type': 'feature', 'estimate': 3}
            ],
            'performance': [
                {'title': 'Profile Current Performance', 'type': 'research', 'estimate': 2},
                {'title': 'Optimize Database Queries', 'type': 'optimization', 'estimate': 5},
                {'title': 'Implement Caching', 'type': 'optimization', 'estimate': 4},
                {'title': 'Optimize Asset Loading', 'type': 'optimization', 'estimate': 3},
                {'title': 'Add Performance Monitoring', 'type': 'feature', 'estimate': 3}
            ],
            'testing': [
                {'title': 'Unit Test Coverage', 'type': 'tech_debt', 'estimate': 4},
                {'title': 'Integration Tests', 'type': 'tech_debt', 'estimate': 5},
                {'title': 'E2E Test Suite', 'type': 'tech_debt', 'estimate': 8},
                {'title': 'Performance Tests', 'type': 'tech_debt', 'estimate': 3},
                {'title': 'Security Tests', 'type': 'tech_debt', 'estimate': 4}
            ],
            'mobile_optimization': [
                {'title': 'Responsive UI', 'type': 'feature', 'estimate': 5},
                {'title': 'Touch Gesture Support', 'type': 'feature', 'estimate': 4},
                {'title': 'Offline Mode', 'type': 'feature', 'estimate': 6},
                {'title': 'Push Notifications', 'type': 'feature', 'estimate': 4},
                {'title': 'Battery Optimization', 'type': 'optimization', 'estimate': 3}
            ]
        }

    def decompose(self, epic_type: str, entity_name: str = None) -> List[Dict]:
        """Decompose epic into stories"""
        if epic_type not in self.patterns:
            return []

        stories = []
        for pattern in self.patterns[epic_type]:
            story = pattern.copy()
            if entity_name and '{entity}' in story['title']:
                story['title'] = story['title'].replace('{entity}', entity_name)
            stories.append(story)

        return stories

class StoryGenerator:
    """Generate stories from templates"""

    def __init__(self, project_root: Path = None):
        self.project_root = project_root or Path.cwd()
        self.templates_dir = self.project_root / '.atlas' / 'templates'
        self.decomposer = EpicDecomposer()

        self.templates_dir.mkdir(parents=True, exist_ok=True)
        self.load_custom_templates()

    def load_custom_templates(self):
        """Load custom templates from JSON"""
        custom_file = self.templates_dir / 'custom_templates.json'
        if custom_file.exists():
            with open(custom_file) as f:
                self.custom_templates = json.load(f)
        else:
            self.custom_templates = {}

    def save_custom_template(self, name: str, template: Dict):
        """Save custom template"""
        self.custom_templates[name] = template
        custom_file = self.templates_dir / 'custom_templates.json'
        with open(custom_file, 'w') as f:
            json.dump(self.custom_templates, f, indent=2)

    def generate_from_template(self, template_name: str, variables: Dict) -> Dict:
        """Generate story from template"""
        # Get template
        if template_name == 'feature':
            template = StoryTemplate.FEATURE_TEMPLATE
        elif template_name == 'bug':
            template = StoryTemplate.BUG_TEMPLATE
        elif template_name == 'optimization':
            template = StoryTemplate.OPTIMIZATION_TEMPLATE
        elif template_name == 'tech_debt':
            template = StoryTemplate.TECH_DEBT_TEMPLATE
        elif template_name == 'security':
            template = StoryTemplate.SECURITY_TEMPLATE
        elif template_name in self.custom_templates:
            template = self.custom_templates[template_name]
        else:
            raise ValueError(f"Unknown template: {template_name}")

        # Fill in template
        story = {}
        for key, value in template.items():
            if isinstance(value, str):
                story[key] = self._fill_variables(value, variables)
            elif isinstance(value, list):
                story[key] = [self._fill_variables(item, variables) for item in value]
            else:
                story[key] = value

        return story

    def _fill_variables(self, text: str, variables: Dict) -> str:
        """Fill in template variables"""
        for var, value in variables.items():
            text = text.replace(f'{{{var}}}', str(value))
        return text

    def generate_epic_stories(self, epic_type: str, entity_name: str = None) -> List[Dict]:
        """Generate stories for an epic"""
        return self.decomposer.decompose(epic_type, entity_name)

    def batch_generate(self, specifications: List[Dict]) -> List[Dict]:
        """Batch generate multiple stories"""
        stories = []
        for spec in specifications:
            if 'epic' in spec:
                # Generate epic stories
                epic_stories = self.generate_epic_stories(
                    spec['epic'],
                    spec.get('entity')
                )
                stories.extend(epic_stories)
            elif 'template' in spec:
                # Generate from template
                story = self.generate_from_template(
                    spec['template'],
                    spec.get('variables', {})
                )
                stories.append(story)

        return stories

    def generate_test_stories(self, feature_id: str, feature_name: str) -> List[Dict]:
        """Generate test stories for a feature"""
        test_stories = [
            {
                'title': f'Unit Tests for {feature_name}',
                'type': 'tech_debt',
                'estimate': 2,
                'dependencies': [feature_id],
                'labels': ['testing', 'unit-tests']
            },
            {
                'title': f'Integration Tests for {feature_name}',
                'type': 'tech_debt',
                'estimate': 3,
                'dependencies': [feature_id],
                'labels': ['testing', 'integration-tests']
            },
            {
                'title': f'E2E Tests for {feature_name}',
                'type': 'tech_debt',
                'estimate': 3,
                'dependencies': [feature_id],
                'labels': ['testing', 'e2e-tests']
            }
        ]
        return test_stories

    def generate_documentation_story(self, feature_id: str, feature_name: str) -> Dict:
        """Generate documentation story for a feature"""
        return {
            'title': f'Documentation for {feature_name}',
            'type': 'tech_debt',
            'estimate': 1,
            'dependencies': [feature_id],
            'labels': ['documentation'],
            'acceptance_criteria': [
                'User documentation updated',
                'API documentation complete',
                'Code comments added',
                'README updated if needed'
            ]
        }

    def analyze_and_suggest(self, existing_stories: List[Dict]) -> List[Dict]:
        """Analyze existing stories and suggest missing ones"""
        suggestions = []

        # Check for features without tests
        feature_ids = {s['id'] for s in existing_stories if s.get('type') == 'feature'}
        test_dependencies = set()

        for story in existing_stories:
            if 'testing' in story.get('labels', []):
                deps = story.get('dependencies', [])
                test_dependencies.update(deps)

        untested_features = feature_ids - test_dependencies
        for feature_id in untested_features:
            feature = next((s for s in existing_stories if s['id'] == feature_id), None)
            if feature:
                suggestions.append({
                    'suggestion': 'missing_tests',
                    'feature_id': feature_id,
                    'feature_name': feature.get('title', 'Unknown'),
                    'recommended_stories': self.generate_test_stories(
                        feature_id,
                        feature.get('title', 'Feature')
                    )
                })

        # Check for features without documentation
        doc_dependencies = set()
        for story in existing_stories:
            if 'documentation' in story.get('labels', []):
                deps = story.get('dependencies', [])
                doc_dependencies.update(deps)

        undocumented_features = feature_ids - doc_dependencies
        for feature_id in undocumented_features:
            feature = next((s for s in existing_stories if s['id'] == feature_id), None)
            if feature:
                suggestions.append({
                    'suggestion': 'missing_documentation',
                    'feature_id': feature_id,
                    'feature_name': feature.get('title', 'Unknown'),
                    'recommended_story': self.generate_documentation_story(
                        feature_id,
                        feature.get('title', 'Feature')
                    )
                })

        return suggestions

def main():
    """CLI interface"""
    import argparse

    parser = argparse.ArgumentParser(description='Atlas Story Generator')
    parser.add_argument('action', choices=['generate', 'epic', 'suggest'])
    parser.add_argument('--template', help='Template name')
    parser.add_argument('--epic-type', help='Epic type')
    parser.add_argument('--entity', help='Entity name for CRUD operations')
    parser.add_argument('--variables', help='JSON string of template variables')

    args = parser.parse_args()

    generator = StoryGenerator()

    if args.action == 'generate' and args.template:
        variables = json.loads(args.variables) if args.variables else {}
        story = generator.generate_from_template(args.template, variables)
        print(json.dumps(story, indent=2))

    elif args.action == 'epic' and args.epic_type:
        stories = generator.generate_epic_stories(args.epic_type, args.entity)
        for story in stories:
            print(f"- {story['title']} ({story['type']}, {story['estimate']} points)")

    elif args.action == 'suggest':
        # Would need to load existing stories
        print("Analysis requires loading existing stories from backlog")

if __name__ == '__main__':
    main()