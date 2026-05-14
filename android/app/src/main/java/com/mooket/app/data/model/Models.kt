package com.mooket.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * API 统一响应
 */
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
)

/**
 * 头像上传响应
 */
data class AvatarUploadResponse(
    @SerializedName("avatarUrl") val avatarUrl: String,
    @SerializedName("message") val message: String
)

/**
 * 商家详情
 */
data class MerchantDetail(
    val merchantId: Long,
    val merchantName: String,
    val merchantShortName: String,
    val merchantTags: String,
    val contactPhone: String,
    val todayOfferCount: Int,
    val todayInquiryCount: Int,
    val todayProductCount: Int,
    val todayFactoryCount: Int,
    val offers: List<OfferSummary>,
    val inquiries: List<OfferSummary>
)

/**
 * 商家产品分页响应
 */
data class MerchantProductPage(
    val products: List<OfferSummary>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val offerType: String
)

/**
 * 报盘摘要
 */
data class OfferSummary(
    val offerId: Long?,
    val productName: String?,
    val country: String?,
    val factoryNo: String?,
    val price: Double?,
    val priceMax: Double?,
    val goodsLocation: String?,
    val tags: String?,
    val goodsType: String?,
    val feedingType: String?,
    val publishTime: String?,
    val employeeOffers: List<EmployeeOffer>?
)

/**
 * 员工报价
 */
data class EmployeeOffer(
    val offerId: Long?,
    val userNickname: String?,
    val price: Double?,
    val priceMax: Double?,
    val weight: String?,
    val goodsLocation: String?,
    val tags: String?,
    val goodsType: String?,
    val feedingMethod: String?,
    val offerOriginalText: String?,
    val publishTime: String?
)

/**
 * 搜索联想词
 */
data class SearchSuggest(
    val text: String,
    val keyword: String,
    val type: String,
    val priority: Int,
    val targetId: Long,
    val matchType: String
)

/**
 * 厂号筛选数据
 */
data class FactoryFilter(
    val countries: List<String>,
    val factories: List<FactoryItem>
)

/**
 * 厂号条目
 */
data class FactoryItem(
    val country: String,
    val factoryNo: String
)

/**
 * 产品详情
 */
data class ProductDetail(
    val productId: Int,
    val productName: String,
    val category: String,
    val offerCount: Long,
    val priceMin: Double?,
    val priceMax: Double?,
    val merchantCount: Int,
    val factoryCount: Int,
    val summaries: List<ProductSummary>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int
)

/**
 * 产品汇总（按国家厂号分组）
 */
data class ProductSummary(
    val country: String?,
    val factoryNo: String?,
    val countryFactory: String?,
    val priceMin: Double?,
    val priceMax: Double?,
    val merchantNames: List<String>?,
    val merchantCount: Int,
    val offerCount: Int
)

/**
 * 国家详情
 */
data class CountryDetail(
    val country: String,
    val offerCount: Long,
    val merchantCount: Int,
    val factoryCount: Int,
    val priceMin: Double?,
    val priceMax: Double?,
    val hotFactories: List<HotFactory>,
    val hotProducts: List<HotProduct>,
    val summaries: List<CountryProductSummary>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int
)

/**
 * 国家详情页 - 产品汇总
 */
data class CountryProductSummary(
    val productId: Int,
    val productName: String,
    val priceMin: Double?,
    val priceMax: Double?,
    val factoryNos: List<String>,
    val factoryCount: Int,
    val offerCount: Int
)

/**
 * 热门厂号
 */
data class HotFactory(
    val factoryNo: String,
    val offerCount: Int,
    val rank: Int
)

/**
 * 热门产品
 */
data class HotProduct(
    val productName: String,
    val offerCount: Int,
    val rank: Int
)

/**
 * 厂号详情
 */
data class FactoryDetail(
    val factoryId: Int?,
    val country: String,
    val countryAlias: String?,
    val factoryNo: String,
    val productCount: Int,
    val inquiryCount: Int,
    val recentOfferCount: Int,
    val products: List<FactoryProduct>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int
)

/**
 * 厂号详情页 - 产品
 */
data class FactoryProduct(
    val productId: Int,
    val productName: String,
    val priceMin: Double?,
    val priceMax: Double?,
    val merchantNames: List<String>,
    val merchantCount: Int,
    val offerCount: Int
)

/**
 * 国家+产品详情
 */
data class CountryProductDetail(
    val country: String,
    val productId: Int?,
    val productName: String,
    val priceMin: Double?,
    val priceMax: Double?,
    val priceChange: Double?,      // 相比昨日涨跌值
    val priceChangeRate: Double?,  // 涨跌幅百分比
    val offerCount: Long,
    val inquiryCount: Long,
    val merchantCount: Int,
    val priceHistory7Days: List<DailyPrice>,
    val priceHistory30Days: List<DailyPrice>,
    val factories: List<CountryProductFactory>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int
)

/**
 * 日价格
 */
