import SwiftUI

struct OnboardingView: View {
    @StateObject private var coordinator = OnboardingCoordinator()
    @Environment(\.dismiss) var dismiss

    var body: some View {
        ZStack {
            // Background
            LinearGradient(
                colors: [
                    Color(red: 1.0, green: 0.42, blue: 0.42).opacity(0.1),
                    Color(red: 0.3, green: 0.7, blue: 1.0).opacity(0.1)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                // Navigation bar
                if coordinator.currentStep != .welcome && coordinator.currentStep != .complete {
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

                        Text(coordinator.currentStep.title)
                            .font(.headline)

                        Spacer()

                        // Skip button for applicable steps
                        if coordinator.currentStep.canSkip {
                            Button("Skip") {
                                coordinator.skip()
                            }
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