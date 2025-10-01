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

        // Initialize default categories ONLY if onboarding is already completed
        // For first-time users, onboarding will create categories
        Task { @MainActor in
            // Run photo ID migration if needed
            await PhotoIDMigration.runMigrationIfNeeded()

            // Only initialize default categories if user has completed onboarding
            if SettingsManager.shared.onboardingCompleted {
                let repository = CategoryRepositoryImpl.shared
                do {
                    print("SmilePileApp: Onboarding completed, initializing default categories if needed...")
                    try await repository.initializeDefaultCategories()
                    let categories = try await repository.getAllCategories()
                    print("SmilePileApp: Categories count: \(categories.count)")
                } catch {
                    print("SmilePileApp: Failed to initialize default categories: \(error)")
                }
            } else {
                print("SmilePileApp: Onboarding not completed - skipping default category initialization")
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