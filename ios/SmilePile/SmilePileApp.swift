import SwiftUI
import CoreData
import UIKit

@main
struct SmilePileApp: App {
    let coreDataStack = CoreDataStack.shared
    @StateObject private var settingsManager = SettingsManager.shared

    init() {
        // Initialize font manager to register custom fonts
        _ = FontManager.shared

        // Initialize Core Data stack
        _ = coreDataStack

        // Initialize settings manager and perform migration if needed
        SettingsManager.shared.migrateIfNeeded()

        // Initialize default categories with proper error handling
        Task { @MainActor in
            // Run photo ID migration if needed
            await PhotoIDMigration.runMigrationIfNeeded()

            let repository = CategoryRepositoryImpl.shared
            do {
                print("SmilePileApp: Starting category initialization...")
                try await repository.initializeDefaultCategories()
                let categories = try await repository.getAllCategories()
                print("SmilePileApp: Default categories initialized successfully. Count: \(categories.count)")
                for cat in categories {
                    print("SmilePileApp: Category - \(cat.displayName) (id: \(cat.id))")
                }
            } catch {
                print("SmilePileApp: Failed to initialize default categories: \(error)")
            }
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
                .environmentObject(settingsManager)
                .ignoresSafeArea()
                // Home indicator is now controlled in ContentView based on Kids Mode
                .statusBar(hidden: false) // Keep status bar visible but allow content underneath
                .preferredColorScheme(settingsManager.themeMode == .dark ? .dark :
                                    settingsManager.themeMode == .light ? .light : nil)
        }
    }
}