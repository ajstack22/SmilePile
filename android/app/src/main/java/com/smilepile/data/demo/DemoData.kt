package com.smilepile.data.demo

import com.smilepile.data.models.Category
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/**
 * Demo mode data provider for SmilePile
 * Provides pre-populated demo data for the "Jamie Anderson" demo profile
 */
object DemoData {

    // MARK: - Category Definitions

    data class CategoryData(
        val name: String,
        val displayName: String,
        val colorHex: String,
        val icon: String?,
        val position: Int
    )

    val categories = listOf(
        CategoryData(
            name = "milestones",
            displayName = "Milestones",
            colorHex = "#9C27B0",
            icon = "star",
            position = 0
        ),
        CategoryData(
            name = "birthdays",
            displayName = "Birthdays",
            colorHex = "#E91E63",
            icon = "cake",
            position = 1
        ),
        CategoryData(
            name = "holidays",
            displayName = "Holidays",
            colorHex = "#F44336",
            icon = "celebration",
            position = 2
        ),
        CategoryData(
            name = "family",
            displayName = "Family",
            colorHex = "#4CAF50",
            icon = "family_restroom",
            position = 3
        ),
        CategoryData(
            name = "playtime",
            displayName = "Playtime",
            colorHex = "#FF9800",
            icon = "toys",
            position = 4
        ),
        CategoryData(
            name = "friends",
            displayName = "Friends",
            colorHex = "#2196F3",
            icon = "people",
            position = 5
        ),
        CategoryData(
            name = "creativity",
            displayName = "Creativity",
            colorHex = "#00BCD4",
            icon = "palette",
            position = 6
        ),
        CategoryData(
            name = "adventures",
            displayName = "Adventures",
            colorHex = "#795548",
            icon = "explore",
            position = 7
        )
    )

    // MARK: - Photo Metadata

    data class PhotoMetadata(
        val assetName: String,
        val categoryName: String,
        val caption: String,
        val date: LocalDate
    )

    private fun daysAgo(days: Long): LocalDate {
        return LocalDate.now().minusDays(days)
    }

    private fun yearsAgo(years: Double): LocalDate {
        return LocalDate.now().minusDays((years * 365).toLong())
    }

