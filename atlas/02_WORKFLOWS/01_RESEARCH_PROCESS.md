# Research Process

## Overview
A comprehensive multi-agent research process that transforms vague questions into actionable knowledge through systematic, parallel investigation across technical, business, user, and risk dimensions. The Orchestrator coordinates multiple specialized research agents to provide deep insights for informed decision-making.

## When to Use
- Evaluating new technologies or approaches
- Making architectural decisions
- Understanding market opportunities
- Investigating technical feasibility
- Comparing multiple options
- Solving complex problems
- Reducing uncertainty before commitment

## Process Script
**Script**: `automation/processes/research.py`
**Usage**:
```bash
python research.py "React Native performance" --full        # 5+ agents, deep dive
python research.py "GraphQL vs REST" --quick               # 2 agents, rapid
python research.py "Cloud providers" --comparative         # Comparison matrix
```

## Process Owner
**Role**: ORCHESTRATOR
- Orchestrator coordinates all research agents
- Never researches directly
- Synthesizes findings from multiple agents
- Makes strategic recommendations

## The Multi-Agent Research Architecture

### Agent Constellation
```
            ORCHESTRATOR
                 |
    +------------+------------+
    |            |            |
Technical    Business      User
Researcher   Researcher   Researcher
    |            |            |
    +------------+------------+
         |              |
   Implementation    Risk
     Researcher    Researcher
```

### The 5 Specialized Research Agents

#### Agent 1: Technical Researcher
**Mission**: Deep technical investigation
- Current implementations and patterns
- Technical best practices
- Common pitfalls and anti-patterns
- Performance implications
- Security considerations
- Technical dependencies
**Returns**: Technical feasibility assessment

#### Agent 2: Business & Market Researcher
**Mission**: Business and market context
- Market trends and adoption
- Competitor approaches
- ROI calculation and costs
- Business risks and opportunities
- Regulatory/compliance factors
- Case studies and success stories
**Returns**: Business impact analysis

#### Agent 3: User Experience Researcher
**Mission**: User perspective investigation
- User needs and pain points
- Common user workflows
- Usability best practices
- Accessibility requirements
- User feedback and reviews
- Journey scenarios
**Returns**: User impact assessment

#### Agent 4: Implementation Researcher
**Mission**: Practical implementation analysis
- Existing code patterns
- Implementation examples
- Required tools and frameworks
- Effort and complexity analysis
- Migration strategies
- Testing approaches
**Returns**: Implementation roadmap

#### Agent 5: Risk & Compliance Researcher
**Mission**: Risk and compliance assessment
- Security vulnerabilities
- Compliance requirements
- Failure modes and edge cases
- Operational risks
- Legal implications
- Disaster recovery needs
**Returns**: Risk matrix with mitigations

## The 6-Phase Research Process

### Phase 1: Research Planning
**Orchestrator Actions**:
1. Decompose topic into research questions
2. Identify research dimensions
3. Plan agent missions
4. Set success criteria

**Fundamental Questions**:
- What problem does this solve?
- Who are the stakeholders?
- What are current solutions?
- What defines success?
- What are the constraints?

**Research Dimensions**:
- Technical feasibility
- Business value
- User experience
- Security implications
- Performance factors
- Operational aspects

**Success Criteria**:
- Questions clearly defined
- Dimensions identified
- Agent missions planned

### Phase 2: Multi-Agent Deployment
**Orchestrator Actions**:
1. Spawn all 5 research agents IN PARALLEL
2. Monitor agent progress
3. Handle agent failures
4. Await all completions

**Parallel Execution**:
```python
# All agents launch simultaneously
Use Task tool with general-purpose agents:
- Technical Researcher → Technical investigation
- Business Researcher → Market analysis
- UX Researcher → User research
- Implementation Researcher → Practical analysis
- Risk Researcher → Risk assessment
```

**Success Criteria**:
- All agents successfully spawned
- Research proceeding in parallel
- No blocking dependencies

### Phase 3: Correlation & Synthesis
**Orchestrator Actions**:
1. Gather all agent outputs
2. Identify agreements (high confidence)
3. Identify conflicts (needs investigation)
4. Find patterns across domains
5. Map dependencies

**Correlation Analysis**:
- Agreement points → High confidence findings
- Disagreements → Further investigation needed
- Emerging patterns → Key insights
- Dependencies → Implementation considerations
- Knowledge gaps → Open questions

**Pattern Recognition**:
- Common challenges across agents
- Convergent success factors
- Recurring warnings
- Unanimous recommendations

**Success Criteria**:
- All findings correlated
- Patterns identified
- Conflicts noted

### Phase 4: Deep Dive Investigation
**Orchestrator Actions**:
1. Identify 2-3 critical areas
2. Spawn specialized deep-dive agents
3. Focus on highest-impact findings
4. Resolve conflicts from Phase 3

