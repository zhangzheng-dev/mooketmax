package com.mooket.app.data.repository

import com.mooket.app.data.api.ApiService
import com.mooket.app.data.model.AppVersion
import com.mooket.app.data.model.UpdateProfileRequest
import com.mooket.app.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import android.content.Context
import android.net.Uri

/**
 * 用户资料仓库
 */
class ProfileRepository(private val apiService: ApiService) {

    /**
     * 获取用户资料
     */
    suspend fun getUserProfile(): Result<UserProfile> {
        return try {
            val response = apiService.getUserProfile()
            if (response.code == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新用户资料
     */
    suspend fun updateProfile(nickname: String?, realName: String?, identityTags: List<String>? = null): Result<Unit> {
        return try {
            val response = apiService.updateProfile(UpdateProfileRequest(nickname, realName, identityTags))
            if (response.code == 200) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 上传头像
     */
    suspend fun uploadAvatar(file: File): Result<String> {
        return try {
            val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
            val response = apiService.uploadAvatar(part)
            if (response.code == 200 && response.data != null) {
                Result.success(response.data.avatarUrl)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 上传头像（通过 URI 流式上传，解决相册读取权限问题）
     */
    suspend fun uploadAvatarUri(context: Context, uri: Uri, filename: String): Result<String> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("无法读取图片"))

            val mediaType = "image/*".toMediaTypeOrNull()
            val requestBody = inputStream.readBytes().toRequestBody(mediaType)
            inputStream.close()

            val response = apiService.uploadAvatar(
                MultipartBody.Part.createFormData("file", filename, requestBody)
            )
            if (response.code == 200 && response.data != null) {
                Result.success(response.data.avatarUrl)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 登出
     */
    suspend fun logout(): Result<Unit> {
        return try {
            val response = apiService.logout()
            if (response.code == 200) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取App版本信息
     */
    suspend fun getAppVersion(): Result<AppVersion> {
        return try {
            val response = apiService.getAppVersion()
            if (response.code == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 下载并安装 APK（应用内更新）
     * 返回下载的 APK 文件，供调用方用 Intent 安装
     * @param onProgress 进度回调 0-100
     */
    suspend fun downloadAndInstall(context: Context, updateUrl: String, onProgress: (Int) -> Unit): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val request = okhttp3.Request.Builder()
                    .url(updateUrl)
                    .build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("下载失败: ${response.code}"))
                }

                val body = response.body
                val contentLength = body?.contentLength() ?: -1L
                val apkFile = File(context.cacheDir, "update.apk")

                body?.byteStream()?.use { input ->
                    apkFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Long = 0
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesRead += read
                            if (contentLength > 0) {
                                val progress = ((bytesRead * 100) / contentLength).toInt()
                                withContext(Dispatchers.Main) { onProgress(progress.coerceIn(0, 100)) }
                            }
                        }
                    }
                }

                Result.success(apkFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 注销账号
     */
    suspend fun cancelAccount(): Result<Unit> {
        return try {
            val response = apiService.cancelAccount()
            if (response.code == 200) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}