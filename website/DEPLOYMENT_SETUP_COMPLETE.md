# SmilePile Website Deployment - Setup Complete ✅

## Summary

Your SmilePile website is now ready to deploy to production using **SSH + rsync** - the same deployment method as Manylla.

## What Was Created

### 1. Deployment Script
**File**: `scripts/deploy-website-prod.sh`
- Builds Astro site locally
- Creates backup before deployment
- Uploads via rsync to cPanel
- Verifies deployment
- Provides rollback command

### 2. Apache Configuration
**File**: `public/.htaccess`
- Forces HTTPS
- Query parameter redirects (/?privacy → /privacy)
- Security headers
- Performance optimizations (caching, compression)
- This file is deployed automatically with the site

### 3. Documentation
- `DEPLOYMENT.md` - Complete deployment guide
- `DEPLOY_QUICK_START.md` - Quick reference for deployment
- `README.md` - Updated with cPanel deployment as primary method

## Deployment Method

**Uses SSH + rsync (same as Manylla)**:
- SSH host: `stackmap-cpanel` (already configured in ~/.ssh/config)
- Deploy path: `~/smilepile`
- Method: rsync built files from `dist/` to cPanel

**Does NOT use**:
- ❌ cPanel Git Version Control (removed `.cpanel.yml`)
- ❌ Vercel/Netlify (those are alternatives)

## Ready to Deploy

### Prerequisites ✅
- SSH access configured (stackmap-cpanel)
- cPanel hosting account
- Domain: smilepile.app

### First-Time Setup

1. **Configure domain in cPanel**:
   - Go to cPanel → Domains
   - Add `smilepile.app`
   - Document Root: `/home/stachblx/smilepile`

2. **Enable SSL**:
   - cPanel → SSL/TLS Status
   - Run AutoSSL for smilepile.app

3. **Deploy**:
   ```bash
   cd /Users/adamstack/SmilePile/website
   ./scripts/deploy-website-prod.sh
   ```

4. **Verify**:
   - Visit https://smilepile.app
   - Check all pages load
   - Test redirects

## Deployment Workflow

```bash
# 1. Make changes and test locally
cd /Users/adamstack/SmilePile/website
npm run dev

# 2. Build and preview
npm run build
npm run preview

# 3. Commit to git
git add .
git commit -m "feat: Update website"
git push

# 4. Deploy to production
./scripts/deploy-website-prod.sh

# 5. Verify
# Visit https://smilepile.app
```

## Deployment Time

**~30-60 seconds total**:
- Build: 10-20 seconds
- Upload: 10-20 seconds
- Verification: 5-10 seconds

## Comparison with Other Sites

### Manylla
```bash
rsync web/build/ stackmap-cpanel:~/public_html/manylla/
```

### SmilePile
```bash
rsync dist/ stackmap-cpanel:~/smilepile/
```

**Same infrastructure, same method!**

## Next Steps

1. **Configure domain** in cPanel (if not already done)
2. **Enable SSL** via cPanel AutoSSL
3. **Run first deployment**:
   ```bash
   cd /Users/adamstack/SmilePile/website
   ./scripts/deploy-website-prod.sh
   ```
4. **Verify deployment** at https://smilepile.app
5. **Commit deployment files** to git:
   ```bash
   cd /Users/adamstack/SmilePile
   git add website/
   git commit -m "feat: Add website deployment via SSH + rsync"
   git push
   ```

## Files Overview

```
website/
├── scripts/
│   └── deploy-website-prod.sh          # Deployment script
├── public/
│   └── .htaccess                       # Apache config (deployed with site)
├── DEPLOYMENT.md                       # Full deployment guide
├── DEPLOY_QUICK_START.md              # Quick reference
├── DEPLOYMENT_SETUP_COMPLETE.md       # This file
└── README.md                          # Updated with deployment info
```

## Support

### Quick Reference
- **Deploy**: `./scripts/deploy-website-prod.sh`
- **Test local**: `npm run dev`
- **Test build**: `npm run preview`
- **Rollback**: Command provided in deployment output

### Documentation
- Full guide: `DEPLOYMENT.md`
- Quick start: `DEPLOY_QUICK_START.md`

### Troubleshooting
- SSH issues: Verify `~/.ssh/config` has `stackmap-cpanel`
- Build fails: Run `npm run build` locally to debug
- Domain issues: Check cPanel domain configuration

---

**Ready to deploy!**

```bash
cd /Users/adamstack/SmilePile/website
./scripts/deploy-website-prod.sh
```
