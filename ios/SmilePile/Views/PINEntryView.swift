import SwiftUI
import UIKit

struct PINEntryView: View {
    @Binding var isPresented: Bool
    let mode: PINEntryMode
    var pinLength: Int = 4
    let onSuccess: (String) -> Void
    let onCancel: () -> Void

    @StateObject private var viewModel = PINEntryViewModel()
    @State private var enteredPIN = ""
    @State private var confirmPIN = ""
    @State private var isConfirming = false
    @State private var showError = false
    @State private var errorMessage = ""
    @State private var cooldownRemaining: Int = 0
    @State private var timer: Timer?
    @Environment(\.colorScheme) var colorScheme
    @Environment(\.typography) var typography

    enum PINEntryMode {
        case setup
        case validate
        case change
    }

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Header section with consistent styling
                VStack(spacing: 20) {
                    // Icon container with background
                    ZStack {
                        Circle()
                            .fill(Color.smilePileYellow.opacity(0.1))
                            .frame(width: 80, height: 80)

                        Image(systemName: mode == .validate ? "lock.fill" : "lock.shield.fill")
                            .font(.system(size: 36))
                            .foregroundColor(Color.smilePileYellow)
                    }
                    .padding(.top, 32)

                    // Title and subtitle
                    VStack(spacing: 8) {
                        Text(headerTitle)
                            .font(typography.headlineSmall)
                            .foregroundColor(.primary)

                        Text(headerSubtitle)
                            .font(typography.bodyMedium)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 32)

                        if cooldownRemaining > 0 {
                            Text("Please wait \(cooldownRemaining) seconds")
                                .foregroundColor(Color.smilePileOrange)
                                .font(typography.labelLarge)
                                .padding(.top, 8)
                        }
                    }

                    // PIN Dots with improved design
                    HStack(spacing: 16) {
                        ForEach(0..<pinLength) { index in
                            Circle()
                                .fill(pinDotColor(at: index))
                                .frame(width: 18, height: 18)
                                .overlay(
                                    Circle()
                                        .stroke(
                                            currentPIN.count > index
                                                ? Color.smilePileYellow.opacity(0.3)
                                                : Color.gray.opacity(0.2),
                                            lineWidth: 2
                                        )
                                )
                                .scaleEffect(currentPIN.count > index ? 1.1 : 1.0)
                                .animation(.easeInOut(duration: 0.15), value: currentPIN.count)
                        }
                    }
                    .padding(.vertical, 24)

