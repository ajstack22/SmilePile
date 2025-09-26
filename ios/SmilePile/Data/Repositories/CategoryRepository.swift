import Foundation
import Combine

protocol CategoryRepository {
    // CRUD Operations
    func insertCategory(_ category: Category) async throws -> Int64
    func insertCategories(_ categories: [Category]) async throws
    func updateCategory(_ category: Category) async throws
    func deleteCategory(_ category: Category) async throws

    // Retrieval Operations
    func getCategoryById(_ categoryId: Int64) async throws -> Category?
    func getAllCategories() async throws -> [Category]
    func getAllCategoriesFlow() -> AnyPublisher<[Category], Error>

    // Name Operations
    func getCategoryByName(_ name: String) async throws -> Category?

    // Initialization
    func initializeDefaultCategories() async throws

    // Utility Operations
    func getCategoryCount() async throws -> Int
}