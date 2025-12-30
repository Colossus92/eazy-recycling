package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.application.query.GetAllWeightTickets
import nl.eazysoftware.eazyrecyclingservice.application.query.GetWeightTicketByNumber
import nl.eazysoftware.eazyrecyclingservice.application.query.WeightTicketDetailView
import nl.eazysoftware.eazyrecyclingservice.application.query.WeightTicketListView
import org.springframework.stereotype.Service

@Service
class WeightTicketService(
  private val getAllWeightTickets: GetAllWeightTickets,
  private val getWeightTicketByNumber: GetWeightTicketByNumber,
) {

  fun getAllWeightTickets(): List<WeightTicketListView> {
    return getAllWeightTickets.execute()
  }

  fun getWeightTicketByNumber(weightTicketNumber: Long): WeightTicketDetailView {
    return getWeightTicketByNumber.execute(weightTicketNumber)
      ?: throw EntityNotFoundException("Geen weegbon gevonden voor nummer $weightTicketNumber")
  }
}
