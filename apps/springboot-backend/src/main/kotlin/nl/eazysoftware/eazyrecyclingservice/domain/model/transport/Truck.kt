package nl.eazysoftware.eazyrecyclingservice.domain.model.transport

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import java.time.ZonedDateTime

data class Truck(
  val licensePlate: LicensePlate,
  val brand: String?,
  val description: String?,
  val carrierPartyId: CompanyId?,
  val updatedAt: ZonedDateTime?,
  val displayName: String? = null,
)

data class LicensePlate(val value: String) {
  init {
    require(value.isNotBlank()) { "Het kenteken is verplicht" }
  }
}
