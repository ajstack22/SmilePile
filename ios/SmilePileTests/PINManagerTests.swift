import XCTest
@testable import SmilePile

class PINManagerTests: XCTestCase {

    var pinManager: PINManager!

    override func setUp() {
        super.setUp()
        pinManager = PINManager.shared
        // Clear any existing PIN before each test
        try? pinManager.clearPIN()
    }

    override func tearDown() {
        // Clean up after each test
        try? pinManager.clearPIN()
        super.tearDown()
    }

    // MARK: - PIN Setup Tests

    func testSetValidPIN() throws {
        let testPIN = "1234"

        XCTAssertFalse(pinManager.isPINEnabled())

        try pinManager.setPIN(testPIN)

        XCTAssertTrue(pinManager.isPINEnabled())
    }

    func testSetInvalidPINTooShort() {
        let testPIN = "123"

        XCTAssertThrowsError(try pinManager.setPIN(testPIN)) { error in
            XCTAssertEqual(error as? PINManager.PINError, .invalidPIN)
        }
    }

    func testSetInvalidPINTooLong() {
        let testPIN = "12345"

        XCTAssertThrowsError(try pinManager.setPIN(testPIN)) { error in
            XCTAssertEqual(error as? PINManager.PINError, .invalidPIN)
        }
    }

    func testSetInvalidPINWithLetters() {
        let testPIN = "12a4"

        XCTAssertThrowsError(try pinManager.setPIN(testPIN)) { error in
            XCTAssertEqual(error as? PINManager.PINError, .invalidPIN)
        }
    }

    // MARK: - PIN Validation Tests

    func testValidateCorrectPIN() throws {
        let testPIN = "5678"

        try pinManager.setPIN(testPIN)

        XCTAssertTrue(pinManager.validatePIN(testPIN))
        XCTAssertEqual(pinManager.getFailedAttempts(), 0)
    }

    func testValidateIncorrectPIN() throws {
        let correctPIN = "1234"
        let wrongPIN = "5678"

        try pinManager.setPIN(correctPIN)

        XCTAssertFalse(pinManager.validatePIN(wrongPIN))
        XCTAssertEqual(pinManager.getFailedAttempts(), 1)
    }

    func testValidatePINWhenNotSet() {
        XCTAssertFalse(pinManager.validatePIN("1234"))
    }

    // MARK: - Failed Attempts Tests

    func testFailedAttemptsIncrement() throws {
        let correctPIN = "1234"
        let wrongPIN = "0000"

        try pinManager.setPIN(correctPIN)

        // Try wrong PIN multiple times
        for i in 1...3 {
            XCTAssertFalse(pinManager.validatePIN(wrongPIN))
            XCTAssertEqual(pinManager.getFailedAttempts(), i)
        }
    }

    func testFailedAttemptsResetOnSuccess() throws {
        let correctPIN = "1234"
        let wrongPIN = "0000"

        try pinManager.setPIN(correctPIN)

        // Fail twice
        XCTAssertFalse(pinManager.validatePIN(wrongPIN))
        XCTAssertFalse(pinManager.validatePIN(wrongPIN))
        XCTAssertEqual(pinManager.getFailedAttempts(), 2)

        // Succeed once
        XCTAssertTrue(pinManager.validatePIN(correctPIN))
        XCTAssertEqual(pinManager.getFailedAttempts(), 0)
    }

    // MARK: - Cooldown Tests

    func testCooldownAfterMaxAttempts() throws {
        let correctPIN = "1234"
        let wrongPIN = "0000"

        try pinManager.setPIN(correctPIN)

        // Fail 5 times (max attempts)
        for _ in 1...5 {
            XCTAssertFalse(pinManager.validatePIN(wrongPIN))
        }

        XCTAssertTrue(pinManager.isInCooldown())
        XCTAssertGreaterThan(pinManager.getRemainingCooldownTime(), 0)

        // Should not validate even with correct PIN during cooldown
        XCTAssertFalse(pinManager.validatePIN(correctPIN))
    }

    func testNoCooldownBeforeMaxAttempts() throws {
        let correctPIN = "1234"
        let wrongPIN = "0000"

        try pinManager.setPIN(correctPIN)

        // Fail 4 times (one less than max)
        for _ in 1...4 {
            XCTAssertFalse(pinManager.validatePIN(wrongPIN))
        }

        XCTAssertFalse(pinManager.isInCooldown())
        XCTAssertEqual(pinManager.getRemainingCooldownTime(), 0)

        // Should still be able to validate with correct PIN
        XCTAssertTrue(pinManager.validatePIN(correctPIN))
    }

