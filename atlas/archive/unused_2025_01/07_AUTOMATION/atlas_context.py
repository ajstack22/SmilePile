#!/usr/bin/env python3
"""
Atlas Context Injector - Provides role-specific context for spawned agents
Ensures agents receive proper role definition, task context, and business/tech rules
"""

import sys
import json
from pathlib import Path

class AtlasContextInjector:
    """
    Generates focused context for agents based on their role and task
    """

    def __init__(self):
        self.atlas_dir = Path(__file__).parent.parent
        self.roles = self._load_roles()
        self.standards = self._load_standards()

    def _load_roles(self):
        """Load role definitions from Atlas documentation"""
        return {
            'pm': {
                'identity': 'You are a PRODUCT MANAGER agent for Atlas development',
                'capabilities': [
                    'Analyze requirements and create user stories',
                    'Define acceptance criteria',
                    'Prioritize backlog items',
                    'Create epics and break them into features',
                    'Define business value and success metrics'
                ],
                'constraints': [
                    'MUST create stories in F#### format for features',
                    'MUST create bugs in B#### format',
                    'MUST include clear acceptance criteria',
                    'MUST assign appropriate priorities',
                    'Focus on user value and business outcomes',
                    'NEVER implement - only define requirements'
                ],
                'tools': ['Read', 'Bash'],  # For running 02_create_story.py
                'output_format': 'Created stories with IDs, priorities, and acceptance criteria'
            },
            'researcher': {
                'identity': 'You are a RESEARCHER agent for Atlas development',
                'capabilities': [
                    'Search and analyze codebases',
                    'Research patterns and best practices',
                    'Find similar implementations',
                    'Identify technical approaches'
                ],
                'constraints': [
                    'NEVER implement code directly',
                    'Focus on gathering information',
                    'Return structured findings',
                    'Cite sources with file:line references'
                ],
                'tools': ['Grep', 'Glob', 'Read', 'WebSearch'],
                'output_format': 'Structured JSON with findings, patterns, and recommendations'
            },
            'developer': {
                'identity': 'You are a DEVELOPER agent for Atlas development',
                'capabilities': [
                    'Implement features following specifications',
                    'Write clean, maintainable code',
                    'Follow existing patterns in codebase',
                    'Create unit tests',
                    'Verify code compiles and runs'
                ],
                'constraints': [
                    'MUST compile/build code before marking complete',
                    'MUST fix ALL compilation errors',
                    'MUST verify code actually runs',
                    'NEVER mark task complete with broken code',
                    'MUST follow existing code conventions',
                    'MUST write tests for new code',
                    'NEVER add unnecessary comments',
                    'Use existing libraries - do not add new dependencies'
                ],
                'tools': ['Read', 'Write', 'Edit', 'MultiEdit', 'Bash'],
                'output_format': 'Code implementation with test coverage report'
            },
            'tester': {
                'identity': 'You are a TESTER agent for Atlas development',
                'capabilities': [
                    'Write comprehensive test cases',
                    'Run test suites',
                    'Identify edge cases',
                    'Validate acceptance criteria'
                ],
                'constraints': [
                    'Test EVERYTHING - happy path and edge cases',
                    'Use existing test frameworks',
                    'Report coverage metrics',
                    'Document test results clearly'
                ],
                'tools': ['Read', 'Write', 'Edit', 'Bash'],
                'output_format': 'Test results with coverage metrics and any failures'
            },
            'reviewer': {
                'identity': 'You are a REVIEWER agent for Atlas development',
                'capabilities': [
                    'Review code quality',
                    'Check adherence to standards',
                    'Identify potential bugs',
                    'Suggest improvements'
                ],
                'constraints': [
                    'Be constructive but thorough',
                    'Check for Atlas standard compliance',
                    'Verify test coverage',
                    'Look for security issues'
                ],
                'tools': ['Read', 'Grep', 'Glob'],
                'output_format': 'Review report with issues categorized by severity'
            },
            'documenter': {
                'identity': 'You are a DOCUMENTER agent for Atlas development',
                'capabilities': [
                    'Create clear documentation',
                    'Update existing docs',
                    'Generate API documentation',
                    'Write user guides'
                ],
                'constraints': [
                    'Use clear, concise language',
                    'Include code examples',
                    'Follow markdown best practices',
                    'Keep documentation up-to-date'
                ],
                'tools': ['Read', 'Write', 'Edit'],
                'output_format': 'Markdown documentation following Atlas standards'
            },
            'debugger': {
                'identity': 'You are a DEBUGGER agent for Atlas development',
                'capabilities': [
                    'Diagnose issues systematically',
                    'Trace error sources',
                    'Identify root causes',
                    'Propose fixes'
                ],
                'constraints': [
                    'Use scientific method - hypothesis and test',
                    'Document debugging steps',
                    'Verify fixes work',
                    'Check for side effects'
                ],
                'tools': ['Read', 'Grep', 'Bash', 'Edit'],
                'output_format': 'Diagnosis report with root cause and verified fix'
            }
        }

    def _load_standards(self):
        """Load Atlas business and technical standards"""
        return {
            'naming': {
                'features': 'F#### format (e.g., F0001)',
                'bugs': 'B#### format (e.g., B0001)',
                'tech_debt': 'T#### format (e.g., T0001)',
                'epics': 'E### format (e.g., E001)'
            },
            'priority': {
                'levels': ['critical', 'high', 'medium', 'low'],
                'scoring': 'WSJF = (Business Value + Time Criticality + Risk) / Job Size',
                'critical_sla': 'Start within 1 hour',
                'high_sla': 'Start within 1 day'
            },
            'workflow': {
                'statuses': ['backlog', 'ready', 'in_progress', 'in_review', 'testing', 'done', 'blocked'],
                'quality_gates': 'Must pass tests, review, and validation before done',
                'parallel_execution': 'Spawn 3-5 agents for optimal performance'
            },
            'code_standards': {
                'no_comments': 'Do not add comments unless explicitly requested',
                'follow_patterns': 'Match existing code style and patterns',
                'test_coverage': 'Minimum 80% test coverage for new code',
                'security': 'Never expose secrets or credentials'
            }
        }

    def get_context_for_agent(self, role, task_description, project_context=None):
        """
        Generate complete context for an agent

        Args:
            role: Agent role (researcher, developer, tester, etc.)
            task_description: What the agent needs to do
            project_context: Optional project-specific context

        Returns:
            Complete context string for agent
        """
        if role not in self.roles:
            return {'error': f'Unknown role: {role}. Available: {list(self.roles.keys())}'}

        role_def = self.roles[role]

        context = {
            'role_definition': role_def,
            'task': task_description,
            'standards': self.standards,
            'project_context': project_context or {},
            'instructions': self._generate_instructions(role, task_description)
        }

        return context

    def _generate_instructions(self, role, task):
        """Generate specific instructions based on role and task"""
        base_instructions = [
            f"You are operating as a {role.upper()} agent in the Atlas framework.",
            f"Your specific task: {task}",
            "",
            "CRITICAL RULES:",
            "1. Follow your role constraints strictly",
            "2. Use only the tools available to your role",
            "3. Return output in the specified format",
            "4. Follow Atlas naming and priority standards",
            "5. Work efficiently - this is likely running in parallel with other agents"
        ]

        # Add role-specific instructions
        if role == 'researcher':
            base_instructions.extend([
                "",
                "RESEARCH APPROACH:",
                "- Start with broad search, then narrow down",
                "- Always check multiple sources",
                "- Look for existing patterns before suggesting new ones",
                "- Include file:line references for all findings"
            ])
        elif role == 'developer':
            base_instructions.extend([
                "",
                "DEVELOPMENT APPROACH:",
                "- Read existing code first to understand patterns",
                "- Follow TDD - write tests first if possible",
                "- Keep changes focused and atomic",
                "- Ensure backward compatibility",
                "",
                "BUILD VERIFICATION WORKFLOW:",
                "1. Write your code implementation",
                "2. Run appropriate build command:",
                "   - Android: ./gradlew build",
                "   - iOS: xcodebuild",
                "   - Web: npm run build",
                "   - Python: python -m py_compile",
                "3. If build fails: Fix errors and retry",
                "4. Run basic smoke test",
                "5. ONLY mark complete when build passes",
                "",
                "CRITICAL: Never submit code that doesn't compile!"
            ])
        elif role == 'tester':
            base_instructions.extend([
                "",
                "TESTING APPROACH:",
                "- Test happy path first, then edge cases",
                "- Include negative test cases",
                "- Check performance implications",
                "- Verify all acceptance criteria"
            ])

        return "\n".join(base_instructions)

    def get_spawn_template(self, role, task):
        """
        Generate a ready-to-use spawn template for orchestrator

        Args:
            role: Agent role
            task: Task description

        Returns:
            Complete prompt template for Task tool
        """
        context = self.get_context_for_agent(role, task)

        if 'error' in context:
            return context

        template = f"""
{context['role_definition']['identity']}

TASK: {task}

CAPABILITIES:
{chr(10).join('- ' + cap for cap in context['role_definition']['capabilities'])}

CONSTRAINTS:
{chr(10).join('- ' + con for con in context['role_definition']['constraints'])}

ATLAS STANDARDS:
- Item naming: {json.dumps(context['standards']['naming'])}
- Priority levels: {context['standards']['priority']['levels']}
- Workflow statuses: {context['standards']['workflow']['statuses']}
- Code standards: No comments unless asked, follow patterns, 80% test coverage

APPROACH:
{context['instructions']}

OUTPUT FORMAT:
{context['role_definition']['output_format']}

Execute the task and return results in the specified format.
"""

        return {
            'role': role,
            'task': task,
            'prompt': template,
            'tools': context['role_definition']['tools']
        }

    def get_parallel_batch_context(self, agent_configs):
        """
        Generate context for multiple parallel agents

        Args:
            agent_configs: List of {'role': str, 'task': str} dicts

        Returns:
            List of spawn templates for parallel execution
        """
        templates = []
        for config in agent_configs:
            template = self.get_spawn_template(
                config.get('role'),
                config.get('task')
            )
            templates.append(template)

        return {
            'action': 'parallel_batch_context',
            'agent_count': len(templates),
            'templates': templates,
            'optimization_note': f'Spawning {len(templates)} agents in parallel for ~{len(templates)-1}x speedup'
        }

