# Atlas Evidence Templates v2.0

## Overview

Evidence templates provide standardized formats for collecting, organizing, and presenting proof of quality across all Atlas workflow phases. These templates ensure consistent documentation, enable automated validation, and support objective decision-making throughout the development lifecycle.

## Evidence Framework

### Evidence Hierarchy

#### Level 1: Raw Evidence
**Definition**: Unprocessed outputs from tools, tests, and systems
**Examples**: Test logs, build outputs, scan results, performance data
**Format**: Tool-native formats (JSON, XML, logs, reports)
**Automation**: Fully automated collection

#### Level 2: Processed Evidence
**Definition**: Structured, standardized data extracted from raw evidence
**Examples**: Pass/fail summaries, metric calculations, trend analysis
**Format**: Standardized Atlas formats (YAML, JSON schemas)
**Automation**: Automated processing with manual validation

#### Level 3: Evaluated Evidence
**Definition**: Evidence with quality assessments and recommendations
**Examples**: Quality scores, risk assessments, go/no-go decisions
**Format**: Atlas decision documents
**Automation**: Semi-automated with human review

#### Level 4: Aggregated Evidence
**Definition**: Cross-phase evidence summaries for overall assessment
**Examples**: Release readiness reports, project health summaries
**Format**: Executive dashboards and reports
**Automation**: Automated aggregation with stakeholder review

### Evidence Types by Phase

#### Requirements Validation Evidence
- Requirements traceability matrix
- Business value justification
- Acceptance criteria completeness
- Stakeholder approval records
- Risk assessment documentation

#### Design Review Evidence
- Architecture diagrams and documentation
- API contract specifications
- Security review results
- Performance benchmark definitions
- Design peer review records

#### Implementation Evidence
- Source code and version control history
- Unit test results and coverage
- Code review records
- Static analysis reports
- Security scan results

#### Adversarial Review Evidence
- Functional testing results
- Performance testing data
- Security assessment reports
- Code quality metrics
- User experience evaluation

#### Deployment Evidence
- Deployment logs and status
- Smoke test results
- Monitoring and alerting setup
- Rollback procedure validation
- Production readiness checklist

## Evidence Template Schemas

### Common Evidence Metadata
```yaml
evidence_metadata:
  id: "EVD-{YYYYMMDD}-{HHmm}-{type}-{sequence}"
  type: "build|test|performance|security|review"
  version: "2.0"
  phase: "requirements|design|implementation|review|deployment"
  story_id: "S001"
  sprint_id: "SP-2024-10"
  created_at: "2024-11-15T10:30:00Z"
  created_by: "automated|user_id"
  tools_used: ["pytest", "sonarqube", "selenium"]
  environment: "dev|staging|production"
  validation_status: "pending|validated|rejected"
  retention_policy: "30days|6months|permanent"
```

### Evidence Quality Standards
```yaml
quality_standards:
  completeness:
    required_fields: ["all_metadata", "primary_metrics", "supporting_data"]
    data_integrity: "no_missing_values"
    timestamp_accuracy: "within_5_minutes"

  accuracy:
    source_verification: "tool_authenticated"
    data_validation: "schema_compliant"
    cross_reference: "consistent_with_related_evidence"

  timeliness:
    collection_delay: "max_30_minutes"
    processing_delay: "max_5_minutes"
    reporting_delay: "max_10_minutes"

  accessibility:
    format_standard: "atlas_schema_v2"
    storage_location: "centralized_evidence_store"
    access_permissions: "role_based"
```

## Build Evidence Template

### Purpose
Document all aspects of the build process including compilation, packaging, artifact generation, and build environment validation.

### Collection Triggers
- Every code commit to main branches
- Release candidate builds
- Deployment preparation
- Scheduled nightly builds

