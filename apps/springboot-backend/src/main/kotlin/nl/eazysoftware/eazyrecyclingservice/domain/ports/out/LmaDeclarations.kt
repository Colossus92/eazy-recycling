package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.EersteOntvangstMeldingDetails
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.MaandelijkseOntvangstMeldingDetails
import nl.eazysoftware.eazyrecyclingservice.repository.jobs.LmaDeclarationDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface LmaDeclarations {
  fun saveAll(declarations: List<LmaDeclarationDto>): List<LmaDeclarationDto>
  fun findByIds(ids: List<String>): List<LmaDeclarationDto>
  fun saveAllPendingFirstReceivals(firstReceivals: List<EersteOntvangstMeldingDetails>)
  fun saveAllPendingMonthlyReceivals(monthlyReceivals: List<MaandelijkseOntvangstMeldingDetails>)
  fun findAll(pageable: Pageable): Page<LmaDeclarationDto?>
}
