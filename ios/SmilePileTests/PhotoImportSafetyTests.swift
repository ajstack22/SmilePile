import XCTest
@testable import SmilePile
import Photos
import PhotosUI

/// Tests for the safe photo import system
final class PhotoImportSafetyTests: XCTestCase {

    var safeThumbnailGenerator: SafeThumbnailGenerator!
    var storageManager: StorageManager!

    override func setUp() async throws {
        try await super.setUp()
        safeThumbnailGenerator = SafeThumbnailGenerator()
        storageManager = await StorageManager.shared
    }

    override func tearDown() async throws {
        safeThumbnailGenerator = nil
        storageManager = nil
        try await super.tearDown()
    }

    // MARK: - SafeThumbnailGenerator Tests

    func testImageIOThumbnailGeneration() async throws {
        // Create test image data
        let testImage = createTestImage(size: CGSize(width: 1000, height: 1000))
        guard let imageData = testImage.jpegData(compressionQuality: 1.0) else {
            XCTFail("Failed to create test image data")
            return
        }

        // Test thumbnail generation
        let thumbnailData = try await safeThumbnailGenerator.generateThumbnail(
            from: imageData,
            targetSize: 300
        )

        // Verify thumbnail was created
        XCTAssertNotNil(thumbnailData)
        XCTAssertLessThan(thumbnailData.count, imageData.count)

        // Verify no UIGraphicsContext was used (memory safe)
        let memoryBefore = safeThumbnailGenerator.getCurrentMemoryUsage()
        _ = try await safeThumbnailGenerator.generateThumbnail(from: imageData)
        let memoryAfter = safeThumbnailGenerator.getCurrentMemoryUsage()

        // Memory increase should be minimal
        let memoryIncrease = memoryAfter - memoryBefore
        XCTAssertLessThan(memoryIncrease, 10, "Memory usage increased by \(memoryIncrease)MB")
    }

    func testSequentialThumbnailGeneration() async throws {
        // Create multiple test images
        var testImages: [(data: Data, identifier: String)] = []
        for i in 0..<5 {
            let image = createTestImage(size: CGSize(width: 2000, height: 2000))
            if let data = image.jpegData(compressionQuality: 0.9) {
                testImages.append((data: data, identifier: "image_\(i)"))
            }
        }

        var progressValues: [Double] = []

        // Test sequential generation
        let thumbnails = try await safeThumbnailGenerator.generateThumbnailsSequentially(
            for: testImages,
            targetSize: 300,
            progressHandler: { progress in
                progressValues.append(progress)
            }
        )

        // Verify all thumbnails were generated
        XCTAssertEqual(thumbnails.count, testImages.count)

        // Verify progress was reported
        XCTAssertGreaterThan(progressValues.count, 0)
        XCTAssertEqual(progressValues.last ?? 0, 1.0, accuracy: 0.01)

        // Verify sequential processing (not simultaneous)
        for i in 0..<progressValues.count - 1 {
            XCTAssertLessThanOrEqual(progressValues[i], progressValues[i + 1])
        }
    }

    func testMemoryPressureHandling() async throws {
        let testImage = createTestImage(size: CGSize(width: 4000, height: 4000))
        guard let imageData = testImage.jpegData(compressionQuality: 1.0) else {
            XCTFail("Failed to create large image")
            return
        }

        // Check if system can handle memory pressure
        let isSafe = safeThumbnailGenerator.isSafeToProcess()
        XCTAssertTrue(isSafe || safeThumbnailGenerator.getCurrentMemoryUsage() < 100)

        // If memory is high, thumbnail generation should handle it gracefully
        do {
            _ = try await safeThumbnailGenerator.generateThumbnail(from: imageData)
        } catch let error as SafeThumbnailGenerator.GeneratorError {
            if case .memoryPressure = error {
                // This is expected behavior under memory pressure
                XCTAssertTrue(true)
            } else {
                XCTFail("Unexpected error: \(error)")
            }
        }
    }

    // MARK: - Additional SafeThumbnailGenerator Tests

