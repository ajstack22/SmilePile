#!/usr/bin/env python3
"""
Atlas Parallel Orchestrator v2.1
Copyright 2024 Atlas Framework

Orchestrates parallel execution of tasks based on dependency analysis.
Manages agent pools, resource allocation, and execution coordination.
"""

import json
import logging
import datetime
import asyncio
import threading
import queue
import time
from typing import Dict, List, Set, Optional, Any, Callable, Coroutine
from dataclasses import dataclass, asdict
from pathlib import Path
from enum import Enum
from concurrent.futures import ThreadPoolExecutor, ProcessPoolExecutor, as_completed
import multiprocessing

from dependency_graph import (
    DependencyGraphAnalyzer, Task, TaskType, ExecutionWave,
    load_tasks_from_config
)

# Setup logging with thread safety
import multiprocessing_logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - [%(threadName)s] - %(message)s'
)
logger = logging.getLogger(__name__)
multiprocessing_logging.install_mp_handler()


class ExecutionStatus(Enum):
    """Status of task execution."""
    PENDING = "pending"
    QUEUED = "queued"
    RUNNING = "running"
    COMPLETED = "completed"
    FAILED = "failed"
    CANCELLED = "cancelled"
    BLOCKED = "blocked"


class AgentType(Enum):
    """Types of agents available for task execution."""
    RESEARCHER = "researcher"
    ANALYZER = "analyzer"
    DEVELOPER = "developer"
    TESTER = "tester"
    REVIEWER = "reviewer"
    DOCUMENTOR = "documentor"
    GENERALIST = "generalist"


@dataclass
class Agent:
    """Represents an execution agent."""
    id: str
    agent_type: AgentType
    capabilities: List[TaskType]
    max_concurrent_tasks: int = 1
    current_tasks: List[str] = None
    status: str = "idle"
    last_activity: datetime.datetime = None

    def __post_init__(self):
        if self.current_tasks is None:
            self.current_tasks = []
        if self.last_activity is None:
            self.last_activity = datetime.datetime.now()

    def can_handle_task(self, task_type: TaskType) -> bool:
        """Check if agent can handle the given task type."""
        return task_type in self.capabilities

    def is_available(self) -> bool:
        """Check if agent is available for new tasks."""
        return len(self.current_tasks) < self.max_concurrent_tasks

    def assign_task(self, task_id: str) -> None:
        """Assign a task to this agent."""
        if not self.is_available():
            raise ValueError(f"Agent {self.id} is not available")
        self.current_tasks.append(task_id)
        self.status = "busy"
        self.last_activity = datetime.datetime.now()

    def complete_task(self, task_id: str) -> None:
        """Mark a task as completed."""
        if task_id in self.current_tasks:
            self.current_tasks.remove(task_id)
        if not self.current_tasks:
            self.status = "idle"
        self.last_activity = datetime.datetime.now()


@dataclass
class TaskExecution:
    """Represents the execution state of a task."""
    task_id: str
    status: ExecutionStatus
    assigned_agent: Optional[str] = None
    start_time: Optional[datetime.datetime] = None
    end_time: Optional[datetime.datetime] = None
    result: Optional[Dict[str, Any]] = None
    error: Optional[str] = None
    retries: int = 0
    max_retries: int = 3
    wave_number: int = 0

    def duration(self) -> Optional[float]:
        """Calculate execution duration in minutes."""
        if self.start_time and self.end_time:
            return (self.end_time - self.start_time).total_seconds() / 60.0
        return None


