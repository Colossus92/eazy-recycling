package nl.eazysoftware.eazyrecyclingservice.domain.model.address

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProjectLocationId

/**
 * Origin location of waste with three possible variants based on Dutch regulations
 *
 * According to the Dutch regulations, also a foreign address is allowed, but this is currently not supported.
 */
sealed interface Location {

  fun toAddressLine(): String

  /**
   * Variant 1: Dutch address with full postal code
   * Requires: NL + postal code (4 digits + 2 letters) + house number
   */
  data class DutchAddress(
    val address: Address
  ) : Location {
    init {
      require(address.country == "Nederland" || address.country == "NL") { "Het land dient Nederland te zijn, maar was: ${address.country}" }
    }

    fun streetName(): String = address.streetName.value
    fun buildingNumber(): String = address.buildingNumber
    fun buildingNumberAddition(): String? = address.buildingNumberAddition
    fun postalCode(): DutchPostalCode = address.postalCode
    fun city(): String = address.city.value
    fun country(): String = address.country

    override fun toAddressLine() =
      streetName() + " " + buildingNumber() + " " + buildingNumberAddition() + ", " + city()
  }


  /**
   * Variant 2: Proximity description for unusual addressing
   * Requires: NL + numeric postal code (4 digits) + city + proximity description
   */
  data class ProximityDescription(
    val postalCodeDigits: String,
    val city: City,
    val description: String,
    val country: String = "Nederland"
  ) : Location {
    override fun toAddressLine() = "$description, $city"

    init {
      require(postalCodeDigits.matches(Regex("\\d{4}"))) {
        "Voor een nabijheidsbeschrijving moet de postcode alleen vier cijfers bevatten, maar was: $postalCodeDigits"
      }
      require(description.isNotBlank()) { "De nabijheidsbeschrijving is verplicht" }
      require(description.length <= 200) {
        "De nabijheidsbeschrijving mag maximaal 200 tekens bevatten, maar was: ${description.length}"
      }
    }
  }

  /**
   * Variant 3: No origin location for route collection, collectors scheme, or private disposers
   */
  data object NoLocation : Location {
    override fun toAddressLine() = "Geen locatie"
  }

  /**
   * Variant 4: Company location reference
   * References a company's address as the pickup location
   */
  data class Company(
    val companyId: CompanyId,
    val name: String,
    val address: Address,
  ) : Location {
    override fun toAddressLine() = address.toAddressLine()
  }

  /**
   * Variant 5: Project/Branch location
   * A specific physical address that belongs to a company (e.g., construction site, branch office, project location)
   * Combines a full Dutch address with company ownership
   */
  data class ProjectLocationSnapshot(
    val projectLocationId: ProjectLocationId,
    val companyId: CompanyId,
    val address: Address,
  ) : Location {
    init {
      require(address.country == "Nederland") { "Het land dient Nederland te zijn, maar was: ${address.country}" }
    }

    fun streetName(): String = address.streetName.value
    fun buildingNumber(): String = address.buildingNumber
    fun buildingNumberAddition(): String? = address.buildingNumberAddition
    fun postalCode(): DutchPostalCode = address.postalCode
    fun city(): City = address.city
    fun country(): String = address.country

    override fun toAddressLine() = address.toAddressLine()
  }
}
