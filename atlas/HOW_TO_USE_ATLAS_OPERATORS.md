# How to Use Atlas Operator Mode

## The Problem
When you ask an LLM to "fix a bug using Atlas workflow", it often:
- Just searches for files normally
- Doesn't use Atlas standards
- Misses the context and checklists
- Doesn't create proper stories

## The Solution: Operator Scripts

Atlas provides **operator scripts** that automatically orchestrate the full workflow with context injection.

## For Bug Fixes

### What You Say:
```
I'm having a problem where the app crashes when I click import.

Run this Atlas operator command:
python3 atlas/fix_bug.py "App crashes when clicking import button"

Then follow the generated workflow to fix the issue.
```

### What Happens:
1. **Bug story created** automatically in `atlas/09_STORIES/bugs/`
2. **Context injected** with troubleshooting documentation
3. **Checklist included** for verification steps
4. **Workflow prompt generated** with exact steps to follow
5. **LLM receives everything** needed to fix the bug properly

### The Result:
The LLM will:
- Read the generated bug story
- Follow the investigation steps
- Update the story with findings
- Implement the fix using Atlas standards
- Add tests and documentation
- Mark story as RESOLVED

## Complete Example Request

Here's exactly what to paste to your LLM:

```
I'm experiencing a bug where users can't submit the payment form - it shows a spinning loader forever.

Please execute this Atlas operator command first:
```bash
python3 atlas/fix_bug.py "Payment form hangs with infinite spinner on submit"
```

Then follow the generated Atlas workflow to:
1. Investigate and reproduce the issue
2. Update the bug story with your findings
3. Fix the bug following Atlas standards
4. Add regression tests
5. Document the resolution

Make sure to update the bug story file as you progress through each phase.
```

## Why This Works Better

### Without Operator (LLM Freestyle):
```
❌ No structured story
❌ Missing context
❌ No checklist
❌ Inconsistent approach
❌ May not follow standards
```

### With Operator (Atlas Mode):
```
✅ Automatic story creation
✅ Full context injection (2000+ tokens)
✅ Verification checklist
✅ Step-by-step workflow
✅ Enforced standards
✅ Consistent quality
```

## Advanced Usage

### Specify Bug Area:
```bash
python3 atlas/atlas_bug_workflow.py -d "Login fails with OAuth" -a "Authentication"
```

### Set Severity:
```bash
python3 atlas/atlas_bug_workflow.py -d "Minor UI glitch" -s LOW
```

### Save Workflow to File:
```bash
python3 atlas/atlas_bug_workflow.py -d "Database timeout" -o workflow.md
```

## Tips for Best Results

1. **Be Specific** in bug descriptions
2. **Run the operator first** before asking for fixes
3. **Reference the story file** in your request
4. **Ask for story updates** as investigation progresses
5. **Verify checklist completion** before closing

## Coming Soon

- `atlas/create_feature.py` - Feature story automation
- `atlas/run_tests.py` - Test execution with context
- `atlas/deploy.py` - Deployment workflow automation

## Remember

The key phrase is: **"Run this Atlas operator command first"**

This ensures the LLM enters "operator mode" and follows the structured workflow rather than freestyling the solution.