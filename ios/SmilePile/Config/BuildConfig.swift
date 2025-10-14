//
//  BuildConfig.swift
//  SmilePile
//
//  Runtime configuration detection for 4-tier deployment system.
//  Reads BUILD_TYPE_ENV from Info.plist to determine deployment tier.
//
//  Wave 2: iOS 4-Tier Configuration
//  Story: STORY-6.2-ios-tier-config.md
//
//  CRITICAL FIX: Uses safe bundle initialization that works in both
//  app and test contexts. Bundle.main fails in XCTest environment.
//

import Foundation

public struct BuildConfig {
    // MARK: - Bundle Access (Test-Safe)

    /// Returns the appropriate bundle for the current context
    /// - In app context: Returns Bundle.main
    /// - In test context: Returns the test bundle
    /// This prevents crashes when accessing BuildConfig from unit tests
    private static var bundle: Bundle {
        // Check if we're running in a test environment
        if NSClassFromString("XCTestCase") != nil {
            // In test context, use the bundle containing this class
            return Bundle(for: BuildConfigBundleToken.self)
        }
        return Bundle.main
    }

    // MARK: - Build Type Detection

    /// The current build tier: qual, stage, beta, or prod
    /// Read from Info.plist BUILD_TYPE_ENV key (populated by xcconfig files)
    public static var buildType: String {
        guard let buildType = bundle.object(forInfoDictionaryKey: "BUILD_TYPE_ENV") as? String else {
            // Fallback to qual for safety - this should never happen in production
            #if DEBUG
            print("⚠️ Warning: BUILD_TYPE_ENV not found in Info.plist, defaulting to 'qual'")
            return "qual"  // Development default
            #else
            print("⚠️ Warning: BUILD_TYPE_ENV not found in Info.plist, defaulting to 'prod'")
            return "prod"  // Production default for safety
            #endif
        }
        return buildType
    }

    // MARK: - Tier Detection Helpers

    /// Returns true if running in QUAL tier (local development)
    public static var isQual: Bool {
        return buildType == "qual"
    }

    /// Returns true if running in STAGE tier (TestFlight internal)
    public static var isStage: Bool {
        return buildType == "stage"
    }

    /// Returns true if running in BETA tier (TestFlight external)
    public static var isBeta: Bool {
        return buildType == "beta"
    }

    /// Returns true if running in PROD tier (App Store)
    public static var isProd: Bool {
        return buildType == "prod"
    }

    // MARK: - Display Properties

    /// Human-readable tier name (QUAL, STAGE, BETA, PROD)
    public static var tierDisplayName: String {
        switch buildType {
        case "qual":
            return "QUAL"
        case "stage":
            return "STAGE"
        case "beta":
            return "BETA"
        case "prod":
            return "PROD"
        default:
            return "UNKNOWN"
        }
    }

    // MARK: - Bundle Information

    /// The app's bundle identifier (com.smilepile.qual or com.smilepile)
    public static var bundleIdentifier: String {
        return bundle.bundleIdentifier ?? "unknown"
    }

    /// The app's display name (SmilePile Qual, SmilePile Stage, etc.)
    public static var displayName: String {
        return bundle.object(forInfoDictionaryKey: "CFBundleDisplayName") as? String ?? "SmilePile"
    }
}

// MARK: - Bundle Token (For Test Context)

/// Private class used to get the correct bundle in test environment
/// This class exists solely to provide a type for Bundle(for:) in tests
private final class BuildConfigBundleToken {}
