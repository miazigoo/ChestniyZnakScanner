package ru.devandprod.chestniyznak.core.device

import android.content.Context
import android.provider.Settings
import java.util.UUID

object DeviceIdentity {
    private const val PREFS_NAME = "chz_device_identity"
    private const val KEY_CLIENT_DEVICE_ID = "client_device_id"
    private const val SESSION_PREFIX = "android-session-"
    private const val INSTALL_PREFIX = "android-install-"
    private const val ANDROID_PREFIX = "android-"

    @Volatile
    private var cachedClientDeviceId: String? = null

    val clientDeviceId: String
        get() = cachedClientDeviceId ?: synchronized(this) {
            cachedClientDeviceId
                ?: buildSessionDeviceId().also { cachedClientDeviceId = it }
        }

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedDeviceId = prefs.getString(KEY_CLIENT_DEVICE_ID, "").orEmpty().trim()
        if (storedDeviceId.isNotBlank()) {
            cachedClientDeviceId = storedDeviceId
            return
        }

        val androidId = Settings.Secure
            .getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
            .orEmpty()
            .trim()
        val resolvedDeviceId = if (androidId.isNotBlank()) {
            "$ANDROID_PREFIX$androidId"
        } else {
            "$INSTALL_PREFIX${UUID.randomUUID()}"
        }
        prefs.edit().putString(KEY_CLIENT_DEVICE_ID, resolvedDeviceId).apply()
        cachedClientDeviceId = resolvedDeviceId
    }

    private fun buildSessionDeviceId(): String = "$SESSION_PREFIX${UUID.randomUUID()}"
}
