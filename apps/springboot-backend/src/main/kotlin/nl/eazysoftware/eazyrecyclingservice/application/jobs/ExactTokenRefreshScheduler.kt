package nl.eazysoftware.eazyrecyclingservice.application.jobs

import nl.eazysoftware.eazyrecyclingservice.domain.service.ExactOAuthService
import nl.eazysoftware.eazyrecyclingservice.repository.exact.ExactTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Scheduled job to automatically refresh Exact Online OAuth tokens before they expire.
 *
 * This job runs every 10 minutes and checks if tokens are expiring within the next 30 seconds.
 * If so, it refreshes them proactively.
 */
@Component
class ExactTokenRefreshScheduler(
    private val exactOAuthService: ExactOAuthService,
    private val tokenRepository: ExactTokenRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Run every hour to check and refresh tokens if needed
     */
    @Scheduled(fixedRate = 10000) // 10 minutes in milliseconds
    fun refreshExpiringTokens() {
        try {
            val currentToken = tokenRepository.findFirstByOrderByCreatedAtDesc()

            if (currentToken == null) {
                logger.debug("No Exact Online tokens found, skipping refresh")
                return
            }

            // Check if token expires within the next 30
            val expiryThreshold = Instant.now().plusSeconds(30)

            if (currentToken.expiresAt.isBefore(expiryThreshold)) {
                logger.info("Exact Online token is expiring soon (expires at: ${currentToken.expiresAt}), refreshing...")
                exactOAuthService.refreshAccessToken(currentToken.refreshToken)
                logger.info("Successfully refreshed Exact Online token")
            } else {
              logger.debug("Exact Online token is still valid (expires at: {})", currentToken.expiresAt)
            }

        } catch (e: Exception) {
            logger.error("Failed to refresh Exact Online token automatically", e)
            // Don't throw the exception - we'll try again on the next scheduled run
        }
    }
}
