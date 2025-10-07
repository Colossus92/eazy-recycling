package nl.eazysoftware.eazyrecyclingservice.domain.service

import nl.eazysoftware.eazyrecyclingservice.repository.EuralRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.Eural
import org.springframework.data.repository.findByIdOrNull
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

  fun updateEural(code: String, eural: Eural): Eural {
    if (eural.code != code) {
      throw IllegalArgumentException("De code van de euralcode kan niet gewijzigd worden.")
    }

    euralRepository.findByIdOrNull(code)
      ?: throw IllegalArgumentException("Euralcode $code niet gevonden. Maak een nieuwe euralcode aan indien nodig.")

    return euralRepository.save(eural)
  }

}
