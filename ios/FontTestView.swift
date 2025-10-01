import SwiftUI

struct FontTestView: View {
    var body: some View {
        VStack(spacing: 20) {
            Text("Font Test View")
                .font(.largeTitle)
                .padding()

            Group {
                Text("nunitoTitle (36pt heavy)")
                    .font(.nunitoTitle)

                Text("nunitoHeadline (22pt bold)")
                    .font(.nunitoHeadline)

                Text("nunitoBody (16pt medium)")
                    .font(.nunitoBody)

                Text("nunitoCaption (14pt regular)")
                    .font(.nunitoCaption)

                Text("nunitoButton (18pt bold)")
                    .font(.nunitoButton)

                Text("nunito(24, .black)")
                    .font(.nunito(24, weight: .black))
            }
            .padding(.horizontal)

            Spacer()
        }
    }
}