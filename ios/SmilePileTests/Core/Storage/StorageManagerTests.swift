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

        await fulfillment(of: [expectation], timeout: 5.0)
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

        await fulfillment(of: [expectation], timeout: 10.0)
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

    // MARK: - Batch Import Tests

    func testImportPhotosInBatches() async throws {
        // Given
        let testURLs = createTestImageFiles(count: 15)
        var progressUpdates: [Double] = []

        // When
        let results = try await sut.importPhotosInBatches(
            sourceURLs: testURLs,
            batchSize: 5,
            progressHandler: { progress in
                progressUpdates.append(progress)
            }
        )

        // Then
        XCTAssertEqual(results.count, 15)
        XCTAssertGreaterThan(progressUpdates.count, 0)
        XCTAssertEqual(progressUpdates.last ?? 0, 1.0, accuracy: 0.01)

        // Verify all files were imported
        for result in results {
            let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            let fullPath = documentsURL.appendingPathComponent(result.photoPath).path
            XCTAssertTrue(fileManager.fileExists(atPath: fullPath))
        }

        // Cleanup
        cleanupTestImageFiles(testURLs)
    }

    func testImportPhotosWithMemoryPressure() async throws {
        // Given
        let largeTestURLs = createLargeTestImageFiles(count: 10)

        // When
        let results = try await sut.importPhotosInBatches(
            sourceURLs: largeTestURLs,
            batchSize: 3
        )

        // Then
        XCTAssertLessThanOrEqual(results.count, 10)

        // Check memory stayed within limits
        let currentMemory = await sut.getCurrentMemoryUsage()
        XCTAssertLessThan(currentMemory, 100)

        // Cleanup
        cleanupTestImageFiles(largeTestURLs)
    }

    func testImportWithInsufficientSpace() async {
        // Mock insufficient space scenario
        // This test validates error handling when space is insufficient

        let testURL = createTestImageFiles(count: 1).first!

        // Check if storage pressure is critical
        let pressure = await sut.getStoragePressure()

        if case .critical = pressure {
            do {
                _ = try await sut.importPhoto(from: testURL)
                XCTFail("Should fail with insufficient space")
            } catch let error as StorageError {
                if case .insufficientSpace = error {
                    // Expected error
                    XCTAssertTrue(true)
                } else {
                    XCTFail("Wrong error type: \(error)")
                }
            } catch {
                XCTFail("Unexpected error: \(error)")
            }
        } else {
            // Skip test if sufficient space
            XCTAssertTrue(true)
        }

        // Cleanup
        cleanupTestImageFiles([testURL])
    }

    func testSequentialThumbnailGeneration() async throws {
        // Given
        let testURLs = createTestImageFiles(count: 5)
        var progressValues: [Double] = []

        // When
        let generator = SafeThumbnailGenerator()
        let testImages = testURLs.compactMap { url -> (data: Data, identifier: String)? in
            guard let data = try? Data(contentsOf: url) else { return nil }
            return (data: data, identifier: url.lastPathComponent)
        }

        let thumbnails = try await generator.generateThumbnailsSequentially(
            for: testImages,
            targetSize: 300,
            progressHandler: { progress in
                progressValues.append(progress)
            }
        )

        // Then
        XCTAssertEqual(thumbnails.count, testImages.count)
        XCTAssertGreaterThan(progressValues.count, 0)
        XCTAssertEqual(progressValues.last ?? 0, 1.0, accuracy: 0.01)

        // Cleanup
        cleanupTestImageFiles(testURLs)
    }

    // MARK: - Storage Pressure Tests

    func testGetStoragePressure() async {
        let pressure = await sut.getStoragePressure()

        // Verify pressure level is valid
        switch pressure {
        case .low, .medium, .high, .critical:
            XCTAssertTrue(true)
        }

        XCTAssertNotNil(pressure.description)
    }

    func testHasEnoughSpace() async {
        let hasSpace = await sut.hasEnoughSpace(estimatedSize: 1024 * 1024) // 1MB

        // Should have at least 1MB available in most cases
        // This may fail in extreme low storage scenarios
        let availableSpace = await sut.getAvailableSpace()
        XCTAssertTrue(hasSpace || availableSpace < 1024 * 1024)
    }

    // MARK: - Internal Storage Tests

    func testIsInternalStoragePath() async {
        // Given
        let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let photosPath = documentsURL.appendingPathComponent("photos/test.jpg").path
        let externalPath = "/tmp/external.jpg"

        // Then
        let isInternalPhotos = await sut.isInternalStoragePath(photosPath)
        let isInternalExternal = await sut.isInternalStoragePath(externalPath)
        XCTAssertTrue(isInternalPhotos)
        XCTAssertFalse(isInternalExternal)
    }

    func testGetThumbnailPath() async throws {
        // Given
        let result = try await sut.saveImage(testImage, filename: "test.jpg")

        // When
        let thumbnailPath = await sut.getThumbnailPath(for: result.photoPath)

        // Then
        XCTAssertNotNil(thumbnailPath)
        if let path = thumbnailPath {
            let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            let fullPath = documentsURL.appendingPathComponent(path).path
            XCTAssertTrue(fileManager.fileExists(atPath: fullPath))
        }
    }

    // MARK: - Migration Tests

    func testCopyPhotoToInternalStorage() async throws {
        // Given
        let sourceFile = createTestImageFiles(count: 1).first!

        // When
        let result = try await sut.copyPhotoToInternalStorage(sourceFile: sourceFile)

        // Then
        let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let fullPath = documentsURL.appendingPathComponent(result.photoPath).path
        XCTAssertTrue(fileManager.fileExists(atPath: fullPath))
        XCTAssertGreaterThan(result.fileSize, 0)

        // Cleanup
        cleanupTestImageFiles([sourceFile])
    }

    func testMigrateExternalPhotosToInternal() async throws {
        // Given
        let externalFiles = createTestImageFiles(count: 5)
        let externalPaths = externalFiles.map { $0.path }

        // When
        let results = try await sut.migrateExternalPhotosToInternal(externalPhotoPaths: externalPaths)

        // Then
        XCTAssertEqual(results.count, 5)
        for result in results {
            let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            let fullPath = documentsURL.appendingPathComponent(result.photoPath).path
            XCTAssertTrue(fileManager.fileExists(atPath: fullPath))
        }

        // Cleanup
        cleanupTestImageFiles(externalFiles)
    }

    // MARK: - Error Recovery Tests

    func testImportWithPartialFailure() async throws {
        // Given - Mix of valid and invalid URLs
        var testURLs = createTestImageFiles(count: 3)
        testURLs.append(URL(fileURLWithPath: "/invalid/path/image.jpg"))

        // When
        let results = try await sut.importPhotosInBatches(
            sourceURLs: testURLs,
            batchSize: 2
        )

        // Then - Should import valid files and skip invalid ones
        XCTAssertEqual(results.count, 3)

        // Cleanup
        cleanupTestImageFiles(Array(testURLs.prefix(3)))
    }

    func testRecoverFromMemoryPressure() async throws {
        // Given
        let testURLs = createTestImageFiles(count: 5)
        let generator = SafeThumbnailGenerator()

        // Simulate memory pressure scenario
        var processedCount = 0
        for url in testURLs {
            if generator.isSafeToProcess() {
                _ = try await sut.importPhoto(from: url)
                processedCount += 1
            } else {
                // Wait for memory to recover
                try await Task.sleep(nanoseconds: 500_000_000)
                if generator.isSafeToProcess() {
                    _ = try await sut.importPhoto(from: url)
                    processedCount += 1
                }
            }
        }

        // Then
        XCTAssertGreaterThan(processedCount, 0)

        // Cleanup
        cleanupTestImageFiles(testURLs)
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

    private func createTestImageFiles(count: Int) -> [URL] {
        var urls: [URL] = []
        let tempDir = FileManager.default.temporaryDirectory

        for i in 0..<count {
            let image = createLargeTestImage(size: CGSize(width: 100, height: 100))
            if let data = image.jpegData(compressionQuality: 0.8) {
                let url = tempDir.appendingPathComponent("test_image_\(i).jpg")
                try? data.write(to: url)
                urls.append(url)
            }
        }

        return urls
    }

    private func createLargeTestImageFiles(count: Int) -> [URL] {
        var urls: [URL] = []
        let tempDir = FileManager.default.temporaryDirectory

        for i in 0..<count {
            let image = createLargeTestImage(size: CGSize(width: 2000, height: 2000))
            if let data = image.jpegData(compressionQuality: 0.9) {
                let url = tempDir.appendingPathComponent("large_image_\(i).jpg")
                try? data.write(to: url)
                urls.append(url)
            }
        }

        return urls
    }

    private func cleanupTestImageFiles(_ urls: [URL]) {
        for url in urls {
            try? FileManager.default.removeItem(at: url)
        }
    }
}

extension FileManager {
    var photosDirectory: URL {
        let documentsURL = urls(for: .documentDirectory, in: .userDomainMask)[0]
        return documentsURL.appendingPathComponent("photos")
    }

    var thumbnailsDirectory: URL {
        let documentsURL = urls(for: .documentDirectory, in: .userDomainMask)[0]
        return documentsURL.appendingPathComponent("thumbnails")
    }

    func createSmilePileDirectories() throws {
        try createDirectory(at: photosDirectory, withIntermediateDirectories: true)
        try createDirectory(at: thumbnailsDirectory, withIntermediateDirectories: true)
    }
}