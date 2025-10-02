//
//  AppConfig.swift
//  SmilePile
//
//  Configuration constants for SmilePile application.
//  Update URLs here to change privacy policy, terms, or support destinations.
//

import Foundation

struct AppConfig {
    // MARK: - URLs
    // SECURITY: Always use HTTPS for web URLs
    static let privacyPolicyURL = "https://smilepile.app/?privacy"
    static let termsOfServiceURL = "https://smilepile.app/?tos"
    static let supportEmail = "support@stackmap.app"

    // MARK: - Computed Properties
    static var supportMailtoURL: URL? {
        URL(string: "mailto:\(supportEmail)")
    }

    // MARK: - URL Validation
    static func isValidURL(_ urlString: String) -> Bool {
        guard let url = URL(string: urlString),
              let scheme = url.scheme else {
            return false
        }
        return scheme == "https" || scheme == "mailto"
    }
}
