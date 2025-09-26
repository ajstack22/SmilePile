import XCTest
import UIKit
@testable import SmilePile

class ImageProcessorTests: XCTestCase {

    var sut: ImageProcessor!
    var testImage: UIImage!
    var fileManager: FileManager!

    override func setUp() {
        super.setUp()
        sut = ImageProcessor()
        fileManager = FileManager.default
        testImage = createTestImage(width: 3000, height: 2000)
    }

    // MARK: - Resize Tests

    func testImageResizeMaintainsAspectRatio() async throws {
        // Given
        let imageData = testImage.jpegData(compressionQuality: 1.0)!

        // When
        let processed = try await sut.processUIImage(testImage, maxSize: 1024)
        let resultImage = UIImage(data: processed)!

        // Then
        XCTAssertLessThanOrEqual(resultImage.size.width, 1024)
        XCTAssertLessThanOrEqual(resultImage.size.height, 1024)

        // Check aspect ratio maintained (3:2)
        let aspectRatio = resultImage.size.width / resultImage.size.height
        XCTAssertEqual(aspectRatio, 1.5, accuracy: 0.01)
    }

    func testImageNotResizedIfSmallerThanMax() async throws {
        // Given
        let smallImage = createTestImage(width: 500, height: 500)

        // When
        let processed = try await sut.processUIImage(smallImage, maxSize: 2048)
        let resultImage = UIImage(data: processed)!

        // Then
        XCTAssertEqual(resultImage.size.width, 500, accuracy: 1)
        XCTAssertEqual(resultImage.size.height, 500, accuracy: 1)
    }

    // MARK: - Thumbnail Tests

    func testThumbnailGeneration() async throws {
        // Given
        let imageData = testImage.jpegData(compressionQuality: 1.0)!

        // When
        let thumbnailData = try await sut.generateThumbnail(from: imageData)
        let thumbnail = UIImage(data: thumbnailData)!

        // Then
        XCTAssertEqual(thumbnail.size.width, 300, accuracy: 1)
        XCTAssertEqual(thumbnail.size.height, 300, accuracy: 1)
    }

    func testThumbnailCenterCrop() async throws {
        // Given - rectangular image
        let rectImage = createTestImage(width: 400, height: 200)
        let imageData = rectImage.jpegData(compressionQuality: 1.0)!

        // When
        let thumbnailData = try await sut.generateThumbnail(from: imageData)
        let thumbnail = UIImage(data: thumbnailData)!

        // Then - should be square
        XCTAssertEqual(thumbnail.size.width, thumbnail.size.height, accuracy: 1)
        XCTAssertEqual(thumbnail.size.width, 300, accuracy: 1)
    }

    // MARK: - Compression Tests

    func testJPEGCompression() async throws {
        // Given
        let originalData = testImage.pngData()!
        let originalSize = originalData.count

        // When
        let compressed = try await sut.processUIImage(testImage)
        let compressedSize = compressed.count

        // Then
        XCTAssertLessThan(compressedSize, originalSize)
        XCTAssertGreaterThan(compressedSize, 0)

        // Verify it's still a valid image
        let image = UIImage(data: compressed)
        XCTAssertNotNil(image)
    }

    // MARK: - Error Handling Tests

    func testInvalidImageDataThrows() async {
        // Given
        let invalidData = Data("not an image".utf8)

        // When/Then
        do {
            _ = try await sut.generateThumbnail(from: invalidData)
            XCTFail("Should have thrown error")
        } catch {
            XCTAssertTrue(error is StorageError)
        }
    }

    // MARK: - Performance Tests

    func testResizePerformance() {
        let largeImage = createTestImage(width: 4000, height: 3000)

        measure {
            let expectation = XCTestExpectation()
            Task {
                _ = try? await sut.processUIImage(largeImage)
                expectation.fulfill()
            }
            wait(for: [expectation], timeout: 2.0)
        }
    }

    func testThumbnailGenerationPerformance() {
        let imageData = testImage.jpegData(compressionQuality: 1.0)!

        measure {
            let expectation = XCTestExpectation()
            Task {
                _ = try? await sut.generateThumbnail(from: imageData)
                expectation.fulfill()
            }
            wait(for: [expectation], timeout: 1.0)
        }
    }

    // MARK: - Memory Tests

    func testMemoryEfficientLargeImageProcessing() async throws {
        // Given
        let hugeImage = createTestImage(width: 8000, height: 6000)
        let memoryBefore = getCurrentMemoryUsage()

        // When
        _ = try await sut.processUIImage(hugeImage, maxSize: 2048)

        // Then
        let memoryAfter = getCurrentMemoryUsage()
        let increase = memoryAfter - memoryBefore

        // Should not hold entire uncompressed image in memory
        XCTAssertLessThan(increase, 50 * 1024 * 1024) // 50MB max increase
    }

    // MARK: - Helper Methods

    private func createTestImage(width: CGFloat, height: CGFloat) -> UIImage {
        let size = CGSize(width: width, height: height)
        UIGraphicsBeginImageContext(size)

        // Draw gradient for better compression testing
        let context = UIGraphicsGetCurrentContext()!
        let colors = [UIColor.red.cgColor, UIColor.blue.cgColor]
        let gradient = CGGradient(
            colorsSpace: CGColorSpaceCreateDeviceRGB(),
            colors: colors as CFArray,
            locations: nil
        )!

        context.drawLinearGradient(
            gradient,
            start: .zero,
            end: CGPoint(x: width, y: height),
            options: []
        )

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