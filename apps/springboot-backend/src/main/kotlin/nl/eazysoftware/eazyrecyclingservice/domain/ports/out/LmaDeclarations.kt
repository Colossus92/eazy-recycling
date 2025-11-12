package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.EersteOntvangstMeldingDetails
import nl.eazysoftware.eazyrecyclingservice.repository.jobs.LmaDeclarationDto

interface LmaDeclarations {
  fun saveAll(declarations: List<LmaDeclarationDto>): List<LmaDeclarationDto>
  fun findByIds(ids: List<String>): List<LmaDeclarationDto>
  fun saveAllPending(firstReceivals: List<EersteOntvangstMeldingDetails>)
}
