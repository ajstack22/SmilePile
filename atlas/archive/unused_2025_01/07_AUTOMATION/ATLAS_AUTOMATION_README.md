# Atlas Backlog Automation System

A comprehensive, automated backlog management system that reduces manual overhead and keeps stories organized automatically.

## 🚀 Quick Start

```bash
# Install git hooks for automatic story tracking
python atlas_cli.py git install-hooks

# Create a new feature story
python atlas_cli.py story create --type feature --title "Add user authentication" --priority high

# Check backlog status
python atlas_cli.py backlog status

# Generate analytics dashboard
python atlas_cli.py dashboard export dashboard.html

# Run full automation cycle
python atlas_cli.py auto run
```

## 📋 System Components

### 1. Main Automation Engine (`backlog_automation.py`)
- **Automatic Story Status Updates**: Scans commits and code for completion indicators
- **Smart Prioritization**: Auto-prioritizes based on dependencies, age, and business value
- **Story Lifecycle Management**: Handles creation, progression, and completion
- **Quality Gates**: Validates story completion against quality metrics

### 2. Story Template Generator (`story_generator.py`)
- **Template-based Generation**: Create stories from predefined templates
- **Epic Decomposition**: Break down epics into manageable stories
- **Bug-to-Story Conversion**: Convert bug reports into structured stories
- **Acceptance Criteria Generation**: Auto-generate criteria based on story type

### 3. Analytics Dashboard (`backlog_dashboard.py`)
- **Velocity Tracking**: Monitor team velocity and trends
- **Burndown Charts**: Visualize sprint/wave progress
- **Cycle Time Analysis**: Identify bottlenecks in the process
- **Quality Metrics**: Track bugs, tech debt, and quality trends

### 4. Git Integration (`git_hooks.py`)
- **Pre-commit**: Validate story references and quality gates
- **Post-commit**: Update story status based on commit messages
- **Post-merge**: Auto-complete stories when merged to main
- **PR Integration**: Link stories to pull requests

### 5. Unified CLI (`atlas_cli.py`)
- **Story Management**: Create, update, complete stories
- **Backlog Operations**: Status, prioritization, cleanup
- **Wave Planning**: Plan and track sprints/waves
- **Analytics**: Generate reports and insights

### 6. Configuration (`config/backlog_config.yaml`)
- **Auto-completion Rules**: Define patterns for story completion
- **Quality Thresholds**: Set quality gates and metrics
- **Notification Settings**: Configure alerts and reports
- **Integration Settings**: Git, GitHub, Slack configuration

### 7. GitHub Actions (`workflows/backlog_automation.yml`)
- **Automated Triggers**: Daily, weekly, and event-driven automation
- **Quality Monitoring**: Continuous quality assessment
- **Backlog Maintenance**: Automatic cleanup and organization
- **Reporting**: Generate and publish analytics reports

## 🎯 Key Features

### Automatic Story Completion
```bash
# System automatically detects completion from:
git commit -m "Fix user login issue - closes F0031"
git commit -m "Implement authentication feature F0032"
git commit -m "Complete password reset functionality F0033"
```

### Smart Prioritization
```bash
# Auto-prioritize based on:
# - Business value and user impact
# - Dependencies and blockers
# - Age and technical debt
# - Risk assessment
python atlas_cli.py backlog prioritize --auto
```

### Quality Gates
```bash
# Automatic quality story creation from:
# - Lint errors above threshold
# - Test coverage drops
# - Security vulnerabilities
# - Performance regressions
python atlas_cli.py quality check --create-stories
```

### Wave Planning
```bash
# Intelligent sprint planning based on:
# - Team velocity
# - Story dependencies
# - Capacity and priorities
python atlas_cli.py wave plan --velocity auto --stories 8
```

## 📊 Analytics and Reporting

### Velocity Analysis
```bash
# Generate velocity report for last 12 weeks
python atlas_cli.py analytics velocity --weeks 12

# View cycle time analysis
python atlas_cli.py analytics cycle-time

# Check priority distribution
python atlas_cli.py analytics priorities
```

### Dashboard Export
```bash
# Export comprehensive HTML dashboard
python atlas_cli.py dashboard export team_dashboard.html

# Generate JSON analytics
python atlas_cli.py dashboard generate
```

## 🔧 Story Management

### Creating Stories

#### Feature Stories
```bash
# Direct creation
python atlas_cli.py story create --type feature \
  --title "Add dark mode toggle" \
  --priority medium \
  --description "Implement dark/light mode switching"

# Template-based creation
python atlas_cli.py story generate --type feature \
  --template user_interface \
  --param component=dark_mode_toggle \
  --param page=settings
```

#### Bug Stories
```bash
# Bug with severity
python atlas_cli.py story create --type bug \
  --title "App crashes on startup" \
  --priority critical

# From template
python atlas_cli.py story generate --type bug \
  --template crash \
  --param platform=android \
  --param error_type=NullPointerException
```

#### Technical Debt
```bash
# Tech debt item
python atlas_cli.py story create --type tech_debt \
  --title "Refactor authentication module" \
  --priority high

# From template
python atlas_cli.py story generate --type tech_debt \
  --template code_quality \
  --param component=auth_module \
  --param complexity_score=high
```

### Epic Decomposition
```bash
# Break down epic into stories
python story_generator.py decompose E0001 vertical_slice

# Different decomposition strategies
python story_generator.py decompose E0001 horizontal_layer
python story_generator.py decompose E0001 user_journey
```

## 🤖 Automation Workflows

### Daily Automation
- Scan for completed work
- Update story priorities
- Check for blocked stories
- Generate daily status report

