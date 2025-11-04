package nl.eazysoftware.eazyrecyclingservice.repository.wastecontainer

import nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer.WasteContainer
import nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer.WasteContainers
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

interface WasteContainerJpaRepository: JpaRepository<WasteContainerDto, String>

@Repository
class WasteContainerRepository(
  private val jpaRepository: WasteContainerJpaRepository,
  private val wasteContainerMapper: WasteContainerMapper,
): WasteContainers {
  override fun findAll() =
    jpaRepository.findAll()
      .map { wasteContainerMapper.toDomain(it) }

  override fun findById(id: String) =
    jpaRepository.findByIdOrNull(id)
      ?.let { wasteContainerMapper.toDomain(it) }

  override fun deleteById(id: String) {
    jpaRepository.deleteById(id)
  }

  override fun save(container: WasteContainer) {
    jpaRepository.save(wasteContainerMapper.toDto(container))
  }
}
