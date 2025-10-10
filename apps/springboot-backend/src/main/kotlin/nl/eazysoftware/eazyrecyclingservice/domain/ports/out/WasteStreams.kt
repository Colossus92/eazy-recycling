package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStream
import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.WasteStreamDto

/**
 * Domain port for WasteStream repository following hexagonal architecture
 */
interface WasteStreams {
  fun findByNumber(wasteStreamNumber: WasteStreamNumber): WasteStream?

  fun deleteAll()

  fun saveAll(wasteStreams: List<WasteStreamDto>)

  fun save(wasteStreamDto: WasteStreamDto): WasteStreamDto //TODO return domain object
}
