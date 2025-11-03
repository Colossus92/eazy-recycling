package nl.eazysoftware.eazyrecyclingservice.controller.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class AddressRequest(
    @field:NotBlank
    @field:Max(value = 43, message = "Straatnaam mag maximaal 43 tekens bevatten")
    val streetName: String,
    @field:Max(value = 6, message = "Toevoeging mag maximaal 6 tekens bevatten")
    val buildingNumberAddition: String? = null,
    @field:NotBlank
    @field:Max(value = 10, message = "Huisnummer mag maximaal 10 tekens bevatten")
    val buildingNumber: String,
    @field:Pattern(regexp = "^\\d{4}\\s[A-Z]{2}$", message = "Postcode moet bestaan uit 4 cijfers gevolgd door een spatie en 2 hoofdletters")
    val postalCode: String,
    @field:NotBlank
    @field:Max(value = 24, message = "Stad mag maximaal 24 tekens bevatten")
    val city: String,
    @field:NotBlank
    @field:Max(value = 40, message = "Land mag maximaal 40 tekens bevatten")
    val country: String = "Nederland",
)