**Critical Areas** (typically):
- Most critical technical challenge
- Biggest business opportunity/risk
- Primary user concern

**Deep Research Activities**:
- Code pattern analysis
- Detailed cost modeling
- User testing scenarios
- Security vulnerability assessment

**Success Criteria**:
- Critical areas thoroughly investigated
- Conflicts resolved
- High-impact findings validated

### Phase 5: Recommendations & Reporting
**Orchestrator Actions**:
1. Synthesize all findings
2. Create recommendations
3. Assign confidence levels
4. Generate comprehensive report

**Report Structure**:
```markdown
# Research Report: [Topic]

## Executive Summary
[2-3 paragraphs of key findings]

## Key Findings
### Technical
- Finding [Evidence] [Confidence: High/Med/Low]

### Business
- Finding [Evidence] [Confidence: High/Med/Low]

### User Experience
- Finding [Evidence] [Confidence: High/Med/Low]

## Patterns and Insights
[Cross-domain patterns]

## Risk Assessment
| Risk | Probability | Impact | Mitigation |

## Recommendations
### Immediate Actions (This Sprint)
### Short-term (Next Month)
### Strategic (Long-term)

## Implementation Roadmap
[Phased approach if applicable]

## Open Questions
[What needs more research]

## Confidence Assessment
Overall: High/Medium/Low
Rationale: [Why this level]
```

**Success Criteria**:
- Comprehensive report generated
- Clear recommendations provided
- Confidence levels assigned

### Phase 6: Knowledge Preservation
**Orchestrator Actions**:
1. Archive all research artifacts
2. Update knowledge base
3. Document decision rationale
4. Create follow-up tasks

**Preservation Activities**:
- Save agent reports
- Archive evidence
- Document decisions
- Update team wiki
- Create research backlog

**Success Criteria**:
- All artifacts preserved
- Knowledge accessible
- Follow-ups identified

## Script Details

### Research Modes

#### Full Research (--full)
- Deploys all 5 agents
- 6 complete phases
- 2-4 hour investigation
- Comprehensive report

#### Quick Research (--quick)
- 2 agents only (Technical + Business)
- Rapid 30-minute scan
- Concise findings
- Go/no-go recommendation

#### Comparative Research (--comparative)
- Specialized comparison agents
- Feature matrices
- Trade-off analysis
- Clear winner identification

### State Management
Research state in `.atlas/research/`:
```json
{
  "research_id": "R20240115143000",
  "topic": "microservices migration",
  "phase": "synthesis",
  "agents_spawned": 5,
  "agents_completed": 3,
  "findings": {
    "technical": [...],
    "business": [...],
    "risks": [...]
  }
}
```

## Research Quality Metrics

### Coverage Metrics
- All dimensions explored: ✓
- Multiple perspectives gathered: ✓
- Edge cases considered: ✓
- Risks identified: ✓

### Depth Metrics
- Critical areas investigated: ✓
- Evidence provided: ✓
- Examples found: ✓
- Patterns recognized: ✓

### Synthesis Quality
- Insights exceed individual findings: ✓
- Conflicts resolved: ✓
- Clear recommendations: ✓
- Actionable next steps: ✓

## Common Research Topics

### Technical Research
- "Microservices architecture patterns"
- "React vs Vue.js for enterprise"
- "Database migration strategies"
- "CI/CD pipeline optimization"
- "API design patterns"

### Business Research
- "Market opportunity for feature X"
- "Competitor pricing analysis"
- "Cost-benefit of cloud migration"
- "Partnership evaluation"
- "Product-market fit analysis"

### Strategic Research
- "AI integration opportunities"
- "Technical debt prioritization"
- "Platform modernization"
- "Security posture assessment"
- "Scalability planning"

## Success Metrics

- **Comprehensiveness**: All angles explored
- **Accuracy**: Findings verified correct
- **Actionability**: Clear next steps identified
- **Timeliness**: Delivered within useful window
- **Clarity**: Complex topics made understandable
- **Impact**: Decisions improved by research

## Integration Points

- **Output**: Research reports for decision-making
- **Input**: Questions from stakeholders
- **Integrates with**:
  - Story Creation (feasibility research)
  - Troubleshooting (pattern research)
  - Adversarial Workflow (implementation research)

## The Power of Orchestrated Research

**Traditional Research**:
- Single perspective
- Linear investigation
- Limited depth
- 4-8 hours for comprehensive research

**Orchestrated Multi-Agent Research**:
- 5+ perspectives simultaneously
- Parallel investigation
- Unlimited depth per agent
- 1-2 hours for comprehensive research

**Multiplication Effect**:
- 5x faster (parallel vs sequential)
- 5x broader (multiple specializations)
- 5x deeper (focused expertise)
- 5x better (synthesis creates insights)

The Orchestrator coordinates this research symphony, creating understanding that no single researcher could achieve alone.