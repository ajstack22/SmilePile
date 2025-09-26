import SwiftUI

struct SecuritySettingsView: View {
    @StateObject private var viewModel = SecuritySettingsViewModel()
    @State private var showPINSetup = false
    @State private var showPatternSetup = false
    @State private var showPINChange = false
    @State private var showPatternChange = false
    @State private var showRemoveAlert = false
    @State private var removalType: SecurityType = .pin
    @State private var selectedPINLength = 4

    enum SecurityType {
        case pin, pattern
    }

    var body: some View {
        List {
            // Authentication Method Section
            Section(header: Text("Authentication Method")) {
                authenticationMethodPicker
            }

            // PIN Settings Section
            Section(header: Text("PIN Settings")) {
                if viewModel.hasPIN {
                    pinManagementOptions
                } else {
                    setupPINOption
                }
            }

            // Pattern Settings Section
            Section(header: Text("Pattern Settings")) {
                if viewModel.hasPattern {
                    patternManagementOptions
                } else {
                    setupPatternOption
                }
            }

            // Biometric Settings Section
            if viewModel.isBiometricAvailable {
                Section(header: Text("Biometric Authentication")) {
                    biometricToggle
                }
            }

            // Security Status Section
            Section(header: Text("Security Status")) {
                securityStatusView
            }
        }
        .navigationTitle("Security & PIN")
        .navigationBarTitleDisplayMode(.large)
        .onAppear {
            viewModel.checkSecurityStatus()
        }
        .sheet(isPresented: $showPINSetup) {
            PINSetupView(pinLength: selectedPINLength) {
                viewModel.checkSecurityStatus()
                showPINSetup = false
            }
        }
        .sheet(isPresented: $showPINChange) {
            PINChangeView {
                viewModel.checkSecurityStatus()
                showPINChange = false
            }
        }
        .sheet(isPresented: $showPatternSetup) {
            NavigationView {
                PatternLockView(mode: .setup) {
                    viewModel.checkSecurityStatus()
                    showPatternSetup = false
                } onCancel: {
                    showPatternSetup = false
                }
            }
        }
        .sheet(isPresented: $showPatternChange) {
            NavigationView {
                PatternLockView(mode: .change) {
                    viewModel.checkSecurityStatus()
                    showPatternChange = false
                } onCancel: {
                    showPatternChange = false
                }
            }
        }
        .alert("Remove Security", isPresented: $showRemoveAlert) {
            Button("Cancel", role: .cancel) {}
            Button("Remove", role: .destructive) {
                handleRemoveSecurity()
            }
        } message: {
            Text("Are you sure you want to remove your \(removalType == .pin ? "PIN" : "pattern")? This will disable parental controls.")
        }
    }

    // MARK: - View Components

    private var authenticationMethodPicker: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Preferred Method")
                .font(.subheadline)
                .foregroundColor(.secondary)

            Picker("Authentication Method", selection: $viewModel.preferredAuthMethod) {
                Text("PIN").tag(AuthenticationMethod.pin)
                if viewModel.hasPattern {
                    Text("Pattern").tag(AuthenticationMethod.pattern)
                }
                if viewModel.isBiometricAvailable && viewModel.isBiometricEnabled {
                    Text("Biometric").tag(AuthenticationMethod.biometric)
                }
            }
            .pickerStyle(SegmentedPickerStyle())
            .onChange(of: viewModel.preferredAuthMethod) { newValue in
                viewModel.setPreferredAuthMethod(newValue)
            }
        }
    }

    private var pinManagementOptions: some View {
        Group {
            Button(action: { showPINChange = true }) {
                HStack {
                    Image(systemName: "number.square.fill")
                    Text("Change PIN")
                    Spacer()
                    Text("\(viewModel.currentPINLength) digits")
                        .foregroundColor(.secondary)
                }
            }

            Button(action: {
                removalType = .pin
                showRemoveAlert = true
            }) {
                HStack {
                    Image(systemName: "trash")
                    Text("Remove PIN")
                }
                .foregroundColor(.red)
            }
        }
    }

    private var setupPINOption: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "plus.circle.fill")
                Text("Set Up PIN")
            }

            Picker("PIN Length", selection: $selectedPINLength) {
                Text("4 Digits").tag(4)
                Text("5 Digits").tag(5)
                Text("6 Digits").tag(6)
            }
            .pickerStyle(SegmentedPickerStyle())

            Button(action: { showPINSetup = true }) {
                Text("Create PIN")
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(8)
            }
        }
    }

    private var patternManagementOptions: some View {
        Group {
            Button(action: { showPatternChange = true }) {
                HStack {
                    Image(systemName: "circle.grid.3x3.fill")
                    Text("Change Pattern")
                }
            }

            Button(action: {
                removalType = .pattern
                showRemoveAlert = true
            }) {
                HStack {
                    Image(systemName: "trash")
                    Text("Remove Pattern")
                }
                .foregroundColor(.red)
            }
        }
    }

    private var setupPatternOption: some View {
        Button(action: { showPatternSetup = true }) {
            HStack {
                Image(systemName: "plus.circle.fill")
                Text("Set Up Pattern")
                Spacer()
                Image(systemName: "chevron.right")
                    .foregroundColor(.secondary)
            }
        }
    }

    private var biometricToggle: some View {
        Toggle(isOn: $viewModel.isBiometricEnabled) {
            HStack {
                Image(systemName: viewModel.biometricIcon)
                VStack(alignment: .leading) {
                    Text(viewModel.biometricName)
                    Text("Use biometric authentication")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
        }
        .onChange(of: viewModel.isBiometricEnabled) { newValue in
            viewModel.setBiometricEnabled(newValue)
        }
    }

    private var securityStatusView: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: viewModel.isSecure ? "checkmark.shield.fill" : "exclamationmark.shield.fill")
                    .foregroundColor(viewModel.isSecure ? .green : .orange)
                Text(viewModel.isSecure ? "Security Enabled" : "Security Not Configured")
                    .fontWeight(.medium)
            }

            if viewModel.hasPIN {
                Label("PIN configured (\(viewModel.currentPINLength) digits)", systemImage: "checkmark.circle.fill")
                    .font(.caption)
                    .foregroundColor(.green)
            }

            if viewModel.hasPattern {
                Label("Pattern configured", systemImage: "checkmark.circle.fill")
                    .font(.caption)
                    .foregroundColor(.green)
            }

            if viewModel.isBiometricEnabled {
                Label("\(viewModel.biometricName) enabled", systemImage: "checkmark.circle.fill")
                    .font(.caption)
                    .foregroundColor(.green)
            }
        }
        .padding(.vertical, 4)
    }

    // MARK: - Actions

    private func handleRemoveSecurity() {
        switch removalType {
        case .pin:
            viewModel.removePIN()
        case .pattern:
            viewModel.removePattern()
        }
    }
}

// MARK: - Supporting Views

struct PINSetupView: View {
    let pinLength: Int
    let onComplete: () -> Void
    @State private var isPresented = true

    var body: some View {
        PINEntryView(
            isPresented: $isPresented,
            mode: .setup,
            pinLength: pinLength,
            onSuccess: { _ in
                onComplete()
            },
            onCancel: {
                isPresented = false
                onComplete()
            }
        )
    }
}

struct PINChangeView: View {
    let onComplete: () -> Void
    @State private var isPresented = true

    var body: some View {
        PINEntryView(
            isPresented: $isPresented,
            mode: .change,
            onSuccess: { _ in
                onComplete()
            },
            onCancel: {
                isPresented = false
                onComplete()
            }
        )
    }
}

struct SecuritySettingsView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            SecuritySettingsView()
        }
    }
}