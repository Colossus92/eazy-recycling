package nl.eazysoftware.eazyrecyclingservice.repository.wastestream

import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStream
import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStreamNumber
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

interface WasteStreamJpaRepository : JpaRepository<WasteStreamDto, String>


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
}
