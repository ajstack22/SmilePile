# Atlas Workflow Scripts Usage Guide

## Complete Workflow System

The new Atlas workflow system ensures proper process compliance through clear phases and mandatory checkpoints.

## 🎯 Primary Command - Full Workflow

### For Any Task (Bug or Feature):
```bash
python3 atlas/atlas_workflow.py feature "Remove FAB, add 3-dot menu with Import/Cancel"
```

This single command provides:
- All 7 phases with clear instructions
- Checkpoint prompts after each phase
- Prevents skipping ahead
- Forces documentation before coding

## 📋 Individual Phase Scripts

If you need to run specific phases:

### 1. Research Phase
```bash
python3 atlas/atlas_research.py feature "description"
```
Guides thorough investigation before any implementation.

### 2. Story Creation
```bash
python3 atlas/atlas_story.py feature "description"
```
Provides story template with all required sections.

### 3. Adversarial Review
```bash
python3 atlas/atlas_adversarial.py atlas/09_STORIES/features/ATLAS-001.md
```
Forces finding edge cases and missed requirements.

### 4. Checkpoint Guide
```bash
python3 atlas/atlas_checkpoint.py guide
```
Shows how to ask for confirmation at each step.

## 🚀 Recommended Usage

### The Perfect Prompt for Categories Menu Task:

```
I need to modify the categories screen: Remove FAB, add 3-dot menu with Import Mode and Cancel options.

Run this command:
python3 atlas/atlas_workflow.py feature "Replace FAB with 3-dot menu containing Import Mode and Cancel on categories screen"

Follow all 7 phases IN ORDER.
Ask me for confirmation after EACH phase.
Do NOT write any code until Phase 5.

Start with Phase 1 (Research) now.
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

## 📊 Scripts Comparison

| Old Scripts (Archived) | New Scripts | Purpose |
|---|---|---|
| atlas_bug_workflow.py | atlas_workflow.py | Complete workflow with all phases |
| fix_bug.py | - | (Replaced by workflow) |
| create_feature.py | - | (Replaced by workflow) |
| atlas_enforce.py | - | (Built into workflow) |
| atlas_guided.py | atlas_checkpoint.py | Confirmation management |
| - | atlas_research.py | Research phase guide |
| - | atlas_story.py | Story creation guide |
| - | atlas_adversarial.py | Quality review |

## 🎯 Key Differences

### Old System:
- Multiple scripts for different tasks
- Context injection focus
- Less structured phases

### New System:
- Single workflow command
- Clear 7-phase process
- Mandatory checkpoints
- Adversarial review built-in
- Prevents rushing to code

## 💡 Best Practice

Always use `atlas_workflow.py` as your main command. It enforces the complete process and prevents the LLM from taking shortcuts.

The individual phase scripts are there if you need to:
- Re-run a specific phase
- Get more detailed guidance
- Debug a particular step

But for normal usage, the workflow script handles everything.