class ResourceManager:
    """Manages shared resources during parallel execution."""

    def __init__(self):
        self._locks: Dict[str, threading.Lock] = {}
        self._usage: Dict[str, Set[str]] = {}  # resource -> set of task_ids using it
        self._mutex = threading.Lock()

    def acquire_resources(self, task_id: str, resources: List[str]) -> bool:
        """Try to acquire all required resources for a task."""
        with self._mutex:
            # Check if all resources are available
            for resource in resources:
                if resource in self._usage and self._usage[resource]:
                    return False

            # Acquire all resources
            for resource in resources:
                if resource not in self._locks:
                    self._locks[resource] = threading.Lock()
                if resource not in self._usage:
                    self._usage[resource] = set()

                self._locks[resource].acquire()
                self._usage[resource].add(task_id)

            logger.info(f"Task {task_id} acquired resources: {resources}")
            return True

    def release_resources(self, task_id: str, resources: List[str]) -> None:
        """Release resources used by a task."""
        with self._mutex:
            for resource in resources:
                if resource in self._usage and task_id in self._usage[resource]:
                    self._usage[resource].remove(task_id)
                    if resource in self._locks:
                        self._locks[resource].release()

            logger.info(f"Task {task_id} released resources: {resources}")

    def get_resource_usage(self) -> Dict[str, List[str]]:
        """Get current resource usage."""
        with self._mutex:
            return {resource: list(tasks) for resource, tasks in self._usage.items() if tasks}


