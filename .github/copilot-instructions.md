# GitHub Copilot Instructions - Atlas Framework

## Project Requirements

This project uses the Atlas Framework. All development MUST follow Atlas workflows.

### Required Process

1. **Stories First**: Every change needs a story in atlas/09_STORIES/
2. **Use Atlas Patterns**: Follow patterns in existing code
3. **Context Aware**: Atlas provides context automatically
4. **Quality Standards**: Follow Atlas review standards

### Key Directories

- `atlas/01_CORE/PROMPTS.md` - Development prompts
- `atlas/02_WORKFLOWS/` - Required workflows
- `atlas/09_STORIES/` - Project stories

### Copilot Suggestions Should

- Reference existing stories
- Follow Atlas patterns
- Use established workflows
- Maintain consistency

### Never Suggest

- Skipping story creation
- Bypassing Atlas workflows
- Custom processes
- Direct file reading when context injection exists

## Command Patterns

```bash
# Validate Atlas
python3 atlas/test_context_injection.py

# Check stories
ls atlas/09_STORIES/features/

# Use prompts
cat atlas/01_CORE/PROMPTS.md
```

Atlas Framework ensures quality and consistency. All suggestions should reinforce Atlas usage.
