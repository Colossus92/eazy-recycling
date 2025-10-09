package nl.eazysoftware.eazyrecyclingservice.domain.waste

import nl.eazysoftware.eazyrecyclingservice.domain.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId

data class WasteStream(
  val wasteStreamNumber: WasteStreamNumber,
  val wasteType: WasteType,
  val collectionType: WasteCollectionType = WasteCollectionType.DEFAULT,
  val originLocation: OriginLocation,
  val destinationLocation: DestinationLocation,
  /**
   * Dutch: Afzender
   */
  val consignorParty: Consignor,
  /**
   * Dutch: Ontdoener
   */
  val pickupParty: CompanyId,
  /**
   * Dutch: Handelaar
   */
  val dealerParty: CompanyId? = null,
  /**
   * Dutch: Inzamelaar
   */
  val collectorParty: CompanyId? = null,
  /**
   * Dutch: Bemiddelaar
   */
  val brokerParty: CompanyId? = null,
) {
  init {
    require(collectorParty == null || brokerParty == null) {
      "Een afvalstroomnummer kan niet zowel een handelaar als een bemiddelaar hebben"
    }

    // Origin location validation: either has location with default collection + company,
    // or no location with non-default collection or person
    val hasOriginLocation = originLocation !is OriginLocation.NoOriginLocation
    val isDefaultCollection = collectionType == WasteCollectionType.DEFAULT
    val isCompanyConsignor = consignorParty is Consignor.Company

    require(
      (hasOriginLocation && isDefaultCollection && isCompanyConsignor) ||
      (!hasOriginLocation && (!isDefaultCollection || !isCompanyConsignor))
    ) {
      if (hasOriginLocation) {
        "Locatie van herkomst is alleen toegestaan bij normale inzameling en zakelijke ontdoener"
      } else {
        "Locatie van herkomst is verplicht bij normale inzameling en zakelijke ontdoener"
      }
    }

    require( collectionType == WasteCollectionType.DEFAULT || collectorParty != null) {
      "Als er RouteInzameling of InzamelaarsRegeling wordt toegepast dan moet de inzamelaar zijn gevuld"
    }

    require(consignorParty is Consignor.Company || collectionType == WasteCollectionType.DEFAULT) {
      "Als de ontdoener een particulier is dan mag route inzameling en inzamelaarsregeling niet worden toegepast"
    }

    require(wasteStreamNumber.number.toString().substring(0, 5) == destinationLocation.processorPartyId.number.toString()) {
      "De eerste 5 posities van het Afvalstroomnummer moeten gelijk zijn aan de LocatieOntvangst."
    }


  }
}

sealed interface Consignor {
  data class Company(val id: CompanyId) : Consignor
  data object Person : Consignor
}

/**
 * Origin location of waste with three possible variants based on Dutch regulations
 *
 * According to the Dutch regulations, also a foreign address is allowed, but this is currently not supported.
 */
sealed interface OriginLocation {

  /**
   * Variant 1: Dutch address with full postal code
   * Requires: NL + postal code (4 digits + 2 letters) + house number
   */
  data class DutchAddress(
    val postalCode: DutchPostalCode,
    val houseNumber: String,
    val houseNumberAddition: String? = null,
    val country: String = "Nederland"
  ) : OriginLocation {
    init {
      require(houseNumber.isNotBlank()) { "Het huisnummer is verplicht" }
      require(country == "Nederland") { "Het land dient Nederland te zijn, maar was: $country" }
    }
  }

  /**
   * Variant 2: Proximity description for unusual addressing
   * Requires: NL + numeric postal code (4 digits) + city + proximity description
   */
  data class ProximityDescription(
    val postalCodeDigits: String,
    val city: String,
    val proximityDescription: String,
    val country: String = "Nederland"
  ) : OriginLocation {
    init {
      require(postalCodeDigits.matches(Regex("\\d{4}"))) {
        "Voor een nabijheidsbeschrijving moet de postcode alleen vier cijfers bevatten, maar was: $postalCodeDigits"
      }
      require(city.isNotBlank()) { "De stad moet een waarde hebben" }
      require(proximityDescription.isNotBlank()) { "De nabijheidsbeschrijving is verplicht" }
    }
  }

  /**
   * Variant 3: No origin location for route collection, collectors scheme, or private disposers
   */
  data object NoOriginLocation : OriginLocation
}

data class DestinationLocation(
  val processorPartyId: ProcessorPartyId,
  val addressDto: Address,
)

data class ProcessorPartyId(
  val number: Int
) {
  init {
    require(number.toString().length == 5) {
      "Het verwerkersnummer moet exact 5 tekens lang staan, maar is: $number"
    }
  }
}


enum class WasteCollectionType {
  DEFAULT,
  ROUTE,
  COLLECTORS_SCHEME,
}

data class WasteStreamNumber(
  val number: Long,
)
