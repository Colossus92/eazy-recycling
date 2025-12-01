package nl.eazysoftware.eazyrecyclingservice.application.usecase.material

import java.math.BigDecimal

data class MaterialPriceCommand(
    val materialId: Long,
    val price: BigDecimal,
    val currency: String
)
