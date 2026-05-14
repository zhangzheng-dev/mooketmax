package com.mooket.app.data.api

import com.mooket.app.data.model.*
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Body
import retrofit2.http.Header
import okhttp3.RequestBody

/**
 * 牧集 API 服务接口
 */
interface ApiService {

    /**
     * 获取商家详情
     */
    @GET("api/v1/merchant/{id}")
    suspend fun getMerchantDetail(
        @Path("id") merchantId: Long,
        @Query("category") category: String
    ): ApiResponse<MerchantDetail>

    /**
     * 分页获取商家产品列表
     * @param type offer(报盘) 或 inquiry(求购)
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     */
    @GET("api/v1/merchant/{id}/products")
    suspend fun getMerchantProducts(
        @Path("id") merchantId: Long,
        @Query("type") type: String,
        @Query("category") category: String,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): ApiResponse<MerchantProductPage>

    /**
     * 获取搜索联想词
     */
    @GET("api/v1/search/suggest")
    suspend fun getSearchSuggestions(
        @Query("category") category: String,
        @Query("keyword") keyword: String
    ): ApiResponse<List<SearchSuggest>>

    /**
     * 保存搜索历史
     * @param userId 用户ID（可选，默认1）
     * @param searchWord 搜索词
     * @param searchType 搜索类型
     * @param isSelfSelect 是否自选（0-否，1-是）
     * @param productId 产品ID
     * @param productName 产品名称
     * @param country 国家
     * @param factoryNo 厂号
     * @param brandId 品牌ID
     * @param merchantId 商家ID
     */
    @POST("api/v1/search/history")
    suspend fun saveSearchHistory(
        @Query("userId") userId: Long = 1,
        @Query("searchWord") searchWord: String,
        @Query("searchType") searchType: String,
        @Query("isSelfSelect") isSelfSelect: Int = 0,
        @Query("productId") productId: Long? = null,
        @Query("productName") productName: String? = null,
        @Query("country") country: String? = null,
        @Query("factoryNo") factoryNo: String? = null,
        @Query("brandId") brandId: Long? = null,
        @Query("merchantId") merchantId: Long? = null
    ): ApiResponse<Unit>

    /**
     * 获取厂号筛选数据
     */
    @GET("api/v1/factory/filter")
    suspend fun getFactoryFilter(
        @Query("category") category: String = "牛"
    ): ApiResponse<FactoryFilter>

    /**
     * 获取产品详情
     * @param type offer(报盘) 或 inquiry(求购)
     * @param sortBy comprehensive(综合) 或 price(价格)
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     */
    @GET("api/v1/product/{id}")
    suspend fun getProductDetail(
        @Path("id") productId: Int,
        @Query("category") category: String?,
        @Query("type") type: String = "offer",
        @Query("sortBy") sortBy: String = "comprehensive",
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): ApiResponse<ProductDetail>

    /**
     * 获取国家详情
     * @param type offer(报盘) 或 inquiry(求购)
     * @param sortBy comprehensive(综合) 或 price(价格)
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     */
    @GET("api/v1/country/{country}")
    suspend fun getCountryDetail(
        @Path("country") country: String,
        @Query("category") category: String?,
        @Query("type") type: String = "offer",
        @Query("sortBy") sortBy: String = "comprehensive",
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): ApiResponse<CountryDetail>

    /**
     * 获取品牌详情
     * @param brandName 品牌名称
     * @param category 品类（牛/猪）
     * @param type offer(报盘) 或 inquiry(求购)
     * @param sortBy comprehensive(综合) 或 price(价格)
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     */
    @GET("api/v1/brand/{brandName}")
    suspend fun getBrandDetail(
        @Path("brandName") brandName: String,
        @Query("category") category: String?,
        @Query("type") type: String = "offer",
        @Query("sortBy") sortBy: String = "comprehensive",
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): ApiResponse<BrandDetail>

    /**
     * 获取品牌+产品详情（品牌+产品搜索结果页）
     * @param brandName 品牌名称
     * @param productName 产品名称
     * @param category 品类
     * @param type offer 或 inquiry
     * @param sortBy comprehensive 或 price
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     */
    @GET("api/v1/brand/{brandName}/product/{productName}")
    suspend fun getBrandProductDetail(
        @Path("brandName") brandName: String,
        @Path("productName") productName: String,
        @Query("category") category: String?,
        @Query("type") type: String = "offer",
        @Query("sortBy") sortBy: String = "comprehensive",
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): ApiResponse<BrandDetail>

