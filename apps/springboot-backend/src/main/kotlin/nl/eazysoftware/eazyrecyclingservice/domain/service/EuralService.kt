package nl.eazysoftware.eazyrecyclingservice.domain.service

import nl.eazysoftware.eazyrecyclingservice.repository.EuralRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.Eural
import org.springframework.stereotype.Service

@Service
class EuralService(
  private val euralRepository: EuralRepository
) {

  fun getEurals() =
    euralRepository.findAll()

  fun createEural(eural: Eural) =
    euralRepository.save(eural)

  fun deleteEural(code: String) =
    euralRepository.deleteById(code)

}
