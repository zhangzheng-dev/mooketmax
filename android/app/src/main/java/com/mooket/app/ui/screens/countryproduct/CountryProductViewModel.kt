package com.mooket.app.ui.screens.countryproduct

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mooket.app.data.api.RetrofitClient
import com.mooket.app.data.model.CountryProductDetail
import com.mooket.app.data.model.CountryProductFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CountryProductUiState(
    val isLoading: Boolean = false,
    val detail: CountryProductDetail? = null,
    val error: String? = null,
    val selectedTab: Int = 0,  // 0=报盘, 1=求购
    val selectedSort: String = "comprehensive",
    val isListRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val isTrendExpanded: Boolean = false,  // 趋势图是否展开
    val factories: List<CountryProductFactory> = emptyList()
)

class CountryProductViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CountryProductUiState())
    val uiState: StateFlow<CountryProductUiState> = _uiState.asStateFlow()

    private var currentCountry: String = ""
    private var currentProductName: String = ""
    private var currentCategory: String = ""
    private var currentPage: Int = 1

    fun loadCountryProduct(country: String, productName: String, category: String) {
        currentCountry = country
        currentProductName = productName
        currentCategory = category
        currentPage = 1

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, factories = mutableListOf())
            try {
                val type = if (_uiState.value.selectedTab == 0) "offer" else "inquiry"
                val response = RetrofitClient.apiService.getCountryProductDetail(
                    country = country,
                    productName = productName,
                    type = type,
                    category = category,
                    sortBy = _uiState.value.selectedSort,
                    page = 1
                )
                if (response.code == 200 && response.data != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = response.data,
                        factories = response.data.factories,
                        hasMorePages = response.data.page < response.data.totalPages
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }

    fun selectTab(tab: Int) {
        if (_uiState.value.selectedTab != tab) {
            _uiState.value = _uiState.value.copy(selectedTab = tab)
            // Reload data with new type
            if (currentCountry.isNotEmpty() && currentProductName.isNotEmpty()) {
                loadCountryProduct(currentCountry, currentProductName, currentCategory)
            }
        }
    }

    fun selectSort(sort: String) {
        if (_uiState.value.selectedSort != sort) {
            _uiState.value = _uiState.value.copy(selectedSort = sort)
            // Reload data with new sort
            if (currentCountry.isNotEmpty() && currentProductName.isNotEmpty()) {
                loadCountryProduct(currentCountry, currentProductName, currentCategory)
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMorePages) return

        currentPage++

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            try {
                val type = if (_uiState.value.selectedTab == 0) "offer" else "inquiry"
                val response = RetrofitClient.apiService.getCountryProductDetail(
                    country = currentCountry,
                    productName = currentProductName,
                    type = type,
                    category = currentCategory,
                    sortBy = _uiState.value.selectedSort,
                    page = currentPage
                )
                if (response.code == 200 && response.data != null) {
                    val currentFactories = _uiState.value.factories.toMutableList()
                    // 按 country+factoryNo 去重，避免后端返回重复数据导致 key 冲突
                    val existingKeys = currentFactories.map { "${it.country}_${it.factoryNo}" }.toSet()
                    val newFactories = response.data.factories.filter {
                        "${it.country}_${it.factoryNo}" !in existingKeys
                    }
                    currentFactories.addAll(newFactories)
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        factories = currentFactories,
                        hasMorePages = response.data.page < response.data.totalPages
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    fun toggleTrendExpanded() {
        _uiState.value = _uiState.value.copy(
            isTrendExpanded = !_uiState.value.isTrendExpanded
        )
    }
}
