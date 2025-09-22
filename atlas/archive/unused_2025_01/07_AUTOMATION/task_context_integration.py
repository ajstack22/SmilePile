#!/usr/bin/env python3
"""
Task Tool Context Integration - Automatic context injection for LLM agent spawning
Intercepts Task tool calls and automatically injects appropriate context
"""

import json
import re
import time
import hashlib
from pathlib import Path
from typing import Dict, List, Optional, Any, Tuple
from datetime import datetime
from functools import wraps
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class TaskContextIntegration:
    """
    Integrates context injection with Task tool for automatic context delivery
    """

    def __init__(self, manifest_path: Optional[str] = None, config_path: Optional[str] = None):
        self.atlas_dir = Path(__file__).parent.parent

        # Convert to Path objects if strings are provided
        if isinstance(config_path, str):
            self.config_path = Path(config_path)
        else:
            self.config_path = config_path or self.atlas_dir / 'config' / 'agent_context_mapping.yaml'

        # Import the enhanced context injector
        from enhanced_context_injector import EnhancedContextInjector
        self.context_injector = EnhancedContextInjector(manifest_path)
        self.injector = self.context_injector  # Alias for compatibility

        # Load agent mapping configuration
        self.agent_mapping = self._load_agent_mapping()

        # Metrics tracking
        self.metrics = {
            'total_injections': 0,
            'cache_hits': 0,
            'detection_overrides': 0,
            'injection_times': [],
            'success_rate': 1.0
        }

        # Context inheritance tracking
        self.context_chain = {}

    def _load_agent_mapping(self) -> Dict:
        """Load agent type to context profile mapping"""
        # Default mapping if config doesn't exist
        default_mapping = {
            'agent_types': {
                'general-purpose': {
                    'default_profile': 'orchestration',
                    'detection_enabled': True
                },
                'ui-developer': {
                    'default_profile': 'ui_development',
                    'detection_enabled': False
                },
                'backend-developer': {
                    'default_profile': 'backend_development',
                    'detection_enabled': False
                },
                'story-writer': {
                    'default_profile': 'story_creation',
                    'detection_enabled': False
                },
                'bug-fixer': {
                    'default_profile': 'troubleshooting',
                    'detection_enabled': False
                },
                'researcher': {
                    'default_profile': 'research',
                    'detection_enabled': False
                },
                'reviewer': {
                    'default_profile': 'code_review',
                    'detection_enabled': False
                },
                'tester': {
                    'default_profile': 'testing',
                    'detection_enabled': False
                },
                'test-specialist': {
                    'default_profile': 'testing',
                    'detection_enabled': False
                },
                'documenter': {
                    'default_profile': 'documentation',
                    'detection_enabled': False
                },
                'deployer': {
                    'default_profile': 'deployment',
                    'detection_enabled': False
                }
            },
            'keyword_detection': {
                'rules': [
                    {
                        'keywords': ['bug', 'fix', 'crash', 'error', 'debug', 'troubleshoot'],
                        'profile': 'troubleshooting',
                        'confidence': 0.8
                    },
                    {
                        'keywords': ['story', 'requirement', 'epic', 'acceptance criteria'],
                        'profile': 'story_creation',
                        'confidence': 0.9
                    },
                    {
                        'keywords': ['ui', 'frontend', 'component', 'react', 'vue', 'style'],
                        'profile': 'ui_development',
                        'confidence': 0.7
                    },
                    {
                        'keywords': ['api', 'backend', 'endpoint', 'database', 'server'],
                        'profile': 'backend_development',
                        'confidence': 0.7
                    },
                    {
                        'keywords': ['test', 'spec', 'coverage', 'assertion', 'mock'],
                        'profile': 'testing',
                        'confidence': 0.8
                    },
                    {
                        'keywords': ['review', 'quality', 'refactor', 'feedback'],
                        'profile': 'code_review',
                        'confidence': 0.7
                    },
                    {
                        'keywords': ['research', 'investigate', 'analyze', 'explore'],
                        'profile': 'research',
                        'confidence': 0.8
                    },
                    {
                        'keywords': ['deploy', 'release', 'production', 'rollout'],
                        'profile': 'deployment',
                        'confidence': 0.8
                    }
                ]
            },
            'feature_detection': {
                'authentication': ['auth', 'login', 'oauth', 'jwt', 'session', 'password'],
                'performance': ['performance', 'optimize', 'slow', 'speed', 'latency'],
                'database': ['database', 'sql', 'query', 'migration', 'schema'],
                'security': ['security', 'vulnerability', 'encryption', 'secure'],
                'api': ['api', 'rest', 'graphql', 'endpoint', 'swagger']
            }
        }

        # Try to load from config file if it exists
        if self.config_path.exists():
            try:
                import yaml
                with open(self.config_path) as f:
                    return yaml.safe_load(f)
            except:
                logger.warning(f"Failed to load config from {self.config_path}, using defaults")

        return default_mapping

    def intercept_task_tool(self, task_params: Dict[str, Any]) -> Dict[str, Any]:
        """
        Main interception point for Task tool calls
        Automatically injects context before agent execution

        Args:
            task_params: Original Task tool parameters
                - description: Task description
                - prompt: Task prompt
                - subagent_type: Type of agent to spawn

        Returns:
            Modified task_params with injected context
        """
        start_time = time.time()

        try:
            # Extract parameters
            description = task_params.get('description', '')
            prompt = task_params.get('prompt', '')
            agent_type = task_params.get('subagent_type', 'general-purpose')

            # Generate unique task ID for tracking
            task_id = self._generate_task_id(description, agent_type)

            logger.info(f"Intercepting Task tool: {agent_type} - {description[:50]}...")

            # Determine context profile
            profile, confidence, method = self._determine_context_profile(
                agent_type,
                description,
                prompt
            )

            # Detect feature modifiers
            feature = self._detect_features(description, prompt)

            # Check for inherited context
            inherited_context = self._get_inherited_context(task_params)

            # Build context using enhanced injector
            context_result = self.context_injector.build_context(
                task=profile,
                feature=feature,
                additional_files=self._extract_mentioned_files(prompt),
                verbose=False
            )

            # Combine with inherited context if present
            if inherited_context:
                context = self._merge_contexts(inherited_context, context_result['context'])
            else:
                context = context_result['context']

            # Inject context into prompt
            enhanced_prompt = self._format_injected_prompt(
                context,
                prompt,
                profile,
                confidence,
                method
            )

            # Update task parameters
            task_params['prompt'] = enhanced_prompt
            task_params['_context_metadata'] = {
                'task_id': task_id,
                'profile': profile,
                'feature': feature,
                'confidence': confidence,
                'method': method,
                'tokens': context_result['metadata']['total_tokens'],
                'injection_time': time.time() - start_time,
                'checklist_included': context_result['metadata'].get('checklist_included', False)
            }

            # Track metrics
            self._update_metrics(task_params['_context_metadata'])

            # Store for inheritance
            self.context_chain[task_id] = {
                'profile': profile,
                'context': context,
                'timestamp': datetime.now().isoformat()
            }

            logger.info(f"Context injected: {profile} ({context_result['metadata']['total_tokens']} tokens) in {time.time() - start_time:.2f}s")

            return task_params

        except Exception as e:
            logger.error(f"Context injection failed: {e}")
            # Return original params on failure - don't block agent execution
            return task_params

    def _determine_context_profile(
        self,
        agent_type: str,
        description: str,
        prompt: str
    ) -> Tuple[str, float, str]:
        """
        Determine which context profile to use

        Returns:
            Tuple of (profile_name, confidence, detection_method)
        """
        # First check direct agent type mapping
        if agent_type in self.agent_mapping['agent_types']:
            agent_config = self.agent_mapping['agent_types'][agent_type]

            # If detection is disabled, use default profile
            if not agent_config.get('detection_enabled', True):
                return agent_config['default_profile'], 1.0, 'direct_mapping'

            # For general-purpose, try keyword detection
            if agent_type == 'general-purpose' or agent_config.get('detection_enabled'):
                detected = self._detect_profile_by_keywords(description, prompt)
                if detected:
                    profile, confidence = detected
                    if confidence > 0.6:  # Confidence threshold
                        self.metrics['detection_overrides'] += 1
                        return profile, confidence, 'keyword_detection'

            # Fall back to default for agent type
            return agent_config['default_profile'], 0.8, 'agent_default'

        # Unknown agent type - try keyword detection
        detected = self._detect_profile_by_keywords(description, prompt)
        if detected:
            return detected[0], detected[1], 'keyword_fallback'

        # Ultimate fallback
        return 'orchestration', 0.5, 'fallback'

    def _detect_profile_by_keywords(
        self,
        description: str,
        prompt: str
    ) -> Optional[Tuple[str, float]]:
        """
        Detect context profile based on keywords in description and prompt

        Returns:
            Tuple of (profile_name, confidence) or None
        """
        combined_text = f"{description} {prompt}".lower()

        best_match = None
        best_confidence = 0.0

        for rule in self.agent_mapping['keyword_detection']['rules']:
            keywords = rule['keywords']
            matches = sum(1 for kw in keywords if kw in combined_text)

            if matches > 0:
                # Calculate confidence based on number of matches
                confidence = min(1.0, (matches / len(keywords)) * rule['confidence'])

                if confidence > best_confidence:
                    best_confidence = confidence
                    best_match = rule['profile']

        if best_match:
            return (best_match, best_confidence)

        return None

    def _detect_features(self, description: str, prompt: str) -> Optional[str]:
        """
        Detect feature modifiers from description and prompt

        Returns:
            Feature name or None
        """
        combined_text = f"{description} {prompt}".lower()

        # Handle both dict structure from YAML and default dict
        feature_detection = self.agent_mapping.get('feature_detection', {})

        for feature, config in feature_detection.items():
            # Handle both simple list and config dict with 'keywords' key
            if isinstance(config, dict) and 'keywords' in config:
                keywords = config['keywords']
            elif isinstance(config, list):
                keywords = config
            else:
                continue

            if any(kw in combined_text for kw in keywords):
                return feature

        return None

    def _extract_mentioned_files(self, prompt: str) -> List[str]:
        """
        Extract any file paths mentioned in the prompt for additional context

        Returns:
            List of file paths
        """
        # Look for file paths in prompt
        file_patterns = [
            r'[\'"`]([^\'"`]+\.(py|js|ts|md|yaml|json))[\'"`]',
            r'(?:file|path):\s*([^\s]+\.\w+)',
            r'(?:in|from|at)\s+([^\s]+\.\w+)'
        ]

        files = []
        for pattern in file_patterns:
            matches = re.findall(pattern, prompt, re.IGNORECASE)
            for match in matches:
                file_path = match[0] if isinstance(match, tuple) else match
                if file_path and not file_path.startswith('http'):
                    files.append(file_path)

        return files

    def _get_inherited_context(self, task_params: Dict) -> Optional[str]:
        """
        Get context from parent task if this is a sub-agent

        Returns:
            Parent context or None
        """
        # Check if there's a parent task ID
        parent_id = task_params.get('_parent_task_id')

        if parent_id and parent_id in self.context_chain:
            return self.context_chain[parent_id]['context']

        return None

    def _merge_contexts(self, inherited: str, new: str) -> str:
        """
        Merge inherited context with new context, avoiding duplication

        Returns:
            Merged context string
        """
        # Simple deduplication - can be enhanced with semantic similarity
        inherited_lines = set(inherited.split('\n'))
        new_lines = new.split('\n')

        merged = []
        merged.append("## Inherited Context\n")
        merged.append(inherited)
        merged.append("\n## Additional Context\n")

        for line in new_lines:
            if line not in inherited_lines:
                merged.append(line)

        return '\n'.join(merged)

    def _format_injected_prompt(
        self,
        context: str,
        original_prompt: str,
        profile: str,
        confidence: float,
        method: str
    ) -> str:
        """
        Format the prompt with injected context

        Returns:
            Enhanced prompt with context
        """
        formatted = f"""[CONTEXT AUTO-INJECTED]
Profile: {profile} (confidence: {confidence:.1%}, method: {method})
Token count: ~{len(context) // 4}

{context}

---
YOUR SPECIFIC TASK:
{original_prompt}"""

        return formatted

    def _generate_task_id(self, description: str, agent_type: str) -> str:
        """Generate unique task ID for tracking"""
        content = f"{description}:{agent_type}:{time.time()}"
        return hashlib.md5(content.encode()).hexdigest()[:12]

    def _update_metrics(self, metadata: Dict):
        """Update internal metrics"""
        self.metrics['total_injections'] += 1
        self.metrics['injection_times'].append(metadata['injection_time'])

        # Keep only last 100 injection times
        if len(self.metrics['injection_times']) > 100:
            self.metrics['injection_times'] = self.metrics['injection_times'][-100:]

    def get_metrics(self) -> Dict:
        """Get current metrics"""
        avg_time = sum(self.metrics['injection_times']) / len(self.metrics['injection_times']) if self.metrics['injection_times'] else 0

        return {
            'total_injections': self.metrics['total_injections'],
            'cache_hit_rate': self.metrics['cache_hits'] / max(1, self.metrics['total_injections']),
            'detection_override_rate': self.metrics['detection_overrides'] / max(1, self.metrics['total_injections']),
            'average_injection_time': avg_time,
            'success_rate': self.metrics['success_rate']
        }

    def validate_integration(self) -> Dict:
        """Validate that integration is working correctly"""
        validation_results = {
            'status': 'healthy',
            'checks': []
        }

        # Check 1: Context injector is available
        try:
            test_result = self.context_injector.list_profiles()
            validation_results['checks'].append({
                'name': 'context_injector',
                'status': 'pass',
                'profiles_available': len(test_result)
            })
        except Exception as e:
            validation_results['checks'].append({
                'name': 'context_injector',
                'status': 'fail',
                'error': str(e)
            })
            validation_results['status'] = 'unhealthy'

        # Check 2: Agent mapping is loaded
        agent_count = len(self.agent_mapping.get('agent_types', {}))
        validation_results['checks'].append({
            'name': 'agent_mapping',
            'status': 'pass' if agent_count > 0 else 'fail',
            'agents_mapped': agent_count
        })

        # Check 3: Test injection
        try:
            test_params = {
                'description': 'Test task for validation',
                'prompt': 'Test prompt',
                'subagent_type': 'general-purpose'
            }
            result = self.intercept_task_tool(test_params)

            if '_context_metadata' in result:
                validation_results['checks'].append({
                    'name': 'test_injection',
                    'status': 'pass',
                    'tokens_injected': result['_context_metadata']['tokens']
                })
            else:
                validation_results['checks'].append({
                    'name': 'test_injection',
                    'status': 'fail',
                    'error': 'No metadata found'
                })
                validation_results['status'] = 'unhealthy'
        except Exception as e:
            validation_results['checks'].append({
                'name': 'test_injection',
                'status': 'fail',
                'error': str(e)
            })
            validation_results['status'] = 'unhealthy'

        return validation_results


