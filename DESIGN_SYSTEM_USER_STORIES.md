# Design System Implementation User Stories
**Phase 2: Story Creation**
**Date**: January 2025
**Product Manager**: Atlas PM Agent
**Scope**: iOS (SwiftUI), Android (Kotlin/Compose), Website (Astro)

---

## Executive Summary

This document defines the complete set of user stories for implementing SmilePile's design system across all platforms. The implementation will transform the current bright, playful aesthetic into a scientifically-backed, calming design system specifically tailored for families with special needs children.

**Key Changes**:
- Font: Nunito → Atkinson Hyperlegible (accessibility-first)
- Colors: Bright/vibrant → Soft/muted tones (anxiety-reducing)
- Accessibility: Basic → WCAG 2.2 AA compliant
- Components: Ad-hoc → Standardized library
- Voice/Tone: Generic → Warm, supportive, parent-to-parent

**Total Scope**: ~268 files, 216 hours estimated effort

---

## Epic Overview

### Epic 1: Typography System Implementation
**Business Value**: Enhance readability for users with visual processing challenges, demonstrating commitment to accessibility while maintaining brand warmth.

### Epic 2: Color Palette Transformation
**Business Value**: Create a calming, anxiety-reducing visual environment scientifically proven to support families dealing with sensory sensitivities.

### Epic 3: Accessibility Compliance
**Business Value**: Ensure the app is usable by all family members regardless of ability, meeting legal requirements and demonstrating inclusive values.

### Epic 4: Component Library Standardization
**Business Value**: Reduce development time, ensure consistency, and improve maintainability while delivering a cohesive user experience.

### Epic 5: Voice & Tone Alignment (Website)
**Business Value**: Build emotional connection with target audience through empathetic, supportive communication that resonates with special needs families.

---

## Epic 1: Typography System Implementation

### STORY-001: Implement Atkinson Hyperlegible Font [P0]
**As a** parent with a child who has dyslexia or visual processing challenges
**I want** text that uses a font specifically designed for readability
**So that** my child and I can easily distinguish between similar-looking letters

**Acceptance Criteria**:
- [ ] Atkinson Hyperlegible font files downloaded and added to all platforms
- [ ] iOS: Font registered in `FontManager.swift` and Info.plist
- [ ] Android: Font loaded via Google Fonts provider in `Type.kt`
- [ ] Website: Google Fonts link added to `BaseLayout.astro`
- [ ] All text elements use Atkinson Hyperlegible (no Nunito references remain)
- [ ] Font weights available: Regular (400), Bold (700)
- [ ] Font renders correctly at all sizes (12px to 48px)
- [ ] Font licensing documented (free, open source)

**Scope**:
- IN: Font file integration, registration, basic weights
- OUT: Custom font variations, icon fonts, decorative fonts

**Files to Modify**:
- `/ios/SmilePile/FontManager.swift`
- `/ios/SmilePile/Info.plist`
- `/android/app/src/main/java/com/smilepile/ui/theme/Type.kt`
- `/website/src/layouts/BaseLayout.astro`
- `/website/tailwind.config.js`

**Dependencies**: None
**Effort**: Medium (8 hours)

---

### STORY-002: Align Typography Scale Across Platforms [P0]
**As a** developer implementing the design system
**I want** consistent typography sizes across all platforms
**So that** the user experience feels cohesive regardless of device

**Acceptance Criteria**:
- [ ] Typography scale matches design system: 48px, 36px, 28px, 22px, 18px, 16px, 14px
- [ ] iOS: `Typography.swift` updated with correct point sizes
- [ ] Android: `Type.kt` Material3 typography updated with correct sp values
- [ ] Website: `tailwind.config.js` fontSize scale configured
- [ ] Line heights set: H1 (1.1), H2 (1.2), Body (1.6), Button (1.0)
- [ ] Minimum body text size is 16px across all platforms
- [ ] Kids Mode typography is 20% larger than standard

**Scope**:
- IN: Standard typography scale, line heights, Kids Mode sizing
- OUT: Custom typography for special features, animated text

