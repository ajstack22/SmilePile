import XCTest
import CoreData
import Combine
@testable import SmilePile

final class CategoryRepositoryTests: XCTestCase {
    var repository: CategoryRepositoryImpl!
    var coreDataStack: CoreDataStack!
    var cancellables: Set<AnyCancellable>!

    override func setUp() {
        super.setUp()
        coreDataStack = CoreDataStack.shared
        repository = CategoryRepositoryImpl(coreDataStack: coreDataStack)
        cancellables = Set<AnyCancellable>()
    }

    override func tearDown() {
        cancellables = nil
        repository = nil
        coreDataStack = nil
        super.tearDown()
    }

    // MARK: - Initialization Tests

    func testInitializeDefaultCategories() async throws {
        // When
        try await repository.initializeDefaultCategories()

        // Then
        let categories = try await repository.getAllCategories()
        XCTAssertEqual(categories.count, 4)

        // Verify default categories
        let familyCategory = categories.first { $0.name == "family" }
        XCTAssertNotNil(familyCategory)
        XCTAssertEqual(familyCategory?.displayName, "Family")
        XCTAssertEqual(familyCategory?.colorHex, "#E91E63")
        XCTAssertTrue(familyCategory?.isDefault ?? false)

        let carsCategory = categories.first { $0.name == "cars" }
        XCTAssertNotNil(carsCategory)
        XCTAssertEqual(carsCategory?.displayName, "Cars")
        XCTAssertEqual(carsCategory?.colorHex, "#F44336")

        let gamesCategory = categories.first { $0.name == "games" }
        XCTAssertNotNil(gamesCategory)
        XCTAssertEqual(gamesCategory?.displayName, "Games")
        XCTAssertEqual(gamesCategory?.colorHex, "#9C27B0")

        let sportsCategory = categories.first { $0.name == "sports" }
        XCTAssertNotNil(sportsCategory)
        XCTAssertEqual(sportsCategory?.displayName, "Sports")
        XCTAssertEqual(sportsCategory?.colorHex, "#4CAF50")
    }

    func testInitializeDefaultCategoriesIdempotent() async throws {
        // Given
        try await repository.initializeDefaultCategories()
        let firstCount = try await repository.getCategoryCount()

        // When - Initialize again
        try await repository.initializeDefaultCategories()
        let secondCount = try await repository.getCategoryCount()

        // Then - Count should remain the same
        XCTAssertEqual(firstCount, secondCount)
        XCTAssertEqual(secondCount, 4)
    }

    // MARK: - CRUD Operations Tests

    func testInsertCategory() async throws {
        // Given
        let category = SmilePile.Category(
            id: 5,
            name: "test_category",
            displayName: "Test Category",
            position: 4,
            colorHex: "#FF5722"
        )

        // When
        let categoryId = try await repository.insertCategory(category)

        // Then
        XCTAssertEqual(categoryId, category.id)

        let savedCategory = try await repository.getCategoryById(5)
        XCTAssertNotNil(savedCategory)
        XCTAssertEqual(savedCategory?.displayName, "Test Category")
        XCTAssertEqual(savedCategory?.colorHex, "#FF5722")
    }

    func testInsertCategoryWithDuplicateName() async throws {
        // Given
        let category1 = SmilePile.Category(
            id: 5,
            name: "duplicate",
            displayName: "Duplicate",
            position: 4
        )
        _ = try await repository.insertCategory(category1)

        let category2 = SmilePile.Category(
            id: 6,
            name: "duplicate",
            displayName: "Duplicate", // Same display name
            position: 5
        )

        // When/Then
        do {
            _ = try await repository.insertCategory(category2)
            XCTFail("Should have thrown error for duplicate name")
        } catch let error as CategoryRepositoryError {
            if case .duplicateName = error {
                // Expected error
            } else {
                XCTFail("Wrong error type: \(error)")
            }
        } catch {
            XCTFail("Unexpected error: \(error)")
        }
    }

    func testInsertMultipleCategories() async throws {
        // Given
        let categories = [
            SmilePile.Category(id: 5, name: "pets", displayName: "Pets", position: 4),
            SmilePile.Category(id: 6, name: "food", displayName: "Food", position: 5),
            SmilePile.Category(id: 7, name: "travel", displayName: "Travel", position: 6)
        ]

        // When
        try await repository.insertCategories(categories)

        // Then
        let allCategories = try await repository.getAllCategories()
        XCTAssertEqual(allCategories.count, 3)
    }

