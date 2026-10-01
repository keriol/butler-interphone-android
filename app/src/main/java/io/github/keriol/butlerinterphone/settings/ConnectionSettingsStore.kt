package io.github.keriol.butlerinterphone.settings

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class BifrostConnectionSettings(
    val protocol: String = "http",
    val host: String = "",
    val port: String = "",
    val token: String = "",
    val defaultButlerName: String = "",
)

interface ConnectionSettingsStore {
    fun load(): BifrostConnectionSettings?
    fun save(settings: BifrostConnectionSettings)
}

class AndroidConnectionSettingsStore(
    context: Context,
) : ConnectionSettingsStore {
    private val preferences = context.getSharedPreferences(
        "bifrost_connection",
        Context.MODE_PRIVATE,
    )

    override fun load(): BifrostConnectionSettings? {
        if (!preferences.contains(KEY_HOST) &&
            !preferences.contains(KEY_TOKEN_CIPHERTEXT)
        ) {
            return null
        }

        return BifrostConnectionSettings(
            protocol = preferences.getString(KEY_PROTOCOL, "http") ?: "http",
            host = preferences.getString(KEY_HOST, "").orEmpty(),
            port = preferences.getString(KEY_PORT, "").orEmpty(),
            token = decryptToken(),
            defaultButlerName = preferences.getString(
                KEY_DEFAULT_BUTLER,
                "",
            ).orEmpty(),
        )
    }

    override fun save(settings: BifrostConnectionSettings) {
        val encrypted = encryptToken(settings.token)

        preferences.edit()
            .putString(KEY_PROTOCOL, settings.protocol)
            .putString(KEY_HOST, settings.host)
            .putString(KEY_PORT, settings.port)
            .putString(KEY_TOKEN_IV, encrypted.first)
            .putString(KEY_TOKEN_CIPHERTEXT, encrypted.second)
            .putString(
                KEY_DEFAULT_BUTLER,
                settings.defaultButlerName,
            )
            .apply()
    }

    private fun encryptToken(token: String): Pair<String, String> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val ciphertext = cipher.doFinal(token.toByteArray(Charsets.UTF_8))

        return Base64.encodeToString(
            cipher.iv,
            Base64.NO_WRAP,
        ) to Base64.encodeToString(
            ciphertext,
            Base64.NO_WRAP,
        )
    }

    private fun decryptToken(): String {
        val ivValue = preferences.getString(KEY_TOKEN_IV, null)
            ?: return ""
        val ciphertextValue = preferences.getString(
            KEY_TOKEN_CIPHERTEXT,
            null,
        ) ?: return ""

        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(
                    128,
                    Base64.decode(ivValue, Base64.NO_WRAP),
                ),
            )
            String(
                cipher.doFinal(
                    Base64.decode(
                        ciphertextValue,
                        Base64.NO_WRAP,
                    )
                ),
                Charsets.UTF_8,
            )
        } catch (_: Exception) {
            ""
        }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply {
            load(null)
        }

        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let {
            return it
        }

        return KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE,
        ).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(
                        KeyProperties.ENCRYPTION_PADDING_NONE
                    )
                    .build()
            )
        }.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "butler_interphone_bifrost_token"
        const val TRANSFORMATION = "AES/GCM/NoPadding"

        const val KEY_PROTOCOL = "protocol"
        const val KEY_HOST = "host"
        const val KEY_PORT = "port"
        const val KEY_TOKEN_IV = "token_iv"
        const val KEY_TOKEN_CIPHERTEXT = "token_ciphertext"
        const val KEY_DEFAULT_BUTLER = "default_butler"
    }
}
