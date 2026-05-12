package com.mooket.app.ui.screens.countryfactoryproduct

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mooket.app.data.api.RetrofitClient
import com.mooket.app.data.model.CountryFactoryProductDetail
import com.mooket.app.data.model.MerchantOfferGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 国家+厂号+产品详情 ViewModel
 */
class CountryFactoryProductViewModel : ViewModel() {

    private val apiService = RetrofitClient.apiService

    private val _uiState = MutableStateFlow(CountryFactoryProductUiState())
    val uiState: StateFlow<CountryFactoryProductUiState> = _uiState.asStateFlow()

    // 缓存的 productId，用于导航
    private var cachedProductId: Int? = null

    // 缓存当前加载参数，用于切换tab时重新加载
    private var currentCountry: String = ""
    private var currentFactoryNo: String = ""
    private var currentProductName: String = ""
    private var currentCategory: String = "牛"
    private var currentOfferTypeForApi: String = "offer"  // 追踪上次 API 调用使用的 offerType

    /**
     * 加载数据
     */
    fun loadData(country: String, factoryNo: String, productName: String, category: String?) {
        currentCountry = country
        currentFactoryNo = factoryNo
        currentProductName = productName
        currentCategory = category ?: "牛"

        // 判断是否是同参数重复调用（Compose 重组触发），避免重复设置 loading 状态导致 UI 抖动
        // 注意：只判断参数是否一致，不判断数据是否非空——因为 merchantOffers 为空的产品（如新上线）
        // 仍然是有意义的数据，不应该因为空列表就重复触发 loading
        // 修复：同时判断 currentOfferTypeForApi，否则切换报盘/求购 tab 时 API 被错误跳过
        val existingDetail = _uiState.value.detail
        val isSameParams = existingDetail != null &&
                existingDetail!!.country == country &&
                existingDetail!!.factoryNo == factoryNo &&
                existingDetail!!.productName == productName &&
                currentOfferTypeForApi == _uiState.value.offerType

        viewModelScope.launch {
            // 同参数：跳过 loading 状态，直接用缓存数据
            // 不同参数或无缓存：显示 loading 并请求网络
            _uiState.value = _uiState.value.copy(
                isLoading = !isSameParams,
                error = null,
                category = currentCategory
            )

            // 同参数且有缓存数据：跳过网络请求，直接复用
            if (isSameParams) {
                cachedProductId = existingDetail!!.productId ?: cachedProductId
                return@launch
            }

            try {
                val response = apiService.getCountryFactoryProductDetail(
                    country = country,
                    factoryNo = factoryNo,
                    productName = productName,
                    type = _uiState.value.offerType,
                    category = currentCategory,
                    sortBy = _uiState.value.sortBy,
                    page = _uiState.value.page,
                    pageSize = 20
                )

                if (response.code == 200 && response.data != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = response.data,
                        error = null
                    )
                    // 缓存 productId（用于标签点击跳转）
                    response.data.productId?.let { cachedProductId = it }
                    // 更新当前 API 调用使用的 offerType
                    currentOfferTypeForApi = _uiState.value.offerType
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

    /**
     * 获取缓存的 productId
     */
    fun getCachedProductId(): Int? = cachedProductId

    /**
     * 切换报盘/求购
     */
    fun switchOfferType(type: String) {
        if (_uiState.value.offerType != type) {
            _uiState.value = _uiState.value.copy(offerType = type, page = 1)
            // 重新加载数据
            reloadData()
        }
    }

    /**
     * 切换排序方式（只刷新列表，不刷新上面看板）
     */
    fun switchSortBy(sortBy: String) {
        val currentSort = _uiState.value.sortBy
        val newSort = when (sortBy) {
            "comprehensive" -> "comprehensive"
            "publish_time" -> "publish_time"
            "price" -> {
                // 价格排序：综合推荐 → 升序 → 降序 → 综合推荐
                when (currentSort) {
                    "comprehensive" -> "price_asc"
                    "price_asc" -> "price_desc"
                    "price_desc" -> "comprehensive"
                    else -> "price_asc"
                }
            }
            else -> sortBy
        }
        if (currentSort != newSort) {
            _uiState.value = _uiState.value.copy(sortBy = newSort, page = 1)
            // 只刷新列表
            refreshList()
        }
    }

    /**
     * 只刷新列表数据（不刷新上面看板区域）
     */
    private fun refreshList() {
        if (currentCountry.isEmpty() || currentFactoryNo.isEmpty() || currentProductName.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isListLoading = true)

            try {
                val response = apiService.getCountryFactoryProductDetail(
                    country = currentCountry,
                    factoryNo = currentFactoryNo,
                    productName = currentProductName,
                    type = _uiState.value.offerType,
                    category = currentCategory,
                    sortBy = _uiState.value.sortBy,
                    page = 1,  // 排序从头开始
                    pageSize = 20
                )

                if (response.code == 200 && response.data != null) {
                    _uiState.value = _uiState.value.copy(
                        isListLoading = false,
                        detail = response.data,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isListLoading = false,
                        error = response.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isListLoading = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * 重新加载数据（使用当前缓存的参数）
     */
    private fun reloadData() {
        if (currentCountry.isNotEmpty() && currentFactoryNo.isNotEmpty() && currentProductName.isNotEmpty()) {
            loadData(currentCountry, currentFactoryNo, currentProductName, currentCategory)
        }
    }

    /**
     * 切换展开/收起趋势图
     */
    fun toggleTrend() {
        _uiState.value = _uiState.value.copy(isTrendExpanded = !_uiState.value.isTrendExpanded)
    }

    /**
     * 切换标签筛选
     */
    fun toggleFilter(filter: String) {
        val current = _uiState.value.activeFilters
        val updated = if (current.contains(filter)) {
            current - filter
        } else {
            current + filter
        }
        _uiState.value = _uiState.value.copy(activeFilters = updated)
    }

    /**
     * 设置价格区间筛选
     */
    fun setPriceRange(min: String?, max: String?) {
        _uiState.value = _uiState.value.copy(priceMin = min, priceMax = max)
    }

    /**
     * 切换货物类型筛选
     */
    fun toggleGoodsType(type: String) {
        val current = _uiState.value.goodsTypes
        val updated = if (current.contains(type)) {
            current - type
        } else {
            current + type
        }
        _uiState.value = _uiState.value.copy(goodsTypes = updated)
    }

    /**
     * 切换饲养方式筛选
     */
    fun toggleFeedingType(type: String) {
        val current = _uiState.value.feedingTypes
        val updated = if (current.contains(type)) {
            current - type
        } else {
            current + type
        }
        _uiState.value = _uiState.value.copy(feedingTypes = updated)
    }

    /**
     * 切换标签筛选
     */
    fun toggleTag(tag: String) {
        val current = _uiState.value.tags
        val updated = if (current.contains(tag)) {
            current - tag
        } else {
            current + tag
        }
        _uiState.value = _uiState.value.copy(tags = updated)
    }

    /**
     * 清除所有筛选
     */
    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            activeFilters = emptySet(),
            priceMin = null,
            priceMax = null,
            goodsTypes = emptySet(),
            feedingTypes = emptySet(),
            tags = emptySet(),
            isFamousMerchant = false,
            selectedMerchants = emptySet(),
            regions = emptySet()
        )
    }

    /**
     * 切换知名商家筛选
     */
    fun toggleFamousMerchant() {
        _uiState.value = _uiState.value.copy(isFamousMerchant = !_uiState.value.isFamousMerchant)
    }

    /**
     * 切换商家筛选
     */
    fun toggleMerchant(merchantId: Long) {
        val current = _uiState.value.selectedMerchants
        val updated = if (current.contains(merchantId)) {
            current - merchantId
        } else {
            current + merchantId
        }
        _uiState.value = _uiState.value.copy(selectedMerchants = updated)
    }

    /**
     * 切换地区筛选
     */
    fun toggleRegion(region: String) {
        val current = _uiState.value.regions
        val updated = if (current.contains(region)) {
            current - region
        } else {
            current + region
        }
        _uiState.value = _uiState.value.copy(regions = updated)
    }

    /**
     * 加载更多
     */
    fun loadMore() {
        val detail = _uiState.value.detail ?: return
        if (_uiState.value.isLoadingMore) return
        if (detail.page >= detail.totalPages) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            try {
                val nextPage = detail.page + 1
                val response = apiService.getCountryFactoryProductDetail(
                    country = _uiState.value.detail!!.country,
                    factoryNo = _uiState.value.detail!!.factoryNo,
                    productName = _uiState.value.detail!!.productName,
                    type = _uiState.value.offerType,
                    category = _uiState.value.category,
                    sortBy = _uiState.value.sortBy,
                    page = nextPage,
                    pageSize = 20
                )

                if (response.code == 200 && response.data != null) {
                    // 合并新旧分组的报盘列表，相同商家的分组需要合并
                    val mergedGroups = (detail.merchantOffers + response.data.merchantOffers)
                        .groupBy { it.merchantId }
                        .map { (_, groups) ->
                            if (groups.size == 1) {
                                groups.first()
                            } else {
                                // 合并同商家的多个分组（累加报盘数，合并员工报价列表）
                                groups.reduce { acc, g ->
                                    MerchantOfferGroup(
                                        merchantId = acc.merchantId,
                                        merchantName = acc.merchantName,
                                        merchantPhone = acc.merchantPhone,
                                        offerCount = acc.offerCount + g.offerCount,
                                        employeeOffers = acc.employeeOffers + g.employeeOffers
                                    )
                                }
                            }
                        }
                        .sortedWith(
                            when (_uiState.value.sortBy) {
                                "price_asc" -> compareBy { group -> group.employeeOffers.minOfOrNull { it.price?.replace(Regex("[^\\d.]"), "")?.toDoubleOrNull() ?: Double.MAX_VALUE } ?: Double.MAX_VALUE }
                                "price_desc" -> compareByDescending { group -> group.employeeOffers.minOfOrNull { it.price?.replace(Regex("[^\\d.]"), "")?.toDoubleOrNull() ?: -1.0 } ?: -1.0 }
                                "publish_time" -> compareByDescending { it.employeeOffers.maxOfOrNull { o -> o.publishTime ?: "" } }
                                else -> compareByDescending { it.offerCount }
                            }
                        )

                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        detail = response.data.copy(merchantOffers = mergedGroups),
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        error = response.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }
}

/**
 * UI 状态
 */
data class CountryFactoryProductUiState(
    val isLoading: Boolean = true,         // 初始加载（全屏遮罩）
    val isListLoading: Boolean = false,    // 列表刷新（排序/筛选）
    val isLoadingMore: Boolean = false,    // 加载更多（翻页，底部小loading）
    val detail: CountryFactoryProductDetail? = null,
    val error: String? = null,
    val offerType: String = "offer",      // offer / inquiry
    val sortBy: String = "comprehensive", // comprehensive / publish_time / price_asc / price_desc
    val page: Int = 1,
    val isTrendExpanded: Boolean = false,
    val activeFilters: Set<String> = emptySet(),
    val category: String = "牛",
    // 价格区间筛选
    val priceMin: String? = null,
    val priceMax: String? = null,
    // 货物类型筛选（现货、半期货、期货）
    val goodsTypes: Set<String> = emptySet(),
    // 饲养方式筛选（草饲、谷饲）
    val feedingTypes: Set<String> = emptySet(),
    // 标签筛选
    val tags: Set<String> = emptySet(),
    // 知名商家筛选
    val isFamousMerchant: Boolean = false,
    // 商家筛选（merchantId 列表）
    val selectedMerchants: Set<Long> = emptySet(),
    // 地区筛选
    val regions: Set<String> = emptySet()
) {
    /**
     * 过滤后的商家报盘列表（由 detail.merchantOffers + 筛选条件派生）
     */
    val filteredMerchantOffers: List<MerchantOfferGroup>
        get() {
            val offers = detail?.merchantOffers ?: return emptyList()
            val minPrice = priceMin?.toDoubleOrNull()
            val maxPrice = priceMax?.toDoubleOrNull()

            return offers.mapNotNull { group ->
                val filteredEmployees = group.employeeOffers.filter { offer ->
                    // 价格区间筛选
                    if (minPrice != null || maxPrice != null) {
                        val price = offer.price.replace(Regex("[^\\d.]"), "").toDoubleOrNull()
                        if (price != null) {
                            if (minPrice != null && price < minPrice) false
                            else if (maxPrice != null && price > maxPrice) false
                            else true
                        } else false
                    } else true

                    // 货物类型筛选
                    if (goodsTypes.isNotEmpty()) {
                        val goodsType = offer.goodsType ?: return@filter false
                        if (goodsType !in goodsTypes) false else true
                    } else true

                    // 标签筛选
                    if (tags.isNotEmpty()) {
                        val offerTags = offer.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                        if (tags.none { it in offerTags }) false else true
                    } else true

                    // 知名商家筛选（使用 group.isFamousMerchant，不是 offer.isFamousMerchant）
                    if (isFamousMerchant) {
                        if (!group.isFamousMerchant) false else true
                    } else true

                    // 商家筛选
                    if (selectedMerchants.isNotEmpty()) {
                        if (group.merchantId !in selectedMerchants) false else true
                    } else true

                    // 地区筛选
                    if (regions.isNotEmpty()) {
                        val region = offer.goodsLocation ?: return@filter false
                        if (region !in regions) false else true
                    } else true
                }

                if (filteredEmployees.isNotEmpty()) {
                    group.copy(employeeOffers = filteredEmployees)
                } else {
                    null
                }
            }
        }
}
