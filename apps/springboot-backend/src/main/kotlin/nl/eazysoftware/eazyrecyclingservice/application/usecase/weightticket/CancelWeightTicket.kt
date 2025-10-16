package nl.eazysoftware.eazyrecyclingservice.application.usecase.weightticket

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.CancellationReason
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface CancelWeightTicket {
  fun handle(cmd: CancelWeightTicketCommand)
}

data class CancelWeightTicketCommand(
  val weightTicketId: WeightTicketId,
  val cancellationReason: CancellationReason,
)

@Service
class CancelWeightTicketService(
  private val weightTickets: WeightTickets
) : CancelWeightTicket {

  @Transactional
  override fun handle(cmd: CancelWeightTicketCommand) {
    val weightTicket = weightTickets.findById(cmd.weightTicketId)
      ?: throw EntityNotFoundException("Weegbon met nummer ${cmd.weightTicketId.number} bestaat niet")

    weightTicket.cancel(cmd.cancellationReason)
    weightTickets.save(weightTicket)
  }
}
