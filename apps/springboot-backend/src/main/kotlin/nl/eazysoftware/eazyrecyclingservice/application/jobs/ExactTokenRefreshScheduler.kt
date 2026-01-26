package nl.eazysoftware.eazyrecyclingservice.application.jobs

import jakarta.annotation.PostConstruct
import nl.eazysoftware.eazyrecyclingservice.domain.service.ExactOAuthService
import nl.eazysoftware.eazyrecyclingservice.repository.exact.ExactTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ScheduledFuture

/**
 * Scheduled job to automatically refresh Exact Online OAuth tokens before they expire.
 *
 * Uses dynamic scheduling to refresh tokens exactly 25 seconds before expiry,
 * avoiding unnecessary database polling.
 */
@Component
@ConditionalOnBooleanProperty(name = ["exact.oauth.refresh-enabled"], havingValue = true, matchIfMissing = true)
class ExactTokenRefreshScheduler(
    private val exactOAuthService: ExactOAuthService,
    private val tokenRepository: ExactTokenRepository,
    private val taskScheduler: TaskScheduler
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    
    @Volatile
    private var scheduledTask: ScheduledFuture<*>? = null
    
    @Volatile
    private var stopped: Boolean = false

    companion object {
        private const val REFRESH_BEFORE_EXPIRY_SECONDS = 25L
        private const val RETRY_DELAY_SECONDS = 30L
    }

    @PostConstruct
    fun init() {
        scheduleNextRefresh()
    }
    
    /**
     * Stop all scheduled token refresh tasks and prevent new ones from being scheduled.
     * Called when the connection is revoked.
     */
    @Synchronized
    fun stop() {
        logger.info("Stopping Exact Online token refresh scheduler")
        stopped = true
        scheduledTask?.cancel(false)
        scheduledTask = null
    }
    
    /**
     * Resume scheduling token refresh tasks.
     * Called when a new connection is established.
     */
    @Synchronized
    fun resume() {
        logger.info("Resuming Exact Online token refresh scheduler")
        stopped = false
        scheduleNextRefresh()
    }

    private fun scheduleNextRefresh() {
        if (stopped) {
            logger.debug("Scheduler is stopped, not scheduling next refresh")
            return
        }
        try {
            val currentToken = tokenRepository.findFirstByOrderByCreatedAtDesc()

            if (currentToken == null) {
                logger.debug("No Exact Online tokens found, waiting for OAuth connection")
                // Don't schedule retry - wait for resume() to be called after OAuth
                return
            }

            val refreshAt = currentToken.expiresAt.minusSeconds(REFRESH_BEFORE_EXPIRY_SECONDS)
            val now = Instant.now()

            if (refreshAt.isBefore(now)) {
                // Token needs immediate refresh
                logger.info("Exact Online token is expiring soon (expires at: {}), refreshing now...", currentToken.expiresAt)
                refreshTokenAndReschedule(currentToken.refreshToken)
            } else {
                // Schedule refresh for later
                logger.info("Scheduled Exact Online token refresh at {} (token expires at: {})", refreshAt, currentToken.expiresAt)
                scheduledTask = taskScheduler.schedule({ refreshTokenAndReschedule(currentToken.refreshToken) }, refreshAt)
            }

        } catch (e: Exception) {
            logger.error("Failed to schedule Exact Online token refresh, will retry in {} seconds", RETRY_DELAY_SECONDS, e)
            scheduledTask = taskScheduler.schedule(::scheduleNextRefresh, Instant.now().plusSeconds(RETRY_DELAY_SECONDS))
        }
    }

    private fun refreshTokenAndReschedule(refreshToken: String) {
        if (stopped) {
            logger.debug("Scheduler is stopped, not refreshing token")
            return
        }
        try {
            exactOAuthService.refreshAccessToken(refreshToken)
            logger.debug("Successfully refreshed Exact Online token")
            // Schedule the next refresh based on the new token
            scheduleNextRefresh()
        } catch (e: Exception) {
            logger.error("Failed to refresh Exact Online token, will retry in {} seconds", RETRY_DELAY_SECONDS, e)
            scheduledTask = taskScheduler.schedule(::scheduleNextRefresh, Instant.now().plusSeconds(RETRY_DELAY_SECONDS))
        }
    }

    /**
     * Run at 00:00to clean up expired tokens and prevent table bloat
     */
    @Scheduled(cron = "0 0 0 * * *") // at 00:00
    fun cleanupExpiredTokens() {
        try {
            val deletedCount = tokenRepository.deleteExpiredTokens(Instant.now())
            if (deletedCount > 0) {
                logger.info("Cleaned up $deletedCount expired Exact Online tokens")
            } else {
                logger.debug("No expired Exact Online tokens to clean up")
            }
        } catch (e: Exception) {
            logger.error("Failed to clean up expired Exact Online tokens", e)
            // Don't throw the exception - we'll try again on the next scheduled run
        }
    }
}
