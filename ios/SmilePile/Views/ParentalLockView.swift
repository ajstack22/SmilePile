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
                lockIconAndTitle
                Spacer()
                authenticationOptions
                Spacer()
                warningsAndStatus
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
            pinEntrySheet
        }
        .sheet(isPresented: $showPatternEntry) {
            patternEntrySheet
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

    // MARK: - Extracted Views

    private var lockIconAndTitle: some View {
        VStack(spacing: 16) {
            Image(systemName: viewModel.isUnlocked ? "lock.open.fill" : "lock.fill")
                .font(.system(size: 80))
                .foregroundColor(Color(red: 1.0, green: 0.792, blue: 0.157))
                .scaleEffect(unlockAnimationScale)
                .animation(.spring(), value: viewModel.isUnlocked)

            Text("Parental Controls")
                .font(.largeTitle)
                .fontWeight(.bold)

            Text("Authentication required to access settings")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
        }
    }

    private var authenticationOptions: some View {
        VStack(spacing: 20) {
            if viewModel.isPINSet {
                pinButton
            }

            if viewModel.hasPattern {
                patternButton
            }

            if viewModel.isBiometricAvailable && viewModel.isBiometricEnabled {
                biometricButton
            }

            if !viewModel.isPINSet && !viewModel.hasPattern {
                securitySetupOptions
            }
        }
        .padding(.horizontal)
    }

    private var pinButton: some View {
        Button(action: { showPINEntry = true }) {
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

    private var patternButton: some View {
        Button(action: { showPatternEntry = true }) {
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

    private var biometricButton: some View {
        Button(action: { authenticateWithBiometric() }) {
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

    private var securitySetupOptions: some View {
        VStack(spacing: 12) {
            Text("No security configured")
                .font(.caption)
                .foregroundColor(.secondary)

            setupPINButton
            setupPatternButton
        }
    }

    private var setupPINButton: some View {
        Button(action: { showPINEntry = true }) {
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
    }

    private var setupPatternButton: some View {
        Button(action: { showPatternEntry = true }) {
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

    private var warningsAndStatus: some View {
        VStack(spacing: 12) {
            if viewModel.attemptsRemaining < 3 && viewModel.attemptsRemaining > 0 {
                attemptsWarning
            }

            if viewModel.isInCooldown {
                cooldownTimer
            }
        }
    }

    private var attemptsWarning: some View {
        Text("⚠️ \(viewModel.attemptsRemaining) attempts remaining")
            .foregroundColor(.orange)
            .font(.caption)
    }

    private var cooldownTimer: some View {
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

    private var pinEntrySheet: some View {
        PINEntryView(
            isPresented: $showPINEntry,
            mode: viewModel.isPINSet ? .validate : .setup,
            onSuccess: { _ in
                handleUnlock()
            },
            onCancel: {}
        )
    }

    private var patternEntrySheet: some View {
        NavigationView {
            PatternLockView(
                mode: viewModel.hasPattern ? .validate : .setup,
                onSuccess: { handleUnlock() },
                onCancel: { showPatternEntry = false }
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

    // MARK: - Computed Properties

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

    // MARK: - Helper Methods

    private func checkInitialAuth() {
        if shouldAllowSetup() {
            handleUnlock()
        } else if shouldPromptBiometric() {
            promptBiometricAfterDelay()
        }
    }

    private func shouldAllowSetup() -> Bool {
        return !viewModel.isPINSet && !viewModel.hasPattern
    }

    private func shouldPromptBiometric() -> Bool {
        return viewModel.isBiometricEnabled && viewModel.isBiometricAvailable
    }

    private func promptBiometricAfterDelay() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            authenticateWithBiometric()
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