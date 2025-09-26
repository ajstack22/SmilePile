import Foundation
import LocalAuthentication
import Combine

class ParentalLockViewModel: ObservableObject {
    @Published var isUnlocked = false
    @Published var isPINSet = false
    @Published var hasPattern = false
    @Published var isBiometricEnabled = false
    @Published var isBiometricAvailable = false
    @Published var attemptsRemaining = 5
    @Published var isInCooldown = false
    @Published var cooldownTimeRemaining: TimeInterval = 0
    @Published var biometricType: LABiometryType = .none

    private let pinManager = PINManager.shared
    private let patternManager = PatternManager.shared
    private let laContext = LAContext()
    private var cooldownTimer: Timer?
    private var cancellables = Set<AnyCancellable>()

    init() {
        checkPINStatus()
        checkPatternStatus()
        checkBiometricAvailability()
        checkCooldownStatus()
    }

    func checkPINStatus() {
        isPINSet = pinManager.isPINEnabled()
    }

    func checkPatternStatus() {
        hasPattern = patternManager.hasPattern()
    }

    func checkBiometricAvailability() {
        var error: NSError?
        isBiometricAvailable = laContext.canEvaluatePolicy(
            .deviceOwnerAuthenticationWithBiometrics,
            error: &error
        )

        if isBiometricAvailable {
            biometricType = laContext.biometryType
            // Load biometric preference from UserDefaults
            isBiometricEnabled = UserDefaults.standard.bool(forKey: "biometric_enabled")
        }
    }

    func checkCooldownStatus() {
        updateCooldownStatus()
        if pinManager.isInCooldown() {
            startCooldownTimer()
        }
    }

    func authenticateWithBiometric(completion: @escaping (Bool, Error?) -> Void) {
        let context = LAContext()
        context.localizedReason = "Authenticate to access parental controls"
        context.localizedFallbackTitle = "Use PIN"

        context.evaluatePolicy(
            .deviceOwnerAuthenticationWithBiometrics,
            localizedReason: "Authenticate to access parental controls"
        ) { [weak self] success, error in
            DispatchQueue.main.async {
                if success {
                    self?.isUnlocked = true
                    self?.pinManager.resetFailedAttempts()
                }
                completion(success, error)
            }
        }
    }

    func validatePIN(_ pin: String) -> Bool {
        let isValid = pinManager.validatePIN(pin)
        if isValid {
            isUnlocked = true
        }
        updateCooldownStatus()
        return isValid
    }

    func validatePattern(_ pattern: [Int]) -> Bool {
        let isValid = patternManager.validatePattern(pattern)
        if isValid {
            isUnlocked = true
        }
        updatePatternCooldownStatus()
        return isValid
    }

    func setBiometricEnabled(_ enabled: Bool) {
        isBiometricEnabled = enabled
        UserDefaults.standard.set(enabled, forKey: "biometric_enabled")
    }

    private func updateCooldownStatus() {
        isInCooldown = pinManager.isInCooldown() || patternManager.isInCooldown()

        let pinAttempts = max(0, 5 - pinManager.getFailedAttempts())
        let patternAttempts = patternManager.getRemainingAttempts()
        attemptsRemaining = min(pinAttempts, patternAttempts)

        let pinCooldown = pinManager.getRemainingCooldownTime()
        let patternCooldown = patternManager.getRemainingCooldownTime()
        cooldownTimeRemaining = max(pinCooldown, patternCooldown)
    }

    private func updatePatternCooldownStatus() {
        isInCooldown = patternManager.isInCooldown()
        attemptsRemaining = patternManager.getRemainingAttempts()
        cooldownTimeRemaining = patternManager.getRemainingCooldownTime()
    }

    private func startCooldownTimer() {
        cooldownTimer?.invalidate()
        cooldownTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            self?.updateCooldownStatus()
            if self?.cooldownTimeRemaining == 0 {
                self?.cooldownTimer?.invalidate()
                self?.cooldownTimer = nil
            }
        }
    }

    deinit {
        cooldownTimer?.invalidate()
    }
}