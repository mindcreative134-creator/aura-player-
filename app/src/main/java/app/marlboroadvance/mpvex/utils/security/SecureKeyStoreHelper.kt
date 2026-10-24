package app.marlboroadvance.mpvex.utils.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

/**
 * Provides hardware-backed (AndroidKeyStore) AES-256-GCM encryption
 * and cryptographically strong PBKDF2-HMAC-SHA256 password hashing.
 */
object SecureKeyStoreHelper {
  private const val TAG = "SecureKeyStoreHelper"
  private const val ANDROID_KEYSTORE = "AndroidKeyStore"
  private const val TRANSFORMATION = "AES/GCM/NoPadding"
  private const val GCM_TAG_LENGTH = 128
  private const val PBKDF2_ITERATIONS = 12000
  private const val PBKDF2_KEY_LENGTH = 256 // bits

  private val secureRandom = SecureRandom()

  // -------------------------------------------------------------
  // PIN Hashing with Salt & PBKDF2
  // -------------------------------------------------------------

  fun generateSalt(size: Int = 16): ByteArray {
    val salt = ByteArray(size)
    secureRandom.nextBytes(salt)
    return salt
  }

  fun hashPinPbkdf2(pin: String, salt: ByteArray = generateSalt()): String {
    val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val hash = factory.generateSecret(spec).encoded
    val saltHex = salt.joinToString("") { "%02x".format(it) }
    val hashHex = hash.joinToString("") { "%02x".format(it) }
    return "pbkdf2:$PBKDF2_ITERATIONS:$saltHex:$hashHex"
  }

  fun verifyPin(pin: String, storedHash: String): Boolean {
    if (storedHash.isBlank()) return false

    // Check if it's new PBKDF2 format
    if (storedHash.startsWith("pbkdf2:")) {
      val parts = storedHash.split(":")
      if (parts.size != 4) return false
      val iterations = parts[1].toIntOrNull() ?: return false
      val saltHex = parts[2]
      val expectedHashHex = parts[3]
      val salt = hexToBytes(saltHex)

      val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, PBKDF2_KEY_LENGTH)
      val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
      val actualHash = factory.generateSecret(spec).encoded
      val actualHashHex = actualHash.joinToString("") { "%02x".format(it) }

      return constantTimeEquals(actualHashHex, expectedHashHex)
    }

    // Legacy unsalted SHA-256 format (64 hex characters)
    if (isLegacySha256(storedHash)) {
      val legacyHash = hashSha256(pin)
      return constantTimeEquals(legacyHash, storedHash)
    }

    return false
  }

  fun isLegacySha256(storedHash: String): Boolean {
    return storedHash.length == 64 && storedHash.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
  }

  private fun hashSha256(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
    return hashBytes.joinToString("") { "%02x".format(it) }
  }

  private fun constantTimeEquals(a: String, b: String): Boolean {
    if (a.length != b.length) return false
    var result = 0
    for (i in a.indices) {
      result = result or (a[i].code xor b[i].code)
    }
    return result == 0
  }

  private fun hexToBytes(hex: String): ByteArray {
    val bytes = ByteArray(hex.length / 2)
    for (i in bytes.indices) {
      val index = i * 2
      bytes[i] = hex.substring(index, index + 2).toInt(16).toByte()
    }
    return bytes
  }

  // -------------------------------------------------------------
  // AndroidKeyStore AES-256-GCM Encryption / Decryption
  // -------------------------------------------------------------

  private fun getOrCreateSecretKey(alias: String): SecretKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    if (keyStore.containsAlias(alias)) {
      return (keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
    }

    val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
    val spec = KeyGenParameterSpec.Builder(
      alias,
      KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setKeySize(256)
      .build()

    keyGenerator.init(spec)
    return keyGenerator.generateKey()
  }

  /**
   * Encrypts plainText using AndroidKeyStore.
   * Returns a Base64-encoded string in the format "iv:ciphertext".
   */
  fun encrypt(alias: String, plainText: String): String? {
    if (plainText.isEmpty()) return ""
    return try {
      val secretKey = getOrCreateSecretKey(alias)
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(Cipher.ENCRYPT_MODE, secretKey)
      val iv = cipher.iv
      val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
      val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
      val cipherBase64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
      "$ivBase64:$cipherBase64"
    } catch (e: Exception) {
      Log.e(TAG, "Failed to encrypt with KeyStore alias: $alias", e)
      null
    }
  }

  /**
   * Decrypts encryptedData previously generated by [encrypt].
   */
  fun decrypt(alias: String, encryptedData: String): String? {
    if (encryptedData.isEmpty()) return ""
    return try {
      val parts = encryptedData.split(":")
      if (parts.size != 2) return null
      val iv = Base64.decode(parts[0], Base64.NO_WRAP)
      val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)

      val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
      if (!keyStore.containsAlias(alias)) return null
      val secretKey = (keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey

      val cipher = Cipher.getInstance(TRANSFORMATION)
      val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
      cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
      val plainBytes = cipher.doFinal(cipherBytes)
      String(plainBytes, Charsets.UTF_8)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to decrypt with KeyStore alias: $alias", e)
      null
    }
  }
}
