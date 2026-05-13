package com.mooket.app.data.api

import com.mooket.app.data.model.*
import retrofit2.http.*

/**
 * 库存API服务接口
 * 指向我们后端代理，由后端转发到 gateway.mujidigital.com
 * 避免 gateway token 的设备/IP绑定问题
 */
interface InventoryApiService {

    /**
     * 查询用户可下载的数据类型权限
     */
    @GET("api/v1/inventory/queryQuantificationUserAction/{userId}")
    suspend fun queryQuantificationUserAction(
        @Header("X-Gateway-Token") gatewayToken: String,
        @Path("userId") userId: String
    ): ExternalApiResponse<List<QuantificationUserAction>>

    /**
     * 拉取数据快照
     */
    @POST("api/v1/inventory/pullSnapshots")
    suspend fun pullSnapshots(
        @Header("X-Gateway-Token") gatewayToken: String,
        @Body request: PullSnapshotsRequest
    ): ExternalApiResponse<List<SnapshotItem>>

    /**
     * 查询Spot Market列表
     */
    @POST("api/v1/inventory/queryServerSpotMarketListWithPage")
    suspend fun queryServerSpotMarketListWithPage(
        @Header("X-Gateway-Token") gatewayToken: String,
        @Body request: Map<String, Any>
    ): ExternalApiResponse<SpotMarketListResult>

    /**
     * 按市场ID查询Spot Market明细
     */
    @POST("api/v1/inventory/queryServerSpotMarketDetailListByMarketIdWithPage")
    suspend fun queryServerSpotMarketDetailListByMarketIdWithPage(
        @Header("X-Gateway-Token") gatewayToken: String,
        @Body request: Map<String, Any>
    ): ExternalApiResponse<SpotMarketDetailListResult>
}

data class SpotMarketListResult(
    val list: List<SpotMarketSummary>?,
    val totalCount: Int?
)

data class SpotMarketDetailListResult(
    val list: List<SpotMarketDetail>?,
    val totalCount: Int?
)

data class PullSnapshotsRequest(
    val dataTypes: List<Int>
)
