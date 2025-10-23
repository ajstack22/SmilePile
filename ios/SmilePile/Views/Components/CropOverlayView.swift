import SwiftUI

/// Interactive crop overlay with draggable corners and grid
struct CropOverlayView: View {
    @Binding var cropRect: CGRect
    let imageSize: CGSize
    let aspectRatio: CGFloat?
    let onComplete: (CGRect) -> Void
    let onCancel: () -> Void

    @State private var activeHandle: CropHandle? = nil
    @State private var initialCropRect: CGRect
    @State private var dragOffset = CGSize.zero
    @State private var gestureStartRect: CGRect?
    @State private var dragMode: DragMode = .none

    private enum DragMode {
        case none
        case move
        case resize(CropHandle)
    }

    private let handleSize: CGFloat = 24  // Match Android's 24pt radius
    private let borderWidth: CGFloat = 2
    private let gridLineWidth: CGFloat = 1  // Match Android's 1dp

    init(cropRect: Binding<CGRect>, imageSize: CGSize, aspectRatio: CGFloat?, onComplete: @escaping (CGRect) -> Void, onCancel: @escaping () -> Void) {
        self._cropRect = cropRect
        self.imageSize = imageSize
        self.aspectRatio = aspectRatio
        self.onComplete = onComplete
        self.onCancel = onCancel
        self._initialCropRect = State(initialValue: cropRect.wrappedValue)
    }

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                // Dark overlay outside crop area - Match Android's 0.6 opacity
                Rectangle()
                    .fill(Color.black.opacity(0.6))
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
                                        x: {
                                            let offsets = displayOffsets(in: geometry)
                                            let scale = scaleFactor(in: geometry)
                                            return offsets.x + (cropRect.midX * scale)
                                        }(),
                                        y: {
                                            let offsets = displayOffsets(in: geometry)
                                            let scale = scaleFactor(in: geometry)
                                            return offsets.y + (cropRect.midY * scale)
                                        }()
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
                        x: {
                            let offsets = displayOffsets(in: geometry)
                            let scale = scaleFactor(in: geometry)
                            return offsets.x + (cropRect.midX * scale)
                        }(),
                        y: {
                            let offsets = displayOffsets(in: geometry)
                            let scale = scaleFactor(in: geometry)
                            return offsets.y + (cropRect.midY * scale)
                        }()
                    )
                    .allowsHitTesting(false)

                // Invisible tap area for moving crop
                Rectangle()
                    .fill(Color.clear)
                    .frame(
                        width: cropRect.width * scaleFactor(in: geometry),
                        height: cropRect.height * scaleFactor(in: geometry)
                    )
                    .position(
                        x: {
                            let offsets = displayOffsets(in: geometry)
                            let scale = scaleFactor(in: geometry)
                            return offsets.x + (cropRect.midX * scale)
                        }(),
                        y: {
                            let offsets = displayOffsets(in: geometry)
                            let scale = scaleFactor(in: geometry)
                            return offsets.y + (cropRect.midY * scale)
                        }()
                    )
                    .gesture(
                        DragGesture()
                            .onChanged { value in
                                if gestureStartRect == nil {
                                    gestureStartRect = cropRect
                                    dragMode = .move
                                }
                                if case .move = dragMode {
                                    let delta = CGSize(
                                        width: value.translation.width,
                                        height: value.translation.height
                                    )
                                    moveCropRect(delta: delta, in: geometry)
                                }
                            }
                            .onEnded { _ in
                                gestureStartRect = nil
                                dragMode = .none
                            }
                    )

                // Corner handles
                ForEach(CropHandle.allCases, id: \.self) { handle in
                    cropHandle(for: handle, in: geometry)
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
            .stroke(Color.white.opacity(0.5), lineWidth: gridLineWidth)  // Match Android's 0.5 opacity
        }
    }

    private func cropHandle(for handle: CropHandle, in geometry: GeometryProxy) -> some View {
        let position = handlePosition(for: handle, in: geometry)

        // Match Android's white circle handles
        return Circle()
            .fill(Color.white)
            .frame(width: handleSize * 2, height: handleSize * 2)  // Diameter = 2 * radius
            .position(position)
            .gesture(
                DragGesture()
                    .onChanged { value in
                        if gestureStartRect == nil {
                            gestureStartRect = cropRect
                            dragMode = .resize(handle)
                        }
                        if case .resize(let activeHandle) = dragMode, activeHandle == handle {
                            let delta = CGSize(
                                width: value.translation.width,
                                height: value.translation.height
                            )
                            updateCropRect(for: handle, delta: delta, in: geometry)
                        }
                    }
                    .onEnded { _ in
                        gestureStartRect = nil
                        dragMode = .none
                    }
            )
    }

    private func handlePosition(for handle: CropHandle, in geometry: GeometryProxy) -> CGPoint {
        let scale = scaleFactor(in: geometry)
        let offsets = displayOffsets(in: geometry)

        let rect = CGRect(
            x: offsets.x + (cropRect.minX * scale),
            y: offsets.y + (cropRect.minY * scale),
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

    private func updateCropRect(for handle: CropHandle, delta: CGSize, in geometry: GeometryProxy) {
        let scale = scaleFactor(in: geometry)
        let minSize: CGFloat = 50 / scale

        guard let startRect = gestureStartRect else { return }
        var newRect = startRect
        let deltaX = delta.width / scale
        let deltaY = delta.height / scale

        switch handle {
        case .topLeft:
            newRect.origin.x = min(startRect.origin.x + deltaX, startRect.maxX - minSize)
            newRect.origin.y = min(startRect.origin.y + deltaY, startRect.maxY - minSize)
            newRect.size.width = max(startRect.width - deltaX, minSize)
            newRect.size.height = max(startRect.height - deltaY, minSize)

        case .topRight:
            newRect.origin.y = min(startRect.origin.y + deltaY, startRect.maxY - minSize)
            newRect.size.width = max(startRect.width + deltaX, minSize)
            newRect.size.height = max(startRect.height - deltaY, minSize)

        case .bottomLeft:
            newRect.origin.x = min(startRect.origin.x + deltaX, startRect.maxX - minSize)
            newRect.size.width = max(startRect.width - deltaX, minSize)
            newRect.size.height = max(startRect.height + deltaY, minSize)

        case .bottomRight:
            newRect.size.width = max(startRect.width + deltaX, minSize)
            newRect.size.height = max(startRect.height + deltaY, minSize)
        }

        // Enforce aspect ratio if set
        if let ratio = aspectRatio {
            newRect.size.height = newRect.size.width / ratio

            // Re-constrain if height adjustment pushed us out of bounds
            if newRect.maxY > imageSize.height {
                newRect.size.height = imageSize.height - newRect.origin.y
                newRect.size.width = newRect.size.height * ratio
            }
        }

        // Constrain to image bounds
        newRect.origin.x = max(0, min(newRect.origin.x, imageSize.width - newRect.width))
        newRect.origin.y = max(0, min(newRect.origin.y, imageSize.height - newRect.height))
        newRect.size.width = min(newRect.width, imageSize.width - newRect.origin.x)
        newRect.size.height = min(newRect.height, imageSize.height - newRect.origin.y)

        cropRect = newRect
        // Call the completion handler to update the view model
        onComplete(newRect)
    }

    private func moveCropRect(delta: CGSize, in geometry: GeometryProxy) {
        let scale = scaleFactor(in: geometry)
        guard let startRect = gestureStartRect else { return }

        var newRect = startRect
        let deltaX = delta.width / scale
        let deltaY = delta.height / scale

        // Translate origin
        newRect.origin.x += deltaX
        newRect.origin.y += deltaY

        // Constrain to image bounds
        newRect.origin.x = max(0, min(newRect.origin.x, imageSize.width - newRect.width))
        newRect.origin.y = max(0, min(newRect.origin.y, imageSize.height - newRect.height))

        cropRect = newRect
        onComplete(newRect)
    }

    private func scaleFactor(in geometry: GeometryProxy) -> CGFloat {
        let widthScale = geometry.size.width / imageSize.width
        let heightScale = geometry.size.height / imageSize.height
        return min(widthScale, heightScale)
    }

    private func displayDimensions(in geometry: GeometryProxy) -> (width: CGFloat, height: CGFloat) {
        let imageAspectRatio = imageSize.width / imageSize.height
        let canvasAspectRatio = geometry.size.width / geometry.size.height

        if imageAspectRatio > canvasAspectRatio {
            let displayWidth = geometry.size.width
            let displayHeight = displayWidth / imageAspectRatio
            return (displayWidth, displayHeight)
        } else {
            let displayHeight = geometry.size.height
            let displayWidth = displayHeight * imageAspectRatio
            return (displayWidth, displayHeight)
        }
    }

    private func displayOffsets(in geometry: GeometryProxy) -> (x: CGFloat, y: CGFloat) {
        let dims = displayDimensions(in: geometry)
        let offsetX = (geometry.size.width - dims.width) / 2
        let offsetY = (geometry.size.height - dims.height) / 2
        return (offsetX, offsetY)
    }
}

enum CropHandle: CaseIterable {
    case topLeft
    case topRight
    case bottomLeft
    case bottomRight
}