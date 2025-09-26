import Foundation
import CryptoKit

class PatternManager {
    static let shared = PatternManager()
    private let keychainManager = KeychainManager.shared

    private let patternKey = "com.smilepile.pattern"
    private let patternSaltKey = "com.smilepile.pattern.salt"
    private let patternAttemptsKey = "com.smilepile.pattern.attempts"
    private let patternCooldownKey = "com.smilepile.pattern.cooldown"

    private let maxAttempts = 5
    private let cooldownDuration: TimeInterval = 30

    private init() {}

    func setPattern(_ pattern: [Int]) -> Bool {
        guard isValidPattern(pattern) else { return false }

        let salt = generateSalt()
        guard let hashedPattern = hashPattern(pattern, with: salt) else { return false }

        do {
            try keychainManager.save(hashedPattern, for: patternKey)
            try keychainManager.save(salt, for: patternSaltKey)
            resetFailedAttempts()
            return true
        } catch {
            return false
        }
    }

    func validatePattern(_ pattern: [Int]) -> Bool {
        guard !isInCooldown() else { return false }

        do {
            let storedHash = try keychainManager.load(for: patternKey)
            let salt = try keychainManager.load(for: patternSaltKey)

            guard let hashedInput = hashPattern(pattern, with: salt) else {
                incrementFailedAttempts()
                return false
            }

            let isValid = hashedInput == storedHash

            if isValid {
                resetFailedAttempts()
            } else {
                incrementFailedAttempts()
            }

            return isValid
        } catch {
            incrementFailedAttempts()
            return false
        }
    }

    func hasPattern() -> Bool {
        do {
            _ = try keychainManager.load(for: patternKey)
            return true
        } catch {
            return false
        }
    }

    func removePattern() -> Bool {
        do {
            try keychainManager.delete(for: patternKey)
            try keychainManager.delete(for: patternSaltKey)
            resetFailedAttempts()
            return true
        } catch {
            return false
        }
    }

    func isValidPattern(_ pattern: [Int]) -> Bool {
        guard pattern.count >= 4 && pattern.count <= 9 else { return false }

        let uniqueDots = Set(pattern)
        guard uniqueDots.count == pattern.count else { return false }

        for dot in pattern {
            guard dot >= 0 && dot < 9 else { return false }
        }

        return true
    }

    func isInCooldown() -> Bool {
        guard let cooldownEndTime = UserDefaults.standard.object(forKey: patternCooldownKey) as? Date else {
            return false
        }

        return Date() < cooldownEndTime
    }

    func getRemainingCooldownTime() -> TimeInterval {
        guard let cooldownEndTime = UserDefaults.standard.object(forKey: patternCooldownKey) as? Date else {
            return 0
        }

        let remaining = cooldownEndTime.timeIntervalSince(Date())
        return max(0, remaining)
    }

    func getRemainingAttempts() -> Int {
        let attempts = UserDefaults.standard.integer(forKey: patternAttemptsKey)
        return max(0, maxAttempts - attempts)
    }

    private func hashPattern(_ pattern: [Int], with salt: Data) -> Data? {
        let patternString = pattern.map { String($0) }.joined(separator: ",")
        guard let patternData = patternString.data(using: .utf8) else { return nil }

        var combinedData = Data()
        combinedData.append(patternData)
        combinedData.append(salt)

        let hashedData = SHA256.hash(data: combinedData)
        return Data(hashedData)
    }

    private func generateSalt() -> Data {
        var salt = Data(count: 32)
        _ = salt.withUnsafeMutableBytes { bytes in
            SecRandomCopyBytes(kSecRandomDefault, 32, bytes.baseAddress!)
        }
        return salt
    }

    private func incrementFailedAttempts() {
        var attempts = UserDefaults.standard.integer(forKey: patternAttemptsKey)
        attempts += 1
        UserDefaults.standard.set(attempts, forKey: patternAttemptsKey)

        if attempts >= maxAttempts {
            let cooldownEndTime = Date().addingTimeInterval(cooldownDuration)
            UserDefaults.standard.set(cooldownEndTime, forKey: patternCooldownKey)
        }
    }

    private func resetFailedAttempts() {
        UserDefaults.standard.removeObject(forKey: patternAttemptsKey)
        UserDefaults.standard.removeObject(forKey: patternCooldownKey)
    }

    func patternToString(_ pattern: [Int]) -> String {
        return pattern.map { String($0) }.joined(separator: "-")
    }

    func stringToPattern(_ string: String) -> [Int]? {
        let components = string.split(separator: "-")
        let pattern = components.compactMap { Int($0) }
        return pattern.count == components.count ? pattern : nil
    }
}