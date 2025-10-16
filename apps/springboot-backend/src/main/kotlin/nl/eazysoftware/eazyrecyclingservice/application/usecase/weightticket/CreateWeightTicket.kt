package nl.eazysoftware.eazyrecyclingservice.application.usecase.weightticket

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import nl.eazysoftware.eazyrecyclingservice.domain.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.waste.Consignor
import nl.eazysoftware.eazyrecyclingservice.domain.weightticket.WeightTicket
import nl.eazysoftware.eazyrecyclingservice.domain.weightticket.WeightTicketId
import nl.eazysoftware.eazyrecyclingservice.domain.weightticket.WeightTicketLines
import nl.eazysoftware.eazyrecyclingservice.domain.weightticket.WeightTicketStatus
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