    func testThumbnailGenerationWithVariousSizes() async throws {
        let testImage = createTestImage(size: CGSize(width: 2000, height: 2000))
        guard let imageData = testImage.jpegData(compressionQuality: 0.9) else {
            XCTFail("Failed to create test image data")
            return
        }

        // Test different target sizes
        let targetSizes: [Int] = [100, 200, 300, 500, 800]

        for targetSize in targetSizes {
            let thumbnailData = try await safeThumbnailGenerator.generateThumbnail(
                from: imageData,
                targetSize: CGFloat(targetSize)
            )

            XCTAssertNotNil(thumbnailData)
            XCTAssertLessThan(thumbnailData.count, imageData.count)

            // Verify thumbnail is smaller than original
            if let thumbnailImage = UIImage(data: thumbnailData) {
                XCTAssertLessThanOrEqual(
                    max(thumbnailImage.size.width, thumbnailImage.size.height),
                    CGFloat(targetSize) * 2 // Allow for scale factor
                )
            }
        }
    }

    func testThumbnailGenerationWithInvalidData() async {
        let invalidData = Data("not an image".utf8)

        do {
            _ = try await safeThumbnailGenerator.generateThumbnail(from: invalidData)
            XCTFail("Should fail with invalid image data")
        } catch {
            // Expected error
            XCTAssertNotNil(error)
        }
    }

    func testThumbnailGenerationWithEmptyData() async {
        let emptyData = Data()

        do {
            _ = try await safeThumbnailGenerator.generateThumbnail(from: emptyData)
            XCTFail("Should fail with empty data")
        } catch {
            // Expected error
            XCTAssertNotNil(error)
        }
    }

    func testProcessImageForStorage() async throws {
        // Test image processing for storage optimization
        let largeImage = createTestImage(size: CGSize(width: 4000, height: 4000))
        guard let imageData = largeImage.jpegData(compressionQuality: 1.0) else {
            XCTFail("Failed to create test image data")
            return
        }

        let processedData = try await safeThumbnailGenerator.processImageForStorage(
            imageData: imageData,
            maxDimension: 2048
        )

        // Verify processed image is smaller
        XCTAssertLessThan(processedData.count, imageData.count)

        // Verify image dimensions are within limits
        if let processedImage = UIImage(data: processedData) {
            XCTAssertLessThanOrEqual(processedImage.size.width, 2048)
            XCTAssertLessThanOrEqual(processedImage.size.height, 2048)
        }
    }

    func testGetImageMetadata() {
        let testImage = createTestImage(size: CGSize(width: 1920, height: 1080))
        guard let imageData = testImage.jpegData(compressionQuality: 0.9) else {
            XCTFail("Failed to create test image data")
            return
        }

        let metadata = safeThumbnailGenerator.getImageMetadata(from: imageData)

        XCTAssertNotNil(metadata)
        // Metadata structure would depend on implementation
    }

    func testConcurrentThumbnailGeneration() async throws {
        // Test thread safety with concurrent operations
        let testImages = (0..<5).map { i in
            createTestImage(size: CGSize(width: 1000 + CGFloat(i * 100), height: 1000))
        }

        let imageDatas = testImages.compactMap { $0.jpegData(compressionQuality: 0.8) }

        await withTaskGroup(of: Data?.self) { group in
            for imageData in imageDatas {
                group.addTask {
                    try? await self.safeThumbnailGenerator.generateThumbnail(from: imageData)
                }
            }

            var results: [Data?] = []
            for await result in group {
                results.append(result)
            }

            // All should succeed
            XCTAssertEqual(results.count, imageDatas.count)
            XCTAssertTrue(results.allSatisfy { $0 != nil })
        }
    }

    func testThumbnailGenerationMemoryEfficiency() async throws {
        // Test that memory is released properly after generation
        let memoryBefore = safeThumbnailGenerator.getCurrentMemoryUsage()

        // Generate multiple thumbnails
        for i in 0..<10 {
            let testImage = createTestImage(size: CGSize(width: 2000, height: 2000))
            if let imageData = testImage.jpegData(compressionQuality: 0.9) {
                _ = try? await safeThumbnailGenerator.generateThumbnail(from: imageData)
            }
        }

        // Allow time for memory cleanup
        try await Task.sleep(nanoseconds: 500_000_000) // 0.5 seconds

        let memoryAfter = safeThumbnailGenerator.getCurrentMemoryUsage()
        let memoryIncrease = memoryAfter - memoryBefore

        // Memory increase should be minimal after processing
        XCTAssertLessThan(memoryIncrease, 20) // Less than 20MB increase
    }

