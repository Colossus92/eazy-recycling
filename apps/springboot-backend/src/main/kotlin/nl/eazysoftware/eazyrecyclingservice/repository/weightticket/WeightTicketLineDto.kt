package nl.eazysoftware.eazyrecyclingservice.repository.weightticket

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.waste.Weight
import nl.eazysoftware.eazyrecyclingservice.domain.weightticket.WeightTicketLine
import java.math.BigDecimal
import java.util.UUID

@Embeddable
data class WeightTicketLineDto(
  @Column(name = "waste_stream_number", nullable = false)
  val wasteStreamNumber: String,

  @Column(name = "weight_value", nullable = false, precision = 10, scale = 2)
  val weightValue: BigDecimal,

  @Enumerated(EnumType.STRING)
  @Column(name = "weight_unit", nullable = false)
  val weightUnit: WeightUnitDto,
) {
  fun toDomain() = WeightTicketLine(
    waste = WasteStreamNumber(this.wasteStreamNumber),
    weight = Weight(this.weightValue, when(this.weightUnit) {
      WeightUnitDto.KILOGRAM -> Weight.WeightUnit.KILOGRAM
    })
  )
}

enum class WeightUnitDto {
  KILOGRAM,
}
