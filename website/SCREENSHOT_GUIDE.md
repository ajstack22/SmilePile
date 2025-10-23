# Screenshot Capture Guide
**SmilePile Website Assets** | Last Updated: January 2025

This guide will help you capture high-quality screenshots of the SmilePile app (iOS and Android) for use on the website, marketing materials, and app store listings.

---

## Table of Contents
1. [Screenshots Needed](#screenshots-needed)
2. [Technical Requirements](#technical-requirements)
3. [Setup & Preparation](#setup--preparation)
4. [iOS Screenshot Capture](#ios-screenshot-capture)
5. [Android Screenshot Capture](#android-screenshot-capture)
6. [Post-Processing](#post-processing)
7. [Adding to Website](#adding-to-website)
8. [App Store Screenshots](#app-store-screenshots)

---

## Screenshots Needed

### For Website (Homepage)

We need **3-4 key screenshots** that showcase SmilePile's core features:

#### 1. Main Gallery View (Primary Screenshot)
**Purpose**: Shows the core photo organization experience
**Screen**: Main photo gallery with categories visible
**What to show**:
- Timeline view with photos
- Multiple categories (Family, Milestones, Birthdays, etc.)
- Clean, organized layout
- Real photos (use demo mode if needed)
- Navigation visible

**Why it matters**: First impression - shows the app's core value

---

#### 2. Category View / Photo Stack
**Purpose**: Demonstrates how photos are organized by meaningful categories
**Screen**: A single category opened with photos displayed
**What to show**:
- Category name and color (e.g., "Milestones" in lavender)
- Grid or stack of photos within that category
- Easy-to-understand organization
- Add photo button visible

**Why it matters**: Shows the organizational power of categories

---

#### 3. Kids Mode (If Implemented)
**Purpose**: Highlights the child-safe viewing experience
**Screen**: Kids Mode active with simple, large photo viewing
**What to show**:
- Large, clear photo display
- Simple navigation (no delete/edit options visible)
- Category filter if available
- Playful, safe interface

**Why it matters**: Key differentiator for special needs families - safe sharing

---

#### 4. Photo Detail / Viewer (Optional)
**Purpose**: Shows individual photo viewing experience
**Screen**: Single photo opened in full view
**What to show**:
- Large photo display
- Date/metadata
- Category tags
- Simple, clean interface

**Why it matters**: Demonstrates the memory preservation aspect

---

### Optional Additional Screenshots

**5. Onboarding Welcome Screen**
- First screen users see
- Shows brand personality
- Clear value proposition

**6. Settings / Privacy Screen**
- Emphasizes privacy controls
- Shows no-tracking commitment
- Builds trust

---

## Technical Requirements

### Resolution & Format

**iOS**:
- **Device**: iPhone 15 Pro (6.1" display) - 1179 x 2556 pixels
- **Alternative**: iPhone 14 Pro - 1179 x 2556 pixels
- **Format**: PNG (lossless quality)
- **Color space**: sRGB or Display P3

**Android**:
- **Device**: Pixel 8 (6.2" display) - 1080 x 2400 pixels
- **Alternative**: Pixel 7 - 1080 x 2340 pixels
- **Format**: PNG (lossless quality)
- **Color space**: sRGB

### File Naming Convention

Use descriptive, consistent naming:

```
smilepile_ios_gallery_main.png
smilepile_ios_category_view.png
smilepile_ios_kids_mode.png
smilepile_android_gallery_main.png
smilepile_android_category_view.png
smilepile_android_kids_mode.png
```

### File Size Target

- **Raw screenshot**: 500KB - 2MB (PNG)
- **Optimized for web**: 200-300KB (compressed PNG or WebP)
- **Thumbnail**: <100KB

---

## Setup & Preparation

### Before You Start

1. **Clean Install** (Recommended)
   - Delete app from device
   - Reinstall latest build
   - Ensures no personal data in screenshots
   - Fresh, clean UI state

2. **Use Demo Mode** (If Available)
   - Populate app with demo data
   - Ensures diverse, appropriate photos
   - Shows realistic usage without privacy concerns
   - Categories pre-populated with sample photos

3. **Device Setup**
   - **Charge device**: 100% battery (or hide battery indicator)
   - **Time**: Set to 9:41 AM (iOS convention) or 10:00 AM
   - **Do Not Disturb**: Enable (no notifications)
   - **Wi-Fi/Cellular**: Full signal or airplane mode
   - **Clean home screen**: Remove clutter if showing device context

4. **App State**
   - Log in (if required)
   - Dismiss any first-time tooltips/popovers
   - Navigate to desired screen
   - Ensure all content is loaded (no spinners)
   - Check for any UI glitches

### Sample Data Recommendations

**Photos**:
- Use diverse, inclusive stock photos or demo images
- Show families of different ethnicities
- Include children at various ages
- Authentic, joyful moments (not staged studio shots)
- 10-20 photos minimum per category for realistic feel

**Categories**:
- Family (4-6 photos)
- Milestones (5-8 photos)
- Birthdays (3-5 photos)
- Holidays (4-6 photos)
- Adventures (2-4 photos)

**Dates**:
- Spread across recent months
- Shows timeline organization clearly

---

## iOS Screenshot Capture

### Method 1: Device Screenshot (Recommended)

**For iPhone with Face ID** (iPhone X and newer):
1. Navigate to desired screen in SmilePile app
2. Press **Volume Up + Side Button** simultaneously
3. Release quickly
4. Screenshot saves to Photos app
5. AirDrop or sync to Mac for processing

**For iPhone with Home Button** (iPhone 8 and earlier):
1. Navigate to desired screen
2. Press **Home + Side Button** simultaneously
3. Release quickly
4. Screenshot saves to Photos app

### Method 2: Xcode Simulator Screenshot

**When to use**: Testing without physical device, exact device model not available

**Steps**:
1. Open Xcode
2. Run SmilePile on desired simulator (e.g., iPhone 15 Pro)
3. Navigate to screen in simulator
4. Click **File > New Screen Shot** (or Cmd+S)
5. Screenshot saves to Desktop

**Pros**: Any device size, easy to capture
**Cons**: Simulator UI may differ slightly from real device, animations may be slower

### Method 3: Mac Screenshot of Simulator

**Steps**:
1. Run app in Xcode Simulator
2. Navigate to desired screen
3. Press **Cmd+Shift+4**
4. Draw selection around simulator screen (avoid window chrome)
5. Screenshot saves to Desktop

**Note**: Ensure you're capturing the exact simulator screen bounds, not the macOS window.

### Xcode Device Screenshot Capture

**For connected iPhone**:
1. Connect iPhone to Mac via cable
2. Open Xcode > Window > Devices and Simulators
3. Select your iPhone from left sidebar
4. Navigate to desired screen in SmilePile on device
5. Click **Take Screenshot** button in Xcode
6. Screenshot appears in Finder window

**Pros**: Real device, exact hardware rendering
**Cons**: Requires cable connection

---

## Android Screenshot Capture

### Method 1: Physical Device Screenshot

**For most Android devices**:
1. Navigate to desired screen in SmilePile app
2. Press **Power + Volume Down** simultaneously
3. Hold for 1 second
4. Screenshot saves to Photos/Gallery app
5. Transfer to computer via USB or Google Photos

**Alternative** (Samsung, some devices):
- **Palm swipe**: Swipe edge of hand across screen
- **Three-finger swipe**: Swipe down with three fingers (some devices)

### Method 2: Android Studio Emulator

**Steps**:
1. Open Android Studio
2. Launch emulator (Pixel 8 recommended)
3. Install and run SmilePile APK
4. Navigate to desired screen
5. Click **camera icon** in emulator toolbar (right side)
6. Screenshot saves to desktop

**Emulator Setup**:
- Device: Pixel 8 (API 34, Android 14)
- Resolution: 1080 x 2400 (420 dpi)
- Enable hardware acceleration

### Method 3: ADB Screenshot (Command Line)

**For advanced users or automated capture**:

```bash
# Connect device via USB with USB debugging enabled
adb devices

# Capture screenshot
adb shell screencap -p /sdcard/screenshot.png

# Pull to computer
adb pull /sdcard/screenshot.png ~/Desktop/smilepile_screenshot.png

# Clean up device
adb shell rm /sdcard/screenshot.png
```

**Use case**: Batch screenshot capture, CI/CD, automated testing

---

## Post-Processing

### 1. Cropping (If Needed)

**For device mockups**: Crop to exact screen bounds, removing status bar/navigation if desired

**For raw screenshots**: Keep full screen including status bar (shows context)

**Tools**:
- macOS Preview (built-in)
- Figma (for precise pixel cropping)
- Photoshop or GIMP

### 2. Compression & Optimization

**Goal**: Reduce file size while maintaining visual quality

**Tools**:

**ImageOptim** (macOS - FREE):
```bash
# Install via Homebrew
brew install imageoptim

# Optimize PNG
imageoptim smilepile_ios_gallery_main.png
```

**Online Tools**:
- [TinyPNG](https://tinypng.com/) - Smart PNG/JPG compression
- [Squoosh](https://squoosh.app/) - Google's image compressor (supports WebP)

**Command Line** (advanced):
```bash
# Install pngquant
brew install pngquant

# Compress PNG (lossy but high quality)
pngquant --quality=80-95 smilepile_ios_gallery_main.png

# Convert to WebP (better compression)
cwebp -q 85 smilepile_ios_gallery_main.png -o smilepile_ios_gallery_main.webp
```

**Target sizes**:
- Hero screenshot: 200-300KB (WebP) or 300-500KB (PNG)
- Thumbnail: <100KB

### 3. Format Conversion

**WebP** (Recommended for Web):
- 25-35% smaller than PNG/JPEG
- Excellent quality
- Supported by all modern browsers

**PNG** (Fallback):
- Lossless quality
- Universal support
- Larger file size

**Strategy**: Provide both formats, use WebP with PNG fallback

```html
<picture>
  <source srcset="/images/screenshot.webp" type="image/webp">
  <img src="/images/screenshot.png" alt="SmilePile gallery view">
</picture>
```

### 4. Accessibility: Alt Text

Write descriptive alt text for every screenshot:

**Good alt text examples**:
```html
<!-- Specific and descriptive -->
<img src="gallery.png" alt="SmilePile photo gallery showing a timeline view with family photos organized into colorful categories including Milestones, Birthdays, and Family">

<!-- Describes function -->
<img src="category.png" alt="Milestones category view displaying 8 photos in a grid layout with an add photo button in the top right">

<!-- Context for kids mode -->
<img src="kids-mode.png" alt="Kids Mode interface showing a large photo viewer with simple navigation controls and no editing options">
```

**Bad alt text examples**:
```html
<img src="screenshot.png" alt="Screenshot">
<img src="gallery.png" alt="SmilePile app">
<img src="category.png" alt="Image">
```

---

## Adding to Website

### File Organization

**Recommended structure**:
```
/website/public/images/screenshots/
├── ios/
│   ├── gallery-main.png
│   ├── gallery-main.webp
│   ├── category-view.png
│   ├── category-view.webp
│   ├── kids-mode.png
│   └── kids-mode.webp
├── android/
│   ├── gallery-main.png
│   ├── gallery-main.webp
│   ├── category-view.png
│   ├── category-view.webp
│   ├── kids-mode.png
│   └── kids-mode.webp
└── thumbnails/
    ├── gallery-main-thumb.webp
    ├── category-view-thumb.webp
    └── kids-mode-thumb.webp
```

### Update Homepage (index.astro)

**Current placeholder code** (find and replace):
```astro
<!-- BEFORE: SVG placeholder -->
<div class="bg-gray-200 rounded-lg aspect-[9/19] max-w-sm mx-auto flex items-center justify-center">
  <svg>...</svg>
</div>
```

**AFTER: Real screenshots**:
```astro
<!-- Features Section with Screenshots -->
<section class="py-20">
  <div class="container mx-auto px-4">
    <h2 class="text-4xl font-bold text-center mb-16">See SmilePile in Action</h2>

    <div class="grid md:grid-cols-3 gap-8">
      <!-- Screenshot 1: Gallery View -->
      <div class="text-center">
        <picture>
          <source srcset="/images/screenshots/ios/gallery-main.webp" type="image/webp">
          <img
            src="/images/screenshots/ios/gallery-main.png"
            alt="SmilePile photo gallery showing timeline view with family photos organized into colorful categories"
            class="rounded-2xl shadow-xl mx-auto max-w-xs"
            loading="lazy"
          >
        </picture>
        <h3 class="text-xl font-bold mt-6">Organized Timeline</h3>
        <p class="text-gray-600 mt-2">See your family's story unfold, beautifully organized by moments that matter</p>
      </div>

      <!-- Screenshot 2: Category View -->
      <div class="text-center">
        <picture>
          <source srcset="/images/screenshots/ios/category-view.webp" type="image/webp">
          <img
            src="/images/screenshots/ios/category-view.png"
            alt="Milestones category showing grid of photos with add photo button"
            class="rounded-2xl shadow-xl mx-auto max-w-xs"
            loading="lazy"
          >
        </picture>
        <h3 class="text-xl font-bold mt-6">Meaningful Categories</h3>
        <p class="text-gray-600 mt-2">Group photos by what matters - birthdays, milestones, adventures, and more</p>
      </div>

      <!-- Screenshot 3: Kids Mode -->
      <div class="text-center">
        <picture>
          <source srcset="/images/screenshots/ios/kids-mode.webp" type="image/webp">
          <img
            src="/images/screenshots/ios/kids-mode.png"
            alt="Kids Mode showing large photo viewer with simple, safe controls"
            class="rounded-2xl shadow-xl mx-auto max-w-xs"
            loading="lazy"
          >
        </picture>
        <h3 class="text-xl font-bold mt-6">Safe Kids Mode</h3>
        <p class="text-gray-600 mt-2">Let your child explore memories safely - no delete, no share, just joy</p>
      </div>
    </div>
  </div>
</section>
```

### Performance Optimization

**Lazy loading**:
```html
<img loading="lazy" ...>
```
- Don't load images until user scrolls near them
- Faster initial page load

**Responsive images** (different sizes for mobile/desktop):
```html
<img
  srcset="
    /images/screenshots/gallery-main-400.webp 400w,
    /images/screenshots/gallery-main-800.webp 800w,
    /images/screenshots/gallery-main-1200.webp 1200w
  "
  sizes="(max-width: 640px) 400px, (max-width: 1024px) 800px, 1200px"
  src="/images/screenshots/gallery-main-800.webp"
  alt="..."
>
```

**Pre-loading critical images**:
```html
<!-- In <head> for above-fold hero image -->
<link rel="preload" as="image" href="/images/screenshots/gallery-main.webp">
```

---

## App Store Screenshots

### iOS App Store Requirements

**Sizes required** (for iPhone):
- 6.9" display (iPhone 16 Pro Max): 1320 x 2868 pixels
- 6.7" display (iPhone 15 Pro Max): 1290 x 2796 pixels
- 6.5" display (iPhone 11 Pro Max): 1242 x 2688 pixels
- 5.5" display (iPhone 8 Plus): 1242 x 2208 pixels (optional)

**How many**: 3-10 screenshots (8-10 recommended)

**Format**: PNG or JPEG (no transparency)

**Orientation**: Portrait (required for App Store)

**Best practices**:
1. First screenshot is most important (shows in search results)
2. Add text overlays explaining features (optional but recommended)
3. Show app in use, not just splash screens
4. Demonstrate key value propositions visually

**Tools for adding text overlays**:
- [Figma](https://figma.com) (FREE) - Design screenshots with text
- [Canva](https://canva.com) - Templates for app store screenshots
- [App Screenshot Maker](https://www.appstorescreenshot.com/) - Specialized tool

### Google Play Store Requirements

**Sizes required** (for Phone):
- **Phone**: 1080 x 1920 pixels minimum (up to 7680 x 4320)
- **7-inch tablet**: 1080 x 1920 pixels (optional)
- **10-inch tablet**: 1920 x 1200 pixels (optional)

**How many**: 2-8 screenshots per device type

**Format**: PNG or JPEG (24-bit, no alpha)

**Orientation**: Portrait or landscape

**Best practices**:
- Similar to iOS - first screenshot is key
- Consider adding text overlays or captions
- Show actual app functionality

---

## Quick Reference Checklist

### Before Capturing Screenshots

- [ ] Clean install or use demo mode
- [ ] Device fully charged (or hide battery)
- [ ] Set time to 9:41 AM (iOS) or 10:00 AM (Android)
- [ ] Enable Do Not Disturb (no notifications)
- [ ] Full Wi-Fi/cellular signal or airplane mode
- [ ] Dismiss all tooltips and first-time UI
- [ ] Ensure content is fully loaded

### Screenshot Capture

- [ ] Capture 3-4 key screens (gallery, category, kids mode, detail)
- [ ] Use highest quality device available (iPhone 15 Pro, Pixel 8)
- [ ] Save as PNG (lossless)
- [ ] Use descriptive file names

### Post-Processing

- [ ] Crop to exact screen bounds (if needed)
- [ ] Optimize/compress images (target 200-300KB)
- [ ] Convert to WebP (with PNG fallback)
- [ ] Write descriptive alt text for each image
- [ ] Test images load correctly on website

### Website Integration

- [ ] Place images in `/website/public/images/screenshots/`
- [ ] Update `index.astro` with real screenshots
- [ ] Use `<picture>` element for WebP + PNG fallback
- [ ] Add `loading="lazy"` for below-fold images
- [ ] Test responsive display (mobile and desktop)
- [ ] Run Lighthouse audit (check performance impact)

### App Store (Future)

- [ ] Create App Store-specific sizes (see requirements above)
- [ ] Design text overlays highlighting features (optional)
- [ ] Order screenshots by priority (best first)
- [ ] Test on actual App Store Connect upload

---

## Tools Summary

**Screenshot Capture**:
- iOS: Xcode Simulator, Physical Device (Volume Up + Side Button)
- Android: Android Studio Emulator, Physical Device (Power + Volume Down), ADB

**Image Optimization**:
- [ImageOptim](https://imageoptim.com/) (macOS)
- [TinyPNG](https://tinypng.com/) (web)
- [Squoosh](https://squoosh.app/) (web, WebP conversion)

**Design/Mockups**:
- [Figma](https://figma.com) (FREE, web-based)
- Canva (templates)

**Verification**:
- [Google Lighthouse](https://developers.google.com/web/tools/lighthouse) (performance audit)
- Browser DevTools (test responsive images)

---

## Questions or Issues?

If you encounter issues capturing screenshots or have questions about this guide:

**Contact**: support@stackmap.app

---

**Remember**: Screenshots are users' first visual impression of SmilePile. Take time to capture clean, beautiful, representative images that show the app at its best - organized, joyful, and easy to use.
