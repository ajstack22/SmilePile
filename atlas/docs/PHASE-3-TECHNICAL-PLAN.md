# Phase 3: Technical Implementation Plan - SmilePile Landing Page

**Project:** SmilePile.app Landing Page & Legal Documentation
**Phase:** 3 (Planning)
**Agent:** Developer Agent
**Created:** October 2, 2025
**Timeline:** 3-5 days implementation target

---

## Executive Summary

This technical plan provides a comprehensive blueprint for implementing the SmilePile landing page using Astro as a static site generator. The solution delivers:

- **4 static pages**: Homepage, Privacy Policy, Terms of Service, Support
- **Query parameter routing**: Client-side redirects for `/?privacy` and `/?tos`
- **Performance targets**: Lighthouse 95+ scores across all metrics
- **Accessibility**: WCAG 2.1 AA compliance
- **Privacy-first**: No tracking, COPPA-compliant
- **Deployment-ready**: Static files for any hosting provider

**Key Technical Decisions:**
- Static Site Generator: **Astro 4.x** (chosen for speed, simplicity, and built-in optimizations)
- CSS Strategy: **Custom CSS with Tailwind utilities** (lightweight, maintainable)
- Image Optimization: **Astro Image** (automatic WebP conversion, responsive images)
- Routing: **Client-side redirects** for query parameters (static hosting limitation)
- Build Output: **Static HTML/CSS/JS** (~3-5MB total, <500KB initial load)

---

## 1. Astro Project Architecture

### 1.1 Directory Structure

```
smilepile-landing/
├── src/
│   ├── pages/                    # Route-based pages
│   │   ├── index.astro          # Homepage (/)
│   │   ├── privacy.astro        # Privacy Policy (/privacy)
│   │   ├── terms.astro          # Terms of Service (/terms)
│   │   ├── support.astro        # Support (/support)
│   │   └── 404.astro            # Custom 404 page
│   ├── layouts/
│   │   ├── BaseLayout.astro     # Base HTML structure (shared)
│   │   └── LegalLayout.astro    # Legal pages layout (Privacy, Terms)
│   ├── components/
│   │   ├── Header.astro         # Site header with navigation
│   │   ├── Footer.astro         # Site footer with legal links
│   │   ├── Hero.astro           # Homepage hero section
│   │   ├── Features.astro       # Features showcase
│   │   ├── Screenshots.astro    # App screenshots carousel
│   │   ├── DownloadButtons.astro # App Store/Google Play badges
│   │   ├── FAQ.astro            # Accordion FAQ component
│   │   ├── ContactCard.astro    # Support contact information
│   │   └── TableOfContents.astro # TOC for legal pages
│   ├── content/
│   │   ├── privacy.md           # Privacy policy content (markdown)
│   │   ├── terms.md             # Terms of service content (markdown)
│   │   └── faqs.json            # FAQ data structure
│   ├── styles/
│   │   ├── global.css           # Global styles and CSS variables
│   │   ├── legal.css            # Legal page-specific styles
│   │   └── components.css       # Component-specific styles
│   └── scripts/
│       └── redirect.js          # Query parameter redirect logic
├── public/
│   ├── images/
│   │   ├── logo.svg             # SmilePile logo (SVG)
│   │   ├── logo-dark.svg        # Dark mode logo variant
│   │   ├── screenshots/         # App screenshots
│   │   │   ├── ios-home.png
│   │   │   ├── ios-timeline.png
│   │   │   ├── android-home.png
│   │   │   └── android-timeline.png
│   │   └── badges/              # App store badges
│   │       ├── app-store.svg
│   │       └── google-play.svg
│   ├── favicon.ico
│   ├── apple-touch-icon.png
│   ├── favicon-16x16.png
│   ├── favicon-32x32.png
│   ├── android-chrome-192x192.png
│   ├── android-chrome-512x512.png
│   ├── site.webmanifest
│   ├── robots.txt
│   └── og-image.png             # Open Graph share image
├── astro.config.mjs             # Astro configuration
├── tsconfig.json                # TypeScript configuration
├── package.json                 # Dependencies and scripts
├── README.md                    # Project documentation
├── DEPLOYMENT.md                # Deployment instructions
└── .gitignore

dist/ (generated on build)       # Production-ready static files
```

### 1.2 Component Hierarchy and Reusability

**Layout Hierarchy:**
```
BaseLayout (all pages)
├── <head> (SEO meta tags, favicons, CSS)
├── Header component
├── <slot /> (page content)
└── Footer component

LegalLayout (extends BaseLayout)
├── BaseLayout wrapper
└── Legal-specific structure:
    ├── TableOfContents component
    ├── <article> with legal content
    └── Print-friendly styles
```

**Reusable Components:**
- **Header.astro**: Sticky navigation, logo, links to all pages
- **Footer.astro**: Company info, legal links, copyright
- **Hero.astro**: Homepage hero with app description and CTA
- **Features.astro**: Grid of key app features (3-5 items)
- **Screenshots.astro**: Responsive image carousel/grid
- **DownloadButtons.astro**: App Store and Google Play badges with tracking
- **FAQ.astro**: Accordion component using `<details>` element
- **ContactCard.astro**: Highlighted support email with instructions
- **TableOfContents.astro**: Auto-generated TOC from headings

### 1.3 Routing Strategy

**Static Routes (Astro file-based):**
- `/` → `src/pages/index.astro`
- `/privacy` → `src/pages/privacy.astro`
- `/terms` → `src/pages/terms.astro`
- `/support` → `src/pages/support.astro`
- `/404` → `src/pages/404.astro`

**Query Parameter Routing (Client-side):**
```javascript
// In src/pages/index.astro frontmatter
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
```

**Alternative: Server-side redirects (hosting provider config):**
```
// Netlify _redirects
/?privacy    /privacy    301
/?tos        /terms      301

// Vercel vercel.json
{
  "redirects": [
    { "source": "/?privacy", "destination": "/privacy", "permanent": true },
    { "source": "/?tos", "destination": "/terms", "permanent": true }
  ]
}
```

### 1.4 Configuration Files

**astro.config.mjs:**
```javascript
import { defineConfig } from 'astro/config';
import sitemap from '@astrojs/sitemap';
import compress from 'astro-compress';

export default defineConfig({
  site: 'https://smilepile.app',
  compressHTML: true,
  build: {
    inlineStylesheets: 'auto', // Inline critical CSS
    assets: '_astro',
  },
  integrations: [
    sitemap({
      filter: (page) => !page.includes('/404'),
      changefreq: 'monthly',
      priority: 0.7,
      lastmod: new Date(),
    }),
    compress({
      css: true,
      html: true,
      img: true,
      js: true,
      svg: true,
    }),
  ],
  vite: {
    build: {
      cssCodeSplit: false, // Single CSS file for simplicity
      rollupOptions: {
        output: {
          assetFileNames: '_astro/[name].[hash][extname]',
        },
      },
    },
  },
});
```

**package.json:**
```json
{
  "name": "smilepile-landing",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "dev": "astro dev",
    "build": "astro build",
    "preview": "astro preview",
    "validate": "node scripts/validate-build.js",
    "lighthouse": "lhci autorun"
  },
  "dependencies": {
    "astro": "^4.0.0",
    "@astrojs/sitemap": "^3.0.0"
  },
  "devDependencies": {
    "astro-compress": "^2.0.0",
    "@lhci/cli": "^0.13.0",
    "glob": "^10.3.10"
  }
}
```

**tsconfig.json:**
```json
{
  "extends": "astro/tsconfigs/strict",
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@components/*": ["src/components/*"],
      "@layouts/*": ["src/layouts/*"],
      "@styles/*": ["src/styles/*"],
      "@content/*": ["src/content/*"]
    }
  }
}
```

---

## 2. Technology Stack Details

### 2.1 Astro Version and Plugins

