package com.mooket.app.data.api

import com.mooket.app.data.model.GatewaySendSmsResponse
import com.mooket.app.data.model.GatewayTokenResponse
import retrofit2.http.*

/**
 * Gateway OAuth 接口（直接调 gateway.mujidigital.com）
 */
interface GatewayAuthService {

    /**
     * 发送短信验证码
     * mobile 需要用 AES-128-ECB + PKCS7Padding 加密
     */
    @POST("uac/auth/code/sms")
    suspend fun sendSmsCode(
        @Query("mobile") encryptedMobile: String,
        @Header("Authorization") auth: String = "Basic bWFsbGVlLW11amktdWFjOm1hbGlTb2FDbGllbnRTZWNyZXQ=",
        @Header("noAuth") noAuth: String = "1",
        @Header("deviceId") deviceId: String,
        @Header("deviceType") deviceType: String = "Android",
        @Header("deviceMac") deviceMac: String,
        @Header("isPc") isPc: String = "0"
    ): GatewaySendSmsResponse

    /**
     * 用短信验证码换取 Access Token
     * grant_type=sms_code，手机号+验证码+clientId（时间戳）
     */
    @FormUrlEncoded
    @POST("uac/oauth/token")
    suspend fun exchangeToken(
        @Field("grant_type") grantType: String = "sms_code",
        @Field("mobile") mobile: String,
        @Field("smsCode") smsCode: String,
        @Field("registerSource") registerSource: String = "1",
        @Field("registerMode") registerMode: String = "1",
        @Field("oneClickLogin") oneClickLogin: String = "1",
        @Header("Authorization") auth: String = "Basic bWFsbGVlLW11amktdWFjOm1hbGlTb2FDbGllbnRTZWNyZXQ=",
        @Header("noAuth") noAuth: String = "1",
        @Header("deviceId") deviceId: String,
        @Header("deviceType") deviceType: String = "Android",
        @Header("deviceMac") deviceMac: String,
        @Header("isPc") isPc: String = "0"
    ): GatewayTokenResponse
}
