package com.mooket.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * 库存相关数据模型
 * API Base URL: https://gateway.mujidigital.com
 */

object InventoryDataTypes {
    const val INVENTORY = 1
    const val PRICE = 2
    const val CONFIG = 3
    const val PARAM = 4
    const val DELIVERY = 5

    val CORE_DATA_TYPES = listOf(INVENTORY, PRICE, CONFIG, PARAM, DELIVERY)
}

/**
 * 库存原始数据（来自服务器的原始字段名）
 */
data class InventoryItemRaw(
    @SerializedName("Inventory_SKU_ID") val inventorySkuId: String?,
    @SerializedName("Supplier_Contract_ID") val supplierContractId: String?,
    @SerializedName("Container_ID") val containerId: String?,
    @SerializedName("SKU_Code") val skuCode: String?,
    @SerializedName("Product_Name") val productName: String?,
    @SerializedName("Weight_KG") val weightKg: Double?,
    @SerializedName("Pieces") val pieces: Int?,
    @SerializedName("Parameter_Set_ID") val parameterSetId: Int?,
    @SerializedName("Funder_ID") val funderId: String?,
    @SerializedName("Future_Price_USD_Per_T") val futurePriceUsdPerT: Double?,
    @SerializedName("Spot_Price_RMB_Per_KG") val spotPriceRmbPerKg: Double?,
    @SerializedName("Est_Selling_Price_RMB_Per_KG") val estSellingPriceRmbPerKg: Double?,
    @SerializedName("Prepayment_FX_USD_CNY") val prepaymentFxUsdCny: Double?,
    @SerializedName("Transit_Payment_FX_USD_CNY") val transitPaymentFxUsdCny: Double?,
    @SerializedName("Tax_Payment_FX_USD_CNY") val taxPaymentFxUsdCny: Double?,
    @SerializedName("Estimated_Weight_KG") val estimatedWeightKg: Double?,
    @SerializedName("Actual_Weight_KG") val actualWeightKg: Double?,
    @SerializedName("Prepayment_Date") val prepaymentDate: String?,
    @SerializedName("Production_Date") val productionDate: String?,
    @SerializedName("Shipping_Date") val shippingDate: String?,
    @SerializedName("ETA_Date") val etaDate: String?,
    @SerializedName("Actual_Arrival_Date") val actualArrivalDate: String?,
    @SerializedName("Storage_Entry_Date") val storageEntryDate: String?,
    @SerializedName("Transit_Payment_Date") val transitPaymentDate: String?,
    @SerializedName("Tax_Payment_Date") val taxPaymentDate: String?,
    @SerializedName("Transit_Payment_Fee") val transitPaymentFee: Double?,
    @SerializedName("Actual_Prepayment_RMB") val actualPrepaymentRmb: Double?,
    @SerializedName("Actual_Transit_Payment_RMB") val actualTransitPaymentRmb: Double?,
    @SerializedName("Actual_Tax_Payment_RMB") val actualTaxPaymentRmb: Double?,
    @SerializedName("Deposit_Amount") val depositAmount: Double?,
    @SerializedName("Redemption_Status") val redemptionStatus: String?,
    @SerializedName("Custom_Group") val customGroup: String?,
    @SerializedName("Physical_Status") val physicalStatus: String?,
    @SerializedName("Current_Cost_RMB_Per_KG") val currentCostRmbPerKg: Double?,
    @SerializedName("Estimated_Profit_RMB") val estimatedProfitRmb: Double?,
    @SerializedName("Estimated_Net_Cash_Before_Delivery") val estimatedNetCashBeforeDelivery: Double?,
    @SerializedName("Estimated_Net_Cash_After_Delivery") val estimatedNetCashAfterDelivery: Double?,
    @SerializedName("Daily_Cost_RMB") val dailyCostRmb: Double?,
    @SerializedName("Factory_Code") val factoryCode: String?,
    @SerializedName("Country") val country: String?,
    @SerializedName("Cold_Storage") val coldStorage: String?,
    @SerializedName("Port_Misc_Fee") val portMiscFee: Double?,
    @SerializedName("Is_Deleted") val isDeleted: Int?
)

/**
 * 规范化后的库存数据
 */
data class InventoryItem(
    val inventorySkuId: String,
    val supplierContractId: String?,
    val containerId: String,
    val skuCode: String?,
    val productName: String,
    val weightKg: Double,
    val pieces: Int,
    val parameterSetId: Int?,
    val funderId: String?,
    val futurePriceUsdPerT: Double?,
    val spotPriceRmbPerKg: Double?,
    val estSellingPriceRmbPerKg: Double?,
    val prepaymentFxUsdCny: Double?,
    val transitPaymentFxUsdCny: Double?,
    val taxPaymentFxUsdCny: Double?,
    val estimatedWeightKg: Double?,
    val actualWeightKg: Double?,
    val prepaymentDate: String?,
    val productionDate: String?,
    val shippingDate: String?,
    val etaDate: String?,
    val actualArrivalDate: String?,
    val storageEntryDate: String?,
    val transitPaymentDate: String?,
    val taxPaymentDate: String?,
    val transitPaymentFee: Double?,
    val actualPrepaymentRmb: Double?,
    val actualTransitPaymentRmb: Double?,
    val actualTaxPaymentRmb: Double?,
    val depositAmount: Double?,
    val redemptionStatus: String?,
    val customGroup: String?,
    val physicalStatus: String?,
    val currentCostRmbPerKg: Double?,
    val estimatedProfitRmb: Double?,
    val estimatedNetCashBeforeDelivery: Double?,
    val estimatedNetCashAfterDelivery: Double?,
    val dailyCostRmb: Double?,
    val factoryCode: String?,
    val country: String?,
    val coldStorage: String?,
    val portMiscFee: Double?,
    val isDeleted: Int
)

