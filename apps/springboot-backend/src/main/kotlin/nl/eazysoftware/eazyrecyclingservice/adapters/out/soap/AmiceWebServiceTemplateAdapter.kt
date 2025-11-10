package nl.eazysoftware.eazyrecyclingservice.adapters.out.soap

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.*
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.ws.client.core.WebServiceTemplate
import org.springframework.ws.soap.client.SoapFaultClientException
import org.springframework.ws.soap.client.core.SoapActionCallback

/**
 * Amice SOAP service adapter using Spring Web Services WebServiceTemplate.
 *
 * This is an ALTERNATIVE implementation to the JAX-WS approach.
 * Uses Apache HttpClient for proper client certificate authentication.
 *
 * Advantages:
 * - Apache HttpClient handles client certificates correctly
 * - Clean, testable code
 * - Better error handling
 * - Proper SSL/TLS configuration
 *
 * Only active when amice.enabled=true
 */
@Component
@ConditionalOnProperty(name = ["amice.enabled"], havingValue = "true", matchIfMissing = false)
class AmiceWebServiceTemplateAdapter(
  private val webServiceTemplate: WebServiceTemplate,
) {

  private val logger = LoggerFactory.getLogger(javaClass)

  /**
   * Test the MeldingSessie endpoint with a simple request.
   * This is useful for testing client certificate authentication.
   */
  fun testMeldingSessie(): MeldingSessieResponse {
    logger.info("Testing MeldingSessie endpoint")

    val request = MeldingSessie().apply {
      isRetourberichtViaEmail = false
      eersteOntvangstMeldingen = EersteOntvangstMeldingenDetails()
    }

    val soapAction = "http://amice.lma.nl/AmiceWebServices3/MeldingSessie"

    return try {
      val response = webServiceTemplate.marshalSendAndReceive(
        "https://test.lma.nl/Amice.WebService3//MeldingService.asmx",
        request,
        SoapActionCallback(soapAction)
      ) as MeldingSessieResponse

      logger.info("MeldingSessie test successful")
      response
    } catch (e: Exception) {
      logger.error("MeldingSessie test failed", e)
      throw e
    }
  }

  /**
   * Declare first receivals without session using WebServiceTemplate.
   * This sends raw SOAP messages directly to the Melding endpoint.
   *
   * @param messages Array of first receival messages to send
   */
  fun declareWithoutSession(messages: ArrayOfMessageEersteOntvangstMelding) {
    logger.info("Declaring {} first receival message(s) without session", messages.messageEersteOntvangstMelding.size)

    val request = EersteOntvangstMelding()
    request.meldingen = messages

    val soapAction = "http://amice.lma.nl/AmiceWebServices3/MeldingSessie"

    return try {
      val response = webServiceTemplate.marshalSendAndReceive(
        "https://test.lma.nl/Amice.WebService3//MeldingService.asmx",
        request,
        SoapActionCallback(soapAction)
      ) as MeldingSessieResponse

      logger.info("Successfully declared {} first receival message(s)", messages.messageEersteOntvangstMelding.size)
    } catch (e: SoapFaultClientException) {
      // Extract SOAP Fault details
      logger.error("SOAP Fault received: faultCode={}, faultString={}",
        e.faultCode, e.faultStringOrReason)
      logger.error("SOAP Fault detail: {} {}", e.faultCode, e.faultStringOrReason)
      throw RuntimeException("SOAP Fault: ${e.faultStringOrReason}", e)
    } catch (e: Exception) {
      logger.error("Failed to declare first receivals without session", e)
      throw e
    }
  }
}
