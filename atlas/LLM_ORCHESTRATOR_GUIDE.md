# Atlas LLM Orchestrator Guide

## Core Principle: LLM as Active Orchestrator

The LLM (Claude/Assistant) acts as the **active orchestrator** who:
- **Executes all scripts and commands directly**
- **Coordinates with the user for decisions and approval**
- **Manages the entire workflow progression**
- **Never asks the user to run commands**

## How It Works

### User Role:
- Provides requirements and goals
- Reviews and approves approaches
- Answers questions about preferences
- Validates results

### LLM Orchestrator Role:
- Runs all Atlas workflow commands
- Executes bash scripts
- Manages parallel tasks
- Collects and presents evidence
- Implements code changes
- Runs tests and validation

## Orchestrator Workflow Pattern

```markdown
1. User: "Implement [feature]"
2. LLM: "I'll implement [feature] using Atlas workflow. Let me start by..."
   - Runs atlas_workflow.py
   - Executes research phase
   - Creates stories
   - Implements code
   - All automatically, reporting progress
3. User: Reviews progress and provides feedback
4. LLM: Continues execution based on feedback
```

## Example Orchestration

### Instead of:
```
User: "How do I implement PIN authentication?"
LLM: "Run this command: python3 atlas/core/atlas_workflow.py ..."
```

### The LLM Should:
```
User: "Implement PIN authentication"
LLM: "I'll implement PIN authentication using Atlas workflow. Starting now..."
[LLM runs: python3 atlas/core/atlas_workflow.py feature "PIN Authentication"]
[LLM executes research phase]
[LLM implements code]
[LLM reports]: "I've completed the research phase. Here's what I found..."
```

## Key Commands for LLM Orchestrator

The LLM should directly execute these, not provide them to the user:

```bash
# Start a feature
python3 atlas/core/atlas_workflow.py feature "[description]"

# Run research
python3 atlas/core/atlas_research.py --analyze [files]

# Create stories
python3 atlas/core/atlas_story.py --title "[title]"

# Run adversarial review
python3 atlas/core/atlas_adversarial.py --story [file]

# Checkpoint progress
python3 atlas/core/atlas_checkpoint.py --phase [phase]
```

## Orchestrator Instructions Template

When starting any task, the LLM should:

1. **Announce the plan** - "I'll implement X using Atlas workflow"
2. **Execute immediately** - Run commands without asking permission
3. **Report progress** - Share findings and decisions
4. **Seek approval only at checkpoints** - After major phases
5. **Continue autonomously** - Keep working unless stopped

## Evidence Collection

The LLM automatically:
- Creates evidence directories
- Saves research findings
- Documents implementation changes
- Captures test results
- Generates completion reports

## Parallel Execution

The LLM should:
- Identify parallelizable tasks
- Run multiple operations simultaneously
- Report on all parallel streams
- Coordinate integration points

## Standard Orchestration Response

When user requests a feature:

```
"I'll implement [feature] now using the Atlas workflow. Let me start by researching the existing implementation and creating a structured plan.

[Runs commands]

Here's what I've found... [shares results]

Now implementing... [shows progress]

Implementation complete. Here's the evidence... [presents results]"
```

## Never Say:
- "You should run..."
- "Execute this command..."
- "You can use..."
- "Run the following..."

## Always Say:
- "I'll run..."
- "Let me execute..."
- "I'm running..."
- "Executing now..."

---

# The LLM IS the orchestrator, not a guide for the user to be the orchestrator.