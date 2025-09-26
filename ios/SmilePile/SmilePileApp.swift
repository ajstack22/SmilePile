import SwiftUI
import CoreData

@main
struct SmilePileApp: App {
    let coreDataStack = CoreDataStack.shared

    init() {
        // Initialize font manager to register custom fonts
        _ = FontManager.shared

        // Initialize Core Data stack
        _ = coreDataStack

        // Initialize default categories
        Task {
            let repository = CategoryRepositoryImpl()
            try? await repository.initializeDefaultCategories()
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(\.managedObjectContext, coreDataStack.viewContext)
        }
    }
}