**Core Framework:**
- **Astro 4.0.0+**: Latest stable version
  - Zero JavaScript by default (ship only what's needed)
  - Built-in image optimization
  - File-based routing
  - Component islands (if interactive components needed)

**Essential Integrations:**
1. **@astrojs/sitemap** (^3.0.0)
   - Auto-generates sitemap.xml
   - SEO-friendly URL structure

2. **astro-compress** (^2.0.0)
   - Minifies HTML, CSS, JS
   - Optimizes images and SVGs
   - Reduces bundle size

**Optional Integrations (evaluate need):**
- **@astrojs/mdx**: If rich markdown content needed (probably overkill)
- **@astrojs/partytown**: If adding analytics (defers to web worker)
- **@astrojs/prefetch**: Prefetch links on hover (minor perf gain)

### 2.2 CSS Approach (Recommendation: Tailwind CSS)

**Recommended: Tailwind CSS with Custom CSS Variables**

**Why Tailwind:**
- Utility-first approach (rapid development)
- Built-in responsive design
- Excellent purge/tree-shaking (removes unused CSS)
- Consistent spacing/sizing system
- Well-documented accessibility patterns

**Implementation:**
```bash
npm install -D tailwindcss @astrojs/tailwind
```

**tailwind.config.cjs:**
```javascript
module.exports = {
  content: ['./src/**/*.{astro,html,js,jsx,md,mdx,svelte,ts,tsx,vue}'],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#f0f9ff',
          100: '#e0f2fe',
          500: '#0ea5e9',  // SmilePile brand blue
          600: '#0284c7',
          700: '#0369a1',
        },
        accent: {
          400: '#fbbf24',  // Warm yellow accent
          500: '#f59e0b',
        },
      },
      fontFamily: {
        sans: ['-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
      },
      maxWidth: {
        'content': '65ch', // Optimal reading width
      },
    },
  },
  plugins: [
    require('@tailwindcss/typography'), // For legal content
  ],
};
```

**Alternative: Custom CSS (Lightweight)**
If avoiding Tailwind dependency:
```css
/* src/styles/global.css */
:root {
  --color-primary: #0ea5e9;
  --color-accent: #f59e0b;
  --color-text: #1f2937;
  --color-bg: #ffffff;
  --font-sans: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  --spacing-unit: 1rem;
  --max-content-width: 65ch;
}

/* Mobile-first responsive breakpoints */
@media (min-width: 768px) { /* Tablet */ }
@media (min-width: 1024px) { /* Desktop */ }
```

**Decision: Use Tailwind CSS** for this project (faster development, better DX).

### 2.3 Image Optimization Strategy

**Use Astro's Built-in Image Component:**

```astro
---
import { Image } from 'astro:assets';
import iosHome from '../images/screenshots/ios-home.png';
---

<Image
  src={iosHome}
  alt="SmilePile iOS home screen showing organized family photos"
  width={375}
  height={812}
  format="webp"
  quality={80}
  loading="lazy"
  class="rounded-lg shadow-xl"
/>
```

**Optimization Features:**
- Automatic WebP conversion (with PNG/JPEG fallback)
- Responsive image generation (multiple sizes)
- Lazy loading for below-the-fold images
- Built-in srcset generation
- Format detection based on browser support

**Image Preparation Checklist:**
1. Source images at 2x resolution (retina displays)
2. Screenshots: 750x1624px (iPhone), 1080x2400px (Android)
3. Logo: SVG format (scalable, small file size)
4. App badges: Use official SVG from Apple/Google
5. Open Graph image: 1200x630px (social sharing)

**Target Sizes:**
- Logo: <10KB (SVG)
- Screenshots: <200KB each (WebP)
- App badges: <5KB each (SVG)
- OG image: <100KB (optimized PNG/JPEG)

### 2.4 Build Tools and Optimizations

**Build Process:**
```bash
# Development
npm run dev          # Start dev server (http://localhost:4321)

# Production Build
npm run build        # Build static files to /dist
npm run preview      # Preview production build locally

# Validation
npm run validate     # Run pre-deployment checks
npm run lighthouse   # Performance audit
```

**Build Optimizations (Automatic via Astro):**
1. **HTML Minification**: Remove whitespace, comments
2. **CSS Minification**: Compress styles, remove unused CSS
3. **JS Tree-shaking**: Remove unused JavaScript
4. **Image Optimization**: Convert to WebP, resize, compress
5. **Asset Hashing**: Cache-busting filenames (`main.a3f2b1.css`)
6. **Code Splitting**: Separate bundles for better caching

**Additional Optimizations:**
- **Critical CSS Inlining**: Inline above-the-fold CSS in `<head>`
- **Font Subsetting**: If using custom fonts (not needed with system fonts)
- **Preload Key Assets**: `<link rel="preload">` for LCP images
- **Defer Non-Critical JS**: `<script defer>` or `<script type="module">`

**Performance Budget:**
```javascript
// In package.json or CI config
{
  "budgets": [
    { "path": "*.js", "maxSize": "100 KB" },
    { "path": "*.css", "maxSize": "50 KB" },
    { "path": "*.png", "maxSize": "500 KB" },
    { "path": "index.html", "maxSize": "50 KB" }
  ]
}
```

---

## 3. Component Design

### 3.1 Layout Components

**BaseLayout.astro** (Shared HTML structure):
```astro
---
import Header from '@components/Header.astro';
import Footer from '@components/Footer.astro';
import '@styles/global.css';

interface Props {
  title: string;
  description: string;
  ogImage?: string;
  canonical?: string;
}

const {
  title,
  description,
  ogImage = '/og-image.png',
  canonical = Astro.url.pathname
} = Astro.props;

const siteUrl = 'https://smilepile.app';
---

<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>{title}</title>
  <meta name="description" content={description}>

  <!-- Canonical URL -->
  <link rel="canonical" href={siteUrl + canonical}>

  <!-- Open Graph -->
  <meta property="og:type" content="website">
  <meta property="og:url" content={siteUrl + canonical}>
  <meta property="og:title" content={title}>
  <meta property="og:description" content={description}>
  <meta property="og:image" content={siteUrl + ogImage}>

  <!-- Twitter Card -->
  <meta name="twitter:card" content="summary_large_image">
  <meta name="twitter:title" content={title}>
  <meta name="twitter:description" content={description}>
  <meta name="twitter:image" content={siteUrl + ogImage}>

  <!-- Favicons -->
  <link rel="icon" type="image/x-icon" href="/favicon.ico">
  <link rel="icon" type="image/png" sizes="32x32" href="/favicon-32x32.png">
  <link rel="icon" type="image/png" sizes="16x16" href="/favicon-16x16.png">
  <link rel="apple-touch-icon" sizes="180x180" href="/apple-touch-icon.png">
  <link rel="manifest" href="/site.webmanifest">

  <!-- Preload critical assets -->
  <link rel="preload" href="/fonts/system-font.woff2" as="font" type="font/woff2" crossorigin>
</head>
<body class="min-h-screen flex flex-col bg-gray-50 text-gray-900">
  <Header />
  <main class="flex-grow">
    <slot />
  </main>
  <Footer />
</body>
</html>
```

**LegalLayout.astro** (For Privacy/Terms pages):
```astro
---
import BaseLayout from './BaseLayout.astro';
import TableOfContents from '@components/TableOfContents.astro';

interface Props {
  title: string;
  description: string;
  lastUpdated: string;
}

const { title, description, lastUpdated } = Astro.props;
---

<BaseLayout title={title} description={description}>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
    <div class="lg:grid lg:grid-cols-4 lg:gap-8">
      <!-- Sidebar TOC (desktop only) -->
      <aside class="hidden lg:block lg:col-span-1">
        <nav class="sticky top-24" aria-label="Table of Contents">
          <TableOfContents />
        </nav>
      </aside>

      <!-- Main Legal Content -->
      <article class="lg:col-span-3 prose prose-lg max-w-none">
        <header class="not-prose mb-8">
          <h1 class="text-4xl font-bold text-gray-900 mb-2">{title}</h1>
          <p class="text-sm text-gray-600">Last Updated: {lastUpdated}</p>
        </header>

        <slot />

        <footer class="not-prose mt-12 pt-8 border-t border-gray-200">
          <p class="text-sm text-gray-600">
            Have questions? Contact us at
            <a href="mailto:support@stackmap.app" class="text-primary-600 hover:underline">
              support@stackmap.app
            </a>
          </p>
        </footer>
      </article>
    </div>
  </div>
</BaseLayout>
```

### 3.2 Reusable UI Components

**Header.astro** (Site Navigation):
```astro
---
import { Image } from 'astro:assets';
import logo from '../public/images/logo.svg';
---

<header class="sticky top-0 z-50 bg-white shadow-sm">
  <nav class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8" aria-label="Main navigation">
    <div class="flex justify-between items-center h-16">
      <!-- Logo -->
      <a href="/" class="flex items-center space-x-2">
        <Image src={logo} alt="SmilePile" width={40} height={40} />
        <span class="text-xl font-bold text-gray-900">SmilePile</span>
      </a>

      <!-- Desktop Navigation -->
      <ul class="hidden md:flex space-x-8">
        <li><a href="/#features" class="nav-link">Features</a></li>
        <li><a href="/support" class="nav-link">Support</a></li>
        <li><a href="/privacy" class="nav-link">Privacy</a></li>
        <li><a href="/terms" class="nav-link">Terms</a></li>
      </ul>

      <!-- Mobile Menu Button -->
      <button
        id="mobile-menu-btn"
        class="md:hidden p-2"
        aria-label="Toggle menu"
        aria-expanded="false"
      >
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
        </svg>
      </button>
    </div>

    <!-- Mobile Navigation (hidden by default) -->
    <ul id="mobile-menu" class="hidden md:hidden pb-4 space-y-2">
      <li><a href="/#features" class="mobile-nav-link">Features</a></li>
      <li><a href="/support" class="mobile-nav-link">Support</a></li>
      <li><a href="/privacy" class="mobile-nav-link">Privacy</a></li>
      <li><a href="/terms" class="mobile-nav-link">Terms</a></li>
    </ul>
  </nav>
</header>

<script>
  // Mobile menu toggle
  const btn = document.getElementById('mobile-menu-btn');
  const menu = document.getElementById('mobile-menu');

  btn?.addEventListener('click', () => {
    const expanded = btn.getAttribute('aria-expanded') === 'true';
    btn.setAttribute('aria-expanded', (!expanded).toString());
    menu?.classList.toggle('hidden');
  });
</script>

<style>
  .nav-link {
    @apply text-gray-700 hover:text-primary-600 transition-colors;
  }
  .mobile-nav-link {
    @apply block px-4 py-2 text-gray-700 hover:bg-gray-100 rounded;
  }
</style>
```

**Hero.astro** (Homepage Hero Section):
```astro
---
import { Image } from 'astro:assets';
import DownloadButtons from './DownloadButtons.astro';
import heroImage from '../public/images/screenshots/ios-home.png';
---

<section class="bg-gradient-to-b from-primary-50 to-white py-20">
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
    <div class="lg:grid lg:grid-cols-2 lg:gap-12 items-center">
      <!-- Text Content -->
      <div class="text-center lg:text-left">
        <h1 class="text-4xl sm:text-5xl lg:text-6xl font-bold text-gray-900 mb-6">
          Organize Your Family Photos by
          <span class="text-primary-600">Timeline</span>
        </h1>
        <p class="text-xl text-gray-600 mb-8 max-w-2xl mx-auto lg:mx-0">
          SmilePile helps you rediscover precious moments by organizing photos
          into beautiful timelines. Local-only storage keeps your memories private.
        </p>
        <DownloadButtons />
      </div>

      <!-- Hero Image -->
      <div class="mt-12 lg:mt-0">
        <Image
          src={heroImage}
          alt="SmilePile app showing photo timeline"
          width={375}
          height={812}
          format="webp"
          quality={90}
          class="mx-auto rounded-2xl shadow-2xl"
        />
      </div>
    </div>
  </div>
</section>
```

**FAQ.astro** (Accordion Component):
```astro
---
interface Props {
  question: string;
  answer: string;
  id: string;
}

const { question, answer, id } = Astro.props;
---

<details class="faq-item group" id={id}>
  <summary class="faq-question">
    <h3 class="text-lg font-semibold text-gray-900">{question}</h3>
    <svg
      class="faq-icon w-5 h-5 text-gray-500 transition-transform group-open:rotate-45"
      fill="none"
      stroke="currentColor"
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
    </svg>
  </summary>
  <div class="faq-answer">
    <p class="text-gray-600" set:html={answer}></p>
  </div>
</details>

<style>
  .faq-item {
    @apply border-b border-gray-200 py-4;
  }

  .faq-question {
    @apply flex justify-between items-center cursor-pointer list-none;
  }

  .faq-question::-webkit-details-marker {
    display: none;
  }

  .faq-answer {
    @apply mt-4 pl-0;
  }
</style>
```

### 3.3 Page Components

**Homepage (src/pages/index.astro):**
```astro
---
import BaseLayout from '@layouts/BaseLayout.astro';
import Hero from '@components/Hero.astro';
import Features from '@components/Features.astro';
import Screenshots from '@components/Screenshots.astro';
---

<BaseLayout
  title="SmilePile - Family Photo Timeline Organizer"
  description="Organize your family photos into beautiful timelines. Local-only storage keeps memories private. Available on iOS and Android."
>
  <!-- Query parameter redirects -->
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

  <Hero />
  <Features />
  <Screenshots />
</BaseLayout>
```

**Privacy Policy (src/pages/privacy.astro):**
```astro
---
import LegalLayout from '@layouts/LegalLayout.astro';
import { readFileSync } from 'fs';
import { marked } from 'marked';

// Load markdown content
const privacyContent = readFileSync('./src/content/privacy.md', 'utf-8');
const htmlContent = marked(privacyContent);
---

<LegalLayout
  title="Privacy Policy - SmilePile"
  description="Learn how SmilePile protects your family's privacy with local-only photo storage and no tracking."
  lastUpdated="October 2, 2025"
>
  <div set:html={htmlContent} />
</LegalLayout>
```

---

## 4. Query Parameter Routing Solution

### 4.1 Client-Side Redirect Approach

**Implementation (in src/pages/index.astro):**
```astro
---
// No server-side logic needed for static sites
---

<script>
  // Execute on page load
  if (typeof window !== 'undefined') {
    const params = new URLSearchParams(window.location.search);

    // Redirect /?privacy to /privacy
    if (params.has('privacy')) {
      window.location.replace('/privacy');
    }
    // Redirect /?tos to /terms
    else if (params.has('tos')) {
      window.location.replace('/terms');
    }
  }
</script>
```

**Pros:**
- Works on any static hosting
- No server configuration needed
- Simple implementation

**Cons:**
- Brief flash of homepage before redirect
- Not ideal for SEO (301 redirect preferred)
- Requires JavaScript enabled

### 4.2 Server-Side Redirect (Hosting Provider Config)

**Netlify (_redirects file):**
```
# In public/_redirects (copied to dist on build)
/?privacy    /privacy    301
/?tos        /terms      301
```

**Vercel (vercel.json):**
```json
{
  "redirects": [
    {
      "source": "/?privacy",
      "destination": "/privacy",
      "permanent": true
    },
    {
      "source": "/?tos",
      "destination": "/terms",
      "permanent": true
    }
  ]
}
```

**Cloudflare Pages (_redirects or wrangler.toml):**
```
/?privacy    /privacy    301
/?tos        /terms      301
```

**Recommendation: Use BOTH approaches**
- Server-side redirects as primary (when supported)
- Client-side redirects as fallback (works everywhere)

### 4.3 SEO Considerations

**Canonical URLs:**
```astro
<!-- In privacy.astro -->
<link rel="canonical" href="https://smilepile.app/privacy">

<!-- Also add meta tag to discourage indexing of query param version -->
<meta name="robots" content="noindex" if={Astro.url.search.includes('privacy')}>
```

**Sitemap Exclusions:**
```javascript
// In astro.config.mjs
sitemap({
  filter: (page) => {
    // Exclude pages with query parameters from sitemap
    return !page.includes('?');
  }
})
```

**Testing Checklist:**
- [ ] `https://smilepile.app/?privacy` → redirects to `/privacy` (200 status)
- [ ] `https://smilepile.app/?tos` → redirects to `/terms` (200 status)
- [ ] Redirects work in all browsers (Chrome, Safari, Firefox, Edge)
- [ ] Mobile app links work correctly
- [ ] No redirect loops
- [ ] Canonical URLs point to clean paths

---

## 5. Content Strategy

### 5.1 Legal Content Storage

**Approach: Markdown Files in src/content/**

**Why Markdown:**
- Easy to edit (non-technical stakeholders can update)
- Version control friendly (track changes over time)
- Portable (can migrate to CMS later)
- Supports rich formatting (headings, lists, tables)

**File Structure:**
```markdown
# src/content/privacy.md

# Privacy Policy

**Last Updated: October 2, 2025**

## Introduction

SmilePile is a family photo organizer developed by StackMap...

## Information We Collect

### Photos You Choose to Import
- When you select photos...

### Device Information
- Device model (e.g., iPhone 12)...

## What We DON'T Collect
- NO personal identifiable information...
```

**Rendering in Astro:**
```astro
---
import { marked } from 'marked';
import { readFileSync } from 'fs';

const content = readFileSync('./src/content/privacy.md', 'utf-8');
const html = marked(content);
---

<div class="prose prose-lg" set:html={html} />
```

**Alternative: Astro Content Collections** (more structured):
```typescript
// src/content/config.ts
import { defineCollection, z } from 'astro:content';

const legalCollection = defineCollection({
  schema: z.object({
    title: z.string(),
    lastUpdated: z.date(),
    category: z.enum(['privacy', 'terms', 'coppa']),
  }),
});

export const collections = { legal: legalCollection };
```

### 5.2 FAQ Data Structure

**JSON Format (src/content/faqs.json):**
```json
[
  {
    "id": "backup-photos",
    "category": "Getting Started",
    "question": "How do I backup my photos?",
    "answer": "SmilePile stores photos locally on your device. To backup, use your device's native backup solution: <strong>iOS:</strong> iCloud Photo Library or iTunes backup. <strong>Android:</strong> Google Photos or local backup to computer."
  },
  {
    "id": "cloud-sync",
    "category": "Privacy",
    "question": "Is my data synced to the cloud?",
    "answer": "No, all data is stored <strong>locally on your device</strong>. SmilePile does not sync to any cloud service, ensuring your photos remain private and under your control."
  },
  {
    "id": "delete-data",
    "category": "Privacy",
    "question": "How do I delete my data?",
    "answer": "Go to <strong>Settings → Clear All Data</strong> in the app, or simply uninstall SmilePile. This permanently removes all photos and data from the app (but does not delete photos from your device's photo library)."
  },
  {
    "id": "multiple-devices",
    "category": "Getting Started",
    "question": "Can I use SmilePile on multiple devices?",
    "answer": "Yes, you can install SmilePile on multiple devices (iPhone, iPad, Android phone/tablet). However, data is <strong>not synced between devices</strong> - each device maintains its own independent photo library."
  },
  {
    "id": "photos-not-showing",
    "category": "Troubleshooting",
    "question": "Why aren't my photos showing up?",
    "answer": "Check these steps: <br>1. Grant photo permissions: <strong>Settings → Privacy → Photos → SmilePile</strong><br>2. Restart the app<br>3. Verify photos are saved to device (not just cloud)<br>4. Check for app updates in App Store/Google Play"
  },
  {
    "id": "pricing",
    "category": "General",
    "question": "Is SmilePile free?",
    "answer": "Yes, SmilePile is <strong>completely free</strong> with no in-app purchases, subscriptions, or ads. We believe in keeping family memories accessible to everyone."
  },
  {
    "id": "age-requirement",
    "category": "Privacy",
    "question": "What's the minimum age to use SmilePile?",
    "answer": "SmilePile is designed for ages <strong>13 and up</strong>, or younger with parental supervision. We comply with COPPA (Children's Online Privacy Protection Act) and do not collect data from children. See our <a href='/privacy'>Privacy Policy</a> for details."
  }
]
```

**Rendering FAQs:**
```astro
---
import FAQ from '@components/FAQ.astro';
import faqs from '@content/faqs.json';

// Optional: filter by category
const supportFAQs = faqs.filter(faq => faq.category !== 'Privacy');
---

<section class="max-w-4xl mx-auto">
  <h2 class="text-3xl font-bold mb-8">Frequently Asked Questions</h2>
  <div class="space-y-0">
    {faqs.map(faq => (
      <FAQ
        id={faq.id}
        question={faq.question}
        answer={faq.answer}
      />
    ))}
  </div>
</section>
```

### 5.3 Asset Organization

**Images Directory Structure:**
```
public/images/
├── logo.svg                    # Primary logo (SVG)
├── logo-dark.svg               # Dark mode variant
├── logo-icon.svg               # Icon only (for small spaces)
├── og-image.png                # Open Graph share image (1200x630)
├── screenshots/
│   ├── ios-home.png           # iPhone home screen
│   ├── ios-timeline.png       # iPhone timeline view
│   ├── ios-search.png         # iPhone search feature
│   ├── android-home.png       # Android home screen
│   ├── android-timeline.png   # Android timeline view
│   └── android-search.png     # Android search feature
├── badges/
│   ├── app-store.svg          # Download on App Store badge
│   └── google-play.svg        # Get it on Google Play badge
└── icons/
    ├── feature-timeline.svg   # Feature icons
    ├── feature-privacy.svg
    └── feature-search.svg
```

**Asset Naming Conventions:**
- Lowercase with hyphens: `ios-home.png` (not `iOSHome.png`)
- Descriptive names: `app-store-badge.svg` (not `badge1.svg`)
- Include platform: `ios-screenshot.png`, `android-screenshot.png`
- Include size for multiple versions: `logo-192.png`, `logo-512.png`

**Image Optimization Workflow:**
1. Export from design tools at 2x resolution
2. Use ImageOptim, TinyPNG, or Squoosh to compress
3. Convert to WebP for modern browsers (Astro does this automatically)
4. Provide alt text in component props

---

## 6. Performance Optimization Plan

### 6.1 Image Optimization

**Formats:**
```astro
<Image
  src={screenshot}
  alt="SmilePile timeline view"
  widths={[375, 750, 1125]}  # 1x, 2x, 3x for mobile
  formats={['webp', 'png']}  # WebP with PNG fallback
  quality={80}               # Balance quality vs size
  loading="lazy"             # Lazy load below-the-fold images
/>
```

**Responsive Images:**
```astro
<picture>
  <source
    srcset="/images/hero-mobile.webp 1x, /images/hero-mobile@2x.webp 2x"
    media="(max-width: 768px)"
    type="image/webp"
  />
  <source
    srcset="/images/hero-desktop.webp 1x, /images/hero-desktop@2x.webp 2x"
    media="(min-width: 769px)"
    type="image/webp"
  />
  <img
    src="/images/hero-desktop.png"
    alt="SmilePile app interface"
    loading="lazy"
  />
</picture>
```

**Lazy Loading Strategy:**
- **Above-the-fold images**: `loading="eager"` or no attribute (load immediately)
- **Below-the-fold images**: `loading="lazy"` (native lazy loading)
- **Hero LCP image**: Preload in `<head>`:
  ```html
  <link rel="preload" as="image" href="/images/hero.webp">
  ```

### 6.2 CSS/JS Minification

**Astro Handles Automatically:**
- CSS minification via `astro build`
- JS minification via Vite/Rollup
- Tree-shaking removes unused code
- Code splitting for better caching

**Manual Optimizations:**
- **Critical CSS inlining**: Astro's `inlineStylesheets: 'auto'`
- **Unused CSS removal**: Tailwind's purge automatically removes unused classes
- **Font optimization**: Use system fonts (no web font loading)

**CSS Strategy:**
```css
/* Inline in <head> for above-the-fold */
<style is:inline>
  .hero { /* critical styles */ }
</style>

/* Load asynchronously for below-the-fold */
<link rel="stylesheet" href="/styles/non-critical.css" media="print" onload="this.media='all'">
```

### 6.3 Core Web Vitals Targets

**Largest Contentful Paint (LCP): < 2.5s**
- Optimize hero image (main LCP element)
- Preload hero image: `<link rel="preload" as="image">`
- Use WebP format with appropriate quality
- Ensure fast server response time (CDN helps)

**First Input Delay (FID): < 100ms**
- Minimize JavaScript execution
- Use `defer` or `async` for non-critical scripts
- Avoid long-running JS tasks
- Keep interactive elements lightweight

**Cumulative Layout Shift (CLS): < 0.1**
- Reserve space for images with `width` and `height` attributes
- Avoid inserting content above existing content
- Use `font-display: swap` if using web fonts
- Set explicit dimensions for all media

**Performance Monitoring:**
```javascript
// Track Core Web Vitals (optional)
import { getCLS, getFID, getLCP } from 'web-vitals';

getCLS(console.log);
getFID(console.log);
getLCP(console.log);
```

---

## 7. Accessibility Implementation

### 7.1 Semantic HTML Patterns

**Proper Heading Hierarchy:**
```html
<h1>SmilePile - Family Photo Timeline Organizer</h1>
  <section>
    <h2>Features</h2>
      <article>
        <h3>Timeline View</h3>
      </article>
  </section>
```

**Landmarks and ARIA:**
```html
<header role="banner">
  <nav aria-label="Main navigation">...</nav>
</header>

<main role="main">
  <article aria-labelledby="privacy-heading">
    <h1 id="privacy-heading">Privacy Policy</h1>
  </article>
</main>

<footer role="contentinfo">
  <nav aria-label="Legal">...</nav>
</footer>
```

**Skip Navigation:**
```html
<a href="#main-content" class="skip-link">
  Skip to main content
</a>

<main id="main-content">
  <!-- Page content -->
</main>

<style>
  .skip-link {
    position: absolute;
    top: -40px;
    left: 0;
    background: #000;
    color: #fff;
    padding: 8px;
    z-index: 100;
  }
  .skip-link:focus {
    top: 0;
  }
</style>
```

### 7.2 ARIA Labels Strategy

**Interactive Elements:**
```html
<!-- Buttons -->
<button
  aria-label="Open mobile menu"
  aria-expanded="false"
  aria-controls="mobile-nav"
>
  <svg aria-hidden="true">...</svg>
</button>

<!-- Links -->
<a
  href="https://apps.apple.com/..."
  aria-label="Download SmilePile on the App Store"
>
  <img src="/badges/app-store.svg" alt="" aria-hidden="true">
</a>

<!-- Form inputs -->
<label for="email">Email Address</label>
<input
  id="email"
  type="email"
  aria-required="true"
  aria-describedby="email-help"
>
<span id="email-help">We'll never share your email</span>
```

**Accordion/Details:**
```html
<details>
  <summary aria-expanded="false">
    How do I backup my photos?
    <svg aria-hidden="true">...</svg>
  </summary>
  <div role="region">
    <p>Answer content...</p>
  </div>
</details>
```

### 7.3 Keyboard Navigation Plan

**Focus Management:**
```css
/* Visible focus indicator */
:focus-visible {
  outline: 3px solid #0ea5e9;
  outline-offset: 2px;
}

/* Remove focus for mouse users (but keep for keyboard) */
:focus:not(:focus-visible) {
  outline: none;
}
```

**Tab Order:**
```html
<!-- Ensure logical tab order matches visual order -->
<nav>
  <a href="/" tabindex="0">Home</a>
  <a href="/features" tabindex="0">Features</a>
  <a href="/support" tabindex="0">Support</a>
</nav>

<!-- Modal: trap focus inside when open -->
<dialog aria-modal="true">
  <button aria-label="Close modal" tabindex="0">×</button>
  <div tabindex="0">Modal content</div>
</dialog>
```

**Keyboard Shortcuts:**
- **Tab**: Navigate forward
- **Shift+Tab**: Navigate backward
- **Enter/Space**: Activate buttons/links
- **Escape**: Close modals/menus
- **Arrow keys**: Navigate carousels (if implemented)

### 7.4 Screen Reader Testing Approach

**Testing Tools:**
- **VoiceOver** (macOS/iOS): Built-in screen reader
- **NVDA** (Windows): Free screen reader
- **TalkBack** (Android): Built-in screen reader
- **JAWS** (Windows): Commercial screen reader (optional)

**Testing Checklist:**
- [ ] Page title announced on load
- [ ] Headings navigable with screen reader shortcuts (H key)
- [ ] Links clearly identified (text + context)
- [ ] Images have descriptive alt text (or aria-hidden if decorative)
- [ ] Form labels associated with inputs
- [ ] Error messages announced
- [ ] Dynamic content changes announced (aria-live regions)
- [ ] No keyboard traps (can navigate entire page)

**VoiceOver Testing (macOS):**
```bash
# Enable VoiceOver: Cmd+F5
# Navigate by headings: VO+Cmd+H
# Navigate by links: VO+Cmd+L
# Read from current position: VO+A
```

**Automated Testing:**
```bash
# Install axe-core
npm install -D @axe-core/cli

# Run accessibility audit
npx axe http://localhost:4321 --exit
```

---

## 8. SEO Implementation

### 8.1 Meta Tags Strategy (Per-Page Customization)

**Homepage:**
```astro
---
const seo = {
  title: "SmilePile - Family Photo Timeline Organizer",
  description: "Organize your family photos into beautiful timelines. Local-only storage keeps memories private. Available on iOS and Android.",
  keywords: "family photos, photo organizer, timeline, iOS app, Android app, privacy, local storage",
  canonical: "https://smilepile.app/",
  ogImage: "/og-image-home.png",
};
---
```

**Privacy Page:**
```astro
---
const seo = {
  title: "Privacy Policy - SmilePile",
  description: "Learn how SmilePile protects your family's privacy with local-only photo storage and no tracking. COPPA-compliant.",
  canonical: "https://smilepile.app/privacy",
  ogImage: "/og-image-privacy.png",
};
---
```

**Terms Page:**
```astro
---
const seo = {
  title: "Terms of Service - SmilePile",
  description: "Read SmilePile's terms of service. Clear, fair terms for using our family photo organizer app.",
  canonical: "https://smilepile.app/terms",
  ogImage: "/og-image-legal.png",
};
---
```

**Support Page:**
```astro
---
const seo = {
  title: "Support & Help - SmilePile",
  description: "Get help with SmilePile. Browse FAQs, contact support, and find solutions to common photo organization questions.",
  canonical: "https://smilepile.app/support",
  ogImage: "/og-image-support.png",
};
---
```

### 8.2 Open Graph and Twitter Cards

**Complete Meta Tag Set:**
```html
<!-- Primary Meta Tags -->
<title>SmilePile - Family Photo Timeline Organizer</title>
<meta name="title" content="SmilePile - Family Photo Timeline Organizer">
<meta name="description" content="Organize your family photos into beautiful timelines. Local-only storage keeps memories private.">
<meta name="keywords" content="family photos, photo organizer, timeline, privacy, iOS, Android">

<!-- Open Graph / Facebook -->
<meta property="og:type" content="website">
<meta property="og:url" content="https://smilepile.app/">
<meta property="og:title" content="SmilePile - Family Photo Timeline Organizer">
<meta property="og:description" content="Organize your family photos into beautiful timelines. Local-only storage keeps memories private.">
<meta property="og:image" content="https://smilepile.app/og-image.png">
<meta property="og:image:width" content="1200">
<meta property="og:image:height" content="630">
<meta property="og:site_name" content="SmilePile">

<!-- Twitter -->
<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:url" content="https://smilepile.app/">
<meta name="twitter:title" content="SmilePile - Family Photo Timeline Organizer">
<meta name="twitter:description" content="Organize your family photos into beautiful timelines. Local-only storage keeps memories private.">
<meta name="twitter:image" content="https://smilepile.app/og-image.png">

<!-- Additional SEO -->
<link rel="canonical" href="https://smilepile.app/">
<meta name="robots" content="index, follow">
<meta name="language" content="English">
<meta name="author" content="StackMap">
```

**OG Image Specs:**
- **Size**: 1200 x 630 pixels
- **Format**: PNG or JPEG
- **File size**: < 100KB
- **Content**: Logo + tagline + app screenshot
- **Text**: Large, readable (avoid small text)

### 8.3 Sitemap.xml Generation

**Auto-generated by @astrojs/sitemap:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url>
    <loc>https://smilepile.app/</loc>
    <lastmod>2025-10-02</lastmod>
    <changefreq>monthly</changefreq>
    <priority>1.0</priority>
  </url>
  <url>
    <loc>https://smilepile.app/privacy</loc>
    <lastmod>2025-10-02</lastmod>
    <changefreq>monthly</changefreq>
    <priority>0.8</priority>
  </url>
  <url>
    <loc>https://smilepile.app/terms</loc>
    <lastmod>2025-10-02</lastmod>
    <changefreq>monthly</changefreq>
    <priority>0.8</priority>
  </url>
  <url>
    <loc>https://smilepile.app/support</loc>
    <lastmod>2025-10-02</lastmod>
    <changefreq>weekly</changefreq>
    <priority>0.9</priority>
  </url>
</urlset>
```

**Custom Sitemap Configuration:**
```javascript
// astro.config.mjs
sitemap({
  filter: (page) => !page.includes('/404') && !page.includes('?'),
  customPages: [
    'https://smilepile.app/',
    'https://smilepile.app/privacy',
    'https://smilepile.app/terms',
    'https://smilepile.app/support',
  ],
  serialize(item) {
    // Customize priority based on page
    if (item.url === 'https://smilepile.app/') {
      item.priority = 1.0;
      item.changefreq = 'monthly';
    } else if (item.url.includes('/support')) {
      item.priority = 0.9;
      item.changefreq = 'weekly';
    } else {
      item.priority = 0.8;
      item.changefreq = 'monthly';
    }
    return item;
  },
})
```

### 8.4 Robots.txt Configuration

**public/robots.txt:**
```txt
# Allow all crawlers
User-agent: *
Allow: /

# Disallow admin or private pages (if any)
Disallow: /admin/
Disallow: /private/

# Sitemap location
Sitemap: https://smilepile.app/sitemap.xml

# Crawl-delay for specific bots (optional)
User-agent: GPTBot
Crawl-delay: 10
```

**Advanced: Block AI Scrapers (Optional):**
```txt
# Block AI training bots (if desired)
User-agent: CCBot
Disallow: /

User-agent: ChatGPT-User
Disallow: /

User-agent: Google-Extended
Disallow: /
```

---

## 9. Development Workflow

### 9.1 Local Development Setup

**Prerequisites:**
- Node.js 18+ (LTS recommended)
- npm 9+ or yarn 1.22+
- Git
- Code editor (VS Code recommended)

**Installation Steps:**
```bash
# Clone repository (or create new project)
git clone https://github.com/stackmap/smilepile-landing.git
cd smilepile-landing

# Install dependencies
npm install

# Start development server
npm run dev

# Open browser to http://localhost:4321
```

**Development Commands:**
```bash
npm run dev        # Start dev server (hot reload)
npm run build      # Build production files
npm run preview    # Preview production build locally
npm run validate   # Run build validation checks
npm run lighthouse # Run Lighthouse performance audit
```

**VS Code Extensions (Recommended):**
- Astro (astro-build.astro-vscode)
- Tailwind CSS IntelliSense (bradlc.vscode-tailwindcss)
- ESLint (dbaeumer.vscode-eslint)
- Prettier (esbenp.prettier-vscode)

### 9.2 Build Process

**Production Build Flow:**
```
Source Files (src/)
    ↓
Astro Compilation
    ↓
- HTML generation from .astro files
- CSS bundling and minification
- JS bundling and tree-shaking
- Image optimization (WebP conversion)
- Asset hashing for cache-busting
    ↓
Output (dist/)
    ↓
Static Files Ready for Deployment
```

**Build Command:**
```bash
npm run build

# Output:
# 12:34:56 [build] output target: static
# 12:34:56 [build] Collecting build info...
# 12:34:57 [build] Completed in 842ms.
# 12:34:57 [build] Building static entrypoints...
# 12:34:58 [build] ✓ Completed in 1.2s.
#
# dist/
# ├── index.html (12KB)
# ├── privacy/index.html (18KB)
# ├── terms/index.html (15KB)
# ├── support/index.html (14KB)
# ├── _astro/
# │   ├── main.a3f2b1.css (24KB)
# │   └── redirect.b4e9c2.js (2KB)
# └── images/ (optimized)
```

**Build Validation:**
```bash
# Run custom validation script
npm run validate

# Checks:
# ✅ All required HTML files exist
# ✅ No broken internal links
# ✅ Images optimized (< 500KB each)
# ✅ Total page weight within budget
# ✅ Sitemap.xml valid
# ✅ Robots.txt present
```

### 9.3 Preview/Testing Process

**Local Preview:**
```bash
npm run preview

# Serves production build at http://localhost:4321
# Mirrors production environment
```

**Performance Testing:**
```bash
# Lighthouse audit (all pages)
npm run lighthouse

# Output:
# ✅ Performance: 96
# ✅ Accessibility: 100
# ✅ Best Practices: 95
# ✅ SEO: 100
```

**Cross-Browser Testing:**
- Chrome DevTools Device Mode (mobile simulation)
- Safari Technology Preview (iOS simulation)
- Firefox Developer Edition
- BrowserStack (real device testing - optional)

**Testing Checklist:**
- [ ] All pages render correctly
- [ ] Navigation works (header, footer links)
- [ ] Query parameter redirects functional (/?privacy, /?tos)
- [ ] Images load and are optimized
- [ ] Forms work (if any)
- [ ] Mobile responsive (all breakpoints)
- [ ] No console errors
- [ ] Lighthouse scores ≥ 90

### 9.4 Deployment Preparation

**Pre-Deployment Checklist:**
```markdown
## Content
- [ ] All legal text reviewed and accurate
- [ ] Contact email verified (support@stackmap.app)
- [ ] Last updated dates current
- [ ] No placeholder text ("Lorem ipsum")
- [ ] All links functional

## Technical
- [ ] Production build successful (npm run build)
- [ ] Validation passed (npm run validate)
- [ ] Lighthouse scores meet targets
- [ ] Cross-browser tested
- [ ] Mobile responsive verified
- [ ] Accessibility audit passed

## SEO
- [ ] Meta tags unique per page
- [ ] Open Graph images set
- [ ] Sitemap.xml generated
- [ ] Robots.txt configured
- [ ] Canonical URLs set

## Assets
- [ ] Images optimized
- [ ] Favicons present (all sizes)
- [ ] App store badges correct
- [ ] Logo files included

## Security
- [ ] HTTPS configured (hosting provider)
- [ ] Security headers set (CSP, X-Frame-Options)
- [ ] No sensitive data in source
```

**Deployment Package Contents:**
```
/dist/                      # Upload this entire folder
├── index.html
├── privacy/index.html
├── terms/index.html
├── support/index.html
├── 404.html
├── _astro/                # Hashed assets
├── images/                # Optimized images
├── favicon.ico
├── sitemap.xml
├── robots.txt
├── _redirects             # Netlify redirects
└── vercel.json            # Vercel config (if using)
```

---

## 10. Implementation Timeline

### 10.1 Story Breakdown by Development Phase

**Total Estimated Time: 24-32 hours (3-5 business days)**

#### Phase 1: Project Setup (2-3 hours)
**Tasks:**
- Initialize Astro project with TypeScript
- Install dependencies (Tailwind, sitemap, compress)
- Configure astro.config.mjs, tailwind.config.cjs
- Set up directory structure (layouts, components, pages, content)
- Create base layout and shared components (Header, Footer)
- Configure Git repository and .gitignore

**Deliverable:** Working Astro dev environment

---

#### Phase 2: Core Pages Implementation (8-10 hours)

**STORY-9.1: Homepage (4-5 hours)**
- Create Hero component (app description, download buttons)
- Create Features component (3-5 key features)
- Create Screenshots component (iOS/Android screenshots)
- Build index.astro page
- Add query parameter redirect logic (/?privacy, /?tos)
- Style with Tailwind CSS
- Test mobile responsiveness

**STORY-9.2: Privacy Policy Page (2-2.5 hours)**
- Write privacy policy content (markdown)
- Create LegalLayout component
- Create TableOfContents component
- Build privacy.astro page
- Ensure COPPA compliance sections included
- Test readability and formatting

**STORY-9.3: Terms of Service Page (1.5-2 hours)**
- Write terms of service content (markdown)
- Build terms.astro page (reuse LegalLayout)
- Ensure all required ToS sections included
- Test readability and formatting

**STORY-9.4: Support Page (2-2.5 hours)**
- Create FAQ component (accordion/details)
- Create ContactCard component
- Write FAQ content (7-10 questions)
- Build support.astro page
- Add troubleshooting tips section
- Test accordion functionality

---

#### Phase 3: Assets & Content (4-5 hours)

**Tasks:**
- Create or source SmilePile logo (SVG)
- Prepare app screenshots (iOS and Android)
- Optimize all images (TinyPNG, ImageOptim)
- Download official app store badges
- Create Open Graph images (1200x630px)
- Generate favicon set (all sizes)
- Add site.webmanifest
- Create robots.txt
- Review all legal content for accuracy

**Deliverable:** All assets optimized and in place

---

#### Phase 4: SEO & Accessibility (3-4 hours)

**Tasks:**
- Configure sitemap integration
- Add unique meta tags to each page
- Set up Open Graph and Twitter Cards
- Add canonical URLs
- Implement skip navigation links
- Add ARIA labels to interactive elements
- Test keyboard navigation
- Run automated accessibility audit (axe)
- Fix any WCAG violations
- Test with VoiceOver/TalkBack

**Deliverable:** SEO-optimized, accessible site

---

#### Phase 5: Testing & Optimization (5-6 hours)

**Tasks:**
- Run production build (npm run build)
- Test local preview (npm run preview)
- Run Lighthouse audit (all pages)
- Optimize performance issues (if any)
- Test query parameter redirects
- Cross-browser testing (Chrome, Safari, Firefox, Edge)
- Mobile device testing (iOS and Android)
- Test from mobile app links
- Fix any bugs or issues
- Final content review

**Deliverable:** Production-ready site passing all quality gates

---

#### Phase 6: Deployment (2-3 hours)

**Tasks:**
- Set up hosting provider (Vercel/Netlify/Cloudflare)
- Configure custom domain (smilepile.app)
- Set up redirects (_redirects or vercel.json)
- Configure security headers
- Deploy to production
- Verify HTTPS working
- Test all pages live
- Submit sitemap to Google Search Console
- Monitor initial performance
- Document deployment process

**Deliverable:** Live site at https://smilepile.app

---

### 10.2 Hour Estimates per Phase

| Phase | Story | Tasks | Hours | Dependencies |
|-------|-------|-------|-------|--------------|
| 1 | Setup | Project initialization, config | 2-3 | None |
| 2 | STORY-9.1 | Homepage implementation | 4-5 | Phase 1 |
| 2 | STORY-9.2 | Privacy policy page | 2-2.5 | Phase 1 |
| 2 | STORY-9.3 | Terms of service page | 1.5-2 | Phase 1 |
| 2 | STORY-9.4 | Support page | 2-2.5 | Phase 1 |
| 3 | Assets | Image optimization, content | 4-5 | Phase 2 |
| 4 | SEO | Meta tags, sitemap, a11y | 3-4 | Phase 2, 3 |
| 5 | Testing | QA, performance, cross-browser | 5-6 | Phase 2, 3, 4 |
| 6 | STORY-9.5 | Deployment and documentation | 2-3 | Phase 5 |
| **Total** | | | **24-32 hours** | |

### 10.3 Dependencies and Parallel Work

**Sequential Dependencies:**
```
Phase 1 (Setup)
    ↓
Phase 2 (Core Pages) → Can work on all 4 pages in parallel
    ↓
Phase 3 (Assets) → Can work in parallel with Phase 2 end
    ↓
Phase 4 (SEO) → Depends on Phase 2 complete
    ↓
Phase 5 (Testing) → Depends on all previous phases
    ↓
Phase 6 (Deployment) → Depends on Phase 5 passing
```

**Parallel Work Opportunities:**
- **Design + Development**: Work on content (markdown) while building components
- **Assets + Pages**: Optimize images while building page layouts
- **SEO + Testing**: Add meta tags while testing functionality

**Critical Path:**
1. Setup → Homepage → Testing → Deployment (minimum viable path)
2. Legal pages can be developed in parallel
3. Assets can be prepared independently

### 10.4 Timeline: 3-5 Day Completion

**Option 1: Fast Track (3 days, ~10 hours/day)**
- **Day 1**: Setup + Homepage + Privacy page (9-10 hours)
- **Day 2**: Terms + Support + Assets + SEO (10-12 hours)
- **Day 3**: Testing + Deployment (7-9 hours)

**Option 2: Standard Track (4 days, ~7 hours/day)**
- **Day 1**: Setup + Homepage (6-8 hours)
- **Day 2**: Privacy + Terms + Support pages (6-7 hours)
- **Day 3**: Assets + SEO + Accessibility (7-9 hours)
- **Day 4**: Testing + Deployment (7-9 hours)

**Option 3: Comfortable Track (5 days, ~5-6 hours/day)**
- **Day 1**: Setup + Homepage (6-8 hours)
- **Day 2**: Privacy page + Terms page (3.5-4.5 hours)
- **Day 3**: Support page + Assets (6-7.5 hours)
- **Day 4**: SEO + Accessibility + Testing (8-10 hours)
- **Day 5**: Final testing + Deployment + Documentation (2-3 hours)

**Recommended: Standard Track (4 days)** - balances speed with quality.

---

## 11. Risk Assessment & Mitigation

### 11.1 Technical Risks

**Risk: Query Parameter Redirects Fail in Production**
- **Probability**: Medium
- **Impact**: High (broken mobile app links)
- **Mitigation**:
  - Implement both client-side (JS) and server-side (hosting config) redirects
  - Test extensively in staging environment
  - Test from actual mobile app before production deployment
  - Document both approaches in deployment guide

**Risk: Performance Targets Not Met (Lighthouse < 90)**
- **Probability**: Low
- **Impact**: Medium
- **Mitigation**:
  - Use Astro's built-in optimizations from start
  - Optimize images aggressively (WebP, compression)
  - Inline critical CSS, defer non-critical
  - Run Lighthouse continuously during development
  - Set up performance budget alerts

**Risk: Accessibility Violations (WCAG failures)**
- **Probability**: Low
- **Impact**: Medium (legal risk, poor UX)
- **Mitigation**:
  - Use semantic HTML from the start
  - Leverage Tailwind's accessibility utilities
  - Run automated tests (axe) frequently
  - Manual testing with screen readers (VoiceOver)
  - Keyboard navigation testing on every interactive element

**Risk: Build Process Failures**
- **Probability**: Low
- **Impact**: High (cannot deploy)
- **Mitigation**:
  - Lock dependency versions in package.json
  - Use Node.js LTS version
  - Test build process early and often
  - Document exact build environment
  - Create build validation script

### 11.2 Content Risks

**Risk: Legal Content Insufficient (COPPA non-compliance)**
- **Probability**: Medium
- **Impact**: High (FTC fines, app store rejection)
- **Mitigation**:
  - Use vetted COPPA-compliant templates (Termly, TermsFeed)
  - Follow FTC COPPA guidelines explicitly
  - Include all required 2025 amendment disclosures
  - Consider professional legal review before production
  - Document compliance decisions

**Risk: Broken Links from Mobile Apps**
- **Probability**: Low
- **Impact**: High (poor user experience)
- **Mitigation**:
  - Test exact URLs mobile apps link to (/?privacy, /?tos)
  - Create comprehensive link testing checklist
  - Test from actual iOS and Android devices
  - Monitor 404 errors post-launch

**Risk: Outdated Legal Content**
- **Probability**: Medium (over time)
- **Impact**: Medium
- **Mitigation**:
  - Include "Last Updated" date prominently
  - Set calendar reminders for annual review
  - Version control all policy changes
  - Document update process in MAINTENANCE.md

### 11.3 Deployment Risks

**Risk: Hosting Provider Downtime**
- **Probability**: Low
- **Impact**: High (site inaccessible)
- **Mitigation**:
  - Choose reliable provider (Vercel, Netlify, Cloudflare)
  - Keep build hosting-agnostic (can switch providers)
  - Set up uptime monitoring (UptimeRobot)
  - Have backup deployment plan documented

**Risk: DNS/SSL Configuration Issues**
- **Probability**: Low
- **Impact**: High (HTTPS broken, site unreachable)
- **Mitigation**:
  - Use hosting provider's automatic SSL (Let's Encrypt)
  - Test HTTPS immediately after deployment
  - Verify DNS propagation before announcing launch
  - Document DNS configuration steps

