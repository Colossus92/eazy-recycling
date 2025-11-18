package nl.eazysoftware.eazyrecyclingservice.application.usecase.truck

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate

data class TruckCommand(
  val licensePlate: LicensePlate,
  val brand: String?,
  val description: String?,
  val carrierPartyId: CompanyId?
)

data class TruckResult(
  val licensePlate: String
)