**Files to Modify**:
- `/ios/SmilePile/Typography.swift`
- `/android/app/src/main/java/com/smilepile/ui/theme/Type.kt`
- `/website/tailwind.config.js`

**Dependencies**: STORY-001
**Effort**: Small (4 hours)

---

## Epic 2: Color Palette Transformation

### STORY-003: Replace Brand Colors with Design System Palette [P0]
**As a** parent dealing with sensory overload
**I want** calming, scientifically-chosen colors
**So that** using the app doesn't add to my family's stress

**Acceptance Criteria**:
- [ ] Soft Blue (#7FB3D5) replaces bright blue (#2196F3)
- [ ] Sage Green (#A8D8B9) replaces bright green (#4CAF50)
- [ ] Lavender (#C9B3D6) replaces orange (#FF6600) as accent
- [ ] Warm Cream (#F8F3ED) replaces pure white (#FFFFFF) for backgrounds
- [ ] Soft Charcoal (#3A3A3A) replaces pure black (#000000) for text
- [ ] iOS: `ColorConstants.swift` updated with new hex values
- [ ] Android: `ComposeTheme.kt` lightColorScheme updated
- [ ] Website: `tailwind.config.js` and `global.css` updated
- [ ] All color references throughout codebase updated
- [ ] No bright/vibrant colors remain in production code

**Scope**:
- IN: Primary palette colors, systematic replacement
- OUT: Logo redesign, marketing materials, app store assets

**Files to Modify**:
- `/ios/SmilePile/Theme/ColorConstants.swift`
- `/android/app/src/main/java/com/smilepile/ui/theme/ComposeTheme.kt`
- `/website/tailwind.config.js`
- `/website/src/styles/global.css`
- 100+ view files with hardcoded colors

**Dependencies**: None
**Effort**: Large (20 hours)

---

### STORY-004: Implement Dark Mode with Design System Colors [P1]
**As a** parent using the app at night
**I want** a proper dark mode that maintains readability
**So that** I can browse photos without disturbing my sleeping child

**Acceptance Criteria**:
- [ ] Dark mode palette defined: Background (#1E1E1E), Surface (#2A2A2A), etc.
- [ ] iOS: Dark mode colors added to `ColorConstants.swift`
- [ ] Android: `DarkColorScheme` updated in `ComposeTheme.kt`
- [ ] Website: CSS variables support `prefers-color-scheme: dark`
- [ ] All text maintains 4.5:1 contrast ratio in dark mode
- [ ] Automatic switching based on system preference
- [ ] Manual toggle available in settings
- [ ] Smooth transition between modes (no jarring changes)

**Scope**:
- IN: System-level dark mode, automatic switching
- OUT: Custom themes, time-based switching, per-screen overrides

**Files to Modify**:
- `/ios/SmilePile/Theme/ColorConstants.swift`
- `/android/app/src/main/java/com/smilepile/ui/theme/ComposeTheme.kt`
- `/website/src/styles/global.css`
- Settings screens on all platforms

**Dependencies**: STORY-003
**Effort**: Medium (12 hours)

---

### STORY-005: Validate and Document Color Contrast Ratios [P0]
**As an** accessibility advocate
**I want** all color combinations to meet WCAG standards
**So that** users with visual impairments can use the app effectively

**Acceptance Criteria**:
- [ ] All text/background combinations tested with WebAIM Contrast Checker
- [ ] Normal text maintains 4.5:1 minimum contrast ratio
- [ ] Large text (24px+) maintains 3:1 minimum contrast ratio
- [ ] Interactive elements maintain 3:1 minimum contrast ratio
- [ ] Contrast ratios documented in code comments
- [ ] Failing combinations identified and fixed
- [ ] Automated contrast checking added to build process (stretch goal)
- [ ] Documentation created with approved color combinations

**Scope**:
- IN: WCAG 2.2 AA compliance, documentation
- OUT: AAA compliance (unless easily achievable), custom contrast tools

**Files to Modify**:
- Color constant files on all platforms
- New documentation file: `/docs/COLOR_CONTRAST_MATRIX.md`

**Dependencies**: STORY-003
**Effort**: Medium (8 hours)

---

## Epic 3: Accessibility Compliance

### STORY-006: Enforce Touch Target Minimums [P0]
**As a** parent with motor control challenges
**I want** buttons and interactive elements that are easy to tap
**So that** I don't accidentally trigger wrong actions

**Acceptance Criteria**:
- [ ] iOS: All interactive elements minimum 44x44 points
- [ ] Android: All interactive elements minimum 48x48 dp
- [ ] Website: All clickable elements minimum 48x48 pixels
- [ ] Spacing between targets minimum 8px
- [ ] Visual size may be smaller with transparent padding
- [ ] Touch target areas visually indicated on hover/focus
- [ ] No overlapping touch targets
- [ ] Kids Mode maintains even larger targets (60x60 minimum)

**Scope**:
- IN: Buttons, links, form controls, navigation items
- OUT: Decorative elements, non-interactive content

**Files to Modify**:
- All button components across platforms
- Navigation components
- Form elements
- Photo grid items

**Dependencies**: None
**Effort**: Large (16 hours)

---

### STORY-007: Implement Visible Focus Indicators [P0]
**As a** keyboard-only user
**I want** clear visual indicators showing which element has focus
**So that** I know where I am and what will activate when I press Enter

**Acceptance Criteria**:
- [ ] Focus indicator: 3px solid Soft Blue (#7FB3D5) outline
- [ ] Focus indicator: 2px offset from element edge
- [ ] iOS: Custom focus styles implemented in SwiftUI
- [ ] Android: Material focus states overridden with design system colors
- [ ] Website: `:focus-visible` pseudo-class styled
- [ ] Focus indicators visible in both light and dark modes
- [ ] Focus never hidden or removed (no `outline: none` without replacement)
- [ ] Tab order follows logical visual flow

**Scope**:
- IN: Keyboard navigation focus, programmatic focus
- OUT: Custom focus animations, focus trapping (except modals)

**Files to Modify**:
- `/ios/SmilePile/Theme/ColorConstants.swift` (focus color definition)
- `/android/app/src/main/java/com/smilepile/ui/theme/ComposeTheme.kt`
- `/website/src/styles/global.css`
- Component files needing focus state overrides

**Dependencies**: STORY-003
**Effort**: Medium (10 hours)

---

### STORY-008: Add Reduced Motion Support [P1]
**As a** user with vestibular disorders
**I want** to disable animations that might trigger discomfort
**So that** I can use the app without experiencing motion sickness

**Acceptance Criteria**:
- [ ] iOS: Respects `UIAccessibility.isReduceMotionEnabled`
- [ ] Android: Checks `Settings.Global.ANIMATOR_DURATION_SCALE`
- [ ] Website: CSS `@media (prefers-reduced-motion: reduce)` implemented
- [ ] When enabled: All animations duration set to 0.01ms
- [ ] When enabled: All transitions instant or near-instant
- [ ] When enabled: Parallax effects disabled
- [ ] When enabled: Auto-playing content stopped
- [ ] Setting persists across app sessions

**Scope**:
- IN: System-level preference respect, animation disabling
- OUT: Custom animation preferences, partial motion reduction

**Files to Modify**:
- `/website/src/styles/global.css`
- Animation/transition files on all platforms
- View models that control animations

**Dependencies**: None
**Effort**: Small (6 hours)

---

### STORY-009: Enhance Screen Reader Support [P0]
**As a** blind or low-vision user
**I want** comprehensive screen reader announcements
**So that** I can navigate and use all app features independently

**Acceptance Criteria**:
- [ ] All images have descriptive alt text (not filename)
- [ ] Icon buttons have aria-label/accessibilityLabel
- [ ] Form inputs have associated labels (not placeholder only)
- [ ] Error messages announced when they appear
- [ ] Success messages announced when actions complete
- [ ] Heading hierarchy correct (H1 → H2 → H3, no skipping)
- [ ] Landmark regions properly defined
- [ ] iOS: VoiceOver tested on all screens
- [ ] Android: TalkBack tested on all screens
- [ ] Website: NVDA/JAWS tested on all pages

**Scope**:
- IN: Native platform screen readers, semantic markup
- OUT: Custom screen reader features, voice control

**Files to Modify**:
- All view files with images
- All form components
- All icon-only buttons
- Navigation components

**Dependencies**: None
**Effort**: Medium (12 hours)

---

## Epic 4: Component Library Standardization

### STORY-010: Create Button Component Library [P1]
**As a** developer
**I want** standardized button components
**So that** UI remains consistent and maintainable

**Acceptance Criteria**:
- [ ] Primary Button: Soft Blue background, Warm Cream text, 48px height
- [ ] Secondary Button: Soft Blue border, transparent background
- [ ] Text Button: No border, underline on hover
- [ ] Disabled state: 50% opacity, no interaction
- [ ] Loading state: Spinner icon, disabled interaction
- [ ] iOS: `PrimaryButton.swift`, `SecondaryButton.swift` created
- [ ] Android: Custom Button composables created
- [ ] Website: `Button.astro` component with variants
- [ ] All existing buttons migrated to use new components
- [ ] Documentation with usage examples

**Scope**:
- IN: Standard button types, common states
- OUT: Floating action buttons, toggle buttons, button groups

**Files to Create**:
- `/ios/SmilePile/Views/Components/Buttons/PrimaryButton.swift`
- `/ios/SmilePile/Views/Components/Buttons/SecondaryButton.swift`
- `/android/app/src/main/java/com/smilepile/ui/components/Buttons.kt`
- `/website/src/components/Button.astro`

**Files to Modify**:
- 75+ files with button implementations

**Dependencies**: STORY-003, STORY-006, STORY-007
**Effort**: Large (20 hours)

---

### STORY-011: Create Card Component Library [P2]
**As a** user browsing content
**I want** consistent card layouts
**So that** information is predictable and easy to scan

**Acceptance Criteria**:
- [ ] Standard Card: White background, 1px border, 12px radius, subtle shadow
- [ ] Photo Card: Image top, title below, metadata footer
- [ ] Info Card: Icon left, content right, action button
- [ ] Hover state: Elevated shadow, no color change
- [ ] Focus state: Soft Blue outline as per STORY-007
- [ ] iOS: Card view components created
- [ ] Android: Card composables created
- [ ] Website: Card.astro component created
- [ ] Existing ad-hoc cards migrated

**Scope**:
- IN: Basic card layouts, photo cards
- OUT: Flip cards, expandable cards, carousel cards

**Files to Create**:
- `/ios/SmilePile/Views/Components/Cards/`
- `/android/app/src/main/java/com/smilepile/ui/components/Cards.kt`
- `/website/src/components/Card.astro`

**Dependencies**: STORY-003
**Effort**: Medium (12 hours)

---

### STORY-012: Create Form Component Library [P2]
**As a** user entering information
**I want** clear, accessible form fields
**So that** I can provide input without confusion

**Acceptance Criteria**:
- [ ] Text Input: Visible label, 48px height, Soft Blue focus border
- [ ] Select Dropdown: Native styling with design system colors
- [ ] Checkbox/Radio: 24x24px minimum, clear selected state
- [ ] Error State: Red border, error icon, error message below
- [ ] Success State: Sage Green border, checkmark icon
- [ ] Required fields marked with * (and aria-required)
- [ ] Help text supported below fields
- [ ] iOS: Form component views created
- [ ] Android: Form composables created
- [ ] Website: Form components created

**Scope**:
- IN: Basic form inputs, validation states
- OUT: Complex inputs (date pickers, sliders), multi-step forms

**Files to Create**:
- `/ios/SmilePile/Views/Components/Forms/`
- `/android/app/src/main/java/com/smilepile/ui/components/Forms.kt`
- `/website/src/components/FormInput.astro`
- `/website/src/components/FormSelect.astro`

**Dependencies**: STORY-003, STORY-006, STORY-007
**Effort**: Large (18 hours)

---

## Epic 5: Voice & Tone Alignment (Website)

### STORY-013: Rewrite Homepage with Supportive Voice [P1]
**As a** parent of a special needs child visiting the website
**I want** to feel understood and supported
**So that** I trust this app understands my family's journey

**Acceptance Criteria**:
- [ ] Hero headline: "Celebrate every smile, every milestone, every moment"
- [ ] Subheading emphasizes emotional value over features
- [ ] Feature descriptions focus on benefits, not specifications
- [ ] Call-to-action uses warm, inviting language
- [ ] Person-first language throughout ("child with autism" not "autistic child")
- [ ] No medical jargon or corporate speak
- [ ] Parent testimonials feel authentic, not polished
- [ ] Footer includes supportive resources

**Scope**:
- IN: Homepage copy, meta descriptions
- OUT: App store descriptions, email templates

**Files to Modify**:
- `/website/src/pages/index.astro`
- `/website/src/components/Hero.astro`
- `/website/src/components/Features.astro`

**Dependencies**: None
**Effort**: Medium (8 hours)

---

### STORY-014: Rewrite Support Pages with Patient Tone [P2]
**As a** frustrated parent needing help
**I want** clear, patient support documentation
**So that** I can solve problems without added stress

**Acceptance Criteria**:
- [ ] Step-by-step instructions with screenshots
- [ ] Common issues addressed first
- [ ] Language assumes no technical knowledge
- [ ] Troubleshooting written with empathy
- [ ] "We're here to help" tone throughout
- [ ] Contact options prominent and welcoming
- [ ] FAQ answers are conversational, not robotic
- [ ] Response to "nothing works" scenarios included

**Scope**:
- IN: Support pages, FAQ, contact page
- OUT: Technical documentation, API docs

**Files to Modify**:
- `/website/src/pages/support.astro`
- `/website/src/pages/faq.astro`
- `/website/src/pages/contact.astro`

**Dependencies**: None
**Effort**: Small (6 hours)

---

### STORY-015: Create Accessible Error Messages [P1]
**As a** user who encounters an error
**I want** helpful, non-technical error messages
**So that** I understand what went wrong and how to fix it

**Acceptance Criteria**:
- [ ] Error messages written in plain language
- [ ] Specific about what went wrong
- [ ] Includes suggestion for resolution
- [ ] Maintains supportive tone (not blaming)
- [ ] Icon indicates error type visually
- [ ] Screen reader announces errors immediately
- [ ] Examples:
  - Bad: "Error 403: Forbidden"
  - Good: "This photo album is private. Try logging in first."

**Scope**:
- IN: User-facing error messages across all platforms
- OUT: Developer console errors, system logs

**Files to Modify**:
- Error handling in ViewModels/Presenters
- Error display components
- Network error handlers

**Dependencies**: STORY-009
**Effort**: Medium (8 hours)

---

## Implementation Phases and Priority Matrix

### Phase 1: Critical Foundation (Week 1-2)
**Must complete before any other work**

| Story | Priority | Dependencies | Effort | Platforms |
|-------|----------|--------------|--------|-----------|
| STORY-001 | P0 | None | 8h | All |
| STORY-002 | P0 | STORY-001 | 4h | All |
| STORY-003 | P0 | None | 20h | All |
| STORY-005 | P0 | STORY-003 | 8h | All |

**Total Phase 1**: 40 hours

### Phase 2: Accessibility Core (Week 3-4)
**Legal compliance and basic accessibility**

| Story | Priority | Dependencies | Effort | Platforms |
|-------|----------|--------------|--------|-----------|
| STORY-006 | P0 | None | 16h | All |
| STORY-007 | P0 | STORY-003 | 10h | All |
| STORY-009 | P0 | None | 12h | All |

**Total Phase 2**: 38 hours

### Phase 3: Enhanced Experience (Week 5-6)
**Dark mode and motion preferences**

| Story | Priority | Dependencies | Effort | Platforms |
|-------|----------|--------------|--------|-----------|
| STORY-004 | P1 | STORY-003 | 12h | All |
| STORY-008 | P1 | None | 6h | All |
| STORY-015 | P1 | STORY-009 | 8h | All |

**Total Phase 3**: 26 hours

### Phase 4: Component Standardization (Week 6-7)
**Reusable component library**

| Story | Priority | Dependencies | Effort | Platforms |
|-------|----------|--------------|--------|-----------|
| STORY-010 | P1 | STORY-003,006,007 | 20h | All |
| STORY-011 | P2 | STORY-003 | 12h | All |
| STORY-012 | P2 | STORY-003,006,007 | 18h | All |

**Total Phase 4**: 50 hours

### Phase 5: Voice & Tone (Week 7-8)
**Website content alignment**

| Story | Priority | Dependencies | Effort | Platforms |
|-------|----------|--------------|--------|-----------|
| STORY-013 | P1 | None | 8h | Website |
| STORY-014 | P2 | None | 6h | Website |

**Total Phase 5**: 14 hours

---

## Success Metrics

### Quantitative Metrics
- [ ] **100%** WCAG 2.2 AA compliance (0 violations in automated testing)
- [ ] **100%** of interactive elements meet touch target minimums
- [ ] **100%** of color combinations pass contrast requirements
- [ ] **0** instances of Nunito font in codebase
- [ ] **0** instances of bright brand colors in UI
- [ ] **15+** standardized components created
- [ ] **268** files successfully migrated

### Qualitative Metrics
- [ ] User feedback indicates reduced eye strain
- [ ] Parents report feeling understood by website copy
- [ ] Accessibility testing with real users shows improved navigation
- [ ] Development team reports faster feature implementation with component library
- [ ] App store reviews mention accessibility positively

### Accessibility Metrics
- [ ] VoiceOver/TalkBack navigation success rate: >95%
- [ ] Keyboard-only navigation possible on 100% of features
- [ ] Screen reader announcement clarity rated >4/5 by testers
- [ ] Color blind users can distinguish all UI states
- [ ] Motion-sensitive users report no discomfort

---

## Risk Assessment and Mitigation

### High Risk Items

**Risk**: Existing users confused by color changes
**Mitigation**:
- Gradual rollout with announcement
- "What's New" onboarding screen
- Keep navigation patterns identical
- Maintain logo recognition elements

**Risk**: Font loading performance impact
**Mitigation**:
- Use WOFF2 compression
- Implement font-display: swap
- Subset fonts to used characters
- Fallback fonts defined

**Risk**: Touch target changes break muscle memory
**Mitigation**:
- Only increase sizes, never decrease
- Test with existing users
- Visual indicators for tap areas
- Gradual rollout if needed

### Medium Risk Items

**Risk**: Dark mode color combinations fail contrast
**Mitigation**:
- Test all combinations before implementation
- Use slightly brighter colors in dark mode
- Maintain 4.5:1 minimum always

**Risk**: Component migration introduces bugs
**Mitigation**:
- Feature flag for gradual rollout
- Comprehensive testing per component
- Keep old components available temporarily
- A/B testing on critical flows

---

## Scope Boundaries

### Explicitly IN Scope
- Atkinson Hyperlegible font implementation
- Design system color palette (soft/muted)
- WCAG 2.2 AA compliance
- Touch target minimums
- Focus indicators
- Screen reader support
- Dark mode support
- Reduced motion support
- Button, Card, Form component libraries
- Website voice/tone rewrite
- Error message improvement

### Explicitly OUT of Scope
- Logo redesign
- App icon changes
- Marketing materials update
- Email templates
- Push notification styling
- Third-party SDK theming
- Custom animation library
- Advanced accessibility (voice control, eye tracking)
- Internationalization/localization
- Right-to-left language support
- Custom themes beyond light/dark
- Color blind specific modes

### Requires Stakeholder Decision
1. **Logo colors**: Keep current bright colors or adapt to soft palette?
2. **Rollout strategy**: Big bang or gradual feature flags?
3. **Kids Mode**: Even larger changes or maintain current sizing?
4. **Category colors**: Keep current 8 colors or reduce to design system palette?
5. **Onboarding**: Create new onboarding for design changes?

---

## Definition of Done

Each story is considered complete when:

1. **Code Complete**
   - All acceptance criteria met
   - No references to old system remain
   - Code follows SmilePile conventions (no emoji, no unrequested features)
   - Design system values used (no hardcoded colors/fonts)

2. **Testing Complete**
   - Automated tests pass
   - Manual testing on all platforms
   - Accessibility testing with tools
   - Visual regression testing passed
   - Cross-browser testing (website)

3. **Documentation Complete**
   - Component usage documented
   - Accessibility notes included
   - Migration guide for developers
   - Known limitations documented

4. **Review Complete**
   - Code review passed
   - Design review passed
   - Accessibility review passed
   - Product owner approval

---

## Team Allocation Recommendation

### Optimal Team Structure
- **1 iOS Developer** (Full-time, 8 weeks)
- **1 Android Developer** (Full-time, 8 weeks)
- **1 Web Developer** (Full-time, 6 weeks)
- **1 QA/Accessibility Specialist** (Part-time, weeks 2-8)
- **1 UX Writer** (Part-time, weeks 6-8)
- **1 Product Manager** (Part-time, throughout)

### Minimum Viable Team
- **1 Full-Stack Developer** (Can work across platforms)
- **1 QA Tester** (With accessibility experience)
- **1 Product Manager** (For decisions and validation)

Time with minimum team: ~12-16 weeks

---

## Appendix A: Technical Specifications

### Font Specifications
```css
/* Atkinson Hyperlegible */
font-family: 'Atkinson Hyperlegible', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
font-weight: 400 | 700;
font-display: swap;
```

### Color Specifications
```css
/* Light Mode */
--color-primary: #7FB3D5;     /* Soft Blue */
--color-secondary: #A8D8B9;   /* Sage Green */
--color-accent: #C9B3D6;      /* Lavender */
--color-background: #F8F3ED;  /* Warm Cream */
--color-text: #3A3A3A;        /* Soft Charcoal */

/* Dark Mode */
--color-primary-dark: #A8CEEA;
--color-secondary-dark: #C1E8CF;
--color-accent-dark: #D8C5E5;
--color-background-dark: #1E1E1E;
--color-text-dark: #F8F3ED;
```

### Component Specifications
```css
/* Primary Button */
.btn-primary {
  background: var(--color-primary);
  color: var(--color-background);
  height: 48px;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 700;
  font-size: 16px;
}

/* Focus State */
:focus-visible {
  outline: 3px solid var(--color-primary);
  outline-offset: 2px;
}

/* Touch Target */
.interactive {
  min-height: 48px;
  min-width: 48px;
}
```

---

## Appendix B: File Impact Analysis

### iOS Files (129 files)
- Core files: 4 (ColorConstants, FontManager, Typography, Info.plist)
- View files: ~75 (buttons, colors, text)
- Component files: ~20 (need updates)
- New components: ~10 files

### Android Files (109 files)
- Core files: 2 (ComposeTheme, Type)
- Screen files: ~16
- Component files: ~75
- New components: ~8 files

### Website Files (30 files)
- Config files: 2 (tailwind.config, global.css)
- Page files: 5
- Component files: 7 (existing)
- New components: 10-15 files

---

## Conclusion

This comprehensive user story document provides:
- **15 detailed user stories** with clear acceptance criteria
- **5 epics** organizing the work logically
- **Clear priorities** (P0 through P2)
- **Explicit scope boundaries**
- **Success metrics** both quantitative and qualitative
- **Risk mitigation strategies**
- **Team allocation recommendations**
- **Technical specifications** for implementation

The implementation will require approximately **216 hours** of development effort across **268 files**, achievable in **8 weeks** with a dedicated team of 3-4 people.

**Next Step**: Phase 3 - Technical Planning with developer agent to create detailed implementation plans for each story.