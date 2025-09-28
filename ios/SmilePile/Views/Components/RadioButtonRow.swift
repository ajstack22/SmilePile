import SwiftUI

/// Custom radio button row component matching Android Material Design
struct RadioButtonRow: View {
    let isSelected: Bool
    let icon: String
    let title: String
    let subtitle: String
    let action: () -> Void

    private let orangeAccent = Color(red: 1.0, green: 0.596, blue: 0) // #FF9800

    var body: some View {
        Button(action: action) {
            HStack(spacing: 0) {
                // Radio button circle
                ZStack {
                    Circle()
                        .stroke(isSelected ? orangeAccent : Color.secondary.opacity(0.6), lineWidth: 2)
                        .frame(width: 20, height: 20)

                    if isSelected {
                        Circle()
                            .fill(orangeAccent)
                            .frame(width: 12, height: 12)
                    }
                }
                .padding(.leading, 16)

                // Icon
                Image(systemName: icon)
                    .font(.system(size: 24))
                    .foregroundColor(isSelected ? orangeAccent : Color.secondary.opacity(0.6))
                    .frame(width: 24, height: 24)
                    .padding(.leading, 16)

                // Text content
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.body)
                        .foregroundColor(.primary)

                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding(.leading, 12)

                Spacer()
            }
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainButtonStyle())
    }
}