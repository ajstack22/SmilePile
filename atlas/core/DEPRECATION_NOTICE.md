# Python Scripts Deprecation Notice

## Status: LEGACY REFERENCE ONLY

As of the integration with Claude Code's agent system, the Python workflow scripts in this directory are maintained for reference but are no longer the recommended approach.

## Migration Path

### Instead of Python Scripts, Use Agents:

| Old Script | New Approach | Agent Type |
|------------|--------------|------------|
| `atlas_workflow.py` | Agent orchestration | Multiple specialized agents |
| `atlas_research.py` | Research agent | `general-purpose` |
| `atlas_story.py` | Product Manager agent | `product-manager` |
| `atlas_adversarial.py` | Peer Reviewer agent | `peer-reviewer` |
| `atlas_checkpoint.py` | Built into agent flow | N/A |

### Why the Change?

1. **Parallel Execution**: Agents can work simultaneously
2. **Specialized Intelligence**: Each agent optimized for its role
3. **Better Integration**: Native to Claude Code environment
4. **Automatic Documentation**: Agents handle this naturally
5. **No Script Maintenance**: Agents self-update with improvements

### How to Use the New System

Instead of:
```bash
python3 atlas_workflow.py feature "description"
```

Use:
```
"Implement [feature] using Atlas agent workflow"
```

### Should I Delete These Scripts?

**No.** Keep them as reference for:
- Understanding the workflow structure
- Fallback if agent system unavailable
- Historical documentation
- Learning the Atlas philosophy

### For More Information

See:
- `/atlas/docs/AGENT_WORKFLOW.md` - Complete agent guide
- `/atlas/agents/` - Agent role definitions
- `/atlas/agents/simple_workflow.md` - Practical usage

## Note

These scripts still work and can be used, but the agent-based approach provides superior results with less friction.