# SmilePile Landing Page - Architecture Diagrams

**Phase 3: Technical Planning**
**Created:** October 2, 2025

---

## 1. System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                         USER REQUESTS                               │
│  (Mobile App Links, Direct Navigation, Search Engine Crawlers)     │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         CDN / HOSTING LAYER                         │
│  (Vercel / Netlify / Cloudflare Pages)                             │
│  - HTTPS Termination (SSL)                                          │
│  - Server-side Redirects (/?privacy → /privacy)                     │
│  - Caching (Static Assets)                                          │
│  - Security Headers (CSP, X-Frame-Options)                          │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         STATIC FILES (dist/)                        │
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │  index.html  │  │ privacy/     │  │  terms/      │             │
│  │  (Homepage)  │  │ index.html   │  │  index.html  │             │
│  └──────────────┘  └──────────────┘  └──────────────┘             │
│                                                                     │
│  ┌──────────────┐  ┌──────────────────────────────────────────┐   │
│  │  support/    │  │  _astro/                                 │   │
│  │  index.html  │  │  - main.css (minified, 24KB)            │   │
│  └──────────────┘  │  - redirect.js (minified, 2KB)          │   │
│                    └──────────────────────────────────────────┘   │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │  images/                                                     │ │
│  │  - logo.svg (10KB)                                          │ │
│  │  - screenshots/ (WebP optimized, <200KB each)              │ │
│  │  - badges/ (App Store, Google Play SVGs)                   │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐                               │
│  │ sitemap.xml  │  │ robots.txt   │                               │
│  └──────────────┘  └──────────────┘                               │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. Astro Build Pipeline

```
┌─────────────────────────────────────────────────────────────────────┐
│                         SOURCE CODE (src/)                          │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
         ┌────────────────────────────────────────────────┐
         │            ASTRO COMPILATION                    │
         │                                                 │
         │  ┌──────────────────────────────────────────┐  │
         │  │  .astro files → Static HTML              │  │
         │  │  - Server-side rendering (at build time) │  │
         │  │  - Component hydration (minimal JS)      │  │
         │  └──────────────────────────────────────────┘  │
         │                                                 │
         │  ┌──────────────────────────────────────────┐  │
         │  │  Markdown → HTML                         │  │
         │  │  - privacy.md → <article> content        │  │
         │  │  - terms.md → <article> content          │  │
         │  └──────────────────────────────────────────┘  │
         │                                                 │
         │  ┌──────────────────────────────────────────┐  │
         │  │  Tailwind CSS Processing                 │  │
         │  │  - PurgeCSS removes unused styles        │  │
         │  │  - Minification                          │  │
         │  │  - Output: main.css (~24KB)              │  │
         │  └──────────────────────────────────────────┘  │
         │                                                 │
         │  ┌──────────────────────────────────────────┐  │
         │  │  JavaScript Bundling                     │  │
         │  │  - Tree-shaking (remove unused code)     │  │
         │  │  - Minification                          │  │
         │  │  - Code splitting                        │  │
         │  └──────────────────────────────────────────┘  │
         │                                                 │
         │  ┌──────────────────────────────────────────┐  │
         │  │  Image Optimization                      │  │
         │  │  - WebP conversion                       │  │
         │  │  - Responsive srcsets                    │  │
         │  │  - Compression                           │  │
         │  └──────────────────────────────────────────┘  │
         └────────────────────────────────────────────────┘
                                 │
                                 ▼
         ┌────────────────────────────────────────────────┐
         │         INTEGRATIONS & PLUGINS                 │
         │                                                 │
         │  @astrojs/sitemap → sitemap.xml                │
         │  astro-compress  → Minify HTML/CSS/JS/Images   │
         │  @astrojs/tailwind → Tailwind CSS processing   │
         └────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         OUTPUT (dist/)                              │
│  - Static HTML files (one per page)                                │
│  - Optimized CSS/JS bundles                                        │
│  - Compressed images (WebP)                                        │
│  - Sitemap, robots.txt                                             │
│  - Asset hashing for cache busting                                 │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3. Component Hierarchy

```
┌─────────────────────────────────────────────────────────────────────┐
│                         BaseLayout.astro                            │
│  (Root layout for all pages)                                       │
│                                                                     │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  <head>                                                     │   │
│  │  - Meta tags (SEO)                                         │   │
│  │  - Open Graph tags                                         │   │
│  │  - Favicons                                                │   │
│  │  - Preload critical assets                                 │   │
│  │  - Inline critical CSS                                     │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  Header.astro                                              │   │
│  │  ┌──────────────────────────────────────────────────────┐ │   │
│  │  │  Logo + Navigation                                   │ │   │
│  │  │  - Desktop: Horizontal menu                          │ │   │
│  │  │  - Mobile: Hamburger menu (collapsible)             │ │   │
│  │  └──────────────────────────────────────────────────────┘ │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  <main>                                                     │   │
│  │  ┌──────────────────────────────────────────────────────┐ │   │
│  │  │  <slot />                                            │ │   │
│  │  │  (Page-specific content goes here)                   │ │   │
│  │  │                                                       │ │   │
│  │  │  Options:                                            │ │   │
│  │  │  - Homepage content (Hero, Features, Screenshots)   │ │   │
│  │  │  - Legal content (Privacy, Terms via LegalLayout)   │ │   │
│  │  │  - Support content (FAQ, Contact)                   │ │   │
│  │  └──────────────────────────────────────────────────────┘ │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  Footer.astro                                              │   │
│  │  ┌──────────────────────────────────────────────────────┐ │   │
│  │  │  Company info + Legal links                          │ │   │
│  │  │  - Privacy Policy                                    │ │   │
│  │  │  - Terms of Service                                  │ │   │
│  │  │  - Support                                           │ │   │
│  │  │  - Copyright notice                                  │ │   │
│  │  └──────────────────────────────────────────────────────┘ │   │
│  └────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### Homepage Component Breakdown

