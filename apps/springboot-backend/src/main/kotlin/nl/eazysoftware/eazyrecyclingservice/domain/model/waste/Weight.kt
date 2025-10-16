package nl.eazysoftware.eazyrecyclingservice.domain.model.waste

import java.math.BigDecimal

data class Weight(
    val value: BigDecimal,
    val unit: WeightUnit
) {
    init {
        require(value >= BigDecimal.ZERO) { "Gewicht dient een positief getal te zijn" }
    }

    enum class WeightUnit(val code: String) {
        KILOGRAM("Kg"),
    }
}