### Evidence Structure
```yaml
build_evidence:
  metadata: # Common metadata schema

  build_info:
    build_id: "BLD-20241115-1030-main-342"
    trigger: "commit|schedule|manual|release"
    branch: "main"
    commit_sha: "a1b2c3d4e5f6"
    commit_message: "Fix authentication bug in user service"
    build_duration: "00:03:45"
    build_agent: "jenkins-agent-03"

  environment:
    build_server: "jenkins.company.com"
    os_version: "Ubuntu 22.04"
    build_tools:
      - name: "maven"
        version: "3.8.6"
      - name: "openjdk"
        version: "17.0.5"
    dependencies_snapshot: "dependencies-20241115.json"

  compilation:
    status: "success|failure|warning"
    compiler_warnings: 0
    compiler_errors: 0
    compilation_time: "00:01:23"
    output_size: "45.2MB"

  testing:
    unit_tests:
      executed: 1247
      passed: 1247
      failed: 0
      skipped: 0
      duration: "00:00:45"
      coverage: 87.3

    integration_tests:
      executed: 156
      passed: 156
      failed: 0
      skipped: 0
      duration: "00:02:15"

  static_analysis:
    sonarqube:
      quality_gate: "passed"
      bugs: 0
      vulnerabilities: 1
      code_smells: 12
      technical_debt: "2h 15m"
      duplication: 3.2

  security_scanning:
    dependency_check:
      critical: 0
      high: 1
      medium: 3
      low: 8
    container_scan:
      vulnerabilities: 2
      compliance_violations: 0

  artifacts:
    - name: "user-service.jar"
      size: "34.5MB"
      checksum: "sha256:a1b2c3..."
      location: "artifacts/release/user-service-2.1.0.jar"
    - name: "docker-image"
      tag: "user-service:2.1.0-rc.12"
      size: "156MB"
      registry: "registry.company.com"

  deployment_readiness:
    health_checks: "passed"
    configuration_validation: "passed"
    database_migrations: "ready"
    feature_flags: "configured"

  notifications:
    sent_to: ["team-slack", "build-email-list"]
    status: "success"

  evidence_files:
    - "build-log-full.txt"
    - "test-results.xml"
    - "sonarqube-report.json"
    - "dependency-check-report.xml"
```

### Build Evidence Validation Rules
```yaml
validation_rules:
  critical:
    - compilation_status == "success"
    - unit_test_failures == 0
    - security_critical_issues == 0

  blocking:
    - integration_test_failures == 0
    - quality_gate == "passed"
    - artifacts_generated == true

  warning:
    - test_coverage >= 80
    - build_duration <= "00:05:00"
    - static_analysis_violations < 20
```

## Test Evidence Template

### Purpose
Comprehensive documentation of all testing activities including functional, performance, security, and user acceptance testing.

### Collection Triggers
- Test suite execution completion
- Manual testing session completion
- Performance test runs
- Security testing activities
- User acceptance testing cycles