```
index.astro
└── BaseLayout
    ├── Hero.astro
    │   ├── Heading (h1)
    │   ├── Subheading (p)
    │   ├── DownloadButtons.astro
    │   │   ├── App Store badge (SVG link)
    │   │   └── Google Play badge (SVG link)
    │   └── Hero image (optimized screenshot)
    │
    ├── Features.astro
    │   ├── Feature 1: Timeline View
    │   ├── Feature 2: Privacy-First
    │   ├── Feature 3: Easy Search
    │   └── Feature 4: Local Storage
    │
    └── Screenshots.astro
        ├── iOS screenshot 1
        ├── iOS screenshot 2
        ├── Android screenshot 1
        └── Android screenshot 2
```

### Legal Pages Component Breakdown

```
privacy.astro / terms.astro
└── LegalLayout.astro
    └── BaseLayout
        ├── Desktop: Sidebar
        │   └── TableOfContents.astro
        │       ├── Generate TOC from headings
        │       ├── Smooth scroll anchor links
        │       └── Sticky positioning
        │
        └── Main Content
            ├── Header
            │   ├── Page title (h1)
            │   └── Last updated date
            │
            ├── <article>
            │   ├── Markdown content rendered as HTML
            │   ├── Proper heading hierarchy (h2, h3)
            │   └── Styled with Tailwind Typography
            │
            └── Footer
                └── Contact link (support@stackmap.app)
```

### Support Page Component Breakdown

```
support.astro
└── BaseLayout
    ├── Page header (h1)
    │
    ├── FAQ section
    │   ├── FAQ.astro (component)
    │   │   ├── <details> element (native accordion)
    │   │   ├── <summary> (question)
    │   │   └── Answer content
    │   │
    │   └── Loop through faqs.json
    │
    ├── Troubleshooting section
    │   └── List of common tips
    │
    └── ContactCard.astro
        ├── Support email (mailto: link)
        ├── Response time expectation
        └── Instructions for users
```

---

