package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.config.clock.toDisplayString
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteTransports
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

/**
 * Query to retrieve all waste transports associated with a specific weight ticket.
 * 
 * This query follows hexagonal architecture principles:
 * - Depends on domain port (WasteTransports) rather than infrastructure
 * - Returns view models suitable for presentation layer
 * - Read-only operation with @Transactional(readOnly = true)
 */
interface GetWasteTransportsFromWeightTicket {
  fun execute(weightTicketNumber: Long): List<WeightTicketTransportView>
}

@Service
@Transactional(readOnly = true)
class GetWasteTransportsFromWeightTicketService(
  private val wasteTransports: WasteTransports
) : GetWasteTransportsFromWeightTicket {

  override fun execute(weightTicketNumber: Long): List<WeightTicketTransportView> {
    return wasteTransports.findByWeightTicketNumber(weightTicketNumber)
      .map { transport ->
        WeightTicketTransportView(
          transportId = transport.transportId.uuid,
          displayNumber = transport.displayNumber?.value ?: "",
          pickupDateTime = LocalDateTime.parse(transport.pickupDateTime.toDisplayString()),
          status = transport.getStatus().name,
          weightTicketNumber = weightTicketNumber
        )
      }
  }
}

/**
 * View model representing a transport associated with a weight ticket.
 * Optimized for presentation in the UI.
 */
data class WeightTicketTransportView(
  val transportId: UUID,
  val displayNumber: String,
  val pickupDateTime: LocalDateTime,
  val status: String,
  val weightTicketNumber: Long
)
