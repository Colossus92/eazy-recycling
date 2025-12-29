package nl.eazysoftware.eazyrecyclingservice.application.usecase.wastedeclaration

import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.Bedrijf
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.EersteOntvangstMeldingDetails
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.Ontdoener
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.isNetherlands
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Consignor
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteCollectionType
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStream
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import org.springframework.stereotype.Component

/**
 * Reusable mapper for creating fully populated EersteOntvangstMeldingDetails SOAP messages.
 * This ensures consistent data population across different use cases (scheduled jobs, corrective declarations, etc.).
 */
@Component
class FirstReceivalMessageMapper(
  private val companies: Companies
) {

  /**
   * Maps waste stream and declaration data to a fully populated EersteOntvangstMeldingDetails SOAP message.
   *
   * @param declarationId Unique identifier for this declaration (meldingsNummerMelder)
   * @param wasteStream The waste stream being declared
   * @param transporters List of transporter VIHB numbers
   * @param totalWeight Total weight in kg
   * @param totalShipments Number of shipments
   * @param yearMonth The period being declared (format: MMYYYY)
   * @return Fully populated EersteOntvangstMeldingDetails ready for SOAP submission
   */
  fun mapToSoapMessage(
    declarationId: String,
    wasteStream: WasteStream,
    transporters: List<String>,
    totalWeight: Int,
    totalShipments: Short,
    yearMonth: YearMonth
  ): EersteOntvangstMeldingDetails {
    val message = EersteOntvangstMeldingDetails()

    message.meldingsNummerMelder = declarationId
    message.afvalstroomNummer = wasteStream.wasteStreamNumber.number
    message.isRouteInzameling = wasteStream.collectionType == WasteCollectionType.ROUTE
    message.isInzamelaarsRegeling = wasteStream.collectionType == WasteCollectionType.COLLECTORS_SCHEME

    message.ontdoener = mapConsignor(wasteStream.consignorParty)

    mapPickupLocation(wasteStream.pickupLocation, message)

    message.locatieOntvangst = wasteStream.deliveryLocation.processorPartyId.number

    val consignorParty = wasteStream.consignorParty
    if (consignorParty is Consignor.Company) {
      message.afzender = mapCompanyToBedrijf(consignorParty.id)
    }

    wasteStream.collectorParty?.let {
      message.inzamelaar = mapCompanyToBedrijf(it)
    }

    wasteStream.dealerParty?.let {
      message.handelaar = mapCompanyToBedrijf(it)
    }

    wasteStream.brokerParty?.let {
      message.bemiddelaar = mapCompanyToBedrijf(it)
    }

    message.vervoerders = transporters.joinToString(",")

    message.afvalstof = wasteStream.wasteType.euralCode.code.replace(" ", "")
    message.gebruikelijkeNaamAfvalstof = wasteStream.wasteType.name
    message.verwerkingsMethode = wasteStream.wasteType.processingMethod.code.replace(".", "")

    message.totaalGewicht = totalWeight
    message.aantalVrachten = totalShipments

    message.periodeMelding = "${yearMonth.month.number.toString().padStart(2, '0')}${yearMonth.year}"

    return message
  }

  private fun mapConsignor(consignor: Consignor): Ontdoener {
    val ontdoener = Ontdoener()

    when (consignor) {
      is Consignor.Company -> {
        val company = companies.findById(consignor.id)
        if (company != null) {
          ontdoener.handelsregisternummer = company.chamberOfCommerceId
          val country = company.address.country.trim()
          if (!country.isNetherlands()) {
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

  private fun mapPickupLocation(location: Location, message: EersteOntvangstMeldingDetails) {
    when (location) {
      is Location.DutchAddress -> {
        message.locatieHerkomstPostcode = location.postalCode().value
        message.locatieHerkomstHuisnummer = location.buildingNumber()
        message.locatieHerkomstHuisnummerToevoeging = location.buildingNumberAddition()
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
      }
    }
  }

  private fun mapCompanyToBedrijf(companyId: CompanyId): Bedrijf {
    val bedrijf = Bedrijf()
    val company = companies.findById(companyId)

    if (company != null) {
      bedrijf.handelsregisternummer = company.chamberOfCommerceId
      val country = company.address.country.trim()
      if (!country.isNetherlands()) {
        bedrijf.naam = company.name
      }
      bedrijf.land = company.address.country
    }

    return bedrijf
  }
}
