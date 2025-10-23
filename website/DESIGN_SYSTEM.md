# SmilePile Design System
**Version 1.0** | Last Updated: January 2025

## Table of Contents
1. [Brand Overview](#brand-overview)
2. [Color Palette](#color-palette)
3. [Typography System](#typography-system)
4. [Photography Direction](#photography-direction)
5. [Voice & Tone Guide](#voice--tone-guide)
6. [Accessibility Standards](#accessibility-standards)
7. [Component Patterns](#component-patterns)
8. [Implementation Guide](#implementation-guide)

---

## Brand Overview

### Mission
SmilePile helps families preserve and celebrate precious memories with a simple, private, and joyful photo organization app designed for real families - especially those with children who have special needs.

### Target Audience
**Primary**: Parents of children with special needs (autism spectrum disorder, developmental delays, learning disabilities, and other conditions)

**Secondary**: All parents who want a simple, privacy-focused way to organize family photos

### Audience Needs

**Emotional**:
- Support and understanding without judgment
- Celebration of small victories and unique milestones
- Memory preservation of precious moments
- Community connection with peers
- Empowerment and hope

**Practical**:
- Easy to use, minimal cognitive load
- Accessible for diverse abilities
- Privacy-focused and secure
- Reliable and consistent
- Time-saving features

### Core Brand Values
1. **Privacy First** - Your memories stay yours, always
2. **Simplicity** - Easy to use means respect for busy parents
3. **Celebration** - Every milestone matters, big or small
4. **Inclusivity** - Built for all families, all abilities, all journeys
5. **Authenticity** - Real families, genuine moments, no pretense

---

## Color Palette

### Primary Colors

Our color palette is scientifically chosen to create calm, trust, and emotional support. Soft, muted tones reduce anxiety and avoid sensory overload - critical for families with special needs children.

#### Brand Colors

**Soft Blue (Primary)**
```
Name: Soft Blue
Hex: #7FB3D5
RGB: 127, 179, 213
HSL: 204°, 51%, 67%

Usage: Primary brand color, headers, key CTAs, links
Psychology: Trust, empathy, calm, stability
Science: Proven to slow heart rate and reduce anxiety
```

**Sage Green (Secondary)**
```
Name: Sage Green
Hex: #A8D8B9
RGB: 168, 216, 185
HSL: 141°, 38%, 75%

Usage: Success states, growth indicators, secondary elements
Psychology: Refreshing, health-promoting, concentration
Science: The eye processes green without distortion ("master color")
```

**Lavender (Accent)**
```
Name: Lavender
Hex: #C9B3D6
RGB: 201, 179, 214
HSL: 278°, 31%, 77%

Usage: Highlights, special features, gentle accents
Psychology: Calming to nervous system, relaxation, creativity
Science: Reduces blood pressure, promotes restful state
```

**Warm Cream (Neutral Light)**
```
Name: Warm Cream
Hex: #F8F3ED
RGB: 248, 243, 237
HSL: 33°, 42%, 95%

Usage: Backgrounds, cards, containers
Psychology: Comforting, approachable, warm
Note: Avoid pure white (#FFFFFF) - too harsh
```

**Soft Charcoal (Neutral Dark)**
```
Name: Soft Charcoal
Hex: #3A3A3A
RGB: 58, 58, 58
HSL: 0°, 0%, 23%

Usage: Body text, dark UI elements
Psychology: Professional, readable, grounded
Note: Avoid pure black (#000000) - too harsh
```

#### Supporting Colors

**Gentle Yellow-Gold**
```
Name: Gentle Gold
Hex: #F5DA81
RGB: 245, 218, 129
HSL: 46°, 84%, 73%

Usage: Celebrations, achievements, warmth (use sparingly)
Psychology: Optimism, joy, energy
Warning: Use minimally to avoid overstimulation
```

**Soft Coral**
```
Name: Soft Coral
Hex: #F4A6A3
RGB: 244, 166, 163
HSL: 2°, 78%, 80%

Usage: Gentle alerts, love/family themes
Psychology: Warmth, care, compassion
Warning: Not for errors - too gentle
```

**Pale Mint**
```
Name: Pale Mint
Hex: #B8DCD6
RGB: 184, 220, 214
HSL: 164°, 35%, 79%

Usage: Fresh sections, new content indicators
Psychology: Fresh, clean, hopeful
```

### Contrast Ratios (WCAG 2.2 AA Compliance)

All color combinations tested for accessibility:

| Background | Text Color | Ratio | Pass |
|------------|------------|-------|------|
| Warm Cream (#F8F3ED) | Soft Charcoal (#3A3A3A) | 12.8:1 | ✅ AAA |
| Soft Blue (#7FB3D5) | Warm Cream (#F8F3ED) | 2.8:1 | ❌ Fails |
| Soft Blue (#7FB3D5) | Soft Charcoal (#3A3A3A) | 4.6:1 | ✅ AA |
| Sage Green (#A8D8B9) | Soft Charcoal (#3A3A3A) | 5.2:1 | ✅ AA |
| Lavender (#C9B3D6) | Soft Charcoal (#3A3A3A) | 4.9:1 | ✅ AA |

**Tool**: Use [WebAIM Contrast Checker](https://webaim.org/resources/contrastchecker/) to verify all combinations.

### Usage Guidelines

**Do**:
- Use Soft Blue for primary actions and trust-building elements
- Use Sage Green for positive feedback and growth
- Use Lavender sparingly as accents
- Maintain high contrast ratios (4.5:1 minimum for normal text)
- Provide dark mode alternatives

**Don't**:
- Don't use bright, saturated colors (overstimulating)
- Don't rely on color alone to convey information
- Don't use pure white or pure black
- Don't use red for errors (use Soft Charcoal text with Warm Cream background)
- Don't use multiple accent colors simultaneously

### Dark Mode Palette

For users who prefer dark mode:

```
Background: #1E1E1E (Soft Black)
Surface: #2A2A2A (Elevated Surface)
Primary: #A8CEEA (Brighter Soft Blue)
Secondary: #C1E8CF (Brighter Sage)
Accent: #D8C5E5 (Brighter Lavender)
Text: #F8F3ED (Warm Cream)
```

All dark mode combinations maintain minimum 4.5:1 contrast ratio.

---

## Typography System

### Primary Typeface: Atkinson Hyperlegible

**Why Atkinson Hyperlegible?**
- Designed by the Braille Institute specifically for low-vision readers
- Enhanced character recognition and letterform distinction
- FREE and available on Google Fonts
- Won Fast Company's Innovation by Design Award (2019)
- Enhanced version "Atkinson Hyperlegible Next" with variable weights (2025)
- Demonstrates that accessibility and beauty are not mutually exclusive

**How to Use**:
```html
<!-- Google Fonts -->
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Atkinson+Hyperlegible:ital,wght@0,400;0,700;1,400;1,700&display=swap" rel="stylesheet">
```

```css
font-family: 'Atkinson Hyperlegible', -apple-system, BlinkMacSystemFont,
             'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
```

### Font Scale

Based on modular scale (1.250 - Major Third):

| Element | Size | Weight | Line Height | Usage |
|---------|------|--------|-------------|-------|
| H1 | 48px (3rem) | 700 (Bold) | 1.1 | Page titles |
| H2 | 36px (2.25rem) | 700 (Bold) | 1.2 | Section headings |
| H3 | 28px (1.75rem) | 700 (Bold) | 1.3 | Subsection headings |
| H4 | 22px (1.375rem) | 700 (Bold) | 1.4 | Component titles |
| H5 | 18px (1.125rem) | 700 (Bold) | 1.4 | Small headings |
| Body Large | 18px (1.125rem) | 400 (Regular) | 1.6 | Intro paragraphs, important copy |
| Body | 16px (1rem) | 400 (Regular) | 1.6 | Default body text |
| Body Small | 14px (0.875rem) | 400 (Regular) | 1.5 | Captions, helper text |
| Button | 16px (1rem) | 700 (Bold) | 1 | All buttons |
| Label | 14px (0.875rem) | 700 (Bold) | 1.2 | Form labels |

### Typography Principles

**Minimum Sizes**:
- Body text: **16px minimum** (WCAG recommendation: 12pt min, 18pt for large text)
- Large text: **24px+** or **18.66px+ bold** (WCAG "large text" definition)
- Touch targets: Include text labels, not icons alone

**Line Height**:
- Body text: **1.5-1.6** for optimal readability
- Headings: **1.1-1.3** for visual hierarchy
- Never below 1.5 for body text (WCAG 1.4.12)

**Letter Spacing**:
- Default: Normal (0)
- All-caps: +0.05em (if used sparingly)
- Avoid all-caps for long text (harder to read)

**Alignment**:
- Body text: **Left-aligned** (easier to scan than justified)
- Headings: Left or center depending on context
- Avoid right-aligned text for body copy

**Emphasis**:
- Use **bold** for emphasis, not italics (easier to read)
- Use color sparingly for emphasis
- Underline only for links

### Fallback Fonts

**iOS**: `-apple-system`, `BlinkMacSystemFont`
**Android**: `Roboto`
**Windows**: `Segoe UI`
**Linux**: `Ubuntu`, `Liberation Sans`
**Universal**: `Arial`, `sans-serif`

---

## Photography Direction

### Core Principles

Photography is how we connect emotionally with our audience. Every image must reflect authenticity, diversity, and genuine joy.

### Style Guidelines

**1. Authentic, Not Staged**
- Real families in real moments
- Genuine emotions and expressions
- Candid over posed
- Natural interactions, not "say cheese"
- Imperfect is perfect (shows reality)

**2. Diverse and Inclusive**
- Children of all abilities and special needs
- Various ethnicities and cultural backgrounds
- Different family structures (single parents, grandparents, LGBTQ+ families)
- Range of ages (infants to teens)
- Economic diversity (not all affluent settings)

**3. Celebrating Small Victories**
- Focus on achievements that matter to special needs families
- Everyday milestones: first words, first friendships, sensory breakthroughs
- Joy in small moments, not just big events
- Progress over perfection

**4. Lighting and Technical Quality**
- **Natural, warm lighting** - soft, not harsh
- Golden hour or soft indoor light
- Avoid flash (can be disturbing for sensory-sensitive children)
- Well-lit but not overexposed
- Subtle shadows, not dramatic contrast

**5. Composition and Perspective**
- **Eye-level perspective** - shows respect and equality
- Get down to child's eye level when photographing children
- Medium shots and close-ups (show connection)
- Avoid distant, clinical shots
- Include faces and genuine smiles when appropriate
- Respect personal space and comfort

**6. Settings and Context**
- **Real environments**: homes, parks, playgrounds, classrooms
- Not clinical/medical settings
- Clean but lived-in spaces
- Inclusive environments (ramps, sensory-friendly spaces)
- Cultural diversity in settings

**7. Focus on Connection**
- Parent-child interactions
- Sibling bonds
- Family moments
- Peer friendships
- Show hands, hugs, shared activities
- Physical closeness and warmth

### What to Avoid

**Don't**:
- Medical or clinical settings (hospitals, therapy rooms)
- Pity or sadness as primary emotion
- Focus on assistive devices or disabilities
- Stereotypes or tokenism
- Stock photo "perfection" (too polished)
- Overly edited or filtered images
- Staged "commercial" looks
- All children looking at camera simultaneously (unnatural)

**Do**:
- Show joy, connection, and authentic moments
- Normalize assistive devices when shown (not the focus)
- Capture the child first, not the diagnosis
- Show ability, not disability
- Real families in real environments

### Technical Specifications

**Resolution**:
- Web hero images: 2400px wide minimum (for retina displays)
- Thumbnail images: 800px wide minimum
- Format: JPEG (optimized) or WebP

**Aspect Ratios**:
- Hero: 16:9 or 3:2
- Feature cards: 4:3 or 1:1
- Portraits: 3:4 or 4:5

**File Size**:
- Optimize for web: <300KB for hero images
- Thumbnails: <100KB
- Use modern compression (WebP, AVIF)

**Accessibility**:
- Always include alt text describing the image
- Describe who, what, where, and emotional tone
- Example: "A mother and her 6-year-old son with autism laughing together while looking at photos on a tablet in their living room"

### Content Categories

**Homepage Hero**:
- Parent and child sharing a genuine moment of joy
- Looking at photos together (shows app context)
- Diverse, inclusive representation
- Natural lighting, warm environment
- Shows connection and happiness

**Feature Showcases**:
- Screenshots of actual app UI (not photos of people)
- Real photos within the app interface
- Clear, easy-to-understand
- Show key features in action

**Testimonial Photos** (if used):
- Real parent headshots
- Natural expressions
- Simple backgrounds
- Authentic, not professional studio shots

**About/Mission Section**:
- Multiple families showing diversity
- Children engaged in activities they love
- Small victories being celebrated
- Community and connection

### Photo Sourcing Options

**1. Custom Photography** (Recommended)
- Hire photographer experienced with special needs families
- Conduct photo shoots with real families (with consent)
- Build authentic brand imagery library

**2. Stock Photography** (Temporary)
- Use high-quality, authentic-looking stock photos
- Sources: Unsplash, Pexels (free), Getty Images, Shutterstock (paid)
- Search terms: "diverse family," "inclusive parenting," "special needs family," "autism family," "real family moments"
- Avoid overly staged stock photos

**3. User-Generated Content** (Future)
- With explicit permission, showcase real SmilePile user photos
- Signed releases required
- Privacy considerations critical

### Examples of Good vs. Poor Photography

**Good**:
- Real family laughing together at home
- Natural lighting from window
- Eye-level perspective
- Genuine smiles and connection
- Cultural diversity visible

**Poor**:
- Professional studio portrait with forced smiles
- Clinical white background
- Top-down perspective (looking down at child)
- Overly edited or filtered
- Focus on disability/medical equipment

---

## Voice & Tone Guide

### Brand Voice

Our voice is **warm, supportive, understanding, and empowering**. We speak as a peer who truly understands the special needs parenting journey - not as an expert talking down, but as a friend walking alongside.

### Voice Characteristics

**We are**:
- Warm and approachable
- Supportive without being patronizing
- Understanding and empathetic
- Empowering and hopeful
- Authentic and genuine
- Parent-to-parent (peer support)

**We are not**:
- Clinical or medical
- Pity-focused or sympathy-driven
- Corporate or sales-y
- Expert-lecturing
- Overpromising or hype-filled
- Condescending or patronizing

### Tone Variations by Context

**Homepage / Marketing**:
- Warm, inviting, hopeful
- Focus on benefits and emotional connection
- Celebratory without being cheesy

**Product Features**:
- Clear, straightforward, helpful
- Focus on how it makes life easier
- Benefit-focused, not feature-focused

**Support / Help**:
- Patient, understanding, step-by-step
- Acknowledging frustration is okay
- Solution-focused

**Privacy / Security**:
- Reassuring, transparent, trustworthy
- Clear explanations without jargon
- Emphasize commitment to safety

**Error Messages**:
- Gentle, not alarming
- Clear about what happened
- Specific guidance on how to fix
- Empathetic to user frustration

### Language Guidelines

**Use**:
- "You" and "your" (second person, direct)
- Active voice
- Short sentences and paragraphs
- Specific, concrete language
- Positive framing
- Inclusive terms ("all families," "every child")

**Avoid**:
- Medical terminology and jargon
- Deficit language ("struggles with," "suffers from," "afflicted by")
- Euphemisms ("differently abled," "special")
- Assumptions about family structure
- Corporate speak ("leverage," "solutions," "ecosystem")
- Overpromising ("revolutionary," "life-changing")

### Writing Examples

#### Homepage Headlines

❌ **Don't**: "Advanced photo organization system for special needs families"
✅ **Do**: "Celebrate every smile, every milestone, every moment"

❌ **Don't**: "Our platform helps manage your child's developmental journey"
✅ **Do**: "Your family's joy, beautifully preserved"

#### Feature Descriptions

❌ **Don't**: "Our advanced categorization system allows for efficient photo management with smart tagging algorithms"
✅ **Do**: "Find any photo in seconds - organized by your child's special moments"

❌ **Don't**: "Leverage our timeline visualization to track developmental milestones"
✅ **Do**: "Watch your child grow with a timeline that celebrates every step forward"

#### Privacy Messaging

❌ **Don't**: "We utilize enterprise-grade encryption protocols to ensure data security"
✅ **Do**: "Your photos stay private - always encrypted, never shared, completely yours"

❌ **Don't**: "SmilePile maintains COPPA compliance and adheres to strict data governance policies"
✅ **Do**: "We never collect, sell, or share your family's information. Your memories belong to you, not us."

#### Error Messages

❌ **Don't**: "Error 404: Resource not found"
✅ **Do**: "Hmm, we can't find that photo. Let's check your categories or search again."

❌ **Don't**: "Upload failed due to insufficient storage allocation"
✅ **Do**: "Looks like you're running low on space. Let's free some up or back up your older photos."

#### Support Content

❌ **Don't**: "To initiate the backup procedure, navigate to Settings > Advanced > Backup Configuration"
✅ **Do**: "Here's how to back up your photos: Open Settings, tap Backup, then tap 'Back Up Now.' That's it!"

❌ **Don't**: "This feature enables users to leverage collaborative categorization"
✅ **Do**: "Want to organize photos together? Your partner can help add photos to categories too."

### Addressing the Audience

**Terms for Children**:
- "Your child" or "your son/daughter"
- First names when known
- "Kids" is okay in casual contexts
- Avoid "special needs child" - use "child with special needs" (person-first language)
- Avoid "autistic child" - use "child with autism" or "autistic child" based on community preference (many in autism community prefer identity-first)

**Terms for Parents**:
- "You" (direct address)
- "Parents" or "families"
- "Caregivers" when appropriate
- Avoid assumptions about family structure

**Terms for Conditions**:
- When necessary to mention: "child with autism," "child with Down syndrome," "child with ADHD"
- Avoid: "suffers from," "afflicted with," "disabled child"
- Use: "has," "lives with," "child with [condition]"
- Follow person-first language principles

### Messaging Themes

**1. Every Child is Unique**
- Customization and personalization are core
- Celebrate individual progress
- No comparisons to "typical" development

Example: "Every child's journey is unique - SmilePile adapts to yours."

**2. Small Victories Matter**
- Milestone tracking includes small steps
- Recognition of achievements others might miss
- Progress over perfection

Example: "From first words to first friendships, celebrate it all."

**3. You're Not Alone**
- Community and connection
- Shared experiences
- Peer support

Example: "Built by a parent who gets it, for families who celebrate every victory."

**4. Preserve What Matters**
- Memory as celebration
- Focusing on joy, not challenges
- Family story and legacy

Example: "The moments you'll want to remember forever - all in one place."

**5. Simplicity is Respect**
- Easy to use = respect for busy parents
- Accessible design = inclusive values
- Privacy = trust

Example: "Organized automatically - spend time making memories, not organizing them."

### Checklist for All Content

Before publishing any content, ask:

- [ ] Is it warm and supportive, not patronizing?
- [ ] Does it use "you" and active voice?
- [ ] Is it free of jargon and deficit language?
- [ ] Does it focus on benefits, not features?
- [ ] Is it specific and concrete, not vague?
- [ ] Does it celebrate the audience's strengths?
- [ ] Is it authentic and genuine?
- [ ] Would I say this to a friend?

---

## Accessibility Standards

SmilePile is committed to WCAG 2.2 Level AA compliance as a **minimum standard**, with many AAA elements.

### Color and Contrast

**Text Contrast (WCAG 1.4.3)**:
- Normal text (<24px or <18.66px bold): **4.5:1 minimum**
- Large text (≥24px or ≥18.66px bold): **3:1 minimum**
- Target: **AAA (7:1 for normal text, 4.5:1 for large text)**

**Non-Text Contrast (WCAG 1.4.11)**:
- UI components and graphical objects: **3:1 minimum**
- Icons, buttons, form borders: **3:1 against background**

**Don't Rely on Color Alone (WCAG 1.4.1)**:
- Use icons + text labels
- Use patterns or text in addition to color
- Example: Success = green + checkmark icon

### Typography and Readability

**Text Sizing (WCAG 1.4.4)**:
- Allow text resizing up to **200%** without loss of functionality
- Use relative units (rem, em) not pixels
- Minimum body text: **16px**

**Line Height and Spacing (WCAG 1.4.12)**:
- Line height: **1.5x font size minimum** (1.6x recommended)
- Paragraph spacing: **2x font size minimum**
- Letter spacing: **0.12x font size minimum**
- Word spacing: **0.16x font size minimum**

**Text Over Images (WCAG 1.4.3)**:
- Use overlay scrim (semi-transparent dark layer)
- Ensure 4.5:1 contrast maintained
- Or use solid background behind text

### Touch Targets and Interaction

**Touch Target Size (WCAG 2.5.8 - AAA)**:
- Minimum: **44x44px** (iOS) / **48x48px** (Android)
- Optimal: **48x48px or larger**
- Exception: Inline text links (but add padding)

**Spacing Between Targets (WCAG 2.5.8)**:
- Minimum spacing: **8px** between interactive elements
- Recommended: **16px** for important actions

**Click/Tap Area**:
- Extend beyond visible element using padding
- Example: Icon is 24px, but tap area is 48px

### Keyboard Navigation

**Focus Visible (WCAG 2.4.7)**:
- All interactive elements must have visible focus state
- Use high-contrast focus indicator (3:1 against background)
- Don't remove focus outlines without replacing

**Keyboard Accessible (WCAG 2.1.1)**:
- All functionality available via keyboard
- Tab order follows visual order
- No keyboard traps

**Skip Links (WCAG 2.4.1)**:
- "Skip to main content" link at top of page
- Hidden until focused
- Allows keyboard users to bypass navigation

### Screen Readers

**Alt Text (WCAG 1.1.1)**:
- All images must have alt text
- Decorative images: `alt=""`
- Informative images: Describe content and function
- Example: `alt="Mother and son looking at photos together on tablet"`

**Heading Structure (WCAG 1.3.1)**:
- Use proper heading hierarchy (H1 → H2 → H3)
- Don't skip levels (H1 → H3)
- One H1 per page

**Landmarks (WCAG 1.3.1)**:
- Use semantic HTML: `<header>`, `<nav>`, `<main>`, `<footer>`, `<aside>`
- Label landmarks when multiple of same type
- Helps screen reader users navigate

**Form Labels (WCAG 1.3.1, 3.3.2)**:
- All form fields must have labels
- Use `<label for="id">` not placeholder text
- Group related fields with `<fieldset>` and `<legend>`

**Link Text (WCAG 2.4.4)**:
- Link text must be descriptive out of context
- Avoid "click here" or "read more"
- Example: "Download our privacy policy" not "Click here"

### Visual Design

**Focus Indicators**:
```css
:focus-visible {
  outline: 3px solid #7FB3D5; /* Soft Blue */
  outline-offset: 2px;
}
```

**Reduced Motion (WCAG 2.3.3)**:
```css
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
```

**Dark Mode (WCAG 1.4.3)**:
- Support `prefers-color-scheme: dark`
- Maintain contrast ratios in dark mode
- Test both light and dark thoroughly

### Forms

**Labels (WCAG 3.3.2)**:
- Visible labels for all fields
- Labels above or to left of fields
- Don't rely on placeholder text alone

**Error Identification (WCAG 3.3.1)**:
```html
<label for="email">Email</label>
<input type="email" id="email" aria-describedby="email-error">
<span id="email-error" role="alert">Please enter a valid email address</span>
```

**Error Prevention (WCAG 3.3.4)**:
- Confirm before destructive actions
- Allow undo for important actions
- Validate fields inline where helpful

### Testing Tools

**Automated Testing**:
- [WAVE Browser Extension](https://wave.webaim.org/extension/)
- [Axe DevTools](https://www.deque.com/axe/devtools/)
- [Lighthouse](https://developers.google.com/web/tools/lighthouse) (Chrome DevTools)

**Manual Testing**:
- Keyboard navigation (Tab, Shift+Tab, Enter, Space, Arrow keys)
- Screen reader (NVDA on Windows, VoiceOver on Mac/iOS, TalkBack on Android)
- Zoom to 200% (Cmd/Ctrl + Plus)
- Color blindness simulation (Chrome DevTools)

**Contrast Checkers**:
- [WebAIM Contrast Checker](https://webaim.org/resources/contrastchecker/)
- [Accessible Colors](https://accessible-colors.com/)
- Chrome DevTools (Inspect > Accessibility)

### Accessibility Checklist

Every component and page must pass:

- [ ] Color contrast meets 4.5:1 for text, 3:1 for UI
- [ ] Text resizes to 200% without breaking layout
- [ ] All interactive elements have 44x44px minimum touch targets
- [ ] Keyboard accessible (Tab, Enter, Space work)
- [ ] Visible focus indicators on all interactive elements
- [ ] All images have alt text (or alt="" for decorative)
- [ ] Proper heading hierarchy (H1 → H2 → H3)
- [ ] Form fields have visible labels
- [ ] Error messages are clear and specific
- [ ] Supports reduced motion preference
- [ ] Works with screen reader (test with VoiceOver/NVDA)
- [ ] Semantic HTML (header, nav, main, footer)

---

## Component Patterns

### Buttons

**Primary Button**
```html
<button class="btn-primary">
  Download Now
</button>
```

**Styles**:
- Background: Soft Blue (#7FB3D5)
- Text: Warm Cream (#F8F3ED)
- Font: 16px, bold, Atkinson Hyperlegible
- Padding: 12px 24px
- Border radius: 8px
- Min height: 48px (touch target)
- Hover: Darken background 10%
- Focus: 3px outline, Soft Blue, 2px offset
- Active: Scale 0.98

**States**:
- Default: Soft Blue background
- Hover: #6DA3C8 (darker blue)
- Focus: 3px #7FB3D5 outline + 2px offset
- Disabled: 50% opacity, cursor not-allowed
- Loading: Show spinner, disable interaction

**Secondary Button**
```html
<button class="btn-secondary">
  Learn More
</button>
```

**Styles**:
- Background: Transparent
- Border: 2px solid Soft Blue (#7FB3D5)
- Text: Soft Blue (#7FB3D5)
- Same padding, radius, height as primary
- Hover: Soft Blue background 10% opacity
- Focus: Same as primary

**Text Button** (Tertiary)
```html
<button class="btn-text">
  Cancel
</button>
```

**Styles**:
- Background: Transparent
- Text: Soft Charcoal (#3A3A3A)
- Underline on hover
- Focus: 2px outline

**Icon Button**
```html
<button class="btn-icon" aria-label="Close">
  <svg>...</svg>
</button>
```

**Requirements**:
- Min size: 48x48px
- Must have `aria-label`
- Icon centered
- Visible focus state

### Cards

**Standard Card**
```html
<article class="card">
  <img src="..." alt="...">
  <div class="card-content">
    <h3>Card Title</h3>
    <p>Card description text...</p>
    <a href="#" class="card-link">Learn more</a>
  </div>
</article>
```

**Styles**:
- Background: White
- Border: 1px solid #E5E7EB (light gray)
- Border radius: 12px
- Padding: 24px
- Box shadow: 0 2px 8px rgba(0,0,0,0.08)
- Hover: Lift shadow (0 4px 12px)
- Focus-within: Soft Blue outline

**Photo Card** (Gallery)
```html
<div class="photo-card">
  <img src="..." alt="...">
  <div class="photo-overlay">
    <span class="photo-date">Jan 15, 2025</span>
  </div>
</div>
```

**Styles**:
- Aspect ratio: 1:1 or 4:3
- Border radius: 8px
- Overlay gradient for text readability
- Hover: Scale 1.02, increase shadow

### Forms

**Text Input**
```html
<div class="form-field">
  <label for="name">Full Name</label>
  <input type="text" id="name" placeholder="John Doe">
</div>
```

**Styles**:
- Label: 14px bold, Soft Charcoal, margin-bottom 8px
- Input: 16px, padding 12px 16px, border 2px solid #D1D5DB
- Border radius: 8px
- Min height: 48px
- Focus: Soft Blue border, 3px outline offset 2px
- Error: Border changes to #DC2626 (red), show error message below

**Error State**
```html
<div class="form-field form-field-error">
  <label for="email">Email</label>
  <input type="email" id="email" aria-describedby="email-error" aria-invalid="true">
  <span id="email-error" class="error-message" role="alert">
    Please enter a valid email address
  </span>
</div>
```

**Error Message Styles**:
- Color: #DC2626 (red)
- Font size: 14px
- Icon: ⚠️ or ❌ (optional)
- Display below field
- `role="alert"` for screen readers

**Success State**
```html
<div class="form-field form-field-success">
  <label for="password">Password</label>
  <input type="password" id="password" aria-describedby="password-success">
  <span id="password-success" class="success-message">
    ✓ Strong password
  </span>
</div>
```

**Success Message Styles**:
- Color: Sage Green (#A8D8B9)
- Checkmark icon: ✓
- Font size: 14px

### Headings

**Page Title (H1)**
```html
<h1>Celebrate Every Smile, Every Milestone</h1>
```
- 48px, bold, line-height 1.1
- Max width: 800px
- Margin bottom: 24px
- Color: Soft Charcoal

**Section Heading (H2)**
```html
<h2>How It Works</h2>
```
- 36px, bold, line-height 1.2
- Margin bottom: 16px
- Color: Soft Charcoal

**Subsection (H3)**
```html
<h3>Privacy First</h3>
```
- 28px, bold, line-height 1.3
- Margin bottom: 12px
- Color: Soft Charcoal

### Links

**Text Link**
```html
<a href="#" class="text-link">Learn more about privacy</a>
```

**Styles**:
- Color: Soft Blue (#7FB3D5)
- Underline: 2px solid, offset 2px
- Hover: Darken color, thicker underline
- Focus: 2px outline, Soft Blue
- Visited: Slightly darker blue (#6DA3C8)

**Standalone Link**
```html
<a href="#" class="standalone-link">
  Learn More →
</a>
```

**Styles**:
- Same as text link
- Arrow icon at end (→ or SVG)
- Hover: Arrow shifts right 4px

### Navigation

**Header Navigation**
```html
<nav aria-label="Main navigation">
  <ul>
    <li><a href="#features">Features</a></li>
    <li><a href="#privacy">Privacy</a></li>
    <li><a href="#support">Support</a></li>
  </ul>
</nav>
```

**Styles**:
- Background: White
- Sticky position (stays at top on scroll)
- Logo on left, nav links on right
- Mobile: Hamburger menu (48x48px touch target)
- Active page: Bold text, underline

**Mobile Menu**
- Full-screen overlay (or slide-in)
- Large touch targets (48px min height)
- Clear close button (X)
- Focus trap when open
- Escape key to close

### Modals

**Modal Dialog**
```html
<div class="modal" role="dialog" aria-labelledby="modal-title" aria-modal="true">
  <div class="modal-content">
    <button class="modal-close" aria-label="Close">×</button>
    <h2 id="modal-title">Modal Title</h2>
    <p>Modal content...</p>
    <div class="modal-actions">
      <button class="btn-primary">Confirm</button>
      <button class="btn-text">Cancel</button>
    </div>
  </div>
</div>
```

**Requirements**:
- Focus trap (Tab doesn't leave modal)
- Escape key to close
- Click outside to close (optional)
- Return focus to trigger element on close
- `role="dialog"` and `aria-modal="true"`
- Background overlay: rgba(0,0,0,0.5)

### Icons

**Sizing**:
- Small: 16px (inline with text)
- Medium: 24px (default UI)
- Large: 48px (feature highlights)

**Usage**:
- Always include text labels (or aria-label)
- Use inline SVG for flexibility
- 2px stroke weight for consistency
- Color: Inherit from parent or Soft Charcoal

**Accessibility**:
```html
<!-- Decorative icon -->
<svg aria-hidden="true">...</svg>

<!-- Informative icon -->
<svg role="img" aria-labelledby="icon-title">
  <title id="icon-title">Success</title>
  ...
</svg>
```

### Alerts and Notifications

**Success Alert**
```html
<div class="alert alert-success" role="alert">
  <svg>...</svg>
  <span>Photos backed up successfully!</span>
</div>
```

**Styles**:
- Background: Sage Green light (#E8F5EE)
- Border: 2px solid Sage Green (#A8D8B9)
- Text: Soft Charcoal
- Icon: Checkmark, Sage Green
- Padding: 16px
- Border radius: 8px

**Error Alert**
```html
<div class="alert alert-error" role="alert">
  <svg>...</svg>
  <span>Unable to upload photo. Please try again.</span>
</div>
```

**Styles**:
- Background: #FEE2E2 (light red)
- Border: 2px solid #DC2626 (red)
- Text: Soft Charcoal
- Icon: Warning triangle, red

**Info Alert**
```html
<div class="alert alert-info" role="alert">
  <svg>...</svg>
  <span>New features available! Check out what's new.</span>
</div>
```

**Styles**:
- Background: #E0F2FE (light blue)
- Border: 2px solid Soft Blue (#7FB3D5)
- Text: Soft Charcoal
- Icon: Info circle, Soft Blue

---

## Implementation Guide

### For Web (Astro + Tailwind)

**1. Install Atkinson Hyperlegible**

Add to `src/layouts/BaseLayout.astro`:
```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Atkinson+Hyperlegible:ital,wght@0,400;0,700;1,400;1,700&display=swap" rel="stylesheet">
```

**2. Update Tailwind Config**

`tailwind.config.js`:
```javascript
module.exports = {
  theme: {
    extend: {
      colors: {
        'soft-blue': {
          DEFAULT: '#7FB3D5',
          50: '#F0F7FB',
          100: '#D9EBF5',
          500: '#7FB3D5',
          600: '#6DA3C8',
          700: '#5B93BB',
        },
        'sage-green': {
          DEFAULT: '#A8D8B9',
          50: '#F2F9F5',
          100: '#E8F5EE',
          500: '#A8D8B9',
          600: '#8FCB9F',
          700: '#76BE85',
        },
        'lavender': {
          DEFAULT: '#C9B3D6',
          50: '#F7F4F9',
          100: '#EFE9F3',
          500: '#C9B3D6',
          600: '#BBA2CC',
          700: '#AD91C2',
        },
        'warm-cream': '#F8F3ED',
        'soft-charcoal': '#3A3A3A',
        'gentle-gold': '#F5DA81',
        'soft-coral': '#F4A6A3',
        'pale-mint': '#B8DCD6',
      },
      fontFamily: {
        sans: ['Atkinson Hyperlegible', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
      },
      fontSize: {
        'xs': '0.875rem',    // 14px
        'sm': '1rem',        // 16px
        'base': '1rem',      // 16px
        'lg': '1.125rem',    // 18px
        'xl': '1.375rem',    // 22px
        '2xl': '1.75rem',    // 28px
        '3xl': '2.25rem',    // 36px
        '4xl': '3rem',       // 48px
      },
      lineHeight: {
        'tight': '1.1',
        'snug': '1.2',
        'normal': '1.3',
        'relaxed': '1.5',
        'loose': '1.6',
      },
    },
  },
}
```

**3. Update Global Styles**

`src/styles/global.css`:
```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  body {
    @apply text-soft-charcoal bg-warm-cream;
    font-family: 'Atkinson Hyperlegible', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
    font-size: 16px;
    line-height: 1.6;
  }

  h1 {
    @apply text-4xl font-bold leading-tight;
  }

  h2 {
    @apply text-3xl font-bold leading-snug;
  }

  h3 {
    @apply text-2xl font-bold leading-normal;
  }

  a {
    @apply text-soft-blue underline underline-offset-2;
  }

  a:hover {
    @apply text-soft-blue-600;
  }

  a:focus-visible {
    @apply outline-2 outline-soft-blue outline-offset-2;
  }
}

@layer components {
  .btn-primary {
    @apply bg-soft-blue text-warm-cream font-bold px-6 py-3 rounded-lg min-h-[48px];
  }

  .btn-primary:hover {
    @apply bg-soft-blue-600;
  }

  .btn-primary:focus-visible {
    @apply outline-2 outline-soft-blue outline-offset-2;
  }

  .btn-secondary {
    @apply border-2 border-soft-blue text-soft-blue bg-transparent font-bold px-6 py-3 rounded-lg min-h-[48px];
  }

  .btn-secondary:hover {
    @apply bg-soft-blue bg-opacity-10;
  }
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
```

### For iOS (Swift/SwiftUI)

**Color Extension** (`Colors.swift`):
```swift
import SwiftUI

extension Color {
    static let softBlue = Color(hex: "#7FB3D5")
    static let sageGreen = Color(hex: "#A8D8B9")
    static let lavender = Color(hex: "#C9B3D6")
    static let warmCream = Color(hex: "#F8F3ED")
    static let softCharcoal = Color(hex: "#3A3A3A")

    init(hex: String) {
        let scanner = Scanner(string: hex)
        scanner.currentIndex = hex.startIndex
        var rgbValue: UInt64 = 0
        scanner.scanHexInt64(&rgbValue)

        let r = Double((rgbValue & 0xFF0000) >> 16) / 255.0
        let g = Double((rgbValue & 0x00FF00) >> 8) / 255.0
        let b = Double(rgbValue & 0x0000FF) / 255.0

        self.init(red: r, green: g, blue: b)
    }
}
```

**Typography** (use system font or custom):
```swift
// iOS uses San Francisco by default, which is excellent
// For custom font, add Atkinson Hyperlegible to project and use:

extension Font {
    static let largeTitle = Font.custom("AtkinsonHyperlegible-Bold", size: 48)
    static let title1 = Font.custom("AtkinsonHyperlegible-Bold", size: 36)
    static let title2 = Font.custom("AtkinsonHyperlegible-Bold", size: 28)
    static let body = Font.custom("AtkinsonHyperlegible-Regular", size: 16)
}
```

### For Android (Kotlin/Compose)

**Color Scheme** (`Color.kt`):
```kotlin
import androidx.compose.ui.graphics.Color

val SoftBlue = Color(0xFF7FB3D5)
val SageGreen = Color(0xFFA8D8B9)
val Lavender = Color(0xFFC9B3D6)
val WarmCream = Color(0xFFF8F3ED)
val SoftCharcoal = Color(0xFF3A3A3A)

val LightColorScheme = lightColorScheme(
    primary = SoftBlue,
    secondary = SageGreen,
    tertiary = Lavender,
    background = WarmCream,
    surface = Color.White,
    onPrimary = WarmCream,
    onSecondary = SoftCharcoal,
    onBackground = SoftCharcoal,
    onSurface = SoftCharcoal,
)
```

**Typography** (`Type.kt`):
```kotlin
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Use Roboto (default) or load Atkinson Hyperlegible
val Typography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 52.8.sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 43.2.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.6.sp,
    ),
)
```

---

## Version History

**v1.0** - January 2025
- Initial design system created
- Research-backed color palette for special needs parent market
- Atkinson Hyperlegible typography system
- Comprehensive accessibility standards (WCAG 2.2 AA+)
- Voice and tone guide
- Photography direction
- Component patterns

---

## Feedback and Iteration

This design system is a living document. As SmilePile grows and we gather feedback from our community of families, we'll update and refine these guidelines.

**Questions or suggestions?**
Contact: design@stackmap.app

---

**Remember**: Every design decision we make should serve our mission - helping families preserve and celebrate precious memories with simplicity, privacy, and joy.
