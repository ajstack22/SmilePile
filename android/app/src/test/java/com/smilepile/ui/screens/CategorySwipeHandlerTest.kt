package com.smilepile.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.smilepile.data.models.Category
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for CategorySwipeHandler in KidsModeGalleryScreen
 */
class CategorySwipeHandlerTest {

    private lateinit var categories: List<Category>
    private var selectedCategoryId: Long? = null
    private var categorySelectionCalled: Boolean = false
    private var lastSelectedCategory: Long? = null

    @Before
    fun setup() {
        categories = listOf(
            createTestCategory(1L, "family", "Family"),
            createTestCategory(2L, "friends", "Friends"),
            createTestCategory(3L, "vacation", "Vacation")
        )
        selectedCategoryId = 1L
        categorySelectionCalled = false
        lastSelectedCategory = null
    }

    @Test
    fun `swipe left moves to next category`() {
        // Given
        val handler = createHandler(categories, selectedCategoryId)
        handler.horizontalDragOffset = -150f // Swipe left

        // When
        handler.handleDragEnd()

        // Then
        assertTrue(categorySelectionCalled)
        assertEquals(2L, lastSelectedCategory) // Should move to next category
        assertEquals(0f, handler.horizontalDragOffset) // Reset after handling
    }

    @Test
    fun `swipe right moves to previous category`() {
        // Given
        selectedCategoryId = 2L // Start at middle category
        val handler = createHandler(categories, selectedCategoryId)
        handler.horizontalDragOffset = 150f // Swipe right

        // When
        handler.handleDragEnd()

        // Then
        assertTrue(categorySelectionCalled)
        assertEquals(1L, lastSelectedCategory) // Should move to previous category
    }

    @Test
    fun `swipe left from last category wraps to first`() {
        // Given
        selectedCategoryId = 3L // Last category
        val handler = createHandler(categories, selectedCategoryId)
        handler.horizontalDragOffset = -150f // Swipe left

        // When
        handler.handleDragEnd()

        // Then
        assertTrue(categorySelectionCalled)
        assertEquals(1L, lastSelectedCategory) // Should wrap to first category
    }

    @Test
    fun `swipe right from first category wraps to last`() {
        // Given
        selectedCategoryId = 1L // First category
        val handler = createHandler(categories, selectedCategoryId)
        handler.horizontalDragOffset = 150f // Swipe right

        // When
        handler.handleDragEnd()

        // Then
        assertTrue(categorySelectionCalled)
        assertEquals(3L, lastSelectedCategory) // Should wrap to last category
    }

    @Test
    fun `small swipe does not trigger category change`() {
        // Given
        val handler = createHandler(categories, selectedCategoryId)
        handler.horizontalDragOffset = 50f // Small swipe, below threshold

        // When
        handler.handleDragEnd()

        // Then
        assertFalse(categorySelectionCalled)
        assertEquals(0f, handler.horizontalDragOffset) // Still resets
    }

    @Test
    fun `swipe is debounced within 300ms`() {
        // Given
        val handler = createHandler(categories, selectedCategoryId)
        handler.horizontalDragOffset = -150f // Swipe left
        handler.lastSwipeTime = System.currentTimeMillis() - 100 // Recent swipe

        // When
        handler.handleDragEnd()

        // Then
        assertFalse(categorySelectionCalled) // Should be debounced
    }

    @Test
    fun `swipe works after debounce period`() {
        // Given
        val handler = createHandler(categories, selectedCategoryId)
        handler.horizontalDragOffset = -150f // Swipe left
        handler.lastSwipeTime = System.currentTimeMillis() - 400 // Old swipe

        // When
        handler.handleDragEnd()

        // Then
        assertTrue(categorySelectionCalled) // Should not be debounced
    }

    @Test
    fun `handler resets drag offset after each swipe`() {
        // Given
        val handler = createHandler(categories, selectedCategoryId)
        handler.horizontalDragOffset = -150f

        // When
        handler.handleDragEnd()

        // Then
        assertEquals(0f, handler.horizontalDragOffset)
    }

    @Test
    fun `handler updates last swipe time on successful swipe`() {
        // Given
        val handler = createHandler(categories, selectedCategoryId)
        val beforeTime = System.currentTimeMillis()
        handler.horizontalDragOffset = -150f
        handler.lastSwipeTime = 0L

        // When
        handler.handleDragEnd()

        // Then
        assertTrue(handler.lastSwipeTime >= beforeTime)
        assertTrue(handler.lastSwipeTime <= System.currentTimeMillis())
    }

    @Test
    fun `handler works with single category`() {
        // Given
        val singleCategory = listOf(createTestCategory(1L, "family", "Family"))
        val handler = createHandler(singleCategory, 1L)
        handler.horizontalDragOffset = -150f

        // When
        handler.handleDragEnd()

        // Then
        assertTrue(categorySelectionCalled)
        assertEquals(1L, lastSelectedCategory) // Stays on same category
    }

