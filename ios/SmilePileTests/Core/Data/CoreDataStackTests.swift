import XCTest
import CoreData
@testable import SmilePile

class CoreDataStackTests: XCTestCase {

    var sut: CoreDataStack!
    var testContext: NSManagedObjectContext!

    override func setUp() {
        super.setUp()
        sut = CoreDataStack.shared
        testContext = sut.newBackgroundContext()
    }

    override func tearDown() {
        // Clean up test data
        let fetchRequest: NSFetchRequest<NSFetchRequestResult> = Photo.fetchRequest()
        let deleteRequest = NSBatchDeleteRequest(fetchRequest: fetchRequest)
        try? testContext.execute(deleteRequest)

        let categoryFetchRequest: NSFetchRequest<NSFetchRequestResult> = Category.fetchRequest()
        let categoryDeleteRequest = NSBatchDeleteRequest(fetchRequest: categoryFetchRequest)
        try? testContext.execute(categoryDeleteRequest)

        super.tearDown()
    }

    // MARK: - Stack Tests

    func testCoreDataStackInitialization() {
        XCTAssertNotNil(sut.mainContext)
        XCTAssertEqual(sut.mainContext.concurrencyType, .mainQueueConcurrencyType)
    }

    func testBackgroundContextCreation() {
        let context = sut.newBackgroundContext()
        XCTAssertNotNil(context)
        XCTAssertEqual(context.concurrencyType, .privateQueueConcurrencyType)
    }

    // MARK: - Photo CRUD Tests

    func testCreatePhoto() throws {
        // Given
        let uri = "test://photo1.jpg"
        let categoryId: Int64 = 1

        // When
        let photo = try Photo.create(
            in: testContext,
            uri: uri,
            categoryId: categoryId,
            fileSize: 1024
        )

        // Then
        XCTAssertNotNil(photo.id)
        XCTAssertEqual(photo.uri, uri)
        XCTAssertEqual(photo.categoryId, categoryId)
        XCTAssertFalse(photo.isFavorite)
    }

    func testPreventDuplicatePhotos() throws {
        // Given
        let uri = "test://duplicate.jpg"

        // When
        _ = try Photo.create(in: testContext, uri: uri, categoryId: 1)
        try testContext.save()

        // Then
        XCTAssertThrowsError(
            try Photo.create(in: testContext, uri: uri, categoryId: 1)
        ) { error in
            XCTAssertTrue(error.localizedDescription.contains("already exists"))
        }
    }

    func testFetchPhotosByCategory() throws {
        // Given
        let categoryId: Int64 = 1
        for i in 0..<5 {
            _ = try Photo.create(
                in: testContext,
                uri: "test://photo\(i).jpg",
                categoryId: categoryId
            )
        }
        _ = try Photo.create(
            in: testContext,
            uri: "test://other.jpg",
            categoryId: 2
        )
        try testContext.save()

        // When
        let request = Photo.fetchRequest(forCategory: categoryId)
        let photos = try testContext.fetch(request)

        // Then
        XCTAssertEqual(photos.count, 5)
        XCTAssertTrue(photos.allSatisfy { $0.categoryId == categoryId })
    }

    // MARK: - Category CRUD Tests

    func testCreateDefaultCategories() throws {
        // When
        try Category.createDefaults(in: testContext)

        // Then
        let request = Category.fetchRequest()
        let categories = try testContext.fetch(request)

        XCTAssertEqual(categories.count, 8)
        XCTAssertTrue(categories.contains { $0.displayName == "Family" })
        XCTAssertTrue(categories.allSatisfy { $0.isDefault })
    }

    func testCreateCustomCategory() throws {
        // Given
        let name = "Vacation"
        let color = "#00FF00"

        // When
        let category = try Category.create(
            in: testContext,
            name: name,
            colorHex: color
        )

        // Then
        XCTAssertEqual(category.displayName, name)
        XCTAssertEqual(category.colorHex, color)
        XCTAssertFalse(category.isDefault)
    }

    func testPreventDuplicateCategories() throws {
        // Given
        let name = "Duplicate"

        // When
        _ = try Category.create(in: testContext, name: name, colorHex: "#FF0000")
        try testContext.save()

        // Then
        XCTAssertThrowsError(
            try Category.create(in: testContext, name: name, colorHex: "#00FF00")
        ) { error in
            XCTAssertTrue(error.localizedDescription.contains("already exists"))
        }
    }

    // MARK: - Performance Tests

    func testBatchInsertPerformance() throws {
        // Given
        var photoData: [[String: Any]] = []
        for i in 0..<1000 {
            photoData.append([
                "id": UUID(),
                "uri": "test://photo\(i).jpg",
                "categoryId": Int64(i % 5),
                "timestamp": Date(),
                "isFavorite": false
            ])
        }

        // When & Then
        measure {
            let expectation = XCTestExpectation(description: "Batch insert")
            sut.batchInsert(Photo.self, data: photoData) { result in
                XCTAssertNoThrow(try result.get())
                expectation.fulfill()
            }
            wait(for: [expectation], timeout: 5.0)
        }
    }

    func testFetchPerformance() throws {
        // Setup: Create 1000 photos
        for i in 0..<1000 {
            _ = try Photo.create(
                in: testContext,
                uri: "test://perf\(i).jpg",
                categoryId: Int64(i % 5)
            )
        }
        try testContext.save()

        // When & Then
        measure {
            let request = Photo.fetchRequest(offset: 0, limit: 20)
            let photos = try? testContext.fetch(request)
            XCTAssertEqual(photos?.count, 20)
        }
    }

    // MARK: - Thread Safety Tests

    func testConcurrentSaves() {
        let expectation = XCTestExpectation(description: "Concurrent saves")
        expectation.expectedFulfillmentCount = 10

        let group = DispatchGroup()

        for i in 0..<10 {
            group.enter()
            DispatchQueue.global().async {
                let context = self.sut.newBackgroundContext()
                do {
                    _ = try Photo.create(
                        in: context,
                        uri: "test://concurrent\(i).jpg",
                        categoryId: 1
                    )
                    try context.save()
                    expectation.fulfill()
                } catch {
                    XCTFail("Concurrent save failed: \(error)")
                }
                group.leave()
            }
        }

        wait(for: [expectation], timeout: 10.0)
    }

    // MARK: - Validation Tests

    func testPhotoValidation() {
        // Test empty URI
        let photo = Photo(context: testContext)
        photo.id = UUID()
        photo.uri = ""
        photo.categoryId = 1
        photo.timestamp = Date()

        XCTAssertThrowsError(try testContext.save()) { error in
            XCTAssertTrue(error.localizedDescription.contains("URI cannot be empty"))
        }
    }

    func testCategoryValidation() {
        // Test invalid color
        let category = Category(context: testContext)
        category.displayName = "Test"
        category.colorHex = "invalid"
        category.position = 0
        category.createdAt = Date()

        XCTAssertThrowsError(try testContext.save()) { error in
            XCTAssertTrue(error.localizedDescription.contains("Invalid color"))
        }
    }
}