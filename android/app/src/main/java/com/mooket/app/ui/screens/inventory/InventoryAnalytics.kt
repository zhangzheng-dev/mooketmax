package com.mooket.app.ui.screens.inventory

import com.mooket.app.data.model.ContainerPriceConfig
import com.mooket.app.data.model.InventoryDataset
import com.mooket.app.data.model.InventoryItem
import com.mooket.app.data.model.MarketPrice
import com.mooket.app.data.model.ParamSet
import com.mooket.app.data.model.TieredRate
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val DEFAULT_DEPOSIT_AMOUNT = 200000.0
private const val DEFAULT_DELIVERY_DELAY_DAYS = 5L

enum class DynamicGroupBy(val label: String) {
    PRODUCT("品名"),
    FUNDER("资金方"),
    COUNTRY("国家"),
    STATUS("状态"),
    FACTORY("厂号"),
    CUSTOM_GROUP("自定义分组")
}

enum class DetailSortKey(val label: String) {
    PRODUCTION_DATE("生产日期"),
    RECEIVABLE("应收账款"),
    PROFIT("盈利"),
    DAILY_COST("每日成本"),
    RECOVERABLE_CASH("可回现金"),
    TOTAL_COST("总成本"),
    TOTAL_WEIGHT("总重量")
}

enum class SortDirection {
    ASC, DESC
}

data class InventoryAnalyticsResult(
    val products: List<InventoryPivotProduct>,
    val summary: InventoryPivotSummary,
    val detailRows: List<ResolvedInventoryRow>
)

data class InventoryPivotSummary(
    val totalProducts: Int = 0,
    val totalWeight: Double = 0.0,
    val totalPieces: Int = 0,
    val totalItems: Int = 0,
    val totalContainers: Int = 0,
    val totalOccupiedCash: Double = 0.0,
    val totalFloatingPnL: Double = 0.0,
    val totalRecoverableCash: Double = 0.0,
    val totalNetCashBefore: Double = 0.0,
    val totalDailyBurn: Double = 0.0,
    val watchedProducts: Int = 0,
    val estimatedProfit: Double = 0.0
)

data class InventoryPivotProduct(
    val productName: String,
    val totalWeight: Double,
    val totalPieces: Int,
    val containers: Int,
    val weightedAvgCost: Double,
    val pendingWeight: Double,
    val transitWeight: Double,
    val inStockWeight: Double,
    val occupiedCash: Double,
    val floatingPnL: Double,
    val recoverableCash: Double,
    val netCashBefore: Double,
    val dailyBurn: Double,
    val factories: List<InventoryFactoryDetail>
)

data class InventoryFactoryDetail(
    val factoryCode: String,
    val country: String?,
    val weightKg: Double,
    val pieces: Int,
    val containers: Int,
    val pendingWeight: Double,
    val transitWeight: Double,
    val inStockWeight: Double,
    val occupiedCash: Double,
    val floatingPnL: Double,
    val recoverableCash: Double,
    val avgCost: Double,
    val avgStorageAgeDays: Double?,
    val coldStorage: String?
)

data class ResolvedInventoryRow(
    val id: String,
    val contractId: String,
    val productName: String,
    val containerId: String,
    val skuCode: String,
    val factoryCode: String,
    val country: String,
    val coldStorage: String,
    val funder: String,
    val customGroup: String,
    val pieces: Int,
    val weightKg: Double,
    val costPerKg: Double,
    val sellingPricePerKg: Double,
    val dailyCost: Double,
    val recoverableCash: Double,
    val profit: Double,
    val productionDate: String,
    val status: String,
    val occupiedCash: Double
)

data class DynamicInventorySummary(
    val groupCount: Int,
    val containerCount: Int,
    val totalWeight: Double,
    val averageCost: Double,
    val totalProfit: Double,
    val totalOccupiedCash: Double
)

data class DynamicInventoryGroupCard(
    val key: String,
    val titleLines: List<Pair<String, String>>,
    val alertLabel: String,
    val containerCount: Int,
    val fundRatio: Double,
    val lossContainers: Int,
    val profitContainers: Int,
    val averageCost: Double,
    val occupiedCash: Double,
    val profit: Double,
    val avgAge: Int?,
    val roi: Double,
    val insight: String,
    val minCost: Double,
    val avgCost: Double,
    val maxCost: Double
)

private data class EnrichedInventoryItem(
    val source: InventoryItem,
    val status: String,
    val storageDays: Long,
    val currentCostPerKg: Double,
    val marketPricePerKg: Double,
    val receivable: Double,
    val netCashBefore: Double,
    val netCashAfter: Double,
    val profit: Double,
    val dailyCost: Double,
    val occupiedCash: Double
)

