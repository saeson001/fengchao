package com.saeson.fengchao.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 本地配置。非敏感项存普通 SharedPreferences，凭证与 PIN 存 EncryptedSharedPreferences。
 */
object Config {
    const val DEFAULT_BASE = "http://3x.820804.xyz:2095"

    private lateinit var plain: SharedPreferences
    private lateinit var secure: SharedPreferences

    fun init(ctx: Context) {
        plain = ctx.getSharedPreferences("fengchao", Context.MODE_PRIVATE)
        val masterKey = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        secure = EncryptedSharedPreferences.create(
            ctx,
            "fengchao_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** v2c 管理端基础地址（走 CF 前置） */
    var baseUrl: String
        get() = plain.getString("base_url", DEFAULT_BASE)?.takeIf { it.isNotBlank() } ?: DEFAULT_BASE
        set(v) = plain.edit().putString("base_url", v.trim()).apply()

    var username: String?
        get() = secure.getString("username", null)
        private set(v) = secure.edit().putString("username", v).apply()

    var password: String?
        get() = secure.getString("password", null)
        private set(v) = secure.edit().putString("password", v).apply()

    fun hasCredentials(): Boolean =
        !username.isNullOrBlank() && !password.isNullOrBlank()

    fun saveCredentials(user: String, pass: String) {
        username = user
        password = pass
    }

    fun clearCredentials() {
        username = null
        password = null
    }

    // ---- 应用锁 ----

    var lockEnabled: Boolean
        get() = plain.getBoolean("lock_enabled", false)
        set(v) = plain.edit().putBoolean("lock_enabled", v).apply()

    var biometricEnabled: Boolean
        get() = plain.getBoolean("biometric_enabled", false)
        set(v) = plain.edit().putBoolean("biometric_enabled", v).apply()

    /** PIN 的 SHA-256 十六进制，存加密_prefs */
    var pinHash: String?
        get() = secure.getString("pin_hash", null)
        set(v) = secure.edit().putString("pin_hash", v).apply()

    fun hasPin(): Boolean = !pinHash.isNullOrBlank()

    fun setPin(pin: String) {
        pinHash = Fmt.sha256(pin)
    }

    fun checkPin(pin: String): Boolean =
        pinHash == Fmt.sha256(pin)
}
