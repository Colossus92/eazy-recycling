package nl.eazysoftware.eazyrecyclingservice.application.jobs

import nl.eazysoftware.eazyrecyclingservice.adapters.out.exact.ExactSyncException
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ExactOnlineSync
import nl.eazysoftware.eazyrecyclingservice.repository.exact.SyncCursorDto
import nl.eazysoftware.eazyrecyclingservice.repository.exact.SyncCursorRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

/**
 * Scheduled job that syncs accounts from Exact Online daily.
 * 
 * This job runs daily at 3:00 AM to:
 * 1. Fetch new and updated accounts from Exact Online's Sync API
 * 2. Create or update corresponding companies in Eazy Recycling
 * 3. Fetch deleted accounts from Exact Online's Deleted API
 * 4. Soft-delete corresponding companies in Eazy Recycling
 * 
 * Important: Exact Online only retains deletion records for 2 months.
 * Running this job daily ensures we don't miss any deletions.
 */
@Component
class ExactSyncScheduler(
    private val exactOnlineSync: ExactOnlineSync,
    private val syncCursorRepository: SyncCursorRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val SYNC_ENTITY_ACCOUNTS = "accounts"
        private val TWO_WEEKS = Duration.ofDays(14)
    }

    /**
     * Run daily at 3:00 AM to sync accounts from Exact Online.
     * 
     * Cron expression: "0 0 3 * * *" = At 03:00:00 every day
     */
    @Scheduled(cron = "0 0 3 * * *")
    fun syncFromExact() {
        logger.info("Starting scheduled sync from Exact Online")

        try {
            // Check if last sync was more than 2 weeks ago (warning threshold)
            checkSyncGap()

            val result = exactOnlineSync.syncFromExact()
            
            logger.info(
                "Scheduled sync completed: {} records synced ({} created, {} updated, {} conflicts), {} deleted records processed ({} deleted, {} not found)",
                result.recordsSynced,
                result.recordsCreated,
                result.recordsUpdated,
                result.recordsConflicted,
                result.deletedRecordsProcessed,
                result.deletedRecordsDeleted,
                result.deletedRecordsNotFound
            )
        } catch (e: ExactSyncException) {
            logger.warn("Scheduled sync skipped: ${e.message}")
        } catch (e: Exception) {
            logger.error("Scheduled sync failed: ${e.message}", e)
        }
    }

    /**
     * Check if the last deletion sync was more than 2 weeks ago and log a warning.
     */
    private fun checkSyncGap() {
        val cursor = syncCursorRepository.findByEntityAndCursorType(
            SYNC_ENTITY_ACCOUNTS, 
            SyncCursorDto.CURSOR_TYPE_DELETED
        )
        
        if (cursor != null) {
            val timeSinceLastSync = Duration.between(cursor.updatedAt, Instant.now())
            if (timeSinceLastSync > TWO_WEEKS) {
                logger.warn(
                    "WARNING: Last deletion sync was {} days ago. " +
                    "Exact Online only retains deletion records for 2 months. " +
                    "Some deletions may have been missed.",
                    timeSinceLastSync.toDays()
                )
            }
        } else {
            logger.info("First deletion sync - no previous cursor found")
        }
    }
}
