# Atlas Workflow Prompt: SmilePile Landing Page & Legal Documentation Site

## 🔧 WORKFLOW DIRECTIVE: USE ATLAS AGENT-DRIVEN WORKFLOW

**MANDATORY:** This project MUST follow the **9-Phase Atlas Agent-Driven Workflow** as defined in `/Users/adamstack/SmilePile/atlas/docs/AGENT_WORKFLOW.md`

Execute all phases using specialized agents:
- **Phase 1:** Research (general-purpose agent)
- **Phase 2:** Story Creation (product-manager agent)
- **Phase 3:** Planning (developer agent)
- **Phase 4:** Security Review (security + peer-reviewer agents in parallel)
- **Phase 5:** Implementation (developer agent)
- **Phase 6:** Testing (peer-reviewer + general-purpose agents in parallel)
- **Phase 7:** Validation (product-manager agent)
- **Phase 8:** Clean-up (general-purpose agent)
- **Phase 9:** Deployment (devops agent)

**DO NOT skip phases. DO NOT modify the workflow. Follow Atlas methodology exactly.**

---

## Project Context

**What:** Create a professional landing page for SmilePile.app that serves as:
1. Marketing/informational site for the mobile apps
2. Host for legal documents (Privacy Policy, Terms of Service)
3. Support/contact information hub
4. App download links (App Store, Google Play)

**Why:** The SmilePile iOS and Android apps link to `https://smilepile.app/?privacy`, `https://smilepile.app/?tos`, and `mailto:support@stackmap.app`. We need a functional website to serve these pages and provide app information.

**Current State:**
- Domain: smilepile.app (owned, not yet configured)
- Mobile apps: Deployed with links to non-existent pages
- Company: StackMap (developer/publisher)
- No existing website infrastructure

## Goals & Requirements

### Primary Goals (P0)
1. **Legal Compliance Pages:**
   - Privacy Policy page (`/?privacy` or `/privacy`)
   - Terms of Service page (`/?tos` or `/terms`)
   - Support/Contact page (`/support`)

2. **Landing Page:**
   - App description and features
   - Download links (App Store badge, Google Play badge)
   - Screenshots/visuals
   - Child-safe messaging (primary audience: parents)

3. **Technical Requirements:**
   - Fast, lightweight (no heavy frameworks needed)
   - Mobile-responsive
   - SEO-friendly
   - HTTPS (required for app links)
   - Analytics (optional, privacy-respecting)

### Secondary Goals (P1)
- FAQ section
- Blog/news section (future updates)
- Press kit / media resources
- Multi-language support (future)

### Non-Goals (Out of Scope)
- User authentication/accounts
- Dynamic content management system (CMS)
- E-commerce functionality
- Complex backend services

## Technical Constraints

### Domain & Hosting
- **Domain:** smilepile.app (already owned and configured)
- **Hosting:** Already handled separately - focus on site files only
- **SSL:** HTTPS already configured
- **Deliverable:** Static site files ready for deployment

