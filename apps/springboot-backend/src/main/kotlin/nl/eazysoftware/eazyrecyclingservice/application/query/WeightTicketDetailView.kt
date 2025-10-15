package nl.eazysoftware.eazyrecyclingservice.application.query

import kotlinx.datetime.LocalDateTime

data class WeightTicketDetailView(
  val id: Long,
  val consignorParty: ConsignorView,
  val status: String,
  val carrierParty: CompanyView?,
  val truckLicensePlate: String?,
  val reclamation: String?,
  val note: String?,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime?,
  val weightedAt: LocalDateTime?,
) {
}
