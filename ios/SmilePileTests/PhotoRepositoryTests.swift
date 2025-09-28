import XCTest
import CoreData
import Combine
@testable import SmilePile

final class PhotoRepositoryTests: XCTestCase {
    var repository: PhotoRepositoryImpl!
    var coreDataStack: CoreDataStack!
    var cancellables: Set<AnyCancellable>!

    override func setUp() {
        super.setUp()
        coreDataStack = CoreDataStack.shared
        repository = PhotoRepositoryImpl(coreDataStack: coreDataStack)
        cancellables = Set<AnyCancellable>()
    }

    override func tearDown() {
        cancellables = nil
        repository = nil
        coreDataStack = nil
        super.tearDown()
    }

    // MARK: - CRUD Operations Tests

    func testInsertPhoto() async throws {
        // Given
        let photo = Photo(
            id: 1,
            path: "/photos/test.jpg",
            categoryId: 1,
            name: "test"
        )

        // When
        let result = try await repository.insertPhoto(photo)

        // Then
        XCTAssertEqual(result, photo.categoryId)

        // Verify photo was saved
        let savedPhoto = try await repository.getPhotoById(1)
        XCTAssertNotNil(savedPhoto)
        XCTAssertEqual(savedPhoto?.path, photo.path)
    }

    func testInsertPhotoWithInvalidCategory() async {
        // Given
        let photo = Photo(
            id: 1,
            path: "/photos/test.jpg",
            categoryId: 0, // Invalid category
            name: "test"
        )

        // When/Then
        do {
            _ = try await repository.insertPhoto(photo)
            XCTFail("Should have thrown error for invalid category")
        } catch let error as PhotoRepositoryError {
            if case .invalidCategory = error {
                // Expected error
            } else {
                XCTFail("Wrong error type: \(error)")
            }
        } catch {
            XCTFail("Unexpected error: \(error)")
        }
    }

    func testInsertMultiplePhotos() async throws {
        // Given
        let photos = [
            Photo(id: 1, path: "/photos/test1.jpg", categoryId: 1, name: "test1"),
            Photo(id: 2, path: "/photos/test2.jpg", categoryId: 1, name: "test2"),
            Photo(id: 3, path: "/photos/test3.jpg", categoryId: 2, name: "test3")
        ]

        // When
        try await repository.insertPhotos(photos)

        // Then
        let allPhotos = try await repository.getAllPhotos()
        XCTAssertEqual(allPhotos.count, 3)
    }

    func testUpdatePhoto() async throws {
        // Given
        let photo = Photo(id: 1, path: "/photos/test.jpg", categoryId: 1, name: "test")
        _ = try await repository.insertPhoto(photo)

        // When
        var updatedPhoto = photo
        updatedPhoto = Photo(
            id: photo.id,
            path: "/photos/updated.jpg",
            categoryId: 2,
            name: "updated"
        )
        try await repository.updatePhoto(updatedPhoto)

        // Then
        let savedPhoto = try await repository.getPhotoById(1)
        XCTAssertEqual(savedPhoto?.path, "/photos/updated.jpg")
        XCTAssertEqual(savedPhoto?.categoryId, 2)
        XCTAssertEqual(savedPhoto?.name, "updated")
    }

    func testDeletePhoto() async throws {
        // Given
        let photo = Photo(id: 1, path: "/photos/test.jpg", categoryId: 1, name: "test")
        _ = try await repository.insertPhoto(photo)

        // When
        try await repository.deletePhoto(photo)

        // Then
        let deletedPhoto = try await repository.getPhotoById(1)
        XCTAssertNil(deletedPhoto)
    }

    // MARK: - Retrieval Operations Tests

    func testGetPhotoByPath() async throws {
        // Given
        let photo = Photo(id: 1, path: "/photos/unique.jpg", categoryId: 1, name: "unique")
        _ = try await repository.insertPhoto(photo)

        // When
        let foundPhoto = try await repository.getPhotoByPath("/photos/unique.jpg")

        // Then
        XCTAssertNotNil(foundPhoto)
        XCTAssertEqual(foundPhoto?.id, photo.id)
    }

