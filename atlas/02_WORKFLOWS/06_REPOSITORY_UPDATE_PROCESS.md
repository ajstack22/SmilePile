# Repository Update Process

## Overview
A systematic process for updating repository documentation and presentation to transform code storage into compelling products that attract users, contributors, and stakeholders. This process ensures all public-facing content tells a cohesive story about the product's value and capabilities.

## When to Use
- New repository needs documentation
- README is outdated or unclear
- Low engagement (stars, forks, contributions)
- Preparing for public release
- Major version updates
- Rebranding or repositioning
- Poor first impressions

## Process Script
**Script**: `automation/processes/update_repository.py`
**Usage**: `python update_repository.py --claude`

## Process Owner
**Role**: PRODUCT STRATEGIST via ORCHESTRATOR
- Orchestrator coordinates the update process
- Product Strategist agents handle content creation
- Never writes content directly

## The 6-Phase Update Process

### Phase 1: Discovery & Audit
**Objective**: Understand current repository state

**Orchestrator Actions**:
1. Spawn repository audit agent
2. Spawn documentation analysis agent
3. Review findings
4. Identify gaps and opportunities

**Audit Areas**:
- README structure and content
- Documentation organization
- Contributing guidelines
- License and code of conduct
- GitHub settings and metadata
- Examples and demos

**Strategic Questions**:
1. What does this product DO? (one sentence)
2. WHO is the target user?
3. WHY should they care?
4. HOW is it different?
5. What's the current onboarding experience?

**Research Activities**:
- Find all markdown files
- Identify configuration needing docs
- Locate example code
- Analyze documentation patterns

**Success Criteria**:
- Current state documented
- Gaps identified
- Improvement opportunities clear

### Phase 2: Content Strategy
**Objective**: Plan comprehensive content updates

**Orchestrator Actions**:
1. Spawn content strategist agent
2. Review proposed structure
3. Approve content plan
4. Prioritize updates

**README Structure Plan**:

#### Above the Fold (Hero Section)
- Product name and tagline
- Value proposition (1-2 sentences)
- Quick start/installation
- Status badges

#### Problem/Solution Section
- What problem does this solve?
- Who is it for?
- Key benefits (bullets)

#### Features Section
- Core capabilities
- Screenshots/GIFs
- Links to detailed docs

#### Getting Started
- Prerequisites
- Installation methods
- Basic usage example
- More examples link

#### Documentation Hub
- Full docs link
- API reference
- Guides/tutorials

#### Contributing Section
- How to contribute
- Development setup
- Testing guidelines

#### Support & Community
- Getting help
- Community channels
- Issue reporting

#### License & Credits

**Other Repository Files**:
| File | Purpose | Priority |
|------|---------|----------|
| CONTRIBUTING.md | Contribution guidelines | High |
| CODE_OF_CONDUCT.md | Community standards | Medium |
| .github/ISSUE_TEMPLATE/ | Issue templates | High |
| .github/PULL_REQUEST_TEMPLATE.md | PR template | High |
| docs/ | Comprehensive documentation | High |
| examples/ | Working examples | High |
| CHANGELOG.md | Version history | Medium |
| SECURITY.md | Security policy | Medium |

**Success Criteria**:
- Content plan complete
- Structure defined
- Priorities set

### Phase 3: Content Creation
**Objective**: Write compelling, clear content

**Orchestrator Actions**:
1. Spawn content writer agents for each section
2. Review drafts
3. Ensure consistency
4. Request revisions as needed

**Writing Principles**:
- **Lead with benefits**: What users gain, not features list
- **Use active voice**: "Install the package" not "The package can be installed"
- **Keep it scannable**: Short paragraphs, bullets, headers
- **Show don't tell**: Examples > explanations
- **Progressive disclosure**: Simple first, details later

**For Each Content Piece**:
1. Define PURPOSE (why exists?)
2. Identify AUDIENCE (who reads?)
3. Outline STRUCTURE (sections?)
4. Write with CLARITY
5. Add EXAMPLES
6. Include VISUALS

**Content Patterns**:

**Effective Openings**:
```markdown
# ProjectName
Tired of [problem]? ProjectName [solution] in [benefit].
```

**Clear Installation**:
```markdown
## Quick Start
Get running in under 5 minutes:
\`\`\`bash
npm install projectname
projectname init
\`\`\`
```

**Success Criteria**:
- All content written
- Voice consistent
- Examples included
- Visuals added

### Phase 4: GitHub Optimization
**Objective**: Maximize repository discoverability

**Orchestrator Actions**:
1. Spawn GitHub optimization agent
2. Review proposed settings
3. Approve changes
4. Verify updates

**Repository Settings**:
- **Description**: Clear, compelling one-liner
- **Website**: Link to docs or demo
- **Topics**: Relevant tags for discovery
- **Social Preview**: Custom image

