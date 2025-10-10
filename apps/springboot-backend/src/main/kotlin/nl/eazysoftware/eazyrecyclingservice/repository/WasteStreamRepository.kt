package nl.eazysoftware.eazyrecyclingservice.repository

import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStream
import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.WasteStreamDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.WasteStreamMapper
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

  override fun saveAll(wasteStreams: List<WasteStreamDto>) {
    jpaRepository.saveAll(wasteStreams)
  }

  override fun save(wasteStreamDto: WasteStreamDto) =
    jpaRepository.save(wasteStreamDto)
}
