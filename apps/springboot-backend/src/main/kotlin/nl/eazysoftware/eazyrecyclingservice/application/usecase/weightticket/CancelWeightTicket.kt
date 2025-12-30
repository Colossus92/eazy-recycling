package nl.eazysoftware.eazyrecyclingservice.application.usecase.weightticket

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.CancellationReason
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface CancelWeightTicket {
  fun handle(cmd: CancelWeightTicketCommand)
}

data class CancelWeightTicketCommand(
  val weightTicketNumber: Long,
  val cancellationReason: CancellationReason,
)

@Service
class CancelWeightTicketService(
  private val weightTickets: WeightTickets
) : CancelWeightTicket {

  @Transactional
  override fun handle(cmd: CancelWeightTicketCommand) {
    val weightTicket = weightTickets.findByNumber(cmd.weightTicketNumber)
      ?: throw EntityNotFoundException("Weegbon met nummer ${cmd.weightTicketNumber} bestaat niet")

    weightTicket.cancel(cmd.cancellationReason)
    weightTickets.save(weightTicket)
  }
}
