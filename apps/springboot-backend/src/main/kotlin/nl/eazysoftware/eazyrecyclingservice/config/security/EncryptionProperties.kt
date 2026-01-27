package nl.eazysoftware.eazyrecyclingservice.config.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration properties for token encryption.
 *
 * The encryption key should be stored as a GitHub secret and injected via environment variable.
 *
 * To generate a new key: openssl rand -base64 32
 *
 * GitHub Secret: TOKEN_ENCRYPTION_KEY
 */
@Configuration
@ConfigurationProperties(prefix = "security.encryption")
data class EncryptionProperties(
    /**
     * Base64-encoded 32-byte (256-bit) AES encryption key.
     * Required for encrypting OAuth tokens at rest.
     */
    var tokenEncryptionKey: String = ""
)
