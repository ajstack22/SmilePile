import CoreData
import Foundation

extension CategoryEntity {

    var normalizedName: String {
        return (displayName ?? "").lowercased().replacingOccurrences(of: " ", with: "_")
    }

    var photoCount: Int {
        return (photos?.count) ?? 0
    }

    convenience init(category: Category, context: NSManagedObjectContext) {
        self.init(context: context)
        self.id = category.id
        self.name = category.name
        self.displayName = category.displayName
        self.colorHex = category.colorHex
        self.position = Int32(category.position)
        self.isDefault = category.isDefault
        self.createdAt = category.createdAt
    }

    func toCategory() -> Category? {
        guard let displayName = displayName else { return nil }

        let name = displayName.lowercased().replacingOccurrences(of: " ", with: "_")

        return Category(
            id: id,
            name: name,
            displayName: displayName,
            position: Int(position),
            iconResource: nil,
            colorHex: colorHex,
            isDefault: isDefault,
            createdAt: createdAt
        )
    }

    static func createDefaultCategories(in context: NSManagedObjectContext) -> [CategoryEntity] {
        let defaultCategories = Category.getDefaultCategories()

        return defaultCategories.map { category in
            let entity = CategoryEntity(context: context)
            entity.id = category.id
            entity.name = category.name
            entity.displayName = category.displayName
            entity.colorHex = category.colorHex
            entity.position = Int32(category.position)
            entity.isDefault = true
            entity.createdAt = category.createdAt
            return entity
        }
    }

    static func fetchRequest(sortBy: KeyPath<CategoryEntity, Any>? = nil) -> NSFetchRequest<CategoryEntity> {
        let request = NSFetchRequest<CategoryEntity>(entityName: "CategoryEntity")

        if let keyPath = sortBy {
            request.sortDescriptors = [NSSortDescriptor(keyPath: keyPath, ascending: true)]
        } else {
            request.sortDescriptors = [NSSortDescriptor(keyPath: \CategoryEntity.position, ascending: true)]
        }

        return request
    }

    static func fetchAll(in context: NSManagedObjectContext) throws -> [CategoryEntity] {
        let request = fetchRequest()
        return try context.fetch(request)
    }

    static func count(in context: NSManagedObjectContext) throws -> Int {
        let request = NSFetchRequest<CategoryEntity>(entityName: "CategoryEntity")
        return try context.count(for: request)
    }

    static func findByDisplayName(_ displayName: String, in context: NSManagedObjectContext) throws -> CategoryEntity? {
        let request = NSFetchRequest<CategoryEntity>(entityName: "CategoryEntity")
        request.predicate = NSPredicate(format: "displayName ==[c] %@", displayName)
        request.fetchLimit = 1
        return try context.fetch(request).first
    }

    static func getNextPosition(in context: NSManagedObjectContext) throws -> Int32 {
        let request = NSFetchRequest<CategoryEntity>(entityName: "CategoryEntity")
        request.sortDescriptors = [NSSortDescriptor(keyPath: \CategoryEntity.position, ascending: false)]
        request.fetchLimit = 1

        if let lastCategory = try context.fetch(request).first {
            return lastCategory.position + 1
        }
        return 0
    }
}

struct CategoryWithCount: Identifiable {
    let id = UUID()
    let category: Category
    let photoCount: Int

    init(category: Category, photoCount: Int) {
        self.category = category
        self.photoCount = photoCount
    }

    init(entity: CategoryEntity) {
        self.category = entity.toCategory() ?? Category(
            id: 0,
            name: "",
            displayName: "Unknown",
            position: 0
        )
        self.photoCount = entity.photoCount
    }
}