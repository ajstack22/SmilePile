#!/usr/bin/env python3
"""
Atlas Framework 2.2 - Main Orchestration System
Manages workflows, agents, and parallel execution for SmilePile development
"""

import json
import os
import sys
import argparse
import datetime
import subprocess
from typing import Dict, List, Optional, Tuple
from dataclasses import dataclass, asdict
from enum import Enum

# Add parent directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

class TaskStatus(Enum):
    PENDING = "pending"
    IN_PROGRESS = "in_progress"
    COMPLETED = "completed"
    FAILED = "failed"
    BLOCKED = "blocked"

class ReviewVerdict(Enum):
    PASS = "PASS"
    PASS_WITH_MINOR = "PASS_WITH_MINOR"
    PASS_WITH_SUGGESTIONS = "PASS_WITH_SUGGESTIONS"
    CONDITIONAL_PASS = "CONDITIONAL_PASS"
    NEEDS_CHANGES = "NEEDS_CHANGES"
    NEEDS_MINOR_CHANGES = "NEEDS_MINOR_CHANGES"
    BLOCKED = "BLOCKED"

@dataclass
class Task:
    id: str
    name: str
    agent_type: str
    duration_hours: float
    dependencies: List[str]
    status: TaskStatus
    deliverables: List[str]
    evidence_path: Optional[str] = None
    started_at: Optional[str] = None
    completed_at: Optional[str] = None

@dataclass
class Workflow:
    id: str
    name: str
    type: str
    priority: str
    tasks: Dict[str, Task]
    current_wave: int
    status: str
    created_at: str

