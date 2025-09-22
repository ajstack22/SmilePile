# Atlas Quality Scoring Rubric v2.0

## Overview

The Atlas Quality Scoring Rubric provides a standardized, objective method for evaluating software quality across all dimensions. This rubric generates a composite quality score from 0-100 that enables consistent quality assessment, trend tracking, and continuous improvement.

## Quality Dimensions and Weighting

### Primary Quality Dimensions

| Dimension | Weight | Description | Key Metrics |
|-----------|--------|-------------|-------------|
| **Functionality** | 25% | Features work as specified | Acceptance criteria pass rate, defect count |
| **Reliability** | 20% | System stability and consistency | Uptime, error rates, crash frequency |
| **Performance** | 15% | Speed, responsiveness, efficiency | Response times, throughput, resource usage |
| **Security** | 15% | Protection against threats | Vulnerability count, compliance score |
| **Maintainability** | 10% | Code quality and documentation | Complexity metrics, documentation coverage |
| **Usability** | 10% | User experience quality | User satisfaction, task completion rates |
| **Testability** | 5% | Test coverage and quality | Test coverage, test reliability |

## Detailed Scoring Criteria

### 1. Functionality (25%)

**Excellent (90-100 points)**:
- 100% of acceptance criteria met
- All edge cases handled gracefully
- Error handling is comprehensive
- User workflows are complete and intuitive
- Integration points work flawlessly

**Good (70-89 points)**:
- 95-99% of acceptance criteria met
- Most edge cases handled
- Basic error handling implemented
- Core user workflows work correctly
- Minor integration issues present

**Acceptable (50-69 points)**:
- 85-94% of acceptance criteria met
- Some edge cases unhandled
- Inconsistent error handling
- Primary workflows functional
- Some integration issues affecting non-critical paths

**Poor (25-49 points)**:
- 70-84% of acceptance criteria met
- Many edge cases unhandled
- Limited error handling
- Some primary workflows broken
- Significant integration problems

**Unacceptable (0-24 points)**:
- <70% of acceptance criteria met
- Edge cases not considered
- No error handling
- Core functionality broken
- Integration failures

**Measurement Formula**:
```
Functionality Score = (
    (Acceptance Criteria Pass Rate × 0.4) +
    (Edge Case Coverage × 0.3) +
    (Error Handling Quality × 0.2) +
    (Integration Health × 0.1)
) × 100
```

### 2. Reliability (20%)

**Excellent (90-100 points)**:
- 99.9%+ uptime
- Zero critical errors in production
- Mean Time Between Failures (MTBF) > 720 hours
- Automatic recovery from transient failures
- Comprehensive monitoring and alerting

**Good (70-89 points)**:
- 99.5-99.8% uptime
- <1 critical error per month
- MTBF 168-720 hours
- Some automatic recovery capabilities
- Good monitoring coverage

**Acceptable (50-69 points)**:
- 99.0-99.4% uptime
- 1-3 critical errors per month
- MTBF 72-168 hours
- Manual intervention sometimes required
- Basic monitoring in place

**Poor (25-49 points)**:
- 98.0-98.9% uptime
- 4-10 critical errors per month
- MTBF 24-72 hours
- Frequent manual intervention required
- Limited monitoring

**Unacceptable (0-24 points)**:
- <98.0% uptime
- >10 critical errors per month
- MTBF <24 hours
- System requires constant attention
- No effective monitoring

**Measurement Formula**:
```
Reliability Score = (
    (Uptime Percentage × 0.4) +
    ((100 - Critical Errors per Month × 10) × 0.3) +
    (MTBF Score × 0.2) +
    (Recovery Capability × 0.1)
) × 100
```

### 3. Performance (15%)

**Excellent (90-100 points)**:
- Response times <100ms for 95th percentile
- Throughput exceeds requirements by 50%+
- CPU/Memory usage <50% under normal load
- Zero performance degradation under stress
- Optimal database query performance

**Good (70-89 points)**:
- Response times <200ms for 95th percentile
- Throughput meets requirements with 25% headroom
- CPU/Memory usage <70% under normal load
- Minimal performance degradation under stress
- Good database query performance

**Acceptable (50-69 points)**:
- Response times <500ms for 95th percentile
- Throughput meets minimum requirements
- CPU/Memory usage <85% under normal load
- Some performance degradation under stress
- Acceptable database performance

