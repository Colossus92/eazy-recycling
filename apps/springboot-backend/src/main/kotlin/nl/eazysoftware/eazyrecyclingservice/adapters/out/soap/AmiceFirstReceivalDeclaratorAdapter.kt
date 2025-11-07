package nl.eazysoftware.eazyrecyclingservice.adapters.out.soap

import kotlinx.datetime.number
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.Bedrijf
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.EersteOntvangstMeldingDetails
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.Ontdoener
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Consignor
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteCollectionType
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.FirstReceivalDeclarator
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ReceivalDeclaration
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * SOAP adapter for first receival declarations using the Amice Melding service.
 * This is an outbound adapter in hexagonal architecture.
 */
@Component
class AmiceFirstReceivalDeclaratorAdapter(
  private val sessionService: AmiceSessionService,
  private val companies: Companies
): FirstReceivalDeclarator {

  private val logger = LoggerFactory.getLogger(javaClass)

  override fun declareFirstReceivals(
    receivalDeclarations: List<ReceivalDeclaration>
  ) {
    logger.info("Declaring first receival for waste streams: ${receivalDeclarations.joinToString(", ") { it.wasteStream.wasteStreamNumber.number }}")

    val message = receivalDeclarations.map { mapToSoapMessage(it) }

    sessionService.declareFirstReceivals(message)
  }

  @Suppress("DuplicatedCode")
  private fun mapToSoapMessage(
    receivalDeclaration: ReceivalDeclaration
  ): EersteOntvangstMeldingDetails {
    val message = EersteOntvangstMeldingDetails()
    val wasteStream = receivalDeclaration.wasteStream

    // Basic waste stream information
    message.afvalstroomNummer = wasteStream.wasteStreamNumber.number
    message.isRouteInzameling = wasteStream.collectionType == WasteCollectionType.ROUTE
    message.isInzamelaarsRegeling = wasteStream.collectionType == WasteCollectionType.COLLECTORS_SCHEME

    // Consignor (Ontdoener)
    message.ontdoener = mapConsignor(wasteStream.consignorParty)

    // Pickup location (Locatie Herkomst)
    mapPickupLocation(wasteStream.pickupLocation, message)

    // Delivery location (Locatie Ontvangst)
    message.locatieOntvangst = wasteStream.deliveryLocation.processorPartyId.number

    // Consignor party (Afzender)
    val consignorParty = wasteStream.consignorParty
    if (consignorParty is Consignor.Company) {
      message.afzender = mapCompanyToBedrijf(consignorParty.id)
    }

    // Collector (Inzamelaar)
    wasteStream.collectorParty?.let {
      message.inzamelaar = mapCompanyToBedrijf(it)
    }

    // Dealer (Handelaar)
    wasteStream.dealerParty?.let {
      message.handelaar = mapCompanyToBedrijf(it)
    }

    // Broker (Bemiddelaar)
    wasteStream.brokerParty?.let {
      message.bemiddelaar = mapCompanyToBedrijf(it)
    }

    // Transporters (Vervoerders) - comma-separated list of chamber of commerce numbers
    message.vervoerders = receivalDeclaration.transporters
      .mapNotNull { it.chamberOfCommerceId }
      .joinToString(",")

    // Waste type information
    message.afvalstof = wasteStream.wasteType.euralCode.code.replace(" ", "")
    message.gebruikelijkeNaamAfvalstof = wasteStream.wasteType.name
    message.verwerkingsMethode = wasteStream.wasteType.processingMethod.code.replace(".", "")

    // Declaration details
    message.totaalGewicht = receivalDeclaration.totalWeight
    message.aantalVrachten = receivalDeclaration.totalShipments

    // Period in format MMYYYY (e.g., 112025 for November 2025)
    val yearMonth = receivalDeclaration.yearMonth
    message.periodeMelding = "${yearMonth.month.number.toString().padStart(2, '0')}${yearMonth.year}"

    return message
  }

  @Suppress("DuplicatedCode")
  private fun mapConsignor(consignor: Consignor): Ontdoener {
    val ontdoener = Ontdoener()

    when (consignor) {
      is Consignor.Company -> {
        val company = companies.findById(consignor.id)
        if (company != null) {
          ontdoener.handelsregisternummer = company.chamberOfCommerceId
          if (company.address.country != "Nederland") {
            ontdoener.naam = company.name
          }
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

  @Suppress("DuplicatedCode")
  private fun mapPickupLocation(location: Location, message: EersteOntvangstMeldingDetails) {
    when (location) {
      is Location.DutchAddress -> {
        message.locatieHerkomstPostcode = location.postalCode().value
        message.locatieHerkomstHuisnummer = location.buildingNumber()
        message.locatieHerkomstHuisnummerToevoeging = location.buildingNumberAddition()
        message.locatieHerkomstWoonplaats = location.city()
        message.locatieHerkomstStraatnaam = location.streetName()
        message.locatieHerkomstLand = location.country()
      }
      is Location.ProximityDescription -> {
        message.locatieHerkomstPostcode = location.postalCodeDigits
        message.locatieHerkomstWoonplaats = location.city.value
        message.locatieHerkomstNabijheidsBeschrijving = location.description
        message.locatieHerkomstLand = location.country
      }
      is Location.Company -> {
        message.locatieHerkomstPostcode = location.address.postalCode.value
        message.locatieHerkomstHuisnummer = location.address.buildingNumber
        message.locatieHerkomstHuisnummerToevoeging = location.address.buildingNumberAddition
        message.locatieHerkomstWoonplaats = location.address.city.value
        message.locatieHerkomstStraatnaam = location.address.streetName.value
        message.locatieHerkomstLand = location.address.country
      }
      is Location.ProjectLocationSnapshot -> {
        message.locatieHerkomstPostcode = location.postalCode().value
        message.locatieHerkomstHuisnummer = location.buildingNumber()
        message.locatieHerkomstHuisnummerToevoeging = location.buildingNumberAddition()
        message.locatieHerkomstWoonplaats = location.city().value
        message.locatieHerkomstStraatnaam = location.streetName()
        message.locatieHerkomstLand = location.country()
      }
      is Location.NoLocation -> {
        // No location data to map
      }
    }
  }

  @Suppress("DuplicatedCode")
  private fun mapCompanyToBedrijf(companyId: CompanyId): Bedrijf {
    val bedrijf = Bedrijf()
    val company = companies.findById(companyId)

    if (company != null) {
      bedrijf.handelsregisternummer = company.chamberOfCommerceId
      if (company.address.country != "Nederland") {
        bedrijf.naam = company.name
      }
      bedrijf.land = company.address.country
    }

    return bedrijf
  }
}
