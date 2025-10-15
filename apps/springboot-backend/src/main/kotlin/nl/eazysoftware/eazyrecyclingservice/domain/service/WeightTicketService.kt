package nl.eazysoftware.eazyrecyclingservice.domain.service

import nl.eazysoftware.eazyrecyclingservice.application.query.GetAllWeightTickets
import nl.eazysoftware.eazyrecyclingservice.application.query.WeightTicketListView
import org.springframework.stereotype.Service

@Service
class WeightTicketService(
    private val getAllWeightTickets: GetAllWeightTickets,
) {

  fun getAllWeightTickets(): List<WeightTicketListView> {
    return getAllWeightTickets.execute()
  }

}
