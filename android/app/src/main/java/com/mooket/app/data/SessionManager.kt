package com.mooket.app.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 会话管理器：管理登录态 token 的持久化
 */
object SessionManager {

    private const val PREFS_NAME = "mooket_session"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_NEEDS_PROFILE = "needs_profile"
    private const val KEY_MOOKET_ID = "mooket_id"
    private const val KEY_GATEWAY_TOKEN = "gateway_token"

    private lateinit var prefs: SharedPreferences

    /**
     * 在 Application 或 MainActivity 中早期初始化
     */
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_TOKEN, value).apply()
        }

    var userId: Long?
        get() = if (prefs.contains(KEY_USER_ID)) prefs.getLong(KEY_USER_ID, -1) else null
        set(value) {
            if (value != null) {
                prefs.edit().putLong(KEY_USER_ID, value).apply()
            } else {
                prefs.edit().remove(KEY_USER_ID).apply()
            }
        }

    var nickname: String?
        get() = prefs.getString(KEY_NICKNAME, null)
        set(value) {
            prefs.edit().putString(KEY_NICKNAME, value).apply()
        }

    /**
     * 牧集号（用于外部API如 gateway.mujidigital.com 的用户ID）
     */
    var mooketId: String?
        get() = prefs.getString(KEY_MOOKET_ID, null)
        set(value) {
            prefs.edit().putString(KEY_MOOKET_ID, value).apply()
        }

    /**
     * Gateway 用户ID（来自 /oauth/token 返回的 userId）
     */
    var gatewayUserId: String?
        get() = prefs.getString("gateway_user_id", null)
        set(value) {
            prefs.edit().putString("gateway_user_id", value).apply()
        }

    /**
     * Gateway Access Token（用于 gateway.mujidigital.com API）
     */
    var gatewayToken: String?
        get() = prefs.getString(KEY_GATEWAY_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_GATEWAY_TOKEN, value).apply()
        }

    /**
     * 标记用户是否需要完善资料（注册流程未完成）
     * 登录成功后写入，注册完成后清除
     */
    var needsProfile: Boolean
        get() = prefs.getBoolean(KEY_NEEDS_PROFILE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_NEEDS_PROFILE, value).apply()
        }

    /**
     * 只有已完成注册（资料完整）的用户才算已登录
     */
    fun isLoggedIn(): Boolean = !token.isNullOrBlank() && !needsProfile

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
