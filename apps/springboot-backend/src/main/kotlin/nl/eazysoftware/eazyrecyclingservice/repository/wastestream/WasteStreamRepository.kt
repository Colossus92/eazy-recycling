package nl.eazysoftware.eazyrecyclingservice.repository.wastestream

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProcessorPartyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStream
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

interface WasteStreamJpaRepository : JpaRepository<WasteStreamDto, String> {

  /**
   * Finds the highest waste stream number for a given processor.
   * Uses string comparison with LIKE to filter by processor prefix, then finds MAX.
   */
  @Query("""
    SELECT w.number
    FROM WasteStreamDto w
    WHERE w.number LIKE CONCAT(:processorId, '%')
    ORDER BY w.number DESC
    LIMIT 1
  """)
  fun findHighestNumberByProcessorId(@Param("processorId") processorId: String): String?
}


@Repository
class WasteStreamRepository(
  private val jpaRepository: WasteStreamJpaRepository,
  private val wasteStreamMapper: WasteStreamMapper,
) : WasteStreams {
  override fun findByNumber(wasteStreamNumber: WasteStreamNumber): WasteStream? {
    return jpaRepository.findByIdOrNull(wasteStreamNumber.number)
      ?.let { wasteStreamMapper.toDomain(it) }
  }

  override fun existsById(wasteStreamNumber: WasteStreamNumber) =
    jpaRepository.existsById(wasteStreamNumber.number)

  override fun deleteAll() {
    jpaRepository.deleteAll()
  }

  override fun deleteByNumber(wasteStreamNumber: WasteStreamNumber) {
    jpaRepository.deleteById(wasteStreamNumber.number)
  }

  override fun save(wasteStream: WasteStream) {
    val dto = wasteStreamMapper.toDto(wasteStream)
    jpaRepository.save(dto)
  }

  override fun findHighestNumberForProcessor(processorId: ProcessorPartyId): WasteStreamNumber? {
    return jpaRepository.findHighestNumberByProcessorId(processorId.number)
      ?.let { WasteStreamNumber(it) }
  }
}