**GitHub-Specific Files**:
```
.github/
├── FUNDING.yml           # Sponsorship
├── dependabot.yml        # Dependencies
├── workflows/            # CI/CD badges
├── ISSUE_TEMPLATE/
│   ├── bug_report.md
│   ├── feature_request.md
│   └── config.yml
└── PULL_REQUEST_TEMPLATE.md
```

**SEO Optimization**:
- Keywords in description
- Relevant topics added
- Clear, searchable title
- Comprehensive README

**Success Criteria**:
- Settings optimized
- Templates created
- SEO improved

### Phase 5: Review & Polish
**Objective**: Ensure quality and consistency

**Orchestrator Actions**:
1. Spawn quality review agent
2. Spawn user testing agent
3. Review feedback
4. Coordinate fixes

**Quality Checklist**:

#### Content Quality
- [ ] Answers What? Why? How? Who?
- [ ] Installation instructions tested
- [ ] Examples run without errors
- [ ] All links valid
- [ ] Images/GIFs load properly

#### Voice & Tone
- [ ] Consistent throughout
- [ ] Professional but approachable
- [ ] No unexplained jargon
- [ ] Clear and concise

#### Technical Accuracy
- [ ] Commands work
- [ ] Versions current
- [ ] Dependencies accurate
- [ ] Requirements clear

#### User Experience Test
- [ ] 30-second understanding test
- [ ] 5-minute setup test
- [ ] Clear help path
- [ ] Contribution path obvious

**Success Criteria**:
- All checks pass
- Feedback addressed
- Polish complete

### Phase 6: Implementation
**Objective**: Execute updates systematically

**Orchestrator Actions**:
1. Spawn implementation agent
2. Monitor progress
3. Review changes
4. Approve pull request

**Implementation Order**:
1. Create branch: `update-repository-content`
2. Update files in priority order:
   - README.md (highest impact)
   - CONTRIBUTING.md
   - Documentation files
   - GitHub-specific files
   - Examples and demos

**Per-File Process**:
1. Make changes
2. Preview in GitHub
3. Verify links
4. Check formatting
5. Test commands

**Pull Request Format**:
```markdown
## Repository Content Update

### Changes Made
- ✨ Complete README overhaul
- 📚 Added comprehensive contributing guide
- 🎯 Improved project description and topics
- 📝 Created issue/PR templates
- 🔍 SEO optimization

### Before/After
[Screenshots showing improvements]

### Checklist
- [ ] All links tested
- [ ] Examples verified
- [ ] Formatting checked
- [ ] Voice consistent
```

**Success Criteria**:
- All updates implemented
- PR created
- Changes merged

## Script Details

### Automated Analysis
The script analyzes:
```python
repo = {
    'has_readme': True/False,
    'has_license': True/False,
    'has_contributing': True/False,
    'has_docs_dir': True/False,
    'has_examples': True/False,
    'has_ci': True/False
}
```

### State Management
Update progress in `.atlas/repository_update/`:
```json
{
  "session": "update_20240115",
  "phase": "content_creation",
  "completed": ["README", "CONTRIBUTING"],
  "pending": ["examples", "docs"],
  "metrics": {
    "files_updated": 7,
    "sections_written": 23
  }
}
```

## Success Metrics

### Quantitative Metrics
- **Time to Understanding**: <30 seconds to grasp purpose
- **Time to First Run**: <5 minutes setup
- **Documentation Coverage**: 100% features documented
- **Link Validity**: 100% links working
- **Example Success**: 100% examples run

### Qualitative Metrics
- **First Impression**: Professional and inviting
- **Clarity**: No confusion about purpose
- **Completeness**: All questions answered
- **Consistency**: Unified voice throughout
- **Compelling**: Makes users want to try

### Engagement Metrics (Post-Update)
- **Stars**: 20% increase in 30 days
- **Forks**: 15% increase
- **Contributors**: New contributor within 2 weeks
- **Issues**: Quality issues (not confusion)
- **Traffic**: Repository views increase

## Common Improvements

### Before → After Examples

**Weak Description**:
> A tool for managing things

**Strong Description**:
> Streamline your workflow with intelligent task automation that learns your patterns

**Weak README Start**:
> This is a project that does stuff with data.

**Strong README Start**:
> Transform messy data into insights in seconds with zero configuration.

**Weak Installation**:
> Install the package and run it.

**Strong Installation**:
```bash
# Install globally
npm install -g datatools

# Run on your data
datatools analyze ./mydata.csv

# View results
open ./results/dashboard.html
```

## Integration Points

- **Input**: Existing repository state
- **Output**: Updated documentation and settings
- **Integrates with**:
  - Research Process (for competitive analysis)
  - Story Creation (for feature documentation)
  - Adversarial Workflow (for review)

## Anti-Patterns to Avoid

- ❌ Overpromising capabilities
- ❌ Burying the value proposition
- ❌ Assuming technical knowledge
- ❌ Ignoring mobile readers
- ❌ Forgetting examples

## When Updates Fail to Improve Engagement

If metrics don't improve after updates:
1. Survey users for feedback
2. A/B test different approaches
3. Research successful similar projects
4. Consider product-market fit
5. Focus on building features users want