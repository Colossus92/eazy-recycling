package nl.eazysoftware.eazyrecyclingservice.repository.material

import java.time.Instant

/**
 * Projection interface for native query results that include material group details.
 * Used to efficiently fetch material data with group code and name in a single query.
 */
interface MaterialQueryResult {
    fun getId(): Long
    fun getCode(): String
    fun getName(): String
    fun getMaterialGroupId(): Long?
    fun getMaterialGroupCode(): String?
    fun getMaterialGroupName(): String?
    fun getUnitOfMeasure(): String
    fun getVatCode(): String
    fun getSalesAccountNumber(): String?
    fun getPurchaseAccountNumber(): String?
    fun getStatus(): String
    fun getCreatedAt(): Instant?
    fun getCreatedBy(): String?
    fun getUpdatedAt(): Instant?
    fun getUpdatedBy(): String?
}