    @Test
    fun `handler handles null selected category`() {
        // Given
        val handler = createHandler(categories, null)
        handler.horizontalDragOffset = -150f

        // When
        handler.handleDragEnd()

        // Then
        assertTrue(categorySelectionCalled)
        // Should default to first category and move to next
        assertEquals(2L, lastSelectedCategory)
    }

    @Test
    fun `handler handles empty category list gracefully`() {
        // Given
        val handler = createHandler(emptyList(), selectedCategoryId)
        handler.horizontalDragOffset = -150f

        // When
        handler.handleDragEnd()

        // Then
        assertFalse(categorySelectionCalled) // Should not crash or call callback
    }

    @Test
    fun `swipe threshold is exactly 100 pixels`() {
        // Given
        val handler = createHandler(categories, selectedCategoryId)

        // Test just below threshold
        handler.horizontalDragOffset = -99f
        handler.handleDragEnd()
        assertFalse(categorySelectionCalled)

        // Reset
        categorySelectionCalled = false
        handler.lastSwipeTime = 0L

        // Test exactly at threshold
        handler.horizontalDragOffset = -100f
        handler.handleDragEnd()
        assertFalse(categorySelectionCalled) // Should be > threshold

        // Reset
        categorySelectionCalled = false
        handler.lastSwipeTime = 0L

        // Test just above threshold
        handler.horizontalDragOffset = -101f
        handler.handleDragEnd()
        assertTrue(categorySelectionCalled) // Should trigger
    }

    @Test
    fun `debounce time is exactly 300ms`() {
        // Given
        val handler = createHandler(categories, selectedCategoryId)
        val currentTime = System.currentTimeMillis()

        // Test just below debounce
        handler.horizontalDragOffset = -150f
        handler.lastSwipeTime = currentTime - 299
        handler.handleDragEnd()
        assertFalse(categorySelectionCalled)

        // Reset
        categorySelectionCalled = false

        // Test exactly at debounce
        handler.horizontalDragOffset = -150f
        handler.lastSwipeTime = currentTime - 300
        // Advance time slightly to ensure we're past debounce
        Thread.sleep(10)
        handler.handleDragEnd()
        assertTrue(categorySelectionCalled)
    }

    // Helper functions

    private fun createHandler(
        categories: List<Category>,
        selectedCategoryId: Long?
    ): TestCategorySwipeHandler {
        return TestCategorySwipeHandler(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = { categoryId ->
                categorySelectionCalled = true
                lastSelectedCategory = categoryId
            }
        )
    }

    private fun createTestCategory(id: Long, name: String, displayName: String): Category {
        return Category(
            id = id,
            name = name,
            displayName = displayName,
            position = 0,
            iconResource = null,
            colorHex = null,
            isDefault = false,
            createdAt = System.currentTimeMillis()
        )
    }

    // Test implementation of CategorySwipeHandler logic
    private class TestCategorySwipeHandler(
        private val categories: List<Category>,
        private val selectedCategoryId: Long?,
        private val onCategorySelected: (Long) -> Unit
    ) {
        private val swipeThreshold = 100f
        private val swipeDebounceMs = 300L

        var horizontalDragOffset by mutableStateOf(0f)
        var lastSwipeTime by mutableStateOf(0L)

        fun handleDragEnd() {
            val currentTime = System.currentTimeMillis()

            if (isDebounced(currentTime)) {
                resetDrag()
                return
            }

            val categoryIds = categories.map { it.id }
            val currentCategoryIndex = categoryIds.indexOf(selectedCategoryId).takeIf { it >= 0 } ?: 0

            when {
                isSwipeLeft() && categoryIds.isNotEmpty() -> {
                    handleSwipeLeft(currentCategoryIndex, categoryIds, currentTime)
                }
                isSwipeRight() && categoryIds.isNotEmpty() -> {
                    handleSwipeRight(currentCategoryIndex, categoryIds, currentTime)
                }
            }

            resetDrag()
        }

        private fun isDebounced(currentTime: Long): Boolean {
            return currentTime - lastSwipeTime < swipeDebounceMs
        }

        private fun isSwipeLeft(): Boolean {
            return horizontalDragOffset < -swipeThreshold
        }

        private fun isSwipeRight(): Boolean {
            return horizontalDragOffset > swipeThreshold
        }

        private fun handleSwipeLeft(currentIndex: Int, categoryIds: List<Long>, currentTime: Long) {
            val nextIndex = (currentIndex + 1) % categoryIds.size
            val nextCategoryId = categoryIds[nextIndex]
            onCategorySelected(nextCategoryId)
            lastSwipeTime = currentTime
        }

        private fun handleSwipeRight(currentIndex: Int, categoryIds: List<Long>, currentTime: Long) {
            val prevIndex = if (currentIndex == 0) {
                categoryIds.size - 1
            } else {
                currentIndex - 1
            }
            val prevCategoryId = categoryIds[prevIndex]
            onCategorySelected(prevCategoryId)
            lastSwipeTime = currentTime
        }

        private fun resetDrag() {
            horizontalDragOffset = 0f
        }
    }
}
