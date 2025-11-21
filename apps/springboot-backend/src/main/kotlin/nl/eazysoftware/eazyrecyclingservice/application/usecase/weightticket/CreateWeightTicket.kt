package nl.eazysoftware.eazyrecyclingservice.application.usecase.weightticket

import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream.PickupLocationCommand
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream.toDomain
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Consignor
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Weight
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.*
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

interface CreateWeightTicket {
  fun handle(cmd: WeightTicketCommand): WeightTicketResult
}

/**
 * Command for creating a weight ticket.
 * Contains GoodsCommand with only waste stream reference - full WasteStream will be fetched in the service.
 */
data class WeightTicketCommand(
    val lines: WeightTicketLines,
    val secondWeighing: Weight?,
    val tarraWeight: Weight?,
    val consignorParty: Consignor,
    val carrierParty: CompanyId?,
    val direction: WeightTicketDirection,
    val pickupLocation: PickupLocationCommand?,
    val deliveryLocation: PickupLocationCommand?,
    val truckLicensePlate: LicensePlate?,
    val reclamation: String?,
    val note: Note?,
    val weightedAt: Instant?,
)

data class WeightTicketResult(val id: WeightTicketId)

@Service
class CreateWeightTicketService(
  private val weightTickets: WeightTickets,
  private val projectLocations: ProjectLocations,
  private val companies: Companies
) : CreateWeightTicket {

  @Transactional
  override fun handle(cmd: WeightTicketCommand): WeightTicketResult {
    val id = weightTickets.nextId()

    val ticket = WeightTicket(
      id = id,
      consignorParty = cmd.consignorParty,
      lines = cmd.lines,
      secondWeighing = cmd.secondWeighing,
      tarraWeight = cmd.tarraWeight,
      carrierParty = cmd.carrierParty,
      direction = cmd.direction,
      pickupLocation = cmd.pickupLocation?.toDomain(companies, projectLocations),
      deliveryLocation = cmd.deliveryLocation?.toDomain(companies, projectLocations),
      truckLicensePlate = cmd.truckLicensePlate,
      reclamation = cmd.reclamation,
      note = cmd.note,
      status = WeightTicketStatus.DRAFT,
    )

    weightTickets.save(ticket)
    return WeightTicketResult(id)
  }
}