object InventoryAnalytics {
    private val defaultParamSet = ParamSet(
        parameterSetId = 1,
        parameterSetName = "标准配置",
        factoryPrepaymentRatio = 0.4,
        clientOwnFundRatio = 0.2,
        funderAdvanceRatio = 0.2,
        prepaymentInterestRate = 0.065,
        transitPaymentInterestRate = 0.065,
        taxPaymentInterestRate = 0.065,
        storageCostPerTonDay = 2.2,
        tariffRate = 0.12,
        vatRate = 0.09,
        prepaymentFeePerContainer = 500.0,
        miscCostPerContainer = 500.0,
        agentFeePerContainer = 5500.0,
        procurementCostPerTon = 150.0,
        warehouseCostPerTon = 60.0,
        insuranceFeeRate = 0.0008,
        defaultFxUsdCny = 7.0,
        prepaymentTieredRates = null,
        transitPaymentTieredRates = null,
        taxPaymentTieredRates = null
    )

    fun analyze(dataset: InventoryDataset, searchQuery: String = ""): InventoryAnalyticsResult {
        val valuationDate = LocalDate.now().plusDays(1)
        val context = InventoryContext(dataset, valuationDate)
        val enriched = dataset.items
            .map { enrichItem(it, context) }
            .filter { isValidPivotItem(it.source) && it.source.isDeleted != 1 }
            .filter { matchesQuery(it, searchQuery) }

        val products = enriched
            .groupBy { it.source.productName.ifBlank { "未知品名" } }
            .map { (productName, group) ->
                val productSummary = summarizeItems(group)
                val factories = group
                    .groupBy { it.source.factoryCode ?: "未知厂号" }
                    .map { (factoryCode, factoryGroup) ->
                        val s = summarizeItems(factoryGroup)
                        val ages = factoryGroup.mapNotNull { inventoryAgeDays(it.source, valuationDate) }
                        InventoryFactoryDetail(
                            factoryCode = factoryCode,
                            country = factoryGroup.firstOrNull()?.source?.country,
                            weightKg = s.totalWeight,
                            pieces = s.totalPieces,
                            containers = factoryGroup.map { it.source.containerId }.filter { it.isNotBlank() }.toSet().size,
                            pendingWeight = s.pendingWeight,
                            transitWeight = s.transitWeight,
                            inStockWeight = s.inStockWeight,
                            occupiedCash = s.occupiedCash,
                            floatingPnL = s.floatingPnL,
                            recoverableCash = s.recoverableCash,
                            avgCost = s.weightedAvgCost,
                            avgStorageAgeDays = ages.takeIf { it.isNotEmpty() }?.average(),
                            coldStorage = factoryGroup.firstOrNull()?.source?.coldStorage
                        )
                    }
                    .sortedByDescending { it.weightKg }

                InventoryPivotProduct(
                    productName = productName,
                    totalWeight = productSummary.totalWeight,
                    totalPieces = productSummary.totalPieces,
                    containers = group.map { it.source.containerId }.filter { it.isNotBlank() }.toSet().size,
                    weightedAvgCost = productSummary.weightedAvgCost,
                    pendingWeight = productSummary.pendingWeight,
                    transitWeight = productSummary.transitWeight,
                    inStockWeight = productSummary.inStockWeight,
                    occupiedCash = productSummary.occupiedCash,
                    floatingPnL = productSummary.floatingPnL,
                    recoverableCash = productSummary.recoverableCash,
                    netCashBefore = productSummary.netCashBefore,
                    dailyBurn = productSummary.dailyBurn,
                    factories = factories
                )
            }
            .sortedByDescending { it.totalWeight }

        val summary = InventoryPivotSummary(
            totalProducts = products.size,
            totalWeight = products.sumOf { it.totalWeight },
            totalPieces = products.sumOf { it.totalPieces },
            totalItems = enriched.size,
            totalContainers = enriched.map { it.source.containerId }.filter { it.isNotBlank() }.toSet().size,
            totalOccupiedCash = products.sumOf { it.occupiedCash },
            totalFloatingPnL = products.sumOf { it.floatingPnL },
            totalRecoverableCash = products.sumOf { it.recoverableCash },
            totalNetCashBefore = products.sumOf { it.netCashBefore },
            totalDailyBurn = products.sumOf { it.dailyBurn },
            watchedProducts = enriched.count { hasExactPriceConfig(it.source, context) },
            estimatedProfit = products.sumOf { it.floatingPnL }
        )

        val rows = enriched.map { item ->
            ResolvedInventoryRow(
                id = item.source.inventorySkuId,
                contractId = item.source.supplierContractId ?: "--",
                productName = item.source.productName,
                containerId = item.source.containerId,
                skuCode = item.source.skuCode ?: "--",
                factoryCode = item.source.factoryCode ?: "--",
                country = item.source.country ?: "--",
                coldStorage = item.source.coldStorage ?: "--",
                funder = item.source.funderId ?: "--",
                customGroup = item.source.customGroup ?: "--",
                pieces = item.source.pieces,
                weightKg = itemWeight(item.source),
                costPerKg = item.currentCostPerKg,
                sellingPricePerKg = item.marketPricePerKg,
                dailyCost = item.dailyCost,
                recoverableCash = item.netCashAfter,
                profit = item.profit,
                productionDate = item.source.productionDate ?: "--",
                status = item.status,
                occupiedCash = item.occupiedCash
            )
        }

        return InventoryAnalyticsResult(products = products, summary = summary, detailRows = rows)
    }

