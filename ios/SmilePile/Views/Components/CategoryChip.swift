import SwiftUI

struct CategoryChip: View {
    let displayName: String
    let colorHex: String
    let isSelected: Bool
    let photoCount: Int?
    let onTap: (() -> Void)?
    @Environment(\.colorScheme) var colorScheme

    init(
        displayName: String,
        colorHex: String,
        isSelected: Bool = false,
        photoCount: Int? = nil,
        onTap: (() -> Void)? = nil
    ) {
        self.displayName = displayName
        self.colorHex = colorHex
        self.isSelected = isSelected
        self.photoCount = photoCount
        self.onTap = onTap
    }

    private var textWeight: Font.Weight {
        if colorScheme == .dark {
            return .bold // Bold in dark mode
        } else if isSelected {
            return .medium // Medium when selected in light mode
        } else {
            return .regular // Regular otherwise
        }
    }

    var body: some View {
        Button(action: { onTap?() }) {
            HStack(spacing: 8) {
                Circle()
                    .fill(Color(hex: colorHex) ?? .orange)
                    .frame(width: 12, height: 12)
                    .overlay(
                        Circle()
                            .stroke(Color.primary.opacity(0.3), lineWidth: 1)
                    )

                Text(displayName)
                    .font(.system(size: 14, weight: textWeight))
                    .foregroundColor(isSelected ? .primary : .secondary)

                if let count = photoCount, count > 0 {
                    Text("\(count)")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color.secondary.opacity(0.1))
                        .clipShape(Capsule())
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(isSelected ? Color.primary.opacity(0.1) : Color.clear)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(isSelected ? Color.primary : Color.primary.opacity(0.3), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(PlainButtonStyle())
        .disabled(onTap == nil)
    }
}

struct SelectableCategoryChip: View {
    let category: Category
    let isSelected: Bool
    let photoCount: Int
    let onTap: () -> Void

    var body: some View {
        CategoryChip(
            displayName: category.displayName,
            colorHex: category.colorHex ?? "#4CAF50",
            isSelected: isSelected,
            photoCount: photoCount,
            onTap: onTap
        )
    }
}

struct CategoryColorIndicator: View {
    let colorHex: String
    let size: CGFloat

    init(colorHex: String, size: CGFloat = 14) {
        self.colorHex = colorHex
        self.size = size
    }

    var body: some View {
        Circle()
            .fill(Color(hex: colorHex) ?? .orange)
            .frame(width: size, height: size)
            .overlay(
                Circle()
                    .stroke(Color.primary.opacity(0.3), lineWidth: 1)
            )
    }
}

#Preview {
    VStack(spacing: 20) {
        CategoryChip(
            displayName: "Family",
            colorHex: "#E91E63",
            isSelected: false
        )

        CategoryChip(
            displayName: "Games",
            colorHex: "#9C27B0",
            isSelected: true,
            photoCount: 25
        )

        HStack {
            CategoryColorIndicator(colorHex: "#4CAF50")
            CategoryColorIndicator(colorHex: "#2196F3", size: 20)
            CategoryColorIndicator(colorHex: "#FF9800", size: 24)
        }
    }
    .padding()
}