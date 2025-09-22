package com.smilepile.ui.components

/**
 * SmilePile Component Library
 * Master barrel file for easy component imports across the app.
 *
 * This file provides centralized access to all component categories:
 * - Gallery components (PhotoGrid, CategoryFilter, SelectionToolbar)
 * - Dialog components (UniversalCrudDialog)
 * - Shared components (LoadingIndicator, EmptyStateComponent)
 * - Orchestrators (PhotoGalleryOrchestrator)
 *
 * Usage examples:
 *
 * // Import all components from a specific category
 * import com.smilepile.ui.components.gallery.*
 * import com.smilepile.ui.components.shared.*
 * import com.smilepile.ui.components.dialogs.*
 *
 * // Import specific components
 * import com.smilepile.ui.components.gallery.PhotoGridComponent
 * import com.smilepile.ui.components.shared.LoadingIndicator
 *
 * Component Architecture:
 *
 * /components/
 * ├── gallery/                 # Photo gallery specific components
 * │   ├── PhotoGridComponent           # Main photo grid with selection support
 * │   ├── CategoryFilterComponent      # Category filtering chips
 * │   └── SelectionToolbarComponent    # Multi-selection toolbar
 * │
 * ├── dialogs/                 # Dialog components
 * │   └── UniversalCrudDialog          # Reusable CRUD dialog
 * │
 * ├── shared/                  # Shared/reusable components
 * │   ├── LoadingIndicator            # Loading states with shimmer effects
 * │   └── EmptyStateComponent         # Empty state UI variations
 * │
 * └── [other]/                 # Additional component categories
 *     ├── SearchBar                   # Search and filtering
 *     ├── CategoryChip                # Category display
 *     ├── DateRangePickerDialog       # Date selection
 *     └── ...                         # Other standalone components
 *
 * /orchestrators/              # Business logic orchestrators
 * └── PhotoGalleryOrchestrator        # Main gallery state management
 */

// Note: Kotlin doesn't support re-exports like TypeScript/JavaScript
// Users should import directly from the specific packages above
// or use package-level imports (import com.smilepile.ui.components.gallery.*)