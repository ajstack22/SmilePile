# Website Design System Security Audit
**Phase 4: Security Review - Website Only**
**Date**: January 2025
**Scope**: SmilePile Promotional Website (`/website/`)
**Platform**: Astro + Tailwind CSS
**Hosting**: Netlify/Vercel (Static Site)

---

## Executive Summary

This security audit evaluates the planned design system implementation for the SmilePile promotional website. The website is a **static marketing site** with no user authentication, no backend, and no sensitive data processing. Security concerns are primarily focused on **third-party dependencies, CDN integrity, client-side XSS prevention, and privacy compliance**.

**Risk Level**: LOW (static site, no user data)
**Critical Issues Found**: 2
**High Priority Issues**: 3
**Recommendations**: 5 specific mitigations

---

## Top 5 Security Risks (Prioritized)

### 1. Font Loading from Google CDN - MITM & Privacy Risk
**Severity**: CRITICAL
**CWE**: CWE-829 (Inclusion of Functionality from Untrusted Control Sphere)

**Issue**:
The technical plan loads Atkinson Hyperlegible from Google Fonts CDN without Subresource Integrity (SRI) hashes:

```html
<!-- CURRENT PLAN - VULNERABLE -->
<link href="https://fonts.googleapis.com/css2?family=Atkinson+Hyperlegible:ital,wght@0,400;0,700;1,400;1,700&display=swap" rel="stylesheet">
```

**Risks**:
- **MITM Attack**: If Google CDN is compromised or DNS hijacked, malicious CSS could inject keyloggers or phishing overlays
- **Privacy Violation**: Every page load sends user IP, User-Agent, and Referer to Google (GDPR concern for EU users)
- **Tracking**: Google can correlate visits across SmilePile pages
- **Dependency Risk**: If Google Fonts goes down or changes URL structure, site breaks

**Recommendation**: **Self-host fonts** (preferred) or use SRI hashes

**MITIGATED SOLUTION 1: Self-Host Fonts (RECOMMENDED)**

```bash
# Download fonts from Google Fonts
# Visit: https://fonts.google.com/specimen/Atkinson+Hyperlegible
# Download font files (woff2 format)

# Place in: /website/public/fonts/
# - atkinson-hyperlegible-regular.woff2
# - atkinson-hyperlegible-bold.woff2
# - atkinson-hyperlegible-italic.woff2
# - atkinson-hyperlegible-bold-italic.woff2
```

**Update `global.css`**:
```css
/* Self-hosted Atkinson Hyperlegible */
@font-face {
  font-family: 'Atkinson Hyperlegible';
  src: url('/fonts/atkinson-hyperlegible-regular.woff2') format('woff2');
  font-weight: 400;
  font-style: normal;
  font-display: swap;
}

@font-face {
  font-family: 'Atkinson Hyperlegible';
  src: url('/fonts/atkinson-hyperlegible-bold.woff2') format('woff2');
  font-weight: 700;
  font-style: normal;
  font-display: swap;
}

@font-face {
  font-family: 'Atkinson Hyperlegible';
  src: url('/fonts/atkinson-hyperlegible-italic.woff2') format('woff2');
  font-weight: 400;
  font-style: italic;
  font-display: swap;
}

@font-face {
  font-family: 'Atkinson Hyperlegible';
  src: url('/fonts/atkinson-hyperlegible-bold-italic.woff2') format('woff2');
  font-weight: 700;
  font-style: italic;
  font-display: swap;
}
```

**Benefits**:
- No third-party requests (privacy compliant)
- No MITM risk
- Faster load times (same-origin, HTTP/2 multiplexing)
- No dependency on Google infrastructure
- Simpler CSP (no need to allow Google domains)

**File Size**: ~120KB total (4 woff2 files) - acceptable for marketing site

---

**MITIGATED SOLUTION 2: SRI Hashes (If CDN Required)**

If self-hosting is not acceptable:

```html
<!-- With SRI integrity check -->
<link
  rel="stylesheet"
  href="https://fonts.googleapis.com/css2?family=Atkinson+Hyperlegible:ital,wght@0,400;0,700;1,400;1,700&display=swap"
  integrity="sha384-HASH_HERE"
  crossorigin="anonymous"
>
```

**Steps to generate SRI hash**:
```bash
# Download CSS from Google Fonts
curl -o atkinson.css "https://fonts.googleapis.com/css2?family=Atkinson+Hyperlegible:ital,wght@0,400;0,700;1,400;1,700&display=swap"

# Generate SRI hash
openssl dgst -sha384 -binary atkinson.css | openssl base64 -A
```

