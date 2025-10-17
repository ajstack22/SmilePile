import Foundation
import UIKit

/// Demo mode data provider for SmilePile
/// Provides pre-populated demo data for the "Jamie Anderson" demo profile
struct DemoData {

    // MARK: - Category Definitions

    struct CategoryData {
        let name: String
        let displayName: String
        let colorHex: String
        let icon: String
        let position: Int
    }

    static let categories: [CategoryData] = [
        CategoryData(
            name: "milestones",
            displayName: "Milestones",
            colorHex: "#9C27B0",
            icon: "star.fill",
            position: 0
        ),
        CategoryData(
            name: "birthdays",
            displayName: "Birthdays",
            colorHex: "#E91E63",
            icon: "gift.fill",
            position: 1
        ),
        CategoryData(
            name: "holidays",
            displayName: "Holidays",
            colorHex: "#F44336",
            icon: "heart.fill",
            position: 2
        ),
        CategoryData(
            name: "family",
            displayName: "Family",
            colorHex: "#4CAF50",
            icon: "person.3.fill",
            position: 3
        ),
        CategoryData(
            name: "playtime",
            displayName: "Playtime",
            colorHex: "#FF9800",
            icon: "gamecontroller.fill",
            position: 4
        ),
        CategoryData(
            name: "friends",
            displayName: "Friends",
            colorHex: "#2196F3",
            icon: "person.2.fill",
            position: 5
        ),
        CategoryData(
            name: "creativity",
            displayName: "Creativity",
            colorHex: "#00BCD4",
            icon: "paintbrush.fill",
            position: 6
        ),
        CategoryData(
            name: "adventures",
            displayName: "Adventures",
            colorHex: "#795548",
            icon: "map.fill",
            position: 7
        )
    ]

    // MARK: - Photo Metadata

    struct PhotoMetadata {
        let assetName: String
        let categoryName: String
        let caption: String
        let date: Date
    }

