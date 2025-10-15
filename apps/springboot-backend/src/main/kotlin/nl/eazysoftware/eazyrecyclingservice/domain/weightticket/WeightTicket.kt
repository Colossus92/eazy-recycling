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
  var status: WeightTicketStatus = WeightTicketStatus.DRAFT,
  val carrierParty: CompanyId?,
  val truckLicensePlate: LicensePlate?,
  val reclamation: String?,
  val note: Note?,
  val createdAt: Instant = Clock.System.now(),
  val updatedAt: Instant? = null,
  val weightedAt: Instant? = null,
) {
  fun delete() {
    check(status != WeightTicketStatus.CANCELLED) {
      "Weegbon is al geannuleerd en kan niet opnieuw worden geannuleerd"
    }
    check(status != WeightTicketStatus.INVOICED) {
      "Weegbon is al gefactureerd en kan niet worden geannuleerd"
    }
    check(status != WeightTicketStatus.COMPLETED) {
      "Weegbon is al verwerkt en kan niet worden geannuleerd"
    }
    this.status = WeightTicketStatus.CANCELLED
  }
}

enum class WeightTicketStatus {
  DRAFT,
  COMPLETED,
  INVOICED,
  CANCELLED,
}

data class WeightTicketId(
  val number: Long
)
