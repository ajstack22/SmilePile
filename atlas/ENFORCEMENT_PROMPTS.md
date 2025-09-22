# Atlas Enforcement Prompts - Force Process Compliance

## The Problem
LLMs tend to jump straight to solutions, skipping the Atlas process entirely. They see a bug and immediately start fixing code instead of following the workflow.

## The Solution: Enforcement Mode

### For Your Photo Display Feature

```
STOP. You failed to follow Atlas process last time and missed UI elements.

Run this enforcement command:
python3 atlas/atlas_enforce.py feature "Remove ALL UI elements from photo display, show only photo maximized, add swipe navigation"

Follow EVERY phase. NO shortcuts.
Do NOT write ANY code until Phase 4.
Document EVERYTHING in the story file.

If you skip steps, I will stop you.
```

### For Bug Fixes

```
There's a bug where [describe bug].

Run enforcement mode:
python3 atlas/atlas_enforce.py bug "[bug description]"

You MUST complete each phase before proceeding.
NO code until the story and plan are complete.
```

## Key Enforcement Phrases

Use these to keep the LLM in line:

### When They Skip Steps:
```
STOP. You're skipping the Atlas process.
Go back and complete the story file first.
NO code until the documentation is complete.
```

### When They Rush:
```
You're moving too fast.
Show me the completed story file.
Show me the implementation plan.
Get my approval before writing code.
```

### When They Miss Requirements:
```
You missed requirements. This is why we have the Atlas process.
Update the story with what you missed.
Document ALL the places that need changes.
Use the adversarial review to find edge cases.
```

## The Enforcement Workflow

### What Enforcement Mode Does:

1. **BLOCKS CODE WRITING** until documentation complete
2. **FORCES STORY CREATION** as first step
3. **REQUIRES RESEARCH** before solutions
4. **DEMANDS APPROVAL** before implementation
5. **ENFORCES VERIFICATION** before completion

### Visual Barriers:

The enforcement script uses:
- ⛔ STOP signs
- ━━━ Visual separators
- □ Checkboxes that must be checked
- CAPS LOCK warnings
- Numbered mandatory phases

## Example: Fixing Your Photo Display

Here's the exact prompt to ensure compliance:

```
The photo display still has buttons (share, edit, favorite) that shouldn't be there.
The photo isn't centered and maximized.

You clearly didn't follow the Atlas Adversarial Workflow.

Now run this and follow it EXACTLY:
python3 atlas/atlas_enforce.py feature "Photo display with ONLY photo, no UI elements, maximized to screen, swipe navigation only"

Requirements enforcement:
1. Create the story file FIRST
2. Document EVERY layout file variant (including layout-h840dp)
3. Find ALL places buttons are defined
4. Create removal plan for ALL UI elements
5. Get my approval on the plan
6. Only THEN implement

If you write code before I approve the plan, you fail.
Show me the story file when Phase 1 is complete.
```

## Compliance Checkpoints

Insert these at each phase:

### After Phase 1:
```
STOP. Show me the story file.
Is every requirement documented?
Continue to Phase 2 only after I confirm.
```

### After Phase 2:
```
STOP. Show me the adversarial review.
Did you find ALL the edge cases?
List every file that needs changes.
```

### After Phase 3:
```
STOP. Show me the implementation plan.
I need to approve before you write code.
```

### After Phase 4:
```
Show me the test results.
Show me screenshots proving it works.
Update the story with evidence.
```

## The Nuclear Option

If the LLM keeps skipping steps:

```
You are failing to follow the Atlas process.
From now on, you must:
1. Show me each step before proceeding
2. Get explicit approval for each phase
3. Update the story file after every change
4. Run atlas_enforce.py before any task

If you skip the process again, I will restart the conversation.
```

## Why This Works

1. **Clear Boundaries** - Can't skip to coding
2. **Forced Documentation** - Must write before doing
3. **Approval Gates** - Human stays in control
4. **Visual Enforcement** - Hard to ignore warnings
5. **Shame Factor** - "You failed" motivates compliance

## Remember

The key is to be firm:
- "You skipped the process"
- "Follow the enforcement mode"
- "No code until documentation"
- "Show me the story first"
- "You missed requirements"

This forces the LLM into process-following mode instead of problem-solving mode.