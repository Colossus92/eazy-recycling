package nl.eazysoftware.eazyrecyclingservice.domain.model.product

import java.util.*
import kotlin.time.Instant

data class ProductCategory(
    val id: UUID?,
    val code: String,
    val name: String,
    val description: String?,
    val createdAt: Instant? = null,
    val createdBy: String? = null,
    val updatedAt: Instant? = null,
    val updatedBy: String? = null,
)
