import SwiftUI

// Enhanced fullscreen photo viewer for Kids Mode with dual-pager system
struct EnhancedPhotoViewer: View {
    @EnvironmentObject var viewModel: KidsModeViewModel
    @Binding var isPresented: Bool
    let initialPhotoIndex: Int

    @State private var currentCategoryIndex: Int = 0
    @State private var currentPhotoIndices: [Int64: Int] = [:] // Track photo index per category
    @State private var showToast = false
    @State private var toastCategory: Category?

    var body: some View {
        ZStack {
            // Black background for letterboxing
            Color.black
                .ignoresSafeArea(.all)

            // Dual-pager system: Horizontal for categories
            TabView(selection: $currentCategoryIndex) {
                ForEach(Array(viewModel.categories.enumerated()), id: \.offset) { categoryIndex, category in
                    categoryPhotoView(category: category, categoryIndex: categoryIndex)
                        .tag(categoryIndex)
                }
            }
            .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
            .ignoresSafeArea()
            .onChange(of: currentCategoryIndex) { newIndex in
                // Update selected category and show toast
                if newIndex < viewModel.categories.count {
                    let category = viewModel.categories[newIndex]
                    viewModel.selectCategory(category)
                    ToastManager.shared.showCategoryToast(category)
                }
            }

            // Tap gesture to exit fullscreen (on background areas)
            .onTapGesture {
                isPresented = false
            }
        }
        .statusBarHidden(true)
        .onAppear {
            setupInitialState()
        }
    }

    private func categoryPhotoView(category: Category, categoryIndex: Int) -> some View {
        let photos = viewModel.getPhotosForCategory(category.id)

        return ZStack {
            if photos.isEmpty {
                // Empty category view
                VStack {
                    Image(systemName: "photo")
                        .font(.system(size: 64))
                        .foregroundColor(.gray)
                    Text("No photos in \(category.displayName)")
                        .foregroundColor(.gray)
                        .padding(.top)
                }
            } else {
                // Vertical pager for photos within category
                TabView(selection: Binding(
                    get: { currentPhotoIndices[category.id] ?? 0 },
                    set: { currentPhotoIndices[category.id] = $0 }
                )) {
                    ForEach(Array(photos.enumerated()), id: \.offset) { photoIndex, photo in
                        EnhancedPhotoPage(
                            photo: photo,
                            onTap: {
                                isPresented = false
                            }
                        )
                        .tag(photoIndex)
                    }
                }
                .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
                .rotationEffect(.degrees(-90)) // Rotate for vertical paging
                .frame(
                    width: UIScreen.main.bounds.height,
                    height: UIScreen.main.bounds.width
                )
                .rotationEffect(.degrees(90))
                .scaleEffect(x: -1, y: 1) // Flip to correct orientation
            }
        }
    }

    private func setupInitialState() {
        // Find the category index for the current selected category
        if let selectedCategory = viewModel.selectedCategory,
           let categoryIndex = viewModel.categories.firstIndex(where: { $0.id == selectedCategory.id }) {
            currentCategoryIndex = categoryIndex

            // Set the initial photo index for this category
            currentPhotoIndices[selectedCategory.id] = initialPhotoIndex
        }
    }
}

// Individual photo page with zoom and pan support
struct EnhancedPhotoPage: View {
    let photo: Photo
    let onTap: () -> Void

    @State private var scale: CGFloat = 1.0
    @State private var lastScale: CGFloat = 1.0
    @State private var offset: CGSize = .zero
    @State private var lastOffset: CGSize = .zero
    @State private var isZoomed: Bool = false

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                Color.black

                AsyncImageView(photo: photo, contentMode: .fit)
                    .scaleEffect(scale)
                    .offset(offset)
                    .scaleEffect(x: -1, y: 1) // Counteract parent flip
                    .gesture(
                        SimultaneousGesture(
                            // Pinch to zoom
                            MagnificationGesture()
                                .onChanged { value in
                                    let delta = value / lastScale
                                    lastScale = value
                                    scale = min(max(scale * delta, 1), 4)
                                    isZoomed = scale > 1
                                }
                                .onEnded { _ in
                                    lastScale = 1.0
                                    withAnimation(.spring()) {
                                        if scale < 1 {
                                            scale = 1
                                            offset = .zero
                                            isZoomed = false
                                        }
                                    }
                                },
                            // Pan when zoomed
                            DragGesture()
                                .onChanged { value in
                                    if scale > 1 {
                                        offset = CGSize(
                                            width: lastOffset.width + value.translation.width,
                                            height: lastOffset.height + value.translation.height
                                        )
                                    }
                                }
                                .onEnded { _ in
                                    lastOffset = offset
                                }
                        )
                    )
                    // Double tap to zoom
                    .onTapGesture(count: 2) {
                        withAnimation(.spring()) {
                            if scale > 1 {
                                scale = 1
                                offset = .zero
                                lastOffset = .zero
                                isZoomed = false
                            } else {
                                scale = 2
                                isZoomed = true
                            }
                        }
                    }
                    // Single tap to exit (only when not zoomed)
                    .onTapGesture(count: 1) {
                        if !isZoomed {
                            onTap()
                        }
                    }
            }
        }
        .ignoresSafeArea()
    }
}

#Preview {
    EnhancedPhotoViewer(isPresented: .constant(true), initialPhotoIndex: 0)
        .environmentObject(KidsModeViewModel())
}