**Risk: Caching Issues (Users See Stale Content)**
- **Probability**: Medium
- **Impact**: Low
- **Mitigation**:
  - Use Astro's automatic asset hashing (cache busting)
  - Set appropriate cache headers via hosting provider
  - Document cache clearing process
  - Test cache behavior in staging

### 11.4 Timeline Risks

**Risk: Underestimated Complexity (Scope Creep)**
- **Probability**: Medium
- **Impact**: Medium (missed deadline)
- **Mitigation**:
  - Stick to MVP scope (no extra features)
  - Track hours against estimates daily
  - Identify blockers early
  - Have fallback plan (reduce "should have" items)

**Risk: Dependency on External Assets**
- **Probability**: Low
- **Impact**: Low
- **Mitigation**:
  - Use placeholder images if real screenshots delayed
  - Source official app store badges from Apple/Google
  - Prepare asset checklist early
  - Identify asset sources before starting development

---

## 12. Success Criteria & Validation

### 12.1 Functional Requirements

**Must Pass (Blockers):**
- [ ] Homepage accessible at `https://smilepile.app/`
- [ ] Privacy page accessible at `/privacy` and `/?privacy` redirects
- [ ] Terms page accessible at `/terms` and `/?tos` redirects
- [ ] Support page accessible at `/support`
- [ ] All navigation links functional (header, footer)
- [ ] App store badges link to correct app store listings
- [ ] Contact email link opens email client with correct address
- [ ] FAQ accordions expand/collapse correctly
- [ ] Mobile responsive on all pages (iPhone SE to iPad Pro)
- [ ] No console errors on any page
- [ ] No broken links (internal or external)

