package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.ContainerTransport
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportId

/**
 * Port for container transport persistence
 */
interface ContainerTransports {
  fun save(containerTransport: ContainerTransport): ContainerTransport
  fun findById(transportId: TransportId): ContainerTransport?
  fun findAll(): List<ContainerTransport>
  fun delete(transportId: TransportId)
}