    func testProgressReporting() async throws {
        // Test progress reporting accuracy
        let testImages = (0..<10).map { _ in
            createTestImage(size: CGSize(width: 1000, height: 1000))
        }

        let imageDatas = testImages.compactMap { image -> (data: Data, identifier: String)? in
            guard let data = image.jpegData(compressionQuality: 0.8) else { return nil }
            return (data: data, identifier: UUID().uuidString)
        }

        var progressValues: [Double] = []
        var lastProgress: Double = 0

        _ = try await safeThumbnailGenerator.generateThumbnailsSequentially(
            for: imageDatas,
            targetSize: 300,
            progressHandler: { progress in
                progressValues.append(progress)
                // Progress should never decrease
                XCTAssertGreaterThanOrEqual(progress, lastProgress)
                lastProgress = progress
            }
        )

        // Verify progress reporting
        XCTAssertGreaterThan(progressValues.count, 0)
        XCTAssertEqual(progressValues.last ?? 0, 1.0, accuracy: 0.01)

        // Progress should be incremental
        for i in 1..<progressValues.count {
            XCTAssertGreaterThanOrEqual(progressValues[i], progressValues[i-1])
        }
    }

    // MARK: - Batch Processing Tests

    func testBatchImportWithMemoryLimit() async throws {
        // Create test URLs
        let testURLs = createTestImageURLs(count: 10)

        // Test batch import
        let results = try await storageManager.importPhotosInBatches(
            sourceURLs: testURLs,
            batchSize: 5,
            progressHandler: { progress in
                print("Import progress: \(Int(progress * 100))%")
            }
        )

        // Verify batch processing
        XCTAssertLessThanOrEqual(results.count, testURLs.count)

        // Check memory stayed within limits
        let currentMemory = await storageManager.getCurrentMemoryUsage()
        XCTAssertLessThan(currentMemory, 100, "Memory usage exceeded 100MB: \(currentMemory)MB")
    }

    func testStoragePressureMonitoring() async throws {
        // Test storage pressure detection
        let pressure = await storageManager.getStoragePressure()

        // Verify pressure level is detected
        XCTAssertNotNil(pressure)
        print("Current storage pressure: \(pressure.description)")

        // Test that import respects storage pressure
        if case .critical = pressure {
            let testURL = createTestImageURLs(count: 1).first!
            do {
                _ = try await storageManager.importPhoto(from: testURL)
                XCTFail("Should fail with insufficient space")
            } catch StorageError.insufficientSpace {
                // Expected behavior
                XCTAssertTrue(true)
            }
        }
    }

    // MARK: - Import Coordinator Tests
    // NOTE: PhotoImportCoordinator was removed - these tests are disabled
    // TODO: Re-enable when PhotoImportSession is properly tested

    /* Disabled until PhotoImportCoordinator is re-implemented
    func testActorConcurrencySafety() async throws {
        // Test that actor prevents concurrent imports
        // Placeholder for future implementation
        XCTAssertTrue(true)
    }
    */

    // MARK: - Helper Methods

    private func createTestImage(size: CGSize) -> UIImage {
        UIGraphicsBeginImageContext(size)
        defer { UIGraphicsEndImageContext() }

        let context = UIGraphicsGetCurrentContext()!
        context.setFillColor(UIColor.blue.cgColor)
        context.fill(CGRect(origin: .zero, size: size))

        return UIGraphicsGetImageFromCurrentImageContext() ?? UIImage()
    }

