package com.zenhold.app.data.cloud

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

data class CloudCredentials(val userId: String, val token: String)

/** Keeps the bearer token encrypted by an Android Keystore key that cannot leave this device. */
@Singleton
class CloudCredentialStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences("aquaflow_cloud_credentials", Context.MODE_PRIVATE)

    @Synchronized
    fun read(): CloudCredentials? = runCatching {
        val userId = preferences.getString(KEY_USER_ID, null) ?: return null
        val payload = preferences.getString(KEY_TOKEN, null) ?: return null
        CloudCredentials(userId, decrypt(payload))
    }.getOrElse {
        clear()
        null
    }

    @Synchronized
    fun save(credentials: CloudCredentials) {
        preferences.edit()
            .putString(KEY_USER_ID, credentials.userId)
            .putString(KEY_TOKEN, encrypt(credentials.token))
            .apply()
    }

    @Synchronized
    fun clear() {
        preferences.edit().clear().apply()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(payload: String): String {
        val bytes = Base64.decode(payload, Base64.NO_WRAP)
        require(bytes.size > IV_SIZE)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, bytes.copyOfRange(0, IV_SIZE)))
        return cipher.doFinal(bytes.copyOfRange(IV_SIZE, bytes.size)).toString(Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_TOKEN = "encrypted_token"
        const val KEY_ALIAS = "aquaflow_cloud_token_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
    }
}
