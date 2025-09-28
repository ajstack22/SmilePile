import SwiftUI

// MARK: - Floating Action Button Component
struct FloatingActionButton: View {
    let action: () -> Void
    let isPulsing: Bool

    @State private var pulseScale: CGFloat = 1.0
    @State private var isPressed = false

    private let buttonSize: CGFloat = 56
    private let iconSize: CGFloat = 24
    private let pinkColor = Color(red: 233/255, green: 30/255, blue: 99/255) // #E91E63

    var body: some View {
        Button(action: {
            // Haptic feedback
            let impactFeedback = UIImpactFeedbackGenerator(style: .medium)
            impactFeedback.impactOccurred()

            action()
        }) {
            ZStack {
                // Square FAB with rounded corners to match categories
                RoundedRectangle(cornerRadius: 12)
                    .fill(pinkColor)
                    .frame(width: buttonSize, height: buttonSize)
                    .shadow(color: Color.black.opacity(0.3), radius: 4, x: 0, y: 4)

                // Plus icon
                Image(systemName: "plus")
                    .font(.system(size: iconSize, weight: .medium))
                    .foregroundColor(.white)
            }
        }
        .scaleEffect(isPressed ? 0.9 : (isPulsing ? pulseScale : 1.0))
        .animation(isPressed ? .easeOut(duration: 0.1) : nil, value: isPressed)
        .onLongPressGesture(minimumDuration: 0, maximumDistance: .infinity) { pressing in
            isPressed = pressing
        } perform: {
            action()
        }
        .onAppear {
            if isPulsing {
                startPulseAnimation()
            }
        }
        .onChange(of: isPulsing) { newValue in
            if newValue {
                startPulseAnimation()
            } else {
                stopPulseAnimation()
            }
        }
        .accessibilityLabel("Add photos")
        .accessibilityHint("Double tap to import photos")
    }

    private func startPulseAnimation() {
        withAnimation(
            Animation.easeInOut(duration: 1.0)
            .repeatForever(autoreverses: true)
        ) {
            pulseScale = 1.1
        }
    }

    private func stopPulseAnimation() {
        withAnimation(.easeOut(duration: 0.2)) {
            pulseScale = 1.0
        }
    }
}

// MARK: - FAB Container for positioning
struct FloatingActionButtonContainer: View {
    let action: () -> Void
    let isPulsing: Bool
    let bottomPadding: CGFloat

    init(
        action: @escaping () -> Void,
        isPulsing: Bool = false,
        bottomPadding: CGFloat = 83 // Default tab bar height
    ) {
        self.action = action
        self.isPulsing = isPulsing
        self.bottomPadding = bottomPadding
    }

    var body: some View {
        VStack {
            Spacer()
            HStack {
                Spacer()
                FloatingActionButton(
                    action: action,
                    isPulsing: isPulsing
                )
                .padding(.trailing, 16)
                .padding(.bottom, bottomPadding + 16) // Tab bar height + padding
            }
        }
    }
}

// MARK: - Preview
#Preview("FAB Static") {
    ZStack {
        Color.gray.opacity(0.1)
            .ignoresSafeArea()

        FloatingActionButtonContainer(
            action: {
                print("FAB tapped")
            },
            isPulsing: false
        )
    }
}

#Preview("FAB Pulsing") {
    ZStack {
        Color.gray.opacity(0.1)
            .ignoresSafeArea()

        FloatingActionButtonContainer(
            action: {
                print("FAB tapped")
            },
            isPulsing: true
        )
    }
}