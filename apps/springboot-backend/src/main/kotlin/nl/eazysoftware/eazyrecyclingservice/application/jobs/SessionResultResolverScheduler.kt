package nl.eazysoftware.eazyrecyclingservice.application.jobs

import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastedeclaration.SessionResultResolver
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaDeclarationSessions
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Scheduled job that resolves pending LMA declaration session results.
 *
 * Runs every 10 minutes to check for pending sessions and processes their results from AMICE.
 * Each session is processed in its own transaction to ensure atomicity and isolation.
 */
@Component
class SessionResultResolverScheduler(
  private val lmaDeclarationSessions: LmaDeclarationSessions,
  private val sessionResultResolver: SessionResultResolver
) {

  private val logger = LoggerFactory.getLogger(SessionResultResolverScheduler::class.java)

  /**
   * Processes pending session results every 10 minutes.
   */
  @Scheduled(cron = "0 */1 * * * *")
  fun resolvePendingSessions() {
    logger.info("Starting session result resolution process")

    val pendingSessions = lmaDeclarationSessions.findPending()

    if (pendingSessions.isEmpty()) {
      logger.debug("No pending sessions found")
      return
    }

    logger.info("Found {} pending session(s)", pendingSessions.size)

    pendingSessions.forEach { session ->
      sessionResultResolver.processSession(session)
    }

    logger.info("Completed session result resolution process")
  }
}
