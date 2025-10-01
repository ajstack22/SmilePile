import Foundation
import LocalAuthentication
import Combine

enum AuthenticationMethod: String, CaseIterable {
    case pin = "PIN"
    case pattern = "Pattern"
    case biometric = "Biometric"
}

class SecuritySettingsViewModel: ObservableObject {
    @Published var hasPIN = false
    @Published var hasPattern = false
    @Published var isBiometricEnabled = false
    @Published var isBiometricAvailable = false
    @Published var preferredAuthMethod: AuthenticationMethod = .pin
    @Published var currentPINLength = 4
    @Published var isSecure = false

    private let pinManager = PINManager.shared
    private let patternManager = PatternManager.shared
    private let context = LAContext()

    private let preferredAuthKey = "preferred_auth_method"
    private let biometricEnabledKey = "biometric_enabled"

    var biometricName: String {
        switch context.biometryType {
        case .faceID:
            return "Face ID"
        case .touchID:
            return "Touch ID"
        case .opticID:
            if #available(iOS 17.0, *) {
                return "Optic ID"
            } else {
                return "Biometric"
            }
        case .none:
            return "Biometric"
        @unknown default:
            return "Biometric"
        }
    }

    var biometricIcon: String {
        switch context.biometryType {
        case .faceID:
            return "faceid"
        case .touchID:
            return "touchid"
        case .opticID:
            return "eye"
        case .none:
            return "lock.shield"
        @unknown default:
            return "lock.shield"
        }
    }

    init() {
        checkSecurityStatus()
        loadPreferences()
    }

    func checkSecurityStatus() {
        updateAuthenticationStatus()
        updateBiometricAvailability()
        isSecure = hasPIN || hasPattern
        updatePreferredMethodIfNeeded()
    }

    private func updateAuthenticationStatus() {
        hasPIN = pinManager.isPINEnabled()
        hasPattern = patternManager.hasPattern()
        currentPINLength = pinManager.getPINLength()
    }

    private func updateBiometricAvailability() {
        var error: NSError?
        isBiometricAvailable = context.canEvaluatePolicy(
            .deviceOwnerAuthenticationWithBiometrics,
            error: &error
        )
    }

    private func updatePreferredMethodIfNeeded() {
        if !hasPIN && preferredAuthMethod == .pin {
            preferredAuthMethod = determineAlternateAuthMethod()
        }
    }

    private func determineAlternateAuthMethod() -> AuthenticationMethod {
        if hasPattern {
            return .pattern
        } else if isBiometricEnabled && isBiometricAvailable {
            return .biometric
        } else {
            return .pin
        }
    }

    func loadPreferences() {
        if let savedMethod = UserDefaults.standard.string(forKey: preferredAuthKey),
           let method = AuthenticationMethod(rawValue: savedMethod) {
            preferredAuthMethod = method
        }

        isBiometricEnabled = UserDefaults.standard.bool(forKey: biometricEnabledKey)
    }

    func setPreferredAuthMethod(_ method: AuthenticationMethod) {
        preferredAuthMethod = method
        UserDefaults.standard.set(method.rawValue, forKey: preferredAuthKey)
    }

    func setBiometricEnabled(_ enabled: Bool) {
        isBiometricEnabled = enabled
        UserDefaults.standard.set(enabled, forKey: biometricEnabledKey)
        SettingsManager.shared.biometricEnabled = enabled

        if enabled && !hasPIN && !hasPattern {
            // If enabling biometric without PIN/Pattern, we need at least one backup method
            // This should trigger a setup flow in the UI
        }
    }

    func removePIN() {
        do {
            try pinManager.clearPIN()
            hasPIN = false
            currentPINLength = 4
            checkSecurityStatus()

            if preferredAuthMethod == .pin {
                preferredAuthMethod = hasPattern ? .pattern : .pin
                setPreferredAuthMethod(preferredAuthMethod)
            }
        } catch {
            print("Failed to remove PIN: \(error)")
        }
    }

    func removePattern() {
        let success = patternManager.removePattern()
        if success {
            hasPattern = false
            checkSecurityStatus()

            if preferredAuthMethod == .pattern {
                preferredAuthMethod = hasPIN ? .pin : .pattern
                setPreferredAuthMethod(preferredAuthMethod)
            }
        }
    }

    func validateCurrentSecurity() -> Bool {
        switch preferredAuthMethod {
        case .pin:
            return hasPIN
        case .pattern:
            return hasPattern
        case .biometric:
            return isBiometricAvailable && isBiometricEnabled
        }
    }

    func refreshSecurityStatus() {
        checkSecurityStatus()
        loadPreferences()
    }

    func getSecuritySummary() -> String {
        var summary: [String] = []

        if hasPIN {
            summary.append("PIN (\(currentPINLength) digits)")
        }

        if hasPattern {
            summary.append("Pattern")
        }

        if isBiometricEnabled && isBiometricAvailable {
            summary.append(biometricName)
        }

        return summary.isEmpty ? "No security configured" : summary.joined(separator: ", ")
    }
}