    fun buildDynamicSummary(rows: List<ResolvedInventoryRow>, groupBys: Set<DynamicGroupBy>): DynamicInventorySummary {
        val totalWeight = rows.sumOf { it.weightKg }
        val totalCost = rows.sumOf { it.costPerKg * it.weightKg }
        val occupied = rows.sumOf { it.occupiedCash }
        val profit = rows.sumOf { it.profit }
        return DynamicInventorySummary(
            groupCount = rows.map { dynamicKey(it, groupBys) }.toSet().size,
            containerCount = rows.map { it.containerId }.toSet().size,
            totalWeight = totalWeight,
            averageCost = safeDiv(totalCost, totalWeight),
            totalProfit = profit,
            totalOccupiedCash = occupied
        )
    }

    fun buildDynamicCards(rows: List<ResolvedInventoryRow>, groupBys: Set<DynamicGroupBy>): List<DynamicInventoryGroupCard> {
        val summary = buildDynamicSummary(rows, groupBys)
        return rows.groupBy { dynamicKey(it, groupBys) }
            .map { (key, group) ->
                val first = group.first()
                val containerProfit = group.groupBy { it.containerId }.mapValues { it.value.sumOf { row -> row.profit } }
                val totalWeight = group.sumOf { it.weightKg }
                val totalCost = group.sumOf { it.costPerKg * it.weightKg }
                val occupied = group.sumOf { it.occupiedCash }
                val profit = group.sumOf { it.profit }
                val costs = group.map { it.costPerKg }.filter { it > 0 }
                val avgCost = safeDiv(totalCost, totalWeight)
                val minCost = costs.minOrNull() ?: 0.0
                val maxCost = costs.maxOrNull() ?: 0.0
                val ages = group.mapNotNull { ageDays(it.productionDate) }
                val avgAge = ages.takeIf { it.isNotEmpty() }?.average()?.toInt()
                val spread = maxCost - minCost
                val center = if (spread > 0) (avgCost - minCost) / max(spread, Double.MIN_VALUE) else 0.5
                val insight = when {
                    spread < 0.8 -> "持仓重心居中，成本分布均衡"
                    center >= 0.66 -> "持仓重心偏高，关注高价柜"
                    center <= 0.34 -> "持仓重心偏低，成本优势明显"
                    else -> "持仓重心居中，成本分布均衡"
                }

                DynamicInventoryGroupCard(
                    key = key,
                    titleLines = groupBys.map { it.label to dynamicValue(first, it) },
                    alertLabel = if (containerProfit.values.any { it < 0 }) "盈亏预警" else if ((avgAge ?: 0) >= 300) "周转预警" else "周转平稳",
                    containerCount = group.map { it.containerId }.toSet().size,
                    fundRatio = safeDiv(occupied, summary.totalOccupiedCash),
                    lossContainers = containerProfit.values.count { it < 0 },
                    profitContainers = containerProfit.values.count { it >= 0 },
                    averageCost = avgCost,
                    occupiedCash = occupied,
                    profit = profit,
                    avgAge = avgAge,
                    roi = if (occupied > 0) profit / occupied * 100 else 0.0,
                    insight = insight,
                    minCost = minCost,
                    avgCost = avgCost,
                    maxCost = maxCost
                )
            }
            .sortedByDescending { it.occupiedCash }
    }

    private data class InventoryContext(val dataset: InventoryDataset, val valuationDate: LocalDate)

    private data class ItemSummary(
        val totalWeight: Double,
        val totalPieces: Int,
        val pendingWeight: Double,
        val transitWeight: Double,
        val inStockWeight: Double,
        val occupiedCash: Double,
        val floatingPnL: Double,
        val recoverableCash: Double,
        val netCashBefore: Double,
        val dailyBurn: Double,
        val weightedAvgCost: Double
    )

