package nl.eazysoftware.eazyrecyclingservice.domain.model.product

import java.math.BigDecimal
import kotlin.time.Instant

data class Product(
    val id: Long?,
    val code: String,
    val name: String,
    val categoryId: Long?,
    val categoryName: String?,
    val unitOfMeasure: String,
    val vatCode: String,
    val glAccountCode: String?,
    val status: String,
    val defaultPrice: BigDecimal?,
    val description: String?,
    val createdAt: Instant? = null,
    val createdBy: String? = null,
    val updatedAt: Instant? = null,
    val updatedBy: String? = null,
)
