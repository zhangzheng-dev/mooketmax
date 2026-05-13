package com.mooket.app.data.api

import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Gateway SMS AES 加密工具
 * AES-128-ECB + PKCS5/PKCS7 Padding
 */
object AesEncryptUtil {
    private const val AES_KEY = "pK7BWDVdX4WnARTE"
    private const val ALGORITHM = "AES/ECB/PKCS5Padding"

    fun encryptMobile(mobile: String): String {
        val key = SecretKeySpec(AES_KEY.toByteArray(StandardCharsets.UTF_8), "AES")
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(mobile.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }
}
