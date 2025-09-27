import SwiftUI
import Combine

// MARK: - Toast Type
enum ToastType {
    case success
    case error
    case info
    case category(name: String, color: Color)

    var icon: String {
        switch self {
        case .success:
            return "checkmark.circle.fill"
        case .error:
            return "xmark.circle.fill"
        case .info:
            return "info.circle.fill"
        case .category:
            return "folder.fill"
        }
    }

    var iconColor: Color {
        switch self {
        case .success:
            return Color.green
        case .error:
            return Color.red
        case .info:
            return Color.blue
        case .category(_, let color):
            return color
        }
    }
}

// MARK: - Toast Position
enum ToastPosition {
    case top
    case bottom
    case categoryTop // Special position for category toasts

    var alignment: Alignment {
        switch self {
        case .top, .categoryTop:
            return .top
        case .bottom:
            return .bottom
        }
    }

    var offset: CGFloat {
        switch self {
        case .top:
            return 50
        case .categoryTop:
            return 80 // Android spec: 80dp from top for category toasts
        case .bottom:
            return 100 // Android spec: 100dp from bottom for standard toasts
        }
    }
}

// MARK: - Toast Item
struct ToastItem: Identifiable, Equatable {
    let id = UUID()
    let message: String
    let type: ToastType
    let position: ToastPosition
    let duration: TimeInterval
    let showIcon: Bool

    static func == (lhs: ToastItem, rhs: ToastItem) -> Bool {
        lhs.id == rhs.id
    }

    init(
        message: String,
        type: ToastType = .info,
        position: ToastPosition? = nil,
        duration: TimeInterval = 3.0,
        showIcon: Bool = true
    ) {
        self.message = message
        self.type = type
        // Category toasts go to top, others to bottom by default
        self.position = position ?? (type.isCategoryToast ? .categoryTop : .bottom)
        self.duration = duration
        self.showIcon = showIcon
    }
}

extension ToastType {
    var isCategoryToast: Bool {
        if case .category = self {
            return true
        }
        return false
    }
}

// MARK: - Toast Manager
class ToastManager: ObservableObject {
    static let shared = ToastManager()

    @Published var currentToast: ToastItem?
    private var toastQueue: [ToastItem] = []
    private var cancellable: AnyCancellable?
    private var isAnimating = false

    // Toast duration constants matching Android
    static let TOAST_DURATION_SHORT: TimeInterval = 2.0  // 2000ms in Android
    static let TOAST_DURATION_DEFAULT: TimeInterval = 3.0  // 3000ms in Android
    static let TOAST_DURATION_LONG: TimeInterval = 5.0  // 5000ms in Android

    private init() {}

    func show(
        _ message: String,
        type: ToastType = .info,
        position: ToastPosition? = nil,
        duration: TimeInterval = ToastManager.TOAST_DURATION_DEFAULT,
        showIcon: Bool = true
    ) {
        let toast = ToastItem(
            message: message,
            type: type,
            position: position,
            duration: duration,
            showIcon: showIcon
        )

        // FIFO queue management - if no toast is showing, show immediately
        if currentToast == nil && !isAnimating {
            showToast(toast)
        } else {
            // Queue the toast (FIFO)
            toastQueue.append(toast)
        }
    }

    func showCategoryToast(_ category: Category) {
        let color = Color(hex: category.colorHex ?? "#4CAF50") ?? Color.green
        show(
            category.displayName,
            type: .category(name: category.displayName, color: color),
            position: .categoryTop,
            duration: ToastManager.TOAST_DURATION_SHORT  // 2-second for category toasts matching Android
        )
    }

    private func showToast(_ toast: ToastItem) {
        isAnimating = true
        withAnimation(.easeInOut(duration: 0.3)) {
            currentToast = toast
        }

        // Auto-dismiss after duration
        cancellable?.cancel()
        cancellable = Timer.publish(every: toast.duration, on: .main, in: .common)
            .autoconnect()
            .first()
            .sink { _ in
                self.dismiss()
            }
    }

    func dismiss() {
        isAnimating = true
        withAnimation(.easeInOut(duration: 0.3)) {
            currentToast = nil
        }

        // Show next toast in queue after animation completes (300ms animation + small buffer)
        if !toastQueue.isEmpty {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                self.isAnimating = false
                if let nextToast = self.toastQueue.first {
                    self.toastQueue.removeFirst()
                    self.showToast(nextToast)
                }
            }
        } else {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                self.isAnimating = false
            }
        }
    }

    // Quick methods for common toasts matching Android
    func showSuccess(_ message: String) {
        show(message, type: .success, duration: ToastManager.TOAST_DURATION_DEFAULT)
    }

    func showError(_ message: String) {
        show(message, type: .error, duration: ToastManager.TOAST_DURATION_LONG)
    }

    func showInfo(_ message: String) {
        show(message, type: .info, duration: ToastManager.TOAST_DURATION_DEFAULT)
    }

    func clearQueue() {
        toastQueue.removeAll()
        cancellable?.cancel()
        isAnimating = false
        currentToast = nil
    }
}

