# Atlas Workflow Usage Guide

## Agent-Driven Workflow (Recommended) 🆕

The Atlas workflow now uses intelligent agents for each phase, enabling parallel execution and higher quality results.

## 🎯 Primary Command - Agent Orchestration

### Simple Pattern for Any Task:
```
"I need to [TASK]. Use the Atlas agent workflow with practical parallelization."
```

This triggers:
- Smart agent selection per phase
- Parallel execution where beneficial
- Context-aware handoffs
- Automatic documentation

### Context-Aware Example:
```
"Fix the login bug where users can't sign in with email.
Use Atlas workflow: one researcher, then sequential agents,
parallel implementation only if needed."
```

## 📋 Agent Phases (Simple & Practical)

### Phase-by-Phase Agent Usage:

#### 1. Research Phase
```
Launch researcher agent: "Find all files related to [feature/bug]"
```

#### 2. Story Creation
```
Launch product-manager agent: "Create story with acceptance criteria"
```

#### 3. Planning
```
Launch developer agent: "Plan implementation approach"
```

#### 4. Adversarial Review
```
Launch peer-reviewer agent: "Find edge cases and risks"
```

#### 5. Implementation
```
Launch 2-3 developer agents if needed:
- Agent 1: Frontend changes
- Agent 2: Backend changes
```

#### 6. Testing
```
Launch QA agent: "Test all acceptance criteria"
```

#### 7. Validation
```
Launch product-manager agent: "Validate requirements met"
```

#### 8. Clean-up 🆕
```
Launch organizer agent: "Clean up work artifacts"
```

## 🚀 Practical Examples for Claude Code

### Simple Bug Fix (Sequential):
```
"Fix the null pointer error in login. Use Atlas workflow:
1. Research agent finds the bug
2. Sequential agents for story/plan/review
3. Developer agent fixes it
4. QA agent tests
5. Clean up"
```

### Feature Addition (Limited Parallel):
```
"Add dark mode to settings. Use Atlas workflow with:
- One researcher for all theme files
- Sequential story/plan/review
- Two parallel devs: UI and state management
- Sequential test/validate/cleanup"
```

### Keep It Simple:
```
"Implement [feature]. Use standard Atlas workflow,
only parallelize if you find 3+ independent components."
```

## ✅ What the LLM Should Do

### Phase 1: Research
```
LLM: "Starting research phase. I'll locate all relevant files..."
[Does research]
LLM: "Research complete. Found 5 files including layout variants. Ready to create the story?"
You: "yes"
```

### Phase 2: Story Creation
```
LLM: "Creating story at atlas/09_STORIES/features/ATLAS-002.md..."
[Creates story]
LLM: "Story created with 4 acceptance criteria. Ready for you to review it?"
You: "show me"
[Shows story]
You: "yes"
```

### Phase 3: Planning
```
LLM: "Creating implementation plan..."
[Creates plan]
LLM: "Plan ready with 3 file changes. Ready to review the plan?"
You: "yes"
```

### Phase 4: Adversarial Review
```
LLM: "Running adversarial review..."
[Finds issues]
LLM: "Found 2 potential issues: landscape layout and dark theme. Ready to address these?"
You: "yes"
```

### Phase 5: Implementation
```
LLM: "Beginning implementation..."
[Modifies file]
LLM: "Updated CategoriesFragment.kt. 1 of 3 files complete. Continue?"
You: "yes"
```

### Phase 6: Testing
```
LLM: "Running tests..."
[Tests]
LLM: "All tests passing. Ready to see results?"
You: "yes"
```

### Phase 7: Validation
```
LLM: "Validating all acceptance criteria..."
[Validates]
LLM: "All requirements met. Ready to close the story?"
You: "yes"
```

## 🛑 If LLM Goes Off Rails

### They skip ahead:
```
"STOP. You skipped Phase 2. Go back and create the story first."
```

### They don't ask for confirmation:
```
"Stop. Ask me for confirmation before proceeding to the next phase."
```

### They start coding too early:
```
"NO CODE until Phase 5. Complete the planning phase first."
```

## 📊 Evolution to Agent-Based System

| Phase | Legacy (Scripts) | New (Agents) | Benefit |
|---|---|---|---|
| Research | `atlas_research.py` | Researcher agent | Parallel searches |
| Story | `atlas_story.py` | Product Manager agent | Better requirements |
| Planning | Manual | Developer agent | Technical expertise |
| Review | `atlas_adversarial.py` | Peer Reviewer agent | Finds more issues |
| Implementation | Manual coding | Developer agents | Parallel coding |
| Testing | Manual | QA agent | Systematic coverage |
| Validation | Manual | Product Manager agent | Criteria verification |
| Clean-up | None | Organizer agent 🆕 | No debt accumulation |

## 🎯 Key Improvements with Agents

### Agent System Advantages:
- **Speed**: 3-5x faster through parallelization
- **Quality**: Specialized agents for each domain
- **Consistency**: Agents follow patterns reliably
- **Documentation**: Automatic and complete
- **Clean-up**: New phase prevents technical debt

### Practical Constraints:
- Keep parallel agents to 2-3 maximum
- Use sequential for simple tasks
- Save context between phases
- Don't over-orchestrate

## 💡 Best Practice

Always use `atlas_workflow.py` as your main command. It enforces the complete process and prevents the LLM from taking shortcuts.

The individual phase scripts are there if you need to:
- Re-run a specific phase
- Get more detailed guidance
- Debug a particular step

But for normal usage, the workflow script handles everything.