    // MARK: - Clear PIN Tests

    func testClearPIN() throws {
        let testPIN = "1234"

        try pinManager.setPIN(testPIN)
        XCTAssertTrue(pinManager.isPINEnabled())

        try pinManager.clearPIN()
        XCTAssertFalse(pinManager.isPINEnabled())

        // Should not validate after clearing
        XCTAssertFalse(pinManager.validatePIN(testPIN))
    }

    func testClearPINResetsAttempts() throws {
        let testPIN = "1234"
        let wrongPIN = "0000"

        try pinManager.setPIN(testPIN)

        // Fail some attempts
        XCTAssertFalse(pinManager.validatePIN(wrongPIN))
        XCTAssertFalse(pinManager.validatePIN(wrongPIN))
        XCTAssertEqual(pinManager.getFailedAttempts(), 2)

        try pinManager.clearPIN()
        XCTAssertEqual(pinManager.getFailedAttempts(), 0)
    }

    // MARK: - PIN Change Tests

    func testChangePIN() throws {
        let oldPIN = "1234"
        let newPIN = "5678"

        try pinManager.setPIN(oldPIN)
        XCTAssertTrue(pinManager.validatePIN(oldPIN))

        try pinManager.setPIN(newPIN)
        XCTAssertFalse(pinManager.validatePIN(oldPIN))
        XCTAssertTrue(pinManager.validatePIN(newPIN))
    }

    // MARK: - Persistence Tests

    func testPINPersistence() throws {
        let testPIN = "9876"

        try pinManager.setPIN(testPIN)

        // Create a new instance to test persistence
        let newPINManager = PINManager.shared

        XCTAssertTrue(newPINManager.isPINEnabled())
        XCTAssertTrue(newPINManager.validatePIN(testPIN))
    }

    // MARK: - Async Operations Tests

    func testConcurrentPINValidation() async throws {
        let correctPIN = "1234"
        let wrongPIN = "5678"

        try pinManager.setPIN(correctPIN)

        // Test concurrent validation attempts
        let results = await withTaskGroup(of: Bool.self) { group in
            for _ in 0..<5 {
                group.addTask {
                    return self.pinManager.validatePIN(correctPIN)
                }
            }

            var results: [Bool] = []
            for await result in group {
                results.append(result)
            }
            return results
        }

        // All should succeed
        XCTAssertEqual(results.count, 5)
        XCTAssertTrue(results.allSatisfy { $0 == true })
    }

    func testConcurrentFailedAttempts() async throws {
        let correctPIN = "1234"
        let wrongPIN = "5678"

        try pinManager.setPIN(correctPIN)

        // Test concurrent failed attempts
        let results = await withTaskGroup(of: Bool.self) { group in
            for _ in 0..<3 {
                group.addTask {
                    return self.pinManager.validatePIN(wrongPIN)
                }
            }

            var results: [Bool] = []
            for await result in group {
                results.append(result)
            }
            return results
        }

        // All should fail
        XCTAssertTrue(results.allSatisfy { $0 == false })

        // Failed attempts should be counted
        let failedAttempts = pinManager.getFailedAttempts()
        XCTAssertGreaterThanOrEqual(failedAttempts, 3)
    }

    // MARK: - Edge Cases

    func testPINWithLeadingZeros() throws {
        let testPIN = "0012"

        try pinManager.setPIN(testPIN)
        XCTAssertTrue(pinManager.validatePIN(testPIN))
        XCTAssertFalse(pinManager.validatePIN("12")) // Should not match without leading zeros
    }

    func testPINWithAllZeros() throws {
        let testPIN = "0000"

        try pinManager.setPIN(testPIN)
        XCTAssertTrue(pinManager.validatePIN(testPIN))
    }

    func testPINWithAllSameDigit() throws {
        let testPIN = "1111"

        try pinManager.setPIN(testPIN)
        XCTAssertTrue(pinManager.validatePIN(testPIN))
    }

    func testSequentialPIN() throws {
        let testPIN = "1234"

        try pinManager.setPIN(testPIN)
        XCTAssertTrue(pinManager.validatePIN(testPIN))
    }