    static let photoMetadata: [PhotoMetadata] = [
        // Milestones (8 photos)
        PhotoMetadata(
            assetName: "demo_milestones_001",
            categoryName: "milestones",
            caption: "First steps at 13 months!",
            date: Date().addingTimeInterval(-365*24*60*60*3.5) // 3.5 years ago
        ),
        PhotoMetadata(
            assetName: "demo_milestones_002",
            categoryName: "milestones",
            caption: "First day of preschool",
            date: Date().addingTimeInterval(-365*24*60*60*2) // 2 years ago
        ),
        PhotoMetadata(
            assetName: "demo_milestones_003",
            categoryName: "milestones",
            caption: "Learning to ride a bike",
            date: Date().addingTimeInterval(-365*24*60*60*1.5) // 1.5 years ago
        ),
        PhotoMetadata(
            assetName: "demo_milestones_004",
            categoryName: "milestones",
            caption: "First time tying shoes!",
            date: Date().addingTimeInterval(-365*24*60*60*1) // 1 year ago
        ),
        PhotoMetadata(
            assetName: "demo_milestones_005",
            categoryName: "milestones",
            caption: "Reading first book independently",
            date: Date().addingTimeInterval(-180*24*60*60) // 6 months ago
        ),
        PhotoMetadata(
            assetName: "demo_milestones_006",
            categoryName: "milestones",
            caption: "Writing name for first time",
            date: Date().addingTimeInterval(-90*24*60*60) // 3 months ago
        ),
        PhotoMetadata(
            assetName: "demo_milestones_007",
            categoryName: "milestones",
            caption: "Lost first tooth!",
            date: Date().addingTimeInterval(-60*24*60*60) // 2 months ago
        ),
        PhotoMetadata(
            assetName: "demo_milestones_008",
            categoryName: "milestones",
            caption: "First swimming lesson",
            date: Date().addingTimeInterval(-30*24*60*60) // 1 month ago
        ),

        // Birthdays (5 photos)
        PhotoMetadata(
            assetName: "demo_birthdays_001",
            categoryName: "birthdays",
            caption: "1st birthday - cake smash!",
            date: Date().addingTimeInterval(-365*24*60*60*4) // 4 years ago
        ),
        PhotoMetadata(
            assetName: "demo_birthdays_002",
            categoryName: "birthdays",
            caption: "2nd birthday party at the park",
            date: Date().addingTimeInterval(-365*24*60*60*3) // 3 years ago
        ),
        PhotoMetadata(
            assetName: "demo_birthdays_003",
            categoryName: "birthdays",
            caption: "3rd birthday - dinosaur theme!",
            date: Date().addingTimeInterval(-365*24*60*60*2) // 2 years ago
        ),
        PhotoMetadata(
            assetName: "demo_birthdays_004",
            categoryName: "birthdays",
            caption: "4th birthday with friends",
            date: Date().addingTimeInterval(-365*24*60*60*1) // 1 year ago
        ),
        PhotoMetadata(
            assetName: "demo_birthdays_005",
            categoryName: "birthdays",
            caption: "5th birthday party - big kid now!",
            date: Date().addingTimeInterval(-30*24*60*60) // 1 month ago
        ),

        // Holidays (6 photos)
        PhotoMetadata(
            assetName: "demo_holidays_001",
            categoryName: "holidays",
            caption: "First Halloween - pumpkin costume",
            date: Date().addingTimeInterval(-365*24*60*60*3.2) // Oct 3+ years ago
        ),
        PhotoMetadata(
            assetName: "demo_holidays_002",
            categoryName: "holidays",
            caption: "Christmas morning excitement",
            date: Date().addingTimeInterval(-365*24*60*60*2.1) // Dec 2+ years ago
        ),
        PhotoMetadata(
            assetName: "demo_holidays_003",
            categoryName: "holidays",
            caption: "Easter egg hunt success!",
            date: Date().addingTimeInterval(-365*24*60*60*1.7) // Apr 1.7 years ago
        ),
        PhotoMetadata(
            assetName: "demo_holidays_004",
            categoryName: "holidays",
            caption: "4th of July sparklers",
            date: Date().addingTimeInterval(-365*24*60*60*1.3) // Jul 1.3 years ago
        ),
        PhotoMetadata(
            assetName: "demo_holidays_005",
            categoryName: "holidays",
            caption: "Thanksgiving with grandparents",
            date: Date().addingTimeInterval(-330*24*60*60) // Nov last year
        ),
        PhotoMetadata(
            assetName: "demo_holidays_006",
            categoryName: "holidays",
            caption: "Valentine's Day card making",
            date: Date().addingTimeInterval(-240*24*60*60) // Feb this year
        ),

        // Family (4 photos)
        PhotoMetadata(
            assetName: "demo_family_001",
            categoryName: "family",
            caption: "Family beach vacation",
            date: Date().addingTimeInterval(-365*24*60*60*2.5) // 2.5 years ago
        ),
        PhotoMetadata(
            assetName: "demo_family_002",
            categoryName: "family",
            caption: "Sunday morning pancakes",
            date: Date().addingTimeInterval(-365*24*60*60*1.2) // 1.2 years ago
        ),
        PhotoMetadata(
            assetName: "demo_family_003",
            categoryName: "family",
            caption: "Visiting grandpa's farm",
            date: Date().addingTimeInterval(-200*24*60*60) // ~7 months ago
        ),
        PhotoMetadata(
            assetName: "demo_family_004",
            categoryName: "family",
            caption: "Family movie night",
            date: Date().addingTimeInterval(-45*24*60*60) // 1.5 months ago
        ),

        // Playtime (4 photos)
        PhotoMetadata(
            assetName: "demo_playtime_001",
            categoryName: "playtime",
            caption: "Building block tower",
            date: Date().addingTimeInterval(-365*24*60*60*2.8) // 2.8 years ago
        ),
        PhotoMetadata(
            assetName: "demo_playtime_002",
            categoryName: "playtime",
            caption: "Dress-up day as a superhero",
            date: Date().addingTimeInterval(-365*24*60*60*1.4) // 1.4 years ago
        ),
        PhotoMetadata(
            assetName: "demo_playtime_003",
            categoryName: "playtime",
            caption: "Playground adventure",
            date: Date().addingTimeInterval(-150*24*60*60) // 5 months ago
        ),
        PhotoMetadata(
            assetName: "demo_playtime_004",
            categoryName: "playtime",
            caption: "Puzzle master!",
            date: Date().addingTimeInterval(-60*24*60*60) // 2 months ago
        ),

        // Friends (3 photos)
        PhotoMetadata(
            assetName: "demo_friends_001",
            categoryName: "friends",
            caption: "First playdate with neighbor",
            date: Date().addingTimeInterval(-365*24*60*60*2.2) // 2.2 years ago
        ),
        PhotoMetadata(
            assetName: "demo_friends_002",
            categoryName: "friends",
            caption: "Preschool best friends",
            date: Date().addingTimeInterval(-365*24*60*60*1.1) // 1.1 years ago
        ),
        PhotoMetadata(
            assetName: "demo_friends_003",
            categoryName: "friends",
            caption: "Soccer team buddies",
            date: Date().addingTimeInterval(-120*24*60*60) // 4 months ago
        ),

        // Creativity (3 photos)
        PhotoMetadata(
            assetName: "demo_creativity_001",
            categoryName: "creativity",
            caption: "Finger painting masterpiece",
            date: Date().addingTimeInterval(-365*24*60*60*2.6) // 2.6 years ago
        ),
        PhotoMetadata(
            assetName: "demo_creativity_002",
            categoryName: "creativity",
            caption: "Sidewalk chalk art",
            date: Date().addingTimeInterval(-210*24*60*60) // 7 months ago
        ),
        PhotoMetadata(
            assetName: "demo_creativity_003",
            categoryName: "creativity",
            caption: "Building sandcastle at beach",
            date: Date().addingTimeInterval(-90*24*60*60) // 3 months ago
        ),

        // Adventures (2 photos)
        PhotoMetadata(
            assetName: "demo_adventures_001",
            categoryName: "adventures",
            caption: "First trip to the zoo",
            date: Date().addingTimeInterval(-365*24*60*60*1.8) // 1.8 years ago
        ),
        PhotoMetadata(
            assetName: "demo_adventures_002",
            categoryName: "adventures",
            caption: "Hiking in the mountains",
            date: Date().addingTimeInterval(-75*24*60*60) // 2.5 months ago
        )
    ]

    // MARK: - Helper Methods

    /// Get the category ID for a given category name from loaded categories
    static func getCategoryId(for categoryName: String, from loadedCategories: [Category]) -> Int64? {
        return loadedCategories.first(where: { $0.name == categoryName })?.id
    }
}
