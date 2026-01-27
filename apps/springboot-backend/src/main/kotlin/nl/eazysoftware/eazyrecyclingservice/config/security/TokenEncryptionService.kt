package nl.eazysoftware.eazyrecyclingservice.config.security

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Service for encrypting and decrypting sensitive tokens using AES-256-GCM.
 *
 * AES-256-GCM provides:
 * - Strong encryption (256-bit key)
 * - Authenticated encryption (integrity verification)
 * - Protection against tampering
 *
 * The encryption key must be provided via environment variable: TOKEN_ENCRYPTION_KEY
 * Key format: Base64-encoded 32-byte (256-bit) key
 *
 * To generate a key: openssl rand -base64 32
 */
@Service
class TokenEncryptionService(
    private val encryptionProperties: EncryptionProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12 // 96 bits - recommended for GCM
        private const val GCM_TAG_LENGTH = 128 // 128 bits - authentication tag
        private const val KEY_ALGORITHM = "AES"
    }

    private val secretKey: SecretKeySpec by lazy {
        val keyBytes = Base64.getDecoder().decode(encryptionProperties.tokenEncryptionKey)
        require(keyBytes.size == 32) {
            "Encryption key must be 32 bytes (256 bits). Got ${keyBytes.size} bytes. " +
                "Generate with: openssl rand -base64 32"
        }
        SecretKeySpec(keyBytes, KEY_ALGORITHM)
    }

    /**
     * Encrypts a plaintext token.
     *
     * @param plaintext The token to encrypt
     * @return Base64-encoded string containing IV + ciphertext
     */
    fun encrypt(plaintext: String): String {
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Prepend IV to ciphertext for storage
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)

        return Base64.getEncoder().encodeToString(combined)
    }

    /**
     * Decrypts an encrypted token.
     *
     * @param encrypted Base64-encoded string containing IV + ciphertext
     * @return The original plaintext token
     * @throws TokenDecryptionException if decryption fails
     */
    fun decrypt(encrypted: String): String {
        try {
            val combined = Base64.getDecoder().decode(encrypted)

            require(combined.size > GCM_IV_LENGTH) {
                "Encrypted data too short"
            }

            // Extract IV and ciphertext
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val plaintext = cipher.doFinal(ciphertext)
            return String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            logger.error("Failed to decrypt token", e)
            throw TokenDecryptionException("Failed to decrypt token: ${e.message}", e)
        }
    }

    /**
     * Checks if the encryption service is properly configured.
     */
    fun isConfigured(): Boolean {
        return try {
            encryptionProperties.tokenEncryptionKey.isNotBlank() &&
                Base64.getDecoder().decode(encryptionProperties.tokenEncryptionKey).size == 32
        } catch (e: Exception) {
            false
        }
    }

    class TokenDecryptionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
}
