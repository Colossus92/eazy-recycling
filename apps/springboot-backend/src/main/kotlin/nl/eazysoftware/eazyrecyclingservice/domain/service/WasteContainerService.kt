package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer.WasteContainer
import nl.eazysoftware.eazyrecyclingservice.repository.wastecontainer.WasteContainerRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service

@Service
class WasteContainerService(
  private val wasteContainerRepository: WasteContainerRepository,
) {

  @Transactional
  fun createContainer(container: WasteContainer) {
    if (wasteContainerRepository.findById(container.wasteContainerId.id) != null) {
      throw DuplicateKeyException("Container met kenmerk ${container.wasteContainerId.id} bestaat al")
    }
    wasteContainerRepository.save(container)
  }

  fun getAllContainers(): List<WasteContainer> {
    return wasteContainerRepository.findAll()
      .sortedBy { it.wasteContainerId.id }
  }

  fun getContainerById(id: String): WasteContainer {
    return wasteContainerRepository.findById(id)
      ?: throw EntityNotFoundException("Container met id $id niet gevonden")
  }

  @Transactional
  fun updateContainer(id: String, container: WasteContainer): WasteContainer {
    wasteContainerRepository.findById(id)
      ?: throw EntityNotFoundException("Container met id $id niet gevonden")

    wasteContainerRepository.save(container)

    return container
  }

  fun deleteContainer(id: String) {
    wasteContainerRepository.deleteById(id)
  }
}
