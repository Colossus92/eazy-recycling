package nl.eazysoftware.eazyrecyclingservice.repository.weightticket

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Weight
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketLine
import java.math.BigDecimal

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
      WeightUnitDto.kg -> Weight.WeightUnit.KILOGRAM
    })
  )
}

enum class WeightUnitDto {
  kg,
}