def main():
    """
    Entry point for context injection

    Usage:
        python3 atlas_context.py get [role] "task description"
        python3 atlas_context.py template [role] "task description"
        python3 atlas_context.py batch '[{"role":"researcher","task":"find patterns"}]'
    """
    injector = AtlasContextInjector()

    args = sys.argv[1:]

    if not args:
        usage = {
            'script': 'atlas_context.py',
            'description': 'Atlas context injector for agents',
            'commands': [
                {
                    'command': 'get [role] "task"',
                    'description': 'Get context for single agent'
                },
                {
                    'command': 'template [role] "task"',
                    'description': 'Get spawn template for agent'
                },
                {
                    'command': 'batch \'[{"role":"x","task":"y"}]\'',
                    'description': 'Get templates for parallel agents'
                },
                {
                    'command': 'roles',
                    'description': 'List available roles'
                }
            ],
            'available_roles': list(injector.roles.keys())
        }
        print(json.dumps(usage, indent=2))
        return

    command = args[0]

    if command == 'roles':
        print(json.dumps({
            'available_roles': list(injector.roles.keys()),
            'role_details': injector.roles
        }, indent=2))

    elif command == 'get' and len(args) >= 3:
        role = args[1]
        task = ' '.join(args[2:])
        context = injector.get_context_for_agent(role, task)
        print(json.dumps(context, indent=2))

    elif command == 'template' and len(args) >= 3:
        role = args[1]
        task = ' '.join(args[2:])
        template = injector.get_spawn_template(role, task)
        print(json.dumps(template, indent=2))

    elif command == 'batch' and len(args) >= 2:
        try:
            configs = json.loads(args[1])
            result = injector.get_parallel_batch_context(configs)
            print(json.dumps(result, indent=2))
        except json.JSONDecodeError:
            print(json.dumps({'error': 'Invalid JSON for batch config'}, indent=2))

    else:
        print(json.dumps({'error': f'Unknown command: {command}'}, indent=2))

if __name__ == "__main__":
    main()