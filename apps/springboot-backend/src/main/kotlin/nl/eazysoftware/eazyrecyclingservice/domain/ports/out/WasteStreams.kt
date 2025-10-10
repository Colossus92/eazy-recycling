package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStream
import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamDto

/**
 * Domain port for WasteStream repository following hexagonal architecture
 */
interface WasteStreams {
  fun findByNumber(wasteStreamNumber: WasteStreamNumber): WasteStream?

  fun deleteAll()

  fun deleteByNumber(wasteStreamNumber: WasteStreamNumber)

  fun save(wasteStreamDto: WasteStreamDto)

  fun existsById(wasteStreamNumber: WasteStreamNumber): Boolean
}
