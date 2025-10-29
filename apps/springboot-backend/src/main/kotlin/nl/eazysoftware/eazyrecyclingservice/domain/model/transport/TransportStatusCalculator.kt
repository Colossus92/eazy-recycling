package nl.eazysoftware.eazyrecyclingservice.domain.model.transport

/**
 * Domain service for calculating transport status.
 * 
 * Following DDD principles:
 * - This is a stateless domain service
 * - Encapsulates business logic that doesn't naturally fit in an entity
 * - Can be reused across different transport types (ContainerTransport, WasteTransport)
 * 
 * Business Rules:
 * 1. A transport is UNPLANNED if it lacks a driver or truck
 * 2. A transport is FINISHED if transport hours have been registered
 * 3. A transport is PLANNED if it has driver and truck but no hours yet
 */
object TransportStatusCalculator {
  
  /**
   * Calculate the status of a transport based on its current state.
   * 
   * @param transport The transport to calculate status for
   * @return The calculated status
   */
  fun calculateStatus(transport: Transport): TransportStatus {
    // Rule 1: Missing driver or truck means unplanned
    if (transport.driver == null || transport.truck == null) {
      return TransportStatus.UNPLANNED
    }
    
    // Rule 2: Transport hours registered means finished
    if (transport.transportHours != null) {
      return TransportStatus.FINISHED
    }
    
    // Rule 3: Has driver and truck but not finished
    return TransportStatus.PLANNED
  }
}
