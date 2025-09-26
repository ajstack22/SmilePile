import Foundation
import Combine

class PINEntryViewModel: ObservableObject {
    @Published var isLocked = false
    @Published var attemptsRemaining = 5
    @Published var cooldownTimeRemaining: TimeInterval = 0

    private let pinManager = PINManager.shared
    private var cancellables = Set<AnyCancellable>()
    private var cooldownTimer: Timer?

    init() {
        checkLockStatus()
    }

    func setPIN(_ pin: String) throws {
        try pinManager.setPIN(pin)
    }

    func validatePIN(_ pin: String) -> Bool {
        let isValid = pinManager.validatePIN(pin)
        updateLockStatus()
        return isValid
    }

    func isPINEnabled() -> Bool {
        return pinManager.isPINEnabled()
    }

    func clearPIN() throws {
        try pinManager.clearPIN()
    }

    func getFailedAttempts() -> Int {
        return pinManager.getFailedAttempts()
    }

    func getRemainingCooldownTime() -> TimeInterval {
        return pinManager.getRemainingCooldownTime()
    }

    func resetFailedAttempts() {
        pinManager.resetFailedAttempts()
        updateLockStatus()
    }

    private func checkLockStatus() {
        updateLockStatus()
        if pinManager.isInCooldown() {
            startCooldownTimer()
        }
    }

    private func updateLockStatus() {
        isLocked = pinManager.isInCooldown()
        attemptsRemaining = max(0, 5 - pinManager.getFailedAttempts())
        cooldownTimeRemaining = pinManager.getRemainingCooldownTime()
    }

    private func startCooldownTimer() {
        cooldownTimer?.invalidate()
        cooldownTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            self?.updateLockStatus()
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