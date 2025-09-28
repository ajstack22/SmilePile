import SwiftUI
import UIKit

/// Visual effect blur view for iOS native transparency effects
struct VisualEffectBlur: UIViewRepresentable {
    var blurStyle: UIBlurEffect.Style

    func makeUIView(context: Context) -> UIVisualEffectView {
        UIVisualEffectView(effect: UIBlurEffect(style: blurStyle))
    }

    func updateUIView(_ uiView: UIVisualEffectView, context: Context) {
        uiView.effect = UIBlurEffect(style: blurStyle)
    }
}

/// Unified header component for all parent mode screens
/// Shows SmilePile logo on left and View Mode eye icon on right
/// Implements iOS-native blur effect with surfaceVariant overlay matching Android
struct AppHeaderComponent<Content: View>: View {
    let onViewModeClick: () -> Void
    let showViewModeButton: Bool
    @ViewBuilder let content: () -> Content
    @Environment(\.colorScheme) var colorScheme

    init(
        onViewModeClick: @escaping () -> Void = {},
        showViewModeButton: Bool = true,
        @ViewBuilder content: @escaping () -> Content = { EmptyView() }
    ) {
        self.onViewModeClick = onViewModeClick
        self.showViewModeButton = showViewModeButton
        self.content = content
    }

    // Surface variant color matching Android's Material Design
    private var surfaceVariantColor: Color {
        colorScheme == .dark
            ? Color(hex: "#49454F") ?? Color(UIColor.secondarySystemBackground)
            : Color(hex: "#E0E0E0") ?? Color(UIColor.secondarySystemBackground)
    }

    var body: some View {
        VStack(spacing: 0) {
            // Simple header bar with safe area
            HStack {
                // SmilePile logo on the left (matching Android: 48dp icon, 28sp text)
                SmilePileLogo(iconSize: 48, fontSize: 28, showIcon: true)

                Spacer()

                // View Mode eye icon on the right (matching Android: 48dp button)
                if showViewModeButton {
                    Button(action: onViewModeClick) {
                        ZStack {
                            Circle()
                                .fill(Color(hex: "#4CAF50") ?? Color.green)
                                .frame(width: 48, height: 48)

                            Image(systemName: "eye.fill")
                                .font(.system(size: 28))
                                .foregroundColor(.white)
                        }
                    }
                    .buttonStyle(PlainButtonStyle())
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 50)  // Push content below Dynamic Island
            .padding(.vertical, 8)
            .frame(maxWidth: .infinity)
            .background(
                ZStack {
                    VisualEffectBlur(blurStyle: .systemMaterial)
                    surfaceVariantColor.opacity(0.7)
                }
                .ignoresSafeArea(edges: .top)
            )

            // Additional content (like category filters)
            content()
        }
    }
}

// Extension for convenience init when no content
extension AppHeaderComponent where Content == EmptyView {
    init(
        onViewModeClick: @escaping () -> Void = {},
        showViewModeButton: Bool = true
    ) {
        self.init(
            onViewModeClick: onViewModeClick,
            showViewModeButton: showViewModeButton,
            content: { EmptyView() }
        )
    }
}

#Preview("AppHeaderComponent Light") {
    VStack {
        AppHeaderComponent(
            onViewModeClick: { print("View mode clicked") },
            showViewModeButton: true
        ) {
            // Sample content like category chips
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(0..<5) { index in
                        Chip(text: "Category \(index)")
                    }
                }
                .padding(.horizontal, 16)
            }
        }

        Spacer()
    }
    .preferredColorScheme(.light)
}

#Preview("AppHeaderComponent Dark") {
    VStack {
        AppHeaderComponent(
            onViewModeClick: { print("View mode clicked") },
            showViewModeButton: true
        ) {
            // Empty content
            EmptyView()
        }

        Spacer()
    }
    .preferredColorScheme(.dark)
}

#Preview("AppHeaderComponent Without Button") {
    VStack {
        AppHeaderComponent(
            showViewModeButton: false
        )

        Spacer()
    }
}

// Simple chip view for preview
private struct Chip: View {
    let text: String

    var body: some View {
        Text(text)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(Color.blue.opacity(0.2))
            .cornerRadius(16)
    }
}