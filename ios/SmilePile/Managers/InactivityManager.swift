import Foundation
import SwiftUI
import Combine

/// Monitors user inactivity and automatically returns to Kids Mode after timeout
/// Matches Android InactivityManager behavior for security parity
@MainActor
class InactivityManager: ObservableObject {
    // MARK: - Properties

    /// Timeout interval in seconds (default: 5 minutes)
    static let defaultTimeout: TimeInterval = 300 // 5 minutes

    @Published var shouldReturnToKidsMode = false

    private var timer: Timer?
    private let timeoutInterval: TimeInterval

    // MARK: - Initialization

    init(timeoutInterval: TimeInterval = defaultTimeout) {
        self.timeoutInterval = timeoutInterval
    }

    // MARK: - Public Methods

    /// Start or reset the inactivity timer
    func resetTimer() {
        stopTimer()
        timer = Timer.scheduledTimer(withTimeInterval: timeoutInterval, repeats: false) { [weak self] _ in
            Task { @MainActor in
                self?.handleTimeout()
            }
        }
    }

    /// Stop the inactivity timer (call when re-entering Kids Mode)
    func stopTimer() {
        timer?.invalidate()
        timer = nil
    }

    /// Handle timeout event - return to Kids Mode
    private func handleTimeout() {
        shouldReturnToKidsMode = true
    }

    /// Reset the "should return" flag after Kids Mode is restored
    func acknowledgeReturn() {
        shouldReturnToKidsMode = false
    }
}