    func testUpdateCategory() async throws {
        // Given
        let category = SmilePile.Category(
            id: 5,
            name: "test",
            displayName: "Original",
            position: 4,
            colorHex: "#FF5722"
        )
        _ = try await repository.insertCategory(category)

        // When
        let updatedCategory = SmilePile.Category(
            id: 5,
            name: "test_updated",
            displayName: "Updated",
            position: 5,
            colorHex: "#2196F3"
        )
        try await repository.updateCategory(updatedCategory)

        // Then
        let savedCategory = try await repository.getCategoryById(5)
        XCTAssertEqual(savedCategory?.displayName, "Updated")
        XCTAssertEqual(savedCategory?.colorHex, "#2196F3")
        XCTAssertEqual(savedCategory?.position, 5)
    }

    func testDeleteCategory() async throws {
        // Given
        let category = SmilePile.Category(
            id: 5,
            name: "test",
            displayName: "To Delete",
            position: 4,
            isDefault: false
        )
        _ = try await repository.insertCategory(category)

        // When
        try await repository.deleteCategory(category)

        // Then
        let deletedCategory = try await repository.getCategoryById(5)
        XCTAssertNil(deletedCategory)
    }

    func testDeleteDefaultCategory() async throws {
        // Given - Initialize default categories
        try await repository.initializeDefaultCategories()
        let defaultCategory = try await repository.getCategoryById(1) // Family category

        // When/Then
        do {
            try await repository.deleteCategory(defaultCategory!)
            XCTFail("Should not be able to delete default category")
        } catch let error as CategoryRepositoryError {
            if case .defaultCategoryModification = error {
                // Expected error
            } else {
                XCTFail("Wrong error type: \(error)")
            }
        } catch {
            XCTFail("Unexpected error: \(error)")
        }
    }

    // MARK: - Retrieval Operations Tests

    func testGetCategoryByName() async throws {
        // Given
        let category = SmilePile.Category(
            id: 5,
            name: "unique",
            displayName: "Unique Category",
            position: 4
        )
        _ = try await repository.insertCategory(category)

        // When
        let foundCategory = try await repository.getCategoryByName("Unique Category")

        // Then
        XCTAssertNotNil(foundCategory)
        XCTAssertEqual(foundCategory?.id, 5)
        XCTAssertEqual(foundCategory?.name, "unique")
    }

    func testGetAllCategoriesFlow() async throws {
        // Given
        let categories = [
            SmilePile.Category(id: 5, name: "pets", displayName: "Pets", position: 4),
            SmilePile.Category(id: 6, name: "food", displayName: "Food", position: 5)
        ]
        try await repository.insertCategories(categories)

        // When
        let expectation = expectation(description: "Categories flow")
        var receivedCategories: [SmilePile.Category] = []

        repository.getAllCategoriesFlow()
            .sink(
                receiveCompletion: { _ in },
                receiveValue: { categories in
                    receivedCategories = categories
                    expectation.fulfill()
                }
            )
            .store(in: &cancellables)

        // Then
        await fulfillment(of: [expectation], timeout: 5)
        XCTAssertEqual(receivedCategories.count, 2)
    }

    // MARK: - Utility Operations Tests

    func testGetCategoryCount() async throws {
        // Given
        let categories = [
            SmilePile.Category(id: 5, name: "pets", displayName: "Pets", position: 4),
            SmilePile.Category(id: 6, name: "food", displayName: "Food", position: 5),
            SmilePile.Category(id: 7, name: "travel", displayName: "Travel", position: 6)
        ]
        try await repository.insertCategories(categories)

        // When
        let count = try await repository.getCategoryCount()

        // Then
        XCTAssertEqual(count, 3)
    }

    // MARK: - Validation Tests

    func testInsertCategoryWithEmptyName() async {
        // Given
        let category = SmilePile.Category(
            id: 5,
            name: "",
            displayName: "", // Empty display name
            position: 4
        )

        // When/Then
        do {
            _ = try await repository.insertCategory(category)
            XCTFail("Should have thrown error for empty name")
        } catch let error as CategoryRepositoryError {
            if case .invalidInput = error {
                // Expected error
            } else {
                XCTFail("Wrong error type: \(error)")
            }
        } catch {
            XCTFail("Unexpected error: \(error)")
        }
    }

    func testUpdateNonExistentCategory() async {
        // Given
        let category = SmilePile.Category(
            id: 999,
            name: "nonexistent",
            displayName: "Non Existent",
            position: 0
        )

        // When/Then
        do {
            try await repository.updateCategory(category)
            XCTFail("Should have thrown error for non-existent category")
        } catch let error as CategoryRepositoryError {
            if case .notFound = error {
                // Expected error
            } else {
                XCTFail("Wrong error type: \(error)")
            }
        } catch {
            XCTFail("Unexpected error: \(error)")
        }
    }

