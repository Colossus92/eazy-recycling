package nl.eazysoftware.eazyrecyclingservice.adapters.out.soap

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Consignor
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteCollectionType
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStream
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.*
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * SOAP adapter for waste stream validation using the Amice ToetsenAfvalstroomnummer service.
 * This is an outbound adapter in hexagonal architecture.
 *
 * Only active when amice.enabled=true
 */
@Component
@ConditionalOnProperty(name = ["amice.enabled"], havingValue = "true", matchIfMissing = false)
class AmiceWasteStreamValidatorAdapter(
  private val soapClient: ToetsenAfvalstroomNummerServiceSoap?,
  private val companies: Companies
) : WasteStreamValidator {

  private val logger = LoggerFactory.getLogger(javaClass)

  override fun validate(wasteStream: WasteStream): WasteStreamValidationResult {
    logger.info("Validating waste stream number: ${wasteStream.wasteStreamNumber.number}")

    if (soapClient == null) {
      logger.error("Amice SOAP client is not configured. Please set amice.url environment variable.")
      return WasteStreamValidationResult.invalid(
        wasteStreamNumber = wasteStream.wasteStreamNumber.number,
        errors = listOf(ValidationError("SOAP_NOT_CONFIGURED", "Amice validatieservice is niet geconfigureerd")),
        requestData = null
      )
    }

    return try {
      val request = mapToSoapRequest(wasteStream)

      logger.info("Calling SOAP service to validate waste stream")
      val response = soapClient.toetsenAfvalstroomNummer(request)
      logger.info("Received response from SOAP service")

      val result = mapToValidationResult(response)
      logger.info("Validation result: isValid=${result.isValid}, errors=${result.errors.size}")
      result
    } catch (e: Exception) {
      logger.error("Error calling SOAP validation service for waste stream ${wasteStream.wasteStreamNumber.number}", e)
      logger.error("Exception type: ${e.javaClass.name}")
      logger.error("Exception message: ${e.message}")
      e.printStackTrace()
      WasteStreamValidationResult.invalid(
        wasteStreamNumber = wasteStream.wasteStreamNumber.number,
        errors = listOf(ValidationError("SOAP_ERROR", "Fout bij aanroepen validatieservice: ${e.javaClass.simpleName} - ${e.message}")),
        requestData = null
      )
    }
  }

  private fun mapToSoapRequest(wasteStream: WasteStream): ToetsenAfvalstroomNummer {
    val request = ToetsenAfvalstroomNummer()

    // Waste stream number
    request.afvalstroomNummer = wasteStream.wasteStreamNumber.number

    // Collection type flags
    request.isRouteInzameling = wasteStream.collectionType == WasteCollectionType.ROUTE
    request.isInzamelaarsRegeling = wasteStream.collectionType == WasteCollectionType.COLLECTORS_SCHEME

    // Consignor (Ontdoener)
    request.ontdoener = mapConsignor(wasteStream.consignorParty)

    // Pickup location (Locatie Herkomst)
    mapPickupLocation(wasteStream.pickupLocation, request)

    // Delivery location (Locatie Ontvangst)
    request.locatieOntvangst = wasteStream.deliveryLocation.processorPartyId.number

    // Consignor party (Afzender)
    val consignorParty = wasteStream.consignorParty
    if (consignorParty is Consignor.Company) {
      request.afzender = mapCompanyToBedrijf(consignorParty.id)
    }

    // Collector (Inzamelaar)
    wasteStream.collectorParty?.let {
      request.inzamelaar = mapCompanyToBedrijf(it)
    }

    // Dealer (Handelaar)
    wasteStream.dealerParty?.let {
      request.handelaar = mapCompanyToBedrijf(it)
    }

    // Broker (Bemiddelaar)
    wasteStream.brokerParty?.let {
      request.bemiddelaar = mapCompanyToBedrijf(it)
    }

    // Waste type information
    request.afvalstof = wasteStream.wasteType.euralCode.code
    request.gebruikelijkeNaamAfvalstof = wasteStream.wasteType.name
    request.verwerkingsMethode = wasteStream.wasteType.processingMethod.code

    return request
  }

  private fun mapConsignor(consignor: Consignor): Ontdoener {
    val ontdoener = Ontdoener()

    when (consignor) {
      is Consignor.Company -> {
        val company = companies.findById(consignor.id)
        if (company != null) {
          ontdoener.handelsregisternummer = company.chamberOfCommerceId
          ontdoener.naam = company.name
          ontdoener.land = company.address.country
        }
        ontdoener.isIsParticulier = false
      }
      is Consignor.Person -> {
        ontdoener.isIsParticulier = true
      }
    }

    return ontdoener
  }

  private fun mapPickupLocation(location: Location, request: ToetsenAfvalstroomNummer) {
    when (location) {
      is Location.DutchAddress -> {
        request.locatieHerkomstPostcode = location.postalCode().value
        request.locatieHerkomstHuisnummer = location.buildingNumber()
        request.locatieHerkomstHuisnummerToevoeging = location.buildingNumberAddition()
        request.locatieHerkomstWoonplaats = location.city()
        request.locatieHerkomstStraatnaam = location.streetName()
        request.locatieHerkomstLand = location.country()
      }
      is Location.ProximityDescription -> {
        request.locatieHerkomstPostcode = location.postalCodeDigits
        request.locatieHerkomstWoonplaats = location.city.value
        request.locatieHerkomstNabijheidsBeschrijving = location.description
        request.locatieHerkomstLand = location.country
      }
      is Location.Company -> {
        request.locatieHerkomstPostcode = location.address.postalCode.value
        request.locatieHerkomstHuisnummer = location.address.buildingNumber
        request.locatieHerkomstHuisnummerToevoeging = location.address.buildingNumberAddition
        request.locatieHerkomstWoonplaats = location.address.city.value
        request.locatieHerkomstStraatnaam = location.address.streetName.value
        request.locatieHerkomstLand = location.address.country
      }
      is Location.ProjectLocationSnapshot -> {
        request.locatieHerkomstPostcode = location.postalCode().value
        request.locatieHerkomstHuisnummer = location.buildingNumber()
        request.locatieHerkomstHuisnummerToevoeging = location.buildingNumberAddition()
        request.locatieHerkomstWoonplaats = location.city().value
        request.locatieHerkomstStraatnaam = location.streetName()
        request.locatieHerkomstLand = location.country()
      }
      is Location.NoLocation -> {
        // No location data to map
      }
    }
  }

  private fun mapCompanyToBedrijf(companyId: nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId): Bedrijf {
    val bedrijf = Bedrijf()
    val company = companies.findById(companyId)

    if (company != null) {
      bedrijf.handelsregisternummer = company.chamberOfCommerceId
      bedrijf.naam = company.name
      bedrijf.land = company.address.country
    }

    return bedrijf
  }

  private fun mapToValidationResult(response: ToetsenAfvalstroomNummerResponse): WasteStreamValidationResult {
    val result = response.toetsenAfvalstroomNummerResult

    if (result == null) {
      return WasteStreamValidationResult.invalid(
        wasteStreamNumber = response.toetsenAfvalstroomNummerResult.toetsenAfvalstroomNummerRetourBerichtDetails.aanvraagGegevens.afvalstroomNummer,
        errors = listOf(ValidationError("NO_RESULT", "Geen resultaat ontvangen van validatieservice")),
        requestData = null
      )
    }

    val details = result.toetsenAfvalstroomNummerRetourBerichtDetails
    val errors = details?.fout?.map {
      ValidationError(
        code = it.foutCode ?: "UNKNOWN",
        description = it.foutomschrijving ?: "Onbekende fout"
      )
    } ?: emptyList()

    val isValid = details?.afvalstroomGegevensValide?.equals("Ja", ignoreCase = true) ?: false

    val requestData = details?.aanvraagGegevens?.let { mapRequestData(it) }

    return if (isValid) {
      WasteStreamValidationResult.valid(requestData!!)
    } else {
      WasteStreamValidationResult.invalid(
        wasteStreamNumber = response.toetsenAfvalstroomNummerResult.toetsenAfvalstroomNummerRetourBerichtDetails.aanvraagGegevens.afvalstroomNummer,
        errors,
        requestData
      )
    }
  }

  private fun mapRequestData(aanvraag: AanvraagGegevens): ValidationRequestData {
    return ValidationRequestData(
      wasteStreamNumber = aanvraag.afvalstroomNummer ?: "",
      routeCollection = aanvraag.routeInzameling,
      collectorsScheme = aanvraag.inzamelaarsRegeling,
      consignor = aanvraag.ontdoener?.let {
        ConsignorData(
          companyRegistrationNumber = it.handelsregisternummer,
          name = it.naam,
          country = it.land,
          isPrivate = it.isIsParticulier
        )
      },
      pickupLocation = PickupLocationData(
        postalCode = aanvraag.locatieHerkomstPostcode,
        buildingNumber = aanvraag.locatieHerkomstHuisnummer,
        buildingNumberAddition = aanvraag.locatieHerkomstHuisnummerToevoeging,
        city = aanvraag.locatieHerkomstWoonplaats,
        streetName = aanvraag.locatieHerkomstStraatnaam,
        proximityDescription = aanvraag.locatieHerkomstNabijheidsBeschrijving,
        country = aanvraag.locatieHerkomstLand
      ),
      deliveryLocation = aanvraag.locatieOntvangst,
      consignorParty = aanvraag.afzender?.let {
        CompanyData(
          companyRegistrationNumber = it.handelsregisternummer,
          name = it.naam,
          country = it.land
        )
      },
      collectorParty = aanvraag.inzamelaar?.let {
        CompanyData(
          companyRegistrationNumber = it.handelsregisternummer,
          name = it.naam,
          country = it.land
        )
      },
      dealerParty = aanvraag.handelaar?.let {
        CompanyData(
          companyRegistrationNumber = it.handelsregisternummer,
          name = it.naam,
          country = it.land
        )
      },
      brokerParty = aanvraag.bemiddelaar?.let {
        CompanyData(
          companyRegistrationNumber = it.handelsregisternummer,
          name = it.naam,
          country = it.land
        )
      },
      wasteCode = aanvraag.afvalstof,
      wasteName = aanvraag.gebruikelijkeNaamAfvalstof,
      processingMethod = aanvraag.verwerkingsMethode
    )
  }
}