    private fun enrichItem(item: InventoryItem, context: InventoryContext): EnrichedInventoryItem {
        val param = paramFor(item, context.dataset.paramSets)
        val status = physicalStatus(item, context.valuationDate)
        val estimatedWeight = item.estimatedWeightKg ?: item.weightKg
        val actualWeight = item.actualWeightKg ?: item.estimatedWeightKg ?: item.weightKg.takeIf { it > 0 } ?: 1.0
        val marketQuote = marketQuote(item, context.dataset)

        val baseCost = baseCostPerKg(item, param, status)
        val agentPerKg = safeDiv(param.agentFeePerContainer, actualWeight)
        val prepFeePerKg = safeDiv(param.prepaymentFeePerContainer, estimatedWeight.takeIf { it > 0 } ?: actualWeight)
        val procurementPerKg = param.procurementCostPerTon / 1000
        val miscPerKg = if (status == "待发货") safeDiv(param.miscCostPerContainer, actualWeight) else 0.0
        val transitFeePerKg = safeDiv(item.transitPaymentFee ?: 0.0, actualWeight)
        val portMiscPerKg = safeDiv(item.portMiscFee ?: 0.0, actualWeight)
        val warehousePerKg = if (status == "现货(在库)") param.warehouseCostPerTon / 1000 else 0.0
        val insurancePerKg = insurancePerKg(item, param, actualWeight)
        val financingPerKg = safeDiv(
            prepaymentInterest(item, param, context.valuationDate, status) +
                    transitInterest(item, param, context.valuationDate, status) +
                    taxInterest(item, param, context.valuationDate, status),
            actualWeight
        )
        val storagePerKg = safeDiv(storageCost(item, param, context.valuationDate), actualWeight)

        var calculatedCost = baseCost + agentPerKg + prepFeePerKg + procurementPerKg + insurancePerKg + financingPerKg + miscPerKg
        if (status == "在途(海运)" || status == "清关/待入库" || status == "现货(在库)") calculatedCost += transitFeePerKg
        if (status == "清关/待入库" || status == "现货(在库)") calculatedCost += portMiscPerKg
        if (status == "现货(在库)") calculatedCost += warehousePerKg + storagePerKg

        val resolvedCost = item.currentCostRmbPerKg ?: calculatedCost
        val marketPrice = marketQuote?.first ?: 0.0
        val receivable = marketPrice * actualWeight
        val calculatedProfit = (marketPrice - resolvedCost) * actualWeight
        val useClientValuation = marketQuote != null
        val profit = if (useClientValuation) calculatedProfit else item.estimatedProfitRmb ?: calculatedProfit
        val deposit = item.depositAmount ?: DEFAULT_DEPOSIT_AMOUNT
        val netCashAfter = if (useClientValuation) {
            receivable - resolvedCost * actualWeight - deposit
        } else {
            item.estimatedNetCashAfterDelivery ?: (receivable - resolvedCost * actualWeight - deposit)
        }
        val netCashBefore = if (useClientValuation) deposit else item.estimatedNetCashBeforeDelivery ?: deposit
        val dailyCost = if (useClientValuation) {
            dailyCost(item, param, context.valuationDate, status, actualWeight, estimatedWeight)
        } else {
            item.dailyCostRmb ?: dailyCost(item, param, context.valuationDate, status, actualWeight, estimatedWeight)
        }
        val occupied = occupiedCash(item, param)

        return EnrichedInventoryItem(
            source = item,
            status = status,
            storageDays = item.storageEntryDate?.let { daysBetween(it, context.valuationDate) } ?: 0,
            currentCostPerKg = resolvedCost,
            marketPricePerKg = marketPrice,
            receivable = receivable,
            netCashBefore = netCashBefore,
            netCashAfter = netCashAfter,
            profit = profit,
            dailyCost = dailyCost,
            occupiedCash = occupied
        )
    }

    private fun summarizeItems(items: List<EnrichedInventoryItem>): ItemSummary {
        var totalWeight = 0.0
        var totalPieces = 0
        var pending = 0.0
        var transit = 0.0
        var stock = 0.0
        var occupied = 0.0
        var pnl = 0.0
        var cash = 0.0
        var netBefore = 0.0
        var burn = 0.0
        var costWeight = 0.0

        items.forEach { item ->
            val weight = itemWeight(item.source)
            totalWeight += weight
            totalPieces += item.source.pieces
            costWeight += item.currentCostPerKg * weight
            occupied += item.occupiedCash
            pnl += item.profit
            cash += item.netCashAfter
            netBefore += item.netCashBefore
            burn += item.dailyCost
            when (statusBucket(item.status)) {
                "pending" -> pending += weight
                "transit" -> transit += weight
                "stock" -> stock += weight
            }
        }

        return ItemSummary(
            totalWeight = totalWeight,
            totalPieces = totalPieces,
            pendingWeight = pending,
            transitWeight = transit,
            inStockWeight = stock,
            occupiedCash = occupied,
            floatingPnL = pnl,
            recoverableCash = cash,
            netCashBefore = netBefore,
            dailyBurn = burn,
            weightedAvgCost = safeDiv(costWeight, totalWeight)
        )
    }

    private fun paramFor(item: InventoryItem, params: List<ParamSet>): ParamSet {
        return params.firstOrNull { it.parameterSetId == item.parameterSetId }
            ?: params.firstOrNull()
            ?: defaultParamSet
    }

    private fun isValidPivotItem(item: InventoryItem): Boolean {
        val hasSpotPrice = (item.spotPriceRmbPerKg ?: 0.0) > 0
        val hasPrice = hasSpotPrice || (item.futurePriceUsdPerT ?: 0.0) > 0
        val hasWeight = if (hasSpotPrice) {
            itemWeight(item) > 0
        } else {
            (item.estimatedWeightKg ?: 0.0) > 0
        }
        return item.containerId.isNotBlank() && item.productName.isNotBlank() && hasPrice && hasWeight
    }

