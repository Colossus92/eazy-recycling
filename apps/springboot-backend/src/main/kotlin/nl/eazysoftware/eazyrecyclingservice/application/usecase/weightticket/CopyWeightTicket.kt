package nl.eazysoftware.eazyrecyclingservice.application.usecase.weightticket

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface CopyWeightTicket {
  fun handle(cmd: CopyWeightTicketCommand): WeightTicketId
}

/**
 * Command for copying a weight ticket.
 * It will create a new weight ticket with the same data as the original,
 * but with a new ID and status set to DRAFT.
 */
data class CopyWeightTicketCommand(
  val originalWeightTicketNumber: Long,
)

@Service
class CopyWeightTicketService(
  private val weightTickets: WeightTickets,
) : CopyWeightTicket {

  @Transactional
  override fun handle(cmd: CopyWeightTicketCommand): WeightTicketId {
    // 1. Load aggregate from repository
    val originalTicket = weightTickets.findByNumber(cmd.originalWeightTicketNumber)
      ?: throw EntityNotFoundException("Weegbon met nummer ${cmd.originalWeightTicketNumber} bestaat niet")

    // 2. Domain logic creates new aggregate
    val copiedTicket = originalTicket.copy(
      newId = weightTickets.nextId(),
    )

    // 3. Persist new aggregate
    val savedCopiedTicket = weightTickets.save(copiedTicket)

    // 4. Return new aggregate ID
    return savedCopiedTicket.id
  }
}
