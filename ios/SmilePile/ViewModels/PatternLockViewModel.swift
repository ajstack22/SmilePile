import Foundation
import LocalAuthentication
import Combine

class PatternLockViewModel: ObservableObject {
    @Published var firstPattern: [Int] = []
    @Published var isChangingPattern = false
    @Published var isInCooldown = false
    @Published var remainingCooldownTime: TimeInterval = 0
    @Published var remainingAttempts = 5
    @Published var canUseBiometric = false

    private let patternManager = PatternManager.shared
    private let context = LAContext()
    private var cooldownTimer: Timer?

    var mode: PatternMode = .validate

    init() {
        checkBiometricAvailability()
        updateCooldownStatus()
        updateRemainingAttempts()
    }

    deinit {
        cooldownTimer?.invalidate()
    }

    func savePattern(_ pattern: [Int]) -> Bool {
        return patternManager.setPattern(pattern)
    }

    func validatePattern(_ pattern: [Int]) -> Bool {
        let isValid = patternManager.validatePattern(pattern)
        updateRemainingAttempts()
        updateCooldownStatus()
        return isValid
    }

    func hasPattern() -> Bool {
        return patternManager.hasPattern()
    }

    func removePattern() -> Bool {
        firstPattern = []
        isChangingPattern = false
        return patternManager.removePattern()
    }

    func authenticateWithBiometric(completion: @escaping (Bool) -> Void) {
        var error: NSError?

        guard context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) else {
            completion(false)
            return
        }

        let reason = "Authenticate to access parental controls"

        context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: reason) { success, error in
            DispatchQueue.main.async {
                if success {
                    self.resetFailedAttempts()
                    completion(true)
                } else {
                    if let error = error as? LAError {
                        switch error.code {
                        case .userCancel, .systemCancel:
                            print("Biometric authentication cancelled")
                        case .userFallback:
                            print("User chose to use pattern instead")
                        default:
                            print("Biometric authentication failed: \(error.localizedDescription)")
                        }
                    }
                    completion(false)
                }
            }
        }
    }

    private func checkBiometricAvailability() {
        var error: NSError?
        canUseBiometric = context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error)

        if let error = error {
            print("Biometric not available: \(error.localizedDescription)")
        }
    }

    private func updateCooldownStatus() {
        isInCooldown = patternManager.isInCooldown()

        if isInCooldown {
            remainingCooldownTime = patternManager.getRemainingCooldownTime()
            startCooldownTimer()
        } else {
            cooldownTimer?.invalidate()
            cooldownTimer = nil
        }
    }

    private func startCooldownTimer() {
        cooldownTimer?.invalidate()

        cooldownTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { _ in
            DispatchQueue.main.async {
                self.remainingCooldownTime = self.patternManager.getRemainingCooldownTime()

                if self.remainingCooldownTime <= 0 {
                    self.isInCooldown = false
                    self.cooldownTimer?.invalidate()
                    self.cooldownTimer = nil
                    self.updateRemainingAttempts()
                }
            }
        }
    }

    private func updateRemainingAttempts() {
        remainingAttempts = patternManager.getRemainingAttempts()
    }

    private func resetFailedAttempts() {
        updateRemainingAttempts()
        updateCooldownStatus()
    }

    func convertPatternToString(_ pattern: [Int]) -> String {
        return patternManager.patternToString(pattern)
    }

    func isValidPattern(_ pattern: [Int]) -> Bool {
        return patternManager.isValidPattern(pattern)
    }
}