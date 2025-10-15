package nl.eazysoftware.eazyrecyclingservice.domain.weightticket

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.waste.Consignor

class WeightTicket(
  val id: WeightTicketId,
  val consignorParty: Consignor,
  val status: WeightTicketStatus = WeightTicketStatus.DRAFT,
  val carrierParty: CompanyId?,
  val truckLicensePlate: LicensePlate?,
  val reclamation: String?,
  val note: Note?,
  val createdAt: Instant = Clock.System.now(),
  val updatedAt: Instant? = null,
  val weightedAt: Instant? = null,
) {
}

enum class WeightTicketStatus {
  DRAFT,
  COMPLETED,
  INVOICED,
  CANCELLED,
}

data class WeightTicketId(
  val number: Int
)
