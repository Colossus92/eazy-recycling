package nl.eazysoftware.eazyrecyclingservice.repository.material

import java.time.Instant
import java.util.*

/**
 * Projection interface for native query results that include material group details.
 * Used to efficiently fetch material data with group code and name in a single query.
 */
interface MaterialQueryResult {
    fun getId(): UUID
    fun getCode(): String
    fun getName(): String
    fun getMaterialGroupId(): UUID?
    fun getMaterialGroupCode(): String?
    fun getMaterialGroupName(): String?
    fun getUnitOfMeasure(): String
    fun getVatCode(): String
    fun getVatRateId(): UUID
    fun getSalesAccountNumber(): String?
    fun getPurchaseAccountNumber(): String?
    fun getDefaultPrice(): java.math.BigDecimal?
    fun getStatus(): String
    fun getCreatedAt(): Instant?
    fun getCreatedBy(): String?
    fun getUpdatedAt(): Instant?
    fun getUpdatedBy(): String?
}
