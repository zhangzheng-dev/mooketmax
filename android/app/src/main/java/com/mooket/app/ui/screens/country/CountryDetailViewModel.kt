package com.mooket.app.ui.screens.country

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mooket.app.data.model.CountryDetail
import com.mooket.app.data.model.CountryProductSummary
import com.mooket.app.data.repository.MooketRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CountryDetailUiState(
    val isLoading: Boolean = false,
    val countryDetail: CountryDetail? = null,
    val error: String? = null,
    val selectedTab: Int = 0,  // 0: 报盘, 1: 求购
    val selectedSort: String = "comprehensive",  // comprehensive, price_asc, price_desc
    // 列表刷新状态（用于排序切换时只刷新列表，不刷新整个页面）
    val isListRefreshing: Boolean = false,
    // 分页相关
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 1,
    val pageSize: Int = 10,
    val currentSummaries: List<CountryProductSummary> = emptyList(),
    val hasMorePages: Boolean = false
)

class CountryDetailViewModel : ViewModel() {

    private val repository = MooketRepository()

    private val _uiState = MutableStateFlow(CountryDetailUiState())
    val uiState: StateFlow<CountryDetailUiState> = _uiState.asStateFlow()

    private var currentCountry: String = ""
    private var currentCategory: String = ""

    fun loadCountryDetail(country: String, category: String) {
        currentCountry = country
        currentCategory = category

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val type = if (_uiState.value.selectedTab == 0) "offer" else "inquiry"
            val sortBy = _uiState.value.selectedSort
            val result = repository.getCountryDetail(
                country = country,
                category = category,
                type = type,
                sortBy = sortBy,
                page = 1,
                pageSize = _uiState.value.pageSize
            )

            result.onSuccess { detail ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        countryDetail = detail,
                        currentPage = 1,
                        currentSummaries = detail.summaries,
                        hasMorePages = 1 < detail.totalPages
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun loadMoreSummaries() {
        val state = _uiState.value
        state.countryDetail ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, error = null) }

            val type = if (state.selectedTab == 0) "offer" else "inquiry"
            val nextPage = state.currentPage + 1
            val sortBy = state.selectedSort

            repository.getCountryDetail(
                country = currentCountry,
                category = currentCategory,
                type = type,
                sortBy = sortBy,
                page = nextPage,
                pageSize = state.pageSize
            ).onSuccess { newDetail ->
                _uiState.update {
                    val combinedSummaries = it.currentSummaries + newDetail.summaries
                    it.copy(
                        isLoadingMore = false,
                        countryDetail = newDetail,
                        currentPage = nextPage,
                        currentSummaries = combinedSummaries,
                        hasMorePages = nextPage < newDetail.totalPages
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoadingMore = false, error = e.message) }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMorePages) return
        loadMoreSummaries()
    }

    fun selectTab(tab: Int) {
        if (_uiState.value.selectedTab != tab) {
            _uiState.update {
                it.copy(
                    selectedTab = tab,
                    countryDetail = null,
                    currentPage = 1,
                    currentSummaries = emptyList(),
                    hasMorePages = false
                )
            }
            loadCountryDetail(currentCountry, currentCategory)
        }
    }

    fun selectSort(sort: String) {
        val currentSort = _uiState.value.selectedSort
        val newSort = when (sort) {
            "comprehensive" -> "comprehensive"
            "price" -> {
                // 价格排序：综合推荐 -> 升序 -> 降序 -> 综合推荐（取消）
                when (currentSort) {
                    "comprehensive" -> "price_asc"
                    "price_asc" -> "price_desc"
                    "price_desc" -> "comprehensive"
                    else -> "price_asc"
                }
            }
            else -> sort
        }
        if (currentSort != newSort) {
            _uiState.update {
                it.copy(
                    selectedSort = newSort,
                    isListRefreshing = true,
                    currentPage = 1,
                    currentSummaries = emptyList(),
                    hasMorePages = false
                )
            }
            reloadSummaries()
        }
    }

    private fun reloadSummaries() {
        val state = _uiState.value
        viewModelScope.launch {
            val type = if (state.selectedTab == 0) "offer" else "inquiry"
            val sortBy = state.selectedSort

            repository.getCountryDetail(
                country = currentCountry,
                category = currentCategory,
                type = type,
                sortBy = sortBy,
                page = 1,
                pageSize = state.pageSize
            ).onSuccess { detail ->
                _uiState.update {
                    it.copy(
                        isListRefreshing = false,
                        countryDetail = detail,
                        currentPage = 1,
                        currentSummaries = detail.summaries,
                        hasMorePages = 1 < detail.totalPages
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isListRefreshing = false, error = e.message) }
            }
        }
    }
}