## 4. Routing & Redirect Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                         USER REQUEST                                │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                ┌────────────────┴────────────────┐
                │                                 │
                ▼                                 ▼
    ┌────────────────────┐            ┌────────────────────┐
    │  Direct Path       │            │  Query Parameter   │
    │  /privacy          │            │  /?privacy         │
    │  /terms            │            │  /?tos             │
    │  /support          │            │                    │
    └────────┬───────────┘            └────────┬───────────┘
             │                                 │
             │                                 ▼
             │                    ┌────────────────────────────┐
             │                    │  Server-Side Redirect?     │
             │                    │  (Hosting provider config) │
             │                    └────────┬───────────────────┘
             │                             │
             │                    ┌────────┴────────┐
             │                    │                 │
             │                    ▼                 ▼
             │          ┌──────────────┐    ┌──────────────────┐
             │          │  YES         │    │  NO              │
             │          │  (301/302)   │    │  (Load homepage) │
             │          └──────┬───────┘    └──────┬───────────┘
             │                 │                    │
             │                 │                    ▼
             │                 │         ┌──────────────────────┐
             │                 │         │  Client-Side JS      │
             │                 │         │  Redirect            │
             │                 │         │  window.location     │
             │                 │         │  .replace('/privacy')│
             │                 │         └──────┬───────────────┘
             │                 │                │
             └─────────────────┴────────────────┘
                                 │
                                 ▼
                    ┌─────────────────────────┐
                    │  Load Target Page       │
                    │  /privacy/index.html    │
                    │  /terms/index.html      │
                    │  /support/index.html    │
                    └─────────────────────────┘
```

### Redirect Implementation Details

```javascript
// CLIENT-SIDE (in index.astro)
<script>
  if (typeof window !== 'undefined') {
    const params = new URLSearchParams(window.location.search);

    if (params.has('privacy')) {
      window.location.replace('/privacy');
    } else if (params.has('tos')) {
      window.location.replace('/terms');
    }
  }
</script>

// SERVER-SIDE (Netlify _redirects)
/?privacy    /privacy    301
/?tos        /terms      301

// SERVER-SIDE (Vercel vercel.json)
{
  "redirects": [
    { "source": "/?privacy", "destination": "/privacy", "permanent": true },
    { "source": "/?tos", "destination": "/terms", "permanent": true }
  ]
}
```

---

## 5. Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CONTENT SOURCES                             │
└─────────────────────────────────────────────────────────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Markdown Files │    │  JSON Data      │    │  Static Assets  │
│                 │    │                 │    │                 │
│ - privacy.md    │    │ - faqs.json     │    │ - images/       │
│ - terms.md      │    │                 │    │ - badges/       │
│                 │    │                 │    │ - favicons      │
└────────┬────────┘    └────────┬────────┘    └────────┬────────┘
         │                      │                       │
         └──────────────────────┼───────────────────────┘
                                │
                                ▼
                    ┌───────────────────────┐
                    │  ASTRO BUILD PROCESS  │
                    │                       │
                    │  1. Parse markdown    │
                    │  2. Load JSON         │
                    │  3. Optimize images   │
                    │  4. Compile components│
                    │  5. Generate HTML     │
                    └───────────┬───────────┘
                                │
                                ▼
                    ┌───────────────────────┐
                    │  RENDERED PAGES       │
                    │                       │
                    │  - HTML with content  │
                    │  - Inlined critical CSS│
                    │  - Optimized images   │
                    │  - Minimal JS         │
                    └───────────┬───────────┘
                                │
                                ▼
                    ┌───────────────────────┐
                    │  STATIC OUTPUT (dist/)│
                    │                       │
                    │  Ready for deployment │
                    └───────────────────────┘
```

---

