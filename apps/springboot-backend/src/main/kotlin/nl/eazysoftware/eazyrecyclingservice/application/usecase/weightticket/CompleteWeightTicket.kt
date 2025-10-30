package nl.eazysoftware.eazyrecyclingservice.application.usecase.weightticket

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import org.springframework.stereotype.Service

interface CompleteWeightTicket {
  fun handle(cmd: CompleteWeightTicketCommand)
}

data class CompleteWeightTicketCommand(
  val weightTicketId: WeightTicketId,
)

@Service
class CompleteWeightTicketService(
  private val weightTickets: WeightTickets,
) : CompleteWeightTicket {
  override fun handle(cmd: CompleteWeightTicketCommand) {
    val weightTicket = weightTickets.findById(cmd.weightTicketId)
      ?: throw EntityNotFoundException("Weegbon met nummer ${cmd.weightTicketId.number} bestaat niet")

    weightTicket.complete()
    weightTickets.save(weightTicket)
  }
}
