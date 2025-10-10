package nl.eazysoftware.eazyrecyclingservice.repository.weightticket

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import java.time.ZonedDateTime

@Entity
@Table(name = "weight_ticket")
data class WeightTicketDto(
  @Id
  @Column(name = "id")
  val id: Int,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "consignor_party_id", nullable = false)
  val consignorParty: CompanyDto,

  @OneToMany(mappedBy = "weightTicket", cascade = [CascadeType.ALL], orphanRemoval = true)
  val goods: MutableList<WeightTicketGoodsDto> = mutableListOf(),

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "carrier_party_id", nullable = false)
  val carrierParty: CompanyDto,

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
  val createdAt: ZonedDateTime,

  @Column(name = "updated_at")
  val updatedAt: ZonedDateTime?,

  @Column(name = "weighted_at", nullable = false)
  val weightedAt: ZonedDateTime
)

enum class WeightTicketStatusDto {
  DRAFT,
  PROCESSED,
  COMPLETED,
  CANCELLED
}
