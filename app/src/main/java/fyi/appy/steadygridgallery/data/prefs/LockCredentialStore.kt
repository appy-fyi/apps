package fyi.appy.steadygridgallery.data.prefs

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private const val PREFS_FILE = "steady_gallery_lock_credential"
private const val KEY_SALT = "pinSaltBase64"
private const val KEY_HASH = "pinHashBase64"
private const val KEY_BIOMETRIC_ENABLED = "biometricEnabled"
private const val KEY_CREATED_AT = "createdAt"

private const val PBKDF2_ITERATIONS = 120_000
private const val PBKDF2_KEY_BITS = 256

class LockCredentialStore(context: Context) {
    private val prefs = SecureStorage.open(context, PREFS_FILE)

    fun hasCredential(): Boolean = prefs.contains(KEY_HASH)

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun createCredential(pin: String, biometricEnabled: Boolean) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(pin, salt)
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putBoolean(KEY_BIOMETRIC_ENABLED, biometricEnabled)
            .putLong(KEY_CREATED_AT, System.currentTimeMillis())
            .apply()
    }

    fun clearCredential() {
        prefs.edit().clear().apply()
    }

    fun verifyPin(pin: String): Boolean {
        val saltBase64 = prefs.getString(KEY_SALT, null) ?: return false
        val expectedHashBase64 = prefs.getString(KEY_HASH, null) ?: return false
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val expectedHash = Base64.decode(expectedHashBase64, Base64.NO_WRAP)
        val actualHash = pbkdf2(pin, salt)
        return MessageDigest.isEqual(actualHash, expectedHash)
    }

    private fun pbkdf2(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }
}
