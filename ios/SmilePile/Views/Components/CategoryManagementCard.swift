import SwiftUI

struct CategoryManagementCard: View {
    let categoryWithCount: CategoryWithCount
    let onEdit: (Category) -> Void
    let onDelete: (Category) -> Void

    var body: some View {
        HStack(spacing: 12) {
            // Large plain color circle
            Circle()
                .fill(Color(hex: categoryWithCount.category.colorHex ?? "#4CAF50") ?? Color.gray)
                .frame(width: 48, height: 48)

            // Category info
            VStack(alignment: .leading, spacing: 4) {
                Text(categoryWithCount.category.displayName)
                    .font(.headline)
                    .foregroundColor(.primary)

                Text("\(categoryWithCount.photoCount) photo\(categoryWithCount.photoCount == 1 ? "" : "s")")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }

            Spacer()

            // Action buttons with IconButton-style touch targets
            HStack(spacing: 8) {
                Button(action: { onEdit(categoryWithCount.category) }) {
                    Image(systemName: "pencil")
                        .font(.system(size: 22, weight: .medium))
                        .foregroundColor(Color(hex: "#FF6600"))
                        .frame(width: 44, height: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(PlainButtonStyle())

                Button(action: { onDelete(categoryWithCount.category) }) {
                    Image(systemName: "trash.fill")
                        .font(.system(size: 22, weight: .medium))
                        .foregroundColor(Color(UIColor.systemRed))
                        .frame(width: 44, height: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(PlainButtonStyle())
            }
        }
        .padding(12)
        .background(Color(UIColor.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
    }
}

#Preview {
    VStack(spacing: 16) {
        CategoryManagementCard(
            categoryWithCount: CategoryWithCount(
                category: Category(
                    id: 1,
                    name: "family",
                    displayName: "Family",
                    position: 0,
                    colorHex: "#E91E63",
                    isDefault: true
                ),
                photoCount: 25
            ),
            onEdit: { _ in },
            onDelete: { _ in }
        )

        CategoryManagementCard(
            categoryWithCount: CategoryWithCount(
                category: Category(
                    id: 2,
                    name: "vacation",
                    displayName: "Vacation",
                    position: 1,
                    colorHex: "#2196F3",
                    isDefault: false
                ),
                photoCount: 0
            ),
            onEdit: { _ in },
            onDelete: { _ in }
        )
    }
    .padding()
}