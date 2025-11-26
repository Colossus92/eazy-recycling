package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import nl.eazysoftware.eazyrecyclingservice.repository.exact.CompanySyncDto

/**
 * Outgoing port for synchronizing companies with Exact Online
 */
interface ExactOnlineSync {
    /**
     * Sync a newly created company to Exact Online.
     * This should be a fire-and-forget operation that handles failures gracefully
     * without affecting the main company creation flow.
     */
    fun syncCompany(company: Company)

    /**
     * Update an existing company in Exact Online.
     * This should be a fire-and-forget operation that handles failures gracefully
     * without affecting the main company update flow.
     */
    fun updateCompany(company: Company)

    /**
     * Sync companies from Exact Online to our database.
     * Uses the Exact Online Sync API with timestamp-based pagination.
     * Returns the number of records synced.
     */
    fun syncFromExact(): SyncFromExactResult
    
    /**
     * Get all sync records that have conflicts requiring manual resolution.
     */
    fun getConflicts(): List<CompanySyncDto>
    
    /**
     * Get all sync records that are pending manual review.
     */
    fun getPendingReviews(): List<CompanySyncDto>
    
    /**
     * Sync deleted records from Exact Online.
     * Uses the Exact Online Deleted API with timestamp-based pagination.
     * Soft-deletes companies locally that were deleted in Exact.
     */
    fun syncDeletedFromExact(): SyncDeletedResult
}

/**
 * Result of syncing from Exact Online
 */
data class SyncFromExactResult(
    val recordsSynced: Int,
    val recordsCreated: Int,
    val recordsUpdated: Int,
    val recordsConflicted: Int,
    val recordsPendingReview: Int,
    val newTimestamp: Long
)

/**
 * Result of syncing deleted records from Exact Online
 */
data class SyncDeletedResult(
    val recordsProcessed: Int,
    val recordsDeleted: Int,
    val recordsNotFound: Int,
    val newTimestamp: Long
)
