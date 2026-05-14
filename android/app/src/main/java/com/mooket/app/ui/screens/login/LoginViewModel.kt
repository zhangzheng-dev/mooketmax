package com.mooket.app.ui.screens.login

import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mooket.app.MainActivity
import com.mooket.app.data.SessionManager
import com.mooket.app.data.api.AesEncryptUtil
import com.mooket.app.data.api.GatewayApiClient
import com.mooket.app.data.api.GatewayAuthService
import com.mooket.app.data.api.RetrofitClient
import com.mooket.app.data.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 登录流程 ViewModel
 */
class LoginViewModel : ViewModel() {

    private val apiService = RetrofitClient.apiService

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // 缓存
    private var cachedPhone: String = ""
    private var cachedToken: String? = null
    // Gateway SMS/OAuth 统一 clientId（时间戳）
    private var cachedClientId: String = ""

    /**
     * 设置 token
     */
    fun setToken(token: String) {
        cachedToken = token
    }

    /**
     * 发送验证码
     */
    fun sendCode(phone: String) {
        if (phone.length != 11) {
            _uiState.value = _uiState.value.copy(error = "请输入11位手机号")
            return
        }
        cachedPhone = phone

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val deviceId = Settings.Secure.getString(
                    MainActivity.appContext.contentResolver,
                    Settings.Secure.ANDROID_ID
                )

                // 生成时间戳作为 clientId（与 RN 保持一致）
                cachedClientId = System.currentTimeMillis().toString()

                // 调我们后端的 send-code 接口（后端会存储验证码到 smsCodeStore）
                val response = apiService.sendCode(SendCodeRequest(phone), deviceId)

                if (response.code == 200) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        phone = phone,
                        screen = LoginScreen.SmsVerify,
                        countdown = 60
                    )
                    startCountdown()
                } else {
                    // 失败（如账号被禁用），停留在登录页
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        phone = phone,
                        error = response.message ?: "发送失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "网络错误"
                )
            }
        }
    }

    /**
     * 验证码倒计时
     */
    private fun startCountdown() {
        viewModelScope.launch {
            while (_uiState.value.countdown > 0) {
                delay(1000)
                _uiState.value = _uiState.value.copy(countdown = _uiState.value.countdown - 1)
            }
        }
    }

    /**
     * 验证码登录
     */
    fun loginWithCode(code: String) {
        if (code.length != 6) {
            _uiState.value = _uiState.value.copy(error = "请输入6位验证码")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val deviceId = Settings.Secure.getString(
                    MainActivity.appContext.contentResolver,
                    Settings.Secure.ANDROID_ID
                )

                // 1. 先调我们后端登录（拿 JWT token）
                val response = apiService.login(LoginRequest(cachedPhone, code, deviceId))
                if (response.code == 200 && response.data != null) {
                    cachedToken = response.data.token
                    SessionManager.token = response.data.token
                    response.data.userId?.let { SessionManager.userId = it }
                    // 保存后端返回的 gateway token（优先用后端返回的，比 Android 单独调 gateway 更稳定）
                    response.data.gatewayAccessToken?.let {
                        SessionManager.gatewayToken = it
                        android.util.Log.i("Login", "【后端返回】gatewayToken saved: ${it.take(20)}...")
                    }
                    response.data.gatewayUserId?.let {
                        SessionManager.gatewayUserId = it
                    }
                    response.data.mooketId?.let {
                        SessionManager.mooketId = it
                    }
                    val nickname = response.data.nickname
                    val needsProfile = nickname.isNullOrBlank()
                    SessionManager.needsProfile = needsProfile

                    // 2. Android 直接调 gateway OAuth（用相同的 clientId，解决设备绑定问题）
                    try {
                        val deviceId = Settings.Secure.getString(
                            MainActivity.appContext.contentResolver,
                            Settings.Secure.ANDROID_ID
                        )
                        val gatewayResponse = GatewayApiClient.gatewayAuthService.exchangeToken(
                            mobile = cachedPhone,
                            smsCode = code,
                            deviceId = deviceId,  // 必须与 SMS 发送时一致（都用 ANDROID_ID）
                            deviceMac = deviceId
                        )
                        if (gatewayResponse.result?.accessToken != null) {
                            SessionManager.gatewayToken = gatewayResponse.result.accessToken
                            System.out.println("【Gateway Direct】token获取成功: ${gatewayResponse.result.accessToken}")
                        } else {
                            System.out.println("【Gateway Direct】token为空: code=${gatewayResponse.code}, message=${gatewayResponse.message}")
                        }
                    } catch (e: Exception) {
                        System.out.println("【Gateway Direct】token获取失败: ${e.message}")
                    e.printStackTrace()
                    }

                    if (needsProfile) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            screen = LoginScreen.Register,
                            token = response.data.token,
                            isNewUser = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            screen = LoginScreen.Home,
                            token = response.data.token,
                            isNewUser = false
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.message ?: "验证码错误"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "网络错误"
                )
            }
        }
    }

    /**
     * 注册（完善资料）
     */
    fun register(nickname: String, identityTags: List<String>) {
        if (nickname.length < 2 || nickname.length > 20) {
            _uiState.value = _uiState.value.copy(error = "昵称需2-20个字符")
            return
        }
        if (identityTags.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "请选择至少一个身份")
            return
        }
        val token = cachedToken ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = apiService.register(
                    "Bearer $token",
                    RegisterRequest(nickname, identityTags)
                )
                if (response.code == 200) {
                    SessionManager.nickname = nickname
                    SessionManager.needsProfile = false  // 注册完成，标记为已完善资料
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        screen = LoginScreen.Home,
                        nickname = nickname,
                        identityTags = identityTags
                    )
                    // 获取 mooketId 保存到 SessionManager（用于外部API）
                    fetchAndSaveMooketId()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.message ?: "注册失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "网络错误"
                )
            }
        }
    }

    /**
     * 一键登录（天翼）
     */
    fun oneClickLogin() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            // TODO: 天翼一键登录 SDK 集成
            // 当前阶段：直接跳转首页（模拟）
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                screen = LoginScreen.Home
            )
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * 获取 mooketId 并保存到 SessionManager
     * mooketId 是外部 API（gateway.mujidigital.com）的用户标识
     */
    private fun fetchAndSaveMooketId() {
        viewModelScope.launch {
            try {
                val profileResponse = apiService.getUserProfile()
                if (profileResponse.code == 200 && profileResponse.data != null) {
                    val mooketNo = profileResponse.data.mooketNo
                    if (!mooketNo.isNullOrBlank()) {
                        SessionManager.mooketId = mooketNo
                    }
                }
            } catch (e: Exception) {
                // 获取 mooketId 失败不影响登录流程，静默处理
            }
        }
    }

    /**
     * 返回上一步
     */
    fun goBack() {
        _uiState.value = _uiState.value.copy(
            screen = when (_uiState.value.screen) {
                LoginScreen.SmsVerify -> LoginScreen.PhoneInput
                LoginScreen.Register -> LoginScreen.SmsVerify
                else -> LoginScreen.PhoneInput
            }
        )
    }

    /**
     * 更新昵称
     */
    fun updateNickname(nickname: String) {
        _uiState.value = _uiState.value.copy(nickname = nickname)
    }

    /**
     * 切换身份标签
     */
    fun toggleIdentityTag(tag: String) {
        val current = _uiState.value.selectedIdentityTags
        val updated = if (current.contains(tag)) {
            current - tag
        } else {
            current + tag
        }
        _uiState.value = _uiState.value.copy(selectedIdentityTags = updated)
    }
}

/**
 * UI 状态
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val phone: String = "",
    val code: String = "",
    val screen: LoginScreen = LoginScreen.PhoneInput,
    val countdown: Int = 0,
    val token: String? = null,
    val isNewUser: Boolean = false,
    val nickname: String = "",
    val identityTags: List<String> = emptyList(),
    val selectedIdentityTags: Set<String> = emptySet()
)

/**
 * 登录流程页面
 */
enum class LoginScreen {
    PhoneInput,  // 手机号输入
    SmsVerify,   // 验证码确认
    OneClick,    // 一键登录
    Register,    // 注册信息
    Home         // 跳转首页
}
