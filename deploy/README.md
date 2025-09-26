# SmilePile Deployment System

A robust, self-contained shell script-based deployment system for SmilePile that works locally and in CI/CD environments without external dependencies.

## Overview

This deployment system is inspired by enterprise deployment approaches (similar to StackMap and manylla) and provides:

- **Zero Dependencies**: Works with just bash and standard Unix tools
- **Platform Agnostic**: Runs on macOS and Linux
- **CI/CD Compatible**: Works locally or in any CI/CD system
- **Production Ready**: Comprehensive safeguards, rollback, and monitoring
- **Self-Contained**: No GitHub Actions or external service requirements

## Quick Start

```bash
# Deploy to quality/staging environment
./deploy_qual.sh android

# Deploy to production (with safety checks)
./deploy_prod.sh ios

# Deploy both platforms
./deploy_qual.sh both
```

## Directory Structure

```
deploy/
├── deploy_qual.sh          # Quality/staging deployment
├── deploy_prod.sh          # Production deployment (with safeguards)
├── lib/                    # Core libraries
│   ├── common.sh          # Shared utilities
│   ├── env_manager.sh     # Environment management
│   ├── android_deploy.sh  # Android deployment logic
│   └── ios_deploy.sh      # iOS deployment logic
├── environments/          # Environment configurations
│   ├── base.env          # Common settings
│   ├── development.env   # Dev environment
│   ├── quality.env       # QA environment
│   ├── staging.env       # Staging environment
│   └── production.env    # Production environment
├── secrets/              # Secret storage (gitignored)
├── artifacts/            # Build artifacts (gitignored)
├── backups/              # Deployment backups
├── logs/                 # Deployment logs
├── reports/              # Deployment reports
└── history/              # Deployment history

```

## Features

### 1. Environment Management
- Automatic environment detection
- Secure secrets handling
- Feature flags support
- Environment-specific configurations

### 2. Build & Deployment
- Android APK/AAB building
- iOS IPA building
- Code signing automation
- Multiple distribution channels

### 3. Quality Assurance
- Automated testing
- Security scanning
- Code quality checks
- Performance analysis

### 4. Production Safeguards
- Interactive approval process
- Staging validation
- Automatic rollback
- Comprehensive backups
- Post-deployment verification

### 5. Monitoring & Reporting
- Real-time logging
- HTML/JSON reports
- Deployment history tracking
- Notification system

## Environment Variables

### Common Options
```bash
ENVIRONMENT=<env>           # Target environment
SKIP_TESTS=true            # Skip test execution
SKIP_SECURITY=true         # Skip security scans
CLEAN_BUILD=true           # Clean before building
DRY_RUN=true              # Simulation mode
```

### Production-Specific
```bash
REQUIRE_APPROVAL=false     # Skip interactive approval (CI mode)
ENABLE_ROLLBACK=true       # Enable automatic rollback
MIN_TEST_COVERAGE=80       # Minimum test coverage required
```

## Usage Examples

### Development Deployment
```bash
# Quick development build
ENVIRONMENT=development ./deploy_qual.sh android

# With clean build
CLEAN_BUILD=true ./deploy_qual.sh ios
```

### Quality/Staging Deployment
```bash
# Standard QA deployment
./deploy_qual.sh android

# Skip tests for quick deployment
SKIP_TESTS=true ./deploy_qual.sh both

# Dry run to verify process
DRY_RUN=true ./deploy_qual.sh ios
```

### Production Deployment
```bash
# Interactive production deployment
./deploy_prod.sh android

# Automated CI/CD deployment
REQUIRE_APPROVAL=false CI=true ./deploy_prod.sh both

# Production with custom test coverage
MIN_TEST_COVERAGE=90 ./deploy_prod.sh ios
```

## Setting Up Secrets

1. Create environment-specific secret files:
```bash
# Create secrets directory
mkdir -p deploy/secrets

# Create secret file for environment
cat > deploy/secrets/production.env << EOF
# Android signing
ANDROID_KEYSTORE_PASSWORD=your_password
ANDROID_KEY_PASSWORD=your_key_password

# iOS signing
IOS_CERTIFICATE_PASSWORD=your_cert_password

# API Keys
FIREBASE_API_KEY=your_firebase_key
EOF

# Secure the secrets directory
chmod 700 deploy/secrets
chmod 600 deploy/secrets/*.env
```

2. Encrypt sensitive files (optional):
```bash
# Set encryption key
export DEPLOY_SECRET_KEY="your-strong-password"

# Encrypt a file
./deploy/lib/env_manager.sh encrypt_secret file.env file.env.enc
```

## CI/CD Integration

### GitHub Actions
```yaml
- name: Deploy to Production
  env:
    REQUIRE_APPROVAL: false
    CI: true
  run: |
    cd deploy
    ./deploy_prod.sh android
```

### Jenkins
```groovy
stage('Deploy') {
    environment {
        REQUIRE_APPROVAL = 'false'
        CI = 'true'
    }
    steps {
        sh './deploy/deploy_prod.sh both'
    }
}
```

### GitLab CI
```yaml
deploy:
  script:
    - export REQUIRE_APPROVAL=false
    - export CI=true
    - ./deploy/deploy_prod.sh ios
```

## Security Best Practices

1. **Never commit secrets** - Use `.gitignore` and separate secret files
2. **Use encrypted storage** - Encrypt keystores and certificates
3. **Implement access control** - Restrict production deployment access
4. **Enable audit logging** - Track all deployment activities
5. **Regular rotation** - Rotate keys and certificates periodically

## Troubleshooting

### Common Issues

**Build Fails**
```bash
# Clean build and retry
CLEAN_BUILD=true ./deploy_qual.sh android

# Check logs
tail -f logs/deploy_*.log
```

**Permission Denied**
```bash
# Make scripts executable
chmod +x deploy/*.sh
chmod +x deploy/lib/*.sh
```

**Environment Not Found**
```bash
# List available environments
ls -la environments/

# Set environment explicitly
ENVIRONMENT=staging ./deploy_qual.sh android
```

**Rollback Required**
```bash
# Check rollback points
ls -la backups/

# Manual rollback (if automatic fails)
./deploy/lib/common.sh restore_backup backups/prod_20240101_120000
```

## Development

### Adding a New Environment

1. Create environment file:
```bash
cp environments/base.env environments/custom.env
# Edit custom.env with your settings
```

2. Create secrets file (if needed):
```bash
touch secrets/custom.env
chmod 600 secrets/custom.env
```

3. Deploy using new environment:
```bash
ENVIRONMENT=custom ./deploy_qual.sh android
```

### Extending the System

The deployment system is modular and extensible:

1. **Add new platforms**: Create `lib/platform_deploy.sh`
2. **Add new checks**: Extend `lib/common.sh`
3. **Add new notifications**: Update notification functions
4. **Add new reports**: Modify report generation

## Monitoring & Alerts

The system includes built-in monitoring:

- **Deployment tracking**: All deployments are logged in `history/`
- **Performance metrics**: Build times and sizes are tracked
- **Error reporting**: Failed deployments trigger alerts
- **Health checks**: Post-deployment verification

## Support

For issues or questions:

1. Check the logs: `logs/deploy_*.log`
2. Review reports: `reports/deployment_*.html`
3. Check deployment history: `history/deployments.json`

## License

This deployment system is proprietary to SmilePile.

---

**Version**: 1.0.0
**Last Updated**: 2024
**Maintained By**: SmilePile DevOps Team