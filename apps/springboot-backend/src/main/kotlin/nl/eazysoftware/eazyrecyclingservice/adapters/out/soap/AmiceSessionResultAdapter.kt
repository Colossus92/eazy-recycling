package nl.eazysoftware.eazyrecyclingservice.adapters.out.soap

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.OpvragenResultaatVerwerkingMeldingSessieResponse
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.SessionResults
import org.springframework.stereotype.Component
import java.util.*

@Component
class AmiceSessionResultAdapter(
  private val sessionService: AmiceSessionService
): SessionResults {
  override fun retrieve(sessionId: UUID): OpvragenResultaatVerwerkingMeldingSessieResponse {
    return sessionService.retrieve(sessionId)
  }
}