                    // Error Message
                    if showError {
                        HStack(spacing: 8) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .font(typography.bodyMedium)
                                .foregroundColor(Color.smilePileOrange)

                            Text(errorMessage)
                                .foregroundColor(Color.smilePileOrange)
                                .font(typography.bodyMedium)
                        }
                        .padding(.horizontal, 20)
                        .padding(.vertical, 12)
                        .background(
                            RoundedRectangle(cornerRadius: 8)
                                .fill(Color.smilePileOrange.opacity(0.1))
                        )
                        .transition(.opacity.combined(with: .scale))
                    }
                }

                Spacer()

                // Number Pad with improved design
                VStack(spacing: 20) {
                    ForEach(0..<3) { row in
                        HStack(spacing: 24) {
                            ForEach(1...3, id: \.self) { col in
                                let number = row * 3 + col
                                PINNumberButton(number: "\(number)") {
                                    if cooldownRemaining == 0 {
                                        addDigit("\(number)")
                                        provideHapticFeedback()
                                    }
                                }
                            }
                        }
                    }

                    HStack(spacing: 24) {
                        // Empty space for layout
                        Color.clear
                            .frame(width: 72, height: 72)

                        PINNumberButton(number: "0") {
                            if cooldownRemaining == 0 {
                                addDigit("0")
                                provideHapticFeedback()
                            }
                        }

                        // Backspace with consistent design
                        Button(action: {
                            removeDigit()
                            provideHapticFeedback()
                        }) {
                            ZStack {
                                Circle()
                                    .fill(Color.gray.opacity(0.08))
                                    .frame(width: 72, height: 72)

                                Image(systemName: "delete.left.fill")
                                    .font(.system(size: 24))
                                    .foregroundColor(.secondary)
                            }
                        }
                        .buttonStyle(ScaleButtonStyle())
                    }
                }
                .padding(.horizontal, 40)
                .padding(.bottom, 40)
                .disabled(cooldownRemaining > 0)
                .opacity(cooldownRemaining > 0 ? 0.5 : 1.0)
            }
            .background(Color(UIColor.systemBackground))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: {
                        onCancel()
                        isPresented = false
                    }) {
                        Text("Cancel")
                            .foregroundColor(Color.smilePileYellow)
                            .font(typography.bodyLarge)
                    }
                }
            }
        }
        .onAppear {
            startCooldownTimerIfNeeded()
        }
        .onDisappear {
            timer?.invalidate()
        }
    }

    private var headerTitle: String {
        switch mode {
        case .setup:
            return isConfirming ? "Confirm your PIN" : "Create a 4-digit PIN"
        case .validate:
            return "Enter your PIN"
        case .change:
            return isConfirming ? "Enter new PIN again" : "Enter new PIN"
        }
    }

    private var headerSubtitle: String {
        switch mode {
        case .setup:
            return isConfirming ? "Re-enter the PIN to confirm" : "This PIN will protect Parent Mode"
        case .validate:
            return "Verify your identity to continue"
        case .change:
            return isConfirming ? "Re-enter the new PIN to confirm" : "Choose a new 4-digit PIN"
        }
    }

    private var currentPIN: String {
        isConfirming ? confirmPIN : enteredPIN
    }

    private func pinDotColor(at index: Int) -> Color {
        if currentPIN.count > index {
            return Color.smilePileYellow
        }
        return Color.gray.opacity(0.15)
    }

    private func provideHapticFeedback() {
        let impactFeedback = UIImpactFeedbackGenerator(style: .light)
        impactFeedback.impactOccurred()
    }

    private func addDigit(_ digit: String) {
        guard currentPIN.count < pinLength else { return }

        if isConfirming {
            confirmPIN.append(digit)
            if confirmPIN.count == pinLength {
                validateConfirmation()
            }
        } else {
            enteredPIN.append(digit)
            if enteredPIN.count == pinLength {
                handlePINEntry()
            }
        }
    }

    private func removeDigit() {
        if isConfirming && !confirmPIN.isEmpty {
            confirmPIN.removeLast()
        } else if !enteredPIN.isEmpty {
            enteredPIN.removeLast()
        }
    }

    private func handlePINEntry() {
        switch mode {
        case .setup, .change:
            isConfirming = true
        case .validate:
            if viewModel.validatePIN(enteredPIN) {
                onSuccess(enteredPIN)
                isPresented = false
            } else {
                showInvalidPINError()
                enteredPIN = ""
                startCooldownTimerIfNeeded()
            }
        }
    }

    private func validateConfirmation() {
        if enteredPIN == confirmPIN {
            do {
                try viewModel.setPIN(enteredPIN)
                onSuccess(enteredPIN)
                isPresented = false
            } catch {
                showError(message: error.localizedDescription)
                resetPINEntry()
            }
        } else {
            showError(message: "PINs don't match. Please try again.")
            resetPINEntry()
        }
    }

    private func resetPINEntry() {
        enteredPIN = ""
        confirmPIN = ""
        isConfirming = false
    }

    private func showError(message: String) {
        errorMessage = message
        showError = true

        DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
            showError = false
        }
    }

    private func showInvalidPINError() {
        let attempts = viewModel.getFailedAttempts()
        let remaining = 5 - attempts

        if remaining > 0 {
            showError(message: "Incorrect PIN. \(remaining) attempts remaining.")
        } else {
            showError(message: "Too many attempts. Please wait.")
        }
    }

    private func startCooldownTimerIfNeeded() {
        let remaining = viewModel.getRemainingCooldownTime()
        if remaining > 0 {
            cooldownRemaining = Int(remaining)
            timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
                cooldownRemaining = Int(viewModel.getRemainingCooldownTime())
                if cooldownRemaining == 0 {
                    timer?.invalidate()
                }
            }
        }
    }
}

private struct PINNumberButton: View {
    let number: String
    let action: () -> Void
    @Environment(\.colorScheme) var colorScheme
    @Environment(\.typography) var typography

    var body: some View {
        Button(action: action) {
            ZStack {
                Circle()
                    .fill(colorScheme == .dark
                        ? Color.gray.opacity(0.15)
                        : Color.gray.opacity(0.08))
                    .frame(width: 72, height: 72)

                Text(number)
                    .font(typography.headlineMedium)
                    .foregroundColor(.primary)
            }
        }
        .buttonStyle(ScaleButtonStyle())
    }
}

struct ScaleButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.95 : 1)
            .animation(.easeInOut(duration: 0.1), value: configuration.isPressed)
    }
}