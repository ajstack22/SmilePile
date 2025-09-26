import SwiftUI

/// Interactive crop overlay with draggable corners and grid
struct CropOverlayView: View {
    @Binding var cropRect: CGRect
    let imageSize: CGSize
    let onComplete: (CGRect) -> Void
    let onCancel: () -> Void

    @State private var activeHandle: CropHandle? = nil
    @State private var initialCropRect: CGRect
    @State private var dragOffset = CGSize.zero

    private let handleSize: CGFloat = 44
    private let borderWidth: CGFloat = 2
    private let gridLineWidth: CGFloat = 0.5

    init(cropRect: Binding<CGRect>, imageSize: CGSize, onComplete: @escaping (CGRect) -> Void, onCancel: @escaping () -> Void) {
        self._cropRect = cropRect
        self.imageSize = imageSize
        self.onComplete = onComplete
        self.onCancel = onCancel
        self._initialCropRect = State(initialValue: cropRect.wrappedValue)
    }

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                // Dark overlay outside crop area
                Rectangle()
                    .fill(Color.black.opacity(0.5))
                    .mask(
                        Rectangle()
                            .fill(Color.black)
                            .overlay(
                                Rectangle()
                                    .fill(Color.black)
                                    .frame(
                                        width: cropRect.width * scaleFactor(in: geometry),
                                        height: cropRect.height * scaleFactor(in: geometry)
                                    )
                                    .position(
                                        x: cropRect.midX * scaleFactor(in: geometry),
                                        y: cropRect.midY * scaleFactor(in: geometry)
                                    )
                                    .blendMode(.destinationOut)
                            )
                            .compositingGroup()
                    )
                    .allowsHitTesting(false)

                // Crop area with border and grid
                Rectangle()
                    .stroke(Color.white, lineWidth: borderWidth)
                    .overlay(gridOverlay)
                    .frame(
                        width: cropRect.width * scaleFactor(in: geometry),
                        height: cropRect.height * scaleFactor(in: geometry)
                    )
                    .position(
                        x: cropRect.midX * scaleFactor(in: geometry),
                        y: cropRect.midY * scaleFactor(in: geometry)
                    )
                    .allowsHitTesting(false)

                // Corner handles
                ForEach(CropHandle.allCases, id: \.self) { handle in
                    cropHandle(for: handle, in: geometry)
                }

                // Bottom toolbar
                VStack {
                    Spacer()
                    HStack(spacing: 30) {
                        Button(action: onCancel) {
                            Image(systemName: "xmark")
                                .font(.system(size: 24))
                                .foregroundColor(.white)
                                .frame(width: 44, height: 44)
                        }

                        Button(action: { onComplete(cropRect) }) {
                            Image(systemName: "checkmark")
                                .font(.system(size: 24))
                                .foregroundColor(.white)
                                .frame(width: 44, height: 44)
                        }
                    }
                    .padding()
                    .background(Color.black.opacity(0.7))
                    .cornerRadius(25)
                    .padding(.bottom, 50)
                }
            }
        }
    }

    private var gridOverlay: some View {
        GeometryReader { geometry in
            Path { path in
                // Vertical lines
                for i in 1..<3 {
                    let x = geometry.size.width * CGFloat(i) / 3
                    path.move(to: CGPoint(x: x, y: 0))
                    path.addLine(to: CGPoint(x: x, y: geometry.size.height))
                }

                // Horizontal lines
                for i in 1..<3 {
                    let y = geometry.size.height * CGFloat(i) / 3
                    path.move(to: CGPoint(x: 0, y: y))
                    path.addLine(to: CGPoint(x: geometry.size.width, y: y))
                }
            }
            .stroke(Color.white.opacity(0.3), lineWidth: gridLineWidth)
        }
    }

    private func cropHandle(for handle: CropHandle, in geometry: GeometryProxy) -> some View {
        let position = handlePosition(for: handle, in: geometry)

        return Circle()
            .fill(Color.white)
            .frame(width: handleSize, height: handleSize)
            .overlay(
                Circle()
                    .stroke(Color.black, lineWidth: 1)
            )
            .position(position)
            .gesture(
                DragGesture()
                    .onChanged { value in
                        updateCropRect(for: handle, translation: value.translation, in: geometry)
                    }
            )
    }

    private func handlePosition(for handle: CropHandle, in geometry: GeometryProxy) -> CGPoint {
        let scale = scaleFactor(in: geometry)
        let rect = CGRect(
            x: cropRect.minX * scale,
            y: cropRect.minY * scale,
            width: cropRect.width * scale,
            height: cropRect.height * scale
        )

        switch handle {
        case .topLeft:
            return CGPoint(x: rect.minX, y: rect.minY)
        case .topRight:
            return CGPoint(x: rect.maxX, y: rect.minY)
        case .bottomLeft:
            return CGPoint(x: rect.minX, y: rect.maxY)
        case .bottomRight:
            return CGPoint(x: rect.maxX, y: rect.maxY)
        }
    }

    private func updateCropRect(for handle: CropHandle, translation: CGSize, in geometry: GeometryProxy) {
        let scale = scaleFactor(in: geometry)
        let minSize: CGFloat = 50 / scale

        var newRect = cropRect
        let deltaX = translation.width / scale
        let deltaY = translation.height / scale

        switch handle {
        case .topLeft:
            newRect.origin.x = min(cropRect.origin.x + deltaX, cropRect.maxX - minSize)
            newRect.origin.y = min(cropRect.origin.y + deltaY, cropRect.maxY - minSize)
            newRect.size.width = max(cropRect.width - deltaX, minSize)
            newRect.size.height = max(cropRect.height - deltaY, minSize)

        case .topRight:
            newRect.origin.y = min(cropRect.origin.y + deltaY, cropRect.maxY - minSize)
            newRect.size.width = max(cropRect.width + deltaX, minSize)
            newRect.size.height = max(cropRect.height - deltaY, minSize)

        case .bottomLeft:
            newRect.origin.x = min(cropRect.origin.x + deltaX, cropRect.maxX - minSize)
            newRect.size.width = max(cropRect.width - deltaX, minSize)
            newRect.size.height = max(cropRect.height + deltaY, minSize)

        case .bottomRight:
            newRect.size.width = max(cropRect.width + deltaX, minSize)
            newRect.size.height = max(cropRect.height + deltaY, minSize)
        }

        // Constrain to image bounds
        newRect.origin.x = max(0, min(newRect.origin.x, imageSize.width - newRect.width))
        newRect.origin.y = max(0, min(newRect.origin.y, imageSize.height - newRect.height))
        newRect.size.width = min(newRect.width, imageSize.width - newRect.origin.x)
        newRect.size.height = min(newRect.height, imageSize.height - newRect.origin.y)

        cropRect = newRect
    }

    private func scaleFactor(in geometry: GeometryProxy) -> CGFloat {
        let widthScale = geometry.size.width / imageSize.width
        let heightScale = geometry.size.height / imageSize.height
        return min(widthScale, heightScale)
    }
}

enum CropHandle: CaseIterable {
    case topLeft
    case topRight
    case bottomLeft
    case bottomRight
}