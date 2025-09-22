# Automatic LLM-Driven Context Injection Guide

## Overview

The Automatic Context Injection System seamlessly provides context to LLM agents without any manual intervention. When an LLM (Claude, GPT, etc.) spawns an agent using the Task tool, the system automatically:

1. **Detects** the type of task from agent type and description
2. **Selects** the appropriate context profile
3. **Assembles** all relevant documentation
4. **Injects** context into the agent's prompt
5. **Inherits** context from parent agents when applicable

All of this happens in <500ms, transparently, with zero manual commands.

## How It Works

### The Magic Flow

```python
# What the LLM writes (naturally):
Task(
    description="Fix the login crash bug",
    prompt="Users report app crashes on login",
    subagent_type="general-purpose"
)

# What happens automatically:
# 1. System detects "fix", "crash", "bug" → troubleshooting needed
# 2. Loads troubleshooting process, debugging guides, logging standards
# 3. Injects 35K tokens of context
# 4. Agent receives complete context + specific task
# 5. Bug gets fixed with proper process
```

### No More Manual Context

#### Before (Manual)
```python
# LLM had to do this:
"Please read these files:
- /atlas/02_WORKFLOWS/05_TROUBLESHOOTING_PROCESS.md
- /atlas/07_AUTOMATION/05_troubleshoot.py
- /atlas/01_CORE/DEBUGGING_GUIDE.md

Then fix the bug..."

# Problems:
# - Files might not load
# - Token waste on navigation
# - Inconsistent results
```

#### After (Automatic)
```python
# LLM just does this:
Task("Fix login crash", prompt, "bug-fixer")

# Context automatically injected!
# Agent has everything needed
```

## Agent Type Mapping

### Specialized Agents

| Agent Type | Auto-Context | Use For |
|------------|--------------|---------|
| `story-writer` | Story creation process, templates | Requirements, epics, user stories |
| `ui-developer` | UI patterns, frontend guides | Components, styling, UX |
| `backend-developer` | API patterns, server guides | Endpoints, databases, services |
| `bug-fixer` | Troubleshooting process | Debugging, issue resolution |
| `researcher` | Research methods | Investigation, analysis |
| `reviewer` | Review process, quality rubric | Code review, feedback |
| `tester` | Testing standards, patterns | Test creation, validation |
| `deployer` | Deployment procedures | Releases, rollouts |

### General-Purpose Agent

The `general-purpose` agent type uses intelligent keyword detection:

```python
# These all get the right context automatically:
Task("Create auth story", ..., "general-purpose")  # → story_creation
Task("Debug memory leak", ..., "general-purpose")  # → troubleshooting
Task("Build login UI", ..., "general-purpose")     # → ui_development
Task("Write API tests", ..., "general-purpose")    # → testing
```

## Smart Feature Detection

The system automatically detects features from task descriptions:

### Authentication Tasks
Keywords: `auth`, `login`, `oauth`, `jwt`, `password`
```python
Task("Add OAuth login", ...)  # Automatically includes auth patterns
```

### Performance Tasks
Keywords: `performance`, `optimize`, `slow`, `speed`
```python
Task("Optimize dashboard load", ...)  # Includes performance guides
```

### Database Tasks
Keywords: `database`, `sql`, `migration`, `schema`
```python
Task("Create user migration", ...)  # Includes database patterns
```

## Context Inheritance

Sub-agents automatically inherit parent context:

```python
# Orchestrator spawns story writer
story_agent = Task("Create epic", ...)  # Gets story context

# Story writer spawns sub-agents
ui_agent = Task("Define UI requirements", ...)  # Inherits story context + UI context
api_agent = Task("Define API requirements", ...)  # Inherits story context + API context
```

## Real-World Examples

### Example 1: Creating a Feature Story

