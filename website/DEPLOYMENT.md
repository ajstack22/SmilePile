# SmilePile Landing Page - Deployment Guide

## Pre-Deployment Checklist

Before deploying to production, ensure:

### Content
- [ ] All legal text reviewed and accurate
- [ ] Contact email verified (support@stackmap.app working)
- [ ] Last updated dates current on Privacy and Terms pages
- [ ] No placeholder text remaining
- [ ] All links functional (internal and external)
- [ ] App Store/Google Play links updated with real app IDs

### Technical
- [ ] Production build successful (`npm run build`)
- [ ] No build errors or warnings
- [ ] Cross-browser tested (Chrome, Safari, Firefox, Edge)
- [ ] Mobile responsive verified (iOS Safari, Chrome Android)
- [ ] Query parameter redirects tested (/?privacy, /?tos)

### SEO
- [ ] Meta tags unique per page
- [ ] Open Graph images set
- [ ] Sitemap.xml generated in dist/
- [ ] Robots.txt present in dist/
- [ ] Canonical URLs configured

### Performance
- [ ] All images optimized
- [ ] CSS/JS minified
- [ ] Total bundle size reasonable (<100KB per page)
- [ ] Lighthouse audit passed (scores ≥ 90)

### Security
- [ ] Security headers configured (vercel.json or netlify.toml)
- [ ] No sensitive data in source code
- [ ] No console.log statements in production code

## Deployment Options

### Option 1: Vercel (Recommended)

Vercel provides automatic deployments, edge functions, and excellent performance.

#### Initial Setup

1. **Install Vercel CLI** (optional):
   ```bash
   npm i -g vercel
   ```

2. **Deploy via CLI**:
   ```bash
   cd /Users/adamstack/SmilePile/website
   vercel
   ```
   
   Follow prompts:
   - Set up and deploy: Yes
   - Which scope: Select your account
   - Link to existing project: No
   - Project name: smilepile-landing
   - Directory: . (current directory)
   - Override settings: No

3. **Or Deploy via Dashboard**:
   - Go to https://vercel.com/new
   - Import Git repository
   - Framework Preset: Astro
   - Build Command: `npm run build`
   - Output Directory: `dist`
   - Click "Deploy"

#### Custom Domain Setup

1. Go to Project → Settings → Domains
2. Add domain: `smilepile.app`
3. Configure DNS (Vercel provides instructions):
   - **Option A (Recommended)**: Point nameservers to Vercel
   - **Option B**: Add A/CNAME records

   **A Record Example:**
   ```
   Type: A
   Name: @
   Value: 76.76.21.21
   ```

   **CNAME Example:**
   ```
   Type: CNAME
   Name: www
   Value: cname.vercel-dns.com
   ```

4. Wait for DNS propagation (5-60 minutes)
5. Verify HTTPS is working (green padlock)

#### Environment Variables

No environment variables are required for this static site.

#### Automatic Deployments

Once connected to Git:
- Push to `main` branch → automatic production deployment
- Push to other branches → preview deployments

#### Vercel Configuration

The `vercel.json` file includes:

```json
{
  "redirects": [
    { "source": "/?privacy", "destination": "/privacy", "permanent": true },
    { "source": "/?tos", "destination": "/terms", "permanent": true }
  ],
  "headers": [
    {
      "source": "/(.*)",
      "headers": [
        { "key": "X-Frame-Options", "value": "DENY" },
        { "key": "X-Content-Type-Options", "value": "nosniff" },
        { "key": "Referrer-Policy", "value": "strict-origin-when-cross-origin" }
      ]
    }
  ]
}
```

---

### Option 2: Netlify

Netlify is another excellent option with similar features to Vercel.

#### Initial Setup

1. **Install Netlify CLI** (optional):
   ```bash
   npm i -g netlify-cli
   ```

2. **Deploy via CLI**:
   ```bash
   cd /Users/adamstack/SmilePile/website
   netlify deploy --prod
   ```
   
   Follow prompts:
   - Create new site
   - Team: Select your team
   - Site name: smilepile-landing
   - Publish directory: dist

