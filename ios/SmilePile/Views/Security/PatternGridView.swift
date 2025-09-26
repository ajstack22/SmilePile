import SwiftUI

struct PatternGridView: View {
    @Binding var selectedDots: [Int]
    @Binding var currentPath: Path
    @State private var dotPositions: [Int: CGPoint] = [:]
    @State private var isDrawing = false
    @State private var currentPosition = CGPoint.zero

    let gridSize = 3
    let dotSize: CGFloat = 20
    let activeDotSize: CGFloat = 30
    let lineWidth: CGFloat = 4

    var isDisabled: Bool = false
    var onPatternComplete: (([Int]) -> Void)?

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                drawPatternLines(in: geometry.size)

                ForEach(0..<9) { index in
                    DotView(
                        index: index,
                        position: dotPosition(for: index, in: geometry.size),
                        isSelected: selectedDots.contains(index),
                        dotSize: dotSize,
                        activeDotSize: activeDotSize
                    )
                    .onAppear {
                        dotPositions[index] = dotPosition(for: index, in: geometry.size)
                    }
                }
            }
            .frame(width: geometry.size.width, height: geometry.size.height)
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        if !isDisabled {
                            handleDragChanged(value.location, in: geometry.size)
                        }
                    }
                    .onEnded { _ in
                        if !isDisabled {
                            handleDragEnded()
                        }
                    }
            )
        }
        .aspectRatio(1, contentMode: .fit)
        .frame(maxWidth: 300, maxHeight: 300)
    }

    private func dotPosition(for index: Int, in size: CGSize) -> CGPoint {
        let row = index / gridSize
        let col = index % gridSize

        let cellWidth = size.width / CGFloat(gridSize)
        let cellHeight = size.height / CGFloat(gridSize)

        let x = cellWidth * (CGFloat(col) + 0.5)
        let y = cellHeight * (CGFloat(row) + 0.5)

        return CGPoint(x: x, y: y)
    }

    private func drawPatternLines(in size: CGSize) -> some View {
        ZStack {
            Path { path in
                for i in 0..<selectedDots.count - 1 {
                    let startDot = selectedDots[i]
                    let endDot = selectedDots[i + 1]

                    if let startPos = dotPositions[startDot],
                       let endPos = dotPositions[endDot] {
                        path.move(to: startPos)
                        path.addLine(to: endPos)
                    }
                }
            }
            .stroke(Color.blue, lineWidth: lineWidth)

            if isDrawing && !selectedDots.isEmpty {
                Path { path in
                    if let lastDot = selectedDots.last,
                       let lastPos = dotPositions[lastDot] {
                        path.move(to: lastPos)
                        path.addLine(to: currentPosition)
                    }
                }
                .stroke(Color.blue.opacity(0.5), lineWidth: lineWidth)
            }
        }
    }

    private func handleDragChanged(_ location: CGPoint, in size: CGSize) {
        isDrawing = true
        currentPosition = location

        if let nearestDot = findNearestDot(to: location),
           !selectedDots.contains(nearestDot) {
            withAnimation(.easeInOut(duration: 0.1)) {
                selectedDots.append(nearestDot)
            }

            let impactFeedback = UIImpactFeedbackGenerator(style: .light)
            impactFeedback.impactOccurred()
        }
    }

    private func handleDragEnded() {
        isDrawing = false
        currentPosition = .zero

        if selectedDots.count >= 4 {
            onPatternComplete?(selectedDots)
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            if !isDisabled {
                clearPattern()
            }
        }
    }

    private func findNearestDot(to location: CGPoint) -> Int? {
        let threshold: CGFloat = 40

        for (index, position) in dotPositions {
            let distance = sqrt(pow(location.x - position.x, 2) + pow(location.y - position.y, 2))
            if distance < threshold {
                return index
            }
        }
        return nil
    }

    private func clearPattern() {
        withAnimation(.easeOut(duration: 0.2)) {
            selectedDots.removeAll()
            currentPath = Path()
        }
    }
}

struct DotView: View {
    let index: Int
    let position: CGPoint
    let isSelected: Bool
    let dotSize: CGFloat
    let activeDotSize: CGFloat

    var body: some View {
        Circle()
            .fill(isSelected ? Color.blue : Color.gray.opacity(0.3))
            .frame(width: isSelected ? activeDotSize : dotSize,
                   height: isSelected ? activeDotSize : dotSize)
            .overlay(
                Circle()
                    .stroke(Color.blue, lineWidth: 2)
                    .opacity(isSelected ? 1 : 0.5)
            )
            .position(position)
            .animation(.easeInOut(duration: 0.1), value: isSelected)
    }
}

struct PatternGridView_Previews: PreviewProvider {
    @State static var selectedDots: [Int] = []
    @State static var currentPath = Path()

    static var previews: some View {
        VStack {
            Text("Draw a Pattern")
                .font(.headline)

            PatternGridView(
                selectedDots: $selectedDots,
                currentPath: $currentPath
            ) { pattern in
                print("Pattern completed: \(pattern)")
            }
            .padding()

            Text("Selected: \(selectedDots.map(String.init).joined(separator: "-"))")
                .font(.caption)
        }
    }
}