// MARK: - Toast View
struct ToastView: View {
    let toast: ToastItem
    @State private var isShowing = false
    @State private var dragOffset: CGSize = .zero

    private var isTopPosition: Bool {
        toast.position == .top || toast.position == .categoryTop
    }

    var body: some View {
        HStack(spacing: 12) {
            if toast.showIcon {
                Image(systemName: toast.type.icon)
                    .font(.system(size: 20))
                    .foregroundColor(toast.type.iconColor)
            }

            Text(toast.message)
                .font(.system(size: 16, weight: .medium))
                .foregroundColor(textColor)
                .lineLimit(2)

            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(backgroundView)
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.15), radius: 8, x: 0, y: 4)
        .padding(.horizontal, 16)
        .offset(y: dragOffset.height)
        .opacity(isShowing ? 1 : 0)
        .scaleEffect(isShowing ? 1 : 0.8)
        // Accessibility support
        .accessibilityElement(children: .combine)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityHint("Swipe to dismiss")
        .accessibilityAddTraits(.isStaticText)
        .accessibilityRemoveTraits(.isButton)
        .onAppear {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                isShowing = true
            }
            // Announce toast to VoiceOver users
            UIAccessibility.post(notification: .announcement, argument: toast.message)
        }
        .onDisappear {
            isShowing = false
        }
        .gesture(
            DragGesture()
                .onChanged { value in
                    // Allow dragging in dismiss direction
                    if isTopPosition && value.translation.height < 0 {
                        dragOffset = value.translation
                    } else if !isTopPosition && value.translation.height > 0 {
                        dragOffset = value.translation
                    }
                }
                .onEnded { value in
                    // Dismiss if dragged far enough
                    let threshold: CGFloat = 50
                    if abs(value.translation.height) > threshold {
                        ToastManager.shared.dismiss()
                    } else {
                        withAnimation(.spring()) {
                            dragOffset = .zero
                        }
                    }
                }
        )
    }

    @ViewBuilder
    private var backgroundView: some View {
        if case .category(_, let color) = toast.type {
            // Category toast with colored background matching Android's CategoryToastUI
            color.opacity(0.95)
        } else {
            // Standard toast background
            Color(UIColor.systemBackground)
                .opacity(0.95)
        }
    }

    // Calculate text color based on background luminance (matching Android)
    private var textColor: Color {
        if case .category(_, let color) = toast.type {
            // Calculate luminance using the same formula as Android
            // luminance = 0.299*R + 0.587*G + 0.114*B
            let uiColor = UIColor(color)
            var red: CGFloat = 0
            var green: CGFloat = 0
            var blue: CGFloat = 0
            var alpha: CGFloat = 0

            uiColor.getRed(&red, green: &green, blue: &blue, alpha: &alpha)

            let luminance = 0.299 * red + 0.587 * green + 0.114 * blue

            // Use white text on dark backgrounds, black on light
            return luminance > 0.5 ? Color.black : Color.white
        } else {
            return Color(UIColor.label)
        }
    }

    // Accessibility label for VoiceOver
    private var accessibilityLabel: String {
        switch toast.type {
        case .success:
            return "Success: \(toast.message)"
        case .error:
            return "Error: \(toast.message)"
        case .info:
            return "Information: \(toast.message)"
        case .category:
            return "Category selected: \(toast.message)"
        }
    }
}

// MARK: - Toast Overlay Modifier
struct ToastOverlay: ViewModifier {
    @StateObject private var toastManager = ToastManager.shared

    func body(content: Content) -> some View {
        content
            .overlay(alignment: toastManager.currentToast?.position.alignment ?? .bottom) {
                if let toast = toastManager.currentToast {
                    ToastView(toast: toast)
                        .transition(
                            .asymmetric(
                                insertion: .move(edge: toast.position == .bottom ? .bottom : .top)
                                    .combined(with: .opacity),
                                removal: .move(edge: toast.position == .bottom ? .bottom : .top)
                                    .combined(with: .opacity)
                            )
                        )
                        .padding(
                            toast.position == .bottom ? .bottom : .top,
                            toast.position.offset
                        )
                        .animation(.spring(response: 0.3, dampingFraction: 0.8), value: toastManager.currentToast)
                }
            }
    }
}

// MARK: - View Extension
extension View {
    func toastOverlay() -> some View {
        modifier(ToastOverlay())
    }
}

// Color extension removed - using existing extension from Photo.swift

// MARK: - Usage Examples
/*
 // Show standard toasts
 ToastManager.shared.show("Photo imported successfully", type: .success)
 ToastManager.shared.show("Failed to delete photo", type: .error)
 ToastManager.shared.show("3 photos selected", type: .info)

 // Show category toast (appears at top)
 ToastManager.shared.showCategoryToast(category)

 // In your main app view, add the overlay:
 ContentView()
     .toastOverlay()
 */