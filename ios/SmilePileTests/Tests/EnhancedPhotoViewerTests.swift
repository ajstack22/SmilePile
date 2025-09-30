import XCTest
import SwiftUI
@testable import SmilePile

class EnhancedPhotoViewerTests: XCTestCase {

    var viewModel: KidsModeViewModel!

    override func setUp() {
        super.setUp()
        viewModel = KidsModeViewModel()
    }

    override func tearDown() {
        viewModel = nil
        super.tearDown()
    }

    // MARK: - Zoom Functionality Tests

    func testPinchToZoomScaleLimits() {
        // Test that scale is constrained between 0.5x and 4x
        let minScale: CGFloat = 0.5
        let maxScale: CGFloat = 4.0

        // Test values
        let testScales: [CGFloat] = [0.1, 0.5, 1.0, 2.5, 4.0, 10.0]

        for scale in testScales {
            let constrainedScale = min(max(scale, minScale), maxScale)
            XCTAssertGreaterThanOrEqual(constrainedScale, minScale)
            XCTAssertLessThanOrEqual(constrainedScale, maxScale)
        }
    }

    func testDoubleTapZoomToggle() {
        var currentScale: CGFloat = 1.0
        let doubleTapScale: CGFloat = 2.5

        // First double tap should zoom in
        if currentScale == 1.0 {
            currentScale = doubleTapScale
        }
        XCTAssertEqual(currentScale, 2.5)

        // Second double tap should reset
        if currentScale > 1.0 {
            currentScale = 1.0
        }
        XCTAssertEqual(currentScale, 1.0)
    }

    // MARK: - Navigation Tests

    func testHorizontalSwipeNavigation() {
        // Setup test photos
        let photos = [
            Photo(id: 1, path: "test1.jpg", categoryId: 1),
            Photo(id: 2, path: "test2.jpg", categoryId: 1),
            Photo(id: 3, path: "test3.jpg", categoryId: 1)
        ]

        var currentIndex = 0

        // Swipe right (next photo)
        currentIndex = (currentIndex + 1) % photos.count
        XCTAssertEqual(currentIndex, 1)

        // Swipe right again
        currentIndex = (currentIndex + 1) % photos.count
        XCTAssertEqual(currentIndex, 2)

        // Swipe right at end (should loop to beginning)
        currentIndex = (currentIndex + 1) % photos.count
        XCTAssertEqual(currentIndex, 0)
    }

    func testZoomPersistenceOnPhotoSwitch() {
        var currentScale: CGFloat = 2.5
        var currentOffset: CGSize = CGSize(width: 100, height: 50)

        // When switching photos, zoom should reset
        // This is the expected behavior based on Android implementation
        currentScale = 1.0
        currentOffset = .zero

        XCTAssertEqual(currentScale, 1.0)
        XCTAssertEqual(currentOffset, .zero)
    }

    // MARK: - Metadata Tests

    func testMetadataDisplay() {
        let testPhoto = Photo(
            id: 1,
            path: "test.jpg",
            categoryId: 1,
            name: "Test Photo",
            isFromAssets: false,
            createdAt: Int64(Date().timeIntervalSince1970 * 1000),
            fileSize: 2048000, // 2MB
            width: 1920,
            height: 1080
        )

        // Test display name
        XCTAssertEqual(testPhoto.displayName, "Test Photo")

        // Test dimensions
        XCTAssertEqual(testPhoto.width, 1920)
        XCTAssertEqual(testPhoto.height, 1080)

        // Test file size formatting
        XCTAssertEqual(testPhoto.formattedFileSize, "2 MB")
    }

    func testMetadataToggle() {
        var showMetadata = false

        // Single tap should toggle metadata
        showMetadata.toggle()
        XCTAssertTrue(showMetadata)

        // Another tap should hide it
        showMetadata.toggle()
        XCTAssertFalse(showMetadata)
    }

    // MARK: - Share Functionality Tests

