package com.mooket.app.ui.screens.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mooket.app.data.model.*
import com.mooket.app.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 库存页面UI状态
 */
data class InventoryUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasPermission: Boolean = false,
    val isCheckingPermission: Boolean = true,
    // 数据集
    val items: List<InventoryItem> = emptyList(),
    val paramSets: List<ParamSet> = emptyList(),
    val marketPrices: List<MarketPrice> = emptyList(),
    val priceConfig: Map<String, ContainerPriceConfig> = emptyMap(),
    val deliveryDates: Map<String, String> = emptyMap(),
    // 透视表汇总
    val pivotSummaries: List<PivotSummary> = emptyList(),
    // 当前Tab
    val currentTab: InventoryTab = InventoryTab.PIVOT_STANDARD,
    // 动态库存卡片数据
    val dynamicInventoryCards: List<DynamicInventoryCardData> = emptyList()
)

enum class InventoryTab {
    PIVOT_STANDARD,  // 标准透视
    DYNAMIC_INVENTORY,  // 动态库存
    INVENTORY_DETAIL  // 库存明细
}

data class DynamicInventoryCardData(
    val containerId: String,
    val productName: String,
    val weight: Double,
    val pieces: Int,
    val currentCost: Double?,
    val estimatedProfit: Double?,
    val physicalStatus: String?,
    val country: String?,
    val factoryCode: String?,
    val spotPrice: Double?
)

/**
 * 库存页面ViewModel
 */
class InventoryViewModel(
    private val repository: InventoryRepository = InventoryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    init {
        checkPermissionAndLoad()
    }

    /**
     * 检查权限并加载数据
     */
    fun checkPermissionAndLoad() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingPermission = true, error = null) }

            // 先检查权限
            val permissionResult = repository.fetchDownloadableTypes()
            permissionResult.onSuccess { types ->
                val hasPermission = types.contains(InventoryDataTypes.INVENTORY)
                _uiState.update { it.copy(isCheckingPermission = false, hasPermission = hasPermission) }

                if (hasPermission) {
                    loadInventoryData()
                } else {
                    _uiState.update { it.copy(error = "当前账号没有库存数据查看权限") }
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isCheckingPermission = false,
                        hasPermission = false,
                        error = e.message ?: "权限检查失败"
                    )
                }
            }
        }
    }

    /**
     * 加载库存数据
     */
    fun loadInventoryData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = repository.fetchInventoryDataset()
            result.onSuccess { dataset ->
                val pivotSummaries = calculatePivotSummaries(dataset.items)
                val dynamicCards = calculateDynamicInventoryCards(dataset.items)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = dataset.items,
                        paramSets = dataset.paramSets,
                        marketPrices = dataset.marketPrices,
                        priceConfig = dataset.priceConfig,
                        deliveryDates = dataset.deliveryDates,
                        pivotSummaries = pivotSummaries,
                        dynamicInventoryCards = dynamicCards
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
            }
        }
    }

    /**
     * 计算透视表汇总数据
     */
    private fun calculatePivotSummaries(items: List<InventoryItem>): List<PivotSummary> {
        return items
            .groupBy { it.productName }
            .map { (productName, groupItems) ->
                val totalWeight = groupItems.sumOf { it.weightKg }
                val totalPieces = groupItems.sumOf { it.pieces }
                val itemCount = groupItems.size
                val costs = groupItems.mapNotNull { it.currentCostRmbPerKg }.filter { it > 0 }
                val avgCost = if (costs.isNotEmpty()) costs.average() else null
                val totalProfit = groupItems.mapNotNull { it.estimatedProfitRmb }.filter { it != 0.0 }.sum()

                PivotSummary(
                    productName = productName,
                    totalWeight = totalWeight,
                    totalPieces = totalPieces,
                    itemCount = itemCount,
                    avgCost = avgCost,
                    totalProfit = totalProfit
                )
            }
            .sortedByDescending { it.totalWeight }
    }

    /**
     * 计算动态库存卡片数据
     */
    private fun calculateDynamicInventoryCards(items: List<InventoryItem>): List<DynamicInventoryCardData> {
        return items
            .sortedByDescending { it.estimatedProfitRmb ?: 0.0 }
            .take(20)
            .map { item ->
                DynamicInventoryCardData(
                    containerId = item.containerId,
                    productName = item.productName,
                    weight = item.weightKg,
                    pieces = item.pieces,
                    currentCost = item.currentCostRmbPerKg,
                    estimatedProfit = item.estimatedProfitRmb,
                    physicalStatus = item.physicalStatus,
                    country = item.country,
                    factoryCode = item.factoryCode,
                    spotPrice = item.spotPriceRmbPerKg
                )
            }
    }

    /**
     * 切换Tab
     */
    fun selectTab(tab: InventoryTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * 获取指定产品的市场行情
     */
    fun getMarketPriceForProduct(productName: String, country: String?, factoryCode: String?): MarketPrice? {
        return _uiState.value.marketPrices.find { price ->
            price.product == productName &&
                    (country == null || price.country == country) &&
                    (factoryCode == null || price.factoryCode == factoryCode)
        }
    }
}
