package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.application.query.GetAllWeightTickets
import nl.eazysoftware.eazyrecyclingservice.application.query.GetWeightTicketByNumber
import nl.eazysoftware.eazyrecyclingservice.application.query.GetWeightTicketPdfUrlByTransport
import nl.eazysoftware.eazyrecyclingservice.application.query.WeightTicketDetailView
import nl.eazysoftware.eazyrecyclingservice.application.query.WeightTicketListView
import nl.eazysoftware.eazyrecyclingservice.application.query.WeightTicketPdfUrlView
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class WeightTicketService(
  private val getAllWeightTickets: GetAllWeightTickets,
  private val getWeightTicketByNumber: GetWeightTicketByNumber,
  private val getWeightTicketPdfUrlByTransport: GetWeightTicketPdfUrlByTransport
) {

  fun getAllWeightTickets(): List<WeightTicketListView> {
    return getAllWeightTickets.execute()
  }

  fun getWeightTicketByNumber(weightTicketId: WeightTicketId): WeightTicketDetailView {
    return getWeightTicketByNumber.execute(weightTicketId)
      ?: throw EntityNotFoundException("Geen weegbon gevonden voor nummer $weightTicketId")
  }

  fun getWeightTicketPdfUrlByTransport(transportId: UUID): WeightTicketPdfUrlView? {
    return getWeightTicketPdfUrlByTransport.execute(transportId)
  }

}