# Global instance for hook registration
_global_integration = None

def initialize_integration(manifest_path: Optional[str] = None, config_path: Optional[str] = None):
    """Initialize the global integration instance"""
    global _global_integration
    _global_integration = TaskContextIntegration(manifest_path, config_path)
    return _global_integration

def task_hook(func):
    """
    Decorator to wrap Task tool with automatic context injection

    Usage:
        @task_hook
        def Task(description, prompt, subagent_type):
            # Original Task implementation
            ...
    """
    @wraps(func)
    def wrapper(*args, **kwargs):
        # Build params dict from args/kwargs
        params = {}

        # Handle positional arguments
        if len(args) > 0:
            params['description'] = args[0]
        if len(args) > 1:
            params['prompt'] = args[1]
        if len(args) > 2:
            params['subagent_type'] = args[2]

        # Handle keyword arguments
        params.update(kwargs)

        # Apply context injection
        if _global_integration:
            params = _global_integration.intercept_task_tool(params)

            # Update kwargs with modified params
            kwargs.update(params)

        # Call original function
        return func(**kwargs)

    return wrapper

def register_with_claude_code():
    """
    Register the context injection hook with Claude Code's Task tool
    This would be called during Claude Code initialization
    """
    try:
        # Initialize integration
        integration = initialize_integration()

        # Validate it's working
        validation = integration.validate_integration()

        if validation['status'] == 'healthy':
            logger.info("Task Context Integration registered successfully")

            # In actual implementation, this would hook into Claude's Task tool
            # For now, we return the decorator for manual application
            return task_hook
        else:
            logger.error(f"Integration validation failed: {validation}")
            return None

    except Exception as e:
        logger.error(f"Failed to register integration: {e}")
        return None


