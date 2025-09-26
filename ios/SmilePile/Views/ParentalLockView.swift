import SwiftUI
import LocalAuthentication

struct ParentalLockView: View {
    @Binding var isPresented: Bool
    let onUnlocked: () -> Void
    let onCancel: () -> Void

    @StateObject private var viewModel = ParentalLockViewModel()
    @State private var showPINEntry = false
    @State private var showPatternEntry = false
    @State private var showBiometricOption = false
    @State private var unlockAnimationScale: CGFloat = 1.0

    var body: some View {
        NavigationView {
            VStack(spacing: 30) {
                // Lock Icon
                Image(systemName: viewModel.isUnlocked ? "lock.open.fill" : "lock.fill")
                    .font(.system(size: 80))
                    .foregroundColor(Color(red: 1.0, green: 0.792, blue: 0.157))
                    .scaleEffect(unlockAnimationScale)
                    .animation(.spring(), value: viewModel.isUnlocked)

                // Title
                Text("Parental Controls")
                    .font(.largeTitle)
                    .fontWeight(.bold)

                Text("Authentication required to access settings")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)

                Spacer()

                // Authentication Options
                VStack(spacing: 20) {
                    // PIN Entry Button (if PIN is set)
                    if viewModel.isPINSet {
                        Button(action: {
                            showPINEntry = true
                        }) {
                            HStack {
                                Image(systemName: "number.square.fill")
                                    .font(.title2)
                                Text("Enter PIN")
                                    .fontWeight(.semibold)
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color(red: 1.0, green: 0.792, blue: 0.157))
                            .foregroundColor(.white)
                            .cornerRadius(12)
                        }
                    }

                    // Pattern Entry Button (if Pattern is set)
                    if viewModel.hasPattern {
                        Button(action: {
                            showPatternEntry = true
                        }) {
                            HStack {
                                Image(systemName: "circle.grid.3x3.fill")
                                    .font(.title2)
                                Text("Enter Pattern")
                                    .fontWeight(.semibold)
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.purple)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                        }
                    }

                    // Biometric Button (if available and enabled)
                    if viewModel.isBiometricAvailable && viewModel.isBiometricEnabled {
                        Button(action: {
                            authenticateWithBiometric()
                        }) {
                            HStack {
                                Image(systemName: biometricIcon)
                                    .font(.title2)
                                Text(biometricText)
                                    .fontWeight(.semibold)
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                        }
                    }

                    // Setup Security (if nothing is set)
                    if !viewModel.isPINSet && !viewModel.hasPattern {
                        VStack(spacing: 12) {
                            Text("No security configured")
                                .font(.caption)
                                .foregroundColor(.secondary)

                            Button(action: {
                                showPINEntry = true
                            }) {
                                HStack {
                                    Image(systemName: "plus.circle.fill")
                                        .font(.title2)
                                    Text("Set Up PIN")
                                        .fontWeight(.semibold)
                                }
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.green)
                                .foregroundColor(.white)
                                .cornerRadius(12)
                            }

                            Button(action: {
                                showPatternEntry = true
                            }) {
                                HStack {
                                    Image(systemName: "circle.grid.3x3")
                                        .font(.title2)
                                    Text("Set Up Pattern")
                                        .fontWeight(.semibold)
                                }
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.purple.opacity(0.9))
                                .foregroundColor(.white)
                                .cornerRadius(12)
                            }
                        }
                    }
                }
                .padding(.horizontal)

                Spacer()

                // Failed Attempts Warning
                if viewModel.attemptsRemaining < 3 && viewModel.attemptsRemaining > 0 {
                    Text("⚠️ \(viewModel.attemptsRemaining) attempts remaining")
                        .foregroundColor(.orange)
                        .font(.caption)
                }

                // Cooldown Timer
                if viewModel.isInCooldown {
                    VStack(spacing: 8) {
                        ProgressView()
                        Text("Too many failed attempts")
                            .font(.caption)
                            .foregroundColor(.red)
                        Text("Please wait \(Int(viewModel.cooldownTimeRemaining)) seconds")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                    .padding()
                    .background(Color.red.opacity(0.1))
                    .cornerRadius(10)
                }
            }
            .padding()
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
        .sheet(isPresented: $showPINEntry) {
            PINEntryView(
                isPresented: $showPINEntry,
                mode: viewModel.isPINSet ? .validate : .setup,
                onSuccess: { _ in
                    handleUnlock()
                },
                onCancel: {
                    // Just dismiss
                }
            )
        }
        .sheet(isPresented: $showPatternEntry) {
            NavigationView {
                PatternLockView(
                    mode: viewModel.hasPattern ? .validate : .setup,
                    onSuccess: {
                        handleUnlock()
                    },
                    onCancel: {
                        showPatternEntry = false
                    }
                )
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button("Cancel") {
                            showPatternEntry = false
                        }
                    }
                }
            }
        }
        .onAppear {
            checkInitialAuth()
        }
        .onChange(of: viewModel.isUnlocked) { unlocked in
            if unlocked {
                handleUnlock()
            }
        }
    }

    private var biometricIcon: String {
        switch viewModel.biometricType {
        case .faceID:
            return "faceid"
        case .touchID:
            return "touchid"
        default:
            return "lock.shield"
        }
    }

    private var biometricText: String {
        switch viewModel.biometricType {
        case .faceID:
            return "Use Face ID"
        case .touchID:
            return "Use Touch ID"
        default:
            return "Use Biometric"
        }
    }

    private func checkInitialAuth() {
        // If no security is set, allow access to set it up
        if !viewModel.isPINSet && !viewModel.hasPattern {
            handleUnlock()
        } else if viewModel.isBiometricEnabled && viewModel.isBiometricAvailable {
            // Auto-prompt for biometric if enabled
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                authenticateWithBiometric()
            }
        }
    }

    private func authenticateWithBiometric() {
        viewModel.authenticateWithBiometric { success, error in
            if success {
                handleUnlock()
            } else if let error = error {
                // Fall back to PIN
                showPINEntry = true
            }
        }
    }

    private func handleUnlock() {
        unlockAnimationScale = 1.2
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
            unlockAnimationScale = 1.0
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            onUnlocked()
            isPresented = false
        }
    }
}