package nl.eazysoftware.eazyrecyclingservice.controller.amice

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.AmiceWebServiceTemplateAdapter
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.ArrayOfMessageEersteOntvangstMelding
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.Bedrijf
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.MessageEersteOntvangstMelding
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.Ontdoener
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * Test controller for the WebServiceTemplate approach to SOAP client certificate authentication.
 *
 * This controller allows you to compare the WebServiceTemplate approach with the JAX-WS approach.
 *
 * Endpoints:
 * - POST /api/amice/ws-template/test-toetsen?nummer=XXX - Test ToetsenAfvalstroomNummer
 * - POST /api/amice/ws-template/test-melding - Test MeldingSessie (client cert check)
 * - POST /api/amice/ws-template/declare - Declare first receivals with full body
 *
 * Only active when amice.enabled=true
 */
@RestController
@RequestMapping("/api/amice/ws-template")
@ConditionalOnProperty(name = ["amice.enabled"], havingValue = "true", matchIfMissing = false)
class AmiceWebServiceTemplateTestController(
  private val amiceAdapter: AmiceWebServiceTemplateAdapter
) {

  private val logger = LoggerFactory.getLogger(javaClass)


  /**
   * Test the MeldingSessie endpoint using WebServiceTemplate.
   * This is the endpoint that requires client certificate authentication.
   *
   * Example: POST /api/amice/ws-template/test-melding
   */
  @PostMapping("/test-melding")
  fun testMelding(): ResponseEntity<Map<String, Any?>> {
    logger.info("Testing MeldingSessie with WebServiceTemplate (client certificate check)")

    return try {
      val response = amiceAdapter.testMeldingSessie()

      ResponseEntity.ok(mapOf(
        "success" to true,
        "approach" to "WebServiceTemplate (Apache HttpClient)",
        "message" to "MeldingSessie endpoint accessible with client certificate",
        "responseReceived" to true
      ))
    } catch (e: Exception) {
      logger.error("MeldingSessie test failed with WebServiceTemplate", e)

      val errorMessage = e.message ?: "Unknown error"
      val is403 = errorMessage.contains("403") || errorMessage.contains("Forbidden")

      ResponseEntity.status(if (is403) 403 else 500).body(mapOf(
        "success" to false,
        "approach" to "WebServiceTemplate (Apache HttpClient)",
        "error" to errorMessage,
        "errorType" to e.javaClass.simpleName,
        "likely403ClientCertIssue" to is403
      ))
    }
  }

  /**
   * Declare first receivals using WebServiceTemplate with full SOAP message body.
   *
   * Example: POST /api/amice/ws-template/declare
   * Body: See DirectDeclarationRequest structure below
   */
  @PostMapping("/declare")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  fun declareFirstReceivals(
    @Valid @RequestBody request: DirectDeclarationRequest
  ): DirectDeclarationResponse {
    logger.info("Declaring first receivals with WebServiceTemplate: {} messages", request.messages.size)

    // Build the SOAP message structure
    val arrayOfMessages = ArrayOfMessageEersteOntvangstMelding()

    request.messages.forEach { msg ->
      val soapMessage = MessageEersteOntvangstMelding()

      // Basic fields
      soapMessage.meldingsNummerMelder = msg.meldingsNummerMelder
      soapMessage.afvalstroomNummer = msg.afvalstroomNummer
      soapMessage.isRouteInzameling = msg.routeInzameling
      soapMessage.isInzamelaarsRegeling = msg.inzamelaarsRegeling

      // Ontdoener (Consignor)
      if (msg.ontdoener != null) {
        val ontdoener = Ontdoener()
        ontdoener.handelsregisternummer = msg.ontdoener.handelsregisternummer
        ontdoener.naam = msg.ontdoener.naam
        ontdoener.land = msg.ontdoener.land
        ontdoener.isIsParticulier = msg.ontdoener.isParticulier
        soapMessage.ontdoener = ontdoener
      }

      // Location fields
      soapMessage.locatieHerkomstPostcode = msg.locatieHerkomstPostcode
      soapMessage.locatieHerkomstHuisnummer = msg.locatieHerkomstHuisnummer
      soapMessage.locatieHerkomstHuisnummerToevoeging = msg.locatieHerkomstHuisnummerToevoeging
      soapMessage.locatieHerkomstWoonplaats = msg.locatieHerkomstWoonplaats
      soapMessage.locatieHerkomstStraatnaam = msg.locatieHerkomstStraatnaam
      soapMessage.locatieHerkomstNabijheidsBeschrijving = msg.locatieHerkomstNabijheidsBeschrijving
      soapMessage.locatieHerkomstLand = msg.locatieHerkomstLand
      soapMessage.locatieOntvangst = msg.locatieOntvangst

      // Company parties
      if (msg.afzender != null) {
        val afzender = Bedrijf()
        afzender.handelsregisternummer = msg.afzender.handelsregisternummer
        afzender.naam = msg.afzender.naam
        afzender.land = msg.afzender.land
        soapMessage.afzender = afzender
      }

      if (msg.inzamelaar != null) {
        val inzamelaar = Bedrijf()
        inzamelaar.handelsregisternummer = msg.inzamelaar.handelsregisternummer
        inzamelaar.naam = msg.inzamelaar.naam
        inzamelaar.land = msg.inzamelaar.land
        soapMessage.inzamelaar = inzamelaar
      }

      if (msg.handelaar != null) {
        val handelaar = Bedrijf()
        handelaar.handelsregisternummer = msg.handelaar.handelsregisternummer
        handelaar.naam = msg.handelaar.naam
        handelaar.land = msg.handelaar.land
        soapMessage.handelaar = handelaar
      }

      if (msg.bemiddelaar != null) {
        val bemiddelaar = Bedrijf()
        bemiddelaar.handelsregisternummer = msg.bemiddelaar.handelsregisternummer
        bemiddelaar.naam = msg.bemiddelaar.naam
        bemiddelaar.land = msg.bemiddelaar.land
        soapMessage.bemiddelaar = bemiddelaar
      }

      // Transporters and waste info
      soapMessage.vervoerders = msg.vervoerders
      soapMessage.afvalstof = msg.afvalstof
      soapMessage.gebruikelijkeNaamAfvalstof = msg.gebruikelijkeNaamAfvalstof
      soapMessage.verwerkingsMethode = msg.verwerkingsMethode
      soapMessage.totaalGewicht = msg.totaalGewicht
      soapMessage.aantalVrachten = msg.aantalVrachten
      soapMessage.periodeMelding = msg.periodeMelding

      arrayOfMessages.messageEersteOntvangstMelding.add(soapMessage)
    }

    // Call the service
    amiceAdapter.declareWithoutSession(arrayOfMessages)

    return DirectDeclarationResponse(
      success = true,
      message = "Successfully sent ${request.messages.size} message(s) to Amice using WebServiceTemplate"
    )
  }

  // Request/Response DTOs

  data class DirectDeclarationRequest(
    @field:NotEmpty(message = "At least one message is required")
    val messages: List<SoapMessageRequest>
  )

  data class SoapMessageRequest(
    val meldingsNummerMelder: String?,
    val afvalstroomNummer: String?,
    val routeInzameling: Boolean,
    val inzamelaarsRegeling: Boolean,
    val ontdoener: OntdoenerRequest?,
    val locatieHerkomstPostcode: String?,
    val locatieHerkomstHuisnummer: String?,
    val locatieHerkomstHuisnummerToevoeging: String?,
    val locatieHerkomstWoonplaats: String?,
    val locatieHerkomstStraatnaam: String?,
    val locatieHerkomstNabijheidsBeschrijving: String?,
    val locatieHerkomstLand: String?,
    val locatieOntvangst: String?,
    val afzender: BedrijfRequest?,
    val inzamelaar: BedrijfRequest?,
    val handelaar: BedrijfRequest?,
    val bemiddelaar: BedrijfRequest?,
    val vervoerders: String?,
    val afvalstof: String?,
    val gebruikelijkeNaamAfvalstof: String?,
    val verwerkingsMethode: String?,
    val totaalGewicht: Int,
    val aantalVrachten: Short,
    val periodeMelding: String?
  )

  data class OntdoenerRequest(
    val handelsregisternummer: String?,
    val naam: String?,
    val land: String?,
    val isParticulier: Boolean
  )

  data class BedrijfRequest(
    val handelsregisternummer: String?,
    val naam: String?,
    val land: String?
  )

  data class DirectDeclarationResponse(
    val success: Boolean,
    val message: String
  )
}