def main():
    """CLI for testing and managing the integration"""
    import argparse

    parser = argparse.ArgumentParser(description='Task Context Integration Manager')
    subparsers = parser.add_subparsers(dest='command', help='Commands')

    # Validate command
    validate_parser = subparsers.add_parser('validate', help='Validate integration')

    # Test command
    test_parser = subparsers.add_parser('test', help='Test context injection')
    test_parser.add_argument('--description', required=True, help='Task description')
    test_parser.add_argument('--agent-type', default='general-purpose', help='Agent type')
    test_parser.add_argument('--prompt', default='Test prompt', help='Task prompt')

    # Metrics command
    metrics_parser = subparsers.add_parser('metrics', help='Show metrics')

    # Simulate command
    simulate_parser = subparsers.add_parser('simulate', help='Simulate Task tool calls')
    simulate_parser.add_argument('--scenario', choices=['story', 'bug', 'parallel'],
                                 help='Predefined scenario to simulate')

    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        return

    # Initialize integration
    integration = initialize_integration()

    if args.command == 'validate':
        result = integration.validate_integration()
        print(json.dumps(result, indent=2))

    elif args.command == 'test':
        params = {
            'description': args.description,
            'prompt': args.prompt,
            'subagent_type': args.agent_type
        }

        print(f"\nOriginal params:")
        print(f"  Description: {params['description']}")
        print(f"  Agent type: {params['subagent_type']}")
        print(f"  Prompt length: {len(params['prompt'])} chars")

        # Apply injection
        result = integration.intercept_task_tool(params)

        print(f"\nAfter injection:")
        print(f"  Profile selected: {result['_context_metadata']['profile']}")
        print(f"  Confidence: {result['_context_metadata']['confidence']:.1%}")
        print(f"  Method: {result['_context_metadata']['method']}")
        print(f"  Tokens injected: {result['_context_metadata']['tokens']}")
        print(f"  Injection time: {result['_context_metadata']['injection_time']:.3f}s")
        print(f"  Enhanced prompt length: {len(result['prompt'])} chars")

    elif args.command == 'metrics':
        metrics = integration.get_metrics()
        print(json.dumps(metrics, indent=2))

    elif args.command == 'simulate':
        scenarios = {
            'story': {
                'description': 'Create user story for authentication feature',
                'prompt': 'Users need email/password login with 2FA support',
                'subagent_type': 'story-writer'
            },
            'bug': {
                'description': 'Fix login crash on mobile devices',
                'prompt': 'App crashes when login button pressed on iOS',
                'subagent_type': 'general-purpose'
            },
            'parallel': [
                {
                    'description': 'Build authentication UI',
                    'prompt': 'Create login and registration forms',
                    'subagent_type': 'ui-developer'
                },
                {
                    'description': 'Create auth API endpoints',
                    'prompt': 'Build /login and /register endpoints',
                    'subagent_type': 'backend-developer'
                },
                {
                    'description': 'Write authentication tests',
                    'prompt': 'Test login, register, and 2FA flows',
                    'subagent_type': 'tester'
                }
            ]
        }

        if args.scenario == 'parallel':
            print("\nSimulating parallel agent spawning:")
            for i, params in enumerate(scenarios['parallel'], 1):
                result = integration.intercept_task_tool(params)
                print(f"\nAgent {i}: {params['subagent_type']}")
                print(f"  Profile: {result['_context_metadata']['profile']}")
                print(f"  Tokens: {result['_context_metadata']['tokens']}")
        else:
            params = scenarios[args.scenario]
            result = integration.intercept_task_tool(params)
            print(f"\nScenario: {args.scenario}")
            print(f"  Profile selected: {result['_context_metadata']['profile']}")
            print(f"  Tokens injected: {result['_context_metadata']['tokens']}")


if __name__ == '__main__':
    main()