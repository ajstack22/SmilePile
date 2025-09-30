import SwiftUI

// MARK: - Material Design 3 Tab Bar for iOS
struct MaterialTabBar: View {
    @Binding var selection: Int
    @Namespace private var namespace
    @State private var tabPositions: [Int: CGRect] = [:]

    private let tabs: [(icon: String, selectedIcon: String, label: String)] = [
        ("photo.on.rectangle", "photo.on.rectangle.fill", "Gallery"),
        ("square.stack", "square.stack", "Piles"),
        ("gearshape", "gearshape.fill", "Settings")
    ]

    var body: some View {
        ZStack {
            // Background
            Rectangle()
                .fill(Color(UIColor.secondarySystemBackground))
                .shadow(color: Color.black.opacity(0.1), radius: 1, x: 0, y: -1)

            // Tab Items
            HStack(spacing: 0) {
                ForEach(0..<tabs.count, id: \.self) { index in
                    TabBarItem(
                        icon: selection == index ? tabs[index].selectedIcon : tabs[index].icon,
                        label: tabs[index].label,
                        isSelected: selection == index,
                        namespace: namespace,
                        onTap: {
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                                selection = index
                            }
                            // Haptic feedback
                            let impactFeedback = UIImpactFeedbackGenerator(style: .light)
                            impactFeedback.impactOccurred()
                        }
                    )
                    .frame(maxWidth: .infinity)
                    .background(
                        GeometryReader { geometry in
                            Color.clear
                                .preference(
                                    key: TabPositionPreferenceKey.self,
                                    value: [index: geometry.frame(in: .named("tabbar"))]
                                )
                        }
                    )
                }
            }
            .padding(.top, 12)
            .padding(.bottom, 8)
        }
        .frame(height: 83)
        .coordinateSpace(name: "tabbar")
        .onPreferenceChange(TabPositionPreferenceKey.self) { preferences in
            for (index, frame) in preferences {
                tabPositions[index] = frame
            }
        }
    }
}

// MARK: - Tab Bar Item
struct TabBarItem: View {
    let icon: String
    let label: String
    let isSelected: Bool
    let namespace: Namespace.ID
    let onTap: () -> Void

    @State private var isPressed = false

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 24))
                    .foregroundColor(isSelected ? Color.smilePilePink : Color(UIColor.secondaryLabel))
                    .scaleEffect(isPressed ? 0.92 : (isSelected ? 1.15 : 1.0))
                    .animation(.easeOut(duration: 0.2), value: isSelected)
                    .animation(.easeOut(duration: 0.1), value: isPressed)

                Text(label)
                    .font(.system(size: 12, weight: isSelected ? .semibold : .regular))
                    .foregroundColor(isSelected ? Color.smilePilePink : Color(UIColor.secondaryLabel))
                    .scaleEffect(isPressed ? 0.92 : 1.0)
                    .animation(.easeOut(duration: 0.1), value: isPressed)
            }
            .frame(minWidth: 44, minHeight: 44) // iOS minimum touch target
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainButtonStyle())
        .onLongPressGesture(minimumDuration: 0, maximumDistance: .infinity) { pressing in
            isPressed = pressing
        } perform: {
            onTap()
        }
        .accessibilityLabel("\(label) tab")
        .accessibilityHint(isSelected ? "Selected" : "Double tap to select")
        .accessibilityAddTraits(isSelected ? [.isSelected] : [])
    }
}

// MARK: - Preference Key for Tab Positions
struct TabPositionPreferenceKey: PreferenceKey {
    static var defaultValue: [Int: CGRect] = [:]

    static func reduce(value: inout [Int: CGRect], nextValue: () -> [Int: CGRect]) {
        value.merge(nextValue(), uniquingKeysWith: { $1 })
    }
}

// MARK: - Parent Mode View with Material Tab Bar
struct MaterialParentModeView: View {
    @State private var selectedTab = 0
    @State private var navigationPaths: [NavigationPath] = [
        NavigationPath(),
        NavigationPath(),
        NavigationPath()
    ]

    var body: some View {
        ZStack(alignment: .bottom) {
            // Content
            Group {
                switch selectedTab {
                case 0:
                    NavigationStack(path: $navigationPaths[0]) {
                        PhotoGalleryView()
                    }
                case 1:
                    NavigationStack(path: $navigationPaths[1]) {
                        CategoryManagementView()
                    }
                case 2:
                    NavigationStack(path: $navigationPaths[2]) {
                        SettingsViewNative()
                    }
                default:
                    EmptyView()
                }
            }
            .transition(.asymmetric(
                insertion: .move(edge: .trailing).combined(with: .opacity),
                removal: .move(edge: .leading).combined(with: .opacity)
            ))

            // Custom Material Tab Bar
            VStack(spacing: 0) {
                Divider()
                MaterialTabBar(selection: $selectedTab)
                    .background(Color(UIColor.systemBackground))
            }
        }
        .ignoresSafeArea(.keyboard) // Keep tab bar visible when keyboard appears
    }
}

// MARK: - Color Extension
// Color extension removed - using the one from Photo.swift