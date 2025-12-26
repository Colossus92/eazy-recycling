package nl.eazysoftware.eazyrecyclingservice.domain.service

import nl.eazysoftware.eazyrecyclingservice.config.ExactOnlineProperties
import nl.eazysoftware.eazyrecyclingservice.repository.exact.ExactTokenDto
import nl.eazysoftware.eazyrecyclingservice.repository.exact.ExactTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.time.Instant
import java.util.*

@Service
class ExactOAuthService(
    private val exactProperties: ExactOnlineProperties,
    private val tokenRepository: ExactTokenRepository,
    private val restTemplate: RestTemplate = RestTemplate()
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val stateStore = mutableMapOf<String, Instant>()

    /**
     * Build the authorization URL for the user to grant access
     */
    fun buildAuthorizationUrl(): AuthorizationUrlResponse {
        val state = UUID.randomUUID().toString()

        // Store state with expiration (5 minutes)
        stateStore[state] = Instant.now().plusSeconds(300)

        val authUrl = UriComponentsBuilder
            .fromUriString(exactProperties.authorizationEndpoint)
            .queryParam("client_id", exactProperties.clientId)
            .queryParam("redirect_uri", exactProperties.redirectUri)
            .queryParam("response_type", "code")
            .queryParam("state", state)
            .queryParam("force_login", "1")
            .build()
            .toUriString()

        return AuthorizationUrlResponse(authUrl, state)
    }

    /**
     * Verify state parameter to prevent CSRF attacks
     */
    fun verifyState(state: String): Boolean {
        val storedExpiry = stateStore[state] ?: return false
        logger.info("State: $state, expiry: $storedExpiry")

        // Check if state is expired
        if (Instant.now().isAfter(storedExpiry)) {
            stateStore.remove(state)
            return false
        }

        // Remove state after verification (one-time use)
        stateStore.remove(state)
        return true
    }

    /**
     * Exchange authorization code for access and refresh tokens
     */
    fun exchangeCodeForTokens(code: String): TokenResponse {
        val requestBody: MultiValueMap<String, String> = LinkedMultiValueMap()
        requestBody.add("grant_type", "authorization_code")
        requestBody.add("code", code)
        requestBody.add("redirect_uri", exactProperties.redirectUri)
        requestBody.add("client_id", exactProperties.clientId)
        requestBody.add("client_secret", exactProperties.clientSecret)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val request = HttpEntity(requestBody, headers)

        try {
            val response = restTemplate.postForEntity(
                exactProperties.tokenEndpoint,
                request,
                TokenResponse::class.java
            )

            val tokenResponse = response.body ?: throw IllegalStateException("Empty token response from Exact Online")

            // Store tokens in database
            saveTokens(tokenResponse)

            logger.debug("Successfully exchanged authorization code for tokens")
            return tokenResponse

        } catch (e: HttpClientErrorException) {
            logger.error("Failed to exchange code for tokens: ${e.statusCode} - ${e.responseBodyAsString}")
            throw ExactOAuthException("Failed to exchange code for tokens: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Unexpected error during token exchange", e)
            throw ExactOAuthException("Unexpected error during token exchange", e)
        }
    }

    /**
     * Refresh the access token using the refresh token
     */
    fun refreshAccessToken(): TokenResponse {
        val currentToken = tokenRepository.findFirstByOrderByCreatedAtDesc()
            ?: throw ExactOAuthException("No tokens found to refresh")

        return refreshAccessToken(currentToken.refreshToken)
    }

    /**
     * Refresh the access token using a specific refresh token.
     *
     * Retries automatically on 503/504 errors (e.g., Exact Online maintenance windows)
     * with exponential backoff covering up to ~1 hour.
     *
     * Maintenance window: 04:00-04:30 CET daily (30 minutes)
     * Default retry schedule with 1.5x multiplier and 10min cap:
     * 1m, 2.5m, 4.75m, 8.1m, 13.2m, 23.2m, 33.2m, 43.2m, 53.2m, 63.2m (total ~63min)
     *
     * Retry settings are configurable via properties for testing:
     * - exact.oauth.retry.max-attempts (default: 10)
     * - exact.oauth.retry.delay (default: 60000ms)
     * - exact.oauth.retry.multiplier (default: 1.5)
     * - exact.oauth.retry.max-delay (default: 600000ms)
     */
    @Retryable(
        retryFor = [ExactMaintenanceException::class],
        maxAttemptsExpression = "\${exact.oauth.retry.max-attempts:10}",
        backoff = Backoff(
            delayExpression = "\${exact.oauth.retry.delay:60000}",
            multiplierExpression = "\${exact.oauth.retry.multiplier:1.5}",
            maxDelayExpression = "\${exact.oauth.retry.max-delay:600000}"
        )
    )
    fun refreshAccessToken(refreshToken: String): TokenResponse {
        val requestBody: MultiValueMap<String, String> = LinkedMultiValueMap()
        requestBody.add("grant_type", "refresh_token")
        requestBody.add("refresh_token", refreshToken)
        requestBody.add("client_id", exactProperties.clientId)
        requestBody.add("client_secret", exactProperties.clientSecret)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val request = HttpEntity(requestBody, headers)

        try {
            val response = restTemplate.postForEntity(
                exactProperties.tokenEndpoint,
                request,
                TokenResponse::class.java
            )

            val tokenResponse = response.body ?: throw IllegalStateException("Empty token response from Exact Online")

            // Update tokens in database
            saveTokens(tokenResponse)

            logger.debug("Successfully refreshed access token")
            return tokenResponse

        } catch (e: HttpServerErrorException) {
            // Handle 503/504 errors (maintenance window) - these will be retried
            when (e.statusCode) {
                HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT -> {
                    logger.warn("Exact Online temporarily unavailable (${e.statusCode}), will retry: ${e.responseBodyAsString}")
                    throw ExactMaintenanceException("Exact Online is under maintenance or temporarily unavailable", e)
                }
                else -> {
                    logger.error("Server error during token refresh: ${e.statusCode} - ${e.responseBodyAsString}")
                    throw ExactOAuthException("Server error during token refresh: ${e.message}", e)
                }
            }
        } catch (e: HttpClientErrorException) {
            // 4xx errors are permanent - don't retry
            logger.error("Client error during token refresh: ${e.statusCode} - ${e.responseBodyAsString}")
            throw ExactOAuthException("Failed to refresh token: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Unexpected error during token refresh", e)
            throw ExactOAuthException("Unexpected error during token refresh", e)
        }
    }

    /**
     * Get the current valid access token, refreshing if necessary
     */
    fun getValidAccessToken(): String {
        val currentToken = tokenRepository.findFirstByOrderByCreatedAtDesc()
            ?: throw ExactOAuthException("No tokens available. Please authenticate first.")

        // Check if token expires within the next 30 seconds
        val expiryThreshold = Instant.now().plusSeconds(30)

        if (currentToken.expiresAt.isBefore(expiryThreshold)) {
            logger.info("Token is expiring soon, refreshing...")
            refreshAccessToken(currentToken.refreshToken)
            return tokenRepository.findFirstByOrderByCreatedAtDesc()!!.accessToken
        }

        return currentToken.accessToken
    }

    /**
     * Check if we have valid tokens stored
     */
    fun hasValidTokens(): Boolean {
        val currentToken = tokenRepository.findFirstByOrderByCreatedAtDesc() ?: return false
        return currentToken.expiresAt.isAfter(Instant.now())
    }

    /**
     * Save or update tokens in the database
     */
    private fun saveTokens(tokenResponse: TokenResponse) {
        val expiresAt = Instant.now().plusSeconds(tokenResponse.expiresIn.toLong())

        // For simplicity, always create a new token entry (could update existing instead)
        val tokenDto = ExactTokenDto(
            accessToken = tokenResponse.accessToken,
            refreshToken = tokenResponse.refreshToken,
            tokenType = tokenResponse.tokenType,
            expiresAt = expiresAt,
            updatedAt = Instant.now()
        )

        tokenRepository.save(tokenDto)
      logger.debug("Saved tokens to database, expires at: {}", expiresAt)
    }

    data class AuthorizationUrlResponse(
        val authorizationUrl: String,
        val state: String
    )

    data class TokenResponse(
        val access_token: String,
        val refresh_token: String,
        val token_type: String,
        val expires_in: Int
    ) {
        // Provide getters for Jackson mapping
        val accessToken: String get() = access_token
        val refreshToken: String get() = refresh_token
        val tokenType: String get() = token_type
        val expiresIn: Int get() = expires_in
    }

    class ExactOAuthException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

    /**
     * Exception thrown when Exact Online is under maintenance or temporarily unavailable.
     * This exception triggers retry with exponential backoff.
     */
    class ExactMaintenanceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
}
