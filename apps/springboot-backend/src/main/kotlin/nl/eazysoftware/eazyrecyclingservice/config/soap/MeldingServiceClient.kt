package nl.eazysoftware.eazyrecyclingservice.config.soap

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.MeldingSessie
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.MeldingSessieResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.ws.client.core.WebServiceTemplate
import org.springframework.ws.soap.client.core.SoapActionCallback

@Component
class MeldingServiceClient(
  private val webServiceTemplate: WebServiceTemplate,
) {

  @Value("\${amice.url:}")
  private lateinit var amiceBaseUrl: String

  private val logger = LoggerFactory.getLogger(javaClass)

  private val soapAction = "http://amice.lma.nl/AmiceWebServices3/MeldingSessie"

  fun apply(body: MeldingSessie): MeldingSessieResponse {
    logger.info("Calling SOAP service to declare waste streams")

    return webServiceTemplate.marshalSendAndReceive(
      "$amiceBaseUrl/MeldingService.asmx",
      body,
      SoapActionCallback(soapAction)
    ) as MeldingSessieResponse
  }
}