/**
 * 阶梯费率
 */
data class TieredRate(
    val startDay: Int,
    val endDay: Int?,
    val rate: Double
)

/**
 * 参数配置
 */
data class ParamSet(
    val parameterSetId: Int,
    val parameterSetName: String,
    val factoryPrepaymentRatio: Double,
    val clientOwnFundRatio: Double,
    val funderAdvanceRatio: Double,
    val prepaymentInterestRate: Double,
    val transitPaymentInterestRate: Double,
    val taxPaymentInterestRate: Double,
    val storageCostPerTonDay: Double,
    val tariffRate: Double,
    val vatRate: Double,
    val prepaymentFeePerContainer: Double,
    val miscCostPerContainer: Double,
    val agentFeePerContainer: Double,
    val procurementCostPerTon: Double,
    val warehouseCostPerTon: Double,
    val insuranceFeeRate: Double,
    val defaultFxUsdCny: Double,
    val prepaymentTieredRates: List<TieredRate>?,
    val transitPaymentTieredRates: List<TieredRate>?,
    val taxPaymentTieredRates: List<TieredRate>?
)

/**
 * 市场行情价格
 */
data class MarketPrice(
    val date: String,
    val product: String,
    val country: String,
    val factoryCode: String,
    val currentPrice: Double,
    val latestPrice: Double,
    val latestDate: String,
    val source: String
)

/**
 * 集装箱价格配置
 */
data class ContainerPriceConfig(
    val product: String,
    val factoryCode: String,
    val country: String,
    val estSellingPriceRmbPerKg: Double?,
    val lockedPrice: Double?,
    val price: Double?,
    val latestPrice: Double?,
    val currentPrice: Double?,
    val date: String?
)

/**
 * 完整库存数据集
 */
data class InventoryDataset(
    val items: List<InventoryItem>,
    val paramSets: List<ParamSet>,
    val marketPrices: List<MarketPrice>,
    val priceConfig: Map<String, ContainerPriceConfig>,
    val deliveryDates: Map<String, String>
)

/**
 * 快照数据项
 */
data class SnapshotItem(
    val dataType: Int,
    val dataSnapshots: String,
    val versionTag: Int?,
    val updateTime: String?
)

/**
 * 外部API统一响应
 */
data class ExternalApiResponse<T>(
    val code: Int,
    val message: String?,
    val result: T?
)

/**
 * Gateway OAuth token 响应
 */
data class GatewayTokenResponse(
    @SerializedName("code") val code: Int?,
    @SerializedName("message") val message: String?,
    @SerializedName("result") val result: GatewayTokenResult?
)

data class GatewayTokenResult(
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("token_type") val tokenType: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("expires_in") val expiresIn: Int?,
    @SerializedName("scope") val scope: String?,
    @SerializedName("userId") val userId: String?
)

/**
 * Gateway 发送短信验证码响应
 */
data class GatewaySendSmsResponse(
    @SerializedName("code") val code: Int?,
    @SerializedName("message") val message: String?,
    @SerializedName("result") val result: Boolean?
)

/**
 * 用户权限信息
 */
data class QuantificationUserAction(
    val dataType: Int,
    val canDownload: Boolean
)

/**
 * Spot Market Summary
 */
data class SpotMarketSummary(
    val id: String?,
    val marketDate: String?,
    val priceActual: Double?,
    val priceActualDesc: String?
)

/**
 * Spot Market Detail
 */
data class SpotMarketDetail(
    val id: String?,
    val skuCode: String?,
    val category: Int?,
    val categoryDesc: String?,
    val productName: String?,
    val country: Int?,
    val countryDesc: String?,
    val plantNo: String?,
    val standard: String?,
    val leanRatio: Double?,
    val leanRatioDesc: String?,
    val flowCoefficient: Double?,
    val flowCoefficientDesc: String?,
    val marketDate: String?,
    val marketPrice: Double?,
    val priceType: Int?,
    val priceTypeDesc: String?,
    val priceActual: Double?,
    val priceActualDesc: String?,
    val skuGrade: String?,
    val skuGradeDesc: String?
)

/**
 * 透视表汇总数据
 */
data class PivotSummary(
    val productName: String,
    val totalWeight: Double,
    val totalPieces: Int,
    val itemCount: Int,
    val avgCost: Double?,
    val totalProfit: Double?
)

/**
 * 透视表行数据
 */
data class PivotRow(
    val productName: String,
    val containerId: String,
    val weight: Double,
    val pieces: Int,
    val currentCost: Double?,
    val estimatedProfit: Double?,
    val spotPrice: Double?,
    val physicalStatus: String?
)
