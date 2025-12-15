package nl.eazysoftware.eazyrecyclingservice.repository.catalogitem

import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * Projection interface for native query results that include catalog item category details.
 * Used to efficiently fetch catalog item data with category code and name in a single query.
 */
interface CatalogItemQueryResult {
    fun getId(): Long
    fun getType(): String
    fun getCode(): String
    fun getName(): String
    fun getCategoryId(): Long?
    fun getCategoryCode(): String?
    fun getCategoryName(): String?
    fun getUnitOfMeasure(): String
    fun getVatCode(): String
    fun getGlAccountCode(): String?
    fun getConsignorPartyId(): UUID?
    fun getDefaultPrice(): BigDecimal?
    fun getStatus(): String
    fun getCreatedAt(): Instant?
    fun getCreatedBy(): String?
    fun getUpdatedAt(): Instant?
    fun getUpdatedBy(): String?
}
