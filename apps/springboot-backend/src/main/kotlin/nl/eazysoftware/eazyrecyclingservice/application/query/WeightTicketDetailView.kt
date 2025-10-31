package nl.eazysoftware.eazyrecyclingservice.application.query

import kotlinx.datetime.LocalDateTime
import java.math.BigDecimal

data class WeightTicketDetailView(
  val id: Long,
  val consignorParty: ConsignorView,
  val status: String,
  val lines: List<WeightTicketLineView>,
  val tarraWeightValue: BigDecimal?,
  val tarraWeightUnit: String?,
  val carrierParty: CompanyView?,
  val direction: String,
  val pickupLocation: PickupLocationView?,
  val deliveryLocation: PickupLocationView?,
  val truckLicensePlate: String?,
  val reclamation: String?,
  val note: String?,
  val cancellationReason: String?,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime?,
  val weightedAt: LocalDateTime?,
)

data class WeightTicketLineView(
  val wasteStreamNumber: String,
  val weightValue: BigDecimal,
  val weightUnit: String,
)
