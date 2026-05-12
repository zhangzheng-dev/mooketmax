package com.mooket.app.ui.screens.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mooket.app.data.model.SearchSuggest
import com.mooket.app.data.repository.MooketRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val keyword: String = "",
    val suggestions: List<SearchSuggest> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchHistory: List<String> = emptyList()
)

class SearchViewModel(private val context: Context) : ViewModel() {

    private val repository = MooketRepository()
    private val prefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadSearchHistory()
    }

    private fun loadSearchHistory() {
        viewModelScope.launch {
            repository.getRecentSearches(limit = 20)
                .onSuccess { histories ->
                    // 按实体（商家ID/品牌ID/产品ID等）去重，而非按搜索词精确去重
                    // 两个搜索词 "河南冠乐" 和 "河南冠乐(别名：xxx)" 指向同一个实体，只保留一个
                    val seenEntityIds = mutableSetOf<String>()
                    val words = histories.mapNotNull { history ->
                        val searchWord = history.searchWord
                        // 提取标准名（去掉别名后缀）
                        val standardWord = if (searchWord.contains("(别名：")) {
                            searchWord.substring(0, searchWord.indexOf("(别名："))
                        } else {
                            searchWord
                        }
                        // 用实体ID去重（商家用merchantId，产品用productId等）
                        val entityId = history.merchantId?.toString() ?: history.productId?.toString()
                            ?: history.brandId?.toString() ?: standardWord
                        if (entityId.isNotEmpty() && seenEntityIds.add(entityId)) standardWord else null
                    }.take(10)
                    _uiState.update { it.copy(searchHistory = words) }
                }
                .onFailure {
                    // 降级到本地缓存
                    val history = prefs.getStringSet("history", emptySet())?.toList() ?: emptyList()
                    _uiState.update { it.copy(searchHistory = history.take(10)) }
                }
        }
    }

    fun updateKeyword(keyword: String, category: String) {
        _uiState.update { it.copy(keyword = keyword) }

        searchJob?.cancel()
        if (keyword.isNotEmpty()) {
            searchJob = viewModelScope.launch {
                delay(300)
                search(keyword, category)
            }
        } else {
            _uiState.update { it.copy(suggestions = emptyList()) }
        }
    }

    fun clearKeyword() {
        searchJob?.cancel()
        _uiState.update { it.copy(keyword = "", suggestions = emptyList()) }
    }

    fun addToHistory(keyword: String) {
        if (keyword.isBlank()) return
        val history = prefs.getStringSet("history", emptySet())?.toMutableSet() ?: mutableSetOf()
        history.remove(keyword)
        val newHistory = listOf(keyword) + history.toList().take(9)
        prefs.edit().putStringSet("history", newHistory.toSet()).apply()
        loadSearchHistory()
    }

    /**
     * 保存搜索历史到服务器
     * @param keyword 搜索词（联想结果的完整文本）
     * @param searchType 搜索类型（产品/国家/品牌/商家/国家+产品/国家+厂号+产品等）
     * @param isSelfSelect 是否自选（0-否，1-是）
     * @param productId 产品ID
     * @param productName 产品名称
     * @param country 国家
     * @param factoryNo 厂号
     * @param brandId 品牌ID
     * @param merchantId 商家ID
     */
    fun saveSearchHistoryToServer(
        keyword: String,
        searchType: String,
        isSelfSelect: Int = 0,
        productId: Long? = null,
        productName: String? = null,
        country: String? = null,
        factoryNo: String? = null,
        brandId: Long? = null,
        merchantId: Long? = null
    ) {
        if (keyword.isBlank()) return
        viewModelScope.launch {
            try {
                val result = repository.saveSearchHistory(
                    keyword, searchType, isSelfSelect,
                    productId, productName, country, factoryNo, brandId, merchantId
                )
                result.onFailure { e ->
                    android.util.Log.e("SearchViewModel", "保存搜索历史失败: keyword=$keyword, type=$searchType, error=${e.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "保存搜索历史异常: keyword=$keyword, type=$searchType, error=${e.message}")
            }
        }
    }

    fun clearHistory() {
        prefs.edit().remove("history").apply()
        _uiState.update { it.copy(searchHistory = emptyList()) }
    }

    private suspend fun search(keyword: String, category: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        val result = repository.getSearchSuggestions(category, keyword)
        val apiSuggestions = result.getOrNull() ?: emptyList()
        _uiState.update { it.copy(isLoading = false, suggestions = apiSuggestions) }
    }

}

class SearchViewModelFactory(private val context: Context) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SearchViewModel(context) as T
    }
}
