# Atlas Safety Requirements

## CRITICAL: Prevent Data Loss

### Rule #1: Git Initialize BEFORE Any Work

**EVERY new project MUST start with:**
```bash
git init
git add .
git commit -m "Initial Atlas setup"
```

**EVERY significant change MUST be committed:**
```bash
git add .
git commit -m "Completed Wave 1: Research"
```

### Rule #2: NEVER Use Destructive Commands

**FORBIDDEN Commands (that destroy work):**
```bash
❌ rm -rf [directory]        # NEVER do this without backup
❌ > filename                 # Overwrites entire file
❌ mv old_code /dev/null      # Deletes forever
❌ find . -delete             # Mass deletion
```

**SAFE Alternatives:**
```bash
✅ mv old_code old_code.backup    # Rename instead of delete
✅ git stash                       # Temporarily store changes
✅ git branch experiment           # Work on branch
✅ cp -r src src_backup           # Backup before changes
```

### Rule #3: Version Control Checkpoints

**Mandatory Commit Points:**
1. ✅ After project initialization
2. ✅ After each successful wave
3. ✅ After build verification passes
4. ✅ Before ANY deletion or major refactor
5. ✅ After adversarial review passes

## Script Safety Requirements

### All Scripts MUST:

1. **Check for existing work**
```bash
if [ -d "important_directory" ]; then
    echo "WARNING: Directory exists. Backup first!"
    exit 1
fi
```

2. **Create backups before modifications**
```bash
cp -r app/src app/src.backup.$(date +%Y%m%d_%H%M%S)
```

3. **Use git for safety**
```bash
# At script start
if ! git diff --quiet; then
    echo "Uncommitted changes detected. Commit first!"
    exit 1
fi
```

4. **Confirm destructive operations**
```bash
read -p "This will delete files. Are you sure? (yes/no): " confirm
if [ "$confirm" != "yes" ]; then
    echo "Aborted"
    exit 1
fi
```

## Agent Safety Instructions

### Developer Agents MUST:

1. **Never delete existing code** - Comment out or move to backup
2. **Never overwrite files** - Create new versions (file_v2.kt)
3. **Always preserve work** - Use git branches for experiments
4. **Test in isolation** - Don't break working code

### Orchestrator MUST:

1. **Initialize git** at project start
2. **Commit after each wave** completes successfully
3. **Create branches** for experimental changes
4. **Never approve** destructive operations

## The SmilePile Incident: What Went Wrong

### The Destructive Script:
```bash
# simple-build.sh lines 33-38 (NEVER DO THIS!)
echo "Removing complex components..."
rm -rf app/src/main/java/com/smilepile/cache
rm -rf app/src/main/java/com/smilepile/ui
rm -rf app/src/main/java/com/smilepile/data
rm -rf app/src/main/java/com/smilepile/di
rm -rf app/src/main/java/com/smilepile/util
rm -rf app/src/main/java/com/smilepile/viewmodel
```

**Result**: 100+ files of work destroyed instantly

### What SHOULD Have Happened:
```bash
# SAFE approach
echo "Backing up complex components..."
mkdir -p backup/$(date +%Y%m%d)
mv app/src/main/java/com/smilepile/cache backup/$(date +%Y%m%d)/
# OR
git checkout -b simple-version
# Then make changes on branch
```

## Recovery Procedures

### If Work Is Lost:

1. **Check git reflog** - May recover commits
```bash
git reflog
git checkout <lost-commit-hash>
```

2. **Check system trash** - Some systems don't permanently delete
3. **Check IDE local history** - IntelliJ/Android Studio keep history
4. **Check Time Machine/Backups** - System backups might help

## Enforcement in Atlas

### Add to 00_Prompt.md:
"CRITICAL: Initialize git BEFORE starting any work. Commit after EVERY successful wave. NEVER use rm -rf or destructive commands."

### Add to atlas_context.py:
Developer constraints must include:
- "NEVER delete existing files"
- "ALWAYS create backups before major changes"
- "COMMIT to git after successful builds"

### Add to orchestrator_status.py:
Include git status in progress display:
```
📊 Git Status: 5 commits, on branch: main, clean: ✅
```

## The Three Laws of Atlas Safety

1. **First Law**: An agent may not delete code or, through inaction, allow code to be lost
2. **Second Law**: An agent must preserve all work except where it would conflict with build success
3. **Third Law**: An agent must protect its own work as long as it doesn't conflict with the First or Second Laws

## Summary

The SmilePile incident taught us:
- **Always use version control** - Git first, code second
- **Never use destructive commands** - Move/rename, don't delete
- **Commit frequently** - After every successful milestone
- **Scripts need safety checks** - Confirm before destroying
- **Agents must preserve work** - No data loss allowed

This ensures we NEVER lose work again.