#!/usr/bin/env python3
"""
Atlas Orchestrator Context Manager - Designed to be called by Claude Orchestrator
This script manages long-term context for complex, multi-week projects.
Provides state persistence and memory management across sessions.
"""

import json
import os
from datetime import datetime
from pathlib import Path
import sys
import subprocess

class OrchestratorContext:
    """
    Manages orchestration context and state across multiple sessions
    """

    def __init__(self):
        self.state_dir = Path('.atlas/orchestrator')
        self.state_dir.mkdir(parents=True, exist_ok=True)
        self.context = self.load_context()
        self.current_session = datetime.now().isoformat()

    def load_context(self):
        """Load existing context from disk"""
        context_file = self.state_dir / 'context.json'
        if context_file.exists():
            with open(context_file, 'r') as f:
                return json.load(f)
        return {}

    def save_context(self):
        """Save context to disk"""
        context_file = self.state_dir / 'context.json'
        with open(context_file, 'w') as f:
            json.dump(self.context, f, indent=2)

    def save_state(self, phase, status, data=None):
        """Save state for a specific phase"""
        state = {
            'phase': phase,
            'status': status,
            'timestamp': self.current_session,
            'context': self.context,
            'data': data or {}
        }

        state_file = self.state_dir / f'state_{phase}_{datetime.now().strftime("%Y%m%d_%H%M%S")}.json'
        with open(state_file, 'w') as f:
            json.dump(state, f, indent=2)

    def get_recent_activity(self):
        """Get recent activity from state files"""
        states = []
        for state_file in sorted(self.state_dir.glob('state_*.json'))[-5:]:
            with open(state_file, 'r') as f:
                states.append(json.load(f))
        return states

    def execute_command(self, command, *args):
        """
        Execute orchestrator commands and return structured results

        Returns:
            JSON string with command results
        """
        commands = {
            'new': self.start_new_project,
            'resume': self.resume_project,
            'status': self.get_project_status,
            'objective': self.set_objective,
            'complete': self.complete_phase,
            'insight': self.add_insight,
            'memory': self.get_memory_summary,
            'agents': self.list_active_agents,
            'spawn': self.spawn_agent,
            'task': self.add_task
        }

        if command in commands:
            result = commands[command](*args)
            return json.dumps(result, indent=2)
        else:
            return json.dumps({
                'error': f'Unknown command: {command}',
                'available_commands': list(commands.keys())
            }, indent=2)

    def start_new_project(self, project_name=None):
        """
        Initialize a new orchestration project

        Returns:
            Project initialization details
        """
        project_id = f"P{datetime.now().strftime('%Y%m%d%H%M%S')}"
        project_name = project_name or project_id

        self.context = {
            'project_id': project_id,
            'project_name': project_name,
            'start_date': self.current_session,
            'phase': 'initialization',
            'status': 'active',
            'objectives': [],
            'insights': [],
            'active_agents': [],
            'completed_phases': [],
            'tasks': [],
            'memory_hierarchy': {
                'critical': [],
                'contextual': [],
                'reference': []
            }
        }

        self.save_context()
        self.save_state('initialization', 'started')

        return {
            'action': 'new_project',
            'project_id': project_id,
            'project_name': project_name,
            'status': 'initialized',
            'state_dir': str(self.state_dir),
            'next_steps': [
                'Define project objective',
                'Spawn initial research agents',
                'Create project roadmap'
            ],
            'available_agents': [
                'Researcher',
                'Developer',
                'QA Specialist',
                'DevOps Engineer',
                'Product Strategist'
            ]
        }

    def resume_project(self):
        """
        Resume an existing orchestration project

        Returns:
            Current project state and context
        """
        if not self.context:
            return {
                'error': 'No existing project found',
                'suggestion': 'Use "new" command to start a project'
            }

        recent_activity = self.get_recent_activity()

        # Update last resumed timestamp
        self.context['last_resumed'] = self.current_session
        self.save_context()

        return {
            'action': 'resume_project',
            'project_id': self.context.get('project_id'),
            'project_name': self.context.get('project_name'),
            'current_phase': self.context.get('phase'),
            'status': self.context.get('status'),
            'objectives': self.context.get('objectives', []),
            'active_agents': self.context.get('active_agents', []),
            'active_tasks': [t for t in self.context.get('tasks', []) if t.get('status') != 'completed'],
            'recent_activity': recent_activity,
            'memory_summary': self.get_memory_summary(),
            'next_actions': [
                'Review current state',
                'Check agent progress',
                'Spawn new agents if needed',
                'Update project trajectory'
            ]
        }

    def get_project_status(self):
        """
        Get current project status

        Returns:
            Detailed project status
        """
        if not self.context:
            return {
                'error': 'No active project',
                'suggestion': 'Use "new" command to start a project'
            }

        # Calculate project metrics
        days_active = 0
        if 'start_date' in self.context:
            start = datetime.fromisoformat(self.context['start_date'])
            days_active = (datetime.now() - start).days

        active_tasks = [t for t in self.context.get('tasks', []) if t.get('status') != 'completed']
        completed_tasks = [t for t in self.context.get('tasks', []) if t.get('status') == 'completed']

        return {
            'action': 'status',
            'project_id': self.context.get('project_id'),
            'project_name': self.context.get('project_name'),
            'phase': self.context.get('phase'),
            'status': self.context.get('status'),
            'days_active': days_active,
            'objectives': {
                'total': len(self.context.get('objectives', [])),
                'list': self.context.get('objectives', [])
            },
            'agents': {
                'active': len(self.context.get('active_agents', [])),
                'list': self.context.get('active_agents', [])
            },
            'tasks': {
                'active': len(active_tasks),
                'completed': len(completed_tasks),
                'active_list': active_tasks,
                'recent_completed': completed_tasks[-3:] if completed_tasks else []
            },
            'phases': {
                'completed': len(self.context.get('completed_phases', [])),
                'list': self.context.get('completed_phases', [])
            },
            'insights': {
                'total': len(self.context.get('insights', [])),
                'recent': self.context.get('insights', [])[-3:] if self.context.get('insights') else []
            }
        }

    def set_objective(self, objective_text):
        """
        Set or add a project objective

        Args:
            objective_text: The objective to add

        Returns:
            Updated objectives list
        """
        if not self.context:
            return {
                'error': 'No active project',
                'suggestion': 'Start a project first with "new" command'
            }

        objective = {
            'id': f"OBJ{len(self.context.get('objectives', [])) + 1:03d}",
            'text': objective_text,
            'created': self.current_session,
            'status': 'active'
        }

        if 'objectives' not in self.context:
            self.context['objectives'] = []

        self.context['objectives'].append(objective)
        self.save_context()

        return {
            'action': 'set_objective',
            'objective': objective,
            'total_objectives': len(self.context['objectives']),
            'all_objectives': self.context['objectives']
        }

    def complete_phase(self, phase_name=None, evidence=None):
        """
        Mark a phase as complete

        Args:
            phase_name: Name of the phase to complete
            evidence: Evidence of completion

        Returns:
            Phase completion details
        """
        if not self.context:
            return {
                'error': 'No active project',
                'suggestion': 'Start a project first with "new" command'
            }

        phase_name = phase_name or self.context.get('phase', 'unknown')

        completion = {
            'phase': phase_name,
            'completed_at': self.current_session,
            'evidence': evidence or 'Not provided'
        }

        if 'completed_phases' not in self.context:
            self.context['completed_phases'] = []

        self.context['completed_phases'].append(completion)

        # Determine next phase
        phase_sequence = {
            'initialization': 'research',
            'research': 'story_creation',
            'story_creation': 'development',
            'development': 'testing',
            'testing': 'deployment',
            'deployment': 'monitoring',
            'monitoring': 'optimization'
        }

        next_phase = phase_sequence.get(phase_name, 'unknown')
        self.context['phase'] = next_phase

        self.save_context()
        self.save_state(phase_name, 'completed', {'evidence': evidence})

        return {
            'action': 'complete_phase',
            'completed_phase': phase_name,
            'next_phase': next_phase,
            'total_completed': len(self.context['completed_phases']),
            'completion_record': completion
        }

    def add_insight(self, insight_text, category='general'):
        """
        Add an insight or learning

        Args:
            insight_text: The insight to record
            category: Category of insight (general, technical, process, critical)

        Returns:
            Updated insights
        """
        if not self.context:
            return {
                'error': 'No active project',
                'suggestion': 'Start a project first with "new" command'
            }

        insight = {
            'text': insight_text,
            'category': category,
            'timestamp': self.current_session
        }

        if 'insights' not in self.context:
            self.context['insights'] = []

        self.context['insights'].append(insight)

        # Also add to memory hierarchy based on category
        if category == 'critical':
            self.context['memory_hierarchy']['critical'].append(insight_text)
        elif category == 'technical':
            self.context['memory_hierarchy']['contextual'].append(insight_text)
        else:
            self.context['memory_hierarchy']['reference'].append(insight_text)

        self.save_context()

        return {
            'action': 'add_insight',
            'insight': insight,
            'total_insights': len(self.context['insights'])
        }

    def get_memory_summary(self):
        """Get hierarchical memory summary"""
        if not self.context:
            return {'error': 'No active project'}

        memory = self.context.get('memory_hierarchy', {})
        return {
            'critical_memories': memory.get('critical', [])[-3:],
            'contextual_memories': memory.get('contextual', [])[-5:],
            'reference_count': len(memory.get('reference', [])),
            'total_insights': len(self.context.get('insights', []))
        }

    def list_active_agents(self):
        """
        List all active agents

        Returns:
            Active agents list
        """
        if not self.context:
            return {'error': 'No active project'}

        return {
            'active_agents': self.context.get('active_agents', []),
            'count': len(self.context.get('active_agents', []))
        }

    def spawn_agent(self, agent_type, mission):
        """
        Record spawning of an agent

        Args:
            agent_type: Type of agent (Researcher, Developer, etc.)
            mission: Mission description for the agent

        Returns:
            Agent spawn details
        """
        if not self.context:
            return {
                'error': 'No active project',
                'suggestion': 'Start a project first with "new" command'
            }

        agent = {
            'id': f"AGT{len(self.context.get('active_agents', [])) + 1:03d}",
            'type': agent_type,
            'mission': mission,
            'spawned_at': self.current_session,
            'status': 'active'
        }

        if 'active_agents' not in self.context:
            self.context['active_agents'] = []

        self.context['active_agents'].append(agent)
        self.save_context()

        return {
            'action': 'spawn_agent',
            'agent': agent,
            'total_active_agents': len(self.context['active_agents'])
        }

    def add_task(self, task_description, assigned_to=None):
        """
        Add a task to track

        Args:
            task_description: Description of the task
            assigned_to: Agent ID assigned to this task

        Returns:
            Task creation details
        """
        if not self.context:
            return {
                'error': 'No active project',
                'suggestion': 'Start a project first with "new" command'
            }

        task = {
            'id': f"TSK{len(self.context.get('tasks', [])) + 1:03d}",
            'description': task_description,
            'assigned_to': assigned_to,
            'created_at': self.current_session,
            'status': 'pending'
        }

        if 'tasks' not in self.context:
            self.context['tasks'] = []

        self.context['tasks'].append(task)
        self.save_context()

        return {
            'action': 'add_task',
            'task': task,
            'total_tasks': len(self.context['tasks'])
        }

