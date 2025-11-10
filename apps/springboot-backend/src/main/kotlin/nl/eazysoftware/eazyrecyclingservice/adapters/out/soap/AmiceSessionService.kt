package nl.eazysoftware.eazyrecyclingservice.adapters.out.soap

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AmiceSessionService(
  private val soapClient: MeldingServiceSoap,
) {

  private val logger = LoggerFactory.getLogger(javaClass)

  fun declareFirstReceivals(firstReceivals: List<EersteOntvangstMeldingDetails>): Boolean {
    val content = EersteOntvangstMeldingenDetails()
    content.eersteOntvangstMelding.addAll(firstReceivals)

    val response = soapClient.meldingSessie(
      false,
      content,
      null,
      null,
      null,
    )

    try {
      val success = response.isMeldingSessieResult
      logger.info("First receival declaration result: success=$success, sessionId=${response.meldingSessieUUID}, response=$response")

      if (!success) {
        throw IllegalStateException("First receival declaration failed")
      }
    } catch (e: Exception) {
      logger.error("Error calling SOAP melding service for waste streams ${firstReceivals.joinToString(", ") { it.afvalstroomNummer }}", e)
      throw e
    }

    return response.isMeldingSessieResult
  }

  fun declareMonthlyReceivals(monthlyReceivals: List<MaandelijkseOntvangstMeldingDetails>): Boolean {
    val content = MaandelijkseOntvangstMeldingenDetails()
    content.maandelijkseOntvangstMelding.addAll(monthlyReceivals)
    val response = soapClient.meldingSessie(
      false,
      null,
      content,
      null,
      null,
    )

    return response.isMeldingSessieResult
  }
}