### Evidence Structure
```yaml
test_evidence:
  metadata: # Common metadata schema

  test_execution:
    test_run_id: "TR-20241115-1045-regression"
    test_suite: "regression|smoke|integration|performance|security"
    test_environment: "dev|staging|production|synthetic"
    execution_trigger: "automated|manual|scheduled"
    executor: "automation|tester_name"

  test_planning:
    test_strategy: "test-strategy-v2.1.pdf"
    test_cases_total: 342
    test_cases_executed: 338
    test_cases_skipped: 4
    skip_reasons: ["environment_unavailable", "feature_disabled"]

  functional_testing:
    acceptance_criteria:
      - criteria_id: "AC001"
        description: "User can login with valid credentials"
        status: "passed"
        evidence: "test-case-TC001.html"

    regression_tests:
      executed: 245
      passed: 243
      failed: 2
      duration: "01:23:45"

    smoke_tests:
      executed: 15
      passed: 15
      failed: 0
      duration: "00:05:30"

  performance_testing:
    load_tests:
      concurrent_users: 100
      duration: "00:30:00"
      avg_response_time: "145ms"
      95th_percentile: "320ms"
      throughput: "850 req/min"
      errors: 0

    stress_tests:
      max_users_supported: 500
      breaking_point: "650 users"
      recovery_time: "00:02:15"

    endurance_tests:
      duration: "02:00:00"
      memory_leaks: "none detected"
      performance_degradation: "< 5%"

  security_testing:
    vulnerability_scan:
      critical: 0
      high: 0
      medium: 2
      low: 5

    penetration_testing:
      scope: "authentication|authorization|data_access"
      findings: 1
      risk_level: "low"

    owasp_testing:
      injection: "passed"
      broken_auth: "passed"
      sensitive_data: "passed"
      xxe: "passed"
      broken_access: "passed"
      security_misconfig: "warning"
      xss: "passed"
      insecure_deserialization: "passed"
      vulnerable_components: "warning"
      insufficient_logging: "passed"

  user_acceptance_testing:
    test_scenarios: 25
    scenarios_passed: 23
    scenarios_failed: 2
    user_satisfaction: 4.2
    usability_score: 85

    feedback:
      - category: "navigation"
        severity: "medium"
        description: "Menu items not intuitive"
        status: "acknowledged"
      - category: "performance"
        severity: "low"
        description: "Page load slightly slow"
        status: "in_progress"

  accessibility_testing:
    wcag_compliance:
      level_a: "100%"
      level_aa: "95%"
      level_aaa: "78%"
    screen_reader_compatible: true
    keyboard_navigation: true
    color_contrast_ratio: 4.5

  browser_compatibility:
    chrome: "passed"
    firefox: "passed"
    safari: "passed"
    edge: "passed"
    mobile_chrome: "passed"
    mobile_safari: "passed"

  defects_identified:
    - defect_id: "DEF-001"
      severity: "high"
      priority: "high"
      description: "Payment form validation error"
      status: "open"
      assigned_to: "dev_team"
    - defect_id: "DEF-002"
      severity: "medium"
      priority: "medium"
      description: "UI alignment issue on mobile"
      status: "in_progress"
      assigned_to: "frontend_team"

  test_data:
    data_sets_used: ["user_accounts_test", "product_catalog_test"]
    data_privacy_compliance: "verified"
    data_cleanup_completed: true

  evidence_files:
    - "test-execution-report.html"
    - "performance-test-results.jmx"
    - "security-scan-report.pdf"
    - "browser-compatibility-screenshots/"
    - "accessibility-audit-report.pdf"
```

### Test Evidence Validation Rules
```yaml
validation_rules:
  critical:
    - functional_test_failures == 0 (for critical paths)
    - security_critical_vulnerabilities == 0
    - performance_regression < 10%

  blocking:
    - acceptance_criteria_pass_rate >= 95%
    - load_test_error_rate < 1%
    - browser_compatibility == "passed" (all supported)

  warning:
    - test_coverage >= 80%
    - user_satisfaction >= 4.0
    - accessibility_compliance_aa >= 90%
```

## Performance Evidence Template

### Purpose
Detailed documentation of system performance characteristics including response times, throughput, resource utilization, and scalability metrics.

### Collection Triggers
- Performance test execution
- Production monitoring alerts
- Release candidate validation
- Capacity planning activities
- Performance regression detection

