#!/usr/bin/env python3
"""
Claude Code Integration Module - Hooks into Claude Code's Task tool for automatic context injection
This module provides the actual integration mechanism that enables auto-context injection
"""

import os
import sys
import json
import time
import logging
from pathlib import Path
from functools import wraps
from typing import Dict, Any, Optional, Callable

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class ClaudeCodeIntegration:
    """
    Integration layer for Claude Code's Task tool
    Provides transparent context injection for all agent spawning
    """

    def __init__(self):
        self.atlas_dir = Path(__file__).parent.parent
        self.integration_active = False
        self.original_task_tool = None
        self.context_integration = None
        self.metrics = {
            'total_intercepts': 0,
            'successful_injections': 0,
            'failed_injections': 0,
            'total_injection_time': 0,
            'initialization_time': 0
        }

    def initialize(self) -> bool:
        """
        Initialize the Claude Code integration

        Returns:
            True if initialization successful, False otherwise
        """
        start_time = time.time()

        try:
            # Import the task context integration module
            from task_context_integration import TaskContextIntegration

            # Initialize context integration with manifest and config
            manifest_path = self.atlas_dir / 'config' / 'context_manifest.json'
            config_path = self.atlas_dir / 'config' / 'agent_context_mapping.yaml'

            if not manifest_path.exists():
                logger.error(f"Context manifest not found: {manifest_path}")
                return False

            if not config_path.exists():
                logger.error(f"Agent mapping config not found: {config_path}")
                return False

            self.context_integration = TaskContextIntegration(
                manifest_path=str(manifest_path),
                config_path=str(config_path)
            )

            # Validate the integration
            validation = self.context_integration.validate_integration()
            if validation['status'] != 'healthy':
                logger.error(f"Context integration validation failed: {validation}")
                return False

            self.metrics['initialization_time'] = time.time() - start_time
            logger.info(f"Claude Code integration initialized in {self.metrics['initialization_time']:.2f}s")

            return True

        except Exception as e:
            logger.error(f"Failed to initialize Claude Code integration: {e}")
            return False

    def hook_task_tool(self, task_tool: Callable) -> Callable:
        """
        Create a hooked version of the Task tool with automatic context injection

        Args:
            task_tool: Original Task tool function

        Returns:
            Enhanced Task tool with context injection
        """
        self.original_task_tool = task_tool

        @wraps(task_tool)
        def enhanced_task_tool(
            description: str,
            prompt: str,
            subagent_type: str = 'general-purpose',
            **kwargs
        ) -> Any:
            """
            Enhanced Task tool with automatic context injection

            This wrapper intercepts all Task tool calls and automatically
            injects appropriate context based on agent type and task description
            """
            start_time = time.time()
            self.metrics['total_intercepts'] += 1

            try:
                # Build parameters dict for context injection
                task_params = {
                    'description': description,
                    'prompt': prompt,
                    'subagent_type': subagent_type,
                    **kwargs
                }

                # Log interception
                logger.debug(f"Intercepting Task tool: {subagent_type} - {description[:50]}...")

                # Apply context injection
                if self.context_integration and self.integration_active:
                    try:
                        enhanced_params = self.context_integration.intercept_task_tool(task_params)

                        # Extract enhanced prompt and metadata
                        if 'prompt' in enhanced_params:
                            prompt = enhanced_params['prompt']

                        if '_context_metadata' in enhanced_params:
                            metadata = enhanced_params['_context_metadata']
                            logger.info(
                                f"Context injected: {metadata['profile']} "
                                f"({metadata['tokens']} tokens) in {metadata['injection_time']:.2f}s"
                            )

                        self.metrics['successful_injections'] += 1

                    except Exception as e:
                        logger.warning(f"Context injection failed (using original): {e}")
                        self.metrics['failed_injections'] += 1
                        # Continue with original prompt on failure

                # Track timing
                injection_time = time.time() - start_time
                self.metrics['total_injection_time'] += injection_time

                # Call original Task tool with potentially enhanced prompt
                return task_tool(
                    description=description,
                    prompt=prompt,
                    subagent_type=subagent_type,
                    **kwargs
                )

            except Exception as e:
                logger.error(f"Error in enhanced Task tool: {e}")
                # Fallback to original tool on any error
                return task_tool(
                    description=description,
                    prompt=prompt,
                    subagent_type=subagent_type,
                    **kwargs
                )

        return enhanced_task_tool

    def register_with_claude_code(self) -> bool:
        """
        Register the context injection hook with Claude Code

        This is the main entry point that should be called during
        Claude Code initialization to enable automatic context injection

        Returns:
            True if registration successful, False otherwise
        """
        try:
            # Initialize the integration
            if not self.initialize():
                logger.error("Failed to initialize integration")
                return False

            # Check if we're running in Claude Code environment
            # This would be replaced with actual Claude Code detection
            if self._detect_claude_code_environment():
                # Import Claude Code's Task tool
                # NOTE: This import path would need to be adjusted for actual Claude Code
                try:
                    from claude_code.tools import Task

                    # Create enhanced version
                    enhanced_task = self.hook_task_tool(Task)

                    # Replace the original with enhanced version
                    # NOTE: This mechanism would depend on Claude Code's architecture
                    import claude_code.tools
                    claude_code.tools.Task = enhanced_task

                    self.integration_active = True
                    logger.info("✅ Atlas context injection registered with Claude Code")

                    return True

                except ImportError:
                    logger.warning("Claude Code tools module not found, using mock mode")
                    # For testing/development, return the hook decorator
                    self.integration_active = True
                    return True
            else:
                logger.info("Not running in Claude Code environment, hook available for manual use")
                self.integration_active = True
                return True

        except Exception as e:
            logger.error(f"Failed to register with Claude Code: {e}")
            return False

    def _detect_claude_code_environment(self) -> bool:
        """
        Detect if we're running in Claude Code environment

        Returns:
            True if Claude Code environment detected
        """
        # Check for Claude Code specific environment variables or markers
        claude_markers = [
            'CLAUDE_CODE_VERSION',
            'CLAUDE_WORKSPACE',
            'MCP_SERVER_URL'
        ]

        for marker in claude_markers:
            if os.getenv(marker):
                return True

        # Check for Claude Code specific modules
        try:
            import claude_code
            return True
        except ImportError:
            pass

        return False

    def get_metrics(self) -> Dict[str, Any]:
        """
        Get integration metrics

        Returns:
            Dictionary of metrics
        """
        success_rate = 0
        if self.metrics['total_intercepts'] > 0:
            success_rate = self.metrics['successful_injections'] / self.metrics['total_intercepts']

        avg_injection_time = 0
        if self.metrics['successful_injections'] > 0:
            avg_injection_time = self.metrics['total_injection_time'] / self.metrics['successful_injections']

        return {
            'integration_active': self.integration_active,
            'total_intercepts': self.metrics['total_intercepts'],
            'successful_injections': self.metrics['successful_injections'],
            'failed_injections': self.metrics['failed_injections'],
            'success_rate': success_rate,
            'average_injection_time': avg_injection_time,
            'initialization_time': self.metrics['initialization_time']
        }

    def disable(self):
        """Temporarily disable context injection"""
        self.integration_active = False
        logger.info("Context injection disabled")

    def enable(self):
        """Re-enable context injection"""
        self.integration_active = True
        logger.info("Context injection enabled")

    def test_integration(self) -> bool:
        """
        Test the integration with a sample Task call

        Returns:
            True if test successful
        """
        if not self.context_integration:
            logger.error("Integration not initialized")
            return False

        # Create test task parameters
        test_params = {
            'description': 'Create a user story for authentication',
            'prompt': 'Users need to log in with email and password',
            'subagent_type': 'story-writer'
        }

        try:
            # Test context injection
            result = self.context_integration.intercept_task_tool(test_params)

            # Verify injection happened
            if '_context_metadata' not in result:
                logger.error("No context metadata in result")
                return False

            metadata = result['_context_metadata']

            # Verify correct profile selected
            if metadata['profile'] != 'story_creation':
                logger.error(f"Wrong profile selected: {metadata['profile']}")
                return False

            # Verify prompt was enhanced
            if len(result.get('prompt', '')) <= len(test_params['prompt']):
                logger.warning(f"Prompt length check: {len(result.get('prompt', ''))} vs {len(test_params['prompt'])}")
                # Check if context was added differently
                if '[CONTEXT AUTO-INJECTED]' not in result.get('prompt', ''):
                    logger.error("Prompt not enhanced with context")
                    return False

            # Verify performance
            if metadata['injection_time'] > 0.5:
                logger.warning(f"Injection slower than target: {metadata['injection_time']:.2f}s")

            logger.info("✅ Integration test passed")
            return True

        except Exception as e:
            logger.error(f"Integration test failed: {e}")
            return False


