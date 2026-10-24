package app.marlboroadvance.mpvex.utils.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureKeyStoreHelperTest {

  @Test
  fun testPbkdf2HashFormat() {
    val pin = "1234"
    val hash = SecureKeyStoreHelper.hashPinPbkdf2(pin)
    assertTrue("Hash should start with pbkdf2 prefix", hash.startsWith("pbkdf2:12000:"))
    val parts = hash.split(":")
    org.junit.Assert.assertEquals(4, parts.size)
    org.junit.Assert.assertEquals("12000", parts[1])
    org.junit.Assert.assertEquals(32, parts[2].length) // 16 bytes = 32 hex chars
    org.junit.Assert.assertEquals(64, parts[3].length) // 32 bytes = 64 hex chars
  }

  @Test
  fun testPbkdf2VerifyCorrectPin() {
    val pin = "4567"
    val hash = SecureKeyStoreHelper.hashPinPbkdf2(pin)
    assertTrue("Verification should succeed for matching PIN", SecureKeyStoreHelper.verifyPin(pin, hash))
  }

  @Test
  fun testPbkdf2VerifyIncorrectPin() {
    val pin = "4567"
    val wrongPin = "4568"
    val hash = SecureKeyStoreHelper.hashPinPbkdf2(pin)
    assertFalse("Verification should fail for mismatched PIN", SecureKeyStoreHelper.verifyPin(wrongPin, hash))
  }

  @Test
  fun testLegacySha256Compatibility() {
    // Legacy unsalted SHA-256 for "1234"
    // echo -n "1234" | sha256sum -> 03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4
    val legacyHash = "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4"
    assertTrue("Should detect legacy SHA-256 hash", SecureKeyStoreHelper.isLegacySha256(legacyHash))
    assertTrue("Should verify correct PIN against legacy hash", SecureKeyStoreHelper.verifyPin("1234", legacyHash))
    assertFalse("Should reject wrong PIN against legacy hash", SecureKeyStoreHelper.verifyPin("0000", legacyHash))
  }

  @Test
  fun testSaltRandomness() {
    val salt1 = SecureKeyStoreHelper.generateSalt()
    val salt2 = SecureKeyStoreHelper.generateSalt()
    assertFalse("Salts must be randomly generated and distinct", salt1.contentEquals(salt2))
  }
}
