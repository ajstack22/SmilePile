import SwiftUI

struct CompletionScreen: View {
    @ObservedObject var coordinator: OnboardingCoordinator
    @State private var showCheckmark = false
    @State private var showContent = false

    var body: some View {
        VStack(spacing: 40) {
            Spacer()

            // Success animation
            ZStack {
                Circle()
                    .fill(Color(red: 1.0, green: 0.42, blue: 0.42).opacity(0.1))
                    .frame(width: 120, height: 120)
                    .scaleEffect(showCheckmark ? 1.0 : 0.5)

                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 80))
                    .foregroundColor(Color(red: 1.0, green: 0.42, blue: 0.42))
                    .scaleEffect(showCheckmark ? 1.0 : 0.3)
                    .opacity(showCheckmark ? 1.0 : 0)
            }

            // Success message
            VStack(spacing: 16) {
                Text("All Set!")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .opacity(showContent ? 1.0 : 0)

                Text("SmilePile is ready to use")
                    .font(.title3)
                    .foregroundColor(.secondary)
                    .opacity(showContent ? 1.0 : 0)
            }

            // Summary
            if showContent {
                VStack(alignment: .leading, spacing: 12) {
                    // Categories created
                    if !coordinator.onboardingData.categories.isEmpty {
                        HStack {
                            Image(systemName: "folder.fill")
                                .foregroundColor(Color(red: 1.0, green: 0.42, blue: 0.42))
                                .frame(width: 24)

                            Text("\(coordinator.onboardingData.categories.count) categories created")
                                .font(.subheadline)
                        }
                    }

                    // Photos imported
                    if !coordinator.onboardingData.importedPhotos.isEmpty {
                        HStack {
                            Image(systemName: "photo.fill")
                                .foregroundColor(Color(red: 0.3, green: 0.7, blue: 1.0))
                                .frame(width: 24)

                            Text("\(coordinator.onboardingData.importedPhotos.count) photos imported")
                                .font(.subheadline)
                        }
                    }

                    // PIN set
                    if !coordinator.onboardingData.skipPIN && coordinator.onboardingData.pinCode != nil {
                        HStack {
                            Image(systemName: "lock.fill")
                                .foregroundColor(Color(red: 0.4, green: 0.8, blue: 0.4))
                                .frame(width: 24)

                            Text("PIN protection enabled")
                                .font(.subheadline)
                        }
                    }
                }
                .padding()
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color.gray.opacity(0.1))
                )
                .padding(.horizontal, 40)
                .transition(.opacity)
            }

            Spacer()

            // Get Started button
            if showContent {
                Button(action: {
                    // This will trigger the dismissal via notification
                    coordinator.isComplete = true
                }) {
                    Text("Start Using SmilePile")
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(red: 1.0, green: 0.42, blue: 0.42))
                        .cornerRadius(12)
                }
                .padding(.horizontal, 40)
                .transition(.move(edge: .bottom))
            }

            Spacer()
        }
        .onAppear {
            // Animate the success checkmark
            withAnimation(.spring(response: 0.6, dampingFraction: 0.6)) {
                showCheckmark = true
            }

            // Show content after checkmark animation
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                withAnimation(.easeInOut(duration: 0.5)) {
                    showContent = true
                }
            }
        }
    }
}