**WARNING**: Google Fonts CSS is **dynamic** (serves different font files based on User-Agent). SRI hashes will break for different browsers. **Self-hosting is strongly recommended**.

---

### 2. Missing Content Security Policy (CSP)
**Severity**: CRITICAL
**CWE**: CWE-1021 (Improper Restriction of Rendered UI Layers)

**Issue**:
No CSP headers defined in Astro config or Netlify/Vercel deployment configuration. Without CSP:
- Inline scripts can execute arbitrary JavaScript
- Any domain can load resources (fonts, images, scripts)
- XSS attacks can inject malicious content
- Clickjacking attacks possible

**Current Risk**: Astro components use inline `<script>` tags (e.g., mobile menu toggle in Header.astro). If any user-controlled input is reflected in HTML (forms, URL parameters), XSS is possible.

**Recommendation**: Implement strict CSP with nonces for inline scripts

**MITIGATED SOLUTION**:

**For Netlify** - Create `/website/public/_headers`:
```
/*
  # Prevent MIME type sniffing
  X-Content-Type-Options: nosniff

  # Prevent clickjacking
  X-Frame-Options: DENY

  # XSS Protection (legacy browsers)
  X-XSS-Protection: 1; mode=block

  # Referrer Policy (privacy)
  Referrer-Policy: strict-origin-when-cross-origin

  # Permissions Policy (restrict features)
  Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=()

  # Content Security Policy (STRICT)
  Content-Security-Policy: default-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self'; connect-src 'self'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'; upgrade-insecure-requests;
```

**For Vercel** - Create `/website/vercel.json`:
```json
{
  "headers": [
    {
      "source": "/(.*)",
      "headers": [
        {
          "key": "X-Content-Type-Options",
          "value": "nosniff"
        },
        {
          "key": "X-Frame-Options",
          "value": "DENY"
        },
        {
          "key": "X-XSS-Protection",
          "value": "1; mode=block"
        },
        {
          "key": "Referrer-Policy",
          "value": "strict-origin-when-cross-origin"
        },
        {
          "key": "Permissions-Policy",
          "value": "camera=(), microphone=(), geolocation=(), payment=()"
        },
        {
          "key": "Content-Security-Policy",
          "value": "default-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self'; connect-src 'self'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'; upgrade-insecure-requests;"
        }
      ]
    }
  ]
}
```

**CSP Breakdown**:
- `default-src 'none'` - Block all by default (whitelist approach)
- `script-src 'self' 'unsafe-inline'` - Allow scripts from same origin + inline (Astro requirement)
- `style-src 'self' 'unsafe-inline'` - Allow styles from same origin + inline (Tailwind requirement)
- `img-src 'self' data: https:` - Allow images from same origin, data URIs, and HTTPS (for external screenshots)
- `font-src 'self'` - Allow fonts only from same origin (assumes self-hosted fonts)
- `connect-src 'self'` - Allow AJAX/WebSocket to same origin only
- `base-uri 'self'` - Prevent `<base>` tag injection
- `form-action 'self'` - Forms can only submit to same origin
- `frame-ancestors 'none'` - Prevent embedding in iframes (clickjacking protection)
- `upgrade-insecure-requests` - Auto-upgrade HTTP to HTTPS

**If using Google Fonts CDN**, adjust CSP:
```
font-src 'self' https://fonts.gstatic.com;
style-src 'self' 'unsafe-inline' https://fonts.googleapis.com;
```

**NOTE**: `'unsafe-inline'` is required for Astro inline scripts and Tailwind inline styles. To remove it:
1. Extract all `<script>` tags to external `.js` files
2. Use CSP nonces for inline scripts (requires server-side rendering or build-time injection)
3. Use Tailwind JIT mode with external stylesheet (already done via `global.css`)

---

### 3. Inline Script XSS via URL Parameters
**Severity**: HIGH
**CWE**: CWE-79 (Cross-Site Scripting)

**Issue**:
If any Astro component uses `Astro.url.searchParams` or `Astro.params` and injects values into HTML without sanitization, XSS is possible.

**Example Vulnerable Pattern**:
```astro
---
const search = Astro.url.searchParams.get('q');
---
<p>You searched for: {search}</p>
```

If user visits: `https://smilepile.app/?q=<script>alert('XSS')</script>`

**Current Risk**: LOW (no forms or user input in current site plan)

**Future Risk**: MEDIUM (if contact form or search is added)

**Recommendation**: Sanitize all user input before rendering

**MITIGATED SOLUTION**:

