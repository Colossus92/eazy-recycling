package nl.eazysoftware.eazyrecyclingservice.repository.weightticket

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketDirection
import nl.eazysoftware.eazyrecyclingservice.repository.AuditableEntity
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

  @Column(name = "second_weighing_value", nullable = true)
  val secondWeighingValue: BigDecimal?,

  @Column(name = "second_weighing_unit", nullable = true)
  @Enumerated(EnumType.STRING)
  val secondWeighingUnit: WeightUnitDto?,

  @Column(name = "tarra_weight_value", nullable = true)
  val tarraWeightValue: BigDecimal?,

  @Column(name = "tarra_weight_unit", nullable = true)
  @Enumerated(EnumType.STRING)
  val tarraWeightUnit: WeightUnitDto?,

  @Enumerated(EnumType.STRING)
  @Column(name = "direction", nullable = false)
  val direction: WeightTicketDirection,

  @OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  @JoinColumn(name = "pickup_location_id", referencedColumnName = "id")
  val pickupLocation: PickupLocationDto?,

  @OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  @JoinColumn(name = "delivery_location_id", referencedColumnName = "id")
  val deliveryLocation: PickupLocationDto?,

  @OneToOne(fetch = FetchType.LAZY)
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

  @Column(name = "weighted_at")
  val weightedAt: Instant?,

  @Column(name = "cancellation_reason", nullable = true)
  val cancellationReason: String?,

  @Column(name = "linked_invoice_id")
  val linkedInvoiceId: Long? = null,

  @Column(name = "pdf_url")
  val pdfUrl: String? = null,
) : AuditableEntity()

enum class WeightTicketStatusDto {
  DRAFT,
  COMPLETED,
  INVOICED,
  CANCELLED,
}