### Evidence Structure
```yaml
performance_evidence:
  metadata: # Common metadata schema

  test_configuration:
    test_type: "load|stress|volume|endurance|spike"
    test_duration: "00:30:00"
    ramp_up_time: "00:05:00"
    ramp_down_time: "00:02:00"
    test_tool: "jmeter|gatling|k6|artillery"
    test_environment: "staging|production|synthetic"

  load_profile:
    concurrent_users: 100
    user_ramp_rate: "10 users/minute"
    think_time: "2-5 seconds"
    test_scenarios:
      - name: "user_login"
        weight: 30
        transactions: 1500
      - name: "browse_products"
        weight: 50
        transactions: 2500
      - name: "purchase_flow"
        weight: 20
        transactions: 1000

  response_time_metrics:
    overall:
      average: "145ms"
      median: "120ms"
      90th_percentile: "280ms"
      95th_percentile: "350ms"
      99th_percentile: "800ms"
      min: "45ms"
      max: "2.1s"

    by_endpoint:
      - endpoint: "/api/auth/login"
        average: "95ms"
        95th_percentile: "180ms"
        sla_target: "< 200ms"
        sla_compliance: "98.5%"
      - endpoint: "/api/products/search"
        average: "165ms"
        95th_percentile: "320ms"
        sla_target: "< 500ms"
        sla_compliance: "99.2%"
      - endpoint: "/api/orders/create"
        average: "245ms"
        95th_percentile: "450ms"
        sla_target: "< 1000ms"
        sla_compliance: "97.8%"

  throughput_metrics:
    requests_per_second: 45.2
    transactions_per_minute: 2710
    successful_requests: 98.7
    failed_requests: 1.3
    error_types:
      - type: "timeout"
        count: 12
        percentage: 0.8
      - type: "500_error"
        count: 7
        percentage: 0.5

  resource_utilization:
    application_servers:
      - server: "app-01"
        cpu_average: 65
        cpu_peak: 82
        memory_average: 78
        memory_peak: 89
        disk_io: "normal"
        network_io: "normal"
      - server: "app-02"
        cpu_average: 68
        cpu_peak: 85
        memory_average: 75
        memory_peak: 87
        disk_io: "normal"
        network_io: "normal"

    database_servers:
      - server: "db-primary"
        cpu_average: 45
        memory_average: 82
        disk_io: "high"
        active_connections: 45
        slow_queries: 3
      - server: "db-replica"
        cpu_average: 35
        memory_average: 78
        replication_lag: "< 1s"

    infrastructure:
      load_balancer_cpu: 15
      cache_hit_ratio: 94.5
      cdn_offload_ratio: 78.2

  scalability_analysis:
    linear_scalability_point: "150 users"
    performance_degradation_start: "200 users"
    system_breaking_point: "350 users"
    bottleneck_identification:
      - component: "database_connection_pool"
        impact: "high"
        recommendation: "increase_pool_size"
      - component: "image_processing"
        impact: "medium"
        recommendation: "implement_async_processing"

  memory_analysis:
    heap_utilization:
      initial: "512MB"
      peak: "1.2GB"
      gc_frequency: "every 45s"
      gc_duration: "avg 150ms"
    memory_leaks: "none detected"
    off_heap_usage: "256MB"

  network_analysis:
    bandwidth_utilization: 15
    latency_analysis:
      client_to_lb: "avg 12ms"
      lb_to_app: "avg 2ms"
      app_to_db: "avg 8ms"
    packet_loss: 0.01

  caching_performance:
    application_cache:
      hit_ratio: 92.5
      miss_ratio: 7.5
      eviction_rate: "low"
    database_cache:
      buffer_pool_hit_ratio: 98.2
      query_cache_hit_ratio: 76.3

  baseline_comparison:
    previous_version: "v2.0.7"
    performance_delta:
      response_time: "+5% (improved)"
      throughput: "+12% (improved)"
      resource_usage: "-3% (improved)"
      error_rate: "-0.2% (improved)"
    regression_analysis: "no significant regressions"

  sla_compliance:
    availability_target: "99.9%"
    availability_actual: "99.95%"
    response_time_target: "< 500ms (95th percentile)"
    response_time_actual: "350ms (95th percentile)"
    compliance_status: "exceeds_target"

  recommendations:
    immediate:
      - "Monitor database connection pool usage"
      - "Consider implementing connection pooling optimization"
    short_term:
      - "Implement caching for product search results"
      - "Optimize image processing workflow"
    long_term:
      - "Evaluate microservices architecture for user service"
      - "Implement horizontal scaling strategy"

  evidence_files:
    - "jmeter-test-results.jtl"
    - "system-monitoring-graphs.png"
    - "performance-trend-analysis.xlsx"
    - "load-test-execution-log.txt"
    - "resource-utilization-report.pdf"
```

### Performance Evidence Validation Rules
```yaml
validation_rules:
  critical:
    - response_time_95th <= sla_target
    - error_rate < 1%
    - availability >= 99.9%

  blocking:
    - performance_regression < 20%
    - resource_utilization < 90%
    - scalability_target_met == true

  warning:
    - response_time_average <= previous_version * 1.1
    - throughput >= previous_version * 0.9
    - memory_usage < 85%
```

