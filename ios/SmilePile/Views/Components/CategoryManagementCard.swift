import SwiftUI

struct CategoryManagementCard: View {
    let categoryWithCount: CategoryWithCount
    let onEdit: (Category) -> Void
    let onDelete: (Category) -> Void

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 12) {
                    CategoryColorIndicator(
                        colorHex: categoryWithCount.category.colorHex ?? "#4CAF50",
                        size: 20
                    )

                    Text(categoryWithCount.category.displayName)
                        .font(.headline)
                        .foregroundColor(.primary)

                    if categoryWithCount.category.isDefault {
                        Text("DEFAULT")
                            .font(.caption)
                            .fontWeight(.medium)
                            .foregroundColor(.white)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.orange)
                            .clipShape(Capsule())
                    }

                    Spacer()
                }

                HStack {
                    Label("\(categoryWithCount.photoCount) photo\(categoryWithCount.photoCount == 1 ? "" : "s")",
                          systemImage: "photo")
                        .font(.subheadline)
                        .foregroundColor(.secondary)

                    Spacer()

                    HStack(spacing: 16) {
                        Button(action: { onEdit(categoryWithCount.category) }) {
                            Image(systemName: "pencil")
                                .font(.system(size: 16))
                                .foregroundColor(.blue)
                        }
                        .buttonStyle(PlainButtonStyle())

                        Button(action: { onDelete(categoryWithCount.category) }) {
                            Image(systemName: "trash")
                                .font(.system(size: 16))
                                .foregroundColor(.red)
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                }
            }
            .padding()
        }
        .background(Color(UIColor.systemBackground))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.gray.opacity(0.2), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.05), radius: 2, x: 0, y: 2)
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