### 12.2 Performance Requirements

**Must Pass (Lighthouse Targets):**
- [ ] **Performance**: ≥ 90 (mobile), ≥ 95 (desktop)
- [ ] **Accessibility**: = 100
- [ ] **Best Practices**: ≥ 90
- [ ] **SEO**: = 100

**Core Web Vitals:**
- [ ] **LCP** (Largest Contentful Paint): < 2.5s
- [ ] **FID** (First Input Delay): < 100ms
- [ ] **CLS** (Cumulative Layout Shift): < 0.1

**Page Weight:**
- [ ] Homepage: < 500KB initial load
- [ ] Privacy page: < 150KB
- [ ] Terms page: < 120KB
- [ ] Support page: < 200KB

### 12.3 Accessibility Requirements

**Must Pass (WCAG 2.1 AA):**
- [ ] Semantic HTML structure (proper heading hierarchy)
- [ ] Color contrast ≥ 4.5:1 for body text
- [ ] Color contrast ≥ 3:1 for large text
- [ ] All images have alt text (or aria-hidden if decorative)
- [ ] Keyboard navigable (tab through all interactive elements)
- [ ] Focus indicators visible on all focusable elements
- [ ] Skip navigation link present and functional
- [ ] Screen reader tested (VoiceOver/TalkBack)
- [ ] No accessibility errors (axe DevTools)
- [ ] Forms have proper labels (if any)
- [ ] ARIA attributes used correctly