## Security Evidence Template

### Purpose
Comprehensive documentation of security assessments, vulnerability findings, compliance verification, and risk mitigation evidence.

### Collection Triggers
- Security scan completion
- Penetration testing activities
- Compliance audit preparation
- Security review gate
- Incident response activities

### Evidence Structure
```yaml
security_evidence:
  metadata: # Common metadata schema

  assessment_scope:
    assessment_type: "static|dynamic|interactive|manual|compliance"
    scope_description: "Full application security assessment"
    applications_tested: ["user-service", "payment-service", "web-frontend"]
    assessment_methodology: "OWASP_WSTG_v4.2"
    compliance_frameworks: ["SOX", "PCI_DSS", "GDPR"]

  vulnerability_assessment:
    static_analysis:
      tool: "SonarQube Security"
      scan_date: "2024-11-15T10:30:00Z"
      lines_scanned: 45230

      findings:
        critical: 0
        high: 1
        medium: 4
        low: 12
        info: 8

      critical_vulnerabilities: []

      high_vulnerabilities:
        - id: "SQUID-S001"
          category: "injection"
          cwe: "CWE-89"
          description: "SQL injection in user search function"
          file: "UserController.java"
          line: 145
          status: "in_progress"
          assigned_to: "backend_team"
          remediation_eta: "2024-11-16"

      medium_vulnerabilities:
        - id: "SQUID-S002"
          category: "sensitive_data"
          cwe: "CWE-200"
          description: "Potential information disclosure in error messages"
          file: "ErrorHandler.java"
          line: 67
          status: "open"
          priority: "medium"

    dynamic_analysis:
      tool: "OWASP ZAP"
      scan_duration: "02:15:00"
      urls_scanned: 156

      findings:
        critical: 0
        high: 0
        medium: 2
        low: 7
        info: 15

      authentication_testing:
        brute_force_protection: "verified"
        session_management: "secure"
        password_policy: "compliant"

      authorization_testing:
        role_based_access: "verified"
        privilege_escalation: "none_found"
        forced_browsing: "protected"

      input_validation:
        sql_injection: "protected"
        xss_protection: "verified"
        csrf_protection: "implemented"

    dependency_scanning:
      tool: "OWASP Dependency Check"
      dependencies_scanned: 87

      vulnerable_dependencies:
        critical: 0
        high: 1
        medium: 3
        low: 8

      findings:
        - dependency: "spring-security-web"
          version: "5.7.2"
          vulnerability: "CVE-2023-34034"
          severity: "high"
          fix_available: "5.7.5"
          status: "pending_update"

    container_security:
      base_image: "openjdk:17-alpine"
      scan_tool: "Trivy"

      vulnerabilities:
        critical: 0
        high: 0
        medium: 1
        low: 4

      compliance:
        cis_benchmark: "passed"
        security_policies: "compliant"

  penetration_testing:
    test_date: "2024-11-10"
    tester: "external_security_firm"
    scope: "web_application|api|infrastructure"

    methodology:
      reconnaissance: "completed"
      scanning: "completed"
      enumeration: "completed"
      vulnerability_assessment: "completed"
      exploitation: "attempted"
      post_exploitation: "not_applicable"

    findings:
      critical: 0
      high: 0
      medium: 1
      low: 3

    detailed_findings:
      - finding_id: "PEN-001"
        severity: "medium"
        category: "information_disclosure"
        description: "Server version information exposed in HTTP headers"
        impact: "low"
        likelihood: "high"
        recommendation: "Configure server to hide version information"
        status: "accepted_risk"

  compliance_assessment:
    frameworks:
      owasp_top10:
        a01_broken_access_control: "compliant"
        a02_cryptographic_failures: "compliant"
        a03_injection: "needs_attention"
        a04_insecure_design: "compliant"
        a05_security_misconfiguration: "compliant"
        a06_vulnerable_components: "needs_attention"
        a07_identification_failures: "compliant"
        a08_software_integrity: "compliant"
        a09_logging_failures: "compliant"
        a10_ssrf: "compliant"

      pci_dss:
        requirement_1: "compliant"  # Firewall configuration
        requirement_2: "compliant"  # Default passwords
        requirement_3: "compliant"  # Cardholder data protection
        requirement_4: "compliant"  # Encryption in transit
        requirement_6: "partial"    # Secure development
        requirement_11: "compliant" # Regular testing

    data_protection:
      gdpr_compliance:
        data_minimization: "verified"
        consent_management: "implemented"
        right_to_erasure: "implemented"
        data_portability: "implemented"
        privacy_by_design: "verified"

      encryption:
        data_at_rest: "AES-256"
        data_in_transit: "TLS 1.3"
        key_management: "AWS KMS"
        certificate_management: "automated"

  security_controls:
    authentication:
      method: "multi_factor"
      password_policy: "strong"
      session_timeout: "30 minutes"
      account_lockout: "5 failed attempts"

    authorization:
      model: "role_based_access_control"
      principle: "least_privilege"
      review_frequency: "quarterly"

    monitoring:
      security_logging: "comprehensive"
      anomaly_detection: "enabled"
      incident_response: "automated"
      siem_integration: "configured"

    network_security:
      firewall: "configured"
      intrusion_detection: "active"
      ddos_protection: "cloudflare"

  risk_assessment:
    overall_risk_level: "low"
    critical_risks: 0
    high_risks: 1
    medium_risks: 7
    low_risks: 15

    risk_matrix:
      - risk_id: "RISK-001"
        description: "SQL injection vulnerability"
        likelihood: "medium"
        impact: "high"
        risk_level: "high"
        mitigation: "input_validation_enhancement"
        owner: "backend_team"
        due_date: "2024-11-20"

  remediation_plan:
    immediate_actions:
      - action: "Patch SQL injection vulnerability"
        priority: "critical"
        eta: "2024-11-16"
        owner: "backend_team"

    short_term_actions:
      - action: "Update vulnerable dependencies"
        priority: "high"
        eta: "2024-11-22"
        owner: "devops_team"

    long_term_actions:
      - action: "Implement security code review process"
        priority: "medium"
        eta: "2024-12-15"
        owner: "security_team"

  evidence_files:
    - "sonarqube-security-report.json"
    - "owasp-zap-report.html"
    - "dependency-check-report.xml"
    - "penetration-test-report.pdf"
    - "compliance-assessment-checklist.xlsx"
```

