package nl.eazysoftware.eazyrecyclingservice.repository.product

import java.math.BigDecimal
import java.time.Instant

/**
 * Projection interface for native query results that include product category details.
 * Used to efficiently fetch product data with category code and name in a single query.
 */
interface ProductQueryResult {
  fun getId(): Long
  fun getCode(): String
  fun getName(): String
  fun getCategoryId(): Long?
  fun getCategoryCode(): String?
  fun getCategoryName(): String?
  fun getUnitOfMeasure(): String
  fun getVatCode(): String
  fun getSalesAccountNumber(): String?
  fun getPurchaseAccountNumber(): String?
  fun getDefaultPrice(): BigDecimal?
  fun getStatus(): String
  fun getCreatedAt(): Instant?
  fun getCreatedBy(): String?
  fun getUpdatedAt(): Instant?
  fun getUpdatedBy(): String?
}