## 6. Performance Optimization Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                         BROWSER REQUEST                             │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
                    ┌─────────────────────────┐
                    │  HTTPS (SSL)            │
                    │  - Encrypted connection │
                    │  - Certificate valid    │
                    └────────────┬────────────┘
                                 │
                                 ▼
                    ┌─────────────────────────┐
                    │  CDN / EDGE CACHE       │
                    │  - Cached static files  │
                    │  - Geographic proximity │
                    │  - Reduce latency       │
                    └────────────┬────────────┘
                                 │
                    ┌────────────┴────────────┐
                    │  Cache HIT?             │
                    └────────────┬────────────┘
                                 │
                    ┌────────────┴────────────┐
                    │                         │
                    ▼                         ▼
          ┌──────────────┐          ┌──────────────────┐
          │  YES         │          │  NO              │
          │  Serve cache │          │  Fetch from origin│
          └──────┬───────┘          └──────┬───────────┘
                 │                         │
                 └────────────┬────────────┘
                              │
                              ▼
              ┌───────────────────────────────┐
              │  COMPRESSION (Brotli/Gzip)    │
              │  - Compress HTML/CSS/JS       │
              │  - Reduce transfer size       │
              └───────────────┬───────────────┘
                              │
                              ▼
              ┌───────────────────────────────┐
              │  BROWSER RECEIVES FILES       │
              │                               │
              │  1. HTML (parse)              │
              │  2. Critical CSS (inline)     │
              │  3. Preload hero image        │
              └───────────────┬───────────────┘
                              │
                              ▼
              ┌───────────────────────────────┐
              │  LAZY LOADING                 │
              │  - Below-fold images          │
              │  - Non-critical CSS           │
              │  - Defer JavaScript           │
              └───────────────┬───────────────┘
                              │
                              ▼
              ┌───────────────────────────────┐
              │  RENDERED PAGE                │
              │  - LCP < 2.5s                 │
              │  - FID < 100ms                │
              │  - CLS < 0.1                  │
              └───────────────────────────────┘
```

---

## 7. SEO & Indexing Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                         SEARCH ENGINE CRAWLER                       │
│  (Googlebot, Bingbot, etc.)                                        │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
                    ┌─────────────────────────┐
                    │  robots.txt             │
                    │  - Check crawl rules    │
                    │  - Sitemap location     │
                    └────────────┬────────────┘
                                 │
                                 ▼
                    ┌─────────────────────────┐
                    │  sitemap.xml            │
                    │  - Discover all URLs    │
                    │  - Priority & changefreq│
                    │  - Last modified dates  │
                    └────────────┬────────────┘
                                 │
                                 ▼
                    ┌─────────────────────────┐
                    │  Crawl Pages            │
                    │  - index.html (/)       │
                    │  - privacy/index.html   │
                    │  - terms/index.html     │
                    │  - support/index.html   │
                    └────────────┬────────────┘
                                 │
                                 ▼
              ┌──────────────────────────────────┐
              │  Extract Metadata               │
              │                                  │
              │  - <title> tag                  │
              │  - <meta name="description">    │
              │  - Canonical URL                │
              │  - Open Graph tags              │
              │  - Structured data (if any)     │
              └────────────┬─────────────────────┘
                           │
                           ▼
              ┌──────────────────────────────────┐
              │  INDEX CONTENT                   │
              │                                  │
              │  - Page title                   │
              │  - Headings (h1, h2, h3)        │
              │  - Body text                    │
              │  - Image alt text               │
              │  - Internal links               │
              └────────────┬─────────────────────┘
                           │
                           ▼
              ┌──────────────────────────────────┐
              │  RANK IN SEARCH RESULTS          │
              │                                  │
              │  Factors:                       │
              │  - Content relevance            │
              │  - Page speed (Core Web Vitals) │
              │  - Mobile-friendliness          │
              │  - HTTPS security               │
              │  - Backlinks (over time)        │
              └──────────────────────────────────┘
```

---

