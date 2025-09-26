import SwiftUI

/// Unified header component for all parent mode screens
/// Shows SmilePile logo on left and View Mode eye icon on right
struct AppHeaderComponent<Content: View>: View {
    let onViewModeClick: () -> Void
    let showViewModeButton: Bool
    @ViewBuilder let content: () -> Content

    init(
        onViewModeClick: @escaping () -> Void = {},
        showViewModeButton: Bool = true,
        @ViewBuilder content: @escaping () -> Content = { EmptyView() }
    ) {
        self.onViewModeClick = onViewModeClick
        self.showViewModeButton = showViewModeButton
        self.content = content
    }

    var body: some View {
        VStack(spacing: 0) {
            // Header bar with logo and view mode button
            HStack {
                // SmilePile logo on the left - matching Android's 48dp size
                SmilePileLogo(iconSize: 48, fontSize: 28)

                Spacer()

                // View Mode eye icon on the right
                if showViewModeButton {
                    Button(action: onViewModeClick) {
                        ZStack {
                            Circle()
                                .fill(Color(hex: "#4CAF50") ?? Color.green) // Solid green background with fallback
                                .frame(width: 48, height: 48) // Same size as logo

                            Image(systemName: "eye.fill")
                                .font(.system(size: 28))
                                .foregroundColor(.white)
                        }
                    }
                    .buttonStyle(PlainButtonStyle())
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            // Add minimal spacing between branding and categories
            Spacer().frame(height: 4)

            // Additional content (like category filters)
            content()
        }
        .background(Color(UIColor.secondarySystemBackground))
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