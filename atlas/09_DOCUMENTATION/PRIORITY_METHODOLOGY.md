# Atlas Priority Methodology

## Overview

Atlas uses a **WSJF (Weighted Shortest Job First)** scoring system combined with traditional priority levels to ensure the most valuable work gets done first.

## Priority Levels

### 🔴 Critical (P0)
**When to use:** System down, data loss risk, security breach, blocking entire team
- **Response:** Drop everything, all hands on deck
- **SLA:** Start within 1 hour
- **Examples:**
  - Production system is down
  - Security vulnerability actively being exploited
  - Data corruption affecting users
  - Complete blocker for release

### 🟠 High (P1)
**When to use:** Major functionality broken, significant user impact, blocking key workflows
- **Response:** Address within current sprint
- **SLA:** Start within 1 day
- **Examples:**
  - Key feature not working
  - Performance degraded by >50%
  - Blocking other high-priority work
  - Customer-committed feature at risk

### 🟡 Medium (P2)
**When to use:** Standard features, improvements, non-blocking issues
- **Response:** Plan for next 1-2 sprints
- **SLA:** Start within 1 week
- **Examples:**
  - New feature development
  - UI/UX improvements
  - Minor bugs with workarounds
  - Technical debt that's manageable

### 🟢 Low (P3)
**When to use:** Nice-to-haves, minor improvements, cosmetic issues
- **Response:** Backlog, do when capacity allows
- **SLA:** Best effort
- **Examples:**
  - Cosmetic UI issues
  - Minor text changes
  - Small optimizations
  - "Someday maybe" features

## WSJF Scoring Formula

```
Priority Score = (Business Value + Time Criticality + Risk/Opportunity) / Job Size
```

### Components

#### Business Value (1-10)
- **10:** Critical for business survival
- **7-9:** Major revenue/cost impact
- **4-6:** Moderate business impact
- **1-3:** Minor business value

#### Time Criticality (1-10)
- **10:** Loses all value if delayed
- **7-9:** Significant value decay over time
- **4-6:** Some time sensitivity
- **1-3:** No urgency

#### Risk Reduction/Opportunity Enablement (1-10)
- **10:** Eliminates major risk or enables game-changing opportunity
- **7-9:** Significant risk reduction or opportunity
- **4-6:** Moderate risk/opportunity impact
- **1-3:** Minimal risk/opportunity impact

#### Job Size (1-10)
- **1-2:** Few hours
- **3-4:** 1-2 days
- **5-6:** 3-5 days
- **7-8:** 1-2 weeks
- **9-10:** > 2 weeks

## How to Prioritize

### Step 1: Initial Classification
Assign priority level (Critical/High/Medium/Low) based on impact and urgency.

### Step 2: WSJF Scoring (for non-critical items)
Calculate WSJF score for High/Medium/Low items to determine order within each priority band.

### Step 3: Stack Rank
Order items by:
1. Priority level (Critical → High → Medium → Low)
2. WSJF score within each level
3. Dependencies and blockers

## Priority Decision Matrix

| Impact ↓ / Urgency → | Immediate | This Sprint | Next Sprint | Someday |
|---------------------|-----------|-------------|-------------|---------|
| **System Down**     | Critical  | Critical    | High        | High    |
| **Major Feature**   | Critical  | High        | Medium      | Medium  |
| **Minor Feature**   | High      | Medium      | Medium      | Low     |
| **Cosmetic**        | Medium    | Low         | Low         | Low     |

## Examples

### Example 1: Security Vulnerability
- **Priority:** Critical
- **Business Value:** 10 (protects customer data)
- **Time Criticality:** 10 (active threat)
- **Risk Reduction:** 10 (eliminates security risk)
- **Job Size:** 2 (quick patch)
- **WSJF Score:** (10+10+10)/2 = 15

### Example 2: New Feature Request
- **Priority:** Medium
- **Business Value:** 6 (nice customer feature)
- **Time Criticality:** 3 (no deadline)
- **Risk Reduction:** 2 (minimal risk impact)
- **Job Size:** 5 (3-4 days work)
- **WSJF Score:** (6+3+2)/5 = 2.2

### Example 3: Performance Optimization
- **Priority:** High
- **Business Value:** 7 (improves user experience)
- **Time Criticality:** 6 (users complaining)
- **Risk Reduction:** 5 (reduces churn risk)
- **Job Size:** 3 (1-2 days)
- **WSJF Score:** (7+6+5)/3 = 6

## Priority Review Cadence

- **Daily:** Review Critical items in standup
- **Weekly:** Review High priority backlog
- **Sprint Planning:** Full backlog prioritization
- **Monthly:** Priority methodology effectiveness review

## Anti-Patterns to Avoid

❌ **Everything is Critical** - Dilutes real emergencies
❌ **Priority by Loudest Voice** - Use data, not volume
❌ **Never Finishing Low Priority** - Schedule "cleanup sprints"
❌ **Ignoring Technical Debt** - It becomes Critical eventually
❌ **Not Re-prioritizing** - Priorities change, review regularly

## Quick Priority Checklist

Before setting priority, ask:
- [ ] What happens if we don't do this now?
- [ ] How many users/systems are affected?
- [ ] Is there a workaround?
- [ ] What's the business impact in dollars?
- [ ] Are other teams blocked?
- [ ] Is there a deadline or commitment?
- [ ] What's the effort vs. value?

## Integration with Atlas Tools

### Using backlog_manager.py
```bash
# Create with priority and WSJF inputs
python3 backlog_manager.py create feature "New login" \
  --priority high \
  --business_value 8 \
  --time_criticality 6 \
  --risk_reduction 4 \
  --job_size 3

# View prioritized backlog
python3 backlog_manager.py prioritize

# Get priority recommendations
python3 backlog_manager.py suggest "Security update"
```

### Using 02_create_story.py
```bash
# Create feature with priority
python3 02_create_story.py feature "Add SSO" --priority critical

# Create bug with severity-based priority
python3 02_create_story.py bug "Login fails" --severity high

# Create tech debt with priority
python3 02_create_story.py tech_debt "Refactor auth" --priority medium
```

## Priority Escalation Path

1. **Developer** identifies issue needing priority change
2. **Tech Lead** reviews and validates priority
3. **Product Owner** approves Critical/High priorities
4. **Stakeholders** notified of Critical issues
5. **Team** swarms on Critical items

## Measuring Priority Effectiveness

Track these metrics monthly:
- Critical items that weren't really critical
- Low priority items that became critical
- Average time from priority assignment to completion
- WSJF score accuracy (predicted vs actual value)

## Summary

Good prioritization is about:
1. **Clarity** - Everyone understands what's important
2. **Consistency** - Same criteria applied uniformly
3. **Communication** - Priority changes are broadcast
4. **Courage** - Saying no to low-value work
5. **Continuous Improvement** - Refine the process based on outcomes

Remember: Priority is about maximizing value delivery, not keeping everyone happy. Use data, be transparent, and focus on outcomes.