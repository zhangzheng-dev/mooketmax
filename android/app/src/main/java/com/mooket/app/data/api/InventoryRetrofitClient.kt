package com.mooket.app.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 库存API Retrofit客户端
 * 指向我们后端 /api/v1/inventory，由后端代理转发到 gateway.mujidigital.com
 * 解决 gateway token 的设备/IP绑定问题
 */
object InventoryRetrofitClient {

    private const val BASE_URL = "http://43.139.56.124:8080/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val inventoryApiService: InventoryApiService = retrofit.create(InventoryApiService::class.java)
}
