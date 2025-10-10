package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import nl.eazysoftware.eazyrecyclingservice.domain.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.address.DutchPostalCode

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
    street,
    buildingNumber,
    buildingNumberAddition,
    DutchPostalCode(postalCode),
    city,
    country)
}
