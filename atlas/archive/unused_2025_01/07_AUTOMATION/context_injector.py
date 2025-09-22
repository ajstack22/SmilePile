#!/usr/bin/env python3
"""
Atlas Context Injector - Ensures agents have complete context including:
- Current iteration state
- Relevant documentation from dependencies
- Working baseline code
- Test requirements
- Integration points
"""

import json
import os
from pathlib import Path
from typing import Dict, List, Optional
import subprocess

class ContextInjector:
    """
    Injects complete context for agents so they never have to guess
    """

    def __init__(self):
        self.atlas_dir = Path(__file__).parent.parent
        self.context_cache = self.atlas_dir / '.atlas' / 'context_cache'
        self.context_cache.mkdir(parents=True, exist_ok=True)

    def build_complete_context(
        self,
        role: str,
        task: str,
        iteration: int = None,
        dependencies: List[str] = None
    ) -> Dict:
        """
        Build complete context for an agent

        Args:
            role: Agent role (developer, tester, etc.)
            task: Specific task description
            iteration: Current iteration number
            dependencies: List of components this depends on

        Returns:
            Complete context package
        """
        context = {
            'role': role,
            'task': task,
            'timestamp': self._get_timestamp(),
            'environment': self._get_environment_context(),
            'iteration': self._get_iteration_context(iteration),
            'codebase': self._get_codebase_context(dependencies),
            'documentation': self._get_documentation_context(dependencies),
            'tests': self._get_test_context(iteration),
            'integration': self._get_integration_points(dependencies),
            'standards': self._get_standards_context(role),
            'working_baseline': self._get_working_baseline(),
            'specific_instructions': self._generate_specific_instructions(role, task, iteration)
        }

        # Cache this context
        self._cache_context(context)

        return context

    def _get_timestamp(self) -> str:
        """Get current timestamp"""
        from datetime import datetime
        return datetime.now().isoformat()

    def _get_environment_context(self) -> Dict:
        """Get information about the development environment"""
        env_context = {
            'platform': self._detect_platform(),
            'build_tools': self._detect_build_tools(),
            'test_frameworks': self._detect_test_frameworks(),
            'project_type': self._detect_project_type(),
            'git_branch': self._get_git_branch(),
            'last_commit': self._get_last_commit()
        }
        return env_context

    def _detect_platform(self) -> str:
        """Detect the platform (Android, iOS, Web, etc.)"""
        indicators = {
            'build.gradle': 'Android',
            'Package.swift': 'iOS',
            'package.json': 'Web/Node',
            'Cargo.toml': 'Rust',
            'go.mod': 'Go',
            'requirements.txt': 'Python',
            'pom.xml': 'Java/Maven'
        }

        for file, platform in indicators.items():
            if (self.atlas_dir.parent / file).exists():
                return platform

        return 'Unknown'

    def _detect_build_tools(self) -> List[str]:
        """Detect available build tools"""
        tools = []
        tool_checks = {
            'gradle': './gradlew',
            'npm': 'package.json',
            'cargo': 'Cargo.toml',
            'make': 'Makefile',
            'maven': 'pom.xml'
        }

        for tool, indicator in tool_checks.items():
            if (self.atlas_dir.parent / indicator).exists():
                tools.append(tool)

        return tools

    def _detect_test_frameworks(self) -> List[str]:
        """Detect test frameworks in use"""
        frameworks = []

        # Check package files for test dependencies
        package_json = self.atlas_dir.parent / 'package.json'
        if package_json.exists():
            with open(package_json) as f:
                data = json.load(f)
                deps = {**data.get('dependencies', {}), **data.get('devDependencies', {})}
                if 'jest' in deps:
                    frameworks.append('jest')
                if 'mocha' in deps:
                    frameworks.append('mocha')
                if '@testing-library' in str(deps):
                    frameworks.append('testing-library')

        # Check for other test indicators
        if (self.atlas_dir.parent / 'pytest.ini').exists():
            frameworks.append('pytest')
        if (self.atlas_dir.parent / 'build.gradle').exists():
            frameworks.append('junit/espresso')

        return frameworks

    def _detect_project_type(self) -> str:
        """Detect the type of project"""
        # This could be expanded based on project analysis
        if (self.atlas_dir.parent / 'AndroidManifest.xml').exists():
            return 'Android App'
        elif (self.atlas_dir.parent / 'Info.plist').exists():
            return 'iOS App'
        elif (self.atlas_dir.parent / 'package.json').exists():
            return 'Web Application'
        else:
            return 'General Application'

    def _get_git_branch(self) -> str:
        """Get current git branch"""
        try:
            result = subprocess.run(
                'git rev-parse --abbrev-ref HEAD',
                shell=True,
                capture_output=True,
                text=True,
                cwd=self.atlas_dir.parent
            )
            return result.stdout.strip()
        except:
            return 'unknown'

    def _get_last_commit(self) -> str:
        """Get last commit hash and message"""
        try:
            result = subprocess.run(
                'git log -1 --oneline',
                shell=True,
                capture_output=True,
                text=True,
                cwd=self.atlas_dir.parent
            )
            return result.stdout.strip()
        except:
            return 'unknown'

    def _get_iteration_context(self, iteration: Optional[int]) -> Dict:
        """Get context about current iteration"""
        if iteration is None:
            # Try to read from iteration manager
            iteration_file = self.atlas_dir / '.atlas' / 'iteration_state.json'
            if iteration_file.exists():
                with open(iteration_file) as f:
                    state = json.load(f)
                    iteration = state.get('current_iteration', 0)

        iteration_doc = self.atlas_dir / 'iterations' / f'iteration_{iteration:03d}.md'

        context = {
            'number': iteration,
            'documentation': None,
            'previous_iterations': [],
            'focus': self._get_iteration_focus(iteration),
            'expected_coverage': self._get_expected_coverage(iteration)
        }

        # Read iteration documentation if it exists
        if iteration_doc.exists():
            with open(iteration_doc) as f:
                context['documentation'] = f.read()

        # Get summaries of previous iterations
        for i in range(max(0, iteration - 2), iteration):
            prev_doc = self.atlas_dir / 'iterations' / f'iteration_{i:03d}.md'
            if prev_doc.exists():
                with open(prev_doc) as f:
                    lines = f.readlines()
                    # Get first few lines as summary
                    summary = ''.join(lines[:10])
                    context['previous_iterations'].append({
                        'iteration': i,
                        'summary': summary
                    })

        return context

    def _get_iteration_focus(self, iteration: int) -> str:
        """Get the focus for a specific iteration"""
        focuses = {
            0: "Minimal Working System - just make something run",
            1: "Core functionality - main use case",
            2: "User interaction - basic UI",
            3: "Data persistence - save/load",
            4: "Error handling - graceful failures",
            5: "Advanced features - nice-to-haves",
            6: "Performance - optimization",
            7: "Polish - final touches"
        }
        return focuses.get(iteration, "Refinement")

    def _get_expected_coverage(self, iteration: int) -> int:
        """Get expected test coverage for iteration"""
        coverage_targets = {
            0: 10, 1: 20, 2: 30, 3: 40,
            4: 50, 5: 60, 6: 70, 7: 80
        }
        return coverage_targets.get(iteration, 85)

    def _get_codebase_context(self, dependencies: Optional[List[str]]) -> Dict:
        """Get context about the current codebase"""
        context = {
            'structure': self._analyze_project_structure(),
            'key_files': self._identify_key_files(),
            'dependencies': {}
        }

        if dependencies:
            for dep in dependencies:
                context['dependencies'][dep] = self._get_component_info(dep)

        return context

    def _analyze_project_structure(self) -> Dict:
        """Analyze and return project structure"""
        structure = {
            'directories': [],
            'main_source_dir': None,
            'test_dir': None,
            'doc_dir': None
        }

        # Common source directories
        src_dirs = ['src', 'app', 'lib', 'source']
        for dir_name in src_dirs:
            if (self.atlas_dir.parent / dir_name).exists():
                structure['main_source_dir'] = dir_name
                break

        # Common test directories
        test_dirs = ['test', 'tests', '__tests__', 'spec']
        for dir_name in test_dirs:
            if (self.atlas_dir.parent / dir_name).exists():
                structure['test_dir'] = dir_name
                break

        # List main directories
        for item in self.atlas_dir.parent.iterdir():
            if item.is_dir() and not item.name.startswith('.'):
                structure['directories'].append(item.name)

        return structure

    def _identify_key_files(self) -> List[str]:
        """Identify key files in the project"""
        key_files = []

        # Entry points
        entry_points = [
            'main.py', 'index.js', 'app.py', 'server.js',
            'MainActivity.kt', 'MainActivity.java',
            'main.go', 'main.rs'
        ]

        for entry in entry_points:
            for path in self.atlas_dir.parent.rglob(entry):
                key_files.append(str(path.relative_to(self.atlas_dir.parent)))

        return key_files[:10]  # Limit to 10 most important

    def _get_component_info(self, component: str) -> Dict:
        """Get information about a specific component"""
        info = {
            'name': component,
            'files': [],
            'documentation': None,
            'tests': [],
            'api': None
        }

        # Look for component files
        for path in self.atlas_dir.parent.rglob(f'*{component}*'):
            if path.is_file():
                info['files'].append(str(path.relative_to(self.atlas_dir.parent)))

        # Look for component documentation
        doc_path = self.atlas_dir / 'component_docs' / f'{component}.md'
        if doc_path.exists():
            with open(doc_path) as f:
                info['documentation'] = f.read()

        # Look for component tests
        for path in self.atlas_dir.parent.rglob(f'*test*{component}*'):
            if path.is_file():
                info['tests'].append(str(path.relative_to(self.atlas_dir.parent)))

        return info

    def _get_documentation_context(self, dependencies: Optional[List[str]]) -> Dict:
        """Get all relevant documentation"""
        docs = {
            'readme': None,
            'architecture': None,
            'api': None,
            'component_docs': {},
            'iteration_docs': []
        }

        # Read README
        readme = self.atlas_dir.parent / 'README.md'
        if readme.exists():
            with open(readme) as f:
                docs['readme'] = f.read()

        # Read architecture doc
        arch_doc = self.atlas_dir / 'ARCHITECTURE.md'
        if arch_doc.exists():
            with open(arch_doc) as f:
                docs['architecture'] = f.read()

        # Read component-specific docs
        if dependencies:
            for dep in dependencies:
                doc_file = self.atlas_dir / 'component_docs' / f'{dep}.md'
                if doc_file.exists():
                    with open(doc_file) as f:
                        docs['component_docs'][dep] = f.read()

        return docs

    def _get_test_context(self, iteration: Optional[int]) -> Dict:
        """Get context about testing requirements"""
        context = {
            'frameworks': self._detect_test_frameworks(),
            'current_coverage': self._get_current_coverage(),
            'target_coverage': self._get_expected_coverage(iteration or 0),
            'test_patterns': self._get_test_patterns(),
            'test_commands': self._get_test_commands()
        }
        return context

    def _get_current_coverage(self) -> float:
        """Get current test coverage if available"""
        # Try to read from coverage report
        coverage_file = self.atlas_dir.parent / 'coverage' / 'coverage.json'
        if coverage_file.exists():
            try:
                with open(coverage_file) as f:
                    data = json.load(f)
                    return data.get('total', {}).get('percentage', 0)
            except:
                pass
        return 0

    def _get_test_patterns(self) -> List[str]:
        """Get common test patterns for the project"""
        patterns = []

        platform = self._detect_platform()
        if platform == 'Android':
            patterns = [
                'Unit tests in test/ directory',
                'Instrumented tests in androidTest/',
                'Use JUnit for unit tests',
                'Use Espresso for UI tests'
            ]
        elif platform == 'Web/Node':
            patterns = [
                'Test files as *.test.js or *.spec.js',
                'Tests adjacent to source files',
                'Use describe/it blocks',
                'Mock external dependencies'
            ]
        elif platform == 'Python':
            patterns = [
                'Test files as test_*.py',
                'Tests in tests/ directory',
                'Use pytest fixtures',
                'Test classes inherit from TestCase'
            ]

        return patterns

    def _get_test_commands(self) -> Dict[str, str]:
        """Get test commands for the platform"""
        platform = self._detect_platform()

        commands = {
            'Android': {
                'unit': './gradlew test',
                'instrumented': './gradlew connectedAndroidTest',
                'all': './gradlew test connectedAndroidTest'
            },
            'Web/Node': {
                'unit': 'npm test',
                'coverage': 'npm run test:coverage',
                'watch': 'npm run test:watch'
            },
            'Python': {
                'unit': 'pytest',
                'coverage': 'pytest --cov',
                'verbose': 'pytest -v'
            }
        }

        return commands.get(platform, {})

    def _get_integration_points(self, dependencies: Optional[List[str]]) -> List[Dict]:
        """Get integration points with other components"""
        points = []

        if dependencies:
            for dep in dependencies:
                points.append({
                    'component': dep,
                    'type': 'dependency',
                    'notes': f'This task depends on {dep}',
                    'interface': self._get_component_interface(dep)
                })

        return points

    def _get_component_interface(self, component: str) -> Dict:
        """Get the interface/API of a component"""
        interface = {
            'methods': [],
            'inputs': [],
            'outputs': []
        }

        # Look for interface documentation
        interface_doc = self.atlas_dir / 'interfaces' / f'{component}.json'
        if interface_doc.exists():
            with open(interface_doc) as f:
                interface = json.load(f)

        return interface

    def _get_standards_context(self, role: str) -> Dict:
        """Get coding standards for the role"""
        standards = {
            'general': [
                'Follow existing code patterns',
                'Write tests for new code',
                'Document public APIs',
                'No comments unless necessary'
            ]
        }

        role_standards = {
            'developer': [
                'Use TDD when possible',
                'Keep methods small and focused',
                'Handle errors gracefully',
                'Ensure backward compatibility'
            ],
            'tester': [
                'Test happy path and edge cases',
                'Include negative tests',
                'Mock external dependencies',
                'Aim for 80% coverage minimum'
            ],
            'documenter': [
                'Use clear, concise language',
                'Include code examples',
                'Document all public APIs',
                'Keep docs up-to-date with code'
            ]
        }

        standards['role_specific'] = role_standards.get(role, [])

        return standards

    def _get_working_baseline(self) -> Dict:
        """Get information about the current working baseline"""
        baseline = {
            'branch': self._get_git_branch(),
            'commit': self._get_last_commit(),
            'build_status': 'unknown',
            'test_status': 'unknown'
        }

        # Check if last build passed
        build_log = self.atlas_dir / '.atlas' / 'last_build.json'
        if build_log.exists():
            with open(build_log) as f:
                build_data = json.load(f)
                baseline['build_status'] = 'passing' if build_data.get('success') else 'failing'

        # Check if tests passed
        test_log = self.atlas_dir / '.atlas' / 'last_test.json'
        if test_log.exists():
            with open(test_log) as f:
                test_data = json.load(f)
                baseline['test_status'] = 'passing' if test_data.get('success') else 'failing'

        return baseline

    def _generate_specific_instructions(
        self,
        role: str,
        task: str,
        iteration: Optional[int]
    ) -> List[str]:
        """Generate specific instructions based on context"""
        instructions = []

        # Base instructions for all roles
        instructions.extend([
            f"You are working on iteration {iteration or 0}",
            f"Your role is {role}",
            f"Your specific task is: {task}",
            "You must work with the existing codebase",
            "All changes must maintain backward compatibility",
            "You must ensure all existing tests still pass"
        ])

        # Role-specific instructions
        if role == 'developer':
            instructions.extend([
                "Write tests BEFORE implementing the feature",
                "Ensure your code compiles before marking complete",
                "Create documentation for any new APIs",
                "Follow the existing code patterns exactly"
            ])
        elif role == 'tester':
            instructions.extend([
                "Write tests that cover happy path and edge cases",
                "Include negative test cases",
                "Ensure tests are maintainable and clear",
                "Document what each test validates"
            ])

        # Iteration-specific instructions
        if iteration == 0:
            instructions.append("Focus on getting something minimal working")
        elif iteration and iteration <= 3:
            instructions.append("Focus on core functionality, not optimization")
        elif iteration and iteration > 5:
            instructions.append("Focus on optimization and polish")

        return instructions

    def _cache_context(self, context: Dict):
        """Cache context for debugging and audit"""
        from datetime import datetime

        filename = f"context_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        cache_file = self.context_cache / filename

        with open(cache_file, 'w') as f:
            json.dump(context, f, indent=2, default=str)

    def generate_prompt(self, context: Dict) -> str:
        """Generate a complete prompt from context"""
        prompt = f"""
You are a {context['role']} agent working on {context['task']}.

## Current Iteration
Iteration {context['iteration']['number']}: {context['iteration']['focus']}
Target test coverage: {context['iteration']['expected_coverage']}%

## Environment
- Platform: {context['environment']['platform']}
- Build tools: {', '.join(context['environment']['build_tools'])}
- Test frameworks: {', '.join(context['environment']['test_frameworks'])}
- Current branch: {context['environment']['git_branch']}

## Project Structure
- Main source: {context['codebase']['structure']['main_source_dir']}
- Test directory: {context['codebase']['structure']['test_dir']}
- Key files: {', '.join(context['codebase']['key_files'][:5])}

## Testing Requirements
- Current coverage: {context['tests']['current_coverage']}%
- Target coverage: {context['tests']['target_coverage']}%
- Test commands: {json.dumps(context['tests']['test_commands'], indent=2)}

## Specific Instructions
{chr(10).join('- ' + inst for inst in context['specific_instructions'])}

## Dependencies
{self._format_dependencies(context['codebase'].get('dependencies', {}))}

## Standards
{chr(10).join('- ' + std for std in context['standards']['general'])}
{chr(10).join('- ' + std for std in context['standards']['role_specific'])}

## Your Task
{context['task']}

Remember:
1. Build on the existing working code
2. Write tests for your changes
3. Ensure everything compiles
4. Document your work
5. Follow existing patterns
"""
        return prompt

    def _format_dependencies(self, dependencies: Dict) -> str:
        """Format dependencies for display"""
        if not dependencies:
            return "No specific dependencies"

        lines = []
        for name, info in dependencies.items():
            lines.append(f"- {name}:")
            if info.get('documentation'):
                lines.append(f"  Documentation available")
            if info.get('files'):
                lines.append(f"  Files: {', '.join(info['files'][:3])}")

        return '\n'.join(lines)

