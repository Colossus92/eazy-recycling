package nl.eazysoftware.eazyrecyclingservice.domain.model.waste

import java.math.BigDecimal
import java.math.RoundingMode

data class Weight(
  val value: BigDecimal,
  val unit: WeightUnit
) {
  fun multiplyByPercentage(percentage: Int): Weight {
    require(percentage in 1..100) { "Percentage moet tussen 1 en 100 zijn" }
    val newValue = (value * BigDecimal(percentage)) / BigDecimal(100)
    return Weight(newValue.setScale(2, RoundingMode.HALF_UP), unit)
  }

  init {
    require(value >= BigDecimal.ZERO) { "Gewicht dient een positief getal te zijn" }
  }

  enum class WeightUnit(val code: String) {
    KILOGRAM("Kg"),
  }
}