**Poor (25-49 points)**:
- Response times 500ms-2s for 95th percentile
- Throughput below requirements
- CPU/Memory usage >85% under normal load
- Significant performance degradation
- Poor database performance

**Unacceptable (0-24 points)**:
- Response times >2s for 95th percentile
- Severe throughput limitations
- Resource exhaustion under normal load
- System unusable under stress
- Database performance issues

**Measurement Formula**:
```
Performance Score = (
    (Response Time Score × 0.4) +
    (Throughput Score × 0.3) +
    (Resource Utilization Score × 0.2) +
    (Stress Test Score × 0.1)
) × 100
```

### 4. Security (15%)

**Excellent (90-100 points)**:
- Zero critical or high severity vulnerabilities
- Complete OWASP Top 10 coverage
- Comprehensive authentication and authorization
- Data encryption at rest and in transit
- Regular security audits and penetration testing

**Good (70-89 points)**:
- Zero critical vulnerabilities, <3 high severity
- Most OWASP Top 10 items addressed
- Strong authentication and authorization
- Encryption for sensitive data
- Periodic security reviews

**Acceptable (50-69 points)**:
- Zero critical vulnerabilities, 3-10 high severity
- Basic OWASP Top 10 coverage
- Standard authentication and authorization
- Some data encryption
- Ad-hoc security reviews

**Poor (25-49 points)**:
- 1-2 critical or >10 high severity vulnerabilities
- Limited security controls
- Weak authentication and authorization
- Minimal encryption
- No regular security reviews

**Unacceptable (0-24 points)**:
- >2 critical vulnerabilities
- Major security gaps
- Inadequate authentication and authorization
- No encryption
- No security considerations

**Measurement Formula**:
```
Security Score = (
    ((100 - Critical Vulns × 50 - High Vulns × 5) × 0.4) +
    (OWASP Coverage × 0.3) +
    (Auth/AuthZ Quality × 0.2) +
    (Encryption Coverage × 0.1)
) × 100
```

### 5. Maintainability (10%)

**Excellent (90-100 points)**:
- Cyclomatic complexity <10 for all methods
- 100% API documentation coverage
- Comprehensive inline code comments
- Consistent coding standards adherence
- Clear architectural patterns

**Good (70-89 points)**:
- Cyclomatic complexity <15 for 95% of methods
- >90% API documentation coverage
- Good inline code comments
- Mostly consistent coding standards
- Recognizable architectural patterns

**Acceptable (50-69 points)**:
- Cyclomatic complexity <20 for 90% of methods
- >75% API documentation coverage
- Some inline code comments
- Generally consistent coding standards
- Basic architectural patterns

**Poor (25-49 points)**:
- Cyclomatic complexity >20 for many methods
- 50-75% API documentation coverage
- Limited inline code comments
- Inconsistent coding standards
- Unclear architectural patterns

**Unacceptable (0-24 points)**:
- High cyclomatic complexity throughout
- <50% API documentation coverage
- No meaningful code comments
- No coding standards adherence
- No clear architecture

**Measurement Formula**:
```
Maintainability Score = (
    (Complexity Score × 0.4) +
    (Documentation Coverage × 0.3) +
    (Code Standards Adherence × 0.2) +
    (Architecture Quality × 0.1)
) × 100
```

### 6. Usability (10%)

**Excellent (90-100 points)**:
- User satisfaction score >4.5/5.0
- Task completion rate >95%
- Average task completion time <expected
- Intuitive user interface design
- Comprehensive user documentation

**Good (70-89 points)**:
- User satisfaction score 4.0-4.5/5.0
- Task completion rate 85-95%
- Task completion time meets expectations
- Good user interface design
- Adequate user documentation

**Acceptable (50-69 points)**:
- User satisfaction score 3.5-4.0/5.0
- Task completion rate 75-85%
- Task completion time slightly above expected
- Functional user interface design
- Basic user documentation

**Poor (25-49 points)**:
- User satisfaction score 2.5-3.5/5.0
- Task completion rate 60-75%
- Task completion time significantly above expected
- Poor user interface design
- Limited user documentation

**Unacceptable (0-24 points)**:
- User satisfaction score <2.5/5.0
- Task completion rate <60%
- Tasks take excessive time to complete
- Confusing user interface design
- No user documentation

**Measurement Formula**:
```
Usability Score = (
    (User Satisfaction × 20 × 0.4) +
    (Task Completion Rate × 0.3) +
    (Task Efficiency Score × 0.2) +
    (UI Design Quality × 0.1)
) × 100
```