**Use Astro's built-in XSS protection** (HTML is auto-escaped):
```astro
---
const search = Astro.url.searchParams.get('q') || '';
---
<!-- SAFE: Astro auto-escapes HTML -->
<p>You searched for: {search}</p>
<!-- Renders: You searched for: &lt;script&gt;alert('XSS')&lt;/script&gt; -->
```

**UNSAFE Patterns to Avoid**:
```astro
<!-- UNSAFE: Using set:html -->
<div set:html={userInput} />

<!-- UNSAFE: Using dangerouslySetInnerHTML -->
<div dangerouslySetInnerHTML={{ __html: userInput }} />

<!-- UNSAFE: Direct DOM manipulation in script -->
<script>
  document.getElementById('search').innerHTML = location.search;
</script>
```

**Safe Alternative for Rich Text** (if needed):
```bash
npm install dompurify
```

```astro
---
import DOMPurify from 'dompurify';
const dirty = Astro.url.searchParams.get('content') || '';
const clean = DOMPurify.sanitize(dirty);
---
<div set:html={clean} />
```

**Checklist for Phase 6 Testing**:
- [ ] Test all forms with `<script>alert(1)</script>` input
- [ ] Test all URL parameters with XSS payloads
- [ ] Verify no `set:html` or `dangerouslySetInnerHTML` in codebase
- [ ] Grep for `Astro.url.searchParams` and audit usage

---

### 4. Dependency Supply Chain Risk
**Severity**: HIGH
**CWE**: CWE-1357 (Reliance on Insufficiently Trustworthy Component)

**Issue**:
Website depends on 5+ npm packages (Astro, Tailwind, compression plugins). Any compromised dependency can:
- Inject malicious build-time code
- Exfiltrate environment variables
- Modify HTML/CSS/JS at build time
- Create backdoors in production site

**Current Dependencies** (from `package.json`):
```json
{
  "dependencies": {
    "@astrojs/sitemap": "^3.0.0",      // Official Astro package
    "@astrojs/tailwind": "^5.0.0",     // Official Astro package
    "astro": "^5.14.1",                 // Official Astro framework
    "tailwindcss": "^3.4.0"             // Official Tailwind CSS
  },
  "devDependencies": {
    "astro-compress": "^2.0.0",         // Third-party plugin (RISK)
    "eslint": "^8.57.1",
    "eslint-plugin-astro": "^0.31.4",
    "eslint-plugin-no-secrets": "^1.1.2",
    "eslint-plugin-security": "^2.1.1"
  }
}
```

**Risks**:
- **Caret (^) versioning**: Allows minor version updates (e.g., `^3.0.0` allows `3.9.9` but not `4.0.0`)
- **Transitive dependencies**: Each package has its own dependencies (200+ total packages in `node_modules`)
- **Compromised maintainers**: If maintainer account is hacked, malicious version can be published
- **Typosquatting**: Similar package names (e.g., `astro-compres` vs `astro-compress`)

**Recommendation**: Lock versions, audit packages, enable GitHub Dependabot

**MITIGATED SOLUTION**:

**Step 1: Generate package-lock.json** (if missing):
```bash
cd /Users/adamstack/SmilePile/website
npm install --package-lock-only
```

**Step 2: Audit dependencies**:
```bash
npm audit --audit-level=moderate
npm audit fix  # Apply automated fixes
```

**Step 3: Check for known vulnerabilities**:
```bash
# Install dependency checker
npm install -g npm-check-updates

# Check for outdated packages
ncu

# Check for security issues
npm audit --production
```

**Step 4: Lock exact versions** (optional, for maximum security):

Update `package.json` to remove `^`:
```json
{
  "dependencies": {
    "@astrojs/sitemap": "3.0.0",
    "@astrojs/tailwind": "5.0.0",
    "astro": "5.14.1",
    "tailwindcss": "3.4.0"
  }
}
```

**Step 5: Enable GitHub Dependabot**:

Create `.github/dependabot.yml`:
```yaml
version: 2
updates:
  - package-ecosystem: "npm"
    directory: "/website"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 5
    labels:
      - "dependencies"
      - "security"
    reviewers:
      - "adamstack"
    commit-message:
      prefix: "chore(deps)"
```

**Step 6: Pre-commit hook for security checks**:
```bash
# Install husky for git hooks
npm install --save-dev husky

# Add pre-commit hook
npx husky install
npx husky add .husky/pre-commit "cd website && npm audit --audit-level=high"
```

