package nl.eazysoftware.eazyrecyclingservice.application.usecase.weightticket

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Consignor
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Weight
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicket
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketLines
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketStatus
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
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
  val truckLicensePlate: LicensePlate?,
  val reclamation: String?,
  val note: Note?,
)

data class WeightTicketResult(val id: WeightTicketId)

@Service
class CreateWeightTicketService(
  private val weightTicketRepo: WeightTickets,
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
      truckLicensePlate = cmd.truckLicensePlate,
      reclamation = cmd.reclamation,
      note = cmd.note,
      status = WeightTicketStatus.DRAFT,
    )

    weightTicketRepo.save(ticket)
    return WeightTicketResult(id)
  }
}