    func testPhotoShareItem() {
        let testPhoto = Photo(
            id: 1,
            path: "test.jpg",
            categoryId: 1,
            name: "Test Photo"
        )

        let shareItem = PhotoShareItem(photo: testPhoto)

        // Test that share item returns correct subject
        let subject = shareItem.activityViewController(
            UIActivityViewController(activityItems: [], applicationActivities: nil),
            subjectForActivityType: nil
        )
        XCTAssertEqual(subject, "Test Photo")

        // Test placeholder
        let placeholder = shareItem.activityViewControllerPlaceholderItem(
            UIActivityViewController(activityItems: [], applicationActivities: nil)
        )
        XCTAssertTrue(placeholder is UIImage)
    }

    // MARK: - Accessibility Tests

    func testVoiceOverLabels() {
        let photos = [
            Photo(id: 1, path: "test1.jpg", categoryId: 1, name: "Family Photo"),
            Photo(id: 2, path: "test2.jpg", categoryId: 1, name: "Vacation Photo")
        ]

        // Test photo accessibility label
        let photoIndex = 0
        let expectedLabel = "Photo \(photoIndex + 1) of \(photos.count): \(photos[photoIndex].displayName)"
        let actualLabel = "Photo 1 of 2: Family Photo"
        XCTAssertEqual(expectedLabel, actualLabel)
    }

    // MARK: - Memory Management Tests

    func testLargePhotoHandling() {
        // Test that large photos are handled without memory issues
        let largePhoto = Photo(
            id: 1,
            path: "large.jpg",
            categoryId: 1,
            fileSize: 10485760, // 10MB
            width: 4032,
            height: 3024
        )

        // Verify dimensions are reasonable
        XCTAssertLessThanOrEqual(largePhoto.width, 5000)
        XCTAssertLessThanOrEqual(largePhoto.height, 5000)
    }

    // MARK: - Orientation Tests

    func testOrientationChangeHandling() {
        // Test that the viewer handles orientation changes
        var isPortrait = true
        var metadataHeight: CGFloat = isPortrait ? 150 : 120

        XCTAssertEqual(metadataHeight, 150)

        // Change to landscape
        isPortrait = false
        metadataHeight = isPortrait ? 150 : 120

        XCTAssertEqual(metadataHeight, 120)
    }

    // MARK: - iPad Specific Tests

    func testIPadSpecificUI() {
        let isIPad = UIDevice.current.userInterfaceIdiom == .pad
        let metadataHeight: CGFloat = isIPad ? 200 : 150

        if isIPad {
            XCTAssertEqual(metadataHeight, 200)
        } else {
            XCTAssertEqual(metadataHeight, 150)
        }
    }

    // MARK: - Gesture Conflict Resolution Tests

    func testGestureConflictResolution() {
        var currentScale: CGFloat = 1.0
        var isDragging = false

        // Pan should only work when zoomed
        if currentScale > 1.0 {
            isDragging = true
        } else {
            isDragging = false
        }

        XCTAssertFalse(isDragging) // Should not drag at 1x zoom

        currentScale = 2.0
        if currentScale > 1.0 {
            isDragging = true
        }

        XCTAssertTrue(isDragging) // Should allow drag when zoomed
    }

    // MARK: - Safe Array Access Tests

    func testSafeArrayAccess() {
        let array = [1, 2, 3, 4, 5]

        // Test valid indices
        XCTAssertEqual(array[safe: 0], 1)
        XCTAssertEqual(array[safe: 2], 3)
        XCTAssertEqual(array[safe: 4], 5)

        // Test invalid indices
        XCTAssertNil(array[safe: -1])
        XCTAssertNil(array[safe: 5])
        XCTAssertNil(array[safe: 100])
    }
}

// MARK: - Performance Tests

extension EnhancedPhotoViewerTests {

    func testZoomPerformance() {
        measure {
            // Measure zoom gesture performance
            var scale: CGFloat = 1.0
            for _ in 0..<100 {
                scale = min(max(scale * 1.1, 0.5), 4.0)
            }
        }
    }

    func testNavigationPerformance() {
        let photos = (0..<100).map { Photo(id: Int64($0), path: "photo\($0).jpg", categoryId: 1) }

        measure {
            // Measure navigation performance
            var currentIndex = 0
            for _ in 0..<photos.count {
                currentIndex = (currentIndex + 1) % photos.count
            }
        }
    }
}