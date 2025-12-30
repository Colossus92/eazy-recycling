package nl.eazysoftware.eazyrecyclingservice.application.usecase.weightticket

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface SplitWeightTicket {
  fun handle(cmd: SplitWeightTicketCommand): WeightTicketId
}

/**
 * Command for splitting a weight ticket.
 * It will split the weight ticket into two weight tickets based on the given percentages.
 */
data class SplitWeightTicketCommand(
  val originalWeightTicketNumber: Long,
  val originalWeightTicketPercentage: Int,
  val newWeightTicketPercentage: Int,
)

@Service
class SplitWeightTicketService(
  private val weightTickets: WeightTickets,
) : SplitWeightTicket {

  @Transactional
  override fun handle(cmd: SplitWeightTicketCommand): WeightTicketId {
    // 1. Load aggregate from repository
    val originalTicket = weightTickets.findByNumber(cmd.originalWeightTicketNumber)
      ?: throw EntityNotFoundException("Weegbon met nummer ${cmd.originalWeightTicketNumber} bestaat niet")

    // 2. Domain logic creates new aggregate
    val newTicket = originalTicket.split(
      newId = weightTickets.nextId(),
      originalPercentage = cmd.originalWeightTicketPercentage,
      newPercentage = cmd.newWeightTicketPercentage,
    )

    // 3. Persist both aggregates
    weightTickets.save(originalTicket) // Modified with new percentages
    val savedNewTicket = weightTickets.save(newTicket)

    // 4. Return new aggregate ID
    return savedNewTicket.id
  }
}