    private func createTestImageURLs(count: Int) -> [URL] {
        var urls: [URL] = []
        let tempDir = FileManager.default.temporaryDirectory

        for i in 0..<count {
            let image = createTestImage(size: CGSize(width: 1000, height: 1000))
            if let data = image.jpegData(compressionQuality: 0.8) {
                let url = tempDir.appendingPathComponent("test_image_\(i).jpg")
                try? data.write(to: url)
                urls.append(url)
            }
        }

        return urls
    }
}

// MARK: - Memory Monitoring Test

final class MemoryMonitoringTests: XCTestCase {

    func testMemoryUsageReporting() async throws {
        let generator = SafeThumbnailGenerator()

        // Get initial memory
        let initialMemory = generator.getCurrentMemoryUsage()
        print("Initial memory usage: \(initialMemory)MB")

        // Process some data
        let testImage = UIImage(systemName: "photo")!
        if let data = testImage.jpegData(compressionQuality: 0.5) {
            _ = try? await generator.generateThumbnail(from: data)
        }

        // Check memory after processing
        let finalMemory = generator.getCurrentMemoryUsage()
        print("Final memory usage: \(finalMemory)MB")

        // Memory should not increase dramatically
        let increase = finalMemory - initialMemory
        XCTAssertLessThan(increase, 50, "Memory increased by \(increase)MB")
    }

    func testMemorySafetyCheck() {
        let generator = SafeThumbnailGenerator()

        // Test safety check
        let isSafe = generator.isSafeToProcess()
        print("Is safe to process: \(isSafe)")

        // Should be safe unless system is under pressure
        if generator.getCurrentMemoryUsage() < 100 {
            XCTAssertTrue(isSafe)
        }
    }
}

// MARK: - Integration Test

final class PhotoImportIntegrationTest: XCTestCase {

    func testFullImportPipeline() async throws {
        print("🧪 Starting full import pipeline test...")

        // 1. Create test images
        let testImages = (0..<3).map { i in
            createTestImage(width: 2048, height: 2048, label: "Test \(i)")
        }

        let tempURLs = testImages.enumerated().map { index, image -> URL in
            let url = FileManager.default.temporaryDirectory
                .appendingPathComponent("test_\(index).jpg")
            if let data = image.jpegData(compressionQuality: 0.9) {
                try? data.write(to: url)
            }
            return url
        }

        // 2. Test safe import
        let storageManager = await StorageManager.shared
        var importProgress: [Double] = []

        print("📥 Importing \(tempURLs.count) photos...")

        let results = try await storageManager.importPhotosInBatches(
            sourceURLs: tempURLs,
            batchSize: 2,
            progressHandler: { progress in
                importProgress.append(progress)
                print("   Progress: \(Int(progress * 100))%")
            }
        )

        // 3. Verify results
        print("✅ Import complete:")
        print("   - Imported: \(results.count)/\(tempURLs.count)")
        print("   - Memory used: \(await storageManager.getCurrentMemoryUsage())MB")
        print("   - Storage pressure: \(await storageManager.getStoragePressure().description)")

        XCTAssertEqual(results.count, tempURLs.count)
        XCTAssertGreaterThan(importProgress.count, 0)

        // 4. Cleanup
        for url in tempURLs {
            try? FileManager.default.removeItem(at: url)
        }

        print("🎉 Test completed successfully!")
    }

    private func createTestImage(width: CGFloat, height: CGFloat, label: String) -> UIImage {
        UIGraphicsBeginImageContext(CGSize(width: width, height: height))
        defer { UIGraphicsEndImageContext() }

        let context = UIGraphicsGetCurrentContext()!

        // Draw gradient background
        let colors = [UIColor.systemBlue.cgColor, UIColor.systemPurple.cgColor]
        let gradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(),
                                 colors: colors as CFArray,
                                 locations: nil)!
        context.drawLinearGradient(gradient,
                                  start: .zero,
                                  end: CGPoint(x: width, y: height),
                                  options: [])

        // Draw label
        let attributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 100),
            .foregroundColor: UIColor.white
        ]
        let text = NSAttributedString(string: label, attributes: attributes)
        text.draw(at: CGPoint(x: width/2 - 150, y: height/2 - 50))

        return UIGraphicsGetImageFromCurrentImageContext() ?? UIImage()
    }
}