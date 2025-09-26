# Secrets Management Guide

## Overview

This document provides guidelines for managing secrets and sensitive configuration for the SmilePile deployment system.

## Secret Storage Locations

### Local Development
- Store secrets in `.env.development` file (git-ignored)
- Never commit secrets to version control
- Use `.env.example` as a template

### CI/CD (GitHub Actions)
- Configure secrets in GitHub Settings → Secrets and variables → Actions
- Access via `${{ secrets.SECRET_NAME }}` in workflows
- Required secrets are listed below

### Production
- Use a dedicated secret management service (AWS Secrets Manager, HashiCorp Vault, etc.)
- Implement secret rotation policies
- Audit secret access regularly

## Required GitHub Secrets

### Common Secrets

| Secret Name | Description | Environment |
|------------|-------------|-------------|
| `API_KEY` | Main API authentication key | All |
| `API_SECRET` | API secret for signing requests | All |
| `SLACK_WEBHOOK_URL` | Slack webhook for notifications | All |
| `SENTRY_DSN` | Sentry error tracking DSN | Quality, Production |

### Android Secrets

| Secret Name | Description | Environment |
|------------|-------------|-------------|
| `ANDROID_KEYSTORE_BASE64` | Base64 encoded keystore file | All |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password | All |
| `ANDROID_KEY_ALIAS` | Key alias in keystore | All |
| `ANDROID_KEY_PASSWORD` | Key password | All |
| `GOOGLE_PLAY_KEY_JSON` | Google Play service account JSON | Production |

### iOS Secrets

| Secret Name | Description | Environment |
|------------|-------------|-------------|
| `IOS_CERTIFICATE_BASE64` | Base64 encoded P12 certificate | All |
| `IOS_CERTIFICATE_PASSWORD` | Certificate password | All |
| `IOS_PROVISIONING_PROFILE_BASE64` | Base64 encoded provisioning profile | All |
| `APPLE_ID` | Apple Developer account email | Production |
| `APPLE_APP_PASSWORD` | App-specific password | Production |

## Setting Up GitHub Secrets

### 1. Navigate to Repository Settings
```
Your Repository → Settings → Secrets and variables → Actions
```

### 2. Add New Secret
Click "New repository secret" and add each required secret.

### 3. Encoding Files for Secrets

For binary files (keystores, certificates), encode them as base64:

```bash
# Android Keystore
base64 -i keystore.jks | pbcopy  # macOS
base64 keystore.jks | xclip       # Linux

# iOS Certificate
base64 -i certificate.p12 | pbcopy  # macOS
base64 certificate.p12 | xclip      # Linux
```

### 4. Using Secrets in GitHub Actions

```yaml
- name: Decode Android Keystore
  run: |
    echo "${{ secrets.ANDROID_KEYSTORE_BASE64 }}" | base64 --decode > keystore.jks

- name: Build Android
  env:
    KEYSTORE_PASSWORD: ${{ secrets.ANDROID_KEYSTORE_PASSWORD }}
    KEY_ALIAS: ${{ secrets.ANDROID_KEY_ALIAS }}
    KEY_PASSWORD: ${{ secrets.ANDROID_KEY_PASSWORD }}
  run: |
    ./gradlew assembleRelease \
      -Pandroid.injected.signing.store.file=keystore.jks \
      -Pandroid.injected.signing.store.password=$KEYSTORE_PASSWORD \
      -Pandroid.injected.signing.key.alias=$KEY_ALIAS \
      -Pandroid.injected.signing.key.password=$KEY_PASSWORD
```

## Secret Rotation Policy

### Rotation Schedule

| Secret Type | Rotation Frequency | Notes |
|------------|-------------------|--------|
| API Keys | Every 90 days | Coordinate with backend team |
| Passwords | Every 60 days | Use strong, unique passwords |
| Certificates | Before expiration | Monitor expiration dates |
| Access Tokens | Every 30 days | Automate if possible |

### Rotation Process

1. **Generate New Secret**
   - Create new credential in respective service
   - Test in development environment first

2. **Update Secret Storage**
   - Update GitHub Secrets
   - Update production secret manager
   - Update documentation if needed

3. **Deploy with New Secret**
   - Deploy to quality environment first
   - Verify functionality
   - Deploy to production

4. **Revoke Old Secret**
   - Wait 24-48 hours after successful deployment
   - Revoke/delete old credential
   - Document rotation in audit log

## Security Best Practices

### DO's
- ✅ Use strong, unique passwords (minimum 20 characters)
- ✅ Enable 2FA on all accounts with access to secrets
- ✅ Use environment-specific secrets
- ✅ Encrypt secrets at rest and in transit
- ✅ Implement least privilege access
- ✅ Audit secret access regularly
- ✅ Use secret scanning tools in CI/CD
- ✅ Document all secrets and their purposes

### DON'Ts
- ❌ Never commit secrets to version control
- ❌ Don't share secrets via email or chat
- ❌ Don't use the same secret across environments
- ❌ Don't log secrets in application logs
- ❌ Don't store secrets in plain text files
- ❌ Don't hardcode secrets in source code
- ❌ Don't use default or weak passwords
- ❌ Don't ignore secret rotation schedules

## Emergency Procedures

### If a Secret is Compromised

1. **Immediate Actions**
   - Revoke the compromised secret immediately
   - Generate and deploy new secret
   - Review access logs for unauthorized use

2. **Investigation**
   - Determine how the secret was compromised
   - Identify affected systems and data
   - Check for other potentially compromised secrets

3. **Remediation**
   - Update all systems using the secret
   - Implement additional security measures
   - Document incident and lessons learned

4. **Prevention**
   - Review and update security procedures
   - Conduct security training if needed
   - Implement additional monitoring

## Automated Secret Scanning

### Pre-commit Hooks

Install pre-commit hooks to scan for secrets:

```bash
# Install pre-commit
pip install pre-commit

# Install git secrets
brew install git-secrets  # macOS
apt-get install git-secrets  # Linux

# Configure git secrets
git secrets --install
git secrets --register-aws
```

### GitHub Secret Scanning

GitHub automatically scans for exposed secrets. Enable notifications:
1. Go to Settings → Code security and analysis
2. Enable "Secret scanning"
3. Configure notification preferences

## Tools and Resources

### Recommended Tools
- **HashiCorp Vault**: Enterprise secret management
- **AWS Secrets Manager**: AWS-integrated secret storage
- **Azure Key Vault**: Azure-integrated secret storage
- **1Password Business**: Team password management
- **git-secrets**: Prevent committing secrets

### Useful Commands

```bash
# Generate secure random password
openssl rand -base64 32

# Generate SSH key pair
ssh-keygen -t ed25519 -C "your-email@example.com"

# Check if file contains secrets
git secrets --scan file.txt

# Encrypt file with GPG
gpg --encrypt --recipient your-email@example.com file.txt

# Decrypt file with GPG
gpg --decrypt file.txt.gpg > file.txt
```

## Compliance and Auditing

### Audit Log Requirements
- Record all secret access attempts
- Log secret rotations and changes
- Monitor for unusual access patterns
- Retain logs for compliance period

### Compliance Standards
- Follow industry standards (SOC2, ISO 27001)
- Implement data encryption requirements
- Maintain secret inventory
- Regular security assessments

## Support

For questions about secret management:
- Contact: security@smilepile.app
- Slack: #security channel
- Documentation: Internal wiki