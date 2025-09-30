import Foundation
import Combine

public protocol PhotoRepository {
    // CRUD Operations
    func insertPhoto(_ photo: Photo) async throws -> Int64
    func insertPhotos(_ photos: [Photo]) async throws
    func updatePhoto(_ photo: Photo) async throws
    func deletePhoto(_ photo: Photo) async throws
    func deletePhotoById(_ photoId: Int64) async throws

    // Retrieval Operations
    func getPhotoById(_ photoId: Int64) async throws -> Photo?
    func getPhotoByPath(_ path: String) async throws -> Photo?
    func getAllPhotos() async throws -> [Photo]
    func getAllPhotosFlow() -> AnyPublisher<[Photo], Error>

    // Category Operations
    func getPhotosByCategory(_ categoryId: Int64) async throws -> [Photo]
    func getPhotosByCategoryFlow(_ categoryId: Int64) -> AnyPublisher<[Photo], Error>
    func deletePhotosByCategory(_ categoryId: Int64) async throws
    func getPhotoCategoryCount(_ categoryId: Int64) async throws -> Int
    func getPhotoCountByCategory(categoryId: Int64) async throws -> Int

    // Utility Operations
    func getPhotoCount() async throws -> Int
    func removeFromLibrary(_ photo: Photo) async throws
    func removeFromLibraryById(_ photoId: Int64) async throws
    func cleanupOrphanedPhotos() async throws -> Int

}