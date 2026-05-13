package com.mooket.app.data.repository

import com.mooket.app.data.SessionManager
import com.mooket.app.data.api.InventoryApiService
import com.mooket.app.data.api.InventoryRetrofitClient
import com.mooket.app.data.api.PullSnapshotsRequest
import com.mooket.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 库存数据仓库
 */
class InventoryRepository(
    private val apiService: InventoryApiService = InventoryRetrofitClient.inventoryApiService
) {
    private val dataTypes = InventoryDataTypes

    /**
     * 获取用户可下载的数据类型权限
     */
    suspend fun fetchDownloadableTypes(): Result<List<Int>> = withContext(Dispatchers.IO) {
        try {
            val token = SessionManager.gatewayToken
                ?: return@withContext Result.failure(Exception("库存功能未授权，请先登录"))
            // gatewayUserId 用于权限查询（来自 /oauth/token 返回）
            val userId = SessionManager.gatewayUserId ?: "0"

            val response = apiService.queryQuantificationUserAction(token, userId)
            if (response.code == 200 && response.result != null) {
                val permitted = response.result
                    .filter { it.canDownload }
                    .map { it.dataType }
                Result.success(permitted.ifEmpty { listOf(dataTypes.INVENTORY) })
            } else {
                Result.success(listOf(dataTypes.INVENTORY)) // fallback
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 拉取数据快照
     */
    suspend fun pullSnapshots(dataTypes: List<Int>): Result<List<SnapshotItem>> = withContext(Dispatchers.IO) {
        try {
            val token = SessionManager.gatewayToken ?: return@withContext Result.failure(Exception("库存功能未授权，请先登录"))

            val response = apiService.pullSnapshots(
                token,
                PullSnapshotsRequest(dataTypes = dataTypes)
            )

            if (response.code == 200) {
                Result.success(response.result ?: emptyList())
            } else {
                Result.failure(Exception(response.message ?: "拉取失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取完整库存数据集
     */
    suspend fun fetchInventoryDataset(): Result<InventoryDataset> = withContext(Dispatchers.IO) {
        try {
            val token = SessionManager.gatewayToken
                ?: return@withContext Result.failure(Exception("库存功能未授权，请先登录"))
            // gatewayUserId 用于权限查询
            val userId = SessionManager.gatewayUserId ?: "0"

            // 1. 查询可下载类型
            val downloadable = fetchDownloadableTypes().getOrDefault(listOf(dataTypes.INVENTORY))

            if (!downloadable.contains(dataTypes.INVENTORY)) {
                return@withContext Result.failure(Exception("当前账号没有库存数据查看权限"))
            }

            // 2. 拉取快照
            val snapshotResult = pullSnapshots(dataTypes.CORE_DATA_TYPES)
            val snapshots = snapshotResult.getOrElse {
                println("【InventoryRepository】拉取快照异常: ${it.message}")
                return@withContext Result.failure(Exception("拉取快照失败: ${it.message}"))
            }

            // 3. 解析库存数据
            val inventorySnapshot = snapshots.find { it.dataType == dataTypes.INVENTORY }
            val items = if (inventorySnapshot != null && inventorySnapshot.dataSnapshots.isNotBlank()) {
                parseInventoryItems(inventorySnapshot.dataSnapshots)
            } else {
                // gateway 返回空 result 不算错误，只是没数据
                println("【InventoryRepository】库存快照为空，snapshots=${snapshots.size}, inventorySnapshot=${inventorySnapshot != null}")
                emptyList()
            }

            // 4. 解析参数配置
            val paramSets = parseParamSets(snapshots)

            // 5. 获取市场行情
            val snapshotPrices = parseMarketPrices(snapshots)
            val onlinePrices = fetchLatestSpotMarketPrices().getOrDefault(emptyList())
            val marketPrices = mergeMarketPrices(snapshotPrices, onlinePrices)

            // 6. 解析价格配置和交割日期
            val priceConfig = parsePriceConfig(snapshots)
            val deliveryDates = parseDeliveryDates(snapshots)

            Result.success(
                InventoryDataset(
                    items = items,
                    paramSets = paramSets,
                    marketPrices = marketPrices,
                    priceConfig = priceConfig,
                    deliveryDates = deliveryDates
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 解析库存数据
     */
    private fun parseInventoryItems(json: String): List<InventoryItem> {
        return try {
            val rawItems = parseJsonArray<InventoryItemRaw>(json)
            rawItems
                .filter { (it.isDeleted ?: 0) != 1 }
                .map { normalizeInventoryItem(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 规范化库存数据
     */
    private fun normalizeInventoryItem(raw: InventoryItemRaw): InventoryItem {
        return InventoryItem(
            inventorySkuId = raw.inventorySkuId?.toString() ?: "",
            supplierContractId = raw.supplierContractId?.takeIf { it.isNotBlank() },
            containerId = raw.containerId?.toString() ?: "",
            skuCode = raw.skuCode?.takeIf { it.isNotBlank() },
            productName = raw.productName ?: "",
            weightKg = raw.weightKg ?: 0.0,
            pieces = raw.pieces ?: 0,
            parameterSetId = raw.parameterSetId,
            funderId = raw.funderId?.takeIf { it.isNotBlank() },
            futurePriceUsdPerT = raw.futurePriceUsdPerT,
            spotPriceRmbPerKg = raw.spotPriceRmbPerKg,
            estSellingPriceRmbPerKg = raw.estSellingPriceRmbPerKg,
            prepaymentFxUsdCny = raw.prepaymentFxUsdCny,
            transitPaymentFxUsdCny = raw.transitPaymentFxUsdCny,
            taxPaymentFxUsdCny = raw.taxPaymentFxUsdCny,
            estimatedWeightKg = raw.estimatedWeightKg,
            actualWeightKg = raw.actualWeightKg,
            prepaymentDate = raw.prepaymentDate?.takeIf { it.isNotBlank() },
            productionDate = raw.productionDate?.takeIf { it.isNotBlank() },
            shippingDate = raw.shippingDate?.takeIf { it.isNotBlank() },
            etaDate = raw.etaDate?.takeIf { it.isNotBlank() },
            actualArrivalDate = raw.actualArrivalDate?.takeIf { it.isNotBlank() },
            storageEntryDate = raw.storageEntryDate?.takeIf { it.isNotBlank() },
            transitPaymentDate = raw.transitPaymentDate?.takeIf { it.isNotBlank() },
            taxPaymentDate = raw.taxPaymentDate?.takeIf { it.isNotBlank() },
            transitPaymentFee = raw.transitPaymentFee,
            actualPrepaymentRmb = raw.actualPrepaymentRmb,
            actualTransitPaymentRmb = raw.actualTransitPaymentRmb,
            actualTaxPaymentRmb = raw.actualTaxPaymentRmb,
            depositAmount = raw.depositAmount,
            redemptionStatus = raw.redemptionStatus?.takeIf { it.isNotBlank() },
            customGroup = raw.customGroup?.takeIf { it.isNotBlank() },
            physicalStatus = raw.physicalStatus?.takeIf { it.isNotBlank() },
            currentCostRmbPerKg = raw.currentCostRmbPerKg,
            estimatedProfitRmb = raw.estimatedProfitRmb,
            estimatedNetCashBeforeDelivery = raw.estimatedNetCashBeforeDelivery,
            estimatedNetCashAfterDelivery = raw.estimatedNetCashAfterDelivery,
            dailyCostRmb = raw.dailyCostRmb,
            factoryCode = raw.factoryCode?.takeIf { it.isNotBlank() },
            country = raw.country?.takeIf { it.isNotBlank() },
            coldStorage = raw.coldStorage?.takeIf { it.isNotBlank() },
            portMiscFee = raw.portMiscFee,
            isDeleted = raw.isDeleted ?: 0
        )
    }

    /**
     * 解析参数配置
     */
    private fun parseParamSets(snapshots: List<SnapshotItem>): List<ParamSet> {
        val paramSnapshot = snapshots.find { it.dataType == dataTypes.PARAM } ?: return emptyList()
        return try {
            val rawItems = parseJsonArray<Map<String, Any>>(paramSnapshot.dataSnapshots)
            rawItems.mapNotNull { normalizeParamSet(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun normalizeParamSet(raw: Map<String, Any>): ParamSet? {
        val id = (raw["Parameter_Set_ID"] ?: raw["parameter_set_id"] ?: raw["id"])?.toString()?.toDoubleOrNull()?.toInt()
            ?: return null

        return ParamSet(
            parameterSetId = id,
            parameterSetName = (raw["Parameter_Set_Name"] ?: raw["parameter_set_name"])?.toString() ?: "参数 $id",
            factoryPrepaymentRatio = (raw["Factory_Prepayment_Ratio"] ?: raw["factory_prepayment_ratio"])?.toString()?.toDoubleOrNull() ?: 0.4,
            clientOwnFundRatio = (raw["Client_Own_Fund_Ratio"] ?: raw["client_own_fund_ratio"])?.toString()?.toDoubleOrNull() ?: 0.2,
            funderAdvanceRatio = (raw["Funder_Advance_Ratio"] ?: raw["funder_advance_ratio"])?.toString()?.toDoubleOrNull() ?: 0.2,
            prepaymentInterestRate = (raw["Prepayment_Interest_Rate"] ?: raw["prepayment_interest_rate"])?.toString()?.toDoubleOrNull() ?: 0.065,
            transitPaymentInterestRate = (raw["Transit_Payment_Interest_Rate"] ?: raw["transit_payment_interest_rate"])?.toString()?.toDoubleOrNull() ?: 0.065,
            taxPaymentInterestRate = (raw["Tax_Payment_Interest_Rate"] ?: raw["tax_payment_interest_rate"])?.toString()?.toDoubleOrNull() ?: 0.065,
            storageCostPerTonDay = (raw["Storage_Cost_Per_Ton_Day"] ?: raw["storage_cost_per_ton_day"])?.toString()?.toDoubleOrNull() ?: 2.2,
            tariffRate = (raw["Tariff_Rate"] ?: raw["tariff_rate"])?.toString()?.toDoubleOrNull() ?: 0.12,
            vatRate = (raw["VAT_Rate"] ?: raw["vat_rate"])?.toString()?.toDoubleOrNull() ?: 0.09,
            prepaymentFeePerContainer = (raw["Prepayment_Fee_Per_Container"] ?: raw["prepayment_fee_per_container"])?.toString()?.toDoubleOrNull() ?: 500.0,
            miscCostPerContainer = (raw["Misc_Cost_Per_Container"] ?: raw["misc_cost_per_container"])?.toString()?.toDoubleOrNull() ?: 500.0,
            agentFeePerContainer = (raw["Agent_Fee_Per_Container"] ?: raw["agent_fee_per_container"])?.toString()?.toDoubleOrNull() ?: 5500.0,
            procurementCostPerTon = (raw["Procurement_Cost_Per_Ton"] ?: raw["procurement_cost_per_ton"])?.toString()?.toDoubleOrNull() ?: 150.0,
            warehouseCostPerTon = (raw["Warehouse_Cost_Per_Ton"] ?: raw["warehouse_cost_per_ton"])?.toString()?.toDoubleOrNull() ?: 60.0,
            insuranceFeeRate = (raw["Insurance_Fee_Rate"] ?: raw["insurance_fee_rate"])?.toString()?.toDoubleOrNull() ?: 0.0008,
            defaultFxUsdCny = (raw["Default_FX_USD_CNY"] ?: raw["default_fx_usd_cny"])?.toString()?.toDoubleOrNull() ?: 7.0,
            prepaymentTieredRates = null,
            transitPaymentTieredRates = null,
            taxPaymentTieredRates = null
        )
    }

    /**
     * 解析市场行情（从快照）
     */
    private fun parseMarketPrices(snapshots: List<SnapshotItem>): List<MarketPrice> {
        val priceSnapshot = snapshots.find { it.dataType == dataTypes.PRICE } ?: return emptyList()
        return try {
            val rawItems = parseJsonArray<Map<String, Any>>(priceSnapshot.dataSnapshots)
            rawItems.mapNotNull { normalizeMarketPrice(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun normalizeMarketPrice(raw: Map<String, Any>): MarketPrice? {
        val currentPrice = (raw["currentPrice"] ?: raw["current_price"] ?: raw["latestPrice"] ?: raw["market_price"])?.toString()?.toDoubleOrNull() ?: return null
        return MarketPrice(
            date = normalizeDate(raw["date"] ?: raw["latestDate"] ?: raw["market_date"]),
            product = (raw["product"] ?: raw["product_name"] ?: raw["Product_Name"])?.toString() ?: "",
            country = (raw["country"] ?: raw["country_desc"] ?: raw["Country"])?.toString() ?: "",
            factoryCode = (raw["factoryCode"] ?: raw["factory_code"] ?: raw["plant_no"] ?: raw["Factory_Code"])?.toString() ?: "",
            currentPrice = currentPrice,
            latestPrice = (raw["latestPrice"] ?: raw["currentPrice"] ?: raw["current_price"])?.toString()?.toDoubleOrNull() ?: currentPrice,
            latestDate = normalizeDate(raw["latestDate"] ?: raw["date"] ?: raw["market_date"]),
            source = (raw["source"])?.toString() ?: ""
        )
    }

    private fun normalizeDate(value: Any?): String {
        val str = value?.toString() ?: return ""
        return str.replace("/", "-")
    }

    /**
     * 获取最新的在线市场行情
     */
    suspend fun fetchLatestSpotMarketPrices(): Result<List<MarketPrice>> = withContext(Dispatchers.IO) {
        try {
            val token = SessionManager.gatewayToken ?: return@withContext Result.failure(Exception("库存功能未授权，请先登录"))

            // 获取市场列表
            val marketResponse = apiService.queryServerSpotMarketListWithPage(
                token,
                mapOf("pageNum" to 1, "pageSize" to 20)
            )

            if (marketResponse.code != 200 || marketResponse.result?.list.isNullOrEmpty()) {
                return@withContext Result.success(emptyList())
            }

            val latestMarket = marketResponse.result!!.list!!.firstOrNull()
                ?: return@withContext Result.success(emptyList())

            // 获取市场明细
            val detailResponse = apiService.queryServerSpotMarketDetailListByMarketIdWithPage(
                token,
                mapOf("marketId" to (latestMarket.id ?: ""), "pageNum" to 1, "pageSize" to 500)
            )

            if (detailResponse.code != 200) {
                return@withContext Result.success(emptyList())
            }

            val prices = detailResponse.result?.list?.mapNotNull { detail ->
                if (detail.marketPrice == null) return@mapNotNull null
                MarketPrice(
                    date = normalizeDate(detail.marketDate),
                    product = detail.productName ?: "",
                    country = detail.countryDesc ?: "",
                    factoryCode = detail.plantNo ?: "",
                    currentPrice = detail.marketPrice,
                    latestPrice = detail.marketPrice,
                    latestDate = normalizeDate(detail.marketDate),
                    source = "online"
                )
            } ?: emptyList()

            Result.success(prices)
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    /**
     * 合并市场行情（快照 + 在线）
     */
    private fun mergeMarketPrices(snapshotPrices: List<MarketPrice>, onlinePrices: List<MarketPrice>): List<MarketPrice> {
        val merged = mutableMapOf<String, MarketPrice>()

        (snapshotPrices + onlinePrices).forEach { price ->
            val key = listOf(
                price.product.trim(),
                price.country.trim(),
                price.factoryCode.trim(),
                (price.latestDate.ifBlank { price.date }).trim()
            ).joinToString("__")

            val current = merged[key]
            if (current == null || shouldPreferMarketPrice(price, current)) {
                merged[key] = price
            }
        }

        return merged.values.toList()
    }

    private fun shouldPreferMarketPrice(next: MarketPrice, current: MarketPrice): Boolean {
        val nextDate = next.latestDate.ifBlank { next.date }
        val currentDate = current.latestDate.ifBlank { current.date }

        if (nextDate != currentDate) {
            return nextDate > currentDate
        }

        val nextSourceRank = if (next.source == "online") 2 else 1
        val currentSourceRank = if (current.source == "online") 2 else 1
        if (nextSourceRank != currentSourceRank) {
            return nextSourceRank > currentSourceRank
        }

        return next.latestPrice >= current.latestPrice
    }

    /**
     * 解析价格配置
     */
    private fun parsePriceConfig(snapshots: List<SnapshotItem>): Map<String, ContainerPriceConfig> {
        val configSnapshot = snapshots.find { it.dataType == dataTypes.CONFIG } ?: return emptyMap()
        return try {
            val raw = parseJsonObject(configSnapshot.dataSnapshots) ?: return emptyMap()
            raw.mapValues { (_, value) ->
                val record = value as? Map<String, Any> ?: emptyMap()
                ContainerPriceConfig(
                    product = (record["product"] ?: record["Product_Name"] ?: record["productName"])?.toString() ?: "",
                    factoryCode = (record["factoryCode"] ?: record["Factory_Code"] ?: record["factory_code"])?.toString() ?: "",
                    country = (record["country"] ?: record["Country"])?.toString() ?: "",
                    estSellingPriceRmbPerKg = (record["Est_Selling_Price_RMB_Per_KG"] ?: record["estSellingPriceRmbPerKg"] ?: record["est_selling_price_rmb_per_kg"])?.toString()?.toDoubleOrNull(),
                    lockedPrice = (record["lockedPrice"] ?: record["locked_price"])?.toString()?.toDoubleOrNull(),
                    price = (record["price"])?.toString()?.toDoubleOrNull(),
                    latestPrice = (record["latestPrice"] ?: record["latest_price"])?.toString()?.toDoubleOrNull(),
                    currentPrice = (record["currentPrice"] ?: record["current_price"])?.toString()?.toDoubleOrNull(),
                    date = (record["date"] ?: record["latestDate"])?.toString()
                )
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * 解析交割日期
     */
    private fun parseDeliveryDates(snapshots: List<SnapshotItem>): Map<String, String> {
        val deliverySnapshot = snapshots.find { it.dataType == dataTypes.DELIVERY } ?: return emptyMap()
        return try {
            val raw = parseJsonObject(deliverySnapshot.dataSnapshots) ?: return emptyMap()
            raw.mapValues { it.value?.toString() ?: "" }.filter { it.value.isNotBlank() }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * 简单的JSON解析辅助函数
     */
    private inline fun <reified T> parseJsonArray(json: String): List<T> {
        val gson = com.google.gson.Gson()
        val type = object : com.google.gson.reflect.TypeToken<List<T>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseJsonObject(json: String): Map<String, Any>? {
        val gson = com.google.gson.Gson()
        val type = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }
}
