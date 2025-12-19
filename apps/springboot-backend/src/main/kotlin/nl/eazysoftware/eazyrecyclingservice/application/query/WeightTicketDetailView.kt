package nl.eazysoftware.eazyrecyclingservice.application.query

import kotlinx.datetime.LocalDateTime
import java.math.BigDecimal

data class WeightTicketDetailView(
  val id: Long,
  val consignorParty: ConsignorView,
  val status: String,
  val lines: List<WeightTicketLineView>,
  val secondWeighingValue: BigDecimal?,
  val secondWeighingUnit: String?,
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
  val linkedInvoiceId: Long?,
  val createdAt: LocalDateTime?,
  val createdByName: String?,
  val updatedAt: LocalDateTime?,
  val updatedByName: String?,
  val weightedAt: LocalDateTime?,
  val pdfUrl: String?,
)

data class WeightTicketLineView(
  val wasteStreamNumber: String?,
  val catalogItemId: Long,
  val weightValue: BigDecimal,
  val weightUnit: String,
)
