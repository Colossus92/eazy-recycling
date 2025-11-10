package nl.eazysoftware.eazyrecyclingservice.config.soap

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.MeldingSessie
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.MeldingSessieResponse
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.OpvragenResultaatVerwerkingMeldingSessie
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.OpvragenResultaatVerwerkingMeldingSessieResponse
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

  fun apply(body: MeldingSessie): MeldingSessieResponse {
    logger.info("Calling SOAP service to declare waste streams")

    return marshalSendAndReceive(body, "http://amice.lma.nl/AmiceWebServices3/MeldingSessie") as MeldingSessieResponse
  }

  fun requestStatus(body: OpvragenResultaatVerwerkingMeldingSessie): OpvragenResultaatVerwerkingMeldingSessieResponse {
    logger.info("Calling SOAP service to request status for waste streams")

    return marshalSendAndReceive(body, "http://amice.lma.nl/AmiceWebServices3/OpvragenResultaatVerwerkingMeldingSessie") as OpvragenResultaatVerwerkingMeldingSessieResponse
  }

  private fun marshalSendAndReceive(body: Any, soapAction: String): Any? = webServiceTemplate.marshalSendAndReceive(
    "$amiceBaseUrl/MeldingService.asmx",
    body,
    SoapActionCallback(soapAction)
  )
}
