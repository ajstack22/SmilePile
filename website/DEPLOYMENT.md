# SmilePile Website Deployment Guide

## Overview
The SmilePile website (https://smilepile.app) deploys to cPanel using **SSH + rsync** - the same method used for Manylla and StackMap websites.

## Deployment Method

**SSH + rsync Direct Deployment**:
1. Build Astro site locally (`npm run build`)
2. Use rsync to upload `dist/` contents to cPanel via SSH
3. Files deployed to `~/smilepile` on cPanel server

**Why this method?**:
- Same as Manylla/StackMap (consistent infrastructure)
- Fast deployment (only changed files sync)
- No need for Node.js on cPanel server
- Full control over deployment process
- Easy rollback with backups

## Prerequisites

### 1. SSH Access Configured

You should already have this from Manylla/StackMap setup. Verify:

```bash
cat ~/.ssh/config | grep -A 5 stackmap-cpanel
```

Should show:
```
Host stackmap-cpanel
  HostName 199.188.200.57
  Port 21098
  User stachblx
  IdentityFile ~/.ssh/id_rsa_cpanel
```

### 2. Test SSH Connection

```bash
ssh stackmap-cpanel "echo 'Connected successfully'"
```

Should print `Connected successfully`.

### 3. cPanel Domain Configuration

Ensure `smilepile.app` points to `/home/stachblx/smilepile`:

1. Log into cPanel
2. Go to **Domains**
3. Add or configure `smilepile.app`:
   - Domain: `smilepile.app`
   - Document Root: `/home/stachblx/smilepile`

### 4. SSL Certificate

1. Go to cPanel → **SSL/TLS Status**
2. Find `smilepile.app`
3. Click **Run AutoSSL**
4. Wait for certificate (1-5 minutes)

## Deployment

### Production Deployment

```bash
cd /Users/adamstack/SmilePile/website
./scripts/deploy-website-prod.sh
```

**What it does**:
1. ✅ Verifies SSH connection
2. 📦 Creates backup of existing website
3. 🔨 Builds Astro site (`npm run build`)
4. 📤 Uploads files to cPanel via rsync
5. ✅ Verifies deployment
6. 📋 Provides rollback command if needed

**Deployment time**: ~30-60 seconds

### First-Time Setup

If this is the first deployment:

1. **Create deployment directory** on cPanel:
   ```bash
   ssh stackmap-cpanel "mkdir -p ~/public_html/smilepile"
   ```

2. **Run deployment**:
   ```bash
   ./scripts/deploy-website-prod.sh
   ```

3. **Verify**:
   - Visit https://smilepile.app
   - Check all pages load
   - Verify HTTPS works

## Workflow

### Making Changes

1. **Edit website code locally**:
   ```bash
   cd /Users/adamstack/SmilePile/website
   # Edit files in src/
   ```

2. **Test locally**:
   ```bash
   npm run dev
   # Visit http://localhost:4321
   ```

3. **Build and test production build**:
   ```bash
   npm run build
   npm run preview
   ```

4. **Commit changes**:
   ```bash
   git add .
   git commit -m "feat: Update website content"
   git push origin main
   ```

5. **Deploy to production**:
   ```bash
   ./scripts/deploy-website-prod.sh
   ```

6. **Verify live site**:
   - Visit https://smilepile.app
   - Test changes

## Troubleshooting

### SSH Connection Failed
- Check SSH config: `cat ~/.ssh/config | grep stackmap-cpanel`
- Test connection: `ssh stackmap-cpanel "echo test"`
- Verify SSH key: `ls ~/.ssh/id_rsa_cpanel`

### Build Fails
- Test locally: `npm run build`
- Check Node.js version ≥ 18: `node --version`
- Clear cache: `rm -rf dist node_modules && npm install`

### Website Shows 404
- Check domain in cPanel → Domains
- Verify document root: `/home/stachblx/smilepile`
- Check files exist: `ssh stackmap-cpanel "ls ~/smilepile"`

### HTTPS Not Working
- Check SSL: cPanel → SSL/TLS Status
- Run AutoSSL for smilepile.app
- Verify DNS points to correct IP

## Rollback

If deployment fails, use the rollback command from deployment output:

```bash
ssh stackmap-cpanel 'rm -rf ~/smilepile && mkdir -p ~/smilepile && tar -xzf ~/smilepile-backup-YYYYMMDD-HHMMSS.tar.gz -C ~/smilepile'
```

Backups are also stored locally in `website/backups/`.

## Verification Checklist

After deployment:
- ✅ https://smilepile.app/ loads
- ✅ https://smilepile.app/privacy loads
- ✅ https://smilepile.app/terms loads
- ✅ https://smilepile.app/support loads
- ✅ https://smilepile.app/?privacy redirects to /privacy
- ✅ https://smilepile.app/?tos redirects to /terms
- ✅ HTTPS enabled (padlock icon)

## Related Documentation

- Website README: `README.md`
- Pre-Deployment Checklist: `PRE_DEPLOYMENT_CHECKLIST.md`
- Deployment Script: `scripts/deploy-website-prod.sh`
