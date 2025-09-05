package nl.eazysoftware.eazyrecyclingservice.controller.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class AddressRequest(
    @field:NotBlank
    val streetName: String,
    val buildingName: String? = null,
    @field:NotBlank
    val buildingNumber: String,
    @field:Pattern(regexp = "^\\d{4}\\s[A-Z]{2}$", message = "Postcode moet bestaan uit 4 cijfers gevolgd door een spatie en 2 hoofdletters")
    val postalCode: String,
    @field:NotBlank
    val city: String,
    @field:NotBlank
    val country: String = "Nederland",
)