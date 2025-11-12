package nl.eazysoftware.eazyrecyclingservice.adapters.out.soap

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.*
import nl.eazysoftware.eazyrecyclingservice.config.soap.MeldingServiceClient
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.AmiceSessions
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaDeclarationSessions
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class AmiceSessionService(
  private val meldingServiceClient: MeldingServiceClient,
  private val lmaDeclarationSessions: LmaDeclarationSessions,
) : AmiceSessions {

  private val logger = LoggerFactory.getLogger(javaClass)

  @Transactional
  override fun declareFirstReceivals(firstReceivals: List<EersteOntvangstMeldingDetails>): Boolean {
    val content = EersteOntvangstMeldingenDetails()
    content.eersteOntvangstMelding.addAll(firstReceivals)
    val melding = MeldingSessie()
    melding.isRetourberichtViaEmail = false
    melding.eersteOntvangstMeldingen = content

    val response = meldingServiceClient.apply(melding)
    lmaDeclarationSessions.saveFirstReceivalSession(response.meldingSessieResponseDetails, firstReceivals.map { it.meldingsNummerMelder })

    try {
      val success = response.meldingSessieResponseDetails.isMeldingSessieResult
      logger.info("First receival declaration result: success=$success, sessionId=${response.meldingSessieResponseDetails.meldingSessieUUID}, response=$response")

      if (!success) {
        throw IllegalStateException("Het doen van een eerste ontvangst melding is niet gelukt")
      }
    } catch (e: Exception) {
      logger.error("Error calling SOAP melding service for waste streams ${firstReceivals.joinToString(", ") { it.afvalstroomNummer }}", e)
      throw e
    }

    return response.meldingSessieResponseDetails.isMeldingSessieResult
  }

  override fun retrieve(sessionId: UUID): OpvragenResultaatVerwerkingMeldingSessieResponse {
    val request = OpvragenResultaatVerwerkingMeldingSessie()
    request.meldingSessieUUID = sessionId.toString()


    return meldingServiceClient.requestStatus(request)
  }
}