### 12.4 Content Requirements

**Must Pass (Legal & Accuracy):**
- [ ] Privacy policy includes all COPPA-required disclosures
- [ ] Privacy policy "Last Updated" date accurate
- [ ] Terms of service includes all standard sections
- [ ] Terms of service "Last Updated" date accurate
- [ ] Support page has working email link (support@stackmap.app)
- [ ] FAQ answers are accurate and helpful (7+ questions)
- [ ] No placeholder text ("Lorem ipsum", "Coming soon")
- [ ] No typos or grammatical errors
- [ ] All legal cross-references correct (Privacy ↔ Terms)

### 12.5 SEO Requirements

**Must Pass (Search Optimization):**
- [ ] Unique `<title>` tag on every page
- [ ] Unique meta description on every page
- [ ] Open Graph tags set (og:title, og:description, og:image)
- [ ] Twitter Card tags set
- [ ] Canonical URLs set on all pages
- [ ] Sitemap.xml generated and accessible at /sitemap.xml
- [ ] Robots.txt present and correct
- [ ] Structured data (optional but recommended)
- [ ] All images have descriptive file names
- [ ] Internal linking structure logical

### 12.6 Deployment Validation

**Must Pass (Production Checklist):**
- [ ] HTTPS working correctly (SSL certificate valid)
- [ ] Custom domain (smilepile.app) configured
- [ ] All pages load in production
- [ ] Query parameter redirects work in production
- [ ] 404 page displays correctly
- [ ] Sitemap submitted to Google Search Console
- [ ] Robots.txt accessible and correct
- [ ] Security headers configured (X-Frame-Options, CSP)
- [ ] Mobile app links tested from actual devices
- [ ] Cross-browser tested (Chrome, Safari, Firefox, Edge)

