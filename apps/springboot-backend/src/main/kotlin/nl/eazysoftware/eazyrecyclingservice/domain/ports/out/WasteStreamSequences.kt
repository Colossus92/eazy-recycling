package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProcessorPartyId

/**
 * Domain port for accessing waste stream number sequences.
 * Supports multi-tenancy by providing separate sequences per processor.
 */
interface WasteStreamSequences {
  /**
   * Gets the next sequence value for a given processor.
   * This operation is atomic and thread-safe.
   *
   * @param processorId The processor party ID (5 digits)
   * @return The next sequence value (1-9999999)
   * @throws IllegalStateException if the maximum sequence value is exceeded
   */
  fun nextValue(processorId: ProcessorPartyId): Long
}
