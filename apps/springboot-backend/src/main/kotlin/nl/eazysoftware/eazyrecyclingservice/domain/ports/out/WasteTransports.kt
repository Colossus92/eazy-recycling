package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportId
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.WasteTransport
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId

/**
 * Port for waste transport persistence
 */
interface WasteTransports {
  fun save(wasteTransport: WasteTransport): WasteTransport
  fun findById(transportId: TransportId): WasteTransport?
  fun findAll(): List<WasteTransport>
  fun delete(transportId: TransportId)
  fun findByWeightTicketId(weightTicketId: WeightTicketId): List<WasteTransport>
}
