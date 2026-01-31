package nl.eazysoftware.eazyrecyclingservice.domain.service

import nl.eazysoftware.eazyrecyclingservice.config.ExactOnlineProperties
import nl.eazysoftware.eazyrecyclingservice.config.security.EncryptionProperties
import nl.eazysoftware.eazyrecyclingservice.config.security.TokenEncryptionService
import nl.eazysoftware.eazyrecyclingservice.repository.exact.ExactTokenDto
import nl.eazysoftware.eazyrecyclingservice.repository.exact.ExactTokenRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.retry.annotation.EnableRetry
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import java.util.*

/**
 * Tests for ExactOAuthService retry mechanism.
 * Verifies exponential backoff behavior during Exact Online maintenance windows.
 *
 * Note: This is a Spring integration test because @Retryable requires Spring AOP.
 * Uses fast retry settings (10ms delay, 3 max attempts) for quick test execution.
 */
@SpringJUnitConfig(ExactOAuthServiceRetryTest.TestConfig::class)
@TestPropertySource(properties = [
    "exact.oauth.retry.max-attempts=3",
    "exact.oauth.retry.delay=10",
    "exact.oauth.retry.multiplier=1.0",
    "exact.oauth.retry.max-delay=10"
])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ExactOAuthServiceRetryTest {

    @TestConfiguration
    @EnableRetry
    class TestConfig {
        @Bean
        fun exactOnlineProperties(): ExactOnlineProperties {
            return ExactOnlineProperties(
                clientId = "test-client-id",
                clientSecret = "test-client-secret",
                redirectUri = "http://localhost:8080/callback",
                authorizationEndpoint = "https://start.exactonline.nl/api/oauth2/auth",
                tokenEndpoint = "https://start.exactonline.nl/api/oauth2/token"
            )
        }

        @Bean
        @Primary
        fun tokenRepository(): ExactTokenRepository {
            return mock()
        }

        @Bean
        @Primary
        fun restTemplate(): RestTemplate {
            return mock()
        }

        @Bean
        fun encryptionProperties(): EncryptionProperties {
            // Test key: 32 bytes base64 encoded (openssl rand -base64 32)
            return EncryptionProperties(
                tokenEncryptionKey = "U5hCWgeAe59r7CJjkiMtyZTnCuKwrLms/x8+U7CttHo="
            )
        }

        @Bean
        fun tokenEncryptionService(encryptionProperties: EncryptionProperties): TokenEncryptionService {
            return TokenEncryptionService(encryptionProperties)
        }

        @Bean
        fun exactOAuthService(
            exactProperties: ExactOnlineProperties,
            tokenRepository: ExactTokenRepository,
            encryptionService: TokenEncryptionService,
            restTemplate: RestTemplate
        ): ExactOAuthService {
            return ExactOAuthService(exactProperties, tokenRepository, encryptionService, restTemplate)
        }
    }

    @Autowired
    private lateinit var service: ExactOAuthService

    @Autowired
    private lateinit var tokenRepository: ExactTokenRepository

    @Autowired
    private lateinit var restTemplate: RestTemplate

    @Autowired
    private lateinit var encryptionService: TokenEncryptionService

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(restTemplate, tokenRepository)
    }

    private fun createMockTokenDto(): ExactTokenDto {
        // Create a token with encrypted refresh token that the service will decrypt
        val encryptedRefreshToken = encryptionService.encrypt("test-refresh-token")
        val encryptedAccessToken = encryptionService.encrypt("old-access-token")
        return ExactTokenDto(
            id = UUID.randomUUID(),
            accessToken = encryptedAccessToken,
            refreshToken = encryptedRefreshToken,
            tokenType = "bearer",
            expiresAt = java.time.Instant.now().plusSeconds(600),
            createdAt = java.time.Instant.now(),
            updatedAt = java.time.Instant.now()
        )
    }

    @Test
    fun `refreshAccessToken should retry on 503 Service Unavailable`() {
        var attemptCount = 0

        // Mock token repository to return a token with encrypted refresh token
        whenever(tokenRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(createMockTokenDto())

        // First 2 attempts fail with 503, third succeeds
        whenever(restTemplate.postForEntity(any<String>(), any<HttpEntity<*>>(), eq(ExactOAuthService.TokenResponse::class.java)))
            .thenAnswer {
                attemptCount++
                if (attemptCount <= 2) {
                    throw HttpServerErrorException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Server maintenance: Due to server maintenance the Exact Online server is unavailable"
                    )
                } else {
                    ResponseEntity.ok(
                        ExactOAuthService.TokenResponse(
                            access_token = "new-access-token",
                            refresh_token = "new-refresh-token",
                            token_type = "bearer",
                            expires_in = 600
                        )
                    )
                }
            }

        // Execute
        val result = service.refreshAccessToken()

        // Verify
        assertEquals("new-access-token", result.accessToken)
        assertEquals(3, attemptCount) // Should have retried twice
        verify(restTemplate, times(3)).postForEntity(any<String>(), any<HttpEntity<*>>(), eq(ExactOAuthService.TokenResponse::class.java))
    }

    @Test
    fun `refreshAccessToken should retry on 504 Gateway Timeout`() {
        var attemptCount = 0

        // Mock token repository to return a token with encrypted refresh token
        whenever(tokenRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(createMockTokenDto())

        // First attempt fails with 504, second succeeds
        whenever(restTemplate.postForEntity(any<String>(), any<HttpEntity<*>>(), eq(ExactOAuthService.TokenResponse::class.java)))
            .thenAnswer {
                attemptCount++
                if (attemptCount == 1) {
                    throw HttpServerErrorException(HttpStatus.GATEWAY_TIMEOUT, "Gateway Timeout")
                } else {
                    ResponseEntity.ok(
                        ExactOAuthService.TokenResponse(
                            access_token = "new-access-token",
                            refresh_token = "new-refresh-token",
                            token_type = "bearer",
                            expires_in = 600
                        )
                    )
                }
            }

        // Execute
        val result = service.refreshAccessToken()

        // Verify
        assertEquals("new-access-token", result.accessToken)
        assertEquals(2, attemptCount)
        verify(restTemplate, times(2)).postForEntity(any<String>(), any<HttpEntity<*>>(), eq(ExactOAuthService.TokenResponse::class.java))
    }

    @Test
    fun `refreshAccessToken should NOT retry on 401 Unauthorized`() {
        // Mock token repository to return a token with encrypted refresh token
        whenever(tokenRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(createMockTokenDto())

        // Simulate 401 error
        whenever(restTemplate.postForEntity(any<String>(), any<HttpEntity<*>>(), eq(ExactOAuthService.TokenResponse::class.java)))
            .thenThrow(
                org.springframework.web.client.HttpClientErrorException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid refresh token"
                )
            )

        // Execute & Verify - should throw immediately without retry
        assertThrows(ExactOAuthService.ExactOAuthException::class.java) {
            service.refreshAccessToken()
        }

        // Should only attempt once (no retry on 401)
        verify(restTemplate, times(1)).postForEntity(any<String>(), any<HttpEntity<*>>(), eq(ExactOAuthService.TokenResponse::class.java))
    }

    @Test
    fun `refreshAccessToken should NOT retry on 400 Bad Request`() {
        // Mock token repository to return a token with encrypted refresh token
        whenever(tokenRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(createMockTokenDto())

        // Simulate 400 error
        whenever(restTemplate.postForEntity(any<String>(), any<HttpEntity<*>>(), eq(ExactOAuthService.TokenResponse::class.java)))
            .thenThrow(
                org.springframework.web.client.HttpClientErrorException(
                    HttpStatus.BAD_REQUEST,
                    "Bad request"
                )
            )

        // Execute & Verify
        assertThrows(ExactOAuthService.ExactOAuthException::class.java) {
            service.refreshAccessToken()
        }

        // Should only attempt once (no retry on 400)
        verify(restTemplate, times(1)).postForEntity(any<String>(), any<HttpEntity<*>>(), eq(ExactOAuthService.TokenResponse::class.java))
    }

    @Test
    fun `refreshAccessToken should exhaust retries after max attempts`() {
        // Mock token repository to return a token with encrypted refresh token
        whenever(tokenRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(createMockTokenDto())

        // Always return 503
        whenever(restTemplate.postForEntity(any<String>(), any<HttpEntity<*>>(), eq(ExactOAuthService.TokenResponse::class.java)))
            .thenThrow(
                HttpServerErrorException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Server under maintenance"
                )
            )

        // Execute & Verify - should eventually throw after exhausting retries
        assertThrows(ExactOAuthService.ExactMaintenanceException::class.java) {
            service.refreshAccessToken()
        }

        // Should attempt 3 times (maxAttempts = 3 in test properties)
        verify(restTemplate, times(3)).postForEntity(any<String>(), any<HttpEntity<*>>(), eq(ExactOAuthService.TokenResponse::class.java))
    }

    @Test
    fun `refreshAccessToken should save tokens after successful retry`() {
        val mockToken = createMockTokenDto()

        // Mock token repository to return a token with encrypted refresh token
        whenever(tokenRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(mockToken)

        // First attempt fails, second succeeds
        whenever(restTemplate.postForEntity(any<String>(), any<HttpEntity<*>>(), eq(ExactOAuthService.TokenResponse::class.java)))
            .thenThrow(HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE))
            .thenReturn(
                ResponseEntity.ok(
                    ExactOAuthService.TokenResponse(
                        access_token = "new-access-token",
                        refresh_token = "new-refresh-token",
                        token_type = "bearer",
                        expires_in = 600
                    )
                )
            )

        whenever(tokenRepository.save(any<ExactTokenDto>())).thenReturn(mockToken)

        // Execute
        val result = service.refreshAccessToken()

        // Verify tokens were saved
        assertEquals("new-access-token", result.accessToken)
        verify(tokenRepository).save(any<ExactTokenDto>())
    }

    @Test
    fun `refreshAccessToken should NOT retry on 500 Internal Server Error`() {
        // Mock token repository to return a token with encrypted refresh token
        whenever(tokenRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(createMockTokenDto())

        // Simulate 500 error (not 503/504)
        whenever(restTemplate.postForEntity(any<String>(), any<HttpEntity<*>>(), eq(ExactOAuthService.TokenResponse::class.java)))
            .thenThrow(
                HttpServerErrorException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal server error"
                )
            )

        // Execute & Verify - should throw ExactOAuthException (not retryable)
        assertThrows(ExactOAuthService.ExactOAuthException::class.java) {
            service.refreshAccessToken()
        }

        // Should only attempt once (500 is not in retry list)
        verify(restTemplate, times(1)).postForEntity(any<String>(), any<HttpEntity<*>>(), eq(ExactOAuthService.TokenResponse::class.java))
    }
}
