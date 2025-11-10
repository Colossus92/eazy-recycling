package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.OpvragenResultaatVerwerkingMeldingSessieResponse
import java.util.*

interface SessionResults {

  fun retrieve(sessionId: UUID): OpvragenResultaatVerwerkingMeldingSessieResponse
}