class ParallelOrchestrator:
    """Orchestrates parallel execution of tasks with dependency management."""

    def __init__(self, max_workers: int = 5):
        self.max_workers = max_workers
        self.dependency_analyzer = DependencyGraphAnalyzer()
        self.agents: Dict[str, Agent] = {}
        self.task_executions: Dict[str, TaskExecution] = {}
        self.resource_manager = ResourceManager()
        self.execution_queue = queue.PriorityQueue()
        self.result_queue = queue.Queue()
        self.executor = ThreadPoolExecutor(max_workers=max_workers)
        self.stop_event = threading.Event()
        self.metrics = {
            "start_time": None,
            "end_time": None,
            "total_tasks": 0,
            "completed_tasks": 0,
            "failed_tasks": 0,
            "parallel_efficiency": 0.0,
            "actual_speedup": 0.0,
            "wave_timings": []
        }

    def add_agent(self, agent: Agent) -> None:
        """Add an agent to the execution pool."""
        self.agents[agent.id] = agent
        logger.info(f"Added agent: {agent.id} ({agent.agent_type.value})")

    def load_default_agents(self) -> None:
        """Load a default set of agents for development."""
        default_agents = [
            Agent("RESEARCHER_01", AgentType.RESEARCHER,
                 [TaskType.RESEARCH, TaskType.ANALYSIS], max_concurrent_tasks=2),
            Agent("RESEARCHER_02", AgentType.RESEARCHER,
                 [TaskType.RESEARCH, TaskType.ANALYSIS], max_concurrent_tasks=2),
            Agent("DEVELOPER_01", AgentType.DEVELOPER,
                 [TaskType.IMPLEMENTATION, TaskType.TESTING], max_concurrent_tasks=1),
            Agent("DEVELOPER_02", AgentType.DEVELOPER,
                 [TaskType.IMPLEMENTATION, TaskType.TESTING], max_concurrent_tasks=1),
            Agent("REVIEWER_01", AgentType.REVIEWER,
                 [TaskType.REVIEW, TaskType.TESTING], max_concurrent_tasks=3),
            Agent("DOCUMENTOR_01", AgentType.DOCUMENTOR,
                 [TaskType.DOCUMENTATION, TaskType.ANALYSIS], max_concurrent_tasks=2),
            Agent("GENERALIST_01", AgentType.GENERALIST,
                 list(TaskType), max_concurrent_tasks=1)
        ]

        for agent in default_agents:
            self.add_agent(agent)

    def load_execution_plan(self, config_path: str) -> None:
        """Load tasks and dependencies from configuration."""
        self.dependency_analyzer = load_tasks_from_config(config_path)

        # Initialize task executions
        for task_id in self.dependency_analyzer.tasks:
            self.task_executions[task_id] = TaskExecution(
                task_id=task_id,
                status=ExecutionStatus.PENDING
            )

        self.metrics["total_tasks"] = len(self.dependency_analyzer.tasks)
        logger.info(f"Loaded execution plan with {len(self.dependency_analyzer.tasks)} tasks")

    def find_available_agent(self, task: Task) -> Optional[Agent]:
        """Find an available agent capable of handling the task."""
        suitable_agents = [
            agent for agent in self.agents.values()
            if agent.can_handle_task(task.task_type) and agent.is_available()
        ]

        if not suitable_agents:
            return None

        # Prefer agents with fewer current tasks
        return min(suitable_agents, key=lambda a: len(a.current_tasks))

    def can_start_task(self, task_id: str) -> bool:
        """Check if a task can start (dependencies met, resources available)."""
        task = self.dependency_analyzer.tasks[task_id]
        execution = self.task_executions[task_id]

        # Check if already running or completed
        if execution.status not in [ExecutionStatus.PENDING, ExecutionStatus.BLOCKED]:
            return False

        # Check dependencies
        for pred_task in self.dependency_analyzer.graph.predecessors(task_id):
            pred_execution = self.task_executions[pred_task]
            if pred_execution.status != ExecutionStatus.COMPLETED:
                return False

        # Check agent availability
        agent = self.find_available_agent(task)
        if not agent:
            return False

        # Check resource availability
        if not self.resource_manager.acquire_resources(task_id, task.resources_needed):
            return False

        return True

    def execute_task(self, task_id: str) -> Dict[str, Any]:
        """Execute a single task."""
        task = self.dependency_analyzer.tasks[task_id]
        execution = self.task_executions[task_id]

        try:
            # Find and assign agent
            agent = self.find_available_agent(task)
            if not agent:
                raise RuntimeError(f"No available agent for task {task_id}")

            agent.assign_task(task_id)
            execution.assigned_agent = agent.id
            execution.status = ExecutionStatus.RUNNING
            execution.start_time = datetime.datetime.now()

            logger.info(f"Starting task {task_id} with agent {agent.id}")

            # Simulate task execution (replace with actual task execution logic)
            result = self._simulate_task_execution(task)

            # Mark as completed
            execution.status = ExecutionStatus.COMPLETED
            execution.end_time = datetime.datetime.now()
            execution.result = result

            agent.complete_task(task_id)
            self.resource_manager.release_resources(task_id, task.resources_needed)

            self.metrics["completed_tasks"] += 1
            logger.info(f"Completed task {task_id} in {execution.duration():.1f} minutes")

            return result

        except Exception as e:
            execution.status = ExecutionStatus.FAILED
            execution.error = str(e)
            execution.end_time = datetime.datetime.now()

            if execution.assigned_agent:
                self.agents[execution.assigned_agent].complete_task(task_id)
            self.resource_manager.release_resources(task_id, task.resources_needed)

            self.metrics["failed_tasks"] += 1
            logger.error(f"Task {task_id} failed: {e}")

            # Retry logic
            if execution.retries < execution.max_retries:
                execution.retries += 1
                execution.status = ExecutionStatus.PENDING
                logger.info(f"Retrying task {task_id} (attempt {execution.retries + 1})")

            raise

    def _simulate_task_execution(self, task: Task) -> Dict[str, Any]:
        """Simulate task execution for testing purposes."""
        # Simulate work time based on estimated duration
        simulation_factor = 0.1  # 1 minute real = 6 seconds simulation
        sleep_time = task.estimated_duration * simulation_factor

        time.sleep(sleep_time)

        return {
            "task_id": task.id,
            "task_name": task.name,
            "execution_time": sleep_time,
            "simulated": True,
            "files_created": [f"output_{task.id}.txt"],
            "metrics": {
                "lines_of_code": task.estimated_duration * 10,
                "test_coverage": 0.85 if task.task_type == TaskType.TESTING else 0.0
            }
        }

    def execute_wave(self, wave: ExecutionWave) -> Dict[str, Any]:
        """Execute all tasks in a wave in parallel."""
        wave_start = datetime.datetime.now()
        logger.info(f"Starting wave {wave.wave_number} with {len(wave.tasks)} tasks")

        # Submit all tasks in the wave
        futures = {}
        for task_id in wave.tasks:
            if self.can_start_task(task_id):
                future = self.executor.submit(self.execute_task, task_id)
                futures[future] = task_id
                self.task_executions[task_id].wave_number = wave.wave_number
            else:
                logger.warning(f"Cannot start task {task_id} in wave {wave.wave_number}")

        # Wait for all tasks to complete
        results = {}
        for future in as_completed(futures):
            task_id = futures[future]
            try:
                result = future.result()
                results[task_id] = result
            except Exception as e:
                logger.error(f"Task {task_id} in wave {wave.wave_number} failed: {e}")
                results[task_id] = {"error": str(e)}

        wave_end = datetime.datetime.now()
        wave_duration = (wave_end - wave_start).total_seconds() / 60.0

        wave_result = {
            "wave_number": wave.wave_number,
            "start_time": wave_start.isoformat(),
            "end_time": wave_end.isoformat(),
            "duration_minutes": wave_duration,
            "estimated_duration": wave.estimated_duration,
            "tasks_completed": len([r for r in results.values() if "error" not in r]),
            "tasks_failed": len([r for r in results.values() if "error" in r]),
            "efficiency": (wave.estimated_duration / wave_duration) if wave_duration > 0 else 0.0,
            "results": results
        }

        self.metrics["wave_timings"].append(wave_result)
        logger.info(f"Completed wave {wave.wave_number} in {wave_duration:.1f} minutes")

        return wave_result

    def execute_parallel_plan(self) -> Dict[str, Any]:
        """Execute the complete parallel execution plan."""
        if not self.dependency_analyzer.tasks:
            raise ValueError("No tasks loaded")

        self.metrics["start_time"] = datetime.datetime.now()

        # Generate execution waves
        waves = self.dependency_analyzer.generate_execution_waves(len(self.agents))

        logger.info(f"Executing {len(waves)} waves with {len(self.agents)} agents")

        # Execute waves sequentially (tasks within waves are parallel)
        wave_results = []
        for wave in waves:
            try:
                wave_result = self.execute_wave(wave)
                wave_results.append(wave_result)
            except Exception as e:
                logger.error(f"Wave {wave.wave_number} failed: {e}")
                break

        self.metrics["end_time"] = datetime.datetime.now()

        # Calculate final metrics
        total_duration = (self.metrics["end_time"] - self.metrics["start_time"]).total_seconds() / 60.0
        sequential_time = sum(task.estimated_duration for task in self.dependency_analyzer.tasks.values())

        self.metrics.update({
            "total_duration_minutes": total_duration,
            "sequential_duration_minutes": sequential_time,
            "actual_speedup": sequential_time / total_duration if total_duration > 0 else 1.0,
            "parallel_efficiency": (sequential_time / total_duration) / len(self.agents) if total_duration > 0 else 0.0,
            "waves_executed": len(wave_results),
            "completion_rate": self.metrics["completed_tasks"] / self.metrics["total_tasks"] if self.metrics["total_tasks"] > 0 else 0.0
        })

        return {
            "execution_summary": self.metrics,
            "wave_results": wave_results,
            "task_details": {task_id: asdict(execution) for task_id, execution in self.task_executions.items()},
            "resource_usage": self.resource_manager.get_resource_usage()
        }

    def get_status(self) -> Dict[str, Any]:
        """Get current execution status."""
        status_counts = {}
        for status in ExecutionStatus:
            status_counts[status.value] = sum(
                1 for exec in self.task_executions.values() if exec.status == status
            )

        agent_status = {}
        for agent_id, agent in self.agents.items():
            agent_status[agent_id] = {
                "type": agent.agent_type.value,
                "status": agent.status,
                "current_tasks": agent.current_tasks,
                "capacity": f"{len(agent.current_tasks)}/{agent.max_concurrent_tasks}"
            }

        return {
            "timestamp": datetime.datetime.now().isoformat(),
            "task_status": status_counts,
            "agent_status": agent_status,
            "resource_usage": self.resource_manager.get_resource_usage(),
            "metrics": self.metrics
        }

    def shutdown(self) -> None:
        """Shutdown the orchestrator gracefully."""
        logger.info("Shutting down parallel orchestrator")
        self.stop_event.set()
        self.executor.shutdown(wait=True)

    def save_execution_report(self, output_path: str, results: Dict[str, Any]) -> None:
        """Save detailed execution report."""
        report = {
            "generated_at": datetime.datetime.now().isoformat(),
            "orchestrator_config": {
                "max_workers": self.max_workers,
                "agents_count": len(self.agents),
                "tasks_count": len(self.dependency_analyzer.tasks)
            },
            "execution_results": results,
            "agent_details": {agent_id: asdict(agent) for agent_id, agent in self.agents.items()},
            "dependency_analysis": self.dependency_analyzer.calculate_speedup_potential(len(self.agents))
        }

        with open(output_path, 'w') as f:
            json.dump(report, f, indent=2, default=str)

        logger.info(f"Saved execution report to {output_path}")