---

## 13. Deployment Instructions

### 13.1 Vercel Deployment (Recommended)

**Prerequisites:**
- Vercel account (free tier sufficient)
- Git repository pushed to GitHub

**Deployment Steps:**
```bash
# Install Vercel CLI (optional)
npm i -g vercel

# Deploy from command line
vercel

# Or deploy via Vercel dashboard:
# 1. Go to vercel.com/new
# 2. Import GitHub repository
# 3. Framework Preset: Astro
# 4. Build Command: npm run build
# 5. Output Directory: dist
# 6. Deploy
```

**Custom Domain Setup:**
```bash
# Add custom domain in Vercel dashboard
# 1. Go to Project → Settings → Domains
# 2. Add domain: smilepile.app
# 3. Configure DNS (Vercel provides instructions)
# 4. Wait for DNS propagation (5-60 minutes)
# 5. Verify HTTPS working
```

**Environment Variables:**
- None required for static site

**Vercel Configuration (vercel.json):**
```json
{
  "buildCommand": "npm run build",
  "outputDirectory": "dist",
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

### 13.2 Netlify Deployment (Alternative)

**Deployment Steps:**
```bash
# Install Netlify CLI (optional)
npm i -g netlify-cli

# Deploy from command line
netlify deploy --prod

# Or deploy via Netlify dashboard:
# 1. Go to app.netlify.com/start
# 2. Connect to Git repository
# 3. Build command: npm run build
# 4. Publish directory: dist
# 5. Deploy site
```

**Custom Domain Setup:**
```bash
# Add custom domain in Netlify dashboard
# 1. Go to Site Settings → Domain management
# 2. Add custom domain: smilepile.app
# 3. Verify DNS configuration
# 4. Enable HTTPS (automatic with Let's Encrypt)
```

**Netlify Configuration (netlify.toml):**
```toml
[build]
  command = "npm run build"
  publish = "dist"

