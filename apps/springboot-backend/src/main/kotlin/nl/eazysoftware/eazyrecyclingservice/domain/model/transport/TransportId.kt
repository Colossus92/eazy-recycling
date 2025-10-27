package nl.eazysoftware.eazyrecyclingservice.domain.model.transport

import java.util.UUID

data class TransportId(
  val uuid: UUID,
) {
  companion object {
    /**
     * Generate a new TransportId with a random UUID.
     * This is used when creating new transports in the domain layer.
     */
    fun generate(): TransportId {
      return TransportId(UUID.randomUUID())
    }
  }
}
