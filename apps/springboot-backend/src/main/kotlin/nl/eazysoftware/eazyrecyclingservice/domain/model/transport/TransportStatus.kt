package nl.eazysoftware.eazyrecyclingservice.domain.model.transport

/**
 * Domain model for transport status.
 * This is separate from the persistence layer Status enum to maintain clean architecture.
 */
enum class TransportStatus {
  /**
   * Transport has not been assigned a driver or truck yet
   */
  UNPLANNED,
  
  /**
   * Transport has been assigned a driver and truck but not yet completed
   */
  PLANNED,
  
  /**
   * Transport has been completed and hours have been registered
   */
  FINISHED
}
