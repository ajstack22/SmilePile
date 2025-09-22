# Atlas Context Injection System - Setup Guide

## Overview

The Atlas Context Injection System automatically provides appropriate context to LLM agents when they are spawned via the Task tool. This eliminates the need for manual context management and ensures every agent receives exactly the documentation needed for their task.

## Quick Start

### 1. Enable Context Injection in Claude Code

```python
# In your Claude Code initialization or startup script:
from automation.claude_code_integration import register

# Enable auto-context injection
if register():
    print("✅ Atlas context injection enabled")
```

### 2. Verify Installation

```bash
# Test the integration
python 07_AUTOMATION/claude_code_integration.py test

# Check status
python 07_AUTOMATION/claude_code_integration.py status
```

### 3. Use Task Tool Normally

Once enabled, context injection happens automatically:

```python
# Your normal Claude Code usage - no changes needed!
result = Task(
    description="Create user story for dashboard export",
    prompt="Users need CSV export with date filtering",
    subagent_type="story-writer"
)

# Behind the scenes:
# 1. System detects "story-writer" → loads story_creation context
# 2. Injects 30K+ tokens of templates and guides
# 3. Agent receives complete context automatically
```

## How It Works

### Architecture

```
Task Tool Call
     ↓
[Claude Code Integration Hook]
     ↓
Task Context Integration
     ↓
Enhanced Context Injector
     ↓
Context Manifest + Agent Mapping
     ↓
Agent with Injected Context
```

### Key Components

1. **claude_code_integration.py** - Hooks into Claude Code's Task tool
2. **task_context_integration.py** - Determines which context to inject
3. **enhanced_context_injector.py** - Assembles context from manifest
4. **context_manifest.json** - Defines available context profiles
5. **agent_context_mapping.yaml** - Maps agents to context profiles

## Configuration

### Context Profiles

Profiles define what documentation to include for different task types:

- **orchestration** - Coordination and workflow management
- **story_creation** - User stories, epics, requirements
- **troubleshooting** - Debugging and issue resolution
- **ui_development** - Frontend and UI components
- **backend_development** - APIs and backend services
- **testing** - Test creation and execution
- **code_review** - Quality assessment and feedback
- **deployment** - Release and deployment processes
- **documentation** - Documentation creation
- **research** - Investigation and analysis

### Agent Type Mapping

The system automatically maps agent types to appropriate profiles:

```yaml
agent_types:
  story-writer:
    default_profile: "story_creation"
    detection_enabled: false  # Always use story context

  general-purpose:
    default_profile: "orchestration"
    detection_enabled: true   # Allow keyword detection
```

### Keyword Detection

For general-purpose agents, keywords in the task description trigger specific profiles:

```yaml
keyword_detection:
  rules:
    - keywords: ['bug', 'fix', 'crash', 'error', 'debug']
      profile: "troubleshooting"
      confidence: 0.9

    - keywords: ['story', 'requirement', 'epic']
      profile: "story_creation"
      confidence: 0.95
```

## Performance

The system meets these performance targets:

- **<500ms** injection speed per agent ✅
- **<1s** for 10 parallel agents ✅
- **80%** cache hit rate (after warmup)
- **95%** detection accuracy
- **<50ms** context detection time ✅

Run benchmarks:

```bash
# Full benchmark suite
python 07_AUTOMATION/context_injection_benchmark.py

# Quick test
python 07_AUTOMATION/context_injection_benchmark.py --quick
```

## Advanced Usage

### Manual Context Injection

For custom tools or testing:

```python
from automation.task_context_integration import TaskContextIntegration

integration = TaskContextIntegration()

# Manually inject context
params = {
    'description': 'Fix authentication bug',
    'prompt': 'Users cannot log in',
    'subagent_type': 'general-purpose'
}

enhanced = integration.intercept_task_tool(params)
# enhanced['prompt'] now contains injected context
```

### Disable/Enable at Runtime

```python
from automation.claude_code_integration import get_integration

integration = get_integration()

# Temporarily disable
integration.disable()

# Re-enable
integration.enable()
```

### Custom Context Profiles

Add new profiles to `config/context_manifest.json`:

```json
{
  "context_profiles": {
    "custom_profile": {
      "description": "Custom context for special tasks",
      "files": {
        "core": [
          {"path": "path/to/doc.md", "priority": 1}
        ]
      },
      "size_budget": 40000
    }
  }
}
```

## Troubleshooting

### Context Not Being Injected

1. Check integration is active:
   ```bash
   python 07_AUTOMATION/claude_code_integration.py status
   ```

2. Verify configuration files exist:
   - `config/context_manifest.json`
   - `config/agent_context_mapping.yaml`

3. Check logs for errors:
   ```python
   import logging
   logging.basicConfig(level=logging.DEBUG)
   ```

### Wrong Profile Selected

1. Review keyword detection rules in `agent_context_mapping.yaml`
2. Use specific agent types instead of `general-purpose`
3. Check detection confidence thresholds

### Performance Issues

1. Clear context cache:
   ```bash
   rm -rf .atlas/context_cache/*
   ```

2. Reduce context size budgets in manifest
3. Enable parallel file loading in global settings

## Monitoring

View metrics:

```bash
# Show injection metrics
python 07_AUTOMATION/claude_code_integration.py metrics

# Show integration metrics
python 07_AUTOMATION/task_context_integration.py metrics
```

Sample output:
```json
{
  "integration_active": true,
  "total_intercepts": 156,
  "successful_injections": 154,
  "success_rate": 0.987,
  "average_injection_time": 0.0023
}
```

## Best Practices

1. **Let Detection Work** - Use `general-purpose` agents and let keyword detection select the right profile

2. **Cache Warmup** - Run common scenarios once to populate cache for faster subsequent runs

3. **Monitor Metrics** - Check success rates and injection times regularly

4. **Update Profiles** - Keep context profiles current with your documentation

5. **Test Changes** - Run benchmarks after modifying configuration

## FAQ

**Q: Does this slow down agent spawning?**
A: No, average injection time is <5ms, well below the 500ms target.

**Q: Can I disable it for specific tasks?**
A: Yes, use `integration.disable()` before the task and `enable()` after.

**Q: How much context is injected?**
A: Typically 10-40K tokens depending on the profile and task type.

**Q: Does it work with parallel agent spawning?**
A: Yes, tested with 10+ parallel agents completing in <1 second.

**Q: Can I see what context was injected?**
A: Check the `_context_metadata` in the task result or enable debug logging.

## Support

For issues or questions:
- Check logs in `.atlas/logs/`
- Run validation: `python 07_AUTOMATION/task_context_integration.py validate`
- Review benchmarks: `python 07_AUTOMATION/context_injection_benchmark.py`

---

*Atlas Context Injection System v1.0 - Zero manual intervention, 100% automation*