    private fun matchesQuery(item: EnrichedInventoryItem, query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isBlank()) return true
        return listOf(
            item.source.productName,
            item.source.factoryCode,
            item.source.skuCode,
            item.source.containerId,
            item.source.country,
            item.source.coldStorage
        ).any { it.orEmpty().lowercase().contains(q) }
    }

    private fun itemWeight(item: InventoryItem): Double {
        return item.actualWeightKg ?: item.estimatedWeightKg ?: item.weightKg
    }

    private fun physicalStatus(item: InventoryItem, today: LocalDate): String {
        item.physicalStatus?.takeIf { it.isNotBlank() }?.let { return it }
        val storageEntry = parseDate(item.storageEntryDate)
        if (storageEntry != null && !today.isBefore(storageEntry)) return "现货(在库)"
        val arrival = parseDate(item.actualArrivalDate)
        if (arrival != null && !today.isBefore(arrival) && storageEntry == null) return "清关/待入库"
        val shipping = parseDate(item.shippingDate)
        if (shipping != null && !today.isBefore(shipping)) return "在途(海运)"
        return "待发货"
    }

    private fun statusBucket(status: String): String {
        return when (status) {
            "现货(在库)" -> "stock"
            "在途(海运)", "清关/待入库" -> "transit"
            else -> "pending"
        }
    }

    private fun baseCostPerKg(item: InventoryItem, param: ParamSet, status: String): Double {
        item.spotPriceRmbPerKg?.takeIf { it > 0 }?.let { return it }
        val future = item.futurePriceUsdPerT?.takeIf { it > 0 } ?: return 0.0
        val futurePerKg = future / 1000.0
        if (status == "待发货" || status == "在途(海运)") {
            val fx = item.prepaymentFxUsdCny ?: param.defaultFxUsdCny
            val weight = item.estimatedWeightKg ?: item.weightKg
            if (weight <= 0) return 0.0
            val goods = futurePerKg * weight * fx
            val insurance = goods * 1.1 * param.insuranceFeeRate
            val dutiable = goods + insurance
            val tariff = dutiable * param.tariffRate
            val vat = (dutiable + tariff) * param.vatRate
            return safeDiv(goods + tariff + vat, weight)
        }

        val estimateWeight = item.estimatedWeightKg ?: item.weightKg
        if (estimateWeight <= 0) return 0.0
        val prepaymentFx = item.prepaymentFxUsdCny ?: param.defaultFxUsdCny
        val estimateGoods = futurePerKg * estimateWeight * prepaymentFx
        val prepayment = estimateGoods * param.factoryPrepaymentRatio
        val actualWeight = item.actualWeightKg ?: estimateWeight
        val transitFx = item.transitPaymentFxUsdCny ?: prepaymentFx
        val actualGoodsUsd = futurePerKg * actualWeight
        val prepaidGoodsUsd = futurePerKg * estimateWeight * param.factoryPrepaymentRatio
        val transitPayment = item.actualTransitPaymentRmb ?: ((actualGoodsUsd - prepaidGoodsUsd) * transitFx)
        val taxFx = item.taxPaymentFxUsdCny ?: prepaymentFx
        val goodsForTax = futurePerKg * actualWeight * taxFx
        val insurance = goodsForTax * 1.1 * param.insuranceFeeRate
        val dutiable = goodsForTax + insurance
        val tariff = dutiable * param.tariffRate
        val vat = (dutiable + tariff) * param.vatRate
        val taxPayment = item.actualTaxPaymentRmb ?: (tariff + vat)
        return safeDiv(prepayment + transitPayment + taxPayment, actualWeight)
    }

    private fun insurancePerKg(item: InventoryItem, param: ParamSet, actualWeight: Double): Double {
        val future = item.futurePriceUsdPerT ?: return 0.0
        val estimatedWeight = item.estimatedWeightKg ?: return 0.0
        val fx = item.prepaymentFxUsdCny ?: param.defaultFxUsdCny
        val insurance = (future / 1000.0) * estimatedWeight * fx * 1.1 * param.insuranceFeeRate
        return safeDiv(insurance, actualWeight)
    }

    private fun occupiedCash(item: InventoryItem, param: ParamSet): Double {
        val weight = itemWeight(item)
        val fx = item.prepaymentFxUsdCny ?: param.defaultFxUsdCny
        val goods = ((item.futurePriceUsdPerT ?: 0.0) / 1000.0) * weight * fx
        return item.actualPrepaymentRmb ?: goods * param.factoryPrepaymentRatio
    }

    private fun prepaymentInterest(item: InventoryItem, param: ParamSet, target: LocalDate, status: String): Double {
        val date = prepaymentDate(item)
        if ((status == "待发货" && date == null) || date == null) return 0.0
        val days = daysBetween(date, target)
        if (days <= 0) return 0.0
        val isSpot = (item.spotPriceRmbPerKg ?: 0.0) > 0
        if (isSpot) {
            val principal = item.actualPrepaymentRmb?.takeIf { it > 0 } ?: return 0.0
            return interestFor(principal, days, param.prepaymentInterestRate, param.prepaymentTieredRates)
        }
        val estimateWeight = item.estimatedWeightKg ?: item.weightKg
        val future = item.futurePriceUsdPerT ?: return 0.0
        if (estimateWeight <= 0) return 0.0
        val fx = item.prepaymentFxUsdCny ?: param.defaultFxUsdCny
        val goods = future / 1000.0 * estimateWeight * fx
        val factoryPrepayment = goods * param.factoryPrepaymentRatio
        val principal = if (item.actualPrepaymentRmb != null) factoryPrepayment - item.actualPrepaymentRmb else goods * param.funderAdvanceRatio
        return if (principal > 0) interestFor(principal, days, param.prepaymentInterestRate, param.prepaymentTieredRates) else 0.0
    }

    private fun transitInterest(item: InventoryItem, param: ParamSet, target: LocalDate, status: String): Double {
        val date = transitPaymentDate(item)
        val isSpot = (item.spotPriceRmbPerKg ?: 0.0) > 0
        if ((status == "待发货" && !isSpot) || date == null) return 0.0
        val days = daysBetween(date, target)
        if (days <= 0) return 0.0
        val principal = item.actualTransitPaymentRmb ?: run {
            val weight = item.actualWeightKg ?: item.estimatedWeightKg ?: item.weightKg
            if (isSpot) {
                max(0.0, (item.spotPriceRmbPerKg ?: 0.0) * weight - (item.actualPrepaymentRmb ?: 0.0))
            } else {
                val future = item.futurePriceUsdPerT ?: return 0.0
                val fx = item.transitPaymentFxUsdCny ?: item.prepaymentFxUsdCny ?: param.defaultFxUsdCny
                val estimateWeight = item.estimatedWeightKg ?: item.weightKg
                future / 1000.0 * (weight - estimateWeight * param.factoryPrepaymentRatio) * fx
            }
        }
        return interestFor(principal, days, param.transitPaymentInterestRate, param.transitPaymentTieredRates)
    }

    private fun taxInterest(item: InventoryItem, param: ParamSet, target: LocalDate, status: String): Double {
        val date = taxPaymentDate(item)
        val isSpot = (item.spotPriceRmbPerKg ?: 0.0) > 0
        if ((status == "待发货" && !isSpot) || date == null) return 0.0
        val days = daysBetween(date, target)
        if (days <= 0) return 0.0
        val principal = item.actualTaxPaymentRmb ?: run {
            val future = item.futurePriceUsdPerT ?: return 0.0
            val actualWeight = item.actualWeightKg ?: return 0.0
            val fx = item.taxPaymentFxUsdCny ?: item.prepaymentFxUsdCny ?: param.defaultFxUsdCny
            val goods = future / 1000.0 * actualWeight * fx
            val insurance = goods * 1.1 * param.insuranceFeeRate
            val dutiable = goods + insurance
            dutiable * param.tariffRate + (dutiable + dutiable * param.tariffRate) * param.vatRate
        }
        return interestFor(principal, days, param.taxPaymentInterestRate, param.taxPaymentTieredRates)
    }

    private fun dailyCost(item: InventoryItem, param: ParamSet, today: LocalDate, status: String, actualWeight: Double, estimateWeight: Double): Double {
        val prepayDays = prepaymentDate(item)?.let { max(0, daysBetween(it, today).toInt()) } ?: 0
        val transitDays = transitPaymentDate(item)?.let { max(0, daysBetween(it, today).toInt()) } ?: 0
        val taxDays = taxPaymentDate(item)?.let { max(0, daysBetween(it, today).toInt()) } ?: 0
        val fx = item.prepaymentFxUsdCny ?: param.defaultFxUsdCny
        val futurePerKg = (item.futurePriceUsdPerT ?: 0.0) / 1000.0
        val goods = futurePerKg * estimateWeight * fx
        val expectedPrepayment = goods * param.factoryPrepaymentRatio
        val prepaymentPrincipal = if (item.actualPrepaymentRmb != null) max(0.0, expectedPrepayment - item.actualPrepaymentRmb) else goods * param.funderAdvanceRatio
        val transitFx = item.transitPaymentFxUsdCny ?: fx
        val transitPrincipal = item.actualTransitPaymentRmb ?: ((futurePerKg * actualWeight - futurePerKg * estimateWeight * param.factoryPrepaymentRatio) * transitFx)
        val taxFx = item.taxPaymentFxUsdCny ?: fx
        val taxGoods = futurePerKg * actualWeight * taxFx
        val taxInsurance = taxGoods * 1.1 * param.insuranceFeeRate
        val dutiable = taxGoods + taxInsurance
        val taxPrincipal = item.actualTaxPaymentRmb ?: (dutiable * param.tariffRate + (dutiable + dutiable * param.tariffRate) * param.vatRate)

        val prepayDaily = if (prepayDays > 0) prepaymentPrincipal * tieredRateFor(prepayDays.toLong(), param.prepaymentInterestRate, param.prepaymentTieredRates) / 360 else 0.0
        val transitDaily = if (transitDays > 0) transitPrincipal * tieredRateFor(transitDays.toLong(), param.transitPaymentInterestRate, param.transitPaymentTieredRates) / 360 else 0.0
        val taxDaily = if (taxDays > 0) taxPrincipal * tieredRateFor(taxDays.toLong(), param.taxPaymentInterestRate, param.taxPaymentTieredRates) / 360 else 0.0
        val storageDaily = actualWeight / 1000.0 * param.storageCostPerTonDay

        return when (status) {
            "待发货" -> prepayDaily
            "在途(海运)" -> prepayDaily + transitDaily
            "清关/待入库" -> prepayDaily + transitDaily + taxDaily
            "现货(在库)" -> prepayDaily + transitDaily + taxDaily + storageDaily
            else -> 0.0
        }
    }

    private fun storageCost(item: InventoryItem, param: ParamSet, target: LocalDate): Double {
        val storageEntry = parseDate(item.storageEntryDate) ?: return 0.0
        if (target.isBefore(storageEntry)) return 0.0
        val days = daysBetween(item.storageEntryDate, target)
        if (days <= 0) return 0.0
        return ((item.actualWeightKg ?: item.weightKg) / 1000.0) * param.storageCostPerTonDay * days
    }

    private fun interestFor(amount: Double, days: Long, defaultRate: Double, tieredRates: List<TieredRate>?): Double {
        if (amount <= 0 || days <= 0) return 0.0
        if (tieredRates.isNullOrEmpty()) return amount * defaultRate * days / 360.0
        var total = 0.0
        var cursor = 0L
        val sorted = tieredRates.sortedBy { it.startDay }
        while (cursor < days) {
            val tier = sorted.firstOrNull { cursor >= it.startDay && cursor < (it.endDay ?: Int.MAX_VALUE) }
            val nextStart = sorted.filter { it.startDay > cursor }.minOfOrNull { it.startDay.toLong() } ?: Long.MAX_VALUE
            val span = if (tier != null) min((tier.endDay ?: Int.MAX_VALUE).toLong() - cursor, days - cursor) else min(nextStart - cursor, days - cursor)
            total += amount * (tier?.rate ?: defaultRate) * span / 360.0
            cursor += span
        }
        return total
    }

    private fun tieredRateFor(days: Long, defaultRate: Double, tieredRates: List<TieredRate>?): Double {
        if (days <= 0 || tieredRates.isNullOrEmpty()) return defaultRate
        return tieredRates.sortedBy { it.startDay }
            .firstOrNull { days >= it.startDay && days < (it.endDay ?: Int.MAX_VALUE) }
            ?.rate ?: defaultRate
    }

    private fun marketQuote(item: InventoryItem, dataset: InventoryDataset): Pair<Double, String?>? {
        val config = priceConfig(item, dataset.priceConfig)
        config?.lockedPrice?.takeIf { it > 0 }?.let { return it to (config.date ?: "手动锁价") }
        val configured = config?.estSellingPriceRmbPerKg ?: config?.latestPrice ?: config?.currentPrice ?: config?.price
        configured?.takeIf { it > 0 }?.let { return it to config?.date }
        if (config == null) return null
        val market = bestMarketPrice(config, dataset.marketPrices)
        return market?.latestPrice?.takeIf { it > 0 }?.let { it to (market.latestDate.ifBlank { market.date }) }
    }

    private fun bestMarketPrice(config: ContainerPriceConfig, prices: List<MarketPrice>): MarketPrice? {
        val targetFactory = normalizeCompareText(config.factoryCode)
        val targetProduct = normalizeCompareText(config.product)
        if (targetFactory.isBlank() || targetProduct.isBlank()) return null
        val latestByKey = prices.groupBy { normalizeCompareText(it.factoryCode) + "||" + normalizeCompareText(it.product) }
            .mapValues { (_, group) -> group.maxBy { it.latestDate.ifBlank { it.date } } }
        latestByKey["$targetFactory||$targetProduct"]?.let { return it }
        val fuzzyTarget = normalizeCompareText(stripMarketSuffix(config.product))
        return latestByKey.values.firstOrNull {
            normalizeCompareText(it.factoryCode) == targetFactory &&
                    (normalizeCompareText(it.product).contains(fuzzyTarget) || fuzzyTarget.contains(normalizeCompareText(it.product)))
        }
    }

    private fun priceConfig(item: InventoryItem, priceConfig: Map<String, ContainerPriceConfig>): ContainerPriceConfig? {
        val skuKey = item.containerId.trim() + "__" + item.productName.trim()
        return priceConfig[skuKey] ?: priceConfig[item.containerId]
    }

    private fun hasExactPriceConfig(item: InventoryItem, context: InventoryContext): Boolean {
        val config = priceConfig(item, context.dataset.priceConfig) ?: return false
        return listOf(config.lockedPrice, config.estSellingPriceRmbPerKg, config.latestPrice, config.currentPrice, config.price).any { (it ?: 0.0) > 0 } ||
                (config.factoryCode.isNotBlank() && config.product.isNotBlank())
    }

    private fun prepaymentDate(item: InventoryItem): String? {
        item.prepaymentDate?.let { return it }
        return parseDate(item.shippingDate)?.minusDays(15)?.toString()
    }

    private fun arrivalDate(item: InventoryItem): String? = item.actualArrivalDate ?: item.etaDate

    private fun transitPaymentDate(item: InventoryItem): String? {
        item.transitPaymentDate?.let { return it }
        return parseDate(arrivalDate(item))?.minusDays(14)?.toString()
    }

    private fun taxPaymentDate(item: InventoryItem): String? = item.taxPaymentDate ?: arrivalDate(item)

    private fun parseDate(value: String?): LocalDate? {
        val normalized = value?.trim()?.replace("/", "-")?.takeIf { it.isNotBlank() } ?: return null
        return runCatching { LocalDate.parse(normalized.take(10)) }.getOrNull()
    }

    private fun daysBetween(from: String?, to: LocalDate): Long {
        val start = parseDate(from) ?: return 0
        return ChronoUnit.DAYS.between(start, to)
    }

    private fun inventoryAgeDays(item: InventoryItem, today: LocalDate): Long? {
        val date = parseDate(item.productionDate) ?: return null
        return max(0, ChronoUnit.DAYS.between(date, today)).toLong()
    }

    private fun ageDays(date: String): Int? {
        val parsed = parseDate(date) ?: return null
        return max(0, ChronoUnit.DAYS.between(parsed, LocalDate.now().plusDays(1)).toInt())
    }

    private fun normalizeCompareText(value: String?): String {
        return value.orEmpty().trim().lowercase().replace(Regex("\\s+"), "").replace(Regex("[()（）\\-_/]"), "")
    }

    private fun stripMarketSuffix(value: String?): String {
        return value.orEmpty().replace(Regex("[-\\s](F[\\d+\\-]+|[\\d]+[+\\-][\\d]*)$", RegexOption.IGNORE_CASE), "").trim()
    }

    private fun safeDiv(numerator: Double, denominator: Double): Double {
        if (denominator == 0.0 || !denominator.isFinite()) return 0.0
        val result = numerator / denominator
        return if (result.isFinite()) result else 0.0
    }

    private fun dynamicKey(row: ResolvedInventoryRow, groupBys: Set<DynamicGroupBy>): String {
        val dimensions = groupBys.ifEmpty { setOf(DynamicGroupBy.PRODUCT) }
        return dimensions.joinToString("||") { dynamicValue(row, it) }
    }

    private fun dynamicValue(row: ResolvedInventoryRow, groupBy: DynamicGroupBy): String {
        return when (groupBy) {
            DynamicGroupBy.PRODUCT -> row.productName
            DynamicGroupBy.FUNDER -> row.funder
            DynamicGroupBy.COUNTRY -> row.country
            DynamicGroupBy.STATUS -> row.status
            DynamicGroupBy.FACTORY -> row.factoryCode
            DynamicGroupBy.CUSTOM_GROUP -> row.customGroup
        }.ifBlank { "--" }
    }
}

