package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.weightticket.WeightTicket
import nl.eazysoftware.eazyrecyclingservice.domain.weightticket.WeightTicketId

interface WeightTickets {
  fun nextId(): WeightTicketId
  fun save(aggregate: WeightTicket): WeightTicket
  fun findById(id: WeightTicketId): WeightTicket?
}
