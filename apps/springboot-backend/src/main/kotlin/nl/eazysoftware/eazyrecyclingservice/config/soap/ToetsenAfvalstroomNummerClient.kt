package nl.eazysoftware.eazyrecyclingservice.config.soap

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.toetsen.ToetsenAfvalstroomNummer
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.toetsen.ToetsenAfvalstroomNummerResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.ws.client.core.WebServiceTemplate
import org.springframework.ws.soap.client.core.SoapActionCallback

@Component
class ToetsenAfvalstroomNummerClient(
  private val webServiceTemplate: WebServiceTemplate,
) {

  @Value("\${amice.url:}")
  private lateinit var amiceBaseUrl: String

  private val logger = LoggerFactory.getLogger(javaClass)

  private val soapAction = "http://amice.lma.nl/AmiceWebServices3/ToetsenAfvalstroomNummer"

  fun validate(body: ToetsenAfvalstroomNummer): ToetsenAfvalstroomNummerResponse {
    logger.info("Calling SOAP service to validate waste stream ${body.afvalstroomNummer}")

    return webServiceTemplate.marshalSendAndReceive(
      "$amiceBaseUrl/ToetsenAfvalstroomnummerService.asmx",
      body,
      SoapActionCallback(soapAction)
    ) as ToetsenAfvalstroomNummerResponse
  }
}
