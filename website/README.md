# SmilePile Landing Page

Official landing page for SmilePile - a family photo timeline organizer app.

## Overview

This is a static website built with Astro 5.x and Tailwind CSS, providing:

- Homepage with app features and download buttons
- Privacy Policy (COPPA-compliant)
- Terms of Service
- Support page with FAQs
- SEO optimization with sitemap and meta tags
- Accessibility features (WCAG 2.1 AA compliant)

## Technology Stack

- **Framework**: Astro 5.14.1
- **CSS**: Tailwind CSS 3.4
- **Build**: Vite (bundled with Astro)
- **Deployment**: Vercel/Netlify (static files)

## Quick Start

### Prerequisites

- Node.js 18+ (LTS recommended)
- npm 9+ or yarn 1.22+

### Installation

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

The dev server runs at `http://localhost:4321`

## Project Structure

```
website/
├── src/
│   ├── pages/              # Routes (index, privacy, terms, support, 404)
│   ├── layouts/            # Shared layouts (Base, Legal)
│   ├── components/         # Reusable components (Header, Footer, FAQ, etc.)
│   ├── styles/             # Global CSS with Tailwind
│   └── content/            # Markdown content (if needed)
├── public/                 # Static assets (robots.txt, images, favicons)
├── dist/                   # Built output (generated)
├── astro.config.mjs        # Astro configuration
├── tailwind.config.js      # Tailwind configuration
├── vercel.json             # Vercel deployment config
└── netlify.toml            # Netlify deployment config
```

## Key Features

### Pages

1. **Homepage** (`/`)
   - Hero section with app description
   - Features showcase (3 key features)
   - Download buttons (iOS and Android)
   - Query parameter redirects (/?privacy → /privacy, /?tos → /terms)

2. **Privacy Policy** (`/privacy`)
   - COPPA-compliant content
   - Enhanced parental rights section (2025 FTC amendments)
   - Local-only storage emphasis
   - No tracking or data collection

3. **Terms of Service** (`/terms`)
   - Standard mobile app ToS
   - Clear user rights and responsibilities
   - Liability limitations

4. **Support** (`/support`)
   - FAQ accordion (7 common questions)
   - Troubleshooting tips
   - Contact information (support@stackmap.app)

5. **404 Page** (`/404`)
   - Friendly error page with navigation

### SEO Features

- Unique meta tags per page (title, description)
- Open Graph tags for social sharing
- Twitter Card tags
- Sitemap.xml (auto-generated)
- Robots.txt
- Canonical URLs
- Semantic HTML structure

### Accessibility

- WCAG 2.1 AA compliant
- Semantic HTML (proper heading hierarchy)
- Skip navigation links
- Keyboard navigable
- ARIA labels on interactive elements
- Focus indicators
- Screen reader friendly

### Performance

- Lighthouse score targets: 90+ on all metrics
- Optimized images (WebP with fallbacks)
- Minified CSS/HTML/JS
- Inlined critical CSS
- Lazy loading for images
- Total page weight: <100KB per page

## Query Parameter Routing

The site supports legacy query parameter URLs from mobile apps:

- `/?privacy` → redirects to `/privacy`
- `/?tos` → redirects to `/terms`

**Implementation:**
- Client-side redirect (JavaScript in index.astro)
- Server-side redirect (vercel.json/netlify.toml for hosting providers)

Both methods ensure compatibility across all platforms.

## Deployment

### Option 1: Vercel (Recommended)

```bash
# Install Vercel CLI
npm i -g vercel

# Deploy
vercel

# Or connect GitHub repo at vercel.com
```

Configuration is in `vercel.json`:
- Redirects for query parameters
- Security headers (X-Frame-Options, CSP, etc.)

### Option 2: Netlify

```bash
# Install Netlify CLI
npm i -g netlify-cli

# Deploy
netlify deploy --prod

# Or connect GitHub repo at netlify.com
```

Configuration is in `netlify.toml`:
- Redirects for query parameters
- Security headers

### Option 3: Cloudflare Pages

1. Go to pages.cloudflare.com
2. Connect GitHub repository
3. Build settings:
   - Framework: Astro
   - Build command: `npm run build`
   - Output directory: `dist`

### Manual Deployment

Build and upload the `dist/` folder to any static hosting provider:

```bash
npm run build
# Upload contents of dist/ folder
```

## Security Headers

The following security headers are configured in deployment configs:

- `X-Frame-Options: DENY` (prevent clickjacking)
- `X-Content-Type-Options: nosniff` (prevent MIME sniffing)
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Permissions-Policy: camera=(), microphone=(), geolocation=()`

For local development, these headers are not enforced.

## Environment

No environment variables are required. All configuration is static.

## Browser Support

- Chrome (latest)
- Safari (latest)
- Firefox (latest)
- Edge (latest)
- Mobile Safari (iOS 15+)
- Chrome for Android

## Content Updates

### Updating Legal Pages

1. Edit content directly in:
   - `src/pages/privacy.astro`
   - `src/pages/terms.astro`

2. Update "Last Updated" date

3. Rebuild and redeploy:
   ```bash
   npm run build
   vercel --prod  # or netlify deploy --prod
   ```

### Adding FAQs

1. Edit `src/pages/support.astro`
2. Add new FAQ object to the `faqs` array
3. Rebuild and redeploy

### Updating Download Links

1. Edit `src/components/DownloadButtons.astro`
2. Replace `[APP_ID]` placeholders with actual App Store/Google Play IDs
3. Rebuild and redeploy

## Performance Checklist

Before deployment, verify:

- [ ] All images optimized (<500KB each)
- [ ] Lighthouse score ≥ 90 on all metrics
- [ ] No console errors
- [ ] All links functional
- [ ] Query parameter redirects work
- [ ] Mobile responsive on all pages
- [ ] Accessibility audit passed

## Maintenance

### Regular Updates

- **Monthly**: Review FAQ based on support emails
- **Quarterly**: Update legal content if needed
- **Annual**: Comprehensive content audit

### Monitoring

- Google Search Console (sitemap submission)
- Uptime monitoring (UptimeRobot, Pingdom)
- Analytics (if added later)

## Troubleshooting

### Build Errors

```bash
# Clear cache and rebuild
rm -rf dist/ node_modules/
npm install
npm run build
```

### Routing Issues

- Ensure `vercel.json` or `netlify.toml` redirects are configured
- Test client-side redirects in `src/pages/index.astro`

### Styling Issues

- Check Tailwind CSS purge configuration in `tailwind.config.js`
- Verify global CSS imports in `src/styles/global.css`

## Support

For technical issues with the website:
- Email: support@stackmap.app
- Response time: 1-2 business days

## License

Copyright © 2025 StackMap. All rights reserved.

## Links

- Live site: https://smilepile.app
- iOS App: [App Store Link]
- Android App: [Google Play Link]
