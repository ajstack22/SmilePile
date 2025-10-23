import SwiftUI

struct PINSetupScreen: View {
    @ObservedObject var coordinator: OnboardingCoordinator
    @State private var pinCode = ""
    @State private var confirmPinCode = ""
    @State private var isConfirming = false
    @State private var showError = false
    @State private var errorMessage = ""
    @FocusState private var pinFieldFocused: Bool
    @Environment(\.typography) var typography
    @Environment(\.horizontalSizeClass) var horizontalSizeClass

    let pinLength = 4

    // Adaptive sizing for iPad
    private var isIPad: Bool {
        horizontalSizeClass == .regular
    }

    private var buttonSize: CGFloat {
        isIPad ? 90 : 70
    }

    private var buttonSpacing: CGFloat {
        isIPad ? 32 : 24
    }

    private var iconSize: CGFloat {
        isIPad ? 80 : 64
    }

    private var contentMaxWidth: CGFloat? {
        isIPad ? 500 : nil
    }

    var body: some View {
        VStack(spacing: 0) {
            // Instructions
            VStack(spacing: isIPad ? 20 : 16) {
                Image(systemName: "lock.fill")
                    .font(.system(size: iconSize))
                    .foregroundColor(.smilePileYellow)  // Yellow not pink
                    .padding(.bottom, isIPad ? 24 : 20)

                Text(isConfirming ? "Confirm Your PIN" : "Set Up PIN Protection")
                    .font(typography.headlineSmall)
                    .fontWeight(.bold)

                if isConfirming {
                    Text("Please enter your PIN again")
                        .font(typography.bodySmall)
                        .foregroundColor(.secondary)
                } else {
                    Text("Create a \(pinLength)-digit PIN to protect Parent Mode")
                        .font(typography.bodySmall)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, isIPad ? 48 : 40)
                }
            }
            .padding(.top, isIPad ? 48 : 40)

            Spacer()

            // PIN Input
            VStack(spacing: 30) {
                // PIN dots display
                HStack(spacing: 20) {
                    ForEach(0..<pinLength, id: \.self) { index in
                        Circle()
                            .fill(getPinDotColor(for: index))
                            .frame(width: 20, height: 20)
                            .overlay(
                                Circle()
                                    .stroke(Color.gray.opacity(0.3), lineWidth: 2)
                            )
                    }
                }

                // Hidden text field for PIN input
                TextField("", text: isConfirming ? $confirmPinCode : $pinCode)
                    .keyboardType(.numberPad)
                    .textFieldStyle(PlainTextFieldStyle())
                    .frame(width: 0, height: 0)
                    .opacity(0)
                    .focused($pinFieldFocused)
                    .onChange(of: isConfirming ? confirmPinCode : pinCode) { newValue in
                        handlePinInput(newValue)
                    }

                // Error message
                if showError {
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundColor(.red)
                        .transition(.opacity)
                }

                // Numeric keypad - centered on iPad
                HStack {
                    if isIPad { Spacer() }

                    VStack(spacing: isIPad ? 20 : 16) {
                        ForEach(0..<3) { row in
                            HStack(spacing: buttonSpacing) {
                                ForEach(1...3, id: \.self) { col in
                                    let number = row * 3 + col
                                    NumberButton(number: "\(number)", size: buttonSize) {
                                        addDigit("\(number)")
                                    }
                                }
                            }
                        }

                        HStack(spacing: buttonSpacing) {
                            // Clear button
                            Button(action: clearPin) {
                                Image(systemName: "delete.left")
                                    .font(isIPad ? .title : .title2)
                                    .foregroundColor(.gray)
                                    .frame(width: buttonSize, height: buttonSize)
                            }

                            // Zero button
                            NumberButton(number: "0", size: buttonSize) {
                                addDigit("0")
                            }

                            // Empty space
                            Color.clear
                                .frame(width: buttonSize, height: buttonSize)
                        }
                    }
                    .frame(maxWidth: contentMaxWidth)

                    if isIPad { Spacer() }
                }
            }

            Spacer()

            // Action buttons - horizontal layout at bottom - centered on iPad
            HStack {
                if isIPad { Spacer() }

                HStack(spacing: 16) {
                    // Skip button (only on first entry, not confirmation)
                    if !isConfirming {
                        Button(action: {
                            coordinator.onboardingData.skipPIN = true
                            coordinator.navigateToNext()
                        }) {
                            Text("Skip")
                                .font(typography.bodyLarge)
                                .fontWeight(.medium)
                                .foregroundColor(.secondary)
                                .frame(maxWidth: .infinity)
                                .frame(height: isIPad ? 64 : 56)
                                .background(Color.gray.opacity(0.1))
                                .cornerRadius(12)
                        }
                    }

                    // Continue/Confirm button (always visible)
                    Button(action: {
                        if isConfirming {
                            confirmPin()
                        } else {
                            if pinCode.count == pinLength {
                                proceedToConfirm()
                            }
                        }
                    }) {
                        Text(isConfirming ? "Confirm PIN" : "Continue")
                            .font(typography.bodyLarge)
                            .fontWeight(.medium)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: isIPad ? 64 : 56)
                            .background(
                                (isConfirming && confirmPinCode.count == pinLength) ||
                                (!isConfirming && pinCode.count == pinLength) ?
                                Color.smilePileBlue : Color.gray.opacity(0.3)
                            )
                            .cornerRadius(12)
                    }
                    .disabled(
                        (isConfirming && confirmPinCode.count != pinLength) ||
                        (!isConfirming && pinCode.count != pinLength)
                    )
                }
                .frame(maxWidth: contentMaxWidth)

                if isIPad { Spacer() }
            }
            .padding(.horizontal, isIPad ? 48 : 40)
            .padding(.bottom, isIPad ? 60 : 50)
        }
        .onAppear {
            pinFieldFocused = true
        }
    }

    private func getPinDotColor(for index: Int) -> Color {
        let currentPin = isConfirming ? confirmPinCode : pinCode
        return index < currentPin.count ?
            Color.smilePileYellow :  // Yellow not pink
            Color.gray.opacity(0.2)
    }

    private func handlePinInput(_ value: String) {
        // Limit to PIN length and numbers only
        let filtered = value.filter { $0.isNumber }.prefix(pinLength)

        if isConfirming {
            confirmPinCode = String(filtered)
        } else {
            pinCode = String(filtered)
        }
    }

    private func addDigit(_ digit: String) {
        if isConfirming {
            if confirmPinCode.count < pinLength {
                confirmPinCode += digit
            }
        } else {
            if pinCode.count < pinLength {
                pinCode += digit
            }
        }
    }

    private func clearPin() {
        if isConfirming {
            if !confirmPinCode.isEmpty {
                confirmPinCode.removeLast()
            }
        } else {
            if !pinCode.isEmpty {
                pinCode.removeLast()
            }
        }
    }

    private func proceedToConfirm() {
        isConfirming = true
        confirmPinCode = ""
        showError = false
    }

    private func confirmPin() {
        if pinCode == confirmPinCode {
            // PINs match, save and continue
            coordinator.onboardingData.pinCode = pinCode
            coordinator.onboardingData.skipPIN = false
            coordinator.navigateToNext()
        } else {
            // PINs don't match
            errorMessage = "PINs don't match. Please try again."
            showError = true
            confirmPinCode = ""

            // Hide error after 3 seconds
            DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                showError = false
            }
        }
    }
}

struct NumberButton: View {
    let number: String
    let size: CGFloat
    let action: () -> Void
    @Environment(\.typography) var typography

    var body: some View {
        Button(action: action) {
            Text(number)
                .font(typography.titleLarge)
                .fontWeight(.medium)
                .foregroundColor(.primary)
                .frame(width: size, height: size)
                .background(
                    Circle()
                        .fill(Color.gray.opacity(0.1))
                )
        }
    }
}