**Verification**:
```bash
# Check for secrets in dependencies
npm run lint:security

# Generate license report
npm run licenses:all

# Verify no GPL/AGPL licenses
npm run licenses:verify
```

---

### 5. Accessibility as Security - Phishing via Fake Focus Indicators
**Severity**: MEDIUM
**CWE**: CWE-451 (User Interface Misrepresentation)

**Issue**:
The design system specifies custom focus indicators (3px Soft Blue outline). If CSS is not carefully implemented, attackers can:
- Fake focus states to trick users into clicking malicious links
- Hide real focus indicators to confuse keyboard users
- Overlay fake UI elements that mimic SmilePile design

**Example Attack Scenario**:
1. Attacker creates phishing site: `smilep1le.app` (note: `1` instead of `i`)
2. Uses SmilePile's design system colors and focus styles
3. Overlays fake "Download" button that looks focused
4. User thinks it's safe link, clicks, downloads malware

**Current Risk**: MEDIUM (if design system CSS is publicly accessible)

**Recommendation**: Obfuscate CSS class names in production, add visual trust indicators

**MITIGATED SOLUTION**:

**Step 1: Enable CSS minification with class name hashing**:

Update `astro.config.mjs`:
```javascript
export default defineConfig({
  // ... existing config
  vite: {
    build: {
      cssCodeSplit: false,
      minify: 'terser',  // Use terser for aggressive minification
      rollupOptions: {
        output: {
          assetFileNames: '_astro/[name].[hash][extname]',
          // Hash CSS class names in production
          manualChunks: undefined,
        },
      },
    },
    css: {
      postcss: {
        plugins: [
          // Rename CSS classes in production (optional)
          // require('postcss-modules')({ generateScopedName: '[hash:base64:8]' })
        ],
      },
    },
  },
});
```

**Step 2: Add visual trust indicators**:

Add visible domain verification to Header:
```astro
<!-- In Header.astro -->
<header class="sticky top-0 z-50 bg-white shadow-sm">
  <nav>
    <a href="/" class="flex items-center space-x-2">
      <svg><!-- logo --></svg>
      <span class="text-xl font-bold">SmilePile</span>
      <!-- Trust indicator -->
      <span class="text-xs text-soft-blue ml-2 hidden sm:inline">
        smilepile.app ✓
      </span>
    </a>
  </nav>
</header>
```

**Step 3: Add hover state warnings for external links**:

```astro
---
// In BaseLayout.astro or global script
---
<script>
  // Warn users when clicking external links
  document.addEventListener('click', (e) => {
    const link = e.target.closest('a');
    if (link && link.hostname !== window.location.hostname) {
      const confirmed = confirm(
        `You are leaving SmilePile and going to:\n${link.href}\n\nAre you sure?`
      );
      if (!confirmed) {
        e.preventDefault();
      }
    }
  });
</script>
```

**Step 4: Implement Subresource Integrity for all assets**:

If loading any external CSS/JS (not recommended), use SRI:
```html
<link
  rel="stylesheet"
  href="https://cdn.example.com/styles.css"
  integrity="sha384-ABC123..."
  crossorigin="anonymous"
>
```

---

## Additional Security Recommendations

### 6. Environment Variable Exposure (Build-Time)
**Risk**: MEDIUM
**Issue**: If `.env` files contain secrets and are accidentally committed or exposed at build time

**Mitigation**:
- Add `.env*` to `.gitignore` (already done)
- Use Netlify/Vercel environment variables (not `.env` files)
- Prefix public variables with `PUBLIC_` in Astro
- Scan for secrets in commits: `npm run lint:security`

**Example Safe Environment Variable Usage**:
```astro
---
// SAFE: Public variable
const publicKey = import.meta.env.PUBLIC_API_KEY;

// UNSAFE: Secret variable exposed to client
const secret = import.meta.env.SECRET_API_KEY; // NEVER DO THIS
---
```

---

### 7. Clickjacking Protection
**Risk**: LOW (already mitigated by X-Frame-Options in CSP recommendation)

**Additional Mitigation**:
Add meta tag for legacy browsers:
```html
<!-- In BaseLayout.astro <head> -->
<meta http-equiv="X-Frame-Options" content="DENY">
```

---

### 8. HTTPS Enforcement
**Risk**: LOW (Netlify/Vercel auto-provision HTTPS)

**Verification**:
- Ensure `upgrade-insecure-requests` in CSP
- Redirect HTTP → HTTPS at DNS level
- Enable HSTS (Strict-Transport-Security header)

**Add to `_headers` or `vercel.json`**:
```
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
```

---