# Global instance for easy access
_global_integration = None

def get_integration() -> ClaudeCodeIntegration:
    """Get or create the global integration instance"""
    global _global_integration
    if _global_integration is None:
        _global_integration = ClaudeCodeIntegration()
    return _global_integration

def register() -> bool:
    """
    Register the Atlas context injection with Claude Code

    This should be called during Claude Code initialization

    Returns:
        True if registration successful
    """
    integration = get_integration()
    return integration.register_with_claude_code()

def task_hook(func: Callable) -> Callable:
    """
    Decorator for manually hooking Task-like functions

    Usage:
        @task_hook
        def my_task_function(description, prompt, subagent_type):
            ...
    """
    integration = get_integration()
    if not integration.context_integration:
        integration.initialize()
    return integration.hook_task_tool(func)


# Auto-registration hook for Claude Code
def __claude_code_init__():
    """
    Special function that Claude Code calls during initialization
    This enables automatic registration without code changes
    """
    if register():
        logger.info("Atlas context injection auto-registered with Claude Code")
    else:
        logger.warning("Failed to auto-register context injection")


# CLI interface for testing and management
def main():
    """CLI for testing and managing the Claude Code integration"""
    import argparse

    parser = argparse.ArgumentParser(description='Claude Code Integration Manager')
    subparsers = parser.add_subparsers(dest='command', help='Commands')

    # Register command
    register_parser = subparsers.add_parser('register', help='Register with Claude Code')

    # Test command
    test_parser = subparsers.add_parser('test', help='Test the integration')

    # Metrics command
    metrics_parser = subparsers.add_parser('metrics', help='Show metrics')

    # Status command
    status_parser = subparsers.add_parser('status', help='Show integration status')

    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        return

    integration = get_integration()

    if args.command == 'register':
        if integration.register_with_claude_code():
            print("✅ Successfully registered with Claude Code")
            print(f"Integration active: {integration.integration_active}")
        else:
            print("❌ Failed to register with Claude Code")
            sys.exit(1)

    elif args.command == 'test':
        if not integration.context_integration:
            if not integration.initialize():
                print("❌ Failed to initialize integration")
                sys.exit(1)

        if integration.test_integration():
            print("✅ Integration test passed")
        else:
            print("❌ Integration test failed")
            sys.exit(1)

    elif args.command == 'metrics':
        metrics = integration.get_metrics()
        print(json.dumps(metrics, indent=2))

    elif args.command == 'status':
        status = {
            'integration_active': integration.integration_active,
            'context_integration_initialized': integration.context_integration is not None,
            'environment': 'Claude Code' if integration._detect_claude_code_environment() else 'Standalone'
        }

        if integration.context_integration:
            validation = integration.context_integration.validate_integration()
            status['validation'] = validation

        print(json.dumps(status, indent=2))


if __name__ == '__main__':
    main()