### Security Evidence Validation Rules
```yaml
validation_rules:
  critical:
    - critical_vulnerabilities == 0
    - high_vulnerabilities <= acceptable_threshold
    - compliance_critical_requirements == "all_met"

  blocking:
    - medium_vulnerabilities_with_fixes_available == 0
    - penetration_test_critical_findings == 0
    - encryption_standards == "current"

  warning:
    - vulnerability_remediation_time <= sla
    - compliance_score >= 85%
    - security_control_coverage >= 90%
```

## Evidence Collection Automation

### Collection Scripts Integration
```bash
# Collect all evidence types
python3 evidence_collector.py --type all --story S001

# Collect specific evidence type
python3 evidence_collector.py --type build --format yaml

# Validate evidence completeness
python3 evidence_validator.py --evidence-id EVD-20241115-1030-build-001

# Generate evidence summary
python3 evidence_aggregator.py --sprint SP-2024-10 --format dashboard
```

### Real-time Evidence Pipeline
```python
# evidence_pipeline.py
from atlas_evidence import EvidenceCollector, EvidenceValidator

collector = EvidenceCollector()
validator = EvidenceValidator()

# Automated evidence collection
def collect_evidence(trigger_event):
    evidence = collector.collect_for_event(trigger_event)
    validation_result = validator.validate(evidence)

    if validation_result.is_valid:
        evidence.store()
        evidence.notify_stakeholders()
    else:
        evidence.flag_for_review()

    return evidence
```

This comprehensive evidence template system ensures consistent, objective quality assessment across all Atlas workflow phases while enabling automation and continuous improvement.