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
}