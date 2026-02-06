package nl.eazysoftware.eazyrecyclingservice.application.jobs

import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ExactOnlineSync
import org.jobrunr.jobs.annotations.Job
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * JobRunr-based service for the one-off VAT number backfill.
 * Fetches all Exact Online accounts and updates vatNumber on matched local companies.
 */
@Service
class BackfillVatNumberJobService(
    private val exactOnlineSync: ExactOnlineSync,
) {
    private val logger = LoggerFactory.getLogger(BackfillVatNumberJobService::class.java)

    @Job(name = "Backfill VAT numbers from Exact Online", retries = 2)
    fun execute() {
        logger.info("Starting one-off VAT number backfill job")
        try {
            val updatedCount = exactOnlineSync.backfillVatNumbers()
            logger.info("VAT number backfill job completed: $updatedCount companies updated")
        } catch (e: Exception) {
            logger.error("VAT number backfill job failed: ${e.message}", e)
            throw e
        }
    }
}
