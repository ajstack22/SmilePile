import XCTest
@testable import SmilePile

class DIContainerTests: XCTestCase {

    var sut: DIContainer!

    override func setUp() {
        super.setUp()
        sut = DIContainer.shared
        sut.reset()
    }

    override func tearDown() {
        sut.reset()
        super.tearDown()
    }

    func testServiceRegistrationAndResolution() {
        // Given
        class TestService {
            let id = UUID()
        }

        // When
        sut.register(TestService.self) {
            TestService()
        }

        // Then
        let service1 = sut.resolve(TestService.self)
        let service2 = sut.resolve(TestService.self)

        XCTAssertNotNil(service1)
        XCTAssertNotNil(service2)
        XCTAssertEqual(service1?.id, service2?.id) // Singleton by default
    }

    func testTransientScope() {
        // Given
        class TestService {
            let id = UUID()
        }

        // When
        sut.register(TestService.self, scope: .transient) {
            TestService()
        }

        // Then
        let service1 = sut.resolve(TestService.self)
        let service2 = sut.resolve(TestService.self)

        XCTAssertNotNil(service1)
        XCTAssertNotNil(service2)
        XCTAssertNotEqual(service1?.id, service2?.id) // Different instances
    }

    func testPropertyWrapper() {
        // Given
        protocol TestProtocol {
            var value: String { get }
        }

        struct TestService: TestProtocol {
            let value = "Test"
        }

        struct Consumer {
            @Injected var service: TestProtocol
        }

        // When
        sut.register(TestProtocol.self) {
            TestService()
        }

        // Then
        var consumer = Consumer()
        XCTAssertEqual(consumer.service.value, "Test")
    }

    func testThreadSafety() {
        // Given
        class TestService {}
        let expectation = XCTestExpectation(description: "Thread safety")
        expectation.expectedFulfillmentCount = 100

        // When
        sut.register(TestService.self) {
            TestService()
        }

        // Then
        DispatchQueue.concurrentPerform(iterations: 100) { _ in
            _ = sut.resolve(TestService.self)
            expectation.fulfill()
        }

        wait(for: [expectation], timeout: 5.0)
    }

    func testReset() {
        // Given
        class TestService {}
        sut.register(TestService.self) {
            TestService()
        }

        // When
        sut.reset()

        // Then
        let service = sut.resolve(TestService.self)
        XCTAssertNil(service)
    }

    func testIsRegistered() {
        // Given
        class TestService {}

        // When
        XCTAssertFalse(sut.isRegistered(TestService.self))
        sut.register(TestService.self) {
            TestService()
        }

        // Then
        XCTAssertTrue(sut.isRegistered(TestService.self))
    }
}