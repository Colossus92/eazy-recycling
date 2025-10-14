package nl.eazysoftware.eazyrecyclingservice.domain.waste

import nl.eazysoftware.eazyrecyclingservice.domain.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId

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
