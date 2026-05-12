package com.mooket.app.ui.screens.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mooket.app.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * 编辑资料 UI 状态
 */
data class EditProfileUiState(
    val isLoading: Boolean = false,
    val avatarUrl: String? = null,        // 已保存的头像 URL（从后端加载）
    val pendingAvatarUrl: String? = null, // 待保存的头像预览（本地 URI，退出页面不保存）
    val nickname: String = "",
    val realNameStatus: String? = null,  // approved / pending / rejected / null
    val realName: String? = null,
    val phone: String? = null,
    val identityTags: List<String> = emptyList(),
    val error: String? = null,
    val saveSuccess: Boolean = false
)

/**
 * 编辑资料 ViewModel
 */
class EditProfileViewModel(
    private val repository: ProfileRepository
) : ViewModel() {

    companion object {
        private const val AVATAR_BASE_URL = "http://43.139.56.124:8080"
    }

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private var selectedImageUri: Uri? = null
    private var tempImageFile: File? = null

    init {
        // 延迟加载
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
                            avatarUrl = makeAvatarFullUrl(profile.avatarUrl),
                            nickname = profile.nickname ?: "",
                            realNameStatus = profile.realNameStatus,
                            realName = profile.realName,
                            phone = profile.phone,
                            identityTags = profile.identityTags ?: emptyList()
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    /**
     * 获取临时文件路径（用于拍照）
     */
    fun getTempImageFile(context: Context): File {
        tempImageFile = File.createTempFile(
            "avatar_${System.currentTimeMillis()}",
            ".jpg",
            context.cacheDir
        )
        return tempImageFile!!
    }

    /**
     * 设置选中的图片 URI
     */
    fun setSelectedImageUri(uri: Uri) {
        selectedImageUri = uri
    }

    /**
     * 清除选中的图片
     */
    fun clearSelectedImage() {
        selectedImageUri = null
        tempImageFile?.delete()
        tempImageFile = null
        croppedFileUri = null
        pendingAvatarLocalPath = null
    }

    private var croppedFileUri: Uri? = null
    private var pendingAvatarLocalPath: String? = null  // 本地裁剪文件路径，保存时才上传

    fun setCroppedFileUri(uri: Uri) {
        croppedFileUri = uri
    }

    /**
     * 上传头像
     */
    fun uploadAvatar(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // 优先使用相机临时文件，其次使用相册 URI
                val uploadSource = when {
                    tempImageFile != null -> {
                        // 拍照：直接使用临时文件
                        android.util.Log.d("EditProfile", "using camera temp file: ${tempImageFile!!.length()} bytes")
                        UploadSource.FromFile(tempImageFile!!)
                    }
                    selectedImageUri != null -> {
                        // 相册：使用 URI 流式上传
                        android.util.Log.d("EditProfile", "using gallery URI: $selectedImageUri")
                        UploadSource.FromUri(selectedImageUri!!)
                    }
                    else -> {
                        _uiState.update { it.copy(isLoading = false, error = "未选择图片") }
                        return@launch
                    }
                }

                val result = when (uploadSource) {
                    is UploadSource.FromFile -> repository.uploadAvatar(uploadSource.file)
                    is UploadSource.FromUri -> repository.uploadAvatarUri(
                        context,
                        uploadSource.uri,
                        "avatar_${System.currentTimeMillis()}.jpg"
                    )
                }

                result
                    .onSuccess { newAvatarUrl ->
                        android.util.Log.d("EditProfile", "upload success: $newAvatarUrl")
                        val fullUrl = makeAvatarFullUrl(newAvatarUrl)
                        android.util.Log.d("EditProfile", "full avatar URL: $fullUrl")
                        _uiState.update { it.copy(isLoading = false, avatarUrl = fullUrl) }
                        clearSelectedImage()
                    }
                    .onFailure { e ->
                        android.util.Log.e("EditProfile", "upload failed: ${e.message}", e)
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
            } catch (e: Exception) {
                android.util.Log.e("EditProfile", "upload exception: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * 上传裁剪后的头像（来自 UCrop）
     * 注意：仅保存本地预览路径，点击保存时才真正上传到服务器
     */
    fun uploadCroppedAvatar(croppedUri: Uri) {
        pendingAvatarLocalPath = croppedUri.toString()
        _uiState.update {
            it.copy(
                isLoading = false,
                pendingAvatarUrl = croppedUri.toString()
            )
        }
    }

    private fun makeAvatarFullUrl(relativePath: String?): String? {
        if (relativePath.isNullOrEmpty()) return null
        return if (relativePath.startsWith("http")) relativePath else "$AVATAR_BASE_URL$relativePath"
    }

    private sealed class UploadSource {
        data class FromFile(val file: File) : UploadSource()
        data class FromUri(val uri: Uri) : UploadSource()
    }

    /**
     * 拍照后设置临时文件 URI
     */
    fun setTempFileUri(context: Context, file: File): Uri {
        tempImageFile = file
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * 获取当前临时文件（用于裁剪）
     */
    fun getTempImageFileUri(context: Context): Uri? {
        return tempImageFile?.let {
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                it
            )
        }
    }

    /**
     * 更新昵称
     */
    fun updateNickname(nickname: String) {
        _uiState.update { it.copy(nickname = nickname) }
    }

    /**
     * 更新行业身份
     */
    fun updateIdentityTags(tags: List<String>) {
        _uiState.update { it.copy(identityTags = tags) }
    }

    /**
     * 保存资料（包括头像上传）
     */
    fun saveProfile(context: Context, nickname: String, identityTags: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, saveSuccess = false) }

            try {
                // 1. 如果有待保存的头像，先上传
                var finalAvatarUrl: String? = null
                if (pendingAvatarLocalPath != null) {
                    val localUri = Uri.parse(pendingAvatarLocalPath)
                    repository.uploadAvatarUri(
                        context,
                        localUri,
                        "avatar_${System.currentTimeMillis()}.jpg"
                    ).onSuccess { url ->
                        finalAvatarUrl = makeAvatarFullUrl(url)
                    }.onFailure { e ->
                        _uiState.update { it.copy(isLoading = false, error = "头像上传失败: ${e.message}") }
                        return@launch
                    }
                }

                // 2. 更新用户资料
                repository.updateProfile(
                    nickname = nickname.ifEmpty { null },
                    realName = null,  // 实名认证不允许自行修改
                    identityTags = identityTags.takeIf { it.isNotEmpty() }
                )
                    .onSuccess {
                        // 用后端返回的avatarUrl（如果上传了新头像就用新的，否则保持原值）
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                saveSuccess = true,
                                avatarUrl = finalAvatarUrl ?: it.avatarUrl,
                                pendingAvatarUrl = null
                            )
                        }
                        // 清空本地待上传状态
                        pendingAvatarLocalPath?.let { android.util.Log.d("EditProfile", "saved with pending avatar: $it") }
                        pendingAvatarLocalPath = null
                        croppedFileUri = null
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
            } catch (e: Exception) {
                android.util.Log.e("EditProfile", "save exception: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
