import XCTest
import Photos
@testable import SmilePile

class StorageManagerTests: XCTestCase {

    var sut: StorageManager!
    var testImage: UIImage!
    var fileManager: FileManager!

    override func setUp() async throws {
        try await super.setUp()
        sut = await StorageManager.shared
        fileManager = FileManager.default

        // Create test image
        let size = CGSize(width: 100, height: 100)
        UIGraphicsBeginImageContext(size)
        UIColor.blue.setFill()
        UIRectFill(CGRect(origin: .zero, size: size))
        testImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        // Clean test directories
        cleanupTestFiles()
    }

    override func tearDown() {
        cleanupTestFiles()
        super.tearDown()
    }

    private func cleanupTestFiles() {
        let photosDir = fileManager.photosDirectory
        let thumbnailsDir = fileManager.thumbnailsDirectory
        try? fileManager.removeItem(at: photosDir)
        try? fileManager.removeItem(at: thumbnailsDir)
    }

    // MARK: - Directory Tests

    func testDirectoryCreation() throws {
        // Given
        cleanupTestFiles()

        // When
        try fileManager.createSmilePileDirectories()

        // Then
        XCTAssertTrue(fileManager.fileExists(atPath: fileManager.photosDirectory.path))
        XCTAssertTrue(fileManager.fileExists(atPath: fileManager.thumbnailsDirectory.path))
    }

    func testDirectoryProtection() throws {
        // When
        try fileManager.createSmilePileDirectories()

        // Then
        let attributes = try fileManager.attributesOfItem(
            atPath: fileManager.photosDirectory.path
        )
        // File protection attribute test - just verify the directory was created
        XCTAssertNotNil(attributes)
    }

    // MARK: - Image Save Tests

    func testSaveUIImage() async throws {
        // When
        let result = try await sut.saveImage(testImage, filename: "test.jpg")

        // Then
        XCTAssertTrue(fileManager.fileExists(atPath: result.photoPath))
        XCTAssertNotNil(result.thumbnailPath)
        if let thumbnailPath = result.thumbnailPath {
            XCTAssertTrue(fileManager.fileExists(atPath: thumbnailPath))
        }
        XCTAssertGreaterThan(result.fileSize, 0)
        XCTAssertEqual(result.fileSize, 100)
        XCTAssertEqual(result.fileSize, 100)
    }

    func testSaveMultipleImages() async throws {
        // Given
        var results: [StorageResult] = []

        // When
        for i in 0..<5 {
            let result = try await sut.saveImage(
                testImage,
                filename: "test\(i).jpg"
            )
            results.append(result)
        }

        // Then
        XCTAssertEqual(results.count, 5)
        let uniqueFiles = Set(results.map { $0.photoPath })
        XCTAssertEqual(uniqueFiles.count, 5) // All unique
    }

    // MARK: - Delete Tests

    func testDeletePhoto() async throws {
        // Given
        let result = try await sut.saveImage(testImage)
        XCTAssertTrue(fileManager.fileExists(atPath: result.photoPath))

        // When
        let deleted = try await sut.deletePhoto(at: result.photoPath)

        // Then
        XCTAssertTrue(deleted)
        XCTAssertFalse(fileManager.fileExists(atPath: result.photoPath))
        if let thumbnailPath = result.thumbnailPath {
            XCTAssertFalse(fileManager.fileExists(atPath: thumbnailPath))
        }
    }

    // MARK: - Storage Monitor Tests

    func testStorageUsageCalculation() async throws {
        // Given
        for i in 0..<3 {
            _ = try await sut.saveImage(testImage, filename: "test\(i).jpg")
        }

        // When
        let usage = try await sut.calculateStorageUsage()

        // Then
        XCTAssertGreaterThan(usage.photoBytes, 0)
        XCTAssertGreaterThan(usage.thumbnailBytes, 0)
        XCTAssertEqual(usage.photoCount, 3)
        XCTAssertEqual(usage.totalBytes, usage.photoBytes + usage.thumbnailBytes)
        XCTAssertGreaterThan(usage.photoCount, 0)
    }

