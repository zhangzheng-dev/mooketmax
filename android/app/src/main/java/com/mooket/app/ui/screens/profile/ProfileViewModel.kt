package com.mooket.app.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mooket.app.data.SessionManager
import com.mooket.app.data.model.AppVersion
import com.mooket.app.data.model.UserProfile
import com.mooket.app.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * 个人中心 UI 状态
 */
data class ProfileUiState(
    val isLoading: Boolean = false,
    val profile: UserProfile? = null,
    val error: String? = null,
    val version: String = "1.0",
    val versionCode: Int = 1,
    val hasUpdate: Boolean = false,
    val isCheckingUpdate: Boolean = false,
    val isDownloadingUpdate: Boolean = false,
    val updateDownloadProgress: Int = 0,  // 0-100 下载进度
    val updateMessage: String? = null,
    val updateUrl: String? = null,
    val updateDownloadError: String? = null,
    val isLoggingOut: Boolean = false
)

/**
 * 个人中心 ViewModel
 */
class ProfileViewModel(
    private val repository: ProfileRepository
) : ViewModel() {

    companion object {
        private const val AVATAR_BASE_URL = "http://43.139.56.124:8080"
    }

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun makeAvatarFullUrl(relativePath: String?): String? {
        if (relativePath.isNullOrEmpty()) return null
        return if (relativePath.startsWith("http")) relativePath else "$AVATAR_BASE_URL$relativePath"
    }

    /**
     * 加载用户资料
     */
    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getUserProfile()
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = profile.copy(
                                avatarUrl = makeAvatarFullUrl(profile.avatarUrl)
                            )
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    /**
     * 更新资料
     */
    fun updateProfile(nickname: String?, realName: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.updateProfile(nickname, realName)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    loadProfile()  // 刷新资料
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    /**
     * 上传头像
     */
    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            // 注意：实际项目中需要将 Uri 转换为 File
            // 这里假设 uri 是文件路径，实际可能需要 ContentResolver
            try {
                val file = File(uri.path ?: "")
                repository.uploadAvatar(file)
                    .onSuccess { avatarUrl ->
                        _uiState.update { it.copy(isLoading = false) }
                        loadProfile()  // 刷新资料
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * 登出
     */
    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true, error = null) }
            repository.logout()
            // 不管成功失败都清除本地会话
            SessionManager.clearSession()
            _uiState.update { it.copy(isLoggingOut = false) }
            onLoggedOut()
        }
    }

    /**
     * 检查更新
     */
    fun checkUpdate() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdate = true, updateMessage = null) }
            repository.getAppVersion()
                .onSuccess { appVersion ->
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            version = appVersion.version,
                            versionCode = appVersion.versionCode,
                            hasUpdate = appVersion.hasUpdate,
                            updateMessage = if (appVersion.hasUpdate) appVersion.updateContent else null,
                            updateUrl = appVersion.updateUrl
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isCheckingUpdate = false) }
                }
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * 注销账号
     */
    fun cancelAccount(onCancelled: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true, error = null) }
            repository.cancelAccount()
                .onSuccess {
                    SessionManager.clearSession()
                    _uiState.update { it.copy(isLoggingOut = false) }
                    onCancelled()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoggingOut = false, error = e.message) }
                }
        }
    }

    /**
     * 下载 APK 并返回文件路径，供调用方触发安装
     * @param onProgress 进度回调 0-100
     */
    fun downloadApk(context: android.content.Context, updateUrl: String, onProgress: (Int) -> Unit, onApkReady: (File) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadingUpdate = true, updateDownloadError = null, updateDownloadProgress = 0) }
            repository.downloadAndInstall(context, updateUrl) { progress ->
                _uiState.update { it.copy(updateDownloadProgress = progress) }
                onProgress(progress)
            }
                .onSuccess { apkFile ->
                    _uiState.update { it.copy(isDownloadingUpdate = false, updateDownloadProgress = 100) }
                    onApkReady(apkFile)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isDownloadingUpdate = false, updateDownloadError = e.message) }
                }
        }
    }
}