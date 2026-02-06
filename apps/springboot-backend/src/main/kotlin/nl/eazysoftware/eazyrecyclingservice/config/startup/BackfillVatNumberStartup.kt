package nl.eazysoftware.eazyrecyclingservice.config.startup

import nl.eazysoftware.eazyrecyclingservice.application.jobs.BackfillVatNumberJobService
import nl.eazysoftware.eazyrecyclingservice.config.ExactOnlineProperties
import org.jobrunr.scheduling.BackgroundJob
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Enqueues a one-off VAT number backfill job on application startup.
 * This populates vatNumber on existing companies from Exact Online data.
 * Safe to run multiple times — it only updates companies that don't yet have a vatNumber
 * or whose vatNumber differs from the Exact value.
 *
 * Controlled by exact.oauth.backfill-vat-numbers-enabled (disabled for test and local profiles).
 */
@Component
class BackfillVatNumberStartup(
    private val exactOnlineProperties: ExactOnlineProperties,
) {

    private val logger = LoggerFactory.getLogger(BackfillVatNumberStartup::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun onStartup() {
        if (!exactOnlineProperties.backfillVatNumbersEnabled) {
            logger.info("VAT number backfill is disabled (exact.oauth.backfill-vat-numbers-enabled=false)")
            return
        }
        logger.info("Enqueueing one-off VAT number backfill job")
        BackgroundJob.enqueue<BackfillVatNumberJobService> { it.execute() }
    }
}