def create_sample_config() -> str:
    """Create a sample task configuration for testing."""
    config = {
        "tasks": [
            {
                "id": "research_auth",
                "name": "Research Authentication Patterns",
                "task_type": "research",
                "estimated_duration": 20,
                "required_agents": 1,
                "resources_needed": ["internet"],
                "files_read": [],
                "files_modified": ["research/auth_patterns.md"]
            },
            {
                "id": "research_db",
                "name": "Research Database Options",
                "task_type": "research",
                "estimated_duration": 15,
                "required_agents": 1,
                "resources_needed": ["internet"],
                "files_read": [],
                "files_modified": ["research/database_options.md"]
            },
            {
                "id": "design_auth",
                "name": "Design Authentication System",
                "task_type": "design",
                "estimated_duration": 30,
                "required_agents": 1,
                "resources_needed": ["design_tools"],
                "files_read": ["research/auth_patterns.md"],
                "files_modified": ["design/auth_system.md"]
            },
            {
                "id": "implement_user_model",
                "name": "Implement User Model",
                "task_type": "implementation",
                "estimated_duration": 45,
                "required_agents": 1,
                "resources_needed": ["database"],
                "files_read": ["research/database_options.md", "design/auth_system.md"],
                "files_modified": ["src/models/user.py"]
            },
            {
                "id": "implement_auth_service",
                "name": "Implement Authentication Service",
                "task_type": "implementation",
                "estimated_duration": 60,
                "required_agents": 1,
                "resources_needed": ["database"],
                "files_read": ["design/auth_system.md", "src/models/user.py"],
                "files_modified": ["src/services/auth.py"]
            },
            {
                "id": "test_user_model",
                "name": "Test User Model",
                "task_type": "testing",
                "estimated_duration": 25,
                "required_agents": 1,
                "resources_needed": ["test_database"],
                "files_read": ["src/models/user.py"],
                "files_modified": ["tests/test_user_model.py"]
            },
            {
                "id": "test_auth_service",
                "name": "Test Authentication Service",
                "task_type": "testing",
                "estimated_duration": 35,
                "required_agents": 1,
                "resources_needed": ["test_database"],
                "files_read": ["src/services/auth.py"],
                "files_modified": ["tests/test_auth_service.py"]
            },
            {
                "id": "document_api",
                "name": "Document Authentication API",
                "task_type": "documentation",
                "estimated_duration": 20,
                "required_agents": 1,
                "resources_needed": [],
                "files_read": ["src/services/auth.py"],
                "files_modified": ["docs/auth_api.md"]
            }
        ],
        "dependencies": [
            {
                "from_task": "research_auth",
                "to_task": "design_auth",
                "dependency_type": "blocks",
                "reason": "Design needs research findings"
            },
            {
                "from_task": "research_db",
                "to_task": "implement_user_model",
                "dependency_type": "blocks",
                "reason": "Implementation needs database choice"
            },
            {
                "from_task": "design_auth",
                "to_task": "implement_user_model",
                "dependency_type": "blocks",
                "reason": "Implementation needs design"
            },
            {
                "from_task": "design_auth",
                "to_task": "implement_auth_service",
                "dependency_type": "blocks",
                "reason": "Implementation needs design"
            },
            {
                "from_task": "implement_user_model",
                "to_task": "implement_auth_service",
                "dependency_type": "blocks",
                "reason": "Auth service depends on user model"
            },
            {
                "from_task": "implement_user_model",
                "to_task": "test_user_model",
                "dependency_type": "blocks",
                "reason": "Cannot test before implementation"
            },
            {
                "from_task": "implement_auth_service",
                "to_task": "test_auth_service",
                "dependency_type": "blocks",
                "reason": "Cannot test before implementation"
            },
            {
                "from_task": "implement_auth_service",
                "to_task": "document_api",
                "dependency_type": "blocks",
                "reason": "Cannot document before implementation"
            }
        ]
    }

    config_path = "/tmp/sample_tasks.json"
    with open(config_path, 'w') as f:
        json.dump(config, f, indent=2)

    return config_path


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Atlas Parallel Orchestrator")
    parser.add_argument("--config", help="Path to task configuration JSON file")
    parser.add_argument("--max-agents", type=int, default=5, help="Maximum number of parallel agents")
    parser.add_argument("--output-dir", default="./output", help="Output directory for reports")
    parser.add_argument("--demo", action="store_true", help="Run with sample configuration")

    args = parser.parse_args()

    # Create output directory
    output_dir = Path(args.output_dir)
    output_dir.mkdir(exist_ok=True)

    # Create orchestrator
    orchestrator = ParallelOrchestrator(max_workers=args.max_agents)
    orchestrator.load_default_agents()

    try:
        # Load configuration
        if args.demo:
            config_path = create_sample_config()
            print(f"Created sample configuration at {config_path}")
        else:
            config_path = args.config
            if not config_path:
                print("Error: --config required unless using --demo mode")
                exit(1)

        orchestrator.load_execution_plan(config_path)

        # Show initial status
        print("\n=== Initial Status ===")
        status = orchestrator.get_status()
        print(f"Tasks loaded: {sum(status['task_status'].values())}")
        print(f"Agents available: {len(status['agent_status'])}")

        # Execute parallel plan
        print("\n=== Starting Parallel Execution ===")
        results = orchestrator.execute_parallel_plan()

        # Show results
        print("\n=== Execution Results ===")
        summary = results["execution_summary"]
        print(f"Total duration: {summary['total_duration_minutes']:.1f} minutes")
        print(f"Sequential time: {summary['sequential_duration_minutes']:.1f} minutes")
        print(f"Speedup achieved: {summary['actual_speedup']:.2f}x")
        print(f"Parallel efficiency: {summary['parallel_efficiency']:.1%}")
        print(f"Completion rate: {summary['completion_rate']:.1%}")
        print(f"Waves executed: {summary['waves_executed']}")

        # Save detailed report
        report_path = output_dir / "parallel_execution_report.json"
        orchestrator.save_execution_report(str(report_path), results)
        print(f"\nDetailed report saved to {report_path}")

    except KeyboardInterrupt:
        print("\nExecution interrupted by user")
    except Exception as e:
        print(f"Error during execution: {e}")
        logger.exception("Execution failed")
    finally:
        orchestrator.shutdown()
        print("Orchestrator shutdown complete")