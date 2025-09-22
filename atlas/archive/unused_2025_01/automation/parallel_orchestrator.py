#!/usr/bin/env python3
"""
Atlas Framework - Parallel Execution Orchestrator
Manages parallel execution of tasks with simulated agents
"""

import json
import os
import sys
import time
import random
import argparse
import threading
import queue
from typing import Dict, List, Optional, Set
from dataclasses import dataclass
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timedelta

# Import Atlas core
from atlas import AtlasOrchestrator, Task, TaskStatus

@dataclass
class Agent:
    id: str
    type: str
    name: str
    status: str
    current_task: Optional[str] = None
    trust_score: float = 0.8
    tasks_completed: int = 0

class ParallelOrchestrator:
    def __init__(self, max_agents: int = 3):
        self.max_agents = max_agents
        self.atlas = AtlasOrchestrator()
        self.agents = self._initialize_agents()
        self.execution_log = []
        self.task_queue = queue.Queue()
        self.results = {}

    def _initialize_agents(self) -> Dict[str, Agent]:
        """Initialize agent pool"""
        agents = {
            "backend-1": Agent(
                id="backend-1",
                type="backend",
                name="Backend Developer Agent 1",
                status="idle",
                trust_score=0.85
            ),
            "backend-2": Agent(
                id="backend-2",
                type="backend",
                name="Backend Developer Agent 2",
                status="idle",
                trust_score=0.85
            ),
            "ui-1": Agent(
                id="ui-1",
                type="ui",
                name="UI Developer Agent 1",
                status="idle",
                trust_score=0.80
            ),
            "ui-2": Agent(
                id="ui-2",
                type="ui",
                name="UI Developer Agent 2",
                status="idle",
                trust_score=0.80
            ),
            "performance-1": Agent(
                id="performance-1",
                type="performance",
                name="Performance Reviewer Agent",
                status="idle",
                trust_score=0.90
            ),
            "security-1": Agent(
                id="security-1",
                type="security",
                name="Security Reviewer Agent",
                status="idle",
                trust_score=0.95
            )
        }
        return agents

    def execute_wave(self, workflow_id: str, wave: int, max_agents: Optional[int] = None) -> bool:
        """Execute all tasks in a wave with parallel agents"""
        if workflow_id not in self.atlas.workflows:
            print(f"❌ Workflow {workflow_id} not found")
            return False

        workflow = self.atlas.workflows[workflow_id]
        wave_tasks = self._get_executable_tasks(workflow, wave)

        if not wave_tasks:
            print(f"❌ No executable tasks found for wave {wave}")
            return False

        max_agents = max_agents or self.max_agents

        print(f"\n🚀 Executing Wave {wave}")
        print(f"   Tasks: {len(wave_tasks)}")
        print(f"   Max Parallel Agents: {max_agents}")
        print("=" * 60)

        # Group tasks by dependency levels
        task_levels = self._group_tasks_by_dependencies(wave_tasks, workflow)

        for level, level_tasks in enumerate(task_levels):
            print(f"\n📊 Dependency Level {level + 1}")
            print(f"   Tasks in parallel: {', '.join([t.id for t in level_tasks])}")

            # Execute tasks at this level in parallel
            with ThreadPoolExecutor(max_workers=min(max_agents, len(level_tasks))) as executor:
                futures = {}

                for task in level_tasks:
                    # Find available agent of correct type
                    agent = self._get_available_agent(task.agent_type)
                    if agent:
                        print(f"   🤖 Assigning {task.id} to {agent.name}")
                        future = executor.submit(self._execute_task, workflow_id, task, agent)
                        futures[future] = (task, agent)
                    else:
                        print(f"   ⚠️ No available agent for {task.id} ({task.agent_type})")

                # Wait for completion
                for future in as_completed(futures):
                    task, agent = futures[future]
                    try:
                        result = future.result()
                        if result:
                            print(f"   ✅ {task.id}: {task.name} completed by {agent.name}")
                        else:
                            print(f"   ❌ {task.id}: {task.name} failed")
                    except Exception as e:
                        print(f"   ❌ {task.id} exception: {e}")
                    finally:
                        # Release agent
                        agent.status = "idle"
                        agent.current_task = None

        print(f"\n✅ Wave {wave} execution complete")
        self._generate_wave_report(workflow_id, wave)
        return True

    def _get_executable_tasks(self, workflow, wave: int) -> List[Task]:
        """Get tasks that can be executed in this wave"""
        wave_mapping = {
            1: ["1.1", "1.2", "1.3"],
            2: ["2.1", "2.2", "2.3"],
            3: ["3.1", "3.2", "3.3"],
            4: ["4.1", "4.2", "4.3"],
            5: ["5.1", "5.2", "5.3"],
            6: ["6.1", "6.2", "6.3"]
        }

        task_ids = wave_mapping.get(wave, [])
        tasks = []

        for task_id in task_ids:
            if task_id in workflow.tasks:
                task = workflow.tasks[task_id]
                if task.status == TaskStatus.PENDING:
                    # Check if dependencies are met
                    deps_met = all(
                        workflow.tasks[dep].status == TaskStatus.COMPLETED
                        for dep in task.dependencies
                        if dep in workflow.tasks
                    )
                    if deps_met:
                        tasks.append(task)

        return tasks

    def _group_tasks_by_dependencies(self, tasks: List[Task], workflow) -> List[List[Task]]:
        """Group tasks into levels based on dependencies"""
        levels = []
        remaining_tasks = tasks.copy()
        completed_ids = {t_id for t_id, t in workflow.tasks.items()
                        if t.status == TaskStatus.COMPLETED}

        while remaining_tasks:
            # Find tasks whose dependencies are all completed
            current_level = []
            for task in remaining_tasks:
                if all(dep in completed_ids for dep in task.dependencies):
                    current_level.append(task)

            if not current_level:
                # No progress possible - circular dependency or blocked
                print("⚠️ Warning: Some tasks have unmet dependencies")
                break

            levels.append(current_level)

            # Mark current level as completed for next iteration
            for task in current_level:
                completed_ids.add(task.id)
                remaining_tasks.remove(task)

        return levels

    def _get_available_agent(self, agent_type: str) -> Optional[Agent]:
        """Get an available agent of the specified type"""
        for agent in self.agents.values():
            if agent.type == agent_type and agent.status == "idle":
                return agent
        return None

    def _execute_task(self, workflow_id: str, task: Task, agent: Agent) -> bool:
        """Execute a single task with an agent (simulated)"""
        start_time = datetime.now()

        # Mark agent as busy
        agent.status = "working"
        agent.current_task = task.id

        # Update task status
        task.status = TaskStatus.IN_PROGRESS
        task.started_at = start_time.isoformat()

        # Log execution start
        self.execution_log.append({
            "timestamp": start_time.isoformat(),
            "event": "task_started",
            "task_id": task.id,
            "agent_id": agent.id,
            "agent_name": agent.name
        })

        # Simulate task execution
        self._simulate_task_execution(task, agent)

        # Generate evidence
        evidence_path = self._generate_task_evidence(workflow_id, task, agent)

        # Complete task
        end_time = datetime.now()
        task.status = TaskStatus.COMPLETED
        task.completed_at = end_time.isoformat()
        task.evidence_path = evidence_path

        # Update agent stats
        agent.tasks_completed += 1

        # Log completion
        self.execution_log.append({
            "timestamp": end_time.isoformat(),
            "event": "task_completed",
            "task_id": task.id,
            "agent_id": agent.id,
            "duration_seconds": (end_time - start_time).total_seconds(),
            "evidence_path": evidence_path
        })

        # Save workflow state
        self.atlas._save_workflows()

        return True

    def _simulate_task_execution(self, task: Task, agent: Agent):
        """Simulate task execution with realistic timing"""
        # Simulate work time (scaled down for demo)
        work_time = min(task.duration_hours * 2, 10)  # 2 seconds per hour, max 10 seconds
        time.sleep(work_time)

        # Simulate progress updates
        progress_steps = ["Analyzing requirements", "Implementing solution",
                         "Testing implementation", "Generating documentation"]

        for i, step in enumerate(progress_steps):
            progress = (i + 1) / len(progress_steps) * 100
            print(f"     📝 {task.id}: {step} ({progress:.0f}%)")
            time.sleep(0.5)

    def _generate_task_evidence(self, workflow_id: str, task: Task, agent: Agent) -> str:
        """Generate evidence artifacts for completed task"""
        evidence_dir = os.path.join(
            self.atlas.evidence_dir,
            workflow_id,
            f"wave-{task.id.split('.')[0]}",
            f"task-{task.id}"
        )
        os.makedirs(evidence_dir, exist_ok=True)

        # Generate task completion report
        report = {
            "task_id": task.id,
            "task_name": task.name,
            "agent": {
                "id": agent.id,
                "name": agent.name,
                "type": agent.type,
                "trust_score": agent.trust_score
            },
            "status": "completed",
            "started_at": task.started_at,
            "completed_at": task.completed_at,
            "deliverables": task.deliverables,
            "evidence_files": []
        }

        # Simulate evidence files based on task type
        if "build" in task.name.lower():
            report["evidence_files"].append("build-log.txt")
            report["evidence_files"].append("dependency-tree.txt")
        elif "model" in task.name.lower() or "data" in task.name.lower():
            report["evidence_files"].append("entity-definitions.kt")
            report["evidence_files"].append("repository-interfaces.kt")
        elif "theme" in task.name.lower():
            report["evidence_files"].append("theme-screenshots.png")
            report["evidence_files"].append("color-palette.xml")
        elif "storage" in task.name.lower():
            report["evidence_files"].append("database-schema.sql")
            report["evidence_files"].append("dao-implementation.kt")

        # Write report
        report_file = os.path.join(evidence_dir, "completion-report.json")
        with open(report_file, 'w') as f:
            json.dump(report, f, indent=2)

        return evidence_dir

    def _generate_wave_report(self, workflow_id: str, wave: int):
        """Generate summary report for wave completion"""
        workflow = self.atlas.workflows[workflow_id]
        wave_tasks = [t for t_id, t in workflow.tasks.items()
                     if t_id.startswith(f"{wave}.")]

        completed = sum(1 for t in wave_tasks if t.status == TaskStatus.COMPLETED)
        total = len(wave_tasks)

        print(f"\n📋 Wave {wave} Summary Report")
        print("=" * 40)
        print(f"   Workflow: {workflow_id}")
        print(f"   Wave: {wave}")
        print(f"   Tasks Completed: {completed}/{total}")

        # Agent performance
        print(f"\n   Agent Performance:")
        for agent in self.agents.values():
            if agent.tasks_completed > 0:
                print(f"   - {agent.name}: {agent.tasks_completed} tasks")

        # Save execution log
        log_file = os.path.join(
            self.atlas.evidence_dir,
            workflow_id,
            f"wave-{wave}-execution.json"
        )
        os.makedirs(os.path.dirname(log_file), exist_ok=True)

        with open(log_file, 'w') as f:
            json.dump(self.execution_log, f, indent=2)

        print(f"\n   Execution log saved to: {log_file}")

    def execute_tasks(self, workflow_id: str, task_ids: List[str],
                     sequential: bool = False, max_agents: Optional[int] = None) -> bool:
        """Execute specific tasks"""
        if workflow_id not in self.atlas.workflows:
            print(f"❌ Workflow {workflow_id} not found")
            return False

        workflow = self.atlas.workflows[workflow_id]
        tasks = [workflow.tasks[t_id] for t_id in task_ids if t_id in workflow.tasks]

        if not tasks:
            print(f"❌ No valid tasks found")
            return False

        max_agents = 1 if sequential else (max_agents or self.max_agents)

        print(f"\n🚀 Executing Tasks: {', '.join(task_ids)}")
        print(f"   Mode: {'Sequential' if sequential else 'Parallel'}")
        print(f"   Max Agents: {max_agents}")

        if sequential:
            # Execute tasks one by one
            for task in tasks:
                agent = self._get_available_agent(task.agent_type)
                if agent:
                    print(f"\n🤖 Executing {task.id} with {agent.name}")
                    self._execute_task(workflow_id, task, agent)
                    agent.status = "idle"
                else:
                    print(f"❌ No available agent for {task.id}")
        else:
            # Execute in parallel (already implemented above)
            # Group by dependencies and execute
            task_levels = self._group_tasks_by_dependencies(tasks, workflow)
            for level_tasks in task_levels:
                with ThreadPoolExecutor(max_workers=min(max_agents, len(level_tasks))) as executor:
                    futures = {}
                    for task in level_tasks:
                        agent = self._get_available_agent(task.agent_type)
                        if agent:
                            future = executor.submit(self._execute_task, workflow_id, task, agent)
                            futures[future] = (task, agent)

                    for future in as_completed(futures):
                        task, agent = futures[future]
                        future.result()
                        agent.status = "idle"

        return True

    def get_status(self, workflow_id: str, wave: Optional[int] = None):
        """Get execution status"""
        if workflow_id not in self.atlas.workflows:
            print(f"❌ Workflow {workflow_id} not found")
            return

        workflow = self.atlas.workflows[workflow_id]

        print(f"\n📊 Parallel Execution Status")
        print(f"   Workflow: {workflow_id}")

        if wave:
            print(f"   Wave: {wave}")

        print(f"\n   Agent Status:")
        for agent in self.agents.values():
            status_icon = "🔄" if agent.status == "working" else "✅"
            task_info = f" (Task: {agent.current_task})" if agent.current_task else ""
            print(f"   {status_icon} {agent.name}: {agent.status}{task_info}")

        # Show execution history
        if self.execution_log:
            print(f"\n   Recent Execution Events:")
            for event in self.execution_log[-5:]:
                print(f"   - [{event['timestamp'][:19]}] {event['event']}: {event.get('task_id', 'N/A')}")

