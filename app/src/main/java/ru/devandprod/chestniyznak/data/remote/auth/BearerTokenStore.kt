package ru.devandprod.chestniyznak.data.remote.auth

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class BearerSession(
    val accessToken: String,
    val refreshToken: String,
)

@Singleton
class BearerTokenStore @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val json: Json,
) {
    fun load(): BearerSession? {
        val rawValue = sharedPreferences.getString(KEY_BEARER_SESSION, null) ?: return null
        val decodedValue = runCatching { decryptIfNeeded(rawValue) }.getOrElse {
            clear()
            return null
        }
        val stored = runCatching {
            json.decodeFromString<StoredBearerSessionDto>(decodedValue)
        }.getOrElse {
            clear()
            return null
        }

        if (stored.accessToken.isBlank() || stored.refreshToken.isBlank()) {
            clear()
            return null
        }

        if (decodedValue == rawValue) {
            save(stored.accessToken, stored.refreshToken)
        }

        return BearerSession(
            accessToken = stored.accessToken,
            refreshToken = stored.refreshToken,
        )
    }

    fun save(accessToken: String, refreshToken: String) {
        val payload = json.encodeToString(
            StoredBearerSessionDto(
                accessToken = accessToken,
                refreshToken = refreshToken,
            ),
        )
        sharedPreferences.edit()
            .putString(
                KEY_BEARER_SESSION,
                encrypt(payload),
            )
            .apply()
    }

    fun clear() {
        sharedPreferences.edit().remove(KEY_BEARER_SESSION).apply()
    }

    private companion object {
        const val KEY_BEARER_SESSION = "saas_bearer_session"
        const val KEY_ALIAS = "chz_saas_bearer_session"
        const val ENCRYPTED_PREFIX = "v1:"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        return ENCRYPTED_PREFIX +
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP) +
            ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    private fun decryptIfNeeded(value: String): String {
        if (!value.startsWith(ENCRYPTED_PREFIX)) return value
        val encryptedPayload = value.removePrefix(ENCRYPTED_PREFIX)
        val parts = encryptedPayload.split(":", limit = 2)
        require(parts.size == 2) { "Invalid encrypted bearer session" }

        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }
}

@Serializable
private data class StoredBearerSessionDto(
    val accessToken: String,
    val refreshToken: String,
)
