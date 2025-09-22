# 04: Performance Standards

Performance is a critical feature. A slow, janky application is a broken application. These standards define the non-negotiable performance targets for the project.

## 1. General Benchmarks (All Platforms)

These targets apply to the application running on any supported platform.

-   **UI Response Time**: All user interactions (taps, clicks, swipes) must provide feedback in **< 100ms**.
-   **Scrolling**: All scrolling interfaces must maintain a minimum of **60 FPS** (Frames Per Second).
-   **Memory Usage**: The application's memory footprint should remain below **200MB** during a typical user session.
-   **Initial Load**: The application should be fully interactive in **< 3 seconds** from a warm start.

## 2. Platform-Specific Targets

Different platforms have different capabilities and user expectations. We hold each platform to a high standard.

### Web

-   **Lighthouse Score**: All pages must achieve a Lighthouse Performance score of **> 90**.
-   **First Contentful Paint (FCP)**: Must be **< 1.5 seconds**.
-   **Bundle Size**: The initial JavaScript bundle size must be **< 5MB**.

### iOS

-   **Cold Start Time**: The application must launch from a cold start in **< 800ms** on a target device (e.g., iPhone 14).

### Android

-   **Cold Start Time**: The application must launch from a cold start in **< 1000ms** on a target device (e.g., Pixel 6).

## 3. Performance Testing

Performance cannot be a matter of opinion; it must be measured.

-   **Measure Before and After**: Every significant change must be accompanied by a performance measurement to track its impact.
-   **Use Platform-Specific Tools**: Use the appropriate tools for each platform to get accurate measurements.
    -   **Web**: Lighthouse, Chrome DevTools Performance tab.
    -   **iOS**: Xcode Instruments (Time Profiler, Allocations).
    -   **Android**: Android Studio Profiler (CPU, Memory).
-   **Bundle Analysis**: Regularly use a bundle analyzer tool to inspect the contents of the application bundle and identify opportunities to reduce its size.

## 4. Performance Regressions

A performance regression is a critical bug and must be treated with the same severity.

-   **Decision Framework**:
    -   **< 10% performance hit**: Acceptable for a critical, high-value feature.
    -   **10-20% performance hit**: The feature must be essential. Requires review and approval.
    -   **> 20% performance hit**: Rejected. The feature must be optimized before it can be merged.
-   **No Degradation Over Time**: The overall performance of the application should not degrade as new features are added. Performance metrics should be tracked over time.