data class DailyPrice(
    val date: String,
    val fullDate: String,
    val avgPrice: Double?,
    val priceUnit: String?,
    val offerCount: Int?
)

/**
 * 国家+产品 - 厂号聚合
 */
data class CountryProductFactory(
    val country: String?,
    val factoryNo: String?,
    val countryFactory: String?,
    val priceMin: Double?,
    val priceMax: Double?,
    val merchantNames: List<String>?,
    val merchantCount: Int,
    val offerCount: Int
)

/**
 * 价格趋势
 */
data class PriceTrend(
    val dimensionType: String,
    val country: String,
    val productId: Int,
    val productName: String?,
    val factoryNo: String?,
    val offerType: String,
    val trend: List<TrendPoint>
)

/**
 * 趋势点
 */
data class TrendPoint(
    val date: String,
    val fullDate: String,
    val avgPrice: Double?,
    val offerCount: Int? = null
)

/**
 * 国家+厂号+产品详情
 */
data class CountryFactoryProductDetail(
    val country: String,
    val factoryNo: String,
    val productId: Int?,
    val productName: String,
    val priceMin: Double?,
    val priceMax: Double?,
    val priceChange: Double?,
    val priceChangeRate: Double?,
    val offerCount: Long,
    val inquiryCount: Long,
    val merchantCount: Int,
    val priceHistory7Days: List<DailyPrice>,
    val priceHistory30Days: List<DailyPrice>,
    val merchantOffers: List<MerchantOfferGroup>,  // 按商家分组的报盘列表
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val hasSubstitute: Boolean = false  // 是否有平替产品
)

/**
 * 商家报盘分组（国家+厂号+产品详情页用）
 */
data class MerchantOfferGroup(
    val merchantId: Long?,
    val merchantName: String?,
    val merchantPhone: String?,
    val offerCount: Int,
    val isFamousMerchant: Boolean = false,
    val employeeOffers: List<EmployeeOfferItem>
)

/**
 * 商家选项（用于筛选）
 */
data class MerchantOption(
    val id: Long,
    val name: String
)

/**
 * 员工报价（国家+厂号+产品详情页用）
 */
data class EmployeeOfferItem(
    val offerId: Long?,
    val userNickname: String?,
    val price: String,
    val weight: String?,
    val goodsLocation: String?,
    val goodsType: String?,
    val tags: String?,
    val offerType: String?,
    val publishTime: String?,
    val offerOriginalText: String? = null
)

data class OfferOriginalTextResponse(
    val offerId: Long?,
    val text: String?
)

/**
 * 厂号价格对比
 */
data class FactoryPriceComparison(
    val country: String,
    val productName: String,
    val category: String,
    val offerType: String,
    val factories: List<FactoryTrendData>
)

/**
 * 单个厂号的价格趋势数据
 */
data class FactoryTrendData(
    val factoryNo: String,
    val trend: List<TrendPoint>,
    val avgPrice: Double?
)

/**
 * 热门搜索项
 */
data class HotSearchItem(
    val keyword: String,           // 搜索关键词
    val dimension: String,          // 维度：国家厂号产品/国家产品/国家/产品/品牌/商家
    val todayOfferCount: Int,      // 今日报盘数
    val country: String? = null,   // 国家（国家厂号产品/国家产品/国家/国家厂号）
    val factoryNo: String? = null, // 厂号（国家厂号产品/国家厂号）
    val productId: Int? = null,    // 产品ID（国家产品/产品/品牌产品）
    val brandId: Int? = null,      // 品牌ID（品牌/品牌产品）
    val merchantId: Long? = null   // 商家ID（商家）
)

/**
 * 首页统计数据
 */
data class HomeStatData(
    val totalOfferCount: String,    // 报盘总量（如 "10.2w"）
    val totalInquiryCount: String,   // 求购总量（如 "1.1k"）
    val merchantCount: String,       // 商家总数
    val statTime: String            // 统计时间
)

/**
 * 搜索历史记录
 */
data class SearchHistory(
    val historyId: Long,
    val searchWord: String,
    val searchType: String,
    val isSelfSelect: Int,
    val createTime: String,
    val productId: Int? = null,
    val brandId: Int? = null,
    val merchantId: Long? = null,
    val country: String? = null,
    val factoryNo: String? = null,
    val productName: String? = null
)

// ============ 首页卡片相关 ============

/**
 * 首页卡片响应
 */
data class HomeCardsResponse(
    val cards: List<HomeCardItem>,
    val updateTime: String
)

/**
 * 首页卡片（通用结构，支持所有类型）
 */