## 8. Deployment Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         DEVELOPMENT                                 │
│  Local machine (npm run dev)                                       │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
                    ┌─────────────────────────┐
                    │  Git Push               │
                    │  (GitHub/GitLab/etc.)   │
                    └────────────┬────────────┘
                                 │
                                 ▼
         ┌───────────────────────────────────────────────┐
         │              CI/CD (Optional)                  │
         │  - GitHub Actions                              │
         │  - Run tests                                   │
         │  - Build static files (npm run build)          │
         │  - Run Lighthouse audit                        │
         │  - Deploy to staging (PR preview)              │
         └────────────────────┬──────────────────────────┘
                              │
                              ▼
         ┌───────────────────────────────────────────────┐
         │              HOSTING PLATFORM                  │
         │  (Vercel / Netlify / Cloudflare Pages)        │
         │                                                │
         │  1. Detect push to main branch                │
         │  2. Clone repository                          │
         │  3. Install dependencies (npm install)        │
         │  4. Run build (npm run build)                 │
         │  5. Deploy to edge network                    │
         └────────────────────┬──────────────────────────┘
                              │
                              ▼
         ┌───────────────────────────────────────────────┐
         │              GLOBAL CDN                        │
         │  - Multiple edge locations                     │
         │  - Automatic SSL/HTTPS                        │
         │  - DDoS protection                            │
         │  - Asset caching                              │
         └────────────────────┬──────────────────────────┘
                              │
                              ▼
         ┌───────────────────────────────────────────────┐
         │              CUSTOM DOMAIN                     │
         │  https://smilepile.app                        │
         │  - DNS configuration                          │
         │  - SSL certificate                            │
         │  - HTTPS redirect                             │
         └────────────────────┬──────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         END USERS                                   │
│  - Web browsers                                                    │
│  - Mobile apps (via webview links)                                │
│  - Search engines                                                  │
└─────────────────────────────────────────────────────────────────────┘
```

### Deployment Options Comparison

```
┌────────────────┬─────────────────┬─────────────────┬──────────────────┐
│   Feature      │     Vercel      │     Netlify     │ Cloudflare Pages │
├────────────────┼─────────────────┼─────────────────┼──────────────────┤
│ Free Tier      │ ✓ Generous      │ ✓ Generous      │ ✓ Very generous  │
│ Custom Domain  │ ✓ Free          │ ✓ Free          │ ✓ Free           │
│ Auto HTTPS     │ ✓ Let's Encrypt │ ✓ Let's Encrypt │ ✓ Cloudflare SSL │
│ Global CDN     │ ✓ Edge Network  │ ✓ CDN           │ ✓ Global Edge    │
│ Build Time     │ ⚡ Fast         │ ⚡ Fast         │ ⚡ Very Fast      │
│ PR Previews    │ ✓ Automatic     │ ✓ Deploy Previews│ ✓ Preview URLs  │
│ Redirects      │ ✓ vercel.json   │ ✓ _redirects    │ ✓ _redirects     │
│ Functions      │ ✓ Serverless    │ ✓ Netlify Fns   │ ✓ Workers        │
│ Analytics      │ $ Paid          │ $ Paid          │ ✓ Free           │
│ Rollback       │ ✓ Instant       │ ✓ Instant       │ ✓ Instant        │
└────────────────┴─────────────────┴─────────────────┴──────────────────┘

Recommendation: Vercel (best Astro integration, fastest builds)
Alternative: Netlify (excellent redirects, great DX)
Alternative: Cloudflare (best performance, free analytics)
```

---

## 9. File Size Budget Visualization

```
┌─────────────────────────────────────────────────────────────────────┐
│                         PAGE WEIGHT TARGETS                         │
└─────────────────────────────────────────────────────────────────────┘

HOMEPAGE (Target: < 500KB)
├── HTML (index.html)           ████░░░░░░░░░░░░  50KB   (10%)
├── CSS (main.css)              ████████░░░░░░░░  100KB  (20%)
├── JS (redirect.js)            ██░░░░░░░░░░░░░░  25KB   (5%)
├── Images (hero, logos)        ████████████████  250KB  (50%)
├── Fonts (system, no custom)   ░░░░░░░░░░░░░░░░  0KB    (0%)
└── Other (favicon, etc.)       ███░░░░░░░░░░░░░  50KB   (10%)
    Total:                      ████████████████  475KB  (95%)

