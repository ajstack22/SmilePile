import Foundation
import CryptoKit
import CommonCrypto

class PINManager {
    static let shared = PINManager()

    private let keychain = KeychainManager.shared
    private let pinKey = "parental_pin"
    private let pinLengthKey = "parental_pin_length"
    private let attemptsKey = "failed_attempts"
    private let lastAttemptKey = "last_failed_attempt"
    private let cooldownDuration: TimeInterval = 30 // 30 seconds
    private let maxAttempts = 5
    private let iterations = 10000
    private let minPINLength = 4
    private let maxPINLength = 6

    private init() {}

    struct PINData: Codable {
        let hash: Data
        let salt: Data
    }

    struct AttemptsData: Codable {
        var count: Int
        var lastAttemptTime: Date?
    }

    // MARK: - PIN Management

    func setPIN(_ pin: String, length: Int? = nil) throws {
        let pinLength = length ?? pin.count
        try validatePINRequirements(pin, expectedLength: pinLength)

        let pinData = try createPINData(from: pin)
        try savePINData(pinData, length: pinLength)
        resetFailedAttempts()
    }

    private func validatePINRequirements(_ pin: String, expectedLength: Int) throws {
        try validatePINLength(pin)
        try validatePINMatchesExpectedLength(pin, expectedLength: expectedLength)
        try validatePINContainsOnlyNumbers(pin)
    }

    private func validatePINLength(_ pin: String) throws {
        guard pin.count >= minPINLength && pin.count <= maxPINLength else {
            throw PINError.invalidPIN
        }
    }

    private func validatePINMatchesExpectedLength(_ pin: String, expectedLength: Int) throws {
        guard pin.count == expectedLength else {
            throw PINError.invalidPIN
        }
    }

    private func validatePINContainsOnlyNumbers(_ pin: String) throws {
        guard pin.allSatisfy({ $0.isNumber }) else {
            throw PINError.invalidPIN
        }
    }

    private func createPINData(from pin: String) throws -> PINData {
        let salt = generateSalt()
        let hash = hashPIN(pin, salt: salt)
        return PINData(hash: hash, salt: salt)
    }

    private func savePINData(_ pinData: PINData, length: Int) throws {
        let encoded = try JSONEncoder().encode(pinData)
        try keychain.save(encoded, for: pinKey)
        UserDefaults.standard.set(length, forKey: pinLengthKey)
    }

    func validatePIN(_ pin: String) -> Bool {
        guard !isInCooldown() else { return false }

        do {
            let data = try keychain.load(for: pinKey)
            let pinData = try JSONDecoder().decode(PINData.self, from: data)

            let inputHash = hashPIN(pin, salt: pinData.salt)

            if inputHash == pinData.hash {
                resetFailedAttempts()
                return true
            } else {
                recordFailedAttempt()
                return false
            }
        } catch {
            return false
        }
    }

    func isPINEnabled() -> Bool {
        return keychain.exists(for: pinKey)
    }

    func clearPIN() throws {
        try keychain.delete(for: pinKey)
        UserDefaults.standard.removeObject(forKey: pinLengthKey)
        resetFailedAttempts()
    }

    func getPINLength() -> Int {
        let savedLength = UserDefaults.standard.integer(forKey: pinLengthKey)
        if savedLength >= minPINLength && savedLength <= maxPINLength {
            return savedLength
        }
        return 4 // Default to 4 digits for backward compatibility
    }

    func getMinPINLength() -> Int {
        return minPINLength
    }

    func getMaxPINLength() -> Int {
        return maxPINLength
    }

    // MARK: - Failed Attempts Management

    func recordFailedAttempt() {
        var attempts = getAttemptsData()
        attempts.count += 1
        attempts.lastAttemptTime = Date()
        saveAttemptsData(attempts)
    }

    func resetFailedAttempts() {
        try? keychain.delete(for: attemptsKey)
    }

    func getFailedAttempts() -> Int {
        return getAttemptsData().count
    }

    func isInCooldown() -> Bool {
        let attempts = getAttemptsData()

        if attempts.count < maxAttempts {
            return false
        }

        guard let lastAttempt = attempts.lastAttemptTime else {
            return false
        }

        return Date().timeIntervalSince(lastAttempt) < cooldownDuration
    }

    func getRemainingCooldownTime() -> TimeInterval {
        guard isInCooldown() else { return 0 }

        let attempts = getAttemptsData()
        guard let lastAttempt = attempts.lastAttemptTime else { return 0 }

        let elapsed = Date().timeIntervalSince(lastAttempt)
        return max(0, cooldownDuration - elapsed)
    }

    // MARK: - Private Helpers

    private func generateSalt() -> Data {
        var bytes = [UInt8](repeating: 0, count: 32)
        _ = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
        return Data(bytes)
    }

    private func hashPIN(_ pin: String, salt: Data) -> Data {
        guard let pinData = pin.data(using: .utf8) else {
            return Data()
        }

        var derivedKey = Data(repeating: 0, count: 32)

        derivedKey.withUnsafeMutableBytes { derivedKeyBytes in
            pinData.withUnsafeBytes { pinBytes in
                salt.withUnsafeBytes { saltBytes in
                    CCKeyDerivationPBKDF(
                        CCPBKDFAlgorithm(kCCPBKDF2),
                        pinBytes.bindMemory(to: Int8.self).baseAddress,
                        pinData.count,
                        saltBytes.bindMemory(to: UInt8.self).baseAddress,
                        salt.count,
                        CCPseudoRandomAlgorithm(kCCPRFHmacAlgSHA256),
                        UInt32(iterations),
                        derivedKeyBytes.bindMemory(to: UInt8.self).baseAddress,
                        32
                    )
                }
            }
        }

        return derivedKey
    }

    private func getAttemptsData() -> AttemptsData {
        guard let data = try? keychain.load(for: attemptsKey),
              let attempts = try? JSONDecoder().decode(AttemptsData.self, from: data) else {
            return AttemptsData(count: 0, lastAttemptTime: nil)
        }
        return attempts
    }

    private func saveAttemptsData(_ attempts: AttemptsData) {
        guard let data = try? JSONEncoder().encode(attempts) else { return }
        try? keychain.save(data, for: attemptsKey)
    }

    enum PINError: LocalizedError {
        case invalidPIN
        case pinNotSet
        case tooManyAttempts

        var errorDescription: String? {
            switch self {
            case .invalidPIN:
                return "PIN must be exactly 4 digits"
            case .pinNotSet:
                return "No PIN has been set"
            case .tooManyAttempts:
                return "Too many failed attempts. Please wait."
            }
        }
    }
}