def main():
    """CLI interface for context injector"""
    import sys

    injector = ContextInjector()
    args = sys.argv[1:]

    if not args:
        print(json.dumps({
            'error': 'No command provided',
            'usage': {
                'build': 'context_injector.py build [role] [task] --iteration 2 --deps component1,component2',
                'generate': 'context_injector.py generate [role] [task]',
                'environment': 'context_injector.py environment',
                'test-info': 'context_injector.py test-info'
            }
        }, indent=2))
        return

    command = args[0]

    if command == 'build' and len(args) >= 3:
        role = args[1]
        task = ' '.join(args[2:])

        # Parse optional arguments
        iteration = None
        dependencies = None

        if '--iteration' in args:
            idx = args.index('--iteration')
            if idx + 1 < len(args):
                iteration = int(args[idx + 1])

        if '--deps' in args:
            idx = args.index('--deps')
            if idx + 1 < len(args):
                dependencies = args[idx + 1].split(',')

        context = injector.build_complete_context(role, task, iteration, dependencies)
        print(json.dumps(context, indent=2, default=str))

    elif command == 'generate' and len(args) >= 3:
        role = args[1]
        task = ' '.join(args[2:])
        context = injector.build_complete_context(role, task)
        prompt = injector.generate_prompt(context)
        print(prompt)

    elif command == 'environment':
        env = injector._get_environment_context()
        print(json.dumps(env, indent=2))

    elif command == 'test-info':
        iteration = 0
        if len(args) > 1:
            iteration = int(args[1])
        test_context = injector._get_test_context(iteration)
        print(json.dumps(test_context, indent=2))

    else:
        print(json.dumps({
            'error': f'Unknown command: {command}'
        }, indent=2))

if __name__ == '__main__':
    main()