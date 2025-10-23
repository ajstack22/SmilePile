# SmilePile Website Pre-Deployment Checklist
**Version 1.0** | Last Updated: January 2025

This comprehensive checklist ensures the SmilePile website is fully ready for production deployment to https://smilepile.app.

---

## Table of Contents
1. [Content Updates](#content-updates)
2. [Asset Preparation](#asset-preparation)
3. [Technical Verification](#technical-verification)
4. [SEO & Performance](#seo--performance)
5. [Accessibility Audit](#accessibility-audit)
6. [Security & Privacy](#security--privacy)
7. [Deployment Steps](#deployment-steps)
8. [Post-Deployment Verification](#post-deployment-verification)
9. [Ongoing Monitoring](#ongoing-monitoring)

---

## Content Updates

### App Store Links

**Location**: `/website/src/components/DownloadButtons.astro`

- [ ] **Find Apple App Store ID**
  - Go to [App Store Connect](https://appstoreconnect.apple.com/)
  - Navigate to SmilePile app
  - Copy App ID from URL or App Information section
  - Format: 10-digit number (e.g., `1234567890`)

- [ ] **Update iOS link**
  ```astro
  <!-- BEFORE -->
  <a href="https://apps.apple.com/app/id[APP_ID]">

  <!-- AFTER -->
  <a href="https://apps.apple.com/app/id1234567890">
  ```

- [ ] **Find Google Play ID**
  - Go to [Google Play Console](https://play.google.com/console/)
  - Navigate to SmilePile app
  - Copy package name/app ID
  - Format: `com.yourcompany.appname` (e.g., `app.smilepile.release`)

- [ ] **Update Android link**
  ```astro
  <!-- BEFORE -->
  <a href="https://play.google.com/store/apps/details?id=[APP_ID]">

  <!-- AFTER -->
  <a href="https://play.google.com/store/apps/details?id=app.smilepile.release">
  ```

- [ ] **Verify links work** (may return 404 if app not yet published - that's okay)

### Contact Information

**Location**: Multiple files

- [ ] **Verify support email** (`support@stackmap.app`)
  - Check `/website/src/pages/privacy.astro`
  - Check `/website/src/pages/terms.astro`
  - Check `/website/src/pages/support.astro`
  - Ensure email is monitored and auto-responder configured

- [ ] **Update copyright year** in Footer
  - Location: `/website/src/components/Footer.astro`
  - Should show current year (use dynamic `{new Date().getFullYear()}`)

### Legal Documents

- [ ] **Review Privacy Policy** (`/website/src/pages/privacy.astro`)
  - Verify "Last Updated" date is accurate
  - Confirm COPPA compliance (2025 FTC amendments)
  - Check contact email is correct
  - Review for any product changes

- [ ] **Review Terms of Service** (`/website/src/pages/terms.astro`)
  - Verify "Last Updated" date
  - Confirm app name and company details
  - Check contact information

- [ ] **Review Support/FAQ** (`/website/src/pages/support.astro`)
  - Ensure FAQs are accurate for current app version
  - Update response time expectations if needed
  - Verify all links work

### Content Accuracy

- [ ] **Homepage claims** match actual app features
- [ ] **Feature descriptions** are accurate (not overpromising)
- [ ] **Pricing** is correct (if displayed)
- [ ] **No placeholder text** remains (search for `[`, `TODO`, `FIXME`)

---

## Asset Preparation

### Screenshots

**Required**: 3-4 app screenshots showing key features

**Location**: `/website/public/images/screenshots/`

- [ ] **Capture screenshots** (follow `SCREENSHOT_GUIDE.md`)
  - Gallery/Timeline view
  - Category view
  - Kids Mode (if implemented)
  - Photo detail view (optional)

- [ ] **Optimize screenshots**
  - Compress to 200-300KB each (use ImageOptim or TinyPNG)
  - Create WebP versions for better compression
  - Maintain visual quality

- [ ] **Update homepage** with real screenshots
  - Replace SVG placeholders in `/website/src/pages/index.astro`
  - Add descriptive alt text
  - Test responsive display

- [ ] **Verify loading performance**
  - Use `loading="lazy"` for below-fold images
  - Check total page size <2MB

### Favicons

**Location**: `/website/public/`

**Currently**: Only `favicon.svg` exists

- [ ] **Create multiple favicon sizes**
  ```
  favicon.ico (16x16, 32x32, 48x48 multi-size)
  favicon-16x16.png
  favicon-32x32.png
  apple-touch-icon.png (180x180)
  android-chrome-192x192.png
  android-chrome-512x512.png
  ```

- [ ] **Generate favicons**
  - Use [Favicon Generator](https://realfavicongenerator.net/)
  - Upload `favicon.svg` or a high-res PNG logo
  - Download generated package
  - Place files in `/website/public/`

- [ ] **Update BaseLayout.astro** with favicon links
  ```html
  <link rel="icon" type="image/svg+xml" href="/favicon.svg">
  <link rel="icon" type="image/png" sizes="32x32" href="/favicon-32x32.png">
  <link rel="icon" type="image/png" sizes="16x16" href="/favicon-16x16.png">
  <link rel="apple-touch-icon" sizes="180x180" href="/apple-touch-icon.png">
  <link rel="manifest" href="/site.webmanifest">
  ```

### Open Graph Image

**Purpose**: Social sharing preview (Twitter, Facebook, LinkedIn)

**Required Size**: 1200 x 630 pixels

- [ ] **Design Open Graph image**
  - Create 1200x630px image
  - Include SmilePile logo
  - Tagline or key message
  - Clean, professional design
  - Use brand colors from DESIGN_SYSTEM.md

- [ ] **Save as** `/website/public/og-image.jpg` or `.png`

- [ ] **Update BaseLayout.astro** meta tags
  ```html
  <meta property="og:image" content="https://smilepile.app/og-image.jpg">
  <meta name="twitter:image" content="https://smilepile.app/og-image.jpg">
  ```

- [ ] **Test social sharing**
  - [Facebook Sharing Debugger](https://developers.facebook.com/tools/debug/)
  - [Twitter Card Validator](https://cards-dev.twitter.com/validator)
  - [LinkedIn Post Inspector](https://www.linkedin.com/post-inspector/)

### Logo & Branding

- [ ] **Verify logo displays correctly** in Header
- [ ] **Check logo on mobile** (responsive sizing)
- [ ] **Ensure SVG favicon** is clean and recognizable at small sizes

---

## Technical Verification

### Build Process

- [ ] **Run development server**
  ```bash
  cd /Users/adamstack/SmilePile/website
  npm install
  npm run dev
  ```

- [ ] **Verify local development** at http://localhost:4321
  - All pages load without errors
  - Navigation works
  - Styles apply correctly
  - Images display properly

- [ ] **Run production build**
  ```bash
  npm run build
  ```

- [ ] **Check build output**
  - No errors or warnings in terminal
  - `/website/dist/` folder created
  - All pages rendered (index.html, privacy/index.html, etc.)
  - Assets copied to dist

- [ ] **Preview production build**
  ```bash
  npm run preview
  ```
  - Test at http://localhost:4321
  - Verify all functionality works

- [ ] **Check build size**
  - Total dist size should be <500KB (excluding images)
  - Individual pages <50KB HTML+CSS

### Browser Testing

Test on **major browsers** and **devices**:

- [ ] **Chrome** (latest)
  - Desktop: Windows, macOS
  - Mobile: Android

- [ ] **Safari** (latest)
  - Desktop: macOS
  - Mobile: iOS (iPhone, iPad)

- [ ] **Firefox** (latest)
  - Desktop: Windows, macOS

- [ ] **Edge** (latest)
  - Desktop: Windows

**Test checklist per browser**:
- [ ] All pages load without errors
- [ ] Navigation works (header, footer links)
- [ ] Mobile menu toggles correctly
- [ ] Forms work (contact form, if any)
- [ ] Screenshots display correctly
- [ ] No console errors (open DevTools > Console)

### Responsive Design

- [ ] **Mobile** (375px width - iPhone SE)
  - Header collapses to hamburger menu
  - Text is readable (min 16px)
  - Buttons are tappable (min 44px height)
  - Images scale appropriately
  - No horizontal scroll

- [ ] **Tablet** (768px width - iPad)
  - Layout adapts appropriately
  - Navigation readable
  - Images size correctly

- [ ] **Desktop** (1280px+ width)
  - Content centered, not full-width
  - Max-width containers work
  - Navigation horizontal
  - Proper spacing

### Links & Navigation

- [ ] **All internal links work**
  - Header nav: Features, Support, Privacy, Terms
  - Footer links: Privacy, Terms, Support, Contact
  - Homepage CTAs

- [ ] **External links**
  - App Store link (may 404 if not published yet - that's okay)
  - Google Play link (may 404 if not published yet - that's okay)
  - Any external resource links

- [ ] **Query parameter redirects**
  - Test `/?privacy` → redirects to `/privacy`
  - Test `/?tos` → redirects to `/terms`

- [ ] **404 page**
  - Visit `/random-page-that-doesnt-exist`
  - Verify custom 404 page displays
  - "Back to Home" link works

---

## SEO & Performance

### Meta Tags

Check each page for proper meta tags:

**Homepage** (`/`):
- [ ] `<title>` tag (unique, <60 characters)
- [ ] Meta description (<160 characters)
- [ ] Open Graph tags (og:title, og:description, og:image)
- [ ] Twitter Card tags
- [ ] Canonical URL: `https://smilepile.app/`

**Privacy** (`/privacy`):
- [ ] Unique title: "Privacy Policy - SmilePile"
- [ ] Meta description
- [ ] Canonical URL: `https://smilepile.app/privacy`

**Terms** (`/terms`):
- [ ] Unique title: "Terms of Service - SmilePile"
- [ ] Meta description
- [ ] Canonical URL: `https://smilepile.app/terms`

**Support** (`/support`):
- [ ] Unique title: "Support & FAQ - SmilePile"
- [ ] Meta description
- [ ] Canonical URL: `https://smilepile.app/support`

### Sitemap & Robots

- [ ] **Verify sitemap.xml generated**
  - Check `/website/dist/sitemap-index.xml` after build
  - Contains all pages
  - URLs use `https://smilepile.app/` (production domain)

- [ ] **Verify robots.txt**
  - Located at `/website/public/robots.txt`
  - References sitemap correctly
  - No unintended disallow rules

### Performance Audit

**Run Google Lighthouse** (Chrome DevTools):

```bash
# Open site in Chrome
# Right-click > Inspect > Lighthouse tab
# Select "Performance", "Accessibility", "Best Practices", "SEO"
# Click "Generate report"
```

**Target scores** (out of 100):
- [ ] Performance: **90+**
- [ ] Accessibility: **95+**
- [ ] Best Practices: **95+**
- [ ] SEO: **95+**

**If scores are low**:
- Optimize images (compress, WebP)
- Minimize CSS/JS
- Enable compression (Vercel does this automatically)
- Fix accessibility issues
- Add missing meta tags

### Page Speed

- [ ] **Test page load speed**
  - [Google PageSpeed Insights](https://pagespeed.web.dev/)
  - [WebPageTest](https://www.webpagetest.org/)
  - Target: <3 seconds on 3G, <1 second on fast connection

- [ ] **Core Web Vitals**
  - LCP (Largest Contentful Paint): <2.5s
  - FID (First Input Delay): <100ms
  - CLS (Cumulative Layout Shift): <0.1

---

## Accessibility Audit

### Automated Testing

- [ ] **Run WAVE**
  - Install [WAVE Extension](https://wave.webaim.org/extension/)
  - Test each page
  - Fix all errors, review warnings

- [ ] **Run Axe DevTools**
  - Install [Axe Extension](https://www.deque.com/axe/devtools/)
  - Test each page
  - Fix all critical and serious issues

- [ ] **Lighthouse Accessibility**
  - Should score 95+ (see Performance Audit above)

### Manual Testing

- [ ] **Keyboard navigation**
  - Tab through all interactive elements
  - Verify focus visible on all elements
  - Enter/Space activates buttons and links
  - Escape closes modals/mobile menu
  - No keyboard traps

- [ ] **Screen reader test**
  - macOS: VoiceOver (Cmd+F5)
  - Windows: NVDA (free) or JAWS
  - Navigate entire site
  - Verify all content is announced
  - Check alt text for images
  - Verify form labels

- [ ] **Zoom to 200%**
  - Browser zoom to 200% (Cmd/Ctrl + Plus Plus)
  - Verify text is still readable
  - No content cut off or overlapping
  - Layout still functional

- [ ] **Color contrast**
  - All text meets 4.5:1 ratio (normal text) or 3:1 (large text)
  - Use [WebAIM Contrast Checker](https://webaim.org/resources/contrastchecker/)
  - Check buttons, links, form fields

### Accessibility Checklist

- [ ] All images have alt text (or alt="" for decorative)
- [ ] Headings are hierarchical (H1 → H2 → H3, no skips)
- [ ] Links have descriptive text (not "click here")
- [ ] Form fields have visible labels
- [ ] Buttons have accessible names
- [ ] Color is not the only way information is conveyed
- [ ] Focus indicators are visible and high-contrast
- [ ] Mobile menu is keyboard accessible
- [ ] Skip-to-main-content link exists and works

---

## Security & Privacy

### HTTPS

- [ ] **Verify HTTPS enforced**
  - After deployment, test http://smilepile.app redirects to https://
  - Vercel does this automatically

- [ ] **Check SSL certificate**
  - Visit https://smilepile.app
  - Click padlock icon in browser
  - Verify valid certificate (issued by Let's Encrypt or similar)
  - Certificate should be valid for 90 days minimum

### Security Headers

**Configured in** `/website/vercel.json`

- [ ] **Verify headers are set**:
  - `X-Frame-Options: DENY` (prevents clickjacking)
  - `X-Content-Type-Options: nosniff` (prevents MIME sniffing)
  - `Referrer-Policy: strict-origin-when-cross-origin`
  - `Permissions-Policy: camera=(), microphone=(), geolocation=()`

- [ ] **Test security headers** after deployment
  - Use [Security Headers](https://securityheaders.com/)
  - Should get A or A+ rating

### Privacy Compliance

- [ ] **Privacy Policy is accessible** at `/privacy`
- [ ] **Privacy link in footer** works
- [ ] **No tracking scripts** (Google Analytics, etc.) - privacy-first
- [ ] **No third-party cookies**
- [ ] **COPPA compliant** (privacy policy includes parental rights)
- [ ] **Contact email monitored** for privacy requests

### Content Security

- [ ] **No hardcoded secrets** in code (API keys, passwords)
- [ ] **No PII in screenshots** (use demo mode)
- [ ] **No sensitive data in git history**

---

## Deployment Steps

### Pre-Deployment

- [ ] **Commit all changes**
  ```bash
  cd /Users/adamstack/SmilePile/website
  git status
  git add .
  git commit -m "feat: Website ready for production deployment"
  ```

- [ ] **Push to main branch**
  ```bash
  git push origin main
  ```

- [ ] **Create production build locally** (final verification)
  ```bash
  npm run build
  npm run preview
  ```

### Deploy to Vercel

**Option 1: Vercel CLI** (Recommended)

- [ ] **Install Vercel CLI**
  ```bash
  npm install -g vercel
  ```

- [ ] **Login to Vercel**
  ```bash
  vercel login
  ```

- [ ] **Deploy**
  ```bash
  cd /Users/adamstack/SmilePile/website
  vercel --prod
  ```

- [ ] **Follow prompts**:
  - Set up and deploy: Y
  - Scope: Select your account/team
  - Link to existing project: N (first time)
  - Project name: smilepile-website
  - Directory: `./` (current directory)
  - Override settings: N (use detected settings)

- [ ] **Wait for deployment**
  - Vercel will build and deploy
  - Provides preview URL: `https://smilepile-website-xxx.vercel.app`

**Option 2: Vercel Dashboard** (Alternative)

- [ ] **Go to** [Vercel Dashboard](https://vercel.com/dashboard)
- [ ] **Click "Add New Project"**
- [ ] **Import Git Repository**
  - Connect GitHub/GitLab/Bitbucket
  - Select SmilePile repository
  - Framework: Astro (auto-detected)
  - Root directory: `website`
  - Build command: `npm run build` (auto-detected)
  - Output directory: `dist` (auto-detected)
- [ ] **Click "Deploy"**

### Configure Custom Domain

- [ ] **Add domain in Vercel**
  - Project Settings > Domains
  - Add `smilepile.app`
  - Add `www.smilepile.app` (redirect to non-www)

- [ ] **Configure DNS**
  - Go to your domain registrar (e.g., Namecheap, GoDaddy, Cloudflare)
  - Add DNS records as shown by Vercel:
    ```
    Type: A
    Name: @
    Value: 76.76.21.21 (Vercel IP, may vary)

    Type: CNAME
    Name: www
    Value: cname.vercel-dns.com
    ```

- [ ] **Wait for DNS propagation** (5 minutes - 48 hours, usually <1 hour)

- [ ] **Verify HTTPS certificate issued**
  - Vercel automatically provisions SSL via Let's Encrypt
  - Check Project Settings > Domains
  - Status should show "Valid Certificate"

---

## Post-Deployment Verification

### Immediate Checks (Within 5 Minutes)

- [ ] **Visit** https://smilepile.app
  - Site loads without errors
  - Correct content displays

- [ ] **Test homepage**
  - Hero section loads
  - Screenshots display (if added)
  - Download buttons visible
  - Footer displays

- [ ] **Test all pages**
  - `/privacy` loads
  - `/terms` loads
  - `/support` loads
  - `/404` loads (try https://smilepile.app/nonexistent)

- [ ] **Test navigation**
  - Header links work
  - Footer links work
  - Mobile menu works (resize browser to mobile width)

- [ ] **Test redirects**
  - https://smilepile.app/?privacy → redirects to /privacy
  - https://smilepile.app/?tos → redirects to /terms

- [ ] **Test HTTPS**
  - http://smilepile.app → redirects to https://
  - Padlock icon shows in browser
  - Certificate is valid

- [ ] **Test on mobile device**
  - iPhone: Visit in Safari
  - Android: Visit in Chrome
  - Responsive design works
  - Touch targets are large enough

### Performance Verification (Within 1 Hour)

- [ ] **Run Lighthouse on production**
  - Open https://smilepile.app in Chrome
  - DevTools > Lighthouse > Generate Report
  - Verify 90+ scores

- [ ] **Test load speed**
  - [PageSpeed Insights](https://pagespeed.web.dev/)
  - Enter `https://smilepile.app`
  - Check mobile and desktop scores

- [ ] **Test on slow connection**
  - Chrome DevTools > Network > Throttling > Slow 3G
  - Page should load in <5 seconds

### SEO Verification (Within 24 Hours)

- [ ] **Verify sitemap accessible**
  - Visit https://smilepile.app/sitemap-index.xml
  - Should display XML sitemap

- [ ] **Verify robots.txt**
  - Visit https://smilepile.app/robots.txt
  - Should allow crawling

- [ ] **Submit to Google Search Console**
  - Go to [Google Search Console](https://search.google.com/search-console)
  - Add property: `https://smilepile.app`
  - Verify ownership (Vercel provides DNS verification)
  - Submit sitemap: `https://smilepile.app/sitemap-index.xml`

- [ ] **Submit to Bing Webmaster Tools** (Optional)
  - Go to [Bing Webmaster Tools](https://www.bing.com/webmasters)
  - Add site
  - Submit sitemap

- [ ] **Test social sharing**
  - Share on Twitter - verify card displays
  - Share on Facebook - verify preview
  - Check LinkedIn preview

### Accessibility Verification (Within 1 Week)

- [ ] **Run WAVE on production**
- [ ] **Run Axe on production**
- [ ] **Test with real screen reader** on production site
- [ ] **Zoom test** on production
- [ ] **Keyboard navigation** on production

---

## Ongoing Monitoring

### Set Up Monitoring

- [ ] **Uptime Monitoring**
  - Sign up for [UptimeRobot](https://uptimerobot.com/) (free)
  - Or [Pingdom](https://www.pingdom.com/)
  - Monitor: https://smilepile.app
  - Check interval: 5 minutes
  - Alert via email if down

- [ ] **Performance Monitoring** (Optional)
  - [Google Analytics](https://analytics.google.com/) - if privacy allows
  - Or [Plausible Analytics](https://plausible.io/) - privacy-friendly
  - Or [Fathom Analytics](https://usefathom.com/) - privacy-friendly

- [ ] **Error Monitoring** (Optional)
  - [Sentry](https://sentry.io/) - catch JavaScript errors
  - Or use Vercel Analytics (built-in)

### Regular Checks

**Weekly**:
- [ ] Check uptime monitor - no downtime
- [ ] Review analytics (if configured)
- [ ] Test site loads correctly

**Monthly**:
- [ ] Run Lighthouse audit
- [ ] Check SSL certificate expiry (should auto-renew)
- [ ] Review and respond to support emails
- [ ] Check for broken links (use [Broken Link Checker](https://www.brokenlinkcheck.com/))

**Quarterly**:
- [ ] Review privacy policy for accuracy
- [ ] Review terms of service
- [ ] Update FAQ based on common support questions
- [ ] Security header check ([Security Headers](https://securityheaders.com/))

---

## Rollback Plan

### If Something Goes Wrong

**Vercel Rollback**:
1. Go to Vercel Dashboard > Project > Deployments
2. Find previous working deployment
3. Click "..." > "Promote to Production"
4. Previous version is live immediately

**Git Rollback**:
```bash
# Find commit to revert to
git log --oneline

# Revert to previous commit
git revert <commit-hash>

# Push to trigger redeployment
git push origin main
```

**DNS Issues**:
- If DNS not propagating, wait 24-48 hours
- Check DNS records at your registrar
- Use [DNS Checker](https://dnschecker.org/) to verify propagation

---

## Common Issues & Solutions

### Issue: App Store Links 404

**Solution**: This is normal if apps aren't published yet. Update links when apps go live.

### Issue: Images Not Loading

**Solution**:
- Check file paths are correct (`/images/...` not `./images/...`)
- Verify images are in `/website/public/` directory
- Clear browser cache (Cmd/Ctrl + Shift + R)

### Issue: Slow Page Load

**Solution**:
- Optimize images (compress, use WebP)
- Enable lazy loading for below-fold images
- Check Lighthouse recommendations

### Issue: Mobile Menu Not Working

**Solution**:
- Check JavaScript is loading (no console errors)
- Verify Astro build completed successfully
- Test in different browsers

### Issue: DNS Not Resolving

**Solution**:
- Wait 24-48 hours for propagation
- Verify DNS records match Vercel's requirements
- Use `nslookup smilepile.app` to check DNS

### Issue: SSL Certificate Error

**Solution**:
- Wait a few minutes - Vercel auto-provisions SSL
- Verify domain is correctly configured in Vercel
- Check domain ownership verification

---

## Emergency Contacts

**Vercel Support**: https://vercel.com/support
**Domain Registrar Support**: [Your registrar's support contact]
**SmilePile Team**: support@stackmap.app

---

## Final Pre-Deployment Review

**Before clicking deploy, verify**:

- [ ] All placeholder content replaced (no `[APP_ID]`)
- [ ] Screenshots added or acceptable placeholders
- [ ] Favicons created
- [ ] All links tested
- [ ] Privacy policy and terms reviewed
- [ ] Build succeeds locally (`npm run build`)
- [ ] Lighthouse scores 90+
- [ ] Accessibility tested (WAVE, Axe, keyboard)
- [ ] Mobile responsive tested
- [ ] Git committed and pushed
- [ ] Ready to deploy!

---

## Post-Deployment Celebration

- [ ] 🎉 Site is live at https://smilepile.app
- [ ] 📧 Email team/stakeholders with link
- [ ] 📱 Update mobile app links to point to live site (if needed)
- [ ] 📊 Monitor analytics and uptime
- [ ] 🚀 Plan next iteration (design system updates, blog, etc.)

---

**You're ready to deploy! Follow this checklist step by step, and SmilePile's website will launch smoothly.**

**Questions?** Contact: support@stackmap.app