PRIVACY PAGE (Target: < 150KB)
├── HTML (privacy/index.html)   ████████░░░░░░░░  80KB   (53%)
├── CSS (main.css, cached)      ░░░░░░░░░░░░░░░░  0KB    (cached)
├── JS (minimal)                ██░░░░░░░░░░░░░░  20KB   (13%)
├── Images (logo only)          ██░░░░░░░░░░░░░░  20KB   (13%)
└── Other                       ███░░░░░░░░░░░░░  30KB   (20%)
    Total:                      ███████████░░░░░  150KB  (100%)

TERMS PAGE (Target: < 120KB)
├── HTML (terms/index.html)     ████████░░░░░░░░  70KB   (58%)
├── CSS (main.css, cached)      ░░░░░░░░░░░░░░░░  0KB    (cached)
├── JS (minimal)                ██░░░░░░░░░░░░░░  20KB   (17%)
├── Images (logo only)          ██░░░░░░░░░░░░░░  20KB   (17%)
└── Other                       █░░░░░░░░░░░░░░░  10KB   (8%)
    Total:                      ████████████░░░░  120KB  (100%)

SUPPORT PAGE (Target: < 200KB)
├── HTML (support/index.html)   ████████░░░░░░░░  80KB   (40%)
├── CSS (main.css, cached)      ░░░░░░░░░░░░░░░░  0KB    (cached)
├── JS (FAQ accordion)          ████░░░░░░░░░░░░  40KB   (20%)
├── Images (icons, logo)        ████░░░░░░░░░░░░  50KB   (25%)
└── Other                       ███░░░░░░░░░░░░░  30KB   (15%)
    Total:                      ████████████████  200KB  (100%)

Legend:
█ = Used
░ = Available headroom
```

---

## 10. Accessibility Testing Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                         ACCESSIBILITY TESTING                       │
└─────────────────────────────────────────────────────────────────────┘

AUTOMATED TESTING
├── axe DevTools (Browser Extension)
│   ├── Color contrast violations
│   ├── Missing alt text
│   ├── ARIA errors
│   └── Heading hierarchy issues
│
├── Lighthouse (Chrome DevTools)
│   ├── Accessibility score (target: 100)
│   ├── ARIA best practices
│   ├── Form labels
│   └── Contrast ratios
│
└── pa11y (CLI tool)
    ├── WCAG 2.1 AA validation
    ├── HTML validation
    └── Automated checks

                    │
                    ▼

MANUAL TESTING
├── Keyboard Navigation
│   ├── Tab through all interactive elements
│   ├── Enter/Space to activate buttons
│   ├── Escape to close modals/menus
│   ├── Arrow keys for navigation (if applicable)
│   └── No keyboard traps
│
├── Screen Reader Testing
│   ├── VoiceOver (macOS/iOS)
│   │   ├── Navigate by headings (VO+Cmd+H)
│   │   ├── Navigate by links (VO+Cmd+L)
│   │   ├── Read from top (VO+A)
│   │   └── Verify announcements
│   │
│   ├── TalkBack (Android)
│   │   ├── Swipe to navigate
│   │   ├── Double-tap to activate
│   │   └── Verify content order
│   │
│   └── NVDA/JAWS (Windows)
│       ├── Navigate by heading (H)
│       ├── Navigate by landmarks (D)
│       └── Forms mode (F)
│
├── Visual Testing
│   ├── Color contrast (WebAIM checker)
│   ├── Text size 16px minimum
│   ├── Line height 1.5+ for readability
│   ├── Focus indicators visible
│   └── No color-only information
│
└── Zoom Testing
    ├── 200% zoom (text readable)
    ├── No horizontal scrolling
    ├── No content cutoff
    └── Layout maintains structure

                    │
                    ▼

VALIDATION REPORT
├── ✅ Zero WCAG 2.1 AA violations
├── ✅ Lighthouse Accessibility: 100
├── ✅ Keyboard navigable
├── ✅ Screen reader friendly
└── ✅ Visual contrast compliant
```

---

**Architecture Documentation Complete**
**Next Phase:** Security Review (Phase 4)
**Created:** October 2, 2025
