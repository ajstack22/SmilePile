package com.smilepile.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smilepile.data.models.Category
import com.smilepile.data.models.Photo
import com.smilepile.data.repository.CategoryRepository
import com.smilepile.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filter states
    private val _selectedDateRange = MutableStateFlow<DateRange?>(null)
    val selectedDateRange: StateFlow<DateRange?> = _selectedDateRange.asStateFlow()

    private val _favoritesOnly = MutableStateFlow(false)
    val favoritesOnly: StateFlow<Boolean> = _favoritesOnly.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    // UI state
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private val _showFilters = MutableStateFlow(false)
    val showFilters: StateFlow<Boolean> = _showFilters.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Get all categories for filtering
    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategoriesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Debounced search query to avoid too many API calls
    @OptIn(FlowPreview::class)
    private val debouncedSearchQuery = _searchQuery
        .debounce(300) // Wait 300ms after user stops typing
        .distinctUntilChanged()

    // Combined search filters
    private val searchFilters = combine(
        debouncedSearchQuery,
        _selectedDateRange,
        _favoritesOnly,
        _selectedCategoryId
    ) { query, dateRange, favoritesOnly, categoryId ->
        SearchFilters(
            query = query.trim(),
            dateRange = dateRange,
            favoritesOnly = if (favoritesOnly) true else null,
            categoryId = categoryId
        )
    }

    // Search results based on filters
    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<Photo>> = searchFilters
        .flatMapLatest { filters ->
            _isSearching.value = true

            val startDate = filters.dateRange?.startDate ?: 0L
            val endDate = filters.dateRange?.endDate ?: Long.MAX_VALUE

            photoRepository.searchPhotosWithFilters(
                searchQuery = filters.query,
                startDate = startDate,
                endDate = endDate,
                favoritesOnly = filters.favoritesOnly,
                categoryId = filters.categoryId
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Combine filters
    private val combinedFilters = combine(
        selectedDateRange,
        favoritesOnly,
        selectedCategoryId
    ) { dateRange, favoritesOnly, categoryId ->
        Triple(dateRange, favoritesOnly, categoryId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Triple(null, false, null)
    )

    // Combine search state
    private val combinedSearchState = combine(
        searchQuery,
        searchResults,
        isSearching,
        searchHistory,
        showFilters
    ) { query, results, isSearching, history, showFilters ->
        data class SearchState(
            val query: String,
            val results: List<Photo>,
            val isSearching: Boolean,
            val history: List<String>,
            val showFilters: Boolean
        )
        SearchState(query, results, isSearching, history, showFilters)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Combined UI state
    val uiState: StateFlow<SearchUiState> = combine(
        combinedSearchState,
        combinedFilters,
        error,
        categories
    ) { searchState, filters, error, categories ->
        val (dateRange, favoritesOnly, categoryId) = filters
        SearchUiState(
            searchQuery = searchState?.query ?: "",
            searchResults = searchState?.results ?: emptyList(),
            selectedDateRange = dateRange,
            favoritesOnly = favoritesOnly,
            selectedCategoryId = categoryId,
            isSearching = searchState?.isSearching ?: false,
            searchHistory = searchState?.history ?: emptyList(),
            showFilters = searchState?.showFilters ?: false,
            error = error,
            categories = categories,
            hasFilters = dateRange != null || favoritesOnly || categoryId != null,
            resultsCount = searchState?.results?.size ?: 0
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState()
    )

    init {
        // Load search history from preferences
        loadSearchHistory()

        // Monitor search results to update loading state
        viewModelScope.launch {
            searchResults.collect {
                _isSearching.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query

        // Add to search history if query is meaningful
        if (query.trim().length >= 2) {
            addToSearchHistory(query.trim())
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        clearAllFilters()
    }

    fun setDateRange(dateRange: DateRange?) {
        _selectedDateRange.value = dateRange
    }

    fun setFavoritesOnly(favoritesOnly: Boolean) {
        _favoritesOnly.value = favoritesOnly
    }

    fun setCategoryFilter(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun toggleFilters() {
        _showFilters.value = !_showFilters.value
    }

    fun hideFilters() {
        _showFilters.value = false
    }

    fun clearAllFilters() {
        _selectedDateRange.value = null
        _favoritesOnly.value = false
        _selectedCategoryId.value = null
    }

    fun selectSearchHistoryItem(query: String) {
        _searchQuery.value = query
    }

    fun removeFromSearchHistory(query: String) {
        val currentHistory = _searchHistory.value.toMutableList()
        currentHistory.remove(query)
        _searchHistory.value = currentHistory
        saveSearchHistory()
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
        saveSearchHistory()
    }

    // Date range presets
    fun setDateRangePreset(preset: DateRangePreset) {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis

        val startDate = when (preset) {
            DateRangePreset.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            DateRangePreset.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            DateRangePreset.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            DateRangePreset.THIS_YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            DateRangePreset.LAST_WEEK -> {
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                start
            }
            DateRangePreset.LAST_MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                start
            }
        }

        _selectedDateRange.value = DateRange(
            startDate = startDate,
            endDate = if (preset == DateRangePreset.LAST_WEEK || preset == DateRangePreset.LAST_MONTH) {
                calendar.timeInMillis
            } else {
                endDate
            },
            preset = preset
        )
    }

    private fun addToSearchHistory(query: String) {
        val currentHistory = _searchHistory.value.toMutableList()

        // Remove if already exists to avoid duplicates
        currentHistory.remove(query)

        // Add to the beginning of the list
        currentHistory.add(0, query)

        // Keep only the last 10 searches
        if (currentHistory.size > 10) {
            currentHistory.removeAt(currentHistory.size - 1)
        }

        _searchHistory.value = currentHistory
        saveSearchHistory()
    }

    private fun loadSearchHistory() {
        // TODO: Load from SharedPreferences or local storage
        // For now, using empty list
        _searchHistory.value = emptyList()
    }

    private fun saveSearchHistory() {
        // TODO: Save to SharedPreferences or local storage
        // For now, just keeping in memory
    }

    fun clearError() {
        _error.value = null
    }
}

// Data classes for search functionality
data class SearchFilters(
    val query: String = "",
    val dateRange: DateRange? = null,
    val favoritesOnly: Boolean? = null,
    val categoryId: Long? = null
)

data class DateRange(
    val startDate: Long,
    val endDate: Long,
    val preset: DateRangePreset? = null
) {
    fun getDisplayText(): String {
        return preset?.displayName ?: "Custom Range"
    }
}

enum class DateRangePreset(val displayName: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year"),
    LAST_WEEK("Last Week"),
    LAST_MONTH("Last Month")
}

data class SearchUiState(
    val searchQuery: String = "",
    val searchResults: List<Photo> = emptyList(),
    val selectedDateRange: DateRange? = null,
    val favoritesOnly: Boolean = false,
    val selectedCategoryId: Long? = null,
    val isSearching: Boolean = false,
    val searchHistory: List<String> = emptyList(),
    val showFilters: Boolean = false,
    val error: String? = null,
    val categories: List<Category> = emptyList(),
    val hasFilters: Boolean = false,
    val resultsCount: Int = 0
) {
    val isSearchActive: Boolean
        get() = searchQuery.isNotEmpty() || hasFilters

    val showSearchResults: Boolean
        get() = isSearchActive && !isSearching

    val showEmptyState: Boolean
        get() = isSearchActive && !isSearching && searchResults.isEmpty()
}