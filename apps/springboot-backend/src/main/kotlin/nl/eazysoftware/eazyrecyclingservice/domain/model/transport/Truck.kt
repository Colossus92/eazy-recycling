package nl.eazysoftware.eazyrecyclingservice.domain.model.transport

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import java.time.Instant

data class Truck(
  val licensePlate: LicensePlate,
  val brand: String?,
  val description: String?,
  val carrierPartyId: CompanyId?,
  val createdAt: Instant? = null,
  val createdBy: String? = null,
  val updatedAt: Instant? = null,
  val updatedBy: String? = null,
  val displayName: String? = null,
)

data class LicensePlate(val value: String) {
  init {
    require(value.isNotBlank()) { "Het kenteken is verplicht" }
  }
}
