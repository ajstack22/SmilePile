import SwiftUI

struct CompletionScreen: View {
    @ObservedObject var coordinator: OnboardingCoordinator
    @State private var showCheckmark = false
    @State private var showContent = false

    var body: some View {
        VStack(spacing: 40) {
            Spacer()

            // Success animation
            if showCheckmark {
                ZStack {
                    Circle()
                        .fill(Color.smilePileGreen.opacity(0.1))
                        .frame(width: 120, height: 120)

                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 80))
                        .foregroundColor(.smilePileGreen)
                }
                .scaleEffect(showCheckmark ? 1 : 0.5)
                .animation(.spring(response: 0.5, dampingFraction: 0.6), value: showCheckmark)
            }

            // Text content
            if showContent {
                VStack(spacing: 12) {
                    Text("All Set!")
                        .font(.custom("Nunito-Bold", size: 32))

                    Text("SmilePile is ready to use")
                        .font(.custom("Nunito-Regular", size: 18))
                        .foregroundColor(.secondary)
                }
                .transition(.opacity.combined(with: .move(edge: .bottom)))
            }

            // Summary card
            if showContent {
                VStack(alignment: .leading, spacing: 16) {
                    // Piles created
                    if !coordinator.onboardingData.categories.isEmpty {
                        HStack(spacing: 12) {
                            Image(systemName: "square.stack")
                                .foregroundColor(.smilePileOrange)

                            Text("\(coordinator.onboardingData.categories.count) piles created")
                                .font(.custom("Nunito-Regular", size: 16))
                        }
                    }

                    // PIN enabled
                    if !coordinator.onboardingData.skipPIN && coordinator.onboardingData.pinCode != nil {
                        HStack(spacing: 12) {
                            Image(systemName: "lock.fill")
                                .foregroundColor(Color(hex: "#45B7D1") ?? .smilePileBlue)

                            Text("PIN protection enabled")
                                .font(.custom("Nunito-Regular", size: 16))
                        }
                    }
                }
                .padding(20)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color.gray.opacity(0.5))
                )
                .padding(.horizontal, 40)
                .transition(.opacity.combined(with: .move(edge: .bottom)))
            }

            Spacer()

            // Start button
            if showContent {
                Button(action: {
                    // This will trigger the dismissal via notification
                    coordinator.isComplete = true
                }) {
                    Text("Start Using SmilePile")
                        .font(.custom("Nunito-Medium", size: 18))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(Color.smilePileBlue)
                        .cornerRadius(12)
                }
                .padding(.horizontal, 40)
                .padding(.bottom, 50)
                .transition(.opacity.combined(with: .move(edge: .bottom)))
            }
        }
        .onAppear {
            // Animate in sequence
            withAnimation(.default.delay(0.1)) {
                showCheckmark = true
            }

            withAnimation(.default.delay(0.5)) {
                showContent = true
            }
        }
    }
}