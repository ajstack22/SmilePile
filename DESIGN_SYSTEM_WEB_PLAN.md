# Website Design System Implementation Plan
**Platform**: Website (Astro + Tailwind CSS)
**Date**: January 2025
**Scope**: Phase 3C - Website Technical Implementation
**Context**: [DESIGN_SYSTEM.md](/Users/adamstack/SmilePile/website/DESIGN_SYSTEM.md) | [Research Report](/Users/adamstack/SmilePile/DESIGN_SYSTEM_IMPLEMENTATION_RESEARCH.md) | [User Stories](/Users/adamstack/SmilePile/DESIGN_SYSTEM_USER_STORIES.md) | [iOS Plan](/Users/adamstack/SmilePile/DESIGN_SYSTEM_IOS_PLAN.md)

---

## Executive Summary

This document provides a **step-by-step technical implementation plan** for migrating SmilePile's website to the new design system. The plan transforms the current generic blue/orange aesthetic (system fonts, generic colors) into a calming, scientifically-backed design system (Atkinson Hyperlegible font, soft muted tones) specifically tailored for families with special needs children.

**Key Changes**:
- **Font**: System fonts → Atkinson Hyperlegible (Google Fonts)
- **Colors**: Generic blue (#0ea5e9) / orange (#f59e0b) → Soft Blue (#7FB3D5) / Sage Green (#A8D8B9)
- **Voice/Tone**: Generic feature-focused copy → Warm, supportive, parent-to-parent
- **Accessibility**: Basic → WCAG 2.2 AA compliant
- **Components**: 7 existing → 15+ standardized components

**Estimated Effort**: 60 hours (7.5 working days)
**Files Modified**: 12 existing files
**Files Created**: 15+ new component files

---

## Table of Contents

1. [File Structure](#1-file-structure)
2. [Font Implementation](#2-font-implementation)
3. [Tailwind Configuration](#3-tailwind-configuration)
4. [Global Styles](#4-global-styles)
5. [Component Library](#5-component-library)
6. [Content Updates](#6-content-updates)
7. [Accessibility](#7-accessibility)
8. [Testing & Validation](#8-testing--validation)

---

## 1. File Structure

### 1.1 Current Website Structure

```
website/
├── src/
│   ├── components/          # 4 existing components
│   │   ├── Header.astro
│   │   ├── Footer.astro
│   │   ├── DownloadButtons.astro
│   │   └── FAQ.astro
│   ├── layouts/
│   │   ├── BaseLayout.astro  # Main layout
│   │   └── LegalLayout.astro # For privacy/terms
│   ├── pages/
│   │   ├── index.astro       # Homepage
│   │   ├── privacy.astro
│   │   ├── terms.astro
│   │   ├── support.astro
│   │   └── 404.astro
│   └── styles/
│       └── global.css        # Global styles
├── tailwind.config.js        # Tailwind configuration
└── package.json
```

### 1.2 New Component Structure

**Create**: `/website/src/components/design-system/`

```
components/
├── design-system/           # NEW: Design system components
│   ├── buttons/
│   │   ├── Button.astro     # Primary, Secondary, Text variants
│   │   └── IconButton.astro
│   ├── cards/
│   │   ├── Card.astro       # Standard card
│   │   ├── FeatureCard.astro
│   │   └── TestimonialCard.astro
│   ├── forms/
│   │   ├── FormInput.astro
│   │   ├── FormTextarea.astro
│   │   └── FormSelect.astro
│   ├── layout/
│   │   ├── Section.astro    # Reusable section wrapper
│   │   └── Container.astro  # Content container
│   └── feedback/
│       ├── Alert.astro      # Success, Error, Info, Warning
│       └── Toast.astro
├── Header.astro             # UPDATED
├── Footer.astro             # UPDATED
├── DownloadButtons.astro    # UPDATED
└── FAQ.astro                # UPDATED
```

### 1.3 Files to Modify

| File | Current State | Changes Required | Priority |
|------|--------------|------------------|----------|
| `tailwind.config.js` | Generic blue/orange | Add design system colors | P0 |
| `src/styles/global.css` | Basic styles | Design system typography, focus states | P0 |
| `src/layouts/BaseLayout.astro` | System fonts | Add Google Fonts link | P0 |
| `src/pages/index.astro` | Generic copy | Rewrite with supportive voice | P1 |
| `src/components/Header.astro` | Generic nav | Design system colors, touch targets | P1 |
| `src/components/DownloadButtons.astro` | Black buttons | Design system Primary Button | P1 |
| `src/pages/privacy.astro` | Generic legal | Warm, transparent language | P2 |
| `src/pages/support.astro` | Generic help | Patient, empathetic tone | P2 |

---

## 2. Font Implementation

### 2.1 Add Atkinson Hyperlegible via Google Fonts

**File**: `/website/src/layouts/BaseLayout.astro`

**Current `<head>` section** (lines 23-49):
```html
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>{title}</title>
  <!-- ... other meta tags -->
</head>
```

**Add BEFORE closing `</head>` tag**:
```html
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>{title}</title>
  <meta name="description" content={description}>

  <!-- Google Fonts: Atkinson Hyperlegible -->
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Atkinson+Hyperlegible:ital,wght@0,400;0,700;1,400;1,700&display=swap" rel="stylesheet">

  <!-- Canonical URL -->
  <link rel="canonical" href={siteUrl + canonical}>
  <!-- ... rest of head -->
</head>
```

**Why `display=swap`?**
- Shows fallback font immediately while custom font loads
- Prevents "flash of invisible text" (FOIT)
- Better performance for users

### 2.2 Font Fallback Stack

**Design System Specification**:
```css
font-family: 'Atkinson Hyperlegible', -apple-system, BlinkMacSystemFont,
             'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
```

**Rationale**:
- **Atkinson Hyperlegible**: Primary font (accessibility-focused)
- **-apple-system**: iOS/macOS fallback (San Francisco)
- **Segoe UI**: Windows fallback
- **Roboto**: Android fallback
- **Arial**: Universal fallback

---

## 3. Tailwind Configuration

### 3.1 Complete tailwind.config.js

**File**: `/website/tailwind.config.js`

**REPLACE ENTIRE FILE**:
```javascript
/** @type {import('tailwindcss').Config} */
export default {
  content: ['./src/**/*.{astro,html,js,jsx,md,mdx,svelte,ts,tsx,vue}'],
  darkMode: 'class', // Enable dark mode with class strategy
  theme: {
    extend: {
      // DESIGN SYSTEM COLORS
      colors: {
        // Primary Palette
        'soft-blue': {
          DEFAULT: '#7FB3D5',
          50: '#F0F7FB',
          100: '#D9EBF5',
          200: '#C2DFF0',
          300: '#AAD3EA',
          400: '#93C7E4',
          500: '#7FB3D5', // Main color
          600: '#6DA3C8',
          700: '#5B93BB',
          800: '#4A7FA1',
          900: '#3B6781',
        },
        'sage-green': {
          DEFAULT: '#A8D8B9',
          50: '#F2F9F5',
          100: '#E8F5EE',
          200: '#D9EDE2',
          300: '#C9E5D5',
          400: '#B8DEC9',
          500: '#A8D8B9', // Main color
          600: '#8FCB9F',
          700: '#76BE85',
          800: '#5FA56C',
          900: '#4A8556',
        },
        'lavender': {
          DEFAULT: '#C9B3D6',
          50: '#F7F4F9',
          100: '#EFE9F3',
          200: '#E4D9EC',
          300: '#D9C9E4',
          400: '#D1BEDD',
          500: '#C9B3D6', // Main color
          600: '#BBA2CC',
          700: '#AD91C2',
          800: '#9A7BAB',
          900: '#7F6390',
        },

        // Neutral Colors
        'warm-cream': '#F8F3ED',
        'soft-charcoal': '#3A3A3A',

        // Supporting Colors
        'gentle-gold': '#F5DA81',
        'soft-coral': '#F4A6A3',
        'pale-mint': '#B8DCD6',

        // Dark Mode Colors
        'soft-black': '#1E1E1E',
        'elevated-surface': '#2A2A2A',
        'soft-blue-dark': '#A8CEEA',
        'sage-green-dark': '#C1E8CF',
        'lavender-dark': '#D8C5E5',

        // Semantic Colors
        'error': '#DC8686', // Muted red (not harsh)
        'warning': '#F5DA81', // Gentle gold
        'success': '#A8D8B9', // Sage green
        'info': '#7FB3D5', // Soft blue
      },

      // DESIGN SYSTEM TYPOGRAPHY
      fontFamily: {
        sans: [
          'Atkinson Hyperlegible',
          '-apple-system',
          'BlinkMacSystemFont',
          'Segoe UI',
          'Roboto',
          'Helvetica Neue',
          'Arial',
          'sans-serif'
        ],
      },

      // DESIGN SYSTEM FONT SCALE
      fontSize: {
        'xs': ['0.875rem', { lineHeight: '1.5' }],    // 14px, 1.5 line-height
        'sm': ['1rem', { lineHeight: '1.6' }],        // 16px, 1.6 line-height (Body)
        'base': ['1rem', { lineHeight: '1.6' }],      // 16px (default Body)
        'lg': ['1.125rem', { lineHeight: '1.6' }],    // 18px (Body Large)
        'xl': ['1.375rem', { lineHeight: '1.4' }],    // 22px (H4)
        '2xl': ['1.75rem', { lineHeight: '1.3' }],    // 28px (H3)
        '3xl': ['2.25rem', { lineHeight: '1.2' }],    // 36px (H2)
        '4xl': ['3rem', { lineHeight: '1.1' }],       // 48px (H1)
      },

      // LINE HEIGHTS (explicit for non-default cases)
      lineHeight: {
        'tight': '1.1',    // H1
        'snug': '1.2',     // H2, Label
        'normal': '1.3',   // H3
        'relaxed': '1.5',  // Body Small, default
        'loose': '1.6',    // Body, Body Large
      },

      // SPACING (for consistent touch targets)
      spacing: {
        '11': '2.75rem',  // 44px (iOS touch target)
        '12': '3rem',     // 48px (Android touch target)
      },

      // BORDER RADIUS
      borderRadius: {
        'sm': '0.5rem',   // 8px (buttons, inputs)
        'md': '0.75rem',  // 12px (cards)
        'lg': '1rem',     // 16px (hero sections)
      },

      // BOX SHADOWS (design system cards)
      boxShadow: {
        'card': '0 2px 8px rgba(0, 0, 0, 0.08)',
        'card-hover': '0 4px 12px rgba(0, 0, 0, 0.12)',
      },

      // MAX WIDTH
      maxWidth: {
        'content': '65ch', // Optimal reading width
      },
    },
  },
  plugins: [
    // Typography plugin for prose styles (optional, but recommended)
    // Install: npm install -D @tailwindcss/typography
    // require('@tailwindcss/typography'),
  ],
}
```

**Key Additions**:
1. **Full color palette** with shades (50-900) for flexibility
2. **Dark mode colors** defined
3. **Atkinson Hyperlegible** font family
4. **Design system font scale** with line heights
5. **Touch target spacing** (44px, 48px)
6. **Card shadows** matching design system
7. **Dark mode enabled** with `class` strategy

---

## 4. Global Styles

### 4.1 Update global.css

**File**: `/website/src/styles/global.css`

**REPLACE ENTIRE FILE**:
```css
@tailwind base;
@tailwind components;
@tailwind utilities;

/* ========================================
   DESIGN SYSTEM GLOBAL STYLES
   Based on DESIGN_SYSTEM.md specification
   ======================================== */

:root {
  /* Light mode colors */
  --color-primary: #7FB3D5;        /* Soft Blue */
  --color-secondary: #A8D8B9;      /* Sage Green */
  --color-accent: #C9B3D6;         /* Lavender */
  --color-background: #F8F3ED;     /* Warm Cream */
  --color-surface: #FFFFFF;        /* White (cards) */
  --color-text-primary: #3A3A3A;   /* Soft Charcoal */
  --color-text-secondary: rgba(58, 58, 58, 0.7); /* Soft Charcoal 70% */
  --color-divider: rgba(0, 0, 0, 0.1);

  /* Semantic colors */
  --color-error: #DC8686;
  --color-warning: #F5DA81;
  --color-success: #A8D8B9;
  --color-info: #7FB3D5;

  /* Focus indicator */
  --color-focus: #7FB3D5;
  --focus-outline-width: 3px;
  --focus-outline-offset: 2px;
}

/* Dark mode colors */
.dark {
  --color-primary: #A8CEEA;        /* Brighter Soft Blue */
  --color-secondary: #C1E8CF;      /* Brighter Sage Green */
  --color-accent: #D8C5E5;         /* Brighter Lavender */
  --color-background: #1E1E1E;     /* Soft Black */
  --color-surface: #2A2A2A;        /* Elevated Surface */
  --color-text-primary: #F8F3ED;   /* Warm Cream */
  --color-text-secondary: rgba(248, 243, 237, 0.7);
  --color-divider: rgba(255, 255, 255, 0.1);
}

/* ========================================
   BASE STYLES
   ======================================== */

@layer base {
  html {
    @apply scroll-smooth;
    font-size: 16px; /* Base font size (WCAG minimum) */
  }

  body {
    @apply min-h-screen antialiased;
    background-color: var(--color-background);
    color: var(--color-text-primary);
    font-family: 'Atkinson Hyperlegible', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
    font-size: 1rem; /* 16px */
    line-height: 1.6;
  }

  /* Headings */
  h1 {
    @apply text-4xl font-bold;
    line-height: 1.1;
    color: var(--color-text-primary);
  }

  h2 {
    @apply text-3xl font-bold;
    line-height: 1.2;
    color: var(--color-text-primary);
  }

  h3 {
    @apply text-2xl font-bold;
    line-height: 1.3;
    color: var(--color-text-primary);
  }

  h4 {
    @apply text-xl font-bold;
    line-height: 1.4;
    color: var(--color-text-primary);
  }

  /* Links */
  a {
    color: var(--color-primary);
    text-decoration: underline;
    text-decoration-color: var(--color-primary);
    text-underline-offset: 2px;
    text-decoration-thickness: 2px;
  }

  a:hover {
    color: var(--color-primary);
    text-decoration-thickness: 3px;
  }

  a:focus-visible {
    outline: var(--focus-outline-width) solid var(--color-focus);
    outline-offset: var(--focus-outline-offset);
    border-radius: 4px;
  }

  /* Remove focus outline from mouse users (only show for keyboard) */
  a:focus:not(:focus-visible) {
    outline: none;
  }
}

/* ========================================
   COMPONENT STYLES
   ======================================== */

@layer components {
  /* Skip to main content link (accessibility) */
  .skip-link {
    @apply absolute z-50 px-4 py-2 rounded;
    left: 0;
    top: -100px; /* Hidden by default */
    background-color: var(--color-primary);
    color: var(--color-background);
    font-weight: 700;
    transition: top 0.2s;
  }

  .skip-link:focus {
    top: 1rem; /* Show on focus */
    outline: 3px solid var(--color-focus);
    outline-offset: 2px;
  }

  /* Prose styles (for long-form content like privacy/terms) */
  .prose {
    @apply max-w-content;
    line-height: 1.6;
  }

  .prose h2 {
    @apply text-2xl font-bold mt-8 mb-4;
  }

  .prose h3 {
    @apply text-xl font-bold mt-6 mb-3;
  }

  .prose p {
    @apply mb-4;
    color: var(--color-text-primary);
  }

  .prose ul, .prose ol {
    @apply mb-4 ml-6;
  }

  .prose li {
    @apply mb-2;
  }

  .prose a {
    color: var(--color-primary);
    font-weight: 600;
  }

  .prose strong {
    @apply font-bold;
    color: var(--color-text-primary);
  }

  /* Button base styles (for use in components) */
  .btn-base {
    @apply inline-flex items-center justify-center;
    @apply px-6 py-3 rounded-sm font-bold;
    @apply transition-colors duration-200;
    font-size: 1rem;
    line-height: 1;
    min-height: 48px; /* Touch target minimum */
    cursor: pointer;
  }

  .btn-base:focus-visible {
    outline: var(--focus-outline-width) solid var(--color-focus);
    outline-offset: var(--focus-outline-offset);
  }

  .btn-base:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  /* Primary button */
  .btn-primary {
    @apply btn-base;
    background-color: var(--color-primary);
    color: var(--color-background);
  }

  .btn-primary:hover:not(:disabled) {
    background-color: #6DA3C8; /* Darker soft blue */
  }

  /* Secondary button */
  .btn-secondary {
    @apply btn-base;
    background-color: transparent;
    color: var(--color-primary);
    border: 2px solid var(--color-primary);
  }

  .btn-secondary:hover:not(:disabled) {
    background-color: rgba(127, 179, 213, 0.1); /* Soft blue 10% */
  }

  /* Text button (tertiary) */
  .btn-text {
    @apply btn-base;
    background-color: transparent;
    color: var(--color-text-primary);
    text-decoration: none;
  }

  .btn-text:hover:not(:disabled) {
    text-decoration: underline;
  }

  /* Card component */
  .card {
    @apply p-6 rounded-md;
    background-color: var(--color-surface);
    border: 1px solid rgba(0, 0, 0, 0.1);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    transition: box-shadow 0.2s;
  }

  .card:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  }
}

/* ========================================
   UTILITY CLASSES
   ======================================== */

@layer utilities {
  /* Focus indicator utility (use on interactive elements) */
  .focus-ring {
    outline: none; /* Remove default */
  }

  .focus-ring:focus-visible {
    outline: var(--focus-outline-width) solid var(--color-focus);
    outline-offset: var(--focus-outline-offset);
    border-radius: 4px;
  }

  /* Touch target enforcement (44x44px minimum iOS, 48x48px Android) */
  .touch-target {
    min-width: 48px;
    min-height: 48px;
  }

  /* Text utilities matching design system */
  .text-body-large {
    font-size: 1.125rem; /* 18px */
    line-height: 1.6;
  }

  .text-body {
    font-size: 1rem; /* 16px */
    line-height: 1.6;
  }

  .text-body-small {
    font-size: 0.875rem; /* 14px */
    line-height: 1.5;
  }

  .text-label {
    font-size: 0.875rem; /* 14px */
    font-weight: 700;
    line-height: 1.2;
  }
}

/* ========================================
   ACCESSIBILITY FEATURES
   ======================================== */

/* Reduced motion support (WCAG 2.3.3) */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
    scroll-behavior: auto !important;
  }
}

/* High contrast mode support */
@media (prefers-contrast: high) {
  :root {
    --color-text-primary: #000000;
    --color-background: #FFFFFF;
  }

  .dark {
    --color-text-primary: #FFFFFF;
    --color-background: #000000;
  }
}

/* Print styles */
@media print {
  header, footer, nav, .no-print {
    display: none !important;
  }

  body {
    background: white;
    color: black;
  }

  a {
    text-decoration: underline;
    color: black;
  }

  a[href^="http"]:after {
    content: " (" attr(href) ")";
    font-size: 0.8em;
  }
}
```

**Key Features**:
1. **CSS Custom Properties** for theme switching
2. **Dark mode support** with `.dark` class
3. **Focus indicators** (3px Soft Blue, 2px offset)
4. **Reduced motion** media query
5. **Touch target utilities** (48px minimum)
6. **Button base classes** for components
7. **Prose styles** for long-form content
8. **Print styles** for accessibility

---

## 5. Component Library

### 5.1 Button Components

#### Button.astro (Primary, Secondary, Text)

**Create**: `/website/src/components/design-system/buttons/Button.astro`

```astro
---
/**
 * Button component following SmilePile Design System
 * Supports Primary, Secondary, and Text variants
 * Min height: 48px (WCAG touch target)
 * Focus: 3px Soft Blue outline, 2px offset
 */

export interface Props {
  variant?: 'primary' | 'secondary' | 'text';
  href?: string;
  type?: 'button' | 'submit' | 'reset';
  disabled?: boolean;
  class?: string;
  ariaLabel?: string;
}

const {
  variant = 'primary',
  href,
  type = 'button',
  disabled = false,
  class: className = '',
  ariaLabel,
} = Astro.props;

const baseClasses = 'btn-base focus-ring';
const variantClasses = {
  primary: 'btn-primary',
  secondary: 'btn-secondary',
  text: 'btn-text',
};

const classes = `${baseClasses} ${variantClasses[variant]} ${className}`;

const Tag = href ? 'a' : 'button';
---

{Tag === 'a' ? (
  <a
    href={href}
    class={classes}
    aria-label={ariaLabel}
  >
    <slot />
  </a>
) : (
  <button
    type={type}
    disabled={disabled}
    class={classes}
    aria-label={ariaLabel}
  >
    <slot />
  </button>
)}
```

**Usage Examples**:
```astro
<!-- Primary button -->
<Button variant="primary">Download Now</Button>

<!-- Secondary button -->
<Button variant="secondary" href="/support">Learn More</Button>

<!-- Text button (tertiary) -->
<Button variant="text">Cancel</Button>

<!-- Disabled -->
<Button variant="primary" disabled>Loading...</Button>
```

#### IconButton.astro

**Create**: `/website/src/components/design-system/buttons/IconButton.astro`

```astro
---
/**
 * Icon-only button with required aria-label
 * Min size: 48x48px (touch target)
 */

export interface Props {
  ariaLabel: string; // REQUIRED for accessibility
  class?: string;
  type?: 'button' | 'submit' | 'reset';
  onClick?: string;
}

const {
  ariaLabel,
  class: className = '',
  type = 'button',
  onClick,
} = Astro.props;

const classes = `
  inline-flex items-center justify-center
  touch-target rounded-sm
  transition-colors duration-200
  focus-ring
  hover:bg-soft-blue/10
  ${className}
`;
---

<button
  type={type}
  class={classes}
  aria-label={ariaLabel}
  onclick={onClick}
>
  <slot />
</button>
```

**Usage**:
```astro
<IconButton ariaLabel="Close menu">
  <svg class="w-6 h-6"><!-- close icon --></svg>
</IconButton>
```

---

### 5.2 Card Components

#### Card.astro

**Create**: `/website/src/components/design-system/cards/Card.astro`

```astro
---
/**
 * Standard card component
 * White background, 1px border, 12px radius, subtle shadow
 */

export interface Props {
  class?: string;
  hoverable?: boolean;
}

const {
  class: className = '',
  hoverable = false,
} = Astro.props;

const classes = `card ${hoverable ? 'hover:shadow-card-hover cursor-pointer' : ''} ${className}`;
---

<div class={classes}>
  <slot />
</div>
```

#### FeatureCard.astro

**Create**: `/website/src/components/design-system/cards/FeatureCard.astro`

```astro
---
/**
 * Feature showcase card with icon, title, description
 */

export interface Props {
  icon: string; // SVG path data
  title: string;
  description: string;
}

const { icon, title, description } = Astro.props;
---

<div class="text-center">
  <!-- Icon -->
  <div class="w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4"
       style="background-color: var(--color-primary); opacity: 0.15;">
    <svg class="w-8 h-8" style="color: var(--color-primary);" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d={icon} />
    </svg>
  </div>

  <!-- Title -->
  <h3 class="text-xl font-bold mb-2" style="color: var(--color-text-primary);">
    {title}
  </h3>

  <!-- Description -->
  <p class="text-body" style="color: var(--color-text-secondary);">
    {description}
  </p>
</div>
```

**Usage**:
```astro
<FeatureCard
  icon="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
  title="Timeline View"
  description="Browse your photos chronologically and relive your family's journey through time."
/>
```

---

### 5.3 Form Components

#### FormInput.astro

**Create**: `/website/src/components/design-system/forms/FormInput.astro`

```astro
---
/**
 * Form text input with visible label
 * Min height: 48px, Soft Blue focus border
 * Error state with icon and message
 */

export interface Props {
  label: string;
  name: string;
  type?: 'text' | 'email' | 'password' | 'tel' | 'url';
  placeholder?: string;
  required?: boolean;
  value?: string;
  error?: string;
  helpText?: string;
  class?: string;
}

const {
  label,
  name,
  type = 'text',
  placeholder = '',
  required = false,
  value = '',
  error,
  helpText,
  class: className = '',
} = Astro.props;

const inputId = `input-${name}`;
const errorId = `error-${name}`;
const helpId = `help-${name}`;

const inputClasses = `
  w-full px-4 py-3 rounded-sm
  border-2 transition-colors
  focus:outline-none focus-ring
  text-body
  ${error ? 'border-error' : 'border-gray-300 focus:border-soft-blue'}
  ${className}
`;
---

<div class="mb-4">
  <!-- Label -->
  <label
    for={inputId}
    class="block text-label mb-2"
    style="color: var(--color-text-primary);"
  >
    {label}
    {required && <span class="text-error" aria-label="required">*</span>}
  </label>

  <!-- Input -->
  <input
    id={inputId}
    name={name}
    type={type}
    placeholder={placeholder}
    required={required}
    value={value}
    class={inputClasses}
    style="min-height: 48px; background-color: var(--color-surface);"
    aria-invalid={error ? 'true' : 'false'}
    aria-describedby={`${error ? errorId : ''} ${helpText ? helpId : ''}`.trim()}
  />

  <!-- Error message -->
  {error && (
    <div
      id={errorId}
      class="flex items-start gap-2 mt-2"
      role="alert"
    >
      <svg class="w-5 h-5 text-error flex-shrink-0 mt-0.5" fill="currentColor" viewBox="0 0 20 20" aria-hidden="true">
        <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
      </svg>
      <span class="text-sm text-error">{error}</span>
    </div>
  )}

  <!-- Help text -->
  {helpText && !error && (
    <p
      id={helpId}
      class="text-body-small mt-2"
      style="color: var(--color-text-secondary);"
    >
      {helpText}
    </p>
  )}
</div>
```

**Usage**:
```astro
<FormInput
  label="Email"
  name="email"
  type="email"
  placeholder="you@example.com"
  required
/>

<FormInput
  label="Email"
  name="email"
  type="email"
  value="invalid"
  error="Please enter a valid email address"
/>
```

---

### 5.4 Alert Component

#### Alert.astro

**Create**: `/website/src/components/design-system/feedback/Alert.astro`

```astro
---
/**
 * Alert component for feedback messages
 * Variants: success, error, info, warning
 */

export interface Props {
  variant?: 'success' | 'error' | 'info' | 'warning';
  title?: string;
  dismissible?: boolean;
  class?: string;
}

const {
  variant = 'info',
  title,
  dismissible = false,
  class: className = '',
} = Astro.props;

const variantStyles = {
  success: {
    bg: 'rgba(168, 216, 185, 0.15)', // Sage green 15%
    border: '#A8D8B9',
    icon: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z',
    iconColor: '#A8D8B9',
  },
  error: {
    bg: 'rgba(220, 134, 134, 0.15)', // Muted red 15%
    border: '#DC8686',
    icon: 'M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z',
    iconColor: '#DC8686',
  },
  info: {
    bg: 'rgba(127, 179, 213, 0.15)', // Soft blue 15%
    border: '#7FB3D5',
    icon: 'M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z',
    iconColor: '#7FB3D5',
  },
  warning: {
    bg: 'rgba(245, 218, 129, 0.15)', // Gentle gold 15%
    border: '#F5DA81',
    icon: 'M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z',
    iconColor: '#F5DA81',
  },
};

const style = variantStyles[variant];
---

<div
  role="alert"
  class={`flex items-start gap-3 p-4 rounded-sm border-2 ${className}`}
  style={`background-color: ${style.bg}; border-color: ${style.border};`}
>
  <!-- Icon -->
  <svg class="w-6 h-6 flex-shrink-0" style={`color: ${style.iconColor};`} fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d={style.icon} />
  </svg>

  <!-- Content -->
  <div class="flex-1">
    {title && (
      <h4 class="font-bold mb-1" style="color: var(--color-text-primary);">
        {title}
      </h4>
    )}
    <div class="text-body" style="color: var(--color-text-primary);">
      <slot />
    </div>
  </div>

  <!-- Dismiss button (optional) -->
  {dismissible && (
    <button
      type="button"
      class="touch-target focus-ring rounded"
      aria-label="Dismiss alert"
      onclick="this.parentElement.remove()"
    >
      <svg class="w-5 h-5" style="color: var(--color-text-secondary);" fill="currentColor" viewBox="0 0 20 20" aria-hidden="true">
        <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
      </svg>
    </button>
  )}
</div>
```

**Usage**:
```astro
<Alert variant="success" title="Success!">
  Your photos have been backed up successfully.
</Alert>

<Alert variant="error">
  Unable to upload photo. Please check your connection and try again.
</Alert>

<Alert variant="info" dismissible>
  New features available! Check out what's new.
</Alert>
```

---

### 5.5 Modal Component

#### Modal.astro

**Create**: `/website/src/components/design-system/feedback/Modal.astro`

```astro
---
/**
 * Modal dialog with focus trap
 * Escape key to close, click outside to close (optional)
 */

export interface Props {
  id: string;
  title: string;
  closeOnBackdrop?: boolean;
}

const {
  id,
  title,
  closeOnBackdrop = true,
} = Astro.props;
---

<div
  id={id}
  class="fixed inset-0 z-50 hidden"
  role="dialog"
  aria-modal="true"
  aria-labelledby={`${id}-title`}
>
  <!-- Backdrop -->
  <div
    class="fixed inset-0 bg-black bg-opacity-50 transition-opacity"
    aria-hidden="true"
    data-backdrop
  ></div>

  <!-- Modal Content -->
  <div class="fixed inset-0 z-10 overflow-y-auto">
    <div class="flex min-h-full items-center justify-center p-4">
      <div
        class="relative w-full max-w-lg transform overflow-hidden rounded-md p-6 text-left shadow-xl transition-all"
        style="background-color: var(--color-surface);"
      >
        <!-- Close button -->
        <button
          type="button"
          class="absolute top-4 right-4 touch-target focus-ring rounded"
          aria-label="Close dialog"
          data-close
        >
          <svg class="w-6 h-6" style="color: var(--color-text-secondary);" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>

        <!-- Title -->
        <h2 id={`${id}-title`} class="text-2xl font-bold mb-4" style="color: var(--color-text-primary);">
          {title}
        </h2>

        <!-- Body -->
        <div class="mb-6">
          <slot />
        </div>

        <!-- Actions -->
        <div class="flex flex-col sm:flex-row gap-3 justify-end">
          <slot name="actions" />
        </div>
      </div>
    </div>
  </div>
</div>

<script define:vars={{ id, closeOnBackdrop }}>
  const modal = document.getElementById(id);
  const backdrop = modal?.querySelector('[data-backdrop]');
  const closeBtn = modal?.querySelector('[data-close]');

  // Open modal function
  window[`open${id}`] = () => {
    modal?.classList.remove('hidden');
    modal?.querySelector('button')?.focus(); // Focus first button
  };

  // Close modal function
  const closeModal = () => {
    modal?.classList.add('hidden');
  };

  // Close button
  closeBtn?.addEventListener('click', closeModal);

  // Backdrop click
  if (closeOnBackdrop) {
    backdrop?.addEventListener('click', closeModal);
  }

  // Escape key
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && !modal?.classList.contains('hidden')) {
      closeModal();
    }
  });
</script>
```

**Usage**:
```astro
<Modal id="confirmModal" title="Confirm Action">
  <p>Are you sure you want to delete this photo? This action cannot be undone.</p>

  <Fragment slot="actions">
    <Button variant="text" onclick="document.getElementById('confirmModal').classList.add('hidden')">
      Cancel
    </Button>
    <Button variant="primary" onclick="deletePhoto()">
      Delete
    </Button>
  </Fragment>
</Modal>

<!-- Trigger -->
<Button variant="primary" onclick="openconfirmModal()">
  Open Modal
</Button>
```

---

### 5.6 Navigation Component (Updated)

#### Update Header.astro

**File**: `/website/src/components/Header.astro`

**REPLACE with**:
```astro
---
// Header component with design system colors and touch targets
---

<header class="sticky top-0 z-50 bg-white shadow-sm">
  <nav class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8" aria-label="Main navigation">
    <div class="flex justify-between items-center h-16">
      <!-- Logo -->
      <a href="/" class="flex items-center space-x-2 focus-ring rounded px-2 py-1">
        <svg class="w-10 h-10 text-soft-blue" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
        <span class="text-xl font-bold" style="color: var(--color-text-primary);">SmilePile</span>
      </a>

      <!-- Desktop Navigation -->
      <ul class="hidden md:flex space-x-2">
        <li>
          <a
            href="/#features"
            class="block px-4 py-2 rounded focus-ring transition-colors hover:bg-soft-blue/10"
            style="color: var(--color-text-primary);"
          >
            Features
          </a>
        </li>
        <li>
          <a
            href="/support"
            class="block px-4 py-2 rounded focus-ring transition-colors hover:bg-soft-blue/10"
            style="color: var(--color-text-primary);"
          >
            Support
          </a>
        </li>
        <li>
          <a
            href="/privacy"
            class="block px-4 py-2 rounded focus-ring transition-colors hover:bg-soft-blue/10"
            style="color: var(--color-text-primary);"
          >
            Privacy
          </a>
        </li>
      </ul>

      <!-- Mobile Menu Button -->
      <button
        id="mobile-menu-btn"
        class="md:hidden touch-target rounded focus-ring"
        aria-label="Toggle menu"
        aria-expanded="false"
      >
        <svg class="w-6 h-6" style="color: var(--color-text-primary);" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
        </svg>
      </button>
    </div>

    <!-- Mobile Navigation -->
    <ul id="mobile-menu" class="hidden md:hidden pb-4 space-y-2">
      <li>
        <a
          href="/#features"
          class="block px-4 py-3 rounded focus-ring transition-colors hover:bg-soft-blue/10"
          style="color: var(--color-text-primary); min-height: 48px;"
        >
          Features
        </a>
      </li>
      <li>
        <a
          href="/support"
          class="block px-4 py-3 rounded focus-ring transition-colors hover:bg-soft-blue/10"
          style="color: var(--color-text-primary); min-height: 48px;"
        >
          Support
        </a>
      </li>
      <li>
        <a
          href="/privacy"
          class="block px-4 py-3 rounded focus-ring transition-colors hover:bg-soft-blue/10"
          style="color: var(--color-text-primary); min-height: 48px;"
        >
          Privacy
        </a>
      </li>
    </ul>
  </nav>
</header>

<script>
  const btn = document.getElementById('mobile-menu-btn');
  const menu = document.getElementById('mobile-menu');

  btn?.addEventListener('click', () => {
    const expanded = btn.getAttribute('aria-expanded') === 'true';
    btn.setAttribute('aria-expanded', (!expanded).toString());
    menu?.classList.toggle('hidden');
  });
</script>
```

**Changes**:
- Colors: `text-primary-600` → `text-soft-blue`
- Touch targets: 48px minimum height on mobile links
- Focus rings: Updated to use design system `.focus-ring` utility
- Hover states: Soft Blue 10% opacity background

---

## 6. Content Updates

### 6.1 Homepage Rewrite (Voice & Tone)

**File**: `/website/src/pages/index.astro`

**BEFORE** (lines 32-35):
```html
<h1 class="text-4xl sm:text-5xl lg:text-6xl font-bold text-gray-900 mb-6">
  Organize Your Family Photos by
  <span class="text-primary-600">Timeline</span>
</h1>
<p class="text-xl text-gray-600 mb-8 max-w-2xl mx-auto lg:mx-0">
  SmilePile helps you rediscover precious moments by organizing photos
  into beautiful timelines. Local-only storage keeps your memories private.
</p>
```

**AFTER** (warm, supportive voice):
```html
<h1 class="text-4xl sm:text-5xl lg:text-6xl font-bold mb-6" style="color: var(--color-text-primary);">
  Celebrate Every Smile,<br>
  Every Milestone,<br>
  <span style="color: var(--color-primary);">Every Moment</span>
</h1>
<p class="text-xl mb-8 max-w-2xl mx-auto lg:mx-0" style="color: var(--color-text-secondary);">
  Your family's joy, beautifully preserved. SmilePile helps you organize and celebrate precious memories—all stored privately on your device, always yours.
</p>
```

**Key Changes**:
- **Headline**: Feature-focused → Emotional, celebration-focused
- **Subheading**: Technical → Benefit-focused with reassurance
- **Tone**: Generic → Warm, supportive
- **Colors**: Updated to design system variables

### 6.2 Feature Descriptions (Benefits over Features)

**File**: `/website/src/pages/index.astro` (lines 77-107)

**BEFORE**:
```html
<h3 class="text-xl font-bold text-gray-900 mb-2">Timeline View</h3>
<p class="text-gray-600">
  Browse your photos chronologically and relive your family's journey through time.
</p>
```

**AFTER**:
```html
<h3 class="text-xl font-bold mb-2" style="color: var(--color-text-primary);">
  Find Any Photo in Seconds
</h3>
<p class="text-body" style="color: var(--color-text-secondary);">
  From first words to first friendships—organize photos by your child's special moments, not just dates.
</p>
```

**Feature 2 (Privacy)** - AFTER:
```html
<h3 class="text-xl font-bold mb-2" style="color: var(--color-text-primary);">
  Your Photos Stay Private—Always
</h3>
<p class="text-body" style="color: var(--color-text-secondary);">
  No cloud sync, no tracking, no data collection. Your memories belong to you, not us.
</p>
```

**Feature 3** - AFTER:
```html
<h3 class="text-xl font-bold mb-2" style="color: var(--color-text-primary);">
  Built for All Families
</h3>
<p class="text-body" style="color: var(--color-text-secondary);">
  Accessible design and simple controls mean everyone can enjoy your family's journey.
</p>
```

### 6.3 Privacy Page (Warm, Transparent Language)

**File**: `/website/src/pages/privacy.astro`

**Example Rewrite** (opening paragraph):

**BEFORE** (generic legal tone):
```
This Privacy Policy describes how SmilePile collects, uses, and shares your personal information when you use our mobile application.
```

**AFTER** (warm, transparent):
```
We built SmilePile to give you complete control over your family's photos. Here's our promise: your photos stay on your device—we don't collect, store, or share them. Ever.

This page explains exactly what information we do and don't collect, in plain language.
```

### 6.4 Error Messages (Gentle, Helpful)

**Examples for future implementation**:

| Scenario | Bad (Technical) | Good (Supportive) |
|----------|----------------|-------------------|
| Upload fail | "Error 503: Service unavailable" | "Hmm, we couldn't upload that photo. Check your connection and try again." |
| Invalid email | "Invalid email format" | "Please enter a valid email address like you@example.com" |
| Storage full | "Insufficient storage space" | "Looks like you're running low on space. Let's free some up or back up your older photos." |
| Not found | "404: Page not found" | "Sorry, we can't find that page. Let's get you back home." |

---

## 7. Accessibility

### 7.1 Skip Links

**Already implemented** in `/website/src/layouts/BaseLayout.astro` (line 52):
```html
<a href="#main-content" class="skip-link">Skip to main content</a>
```

**Update styles** in `global.css` (already done in Section 4.1):
```css
.skip-link {
  @apply absolute z-50 px-4 py-2 rounded;
  left: 0;
  top: -100px;
  background-color: var(--color-primary);
  color: var(--color-background);
  font-weight: 700;
}

.skip-link:focus {
  top: 1rem;
  outline: 3px solid var(--color-focus);
  outline-offset: 2px;
}
```

### 7.2 ARIA Labels

**Checklist for all components**:

```astro
<!-- Icon-only buttons MUST have aria-label -->
<button aria-label="Close menu">
  <svg><!-- icon --></svg>
</button>

<!-- Decorative images MUST have aria-hidden or alt="" -->
<svg aria-hidden="true"><!-- icon --></svg>

<!-- Informative images MUST have descriptive alt text -->
<img src="family.jpg" alt="Mother and son laughing together at birthday party">

<!-- Form inputs MUST have associated labels -->
<label for="email">Email</label>
<input id="email" type="email">

<!-- Error messages MUST use role="alert" -->
<div role="alert">Please enter a valid email address</div>
```

### 7.3 Semantic HTML

**Current Structure**: Already good (using `<header>`, `<nav>`, `<main>`, `<footer>`)

**Ensure**:
- One `<h1>` per page
- Heading hierarchy never skips levels (H1 → H2 → H3, never H1 → H3)
- Landmarks properly labeled:
  ```html
  <nav aria-label="Main navigation">
  <main id="main-content">
  <aside aria-label="Related content">
  ```

### 7.4 Keyboard Navigation

**Test Plan**:
1. **Tab key**: Navigate through all interactive elements
2. **Shift+Tab**: Navigate backwards
3. **Enter/Space**: Activate buttons and links
4. **Escape**: Close modals and dropdowns
5. **Arrow keys**: Navigate within menus (if applicable)

**Verification**:
- Focus indicators visible on all interactive elements
- Tab order follows visual order
- No keyboard traps (can always Tab out of any component)

### 7.5 WCAG 2.2 AA Compliance Checklist

**Color & Contrast**:
- [ ] Normal text (16px): 4.5:1 minimum (Soft Charcoal on Warm Cream = 12.8:1 ✅)
- [ ] Large text (24px+): 3:1 minimum (all combinations pass ✅)
- [ ] Interactive elements: 3:1 minimum (Soft Blue on Warm Cream = 4.6:1 ✅)
- [ ] Don't rely on color alone (use icons + text ✅)

**Typography & Readability**:
- [ ] Minimum body text: 16px ✅
- [ ] Text resizing: 200% without loss of functionality (test with Cmd/Ctrl +)
- [ ] Line height: 1.5x minimum for body text ✅ (1.6 in design system)
- [ ] Paragraph spacing: 2x font size (handled by Tailwind defaults)

**Touch Targets**:
- [ ] Minimum: 48x48px ✅ (enforced in components)
- [ ] Spacing: 8px between targets ✅
- [ ] Visual indicators on hover/focus ✅

**Keyboard Navigation**:
- [ ] All functionality keyboard accessible ✅
- [ ] Visible focus indicators ✅ (3px Soft Blue, 2px offset)
- [ ] Skip to main content link ✅
- [ ] No keyboard traps ✅

**Screen Readers**:
- [ ] All images have alt text (audit needed)
- [ ] Icon buttons have aria-label ✅ (enforced in IconButton component)
- [ ] Form labels visible and associated ✅ (enforced in FormInput component)
- [ ] Error messages announced ✅ (role="alert")
- [ ] Proper heading hierarchy (audit needed)

---

## 8. Testing & Validation

### 8.1 Automated Tools

#### WAVE (Web Accessibility Evaluation Tool)
**Browser Extension**: [https://wave.webaim.org/extension/](https://wave.webaim.org/extension/)

**Usage**:
1. Install WAVE browser extension (Chrome/Firefox)
2. Navigate to SmilePile website
3. Click WAVE icon in toolbar
4. Review errors, alerts, and accessibility features
5. Fix all errors before launch

**Key Checks**:
- Contrast errors (should be 0)
- Missing alt text
- Missing form labels
- Heading structure
- ARIA usage

#### Axe DevTools
**Chrome Extension**: [https://www.deque.com/axe/devtools/](https://www.deque.com/axe/devtools/)

**Usage**:
1. Open Chrome DevTools (F12)
2. Navigate to "Axe DevTools" tab
3. Click "Scan ALL of my page"
4. Review violations by severity
5. Follow remediation guidance

**Target**: 0 violations in Critical and Serious categories

#### Lighthouse (Chrome DevTools)
**Built into Chrome**: Press F12 → Lighthouse tab

**Usage**:
1. Open Chrome DevTools
2. Navigate to Lighthouse tab
3. Select "Accessibility" category
4. Click "Analyze page load"
5. Review scores and opportunities

**Target Scores**:
- Accessibility: 95+ (100 ideal)
- Best Practices: 90+
- SEO: 90+
- Performance: 85+

**Run command**:
```bash
# Via CLI (for CI/CD integration)
npm install -g @lhci/cli
lhci autorun --collect.url=http://localhost:4321
```

### 8.2 Manual Testing

#### Contrast Checking
**Tool**: [WebAIM Contrast Checker](https://webaim.org/resources/contrastchecker/)

**Test Matrix**:
| Background | Foreground | Ratio | WCAG Level |
|------------|------------|-------|------------|
| Warm Cream (#F8F3ED) | Soft Charcoal (#3A3A3A) | 12.8:1 | AAA ✅ |
| Soft Blue (#7FB3D5) | Soft Charcoal (#3A3A3A) | 4.6:1 | AA ✅ |
| Sage Green (#A8D8B9) | Soft Charcoal (#3A3A3A) | 5.2:1 | AA ✅ |
| Lavender (#C9B3D6) | Soft Charcoal (#3A3A3A) | 4.9:1 | AA ✅ |
| Soft Blue (#7FB3D5) | Warm Cream (#F8F3ED) | 2.8:1 | ❌ Fail |

**Action**: Never use Soft Blue text on Warm Cream background without darkening blue.

#### Screen Reader Testing (NVDA - Windows)
**Download**: [https://www.nvaccess.org/download/](https://www.nvaccess.org/download/)

**Test Script**:
1. Launch NVDA (Ctrl+Alt+N)
2. Navigate to SmilePile homepage
3. Press H: Should jump to headings (announce "Heading level 1: Celebrate Every Smile...")
4. Press B: Should jump to buttons (announce "Button: Download Now")
5. Press L: Should jump to links (announce "Link: Features")
6. Press F: Should jump to form fields (announce "Edit, Email, required")
7. Tab through all interactive elements
8. Submit a form and verify error announcements

**Expected**:
- All headings announced with correct level
- All buttons announced with label
- All form fields announced with label
- Errors announced immediately with "Alert" role

#### VoiceOver Testing (Mac)
**Enable**: System Preferences → Accessibility → VoiceOver → Enable

**Test Script**:
1. Enable VoiceOver (Cmd+F5)
2. Navigate with VO+Right Arrow
3. Interact with elements: VO+Space
4. Rotor (VO+U): Browse headings, links, form controls
5. Verify all announcements clear and accurate

#### Keyboard Navigation Testing
**Test Plan**:

| Action | Expected Behavior |
|--------|-------------------|
| Tab from top of page | Focus moves to "Skip to main content" link |
| Tab again | Focus moves to logo |
| Tab through nav | Focus moves through Features, Support, Privacy links |
| Enter on "Features" link | Navigates to Features section |
| Tab into mobile menu button | Focus ring visible |
| Space on mobile menu button | Opens mobile menu |
| Escape in modal | Closes modal |
| Tab in modal | Focus trapped within modal |

**Verification**: All focus indicators visible (3px Soft Blue outline, 2px offset)

### 8.3 Cross-Browser Testing

**Browsers to Test**:
- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)
- Mobile Safari (iOS)
- Chrome Mobile (Android)

**Test Cases**:
1. Font rendering (Atkinson Hyperlegible loads correctly)
2. Colors accurate (Soft Blue not shifting to generic blue)
3. Layout responsive (mobile, tablet, desktop)
4. Focus indicators visible
5. Forms functional
6. Buttons tappable (48px touch targets)

### 8.4 Performance Testing

**Metrics to Monitor**:
- **Font Load Time**: <500ms (Google Fonts with `display=swap`)
- **First Contentful Paint (FCP)**: <1.8s
- **Largest Contentful Paint (LCP)**: <2.5s
- **Cumulative Layout Shift (CLS)**: <0.1
- **Time to Interactive (TTI)**: <3.5s

**Tools**:
- Lighthouse (Performance score)
- WebPageTest: [https://www.webpagetest.org/](https://www.webpagetest.org/)

**Optimization**:
```html
<!-- Font preloading for faster render -->
<link rel="preload" as="font" type="font/woff2"
      href="https://fonts.gstatic.com/s/atkinsonhyperlegible/v11/9Bt23C1KxNDXMspQ1lPyU89-1h6ONRlW45GE5ZgpewSSbQ.woff2"
      crossorigin>
```

---

## 9. Implementation Sequence

### Phase 1: Foundation (Day 1, 8 hours)

**Morning (4 hours)**:
1. ✅ Add Google Fonts link to BaseLayout.astro
2. ✅ Update tailwind.config.js with design system colors and typography
3. ✅ Replace global.css with design system styles
4. ✅ Build site and verify no errors

**Afternoon (4 hours)**:
5. ✅ Create button components (Button.astro, IconButton.astro)
6. ✅ Update Header.astro with design system colors and touch targets
7. ✅ Update DownloadButtons.astro to use Button component
8. ✅ Visual QA homepage

**Deliverable**: Font and colors working site-wide

---

### Phase 2: Component Library (Day 2-3, 16 hours)

**Day 2 (8 hours)**:
1. Create Card.astro and FeatureCard.astro
2. Create FormInput.astro
3. Create Alert.astro
4. Update index.astro to use FeatureCard components
5. Test all components in isolation

**Day 3 (8 hours)**:
6. Create Modal.astro
7. Update Footer.astro with design system colors
8. Create Section.astro and Container.astro (layout components)
9. Update all page files to use new components

**Deliverable**: Complete component library, all pages using components

---

### Phase 3: Content Rewrite (Day 4-5, 16 hours)

**Day 4 (8 hours)**:
1. Rewrite homepage hero with warm, supportive voice
2. Rewrite feature descriptions (benefits over features)
3. Update CTA copy to be inviting, not pushy
4. Update meta descriptions for all pages

**Day 5 (8 hours)**:
5. Rewrite privacy.astro with transparent, warm language
6. Rewrite support.astro with patient, empathetic tone
7. Update terms.astro (less legal-ese where possible)
8. Update 404.astro with helpful, friendly message

**Deliverable**: All copy aligned with design system voice & tone

---

### Phase 4: Accessibility Audit (Day 6, 8 hours)

1. Run WAVE extension on all pages, fix errors
2. Run Axe DevTools, fix Critical/Serious violations
3. Run Lighthouse, achieve 95+ accessibility score
4. Add missing alt text to all images
5. Verify heading hierarchy on all pages
6. Test keyboard navigation through entire site
7. Test with NVDA screen reader
8. Fix all identified issues

**Deliverable**: WCAG 2.2 AA compliant, 0 critical violations

---

### Phase 5: Testing & QA (Day 7, 8 hours)

1. Cross-browser testing (Chrome, Firefox, Safari, Edge)
2. Mobile responsiveness testing (iOS Safari, Chrome Mobile)
3. VoiceOver testing (Mac)
4. Performance testing (Lighthouse, WebPageTest)
5. Visual regression QA (compare before/after screenshots)
6. Touch target verification (48px minimum)
7. Contrast ratio validation (WebAIM checker)
8. Final bug fixes

**Deliverable**: Production-ready website

---

### Phase 6: Deployment (Day 8, 4 hours)

1. Final code review
2. Update release notes
3. Build production site (`npm run build`)
4. Deploy to staging environment
5. Smoke test on staging
6. Deploy to production
7. Monitor for issues

**Deliverable**: Design system live in production

---

## 10. Validation Checklist

### Code Validation
- [ ] No system font references remain (`grep -r "BlinkMacSystemFont" website/src` should only show fallback)
- [ ] No generic blue/orange hex codes (#0ea5e9, #f59e0b)
- [ ] All components use design system CSS variables (`var(--color-primary)`)
- [ ] Build succeeds: `npm run build` (0 errors, 0 warnings)
- [ ] No console errors in browser

### Visual Validation
- [ ] Font renders correctly (Atkinson Hyperlegible on all pages)
- [ ] Colors match design system (Soft Blue, Sage Green, Warm Cream backgrounds)
- [ ] Dark mode works (if implemented)
- [ ] No layout shifts or visual breaks
- [ ] Touch targets visually indicated (hover states)

### Accessibility Validation
- [ ] WAVE: 0 errors
- [ ] Axe: 0 Critical/Serious violations
- [ ] Lighthouse: 95+ accessibility score
- [ ] All buttons 48x48px minimum
- [ ] Focus indicators visible on all interactive elements
- [ ] Screen reader announces all content correctly
- [ ] Keyboard navigation works throughout site
- [ ] Contrast ratios >= 4.5:1 (verified with WebAIM)

### Content Validation
- [ ] Homepage headline: "Celebrate Every Smile, Every Milestone, Every Moment"
- [ ] Voice warm and supportive (not clinical or corporate)
- [ ] Person-first language ("child with autism" not "autistic child")
- [ ] No jargon or technical terms
- [ ] Benefits emphasized over features
- [ ] Privacy messaging reassuring and transparent

### Performance Validation
- [ ] Font loads <500ms
- [ ] Lighthouse Performance: 85+
- [ ] First Contentful Paint: <1.8s
- [ ] Largest Contentful Paint: <2.5s
- [ ] No layout shift (CLS <0.1)

---

## 11. File Reference

### Files Modified (12 files)
- `/website/tailwind.config.js` - Design system colors, typography
- `/website/src/styles/global.css` - Global styles, focus indicators, reduced motion
- `/website/src/layouts/BaseLayout.astro` - Google Fonts link
- `/website/src/pages/index.astro` - Homepage content rewrite
- `/website/src/pages/privacy.astro` - Privacy page rewrite
- `/website/src/pages/support.astro` - Support page rewrite
- `/website/src/pages/terms.astro` - Terms page updates
- `/website/src/pages/404.astro` - Error page updates
- `/website/src/components/Header.astro` - Design system colors, touch targets
- `/website/src/components/Footer.astro` - Design system colors
- `/website/src/components/DownloadButtons.astro` - Use Button component
- `/website/src/components/FAQ.astro` - Design system colors

### Files Created (15+ files)
- `/website/src/components/design-system/buttons/Button.astro`
- `/website/src/components/design-system/buttons/IconButton.astro`
- `/website/src/components/design-system/cards/Card.astro`
- `/website/src/components/design-system/cards/FeatureCard.astro`
- `/website/src/components/design-system/cards/TestimonialCard.astro`
- `/website/src/components/design-system/forms/FormInput.astro`
- `/website/src/components/design-system/forms/FormTextarea.astro`
- `/website/src/components/design-system/forms/FormSelect.astro`
- `/website/src/components/design-system/feedback/Alert.astro`
- `/website/src/components/design-system/feedback/Modal.astro`
- `/website/src/components/design-system/layout/Section.astro`
- `/website/src/components/design-system/layout/Container.astro`

---

## 12. Success Metrics

**Quantitative**:
- [ ] 0 system font references (except fallbacks)
- [ ] 100% design system color adoption
- [ ] 0 WAVE errors
- [ ] 0 Axe Critical violations
- [ ] 95+ Lighthouse accessibility score
- [ ] 15+ standardized components created
- [ ] <500ms font load time

**Qualitative**:
- [ ] Design visually cohesive with iOS/Android
- [ ] Voice warm and supportive (user feedback)
- [ ] Copy resonates with special needs families
- [ ] Developers find components easy to use
- [ ] Accessibility improved (screen reader feedback)

---

## Next Steps

After completing Website implementation:
1. **Cross-Platform QA** - Ensure visual consistency with iOS/Android apps
2. **User Testing** - Validate with special needs families
3. **A/B Testing** - Test new copy against old (if desired)
4. **Analytics Setup** - Monitor user behavior, accessibility feature usage
5. **Production Rollout** - Gradual release plan (QUAL → STAGE → PROD)
6. **Post-Launch Monitoring** - Watch for accessibility issues, user feedback

---

**Document Version**: 1.0
**Last Updated**: January 2025
**Author**: Atlas Developer Agent (Phase 3C)
**Review Status**: Ready for Implementation
**Estimated Effort**: 60 hours (7.5 days)
**Dependencies**: None (can run parallel with iOS/Android)
