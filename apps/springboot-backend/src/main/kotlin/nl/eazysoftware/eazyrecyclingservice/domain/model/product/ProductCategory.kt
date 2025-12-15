package nl.eazysoftware.eazyrecyclingservice.domain.model.product

import kotlin.time.Instant

data class ProductCategory(
    val id: Long?,
    val code: String,
    val name: String,
    val description: String?,
    val createdAt: Instant? = null,
    val createdBy: String? = null,
    val updatedAt: Instant? = null,
    val updatedBy: String? = null,
)
