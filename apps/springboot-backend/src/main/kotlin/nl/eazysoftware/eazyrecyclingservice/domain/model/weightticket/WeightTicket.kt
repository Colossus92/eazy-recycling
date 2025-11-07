package nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Consignor
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Weight
import kotlin.time.Clock
import kotlin.time.Instant

class WeightTicket(
  val id: WeightTicketId,
  var consignorParty: Consignor,
  var status: WeightTicketStatus = WeightTicketStatus.DRAFT,
  /**
   * The weight of any kind of container (waste container, truck) without waste
   */
  var tarraWeight: Weight?,
  var lines: WeightTicketLines,
  var carrierParty: CompanyId?,
  var direction: WeightTicketDirection,
  var pickupLocation: Location?,
  var deliveryLocation: Location?,
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
    direction: WeightTicketDirection = this.direction,
    pickupLocation: Location? = this.pickupLocation,
    deliveryLocation: Location? = this.deliveryLocation,
    tarraWeight: Weight? = this.tarraWeight,
    truckLicensePlate: LicensePlate? = this.truckLicensePlate,
    reclamation: String? = this.reclamation,
    note: Note? = this.note,
  ) {
    check(status == WeightTicketStatus.DRAFT) {
      "Weegbon kan alleen worden gewijzigd als de status concept is."
    }
    this.lines = lines
    this.tarraWeight = tarraWeight
    this.direction = direction
    this.pickupLocation = pickupLocation
    this.deliveryLocation = deliveryLocation
    this.carrierParty = carrierParty
    this.consignorParty = consignorParty
    this.truckLicensePlate = truckLicensePlate
    this.reclamation = reclamation
    this.note = note
    this.updatedAt = Clock.System.now()
  }

  fun split(newId: WeightTicketId, originalPercentage: Int, newPercentage: Int) : WeightTicket {
    // 1. Validate business rules
    require(originalPercentage + newPercentage == 100) {
      "Percentages moeten optellen tot 100"
    }
    require(originalPercentage > 0 && newPercentage > 0) {
      "Percentages moeten positief zijn"
    }
    check(status == WeightTicketStatus.DRAFT) {
      "Weegbon kan alleen worden gesplitst als de status openstaand is"
    }
    check(!lines.isEmpty()) {
      "Kan geen lege weegbon splitsen"
    }

    // 2. Split lines by percentage
    val originalLines = lines.getLines().map { line ->
      WeightTicketLine(
        waste = line.waste,
        weight = line.weight.multiplyByPercentage(originalPercentage)
      )
    }

    val newLines = lines.getLines().map { line ->
      WeightTicketLine(
        waste = line.waste,
        weight = line.weight.multiplyByPercentage(newPercentage)
      )
    }

    // 3. Update this aggregate's lines
    this.lines = WeightTicketLines(originalLines)
    this.updatedAt = Clock.System.now()

    // 4. Return new aggregate
    return WeightTicket(
      id = newId,
      consignorParty = this.consignorParty,
      status = WeightTicketStatus.DRAFT,
      lines = WeightTicketLines(newLines),
      tarraWeight = null,
      carrierParty = this.carrierParty,
      direction = this.direction,
      pickupLocation = this.pickupLocation,
      deliveryLocation = this.deliveryLocation,
      truckLicensePlate = this.truckLicensePlate,
      reclamation = this.reclamation,
      note = this.note?.copy(),
      createdAt = Clock.System.now()
    )
  }

  fun complete() {
    check(status == WeightTicketStatus.DRAFT) {
      "Weegbon kan alleen worden voltooid als de status openstaand is"
    }
    check (!lines.isEmpty()) {
      "Weegbon kan alleen worden voltooid als er minimaal één sorteerweging is"
    }
    this.status = WeightTicketStatus.COMPLETED
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

enum class WeightTicketDirection {
  INBOUND,   // Material coming in (e.g. waste delivered)
  OUTBOUND,  // Material going out (e.g. processed metal leaving site)
}