### Technology Stack Recommendations
- **Static Site:** HTML/CSS/JS or static site generator (11ty, Astro, Next.js static export)
- **No Backend:** Pure static hosting initially
- **Analytics:** Privacy-respecting (Plausible, Simple Analytics, or none)
- **No Tracking:** COPPA-compliant (no user tracking for children's app)

### Content Requirements

**Privacy Policy Must Include:**
- What data is collected (photos, device info)
- How data is used (local storage, no cloud sync)
- Third-party services (none currently)
- Children's privacy (COPPA compliance statement)
- Parental rights (data access, deletion)
- Contact information for privacy concerns
- Last updated date

**Terms of Service Must Include:**
- Acceptable use policy
- Intellectual property rights
- Disclaimer of warranties
- Limitation of liability
- Governing law
- Last updated date

**Support Page Must Include:**
- Contact email: support@stackmap.app
- FAQ (common questions)
- Troubleshooting guides (optional P1)
- Feature request process (optional P1)

## Atlas Workflow Execution Plan

Use the 9-phase Atlas workflow with the following agent assignments:

### Phase 1: Research (general-purpose agent)
**Research the following:**

1. **Static Site Generator Analysis:**
   - Options: Plain HTML, 11ty, Astro, Hugo, Next.js static
   - Criteria: Simplicity, build speed, flexibility
   - Recommendation for this use case

3. **Legal Document Templates:**
   - Research COPPA-compliant privacy policy templates
   - Terms of service templates for mobile apps
   - Privacy policy requirements for iOS App Store & Google Play

4. **Design Inspiration:**
   - Review 3-5 similar app landing pages (family/photo apps)
   - Identify common patterns (hero section, features, download CTAs)
   - Note design elements that work well

5. **SEO & Performance:**
   - Core Web Vitals requirements
   - Meta tags needed (Open Graph, Twitter Cards)
   - Accessibility requirements (WCAG 2.1 Level AA)

**Deliverable:** Research report with recommendations for tech stack, legal templates, and design approach.

---

### Phase 2: Story Creation (product-manager agent)
**Create user stories for:**

**LANDING-1: Core Landing Page (P0)**
- As a parent, I want to learn about SmilePile so I can decide if it's right for my child
- Acceptance Criteria:
  - Hero section with app description
  - Feature highlights (3-5 key features)
  - Screenshots (iOS and Android)
  - Download buttons (App Store, Google Play)
  - Footer with company info and legal links

**LANDING-2: Privacy Policy Page (P0)**
- As a parent, I want to read the privacy policy so I understand data handling
- Acceptance Criteria:
  - Accessible at /?privacy or /privacy
  - COPPA-compliant content
  - Clear, readable formatting
  - Last updated date
  - Contact information

**LANDING-3: Terms of Service Page (P0)**
- As a user, I want to understand the terms so I know my rights
- Acceptance Criteria:
  - Accessible at /?tos or /terms
  - Standard TOS sections
  - Clear, readable formatting
  - Last updated date

**LANDING-4: Support/Contact Page (P0)**
- As a user, I want to contact support so I can get help
- Acceptance Criteria:
  - Email: support@stackmap.app prominently displayed
  - FAQ section (3-5 common questions)
  - Clear instructions for contacting support

**LANDING-5: Build & Package (P0)**
- As a developer, I want production-ready static files so they can be deployed
- Acceptance Criteria:
  - All static files generated and optimized
  - Build process documented
  - Files ready for upload to any static host
  - README with deployment instructions

**Deliverable:** Story backlog with priorities, acceptance criteria, and effort estimates.

---

### Phase 3: Planning (developer agent)
**Create technical implementation plan for:**

1. **Site Structure:**
   ```
   /
   ├── index.html (landing page)
   ├── privacy.html (privacy policy)
   ├── terms.html (terms of service)
   ├── support.html (support/contact)
   ├── css/
   │   └── styles.css
   ├── js/
   │   └── main.js (optional)
   ├── images/
   │   ├── logo.png
   │   ├── screenshots/
   │   └── app-badges/
   └── README.md
   ```

2. **Technology Decisions:**
   - Static site generator: [CHOSEN OPTION]
   - CSS framework: Tailwind/Bootstrap/Custom
   - Build process: [DESCRIBE]
   - Output: Static HTML/CSS/JS files

3. **Content Requirements:**
   - Privacy policy copy (legal review needed)
   - Terms of service copy (legal review needed)
   - App description and features
   - Screenshots (iOS and Android)
   - App Store/Google Play badges

4. **SEO & Performance:**
   - Meta tags strategy
   - Image optimization
   - Lazy loading (if needed)
   - Core Web Vitals targets

**Deliverable:** Technical design document with architecture, tech stack, build process, and timeline.

---

### Phase 4: Security Review (security + peer-reviewer agents in parallel)

**Security Agent Reviews:**
- HTTPS enforcement (no HTTP fallback)
- Content Security Policy (CSP) headers
- No XSS vulnerabilities in static content
- Privacy-respecting analytics (if any)
- COPPA compliance verification
- Contact form security (if implemented)

**Peer Reviewer Reviews:**
- Architecture soundness
- Tech stack appropriateness
- Accessibility compliance
- Mobile responsiveness plan
- SEO best practices
- Code quality standards

**Deliverable:** Security assessment and peer review report with any required changes.

---

### Phase 5: Implementation (developer agent)

**Build the website:**

1. **Setup:**
   - Initialize project with chosen static site generator
   - Configure build system
   - Setup local development environment

2. **Core Pages:**
   - Landing page (index.html)
   - Privacy policy (privacy.html or /?privacy routing)
   - Terms of service (terms.html or /?tos routing)
   - Support page (support.html)

3. **Styling:**
   - Responsive design (mobile-first)
   - Brand colors matching SmilePile app
   - Typography (readable, accessible)
   - Components (buttons, cards, footer)

4. **Assets:**
   - Logo and branding
   - Screenshots (placeholder or actual)
   - App Store/Google Play badges
   - Favicon and touch icons

5. **Functionality:**
   - Smooth scrolling navigation
   - Mobile menu (if needed)
   - mailto: link for support email
   - External links open in new tabs

**Deliverable:** Working website deployable to hosting platform.

---

### Phase 6: Testing (peer-reviewer + general-purpose agents in parallel)

**UX/Accessibility Testing:**
- Mobile responsiveness (iOS Safari, Android Chrome)
- Accessibility audit (WCAG 2.1 AA)
- Screen reader testing (VoiceOver, TalkBack)
- Color contrast validation
- Touch target sizing

**Functional Testing:**
- All links work correctly
- /?privacy and /?tos routes work
- App store badges link correctly
- Support email link works (mailto:)
- Forms work (if any)
- 404 page exists

**Performance Testing:**
- Lighthouse audit (target: 90+ all metrics)
- Core Web Vitals measurement
- Page load speed < 2s
- Image optimization verified
- No console errors

**Deliverable:** Test report with issues found and recommendations.

---

### Phase 7: Validation (product-manager agent)

**Validate all acceptance criteria:**
- LANDING-1: Core landing page complete and functional
- LANDING-2: Privacy policy accessible and COPPA-compliant
- LANDING-3: Terms of service accessible and complete
- LANDING-4: Support page with email and FAQ
- LANDING-5: Deployment pipeline working

**Content Review:**
- Legal copy reviewed (recommend professional legal review)
- Marketing copy compelling and accurate
- No typos or grammatical errors
- Consistent brand voice
- All links functional

**Deliverable:** Validation report with sign-off or required changes.

---

### Phase 8: Clean-up (general-purpose agent)

**Final cleanup:**
- Remove placeholder content
- Optimize images
- Minify CSS/JS (if applicable)
- Add sitemap.xml
- Add robots.txt
- Configure analytics (if used)
- Remove debug code
- Update README with deployment instructions

**Deliverable:** Production-ready codebase.

---

### Phase 9: Build & Package (devops agent)

**Prepare deployment package:**

1. **Production Build:**
   - Run production build process
   - Minify CSS/JS files
   - Optimize images
   - Generate sitemap.xml

2. **Package Files:**
   - Create deployment-ready directory
   - Include all static assets
   - Verify file structure is correct
   - Test locally before packaging

3. **Documentation:**
   - Create deployment README
   - Document required URL routes (/?privacy, /?tos)
   - List any server configuration needed
   - Include troubleshooting guide

4. **Verification:**
   - Test all links work locally
   - Verify /?privacy and /?tos routes
   - Check mailto: links
   - Validate HTML/CSS
   - Run Lighthouse audit

5. **Handoff Package:**
   - ZIP or tarball of all files
   - Deployment instructions
   - Server configuration notes (if any)
   - Contact for support

**Deliverable:** Production-ready static site package ready for deployment to any web host.

---

## Key Decisions Needed Before Starting

Before launching the Atlas workflow, decide:

1. **Static Site Generator:**
   - Plain HTML (simplest, fastest)
   - 11ty (flexible, fast builds)
   - Astro (modern, component-based)
   - Next.js static export (React-based)

3. **Legal Content:**
   - Use template + customize (faster)
   - Hire lawyer to draft (more thorough, recommended)

4. **Design Style:**
   - Minimal/clean (recommended for family app)
   - Playful/colorful (matches app theme)
   - Professional/corporate

5. **Analytics:**
   - None (most privacy-respecting)
   - Plausible Analytics (privacy-friendly, paid)
   - Simple Analytics (privacy-friendly, paid)
   - Google Analytics (not recommended for COPPA)

## Success Metrics

**Launch Criteria:**
- ✅ All static files built and optimized
- ✅ Privacy policy page accessible at /?privacy
- ✅ Terms page accessible at /?tos
- ✅ Support email link functional (mailto:)
- ✅ App download links work
- ✅ Mobile responsive (iOS and Android tested)
- ✅ Lighthouse score 90+ (all metrics)
- ✅ No accessibility violations (WCAG 2.1 AA)
- ✅ Deployment package ready with documentation

**Post-Launch Metrics (optional):**
- Page views per month
- App store link click-through rate
- Support email volume
- Core Web Vitals (LCP, FID, CLS)

## Estimated Timeline

**Fast Track (1-2 days):**
- Plain HTML + Vercel/Netlify
- Template-based legal docs
- Minimal design

**Standard Track (3-5 days):**
- Static site generator (11ty/Astro)
- Custom legal docs (with legal review)
- Polished design

**Full Track (1-2 weeks):**
- Advanced features (blog, CMS integration)
- Professional legal review
- Custom illustrations/design

## Example Kickoff Command

```markdown
Create a landing page for SmilePile.app using the Atlas 9-phase workflow as defined in /Users/adamstack/SmilePile/atlas/docs/AGENT_WORKFLOW.md:

**Requirements:**
- Privacy policy at /?privacy (COPPA-compliant)
- Terms of service at /?tos
- Support page with support@stackmap.app
- Landing page with app features and download links
- Mobile-responsive, fast, accessible
- Static files ready for deployment (hosting handled separately)

**Preferences:**
- Technology: Astro or plain HTML (static site generator)
- Design: Clean, minimal, family-friendly
- Analytics: None (privacy-first)
- Deliverable: Production-ready static files + deployment docs

**Domain:** smilepile.app (already configured - hosting handled separately)
**Company:** StackMap
**Timeline:** 3-5 days

Execute Phase 1 (Research) using general-purpose agent to analyze static site generators and legal document templates. Provide recommendations for tech stack and implementation approach.
```

---

## Additional Resources

### Legal Templates (Starting Points)
- [Termly](https://termly.io/resources/templates/) - Free privacy policy generator
- [PrivacyPolicies.com](https://www.privacypolicies.com/) - App-specific templates
- [COPPA Safe Harbor](https://www.ftc.gov/business-guidance/privacy-security/childrens-privacy) - FTC guidelines

### Design Inspiration
- [Land-book](https://land-book.com/) - Landing page gallery
- [SaaS Landing Pages](https://saaslandingpage.com/) - SaaS examples
- [Mobbin](https://mobbin.com/browse/web/apps) - App landing pages

### Hosting Platforms
- [Vercel](https://vercel.com/) - Recommended for static sites
- [Netlify](https://www.netlify.com/) - Alternative to Vercel
- [Cloudflare Pages](https://pages.cloudflare.com/) - Fast global CDN

### Performance Tools
- [Google PageSpeed Insights](https://pagespeed.web.dev/)
- [WebPageTest](https://www.webpagetest.org/)
- [Lighthouse CI](https://github.com/GoogleChrome/lighthouse-ci)

---

**Ready to execute? Run Phase 1 (Research) to begin the Atlas workflow!**
