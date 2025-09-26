import SwiftUI
import CoreData
import UIKit

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

        // Configure appearance for true edge-to-edge
        configureAppearance()
    }

    private func configureAppearance() {
        // Ensure navigation bars are transparent
        let navBarAppearance = UINavigationBarAppearance()
        navBarAppearance.configureWithTransparentBackground()
        navBarAppearance.backgroundColor = .clear
        navBarAppearance.shadowColor = .clear
        UINavigationBar.appearance().standardAppearance = navBarAppearance
        UINavigationBar.appearance().compactAppearance = navBarAppearance
        UINavigationBar.appearance().scrollEdgeAppearance = navBarAppearance
        UINavigationBar.appearance().isTranslucent = true
        UINavigationBar.appearance().prefersLargeTitles = false

        // Configure tab bar for edge-to-edge
        if #available(iOS 15.0, *) {
            let tabBarAppearance = UITabBarAppearance()
            tabBarAppearance.configureWithDefaultBackground()
            tabBarAppearance.backgroundColor = UIColor.systemBackground.withAlphaComponent(0.94)
            UITabBar.appearance().standardAppearance = tabBarAppearance
            UITabBar.appearance().scrollEdgeAppearance = tabBarAppearance
        }
        UITabBar.appearance().isTranslucent = true
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(\.managedObjectContext, coreDataStack.viewContext)
                .ignoresSafeArea()
                .persistentSystemOverlays(.hidden) // Hide home indicator for true fullscreen
                .statusBar(hidden: false) // Keep status bar visible but allow content underneath
        }
    }
}