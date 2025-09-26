import SwiftUI

struct ColorSelectionButton: View {
    let colorHex: String
    let isSelected: Bool
    let onSelect: () -> Void

    var body: some View {
        Button(action: onSelect) {
            ZStack {
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color(hex: colorHex) ?? .gray)
                    .frame(width: 44, height: 44)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(
                                isSelected ? Color.primary : Color.gray.opacity(0.3),
                                lineWidth: isSelected ? 2 : 1
                            )
                    )

                if isSelected {
                    Image(systemName: "checkmark")
                        .foregroundColor(.white)
                        .font(.system(size: 16, weight: .bold))
                        .shadow(color: .black.opacity(0.5), radius: 1, x: 0, y: 1)
                }
            }
        }
        .buttonStyle(PlainButtonStyle())
        .scaleEffect(isSelected ? 1.1 : 1.0)
        .animation(.easeInOut(duration: 0.15), value: isSelected)
    }
}

struct ColorPickerGrid: View {
    let colors: [String]
    @Binding var selectedColor: String
    let columns = Array(repeating: GridItem(.flexible(), spacing: 10), count: 6)

    init(colors: [String] = ColorPickerGrid.defaultColors, selectedColor: Binding<String>) {
        self.colors = colors
        self._selectedColor = selectedColor
    }

    static let defaultColors = [
        "#4CAF50", "#2196F3", "#FF9800", "#9C27B0",
        "#F44336", "#FF5722", "#795548", "#607D8B",
        "#E91E63", "#009688", "#FFEB3B", "#3F51B5"
    ]

    var body: some View {
        LazyVGrid(columns: columns, spacing: 10) {
            ForEach(colors, id: \.self) { colorHex in
                ColorSelectionButton(
                    colorHex: colorHex,
                    isSelected: selectedColor == colorHex,
                    onSelect: {
                        selectedColor = colorHex
                    }
                )
            }
        }
    }
}

#Preview {
    struct PreviewWrapper: View {
        @State private var selectedColor = "#4CAF50"

        var body: some View {
            VStack(spacing: 30) {
                Text("Selected: \(selectedColor)")
                    .font(.headline)

                ColorPickerGrid(selectedColor: $selectedColor)
                    .padding()

                CategoryChip(
                    displayName: "Preview",
                    colorHex: selectedColor,
                    isSelected: true
                )
            }
            .padding()
        }
    }

    return PreviewWrapper()
}