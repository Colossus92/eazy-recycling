package nl.eazysoftware.eazyrecyclingservice.domain.model.product

import java.math.BigDecimal
import java.util.*
import kotlin.time.Instant

data class Product(
    val id: UUID?,
    val code: String,
    val name: String,
    val categoryId: UUID?,
    val categoryName: String?,
    val unitOfMeasure: String,
    val vatRateId: UUID,
    val salesAccountNumber: String?,
    val purchaseAccountNumber: String?,
    val status: String,
    val defaultPrice: BigDecimal?,
    val createdAt: Instant? = null,
    val createdBy: String? = null,
    val updatedAt: Instant? = null,
    val updatedBy: String? = null,
)
