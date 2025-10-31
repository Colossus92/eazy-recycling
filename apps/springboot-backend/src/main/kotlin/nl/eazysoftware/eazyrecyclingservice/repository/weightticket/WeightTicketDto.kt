package nl.eazysoftware.eazyrecyclingservice.repository.weightticket

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketDirection
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "weight_tickets")
data class WeightTicketDto(
  @Id
  @Column(name = "id")
  val id: Long,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "consignor_party_id", nullable = false)
  val consignorParty: CompanyDto,

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
    name = "weight_ticket_lines",
    joinColumns = [JoinColumn(name = "weight_ticket_id")]
  )
  val lines: List<WeightTicketLineDto> = emptyList(),

  @Column(name = "tarra_weight_value", nullable = true)
  val tarraWeightValue: BigDecimal?,

  @Column(name = "tarra_weight_unit", nullable = true)
  @Enumerated(EnumType.STRING)
  val tarraWeightUnit: WeightUnitDto?,

  @Enumerated(EnumType.STRING)
  @Column(name = "direction", nullable = false)
  val direction: WeightTicketDirection,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pickup_location_id", referencedColumnName = "id")
  val pickupLocation: PickupLocationDto?,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "delivery_location_id", referencedColumnName = "id")
  val deliveryLocation: PickupLocationDto?,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "carrier_party_id")
  val carrierParty: CompanyDto?,

  @Column(name = "truck_license_plate")
  val truckLicensePlate: String?,

  @Column(name = "reclamation")
  val reclamation: String?,

  @Column(name = "note")
  val note: String?,

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  val status: WeightTicketStatusDto = WeightTicketStatusDto.DRAFT,

  @Column(name = "created_at", nullable = false)
  val createdAt: Instant,

  @Column(name = "updated_at")
  val updatedAt: Instant?,

  @Column(name = "weighted_at")
  val weightedAt: Instant?,

  @Column(name = "cancellation_reason", nullable = true)
  val cancellationReason: String?,
)

enum class WeightTicketStatusDto {
  DRAFT,
  COMPLETED,
  INVOICED,
  CANCELLED,
}