    val photoMetadata = listOf(
        // Milestones (8 photos)
        PhotoMetadata(
            assetName = "demo_milestones_001",
            categoryName = "milestones",
            caption = "First steps at 13 months!",
            date = yearsAgo(3.5)
        ),
        PhotoMetadata(
            assetName = "demo_milestones_002",
            categoryName = "milestones",
            caption = "First day of preschool",
            date = yearsAgo(2.0)
        ),
        PhotoMetadata(
            assetName = "demo_milestones_003",
            categoryName = "milestones",
            caption = "Learning to ride a bike",
            date = yearsAgo(1.5)
        ),
        PhotoMetadata(
            assetName = "demo_milestones_004",
            categoryName = "milestones",
            caption = "First time tying shoes!",
            date = yearsAgo(1.0)
        ),
        PhotoMetadata(
            assetName = "demo_milestones_005",
            categoryName = "milestones",
            caption = "Reading first book independently",
            date = daysAgo(180)
        ),
        PhotoMetadata(
            assetName = "demo_milestones_006",
            categoryName = "milestones",
            caption = "Writing name for first time",
            date = daysAgo(90)
        ),
        PhotoMetadata(
            assetName = "demo_milestones_007",
            categoryName = "milestones",
            caption = "Lost first tooth!",
            date = daysAgo(60)
        ),
        PhotoMetadata(
            assetName = "demo_milestones_008",
            categoryName = "milestones",
            caption = "First swimming lesson",
            date = daysAgo(30)
        ),

        // Birthdays (5 photos)
        PhotoMetadata(
            assetName = "demo_birthdays_001",
            categoryName = "birthdays",
            caption = "1st birthday - cake smash!",
            date = yearsAgo(4.0)
        ),
        PhotoMetadata(
            assetName = "demo_birthdays_002",
            categoryName = "birthdays",
            caption = "2nd birthday party at the park",
            date = yearsAgo(3.0)
        ),
        PhotoMetadata(
            assetName = "demo_birthdays_003",
            categoryName = "birthdays",
            caption = "3rd birthday - dinosaur theme!",
            date = yearsAgo(2.0)
        ),
        PhotoMetadata(
            assetName = "demo_birthdays_004",
            categoryName = "birthdays",
            caption = "4th birthday with friends",
            date = yearsAgo(1.0)
        ),
        PhotoMetadata(
            assetName = "demo_birthdays_005",
            categoryName = "birthdays",
            caption = "5th birthday party - big kid now!",
            date = daysAgo(30)
        ),

        // Holidays (6 photos)
        PhotoMetadata(
            assetName = "demo_holidays_001",
            categoryName = "holidays",
            caption = "First Halloween - pumpkin costume",
            date = yearsAgo(3.2)
        ),
        PhotoMetadata(
            assetName = "demo_holidays_002",
            categoryName = "holidays",
            caption = "Christmas morning excitement",
            date = yearsAgo(2.1)
        ),
        PhotoMetadata(
            assetName = "demo_holidays_003",
            categoryName = "holidays",
            caption = "Easter egg hunt success!",
            date = yearsAgo(1.7)
        ),
        PhotoMetadata(
            assetName = "demo_holidays_004",
            categoryName = "holidays",
            caption = "4th of July sparklers",
            date = yearsAgo(1.3)
        ),
        PhotoMetadata(
            assetName = "demo_holidays_005",
            categoryName = "holidays",
            caption = "Thanksgiving with grandparents",
            date = daysAgo(330)
        ),
        PhotoMetadata(
            assetName = "demo_holidays_006",
            categoryName = "holidays",
            caption = "Valentine's Day card making",
            date = daysAgo(240)
        ),

        // Family (4 photos)
        PhotoMetadata(
            assetName = "demo_family_001",
            categoryName = "family",
            caption = "Family beach vacation",
            date = yearsAgo(2.5)
        ),
        PhotoMetadata(
            assetName = "demo_family_002",
            categoryName = "family",
            caption = "Sunday morning pancakes",
            date = yearsAgo(1.2)
        ),
        PhotoMetadata(
            assetName = "demo_family_003",
            categoryName = "family",
            caption = "Visiting grandpa's farm",
            date = daysAgo(200)
        ),
        PhotoMetadata(
            assetName = "demo_family_004",
            categoryName = "family",
            caption = "Family movie night",
            date = daysAgo(45)
        ),

        // Playtime (4 photos)
        PhotoMetadata(
            assetName = "demo_playtime_001",
            categoryName = "playtime",
            caption = "Building block tower",
            date = yearsAgo(2.8)
        ),
        PhotoMetadata(
            assetName = "demo_playtime_002",
            categoryName = "playtime",
            caption = "Dress-up day as a superhero",
            date = yearsAgo(1.4)
        ),
        PhotoMetadata(
            assetName = "demo_playtime_003",
            categoryName = "playtime",
            caption = "Playground adventure",
            date = daysAgo(150)
        ),
        PhotoMetadata(
            assetName = "demo_playtime_004",
            categoryName = "playtime",
            caption = "Puzzle master!",
            date = daysAgo(60)
        ),

        // Friends (3 photos)
        PhotoMetadata(
            assetName = "demo_friends_001",
            categoryName = "friends",
            caption = "First playdate with neighbor",
            date = yearsAgo(2.2)
        ),
        PhotoMetadata(
            assetName = "demo_friends_002",
            categoryName = "friends",
            caption = "Preschool best friends",
            date = yearsAgo(1.1)
        ),
        PhotoMetadata(
            assetName = "demo_friends_003",
            categoryName = "friends",
            caption = "Soccer team buddies",
            date = daysAgo(120)
        ),

        // Creativity (3 photos)
        PhotoMetadata(
            assetName = "demo_creativity_001",
            categoryName = "creativity",
            caption = "Finger painting masterpiece",
            date = yearsAgo(2.6)
        ),
        PhotoMetadata(
            assetName = "demo_creativity_002",
            categoryName = "creativity",
            caption = "Sidewalk chalk art",
            date = daysAgo(210)
        ),
        PhotoMetadata(
            assetName = "demo_creativity_003",
            categoryName = "creativity",
            caption = "Building sandcastle at beach",
            date = daysAgo(90)
        ),

        // Adventures (2 photos)
        PhotoMetadata(
            assetName = "demo_adventures_001",
            categoryName = "adventures",
            caption = "First trip to the zoo",
            date = yearsAgo(1.8)
        ),
        PhotoMetadata(
            assetName = "demo_adventures_002",
            categoryName = "adventures",
            caption = "Hiking in the mountains",
            date = daysAgo(75)
        )
    )

    // MARK: - Helper Methods

    /**
     * Get the category ID for a given category name from loaded categories
     */
    fun getCategoryId(categoryName: String, loadedCategories: List<Category>): Long? {
        return loadedCategories.firstOrNull { it.name == categoryName }?.id
    }
}