data class HomeCardItem(
    val cardType: String? = null,
    val rank: Int? = null,
    val todayOfferCount: Int? = null,
    // 历史记录ID（用于删除和添加到自选）
    val historyId: Long? = null,
    // 产品卡片
    val productId: Int? = null,
    val productName: String? = null,
    val merchantCount: Int? = null,
    val factoryCount: Int? = null,
    val priceMin: Double? = null,
    val priceMax: Double? = null,
    // 国家卡片
    val country: String? = null,
    val countryAlias: String? = null,
    val hotFactories: List<Map<String, Any>>? = null,
    val hotProducts: List<Map<String, Any>>? = null,
    // 品牌卡片
    val brandId: Int? = null,
    val brandName: String? = null,
    val productCount: Int? = null,
    // 商家卡片
    val merchantId: Long? = null,
    val merchantName: String? = null,
    val merchantShortName: String? = null,
    val merchantTags: String? = null,
    val latestOffers: List<Map<String, Any>>? = null,
    // 国家厂号卡片
    val factoryNo: String? = null,
    // 品牌产品卡片
    val priceChange: Double? = null,
    val priceChangeRate: Double? = null,
    val trendPoints: List<Map<String, Any>>? = null,
    // 国家产品卡片
    val topFactories: List<Map<String, Any>>? = null,
    // 热门商家
    val hotMerchants: List<Map<String, Any>>? = null,
    // 国家厂号产品卡片
    val inquiryCount: Int? = null,
    // 示例标识颜色（可空，为空则使用默认 Primary）
    val exampleBadgeColor: Long? = null
)

/**
 * 品牌详情
 */
data class BrandDetail(
    val brandName: String,
    val todayOfferCount: Long,
    val yesterdayOfferCount: Long,
    val totalOfferCount: Long,
    val todayInquiryCount: Long,
    val yesterdayInquiryCount: Long,
    val totalInquiryCount: Long,
    val factoryCount: Int,
    val productCount: Int,
    val merchantCount: Int?,
    val priceMin: Double?,
    val priceMax: Double?,
    val summaries: List<BrandProductSummary>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int
)

/**
 * 品牌产品汇总
 * 兼容两种场景：
 * - 品牌详情（按产品聚合）：factoryNos（逗号分隔）有值，country/factoryNo/merchantNames 为 null
 * - 品牌+产品详情（按国家厂号聚合）：factoryNos 为 null，country/factoryNo/merchantNames 有值
 */
data class BrandProductSummary(
    val productId: Int,
    val productName: String,
    val priceMin: Double?,
    val priceMax: Double?,
    val factoryNos: String,
    val factoryCount: Int,
    val offerCount: Int,
    // 以下字段用于品牌+产品详情（按国家厂号分组时）
    val country: String? = null,
    val factoryNo: String? = null,
    val countryFactory: String? = null,
    val merchantNames: List<String>? = null,
    val merchantCount: Int? = null
)

/**
 * 品牌产品汇总（按产品名合并后，用于 UI 显示）
 */
data class MergedBrandProductSummary(
    val productId: Int,
    val productName: String,
    val priceMin: Double?,
    val priceMax: Double?,
    val factoryNos: List<String>,
    val factoryCount: Int,
    val offerCount: Int
)

/**
 * 品牌+产品详情页结果（包含看板数据和列表）
 */
data class BrandProductDetailResult(
    val brandName: String,
    val factoryCount: Int,
    val productCount: Int,
    val merchantCount: Int?,
    val priceMin: Double?,
    val priceMax: Double?,
    val todayOfferCount: Long,
    val yesterdayOfferCount: Long,
    val todayInquiryCount: Long,
    val yesterdayInquiryCount: Long,
    val summaries: List<BrandProductSummary>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int
)

// ============ 登录注册相关 ============

/**
 * 发送验证码请求
 */
data class SendCodeRequest(
    val phone: String
)

/**
 * 登录请求
 */
data class LoginRequest(
    val phone: String,
    val code: String,
    val deviceId: String? = null
)

/**
 * 注册请求
 */
data class RegisterRequest(
    val nickname: String,
    val identityTags: List<String>
)

/**
 * 登录响应
 */
data class AuthResult(
    val token: String,
    val isNewUser: Boolean,
    val userId: Long? = null,
    val nickname: String? = null,
    val gatewayAccessToken: String? = null,
    val gatewayUserId: String? = null,
    val mooketId: String? = null
)

/**
 * 用户信息
 */
data class UserInfo(
    val userId: Long,
    val phone: String,
    val nickname: String?,
    val identityTags: List<String>?,
    val createTime: String?
)

/**
 * 用户资料
 */
data class UserProfile(
    val userId: Long,
    val phone: String,
    val nickname: String?,
    val avatarUrl: String?,
    val mooketId: String?,
    val mooketNo: String?,      // 牧集号（来自UAC user_muji_no）
    val realNameStatus: String?,  // 实名状态: pending/approved/rejected
    val realName: String?,         // 真实姓名（如果已实名）
    val identityTags: List<String>? // 行业身份标签
)

/**
 * 更新资料请求
 */
data class UpdateProfileRequest(
    val nickname: String?,
    val realName: String?,
    val identityTags: List<String>? = null
)

/**
 * App版本信息
 */
data class AppVersion(
    val version: String,
    val versionCode: Int,
    val hasUpdate: Boolean,
    val updateUrl: String?,
    val updateContent: String?
)
