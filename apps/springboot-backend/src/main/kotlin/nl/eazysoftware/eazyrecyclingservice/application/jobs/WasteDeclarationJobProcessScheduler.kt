package nl.eazysoftware.eazyrecyclingservice.application.jobs

import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastedeclaration.DeclareFirstReceivals
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastedeclaration.DeclareMonthlyReceivals
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastedeclaration.DetectLateDeclarations
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.FirstReceivalWasteStreamQuery
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MonthlyReceivalWasteStreamQuery
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MonthlyWasteDeclarationJob
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MonthlyWasteDeclarationJobs
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Scheduled job that processes pending monthly waste declaration jobs.
 *
 * Runs every 10 minutes to check for pending jobs and processes them according to their type:
 * - FIRST_RECEIVALS: Triggers the FirstReceivalDeclarator
 * - MONTHLY_RECEIVALS: Triggers the MonthlyReceivalDeclarator
 * - LATE_WEIGHT_TICKETS: Processes late weight tickets for declarations
 */
@Component
class WasteDeclarationJobProcessScheduler(
  private val monthlyWasteDeclarationJobs: MonthlyWasteDeclarationJobs,
  private val firstReceivalWasteStreamQuery: FirstReceivalWasteStreamQuery,
  private val declareFirstReceivals: DeclareFirstReceivals,
  private val monthlyReceivalWasteStreamQuery: MonthlyReceivalWasteStreamQuery,
  private val declareMonthlyReceivals: DeclareMonthlyReceivals,
  private val detectLateDeclarations: DetectLateDeclarations,
) {

  private val logger = LoggerFactory.getLogger(WasteDeclarationJobProcessScheduler::class.java)

  /**
   * Processes pending monthly waste declaration jobs every 10 minutes.
   */
  @Scheduled(cron = "0 */10 * * * *")
  fun processPendingJobs() {
    logger.info("Starting monthly waste declaration job processing")

    val pendingJobs = monthlyWasteDeclarationJobs.findPending()

    if (pendingJobs.isEmpty()) {
      logger.debug("No pending monthly waste declaration jobs found")
      return
    }

    logger.info("Found {} pending monthly waste declaration job(s)", pendingJobs.size)

    pendingJobs.forEach { job ->
      processJob(job)
    }

    logger.info("Completed monthly waste declaration job processing")
  }

  private fun processJob(job: MonthlyWasteDeclarationJob) {
    logger.info("Processing job: id={}, type={}, yearMonth={}", job.id, job.jobType, job.yearMonth)

    try {
      when (job.jobType) {
        MonthlyWasteDeclarationJob.JobType.FIRST_RECEIVALS -> processFirstReceivalsJob(job)
        MonthlyWasteDeclarationJob.JobType.MONTHLY_RECEIVALS -> processMonthlyReceivalsJob(job)
        MonthlyWasteDeclarationJob.JobType.LATE_WEIGHT_TICKETS -> processLateWeightTicketsJob(job)
      }
    } catch (e: Exception) {
      logger.error("Failed to process job: id={}, type={}", job.id, job.jobType, e)
      monthlyWasteDeclarationJobs.save(job.markFailed())
    }
  }

  private fun processFirstReceivalsJob(job: MonthlyWasteDeclarationJob) {
    logger.info("Processing FIRST_RECEIVALS job for yearMonth={}", job.yearMonth)

    // Query for all waste streams that need to be declared for the first time
    val receivalDeclarations = firstReceivalWasteStreamQuery.findFirstReceivalDeclarations(job.yearMonth)

    logger.info("Found {} first receival declaration(s) for yearMonth={}", receivalDeclarations.size, job.yearMonth)

    if (receivalDeclarations.isEmpty()) {
      logger.info("No first receivals to declare for yearMonth={}", job.yearMonth)
      monthlyWasteDeclarationJobs.save(job.markCompleted())
      return
    }

    // Trigger the declarator to process the declarations
    declareFirstReceivals.declareFirstReceivals(receivalDeclarations)

    // Mark job as completed
    monthlyWasteDeclarationJobs.save(job.markCompleted())

    logger.info("Successfully completed FIRST_RECEIVALS job for yearMonth={}", job.yearMonth)
  }

  private fun processMonthlyReceivalsJob(job: MonthlyWasteDeclarationJob) {
    logger.info("Processing MONTHLY_RECEIVALS job for yearMonth={}", job.yearMonth)

    // Query for all waste streams that need monthly receival declarations
    val receivalDeclarations = monthlyReceivalWasteStreamQuery.findMonthlyReceivalDeclarations(job.yearMonth)

    logger.info("Found {} monthly receival declaration(s) for yearMonth={}", receivalDeclarations.size, job.yearMonth)

    if (receivalDeclarations.isEmpty()) {
      logger.info("No monthly receivals to declare for yearMonth={}", job.yearMonth)
      monthlyWasteDeclarationJobs.save(job.markCompleted())
      return
    }

    // Trigger the declarator to process the declarations
    declareMonthlyReceivals.declare(receivalDeclarations)

    // Mark job as completed
    monthlyWasteDeclarationJobs.save(job.markCompleted())

    logger.info("Successfully completed MONTHLY_RECEIVALS job for yearMonth={}", job.yearMonth)
  }

  private fun processLateWeightTicketsJob(job: MonthlyWasteDeclarationJob) {
    logger.info("Processing LATE_WEIGHT_TICKETS job")

    detectLateDeclarations.detectAndCreateForLateWeightTickets()

    // Mark job as completed
    monthlyWasteDeclarationJobs.save(job.markCompleted())

    logger.info("Successfully completed LATE_WEIGHT_TICKETS job")
  }

}
