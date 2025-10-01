import Foundation
import SwiftUI

public struct Photo: Identifiable, Codable, Equatable {
    public let id: Int64
    public let path: String
    public let categoryId: Int64
    public let name: String
    public let isFromAssets: Bool
    public let createdAt: Int64
    public let fileSize: Int64
    public let width: Int
    public let height: Int

    // Computed properties matching Android
    public var displayName: String {
        if !name.isEmpty {
            return name
        }
        let url = URL(fileURLWithPath: path)
        return url.deletingPathExtension().lastPathComponent
    }

    public var isValid: Bool {
        !path.isEmpty && categoryId > 0
    }

    public init(
        id: Int64 = 0,
        path: String,
        categoryId: Int64,
        name: String = "",
        isFromAssets: Bool = false,
        createdAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        fileSize: Int64 = 0,
        width: Int = 0,
        height: Int = 0
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
    }

    // Helper methods
    public var createdDate: Date {
        Date(timeIntervalSince1970: TimeInterval(createdAt) / 1000)
    }

    public var formattedFileSize: String {
        let formatter = ByteCountFormatter()
        formatter.countStyle = .binary
        return formatter.string(fromByteCount: fileSize)
    }
}

public struct Category: Identifiable, Codable, Equatable {
    public let id: Int64
    public let name: String
    public let displayName: String
    public let position: Int
    public let iconResource: String?
    public let colorHex: String?
    public let isDefault: Bool
    public let createdAt: Int64

    public init(
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

    public var color: Color {
        if let hex = colorHex {
            return Color(hex: hex) ?? Color.orange
        }
        return Color.orange
    }

    public var createdDate: Date {
        Date(timeIntervalSince1970: TimeInterval(createdAt) / 1000)
    }

    // Default categories matching Android exactly
    public static func getDefaultCategories() -> [Category] {
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
    init(hex: String) {
        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        hexSanitized = hexSanitized.replacingOccurrences(of: "#", with: "")

        var rgb: UInt64 = 0

        guard Scanner(string: hexSanitized).scanHexInt64(&rgb), hexSanitized.count == 6 else {
            // Default to gray if hex parsing fails
            self = .gray
            return
        }

        self.init(
            red: Double((rgb & 0xFF0000) >> 16) / 255.0,
            green: Double((rgb & 0x00FF00) >> 8) / 255.0,
            blue: Double(rgb & 0x0000FF) / 255.0
        )
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