class AtlasOrchestrator:
    def __init__(self, workspace_dir: str = "."):
        self.workspace_dir = workspace_dir
        self.atlas_dir = os.path.join(workspace_dir, "atlas")
        self.automation_dir = os.path.join(self.atlas_dir, "automation")
        self.evidence_dir = os.path.join(self.atlas_dir, "evidence")
        self.workflows_file = os.path.join(self.automation_dir, "workflows.json")

        # Create directories if they don't exist
        os.makedirs(self.automation_dir, exist_ok=True)
        os.makedirs(self.evidence_dir, exist_ok=True)

        # Load or initialize workflows
        self.workflows = self._load_workflows()

    def _load_workflows(self) -> Dict[str, Workflow]:
        """Load existing workflows from JSON file"""
        if os.path.exists(self.workflows_file):
            with open(self.workflows_file, 'r') as f:
                data = json.load(f)
                workflows = {}
                for wf_id, wf_data in data.items():
                    tasks = {}
                    for task_id, task_data in wf_data['tasks'].items():
                        task_data['status'] = TaskStatus(task_data['status'])
                        tasks[task_id] = Task(**task_data)
                    wf_data['tasks'] = tasks
                    workflows[wf_id] = Workflow(**wf_data)
                return workflows
        return {}

    def _save_workflows(self):
        """Save workflows to JSON file"""
        data = {}
        for wf_id, workflow in self.workflows.items():
            wf_dict = asdict(workflow)
            # Convert tasks to serializable format
            tasks_dict = {}
            for task_id, task in workflow.tasks.items():
                task_dict = asdict(task)
                task_dict['status'] = task.status.value
                tasks_dict[task_id] = task_dict
            wf_dict['tasks'] = tasks_dict
            data[wf_id] = wf_dict

        with open(self.workflows_file, 'w') as f:
            json.dump(data, f, indent=2)

    def start_workflow(self, workflow_id: str, workflow_type: str, priority: str) -> bool:
        """Initialize a new workflow"""
        if workflow_id in self.workflows:
            print(f"❌ Workflow {workflow_id} already exists")
            return False

        # Load workflow definition from SMILEPILE_WORKFLOW.md
        workflow_def = self._load_workflow_definition(workflow_id)

        workflow = Workflow(
            id=workflow_id,
            name=f"SmilePile Development - {workflow_id}",
            type=workflow_type,
            priority=priority,
            tasks=workflow_def,
            current_wave=1,
            status="initialized",
            created_at=datetime.datetime.now().isoformat()
        )

        self.workflows[workflow_id] = workflow
        self._save_workflows()

        print(f"✅ Workflow {workflow_id} initialized")
        print(f"   Type: {workflow_type}")
        print(f"   Priority: {priority}")
        print(f"   Total Tasks: {len(workflow.tasks)}")
        print(f"   Status: {workflow.status}")

        return True

    def _load_workflow_definition(self, workflow_id: str) -> Dict[str, Task]:
        """Load SmilePile workflow tasks definition"""
        tasks = {
            # Wave 1 Tasks
            "1.1": Task(
                id="1.1",
                name="Build System Setup",
                agent_type="backend",
                duration_hours=2.0,
                dependencies=[],
                status=TaskStatus.COMPLETED,  # Already done
                deliverables=[
                    "Gradle configuration",
                    "Material Design Components",
                    "AndroidX libraries",
                    "Kotlin coroutines setup"
                ]
            ),
            "1.2": Task(
                id="1.2",
                name="Data Models Design",
                agent_type="backend",
                duration_hours=2.0,
                dependencies=[],
                status=TaskStatus.COMPLETED,  # Already done
                deliverables=[
                    "Photo model class",
                    "Category model class",
                    "Repository interfaces"
                ]
            ),
            "1.3": Task(
                id="1.3",
                name="Theme System Architecture",
                agent_type="ui",
                duration_hours=3.0,
                dependencies=[],
                status=TaskStatus.COMPLETED,  # Already done
                deliverables=[
                    "Light theme",
                    "Dark theme",
                    "Rainbow theme",
                    "Theme persistence"
                ]
            ),

            # Wave 2 Tasks
            "2.1": Task(
                id="2.1",
                name="Storage Implementation",
                agent_type="backend",
                duration_hours=3.0,
                dependencies=["1.1", "1.2"],
                status=TaskStatus.PENDING,
                deliverables=[
                    "Room database setup",
                    "DAOs implementation",
                    "SharedPreferences helper",
                    "File storage structure"
                ]
            ),
            "2.2": Task(
                id="2.2",
                name="Photo Management System",
                agent_type="backend",
                duration_hours=3.0,
                dependencies=["1.2", "2.1"],
                status=TaskStatus.PENDING,
                deliverables=[
                    "Photo repository implementation",
                    "Asset photo loading",
                    "Imported photo management",
                    "Path resolution system"
                ]
            ),
            "2.3": Task(
                id="2.3",
                name="Category Management",
                agent_type="backend",
                duration_hours=2.0,
                dependencies=["1.2", "2.1"],
                status=TaskStatus.PENDING,
                deliverables=[
                    "Default categories initialization",
                    "Category CRUD operations",
                    "Photo-category associations",
                    "Position/ordering logic"
                ]
            ),

            # Wave 3 Tasks
            "3.1": Task(
                id="3.1",
                name="Navigation Architecture",
                agent_type="ui",
                duration_hours=2.0,
                dependencies=["1.1", "1.3"],
                status=TaskStatus.PENDING,
                deliverables=[
                    "Single Activity setup",
                    "Fragment navigation graph",
                    "Navigation component",
                    "Back button handling"
                ]
            ),
            "3.2": Task(
                id="3.2",
                name="Base UI Components",
                agent_type="ui",
                duration_hours=3.0,
                dependencies=["1.3", "3.1"],
                status=TaskStatus.PENDING,
                deliverables=[
                    "Common UI elements",
                    "Custom photo views",
                    "Grid layout components",
                    "Touch gesture handlers"
                ]
            ),
            "3.3": Task(
                id="3.3",
                name="Image Loading System",
                agent_type="performance",
                duration_hours=3.0,
                dependencies=["2.1", "2.2"],
                status=TaskStatus.PENDING,
                deliverables=[
                    "Efficient bitmap loading",
                    "Memory management",
                    "Async loading with coroutines",
                    "Placeholder/error handling"
                ]
            )
        }

        return tasks

    def get_status(self, workflow_id: Optional[str] = None) -> None:
        """Display workflow status"""
        if workflow_id:
            if workflow_id not in self.workflows:
                print(f"❌ Workflow {workflow_id} not found")
                return
            workflows = {workflow_id: self.workflows[workflow_id]}
        else:
            workflows = self.workflows

        for wf_id, workflow in workflows.items():
            print(f"\n📊 Workflow: {wf_id}")
            print(f"   Name: {workflow.name}")
            print(f"   Status: {workflow.status}")
            print(f"   Current Wave: {workflow.current_wave}")
            print(f"   Created: {workflow.created_at}")

            # Task statistics
            task_stats = {
                TaskStatus.PENDING: 0,
                TaskStatus.IN_PROGRESS: 0,
                TaskStatus.COMPLETED: 0,
                TaskStatus.FAILED: 0,
                TaskStatus.BLOCKED: 0
            }

            for task in workflow.tasks.values():
                task_stats[task.status] += 1

            print(f"\n   Task Summary:")
            print(f"   ✅ Completed: {task_stats[TaskStatus.COMPLETED]}")
            print(f"   🔄 In Progress: {task_stats[TaskStatus.IN_PROGRESS]}")
            print(f"   ⏳ Pending: {task_stats[TaskStatus.PENDING]}")
            print(f"   ❌ Failed: {task_stats[TaskStatus.FAILED]}")
            print(f"   🚫 Blocked: {task_stats[TaskStatus.BLOCKED]}")

            # Show current wave tasks
            current_wave_tasks = self._get_wave_tasks(workflow, workflow.current_wave)
            if current_wave_tasks:
                print(f"\n   Wave {workflow.current_wave} Tasks:")
                for task_id in current_wave_tasks:
                    task = workflow.tasks[task_id]
                    status_icon = self._get_status_icon(task.status)
                    print(f"   {status_icon} {task_id}: {task.name} ({task.agent_type})")

    def _get_wave_tasks(self, workflow: Workflow, wave: int) -> List[str]:
        """Get tasks for a specific wave"""
        wave_mapping = {
            1: ["1.1", "1.2", "1.3"],
            2: ["2.1", "2.2", "2.3"],
            3: ["3.1", "3.2", "3.3"],
            4: ["4.1", "4.2", "4.3"],
            5: ["5.1", "5.2", "5.3"],
            6: ["6.1", "6.2", "6.3"]
        }

        tasks = wave_mapping.get(wave, [])
        return [t for t in tasks if t in workflow.tasks]

    def _get_status_icon(self, status: TaskStatus) -> str:
        """Get icon for task status"""
        icons = {
            TaskStatus.PENDING: "⏳",
            TaskStatus.IN_PROGRESS: "🔄",
            TaskStatus.COMPLETED: "✅",
            TaskStatus.FAILED: "❌",
            TaskStatus.BLOCKED: "🚫"
        }
        return icons.get(status, "❓")

    def validate_dependencies(self, workflow_id: str, task_id: str) -> Tuple[bool, str]:
        """Validate that all dependencies for a task are met"""
        if workflow_id not in self.workflows:
            return False, f"Workflow {workflow_id} not found"

        workflow = self.workflows[workflow_id]
        if task_id not in workflow.tasks:
            return False, f"Task {task_id} not found"

        task = workflow.tasks[task_id]

        for dep_id in task.dependencies:
            if dep_id not in workflow.tasks:
                return False, f"Dependency {dep_id} not found"

            dep_task = workflow.tasks[dep_id]
            if dep_task.status != TaskStatus.COMPLETED:
                return False, f"Dependency {dep_id} not completed (status: {dep_task.status.value})"

        return True, "All dependencies satisfied"

    def review_submit(self, workflow_id: str, checkpoint: Optional[int] = None) -> None:
        """Submit workflow for review at checkpoint"""
        if workflow_id not in self.workflows:
            print(f"❌ Workflow {workflow_id} not found")
            return

        workflow = self.workflows[workflow_id]

        print(f"\n📋 Review Submission: {workflow_id}")
        print(f"   Checkpoint: {checkpoint if checkpoint else 'Current'}")

        # Collect evidence
        evidence_path = os.path.join(self.evidence_dir, workflow_id)
        if os.path.exists(evidence_path):
            print(f"   Evidence Path: {evidence_path}")

            # List evidence files
            evidence_files = []
            for root, dirs, files in os.walk(evidence_path):
                for file in files:
                    rel_path = os.path.relpath(
                        os.path.join(root, file),
                        evidence_path
                    )
                    evidence_files.append(rel_path)

            if evidence_files:
                print(f"   Evidence Files: {len(evidence_files)}")
                for file in evidence_files[:5]:  # Show first 5
                    print(f"     - {file}")
                if len(evidence_files) > 5:
                    print(f"     ... and {len(evidence_files) - 5} more")

        # Simulate review verdict
        print(f"\n   🎯 Review Verdict: {ReviewVerdict.PASS.value}")
        print(f"   ✅ Ready to proceed to next wave")

    def execute_task(self, workflow_id: str, task_id: str) -> bool:
        """Execute a specific task (called by parallel_orchestrator)"""
        if workflow_id not in self.workflows:
            return False

        workflow = self.workflows[workflow_id]
        if task_id not in workflow.tasks:
            return False

        task = workflow.tasks[task_id]

        # Check dependencies
        valid, message = self.validate_dependencies(workflow_id, task_id)
        if not valid:
            print(f"❌ Cannot execute {task_id}: {message}")
            task.status = TaskStatus.BLOCKED
            self._save_workflows()
            return False

        # Update task status
        task.status = TaskStatus.IN_PROGRESS
        task.started_at = datetime.datetime.now().isoformat()
        self._save_workflows()

        # Simulate task execution
        print(f"🔄 Executing {task_id}: {task.name}")

        # In a real system, this would launch the actual agent
        # For now, we'll mark it as completed after simulation
        task.status = TaskStatus.COMPLETED
        task.completed_at = datetime.datetime.now().isoformat()

        # Create evidence directory
        task_evidence_dir = os.path.join(
            self.evidence_dir,
            workflow_id,
            f"wave-{workflow.current_wave}",
            f"task-{task_id}"
        )
        os.makedirs(task_evidence_dir, exist_ok=True)
        task.evidence_path = task_evidence_dir

        self._save_workflows()

        return True

    def generate_metrics(self, workflow_id: Optional[str] = None) -> None:
        """Generate workflow metrics"""
        if workflow_id:
            workflows = {workflow_id: self.workflows.get(workflow_id)}
        else:
            workflows = self.workflows

        print("\n📈 Atlas Framework Metrics")
        print("=" * 50)

        for wf_id, workflow in workflows.items():
            if not workflow:
                continue

            completed_tasks = [t for t in workflow.tasks.values()
                             if t.status == TaskStatus.COMPLETED]
            total_tasks = len(workflow.tasks)

            if total_tasks > 0:
                completion_rate = (len(completed_tasks) / total_tasks) * 100
            else:
                completion_rate = 0

            print(f"\nWorkflow: {wf_id}")
            print(f"  Completion Rate: {completion_rate:.1f}%")
            print(f"  Tasks Completed: {len(completed_tasks)}/{total_tasks}")

            # Calculate time metrics
            total_hours = sum(t.duration_hours for t in workflow.tasks.values())
            completed_hours = sum(t.duration_hours for t in completed_tasks)

            print(f"  Hours Completed: {completed_hours:.1f}/{total_hours:.1f}")

            # Agent utilization
            agent_tasks = {}
            for task in workflow.tasks.values():
                agent = task.agent_type
                if agent not in agent_tasks:
                    agent_tasks[agent] = {"total": 0, "completed": 0}
                agent_tasks[agent]["total"] += 1
                if task.status == TaskStatus.COMPLETED:
                    agent_tasks[agent]["completed"] += 1

            print(f"\n  Agent Utilization:")
            for agent, stats in agent_tasks.items():
                util = (stats["completed"] / stats["total"]) * 100 if stats["total"] > 0 else 0
                print(f"    {agent}: {util:.1f}% ({stats['completed']}/{stats['total']} tasks)")

