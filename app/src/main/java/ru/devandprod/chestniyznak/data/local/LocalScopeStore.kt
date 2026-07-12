package ru.devandprod.chestniyznak.data.local

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton
import ru.devandprod.chestniyznak.domain.auth.AuthSession

@Singleton
class LocalScopeStore @Inject constructor(
    private val sharedPreferences: SharedPreferences,
) {
    fun update(session: AuthSession) {
        if (!session.isAuthenticated) return
        sharedPreferences.edit()
            .putString(KEY_USER_ID, session.userId.orEmpty())
            .putString(KEY_USERNAME, session.username)
            .putString(KEY_DISPLAY_NAME, session.displayName)
            .putString(KEY_PLANT_ID, session.plantId)
            .putString(KEY_PLANT_NAME, session.plantName)
            .putString(KEY_SUPPLIER_ID, session.supplierId)
            .putString(KEY_SUPPLIER_NAME, session.supplierName)
            .putString(KEY_DEVICE_ID, session.deviceId)
            .putString(KEY_CLIENT_DEVICE_ID, session.clientDeviceId)
            .putString(KEY_SUBSCRIPTION_STATUS, session.subscriptionStatus)
            .apply()
    }

    fun clear() {
        sharedPreferences.edit()
            .remove(KEY_PLANT_ID)
            .remove(KEY_PLANT_NAME)
            .remove(KEY_SUPPLIER_ID)
            .remove(KEY_SUPPLIER_NAME)
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_DEVICE_ID)
            .remove(KEY_CLIENT_DEVICE_ID)
            .remove(KEY_SUBSCRIPTION_STATUS)
            .apply()
    }

    fun cachedSession(): AuthSession? {
        val userId = sharedPreferences.getString(KEY_USER_ID, null).orEmpty()
        val username = sharedPreferences.getString(KEY_USERNAME, null).orEmpty()
        val displayName = sharedPreferences.getString(KEY_DISPLAY_NAME, null).orEmpty()
        val plantId = sharedPreferences.getString(KEY_PLANT_ID, null).orEmpty()
        val supplierId = sharedPreferences.getString(KEY_SUPPLIER_ID, null).orEmpty()
        if (userId.isBlank() && username.isBlank() && plantId.isBlank() && supplierId.isBlank()) return null
        return AuthSession(
            isLoading = false,
            isAuthenticated = true,
            userId = userId.ifBlank { null },
            username = username,
            displayName = displayName.ifBlank { username },
            plantId = plantId,
            deviceId = sharedPreferences.getString(KEY_DEVICE_ID, null).orEmpty(),
            supplierId = supplierId,
            supplierName = sharedPreferences.getString(KEY_SUPPLIER_NAME, null).orEmpty(),
            plantName = sharedPreferences.getString(KEY_PLANT_NAME, null).orEmpty(),
            clientDeviceId = sharedPreferences.getString(KEY_CLIENT_DEVICE_ID, null).orEmpty(),
            subscriptionStatus = sharedPreferences.getString(KEY_SUBSCRIPTION_STATUS, null).orEmpty(),
        )
    }

    fun currentScopeKey(): String {
        val plantId = sharedPreferences.getString(KEY_PLANT_ID, null).orEmpty()
        val supplierId = sharedPreferences.getString(KEY_SUPPLIER_ID, null).orEmpty()
        val clientDeviceId = sharedPreferences.getString(KEY_CLIENT_DEVICE_ID, null).orEmpty()
        if (plantId.isBlank() && supplierId.isBlank()) return LEGACY_SCOPE_KEY
        return listOf(
            SAAS_SCOPE_PREFIX,
            plantId.ifBlank { "-" },
            supplierId.ifBlank { "-" },
            clientDeviceId.ifBlank { "-" },
        ).joinToString(":")
    }

    companion object {
        const val LEGACY_SCOPE_KEY = "legacy:unscoped"
        private const val SAAS_SCOPE_PREFIX = "saas"
        private const val KEY_USER_ID = "local_scope_user_id"
        private const val KEY_USERNAME = "local_scope_username"
        private const val KEY_DISPLAY_NAME = "local_scope_display_name"
        private const val KEY_PLANT_ID = "local_scope_plant_id"
        private const val KEY_PLANT_NAME = "local_scope_plant_name"
        private const val KEY_SUPPLIER_ID = "local_scope_supplier_id"
        private const val KEY_SUPPLIER_NAME = "local_scope_supplier_name"
        private const val KEY_DEVICE_ID = "local_scope_device_id"
        private const val KEY_CLIENT_DEVICE_ID = "local_scope_client_device_id"
        private const val KEY_SUBSCRIPTION_STATUS = "local_scope_subscription_status"
    }
}