3. **Or Deploy via Dashboard**:
   - Go to https://app.netlify.com/start
   - Connect to Git repository
   - Build command: `npm run build`
   - Publish directory: `dist`
   - Click "Deploy site"

#### Custom Domain Setup

1. Go to Site Settings → Domain management
2. Add custom domain: `smilepile.app`
3. Configure DNS:
   - **Option A**: Use Netlify DNS (recommended)
   - **Option B**: External DNS with CNAME

   **CNAME Example:**
   ```
   Type: CNAME
   Name: www
   Value: [your-site].netlify.app
   ```

4. Enable HTTPS (automatic with Let's Encrypt)

#### Netlify Configuration

The `netlify.toml` file includes:

```toml
[build]
  command = "npm run build"
  publish = "dist"

[[redirects]]
  from = "/?privacy"
  to = "/privacy"
  status = 301

[[headers]]
  for = "/*"
  [headers.values]
    X-Frame-Options = "DENY"
    X-Content-Type-Options = "nosniff"
```

---

### Option 3: Cloudflare Pages

Cloudflare Pages offers excellent performance with global CDN.

#### Setup

1. Go to https://pages.cloudflare.com
2. Create a project → Connect to Git
3. Select repository
4. Build settings:
   - Framework preset: Astro
   - Build command: `npm run build`
   - Build output directory: `dist`
5. Click "Save and Deploy"

#### Custom Domain

1. Go to Custom domains
2. Add `smilepile.app`
3. If domain is on Cloudflare DNS, automatic setup
4. Otherwise, add CNAME record

#### Redirects

Create `public/_redirects` file:
```
/?privacy    /privacy    301
/?tos        /terms      301
```

---

## Post-Deployment Verification

### Immediate Checks (within 5 minutes)

1. **Homepage**:
   ```bash
   curl -I https://smilepile.app
   # Should return 200 OK
   ```

2. **All Pages Load**:
   - https://smilepile.app/
   - https://smilepile.app/privacy
   - https://smilepile.app/terms
   - https://smilepile.app/support
   - https://smilepile.app/404

3. **Redirects Work**:
   ```bash
   curl -I https://smilepile.app/?privacy
   # Should return 301/302 to /privacy
   
   curl -I https://smilepile.app/?tos
   # Should return 301/302 to /terms
   ```

4. **HTTPS Certificate**:
   - Green padlock in browser
   - Valid SSL certificate
   - No mixed content warnings

5. **Mobile Test**:
   - Open site on iOS Safari
   - Open site on Chrome Android
   - Verify responsiveness

### Within 1 Hour

1. **DNS Propagation**:
   ```bash
   # Check DNS globally
   # Visit: https://www.whatsmydns.net/#A/smilepile.app
   ```

2. **Sitemap**:
   ```bash
   curl https://smilepile.app/sitemap-index.xml
   # Should return XML sitemap
   ```

3. **Robots.txt**:
   ```bash
   curl https://smilepile.app/robots.txt
   # Should return robots.txt content
   ```

4. **Security Headers**:
   ```bash
   curl -I https://smilepile.app | grep -E "(X-Frame-Options|X-Content-Type-Options)"
   # Should show configured headers
   ```

5. **Open Graph Tags**:
   - Share on Facebook Debugger: https://developers.facebook.com/tools/debug/
   - Share on Twitter Card Validator: https://cards-dev.twitter.com/validator

### Within 24 Hours

1. **Submit Sitemap to Google Search Console**:
   - Go to https://search.google.com/search-console
   - Add property: `https://smilepile.app`
   - Verify ownership (DNS or HTML file)
   - Submit sitemap: `https://smilepile.app/sitemap-index.xml`

2. **Submit Sitemap to Bing Webmaster Tools**:
   - Go to https://www.bing.com/webmasters
   - Add site: `https://smilepile.app`
   - Submit sitemap

3. **Lighthouse Audit**:
   ```bash
   # Using Chrome DevTools
   # Open https://smilepile.app
   # Open DevTools → Lighthouse
   # Run audit on Mobile and Desktop
   # Verify scores ≥ 90
   ```

4. **Set Up Uptime Monitoring**:
   - UptimeRobot: https://uptimerobot.com
   - Pingdom: https://www.pingdom.com
   - Monitor: https://smilepile.app
   - Alert email: support@stackmap.app

5. **Test from Mobile Apps**:
   - Open iOS app → About/Settings → Privacy Policy
   - Verify link opens https://smilepile.app/privacy
   - Open Android app → About/Settings → Terms
   - Verify link opens https://smilepile.app/terms

### Within 1 Week

1. **Monitor Core Web Vitals**:
   - Google Search Console → Core Web Vitals report
   - Ensure LCP < 2.5s, FID < 100ms, CLS < 0.1

2. **Check Search Indexing**:
   ```
   site:smilepile.app
   # In Google search
   # Should show all pages indexed
   ```

3. **Review Analytics** (if enabled):
   - Traffic sources
   - Most visited pages
   - Bounce rate
   - Average session duration

## Rollback Procedure

If issues are discovered after deployment:

### Vercel

1. Go to Deployments in Vercel dashboard
2. Find previous working deployment
3. Click "..." → "Promote to Production"
4. Confirm rollback

### Netlify

1. Go to Deploys in Netlify dashboard
2. Find previous working deployment
3. Click "Publish deploy"
4. Confirm rollback

### Emergency Local Fix

```bash
# Revert to last working commit
git log --oneline  # Find last working commit hash
git revert <commit-hash>
git push origin main

# This triggers automatic redeployment
```

## Troubleshooting

### Issue: Redirects Not Working

**Symptom**: /?privacy shows homepage instead of redirecting

**Solution**:
1. Verify `vercel.json` or `netlify.toml` is in project root
2. Check redirect configuration syntax
3. Redeploy to apply changes
4. Test with client-side redirect in `index.astro` as fallback

### Issue: Security Headers Missing

**Symptom**: Headers not showing in curl/browser inspector

**Solution**:
1. Verify headers configuration in deployment config
2. Check hosting provider dashboard for custom headers
3. Some providers require specific header format
4. Test on production URL (not preview URL)

### Issue: 404 Errors on Routes

**Symptom**: /privacy returns 404

**Solution**:
1. Verify build output contains privacy/index.html
2. Check build logs for errors
3. Ensure static site mode in astro.config.mjs
4. Rebuild and redeploy

### Issue: Slow Load Times

**Symptom**: Lighthouse performance < 90

**Solution**:
1. Check image sizes (optimize with TinyPNG)
2. Verify CSS is minified
3. Enable CDN on hosting provider
4. Check for render-blocking resources
5. Implement lazy loading for images

### Issue: Mobile App Links Broken

**Symptom**: App links don't open website

**Solution**:
1. Verify exact URLs app is using
2. Test redirects manually with curl
3. Check for typos in query parameter names
4. Add console logging to redirect script (temporarily)

## Monitoring & Maintenance

### Daily Checks (Automated)

- Uptime monitoring (UptimeRobot)
- SSL certificate validity
- DNS resolution

### Weekly Checks

- Google Search Console errors
- Core Web Vitals scores
- 404 error reports

### Monthly Tasks

- Review and update FAQs
- Check for broken links
- Update legal content if needed
- Review support emails for common issues

### Quarterly Tasks

- Comprehensive Lighthouse audit
- Accessibility re-audit
- Security headers review
- Dependency updates (npm audit)

## Support

For deployment issues:
- Email: support@stackmap.app
- Response time: 1-2 business days

## Documentation Links

- Vercel Docs: https://vercel.com/docs
- Netlify Docs: https://docs.netlify.com
- Astro Docs: https://docs.astro.build
- Cloudflare Pages Docs: https://developers.cloudflare.com/pages
