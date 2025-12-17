# SmilePile Website - Quick Deployment Guide

## Deploy to Production

```bash
cd /Users/adamstack/SmilePile/website
./scripts/deploy-website-prod.sh
```

That's it! The script handles everything:
- ✅ Verifies SSH connection
- ✅ Creates backup
- ✅ Builds Astro site
- ✅ Uploads to cPanel
- ✅ Verifies deployment

## First Time Setup

### 1. Verify SSH Access (Already Configured)

Test connection:
```bash
ssh stackmap-cpanel "echo 'Connected'"
```

### 2. Configure Domain in cPanel

1. Log into cPanel
2. Go to **Domains** → Add Domain
3. Domain: `smilepile.app`
4. Document Root: `/home/stachblx/smilepile`

### 3. Enable SSL

1. cPanel → **SSL/TLS Status**
2. Find `smilepile.app`
3. Click **Run AutoSSL**

### 4. Deploy

```bash
cd /Users/adamstack/SmilePile/website
./scripts/deploy-website-prod.sh
```

## Development Workflow

### Make Changes

```bash
cd /Users/adamstack/SmilePile/website

# 1. Edit files in src/
# 2. Test locally
npm run dev

# 3. Test production build
npm run build
npm run preview

# 4. Commit
git add .
git commit -m "feat: Update content"
git push

# 5. Deploy
./scripts/deploy-website-prod.sh
```

## Verify Deployment

Visit:
- https://smilepile.app/
- https://smilepile.app/privacy
- https://smilepile.app/terms
- https://smilepile.app/support

Test redirects:
- https://smilepile.app/?privacy → should redirect to /privacy
- https://smilepile.app/?tos → should redirect to /terms

## Troubleshooting

### SSH fails
```bash
# Check SSH config
cat ~/.ssh/config | grep stackmap-cpanel

# Should show stackmap-cpanel host configuration
```

### Build fails
```bash
# Clear and rebuild
rm -rf dist node_modules
npm install
npm run build
```

### Rollback
The deployment script provides a rollback command in its output. Example:
```bash
ssh stackmap-cpanel 'rm -rf ~/smilepile && mkdir -p ~/smilepile && tar -xzf ~/smilepile-backup-YYYYMMDD-HHMMSS.tar.gz -C ~/smilepile'
```

## More Info

See `DEPLOYMENT.md` for complete documentation.
