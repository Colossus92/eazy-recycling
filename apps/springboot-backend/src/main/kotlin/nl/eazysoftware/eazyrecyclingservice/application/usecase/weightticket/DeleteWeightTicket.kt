package nl.eazysoftware.eazyrecyclingservice.application.usecase.weightticket

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import nl.eazysoftware.eazyrecyclingservice.domain.weightticket.WeightTicketId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface DeleteWeightTicket {
  fun handle(cmd: DeleteWeightTicketCommand)
}

data class DeleteWeightTicketCommand(
  val weightTicketId: WeightTicketId
)

@Service
class DeleteWeightTicketService(
  private val weightTickets: WeightTickets
) : DeleteWeightTicket {

  @Transactional
  override fun handle(cmd: DeleteWeightTicketCommand) {
    val weightTicket = weightTickets.findById(cmd.weightTicketId)
      ?: throw EntityNotFoundException("Weegbon met nummer ${cmd.weightTicketId.number} bestaat niet")

    weightTicket.delete()
    weightTickets.save(weightTicket)
  }
}
