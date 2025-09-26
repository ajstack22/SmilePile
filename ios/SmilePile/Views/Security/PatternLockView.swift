import SwiftUI

enum PatternMode {
    case setup
    case confirm
    case validate
    case change
}

struct PatternLockView: View {
    @StateObject private var viewModel = PatternLockViewModel()
    @State private var selectedDots: [Int] = []
    @State private var currentPath = Path()
    @State private var showError = false
    @State private var errorMessage = ""

    let mode: PatternMode
    let onSuccess: () -> Void
    let onCancel: (() -> Void)?

    init(mode: PatternMode, onSuccess: @escaping () -> Void, onCancel: (() -> Void)? = nil) {
        self.mode = mode
        self.onSuccess = onSuccess
        self.onCancel = onCancel
    }

    var body: some View {
        VStack(spacing: 30) {
            headerView

            if viewModel.isInCooldown {
                cooldownView
            } else {
                PatternGridView(
                    selectedDots: $selectedDots,
                    currentPath: $currentPath,
                    isDisabled: viewModel.isInCooldown,
                    onPatternComplete: handlePatternComplete
                )
                .padding()
            }

            if showError {
                Text(errorMessage)
                    .foregroundColor(.red)
                    .font(.caption)
                    .padding()
                    .transition(.opacity)
            }

            if mode == .validate {
                attemptsView
            }

            actionButtons
        }
        .padding()
        .onAppear {
            viewModel.mode = mode
        }
    }

    private var headerView: some View {
        VStack(spacing: 10) {
            Image(systemName: "lock.shield")
                .font(.system(size: 50))
                .foregroundColor(.blue)

            Text(headerTitle)
                .font(.title2)
                .fontWeight(.bold)

            Text(headerSubtitle)
                .font(.caption)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
    }

    private var headerTitle: String {
        switch mode {
        case .setup:
            return viewModel.firstPattern.isEmpty ? "Create Pattern" : "Confirm Pattern"
        case .confirm:
            return "Confirm Pattern"
        case .validate:
            return "Enter Pattern"
        case .change:
            return viewModel.isChangingPattern ? "Enter New Pattern" : "Enter Current Pattern"
        }
    }

    private var headerSubtitle: String {
        switch mode {
        case .setup:
            return viewModel.firstPattern.isEmpty ?
                "Draw a pattern with at least 4 dots" :
                "Draw the pattern again to confirm"
        case .confirm:
            return "Draw the pattern again to confirm"
        case .validate:
            return "Draw your pattern to continue"
        case .change:
            return viewModel.isChangingPattern ?
                "Draw a new pattern with at least 4 dots" :
                "Enter your current pattern first"
        }
    }

    private var cooldownView: some View {
        VStack(spacing: 20) {
            Image(systemName: "timer")
                .font(.system(size: 60))
                .foregroundColor(.orange)

            Text("Too Many Attempts")
                .font(.headline)

            Text("Please wait \(Int(viewModel.remainingCooldownTime)) seconds")
                .font(.subheadline)
                .foregroundColor(.secondary)

            ProgressView()
                .progressViewStyle(CircularProgressViewStyle())
        }
        .padding()
    }

    private var attemptsView: some View {
        HStack {
            Image(systemName: "exclamationmark.triangle")
                .foregroundColor(.orange)
            Text("\(viewModel.remainingAttempts) attempts remaining")
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }

    private var actionButtons: some View {
        HStack(spacing: 20) {
            if let onCancel = onCancel {
                Button("Cancel") {
                    onCancel()
                }
                .buttonStyle(.bordered)
            }

            if mode == .validate && viewModel.canUseBiometric {
                Button("Use Biometric") {
                    viewModel.authenticateWithBiometric { success in
                        if success {
                            onSuccess()
                        }
                    }
                }
                .buttonStyle(.borderedProminent)
            }
        }
    }

    private func handlePatternComplete(_ pattern: [Int]) {
        switch mode {
        case .setup:
            handleSetupPattern(pattern)
        case .confirm:
            handleConfirmPattern(pattern)
        case .validate:
            handleValidatePattern(pattern)
        case .change:
            handleChangePattern(pattern)
        }
    }

    private func handleSetupPattern(_ pattern: [Int]) {
        if viewModel.firstPattern.isEmpty {
            if pattern.count < 4 {
                showErrorMessage("Pattern must have at least 4 dots")
                return
            }
            viewModel.firstPattern = pattern
            clearPattern()
        } else {
            if pattern == viewModel.firstPattern {
                if viewModel.savePattern(pattern) {
                    onSuccess()
                } else {
                    showErrorMessage("Failed to save pattern")
                }
            } else {
                showErrorMessage("Patterns don't match. Try again.")
                viewModel.firstPattern = []
                clearPattern()
            }
        }
    }

    private func handleConfirmPattern(_ pattern: [Int]) {
        if pattern == viewModel.firstPattern {
            if viewModel.savePattern(pattern) {
                onSuccess()
            } else {
                showErrorMessage("Failed to save pattern")
            }
        } else {
            showErrorMessage("Patterns don't match. Try again.")
        }
    }

    private func handleValidatePattern(_ pattern: [Int]) {
        if viewModel.validatePattern(pattern) {
            onSuccess()
        } else {
            showErrorMessage("Incorrect pattern")
            clearPattern()
        }
    }

    private func handleChangePattern(_ pattern: [Int]) {
        if !viewModel.isChangingPattern {
            if viewModel.validatePattern(pattern) {
                viewModel.isChangingPattern = true
                clearPattern()
            } else {
                showErrorMessage("Incorrect pattern")
                clearPattern()
            }
        } else {
            handleSetupPattern(pattern)
        }
    }

    private func showErrorMessage(_ message: String) {
        errorMessage = message
        withAnimation {
            showError = true
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            withAnimation {
                showError = false
            }
        }
    }

    private func clearPattern() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            selectedDots.removeAll()
            currentPath = Path()
        }
    }
}

struct PatternLockView_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            PatternLockView(mode: .setup) {
                print("Pattern setup complete")
            }
            .previewDisplayName("Setup Mode")

            PatternLockView(mode: .validate) {
                print("Pattern validated")
            } onCancel: {
                print("Cancelled")
            }
            .previewDisplayName("Validate Mode")
        }
    }
}