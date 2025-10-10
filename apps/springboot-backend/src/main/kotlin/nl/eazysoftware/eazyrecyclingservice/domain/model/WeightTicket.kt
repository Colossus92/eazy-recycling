package nl.eazysoftware.eazyrecyclingservice.domain.model

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.waste.Goods
import java.time.ZonedDateTime

data class WeightTicket(
  val id: WeightTicketId,
  // TODO: add val id: OrderId
  val carrierParty: CompanyId,
  val consignorParty: CompanyId,
  val truck: LicensePlate?,
  val goods: List<Goods>,

  /**
   * Small not for display on a printed weight ticket
   */
  val reclamation: String?,

  /**
   * Larger note, not displayed on a printed weight ticket
   */
  val note: Note?,
  val status: WeightTicketStatus = WeightTicketStatus.DRAFT,
  val createdAt: ZonedDateTime,
  val updatedAt: ZonedDateTime?,
  val weightedAt: ZonedDateTime
)

enum class WeightTicketStatus {
  DRAFT,
  PROCESSED,
  COMPLETED,
  CANCELLED,
}

data class WeightTicketId(
  val number: Int
)