```python
# LLM receives request: "Create a story for CSV export feature"

# LLM naturally writes:
result = Task(
    description="Create story for dashboard CSV export",
    prompt="Users need to export data with date ranges and filtering",
    subagent_type="story-writer"
)

# Automatic injection provides:
# - Story creation process (02_WORKFLOWS/02_STORY_CREATION_PROCESS.md)
# - Story templates (05_TEMPLATES/STORY_TEMPLATE.md)
# - Acceptance criteria patterns
# - INVEST criteria guidelines
# Total: ~30K tokens of story context

# Result: Perfectly formatted Atlas story
```

### Example 2: Fixing a Bug

```python
# LLM receives: "Fix the image upload crash"

# LLM writes:
fix = Task(
    description="Debug and fix image upload crash for large files",
    prompt="Stack overflow in ImageProcessor.recursiveResize",
    subagent_type="general-purpose"
)

# System detects "debug", "fix", "crash" → troubleshooting profile
# Automatic injection provides:
# - Troubleshooting process
# - Debugging patterns
# - Error handling guides
# - Logging standards

# Result: Systematic debugging with proper fix
```

### Example 3: Parallel Development

```python
# LLM coordinates feature development

agents = [
    Task("Build auth UI components", ..., "ui-developer"),
    Task("Create auth API endpoints", ..., "backend-developer"),
    Task("Write auth integration tests", ..., "tester")
]

# Each agent automatically gets specialized context:
# - ui-developer: UI patterns + auth UI guidelines
# - backend-developer: API patterns + auth security
# - tester: Testing standards + auth test patterns

# All injections happen in parallel, <1 second total
```

## Configuration

### Customizing Agent Mappings

Edit `config/agent_context_mapping.yaml`:

```yaml
agent_types:
  my-custom-agent:
    default_profile: "my_profile"
    detection_enabled: false
    description: "Custom specialist agent"
```

### Adding Keyword Detection

```yaml
keyword_detection:
  rules:
    - keywords: ['payment', 'checkout', 'stripe', 'billing']
      profile: "payment_processing"
      confidence: 0.9
      min_matches: 2
```

### Feature Detection

```yaml
feature_detection:
  payments:
    keywords: ['payment', 'stripe', 'checkout', 'billing']
    additional_context:
      - "payment_patterns"
      - "pci_compliance"
```

## Monitoring & Metrics

### View Injection Metrics

```bash
python task_context_integration.py metrics
```

Output:
```json
{
  "total_injections": 1247,
  "cache_hit_rate": 0.82,
  "detection_override_rate": 0.15,
  "average_injection_time": 0.043,
  "success_rate": 0.97
}
```

### Validate Integration

```bash
python task_context_integration.py validate
```

Checks:
- Context injector availability
- Agent mappings loaded
- Test injection successful

## Testing Context Injection

### Test Specific Scenarios

```bash
# Test story creation
python task_context_integration.py test \
  --description "Create user story for notifications" \
  --agent-type story-writer

# Test bug fixing
python task_context_integration.py test \
  --description "Fix crash in payment processing" \
  --agent-type general-purpose

# Test feature detection
python task_context_integration.py test \
  --description "Add OAuth authentication to API" \
  --agent-type backend-developer
```

### Simulate Common Scenarios

```bash
# Simulate story creation
python task_context_integration.py simulate --scenario story

# Simulate bug fix
python task_context_integration.py simulate --scenario bug

# Simulate parallel agents
python task_context_integration.py simulate --scenario parallel
```

## Performance

### Injection Speed

| Operation | Time | Notes |
|-----------|------|-------|
| Agent type mapping | <5ms | Direct lookup |
| Keyword detection | <10ms | For general-purpose |
| Context assembly | <200ms | From cache |
| Total injection | <250ms | Full process |
| Parallel (10 agents) | <1s | All agents |

### Token Efficiency

| Metric | Before | After | Savings |
|--------|--------|-------|---------|
| Context tokens | 60K | 35K | 42% |
| Navigation tokens | 15K | 0 | 100% |
| Total per task | 75K | 35K | 53% |

## Troubleshooting

### Context Not Injecting

1. **Check Integration Status**
```bash
python task_context_integration.py validate
```

2. **Verify Agent Type**
- Ensure agent type is recognized
- Check `agent_context_mapping.yaml`

