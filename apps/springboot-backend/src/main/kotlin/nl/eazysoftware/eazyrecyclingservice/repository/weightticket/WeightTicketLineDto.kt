package nl.eazysoftware.eazyrecyclingservice.repository.weightticket

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.declaration.LineDeclarationState
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Weight
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketLine
import java.math.BigDecimal
import java.time.Instant
import java.util.*
import kotlin.time.toKotlinInstant

@Entity
@Table(name = "weight_ticket_lines")
data class WeightTicketLineDto(
  @Id
  @Column(name = "id")
  val id: UUID,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "weight_ticket_id", nullable = false)
  val weightTicket: WeightTicketDto? = null,

  @Column(name = "waste_stream_number")
  val wasteStreamNumber: String?,

  @Column(name = "catalog_item_id", nullable = false)
  val catalogItemId: UUID,

  @Column(name = "weight_value", nullable = false, precision = 10, scale = 2)
  val weightValue: BigDecimal,

  @Enumerated(EnumType.STRING)
  @Column(name = "weight_unit", nullable = false)
  val weightUnit: WeightUnitDto,

  @Column(name = "declared_weight", precision = 10, scale = 2)
  val declaredWeight: BigDecimal? = null,

  @Column(name = "last_declared_at")
  val lastDeclaredAt: Instant? = null,
) {
  fun toDomain(catalogItemId: UUID) = WeightTicketLine(
    waste = this.wasteStreamNumber?.let { WasteStreamNumber(it) },
    catalogItemId = catalogItemId,
    weight = Weight(this.weightValue, when(this.weightUnit) {
      WeightUnitDto.kg -> Weight.WeightUnit.KILOGRAM
    }),
    declarationState = LineDeclarationState(
      declaredWeight = this.declaredWeight,
      lastDeclaredAt = this.lastDeclaredAt?.toKotlinInstant()
    )
  )
}

enum class WeightUnitDto {
  kg,
}