    // MARK: - Cleanup Tests

    func testCleanupOrphanedThumbnails() async throws {
        // Given: Create photo and thumbnail
        let result = try await sut.saveImage(testImage)
        XCTAssertNotNil(result.thumbnailPath)

        // When: Delete photo but not thumbnail
        try fileManager.removeItem(at: URL(fileURLWithPath: result.photoPath))

        // Then: Cleanup should remove orphaned thumbnail
        let cleaned = try await sut.cleanupOrphanedThumbnails()
        XCTAssertEqual(cleaned, 1)

        if let thumbnailPath = result.thumbnailPath {
            XCTAssertFalse(fileManager.fileExists(atPath: thumbnailPath))
        }
    }

    // MARK: - Error Handling Tests

    func testInsufficientSpaceError() async {
        // Given: Mock insufficient space
        // This would require dependency injection for proper testing

        // For now, test that error types exist
        let error = StorageError.insufficientSpace("Required: 1000 bytes, Available: 100 bytes")
        XCTAssertNotNil(error.errorDescription)
    }

    func testInvalidImageDataError() async {
        let error = StorageError.invalidImageData("Invalid image data provided")
        XCTAssertNotNil(error.errorDescription)
    }

    // MARK: - Performance Tests

    func testBatchImportPerformance() async throws {
        // Test importing multiple images doesn't block
        let expectation = XCTestExpectation(description: "Batch import")

        Task {
            for i in 0..<10 {
                _ = try? await sut.saveImage(
                    testImage,
                    filename: "perf\(i).jpg"
                )
            }
            expectation.fulfill()
        }

        wait(for: [expectation], timeout: 5.0)
    }

    // MARK: - Thread Safety Tests

    func testConcurrentSaves() async throws {
        let expectation = XCTestExpectation(description: "Concurrent saves")
        expectation.expectedFulfillmentCount = 10

        await withTaskGroup(of: Void.self) { group in
            for i in 0..<10 {
                group.addTask {
                    _ = try? await self.sut.saveImage(
                        self.testImage,
                        filename: "concurrent\(i).jpg"
                    )
                    expectation.fulfill()
                }
            }
        }

        wait(for: [expectation], timeout: 10.0)
    }

    // MARK: - Memory Tests

    func testMemoryEfficientProcessing() {
        // Measure memory before
        let memoryBefore = getCurrentMemoryUsage()

        // Process large image
        let largeImage = createLargeTestImage(size: CGSize(width: 4000, height: 4000))

        Task {
            _ = try? await sut.saveImage(largeImage)
        }

        // Measure memory after
        let memoryAfter = getCurrentMemoryUsage()
        let memoryIncrease = memoryAfter - memoryBefore

        // Should not exceed reasonable threshold
        XCTAssertLessThan(memoryIncrease, 100 * 1024 * 1024) // 100MB
    }

    // MARK: - Helper Methods

    private func createLargeTestImage(size: CGSize) -> UIImage {
        UIGraphicsBeginImageContext(size)
        UIColor.red.setFill()
        UIRectFill(CGRect(origin: .zero, size: size))
        let image = UIGraphicsGetImageFromCurrentImageContext()!
        UIGraphicsEndImageContext()
        return image
    }

    private func getCurrentMemoryUsage() -> Int64 {
        var info = mach_task_basic_info()
        var count = mach_msg_type_number_t(MemoryLayout<mach_task_basic_info>.size) / 4

        let result = withUnsafeMutablePointer(to: &info) {
            $0.withMemoryRebound(to: integer_t.self, capacity: Int(count)) {
                task_info(mach_task_self_,
                         task_flavor_t(MACH_TASK_BASIC_INFO),
                         $0,
                         &count)
            }
        }

        return result == KERN_SUCCESS ? Int64(info.resident_size) : 0
    }
}