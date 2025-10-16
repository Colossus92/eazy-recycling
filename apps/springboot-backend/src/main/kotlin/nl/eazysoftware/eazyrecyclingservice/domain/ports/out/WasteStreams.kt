package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProcessorPartyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStream
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber

/**
 * Domain port for WasteStream repository following hexagonal architecture
 */
interface WasteStreams {
  fun findByNumber(wasteStreamNumber: WasteStreamNumber): WasteStream?

  fun deleteAll()

  fun deleteByNumber(wasteStreamNumber: WasteStreamNumber)

  fun save(wasteStream: WasteStream)

  fun existsById(wasteStreamNumber: WasteStreamNumber): Boolean

  /**
   * Finds the highest waste stream number for a given processor.
   * Used for generating sequential waste stream numbers.
   *
   * @param processorId The processor party ID (5 digits)
   * @return The highest WasteStreamNumber for this processor, or null if none exist
   */
  fun findHighestNumberForProcessor(processorId: ProcessorPartyId): WasteStreamNumber?
}
