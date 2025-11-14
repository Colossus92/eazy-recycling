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
    val content = EersteOntvangstMeldingenDetails().apply {
      eersteOntvangstMelding.addAll(firstReceivals)
    }
    val melding = MeldingSessie().apply {
      isRetourberichtViaEmail = false
      eersteOntvangstMeldingen = content
    }

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

  @Transactional
  override fun declareMonthlyReceivals(monthlyReceivals: List<MaandelijkseOntvangstMeldingDetails>): Boolean {
    val content = MaandelijkseOntvangstMeldingenDetails().apply {
      maandelijkseOntvangstMelding.addAll(monthlyReceivals)
    }
    val melding = MeldingSessie().apply {
      isRetourberichtViaEmail = false
      maandelijkseOntvangstMeldingen = content
    }

    val response = meldingServiceClient.apply(melding)
    lmaDeclarationSessions.saveFirstReceivalSession(response.meldingSessieResponseDetails, monthlyReceivals.map { it.meldingsNummerMelder })

    try {
      val success = response.meldingSessieResponseDetails.isMeldingSessieResult
      logger.info("Monthly receival declaration result: success=$success, sessionId=${response.meldingSessieResponseDetails.meldingSessieUUID}, response=$response")

      if (!success) {
        throw IllegalStateException("Het doen van een maandelijkse ontvangst melding is niet gelukt")
      }
    } catch (e: Exception) {
      logger.error("Error calling SOAP melding service for waste streams ${monthlyReceivals.joinToString(", ") { it.afvalstroomNummer }}", e
      )
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
