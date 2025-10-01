// ColorConstants.swift
// SmilePile Brand Colors and UI Theme
//
// These colors match the Android implementation exactly
// and are used throughout the iOS app for visual consistency.

import SwiftUI

extension Color {
    // SmilePile Brand Colors
    // These colors are used in the SmilePile logo where each letter has its own color
    static let smilePileYellow = Color(hex: "#FFBF00")  // Smile character
    static let smilePileGreen = Color(hex: "#4CAF50")   // P character
    static let smilePileBlue = Color(hex: "#2196F3")    // i character, buttons
    static let smilePileOrange = Color(hex: "#FF6600")  // l character, icons
    static let smilePilePink = Color(hex: "#E86082")    // e character

    // Category/Pile Colors
    // Default colors for user-created piles
    static let pileRed = Color(hex: "#FF6B6B")
    static let pileTeal = Color(hex: "#4ECDC4")
    static let pileYellow = Color(hex: "#FFEAA7")

    // Additional named colors from palette
    static let pileAqua = Color(hex: "#45B7D1")     // Light blue/aqua
    static let pilePurple = Color(hex: "#DDA0DD")   // Light purple

    // Additional Palette Colors
    // Full set of colors available for pile creation
    static let paletteColors = [
        "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4",
        "#FFEAA7", "#DDA0DD", "#FFA07A", "#98D8C8",
        "#F7DC6F", "#BB8FCE", "#85C1E2", "#F8B739"
    ]

    // UI Colors
    // Common colors used throughout the interface
    static let primaryButton = smilePileBlue
    static let secondaryText = Color.secondary
}