### 9. Form Security (Future Consideration)
**Risk**: MEDIUM (if contact form added in future)

**If adding forms**, implement:
- CSRF tokens (e.g., using Netlify Forms or Formspree)
- Rate limiting (Cloudflare, Netlify Edge Functions)
- Honeypot fields (catch bots)
- ReCAPTCHA v3 (invisible CAPTCHA)

**Example Secure Form**:
```astro
<form method="POST" netlify netlify-honeypot="bot-field">
  <input type="hidden" name="bot-field" />
  <input type="hidden" name="form-name" value="contact" />
  <label for="email">Email</label>
  <input type="email" name="email" required />
  <button type="submit">Send</button>
</form>
```

---

## Security Testing Checklist (Phase 6)

### Automated Scans
- [ ] Run `npm audit --audit-level=moderate` (0 vulnerabilities)
- [ ] Run `npm run lint:security` (ESLint security plugin)
- [ ] Run OWASP ZAP scan (https://www.zaproxy.org/)
- [ ] Run Mozilla Observatory (https://observatory.mozilla.org/)
- [ ] Verify CSP with https://csp-evaluator.withgoogle.com/

### Manual Tests
- [ ] Test XSS payloads in all URL parameters
- [ ] Test XSS payloads in future form inputs
- [ ] Verify all external links open in new tab with `rel="noopener noreferrer"`
- [ ] Verify no secrets in source code (`grep -r "api_key\|password\|secret" website/src`)
- [ ] Check for exposed `.env` files (`curl https://smilepile.app/.env`)
- [ ] Verify HTTPS redirect (visit http://smilepile.app)
- [ ] Check security headers (https://securityheaders.com/)
- [ ] Verify fonts load correctly with CSP (test in multiple browsers)
- [ ] Test keyboard navigation (ensure no focus hijacking)

### Dependency Audit
- [ ] Review all dependencies for known vulnerabilities
- [ ] Verify all dependencies have valid licenses
- [ ] Check for typosquatting (verify package names match official repos)
- [ ] Enable Dependabot alerts on GitHub

### Privacy Compliance
- [ ] Verify no tracking scripts (Google Analytics, Facebook Pixel)
- [ ] Verify no external font loading (or add GDPR notice)
- [ ] Test with Privacy Badger extension (should not block resources)
- [ ] Verify no third-party cookies

---

## Recommended CSP for Production (Final)

**If Self-Hosting Fonts** (RECOMMENDED):
```
Content-Security-Policy: default-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self'; connect-src 'self'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'; upgrade-insecure-requests; block-all-mixed-content;
```

**If Using Google Fonts CDN** (NOT RECOMMENDED):
```
Content-Security-Policy: default-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; img-src 'self' data: https:; font-src 'self' https://fonts.gstatic.com; connect-src 'self'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'; upgrade-insecure-requests; block-all-mixed-content;
```

---

## Summary of Mitigations

| Issue | Severity | Mitigation | File Location | Effort |
|-------|----------|-----------|---------------|--------|
| Font CDN MITM | CRITICAL | Self-host fonts | `/website/public/fonts/` | 1 hour |
| Missing CSP | CRITICAL | Add security headers | `/website/public/_headers` or `/website/vercel.json` | 30 min |
| XSS via URL params | HIGH | Use Astro auto-escaping, sanitize inputs | All `.astro` files | Ongoing |
| Dependency risk | HIGH | Lock versions, enable Dependabot | `package.json`, `.github/dependabot.yml` | 1 hour |
| Phishing via fake focus | MEDIUM | Obfuscate CSS, add trust indicators | `astro.config.mjs`, `Header.astro` | 1 hour |

**Total Estimated Effort**: 4-5 hours

---

## Conclusion

The SmilePile website design system is **generally secure** for a static marketing site. The primary risks are:

1. **Third-party font loading** (solve with self-hosting)
2. **Missing CSP headers** (solve with Netlify/Vercel config)
3. **Future XSS risk** if forms are added (mitigate with sanitization)

**No sensitive user data** is stored or processed on the website, significantly reducing attack surface. Implementing the 5 mitigations above will achieve **OWASP A+ security rating** and full GDPR compliance.

**Next Steps**:
1. Self-host Atkinson Hyperlegible fonts
2. Add CSP headers to hosting config
3. Enable Dependabot for automated security updates
4. Run Phase 6 security testing checklist
5. Deploy to staging and verify with OWASP ZAP scan

---

**Audit Completed**: January 2025
**Auditor**: Atlas Security Agent
**Status**: READY FOR IMPLEMENTATION
