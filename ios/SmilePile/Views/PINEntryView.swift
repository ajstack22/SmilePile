import SwiftUI

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

    enum PINEntryMode {
        case setup
        case validate
        case change
    }

    var body: some View {
        NavigationView {
            VStack(spacing: 30) {
                // Header
                VStack(spacing: 12) {
                    Image(systemName: "lock.circle.fill")
                        .font(.system(size: 60))
                        .foregroundColor(Color(red: 1.0, green: 0.792, blue: 0.157))

                    Text(headerTitle)
                        .font(.title2)
                        .fontWeight(.semibold)

                    if cooldownRemaining > 0 {
                        Text("Please wait \(cooldownRemaining) seconds")
                            .foregroundColor(.red)
                            .font(.subheadline)
                    }
                }
                .padding(.top, 20)

                // PIN Dots
                HStack(spacing: 20) {
                    ForEach(0..<pinLength) { index in
                        Circle()
                            .fill(pinDotColor(at: index))
                            .frame(width: 16, height: 16)
                            .overlay(
                                Circle()
                                    .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                            )
                            .scaleEffect(currentPIN.count > index ? 1.2 : 1.0)
                            .animation(.spring(response: 0.3), value: currentPIN.count)
                    }
                }
                .padding(.vertical, 20)

                // Error Message
                if showError {
                    Text(errorMessage)
                        .foregroundColor(.red)
                        .font(.caption)
                        .transition(.opacity)
                }

                Spacer()

                // Number Pad
                VStack(spacing: 16) {
                    ForEach(0..<3) { row in
                        HStack(spacing: 30) {
                            ForEach(1...3, id: \.self) { col in
                                let number = row * 3 + col
                                NumberButton(number: "\(number)") {
                                    if cooldownRemaining == 0 {
                                        addDigit("\(number)")
                                    }
                                }
                            }
                        }
                    }

                    HStack(spacing: 30) {
                        // Empty space
                        Color.clear
                            .frame(width: 70, height: 70)

                        NumberButton(number: "0") {
                            if cooldownRemaining == 0 {
                                addDigit("0")
                            }
                        }

                        // Backspace
                        Button(action: removeDigit) {
                            Image(systemName: "delete.left.fill")
                                .font(.title2)
                                .foregroundColor(.gray)
                                .frame(width: 70, height: 70)
                        }
                    }
                }
                .padding(.bottom, 30)
                .disabled(cooldownRemaining > 0)
            }
            .padding(.horizontal)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        onCancel()
                        isPresented = false
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

    private var currentPIN: String {
        isConfirming ? confirmPIN : enteredPIN
    }

    private func pinDotColor(at index: Int) -> Color {
        if currentPIN.count > index {
            return Color(red: 1.0, green: 0.792, blue: 0.157)
        }
        return Color.gray.opacity(0.2)
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

struct NumberButton: View {
    let number: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(number)
                .font(.title)
                .fontWeight(.medium)
                .foregroundColor(.primary)
                .frame(width: 70, height: 70)
                .background(
                    Circle()
                        .fill(Color.gray.opacity(0.1))
                )
                .overlay(
                    Circle()
                        .stroke(Color.gray.opacity(0.2), lineWidth: 1)
                )
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