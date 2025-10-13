package nl.eazysoftware.eazyrecyclingservice.domain.waste

import nl.eazysoftware.eazyrecyclingservice.domain.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId

data class WasteStream(
  val wasteStreamNumber: WasteStreamNumber,
  val wasteType: WasteType,
  val collectionType: WasteCollectionType = WasteCollectionType.DEFAULT,
  val pickupLocation: PickupLocation,
  val deliveryLocation: DeliveryLocation,
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
    val hasPickupLocation = pickupLocation !is PickupLocation.NoPickupLocation
    val isDefaultCollection = collectionType == WasteCollectionType.DEFAULT
    val isCompanyConsignor = consignorParty is Consignor.Company

    require(
      (hasPickupLocation && isDefaultCollection && isCompanyConsignor) ||
      (!hasPickupLocation && (!isDefaultCollection || !isCompanyConsignor))
    ) {
      if (hasPickupLocation) {
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

    require(wasteStreamNumber.number.substring(0, 5) == deliveryLocation.processorPartyId.number) {
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
sealed interface PickupLocation {

  /**
   * Variant 1: Dutch address with full postal code
   * Requires: NL + postal code (4 digits + 2 letters) + house number
   */
  data class DutchAddress(
    val streetName: String,
    val postalCode: DutchPostalCode,
    val buildingNumber: String,
    val buildingNumberAddition: String? = null,
    val city: String,
    val country: String = "Nederland"
  ) : PickupLocation {
    init {
      require(buildingNumber.isNotBlank()) { "Het huisnummer is verplicht" }
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
    val description: String,
    val country: String = "Nederland"
  ) : PickupLocation {
    init {
      require(postalCodeDigits.matches(Regex("\\d{4}"))) {
        "Voor een nabijheidsbeschrijving moet de postcode alleen vier cijfers bevatten, maar was: $postalCodeDigits"
      }
      require(city.isNotBlank()) { "De stad moet een waarde hebben" }
      require(description.isNotBlank()) { "De nabijheidsbeschrijving is verplicht" }
    }
  }

  /**
   * Variant 3: No origin location for route collection, collectors scheme, or private disposers
   */
  data object NoPickupLocation : PickupLocation

  /**
   * Variant 4: Company location reference
   * References a company's address as the pickup location
   */
  data class PickupCompany(val companyId: CompanyId) : PickupLocation
}

data class DeliveryLocation(
  val processorPartyId: ProcessorPartyId,
)

data class ProcessorPartyId(
  val number: String
) {
  init {
    require(number.length == 5) {
      "Het verwerkersnummer moet exact 5 tekens lang zijn, maar is: $number"
    }
  }
}


enum class WasteCollectionType {
  DEFAULT,
  ROUTE,
  COLLECTORS_SCHEME,
}

data class WasteStreamNumber(
  val number: String,
) {
  init {
    require(number.length == 12) {
      "Een afvalstroomnummer dient 12 tekens lang te zijn"
    }
  }
}