[[redirects]]
  from = "/?privacy"
  to = "/privacy"
  status = 301

[[redirects]]
  from = "/?tos"
  to = "/terms"
  status = 301

[[headers]]
  for = "/*"
  [headers.values]
    X-Frame-Options = "DENY"
    X-Content-Type-Options = "nosniff"
    Referrer-Policy = "strict-origin-when-cross-origin"
```

### 13.3 Cloudflare Pages (Alternative)

**Deployment Steps:**
```bash
# 1. Go to pages.cloudflare.com
# 2. Create a project → Connect to Git
# 3. Select repository
# 4. Build settings:
#    - Framework preset: Astro
#    - Build command: npm run build
#    - Build output directory: dist
# 5. Deploy
```

**Custom Domain:**
- Cloudflare automatically handles DNS if domain on Cloudflare
- Add custom domain in Pages dashboard

**_redirects File (in public/ folder):**
```
/?privacy    /privacy    301
/?tos        /terms      301
```

### 13.4 Post-Deployment Checklist

**Immediate Verification (within 5 minutes):**
- [ ] Visit https://smilepile.app → homepage loads
- [ ] Visit https://smilepile.app/privacy → privacy page loads
- [ ] Visit https://smilepile.app/terms → terms page loads
- [ ] Visit https://smilepile.app/support → support page loads
- [ ] Visit https://smilepile.app/?privacy → redirects to /privacy
- [ ] Visit https://smilepile.app/?tos → redirects to /terms
- [ ] Check HTTPS certificate valid (green padlock)
- [ ] Test from mobile browser (iOS Safari, Android Chrome)

**Within 1 hour:**
- [ ] Run Lighthouse audit on production URL
- [ ] Test all links from mobile apps (iOS and Android)
- [ ] Check DNS propagation globally (whatsmydns.net)
- [ ] Verify sitemap.xml accessible: https://smilepile.app/sitemap.xml
- [ ] Verify robots.txt accessible: https://smilepile.app/robots.txt

**Within 24 hours:**
- [ ] Submit sitemap to Google Search Console
- [ ] Submit sitemap to Bing Webmaster Tools
- [ ] Test Open Graph tags (Facebook Debugger)
- [ ] Test Twitter Cards (Twitter Card Validator)
- [ ] Monitor for 404 errors (hosting provider analytics)
- [ ] Set up uptime monitoring (UptimeRobot, Pingdom)

**Within 1 week:**
- [ ] Monitor Core Web Vitals (Search Console)
- [ ] Check search engine indexing (site:smilepile.app)
- [ ] Review any support emails about website issues
- [ ] Track any mobile app complaints about broken links

---

## 14. Maintenance & Future Enhancements

### 14.1 Ongoing Maintenance

**Monthly Tasks:**
- Review and update FAQ based on support emails
- Check for broken links (automated or manual)
- Monitor performance metrics (Lighthouse, Core Web Vitals)
- Review analytics (if enabled)

**Quarterly Tasks:**
- Review legal content for accuracy (Privacy, Terms)
- Update screenshots if app UI changes
- Check for Astro framework updates
- Review security headers and best practices

**Annual Tasks:**
- Update "Last Updated" date on Privacy/Terms (if changed)
- Comprehensive content audit
- Accessibility re-audit
- Performance optimization review

### 14.2 Future Enhancements (Post-MVP)

**Phase 2 Additions (Next 1-3 months):**
- Blog section for product updates
- Email newsletter signup
- Customer testimonials/reviews
- Feature comparison table
- Video tutorials for common tasks

**Phase 3 Additions (3-6 months):**
- Multi-language support (Spanish, French)
- Advanced FAQ search functionality
- Live chat widget (if support volume justifies)
- Interactive demo/tour
- Press kit / media resources page

**Technical Improvements:**
- CI/CD pipeline (GitHub Actions)
- Automated Lighthouse checks on PRs
- Visual regression testing (Percy)
- A/B testing infrastructure
- Content Management System (CMS) integration

---

## 15. Documentation Deliverables

### 15.1 README.md (Project Root)

**Contents:**
- Project overview and purpose
- Technology stack (Astro, Tailwind, dependencies)
- Prerequisites (Node.js version, npm)
- Installation instructions
- Development commands
- Build process
- Deployment instructions (high-level)
- Project structure overview
- Contributing guidelines (if open source)

### 15.2 DEPLOYMENT.md

**Contents:**
- Detailed deployment instructions for each hosting provider
- Environment variables setup (if any)
- Custom domain configuration
- SSL/HTTPS verification steps
- Redirect configuration (query parameters)
- Security headers setup
- DNS configuration notes
- Troubleshooting common deployment issues
- Rollback procedures

### 15.3 MAINTENANCE.md

**Contents:**
- How to update legal content (Privacy, Terms)
- How to add new FAQ items
- How to update app screenshots
- How to change app store links
- Build and redeploy process
- Performance monitoring setup
- Content update workflow
- Security update procedures

---

## Conclusion

This comprehensive technical plan provides a complete blueprint for implementing the SmilePile landing page. By following this plan, the development team can deliver a high-performance, accessible, SEO-optimized static site that meets all business requirements and technical standards.

**Key Takeaways:**
- **Astro** provides the ideal balance of simplicity and performance for this project
- **Tailwind CSS** accelerates development while maintaining design consistency
- **Client-side + server-side redirects** ensure query parameter routing works universally
- **Markdown content** allows easy legal document updates without code changes
- **Built-in optimizations** (image compression, CSS/JS minification) achieve performance targets
- **Accessibility-first approach** ensures WCAG 2.1 AA compliance from the start
- **Comprehensive testing** validates all requirements before production deployment

**Next Steps:**
1. **Phase 4: Security Review** - Security and peer-reviewer agents validate this plan
2. **Phase 5: Implementation** - Developer agent builds the site following this blueprint
3. **Phase 6: Testing** - QA and accessibility testing
4. **Phase 7: Validation** - Product manager signs off on acceptance criteria
5. **Phase 8: Clean-up** - Final optimizations and polish
6. **Phase 9: Deployment** - Ship to production at https://smilepile.app

**Estimated Completion: 3-5 business days from implementation start**

---

**Plan Created:** October 2, 2025
**Created By:** Developer Agent (Atlas Phase 3)
**Next Phase:** Security Review (Phase 4)
