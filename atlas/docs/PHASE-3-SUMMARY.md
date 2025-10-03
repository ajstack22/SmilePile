# Phase 3 Technical Plan - Executive Summary

**Project:** SmilePile Landing Page
**Phase:** Planning (Phase 3 of 9)
**Status:** COMPLETE
**Created:** October 2, 2025

---

## Quick Reference

### Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Framework** | Astro 4.x | Static site generator with zero JS by default, built-in optimizations |
| **CSS** | Tailwind CSS | Rapid development, excellent purge/tree-shaking, accessibility utilities |
| **Images** | Astro Image | Automatic WebP conversion, responsive srcsets, lazy loading |
| **Routing** | Client + Server redirects | Client-side fallback, server-side optimal (hosting dependent) |
| **Content** | Markdown files | Easy to edit, version control friendly, portable |

### Performance Targets

```
Lighthouse Scores (Target: 90+)
├── Performance:     95+ (mobile), 98+ (desktop)
├── Accessibility:   100
├── Best Practices:  95+
└── SEO:            100

Core Web Vitals
├── LCP: < 2.5s
├── FID: < 100ms
└── CLS: < 0.1

Page Weights
├── Homepage:     < 500KB
├── Privacy:      < 150KB
├── Terms:        < 120KB
└── Support:      < 200KB
```

---

## Project Architecture

### Directory Structure
```
smilepile-landing/
├── src/
│   ├── pages/           # Routes: index, privacy, terms, support
│   ├── layouts/         # BaseLayout, LegalLayout
│   ├── components/      # Reusable UI (Hero, FAQ, Header, Footer)
│   ├── content/         # Markdown (privacy.md, terms.md, faqs.json)
│   └── styles/          # CSS (global, legal, components)
├── public/
│   ├── images/          # Optimized assets
│   ├── robots.txt
│   └── favicons
└── dist/                # Build output (deploy this)
```

### Component Hierarchy
```
BaseLayout (all pages)
├── <head> (SEO, meta tags, CSS)
├── Header (navigation)
├── <main>
│   └── <slot /> (page content)
└── Footer (legal links)

LegalLayout (Privacy, Terms)
├── BaseLayout wrapper
├── TableOfContents
├── <article> (legal content)
└── Print styles
```

---

## Implementation Phases

### Timeline: 3-5 Days (24-32 hours)

```
Day 1: Setup + Homepage
├── Initialize Astro project (2-3h)
├── Configure Tailwind (1h)
├── Build BaseLayout (1h)
├── Create Hero component (2h)
└── Add Features/Screenshots (2-3h)
    Total: 8-10h

Day 2: Legal Pages
├── Privacy Policy page (2-2.5h)
├── Terms of Service page (1.5-2h)
├── Support page + FAQ (2-2.5h)
└── Optimize assets (2h)
    Total: 7.5-9h

Day 3: SEO + Testing
├── Meta tags + sitemap (2h)
├── Accessibility audit (2h)
├── Lighthouse optimization (2h)
└── Cross-browser testing (2h)
    Total: 8h

Day 4: Deployment
├── Production build (1h)
├── Deploy to Vercel/Netlify (1h)
├── DNS/SSL verification (30m)
└── Final testing (30m)
    Total: 3h

TOTAL: 26.5-30 hours (3-4 days at 7-10h/day)
```

---

## Query Parameter Routing Solution

### Mobile App Links
```
Mobile App → /?privacy → Redirect → /privacy
Mobile App → /?tos     → Redirect → /terms
```

### Implementation Strategy
**Dual Approach (Both Methods):**

1. **Client-Side Redirect (works everywhere):**
```javascript
// In index.astro
<script>
  const params = new URLSearchParams(window.location.search);
  if (params.has('privacy')) window.location.replace('/privacy');
  if (params.has('tos')) window.location.replace('/terms');
</script>
```

2. **Server-Side Redirect (optimal, hosting-dependent):**
```
# Netlify _redirects
/?privacy    /privacy    301
/?tos        /terms      301

# Vercel vercel.json
{ "source": "/?privacy", "destination": "/privacy", "permanent": true }
```

---

## Technology Stack

