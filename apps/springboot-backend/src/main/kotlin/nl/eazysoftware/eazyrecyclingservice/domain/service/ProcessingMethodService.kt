package nl.eazysoftware.eazyrecyclingservice.domain.service

import nl.eazysoftware.eazyrecyclingservice.repository.ProcessingMethodRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.ProcessingMethod
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class ProcessingMethodService(
  private val processingMethodRepository: ProcessingMethodRepository
) {

  fun findAll(): List<ProcessingMethod> =
    processingMethodRepository.findAll()

  fun create(processingMethod: ProcessingMethod) =
    processingMethodRepository.save(processingMethod)

  fun update(code: String, processingMethod: ProcessingMethod): Any {
    if (code != processingMethod.code) {
      throw IllegalArgumentException("De code van de verwerkingsmethode kan niet gewijzigd worden.")
    }

    processingMethodRepository.findByIdOrNull(code)
      ?: throw IllegalArgumentException("Verwerkingsmethode met $code niet gevonden. Maak een nieuwe verwerkingsmethode aan indien nodig.")

    return processingMethodRepository.save(processingMethod)
  }

  fun delete(code: String) =
    processingMethodRepository.deleteById(code)

}