fun sortDetailRows(rows: List<ResolvedInventoryRow>, key: DetailSortKey, direction: SortDirection): List<ResolvedInventoryRow> {
    val comparator = compareBy<ResolvedInventoryRow> {
        when (key) {
            DetailSortKey.PRODUCTION_DATE -> it.productionDate
            DetailSortKey.RECEIVABLE -> it.sellingPricePerKg * it.weightKg
            DetailSortKey.PROFIT -> it.profit
            DetailSortKey.DAILY_COST -> it.dailyCost
            DetailSortKey.RECOVERABLE_CASH -> it.recoverableCash
            DetailSortKey.TOTAL_COST -> it.costPerKg * it.weightKg
            DetailSortKey.TOTAL_WEIGHT -> it.weightKg
        } as Comparable<Any>
    }
    return if (direction == SortDirection.ASC) rows.sortedWith(comparator) else rows.sortedWith(comparator.reversed())
}

fun inventoryFormatWeight(kg: Double, showKg: Boolean = false): String {
    return if (showKg) String.format("%.3fkg", kg) else String.format("%.3ft", kg / 1000.0)
}

fun inventoryFormatMoneyWan(value: Double, signed: Boolean = false): String {
    val prefix = when {
        value < 0 -> "-￥"
        signed -> "+￥"
        else -> "￥"
    }
    return "$prefix${String.format("%.1f", abs(value) / 10000.0)}万"
}

fun inventoryFormatMoneyMagnitude(value: Double): String {
    return "￥${String.format("%.1f", abs(value) / 10000.0)}万"
}

fun inventoryFormatPrice(value: Double): String {
    return if (value.isFinite()) "￥${String.format("%.2f", value)}" else "-"
}