    func testGetAllPhotosFlow() async throws {
        // Given
        let photos = [
            Photo(id: 1, path: "/photos/test1.jpg", categoryId: 1, name: "test1"),
            Photo(id: 2, path: "/photos/test2.jpg", categoryId: 1, name: "test2")
        ]
        try await repository.insertPhotos(photos)

        // When
        let expectation = expectation(description: "Photos flow")
        var receivedPhotos: [Photo] = []

        repository.getAllPhotosFlow()
            .sink(
                receiveCompletion: { _ in },
                receiveValue: { photos in
                    receivedPhotos = photos
                    expectation.fulfill()
                }
            )
            .store(in: &cancellables)

        // Then
        await fulfillment(of: [expectation], timeout: 5)
        XCTAssertEqual(receivedPhotos.count, 2)
    }

    // MARK: - Category Operations Tests

    func testGetPhotosByCategory() async throws {
        // Given
        let photos = [
            Photo(id: 1, path: "/photos/test1.jpg", categoryId: 1, name: "test1"),
            Photo(id: 2, path: "/photos/test2.jpg", categoryId: 1, name: "test2"),
            Photo(id: 3, path: "/photos/test3.jpg", categoryId: 2, name: "test3")
        ]
        try await repository.insertPhotos(photos)

        // When
        let category1Photos = try await repository.getPhotosByCategory(1)
        let category2Photos = try await repository.getPhotosByCategory(2)

        // Then
        XCTAssertEqual(category1Photos.count, 2)
        XCTAssertEqual(category2Photos.count, 1)
    }

    func testDeletePhotosByCategory() async throws {
        // Given
        let photos = [
            Photo(id: 1, path: "/photos/test1.jpg", categoryId: 1, name: "test1"),
            Photo(id: 2, path: "/photos/test2.jpg", categoryId: 1, name: "test2"),
            Photo(id: 3, path: "/photos/test3.jpg", categoryId: 2, name: "test3")
        ]
        try await repository.insertPhotos(photos)

        // When
        try await repository.deletePhotosByCategory(1)

        // Then
        let remainingPhotos = try await repository.getAllPhotos()
        XCTAssertEqual(remainingPhotos.count, 1)
        XCTAssertEqual(remainingPhotos.first?.categoryId, 2)
    }

    // MARK: - Date Filter Tests

    func testGetPhotosByDateRange() async throws {
        // Given
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let yesterday = now - 86400000 // 24 hours in milliseconds
        let twoDaysAgo = now - (2 * 86400000)

        let photos = [
            Photo(id: 1, path: "/photos/today.jpg", categoryId: 1, name: "today", createdAt: now),
            Photo(id: 2, path: "/photos/yesterday.jpg", categoryId: 1, name: "yesterday", createdAt: yesterday),
            Photo(id: 3, path: "/photos/old.jpg", categoryId: 2, name: "old", createdAt: twoDaysAgo)
        ]
        try await repository.insertPhotos(photos)

        // When
        let allPhotos = try await repository.getAllPhotos()
        let recentPhotos = allPhotos.filter { photo in
            photo.createdAt >= yesterday
        }

        // Then
        XCTAssertEqual(recentPhotos.count, 2)
        XCTAssertTrue(recentPhotos.contains { $0.name == "today" })
        XCTAssertTrue(recentPhotos.contains { $0.name == "yesterday" })
        XCTAssertFalse(recentPhotos.contains { $0.name == "old" })
    }

    // MARK: - Utility Operations Tests

    func testGetPhotoCount() async throws {
        // Given
        let photos = [
            Photo(id: 1, path: "/photos/test1.jpg", categoryId: 1, name: "test1"),
            Photo(id: 2, path: "/photos/test2.jpg", categoryId: 1, name: "test2"),
            Photo(id: 3, path: "/photos/test3.jpg", categoryId: 2, name: "test3")
        ]
        try await repository.insertPhotos(photos)

        // When
        let count = try await repository.getPhotoCount()

        // Then
        XCTAssertEqual(count, 3)
    }

    func testGetPhotoCategoryCount() async throws {
        // Given
        let photos = [
            Photo(id: 1, path: "/photos/test1.jpg", categoryId: 1, name: "test1"),
            Photo(id: 2, path: "/photos/test2.jpg", categoryId: 1, name: "test2"),
            Photo(id: 3, path: "/photos/test3.jpg", categoryId: 2, name: "test3")
        ]
        try await repository.insertPhotos(photos)

        // When
        let category1Count = try await repository.getPhotoCategoryCount(1)
        let category2Count = try await repository.getPhotoCategoryCount(2)
        let category3Count = try await repository.getPhotoCategoryCount(3)

        // Then
        XCTAssertEqual(category1Count, 2)
        XCTAssertEqual(category2Count, 1)
        XCTAssertEqual(category3Count, 0)
    }
}