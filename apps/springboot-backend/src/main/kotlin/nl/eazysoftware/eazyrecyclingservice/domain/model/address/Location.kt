package nl.eazysoftware.eazyrecyclingservice.domain.model.address

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.*

/**
 * Origin location of waste with three possible variants based on Dutch regulations
 *
 * According to the Dutch regulations, also a foreign address is allowed, but this is currently not supported.
 */
sealed interface Location {

  /**
   * Variant 1: Dutch address with full postal code
   * Requires: NL + postal code (4 digits + 2 letters) + house number
   */
  data class DutchAddress(
    val address: Address
  ) : Location {
    init {
      require(address.country == "Nederland") { "Het land dient Nederland te zijn, maar was: ${address.country}" }
    }

    fun streetName(): String = address.streetName
    fun buildingNumber(): String = address.buildingNumber
    fun buildingNumberAddition(): String? = address.buildingNumberAddition
    fun postalCode(): DutchPostalCode = address.postalCode
    fun city(): String = address.city
    fun country(): String = address.country
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
  ) : Location {
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
  data object NoLocation : Location

  /**
   * Variant 4: Company location reference
   * References a company's address as the pickup location
   */
  data class Company(
    val companyId: CompanyId,
    val name: String,
    val address: Address,
  ) : Location

  /**
   * Variant 5: Project/Branch location
   * A specific physical address that belongs to a company (e.g., construction site, branch office, project location)
   * Combines a full Dutch address with company ownership
   */
  data class ProjectLocation(
    val id: UUID,
    val companyId: CompanyId,
    val address: Address,
  ) : Location {
    init {
      require(address.country == "Nederland") { "Het land dient Nederland te zijn, maar was: ${address.country}" }
    }

    fun streetName(): String = address.streetName
    fun buildingNumber(): String = address.buildingNumber
    fun buildingNumberAddition(): String? = address.buildingNumberAddition
    fun postalCode(): DutchPostalCode = address.postalCode
    fun city(): String = address.city
    fun country(): String = address.country


    /**
     * Entity equality based on identity (ID), not attributes
     * Two ProjectLocations are the same entity if they have the same ID
     */
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is ProjectLocation) return false

      return id == other.id
    }

    override fun hashCode(): Int {
      var result = id.hashCode()
      result = 31 * result + companyId.hashCode()
      result = 31 * result + address.hashCode()
      return result
    }
  }
}

@Component
class LocationFactory(
  private val companyRepository: CompanyRepository,
) {

  fun create(
    companyId: CompanyId? = null,
    streetName: String? = null,
    buildingNumber: String? = null,
    buildingNumberAddition: String? = null,
    postalCode: String? = null,
    description: String? = null,
    city: String? = null,
  ): Location {

    if (description?.isNotBlank() == true) return ProximityDescription(
      postalCodeDigits = postalCode
        ?: throw IllegalArgumentException("De vier cijfers van een postcode zijn verplicht bij een nabijheidsbeschrijving"),
      city = city ?: throw IllegalArgumentException("De stad is verplicht bij een nabijheidsbeschrijving"),
      description = description,
      country = "Nederland"
    )
    if (companyId != null && streetName?.isNotBlank() == true) return ProjectLocation(
      id = UUID.randomUUID(),
      companyId = companyId,
      address = Address(
        streetName = streetName,
        postalCode = postalCode
          ?.let { DutchPostalCode(it) }
          ?: throw IllegalArgumentException("De postcode is verplicht "),
        buildingNumber = buildingNumber
          ?: throw IllegalArgumentException("De vier cijfers van een postcode zijn verplicht bij een nabijheidsbeschrijving"),
        buildingNumberAddition = buildingNumberAddition,
        city = city ?: throw IllegalArgumentException("De stad is verplicht bij een nabijheidsbeschrijving"),
        country = "Nederland"
      )
    )

    if (companyId != null) {
      val company = companyRepository.findByIdOrNull(companyId.uuid)
        ?: throw IllegalArgumentException("Geen bedrijf gevonden met verwerkersnummer: $companyId")
      return Company(
        companyId = companyId,
        name = company.name,
        address = Address(
          streetName = company.address.streetName
            ?: throw IllegalArgumentException("Bedrijf heeft geen straatnaam, maar dit is verplicht"),
          postalCode = DutchPostalCode(company.address.postalCode),
          buildingNumber = company.address.buildingNumber,
          buildingNumberAddition = company.address.buildingName,
          city = company.address.city
            ?: throw IllegalArgumentException("Bedrijf heeft geen stad, maar dit is verplicht"),
          country = "Nederland"
        )
      )
    }

    if (streetName?.isNotBlank() == true) return DutchAddress(
      address = Address(
        streetName = streetName,
        postalCode = postalCode
          ?.let { DutchPostalCode(it) }
          ?: throw IllegalArgumentException("De postcode is verplicht "),
        buildingNumber = buildingNumber
          ?: throw IllegalArgumentException("De vier cijfers van een postcode zijn verplicht bij een nabijheidsbeschrijving"),
        buildingNumberAddition = buildingNumberAddition,
        city = city ?: throw IllegalArgumentException("De stad is verplicht bij een nabijheidsbeschrijving"),
        country = "Nederland"
      )
    )

    return NoLocation
  }
}