    // MARK: - Async Flow Tests

    func testConcurrentCategoryOperations() async throws {
        // Test concurrent read/write operations
        let categories = (0..<10).map { i in
            SmilePile.Category(
                id: Int64(100 + i),
                name: "concurrent_\(i)",
                displayName: "Concurrent \(i)",
                position: i,
                colorHex: "#\(String(format: "%06X", i * 100000))"
            )
        }

        // Concurrent inserts
        await withTaskGroup(of: Void.self) { group in
            for category in categories {
                group.addTask {
                    _ = try? await self.repository.insertCategory(category)
                }
            }
        }

        // Verify all were inserted
        let allCategories = try await repository.getAllCategories()
        let concurrentCategories = allCategories.filter { $0.name.starts(with: "concurrent_") }
        XCTAssertGreaterThanOrEqual(concurrentCategories.count, 5) // At least some should succeed
    }

    func testGetAllCategoriesFlowUpdates() async throws {
        // Given
        let expectation1 = expectation(description: "Initial categories")
        let expectation2 = expectation(description: "Updated categories")
        var receivedUpdates = 0

        // Subscribe to flow
        repository.getAllCategoriesFlow()
            .sink(
                receiveCompletion: { _ in },
                receiveValue: { categories in
                    receivedUpdates += 1
                    if receivedUpdates == 1 {
                        expectation1.fulfill()
                    } else if receivedUpdates == 2 {
                        expectation2.fulfill()
                    }
                }
            )
            .store(in: &cancellables)

        // Initial state
        await fulfillment(of: [expectation1], timeout: 2)

        // Insert a new category
        let newCategory = SmilePile.Category(
            id: 200,
            name: "flow_test",
            displayName: "Flow Test",
            position: 10
        )
        _ = try await repository.insertCategory(newCategory)

        // Should receive update
        await fulfillment(of: [expectation2], timeout: 2)

        XCTAssertGreaterThanOrEqual(receivedUpdates, 2)
    }

    func testCategoryBatchOperations() async throws {
        // Test batch insert performance
        let batchSize = 50
        let categories = (0..<batchSize).map { i in
            SmilePile.Category(
                id: Int64(1000 + i),
                name: "batch_\(i)",
                displayName: "Batch \(i)",
                position: i,
                colorHex: "#4CAF50"
            )
        }

        let startTime = Date()

        // Insert in batch
        try await repository.insertCategories(categories)

        let endTime = Date()
        let duration = endTime.timeIntervalSince(startTime)

        // Verify all inserted
        let count = try await repository.getCategoryCount()
        XCTAssertGreaterThanOrEqual(count, batchSize)

        // Should complete reasonably fast (< 2 seconds for 50 items)
        XCTAssertLessThan(duration, 2.0)
    }

    func testCategoryPositionOrdering() async throws {
        // Given - Categories with specific positions
        let categories = [
            SmilePile.Category(id: 301, name: "pos_3", displayName: "Position 3", position: 3),
            SmilePile.Category(id: 302, name: "pos_1", displayName: "Position 1", position: 1),
            SmilePile.Category(id: 303, name: "pos_2", displayName: "Position 2", position: 2),
            SmilePile.Category(id: 304, name: "pos_0", displayName: "Position 0", position: 0)
        ]

        try await repository.insertCategories(categories)

        // When
        let sortedCategories = try await repository.getAllCategories()
        let positionCategories = sortedCategories.filter { $0.name.starts(with: "pos_") }

        // Then - Should be ordered by position
        for i in 0..<positionCategories.count - 1 {
            XCTAssertLessThanOrEqual(
                positionCategories[i].position,
                positionCategories[i + 1].position
            )
        }
    }

    func testCategoryCaseInsensitiveSearch() async throws {
        // Given
        let category = SmilePile.Category(
            id: 400,
            name: "case_test",
            displayName: "CaSe TeSt",
            position: 0
        )
        _ = try await repository.insertCategory(category)

        // When - Search with different cases
        let upperCase = try await repository.getCategoryByName("CASE TEST")
        let lowerCase = try await repository.getCategoryByName("case test")
        let mixedCase = try await repository.getCategoryByName("Case Test")

        // Then - All should find the same category
        XCTAssertNotNil(upperCase)
        XCTAssertNotNil(lowerCase)
        XCTAssertNotNil(mixedCase)
        XCTAssertEqual(upperCase?.id, 400)
        XCTAssertEqual(lowerCase?.id, 400)
        XCTAssertEqual(mixedCase?.id, 400)
    }

