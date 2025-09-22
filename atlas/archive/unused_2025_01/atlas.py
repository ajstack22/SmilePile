#!/usr/bin/env python3
"""
Atlas Framework - Unified CLI

The single entry point for all Atlas operations.
No more confusion between multiple scripts - this is THE way to use Atlas.

Usage:
    python atlas.py workflow start <feature_id>
    python atlas.py workflow status
    python atlas.py review submit <feature_id>
    python atlas.py review status
    python atlas.py validate
    python atlas.py dashboard
    python atlas.py metrics
"""

import sys
import argparse
from pathlib import Path

# Add automation directory to path
sys.path.insert(0, str(Path(__file__).parent / "automation" / "core"))
sys.path.insert(0, str(Path(__file__).parent / "automation" / "processes"))

def main():
    parser = argparse.ArgumentParser(description="Atlas Framework - Unified CLI")
    subparsers = parser.add_subparsers(dest="command", help="Available commands")

    # Workflow commands
    workflow_parser = subparsers.add_parser("workflow", help="Workflow operations")
    workflow_subparsers = workflow_parser.add_subparsers(dest="workflow_action")

    start_parser = workflow_subparsers.add_parser("start", help="Start a workflow")
    start_parser.add_argument("feature_id", help="Feature ID to start")

    workflow_subparsers.add_parser("status", help="Show workflow status")

    # Review commands
    review_parser = subparsers.add_parser("review", help="Review operations")
    review_subparsers = review_parser.add_subparsers(dest="review_action")

    submit_parser = review_subparsers.add_parser("submit", help="Submit for review")
    submit_parser.add_argument("feature_id", help="Feature ID to review")

    review_subparsers.add_parser("status", help="Show review status")

    # Other commands
    subparsers.add_parser("validate", help="Validate Atlas configuration")
    subparsers.add_parser("dashboard", help="Open Atlas dashboard")
    subparsers.add_parser("metrics", help="Show Atlas metrics")

    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        return

    # Route to appropriate handler
    if args.command == "workflow":
        handle_workflow(args)
    elif args.command == "review":
        handle_review(args)
    elif args.command == "validate":
        handle_validate(args)
    elif args.command == "dashboard":
        handle_dashboard(args)
    elif args.command == "metrics":
        handle_metrics(args)

def handle_workflow(args):
    """Handle workflow commands"""
    if args.workflow_action == "start":
        print(f"Starting workflow for {args.feature_id}")
        # Import and use workflow orchestrator
        try:
            from parallel_orchestrator import ParallelOrchestrator
            orchestrator = ParallelOrchestrator()
            orchestrator.start_workflow(args.feature_id)
        except ImportError:
            print("Workflow orchestrator not found. Please check automation setup.")

    elif args.workflow_action == "status":
        print("Showing workflow status")
        try:
            from workflow_state_machine import WorkflowStateMachine
            state_machine = WorkflowStateMachine()
            state_machine.show_status()
        except ImportError:
            print("Workflow state machine not found. Please check automation setup.")

def handle_review(args):
    """Handle review commands"""
    if args.review_action == "submit":
        print(f"Submitting {args.feature_id} for review")
        try:
            from differential_reviewer import DifferentialReviewer
            reviewer = DifferentialReviewer()
            reviewer.submit_for_review(args.feature_id)
        except ImportError:
            print("Review system not found. Please check automation setup.")

    elif args.review_action == "status":
        print("Showing review status")

def handle_validate(args):
    """Handle validation"""
    print("Validating Atlas configuration")
    try:
        from workflow_validator import WorkflowValidator
        validator = WorkflowValidator()
        validator.validate_all()
    except ImportError:
        print("Validator not found. Please check automation setup.")

def handle_dashboard(args):
    """Handle dashboard"""
    print("Opening Atlas dashboard")
    try:
        from dashboard import Dashboard
        dashboard = Dashboard()
        dashboard.open()
    except ImportError:
        print("Dashboard not found. Please check automation setup.")

def handle_metrics(args):
    """Handle metrics"""
    print("Showing Atlas metrics")

if __name__ == "__main__":
    main()
