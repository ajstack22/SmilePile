import SwiftUI

struct PINSetupScreen: View {
    @ObservedObject var coordinator: OnboardingCoordinator
    @State private var pinCode = ""
    @State private var confirmPinCode = ""
    @State private var isConfirming = false
    @State private var showError = false
    @State private var errorMessage = ""
    @FocusState private var pinFieldFocused: Bool

    let pinLength = 4

    var body: some View {
        VStack(spacing: 0) {
            // Instructions
            VStack(spacing: 8) {
                Image(systemName: "lock.fill")
                    .font(.system(size: 60))
                    .foregroundColor(Color(red: 1.0, green: 0.42, blue: 0.42))
                    .padding(.bottom, 20)

                Text(isConfirming ? "Confirm Your PIN" : "Set Up PIN Protection")
                    .font(.title2)
                    .fontWeight(.bold)

                Text(isConfirming ?
                     "Please enter your PIN again" :
                     "Create a \(pinLength)-digit PIN to protect Parent Mode")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
            }
            .padding(.top, 40)

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

                // Numeric keypad
                VStack(spacing: 20) {
                    ForEach(0..<3) { row in
                        HStack(spacing: 40) {
                            ForEach(1...3, id: \.self) { col in
                                let number = row * 3 + col
                                NumberButton(number: "\(number)") {
                                    addDigit("\(number)")
                                }
                            }
                        }
                    }

                    HStack(spacing: 40) {
                        // Clear button
                        Button(action: clearPin) {
                            Image(systemName: "delete.left")
                                .font(.title2)
                                .foregroundColor(.gray)
                                .frame(width: 70, height: 70)
                        }

                        // Zero button
                        NumberButton(number: "0") {
                            addDigit("0")
                        }

                        // Empty space
                        Color.clear
                            .frame(width: 70, height: 70)
                    }
                }
            }

            Spacer()

            // Action buttons
            VStack(spacing: 12) {
                if !isConfirming {
                    // Skip option
                    Button(action: {
                        coordinator.onboardingData.skipPIN = true
                        coordinator.navigateToNext()
                    }) {
                        Text("Skip for Now")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .padding(.bottom, 8)
                }

                // Continue button (only shown when PIN is entered)
                if (isConfirming && confirmPinCode.count == pinLength) ||
                   (!isConfirming && pinCode.count == pinLength) {
                    Button(action: {
                        if isConfirming {
                            confirmPin()
                        } else {
                            proceedToConfirm()
                        }
                    }) {
                        Text(isConfirming ? "Confirm PIN" : "Continue")
                            .font(.headline)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color(red: 1.0, green: 0.42, blue: 0.42))
                            .cornerRadius(12)
                    }
                    .transition(.opacity)
                }
            }
            .padding()
        }
        .onAppear {
            pinFieldFocused = true
        }
    }

    private func getPinDotColor(for index: Int) -> Color {
        let currentPin = isConfirming ? confirmPinCode : pinCode
        return index < currentPin.count ?
            Color(red: 1.0, green: 0.42, blue: 0.42) :
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
        }
    }
}