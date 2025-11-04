package nl.eazysoftware.eazyrecyclingservice.application.usecase.weightticket

import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream.PickupLocationCommand
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream.toDomain
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Consignor
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Weight
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.*
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import nl.eazysoftware.eazyrecyclingservice.domain.service.CompanyService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface CreateWeightTicket {
  fun handle(cmd: WeightTicketCommand): WeightTicketResult
}

/**
 * Command for creating a weight ticket.
 * Contains GoodsCommand with only waste stream reference - full WasteStream will be fetched in the service.
 */
data class WeightTicketCommand(
  val lines: WeightTicketLines,
  val tarraWeight: Weight?,
  val consignorParty: Consignor,
  val carrierParty: CompanyId?,
  val direction: WeightTicketDirection,
  val pickupLocation: PickupLocationCommand?,
  val deliveryLocation: PickupLocationCommand?,
  val truckLicensePlate: LicensePlate?,
  val reclamation: String?,
  val note: Note?,
)

data class WeightTicketResult(val id: WeightTicketId)

@Service
class CreateWeightTicketService(
  private val weightTicketRepo: WeightTickets,
  private val projectLocations: ProjectLocations,
  private val companyService: CompanyService
) : CreateWeightTicket {

  @Transactional
  override fun handle(cmd: WeightTicketCommand): WeightTicketResult {
    val id = weightTicketRepo.nextId()

    val ticket = WeightTicket(
      id = id,
      consignorParty = cmd.consignorParty,
      lines = cmd.lines,
      tarraWeight = cmd.tarraWeight,
      carrierParty = cmd.carrierParty,
      direction = cmd.direction,
      pickupLocation = cmd.pickupLocation?.toDomain(companyService, projectLocations),
      deliveryLocation = cmd.deliveryLocation?.toDomain(companyService, projectLocations),
      truckLicensePlate = cmd.truckLicensePlate,
      reclamation = cmd.reclamation,
      note = cmd.note,
      status = WeightTicketStatus.DRAFT,
    )

    weightTicketRepo.save(ticket)
    return WeightTicketResult(id)
  }
}
