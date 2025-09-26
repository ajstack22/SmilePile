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
}