def main():
    """
    Entry point when Claude runs this script

    Usage:
        python 00_orchestrator_context.py new [project_name]
        python 00_orchestrator_context.py resume
        python 00_orchestrator_context.py status
        python 00_orchestrator_context.py objective "Build authentication system"
        python 00_orchestrator_context.py complete [phase_name] [evidence]
        python 00_orchestrator_context.py insight "Key learning" [category]
        python 00_orchestrator_context.py memory
        python 00_orchestrator_context.py agents
        python 00_orchestrator_context.py spawn [agent_type] [mission]
        python 00_orchestrator_context.py task "Task description" [assigned_to]
    """
    orchestrator = OrchestratorContext()

    # Parse command line arguments
    args = sys.argv[1:]

    if not args:
        # Return usage information
        usage = {
            'script': '00_orchestrator_context.py',
            'description': 'Orchestrator context manager for long-running projects',
            'commands': [
                {
                    'command': 'new [project_name]',
                    'description': 'Start a new orchestration project'
                },
                {
                    'command': 'resume',
                    'description': 'Resume existing project'
                },
                {
                    'command': 'status',
                    'description': 'Get current project status'
                },
                {
                    'command': 'objective "text"',
                    'description': 'Set project objective'
                },
                {
                    'command': 'complete [phase] [evidence]',
                    'description': 'Mark phase as complete'
                },
                {
                    'command': 'insight "text" [category]',
                    'description': 'Record an insight'
                },
                {
                    'command': 'memory',
                    'description': 'Get memory summary'
                },
                {
                    'command': 'agents',
                    'description': 'List active agents'
                },
                {
                    'command': 'spawn [type] [mission]',
                    'description': 'Record agent spawn'
                },
                {
                    'command': 'task "description" [assigned_to]',
                    'description': 'Add a task'
                }
            ]
        }
        print(json.dumps(usage, indent=2))
        return

    command = args[0]
    remaining_args = args[1:]

    # Execute command and print results
    result = orchestrator.execute_command(command, *remaining_args)
    print(result)

if __name__ == "__main__":
    main()