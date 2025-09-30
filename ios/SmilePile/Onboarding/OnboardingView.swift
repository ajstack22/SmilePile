import SwiftUI

struct OnboardingView: View {
    @StateObject private var coordinator = OnboardingCoordinator()
    @Environment(\.dismiss) var dismiss

    var body: some View {
        ZStack {
            // Background
            LinearGradient(
                colors: [
                    Color.smilePilePink.opacity(0.1),
                    Color.smilePileBlue.opacity(0.1)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                // Navigation bar - simplified to match Android
                if coordinator.currentStep == .categories || coordinator.currentStep == .pinSetup {
                    HStack {
                        Button(action: {
                            coordinator.navigateBack()
                        }) {
                            Image(systemName: "chevron.left")
                                .font(.title2)
                                .foregroundColor(.primary)
                        }
                        .padding()

                        Spacer()

                        Text(coordinator.currentStep == .categories ? "Create Piles" : "PIN Setup")
                            .font(.nunito(18, weight: .semibold))

                        Spacer()

                        // Skip button only for PIN
                        if coordinator.currentStep == .pinSetup {
                            // Skip handled within PIN screen
                            Color.clear
                                .frame(width: 44, height: 44)
                                .padding()
                        } else {
                            Color.clear
                                .frame(width: 44, height: 44)
                                .padding()
                        }
                    }
                }

                // Content
                Group {
                    switch coordinator.currentStep {
                    case .welcome:
                        WelcomeScreen(coordinator: coordinator)
                    case .categories:
                        CategorySetupScreen(coordinator: coordinator)
                    case .photoImport:
                        // PhotoImport removed from flow - should never reach here
                        // Keep case to avoid compilation error but show PIN instead
                        PINSetupScreen(coordinator: coordinator)
                    case .pinSetup:
                        PINSetupScreen(coordinator: coordinator)
                    case .complete:
                        CompletionScreen(coordinator: coordinator)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .alert("Error", isPresented: $coordinator.showError) {
            Button("OK") {
                coordinator.showError = false
            }
        } message: {
            Text(coordinator.errorMessage)
        }
        .onReceive(NotificationCenter.default.publisher(for: .onboardingComplete)) { _ in
            dismiss()
        }
    }
}