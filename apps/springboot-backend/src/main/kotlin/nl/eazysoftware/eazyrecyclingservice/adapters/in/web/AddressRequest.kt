package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.City
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.StreetName

data class AddressRequest(
  @field:NotBlank val street: String,
  @field:NotBlank val buildingNumber: String,
  val buildingNumberAddition: String? = null,
  @field:Pattern(regexp = "^[1-9][0-9]{3}\\s?[A-Z]{2}$",
    message = "Invalid Dutch postal code")
  val postalCode: String,
  @field:NotBlank val city: String,
  @field:NotBlank val country: String
) {
  fun toDomain() = Address(
    StreetName(street),
    buildingNumber,
    buildingNumberAddition,
    DutchPostalCode(postalCode),
    City(city),
    country)
}
