package nl.eazysoftware.eazyrecyclingservice.config.security

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class TokenEncryptionServiceTest {

    // Valid 32-byte key (256 bits) for AES-256
    private val validKey = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })

    @Test
    fun `encrypt and decrypt should return original value`() {
        val service = TokenEncryptionService(EncryptionProperties(tokenEncryptionKey = validKey))
        val originalToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test-access-token"

        val encrypted = service.encrypt(originalToken)
        val decrypted = service.decrypt(encrypted)

        assertEquals(originalToken, decrypted)
    }

    @Test
    fun `encrypt should produce different ciphertext for same plaintext`() {
        val service = TokenEncryptionService(EncryptionProperties(tokenEncryptionKey = validKey))
        val token = "test-token"

        val encrypted1 = service.encrypt(token)
        val encrypted2 = service.encrypt(token)

        // Due to random IV, same plaintext should produce different ciphertext
        assertNotEquals(encrypted1, encrypted2)

        // But both should decrypt to the same value
        assertEquals(token, service.decrypt(encrypted1))
        assertEquals(token, service.decrypt(encrypted2))
    }

    @Test
    fun `decrypt should fail with tampered ciphertext`() {
        val service = TokenEncryptionService(EncryptionProperties(tokenEncryptionKey = validKey))
        val token = "test-token"

        val encrypted = service.encrypt(token)
        val tamperedBytes = Base64.getDecoder().decode(encrypted)
        tamperedBytes[tamperedBytes.size - 1] = (tamperedBytes[tamperedBytes.size - 1].toInt() xor 0xFF).toByte()
        val tampered = Base64.getEncoder().encodeToString(tamperedBytes)

        assertThrows<TokenEncryptionService.TokenDecryptionException> {
            service.decrypt(tampered)
        }
    }

    @Test
    fun `decrypt should fail with invalid base64`() {
        val service = TokenEncryptionService(EncryptionProperties(tokenEncryptionKey = validKey))

        assertThrows<TokenEncryptionService.TokenDecryptionException> {
            service.decrypt("not-valid-base64!!!")
        }
    }

    @Test
    fun `constructor should fail with invalid key length`() {
        val shortKey = Base64.getEncoder().encodeToString(ByteArray(16) { it.toByte() }) // 16 bytes instead of 32

        val service = TokenEncryptionService(EncryptionProperties(tokenEncryptionKey = shortKey))

        // The lazy initialization will fail when trying to use the key
        assertThrows<IllegalArgumentException> {
            service.encrypt("test")
        }
    }

    @Test
    fun `isConfigured should return true for valid key`() {
        val service = TokenEncryptionService(EncryptionProperties(tokenEncryptionKey = validKey))

        assertTrue(service.isConfigured())
    }

    @Test
    fun `isConfigured should return false for empty key`() {
        val service = TokenEncryptionService(EncryptionProperties(tokenEncryptionKey = ""))

        assertFalse(service.isConfigured())
    }

    @Test
    fun `isConfigured should return false for invalid key length`() {
        val shortKey = Base64.getEncoder().encodeToString(ByteArray(16) { it.toByte() })
        val service = TokenEncryptionService(EncryptionProperties(tokenEncryptionKey = shortKey))

        assertFalse(service.isConfigured())
    }

    @Test
    fun `encrypt should handle long tokens`() {
        val service = TokenEncryptionService(EncryptionProperties(tokenEncryptionKey = validKey))
        val longToken = "a".repeat(10000)

        val encrypted = service.encrypt(longToken)
        val decrypted = service.decrypt(encrypted)

        assertEquals(longToken, decrypted)
    }

    @Test
    fun `encrypt should handle special characters`() {
        val service = TokenEncryptionService(EncryptionProperties(tokenEncryptionKey = validKey))
        val tokenWithSpecialChars = "token+with/special=chars&more?stuff"

        val encrypted = service.encrypt(tokenWithSpecialChars)
        val decrypted = service.decrypt(encrypted)

        assertEquals(tokenWithSpecialChars, decrypted)
    }

    @Test
    fun `encrypt should handle unicode characters`() {
        val service = TokenEncryptionService(EncryptionProperties(tokenEncryptionKey = validKey))
        val unicodeToken = "token-with-émojis-🔐-and-中文"

        val encrypted = service.encrypt(unicodeToken)
        val decrypted = service.decrypt(encrypted)

        assertEquals(unicodeToken, decrypted)
    }
}