### Weekly Automation
- Plan next wave/sprint
- Cleanup old completed stories
- Generate analytics reports
- Archive outdated items

### Event-Driven Automation
- Auto-complete stories on PR merge
- Update status on commit
- Create quality stories from CI failures
- Link stories to pull requests

## 🔗 Integration Examples

### Git Hooks Setup
```bash
# Install all git hooks
python atlas_cli.py git install-hooks

# Manual hook execution
python atlas_cli.py git hook pre-commit
python atlas_cli.py git hook post-commit abc123
```

### GitHub Actions Integration
The system automatically:
- Runs on PR events (open, merge, close)
- Triggers daily/weekly on schedule
- Creates GitHub issues for blocked stories
- Comments on PRs with automation results
- Uploads analytics dashboards as artifacts

### Quality Integration
```bash
# Check current quality status
python atlas_cli.py quality check

# Generate quality improvement stories
python atlas_cli.py quality create-stories

# View quality report
python atlas_cli.py quality report
```

## 📈 Example Workflows

### 1. Daily Developer Workflow
```bash
# Check what to work on next
python atlas_cli.py story suggest --context "performance" --count 3

# Start working on a story
python atlas_cli.py story update F0042 in_progress

# Complete a story
python atlas_cli.py story complete F0041 --reason "All acceptance criteria met"
```

### 2. Sprint Planning Workflow
```bash
# Check backlog status
python atlas_cli.py backlog status --detailed

# Review blocked stories
python atlas_cli.py analytics blocked

# Plan next wave
python atlas_cli.py wave plan --stories 6 --velocity auto

# Check wave progress
python atlas_cli.py wave progress
```

### 3. Quality Management Workflow
```bash
# Run quality analysis
python atlas_cli.py quality check

# Create improvement stories
python atlas_cli.py quality create-stories

# Monitor quality trends
python atlas_cli.py analytics quality
```

### 4. Automation Management
```bash
# Run full automation cycle (dry run)
python atlas_cli.py auto run --dry-run

# Scan for completed work only
python atlas_cli.py backlog scan --auto-complete

# Clean up backlog
python atlas_cli.py backlog clean --archive-old --merge-duplicates
```

## 🛠️ Configuration

The system is highly configurable through `/config/backlog_config.yaml`:

- **Auto-completion patterns**: Define commit message patterns for story completion
- **Quality gates**: Set thresholds for test coverage, lint errors, etc.
- **Priority rules**: Configure auto-escalation and priority scoring
- **Wave planning**: Set default sizes and velocity buffers
- **Notifications**: Configure Slack, email, and report settings
- **Git integration**: Customize hook behavior and branch naming
- **Analytics**: Configure retention and calculation methods

## 📱 CLI Commands Reference

### Story Commands
```bash
atlas story create --type [feature|bug|tech_debt|epic] --title "Title"
atlas story complete [STORY_ID]
atlas story update [STORY_ID] [STATUS]
atlas story list --type [TYPE] --status [STATUS] --priority [PRIORITY]
atlas story show [STORY_ID]
atlas story generate --template [TEMPLATE] --type [TYPE]
atlas story dependencies [STORY_ID]
atlas story suggest --context [CONTEXT] --count [N]
```

### Backlog Commands
```bash
atlas backlog status [--detailed]
atlas backlog prioritize [--auto]
atlas backlog clean [--archive-old] [--merge-duplicates]
atlas backlog scan [--auto-complete]
```

### Wave Commands
```bash
atlas wave plan [--stories N] [--velocity auto]
atlas wave current
atlas wave list
atlas wave progress [WAVE_NUMBER]
```

### Analytics Commands
```bash
atlas analytics velocity [--weeks N]
atlas analytics cycle-time
atlas analytics priorities
atlas analytics performance
atlas analytics burndown [--wave N]
```

### Quality Commands
```bash
atlas quality check [--create-stories]
atlas quality report
atlas quality create-stories
```

### Git Commands
```bash
atlas git install-hooks
atlas git report
atlas git hook [HOOK_NAME] [ARGS...]
```

### Dashboard Commands
```bash
atlas dashboard generate
atlas dashboard export [FILENAME]
atlas dashboard live [--port PORT]
```

### Automation Commands
```bash
atlas auto run [--dry-run]
atlas auto schedule [--daily] [--weekly]
```

## 🎉 Benefits

1. **Reduced Manual Overhead**: Automates 80%+ of backlog management tasks
2. **Better Prioritization**: Data-driven priority scoring and dependency management
3. **Improved Quality**: Automated quality gates and improvement story creation
4. **Enhanced Visibility**: Comprehensive analytics and real-time dashboards
5. **Consistent Process**: Standardized story formats and lifecycle management
6. **Team Productivity**: Spend more time building, less time managing backlog

## 🚀 Getting Started

1. **Install Dependencies**:
   ```bash
   pip install pyyaml
   ```

2. **Configure the System**:
   - Edit `config/backlog_config.yaml` for your team's needs
   - Set up GitHub secrets for notifications (optional)

3. **Install Git Hooks**:
   ```bash
   python atlas_cli.py git install-hooks
   ```

4. **Create Your First Story**:
   ```bash
   python atlas_cli.py story create --type feature \
     --title "Set up Atlas automation" --priority high
   ```

5. **Enable GitHub Actions**:
   - Commit the workflow file to enable automation
   - Configure secrets for Slack/email notifications (optional)

6. **Start Using**:
   ```bash
   python atlas_cli.py backlog status
   python atlas_cli.py dashboard export
   ```

The Atlas Backlog Automation System will transform how you manage your development backlog, making it nearly autonomous while maintaining high quality standards.