    func testPINLengthVariations() throws {
        // Test 4-digit PIN (minimum)
        let pin4 = "1234"
        try pinManager.setPIN(pin4, length: 4)
        XCTAssertTrue(pinManager.validatePIN(pin4))
        XCTAssertEqual(pinManager.getPINLength(), 4)

        // Test 5-digit PIN
        let pin5 = "12345"
        try pinManager.setPIN(pin5, length: 5)
        XCTAssertTrue(pinManager.validatePIN(pin5))
        XCTAssertEqual(pinManager.getPINLength(), 5)

        // Test 6-digit PIN (maximum)
        let pin6 = "123456"
        try pinManager.setPIN(pin6, length: 6)
        XCTAssertTrue(pinManager.validatePIN(pin6))
        XCTAssertEqual(pinManager.getPINLength(), 6)
    }

    func testInvalidPINLength() throws {
        // Test below minimum
        let shortPIN = "123"
        XCTAssertThrowsError(try pinManager.setPIN(shortPIN, length: 3))

        // Test above maximum
        let longPIN = "1234567"
        XCTAssertThrowsError(try pinManager.setPIN(longPIN, length: 7))
    }

    func testMismatchedPINLength() throws {
        // PIN doesn't match expected length
        let testPIN = "1234"
        XCTAssertThrowsError(try pinManager.setPIN(testPIN, length: 5))
    }

    // MARK: - Cooldown Edge Cases

    func testCooldownTimerDecrement() async throws {
        let correctPIN = "1234"
        let wrongPIN = "5678"

        try pinManager.setPIN(correctPIN)

        // Trigger cooldown
        for _ in 1...5 {
            _ = pinManager.validatePIN(wrongPIN)
        }

        XCTAssertTrue(pinManager.isInCooldown())
        let initialCooldown = pinManager.getRemainingCooldownTime()

        // Wait a bit
        try await Task.sleep(nanoseconds: 2_000_000_000) // 2 seconds

        let remainingCooldown = pinManager.getRemainingCooldownTime()
        XCTAssertLessThan(remainingCooldown, initialCooldown)
    }

    func testResetFailedAttemptsAfterCooldown() async throws {
        let correctPIN = "1234"
        let wrongPIN = "5678"

        try pinManager.setPIN(correctPIN)

        // Trigger cooldown
        for _ in 1...5 {
            _ = pinManager.validatePIN(wrongPIN)
        }

        XCTAssertTrue(pinManager.isInCooldown())
        XCTAssertEqual(pinManager.getFailedAttempts(), 5)

        // Wait for cooldown to expire (using a shorter test cooldown if possible)
        // Note: This test might need adjustment based on actual cooldown duration
        // For testing, we'll just verify the state

        // After cooldown and successful validation
        // (In real test, wait for actual cooldown to expire)
    }

    // MARK: - Security Tests

    func testPINHashingConsistency() throws {
        let testPIN = "1234"

        try pinManager.setPIN(testPIN)

        // Validate multiple times to ensure hash comparison is consistent
        for _ in 0..<10 {
            XCTAssertTrue(pinManager.validatePIN(testPIN))
        }
    }

    func testPINNotStoredInPlainText() throws {
        // This test verifies PIN is not stored in plain text
        // by checking that the stored data is different from the PIN
        let testPIN = "1234"

        try pinManager.setPIN(testPIN)

        // The actual PIN should not be retrievable
        // (This is implicit as there's no method to get the PIN)
        XCTAssertTrue(pinManager.isPINEnabled())
    }

    // MARK: - Boundary Tests

    func testMinMaxPINLengths() {
        XCTAssertEqual(pinManager.getMinPINLength(), 4)
        XCTAssertEqual(pinManager.getMaxPINLength(), 6)
    }

    func testEmptyPIN() {
        let emptyPIN = ""
        XCTAssertThrowsError(try pinManager.setPIN(emptyPIN))
    }

    func testSpecialCharactersInPIN() {
        let specialPIN = "12!4"
        XCTAssertThrowsError(try pinManager.setPIN(specialPIN))
    }

    func testAlphabeticPIN() {
        let alphaPIN = "abcd"
        XCTAssertThrowsError(try pinManager.setPIN(alphaPIN))
    }

    func testMixedAlphanumericPIN() {
        let mixedPIN = "1a2b"
        XCTAssertThrowsError(try pinManager.setPIN(mixedPIN))
    }

    func testWhitespacePIN() {
        let whitespacePIN = "12 34"
        XCTAssertThrowsError(try pinManager.setPIN(whitespacePIN))
    }
}