def main():
    parser = argparse.ArgumentParser(description="Atlas Parallel Execution Orchestrator")

    subparsers = parser.add_subparsers(dest='command', help='Commands')

    # Execute command
    execute_parser = subparsers.add_parser('execute', help='Execute tasks')
    execute_parser.add_argument('workflow_id', help='Workflow ID')
    execute_parser.add_argument('--wave', type=int, help='Execute entire wave')
    execute_parser.add_argument('--task', help='Execute specific task')
    execute_parser.add_argument('--tasks', help='Execute multiple tasks (comma-separated)')
    execute_parser.add_argument('--max-agents', type=int, default=3, help='Maximum parallel agents')
    execute_parser.add_argument('--sequential', action='store_true', help='Execute sequentially')

    # Status command
    status_parser = subparsers.add_parser('status', help='Get execution status')
    status_parser.add_argument('workflow_id', help='Workflow ID')
    status_parser.add_argument('--wave', type=int, help='Wave number')

    args = parser.parse_args()

    orchestrator = ParallelOrchestrator(max_agents=args.max_agents if hasattr(args, 'max_agents') else 3)

    if args.command == 'execute':
        if args.wave:
            orchestrator.execute_wave(args.workflow_id, args.wave, args.max_agents)
        elif args.task:
            orchestrator.execute_tasks(args.workflow_id, [args.task],
                                     args.sequential, args.max_agents)
        elif args.tasks:
            task_list = args.tasks.split(',')
            orchestrator.execute_tasks(args.workflow_id, task_list,
                                     args.sequential, args.max_agents)
        else:
            print("❌ Must specify --wave, --task, or --tasks")

    elif args.command == 'status':
        orchestrator.get_status(args.workflow_id, args.wave)

    else:
        parser.print_help()

if __name__ == "__main__":
    main()