package nl.eazysoftware.eazyrecyclingservice.domain.model.material

import java.math.BigDecimal
import kotlin.time.Instant

data class MaterialPrice(
    val id: Long?,
    val materialId: Long,
    val price: BigDecimal,
    val currency: String,
    val validFrom: Instant,
    val validTo: Instant?,
    val createdAt: Instant? = null,
    val createdBy: String? = null,
    val updatedAt: Instant? = null,
    val updatedBy: String? = null,
)