3. **Review Keyword Detection**
- For general-purpose agents, check keywords
- Adjust confidence thresholds if needed

### Wrong Context Selected

1. **Enable Debug Logging**
```python
# In task description, add keywords
Task("Fix bug in authentication system", ...)  # More specific
```

2. **Use Specific Agent Types**
```python
# Instead of general-purpose
Task(..., subagent_type="bug-fixer")  # Explicit type
```

### Performance Issues

1. **Check Cache**
```bash
python enhanced_context_injector.py cache --stats
```

2. **Clear Cache if Stale**
```bash
python enhanced_context_injector.py cache --clear
```

## Integration with Claude Code

The system integrates seamlessly with Claude Code's Task tool:

```python
# In Claude Code initialization
from task_context_integration import register_with_claude_code

# Register the hook
hook = register_with_claude_code()

# Now all Task calls get automatic context
```

## Best Practices

### 1. Use Specific Agent Types
```python
# Better
Task(..., subagent_type="story-writer")

# Okay (relies on detection)
Task(..., subagent_type="general-purpose")
```

### 2. Include Keywords in Descriptions
```python
# Better
Task("Debug and fix the login crash bug", ...)

# Vague
Task("Handle the issue", ...)
```

### 3. Leverage Feature Detection
```python
# Automatically includes auth context
Task("Implement OAuth authentication", ...)
```

### 4. Let Context Inherit
```python
# Parent context flows to children automatically
# No need to repeat context
```

## Advanced Usage

### Custom Context Rules

Create custom detection rules for your domain:

```python
class CustomTaskIntegration(TaskContextIntegration):
    def _determine_context_profile(self, agent_type, description, prompt):
        # Custom logic for your domain
        if "machine learning" in description.lower():
            return "ml_development", 0.95, "custom_rule"

        # Fall back to standard detection
        return super()._determine_context_profile(agent_type, description, prompt)
```

### Context Middleware

Add preprocessing or postprocessing:

```python
def context_middleware(context):
    # Add timestamps
    context = f"[Generated: {datetime.now()}]\n{context}"

    # Add warnings
    if "deprecated" in context:
        context = "⚠️ DEPRECATED PATTERNS DETECTED\n" + context

    return context
```

### A/B Testing Contexts

Test different context configurations:

```python
# Enable A/B testing in config
optimization:
  ab_testing:
    enabled: true
    variants:
      - name: "minimal"
        size_budget: 20000
      - name: "comprehensive"
        size_budget: 50000
```

## Migration from Manual Context

### Before: Manual Script Running
```bash
# Developer had to:
python context_injector.py build --task troubleshooting > context.txt
# Then paste into LLM prompt
```

### After: Automatic Injection
```python
# LLM just spawns agent
Task("Fix bug", prompt, subagent_type)
# Context injected automatically!
```

### Rollback Plan

If needed, disable automatic injection:

```yaml
# In agent_context_mapping.yaml
integration:
  claude_code:
    enabled: false  # Disable auto-injection
```

## FAQ

**Q: How do I know what context was injected?**
A: Check the metadata in the enhanced prompt:
```
[CONTEXT AUTO-INJECTED]
Profile: troubleshooting (confidence: 90%, method: keyword_detection)
```

**Q: Can I override the automatic selection?**
A: Yes, use specific agent types instead of general-purpose.

**Q: What if context is too large?**
A: The system automatically manages size budgets and summarizes when needed.

**Q: How fast is injection?**
A: Typically <250ms, with caching <50ms.

**Q: Does this work with all LLMs?**
A: Yes, it's LLM-agnostic. Works with any system using the Task tool pattern.

## Summary

The Automatic Context Injection System makes Atlas truly LLM-native:

✅ **Zero manual intervention** - Context injection is 100% automatic
✅ **Intelligent detection** - Right context every time
✅ **Fast** - <250ms injection time
✅ **Efficient** - 50% reduction in token usage
✅ **Reliable** - Deterministic, cached, validated

Just spawn agents naturally - the system handles the rest!

---

*Automatic Context Injection System - Version 1.0.0*
*Part of the Atlas Framework 2.2*