### Core Dependencies
```json
{
  "dependencies": {
    "astro": "^4.0.0",
    "@astrojs/sitemap": "^3.0.0"
  },
  "devDependencies": {
    "tailwindcss": "^3.4.0",
    "@astrojs/tailwind": "^5.0.0",
    "astro-compress": "^2.0.0",
    "@lhci/cli": "^0.13.0"
  }
}
```

### Build Commands
```bash
npm run dev        # Development server (localhost:4321)
npm run build      # Production build → /dist
npm run preview    # Preview production build
npm run validate   # Pre-deployment validation
npm run lighthouse # Performance audit
```

---

## Content Strategy

### Legal Documents
**Storage:** Markdown files in `src/content/`
```
src/content/
├── privacy.md    # Privacy policy (COPPA-compliant)
├── terms.md      # Terms of service
└── faqs.json     # FAQ data structure
```

**Benefits:**
- Easy to edit (non-technical stakeholders)
- Version control friendly (track changes)
- Portable (can migrate to CMS later)
- Supports rich formatting

### FAQ Structure (JSON)
```json
[
  {
    "id": "backup-photos",
    "category": "Getting Started",
    "question": "How do I backup my photos?",
    "answer": "SmilePile stores photos locally..."
  }
]
```

### Assets Organization
```
public/images/
├── logo.svg
├── screenshots/
│   ├── ios-home.png (< 200KB)
│   └── android-home.png (< 200KB)
└── badges/
    ├── app-store.svg
    └── google-play.svg
```

---

## Optimization Techniques

### Image Optimization
- **Format:** WebP with PNG/JPEG fallback
- **Sizes:** Responsive srcsets (1x, 2x, 3x)
- **Loading:** Lazy load below-the-fold images
- **Compression:** TinyPNG, ImageOptim, or Astro built-in
- **Target:** Each image < 200KB, total < 2MB

### CSS Optimization
- **Minification:** Automatic via Astro build
- **Purging:** Tailwind removes unused classes
- **Critical CSS:** Inlined in `<head>` for LCP
- **Target:** Total CSS < 50KB

### JavaScript Optimization
- **Tree-shaking:** Remove unused code
- **Code splitting:** Separate bundles for caching
- **Defer loading:** Non-critical scripts async
- **Target:** Total JS < 100KB

---

## Accessibility Compliance (WCAG 2.1 AA)

### Key Features
```
✓ Semantic HTML (proper heading hierarchy)
✓ Color contrast ≥ 4.5:1 (body text)
✓ Keyboard navigation (all interactive elements)
✓ Screen reader friendly (VoiceOver/TalkBack tested)
✓ Skip navigation link
✓ ARIA labels on complex widgets
✓ Focus indicators visible
✓ Alt text on all images
```

### Testing Tools
- **Automated:** axe DevTools, Lighthouse
- **Manual:** VoiceOver (macOS/iOS), TalkBack (Android)
- **Keyboard:** Tab navigation, no traps

---

## SEO Implementation

### Per-Page Meta Tags
```html
<!-- Unique for each page -->
<title>SmilePile - Family Photo Timeline Organizer</title>
<meta name="description" content="...">
<link rel="canonical" href="https://smilepile.app/">

<!-- Open Graph -->
<meta property="og:title" content="...">
<meta property="og:description" content="...">
<meta property="og:image" content="/og-image.png">

<!-- Twitter Card -->
<meta name="twitter:card" content="summary_large_image">
```

### Sitemap & Robots
```xml
<!-- Auto-generated sitemap.xml -->
<url>
  <loc>https://smilepile.app/</loc>
  <changefreq>monthly</changefreq>
  <priority>1.0</priority>
</url>
```

```txt
# robots.txt
User-agent: *
Allow: /
Sitemap: https://smilepile.app/sitemap.xml
```

---

## Deployment Options

### Recommended: Vercel
```bash
# Deploy via CLI
vercel

# Or via dashboard:
1. Import GitHub repo
2. Framework: Astro
3. Build: npm run build
4. Output: dist
5. Deploy
```

