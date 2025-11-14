package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.EersteOntvangstMeldingDetails
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.MaandelijkseOntvangstMeldingDetails
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.OpvragenResultaatVerwerkingMeldingSessieResponse
import java.util.*

interface AmiceSessions {

  fun retrieve(sessionId: UUID): OpvragenResultaatVerwerkingMeldingSessieResponse
  fun declareFirstReceivals(firstReceivals: List<EersteOntvangstMeldingDetails>): Boolean
  fun declareMonthlyReceivals(monthlyReceivals: List<MaandelijkseOntvangstMeldingDetails>): Boolean
}
