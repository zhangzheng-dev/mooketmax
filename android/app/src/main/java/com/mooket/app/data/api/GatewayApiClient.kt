package com.mooket.app.data.api

import android.provider.Settings
import com.mooket.app.MainActivity
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Gateway OAuth API 客户端（直接调 gateway.mujidigital.com，不走我们后端）
 * 解决 gateway token 的设备/IP 绑定问题：Android 设备直接与 gateway 交互
 */
object GatewayApiClient {

    private const val GATEWAY_BASE_URL = "https://gateway.mujidigital.com/"

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
        .baseUrl(GATEWAY_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val gatewayAuthService: GatewayAuthService = retrofit.create(GatewayAuthService::class.java)
}
