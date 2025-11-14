package nl.eazysoftware.eazyrecyclingservice.application.jobs

import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MonthlyWasteDeclarator
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Scheduler that triggers monthly waste declaration on the 20th day of each month at 2:00 AM.
 * Uses cron expression: "0 0 2 20 * *" (minute hour day month day-of-week year)
 */
@Component
class MonthlyWasteDeclarationScheduler(
  private val monthlyWasteDeclarator: MonthlyWasteDeclarator
) {

  private val logger = LoggerFactory.getLogger(javaClass)

  /**
   * Triggers the monthly waste declaration.
   * Runs on the 20th day of every month at 2:00 AM.
   */
  @Scheduled(cron = "0 0 2 20 * *")
  fun triggerMonthlyWasteDeclaration() {
    try {
      logger.info("Starting monthly waste declaration job")
      monthlyWasteDeclarator.declare()
      logger.info("Monthly waste declaration job completed successfully")
    } catch (e: Exception) {
      logger.error("Monthly waste declaration job failed", e)
      throw e
    }
  }
}