    /**
     * 获取厂号详情
     * @param country 国家名称
     * @param factoryNo 厂号
     * @param category 品类（牛/猪）
     * @param type offer(报盘) 或 inquiry(求购)
     * @param sortBy comprehensive(综合) 或 price_asc/price_desc(价格)
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     */
    @GET("api/v1/factory/detail")
    suspend fun getFactoryDetail(
        @Query("country") country: String,
        @Query("factoryNo") factoryNo: String,
        @Query("category") category: String?,
        @Query("type") type: String = "offer",
        @Query("sortBy") sortBy: String = "comprehensive",
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): ApiResponse<FactoryDetail>

    /**
     * 获取国家+产品详情
     * @param country 国家名称
     * @param productName 产品名称
     * @param type offer(报盘) 或 inquiry(求购)
     * @param category 品类（牛/猪）
     * @param sortBy comprehensive(综合) 或 price_asc/price_desc(价格)
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     */
    @GET("api/v1/country-product")
    suspend fun getCountryProductDetail(
        @Query("country") country: String,
        @Query("productName") productName: String,
        @Query("type") type: String = "offer",
        @Query("category") category: String?,
        @Query("sortBy") sortBy: String = "comprehensive",
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): ApiResponse<CountryProductDetail>

    /**
     * 获取价格趋势（近30天历史 + 当天实时）
     * @param type 维度类型: country_product / country_factory_product
     * @param country 国家
     * @param productId 产品ID
     * @param factoryNo 厂号（可为空）
     * @param offerType 报盘/求购: 报盘 / 求购
     */
    @GET("api/v1/trend")
    suspend fun getPriceTrend(
        @Query("type") type: String,
        @Query("country") country: String,
        @Query("productId") productId: Int,
        @Query("factoryNo") factoryNo: String? = null,
        @Query("offerType") offerType: String
    ): ApiResponse<PriceTrend>

    /**
     * 获取国家+厂号+产品详情
     * @param country 国家
     * @param factoryNo 厂号
     * @param productName 产品名称
     * @param type offer(报盘) / inquiry(求购)
     * @param category 品类
     * @param sortBy 排序: comprehensive(综合) / publish_time(发布时间) / price_asc/price_desc(价格)
     * @param page 页码
     * @param pageSize 每页大小
     */
    @GET("api/v1/country-factory-product")
    suspend fun getCountryFactoryProductDetail(
        @Query("country") country: String,
        @Query("factoryNo") factoryNo: String,
        @Query("productName") productName: String,
        @Query("type") type: String = "offer",
        @Query("category") category: String? = null,
        @Query("sortBy") sortBy: String = "comprehensive",
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): ApiResponse<CountryFactoryProductDetail>

    @GET("api/v1/country-factory-product/offer/{offerId}/original-text")
    suspend fun getOfferOriginalText(
        @Path("offerId") offerId: Long
    ): ApiResponse<OfferOriginalTextResponse>

    /**
     * 获取平替产品列表
     */
    @GET("api/v1/substitute/products")
    suspend fun getSubstituteProducts(
        @Query("country") country: String,
        @Query("factoryNo") factoryNo: String,
        @Query("productName") productName: String,
        @Query("category") category: String = "牛"
    ): ApiResponse<SubstituteProduct>

    /**
     * 获取平替产品详情
     */
    @GET("api/v1/substitute/product/detail")
    suspend fun getSubstituteProductDetail(
        @Query("country") country: String,
        @Query("factoryNo") factoryNo: String,
        @Query("productName") productName: String,
        @Query("category") category: String = "牛",
        @Query("type") type: String = "offer",
        @Query("sortBy") sortBy: String = "comprehensive",
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): ApiResponse<SubstituteProductDetail>

    /**
     * 获取多厂号价格对比数据
     */
    @GET("api/v1/price-trend/compare")
    suspend fun getFactoryPriceComparison(
        @Query("country") country: String,
        @Query("factoryNos") factoryNos: String,
        @Query("productName") productName: String,
        @Query("category") category: String = "牛",
        @Query("offerType") offerType: String = "报盘",
        @Query("days") days: Int = 30
    ): ApiResponse<FactoryPriceComparison>

    /**
     * 获取热门搜索推荐
     */
    @GET("api/v1/home/hot-search")
    suspend fun getHotSearchRecommendations(
        @Query("category") category: String = "牛"
    ): ApiResponse<List<HotSearchItem>>

    /**
     * 获取首页统计数据
     */
    @GET("api/v1/home/stat")
    suspend fun getHomeStatData(
        @Query("category") category: String = "牛"
    ): ApiResponse<HomeStatData>

