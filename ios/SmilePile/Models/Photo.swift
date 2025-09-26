import Foundation
import SwiftUI

struct Photo: Identifiable, Codable, Equatable {
    let id: Int64
    let path: String
    let categoryId: Int64
    let name: String
    let isFromAssets: Bool
    let createdAt: Int64
    let fileSize: Int64
    let width: Int
    let height: Int
    let isFavorite: Bool

    // Computed properties matching Android
    var displayName: String {
        if !name.isEmpty {
            return name
        }
        let url = URL(fileURLWithPath: path)
        return url.deletingPathExtension().lastPathComponent
    }

    var isValid: Bool {
        !path.isEmpty && categoryId > 0
    }

    init(
        id: Int64 = 0,
        path: String,
        categoryId: Int64,
        name: String = "",
        isFromAssets: Bool = false,
        createdAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        fileSize: Int64 = 0,
        width: Int = 0,
        height: Int = 0,
        isFavorite: Bool = false
    ) {
        self.id = id
        self.path = path
        self.categoryId = categoryId
        self.name = name
        self.isFromAssets = isFromAssets
        self.createdAt = createdAt
        self.fileSize = fileSize
        self.width = width
        self.height = height
        self.isFavorite = isFavorite
    }

    // Helper methods
    var createdDate: Date {
        Date(timeIntervalSince1970: TimeInterval(createdAt) / 1000)
    }

    var formattedFileSize: String {
        let formatter = ByteCountFormatter()
        formatter.countStyle = .binary
        return formatter.string(fromByteCount: fileSize)
    }
}

struct Category: Identifiable, Codable, Equatable {
    let id: Int64
    let name: String
    let displayName: String
    let position: Int
    let iconResource: String?
    let colorHex: String?
    let isDefault: Bool
    let createdAt: Int64

    init(
        id: Int64 = 0,
        name: String,
        displayName: String,
        position: Int = 0,
        iconResource: String? = nil,
        colorHex: String? = nil,
        isDefault: Bool = false,
        createdAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    ) {
        self.id = id
        self.name = name
        self.displayName = displayName
        self.position = position
        self.iconResource = iconResource
        self.colorHex = colorHex
        self.isDefault = isDefault
        self.createdAt = createdAt
    }

    var color: Color {
        if let hex = colorHex {
            return Color(hex: hex) ?? Color.orange
        }
        return Color.orange
    }

    var createdDate: Date {
        Date(timeIntervalSince1970: TimeInterval(createdAt) / 1000)
    }

    // Default categories matching Android exactly
    static func getDefaultCategories() -> [Category] {
        return [
            Category(
                id: 1,
                name: "family",
                displayName: "Family",
                position: 0,
                iconResource: nil,
                colorHex: "#E91E63",
                isDefault: true
            ),
            Category(
                id: 2,
                name: "cars",
                displayName: "Cars",
                position: 1,
                iconResource: nil,
                colorHex: "#F44336",
                isDefault: true
            ),
            Category(
                id: 3,
                name: "games",
                displayName: "Games",
                position: 2,
                iconResource: nil,
                colorHex: "#9C27B0",
                isDefault: true
            ),
            Category(
                id: 4,
                name: "sports",
                displayName: "Sports",
                position: 3,
                iconResource: nil,
                colorHex: "#4CAF50",
                isDefault: true
            )
        ]
    }
}

extension Color {
    init?(hex: String) {
        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        hexSanitized = hexSanitized.replacingOccurrences(of: "#", with: "")

        var rgb: UInt64 = 0

        guard Scanner(string: hexSanitized).scanHexInt64(&rgb) else { return nil }

        if hexSanitized.count == 6 {
            self.init(
                red: Double((rgb & 0xFF0000) >> 16) / 255.0,
                green: Double((rgb & 0x00FF00) >> 8) / 255.0,
                blue: Double(rgb & 0x0000FF) / 255.0
            )
        } else {
            return nil
        }
    }

    func toHex() -> String {
        let uiColor = UIColor(self)
        var red: CGFloat = 0
        var green: CGFloat = 0
        var blue: CGFloat = 0
        var alpha: CGFloat = 0

        uiColor.getRed(&red, green: &green, blue: &blue, alpha: &alpha)

        return String(format: "#%02X%02X%02X",
                     Int(red * 255),
                     Int(green * 255),
                     Int(blue * 255))
    }
}