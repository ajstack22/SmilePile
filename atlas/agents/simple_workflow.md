# Simple Atlas Agent Workflow for Claude Code

## The Reality Check
Claude Code has context limits. Here's how to use agents effectively within those constraints.

## The 80/20 Rule for Agents

### For 80% of Tasks (Simple Sequential)
```
"Implement [TASK]. Use Atlas workflow with single agents per phase."
```

This runs:
- 1 Researcher → 1 Product Manager → 1 Developer → 1 Reviewer → 1 Developer → 1 QA → Done

### For 20% of Tasks (Limited Parallel)
```
"Implement [COMPLEX TASK]. Research thoroughly, then use 2 parallel developers."
```

This runs:
- 1 Researcher → 1 PM → 2 Parallel Devs → 1 QA → Done

## Real Example: Add User Avatar Feature

### ❌ TOO COMPLEX (Will overwhelm context)
```
Launch 5 researcher agents for:
- Database schema
- UI components
- Image processing
- Storage service
- Authentication

Then 3 PM agents for different aspects
Then 6 developer agents for all components
Then 4 QA agents for platforms
```

### ✅ JUST RIGHT (Fits in context)
```
Phase 1: Launch researcher agent to find avatar-related code
Phase 2: Launch PM agent to create story
Phase 3: Launch developer agent to plan approach
Phase 4: Launch reviewer agent for edge cases
Phase 5: Launch 2 developers:
  - Dev 1: UI and image handling
  - Dev 2: Backend storage
Phase 6: Launch QA agent to test
Phase 7: Launch PM agent to validate
Phase 8: Launch organizer to clean up
```

## Quick Decision Tree

```
Is the task simple? (< 3 files)
  └─ YES → Sequential single agents
  └─ NO → Continue...

Can it be split into 2-3 independent parts?
  └─ YES → Use 2-3 parallel agents in Phase 5 only
  └─ NO → Sequential single agents

Is it urgent?
  └─ YES → Skip to essential phases (Research → Plan → Code → Test)
  └─ NO → Full workflow
```

## Templates for Common Scenarios

### Bug Fix Template
```
"Fix [BUG]. Atlas workflow:
1. Research to find bug location
2. Quick story with fix criteria
3. Developer fixes it
4. QA verifies
Total: 4 agents, sequential"
```

### Small Feature Template
```
"Add [FEATURE]. Atlas workflow:
1. Research existing patterns
2. PM creates requirements
3. Developer plans and codes
4. QA tests
5. Organizer cleans up
Total: 5 agents, sequential"
```

### Medium Feature Template
```
"Implement [FEATURE]. Atlas workflow:
1. Thorough research
2. Detailed story
3. Technical planning
4. Adversarial review
5. TWO parallel developers (UI + Backend)
6. QA testing
7. Clean-up
Total: 8 agents, 2 parallel"
```

## Context Management Tips

### DO ✅
- Save key findings between phases
- Keep agent prompts under 500 words
- Summarize before hand-offs
- Focus agents on specific tasks

### DON'T ❌
- Pass entire codebases between agents
- Create deeply nested parallel structures
- Launch agents without clear objectives
- Forget to save important outputs

## Emergency Shortcuts

### When Context is Nearly Full
```
"Quick fix for [ISSUE]:
1. One researcher finds it
2. One developer fixes it
3. One QA tests it"
```

### When You Need Speed
```
"Fast implementation:
Research + Code in parallel, then test"
```

### When Simple is Better
```
"Just fix [SPECIFIC ISSUE] - no ceremony"
```

## The Golden Rules

1. **Start Sequential** - Only parallelize when you KNOW tasks are independent
2. **2-3 Max** - Never launch more than 3 agents in parallel
3. **Save Outputs** - Critical findings must be saved between phases
4. **Stay Focused** - Each agent gets ONE clear job
5. **Know When to Stop** - Not every task needs 8 phases

## Remember

The Atlas agent workflow is powerful, but within Claude Code, **simple and sequential beats complex and parallel** most of the time. Start simple, add complexity only when needed.