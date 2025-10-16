package nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Consignor

class WeightTicket(
  val id: WeightTicketId,
  var consignorParty: Consignor,
  var status: WeightTicketStatus = WeightTicketStatus.DRAFT,
  var lines: WeightTicketLines,
  var carrierParty: CompanyId?,
  var truckLicensePlate: LicensePlate?,
  var reclamation: String?,
  var note: Note?,
  var cancellationReason: CancellationReason? = null,
  val createdAt: Instant = Clock.System.now(),
  var updatedAt: Instant? = null,
  var weightedAt: Instant? = null,
) {
  fun cancel(cancellationReason: CancellationReason) {
    check(status != WeightTicketStatus.CANCELLED) {
      "Weegbon is al geannuleerd en kan niet opnieuw worden geannuleerd."
    }
    check(status != WeightTicketStatus.INVOICED) {
      "Weegbon is al gefactureerd en kan niet worden geannuleerd."
    }
    check(status != WeightTicketStatus.COMPLETED) {
      "Weegbon is al verwerkt en kan niet worden geannuleerd."
    }
    this.status = WeightTicketStatus.CANCELLED
    this.cancellationReason = cancellationReason
    this.updatedAt = Clock.System.now()
  }

  fun update(
    lines: WeightTicketLines = this.lines,
    carrierParty: CompanyId? = this.carrierParty,
    consignorParty: Consignor = this.consignorParty,
    truckLicensePlate: LicensePlate? = this.truckLicensePlate,
    reclamation: String? = this.reclamation,
    note: Note? = this.note,
  ) {
    check(status == WeightTicketStatus.DRAFT) {
      "Weegbon kan alleen worden gewijzigd als de status concept is."
    }
    this.lines = lines
    this.carrierParty = carrierParty
    this.consignorParty = consignorParty
    this.truckLicensePlate = truckLicensePlate
    this.reclamation = reclamation
    this.note = note
    this.updatedAt = Clock.System.now()
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

data class CancellationReason(
  val value: String
) {
  init {
      require(value.isNotBlank()) { "Een reden van annulering is verplicht." }
  }
}
