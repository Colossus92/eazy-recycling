package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.MeldingSessieResponseDetails
import nl.eazysoftware.eazyrecyclingservice.repository.jobs.LmaDeclarationSessionDto

interface LmaDeclarationSessions {

  fun saveFirstReceivalSession(meldingSessieResponseDetails: MeldingSessieResponseDetails, ids: List<String>)
  fun saveMonthlyReceivalSession(meldingSessieResponseDetails: MeldingSessieResponseDetails, ids: List<String>)
  fun findPending(): List<LmaDeclarationSessionDto>
  fun save(session: LmaDeclarationSessionDto): LmaDeclarationSessionDto
}