    func testCategoryColorHexValidation() async throws {
        // Given - Categories with various color formats
        let categories = [
            SmilePile.Category(id: 501, name: "color1", displayName: "Color 1", position: 0, colorHex: "#FF0000"),
            SmilePile.Category(id: 502, name: "color2", displayName: "Color 2", position: 1, colorHex: "#00FF00"),
            SmilePile.Category(id: 503, name: "color3", displayName: "Color 3", position: 2, colorHex: nil)
        ]

        // When
        try await repository.insertCategories(categories)

        // Then
        let color1 = try await repository.getCategoryById(501)
        XCTAssertEqual(color1?.colorHex, "#FF0000")

        let color2 = try await repository.getCategoryById(502)
        XCTAssertEqual(color2?.colorHex, "#00FF00")

        let color3 = try await repository.getCategoryById(503)
        XCTAssertEqual(color3?.colorHex, "#4CAF50") // Default color
    }

    func testUpdateCategoryColorRetention() async throws {
        // Given
        let category = SmilePile.Category(
            id: 600,
            name: "color_update",
            displayName: "Color Update",
            position: 0,
            colorHex: "#123456"
        )
        _ = try await repository.insertCategory(category)

        // When - Update without specifying color
        let updateWithoutColor = SmilePile.Category(
            id: 600,
            name: "color_update",
            displayName: "Color Updated",
            position: 1,
            colorHex: nil
        )
        try await repository.updateCategory(updateWithoutColor)

        // Then - Should retain original color
        let updated = try await repository.getCategoryById(600)
        XCTAssertEqual(updated?.colorHex, "#123456")
    }

    func testDefaultCategoryProtection() async throws {
        // Given - Initialize default categories
        try await repository.initializeDefaultCategories()
        let defaultCategory = try await repository.getCategoryById(1) // Family is default

        // Test 1: Cannot change display name of default category
        let modifiedDefault = SmilePile.Category(
            id: 1,
            name: defaultCategory!.name,
            displayName: "Modified Family",
            position: defaultCategory!.position,
            isDefault: true
        )

        do {
            try await repository.updateCategory(modifiedDefault)
            XCTFail("Should not allow modifying default category name")
        } catch let error as CategoryRepositoryError {
            if case .defaultCategoryModification = error {
                // Expected
            } else {
                XCTFail("Wrong error type: \(error)")
            }
        }

        // Test 2: Can update color of default category
        let colorUpdate = SmilePile.Category(
            id: 1,
            name: defaultCategory!.name,
            displayName: defaultCategory!.displayName,
            position: defaultCategory!.position,
            colorHex: "#00FF00",
            isDefault: true
        )
        try await repository.updateCategory(colorUpdate)

        let updatedDefault = try await repository.getCategoryById(1)
        XCTAssertEqual(updatedDefault?.colorHex, "#00FF00")
    }

    func testTransactionRollback() async throws {
        // Test that failed operations don't corrupt data
        let validCategory = SmilePile.Category(
            id: 700,
            name: "valid",
            displayName: "Valid Category",
            position: 0
        )

        _ = try await repository.insertCategory(validCategory)

        // Try to insert duplicate (should fail)
        do {
            _ = try await repository.insertCategory(validCategory)
            XCTFail("Should have failed on duplicate")
        } catch {
            // Expected
        }

        // Verify original is still intact
        let categories = try await repository.getAllCategories()
        let validCategories = categories.filter { $0.id == 700 }
        XCTAssertEqual(validCategories.count, 1)
    }

    // MARK: - Performance Tests

    func testBulkInsertPerformance() {
        measure {
            let expectation = expectation(description: "Bulk insert")

            Task {
                let categories = (0..<100).map { i in
                    SmilePile.Category(
                        id: Int64(10000 + i),
                        name: "perf_\(i)",
                        displayName: "Performance \(i)",
                        position: i
                    )
                }

                try? await repository.insertCategories(categories)
                expectation.fulfill()
            }

            wait(for: [expectation], timeout: 5.0)
        }
    }

    func testConcurrentReadPerformance() {
        measure {
            let expectation = expectation(description: "Concurrent reads")
            var completedReads = 0

            Task {
                await withTaskGroup(of: Void.self) { group in
                    for _ in 0..<20 {
                        group.addTask {
                            _ = try? await self.repository.getAllCategories()
                        }
                    }
                }
                expectation.fulfill()
            }

            wait(for: [expectation], timeout: 5.0)
        }
    }
}