### 7. Testability (5%)

**Excellent (90-100 points)**:
- Test coverage >95%
- All tests pass consistently
- Fast test execution (<5 minutes)
- Comprehensive test types (unit, integration, e2e)
- Test documentation is complete

**Good (70-89 points)**:
- Test coverage 85-95%
- Tests pass >98% of the time
- Reasonable test execution time (<10 minutes)
- Good test type coverage
- Test documentation is adequate

**Acceptable (50-69 points)**:
- Test coverage 75-85%
- Tests pass >95% of the time
- Acceptable test execution time (<15 minutes)
- Basic test type coverage
- Some test documentation

**Poor (25-49 points)**:
- Test coverage 60-75%
- Tests pass 90-95% of the time
- Slow test execution (>15 minutes)
- Limited test type coverage
- Minimal test documentation

**Unacceptable (0-24 points)**:
- Test coverage <60%
- Tests pass <90% of the time
- Very slow or unreliable test execution
- Inadequate test coverage
- No test documentation

**Measurement Formula**:
```
Testability Score = (
    (Test Coverage × 0.4) +
    (Test Reliability × 0.3) +
    (Test Performance × 0.2) +
    (Test Documentation × 0.1)
) × 100
```

## Composite Quality Score Calculation

The final Atlas Quality Score is calculated as:

```
Atlas Quality Score =
    (Functionality × 0.25) +
    (Reliability × 0.20) +
    (Performance × 0.15) +
    (Security × 0.15) +
    (Maintainability × 0.10) +
    (Usability × 0.10) +
    (Testability × 0.05)
```

## Quality Score Interpretation

| Score Range | Quality Level | Action Required |
|-------------|---------------|-----------------|
| 90-100 | **Excellent** | Continue current practices, share best practices |
| 80-89 | **Good** | Minor improvements, maintain quality |
| 70-79 | **Acceptable** | Focused improvement in weak areas |
| 60-69 | **Below Standard** | Significant improvement required before release |
| 0-59 | **Unacceptable** | Major rework required, do not release |

## Automated Scoring Integration

### Script Integration
```bash
# Calculate quality score for a feature
python3 quality_score.py calculate --feature F001 --report detailed

# Generate quality report
python3 quality_score.py report --sprint S2023-10 --format json

# Track quality trends
python3 quality_score.py trend --period 6months --dimension security
```

### Data Collection Points
1. **Build Pipeline**: Automated test results, code coverage, static analysis
2. **Monitoring Systems**: Performance metrics, error rates, uptime
3. **Security Scans**: Vulnerability assessments, compliance checks
4. **User Feedback**: Satisfaction surveys, usability testing results
5. **Code Review**: Complexity metrics, documentation coverage

## Quality Gates

### Pre-Release Gates
- Minimum quality score: 75
- No unacceptable (0-24) dimension scores
- Critical security vulnerabilities: 0
- Test coverage: >80%
- Performance regression: <10%

### Production Gates
- Minimum quality score: 80
- Reliability score: >70
- Security score: >70
- User satisfaction: >3.5/5.0

## Continuous Improvement Process

### Weekly Quality Reviews
1. Review quality score trends
2. Identify declining dimensions
3. Root cause analysis for drops >10 points
4. Create improvement action items
5. Track previous action item progress

### Monthly Quality Assessment
1. Compare scores against quality targets
2. Benchmark against historical performance
3. Identify systemic quality issues
4. Update quality improvement roadmap
5. Share quality insights with stakeholders

### Quarterly Quality Planning
1. Set quality targets for next quarter
2. Review and update quality rubric
3. Plan quality-focused initiatives
4. Resource allocation for quality improvements
5. Update quality training materials

## Quality Score Automation

The quality score calculation can be automated through the Atlas framework:

```python
# Example quality score calculation
from atlas_quality import QualityScorer

scorer = QualityScorer()
score = scorer.calculate_feature_quality('F001')
print(f"Quality Score: {score.total}/100")
print(f"Dimensions: {score.dimensions}")
```

## Success Metrics

The quality rubric is successful when:
1. **Quality scores trend upward** over time
2. **Production defects decrease** by 25% quarter-over-quarter
3. **User satisfaction improves** consistently
4. **Development velocity maintains** while quality improves
5. **Quality discussions become data-driven** using objective scores

This rubric provides the foundation for objective quality assessment and continuous improvement in the Atlas framework.