**Why Vercel:**
- Automatic HTTPS (Let's Encrypt)
- Global CDN
- Instant rollbacks
- Preview deployments
- Generous free tier

### Alternative: Netlify
```bash
# Deploy via CLI
netlify deploy --prod

# Or via dashboard:
1. Connect Git repo
2. Build: npm run build
3. Publish: dist
4. Deploy
```

### Alternative: Cloudflare Pages
- Excellent performance
- Integrated with Cloudflare DNS
- Free Workers (edge functions)

---

## Risk Mitigation

### Top Risks & Solutions

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Query param redirects fail | Medium | High | Use both client + server redirects, test extensively |
| Performance targets missed | Low | Medium | Optimize aggressively, run Lighthouse continuously |
| COPPA non-compliance | Low | High | Use vetted templates, follow FTC guidelines |
| Accessibility violations | Low | Medium | Semantic HTML, automated + manual testing |
| Deployment issues | Low | High | Test in staging, document process thoroughly |

---

## Success Criteria

### Must Pass (Blockers)
- [ ] All 4 pages accessible (/, /privacy, /terms, /support)
- [ ] Query param redirects work (/?privacy, /?tos)
- [ ] Lighthouse scores ≥ 90 (all metrics)
- [ ] Zero accessibility violations (WCAG 2.1 AA)
- [ ] Mobile responsive (iPhone SE to iPad Pro)
- [ ] HTTPS working correctly
- [ ] All links functional (no 404s)
- [ ] Legal content COPPA-compliant
- [ ] Page loads < 2s on mobile

### Validation Process
```
1. Local build validation (npm run validate)
2. Lighthouse audit (all pages)
3. Accessibility audit (axe + manual)
4. Cross-browser testing
5. Mobile device testing
6. Production deployment
7. Post-deployment verification
```

---

## Next Steps

### Phase 4: Security Review (Next)
**Agents:** Security + Peer Reviewer (parallel)
**Tasks:**
- Review architecture for security vulnerabilities
- Validate HTTPS enforcement
- Check Content Security Policy (CSP)
- Verify COPPA compliance
- Code quality review
- Performance validation

### Phase 5: Implementation
**Agent:** Developer
**Tasks:**
- Build all components
- Implement routing
- Optimize assets
- Add SEO meta tags
- Ensure accessibility

### Phase 6: Testing
**Agents:** Peer Reviewer + UX Analyst (parallel)
**Tasks:**
- Functional testing
- Performance testing
- Accessibility testing
- Cross-browser testing
- Mobile device testing

---

## Documentation Deliverables

### Included in This Plan
1. **Technical Architecture** - Complete system design
2. **Component Specifications** - Detailed component structure
3. **Implementation Timeline** - 3-5 day breakdown
4. **Risk Assessment** - Risks + mitigation strategies
5. **Deployment Guide** - Step-by-step deployment
6. **Success Criteria** - Validation checklist

### To Be Created (Phase 5+)
1. **README.md** - Project overview and setup
2. **DEPLOYMENT.md** - Detailed deployment instructions
3. **MAINTENANCE.md** - Post-launch maintenance guide
4. **CONTRIBUTING.md** - Contribution guidelines (if open source)

---

## Key Files Reference

| File | Purpose | Location |
|------|---------|----------|
| **Full Technical Plan** | Complete implementation details | `/Users/adamstack/SmilePile/atlas/docs/PHASE-3-TECHNICAL-PLAN.md` |
| **This Summary** | Quick reference guide | `/Users/adamstack/SmilePile/atlas/docs/PHASE-3-SUMMARY.md` |
| **Story 9.2** | Privacy Policy requirements | `/Users/adamstack/SmilePile/backlog/sprint-9/STORY-9.2-landing-privacy.md` |
| **Story 9.3** | Terms of Service requirements | `/Users/adamstack/SmilePile/backlog/sprint-9/STORY-9.3-landing-terms.md` |
| **Story 9.4** | Support page requirements | `/Users/adamstack/SmilePile/backlog/sprint-9/STORY-9.4-landing-support.md` |
| **Story 9.5** | Build & deployment requirements | `/Users/adamstack/SmilePile/backlog/sprint-9/STORY-9.5-landing-deployment.md` |

---

**Plan Status:** COMPLETE ✓
**Next Phase:** Security Review (Phase 4)
**Estimated Implementation:** 3-5 days (24-32 hours)
**Target Launch:** Within 1 week of implementation start
