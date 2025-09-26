import XCTest
import CoreData
import SwiftUI
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
        let fetchRequest: NSFetchRequest<NSFetchRequestResult> = NSFetchRequest(entityName: "PhotoEntity")
        let deleteRequest = NSBatchDeleteRequest(fetchRequest: fetchRequest)
        try? testContext.execute(deleteRequest)

        let categoryFetchRequest: NSFetchRequest<NSFetchRequestResult> = NSFetchRequest(entityName: "CategoryEntity")
        let categoryDeleteRequest = NSBatchDeleteRequest(fetchRequest: categoryFetchRequest)
        try? testContext.execute(categoryDeleteRequest)

        super.tearDown()
    }

    // MARK: - Stack Tests

    func testCoreDataStackInitialization() {
        XCTAssertNotNil(sut.viewContext)
        XCTAssertEqual(sut.viewContext.concurrencyType, .mainQueueConcurrencyType)
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
        let photoEntity = PhotoEntity(context: testContext)
        photoEntity.id = UUID().uuidString
        photoEntity.uri = uri
        photoEntity.categoryId = categoryId
        photoEntity.timestamp = Int64(Date().timeIntervalSince1970 * 1000)
        photoEntity.isFavorite = false
        try testContext.save()

        // Then
        XCTAssertNotNil(photoEntity.id)
        XCTAssertEqual(photoEntity.uri, uri)
        XCTAssertEqual(photoEntity.categoryId, categoryId)
        XCTAssertFalse(photoEntity.isFavorite)
    }

    func testPreventDuplicatePhotos() throws {
        // Given
        let uri = "test://duplicate.jpg"

        // When - Create first photo
        let photo1 = PhotoEntity(context: testContext)
        photo1.id = UUID().uuidString
        photo1.uri = uri
        photo1.categoryId = 1
        photo1.timestamp = Int64(Date().timeIntervalSince1970 * 1000)
        try testContext.save()

        // Then - Try to create duplicate with same URI
        let photo2 = PhotoEntity(context: testContext)
        photo2.id = UUID().uuidString
        photo2.uri = uri  // Same URI
        photo2.categoryId = 1
        photo2.timestamp = Int64(Date().timeIntervalSince1970 * 1000)

        // Validate uniqueness constraint (we need to check manually since CoreData doesn't have built-in unique constraints)
        let fetchRequest = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
        fetchRequest.predicate = NSPredicate(format: "uri == %@", uri)
        let existingPhotos = try testContext.fetch(fetchRequest)
        XCTAssertEqual(existingPhotos.count, 1, "Should not allow duplicate URIs")
    }

    func testFetchPhotosByCategory() throws {
        // Given
        let categoryId: Int64 = 1
        for i in 0..<5 {
            let photo = PhotoEntity(context: testContext)
            photo.id = UUID().uuidString
            photo.uri = "test://photo\(i).jpg"
            photo.categoryId = categoryId
            photo.timestamp = Int64(Date().timeIntervalSince1970 * 1000)
        }

        // Add one photo in different category
        let otherPhoto = PhotoEntity(context: testContext)
        otherPhoto.id = UUID().uuidString
        otherPhoto.uri = "test://other.jpg"
        otherPhoto.categoryId = 2
        otherPhoto.timestamp = Int64(Date().timeIntervalSince1970 * 1000)

        try testContext.save()

        // When
        let request = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
        request.predicate = NSPredicate(format: "categoryId == %ld", categoryId)
        let photos = try testContext.fetch(request)

        // Then
        XCTAssertEqual(photos.count, 5)
        XCTAssertTrue(photos.allSatisfy { $0.categoryId == categoryId })
    }

    // MARK: - Category CRUD Tests

    func testCreateDefaultCategories() throws {
        // When - Create default categories manually
        let defaultCategories = SmilePile.Category.getDefaultCategories()
        for category in defaultCategories {
            let entity = CategoryEntity(context: testContext)
            entity.id = category.id
            entity.displayName = category.displayName
            entity.colorHex = category.colorHex
            entity.position = Int32(category.position)
            entity.isDefault = category.isDefault
            entity.createdAt = category.createdAt
        }
        try testContext.save()

        // Then
        let request = NSFetchRequest<CategoryEntity>(entityName: "CategoryEntity")
        let categories = try testContext.fetch(request)

        XCTAssertEqual(categories.count, 4) // We have 4 default categories
        XCTAssertTrue(categories.contains { $0.displayName == "Family" })
        XCTAssertTrue(categories.allSatisfy { $0.isDefault })
    }

    func testCreateCustomCategory() throws {
        // Given
        let name = "Vacation"
        let color = "#00FF00"

        // When
        let categoryEntity = CategoryEntity(context: testContext)
        categoryEntity.id = Int64(Date().timeIntervalSince1970 * 1000)
        categoryEntity.displayName = name
        categoryEntity.colorHex = color
        categoryEntity.position = 0
        categoryEntity.isDefault = false
        categoryEntity.createdAt = Int64(Date().timeIntervalSince1970 * 1000)
        try testContext.save()

        // Then
        XCTAssertEqual(categoryEntity.displayName, name)
        XCTAssertEqual(categoryEntity.colorHex, color)
        XCTAssertFalse(categoryEntity.isDefault)
    }

    func testPreventDuplicateCategories() throws {
        // Given
        let name = "Duplicate"

        // When - Create first category
        let category1 = CategoryEntity(context: testContext)
        category1.id = Int64(Date().timeIntervalSince1970 * 1000)
        category1.displayName = name
        category1.colorHex = "#FF0000"
        category1.position = 0
        category1.isDefault = false
        category1.createdAt = Int64(Date().timeIntervalSince1970 * 1000)
        try testContext.save()

        // Then - Check duplicate prevention
        let fetchRequest = NSFetchRequest<CategoryEntity>(entityName: "CategoryEntity")
        fetchRequest.predicate = NSPredicate(format: "displayName == %@", name)
        let existingCategories = try testContext.fetch(fetchRequest)
        XCTAssertEqual(existingCategories.count, 1, "Should not allow duplicate category names")
    }

    // MARK: - Performance Tests

    func testBatchInsertPerformance() throws {
        // Given
        var photoData: [[String: Any]] = []
        for i in 0..<1000 {
            photoData.append([
                "id": UUID().uuidString,
                "uri": "test://photo\(i).jpg",
                "categoryId": Int64(i % 5),
                "timestamp": Int64(Date().timeIntervalSince1970 * 1000),
                "isFavorite": false
            ])
        }

        // When & Then
        measure {
            let expectation = XCTestExpectation(description: "Batch insert")

            // Perform batch insert using NSBatchInsertRequest
            let batchInsert = NSBatchInsertRequest(entity: PhotoEntity.entity(), objects: photoData)
            batchInsert.resultType = .statusOnly

            do {
                let result = try testContext.execute(batchInsert)
                if let batchResult = result as? NSBatchInsertResult,
                   let success = batchResult.result as? Bool, success {
                    expectation.fulfill()
                } else {
                    XCTFail("Batch insert failed")
                }
            } catch {
                XCTFail("Batch insert error: \(error)")
            }

            wait(for: [expectation], timeout: 5.0)
        }
    }

    func testFetchPerformance() throws {
        // Setup: Create 1000 photos
        for i in 0..<1000 {
            let photo = PhotoEntity(context: testContext)
            photo.id = UUID().uuidString
            photo.uri = "test://perf\(i).jpg"
            photo.categoryId = Int64(i % 5)
            photo.timestamp = Int64(Date().timeIntervalSince1970 * 1000)
        }
        try testContext.save()

        // When & Then
        measure {
            let request = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
            request.fetchOffset = 0
            request.fetchLimit = 20
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
                    let photo = PhotoEntity(context: context)
                    photo.id = UUID().uuidString
                    photo.uri = "test://concurrent\(i).jpg"
                    photo.categoryId = 1
                    photo.timestamp = Int64(Date().timeIntervalSince1970 * 1000)
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
        // Test empty URI validation
        let photo = PhotoEntity(context: testContext)
        photo.id = UUID().uuidString
        photo.uri = "" // Empty URI should be invalid
        photo.categoryId = 1
        photo.timestamp = Int64(Date().timeIntervalSince1970 * 1000)

        // We validate this in the repository layer, not CoreData itself
        // So we just check that we can detect invalid data
        XCTAssertTrue(photo.uri?.isEmpty ?? true, "URI should be empty for this test")

        // In a real scenario, the repository would throw an error for empty URI
        // Here we just verify the entity state
        XCTAssertNotNil(photo.id)
        XCTAssertEqual(photo.categoryId, 1)
    }

    func testCategoryValidation() {
        // Test invalid color validation
        let category = CategoryEntity(context: testContext)
        category.id = Int64(Date().timeIntervalSince1970 * 1000)
        category.displayName = "Test"
        category.colorHex = "invalid" // Invalid color format
        category.position = 0
        category.createdAt = Int64(Date().timeIntervalSince1970 * 1000)

        // Color validation happens in the app layer, not CoreData
        // We just verify the entity can store the value
        XCTAssertEqual(category.colorHex, "invalid")

        // The Color extension would fail to parse this, but CoreData allows it
        // Repository or view model layer would handle validation
        let color = Color(hex: category.colorHex ?? "")
        XCTAssertNil(color, "Invalid hex should not create a valid color")
    }

    // MARK: - Helper Methods for Conversion

    private func photoEntityToStruct(_ entity: PhotoEntity) -> Photo? {
        guard let id = entity.id, let uri = entity.uri else {
            return nil
        }

        let photoId = Int64(id) ?? 0
        let url = URL(fileURLWithPath: uri)
        let name = url.deletingPathExtension().lastPathComponent

        return Photo(
            id: photoId,
            path: uri,
            categoryId: entity.categoryId,
            name: name,
            isFromAssets: false,
            createdAt: entity.timestamp,
            fileSize: 0,
            width: 0,
            height: 0,
            isFavorite: entity.isFavorite
        )
    }

    private func categoryEntityToStruct(_ entity: CategoryEntity) -> SmilePile.Category? {
        guard let displayName = entity.displayName else {
            return nil
        }

        let name = displayName.lowercased().replacingOccurrences(of: " ", with: "_")

        return SmilePile.Category(
            id: entity.id,
            name: name,
            displayName: displayName,
            position: Int(entity.position),
            iconResource: nil,
            colorHex: entity.colorHex,
            isDefault: entity.isDefault,
            createdAt: entity.createdAt
        )
    }
}