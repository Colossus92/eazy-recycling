package nl.eazysoftware.eazyrecyclingservice.application.jobs

import nl.eazysoftware.eazyrecyclingservice.application.util.DeclarationCutoffDateCalculator
import nl.eazysoftware.eazyrecyclingservice.config.clock.toYearMonth
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MonthlyWasteDeclarationJob
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MonthlyWasteDeclarationJob.JobType
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MonthlyWasteDeclarationJobs
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTicketDeclarationSnapshots
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import kotlin.time.Clock

/**
 * Scheduler that triggers late weight tickets jobs daily at 23:00 PM.
 *
 * This scheduler creates jobs for weight tickets that are finalized after the declaration deadline
 *
 * The declaration deadline is the 20th of the following month. For example:
 * - October weight tickets have a deadline of November 20th
 * - On December 4th: October and earlier are processed (November's deadline hasn't passed)
 * - On December 21st: November and earlier are processed (November's deadline passed on December 20th)
 */
@Component
class LateAndCorrectiveDeclarationScheduler(
  private val monthlyWasteDeclarationJobs: MonthlyWasteDeclarationJobs,
  private val weightTicketDeclarationSnapshots: WeightTicketDeclarationSnapshots,
) {

  private val logger = LoggerFactory.getLogger(javaClass)

  /**
   * Triggers late and corrective declaration jobs daily at 23:00 PM.
   * Cron expression: "0 0 23 * * *" (second minute hour day month day-of-week)
   */
  @Scheduled(cron = "0 0 23 * * *")
  fun triggerLateDeclarations() {
    try {
      logger.info("Starting late and corrective declaration job scheduling")

      val now = Clock.System.now()
      val cutoffDate = DeclarationCutoffDateCalculator.calculateDeclarationCutoffDate(now)

      logger.info("Declaration cutoff date: {} (only processing weight tickets from before this date)", cutoffDate)

      // Check if there are undeclared lines to process
      val undeclaredLines = weightTicketDeclarationSnapshots.findUndeclaredLines(cutoffDate)
      if (undeclaredLines.isNotEmpty()) {
        logger.info("Found {} undeclared weight ticket line(s) - creating late declaration jobs", undeclaredLines.size)
        createLateDeclarationJobs(now)
      } else {
        logger.debug("No undeclared weight ticket lines found")
      }

      logger.info("Late declaration job scheduling completed")
    } catch (e: Exception) {
      logger.error("Late declaration job scheduling failed", e)
      throw e
    }
  }

  private fun createLateDeclarationJobs(now: kotlin.time.Instant) {
    val lateFirstReceivalJob = MonthlyWasteDeclarationJob(
      jobType = JobType.LATE_WEIGHT_TICKETS,
      yearMonth = now.toYearMonth(), // The job will determine which months to process
      status = MonthlyWasteDeclarationJob.Status.PENDING,
      created = now,
      fulfilled = null
    )

    monthlyWasteDeclarationJobs.save(lateFirstReceivalJob,)
    logger.info("Created LATE_WEIGHT_TICKETS jobs")
  }
}