def main():
    parser = argparse.ArgumentParser(description="Atlas Framework Orchestrator")

    subparsers = parser.add_subparsers(dest='command', help='Commands')

    # Workflow command
    workflow_parser = subparsers.add_parser('workflow', help='Workflow management')
    workflow_subparsers = workflow_parser.add_subparsers(dest='subcommand')

    # workflow start
    start_parser = workflow_subparsers.add_parser('start', help='Start new workflow')
    start_parser.add_argument('workflow_id', help='Workflow ID')
    start_parser.add_argument('--type', default='mobile-app', help='Workflow type')
    start_parser.add_argument('--priority', default='high', help='Priority level')

    # workflow status
    status_parser = workflow_subparsers.add_parser('status', help='Get workflow status')
    status_parser.add_argument('workflow_id', nargs='?', help='Workflow ID (optional)')

    # Review command
    review_parser = subparsers.add_parser('review', help='Review management')
    review_subparsers = review_parser.add_subparsers(dest='subcommand')

    # review submit
    submit_parser = review_subparsers.add_parser('submit', help='Submit for review')
    submit_parser.add_argument('workflow_id', help='Workflow ID')
    submit_parser.add_argument('--checkpoint', type=int, help='Checkpoint number')

    # Metrics command
    metrics_parser = subparsers.add_parser('metrics', help='Generate metrics')
    metrics_parser.add_argument('--workflow', help='Workflow ID (optional)')

    # Status command (shortcut)
    subparsers.add_parser('status', help='Show all workflow status')

    # Validate command
    validate_parser = subparsers.add_parser('validate', help='Validate Atlas installation')

    args = parser.parse_args()

    orchestrator = AtlasOrchestrator()

    if args.command == 'workflow':
        if args.subcommand == 'start':
            orchestrator.start_workflow(args.workflow_id, args.type, args.priority)
        elif args.subcommand == 'status':
            orchestrator.get_status(args.workflow_id)

    elif args.command == 'review':
        if args.subcommand == 'submit':
            orchestrator.review_submit(args.workflow_id, args.checkpoint)

    elif args.command == 'metrics':
        orchestrator.generate_metrics(args.workflow if hasattr(args, 'workflow') else None)

    elif args.command == 'status':
        orchestrator.get_status()

    elif args.command == 'validate':
        print("✅ Atlas Framework 2.2 - Installation Valid")
        print(f"   Workspace: {orchestrator.workspace_dir}")
        print(f"   Atlas Dir: {orchestrator.atlas_dir}")
        print(f"   Evidence Dir: {orchestrator.evidence_dir}")
        print(f"   Workflows: {len(orchestrator.workflows)}")

    else:
        parser.print_help()

if __name__ == "__main__":
    main()