    /**
     * 获取首页卡片数据（瀑布流8种卡片）
     */
    @GET("api/v1/home/cards")
    suspend fun getHomeCards(
        @Query("category") category: String = "牛"
    ): ApiResponse<HomeCardsResponse>

    /**
     * 获取最近搜索记录
     */
    @GET("api/v1/search-history/recent")
    suspend fun getRecentSearches(
        @Query("limit") limit: Int = 200
    ): ApiResponse<List<SearchHistory>>

    /**
     * 获取自选搜索记录
     */
    @GET("api/v1/search-history/self-select")
    suspend fun getSelfSelectSearches(
        @Query("limit") limit: Int = 200
    ): ApiResponse<List<SearchHistory>>

    /**
     * 添加搜索记录
     */
    @POST("api/v1/search-history/add")
    suspend fun addSearchHistory(
        @Query("searchWord") searchWord: String,
        @Query("searchType") searchType: String
    ): ApiResponse<Map<String, String>>

    /**
     * 删除搜索记录
     */
    @DELETE("api/v1/search-history/{historyId}")
    suspend fun deleteSearchHistory(
        @Path("historyId") historyId: Long
    ): ApiResponse<Map<String, String>>

    /**
     * 添加自选
     */
    @POST("api/v1/search-history/self-select/add")
    suspend fun addSelfSelect(
        @Query("searchWord") searchWord: String,
        @Query("searchType") searchType: String
    ): ApiResponse<Map<String, String>>

    /**
     * 取消自选
     */
    @POST("api/v1/search-history/self-select/cancel/{historyId}")
    suspend fun cancelSelfSelect(
        @Path("historyId") historyId: Long
    ): ApiResponse<Map<String, String>>

    /**
     * 将历史记录移动到自选
     */
    @POST("api/v1/search-history/self-select/move/{historyId}")
    suspend fun moveToSelfSelect(
        @Path("historyId") historyId: Long
    ): ApiResponse<Map<String, String>>

    /**
     * 获取最近搜索的卡片数据（带完整统计信息）
     */
    @GET("api/v1/search-history/cards/recent")
    suspend fun getRecentSearchCards(
        @Query("category") category: String = "牛"
    ): ApiResponse<HomeCardsResponse>

    /**
     * 获取自选搜索的卡片数据（带完整统计信息）
     */
    @GET("api/v1/search-history/cards/self-select")
    suspend fun getSelfSelectCards(
        @Query("category") category: String = "牛"
    ): ApiResponse<HomeCardsResponse>

    /**
     * 发送验证码
     */
    @POST("api/v1/auth/send-code")
    suspend fun sendCode(
        @Body request: SendCodeRequest,
        @Header("X-Device-Id") deviceId: String
    ): ApiResponse<Map<String, String>>

    /**
     * 登录（验证码登录）
     */
    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): ApiResponse<AuthResult>

    /**
     * 注册
     */
    @POST("api/v1/auth/register")
    suspend fun register(
        @Header("Authorization") token: String,
        @Body request: RegisterRequest
    ): ApiResponse<Map<String, String>>

    /**
     * 获取用户信息
     */
    @GET("api/v1/auth/userinfo")
    suspend fun getUserInfo(
        @Header("Authorization") token: String
    ): ApiResponse<UserInfo>

    /**
     * 获取用户资料
     */
    @GET("api/v1/user/profile")
    suspend fun getUserProfile(): ApiResponse<UserProfile>

    /**
     * 更新用户资料
     */
    @POST("api/v1/user/profile/update")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): ApiResponse<Unit>

    /**
     * 上传头像
     */
    @Multipart
    @POST("api/v1/user/avatar/upload")
    suspend fun uploadAvatar(
        @Part avatar: MultipartBody.Part
    ): ApiResponse<AvatarUploadResponse>

    /**
     * 上传头像（通过 URI 直接流式上传，解决相册权限问题）
     */
    @POST("api/v1/user/avatar/upload")
    suspend fun uploadAvatarUri(
        @Body avatar: RequestBody
    ): ApiResponse<AvatarUploadResponse>

    /**
     * 登出
     */
    @POST("api/v1/user/logout")
    suspend fun logout(): ApiResponse<Unit>

    /**
     * 获取App版本信息
     */
    @GET("api/v1/app/version")
    suspend fun getAppVersion(): ApiResponse<AppVersion>

    /**
     * 注销账号
     */
    @POST("api/v1/user/cancel-account")
    suspend fun cancelAccount(): ApiResponse<Map<String, String>>
}
