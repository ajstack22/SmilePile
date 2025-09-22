# Atlas Visibility & Tracking

## 🎯 Core Principle
**100% visibility of work status without hassle** - Know what's done, what's in progress, and what's next at a glance.

## 📊 Dashboard (Overall Project View)

```bash
python3 dashboard.py
```

Shows:
- Project status and phase
- Progress percentage with visual bar
- Current sprint items
- Recent completions
- Upcoming work
- Key metrics (velocity, WIP, blockers)
- Health indicators

## 🎯 Kanban Board (Work Status)

```bash
python3 kanban.py                    # Text kanban board
python3 kanban.py --format markdown  # Markdown format (for docs)
python3 kanban.py --format json      # JSON export
```

Visual columns:
- 📋 BACKLOG - Work not started
- 🎯 READY - Ready to start
- 🚧 IN PROGRESS - Active work
- 👀 IN REVIEW - Being reviewed
- 🧪 TESTING - In testing
- ✅ DONE - Completed
- 🚫 BLOCKED - Needs help

## 📋 Backlog (Prioritized Work Queue)

```bash
python3 kanban.py backlog
```

Shows prioritized list:
- 🔴 CRITICAL - Do immediately
- 🟠 HIGH - Do next
- 🟡 MEDIUM - Standard priority
- 🟢 LOW - When time permits

## 📈 Metrics & Velocity

```bash
python3 kanban.py metrics
```

Tracks:
- Velocity (items/week)
- Cycle time
- WIP limits
- Bottlenecks

## 🏃 Daily Standup

```bash
python3 dashboard.py standup
```

Generates standup report:
- Yesterday's completions
- Today's focus
- Current blockers
- What's next

## 📝 Story Management

### List All Items
```bash
python3 02_create_story.py list all          # All items
python3 02_create_story.py list story        # Just stories
python3 02_create_story.py list bug          # Just bugs
python3 02_create_story.py list --status backlog  # By status
```

### Update Status
```bash
python3 02_create_story.py update S001 in_progress
python3 02_create_story.py update B002 blocked
python3 02_create_story.py update S003 done
```

### Quick Add
```bash
python3 02_create_story.py story "Add login" --priority high
python3 02_create_story.py bug "Fix crash" --severity critical
```

## 🔄 Workflow Tracking

```bash
python3 03_adversarial_workflow.py status    # Current workflow
python3 03_adversarial_workflow.py report    # Detailed report
```

## 📊 Status Values

Standard kanban flow:
1. `backlog` - Not started
2. `ready` - Ready to start
3. `in_progress` - Being worked on
4. `in_review` - Code review
5. `testing` - Testing phase
6. `done` - Complete
7. `blocked` - Stuck, needs help

## 🚀 Quick Start Your Day

```bash
# 1. Check overall status
python3 dashboard.py

# 2. Review kanban board
python3 kanban.py

# 3. Update any status changes
python3 02_create_story.py update S001 in_progress

# 4. Check for blockers
python3 kanban.py backlog | grep blocked
```

## 📱 Mobile-Friendly Output

All commands produce text output that's:
- Easy to read on any screen
- Copy-paste friendly
- Slack/Discord ready
- Terminal optimized

## 🎨 Visual Indicators

- 🚧 = Work in progress
- ✅ = Completed
- 🚫 = Blocked
- 🔴 = Critical priority
- 🟠 = High priority
- 🟡 = Medium priority
- 🟢 = Low priority
- 📋 = Backlog
- 🎯 = Ready to start
- 👀 = In review
- 🧪 = Testing

## 💡 Pro Tips

1. **Start each day with**: `python3 dashboard.py`
2. **Track progress with**: `python3 kanban.py`
3. **Update immediately**: Don't batch status updates
4. **Keep WIP low**: Max 3 items in progress
5. **Clear blockers fast**: Check blocked items daily
6. **Use priorities**: Mark critical items clearly

## 🔄 Continuous Visibility

The system auto-updates when you:
- Create new stories
- Update statuses
- Complete workflows
- Mark items done

No manual tracking needed - just use the Atlas scripts and visibility is automatic!

## 📊 Example Output

### Dashboard
```
===============================================================================
                           ATLAS PROJECT DASHBOARD
===============================================================================
🚀 PROJECT: SmilePile
📅 Started: 2024-01-15
🎯 Phase: development

📊 WORK STATUS
Progress: [████████████░░░░░░░░░░░░░] 12/30 (40.0%)
🚧 In Progress: 3  |  ✅ Done: 12  |  🚫 Blocked: 1

🏃 CURRENT SPRINT
🚧 [S001] Setup Android project with ViewPager2
🚧 [S002] Create category data model
👀 [S003] Build category UI with titles
🚫 [B001] Fix image loading crash

✅ RECENT COMPLETIONS
✓ [S009] Add swipe gesture detection
✓ [S008] Configure Room database
```

### Kanban Board
```
📋 BACKLOG (8)        🚧 IN PROGRESS (3)     ✅ DONE (12)
--------------        -----------------      ------------
📝 [S004] Import      📝 [S001] Setup        📝 [S009] Swipes
📝 [S005] Camera      📝 [S002] Models       📝 [S008] Database
📝 [S006] Zoom        🐛 [B001] Crash        📝 [S007] Layout
```

This gives you and any stakeholders instant visibility into project status without any manual tracking overhead!