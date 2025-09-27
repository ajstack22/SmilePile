# SonarQube Code Analysis for SmilePile

This document describes how to set up and run SonarQube code analysis for the SmilePile project, covering both Android and iOS codebases.

## Overview

SonarQube provides continuous code quality inspection with:
- Bug detection
- Code smell identification
- Security vulnerability scanning
- Technical debt measurement
- Code coverage reporting
- Duplicated code detection

## Quick Start

### 1. Local Setup

```bash
# Make scripts executable
chmod +x scripts/sonarqube-setup.sh scripts/sonarqube-analyze.sh

# Setup SonarQube server
./scripts/sonarqube-setup.sh

# Access SonarQube at http://localhost:9000
# Default login: admin/admin (you'll be prompted to change)

# Generate a token in SonarQube UI
# Account → Security → Generate Token

# Run analysis
./scripts/sonarqube-analyze.sh <your-token>
```

### 2. Docker Commands

```bash
# Start SonarQube
docker-compose -f docker-compose.sonarqube.yml up -d

# Stop SonarQube
docker-compose -f docker-compose.sonarqube.yml down

# View logs
docker-compose -f docker-compose.sonarqube.yml logs -f

# Clean up (removes all data)
docker-compose -f docker-compose.sonarqube.yml down -v
```

## Configuration Files

### `sonar-project.properties`
Main configuration file defining:
- Project metadata
- Source and test directories
- Language-specific settings
- Coverage paths
- Exclusion patterns

### `docker-compose.sonarqube.yml`
Docker Compose configuration for:
- SonarQube Community Edition
- PostgreSQL database
- Persistent volumes
- Network configuration

## Analysis Coverage

### Android
- **Languages**: Java, Kotlin
- **Coverage**: JaCoCo
- **Linting**: Android Lint
- **Build**: Gradle

### iOS
- **Languages**: Swift, Objective-C
- **Coverage**: XCTest Coverage
- **Linting**: SwiftLint, Tailor
- **Build**: xcodebuild

## Quality Gates

Default quality gate criteria:
- Coverage: ≥ 80%
- Duplicated Lines: < 3%
- Maintainability Rating: A
- Reliability Rating: A
- Security Rating: A
- Security Hotspots Reviewed: 100%

## CI/CD Integration

### GitHub Actions

The project includes `.github/workflows/sonarqube.yml` for automated analysis on:
- Push to main/develop branches
- Pull requests

Required GitHub Secrets:
```yaml
SONAR_TOKEN: <your-sonarqube-token>
SONAR_HOST_URL: <your-sonarqube-url>
```

### Manual CI Run

```bash
# Set environment variables
export SONAR_TOKEN="your-token"
export SONAR_HOST_URL="https://your-sonarqube.com"

# Run analysis
./scripts/sonarqube-analyze.sh $SONAR_TOKEN
```

## Interpreting Results

### Dashboard Metrics

1. **Reliability** (Bugs)
   - Critical/Major/Minor bugs
   - Impact on application stability

2. **Security** (Vulnerabilities & Hotspots)
   - Security vulnerabilities
   - Security hotspots requiring review

3. **Maintainability** (Code Smells)
   - Code quality issues
   - Technical debt ratio
   - Estimated time to fix

4. **Coverage**
   - Line coverage percentage
   - Branch coverage percentage
   - Uncovered lines/conditions

5. **Duplications**
   - Duplicated blocks
   - Duplicated lines percentage

### Fixing Issues

1. **Navigate to Issues tab** in SonarQube
2. **Filter by**:
   - Severity (Blocker → Info)
   - Type (Bug, Vulnerability, Code Smell)
   - Language
   - File/Component

3. **Each issue shows**:
   - Description
   - Why it's a problem
   - How to fix it
   - Code examples

## Best Practices

### Before Analysis
1. Ensure code compiles successfully
2. Run tests locally
3. Fix any build warnings

### Regular Analysis
- Run analysis before committing major changes
- Review new issues introduced
- Maintain or improve quality gate status

### Code Coverage
- Write tests for new code
- Aim for ≥ 80% coverage
- Focus on critical business logic

## Troubleshooting

### SonarQube Won't Start
```bash
# Check Docker status
docker ps

# Check logs
docker-compose -f docker-compose.sonarqube.yml logs sonarqube

# Common issues:
# - Port 9000 already in use
# - Insufficient memory (needs ~2GB RAM)
# - Elasticsearch bootstrap checks
```

### Analysis Fails
```bash
# Verbose mode
sonar-scanner -X

# Common issues:
# - Invalid token
# - Network connectivity
# - Missing binaries (build project first)
# - Incorrect source paths
```

### No Coverage Data
- Android: Ensure `jacocoTestReport` task runs
- iOS: Enable code coverage in scheme settings
- Check coverage report paths in properties file

### Memory Issues
```bash
# Increase scanner memory
export SONAR_SCANNER_OPTS="-Xmx512m"

# Increase SonarQube memory
# Edit docker-compose.sonarqube.yml:
# environment:
#   - SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true
#   - SONAR_HEAP_SIZE=2G
```

## Advanced Configuration

### Custom Rules
1. Go to Rules tab in SonarQube
2. Create custom rule profile
3. Activate/deactivate specific rules
4. Set as default for project

### Exclusions
Edit `sonar-project.properties`:
```properties
# Exclude generated files
sonar.exclusions=**/Generated/**,**/build/**

# Exclude from coverage
sonar.coverage.exclusions=**/*Test*,**/*Mock*

# Include specific files
sonar.inclusions=**/*.swift,**/*.kt
```

### Branch Analysis
```bash
# Analyze feature branch
sonar-scanner \
  -Dsonar.branch.name=feature/my-feature \
  -Dsonar.branch.target=main
```

### Pull Request Decoration
Configure in SonarQube:
1. Administration → Configuration → Pull Requests
2. Select provider (GitHub, GitLab, etc.)
3. Add authentication token
4. Enable PR decoration

## Resources

- [SonarQube Documentation](https://docs.sonarqube.org/latest/)
- [SonarQube Rules](https://rules.sonarsource.com/)
- [SonarQube Community](https://community.sonarsource.com/)
- [Swift Plugin](https://github.com/Idean/sonar-swift)
- [Kotlin Plugin](https://docs.sonarqube.org/latest/analysis/languages/kotlin/)